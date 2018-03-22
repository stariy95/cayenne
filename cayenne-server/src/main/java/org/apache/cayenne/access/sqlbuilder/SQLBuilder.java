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

    public static ExpNodeBuilder exp(NodeBuilder builder) {
        return new ExpNodeBuilder(builder);
    }

    public static Node aliased(Node node, String alias) {
//        if(node instanceof EmptyNode) {
//            return node;
//        }
        if(node.getType() == NodeType.COLUMN) {
            if(((ColumnNode)node).getAlias() != null) {
                return node;
            }
        } else if(node.getType() == NodeType.FUNCTION) {
            if(((FunctionNode)node).getAlias() != null) {
                return node;
            }
        }
        Node root = new EmptyNode();
        root.addChild(node);
        root.addChild(new TextNode("AS " + alias));
        return root;
    }

    public static NodeBuilder text(String text) {
        return () -> new TextNode(text);
    }

    public static NodeBuilder star() {
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

    public static class ExpNodeBuilder implements NodeBuilder {

        private final NodeBuilder arg;
        private String alias;

        public ExpNodeBuilder(NodeBuilder arg) {
            this.arg = arg;
        }

        public ExpNodeBuilder as(String alias) {
            this.alias = alias;
            return this;
        }

        @Override
        public Node build() {
            Node node = new EmptyNode();
            Node exp = new ExpressionNode();
            exp.addChild(arg.build());
            node.addChild(exp);
            if(alias != null) {
                node.addChild(new TextNode(" " + alias));
            }
            return node;
        }
    }
}
