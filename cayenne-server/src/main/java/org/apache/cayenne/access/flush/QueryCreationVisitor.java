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

package org.apache.cayenne.access.flush;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
class QueryCreationVisitor implements OperationVisitor<BatchQuery> {

    private final DataContext context;

    QueryCreationVisitor(DataContext context) {
        this.context = context;
    }

    @Override
    public BatchQuery visitInsert(InsertOperation operation) {
        ObjEntity objEntity = context.getEntityResolver().getObjEntity(operation.getObject());
        if(objEntity.isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", objEntity.getName());
        }

        InsertSnapshotHandler handler = new InsertSnapshotHandler(objEntity);
        operation.getDiff().apply(handler);
        Map<String, Object> snapshot = handler.getSnapshot();
        if(snapshot == null) {
            snapshot = operation.getId().getIdSnapshot();
        } else {
            snapshot.putAll(operation.getId().getIdSnapshot());
        }

        InsertBatchQuery query = new InsertBatchQuery(objEntity.getDbEntity(), 1);
        query.add(snapshot, operation.getId());

        return query;
    }

    @Override
    public BatchQuery visitUpdate(UpdateOperation operation) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(operation.getId().getEntityName());
        ObjEntity objEntity = descriptor.getEntity();
        DbEntity dbEntity = objEntity.getDbEntity();
        ArrayList<DbAttribute> qualifierAttributes = new ArrayList<>(dbEntity.getPrimaryKeys());

        UpdateSnapshotHandler handler = new UpdateSnapshotHandler(descriptor);
        operation.getDiff().apply(handler);

        if(handler.hasChanges()) {
            UpdateBatchQuery query = new UpdateBatchQuery(dbEntity, qualifierAttributes, handler.getModifiedAttributes(), Collections.emptyList(), 1);
            query.add(operation.getId().getIdSnapshot(), handler.getSnapshot(), operation.getId());
            return query;
        }

        return null;
    }

    @Override
    public BatchQuery visitDelete(DeleteOperation operation) {
        ObjEntity objEntity = context.getEntityResolver().getObjEntity(operation.getObject());
        if(objEntity.isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", objEntity.getName());
        }

        DbEntity dbEntity = objEntity.getDbEntity();
        ArrayList<DbAttribute> qualifierAttributes = new ArrayList<>(dbEntity.getPrimaryKeys());
        DeleteBatchQuery query = new DeleteBatchQuery(dbEntity, qualifierAttributes, Collections.emptyList(), 1);
        query.add(operation.getId().getIdSnapshot());

        return query;
    }

}
