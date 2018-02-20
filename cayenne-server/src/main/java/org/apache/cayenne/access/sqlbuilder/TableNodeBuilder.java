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
public class TableNodeBuilder implements NodeBuilder {

    private final String tableName;

    private String alias;

    public TableNodeBuilder(String tableName) {
        this.tableName = tableName;
    }

    public TableNodeBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    public ColumnNodeBuilder column(String column) {
        return new ColumnNodeBuilder(tableName, column);
    }

    public String getAlias() {
        return alias;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public Node buildNode() {
        return new Node() {
            @Override
            public void append(StringBuilder buffer) {
                buffer.append(tableName);
                if(alias != null) {
                    buffer.append(" AS ").append(alias);
                }
            }
        };
    }
}
