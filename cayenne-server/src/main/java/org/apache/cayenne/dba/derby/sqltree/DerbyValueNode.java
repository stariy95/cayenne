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

import java.sql.Types;

import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.1
 */
public class DerbyValueNode extends ValueNode {

    public DerbyValueNode(Object value, DbAttribute attribute) {
        super(value, attribute);
    }

    protected void appendStringValue(QuotingAppendable buffer, CharSequence value) {
        if(getAttribute() != null && getAttribute().getType() == Types.CLOB) {
            buffer.append("CAST(? AS VARCHAR(").append(value.length()).append("))");
        } else {
            buffer.append('?');
        }
        addValueBinding(buffer, value);
    }

}
