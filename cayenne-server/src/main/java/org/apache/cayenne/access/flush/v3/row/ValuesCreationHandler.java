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
            processFlattenedPath(id, null, dbEntity, attribute.getDbAttributePath(), newValue != null);
        }

        DbAttribute dbAttribute = attribute.getDbAttribute();
        if(dbAttribute.isPrimaryKey()) {
            if(!(newValue instanceof Number) || ((Number) newValue).longValue() != 0) {
                factory.get(dbAttribute.getEntity()).getChangeId().getReplacementIdMap().put(dbAttribute.getName(), newValue);
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

        if(objRelationship.isFlattened()) {
            processFlattenedPath(id, targetId, entity.getDbEntity(), objRelationship.getDbRelationshipPath(), created);
        } else {
            DbRelationship dbRelationship = objRelationship.getDbRelationships().get(0);
            processRelationship(dbRelationship, id, targetId, created);
        }
    }

    private void processFlattenedPath(ObjectId id, ObjectId finalTargetId, DbEntity entity, String dbPath, boolean add) {
        Iterator<CayenneMapEntry> dbPathIterator = entity.resolvePathComponents(dbPath);
        StringBuilder path = new StringBuilder();

        ObjectId srcId = id;
        ObjectId targetId;

        while(dbPathIterator.hasNext()) {
            CayenneMapEntry entry = dbPathIterator.next();
            if(path.length() > 0) {
                path.append('.');
            }

            path.append(entry.getName());
            if(entry instanceof DbRelationship) {
                DbRelationship relationship = (DbRelationship)entry;
                // intermediate db entity to be inserted
                DbEntity target = relationship.getTargetEntity();
                // if ID is present, just use it, otherwise create new
                String flattenedPath = path.toString();

                // if this is last segment and it's a relationship, use known target id from arc creation
                if(!dbPathIterator.hasNext()) {
                    targetId = finalTargetId;
                } else {
                    targetId = factory.getStore().getFlattenedId(id, flattenedPath);
                }

                if(targetId == null) {
                    // should insert, regardless of original operation (insert/update)
                    // TODO: prefix..
                    targetId = ObjectId.of(PermanentObjectIdVisitor.DB_ID_PREFIX + target.getName());
                    factory.getStore().markFlattenedPath(id, flattenedPath, targetId);
                    factory.<DbRowWithValues>getOrCreate(target, targetId, DbRowType.INSERT)
                            .getValues()
                            .addFlattenedId(flattenedPath, targetId);
                } else  {
                    // should update existing DB row
                    factory.getOrCreate(target, targetId, DbRowType.UPDATE);
                }
                processRelationship(relationship, srcId, targetId, add);
                srcId = targetId; // use target as next source..
            }
        }
    }

    private void processRelationship(DbRelationship dbRelationship, ObjectId srcId, ObjectId targetId, boolean add) {
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
                // case 2 and 3
                if(dbRelationship.isToDependentPK()) {
                    if(targetPK) {
                        targetId.getReplacementIdMap().put(join.getTargetName(), srcValue);
                    }
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getTargetEntity(), targetId, defaultType)
                            .getValues()
                            .addValue(join.getTarget(), srcValue);
                } else {
                    if(srcPK) {
                        srcId.getReplacementIdMap().put(join.getSourceName(), dstValue);
                    }
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getSourceEntity(), targetId, defaultType)
                            .getValues()
                            .addValue(join.getSource(), dstValue);
                }
            } else {
                // case 1
                if(srcPK) {
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getTargetEntity(), targetId, defaultType)
                            .getValues()
                            .addValue(join.getTarget(), srcValue);
                } else {
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType)
                            .getValues()
                            .addValue(join.getSource(), dstValue);
                }
            }
        }
    }

    // not interested in following events in this handler

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
