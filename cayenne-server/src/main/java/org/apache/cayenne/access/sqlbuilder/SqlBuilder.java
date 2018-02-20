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

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.1
 */
public final class SqlBuilder {

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

    public static JoinNodeBuilder join(TableNodeBuilder table) {
        return new JoinNodeBuilder("JOIN", table);
    }

    public static JoinNodeBuilder leftJoin(TableNodeBuilder table) {
        return new JoinNodeBuilder("LEFT JOIN", table);
    }

    public static ExpressionNodeBuilder exists(NodeBuilder builder) {
        return new ExpressionNodeBuilder(() -> {
            Node node = new Node() {
                @Override
                public void append(StringBuilder buffer) {
                    buffer.append("EXISTS");
                }
            };
            node.addChild(builder.buildNode());
            return node;
        });
    }

    public static ExpressionNodeBuilder value(Object value) {
        return new ExpressionNodeBuilder(() -> new Node() {
            @Override
            public void append(StringBuilder buffer) {
                if(value instanceof CharSequence) {
                    buffer.append('\'');
                }
                buffer.append(String.valueOf(value));
                if(value instanceof CharSequence) {
                    buffer.append('\'');
                }
            }
        });
    }

    public static NodeBuilder count(NodeBuilder value) {
        return () -> new Node() {
            @Override
            public void append(StringBuilder buffer) {
                buffer.append("COUNT");
            }
        }.addChild(value.buildNode());
    }

    private SqlBuilder() {
    }

}
