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
        buffer.append(functionName);
    }

    @Override
    public void appendChildrenStart(QuotingAppendable builder) {
        if (needParentheses) {
            builder.append('(');
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable builder) {
        if (needParentheses) {
            builder.append(')');
        }
        if (alias != null) {
            builder.append(" AS ").appendQuoted(alias).append(' ');
        }
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
}
