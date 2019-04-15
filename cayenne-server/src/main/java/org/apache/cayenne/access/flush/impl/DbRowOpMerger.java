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

import org.apache.cayenne.access.flush.row.DbRowOp;
import org.apache.cayenne.access.flush.row.DbRowOpVisitor;
import org.apache.cayenne.access.flush.row.DbRowOpWithValues;
import org.apache.cayenne.access.flush.row.DeleteDbRowOp;
import org.apache.cayenne.access.flush.row.DeleteInsertDbRowOp;
import org.apache.cayenne.access.flush.row.InsertDbRowOp;
import org.apache.cayenne.access.flush.row.UpdateDbRowOp;

/**
 * @since 4.2
 */
class DbRowOpMerger implements DbRowOpVisitor<DbRowOp> {

    private final DbRowOp dbRow;

    DbRowOpMerger(DbRowOp dbRow) {
        this.dbRow = dbRow;
    }

    @Override
    public DbRowOp visitInsert(InsertDbRowOp other) {
        if(dbRow instanceof DeleteDbRowOp) {
            return new DeleteInsertDbRowOp((DeleteDbRowOp)dbRow, other);
        }
        return mergeValues((DbRowOpWithValues) dbRow, other);
    }

    @Override
    public DbRowOp visitUpdate(UpdateDbRowOp other) {
        // delete beats update ...
        if(dbRow instanceof DeleteDbRowOp) {
            return dbRow;
        }
        return mergeValues((DbRowOpWithValues) dbRow, other);
    }

    @Override
    public DbRowOp visitDelete(DeleteDbRowOp other) {
        if(dbRow.getChangeId() == other.getChangeId()) {
            return other;
        }
        // clash of Insert/Delete with equal ObjectId
        if(dbRow instanceof InsertDbRowOp) {
            return new DeleteInsertDbRowOp(other, (InsertDbRowOp)dbRow);
        }
        return other;
    }

    private DbRowOp mergeValues(DbRowOpWithValues left, DbRowOpWithValues right) {
        if(right.getChangeId() == right.getObject().getObjectId()) {
            right.getValues().merge(left.getValues());
            return right;
        } else {
            left.getValues().merge(right.getValues());
            return left;
        }
    }
}
