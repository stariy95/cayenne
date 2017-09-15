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
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntitySorter;
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
    private List<ExpBatchRow> rowList;

    private Map<ClassDescriptor, DbEntityClassDescriptor> dbEntityClassDescriptorMap = new HashMap<>();
    private Set<ClassDescriptor> descriptors = new TreeSet<>(Comparator.comparing(o -> o.getEntity().getClassName()));

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
        this.rowList = new ArrayList<>();

        buildBatchRows();
        sortBatchRows();

        preprocess();

        List<Query> queries = buildQueries();
        runQueries(queries);

        postprocess();

        return resultDiff;
    }

    private void buildBatchRows() {
        sourceDiff.getChangesByObjectId()
                .forEach((e, v) -> {
                    ObjectId id = (ObjectId) e;
                    rowList.add(createBatchRow(id, v, fetchDescriptor(id)));
                });
    }

    private ClassDescriptor fetchDescriptor(ObjectId id) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(id.getEntityName());
        descriptors.add(descriptor);
        return descriptor;
    }

    private ExpBatchRow createBatchRow(ObjectId id, ObjectDiff diff, ClassDescriptor descriptor) {
        Persistent object = (Persistent) context.getObjectStore().getNode(id);

        ExpBatchRow row = new ExpBatchRow(object.getPersistenceState(), descriptor, id);
        if(object.getPersistenceState() != PersistenceState.DELETED) {
            diffBuilder.reset(dbEntityClassDescriptorMap.computeIfAbsent(descriptor, DbEntityClassDescriptor::new));
            row.setFullSnapshot(diffBuilder.buildDBDiff(diff));
        }
        return row;
    }

    private void sortBatchRows() {
        AtomicInteger i = new AtomicInteger(0);
        EntitySorter sorter = domain.getEntitySorter();
        List<DbEntity> entities = new ArrayList<>(descriptors.size());
        Map<DbEntity, Integer> entityOrder = new HashMap<>(descriptors.size());

        descriptors.forEach(d -> {
            entities.add(d.getEntity().getDbEntity());
        });
        sorter.sortDbEntities(entities, false);
        entities.forEach(e -> entityOrder.put(e, i.getAndIncrement()));

        rowList.sort((o1, o2) -> {
            // default order should be INSERT(2) -> DELETE(6) -> UPDATE(4)
            int res = o1.getType() - o2.getType();
            if(res != 0) {
                return res;
            }

            return entityOrder.get(o1.getDescriptor().getEntity().getDbEntity())
                    - entityOrder.get(o2.getDescriptor().getEntity().getDbEntity());
        });
    }

    private void preprocess() {

        List<ExpBatchRow> changed = new ArrayList<>();
        rowList.forEach(r -> {
            switch (r.getType()) {
                case PersistenceState.NEW:
                    changed.addAll(preprocessInsert(r));
                    break;
                case PersistenceState.MODIFIED:
                    changed.addAll(preprocessUpdate(r));
                    break;
                case PersistenceState.DELETED:
                    changed.addAll(preprocessDelete(r));
                    break;
            }
        });

        rowList.removeAll(changed);
        rowList.addAll(changed);
    }

    private void postprocess() {

    }

    private List<ExpBatchRow> preprocessInsert(ExpBatchRow row) {
        ObjectId id = row.getObjectId();
        if(id == null || !id.isTemporary()) {
            return Collections.emptyList();
        }

        // TODO optimize to batch it (update only when row descriptor changes)
        DbEntity entity = dbEntityClassDescriptorMap.get(row.getDescriptor()).getDbEntity();
        DataNode node = domain.lookupDataNode(entity.getDataMap());
        boolean supportsGeneratedKeys = node.getAdapter().supportsGeneratedKeys();
        PkGenerator pkGenerator = node.getAdapter().getPkGenerator();

        Map<String, Object> idMap = id.getReplacementIdMap();
        boolean autoPkDone = false;

        for (DbAttribute dbAttr : entity.getPrimaryKeys()) {
            String dbAttrName = dbAttr.getName();
            if (idMap.containsKey(dbAttrName)) {
                continue;
            }
            // skip db-generated
            if (supportsGeneratedKeys && dbAttr.isGenerated()) {
                continue;
            }
            // skip propagated
            if (dbAttr.isPropagated()) {
                continue;
            }

            // TODO handle meaningful PK ... should be done in place via code generation in setter...

            // only a single key can be generated from DB... if this is done
            // already in this loop, we must bail out.
            if (autoPkDone) {
                throw new CayenneRuntimeException("Primary Key autogeneration only works for a single attribute.");
            }

            // finally, use database generation mechanism
            autoPkDone = true;
            try {
                idMap.put(dbAttrName, pkGenerator.generatePk(node, dbAttr));
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK: %s", ex,  ex.getMessage());
            }
        }

        return Collections.emptyList();
    }

    private List<ExpBatchRow> preprocessUpdate(ExpBatchRow row) {
        return Collections.emptyList();
    }

    private List<ExpBatchRow> preprocessDelete(ExpBatchRow row) {
        return rowList.stream()
                .filter(r -> r != row
                        && r.getObjectId().getIdSnapshot().equals(row.getObjectId().getIdSnapshot())
                        && r.getType() == PersistenceState.NEW)
                .collect(Collectors.toList());
    }

    private InsertBatchQuery newInsertQuery(ExpBatchRow row) {
        return new InsertBatchQuery(row.getDescriptor().getEntity().getDbEntity(), 16);
    }

    private UpdateBatchQuery newUpdateQuery(ExpBatchRow row) {
        DbEntity dbEntity = row.getDescriptor().getEntity().getDbEntity();
        List<DbAttribute> qualifierAttributes = new ArrayList<>(dbEntity.getPrimaryKeys());
        List<DbAttribute> modifiedAttributes = Collections.emptyList();//row.fullSnapshot.keySet();
        return new UpdateBatchQuery(dbEntity, qualifierAttributes, modifiedAttributes, Collections.emptySet(), 16);
    }

    private DeleteBatchQuery newDeleteQuery(ExpBatchRow row) {
        DbEntity dbEntity = row.getDescriptor().getEntity().getDbEntity();
        List<DbAttribute> qualifierAttributes = new ArrayList<>(dbEntity.getPrimaryKeys());
        return new DeleteBatchQuery(dbEntity, qualifierAttributes, Collections.emptySet(), 16);
    }

    private List<Query> buildQueries() {

        InsertBatchQuery insertQuery = null;
        UpdateBatchQuery updateQuery = null;
        DeleteBatchQuery deleteQuery = null;
        ClassDescriptor lastDescriptor = null;
        int lastType = -1;
        boolean newQuery = false;
        List<Query> queries = new ArrayList<>();

        for(ExpBatchRow r : rowList) {
            if(r.getDescriptor() != lastDescriptor || lastType != r.getType()) {
                lastDescriptor = r.getDescriptor();
                lastType = r.getType();
                newQuery = true;
            }

            switch (r.getType()) {
                case PersistenceState.NEW:
                    if(newQuery) {
                        insertQuery = newInsertQuery(r);
                        queries.add(insertQuery);
                    }
                    insertQuery.add(r.getFullSnapshot(), r.getObjectId());
                    break;

                case PersistenceState.MODIFIED:
                    if(r.getFullSnapshot() == null || r.getFullSnapshot().isEmpty()) {
                        continue; // skip empty change
                    }
                    if(newQuery) {
                        updateQuery = newUpdateQuery(r);
                        queries.add(updateQuery);
                    }
                    updateQuery.add(r.getObjectId().getIdSnapshot(), r.getFullSnapshot());
                    break;

                case PersistenceState.DELETED:
                    if(newQuery) {
                        deleteQuery = newDeleteQuery(r);
                        queries.add(deleteQuery);
                    }
                    deleteQuery.add(r.getObjectId().getIdSnapshot());
                    break;
            }

            newQuery = false;
        }

        return queries;
    }

    private void runQueries(List<Query> queries) {
        DataDomainFlushObserver observer = new DataDomainFlushObserver(domain.getJdbcEventLogger());
        DataNode lastNode = null;
        DbEntity lastEntity = null;
        int rangeStart = 0;
        int len = queries.size();

        try {
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
