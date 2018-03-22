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

package org.apache.cayenne.dba.sqlserver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TopNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.dba.derby.sqltree.DerbyColumnNode;

/**
 * @since 4.1
 */
public class SQLServerTreeProcessor implements Function<Node, Node> {

    @Override
    public Node apply(Node node) {

        AtomicInteger limit = new AtomicInteger();

        NodeTreeVisitor visitor = new NodeTreeVisitor() {

            @Override
            public void onNodeStart(Node node) {

            }

            @Override
            public void onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
                if(child.getType() == NodeType.COLUMN) {
                    SQLServerColumnNode replacement = new SQLServerColumnNode((ColumnNode)child);
                    for(int i=0; i<child.getChildrenCount(); i++) {
                        replacement.addChild(child.getChild(i));
                    }
                    parent.replaceChild(index, replacement);
                } else if(child.getType() == NodeType.LIMIT_OFFSET) {
                    LimitOffsetNode limitNode = (LimitOffsetNode)child;
                    if(limitNode.getLimit() > 0) {
                        limit.set(limitNode.getLimit() + limitNode.getOffset());
                    }
                    parent.replaceChild(index, new EmptyNode());
                } else if(child.getType() == NodeType.FUNCTION) {
                    FunctionNode oldNode = (FunctionNode) child;
                    String functionName = oldNode.getFunctionName();
                    Node replacement = null;
                    switch (functionName) {
                        case "LENGTH":
                            replacement = new FunctionNode("LEN", oldNode.getAlias(), true);
                            break;
                        case "LOCATE":
                            replacement = new FunctionNode("CHARINDEX", oldNode.getAlias(), true);
                            break;
                        case "MOD":
                            replacement = new ExpressionNode() {
                                @Override
                                public void appendChildSeparator(QuotingAppendable builder, int childInd) {
                                    builder.append('%');
                                }
                            };
                            break;
                        case "TRIM":
                            Node rtrim = new FunctionNode("RTRIM", null, true);
                            replacement = new FunctionNode("LTRIM", oldNode.getAlias(), true);
                            for(int i=0; i<child.getChildrenCount(); i++) {
                                rtrim.addChild(child.getChild(i));
                            }
                            replacement.addChild(rtrim);
                            parent.replaceChild(index, replacement);
                            return;
                        case "CURRENT_DATE":
                            replacement = new FunctionNode("{fn CURDATE()}", oldNode.getAlias(), false);
                            break;
                        case "CURRENT_TIME":
                            replacement = new FunctionNode("{fn CURTIME()}", oldNode.getAlias(), false);
                            break;
                        case "CURRENT_TIMESTAMP":
                            replacement = new FunctionNode("CURRENT_TIMESTAMP", oldNode.getAlias(), false);
                            break;

                        case "YEAR":
                        case "MONTH":
                        case "WEEK":
                        case "DAY_OF_YEAR":
                        case "DAY":
                        case "DAY_OF_MONTH":
                        case "DAY_OF_WEEK":
                        case "HOUR":
                        case "MINUTE":
                        case "SECOND":
                            replacement = new FunctionNode("DATEPART", oldNode.getAlias(), true);
                            if("DAY_OF_MONTH".equals(functionName)) {
                                functionName = "DAY";
                            } else if("DAY_OF_WEEK".equals(functionName)) {
                                functionName = "WEEKDAY";
                            } else if("DAY_OF_YEAR".equals(functionName)) {
                                functionName = "DAYOFYEAR";
                            }
                            replacement.addChild(new TextNode(functionName));
                            break;
                    }

                    if(replacement != null) {
                        for(int i=0; i<child.getChildrenCount(); i++) {
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
        if(limit.get() > 0) {
            // "SELECT DISTINCT TOP N" vs "SELECT TOP N"
            int idx = 0;
            if(node.getChild(0).getType() == NodeType.DISTINCT) {
                idx = 1;
            }
            node.addChild(idx, new TopNode(limit.get()));
        }


        return node;
    }
}
