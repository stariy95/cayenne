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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class DeleteDiffSnapshot extends DiffSnapshot {

    Map<DbAttribute, Object> optimisticLockQualifier;   // additional qualifier for optimistic lock

    DeleteDiffSnapshot(Persistent object, DbEntity entity) {
        super(object, entity);
    }

    @Override
    public <T> T accept(DiffSnapshotVisitor<T> visitor) {
        return visitor.visitDelete(this);
    }

    protected Map<String, Object> getQualifier() {
        Map<String, Object> idSnapshot = changeId.getIdSnapshot();
        if(optimisticLockQualifier == null || optimisticLockQualifier.isEmpty()) {
            return idSnapshot;
        }

        Map<String, Object> qualifier = new HashMap<>(optimisticLockQualifier.size() + idSnapshot.size());
        qualifier.putAll(idSnapshot);
        optimisticLockQualifier.forEach((attr, value) -> {
            qualifier.put(attr.getName(), value);
        });

        return qualifier;
    }

    protected List<DbAttribute> getQualifierAttributes() {
        List<DbAttribute> attributes = new ArrayList<>(entity.getPrimaryKeys());
        if(optimisticLockQualifier == null || optimisticLockQualifier.isEmpty()) {
            return attributes;
        }

        attributes.addAll(optimisticLockQualifier.keySet());
        return attributes;
    }

    protected void addOptimisticLockQualifier(DbAttribute dbAttribute, Object value) {
        if(optimisticLockQualifier == null) {
            optimisticLockQualifier = new HashMap<>();
        }

        optimisticLockQualifier.put(dbAttribute, value);
    }

    protected Collection<String> getNullQualifierNames() {
        if(optimisticLockQualifier == null || optimisticLockQualifier.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> nullNames = new ArrayList<>(optimisticLockQualifier.size() / 2);
        optimisticLockQualifier.forEach((attr, value) -> {
            if(value == null) {
                nullNames.add(attr.getName());
            }
        });

        return nullNames;
    }
}
