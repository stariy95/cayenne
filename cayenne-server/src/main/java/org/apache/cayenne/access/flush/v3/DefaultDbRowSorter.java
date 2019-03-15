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

    @Override
    public List<DbRow> sortDbRows(Collection<DbRow> dbRows) {
        DataDomain dataDomain = dataDomainProvider.get();
        EntitySorter entitySorter = dataDomain.getEntitySorter();

//        EntityResolver resolver = dataDomain.getEntityResolver();
//        List<DiffSnapshot> sublist = snapshots.subList(0, 2);
        List<DbRow> snapshotList = new ArrayList<>(dbRows);

        snapshotList.sort(Comparator
                .comparing(DbRowTypeExtractor.INSTANCE)
                .thenComparing(DbRow::getEntity, entitySorter.getDbEntityComparator()));

        return snapshotList;
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
