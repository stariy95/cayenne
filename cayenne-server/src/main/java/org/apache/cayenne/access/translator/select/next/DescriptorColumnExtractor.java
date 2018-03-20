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

import java.util.HashSet;
import java.util.Set;

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
    private Set<String> columnTracker = new HashSet<>();

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
        String alias = context.getTableTree().aliasForPath(prefix);
        for (DbAttribute dba : table.getPrimaryKeys()) {
            String columnUniqueName = alias + '.' + dba.getName();
            if(columnTracker.add(columnUniqueName)) {
                addDbAttribute(prefix, columnLabelPrefix, dba);
            }
        }
    }

    @Override
    public boolean visitAttribute(AttributeProperty property) {
        ObjAttribute oa = property.getAttribute();
        PathTranslator.PathTranslationResult result = pathTranslator.translatePath(oa.getEntity(), property.getName());

        DbAttribute attribute = result.getLastAttribute();

        String path = result.getFinalPath();
        if(prefix != null) {
            path = prefix + '.' + path;
        }
        String alias = context.getTableTree().aliasForAttributePath(path);

        String columnUniqueName = alias + '.' + attribute.getName();
        if(columnTracker.add(columnUniqueName)) {
            String dataRowKey = columnLabelPrefix != null
                    ? columnLabelPrefix + '.' + oa.getDbAttributePath()
                    : oa.getDbAttributePath();
            Node columnNode = table(alias).column(attribute).build();
            context.addResultNode(columnNode, true, dataRowKey).setJavaType(oa.getType());
        }

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
            String columnUniqueName = alias + '.' + attribute.getName();
            if(columnTracker.add(columnUniqueName)) {
                Node columnNode = table(alias).column(attribute).build();
                String dataRowKey = columnLabelPrefix != null
                        ? columnLabelPrefix + '.' + attribute.getName()
                        : attribute.getName();
                context.addResultNode(columnNode, true, dataRowKey);
            }
        }

        return true;
    }

    @Override
    public boolean visitToMany(ToManyProperty property) {
        return true;
    }
}
