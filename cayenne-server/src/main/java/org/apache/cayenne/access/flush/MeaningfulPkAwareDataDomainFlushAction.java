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

package org.apache.cayenne.access.flush;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpMerger;
import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * @since 4.2
 */
public class MeaningfulPkAwareDataDomainFlushAction extends DefaultDataDomainFlushAction {

    protected MeaningfulPkAwareDataDomainFlushAction(DataDomain dataDomain, DbRowOpSorter dbRowOpSorter, JdbcEventLogger jdbcEventLogger) {
        super(dataDomain, dbRowOpSorter, jdbcEventLogger);
    }

    protected List<DbRowOp> mergeSameObjectIds(List<DbRowOp> dbRowOps) {
        Map<EffectiveOpId, DbRowOp> index = new HashMap<>(dbRowOps.size());
        // Use EffectiveOpId instead of plain ObjectId to match id's content
        dbRowOps.forEach(row -> index.merge(new EffectiveOpId(row.getChangeId()), row, DbRowOpMerger.INSTANCE));
        dbRowOps.clear();
        dbRowOps.addAll(index.values());
        return dbRowOps;
    }
}
