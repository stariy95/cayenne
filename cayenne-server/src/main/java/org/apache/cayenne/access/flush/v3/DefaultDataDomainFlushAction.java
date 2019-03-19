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

package org.apache.cayenne.access.flush.v3;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.apache.cayenne.access.flush.SnapshotSorter;
import org.apache.cayenne.access.flush.v3.row.DbRow;
import org.apache.cayenne.access.flush.v3.row.DbRowFactory;
import org.apache.cayenne.access.flush.v3.row.DbRowVisitor;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final SnapshotSorter snapshotSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, SnapshotSorter snapshotSorter, JdbcEventLogger jdbcEventLogger) {
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
        objectStore.postprocessAfterCommit(result);

        return result;
    }

    private List<DbRow> sort(Collection<DbRow> dbRows) {
        return snapshotSorter.sortDbRows(dbRows);
    }

    protected Collection<DbRow> createDbRows(DataContext context, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();
        ObjectStore objectStore = context.getObjectStore();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        Map<DbRow, DbRow> dbRows = new HashMap<>();

        changesByObjectId.forEach((obj, diff) -> {
            ObjectId id = (ObjectId)obj;
            Persistent object = (Persistent) objectStore.getNode(id);
            ClassDescriptor descriptor = resolver.getClassDescriptor(id.getEntityName());

            DbRowFactory factory = new DbRowFactory(resolver, objectStore, descriptor, object);
            Collection<? extends DbRow> rows = factory.createRows(diff);
            rows.forEach(dbRow -> dbRows.compute(dbRow, (key, value) -> {
                if(value != null) {
                    return value.accept(new DbRowMerger(dbRow));
                }
                return dbRow;
            }));
        });

        return dbRows.values();
    }

    protected void updateObjectIds(Collection<DbRow> snapshots) {
        DbRowVisitor<Void> permIdVisitor = new PermanentObjectIdVisitor(dataDomain);
        snapshots.forEach(snapshot -> snapshot.accept(permIdVisitor));
    }

    protected List<BatchQuery> createQueries(List<DbRow> dbRows) {
        DbRowVisitor<BatchQuery> queryCreator = new QueryCreatorVisitor();
        return dbRows.stream()
                .map(dbRow -> dbRow.accept(queryCreator))
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

    protected void createReplacementIds(ObjectStore store, CompoundDiff result, List<DbRow> snapshots) {
        ReplacementIdVisitor visitor = new ReplacementIdVisitor(store, dataDomain.getEntityResolver(), result);
        snapshots.forEach(snapshot -> snapshot.accept(visitor));
    }
}
