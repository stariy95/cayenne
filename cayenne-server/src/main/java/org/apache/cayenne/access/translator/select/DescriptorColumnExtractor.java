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

package org.apache.cayenne.access.translator.select;

import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.2
 */
class DescriptorColumnExtractor extends BaseColumnExtractor implements PropertyVisitor {

    private static final String PREFETCH_PREFIX = "p:";

    private final ClassDescriptor descriptor;
    private final PathTranslator pathTranslator;
    private final Set<String> columnTracker = new HashSet<>();

    private EntityResult entityResult;
    private String prefix;
    private String labelPrefix;

    DescriptorColumnExtractor(TranslatorContext context, ClassDescriptor descriptor) {
        super(context);
        this.descriptor = descriptor;
        this.pathTranslator = context.getPathTranslator();
        this.entityResult = new EntityResult(descriptor.getObjectClass());
    }

    public void extract(String prefix) {
        this.prefix = prefix;
        boolean newEntityResult = true;
        this.labelPrefix = null;
        TranslatorContext.DescriptorType type = TranslatorContext.DescriptorType.OTHER;

        if(prefix != null && prefix.startsWith(PREFETCH_PREFIX)) {
            type = TranslatorContext.DescriptorType.PREFETCH;
            labelPrefix = prefix.substring(2);
            entityResult = context.getSqlResult().getLastEntityResult();
            newEntityResult = false;
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
                String name = labelPrefix == null ? dba.getName() : labelPrefix + '.' + dba.getName();
                entityResult.addDbField(name, name);
            }
        }

        if(newEntityResult) {
            context.getSqlResult().addEntityResult(entityResult);
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
                String name = labelPrefix == null
                        ? oa.getDbAttribute().getName()
                        : labelPrefix + '.' + oa.getDbAttribute().getName();
                entityResult.addDbField(name, name);
            }
        }

        return true;
    }

    @Override
    public boolean visitToOne(ToOneProperty property) {
        ObjRelationship rel = property.getRelationship();
        // outer join should be used for to-one relationships
        PathTranslationResult result = pathTranslator.translatePath(rel.getSourceEntity(), property.getName() + '+', prefix);

        int count = result.getDbAttributes().size();
        for(int i=0; i<count; i++) {
            processTranslationResult(result, i);
            DbAttribute attribute = result.getDbAttributes().get(i);
            String name = labelPrefix == null ? attribute.getName() : labelPrefix + '.' + attribute.getName();
            entityResult.addDbField(name, name);
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
                if(columnLabelPrefix.startsWith(PREFETCH_PREFIX)) {
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
