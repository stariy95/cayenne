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

import java.util.Map;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class UpdateDiffSnapshot extends DiffSnapshot {

    Map<DbAttribute, Object> values; // values to store to DB (new or updated)
    Map<DbAttribute, Object> optimisticLockQualifier;   // additional qualifier for optimistic lock

    UpdateDiffSnapshot(DbEntity entity) {
        super(entity);
    }

    @Override
    <T> T accept(DiffVisitor<T> visitor) {
        return visitor.visitUpdate(this);
    }
}
