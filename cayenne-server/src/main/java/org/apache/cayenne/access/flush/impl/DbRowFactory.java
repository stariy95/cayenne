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
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.2
 */
public class DbRowFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRowFactory.class);

    protected final EntityResolver resolver;
    protected final ObjectStore store;
    protected final ClassDescriptor descriptor;
    protected final Persistent object;

    protected final Set<ArcTarget> processedArcs;
    protected final Map<ObjectId, DbRow> dbRows;

    public DbRowFactory(EntityResolver resolver, ObjectStore store, ClassDescriptor descriptor, Persistent object, Set<ArcTarget> processedArcs) {
        this.resolver = resolver;
        this.store = store;
        this.descriptor = descriptor;
        this.object = object;
        this.dbRows = new HashMap<>();
        this.processedArcs = processedArcs;
    }

    public Collection<? extends DbRow> createRows(ObjectDiff diff) {
        DbEntity rootEntity = descriptor.getEntity().getDbEntity();
        DbRow row = getOrCreate(rootEntity, object.getObjectId(), DbRowType.forObject(object));
        row.accept(new RootRowProcessor(diff));
        return dbRows.values()
                .stream()
                .map(next -> next.accept(new ReplacementVisitor()))
                .collect(Collectors.toList());
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
                LOGGER.info("Create insert for " + entity.getName() + " " + id);
                return new InsertDbRow(object, entity, id);
            case UPDATE:
                LOGGER.info("Create update for " + entity.getName() + " " + id);
                return new UpdateDbRow(object, entity, id);
            case DELETE:
                LOGGER.info("Create delete for " + entity.getName() + " " + id);
                return new DeleteDbRow(object, entity, id);
        }
        throw new CayenneRuntimeException("Unknown DbRowType '%s'", type);
    }

    public ClassDescriptor getDescriptor() {
        return descriptor;
    }

    public Persistent getObject() {
        return object;
    }

    public ObjectStore getStore() {
        return store;
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

    public Set<ArcTarget> getProcessedArcs() {
        return processedArcs;
    }

    private static class ReplacementVisitor implements DbRowVisitor<DbRow> {
        @Override
        public DbRow visitUpdate(UpdateDbRow dbRow) {
            return dbRow;
//            Map<String, Object> snapshot = dbRow.getValues().getSnapshot();
//            for(DbAttribute pk: dbRow.getEntity().getPrimaryKeys()) {
//                if(dbRow.getValues().getUpdatedAttributes().contains(pk)) {
//                    Object value = snapshot.get(pk.getName());
//                    if(value instanceof Supplier) {
//                        value = ((Supplier) value).get();
//                    }
//                    if(value != null) {
//                        return dbRow;
//                    }
//                }
//            }
//            return new DeleteDbRow(dbRow.getObject(), dbRow.getEntity(), dbRow.getChangeId());
        }

        @Override
        public DbRow visitInsert(InsertDbRow dbRow) {
            return dbRow;
        }

        @Override
        public DbRow visitDelete(DeleteDbRow dbRow) {
            return dbRow;
        }
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
            descriptor.visitAllProperties(new OptimisticLockQualifierBuilder(DbRowFactory.this, dbRow));
            return null;
        }

        @Override
        public Void visitDelete(DeleteDbRow dbRow) {
            if(descriptor.getEntity().isReadOnly()) {
                throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                        "Can't commit changes.", descriptor.getEntity().getName());
            }
            Collection<ObjectId> flattenedIds = store.getFlattenedIds(dbRow.getChangeId());
            flattenedIds.forEach(id -> DbRowFactory.this.getOrCreate(getDbEntity(id), id, DbRowType.DELETE));
            descriptor.visitAllProperties(new OptimisticLockQualifierBuilder(DbRowFactory.this, dbRow));
            return null;
        }
    }

}
