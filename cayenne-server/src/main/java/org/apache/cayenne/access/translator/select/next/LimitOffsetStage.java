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

/**
 * @since 4.1
 */
class LimitOffsetStage extends TranslationStage {
    LimitOffsetStage(TranslatorContext context) {
        super(context);
    }

    @Override
    void perform() {
        int offset = context.getMetadata().getFetchOffset();
        int limit = context.getMetadata().getFetchLimit();

        if (offset > 0 || limit > 0) {
            // both OFFSET and LIMIT must be present, so come up with defaults
            // if one of them is not set by the user
            if (limit == 0) {
                limit = Integer.MAX_VALUE;
            }

            context.getSelectBuilder().limit(limit).offset(offset);
        }
    }
}
