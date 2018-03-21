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

package org.apache.cayenne.dba.derby.sqltree;

import java.sql.Types;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;

/**
 * @since 4.1
 */
public class DerbyColumnNode extends Node {

    ColumnNode columnNode ;

    public DerbyColumnNode(ColumnNode columnNode) {
        this.columnNode = columnNode;
    }

    @Override
    public void append(QuotingAppendable buffer) {
        boolean isResult = isResultNode();
        if(columnNode.getAlias() == null || isResult) {
            if(columnNode.getAttribute() != null
                    && columnNode.getAttribute().getType() == Types.CHAR) {
                buffer.append("RTRIM(");
                appendColumnNode(buffer);
                buffer.append(")");
                appendAlias(buffer, isResult);
            } else if(getParent().getType() == NodeType.EQUALITY
                    && columnNode.getAttribute() != null
                    && columnNode.getAttribute().getType() == Types.CLOB) {
                buffer.append("CAST(");
                appendColumnNode(buffer);
                buffer.append(" AS VARCHAR(").append(getColumnSize()).append("))");
                appendAlias(buffer, isResult);
            } else {
                columnNode.append(buffer);
            }
        } else {
            appendAlias(buffer, false);
        }
    }

    protected boolean isResultNode() {
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == NodeType.RESULT) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    protected void appendColumnNode(QuotingAppendable buffer) {
        if (columnNode.getTable() != null) {
            buffer.appendQuoted(columnNode.getTable()).append('.');
        }
        buffer.appendQuoted(columnNode.getColumn());
    }

    protected void appendAlias(QuotingAppendable buffer, boolean isResult) {
        if(!isResult) {
            return;
        }
        if (columnNode.getAlias() != null) {
            buffer.append(' ').appendQuoted(columnNode.getAlias());
        }
    }

    protected int getColumnSize() {
        int size = columnNode.getAttribute().getMaxLength();
        if(size > 0) {
            return size;
        }

        int siblings = getParent().getChildrenCount();
        for(int i=0; i<siblings; i++) {
            Node sibling = getParent().getChild(i);
            if(sibling == this) {
                continue;
            }
            if(sibling.getType() == NodeType.VALUE) {
                if(((ValueNode)sibling).getValue() instanceof CharSequence) {
                    return ((CharSequence) ((ValueNode)sibling).getValue()).length();
                }
            }
        }

        return 255;
    }
}
