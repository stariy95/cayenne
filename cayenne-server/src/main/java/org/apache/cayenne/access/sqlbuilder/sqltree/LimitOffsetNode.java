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
public class LimitOffsetNode extends Node {

    protected int limit;
    protected int offset;

    public LimitOffsetNode() {
    }

    public LimitOffsetNode(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public void append(QuotingAppendable buffer) {
        if(limit == 0 && offset == 0) {
            return;
        }
        buffer.append("LIMIT ").append(limit)
                .append(" OFFSET ").append(offset);
    }

    @Override
    public NodeType getType() {
        return NodeType.LIMIT_OFFSET;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public Node copy() {
        return new LimitOffsetNode(limit, offset);
    }
}