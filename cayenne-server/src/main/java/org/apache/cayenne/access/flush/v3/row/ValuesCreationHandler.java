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

package org.apache.cayenne.access.flush.v3.row;

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.flush.v3.PermanentObjectIdVisitor;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.2
 */
public class ValuesCreationHandler implements GraphChangeHandler {

    private final DbRowFactory factory;
    private final DbRowType defaultType;

    ValuesCreationHandler(DbRowFactory factory, DbRowType defaultType) {
        this.factory = factory;
        this.defaultType = defaultType;
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        ObjectId id = (ObjectId)nodeId;
        ObjEntity entity = factory.getDescriptor().getEntity();
        ObjAttribute attribute = entity.getAttribute(property);
        DbEntity dbEntity = entity.getDbEntity();

        if(attribute.isFlattened()) {
            processFlattenedPath(id, dbEntity, attribute.getDbAttributePath(), newValue != null);
        }

        DbAttribute dbAttribute = attribute.getDbAttribute();
        if(dbAttribute.isPrimaryKey()) {
            if(!(newValue instanceof Number) || ((Number) newValue).longValue() != 0) {
                factory.getDbId(dbAttribute.getEntity()).getReplacementIdMap().put(dbAttribute.getName(), newValue);
            }
        }

        // TODO: any actual check that we can cast DbRow to DbRowWithValues?
        DbRowWithValues dbRow = factory.get(dbAttribute.getEntity());
        dbRow.getValues().addValue(dbAttribute, newValue);
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        processArcChange((ObjectId)nodeId, (ObjectId)targetNodeId, arcId.toString(), true);
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        processArcChange((ObjectId)nodeId, (ObjectId)targetNodeId, arcId.toString(), false);
    }

    private void processArcChange(ObjectId id, ObjectId targetId, String arcName, boolean created) {
        ObjEntity entity = factory.getDescriptor().getEntity();
        ObjRelationship objRelationship = entity.getRelationship(arcName);
        if(objRelationship == null) {
            // todo: process other variants like "db:relname"
            return;
        }

        // grab Object ids of connected objects
        factory.addDbId(objRelationship.getSourceEntity().getDbEntity(), id);
        factory.addDbId(objRelationship.getTargetEntity().getDbEntity(), targetId);
        factory.getOrCreate(objRelationship.getTargetEntity().getDbEntity(),
                targetId.isTemporary() ? DbRowType.INSERT : DbRowType.UPDATE);

        if(objRelationship.isFlattened()) {
            processFlattenedPath(id, entity.getDbEntity(), objRelationship.getDbRelationshipPath(), created);
        }

        List<DbRelationship> dbRelationships = objRelationship.getDbRelationships();
        DbRelationship lastDbRelationship = dbRelationships.get(dbRelationships.size() - 1);

        processRelationship(lastDbRelationship, created);
    }

    private void processFlattenedPath(ObjectId id, DbEntity entity, String dbPath, boolean add) {
        Iterator<CayenneMapEntry> dbPathIterator = entity.resolvePathComponents(dbPath);
        StringBuilder path = new StringBuilder();
        while(dbPathIterator.hasNext()) {
            CayenneMapEntry entry = dbPathIterator.next();
            if(path.length() > 0) {
                path.append('.');
            }

            path.append(entry.getName());
            // skip last segment of the path, it will be processed by the caller
            if(entry instanceof DbRelationship && dbPathIterator.hasNext()) {
                DbRelationship relationship = (DbRelationship)entry;
                // intermediate db entity to be inserted
                DbEntity target = relationship.getTargetEntity();
                // if ID is present, just use it, otherwise create new
                String flattenedPath = path.toString();
                ObjectId flattenedId = factory.getStore().getFlattenedId(id, flattenedPath);
                if(flattenedId == null) {
                    // should insert, regardless of original operation (insert/update)
                    // TODO: prefix..
                    flattenedId = ObjectId.of(PermanentObjectIdVisitor.DB_ID_PREFIX + target.getName());
                    factory.getStore().markFlattenedPath(id, flattenedPath, flattenedId);
                    factory.addDbId(target, flattenedId);
                    factory.<DbRowWithValues>getOrCreate(target, DbRowType.INSERT)
                            .getValues()
                            .addFlattenedId(flattenedPath, flattenedId);
                } else  {
                    // should update existing DB row
                    factory.addDbId(target, flattenedId);
                    factory.getOrCreate(target, DbRowType.UPDATE);
                }
                processRelationship(relationship, add);
            }
        }
    }

    private void processRelationship(DbRelationship dbRelationship, boolean add) {
        ObjectId srcId = factory.getDbId(dbRelationship.getSourceEntity());
        ObjectId targetId = factory.getDbId(dbRelationship.getTargetEntity());

        for(DbJoin join : dbRelationship.getJoins()) {
            boolean srcPK = join.getSource().isPrimaryKey();
            boolean targetPK = join.getTarget().isPrimaryKey();
            Object srcValue = add ? ObjectIdValueSupplier.getFor(srcId, join.getSourceName()) : null;
            Object dstValue = add ? ObjectIdValueSupplier.getFor(targetId, join.getTargetName()) : null;

            // Push values from/to source to/from target...
            // We have 3 cases globally here:
            // 1. PK -> FK: just grab value from PK and propagate it to FK
            // 2. PK -> PK: check isToDep flag and
            // 3. NON-PK -> FK (not supported fully for now): also check isToDep flag, but get value from DbRow, not ObjID
            if(srcPK == targetPK) {
                if(dbRelationship.isToDependentPK()) {
                    if(targetPK) {
                        targetId.getReplacementIdMap().put(join.getTargetName(), srcValue);
                    } else {
                        factory.<DbRowWithValues>getOrCreate(dbRelationship.getTargetEntity(), defaultType)
                                .getValues()
                                .addValue(join.getTarget(), srcValue);
                    }
                } else {
                    if(srcPK) {
                        srcId.getReplacementIdMap().put(join.getSourceName(), dstValue);
                    } else {
                        factory.<DbRowWithValues>getOrCreate(dbRelationship.getSourceEntity(), defaultType)
                                .getValues()
                                .addValue(join.getSource(), dstValue);
                    }
                }
            } else {
                if(srcPK) {
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getTargetEntity(), defaultType)
                            .getValues()
                            .addValue(join.getTarget(), srcValue);
                } else {
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getSourceEntity(), defaultType)
                            .getValues()
                            .addValue(join.getSource(), dstValue);
                }
            }
        }
    }

    // not interested in following events

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
