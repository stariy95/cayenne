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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

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

        EntityResolver resolver = dataDomain.getEntityResolver();

        // ObjectStoreGraphDiff contains changes already categorized by objectId...
        Map<Object, ObjectDiff> changesByObjectId = ((ObjectStoreGraphDiff) changes).getChangesByObjectId();
        changesByObjectId.forEach((obj, diff) -> {
            Persistent persistent = (Persistent)obj;
            ClassDescriptor descriptor = resolver.getClassDescriptor(persistent.getObjectId().getEntityName());
            SnapshotCreationHandler handler = new SnapshotCreationHandler(descriptor, persistent);
            diff.apply(handler);

        });

        return null;
    }

    enum OperationType {
        INSERT,
        UPDATE,
        DELETE;

        static OperationType forObject(Persistent object) {
            switch (object.getPersistenceState()) {
                case PersistenceState.NEW:
                    return UPDATE;
                case PersistenceState.DELETED:
                    return DELETE;
                case PersistenceState.MODIFIED:
                    return UPDATE;
            }
            throw new CayenneRuntimeException("Triyng to flush object (%s) in wrong persistence state (%s)",
                    object, PersistenceState.persistenceStateName(object.getPersistenceState()));
        }
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
        final ObjectId id; // ???
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

    private static class SnapshotCreationHandler implements GraphChangeHandler {

        private final Persistent persistent;
        private final ClassDescriptor descriptor;

        private Snapshot snapshot;

        SnapshotCreationHandler(ClassDescriptor descriptor, Persistent persistent) {
            this.descriptor = descriptor;
            this.persistent = persistent;
            OperationType type = OperationType.forObject(persistent);
            if(type == OperationType.DELETE) {
                // only qualifier needed
            }
        }

        @Override
        public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
            PropertyDescriptor propertyDescriptor = descriptor.getProperty(property);
            MyPropertyVisitor visitor = new MyPropertyVisitor();
            propertyDescriptor.visit(visitor);
        }

        @Override
        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

        }

        @Override
        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        }

        //
        @Override
        public void nodeIdChanged(Object nodeId, Object newId) {}

        @Override
        public void nodeCreated(Object nodeId) {}

        @Override
        public void nodeRemoved(Object nodeId) {}

        private static class MyPropertyVisitor implements PropertyVisitor {
            private ClassDescriptor target;
            private ObjAttribute attribute;

            @Override
            public boolean visitAttribute(AttributeProperty property) {
                this.attribute = property.getAttribute();
                return false;
            }

            @Override
            public boolean visitToOne(ToOneProperty property) {
                this.target = property.getTargetDescriptor();
                return false;
            }

            @Override
            public boolean visitToMany(ToManyProperty property) {
                return false;
            }
        }
    }
}
