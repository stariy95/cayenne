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

package org.apache.cayenne.access.flush.impl;

import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.access.flush.row.DbRowWithValues;
import org.apache.cayenne.access.flush.row.DeleteDbRow;
import org.apache.cayenne.access.flush.row.InsertDbRow;
import org.apache.cayenne.access.flush.row.UpdateDbRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.2
 */
class DbRowMerger implements DbRowVisitor<DbRow> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRowFactory.class);

    private final DbRow dbRow;

    DbRowMerger(DbRow dbRow) {
        this.dbRow = dbRow;
    }

    @Override
    public DbRow visitInsert(InsertDbRow other) {
        LOGGER.info("Merge " + dbRow + " with " + other);
        return mergeValues((DbRowWithValues) dbRow, other);
    }

    @Override
    public DbRow visitUpdate(UpdateDbRow other) {
        // delete beats update ...
        if(dbRow instanceof DeleteDbRow) {
            LOGGER.info("Replace " + other + " with " + dbRow);
            return dbRow;
        }
        LOGGER.info("Merge " + dbRow + " with " + other);
        return mergeValues((DbRowWithValues) dbRow, other);
    }

    private DbRow mergeValues(DbRowWithValues left, DbRowWithValues right) {
        left.getValues().merge(right.getValues());
        return left;
    }
}
