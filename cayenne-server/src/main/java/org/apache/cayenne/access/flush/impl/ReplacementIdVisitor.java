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

import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.access.flush.row.InsertDbRow;
import org.apache.cayenne.access.flush.row.UpdateDbRow;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.AttributeProperty;

/**
 * @since 4.2
 */
class ReplacementIdVisitor implements DbRowVisitor<Void> {

    private final ObjectStore store;
    private final EntityResolver resolver;
    private final CompoundDiff result;

    ReplacementIdVisitor(ObjectStore store, EntityResolver resolver, CompoundDiff result) {
        this.store = store;
        this.resolver = resolver;
        this.result = result;
    }

    @Override
    public Void visitInsert(InsertDbRow dbRow) {
        updateId(dbRow);
        dbRow.getValues().getFlattenedIds().forEach((path, id) -> {
            if(id.isTemporary() && id.isReplacementIdAttached()) {
                // resolve lazy suppliers
                for (Map.Entry<String, Object> next : id.getReplacementIdMap().entrySet()) {
                    if (next.getValue() instanceof Supplier) {
                        next.setValue(((Supplier) next.getValue()).get());
                    }
                }
                store.markFlattenedPath(dbRow.getChangeId(), path, id.createReplacementId());
            } else {
                throw new CayenneRuntimeException("PK for flattened path '%s' of object %s is not set during insert."
                        , path, dbRow.getObject());
            }
        });
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRow dbRow) {
        updateId(dbRow);
        return null;
    }

    protected void updateId(DbRow dbRow) {
        ObjectId id = dbRow.getChangeId();
        if (!id.isReplacementIdAttached()) {
            if (id.isTemporary()) {
                throw new CayenneRuntimeException("PK for the object %s is not set during insert.", dbRow.getObject());
            }
            return;
        }

        Persistent object = dbRow.getObject();
        Map<String, Object> replacement = id.getReplacementIdMap();
        ObjectId replacementId = id.createReplacementId();
        if (object.getObjectId() == id && !replacementId.getEntityName().startsWith(PermanentObjectIdVisitor.DB_ID_PREFIX)) {
            object.setObjectId(replacementId);
            // update meaningful PKs
            // TODO: optimize this?
            for (AttributeProperty property: resolver.getClassDescriptor(replacementId.getEntityName()).getIdProperties()) {
                if(property.getAttribute() != null) {
                    Object value = replacement.get(property.getAttribute().getDbAttributeName());
                    if (value != null) {
                        property.writePropertyDirectly(object, null, value);
                    }
                }
            }
            result.add(new NodeIdChangeOperation(id, replacementId));
        }
    }
}