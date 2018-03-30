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

package org.apache.cayenne.dba.firebird;

import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;

/**
 * SUBSTRING function for Firebird
 *
 * It has following format:
 *
 * SUBSTRING (string FROM CAST(? AS INTEGER) FOR CAST(? AS INTEGER))
 *
 * @since 4.1
 */
class FirebirdSubstringFunctionNode extends FunctionNode {
    public FirebirdSubstringFunctionNode(String alias) {
        super("SUBSTRING", alias);
    }

    @Override
    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
        if(childIdx == 0) {
            builder.append(" FROM CAST(");
        } else if(childIdx == 1) {
            builder.append(" AS INTEGER) FOR CAST(");
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable builder) {
        if(getAlias() == null || isResultNode()) {
            builder.append(" AS INTEGER)");
        }
        super.appendChildrenEnd(builder);
    }

    @Override
    public Node copy() {
        return new FirebirdSubstringFunctionNode(getAlias());
    }
}
