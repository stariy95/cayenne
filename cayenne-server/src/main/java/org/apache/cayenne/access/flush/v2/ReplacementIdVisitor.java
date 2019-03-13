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

package org.apache.cayenne.access.flush.v2;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;

/**
 * @since 4.2
 */
class ReplacementIdVisitor implements DiffSnapshotVisitor<Void> {

    private final ObjectStore store;
    private final CompoundDiff result;

    ReplacementIdVisitor(ObjectStore store, CompoundDiff result) {
        this.store = store;
        this.result = result;
    }

    @Override
    public Void visitInsert(InsertDiffSnapshot diffSnapshot) {
        updateId(diffSnapshot);
        diffSnapshot.getFlattenedIds().forEach((path, id) -> {
            if(id.isTemporary() && id.isReplacementIdAttached()) {
                // resolve lazy suppliers
                for (Map.Entry<String, Object> next : id.getReplacementIdMap().entrySet()) {
                    if (next.getValue() instanceof Supplier) {
                        next.setValue(((Supplier) next.getValue()).get());
                    }
                }
                store.markFlattenedPath(diffSnapshot.getChangeId(), path, id.createReplacementId());
            } else {
                throw new CayenneRuntimeException("PK for flattened path % of object %s is not set during insert."
                        , path, diffSnapshot.getObject());
            }
        });
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDiffSnapshot diffSnapshot) {
        updateId(diffSnapshot);
        return null;
    }

    protected void updateId(DiffSnapshot diffSnapshot) {
        ObjectId id = diffSnapshot.getChangeId();
        if (!id.isTemporary()) {
            return;
        }
        if (!id.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("PK for the object %s is not set during insert.", diffSnapshot.getObject());
        }

        Persistent object = diffSnapshot.getObject();
        ObjectId replacementId = id.createReplacementId();
        if (!replacementId.getEntityName().startsWith(PermanentObjectIdVisitor.DB_ID_PREFIX)) {
            object.setObjectId(replacementId);
            // TODO: process meaningful IDs
        }
        result.add(new NodeIdChangeOperation(id, replacementId));
    }
}
