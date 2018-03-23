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

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;

/**
 * @since 4.1
 */
public final class SQLBuilder {

    public static SelectBuilder select(NodeBuilder... params) {
        return new SelectBuilder(params);
    }

    public static UpdateBuilder update() {
        return new UpdateBuilder();
    }

    public static DeleteBuilder delete() {
        return new DeleteBuilder();
    }

    public static TableNodeBuilder table(String table) {
        return new TableNodeBuilder(table);
    }

    public static ColumnNodeBuilder column(String column) {
        return new ColumnNodeBuilder(null, column);
    }

    public static JoinNodeBuilder join(NodeBuilder table) {
        return new JoinNodeBuilder("JOIN", table);
    }

    public static JoinNodeBuilder leftJoin(NodeBuilder table) {
        return new JoinNodeBuilder("LEFT JOIN", table);
    }

    public static JoinNodeBuilder rightJoin(NodeBuilder table) {
        return new JoinNodeBuilder("RIGHT JOIN", table);
    }

    public static JoinNodeBuilder innerJoin(NodeBuilder table) {
        return new JoinNodeBuilder("INNER JOIN", table);
    }

    public static JoinNodeBuilder outerJoin(NodeBuilder table) {
        return new JoinNodeBuilder("OUTER JOIN", table);
    }

    public static ExpressionNodeBuilder exists(NodeBuilder builder) {
        return new ExpressionNodeBuilder(new ExistsNodeBuilder(builder));
    }

    public static ValueNodeBuilder value(Object value) {
        return new ValueNodeBuilder(value);
    }

    public static ExpressionNodeBuilder exp(NodeBuilder builder) {
        return new ExpressionNodeBuilder(builder);
    }

    public static NodeBuilder node(Node node) {
        return () -> node;
    }

    public static NodeBuilder aliased(NodeBuilder nodeBuilder, String alias) {
        return () -> {
            Node root = new EmptyNode();
            root.addChild(nodeBuilder.build());
            root.addChild(new TextNode(" " + alias));
            return root;
        };
    }

    public static NodeBuilder aliased(Node node, String alias) {
        if(suppressAlias(node)) {
            return node(node);
        }
        return () -> {
            Node root = new EmptyNode();
            root.addChild(node);
            root.addChild(new TextNode(" " + alias));
            return root;
        };
    }

    public static boolean suppressAlias(Node node) {
        boolean[] suppressAlias = {false};

        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {
                if(node.getType() == NodeType.COLUMN && ((ColumnNode) node).getAlias() != null) {
                    suppressAlias[0] = true;
                } else if(node.getType() == NodeType.FUNCTION && ((FunctionNode) node).getAlias() != null) {
                    suppressAlias[0] = true;
                }
            }

            @Override
            public void onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
            }

            @Override
            public void onChildNodeEnd(Node parent, Node child, int index, boolean hasMore) {
            }

            @Override
            public void onNodeEnd(Node node) {
            }
        };
        node.visit(visitor);
        return suppressAlias[0];
    }

    public static NodeBuilder text(String text) {
        return () -> new TextNode(text);
    }

    public static NodeBuilder all() {
        return text("*");
    }

    public static ExpressionNodeBuilder not(NodeBuilder value) {
        return new ExpressionNodeBuilder(value).not();
    }

    public static FunctionNodeBuilder count(NodeBuilder value) {
        return function("COUNT", value);
    }

    public static FunctionNodeBuilder count() {
        return function("COUNT", column("*"));
    }

    public static FunctionNodeBuilder avg(NodeBuilder value) {
        return function("AVG", value);
    }

    public static FunctionNodeBuilder min(NodeBuilder value) {
        return function("MIN", value);
    }

    public static FunctionNodeBuilder max(NodeBuilder value) {
        return function("MAX", value);
    }

    public static FunctionNodeBuilder function(String function, NodeBuilder... values) {
        return new FunctionNodeBuilder(function, values);
    }

    public static OrderingNodeBuilder order(NodeBuilder expression) {
        return new OrderingNodeBuilder(expression);
    }

    private SQLBuilder() {
    }
}
