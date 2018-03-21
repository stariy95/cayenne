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
public class FunctionNode extends Node {

    private final String functionName;
    private final String alias;
    private final boolean needParentheses;

    public FunctionNode(String functionName, String alias, boolean needParentheses) {
        this.functionName = functionName;
        this.alias = alias;
        this.needParentheses = needParentheses;
    }

    @Override
    public void append(QuotingAppendable buffer) {
        if(alias != null && !isResultNode()) {
            buffer.append(alias);
        } else {
            buffer.append(functionName);
        }
    }

    @Override
    public void visit(NodeTreeVisitor visitor) {
        if(alias != null && !isResultNode()) {
            visitor.onNodeStart(this);
            visitor.onNodeEnd(this);
            return;
        }
        super.visit(visitor);
    }

    @Override
    public void appendChildrenStart(QuotingAppendable builder) {
        if (needParentheses && (alias == null || isResultNode())) {
            builder.append('(');
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable builder) {
        if (needParentheses && (alias == null || isResultNode())) {
            builder.append(')');
        }
        if (alias != null && isResultNode()) {
            builder.append(" AS ").appendQuoted(alias).append(' ');
        }
    }

    protected boolean isResultNode() {
        // TODO: this check is broken for now, as we have same nodes with same parent for result and group, order, qualifier...
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == NodeType.RESULT) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    @Override
    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
        builder.append(',');
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public NodeType getType() {
        return NodeType.FUNCTION;
    }

    @Override
    public Node copy() {
        return new FunctionNode(functionName, alias, needParentheses);
    }
}
