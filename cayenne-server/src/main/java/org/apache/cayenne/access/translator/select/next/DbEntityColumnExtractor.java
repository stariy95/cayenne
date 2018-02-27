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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.1
 */
class DbEntityColumnExtractor implements ColumnExtractor {

    private final TranslatorContext context;
    private final DbEntity dbEntity;

    DbEntityColumnExtractor(TranslatorContext context) {
        this.context = context;
        this.dbEntity = context.getMetadata().getDbEntity();
    }

    @Override
    public void extract(String prefix) {
        for(DbAttribute attribute : dbEntity.getAttributes()) {
            String path = attribute.getName();
            if(prefix != null) {
                path = prefix + '.' + path;
            }
            String alias = context.getTableTree().aliasForAttributePath(path);
            ColumnDescriptor column = new ColumnDescriptor(attribute, alias);
            column.setDataRowKey(path);
            context.getColumnDescriptors().add(column);
        }
    }
}
