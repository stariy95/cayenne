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

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.1
 */
public class ColumnNodeBuilder implements ExpressionTrait {

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

    public NodeBuilder desc() {
        return new OrderingNodeBuilder(this, "DESC");
    }

    public NodeBuilder asc() {
        return new OrderingNodeBuilder(this, "ASC");
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
