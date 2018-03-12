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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.access.translator.select.next.QuotingAppendable;

/**
 * @since 4.1
 */
public class UnescapedColumnNode extends ColumnNode {

    public UnescapedColumnNode(String table, String column, String alias) {
        super(table, column, alias);
    }

    @Override
    public void append(QuotingAppendable buffer) {
        if (table != null) {
            buffer.append(table).append('.');
        }
        buffer.append(column);
        if (alias != null) {
            buffer.append(' ').appendQuoted(alias);
        }
    }

    @Override
    public NodeType getType() {
        return NodeType.COLUMN;
    }
}
