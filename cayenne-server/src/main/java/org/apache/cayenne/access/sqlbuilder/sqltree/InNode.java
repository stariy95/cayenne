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
public class InNode extends Node {

    private final boolean not;

    public InNode(boolean not) {
        this.not = not;
    }

    @Override
    public void append(QuotingAppendable buffer) {
    }

    @Override
    public void appendChildSeparator(QuotingAppendable builder, int childInd) {
        if (childInd == 0) {
            builder.append(' ');
            if (not) {
                builder.append("NOT ");
            }
            builder.append("IN (");
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable builder) {
        builder.append(')');
    }

    @Override
    public Node copy() {
        return new InNode(not);
    }

    public boolean isNot() {
        return not;
    }

    @Override
    public NodeType getType() {
        return NodeType.IN;
    }
}
