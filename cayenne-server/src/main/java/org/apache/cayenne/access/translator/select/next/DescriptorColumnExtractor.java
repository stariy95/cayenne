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
import java.util.Map;
import java.util.Set;

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

    private static final char[] PREFETCH_PREFIX = {'p', ':'};

    private final ClassDescriptor descriptor;
    private final PathTranslator pathTranslator;

    private String prefix;
    private Set<String> columnTracker = new HashSet<>();

    DescriptorColumnExtractor(TranslatorContext context, ClassDescriptor descriptor) {
        super(context);
        this.descriptor = descriptor;
        this.pathTranslator = context.getPathTranslator();
    }

    public void extract(String prefix) {
        this.prefix = prefix;
        String labelPrefix = prefix;
        TranslatorContext.DescriptorType type = TranslatorContext.DescriptorType.OTHER;
        if(prefix != null && prefix.length() > 2
                && prefix.charAt(0) == PREFETCH_PREFIX[0] && prefix.charAt(1) == PREFETCH_PREFIX[1]) {
            type = TranslatorContext.DescriptorType.PREFETCH;
            labelPrefix = prefix.substring(2);
        } else if(descriptor.getEntity().getDbEntity() == context.getRootDbEntity()) {
            type = TranslatorContext.DescriptorType.ROOT;
        }
        context.markDescriptorStart(type);

        descriptor.visitAllProperties(this);

        // add remaining needed attrs from DbEntity
        DbEntity table = descriptor.getEntity().getDbEntity();
        String alias = context.getTableTree().aliasForPath(prefix);
        for (DbAttribute dba : table.getPrimaryKeys()) {
            String columnUniqueName = alias + '.' + dba.getName();
            if(columnTracker.add(columnUniqueName)) {
                addDbAttribute(prefix, labelPrefix, dba);
            }
        }
        context.markDescriptorEnd(type);
    }

    @Override
    public boolean visitAttribute(AttributeProperty property) {
        ObjAttribute oa = property.getAttribute();
        PathTranslationResult result = pathTranslator.translatePath(oa.getEntity(), property.getName(), prefix);

        int count = result.getDbAttributes().size();
        for(int i=0; i<count; i++) {
            ResultNodeDescriptor resultNodeDescriptor = processTranslationResult(result, i);
            if(resultNodeDescriptor != null && i == count - 1) {
                resultNodeDescriptor.setJavaType(oa.getType());
            }
        }

        return true;
    }

    @Override
    public boolean visitToOne(ToOneProperty property) {
        ObjRelationship rel = property.getRelationship();
        PathTranslationResult result = pathTranslator.translatePath(rel.getSourceEntity(), property.getName(), prefix);

        int count = result.getDbAttributes().size();
        for(int i=0; i<count; i++) {
            processTranslationResult(result, i);
        }

        return true;
    }

    private ResultNodeDescriptor processTranslationResult(PathTranslationResult result, int i) {
        String path = result.getAttributePaths().get(i);
        String alias = context.getTableTree().aliasForPath(path);
        DbAttribute attribute = result.getDbAttributes().get(i);

        String columnUniqueName = alias + '.' + attribute.getName();
        if(columnTracker.add(columnUniqueName)) {
            String columnLabelPrefix = path;
            if(columnLabelPrefix.length() > 2) {
                if(columnLabelPrefix.charAt(0) == PREFETCH_PREFIX[0] && columnLabelPrefix.charAt(1) == PREFETCH_PREFIX[1]) {
                    columnLabelPrefix = columnLabelPrefix.substring(2);
                }
            }
            String attributeName = columnLabelPrefix.isEmpty()
                    ? attribute.getName()
                    : columnLabelPrefix + '.' + attribute.getName();

            Node columnNode = table(alias).column(attribute).build();
            return context.addResultNode(columnNode, attributeName).setDbAttribute(attribute);
        }

        return null;
    }

    @Override
    public boolean visitToMany(ToManyProperty property) {
        return true;
    }
}
