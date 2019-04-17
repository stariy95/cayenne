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

package org.apache.cayenne.access.flush;

import java.util.ArrayList;
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
import org.apache.cayenne.access.flush.operation.DbRowOpMerger;
import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * Default implementation of {@link DataDomainFlushAction}.
 *
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final DbRowOpSorter dbRowOpSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, DbRowOpSorter dbRowOpSorter, JdbcEventLogger jdbcEventLogger) {
        this.dataDomain = dataDomain;
        this.dbRowOpSorter = dbRowOpSorter;
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {
        CompoundDiff afterCommitDiff = new CompoundDiff();
        if (changes == null) {
            return afterCommitDiff;
        }
        if(!(changes instanceof ObjectStoreGraphDiff)) {
            throw new CayenneRuntimeException("Instance of ObjectStoreGraphDiff expected, got %s", changes.getClass());
        }

        ObjectStore objectStore = context.getObjectStore();
        ObjectStoreGraphDiff objectStoreGraphDiff = (ObjectStoreGraphDiff) changes;

        List<DbRowOp> dbRows = createDbRows(context, objectStoreGraphDiff);
        updateObjectIds(dbRows);
        List<DbRowOp> deduplicatedRows = mergeSameObjectIds(dbRows);
        List<DbRowOp> sortedDbRows = sort(deduplicatedRows);
        List<? extends Query> queries = createQueries(sortedDbRows);
        executeQueries(queries);
        createReplacementIds(objectStore, afterCommitDiff, sortedDbRows);
        postprocess(context, objectStoreGraphDiff, afterCommitDiff, sortedDbRows);

        return afterCommitDiff;
    }

    /**
     * Create ops based on incoming graph changes
     * @param context originating context
     * @param changes object graph diff
     * @return collection of {@link DbRowOp}
     */
    protected List<DbRowOp> createDbRows(DataContext context, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();
        ObjectStore objectStore = context.getObjectStore();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        List<DbRowOp> ops = new ArrayList<>(changesByObjectId.size());
        Set<ArcTarget> processedArcs = new HashSet<>();

        DbRowOpFactory factory = new DbRowOpFactory(resolver, objectStore, processedArcs);
        changesByObjectId.forEach((obj, diff) -> ops.addAll(factory.createRows(diff)));

        return ops;
    }

    /**
     * Fill in replacement IDs' data for given operations
     * @param dbRows collection of {@link DbRowOp}
     */
    protected void updateObjectIds(Collection<DbRowOp> dbRows) {
        DbRowOpVisitor<Void> permIdVisitor = new PermanentObjectIdVisitor(dataDomain);
        dbRows.forEach(row -> row.accept(permIdVisitor));
    }

    /**
     * @param dbRows collection of {@link DbRowOp}
     * @return collection of ops with merged duplicates
     */
    protected List<DbRowOp> mergeSameObjectIds(List<DbRowOp> dbRows) {
        Map<ObjectId, DbRowOp> index = new HashMap<>(dbRows.size());
        DbRowOpMerger merger = new DbRowOpMerger();
        // new EffectiveOpId()
        dbRows.forEach(row -> index.merge(row.getChangeId(), row, merger));
        // reuse list
        dbRows.clear();
        dbRows.addAll(index.values());
        return dbRows;
    }

    /**
     * Sort all operations
     * @param dbRows collection of {@link DbRowOp}
     * @return sorted collection of operations
     * @see DbRowOpSorter interface and it's default implementation
     */
    protected List<DbRowOp> sort(List<DbRowOp> dbRows) {
        return dbRowOpSorter.sort(dbRows);
    }

    /**
     *
     * @param dbRows collection of {@link DbRowOp}
     * @return collection of {@link Query} to perform
     */
    protected List<? extends Query> createQueries(List<DbRowOp> dbRows) {
        QueryCreatorVisitor queryCreator = new QueryCreatorVisitor(dbRows.size());
        dbRows.forEach(row -> row.accept(queryCreator));
        return queryCreator.getQueryList();
    }

    /**
     * Execute queries, grouping them by nodes
     * @param queries to execute
     */
    protected void executeQueries(List<? extends Query> queries) {
        EntityResolver entityResolver = dataDomain.getEntityResolver();
        OperationObserver observer = new FlushObserver(jdbcEventLogger);
        queries.stream()
                .collect(Collectors.groupingBy(query
                        -> dataDomain.lookupDataNode(query.getMetaData(entityResolver).getDataMap())))
                .forEach((node, nodeQueries)
                        -> node.performQueries(nodeQueries, observer));
    }

    /**
     * Set final {@link ObjectId} for persistent objects
     *
     * @param store object store
     * @param afterCommitDiff result graph diff
     * @param dbRows collection of {@link DbRowOp}
     */
    protected void createReplacementIds(ObjectStore store, CompoundDiff afterCommitDiff, List<DbRowOp> dbRows) {
        ReplacementIdVisitor visitor = new ReplacementIdVisitor(store, dataDomain.getEntityResolver(), afterCommitDiff);
        dbRows.forEach(row -> row.accept(visitor));
    }

    /**
     * Notify {@link ObjectStore} and it's data row cache about actual changes we performed.
     *
     * @param context originating context
     * @param changes incoming diff
     * @param afterCommitDiff resulting diff
     * @param dbRows collection of {@link DbRowOp}
     */
    protected void postprocess(DataContext context, ObjectStoreGraphDiff changes, CompoundDiff afterCommitDiff, List<DbRowOp> dbRows) {
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
        objectStore.postprocessAfterCommit(afterCommitDiff);
    }

}
