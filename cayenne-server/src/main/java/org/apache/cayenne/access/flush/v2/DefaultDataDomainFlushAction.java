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

package org.apache.cayenne.access.flush.v2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.SnapshotSorter;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
@SuppressWarnings("WeakerAccess")
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
        Collection<DiffSnapshot> snapshots = createSnapshots(context, (ObjectStoreGraphDiff) changes);
        updateObjectIds(snapshots);
        List<DiffSnapshot> sortedSnapshots = sort(snapshots);
        List<BatchQuery> queries = createQueries(sortedSnapshots);
        executeQueries(queries);
        createReplacementIds(objectStore, result, sortedSnapshots);
        objectStore.postprocessAfterCommit(result);

        return result;
    }

    protected Collection<DiffSnapshot> createSnapshots(DataContext context, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();
        ObjectStore objectStore = context.getObjectStore();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        Map<DiffSnapshot, DiffSnapshot> snapshots = new HashMap<>();

        changesByObjectId.forEach((obj, diff) -> {
            ObjectId id = (ObjectId)obj;
            Persistent object = (Persistent) objectStore.getNode(id);
            ClassDescriptor descriptor = resolver.getClassDescriptor(id.getEntityName());

            createHandler(objectStore, object, descriptor)
                    .processDiff(diff)
                    .forEach(snapshot -> snapshots.compute(snapshot, (key, value) -> {
                        if(value != null) {
                            return value.accept(new DiffSnapshotMerger(snapshot));
                        }
                        return snapshot;
                    }));
        });

        return snapshots.values();
    }

    protected void updateObjectIds(Collection<DiffSnapshot> snapshots) {
        DiffSnapshotVisitor<Void> permIdVisitor = new PermanentObjectIdVisitor(dataDomain);
        snapshots.forEach(snapshot -> snapshot.accept(permIdVisitor));
    }

    protected List<DiffSnapshot> sort(Collection<DiffSnapshot> snapshots) {
        return snapshotSorter.sortSnapshots(snapshots);
    }

    protected List<BatchQuery> createQueries(List<DiffSnapshot> snapshots) {
        DiffSnapshotVisitor<BatchQuery> queryCreator = new QueryCreatorVisitor();
        return snapshots.stream()
                .map(snapshot -> snapshot.accept(queryCreator))
                .collect(Collectors.toList());
    }

    protected void executeQueries(List<BatchQuery> queries) {
        OperationObserver observer = new FlushOperationObserver(jdbcEventLogger);
        // TODO: batch queries by node change, when queries are sorted should speedup a bit
        queries.forEach(query -> dataDomain
                .lookupDataNode(query.getDbEntity().getDataMap())
                .performQueries(Collections.singleton(query), observer));
    }

    protected void createReplacementIds(ObjectStore store, CompoundDiff result, List<DiffSnapshot> snapshots) {
        ReplacementIdVisitor visitor = new ReplacementIdVisitor(store, result);
        snapshots.forEach(snapshot -> snapshot.accept(visitor));
    }

    protected SnapshotCreationHandler createHandler(ObjectStore objectStore, Persistent object, ClassDescriptor descriptor) {
        switch (object.getPersistenceState()) {
            case PersistenceState.NEW:
                return new InsertSnapshotCreationHandler(objectStore, descriptor, object);
            case PersistenceState.MODIFIED:
                return new UpdateSnapshotCreationHandler(objectStore, descriptor, object);
            case PersistenceState.DELETED:
                return new DeleteSnapshotCreationHandler(objectStore, descriptor, object);
            default:
                throw new CayenneRuntimeException("Trying to flush object (%s) in wrong persistence state (%s)",
                        object, PersistenceState.persistenceStateName(object.getPersistenceState()));
        }
    }

}
