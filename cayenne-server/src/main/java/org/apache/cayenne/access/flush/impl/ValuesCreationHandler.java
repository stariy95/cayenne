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

package org.apache.cayenne.access.flush.impl;

import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.flush.row.DbRowWithValues;
import org.apache.cayenne.graph.ArcId;
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
        if(entity.isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", entity.getName());
        }
        ObjAttribute attribute = entity.getAttribute(property);
        DbEntity dbEntity = entity.getDbEntity();

        if(attribute.isFlattened()) {
            // get target row ID
            id = processFlattenedPath(id, null, dbEntity, attribute.getDbAttributePath(), newValue != null);
        }

        DbAttribute dbAttribute = attribute.getDbAttribute();
        // TODO: id == null?
        if(id != null && dbAttribute.isPrimaryKey()) {
            if(!(newValue instanceof Number) || ((Number) newValue).longValue() != 0) {
                id.getReplacementIdMap().put(dbAttribute.getName(), newValue);
            }
        }

        // TODO: any actual check that we can cast DbRow to DbRowWithValues?
        DbRowWithValues dbRow = factory.get(id);
        dbRow.getValues().addValue(dbAttribute, newValue);
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        ArcTarget arcTarget = new ArcTarget((ObjectId) nodeId, (ObjectId) targetNodeId, arcId);
        processArcChange(arcTarget, true);
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        ArcTarget arcTarget = new ArcTarget((ObjectId) nodeId, (ObjectId) targetNodeId, arcId);
        processArcChange(arcTarget, false);
    }

    private void processArcChange(ArcTarget arcTarget, boolean created) {
        if(factory.getProcessedArcs().contains(arcTarget.getReversed())) {
            return;
        }

        ObjEntity entity = factory.getDescriptor().getEntity();
        ObjRelationship objRelationship = entity.getRelationship(arcTarget.getArcId().getForwardArc());
        if(objRelationship == null) {
            // todo: process other variants like "db:relname"
            return;
        }

        if(objRelationship.isFlattened()) {
            processFlattenedPath(arcTarget.getSourceId(), arcTarget.getTargetId(), entity.getDbEntity(), objRelationship.getDbRelationshipPath(), created);
        } else {
            DbRelationship dbRelationship = objRelationship.getDbRelationships().get(0);
            processRelationship(dbRelationship, arcTarget.getSourceId(), arcTarget.getTargetId(), created);
        }

        factory.processedArcs.add(arcTarget);
    }

    private ObjectId processFlattenedPath(ObjectId id, ObjectId finalTargetId, DbEntity entity, String dbPath, boolean add) {
        Iterator<CayenneMapEntry> dbPathIterator = entity.resolvePathComponents(dbPath);
        StringBuilder path = new StringBuilder();

        ObjectId srcId = id;
        ObjectId targetId = null;

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
                    if(!relationship.isToMany()) {
                        targetId = factory.getStore().getFlattenedId(id, flattenedPath);
                    } else {
                        targetId = null;
                    }
                }

                if(targetId == null) {
                    // should insert, regardless of original operation (insert/update)
                    targetId = ObjectId.of(PermanentObjectIdVisitor.DB_ID_PREFIX + target.getName());
                    if(!relationship.isToMany()) {
                        factory.getStore().markFlattenedPath(id, flattenedPath, targetId);
                    }

                    factory.<DbRowWithValues>getOrCreate(target, targetId, add ? DbRowType.INSERT : DbRowType.UPDATE)
                            .getValues()
                            .addFlattenedId(flattenedPath, targetId);
                } else if(dbPathIterator.hasNext()) {
                    // should update existing DB row
                    factory.getOrCreate(target, targetId, DbRowType.UPDATE);
                }
                processRelationship(relationship, srcId, targetId, add);
                srcId = targetId; // use target as next source..
            }
        }

        return targetId;
    }

    private void processRelationship(DbRelationship dbRelationship, ObjectId srcId, ObjectId targetId, boolean add) {
        for(DbJoin join : dbRelationship.getJoins()) {
            boolean srcPK = join.getSource().isPrimaryKey();
            boolean targetPK = join.getTarget().isPrimaryKey();
            Object srcValue = ObjectIdValueSupplier.getFor(srcId, join.getSourceName());
            Object dstValue = ObjectIdValueSupplier.getFor(targetId, join.getTargetName());

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
                            .addValue(join.getTarget(), add ? srcValue : null);
                } else {
                    if(srcPK) {
                        srcId.getReplacementIdMap().put(join.getSourceName(), dstValue);
                    }
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType)
                            .getValues()
                            .addValue(join.getSource(), add ? dstValue : null);
                }
            } else {
                // case 1
                if(srcPK) {
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getTargetEntity(), targetId, defaultType)
                            .getValues()
                            .addValue(join.getTarget(), add ? srcValue : null);
                } else {
                    factory.<DbRowWithValues>getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType)
                            .getValues()
                            .addValue(join.getSource(), add ? dstValue : null);
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
