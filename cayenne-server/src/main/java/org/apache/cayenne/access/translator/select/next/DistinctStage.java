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

package org.apache.cayenne.access.translator.select.next;

import java.sql.Types;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;

/**
 * @since 4.1
 */
public class DistinctStage implements TranslationStage {

    protected static final int[] UNSUPPORTED_DISTINCT_TYPES = { Types.BLOB, Types.CLOB, Types.NCLOB,
            Types.LONGVARBINARY, Types.LONGVARCHAR, Types.LONGNVARCHAR };

    protected static boolean isUnsupportedForDistinct(int type) {
        for (int unsupportedDistinctType : UNSUPPORTED_DISTINCT_TYPES) {
            if (unsupportedDistinctType == type) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void perform(TranslatorContext context) {
        // explicit suppressing of distinct
        if(context.getMetadata().isSuppressingDistinct()) {
            context.setDistinctSuppression(true);
            return;
        }

        // query forcing distinct or query have joins (qualifier or prefetch)
        if(context.getQuery().isDistinct() || context.getTableTree().getNodeCount() > 1) {
            // unsuitable jdbc type for distinct clause
            for(ColumnDescriptor columnDescriptor : context.getColumnDescriptors()) {
                if(isUnsupportedForDistinct(columnDescriptor.getJdbcType())) {
                    context.setDistinctSuppression(true);
                    return;
                }
            }
            context.getSelectBuilder().distinct();
        }
    }
}
