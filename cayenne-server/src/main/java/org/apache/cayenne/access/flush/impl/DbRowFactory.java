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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.access.flush.row.DeleteDbRow;
import org.apache.cayenne.access.flush.row.InsertDbRow;
import org.apache.cayenne.access.flush.row.UpdateDbRow;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
class DbRowFactory {

    private final EntityResolver resolver;
    private final ObjectStore store;
    private final ClassDescriptor descriptor;
    private final Persistent object;
    private final ObjectDiff diff;
    private final Set<ArcTarget> processedArcs;
    private final Map<ObjectId, DbRow> dbRows;

    DbRowFactory(EntityResolver resolver, ObjectStore store, ObjectDiff diff, Set<ArcTarget> processedArcs) {
        ObjectId id = (ObjectId)diff.getNodeId();
        this.resolver = resolver;
        this.store = store;
        this.diff = diff;
        this.descriptor = resolver.getClassDescriptor(id.getEntityName());
        this.object = (Persistent) store.getNode(id);
        this.dbRows = new HashMap<>();
        this.processedArcs = processedArcs;
    }

    Collection<? extends DbRow> createRows() {
        DbEntity rootEntity = descriptor.getEntity().getDbEntity();
        DbRow row = getOrCreate(rootEntity, object.getObjectId(), DbRowType.forObject(object));
        row.accept(new RootRowProcessor(diff));
        return dbRows.values();
    }

    @SuppressWarnings("unchecked")
    <E extends DbRow> E get(ObjectId id) {
        return Objects.requireNonNull((E) dbRows.get(id));
    }

    @SuppressWarnings("unchecked")
    <E extends DbRow> E getOrCreate(DbEntity entity, ObjectId id, DbRowType type) {
        return (E) dbRows.computeIfAbsent(id, nextId -> createRow(entity, id, type));
    }

    private DbRow createRow(DbEntity entity, ObjectId id, DbRowType type) {
        switch (type) {
            case INSERT:
                return new InsertDbRow(object, entity, id);
            case UPDATE:
                return new UpdateDbRow(object, entity, id);
            case DELETE:
                return new DeleteDbRow(object, entity, id);
        }
        throw new CayenneRuntimeException("Unknown DbRowType '%s'", type);
    }

    ClassDescriptor getDescriptor() {
        return descriptor;
    }

    Persistent getObject() {
        return object;
    }

    ObjectStore getStore() {
        return store;
    }

    ObjectDiff getDiff() {
        return diff;
    }

    private DbEntity getDbEntity(ObjectId id) {
        String entityName = id.getEntityName();
        if(entityName.startsWith(PermanentObjectIdVisitor.DB_ID_PREFIX)) {
            entityName = entityName.substring(PermanentObjectIdVisitor.DB_ID_PREFIX.length());
            return resolver.getDbEntity(entityName);
        } else {
            ObjEntity objEntity = resolver.getObjEntity(entityName);
            return objEntity.getDbEntity();
        }
    }

    Set<ArcTarget> getProcessedArcs() {
        return processedArcs;
    }

    private class RootRowProcessor implements DbRowVisitor<Void> {
        private final ObjectDiff diff;

        RootRowProcessor(ObjectDiff diff) {
            this.diff = diff;
        }

        @Override
        public Void visitInsert(InsertDbRow dbRow) {
            diff.apply(new ValuesCreationHandler(DbRowFactory.this, DbRowType.INSERT));
            return null;
        }

        @Override
        public Void visitUpdate(UpdateDbRow dbRow) {
            diff.apply(new ValuesCreationHandler(DbRowFactory.this, DbRowType.UPDATE));
            if(descriptor.getEntity().getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
                descriptor.visitAllProperties(new OptimisticLockQualifierBuilder(dbRow, diff));
            }
            return null;
        }

        @Override
        public Void visitDelete(DeleteDbRow dbRow) {
            if(descriptor.getEntity().isReadOnly()) {
                throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                        "Can't commit changes.", descriptor.getEntity().getName());
            }
            diff.apply(new ArcValuesCreationHandler(DbRowFactory.this, DbRowType.DELETE));
            Collection<ObjectId> flattenedIds = store.getFlattenedIds(dbRow.getChangeId());
            flattenedIds.forEach(id -> DbRowFactory.this.getOrCreate(getDbEntity(id), id, DbRowType.DELETE));
            if(descriptor.getEntity().getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
                descriptor.visitAllProperties(new OptimisticLockQualifierBuilder(dbRow, diff));
            }
            return null;
        }
    }
}
