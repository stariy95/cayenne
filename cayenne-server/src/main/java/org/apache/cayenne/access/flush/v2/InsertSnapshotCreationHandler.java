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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.graph.ArcId;
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

    InsertSnapshotCreationHandler(ObjectStore store, ClassDescriptor descriptor, Persistent object) {
        super(store, descriptor, object);
        DbEntity dbEntity = descriptor.getEntity().getDbEntity();

        dbIds.put(dbEntity.getName(), object.getObjectId());
        // force create snapshot to insert id if no fields are provided
        getSnapshot(dbEntity);
    }

    @Override
    protected InsertDiffSnapshot createSnapshot(DbEntity entity) {
        return new InsertDiffSnapshot(object, entity);
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        ObjectId id = (ObjectId)nodeId;
        ObjEntity entity = descriptor.getEntity();
        ObjAttribute attribute = entity.getAttribute(property);
        DbEntity dbEntity = entity.getDbEntity();

        if(attribute.isFlattened()) {
            processFlattenedPath(id, dbEntity, attribute.getDbAttributePath());
        }

        DbAttribute dbAttribute = attribute.getDbAttribute();
        this.<InsertDiffSnapshot>getSnapshot(dbAttribute).addValue(dbAttribute, newValue);
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        ObjectId id = (ObjectId)nodeId;
        String arcName = arcId.toString();
        ObjEntity entity = descriptor.getEntity();
        ObjRelationship objRelationship = entity.getRelationship(arcName);
        if(objRelationship == null) {
            // todo: process other variants like "db:relname"
            return;
        }

        // grab Object ids of connected objects
        dbIds.put(objRelationship.getSourceEntity().getDbEntity().getName(), id);
        dbIds.put(objRelationship.getTargetEntity().getDbEntity().getName(), (ObjectId)targetNodeId);

        if(objRelationship.isFlattened()) {
            processFlattenedPath(id, entity.getDbEntity(), objRelationship.getDbRelationshipPath());
        }

        List<DbRelationship> dbRelationships = objRelationship.getDbRelationships();
        DbRelationship lastDbRelationship = dbRelationships.get(dbRelationships.size() - 1);

        processRelationship(lastDbRelationship);
    }

    private void processRelationship(DbRelationship dbRelationship) {
        ObjectId srcId = dbIds.computeIfAbsent(dbRelationship.getSourceEntityName(), entityName
                -> ObjectId.of(PermanentObjectIdVisitor.DB_ID_PREFIX + entityName));
        ObjectId targetId = dbIds.computeIfAbsent(dbRelationship.getTargetEntityName(), entityName
                -> ObjectId.of(PermanentObjectIdVisitor.DB_ID_PREFIX + entityName));

        for(DbJoin join : dbRelationship.getJoins()) {
            boolean srcPK = join.getSource().isPrimaryKey();
            boolean targetPK = join.getTarget().isPrimaryKey();
            if((srcPK == targetPK && dbRelationship.isToDependentPK()) || srcPK) {
                // source -> target
                Object value = ObjectValueSupplier.getFor(srcId, join.getSource());
                if(targetPK) {
                    targetId.getReplacementIdMap().put(join.getTargetName(), value);
                } else {
                    AddToSnapshotVisitor visitor = new AddToSnapshotVisitor(join.getTarget(), value);
                    this.getSnapshot(dbRelationship.getTargetEntity()).accept(visitor);
                }
            } else {
                // target -> source
                // TODO: get target descriptor ...
                Object value = ObjectValueSupplier.getFor(targetId, join.getTarget());
                AddToSnapshotVisitor visitor = new AddToSnapshotVisitor(join.getSource(), value);
                this.getSnapshot(dbRelationship.getSourceEntity()).accept(visitor);
            }
        }
    }

    private void processFlattenedPath(ObjectId id, DbEntity entity, String dbPath) {
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
                String flattenedPath = path.toString();
                ObjectId flattenedId = store.getFlattenedId(id, flattenedPath);
                if(flattenedId == null) {
                    flattenedId = ObjectId.of(PermanentObjectIdVisitor.DB_ID_PREFIX + target.getName());
                    store.markFlattenedPath(id, flattenedPath, flattenedId);
                    this.<InsertDiffSnapshot>getSnapshot(descriptor.getEntity().getDbEntity())
                            .addFlattenedId(flattenedPath, flattenedId);
                }
                dbIds.put(target.getName(), flattenedId);
                processRelationship(relationship);
            }
        }
    }

    private static class AddToSnapshotVisitor implements DiffSnapshotVisitor<Void> {
        private final DbAttribute attribute;
        private final Object value;

        public AddToSnapshotVisitor(DbAttribute attribute, Object value) {
            this.attribute = attribute;
            this.value = value;
        }

        @Override
        public Void visitInsert(InsertDiffSnapshot diffSnapshot) {
            diffSnapshot.addValue(attribute, value);
            return null;
        }

        @Override
        public Void visitUpdate(UpdateDiffSnapshot diffSnapshot) {
            diffSnapshot.addValue(attribute, value);
            return null;
        }
    }
}
