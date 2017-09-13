/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.tx.BaseTransaction;

/**
 * @since 4.1
 */
class ExpDataDomainFlushAction {

    private final DataDomain domain;
    private DataContext context;
    private ObjectStoreGraphDiff sourceDiff;
    private CompoundDiff resultDiff;
    private DataDomainDBDiffBuilder diffBuilder;

    private Map<ClassDescriptor, DbEntityClassDescriptor> dbEntityClassDescriptorMap = new HashMap<>();
    private Set<ClassDescriptor> descriptors = new TreeSet<>(Comparator.comparing(o -> o.getEntity().getClassName()));
    private Map<ClassDescriptor, Integer> descriptorOrder = new HashMap<>();

    ExpDataDomainFlushAction(DataDomain domain) {
        this.domain = domain;
    }

    GraphDiff flush(DataContext context, GraphDiff changes) {
        if (changes == null) {
            return new CompoundDiff();
        }

        if (!(changes instanceof ObjectStoreGraphDiff)) {
            throw new IllegalArgumentException("Expected 'ObjectStoreGraphDiff', got: " + changes.getClass().getName());
        }

        this.context = context;
        this.sourceDiff = (ObjectStoreGraphDiff) changes;
        this.resultDiff = new CompoundDiff();
        this.diffBuilder = new DataDomainDBDiffBuilder();

        List<ExpBatchRow> rowList = getBatchRows();
        sortRows(rowList);

//        List<Query> queries = new ArrayList<>();
//        rowList.forEach(row -> queries.add(getQueryForRow(row)));

//        runQueries(queries);

        return resultDiff;
    }

    private List<ExpBatchRow> getBatchRows() {
        List<ExpBatchRow> rowList = new ArrayList<>();

        sourceDiff.getChangesByObjectId()
                .forEach((e, v) -> rowList.add(createBatchRow((ObjectId)e, v)));

        return rowList;
    }

    private ExpBatchRow createBatchRow(ObjectId id, ObjectDiff diff) {
        Persistent object = (Persistent) context.getObjectStore().getNode(id);
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(id.getEntityName());
        descriptors.add(descriptor); // TODO side effect; fill collection of unique descriptors here to not look up them twice

        ExpBatchRow row = new ExpBatchRow(object.getPersistenceState(), descriptor, id.getIdSnapshot());
        if(object.getPersistenceState() != PersistenceState.DELETED) {
            diffBuilder.reset(dbEntityClassDescriptorMap.computeIfAbsent(descriptor, DbEntityClassDescriptor::new));
            row.setFullSnapshot(diffBuilder.buildDBDiff(diff));
        }
        return row;
    }

    private void sortRows(List<ExpBatchRow> rowList) {
        AtomicInteger i = new AtomicInteger(0);
        descriptors.forEach(d -> descriptorOrder.put(d, i.getAndIncrement()));
        rowList.sort((o1, o2) -> {
            // default order should be INSERT(2) -> DELETE(6) -> UPDATE(4)
            int res = o1.getType() - o2.getType();
            if(res != 0) {
                return res;
            }

            return descriptorOrder.get(o1.getDescriptor()) - descriptorOrder.get(o2.getDescriptor());
        });
    }

    private BatchQuery getQueryForRow(ExpBatchRow row) {
        switch (row.getType()) {
            case PersistenceState.NEW: {
                InsertBatchQuery insert = new InsertBatchQuery(row.getDescriptor().getEntity().getDbEntity(), 1);
                insert.add(row.getFullSnapshot());
                return insert;
            }

            case PersistenceState.MODIFIED: {
                List<DbAttribute> qualifierAttributes = Collections.emptyList();//row.objectIdSnapshot.keySet();
                List<DbAttribute> modifiedAttributes = Collections.emptyList();//row.fullSnapshot.keySet();
                UpdateBatchQuery update = new UpdateBatchQuery(
                        row.getDescriptor().getEntity().getDbEntity(),
                        qualifierAttributes,
                        modifiedAttributes,
                        Collections.emptySet(),
                        1);
                update.add(row.getObjectIdSnapshot(), row.getFullSnapshot());
                return update;
            }

            case PersistenceState.DELETED: {
                List<DbAttribute> qualifierAttributes = Collections.emptyList();//row.objectIdSnapshot.keySet();
                DeleteBatchQuery delete = new DeleteBatchQuery(
                        row.getDescriptor().getEntity().getDbEntity(),
                        qualifierAttributes,
                        Collections.emptySet(),
                        1);
                delete.add(row.getObjectIdSnapshot());
                return delete;
            }
        }

        throw new CayenneRuntimeException("Invalid object state in flush action: %d; " +
                "only NEW(2), MODIFIED(4) and DELETED(6) are allowed", row.getType());
    }

    private void runQueries(List<Query> queries) {
        DataDomainFlushObserver observer = new DataDomainFlushObserver(
                domain.getJdbcEventLogger());

        // split query list by spanned nodes and run each single node range individually.
        // Since connections are reused per node within an open transaction, there should
        // not be much overhead in accessing the same node multiple times (may happen due
        // to imperfect sorting)

        try {

            DataNode lastNode = null;
            DbEntity lastEntity = null;
            int rangeStart = 0;
            int len = queries.size();

            for (int i = 0; i < len; i++) {

                BatchQuery query = (BatchQuery) queries.get(i);
                if (query.getDbEntity() != lastEntity) {
                    lastEntity = query.getDbEntity();

                    DataNode node = domain.lookupDataNode(lastEntity.getDataMap());
                    if (node != lastNode) {

                        if (i - rangeStart > 0) {
                            lastNode.performQueries(queries.subList(rangeStart, i), observer);
                        }

                        rangeStart = i;
                        lastNode = node;
                    }
                }
            }

            // process last segment of the query list...
            lastNode.performQueries(queries.subList(rangeStart, len), observer);
        } catch (Throwable th) {
            BaseTransaction.getThreadTransaction().setRollbackOnly();
            throw new CayenneRuntimeException("Transaction was rolledback.", th);
        }
    }

}
