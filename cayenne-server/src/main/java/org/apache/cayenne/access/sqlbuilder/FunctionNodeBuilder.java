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
public class FunctionNodeBuilder implements ExpressionTrait {

    private final String functionName;

    private final NodeBuilder[] args;

    private String alias;

    public FunctionNodeBuilder(String functionName, NodeBuilder... args) {
        this.functionName = functionName;
        this.args = args;
    }

    public FunctionNodeBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    @Override
    public Node build() {
        Node functionNode = new Node() {
            @Override
            public void append(StringBuilder buffer) {
                buffer.append(functionName);
            }

            @Override
            public void appendChildrenStart(StringBuilder builder) {
                builder.append('(');
            }

            @Override
            public void appendChildrenEnd(StringBuilder builder) {
                builder.append(')');
                if(alias != null) {
                    builder.append(" AS ").append(alias).append(' ');
                }
            }

            @Override
            public void appendChildSeparator(StringBuilder builder) {
                builder.append(',');
            }
        };

        for(NodeBuilder arg : args) {
            functionNode.addChild(arg.build());
        }

        return functionNode;
    }
}
