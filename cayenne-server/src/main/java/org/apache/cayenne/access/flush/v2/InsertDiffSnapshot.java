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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class InsertDiffSnapshot extends DiffSnapshot implements SnapshotWithValues {

    // new values to store to DB
    protected Map<String, Object> values;
    // generated flattened Ids for this insert
    protected Map<String, ObjectId> flattenedIds;

    protected InsertDiffSnapshot(Persistent object, DbEntity entity) {
        super(object, entity);
    }

    @Override
    public <T> T accept(DiffSnapshotVisitor<T> visitor) {
        return visitor.visitInsert(this);
    }

    @Override
    public void addValue(DbAttribute attribute, Object value) {
        if(values == null) {
            values = new HashMap<>();
        }
        values.put(attribute.getName(), value);
    }

    protected void addFlattenedId(String path, ObjectId id) {
        if(flattenedIds == null) {
            flattenedIds = new HashMap<>();
        }
        flattenedIds.put(path, id);
    }

    protected Map<String, Object> getSnapshot() {
        if(values == null) {
            return changeId.getIdSnapshot();
        }
        values.putAll(changeId.getIdSnapshot());
        return values;
    }

    protected Map<String, ObjectId> getFlattenedIds() {
        if(flattenedIds == null) {
            return Collections.emptyMap();
        }
        return flattenedIds;
    }
}
