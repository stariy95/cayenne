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
 * expressions: LIKE, ILIKE, NOT LIKE, NOT ILIKE + ESCAPE
 *
 * @since 4.1
 */
public class LikeNode extends ExpressionNode {

    private final boolean ignoreCase;
    private final boolean not;
    private final char escape;

    public LikeNode(boolean ignoreCase, boolean not, char escape) {
        this.ignoreCase = ignoreCase;
        this.not = not;
        this.escape = escape;
    }

    @Override
    public void appendChildrenStart(QuotingAppendable builder) {
        if(ignoreCase) {
            builder.append("UPPER(");
        }
    }

    @Override
    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
        builder.append(' ');
        if(ignoreCase) {
            builder.append(')');
        }
        if(not) {
            builder.append("NOT ");
        }
        builder.append("LIKE");
        builder.append(' ');
        if(ignoreCase) {
            builder.append("UPPER(");
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable builder) {
        if(ignoreCase) {
            builder.append(')');
        }
        if(escape != 0) {
            builder.append(" ESCAPE '").append(escape).append('\'');
        }
    }
}
