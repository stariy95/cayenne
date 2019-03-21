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

package org.apache.cayenne.access.flush.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.SnapshotSorter;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
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
    protected final SnapshotSorter operationSorter;
    protected final JdbcEventLogger jdbcEventLogger;

    protected NewDataDomainFlushAction(DataDomain dataDomain, SnapshotSorter operationSorter, JdbcEventLogger jdbcEventLogger) {
        this.dataDomain = dataDomain;
        this.operationSorter = operationSorter;
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {
        CompoundDiff result = new CompoundDiff();
        if (changes == null) {
            return result;
        }

        List<Operation> operations = createOperations(context, changes);
        objectIdUpdate(operations);
        List<BatchQuery> queries = createQueries(context, operations);
        executeQueries(queries);
        setFinalIds(operations, result);

        context.getObjectStore().postprocessAfterCommit(result);

        return changes;
    }

    protected List<Operation> createOperations(DataContext context, GraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();

        Map<Object, ObjectDiff> changesByObjectId = ((ObjectStoreGraphDiff) changes).getChangesByObjectId();
        List<Operation> operations = new ArrayList<>(changesByObjectId.size());
        changesByObjectId.forEach((obj, diff) -> {
            ObjectId id = (ObjectId)obj;
            Persistent object = (Persistent)context.getObjectStore().getNode(id);
            ClassDescriptor descriptor = resolver.getClassDescriptor(id.getEntityName());
            SnapshotCreationHandler handler = new SnapshotCreationHandler(context.getObjectStore(), descriptor, object);
            diff.apply(handler);
            for(Snapshot snapshot : handler.getSnapshotMap().values()) {
                Operation operation;
                switch (snapshot.type) {
                    case INSERT:
                        operation = new InsertOperation(id, object, diff);
                        break;
                    case UPDATE:
                        operation = new UpdateOperation(id, object, diff);
                        break;
                    case DELETE:
                        operation = new DeleteOperation(id, object, diff);
                        break;
                    default:
                        throw new CayenneRuntimeException("Unknown operation type " + snapshot.type.name());
                }
                operation.setSnapshot(snapshot);
                operations.add(operation);
            }
        });
        return operationSorter.sort(operations);
    }

    protected List<BatchQuery> createQueries(DataContext context, List<Operation> operations) {
        OperationVisitor<BatchQuery> visitor = new SnapshotQueryCreationVisitor();
        return operations.stream()
                .map(op -> op.visit(visitor)) // create query from operation
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void executeQueries(List<BatchQuery> queries) {
        OperationObserver observer = new FlushOperationObserver(jdbcEventLogger);
        // TODO: batch queries by node change, when queries are sorted should speedup a bit
        queries.forEach(query -> dataDomain
                .lookupDataNode(query.getDbEntity().getDataMap())
                .performQueries(Collections.singleton(query), observer));
    }

    protected void objectIdUpdate(List<Operation> operations) {
        OperationVisitor<Void> visitor = new NewPermObjectIdVisitor(dataDomain);
        operations.forEach(op -> op.visit(visitor));
    }

    protected void setFinalIds(List<Operation> operations, CompoundDiff result) {
        OperationVisitor<Void> visitor = new FinalIdVisitor(dataDomain.getEntityResolver(), result);
        operations.forEach(op -> op.visit(visitor));
    }

    enum OperationType {
        INSERT,
        UPDATE,
        DELETE;

        static OperationType forObject(Persistent object) {
            switch (object.getPersistenceState()) {
                case PersistenceState.NEW:
                    return INSERT;
                case PersistenceState.DELETED:
                    return DELETE;
                case PersistenceState.MODIFIED:
                    return UPDATE;
            }
            throw new CayenneRuntimeException("Trying to flush object (%s) in wrong persistence state (%s)",
                    object, PersistenceState.persistenceStateName(object.getPersistenceState()));
        }
    }

    static class ChangeId {
        final DbEntity entity;
        final Object[] id;

        static ChangeId fromSnapshot(Snapshot snapshot) {
            Collection<DbAttribute> primaryKeys = snapshot.entity.getPrimaryKeys();
            Object[] id = new Object[primaryKeys.size()];
            int i=0;
            for (DbAttribute primaryKey : primaryKeys) {
                Object idValue = snapshot.getValue(primaryKey);
                if(idValue != null) {
                    id[i++] = idValue;
                } else {
                    id[i++] = snapshot.getId().getIdSnapshot().get(primaryKey.getName());
                }
            }
            return new ChangeId(snapshot.getEntity(), id);
        }

        ChangeId(DbEntity entity, Object[] id) {
            this.entity = entity;
            this.id = id;
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

    static class Snapshot extends BatchQueryRow {
        final OperationType type;
        // header
        final DbEntity entity;
        final List<DbAttribute> attributes;
        // data
        final Map<DbAttribute, Object> data;

        ObjectId id; // ???

        Snapshot(ObjectId id, DbEntity entity, OperationType type) {
            super(id, new HashMap<>());
            this.id = id;
            this.entity = entity;
            this.type = type;
            this.data = new HashMap<>();
            this.attributes = new ArrayList<>(entity.getAttributes());
        }

        Object getValue(DbAttribute attribute) {
            return data.get(attribute);
        }

        public Object getValue(int i) {
            return data.get(attributes.get(i));
        }

        ObjectId getId() {
            return id;
        }

        void setId(ObjectId id) {
            this.id = id;
        }

        DbEntity getEntity() {
            return entity;
        }

        Map<String, Object> getInsertSnapshot() {
            Map<String, Object> map = new HashMap<>(data.size() + 1);
            data.forEach((attr, value) -> {
                map.put(attr.getName(), value);
            });
            map.putAll(id.getIdSnapshot());
            return map;
        }

        Map<String, Object> getUpdateSnapshot() {
            Map<String, Object> map = new HashMap<>(data.size());
            data.forEach((attr, value) -> {
                map.put(attr.getName(), value);
            });
            return map;
        }

        public List<DbAttribute> getModifiedAttributes() {
            return new ArrayList<>(data.keySet());
        }
    }

    private static class SnapshotQueryCreationVisitor implements OperationVisitor<BatchQuery> {

        public SnapshotQueryCreationVisitor() {
        }

        @Override
        public BatchQuery visitInsert(InsertOperation operation) {
            InsertBatchQuery query = new InsertBatchQuery(operation.getSnapshot().getEntity(), 1);
            query.add(operation.getSnapshot().getInsertSnapshot(), operation.getId());
            return query;
        }

        @Override
        public BatchQuery visitUpdate(UpdateOperation operation) {
            Snapshot snapshot = operation.getSnapshot();
            ArrayList<DbAttribute> qualifierAttributes = new ArrayList<>(snapshot.getEntity().getPrimaryKeys());
            UpdateBatchQuery query = new UpdateBatchQuery(snapshot.getEntity(), qualifierAttributes, snapshot.getModifiedAttributes(), Collections.emptyList(), 1);
            query.add(snapshot.getId().getIdSnapshot(), snapshot.getUpdateSnapshot(), operation.getId());
            return query;
        }
    }

    private static class SnapshotCreationHandler implements GraphChangeHandler {

        private final ObjectStore store;
        private final Persistent persistent;
        private final ClassDescriptor descriptor;
        private final OperationType type;

        private final boolean qualifierOnly;
        private PropertyExtractor visitor;
        private Map<DbEntity, Snapshot> snapshotMap;

        SnapshotCreationHandler(ObjectStore store, ClassDescriptor descriptor, Persistent persistent) {
            this.store = store;
            this.descriptor = descriptor;
            this.persistent = persistent;
            this.type = OperationType.forObject(persistent);
            visitor = new PropertyExtractor();
            qualifierOnly = type == OperationType.DELETE;
        }

        @Override
        public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
            if(descriptor.getEntity().isReadOnly()) {
                throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                        "Can't commit changes.", descriptor.getEntity().getName());
            }

            PropertyDescriptor propertyDescriptor = descriptor.getProperty(property);
            visitor.reset();
            propertyDescriptor.visit(visitor);

            ObjAttribute attribute = visitor.getAttribute();
            ObjectId id = persistent.getObjectId();

            if(attribute.isFlattened()) {
                String path = attribute.getDbAttributePath();
                String parent = path.substring(0, path.lastIndexOf('.'));
                id = store.getFlattenedId(persistent.getObjectId(), parent);
            }

            addToSnapshot(id, attribute.getDbAttribute(), newValue);
        }

        @Override
        public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
            String relationshipName = arcId.toString();
            PropertyDescriptor propertyDescriptor = descriptor.getProperty(relationshipName);
            if(propertyDescriptor != null) {
                visitor.reset();
                propertyDescriptor.visit(visitor);
                ObjRelationship relationship = visitor.getRelationship();
                DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
                ObjectId targetId = (ObjectId)targetNodeId;
                for(DbJoin join : dbRelationship.getJoins()) {
                    // skip PK
                    if(join.getSource().isPrimaryKey() && !dbRelationship.isToMasterPK()) {
                        continue;
                    }
                    addToSnapshot((ObjectId)nodeId, join.getSource(), (Supplier) () -> targetId.getIdSnapshot().get(join.getTargetName()));
                }
            }
        }

        @Override
        public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {

        }

        Map<DbEntity, Snapshot> getSnapshotMap() {
            return snapshotMap == null ? Collections.emptyMap() : snapshotMap;
        }

        private void addToSnapshot(ObjectId id, DbAttribute attribute, Object value) {
            if(snapshotMap == null) {
                snapshotMap = new HashMap<>();
            }

            Snapshot snapshot = snapshotMap
                    .computeIfAbsent(attribute.getEntity(), entity -> new Snapshot(id, entity, type));
            snapshot.data.put(attribute, value);
        }

        // not used

        @Override
        public void nodeIdChanged(Object nodeId, Object newId) {}

        @Override
        public void nodeCreated(Object nodeId) {}

        @Override
        public void nodeRemoved(Object nodeId) {}

        private static class PropertyExtractor implements PropertyVisitor {
            private ClassDescriptor target;
            private ObjAttribute attribute;
            private ObjRelationship relationship;

            void reset() {
                target = null;
                attribute = null;
                relationship = null;
            }

            ObjAttribute getAttribute() {
                return Objects.requireNonNull(attribute, "No attribute found");
            }

            ClassDescriptor getTarget() {
                return Objects.requireNonNull(target, "No target descriptor found");
            }

            ObjRelationship getRelationship() {
                return Objects.requireNonNull(relationship, "No relationship found");
            }

            @Override
            public boolean visitAttribute(AttributeProperty property) {
                this.attribute = property.getAttribute();
                return false;
            }

            @Override
            public boolean visitToOne(ToOneProperty property) {
                this.target = property.getTargetDescriptor();
                this.relationship = property.getRelationship();
                return false;
            }

            @Override
            public boolean visitToMany(ToManyProperty property) {
                this.target = property.getTargetDescriptor();
                this.relationship = property.getRelationship();
                return false;
            }
        }
    }
}
