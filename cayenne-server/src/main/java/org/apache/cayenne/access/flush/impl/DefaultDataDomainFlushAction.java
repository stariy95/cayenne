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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.row.DbRowSorter;
import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRowFactory.class);

    protected final DataDomain dataDomain;
    protected final DbRowSorter snapshotSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, DbRowSorter snapshotSorter, JdbcEventLogger jdbcEventLogger) {
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

        ObjectStore objectStore = context.getObjectStore();
        Collection<DbRow> dbRows = createDbRows(context, (ObjectStoreGraphDiff) changes);
        updateObjectIds(dbRows);
        List<DbRow> sortedDbRows = sort(dbRows);
        List<BatchQuery> queries = createQueries(sortedDbRows);
        executeQueries(queries);
        createReplacementIds(objectStore, result, sortedDbRows);

        postprocess(context, changes, result, sortedDbRows);

        return result;
    }

    private List<DbRow> sort(Collection<DbRow> dbRows) {
        return snapshotSorter.sort(dbRows);
    }

    protected Collection<DbRow> createDbRows(DataContext context, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();
        ObjectStore objectStore = context.getObjectStore();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        Map<DbRow, DbRow> dbRows = new HashMap<>();
        Set<ArcTarget> processedArcs = new HashSet<>();

        changesByObjectId.forEach((obj, diff) -> {
            ObjectId id = (ObjectId)obj;
            Persistent object = (Persistent) objectStore.getNode(id);
            ClassDescriptor descriptor = resolver.getClassDescriptor(id.getEntityName());

            DbRowFactory factory = new DbRowFactory(resolver, objectStore, descriptor, object, processedArcs);
            LOGGER.info("Get rows for object " + obj);
            Collection<? extends DbRow> rows = factory.createRows(diff);
            rows.forEach(dbRow -> {
                LOGGER.info("Process " + dbRow);
                dbRows.compute(dbRow, (key, value) -> {
                    if (value != null) {
                        return value.accept(new DbRowMerger(dbRow));
                    }
                    return dbRow;
                });
            });
        });

        return dbRows.values();
    }

    protected void updateObjectIds(Collection<DbRow> dbRows) {
        DbRowVisitor<Void> permIdVisitor = new PermanentObjectIdVisitor(dataDomain);
        dbRows.forEach(snapshot -> snapshot.accept(permIdVisitor));
    }

    protected List<BatchQuery> createQueries(List<DbRow> dbRows) {
        DbRowVisitor<BatchQuery> queryCreator = new QueryCreatorVisitor();
        return dbRows.stream()
                .map(row -> row.accept(queryCreator))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void executeQueries(List<BatchQuery> queries) {
        OperationObserver observer = new FlushOperationObserver(jdbcEventLogger);
        // TODO: batch queries by node change, when queries are sorted should speedup a bit
        queries.forEach(query -> dataDomain
                .lookupDataNode(query.getDbEntity().getDataMap())
                .performQueries(Collections.singleton(query), observer));
    }

    protected void createReplacementIds(ObjectStore store, CompoundDiff result, List<DbRow> dbRows) {
        ReplacementIdVisitor visitor = new ReplacementIdVisitor(store, dataDomain.getEntityResolver(), result);
        dbRows.forEach(row -> row.accept(visitor));
    }

    protected void postprocess(DataContext context, GraphDiff changes, CompoundDiff result, List<DbRow> dbRows) {
        ObjectStore objectStore = context.getObjectStore();

        PostprocessVisitor postprocessor = new PostprocessVisitor(context);
        dbRows.forEach(row -> {
            row.accept(postprocessor);
        });

        DataDomainIndirectDiffBuilder dataDomainIndirectDiffBuilder = new DataDomainIndirectDiffBuilder(context.getEntityResolver());
        dataDomainIndirectDiffBuilder.processIndirectChanges(changes);

        objectStore.getDataRowCache()
                .processSnapshotChanges(
                        objectStore,
                        postprocessor.getUpdatedSnapshots(),
                        postprocessor.getDeletedIds(), // TODO: old tmp id should go int this collection too...
                        Collections.emptyList(),
                        dataDomainIndirectDiffBuilder.getIndirectModifications()
                );
        objectStore.postprocessAfterCommit(result);
    }

}
