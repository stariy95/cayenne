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

package org.apache.cayenne.dba.oracle;

import java.util.function.Function;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.SelectBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.dba.derby.sqltree.DerbyColumnNode;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.1
 */
public class OracleSqlTreeProcessor implements Function<Node, Node> {

    SelectBuilder selectBuilder = select(column("*"))
            .from(select(column("rownum").as("rnum"), table("a").column("*")).from().where(column("rownum").lte(value(10))))
            .where(column("rnum").gt(value(5)));

    @Override
    public Node apply(Node node) {
        SelectBuilder[] selectBuilder = {null};

        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {

            }

            @Override
            public void onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
                switch (child.getType()) {
                    case RESULT:
                        for(int i=0; i<child.getChildrenCount(); i++) {
                            child.replaceChild(i, aliased(child.getChild(i), "c" + i));
                        }
                        break;
                    case COLUMN:
                        DerbyColumnNode replacement = new DerbyColumnNode((ColumnNode)child);
                        for(int i=0; i<child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                        break;
                    case FUNCTION:
                        FunctionNode oldNode = (FunctionNode) child;
                        String functionName = oldNode.getFunctionName();
                        Node functionReplacement = null;
                        switch (functionName) {
                            case "SUBSTRING":
                                functionReplacement = new FunctionNode("SUBSTR", oldNode.getAlias(), true);
                                break;
                            case "LOCATE":
                                functionReplacement = new FunctionNode("INSTR", oldNode.getAlias(), true);
                                for(int i=0; i<=1; i++) {
                                    functionReplacement.addChild(child.getChild(1-i));
                                }
                                parent.replaceChild(index, functionReplacement);
                                return;
                            case "CONCAT":
                                functionReplacement = new ExpressionNode() {
                                    @Override
                                    public void appendChildSeparator(QuotingAppendable builder, int childInd) {
                                        builder.append("||");
                                    }
                                };
                                break;
                            case "CURRENT_TIMESTAMP":
                            case "CURRENT_DATE":
                                functionReplacement = new FunctionNode(functionName, oldNode.getAlias(), false);
                                break;

                            case "CURRENT_TIME":
                                functionReplacement = new FunctionNode("{fn CURTIME()}", oldNode.getAlias(), false);
                                break;

                            case "DAY_OF_YEAR":
                            case "DAY_OF_WEEK":
                            case "WEEK":
                                functionReplacement = new FunctionNode("TO_CHAR", oldNode.getAlias(), true);
                                functionReplacement.addChild(child.getChild(0));
                                if("DAY_OF_YEAR".equals(functionName)) {
                                    functionName = "'DDD'";
                                } else if("DAY_OF_WEEK".equals(functionName)) {
                                    functionName = "'D'";
                                } else {
                                    functionName = "'IW'";
                                }
                                functionReplacement.addChild(new TextNode(functionName));
                                parent.replaceChild(index, functionReplacement);
                                return;

                            case "YEAR":
                            case "MONTH":
                            case "DAY":
                            case "DAY_OF_MONTH":
                            case "HOUR":
                            case "MINUTE":
                            case "SECOND":
                                functionReplacement = new FunctionNode("EXTRACT", oldNode.getAlias(), true) {
                                    @Override
                                    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
                                        builder.append(' ');
                                    }
                                };
                                if("DAY_OF_MONTH".equals(functionName)) {
                                    functionName = "DAY";
                                }
                                functionReplacement.addChild(new TextNode(functionName + " FROM "));
                                break;
                        }

                        if(functionReplacement != null) {
                            for(int i=0; i<child.getChildrenCount(); i++) {
                                functionReplacement.addChild(child.getChild(i));
                            }
                            parent.replaceChild(index, functionReplacement);
                        }
                        break;
                    case LIMIT_OFFSET:
                        LimitOffsetNode limitOffsetNode = (LimitOffsetNode)child;
                        if(limitOffsetNode.getLimit() > 0 || limitOffsetNode.getOffset() > 0) {
                            int max = (limitOffsetNode.getLimit() <= 0)
                                    ? Integer.MAX_VALUE
                                    : limitOffsetNode.getLimit() + limitOffsetNode.getOffset();

                            /*
                            Transform query with limit/offset into following form:
                             SELECT *
                             FROM (
                                SELECT tid.*, rownum rnum
                                FROM (
                                    SELECT fieldA,fieldB
                                    FROM table
                                ) tid
                                WHERE rownum <= OFFSET + LIMIT
                             )
                             WHERE rnum > OFFSET
                             */
                            selectBuilder[0] = select(star())
                                    .from(exp(select(text("tid.*"), text("ROWNUM rnum"))
                                            .from(exp(() -> node).as("tid"))
                                            .where(new ExpressionNodeBuilder(text("ROWNUM")).lte(value(max)))))
                                    .where(new ExpressionNodeBuilder(text("rnum")).gt(value(limitOffsetNode.getOffset())));
                        }
                        parent.replaceChild(index, new EmptyNode());
                        break;
                }
            }

            @Override
            public void onChildNodeEnd(Node parent, Node child, int index, boolean hasMore) {

            }

            @Override
            public void onNodeEnd(Node node) {

            }
        };

        node.visit(visitor);

        if(selectBuilder[0] != null) {
            return selectBuilder[0].build();
        }

        return node;
    }
}
