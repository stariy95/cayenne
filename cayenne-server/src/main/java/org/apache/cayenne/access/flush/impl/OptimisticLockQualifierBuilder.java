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

import java.util.function.Supplier;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.flush.row.DbRowWithQualifier;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * @since 4.2
 */
class OptimisticLockQualifierBuilder implements PropertyVisitor {
    private final DbRowWithQualifier dbRow;
    private final ObjectDiff diff;

    OptimisticLockQualifierBuilder(DbRowWithQualifier dbRow, ObjectDiff diff) {
        this.dbRow = dbRow;
        this.diff = diff;
    }

    @Override
    public boolean visitAttribute(AttributeProperty property) {
        ObjAttribute attribute = property.getAttribute();
        DbAttribute dbAttribute = attribute.getDbAttribute();
        if (attribute.isUsedForLocking() && dbAttribute.getEntity() == dbRow.getEntity()) {
            // TODO: dbAttribute.getEntity() != dbRow.getEntity() ???
            dbRow.getQualifier()
                    .addAdditionalQualifier(dbAttribute, diff.getSnapshotValue(property.getName()), true);

        }
        return true;
    }

    @Override
    public boolean visitToOne(ToOneProperty property) {
        // TODO: implement all cases...
        ObjRelationship relationship = property.getRelationship();
        if(relationship.isUsedForLocking()) {
            ObjectId value = diff.getArcSnapshotValue(property.getName());
            DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
            for(DbJoin join : dbRelationship.getJoins()) {
                DbAttribute source = join.getSource();
                if(!source.isPrimaryKey()) {
                    dbRow.getQualifier().addAdditionalQualifier(source,
                            (Supplier)() -> value.getIdSnapshot().get(join.getTargetName()), true);
                }
            }
        }
        return true;
    }

    @Override
    public boolean visitToMany(ToManyProperty property) {
        return true;
    }
}
