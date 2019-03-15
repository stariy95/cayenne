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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
public class DbRowFactory {

    protected final ObjectStore store;
    protected final ClassDescriptor descriptor;
    protected final Persistent object;

    protected final Map<DbEntity, DbRow> dbRows;
    protected final Map<String, ObjectId> dbIds;

    public DbRowFactory(ObjectStore store, ClassDescriptor descriptor, Persistent object) {
        this.store = store;
        this.descriptor = descriptor;
        this.object = object;
        this.dbRows = new HashMap<>();
        this.dbIds = new HashMap<>();
    }

    public Collection<? extends DbRow> createRows(ObjectDiff diff) {
        DbEntity rootEntity = descriptor.getEntity().getDbEntity();
        addDbId(rootEntity, object.getObjectId());
        DbRow row = getOrCreate(rootEntity, DbRowType.forObject(object));
        row.accept(new RootRowProcessor(diff));
        return dbRows.values();
    }

    void addDbId(DbEntity entity, ObjectId id) {
        dbIds.put(entity.getName(), id);
    }

    ObjectId getDbId(DbEntity entity) {
        return dbIds.computeIfAbsent(entity.getName(), name -> ObjectId.of("db:" + name));
    }

    @SuppressWarnings("unchecked")
    <E extends DbRow> E get(DbEntity entity) {
        return Objects.requireNonNull((E) dbRows.get(entity));
    }

    @SuppressWarnings("unchecked")
    <E extends DbRow> E getOrCreate(DbEntity entity, DbRowType type) {
        return (E)dbRows.computeIfAbsent(entity, ent -> createRow(ent, type));
    }

    DbRow createRow(DbEntity entity, DbRowType type) {
        switch (type) {
            case INSERT:
                return new InsertDbRow(object, entity, getDbId(entity));
            case UPDATE:
                return new UpdateDbRow(object, entity, getDbId(entity));
            case DELETE:
                return new DeleteDbRow(object, entity, getDbId(entity));
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

    private class RootRowProcessor implements DbRowVisitor<Void> {
        private final ObjectDiff diff;

        public RootRowProcessor(ObjectDiff diff) {
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
            return null;
        }

        @Override
        public Void visitDelete(DeleteDbRow dbRow) {
            // TODO: need to delete all flattened rows...
            return null;
        }
    }
}
