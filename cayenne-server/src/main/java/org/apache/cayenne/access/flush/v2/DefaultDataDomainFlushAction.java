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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.OperationSorter;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final OperationSorter operationSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, OperationSorter operationSorter, JdbcEventLogger jdbcEventLogger) {
        this.dataDomain = dataDomain;
        this.operationSorter = operationSorter;
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {
        CompoundDiff result = new CompoundDiff();
        if (changes == null) {
            return result;
        }

        Map<ObjectId, DiffSnapshot> operations = createSnapshots(context, (ObjectStoreGraphDiff) changes);
        return result;
    }

    private Map<ObjectId, DiffSnapshot> createSnapshots(DataContext context, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();
        ObjectStore objectStore = context.getObjectStore();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        Map<ObjectId, DiffSnapshot> snapshots = new HashMap<>(changesByObjectId.size());

        changesByObjectId.forEach((obj, diff) -> {
            ObjectId id = (ObjectId)obj;
            Persistent object = (Persistent) objectStore.getNode(id);
            ClassDescriptor descriptor = resolver.getClassDescriptor(id.getEntityName());
            getHandler(objectStore, object, descriptor)
                    .processDiff(diff)
                    .forEach(snapshot -> snapshots.put(snapshot.changeId, snapshot));
        });

        return snapshots;
    }

    private SnapshotCreationHandler getHandler(ObjectStore objectStore, Persistent object, ClassDescriptor descriptor) {
        switch (object.getPersistenceState()) {
            case PersistenceState.NEW:
                return new InsertSnapshotCreationHandler(objectStore, descriptor);
            case PersistenceState.MODIFIED:
                return new UpdateSnapshotCreationHandler(objectStore, descriptor);
            case PersistenceState.DELETED:
                return new DeleteSnapshotCreationHandler(objectStore, descriptor);
            default:
                throw new CayenneRuntimeException("Trying to flush object (%s) in wrong persistence state (%s)",
                        object, PersistenceState.persistenceStateName(object.getPersistenceState()));
        }
    }

}
