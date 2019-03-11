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

package org.apache.cayenne.access.flush.v1;

import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
class FinalIdVisitor implements OperationVisitor<Void> {

    private final EntityResolver resolver;
    private final CompoundDiff result;

    private ClassDescriptor lastDescriptor;
    private ObjEntity lastObjEntity;

    FinalIdVisitor(EntityResolver resolver, CompoundDiff result) {
        this.resolver = resolver;
        this.result = result;
    }

    @Override
    public Void visitInsert(InsertOperation operation) {
        setEntity(operation);
        updateObjectId(operation.getObject(), operation.getId());
        return null;
    }

    @Override
    public Void visitUpdate(UpdateOperation operation) {
        setEntity(operation);
        updateObjectId(operation.getObject(), operation.getId());
        return null;
    }

    private void setEntity(Operation operation) {
        ObjectId id = operation.getId();
        if(lastObjEntity == null || !id.getEntityName().equals(lastObjEntity.getName())) {
            lastObjEntity = resolver.getObjEntity(id.getEntityName());
            lastDescriptor = resolver.getClassDescriptor(lastObjEntity.getName());
        }
    }

    private void updateObjectId(Persistent object, ObjectId id) {
        if (id.isReplacementIdAttached()) {
            ObjectId replacementId = id.createReplacementId();
            Map<String, Object> replacementMap = id.getReplacementIdMap();
            if (replacementId.isTemporary()) {
                throw new CayenneRuntimeException("PK for object " + object + " is not set during insert.");
            }

            // TODO: looks really slow..
            //  need to check that we have any meaningful PK at least
            replacementMap.forEach((k, v) -> {
                DbAttribute dbAttribute = lastObjEntity.getDbEntity().getAttribute(k);
                ObjAttribute objAttribute = lastObjEntity.getAttributeForDbAttribute(dbAttribute);
                if(objAttribute != null) {
                    lastDescriptor.getProperty(objAttribute.getName()).writePropertyDirectly(object, null, v);
                }
            });

            object.setObjectId(replacementId);
            result.add(new NodeIdChangeOperation(id, replacementId));
        }
    }
}
