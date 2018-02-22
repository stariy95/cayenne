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

package org.apache.cayenne.access.translator.select.next;

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.1
 */
class DescriptorColumnExtractor implements PropertyVisitor, ColumnExtractor {

    private final TranslatorContext context;
    private final ClassDescriptor descriptor;

    DescriptorColumnExtractor(TranslatorContext context) {
        this.context = context;
        this.descriptor = context.getMetadata().getClassDescriptor();
    }

    public void extract() {
        context.getTableTree().addRootTable(descriptor.getEntity().getDbEntity());
        descriptor.visitAllProperties(this);

        // add remaining needed attrs from DbEntity
        DbEntity table = descriptor.getEntity().getDbEntity();
        for (DbAttribute dba : table.getPrimaryKeys()) {
            String alias = context.getTableTree().aliasForAttributePath(dba.getName());
            ColumnDescriptor column = new ColumnDescriptor(dba, alias);
            context.getColumnDescriptors().add(column);
        }
    }

    public boolean visitAttribute(AttributeProperty property) {
        ObjAttribute oa = property.getAttribute();
        Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
        StringBuilder sb = new StringBuilder();
        while (dbPathIterator.hasNext()) {
            Object pathPart = dbPathIterator.next();
            sb.append(pathPart);

            if (pathPart == null) {
                throw new CayenneRuntimeException("ObjAttribute has no component: %s", oa.getName());
            } else if (pathPart instanceof DbRelationship) {
                DbRelationship rel = (DbRelationship) pathPart;
                context.getTableTree().addJoinTable(sb.toString(), rel, "left");
            } else if (pathPart instanceof DbAttribute) {
                DbAttribute dbAttr = (DbAttribute) pathPart;
                String alias = context.getTableTree().aliasForAttributePath(oa.getDbAttributePath());
                ColumnDescriptor column = new ColumnDescriptor(oa, dbAttr, alias);
                context.getColumnDescriptors().add(column);
            }
        }
        return true;
    }

    public boolean visitToOne(ToOneProperty property) {
        ObjRelationship rel = property.getRelationship();
        DbRelationship dbRel = rel.getDbRelationships().get(0);

        List<DbJoin> joins = dbRel.getJoins();
        for (DbJoin join : joins) {
            DbAttribute src = join.getSource();
            String alias = context.getTableTree().aliasForAttributePath(rel.getDbRelationshipPath());
            ColumnDescriptor column = new ColumnDescriptor(src, alias);
            context.getColumnDescriptors().add(column);
        }
        return true;
    }

    public boolean visitToMany(ToManyProperty property) {
        return true;
    }
}
