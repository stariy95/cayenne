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

import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.1
 */
public class ExpressionNodeBuilder implements ExpressionTrait {

    private final NodeBuilder left;

    public ExpressionNodeBuilder(NodeBuilder left) {
        this.left = left;
    }

    public ExpressionNodeBuilder and(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "AND"));
    }

    public ExpressionNodeBuilder or(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "OR"));
    }

    @Override
    public ExpressionNodeBuilder plus(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "+"));
    }

    @Override
    public ExpressionNodeBuilder minus(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "-"));
    }

    @Override
    public ExpressionNodeBuilder mul(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "*"));
    }

    @Override
    public ExpressionNodeBuilder div(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "/"));
    }

    @Override
    public ExpressionNodeBuilder eq(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "="));
    }

    @Override
    public ExpressionNodeBuilder lt(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "<"));
    }

    @Override
    public ExpressionNodeBuilder gt(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, ">"));
    }

    @Override
    public ExpressionNodeBuilder lte(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, "<="));
    }

    @Override
    public ExpressionNodeBuilder gte(NodeBuilder operand) {
        return new ExpressionNodeBuilder(new ExpNodeBuilder(operand, ">="));
    }

    public ExpressionNodeBuilder not() {
        return new ExpressionNodeBuilder(() -> {
            Node and = new Node() {
                @Override
                public void append(StringBuilder buffer) {
                    buffer.append("NOT ");
                }
            };
            and.addChild(left.build());
            return and;
        });
    }

    @Override
    public Node build() {
        return left.build();
    }

    private class ExpNodeBuilder implements NodeBuilder {

        private final NodeBuilder operand;

        private final String operation;

        public ExpNodeBuilder(NodeBuilder operand, String operation) {
            this.operand = operand;
            this.operation = operation;
        }

        @Override
        public Node build() {
            Node node = new ExpressionNode();
            node.addChild(left.build())
                    .addChild(new Node() {
                        @Override
                        public void append(StringBuilder buffer) {
                            buffer.append(operation);
                        }
                    })
                    .addChild(operand.build());
            return node;
        }
    }
}
