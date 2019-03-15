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

package org.apache.cayenne.access.flush.v3.row;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
public class Qualifier {

    protected final DbRow row;
    // additional qualifier for optimistic lock
    protected Map<DbAttribute, Object> optimisticLockQualifier;
    protected List<String> nullNames;

    protected Qualifier(DbRow row) {
        this.row = row;
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> idSnapshot = row.getChangeId().getIdSnapshot();
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

    public List<DbAttribute> getQualifierAttributes() {
        if(optimisticLockQualifier == null || optimisticLockQualifier.isEmpty()) {
            return row.getEntity().getPrimaryKeys();
        }

        List<DbAttribute> attributes = new ArrayList<>(row.getEntity().getPrimaryKeys());
        attributes.addAll(optimisticLockQualifier.keySet());
        return attributes;
    }

    public Collection<String> getNullQualifierNames() {
        if(nullNames == null || nullNames.isEmpty()) {
            return Collections.emptyList();
        }
        return nullNames;
    }

    public void addOptimisticLockQualifier(DbAttribute dbAttribute, Object value) {
        if(optimisticLockQualifier == null) {
            optimisticLockQualifier = new HashMap<>();
        }

        optimisticLockQualifier.put(dbAttribute, value);
        if(value == null) {
            if(nullNames == null) {
                nullNames = new ArrayList<>();
            }
            nullNames.add(dbAttribute.getName());
        }
    }

    public boolean isUsingOptimisticLocking() {
        return optimisticLockQualifier != null && !optimisticLockQualifier.isEmpty();
    }

    public boolean isSameBatch(Qualifier other) {
        if(optimisticLockQualifier == null) {
            return other.optimisticLockQualifier == null;
        }
        if (!optimisticLockQualifier.values().equals(other.optimisticLockQualifier.values())) {
            return false;
        }
        return Objects.equals(nullNames, other.nullNames);
    }

}
