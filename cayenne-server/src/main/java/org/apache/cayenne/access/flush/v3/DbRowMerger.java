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

package org.apache.cayenne.access.flush.v3;

import org.apache.cayenne.access.flush.v3.row.DbRow;
import org.apache.cayenne.access.flush.v3.row.DbRowVisitor;
import org.apache.cayenne.access.flush.v3.row.DbRowWithValues;
import org.apache.cayenne.access.flush.v3.row.DeleteDbRow;
import org.apache.cayenne.access.flush.v3.row.InsertDbRow;
import org.apache.cayenne.access.flush.v3.row.UpdateDbRow;

/**
 * @since 4.2
 */
class DbRowMerger implements DbRowVisitor<DbRow> {

    private final DbRow dbRow;

    DbRowMerger(DbRow dbRow) {
        this.dbRow = dbRow;
    }

    @Override
    public DbRow visitInsert(InsertDbRow other) {
        return mergeValues((DbRowWithValues) dbRow, other);
    }

    @Override
    public DbRow visitUpdate(UpdateDbRow other) {
        // delete beats update ...
        if(dbRow instanceof DeleteDbRow) {
            return dbRow;
        }
        return mergeValues((DbRowWithValues) dbRow, other);
    }

    private DbRow mergeValues(DbRowWithValues left, DbRowWithValues right) {
        left.getValues().merge(right.getValues());
        return left;
    }
}
