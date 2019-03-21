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

package org.apache.cayenne.access.flush.v3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.flush.SnapshotSorter;
import org.apache.cayenne.access.flush.v3.row.DbRow;
import org.apache.cayenne.access.flush.v3.row.DbRowVisitor;
import org.apache.cayenne.access.flush.v3.row.DeleteDbRow;
import org.apache.cayenne.access.flush.v3.row.InsertDbRow;
import org.apache.cayenne.access.flush.v3.row.UpdateDbRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.EntitySorter;

/**
 * @since 4.2
 */
public class DefaultDbRowSorter implements SnapshotSorter {

    @Inject
    private Provider<DataDomain> dataDomainProvider;

    private volatile Comparator<DbRow> comparator;

    @Override
    public List<DbRow> sortDbRows(Collection<DbRow> dbRows) {

        List<DbRow> snapshotList = new ArrayList<>(dbRows);
        snapshotList.sort(getComparator());

        // TODO: sort objects with reflexive relationships
//        EntitySorter sorter = dataDomainProvider.get().getEntitySorter();
//        DbEntity lastEntity = null;
//        List<Persistent> objects = null;
//        for(DbRow row : dbRows) {
//            if (row.getEntity() != lastEntity) {
//                if(objects != null) {
//                }
//                lastEntity = row.getEntity();
//            }
//            if (sorter.isReflexive(lastEntity)) {
//                if(objects == null) {
//                    objects = new ArrayList<>();
//                }
//                objects.add(row.getObject());
//            } else {
//                objects = null;
//            }
//        }

        return snapshotList;
    }

    private Comparator<DbRow> getComparator() {
        Comparator<DbRow> local = comparator;
        if(local == null) {
            synchronized (this) {
                local = comparator;
                if(local == null) {
                    local = new DbRowComparator(dataDomainProvider.get().getEntitySorter());
                    comparator = local;
                }
            }
        }
        return local;
    }

    static class DbRowComparator implements Comparator<DbRow> {

        private final EntitySorter entitySorter;

        DbRowComparator(EntitySorter entitySorter) {
            this.entitySorter = entitySorter;
        }

        @Override
        public int compare(DbRow left, DbRow right) {
            int leftType = DbRowTypeExtractor.INSTANCE.apply(left);
            int rightType = DbRowTypeExtractor.INSTANCE.apply(right);

            int result;
            if(left.getChangeId().equals(right.getChangeId())) {
                result = Integer.compare(rightType, leftType);
            } else {
                result = Integer.compare(leftType, rightType);
            }

            if(result != 0) {
                return result;
            }

            result = entitySorter.getDbEntityComparator().compare(left.getEntity(), right.getEntity());
            if(result != 0) {
                if(leftType == 3) {
                    return -result;
                }
                return result;
            }

            return 0;
        }
    }

    private static class DbRowTypeExtractor implements Function<DbRow, Integer> {

        static private final DbRowTypeExtractor INSTANCE = new DbRowTypeExtractor();

        @Override
        public Integer apply(DbRow o) {
            return o.accept(DbRowTypeVisitor.INSTANCE);
        }
    }

    private static class DbRowTypeVisitor implements DbRowVisitor<Integer> {

        private static final DbRowTypeVisitor INSTANCE = new DbRowTypeVisitor();

        @Override
        public Integer visitInsert(InsertDbRow diffSnapshot) {
            return 1;
        }

        @Override
        public Integer visitUpdate(UpdateDbRow diffSnapshot) {
            return 2;
        }

        @Override
        public Integer visitDelete(DeleteDbRow diffSnapshot) {
            return 3;
        }
    }
}
