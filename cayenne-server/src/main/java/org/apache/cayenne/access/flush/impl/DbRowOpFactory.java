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
import org.apache.cayenne.access.flush.row.DbRowOp;
import org.apache.cayenne.access.flush.row.DeleteDbRowOp;
import org.apache.cayenne.access.flush.row.InsertDbRowOp;
import org.apache.cayenne.access.flush.row.UpdateDbRowOp;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
class DbRowOpFactory {

    private final EntityResolver resolver;
    private final ObjectStore store;
    private final ClassDescriptor descriptor;
    private final Persistent object;
    private final ObjectDiff diff;
    private final Set<ArcTarget> processedArcs;
    private final Map<ObjectId, DbRowOp> dbRows;

    DbRowOpFactory(EntityResolver resolver, ObjectStore store, ObjectDiff diff, Set<ArcTarget> processedArcs) {
        ObjectId id = (ObjectId)diff.getNodeId();
        this.resolver = resolver;
        this.store = store;
        this.diff = diff;
        this.descriptor = resolver.getClassDescriptor(id.getEntityName());
        this.object = (Persistent) store.getNode(id);
        this.dbRows = new HashMap<>();
        this.processedArcs = processedArcs;
    }

    Collection<? extends DbRowOp> createRows() {
        DbEntity rootEntity = descriptor.getEntity().getDbEntity();
        DbRowOp row = getOrCreate(rootEntity, object.getObjectId(), DbRowOpType.forObject(object));
        row.accept(new RootRowOpProcessor(this, diff));
        return dbRows.values();
    }

    @SuppressWarnings("unchecked")
    <E extends DbRowOp> E get(ObjectId id) {
        return Objects.requireNonNull((E) dbRows.get(id));
    }

    @SuppressWarnings("unchecked")
    <E extends DbRowOp> E getOrCreate(DbEntity entity, ObjectId id, DbRowOpType type) {
        return (E) dbRows.computeIfAbsent(id, nextId -> createRow(entity, id, type));
    }

    private DbRowOp createRow(DbEntity entity, ObjectId id, DbRowOpType type) {
        boolean meaningfulPk = false;
        if(id == object.getObjectId()) {
            meaningfulPk = !descriptor.getIdProperties().isEmpty();
        }
        switch (type) {
            case INSERT:
                return new InsertDbRowOp(object, entity, id, meaningfulPk);
            case UPDATE:
                return new UpdateDbRowOp(object, entity, id, meaningfulPk);
            case DELETE:
                return new DeleteDbRowOp(object, entity, id, meaningfulPk);
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

    DbEntity getDbEntity(ObjectId id) {
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
}
