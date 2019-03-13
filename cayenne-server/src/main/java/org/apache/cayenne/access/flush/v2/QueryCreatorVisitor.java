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

package org.apache.cayenne.access.flush.v2;

import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * @since 4.2
 */
class QueryCreatorVisitor implements DiffSnapshotVisitor<BatchQuery> {

    @Override
    public BatchQuery visitInsert(InsertDiffSnapshot diffSnapshot) {
        // TODO: pass snapshot as argument directly to batch...
        InsertBatchQuery query = new InsertBatchQuery(diffSnapshot.getEntity(), 1);
        query.add(diffSnapshot.getSnapshot());
        return query;
    }

    @Override
    public BatchQuery visitUpdate(UpdateDiffSnapshot diffSnapshot) {
        // TODO: pass snapshot as argument directly to batch...
        UpdateBatchQuery query = new UpdateBatchQuery(
                diffSnapshot.getEntity(),
                diffSnapshot.getQualifierAttributes(),
                diffSnapshot.getUpdatedAttributes(),
                diffSnapshot.getNullQualifierNames(),
                1
        );
        query.setUsingOptimisticLocking(diffSnapshot.isUsingOptimisticLocking());
        query.add(diffSnapshot.getQualifier(), diffSnapshot.getSnapshot(), diffSnapshot.getChangeId());
        return query;
    }

    @Override
    public BatchQuery visitDelete(DeleteDiffSnapshot diffSnapshot) {
        DeleteBatchQuery query = new DeleteBatchQuery(
                diffSnapshot.getEntity(),
                diffSnapshot.getQualifierAttributes(),
                diffSnapshot.getNullQualifierNames(),
                1
        );
        query.add(diffSnapshot.getQualifier());
        return query;
    }
}
