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
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
abstract class SnapshotCreationHandler implements GraphChangeHandler {

    protected final ObjectStore store;
    protected final ClassDescriptor descriptor;
    protected final Persistent object;

    protected final Map<DbEntity, DiffSnapshot> snapshots;
    protected final Map<String, ObjectId> dbIds;

    SnapshotCreationHandler(ObjectStore store, ClassDescriptor descriptor, Persistent object) {
        this.store = store;
        this.descriptor = descriptor;
        this.object = object;
        this.snapshots = new HashMap<>();
        this.dbIds = new HashMap<>();
    }

    Collection<DiffSnapshot> processDiff(ObjectDiff diff) {
        diff.apply(this);
        // link snapshot with id after all processing is done
        snapshots.forEach((ent, snapshot) -> snapshot.changeId = dbIds.get(ent.getName()));
        return snapshots.values();
    }

    @SuppressWarnings("unchecked")
    protected <E extends DiffSnapshot> E getSnapshot(DbEntity entity) {
        return (E)snapshots.computeIfAbsent(entity, this::createSnapshot);
    }

    protected <E extends DiffSnapshot> E getSnapshot(DbAttribute attribute) {
        return getSnapshot(attribute.getEntity());
    }

    protected abstract DiffSnapshot createSnapshot(DbEntity entity);

    /* * * GraphChangeHandler methods' stubs * * */

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
    }

    @Override
    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    @Override
    public void nodeCreated(Object nodeId) {
    }

    @Override
    public void nodeRemoved(Object nodeId) {
    }
}
