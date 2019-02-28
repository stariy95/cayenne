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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class NewDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final OperationSorter operationSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected NewDataDomainFlushAction(DataDomain dataDomain, OperationSorter operationSorter, JdbcEventLogger jdbcEventLogger) {
        this.dataDomain = dataDomain;
        this.operationSorter = operationSorter;
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {

        ObjectStore objectStore = context.getObjectStore();
        // ObjectStoreGraphDiff contains changes already categorized by objectId...
        Map<Object, ObjectDiff> changesByObjectId = ((ObjectStoreGraphDiff) changes).getChangesByObjectId();

        return null;
    }

    enum OperationType {
        INSERT,
        UPDATE,
        DELETE
    }

    static class ChangeId {
        final DbEntity entity;
        final Object[] id;

        ObjectId objectIdRef;

        static ChangeId fromSnapshot(Snapshot snapshot) {
            Collection<DbAttribute> primaryKeys = snapshot.entity.getPrimaryKeys();
            Object[] id = new Object[primaryKeys.size()];
            int i=0;
            for (DbAttribute primaryKey : primaryKeys) {
                Object idValue = snapshot.getValue(primaryKey);
                if(idValue != null) {
                    id[i++] = idValue;
                } else {
                    // TODO: need to resolve relationship here...
                    id[i++] = snapshot.id.getIdSnapshot().get(primaryKey.getName());
                }
            }
            return new ChangeId(snapshot.entity, id);
        }

        ChangeId(DbEntity entity, Object[] id) {
            this.entity = entity;
            this.id = id;
        }

        public void setObjectIdRef(ObjectId objectIdRef) {
            this.objectIdRef = objectIdRef;
        }

        public ObjectId getObjectIdRef() {
            return objectIdRef;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChangeId changeId = (ChangeId) o;
            if (!entity.equals(changeId.entity)) return false;
            return Arrays.deepEquals(id, changeId.id);
        }

        @Override
        public int hashCode() {
            return 31 * entity.hashCode() + Arrays.deepHashCode(id);
        }
    }

    static class Snapshot {
        final OperationType type;

        // header
        final ObjectId id;
        final DbEntity entity;
        final List<DbAttribute> attributes;
        // data
        final Map<DbAttribute, Object> data;

        Snapshot(ObjectId id, DbEntity entity, OperationType type) {
            this.id = id;
            this.entity = entity;
            this.type = type;
            this.data = new HashMap<>();
            this.attributes = new ArrayList<>(entity.getAttributes());
        }

        Object getValue(DbAttribute attribute) {
            return data.get(attribute);
        }

        Object getValue(int i) {
            return data.get(attributes.get(i));
        }
    }

}
