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

import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.access.flush.row.DeleteDbRow;
import org.apache.cayenne.access.flush.row.InsertDbRow;
import org.apache.cayenne.access.flush.row.UpdateDbRow;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * @since 4.2
 */
class QueryCreatorVisitor implements DbRowVisitor<BatchQuery> {

    @Override
    public BatchQuery visitInsert(InsertDbRow dbRow) {
        // TODO: pass snapshot as argument directly to batch...
        InsertBatchQuery query = new InsertBatchQuery(dbRow.getEntity(), 1);
        query.add(dbRow.getValues().getSnapshot(), dbRow.getChangeId());
        return query;
    }

    @Override
    public BatchQuery visitUpdate(UpdateDbRow dbRow) {
        // skip empty update..
        if(dbRow.getValues().isEmpty()) {
            return null;
        }
        // TODO: pass snapshot as argument directly to batch...
        UpdateBatchQuery query = new UpdateBatchQuery(
                dbRow.getEntity(),
                dbRow.getQualifier().getQualifierAttributes(),
                dbRow.getValues().getUpdatedAttributes(),
                dbRow.getQualifier().getNullQualifierNames(),
                1
        );
        query.setUsingOptimisticLocking(dbRow.getQualifier().isUsingOptimisticLocking());
        query.add(dbRow.getQualifier().getSnapshot(), dbRow.getValues().getSnapshot(), dbRow.getChangeId());
        return query;
    }

    @Override
    public BatchQuery visitDelete(DeleteDbRow dbRow) {
        DeleteBatchQuery query = new DeleteBatchQuery(
                dbRow.getEntity(),
                dbRow.getQualifier().getQualifierAttributes(),
                dbRow.getQualifier().getNullQualifierNames(),
                1
        );
        query.setUsingOptimisticLocking(dbRow.getQualifier().isUsingOptimisticLocking());
        query.add(dbRow.getQualifier().getSnapshot());
        return query;
    }
}
