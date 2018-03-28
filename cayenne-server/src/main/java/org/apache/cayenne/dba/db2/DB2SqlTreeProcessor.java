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

package org.apache.cayenne.dba.db2;

import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.dba.derby.sqltree.DerbyColumnNode;
import org.apache.cayenne.dba.derby.sqltree.DerbyValueNode;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLimitOffsetNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresLimitOffsetNode;

/**
 * @since 4.1
 */
public class DB2SqlTreeProcessor implements Function<Node, Node> {
    @Override
    public Node apply(Node node) {
        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {

            }

            @Override
            public void onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
                if(child.getType() == NodeType.LIMIT_OFFSET) {
                    LimitOffsetNode limitOffsetNode = (LimitOffsetNode)child;
                    Node replacement = new MysqlLimitOffsetNode(limitOffsetNode.getLimit(), limitOffsetNode.getOffset());
                    parent.replaceChild(index, replacement);
                } else if(child.getType() == NodeType.COLUMN) {
                    DerbyColumnNode replacement = new DerbyColumnNode((ColumnNode)child);
                    for(int i=0; i<child.getChildrenCount(); i++) {
                        replacement.addChild(child.getChild(i));
                    }
                    parent.replaceChild(index, replacement);
                } else if(child.getType() == NodeType.VALUE) {
                    ValueNode valueNode = (ValueNode)child;
                    Node replacement = new DerbyValueNode(valueNode.getValue(), valueNode.getAttribute());
                    parent.replaceChild(index, replacement);
                } else if(child.getType() == NodeType.FUNCTION) {
                    FunctionNode oldNode = (FunctionNode) child;
                    String functionName = oldNode.getFunctionName();
                    if ("SUBSTRING".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode("SUBSTR", oldNode.getAlias(), true);
                        for (int i = 0; i < child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    } else if ("DAY_OF_MONTH".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode("DAY", oldNode.getAlias(), true);
                        for (int i = 0; i < child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    } else if ("DAY_OF_WEEK".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode("DAYOFWEEK", oldNode.getAlias(), true);
                        for (int i = 0; i < child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    } else if ("DAY_OF_YEAR".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode("DAYOFYEAR", oldNode.getAlias(), true);
                        for (int i = 0; i < child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    } else if ("CURRENT_DATE".equals(functionName)
                            || "CURRENT_TIME".equals(functionName)
                            || "CURRENT_TIMESTAMP".equals(functionName)) {
                        FunctionNode replacement = new FunctionNode(functionName, oldNode.getAlias(), false);
                        parent.replaceChild(index, replacement);
                    } else if ("CONCAT".equals(functionName)) {
                        Node replacement = new ExpressionNode() {
                            @Override
                            public void appendChildSeparator(QuotingAppendable builder, int childInd) {
                                builder.append(" || ");
                            }
                        };
                        for (int i = 0; i < child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                    }
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

        return node;
    }
}
