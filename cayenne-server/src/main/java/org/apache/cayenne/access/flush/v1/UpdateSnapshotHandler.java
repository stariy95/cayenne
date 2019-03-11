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

package org.apache.cayenne.access.flush.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
public class UpdateSnapshotHandler implements GraphChangeHandler {

    private final ClassDescriptor descriptor;
    private final ObjEntity entity;
    private final DbEntity root;

    // All DB changes produced by this diff
    private Map<DbEntity, Map<DbAttribute, Object>> snapshots;

    UpdateSnapshotHandler(ClassDescriptor descriptor) {
        this.descriptor = descriptor;
        this.entity = descriptor.getEntity();
        this.root = entity.getDbEntity();
    }

    Map<String, Object> getSnapshot() {
        Map<DbAttribute, Object> snapshot = getSnapshotForEntity(root);
        if(snapshot.isEmpty()){
            return Collections.emptyMap();
        }

        Map<String, Object> snapshotByName = new HashMap<>();
        snapshot.forEach((attr, val) -> {
            snapshotByName.put(attr.getName(), val);
        });

        if(snapshots.size() > 1) {
            System.out.println("UpdateSnapshotHandler!!! multiple entities > " + snapshots.size());
        }
        return snapshotByName;
    }

    Map<DbAttribute, Object> getSnapshotForEntity(DbEntity entity) {
        if(snapshots == null) {
            return Collections.emptyMap();
        }

        Map<DbAttribute, Object> snapshot = snapshots.get(root);
        if(snapshot == null || snapshot.isEmpty()){
            return Collections.emptyMap();
        }

        return snapshot;
    }

    List<DbAttribute> getModifiedAttributes() {
        Map<DbAttribute, Object> snapshot = getSnapshotForEntity(root);
        if(snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(snapshot.keySet());
    }

    boolean hasChanges() {
        return snapshots != null && !snapshots.isEmpty();
    }

    Collection<DbEntity> getModifiedEntities() {
        return snapshots == null ? Collections.emptyList() : snapshots.keySet();
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        if(entity.isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", entity.getName());
        }

        DbAttribute dbAttribute = entity.getAttribute(property).getDbAttribute();
        addToSnapshot(dbAttribute, newValue);

        // process meaningful PK
        if(dbAttribute.isPrimaryKey()) {
            ObjectId id = (ObjectId)nodeId;
            id.getReplacementIdMap().put(dbAttribute.getName(), newValue);
        }
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        // todo: readonly entity should be rejected here if we need to update it
        String relationshipPath = arcId.toString(); // can be db:path, obj.path, etc.
        ObjectId srcId = (ObjectId)nodeId;
        ObjectId targetId = (ObjectId)targetNodeId;

        ObjRelationship relationship = entity.getRelationship(relationshipPath);
        if(relationship == null) {
            // TODO: do something else ...
            return;
        }

        DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
        for(DbJoin join : dbRelationship.getJoins()) {
            // skip PK if it's not depend on master PK
            Object value = targetId.getIdSnapshot().get(join.getTargetName());
            if(join.getSource().isPrimaryKey()) {
                if(!dbRelationship.isToMasterPK()) {
                    continue;
                }
                srcId.getReplacementIdMap().put(join.getSourceName(), value);
            }
            // todo: fix for CAY-2488 should be here...
            addToSnapshot(join.getSource(), (Supplier) () -> targetId.getIdSnapshot().get(join.getTargetName()));
        }
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        String relationshipPath = arcId.toString(); // can be db:path, obj.path, etc.
        ObjRelationship relationship = entity.getRelationship(relationshipPath);
        if(relationship == null) {
            // TODO: do something else ...
            return;
        }

        DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
        for(DbJoin join : dbRelationship.getJoins()) {
            // skip PK
            if(join.getSource().isPrimaryKey() && !dbRelationship.isToMasterPK()) {
                continue;
            }
            addToSnapshot(join.getSource(), null);
        }
    }

    protected void addToSnapshot(DbAttribute key, Object value) {
        if(value == null) {
            return;
        }

        if(snapshots == null) {
            snapshots = new HashMap<>();
        }
        snapshots.computeIfAbsent(key.getEntity(), (e) -> new HashMap<>())
                .put(key, value);
    }

    // We don't interested in other changes here

    @Override
    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    @Override
    public void nodeRemoved(Object nodeId) {
    }

    @Override
    public void nodeCreated(Object nodeId) {
    }
}
