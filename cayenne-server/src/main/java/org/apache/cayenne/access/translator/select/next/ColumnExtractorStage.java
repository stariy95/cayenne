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
class ColumnExtractorStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        ColumnExtractor extractor;

        context.getTableTree().addRootTable(context.getMetadata().getDbEntity());

        if(context.getQuery().getColumns() != null && !context.getQuery().getColumns().isEmpty()) {
            extractor = new CustomColumnSetExtractor(context, context.getQuery().getColumns());
        } else if (context.getMetadata().getClassDescriptor() != null) {
            extractor = new DescriptorColumnExtractor(context, context.getMetadata().getClassDescriptor());
        } else if (context.getMetadata().getPageSize() > 0) {
            extractor = new IdColumnExtractor(context, context.getMetadata().getObjEntity());
        } else {
            extractor = new DbEntityColumnExtractor(context);
        }

        extractor.extract(null);
    }
}
