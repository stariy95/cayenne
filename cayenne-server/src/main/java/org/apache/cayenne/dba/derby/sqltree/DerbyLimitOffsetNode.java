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

import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;

/**
 * @since 4.1
 */
public class DerbyLimitOffsetNode extends LimitOffsetNode {

    public DerbyLimitOffsetNode(LimitOffsetNode node) {
        super(node.getLimit(), node.getOffset());
    }

    @Override
    public void append(QuotingAppendable buffer) {
        // using JDBC escape syntax
//        if(offset > 0 || limit > 0) {
//            buffer.append("{ ");
//            buffer.append("LIMIT ").append(limit).append(' ');
//            if(offset > 0) {
//                buffer.append("OFFSET ").append(offset);
//            }
//            buffer.append(" }");
//        }

        // OFFSET X ROWS FETCH NEXT Y ROWS ONLY
        if(offset > 0) {
            buffer.append("OFFSET ").append(offset).append(" ROWS ");
        }
        if(limit > 0) {
            buffer.append("FETCH NEXT ").append(limit).append(" ROWS ONLY");
        }
    }
}
