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

package org.apache.cayenne.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;

/**
 * @since 1.2
 */
class DataDomainDeleteBucket extends DataDomainSyncBucket {

    DataDomainDeleteBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    @Override
    void postprocess() {

        if (!objectsByDescriptor.isEmpty()) {

            Collection<ObjectId> deletedIds = parent.getResultDeletedIds();

            for (List<Persistent> objects : objectsByDescriptor.values()) {
                for (Persistent object : objects) {
                    deletedIds.add(object.getObjectId());
                }
            }
        }
    }

    @Override
    void appendQueriesInternal(Collection<Query> queries) {

        DataNodeSyncQualifierDescriptor qualifierBuilder = new DataNodeSyncQualifierDescriptor();

        EntitySorter sorter = parent.getDomain().getEntitySorter();
        sorter.sortDbEntities(dbEntities, true);
        // delete can spawn out-of-order insert for meaningful PKs that both deleted and inserted
        LinkedList<Query> outOfOrderInserts = null;

        for (DbEntity dbEntity : dbEntities) {
            Collection<DbEntityClassDescriptor> descriptors = descriptorsByDbEntity
                    .get(dbEntity);
            Map<Object, Query> batches = new LinkedHashMap<>();

            for (DbEntityClassDescriptor descriptor : descriptors) {

                qualifierBuilder.reset(descriptor);

                boolean isRootDbEntity = descriptor.isMaster();

                // remove object set for dependent entity, so that it does not show up
                // on post processing
                List<Persistent> objects = objectsByDescriptor.get(descriptor
                        .getClassDescriptor());
                if (objects.isEmpty()) {
                    continue;
                }

                checkReadOnly(descriptor.getEntity());

                if (isRootDbEntity) {
                    sorter.sortObjectsForEntity(descriptor.getEntity(), objects, true);
                }

                List<Persistent> insertedObjectsForDescriptor = parent
                        .getInsertedObjectsForDescriptor(descriptor.getClassDescriptor());

                for (Persistent o : objects) {
                    ObjectDiff diff = parent.objectDiff(o.getObjectId());

                    Map<String, Object> qualifierSnapshot = qualifierBuilder
                            .createQualifierSnapshot(diff);

                    // organize batches by the nulls in qualifier
                    Set<String> nullQualifierNames = new HashSet<String>();
                    for (Map.Entry<String, ?> entry : qualifierSnapshot.entrySet()) {
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    Object batchKey = Arrays.asList(nullQualifierNames);

                    DeleteBatchQuery batch = (DeleteBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new DeleteBatchQuery(dbEntity, qualifierBuilder
                                .getAttributes(), nullQualifierNames, 27);
                        batch.setUsingOptimisticLocking(qualifierBuilder
                                .isUsingOptimisticLocking());
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot);

                    // check that no entity is inserted with this ObjectId
                    if(insertedObjectsForDescriptor == null || insertedObjectsForDescriptor.isEmpty()) {
                        continue;
                    }

                    // this can be slow
                    BatchQueryRow queryRow = parent
                            .findAndRemoveInsertFromBatch(descriptor.getDbEntity(), o.getObjectId().getIdSnapshot());
                    if(queryRow != null) {
                        InsertBatchQuery query = new InsertBatchQuery(descriptor.getDbEntity(), 1);
                        query.add(queryRow);
                        if(outOfOrderInserts == null) {
                            outOfOrderInserts = new LinkedList<>();
                        }
                        outOfOrderInserts.addFirst(query);
                    }
                }
            }

            queries.addAll(batches.values());
        }

        if(outOfOrderInserts != null) {
            queries.addAll(outOfOrderInserts);
        }
    }
}
