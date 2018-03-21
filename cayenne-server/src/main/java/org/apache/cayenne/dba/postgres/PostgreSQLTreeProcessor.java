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

package org.apache.cayenne.dba.postgres;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.dba.derby.sqltree.DerbyColumnNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresLimitOffsetNode;

/**
 * @since 4.1
 */
public class PostgreSQLTreeProcessor implements Function<Node, Node> {

    private static final Set<String> EXTRACT_FUNCTION_NAMES = new HashSet<>(Arrays.asList(
            "DAY_OF_MONTH", "DAY", "MONTH", "HOUR", "WEEK", "YEAR", "DAY_OF_WEEK", "DAY_OF_YEAR", "MINUTE", "SECOND"
    ));


    @Override
    public Node apply(Node node) {
        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {

            }

            @Override
            public void onChildNodeStart(Node parent, Node node, int index, boolean hasMore) {
                if(node.getType() == NodeType.LIMIT_OFFSET) {
                    LimitOffsetNode limitOffsetNode = (LimitOffsetNode)node;
                    Node replacement = new PostgresLimitOffsetNode(limitOffsetNode);
                    parent.replaceChild(index, replacement);
                } else if(node.getType() == NodeType.COLUMN) {
                    DerbyColumnNode replacement = new DerbyColumnNode((ColumnNode)node);
                    for(int i=0; i<node.getChildrenCount(); i++) {
                        replacement.addChild(node.getChild(i));
                    }
                    parent.replaceChild(index, replacement);
                } else if(node.getType() == NodeType.FUNCTION) {
                    FunctionNode oldNode = (FunctionNode) node;
                    String functionName = oldNode.getFunctionName();
                    if(EXTRACT_FUNCTION_NAMES.contains(functionName)) {
                        Node replacement = new PostgresExtractFunctionNode(functionName);
                        for(int i=0; i<node.getChildrenCount(); i++) {
                            replacement.addChild(node.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    } else if("CURRENT_DATE".equals(functionName)
                            || "CURRENT_TIME".equals(functionName)
                            || "CURRENT_TIMESTAMP".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode(functionName, oldNode.getAlias(), false);
                        parent.replaceChild(index, replacement);
                    } else if("LOCATE".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode("POSITION", oldNode.getAlias(), true) {
                            @Override
                            public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
                                builder.append(" IN ");
                            }
                        };
                        for(int i=0; i<node.getChildrenCount(); i++) {
                            replacement.addChild(node.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    }
                }
            }

            @Override
            public void onChildNodeEnd(Node parent, Node node, int index, boolean hasMore) {

            }

            @Override
            public void onNodeEnd(Node node) {

            }
        };
        node.visit(visitor);
        return node;
    }

    private static class PostgresExtractFunctionNode extends Node {
        private final String functionName;

        public PostgresExtractFunctionNode(String functionName) {
            this.functionName = functionName;
        }

        @Override
        public void append(QuotingAppendable buffer) {
            buffer.append("EXTRACT(");
            if("DAY_OF_MONTH".equals(functionName)) {
                buffer.append("day");
            } else if("DAY_OF_WEEK".equals(functionName)) {
                buffer.append("dow");
            } else if("DAY_OF_YEAR".equals(functionName)) {
                buffer.append("doy");
            } else {
                buffer.append(functionName);
            }
            buffer.append(" FROM ");
        }

        @Override
        public void appendChildrenEnd(QuotingAppendable builder) {
            builder.append(")");
        }

        @Override
        public Node copy() {
            return new PostgresExtractFunctionNode(functionName);
        }
    }
}
