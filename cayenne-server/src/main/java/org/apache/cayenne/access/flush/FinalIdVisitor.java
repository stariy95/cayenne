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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;

/**
 * @since 4.2
 */
class FinalIdVisitor implements OperationVisitor<Void> {
    private final CompoundDiff result;

    public FinalIdVisitor(CompoundDiff result) {
        this.result = result;
    }

    @Override
    public Void visitInsert(InsertOperation operation) {
        Persistent object = operation.getObject();
        ObjectId id = operation.getId();

        // TODO: more logic should be here ...

        if (id.isReplacementIdAttached()) {
            ObjectId replacementId = id.createReplacementId();
            if (replacementId.isTemporary()) {
                throw new CayenneRuntimeException("PK for object " + object + " is not set during insert.");
            }
            object.setObjectId(replacementId);
            result.add(new NodeIdChangeOperation(id, replacementId));
        }
        return null;
    }
}
