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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DbJoin;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.1
 */
class SqlGenerationStage extends TranslationStage {

    SqlGenerationStage(TranslatorContext context) {
        super(context);
    }

    @Override
    void perform() {
        addResult();
        addFrom();
        context.getSelectBuilder().distinct();
    }

    private void addResult() {
        for(ColumnDescriptor descriptor : context.getColumnDescriptors()) {
            context.getSelectBuilder().result(table(descriptor.getNamePrefix()).column(descriptor.getName()));
        }
    }

    private void addFrom() {
        context.getTableTree().visit(node -> {
            NodeBuilder table = table(node.entity.getFullyQualifiedName()).as(node.tableAlias);
            if(node.relationship != null) {
                table = getJoin(node, table).on(getJoinExpression(node));
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

    private NodeBuilder getJoinExpression(TableTreeNode node) {
        List<DbJoin> joins = node.relationship.getJoins();

        ExpressionNodeBuilder expressionNodeBuilder = null;
        String sourceAlias = context.getTableTree().aliasForAttributePath(node.attributePath);
        for (DbJoin dbJoin : joins) {
            String srcColumn = dbJoin.getSourceName();
            String dstColumn = dbJoin.getTargetName();
            ExpressionNodeBuilder joinExp = table(sourceAlias).column(srcColumn)
                    .eq(table(node.tableAlias).column(dstColumn));

            if (expressionNodeBuilder != null) {
                expressionNodeBuilder.and(joinExp);
            } else {
                expressionNodeBuilder = joinExp;
            }
        }

        expressionNodeBuilder = attachTargetQualifier(node, expressionNodeBuilder);

        return expressionNodeBuilder;
    }

    private ExpressionNodeBuilder attachTargetQualifier(TableTreeNode node, ExpressionNodeBuilder expressionNodeBuilder) {
        Expression dbQualifier = node.relationship.getTargetEntity().getQualifier();
        if (dbQualifier != null) {
            QualifierTranslator translator = new QualifierTranslator(context);
            dbQualifier = dbQualifier.transform(new JoinedDbEntityQualifierTransformer(node));
            NodeBuilder translatedQualifier = translator.translate(dbQualifier);
            if (expressionNodeBuilder != null) {
                expressionNodeBuilder.and(translatedQualifier);
            } else {
                expressionNodeBuilder = new ExpressionNodeBuilder(translatedQualifier);
            }
        }
        return expressionNodeBuilder;
    }


    static class JoinedDbEntityQualifierTransformer implements Function<Object, Object> {

        String pathToRoot;

        JoinedDbEntityQualifierTransformer(TableTreeNode node) {
            pathToRoot = node.attributePath;
        }

        public Object apply(Object input) {
            if (input instanceof ASTPath) {
                return new ASTDbPath(pathToRoot + ((ASTPath) input).getPath());
            }
            return input;
        }
    }
}
