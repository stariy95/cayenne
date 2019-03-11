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

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.2
 */
class InsertSnapshotCreationHandler extends SnapshotCreationHandler {

    InsertSnapshotCreationHandler(ObjectStore store, ClassDescriptor descriptor) {
        super(store, descriptor);
    }

    @Override
    protected InsertDiffSnapshot createSnapshot(DbEntity entity) {
        return new InsertDiffSnapshot(entity);
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        ObjectId id = (ObjectId)nodeId;
        ObjEntity entity = descriptor.getEntity();
        ObjAttribute attribute = entity.getAttribute(property);
        DbEntity dbEntity = entity.getDbEntity();
        dbIds.put(dbEntity.getName(), id);

        if(attribute.isFlattened()) {
            processFlattenedAttribute(id, dbEntity, attribute.getDbAttributePath());
        }

        DbAttribute dbAttribute = attribute.getDbAttribute();
        this.<InsertDiffSnapshot>getSnapshot(dbAttribute).addValue(dbAttribute, newValue);
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        ObjectId id = (ObjectId)nodeId;
        String arcName = arcId.toString();
        ObjEntity entity = descriptor.getEntity();
        ObjRelationship objRelationship = entity.getRelationship(arcName);
        if(objRelationship == null) {
            // todo: process other variants like "db:relname"
            return;
        }

        if(objRelationship.isFlattened()) {
            processFlattenedAttribute(id, entity.getDbEntity(), objRelationship.getDbRelationshipPath());
        }

        List<DbRelationship> dbRelationships = objRelationship.getDbRelationships();
        DbRelationship lastDbRelationship = dbRelationships.get(dbRelationships.size() - 1);

        for(DbJoin join : lastDbRelationship.getJoins()) {
            boolean srcPK = join.getSource().isPrimaryKey();
            boolean targetPk = join.getTarget().isPrimaryKey();
            if((srcPK == targetPk && lastDbRelationship.isToDependentPK()) || srcPK) {
                // source -> target
            } else {
                // target -> source

            }
        }
    }


    private void processFlattenedAttribute(ObjectId id, DbEntity entity, String dbPath) {
        Iterator<CayenneMapEntry> dbPathIterator = entity.resolvePathComponents(dbPath);
        StringBuilder path = new StringBuilder();
        while(dbPathIterator.hasNext()) {
            CayenneMapEntry entry = dbPathIterator.next();
            if(path.length() > 0) {
                path.append('.');
            }

            path.append(entry.getName());
            if(entry instanceof DbRelationship && dbPathIterator.hasNext()) {
                DbRelationship relationship = (DbRelationship)entry;
                // intermediate db entity to be inserted
                DbEntity target = relationship.getTargetEntity();
                // if ID is present, just use it, otherwise create new
                ObjectId flattenedId = store.getFlattenedId(id, path.toString());
                if(flattenedId == null) {
                    flattenedId = ObjectId.of(target.getName());
                }
                dbIds.put(target.getName(), flattenedId);
            }
        }
    }
}
