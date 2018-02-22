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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.JoinNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.TableNodeBuilder;
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
    }

    private void addResult() {
        for(ColumnDescriptor descriptor : context.getColumnDescriptors()) {
            context.getSelectBuilder().result(table(descriptor.getNamePrefix()).column(descriptor.getName()));
        }
    }

    private void addFrom() {
        context.getTableTree().visit(node -> {
            if(node.relationship == null) {
                context.getSelectBuilder().from(table(node.entity.getFullyQualifiedName()).as(node.tableAlias));
            } else {
                JoinNodeBuilder join;
                TableNodeBuilder tableNode = table(node.entity.getFullyQualifiedName()).as(node.tableAlias);

                switch (node.joinType) {
                    case "inner":
                        join = join(tableNode);
                        break;
                    case "left":
                        join = leftJoin(tableNode);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported join type: " + node.joinType);
                }

                List<DbJoin> joins = node.relationship.getJoins();

                ExpressionNodeBuilder expressionNodeBuilder = null;
                String sourceAlias = context.getTableTree()
                        .aliasForAttributePath(node.attributePath.substring(0, node.attributePath.lastIndexOf('.')));
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

                /*
                 * Attaching root Db entity's qualifier
                 */
//                Expression dbQualifier = node.relationship.getTargetEntity().getQualifier();
//                if (dbQualifier != null) {
//                    dbQualifier = dbQualifier.transform(new JoinStack.JoinedDbEntityQualifierTransformer(node));
//                    qualifierTranslator.setQualifier(dbQualifier);
//                    StringBuilder sb = qualifierTranslator.appendPart(new StringBuilder());
//
//                    NodeBuilder qualifierNode = () -> new TextNode(sb);
//                    if(expressionNodeBuilder == null) {
//                        expressionNodeBuilder = new ExpressionNodeBuilder(qualifierNode);
//                    } else {
//                        expressionNodeBuilder = expressionNodeBuilder.and(qualifierNode);
//                    }
//                }

                context.getSelectBuilder().from(join.on(expressionNodeBuilder));
            }
        });
    }


}
