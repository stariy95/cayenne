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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * @since 4.2
 */
class OptimisticLockDeleteQualifierBuilder implements PropertyVisitor {

    private DeleteSnapshotCreationHandler deleteSnapshotCreationHandler;
    private final Persistent object;

    OptimisticLockDeleteQualifierBuilder(DeleteSnapshotCreationHandler deleteSnapshotCreationHandler, Persistent object) {
        this.deleteSnapshotCreationHandler = deleteSnapshotCreationHandler;
        this.object = object;
    }

    @Override
    public boolean visitAttribute(AttributeProperty property) {
        if (property.getAttribute().isUsedForLocking()) {
            DbAttribute attribute = property.getAttribute().getDbAttribute();
            DeleteDiffSnapshot snapshot = deleteSnapshotCreationHandler.getSnapshot(attribute);
            snapshot.addOptimisticLockQualifier(attribute, property.readPropertyDirectly(object));
        }
        return true;
    }

    @Override
    public boolean visitToOne(ToOneProperty property) {
        if (property.getRelationship().isUsedForLocking()) {
            DbRelationship sourceRelationship = property.getRelationship().getDbRelationships().get(0);
            for (DbJoin join : sourceRelationship.getJoins()) {
                UpdateDiffSnapshot snapshot = deleteSnapshotCreationHandler.getSnapshot(join.getSource());
                ObjectId targetId = getTargetId(property.readPropertyDirectly(object));
                snapshot.addOptimisticLockQualifier(join.getSource(), ObjectValueSupplier.getFor(targetId, join.getTarget()));
            }
        }
        return true;
    }

    private ObjectId getTargetId(Object value) {
        if(!(value instanceof Persistent)) {
            return null;
        }
        return ((Persistent)value).getObjectId();
    }

    @Override
    public boolean visitToMany(ToManyProperty property) {
        return true;
    }
}
