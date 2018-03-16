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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.1
 */
class DescriptorColumnExtractor extends BaseColumnExtractor implements PropertyVisitor {

    private final ClassDescriptor descriptor;
    private final PathTranslator pathTranslator;
    private final String columnLabelPrefix;
    private String prefix;

    DescriptorColumnExtractor(TranslatorContext context, ClassDescriptor descriptor, String columnLabelPrefix) {
        super(context);
        this.descriptor = descriptor;
        this.pathTranslator = new PathTranslator(context);
        this.columnLabelPrefix = columnLabelPrefix;
    }

    public void extract(String prefix) {
        this.prefix = prefix;

        descriptor.visitAllProperties(this);

        // add remaining needed attrs from DbEntity
        DbEntity table = descriptor.getEntity().getDbEntity();
        for (DbAttribute dba : table.getPrimaryKeys()) {
            addDbAttribute(prefix, columnLabelPrefix, dba);
        }
    }

    @Override
    public boolean visitAttribute(AttributeProperty property) {
        ObjAttribute oa = property.getAttribute();
        PathTranslator.PathTranslationResult result = pathTranslator.translatePath(oa.getEntity(), property.getName());
        String path = result.getFinalPath();
        if(prefix != null) {
            path = prefix + '.' + path;
        }
        String alias = context.getTableTree().aliasForAttributePath(path);

        DbAttribute attribute = result.getDbAttributes().get(result.getDbAttributes().size() - 1);
        ColumnDescriptor column = new ColumnDescriptor(oa, attribute, alias);
        if(columnLabelPrefix != null) {
            column.setDataRowKey(columnLabelPrefix + '.' + attribute.getName());
        }
//        context.getColumnDescriptors().add(column);

        Node columnNode = table(alias).column(attribute.getName()).attribute(attribute).build();
        context.addResultNode(columnNode, true,
                columnLabelPrefix != null ? columnLabelPrefix + '.' + attribute.getName() : null);

        return true;
    }

    @Override
    public boolean visitToOne(ToOneProperty property) {
        ObjRelationship rel = property.getRelationship();
        PathTranslator.PathTranslationResult result = pathTranslator.translatePath(rel.getSourceEntity(), property.getName());

        String path = rel.getDbRelationshipPath();
        if(prefix != null) {
            path = prefix + '.' + path;
        }
        String alias = context.getTableTree().aliasForAttributePath(path);

        for(DbAttribute attribute : result.getDbAttributes()) {
            ColumnDescriptor column = new ColumnDescriptor(attribute, alias);
            if(columnLabelPrefix != null) {
                column.setDataRowKey(columnLabelPrefix + '.' + attribute.getName());
            }
//            context.getColumnDescriptors().add(column);
            Node columnNode = table(alias).column(attribute.getName()).attribute(attribute).build();
            context.addResultNode(columnNode, true,
                    columnLabelPrefix != null ? columnLabelPrefix + '.' + attribute.getName() : null);
        }

        return true;
    }

    @Override
    public boolean visitToMany(ToManyProperty property) {
        return true;
    }
}
