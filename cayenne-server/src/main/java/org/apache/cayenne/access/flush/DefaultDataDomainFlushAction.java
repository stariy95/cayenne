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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final OperationSorter operationSorter;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, OperationSorter operationSorter) {
        this.dataDomain = dataDomain;
        this.operationSorter = operationSorter;
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {
        if (changes == null) {
            return new CompoundDiff();
        }

        List<Operation> operations = createOperations(context, changes);
        operations = operationSorter.sort(operations);
        List<BatchQuery> queries = createQueries(context, operations);
        executeQueries(queries);

        return null;
    }

    protected List<Operation> createOperations(DataContext context, GraphDiff changes) {
        if (!(changes instanceof ObjectStoreGraphDiff)) {
            throw new IllegalArgumentException("Expected 'ObjectStoreGraphDiff', got: " + changes.getClass().getName());
        }

        ObjectStore objectStore = context.getObjectStore();
        // ObjectStoreGraphDiff contains changes already categorized by objectId...
        Map<Object, ObjectDiff> changesByObjectId = ((ObjectStoreGraphDiff) changes).getChangesByObjectId();
        return changesByObjectId.keySet().stream().map(key -> {
            ObjectId id = (ObjectId) key;
            Persistent object = (Persistent) objectStore.getNode(id);
            return createOperationForObject(id, object);
        }).collect(Collectors.toList());
    }

    protected List<BatchQuery> createQueries(DataContext context, List<Operation> operations) {
        OperationVisitor<BatchQuery> visitor = new QueryOperationVisitor(context);
        return operations.stream().map(op -> op.visit(visitor)).collect(Collectors.toList());
    }

    protected void executeQueries(List<BatchQuery> queries) {
        queries.forEach(
                q -> dataDomain
                        .lookupDataNode(q.getDbEntity().getDataMap())
                        .performQueries(Collections.singleton(q), null)
        );
    }

    protected Operation createOperationForObject(ObjectId id, Persistent object) {
        switch (object.getPersistenceState()) {
            case PersistenceState.NEW:
                return new InsertOperation(id, object);
            case PersistenceState.MODIFIED:
                return new UpdateOperation(id, object);
            case PersistenceState.DELETED:
                return new DeleteOperation(id, object);
        }
        throw new CayenneRuntimeException("Changed object in unknown state " + object.getPersistenceState());
    }

    static class QueryOperationVisitor implements OperationVisitor<BatchQuery> {
        private final DataContext context;

        QueryOperationVisitor(DataContext context) {
            this.context = context;
        }

        @Override
        public BatchQuery visitInsert(InsertOperation operation) {
            ObjEntity objEntity = context.getEntityResolver().getObjEntity(operation.getObject());
            InsertBatchQuery query = new InsertBatchQuery(objEntity.getDbEntity(), 1);
            DataRow snapshot = context.getObjectStore().getSnapshot(operation.getId());
            query.add(snapshot, operation.getId());
            return query;
        }

        @Override
        public BatchQuery visitUpdate(UpdateOperation operation) {
            return null;
        }

        @Override
        public BatchQuery visitDelete(DeleteOperation operation) {
            return null;
        }
    }
}
