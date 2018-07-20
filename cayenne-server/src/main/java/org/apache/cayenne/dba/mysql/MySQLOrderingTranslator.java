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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.translator.select.OrderingTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.query.Ordering;

/**
 * Ordering translator for MySQL database
 * <p>
 * Supports NULLS FIRST and NULLS LAST functionality via ordering inversion syntax.
 * <p>
 * E.g.: "ORDER BY column" implies ascending order with nulls first,
 * while "ORDER BY -column DESC" will be ascending order with nulls last.
 *
 * @since 4.2
 */
public class MySQLOrderingTranslator extends OrderingTranslator {

    public MySQLOrderingTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler, true);
    }

    protected void appendColumn(Ordering ordering, String columnSQL) {
        if(ordering.isAscending() != ordering.isNullSortedFirst()) {
            mainBuffer.append('-');
        }
        mainBuffer.append(columnSQL);
        orderByColumnList.add(columnSQL);
    }

    protected void appendDirection(Ordering ordering) {
        // DESC will be for NOT ASC and NULLS FIRST, or for ASC and NOT NULLS FIRST
        if (!ordering.isNullSortedFirst()) {
            mainBuffer.append(" DESC");
        }
    }

    protected void appendNullsOrdering(Ordering ordering) {
        // do nothing here
    }
}
