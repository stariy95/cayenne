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

import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.JoinNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.1
 */
public class SQLPreparationStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        addResult(context);
        addFrom(context);
    }

    private void addResult(TranslatorContext context) {
        int counter = 0;
        for(TranslatorContext.ResultNode resultNode : context.getResultNodeList()) {
            context.getSelectBuilder().result(resultNode::getNode);

            if(!resultNode.isInDataRow()) {
                continue;
            }

            String name = "c" + counter;
            DbAttribute attribute = resultNode.getDbAttribute();
            if(attribute != null) {
                name = resultNode.getDbAttribute().getName();
            }

            ColumnDescriptor descriptor = new ColumnDescriptor(name, resultNode.getJdbcType());
            descriptor.setAttribute(resultNode.getDbAttribute());
            descriptor.setDataRowKey(resultNode.getDataRowKey());
            descriptor.setJavaClass(resultNode.getJavaType());

            context.getColumnDescriptors().add(descriptor);
            counter++;
        }
    }

    private void addFrom(TranslatorContext context) {
        context.getTableTree().visit(node -> {
            NodeBuilder table = table(node.entity.getFullyQualifiedName()).as(node.tableAlias);
            if(node.relationship != null) {
                table = getJoin(node, table).on(getJoinExpression(context, node));
            }

            context.getSelectBuilder().from(table);
        });
    }

    private JoinNodeBuilder getJoin(TableTreeNode node, NodeBuilder table) {
        switch (node.joinType) {
            case INNER:
                return join(table);
            case LEFT_OUTER:
                return leftJoin(table);
            default:
                throw new IllegalArgumentException("Unsupported join type: " + node.joinType);
        }
    }

    private NodeBuilder getJoinExpression(TranslatorContext context, TableTreeNode node) {
        List<DbJoin> joins = node.relationship.getJoins();

        ExpressionNodeBuilder expressionNodeBuilder = null;
        String sourceAlias = context.getTableTree().aliasForAttributePath(node.attributePath.getPath());
        for (DbJoin dbJoin : joins) {
            DbAttribute src = dbJoin.getSource();
            DbAttribute dst = dbJoin.getTarget();
            ExpressionNodeBuilder joinExp = table(sourceAlias).column(src)
                    .eq(table(node.tableAlias).column(dst));

            if (expressionNodeBuilder != null) {
                expressionNodeBuilder = expressionNodeBuilder.and(joinExp);
            } else {
                expressionNodeBuilder = joinExp;
            }
        }

        expressionNodeBuilder = attachTargetQualifier(context, node, expressionNodeBuilder);

        return expressionNodeBuilder;
    }

    private ExpressionNodeBuilder attachTargetQualifier(TranslatorContext context, TableTreeNode node, ExpressionNodeBuilder expressionNodeBuilder) {
        Expression dbQualifier = node.relationship.getTargetEntity().getQualifier();
        if (dbQualifier != null) {
            QualifierTranslator translator = new QualifierTranslator(context);
            dbQualifier = dbQualifier.transform(new JoinedDbEntityQualifierTransformer(node));
            Node translatedQualifier = translator.translate(dbQualifier);
            if (expressionNodeBuilder != null) {
                expressionNodeBuilder = expressionNodeBuilder.and(() -> translatedQualifier);
            } else {
                expressionNodeBuilder = new ExpressionNodeBuilder(() -> translatedQualifier);
            }
        }
        return expressionNodeBuilder;
    }


    static class JoinedDbEntityQualifierTransformer implements Function<Object, Object> {

        String pathToRoot;

        JoinedDbEntityQualifierTransformer(TableTreeNode node) {
            pathToRoot = node.attributePath.getPath();
        }

        public Object apply(Object input) {
            if (input instanceof ASTPath) {
                return new ASTDbPath(pathToRoot + '.' + ((ASTPath) input).getPath());
            }
            return input;
        }
    }
}
