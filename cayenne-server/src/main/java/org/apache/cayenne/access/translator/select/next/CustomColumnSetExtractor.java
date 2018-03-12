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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.exp.parser.ASTFullObject;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.1
 */
class CustomColumnSetExtractor implements ColumnExtractor {

    private final TranslatorContext context;
    private final Collection<Property<?>> columns;

    CustomColumnSetExtractor(TranslatorContext context, Collection<Property<?>> columns) {
        this.context = context;
        this.columns = columns;
    }

    @Override
    public void extract(String prefix) {
        QualifierTranslator translator = new QualifierTranslator(context);
        Collection<String> aggregateExpressions = new HashSet<>(columns.size());

        for(Property<?> property : columns) {
            NodeBuilder nextNode = translator.translate(property.getExpression());

            if(checkAndExtractFullObject(prefix, property)) {
                continue;
            }

            SQLGenerationVisitor visitor = new SQLGenerationVisitor(context);
            nextNode.build().visit(visitor);
            String exp = visitor.getSQLString();

            int type = getJdbcTypeForProperty(property);

            ColumnDescriptor descriptor = new ColumnDescriptor(exp, type,
                    property.getType() == null ? null : property.getType().getCanonicalName());
            descriptor.setDataRowKey(property.getAlias());
            descriptor.setIsExpression(true);
            context.getColumnDescriptors().add(descriptor);

            if(property.getExpression() instanceof ASTAggregateFunctionCall) {
                aggregateExpressions.add(exp);
            }
        }

        if(!aggregateExpressions.isEmpty() || context.getQuery().getHavingQualifier() != null) {
            context.getColumnDescriptors().forEach(c -> {
                String groupByExp = c.getName();
                String groupByPrefix = c.getNamePrefix();
                if(groupByPrefix != null) {
                    groupByExp = groupByPrefix + '.' + groupByExp;
                }
                Node node = new TextNode(groupByExp);
                if (!aggregateExpressions.contains(groupByExp)) {
                    context.getSelectBuilder().groupBy(() -> node);
                }
            });
        }
    }

    private boolean checkAndExtractFullObject(String prefix, Property<?> property) {
        int expressionType = property.getExpression().getType();

        // forbid direct selection of toMany relationships columns
        if(property.getType() != null && (expressionType == Expression.OBJ_PATH || expressionType == Expression.DB_PATH)
                && (Collection.class.isAssignableFrom(property.getType())
                || Map.class.isAssignableFrom(property.getType()))) {
            throw new CayenneRuntimeException("Can't directly select toMany relationship columns. " +
                    "Either select it with aggregate functions like count() or with flat() function to select full related objects.");
        }

        // evaluate ObjPath with Persistent type as toOne relations and use it as full object
        boolean objectProperty = expressionType == Expression.FULL_OBJECT
                || (property.getType() != null && expressionType == Expression.OBJ_PATH
                    && Persistent.class.isAssignableFrom(property.getType()));

        if(!objectProperty) {
            return false;
        }

        Expression propertyExpression = property.getExpression();
        if(expressionType == Expression.FULL_OBJECT && propertyExpression != null
                && propertyExpression.getOperandCount() > 0) {
            Object op = propertyExpression.getOperand(0);
            if(op instanceof ASTPath) {
                prefix = ((ASTPath) op).getPath();
            }
        } else if(propertyExpression instanceof ASTPath) {
            prefix = ((ASTPath) propertyExpression).getPath();
        }

        ColumnExtractor extractor;
        ObjEntity entity = context.getResolver().getObjEntity(property.getType());
        if(context.getMetadata().getPageSize() > 0) {
            extractor = new IdColumnExtractor(context, entity);
        } else {
            ClassDescriptor descriptor = context.getResolver().getClassDescriptor(entity.getName());
            extractor = new DescriptorColumnExtractor(context, descriptor);
        }
        extractor.extract(prefix);
        return true;
    }

    private int getJdbcTypeForProperty(Property<?> property) {
        int expressionType = property.getExpression().getType();
        if(expressionType == Expression.OBJ_PATH) {
            // Scan obj path, stop as soon as DbAttribute found
            for (PathComponent<ObjAttribute, ObjRelationship> component :
                    context.getMetadata().getObjEntity().resolvePath(property.getExpression(), Collections.emptyMap())) {
                if(component.getAttribute() != null) {
                    Iterator<CayenneMapEntry> dbPathIterator = component.getAttribute().getDbPathIterator();
                    while (dbPathIterator.hasNext()) {
                        Object pathPart = dbPathIterator.next();
                        if (pathPart instanceof DbAttribute) {
                            return ((DbAttribute) pathPart).getType();
                        }
                    }
                }
            }
        } else if(expressionType == Expression.DB_PATH) {
            // Scan db path, stop as soon as DbAttribute found
            for (PathComponent<DbAttribute, DbRelationship> component :
                    context.getMetadata().getDbEntity().resolvePath(property.getExpression(), Collections.emptyMap())) {
                if(component.getAttribute() != null) {
                    return component.getAttribute().getType();
                }
            }
        }
        // NOTE: If no attribute found or expression have some other type
        // return JDBC type based on Java type of the property.
        // This can lead to incorrect behavior in case we deal with some custom type
        // backed by ExtendedType with logic based on correct JDBC type provided (luckily not very common case).
        // In general we can't map any meaningful type, as we don't know outcome of the expression in the property.
        // If this ever become a problem correct type can be provided at the query call time, using meta data.
        return TypesMapping.getSqlTypeByJava(property.getType());
    }
}
