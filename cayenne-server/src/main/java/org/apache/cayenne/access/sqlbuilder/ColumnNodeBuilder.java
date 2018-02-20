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

import java.util.Objects;

import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.1
 */
public class ColumnNodeBuilder implements NodeBuilder {

    private final String table;

    private final String field;

    private String alias;

    public ColumnNodeBuilder(String table, String field) {
        this.table = table;
        this.field = Objects.requireNonNull(field);
    }

    public ColumnNodeBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    public ExpressionNodeBuilder eq(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).eq(nodeBuilder);
    }

    public ExpressionNodeBuilder gt(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).gt(nodeBuilder);
    }

    public ExpressionNodeBuilder lt(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).lt(nodeBuilder);
    }


    public NodeBuilder desc() {
        return () -> {
            Node node = new EmptyNode();
            node.addChild(ColumnNodeBuilder.this.buildNode());
            node.addChild(new Node() {
                @Override
                public void append(StringBuilder buffer) {
                    buffer.append("DESC");
                }
            });
            return node;
        };
    }

    public NodeBuilder asc() {
        return () -> {
            Node node = new EmptyNode();
            node.addChild(ColumnNodeBuilder.this.buildNode());
            node.addChild(new Node() {
                @Override
                public void append(StringBuilder buffer) {
                    buffer.append("ASC");
                }
            });
            return node;
        };
    }

    @Override
    public Node buildNode() {
        return new Node() {
            @Override
            public void append(StringBuilder buffer) {
                if(table != null) {
                    buffer.append(table).append('.');
                }
                buffer.append(field);
                if(alias != null) {
                    buffer.append(" AS ").append(alias);
                }
            }
        };
    }
}
