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

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
public class DefaultOperationSorter implements OperationSorter{

    @Inject
    private Provider<DataDomain> dataDomainProvider;

    public List<Operation> sort(List<Operation> operations) {
        // TODO:
        //  1. extract entities for delete and change
        //  2. sort with entity sorter
        //  3. sort by op
        //  4. sort by entity order
        //  5. rearrange ops by ObjectId (delete before insert/update)

        DataDomain dataDomain = dataDomainProvider.get();
        EntityResolver resolver = dataDomain.getEntityResolver();
        EntitySorter entitySorter = dataDomain.getEntitySorter();

        Function<Operation, Integer> operationType = new OperationTypeExtractor();
        Function<Operation, ObjEntity> entityType = o -> resolver.getObjEntity(o.id.getEntityName());

        operations.sort(Comparator.comparing(operationType)
                .thenComparing(entityType, entitySorter.getObjEntityComparator()));

        return operations;
    }

    private static class OperationTypeExtractor implements Function<Operation, Integer> {
        @Override
        public Integer apply(Operation o) {
            return o.visit(new OperationVisitor<Integer>() {
                @Override
                public Integer visitInsert(InsertOperation op) {
                    return 1;
                }

                @Override
                public Integer visitUpdate(UpdateOperation op) {
                    return 2;
                }

                @Override
                public Integer visitDelete(DeleteOperation op) {
                    return 3;
                }
            });
        }
    }
}
