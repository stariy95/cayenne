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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowWithQualifier;
import org.apache.cayenne.access.flush.row.DbRowWithValues;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.2
 */
class ArcValuesCreationHandler implements GraphChangeHandler {

    final DbRowFactory factory;
    final DbRowType defaultType;

    ArcValuesCreationHandler(DbRowFactory factory, DbRowType defaultType) {
        this.factory = factory;
        this.defaultType = defaultType;
    }

    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        processArcChange(nodeId, targetNodeId, arcId, true);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        processArcChange(nodeId, targetNodeId, arcId, false);
    }

    private void processArcChange(Object nodeId, Object targetNodeId, ArcId arcId, boolean created) {
        ObjectId actualTargetId = (ObjectId)targetNodeId;
        ObjectId snapshotId = factory.getDiff().getCurrentArcSnapshotValue(arcId.getForwardArc());
        if(snapshotId != null) {
            actualTargetId = snapshotId;
        }
        ArcTarget arcTarget = new ArcTarget((ObjectId) nodeId, actualTargetId, arcId);
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
            processFlattenedPath(arcTarget.getSourceId(), arcTarget.getTargetId(), entity.getDbEntity(),
                    objRelationship.getDbRelationshipPath(), created);
        } else {
            DbRelationship dbRelationship = objRelationship.getDbRelationships().get(0);
            processRelationship(dbRelationship, arcTarget.getSourceId(), arcTarget.getTargetId(), created);
        }

        factory.getProcessedArcs().add(arcTarget);
    }

    protected ObjectId processFlattenedPath(ObjectId id, ObjectId finalTargetId, DbEntity entity, String dbPath, boolean add) {
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

                    DbRowType type;
                    if(relationship.isToMany()) {
                        type = add ? DbRowType.INSERT : DbRowType.DELETE;
                        factory.getOrCreate(target, targetId, type);
                    } else {
                        type = add ? DbRowType.INSERT : DbRowType.UPDATE;
                        factory.<DbRowWithValues>getOrCreate(target, targetId, type)
                            .getValues()
                            .addFlattenedId(flattenedPath, targetId);
                    }
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
                    DbRow row = factory.getOrCreate(dbRelationship.getTargetEntity(), targetId, defaultType);
                    if(row instanceof DbRowWithValues && !dbRelationship.isToMany()) {
                        ((DbRowWithValues)row).getValues().addValue(join.getTarget(), add ? srcValue : null);
                    }
                } else {
                    if(srcPK) {
                        srcId.getReplacementIdMap().put(join.getSourceName(), dstValue);
                    }
                    DbRow row = factory.getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType);
                    if(row instanceof DbRowWithValues && !dbRelationship.getReverseRelationship().isToMany()) {
                        ((DbRowWithValues)row).getValues().addValue(join.getSource(), add ? dstValue : null);
                    }
                }
            } else {
                // case 1
                if(srcPK) {
                    DbRow row = factory.getOrCreate(dbRelationship.getTargetEntity(), targetId, defaultType);
                    if(row instanceof DbRowWithValues) {
                        ((DbRowWithValues)row).getValues().addValue(join.getTarget(), add ? srcValue : null);
                    } else {
                        ((DbRowWithQualifier)row).getQualifier().addAdditionalQualifier(join.getTarget(), srcValue);
                    }
                } else {
                    DbRow row = factory.getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType);
                    if(row instanceof DbRowWithValues) {
                        ((DbRowWithValues)row).getValues().addValue(join.getSource(), add ? dstValue : null);
                    } else {
                        ((DbRowWithQualifier)row).getQualifier().addAdditionalQualifier(join.getSource(), dstValue);
                    }
                }
            }
        }
    }

    // not interested in following events in this handler
    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    public void nodeCreated(Object nodeId) {
    }

    public void nodeRemoved(Object nodeId) {
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
    }
}
