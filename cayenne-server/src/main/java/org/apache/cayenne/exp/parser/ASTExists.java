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

package org.apache.cayenne.exp.parser;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.FluentSelect;

/**
 * @since 4.1
 */
public class ASTExists extends ConditionNode {

    private FluentSelect<?> query;

    public ASTExists() {
        super(-1);
    }

    public ASTExists(FluentSelect<?> query) {
        this();
        this.query = query;
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 1;
    }

    @Override
    public int getOperandCount() {
        return 1;
    }

    @Override
    public Object getOperand(int index) {
        return query;
    }

    @Override
    protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return null;
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "EXISTS";
    }

    @Override
    public Expression shallowCopy() {
        return new ASTExists();
    }

    @Override
    public int getType() {
        return Expression.EXISTS;
    }
}
