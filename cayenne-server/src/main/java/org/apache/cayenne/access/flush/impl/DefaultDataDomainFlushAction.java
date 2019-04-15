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

package org.apache.cayenne.access.flush.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.row.DbRowOpSorter;
import org.apache.cayenne.access.flush.row.DbRowOp;
import org.apache.cayenne.access.flush.row.DbRowOpVisitor;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BatchQuery;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final DbRowOpSorter snapshotSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, DbRowOpSorter snapshotSorter, JdbcEventLogger jdbcEventLogger) {
        this.dataDomain = dataDomain;
        this.snapshotSorter = snapshotSorter;
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {
        CompoundDiff result = new CompoundDiff();
        if (changes == null) {
            return result;
        }
        if(!(changes instanceof ObjectStoreGraphDiff)) {
            throw new CayenneRuntimeException("Instance of ObjectStoreGraphDiff expected, got %s", changes.getClass());
        }

        ObjectStore objectStore = context.getObjectStore();
        ObjectStoreGraphDiff objectStoreGraphDiff = (ObjectStoreGraphDiff) changes;

        Collection<DbRowOp> dbRows = createDbRows(context, objectStoreGraphDiff);
        updateObjectIds(dbRows);
        dbRows = mergeSameObjectIds(dbRows);
        List<DbRowOp> sortedDbRows = sort(dbRows);
        List<BatchQuery> queries = createQueries(sortedDbRows);
        executeQueries(queries);
        createReplacementIds(objectStore, result, sortedDbRows);
        postprocess(context, objectStoreGraphDiff, result, sortedDbRows);

        return result;
    }

    protected List<DbRowOp> sort(Collection<DbRowOp> dbRows) {
        return snapshotSorter.sort(dbRows);
    }

    protected Collection<DbRowOp> createDbRows(DataContext context, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();
        ObjectStore objectStore = context.getObjectStore();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        Map<DbRowOp, DbRowOp> dbRows = new HashMap<>(changesByObjectId.size());
        Set<ArcTarget> processedArcs = new HashSet<>();
        DbRowOpFactory factory = new DbRowOpFactory(resolver, objectStore, processedArcs);

        changesByObjectId.forEach((obj, diff) -> {
            Collection<? extends DbRowOp> rows = factory.createRows(diff);
            rows.forEach(dbRow -> dbRows.compute(dbRow, (key, value) -> {
                if (value != null) {
                    return dbRow.accept(new DbRowOpMerger(value));
                }
                return dbRow;
            }));
        });

        return dbRows.values();
    }

    protected Collection<DbRowOp> mergeSameObjectIds(Collection<DbRowOp> dbRows) {
        Map<EffectiveId, DbRowOp> index = new HashMap<>(dbRows.size());
        dbRows.forEach(row -> index.compute(new EffectiveId(row.getChangeId()),
                (id, value) -> {
                    if(value != null) {
                        return row.accept(new DbRowOpMerger(value));
                    }
                    return row;
                }));
        return index.values();
    }

    protected void updateObjectIds(Collection<DbRowOp> dbRows) {
        DbRowOpVisitor<Void> permIdVisitor = new PermanentObjectIdVisitor(dataDomain);
        dbRows.forEach(row -> row.accept(permIdVisitor));
    }

    protected List<BatchQuery> createQueries(List<DbRowOp> dbRows) {
        QueryCreatorVisitor queryCreator = new QueryCreatorVisitor(dbRows.size());
        dbRows.forEach(row -> row.accept(queryCreator));
        return queryCreator.getQueryList();
    }

    protected void executeQueries(List<BatchQuery> queries) {
        OperationObserver observer = new FlushOperationObserver(jdbcEventLogger);
        queries.stream()
                .collect(Collectors.groupingBy(query -> dataDomain.lookupDataNode(query.getDbEntity().getDataMap())))
                .forEach((node, nodeQueries) -> node.performQueries(nodeQueries, observer));
    }

    protected void createReplacementIds(ObjectStore store, CompoundDiff result, List<DbRowOp> dbRows) {
        ReplacementIdVisitor visitor = new ReplacementIdVisitor(store, dataDomain.getEntityResolver(), result);
        dbRows.forEach(row -> row.accept(visitor));
    }

    protected void postprocess(DataContext context, ObjectStoreGraphDiff changes, CompoundDiff result, List<DbRowOp> dbRows) {
        ObjectStore objectStore = context.getObjectStore();

        PostprocessVisitor postprocessor = new PostprocessVisitor(context);
        dbRows.forEach(row -> row.accept(postprocessor));

        DataDomainIndirectDiffBuilder indirectDiffBuilder = new DataDomainIndirectDiffBuilder(context.getEntityResolver());
        indirectDiffBuilder.processChanges(changes);

        objectStore.getDataRowCache()
                .processSnapshotChanges(
                        objectStore,
                        postprocessor.getUpdatedSnapshots(),
                        postprocessor.getDeletedIds(),
                        Collections.emptyList(),
                        indirectDiffBuilder.getIndirectModifications()
                );
        objectStore.postprocessAfterCommit(result);
    }


    static class EffectiveId {
        private final String entityName;
        private final Map<String, Object> snapshot;

        EffectiveId(ObjectId id) {
            this.entityName = id.getEntityName();
            this.snapshot = id.getIdSnapshot();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            if(snapshot.isEmpty()) {
                return false;
            }

            EffectiveId that = (EffectiveId) o;

            if (!entityName.equals(that.entityName)) return false;
            return snapshot.equals(that.snapshot);

        }

        @Override
        public int hashCode() {
            int result = entityName.hashCode();
            result = 31 * result + snapshot.hashCode();
            return result;
        }
    }
}
