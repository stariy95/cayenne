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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * @since 4.2
 */
class InsertSnapshotHandler implements GraphChangeHandler {

    private final ObjEntity entity;
    private Map<String, Object> snapshot;

    InsertSnapshotHandler(ObjEntity entity) {
        this.entity = entity;
    }

    public Map<String, Object> getSnapshot() {
        return snapshot;
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        ObjAttribute attribute = entity.getAttribute(property);
        addToSnapshot(attribute.getDbAttribute().getName(), newValue);
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        String relationshipPath = arcId.toString(); // can be db:path, obj.path, etc.
        ObjRelationship relationship = entity.getRelationship(relationshipPath);
        if(relationship == null) {
            // TODO: do something else ...
            return;
        }

        DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
        ObjectId targetId = (ObjectId)targetNodeId;
        for(DbJoin join : dbRelationship.getJoins()) {
            // skip PK
            if(join.getSource().isPrimaryKey() && !dbRelationship.isToMasterPK()) {
                continue;
            }
            addToSnapshot(join.getSourceName(), (Supplier) () -> targetId.getIdSnapshot().get(join.getTargetName()));
        }
    }

    protected void addToSnapshot(String key, Object value) {
        if(value == null) {
            return;
        }

        if(snapshot == null) {
            snapshot = new HashMap<>();
        }

        snapshot.put(key, value);
    }

    // We don't interested in other changes in insert context...

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
    }

    @Override
    public void nodeRemoved(Object nodeId) {
    }

    @Override
    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    @Override
    public void nodeCreated(Object nodeId) {
    }
}
