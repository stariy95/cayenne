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

import java.sql.Types;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;

/**
 * @since 4.2
 */
public class TrimmingColumnNode extends Node {

    protected final ColumnNode columnNode ;

    public TrimmingColumnNode(ColumnNode columnNode) {
        this.columnNode = columnNode;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        boolean isResult = isResultNode();
        if(columnNode.getAlias() == null || isResult) {
            if(isCharType() && isAllowedForTrimming()) {
                appendRtrim(buffer);
                appendAlias(buffer, isResult);
            } else if(isComparisionWithClob()) {
                appendClobColumnNode(buffer);
                appendAlias(buffer, isResult);
            } else {
                columnNode.append(buffer);
            }
        } else {
            appendAlias(buffer, false);
        }

        return buffer;
    }

    private boolean isComparisionWithClob() {
        return (getParent().getType() == NodeType.EQUALITY
                || getParent().getType() == NodeType.LIKE)
                && columnNode.getAttribute() != null
                && columnNode.getAttribute().getType() == Types.CLOB;
    }

    protected void appendRtrim(QuotingAppendable buffer) {
        buffer.append(" RTRIM(");
        appendColumnNode(buffer);
        buffer.append(")");
    }

    private boolean isCharType() {
        return columnNode.getAttribute() != null
                && columnNode.getAttribute().getType() == Types.CHAR;
    }

    protected boolean isAllowedForTrimming() {
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == NodeType.JOIN || parent.getType() == NodeType.FUNCTION) {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
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

    protected void appendClobColumnNode(QuotingAppendable buffer) {
        buffer.append(" CAST(");
        appendColumnNode(buffer);
        buffer.append(" AS VARCHAR(").append(getColumnSize()).append("))");
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

    @Override
    public Node copy() {
        return new TrimmingColumnNode(columnNode.deepCopy());
    }
}
