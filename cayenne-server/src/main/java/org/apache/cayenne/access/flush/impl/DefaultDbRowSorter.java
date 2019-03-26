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

package org.apache.cayenne.access.flush.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.flush.row.DbRowSorter;
import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.access.flush.row.DeleteDbRow;
import org.apache.cayenne.access.flush.row.InsertDbRow;
import org.apache.cayenne.access.flush.row.UpdateDbRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
public class DefaultDbRowSorter implements DbRowSorter {

    // This is default order of operations
    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;

    @Inject
    private Provider<DataDomain> dataDomainProvider;

    private volatile Comparator<DbRow> comparator;

    @Override
    public List<DbRow> sort(Collection<DbRow> dbRows) {

        List<DbRow> sortedDbRows = new ArrayList<>(dbRows);
        // sort by id, operation type and entity relations
        sortedDbRows.sort(getComparator());
        // sort reflexively dependent objects
        sortReflexive(sortedDbRows);

        return sortedDbRows;
    }

    private void sortReflexive(List<DbRow> sortedDbRows) {
        DataDomain dataDomain = dataDomainProvider.get();
        EntitySorter sorter = dataDomain.getEntitySorter();
        EntityResolver resolver = dataDomain.getEntityResolver();

        DbEntity lastEntity = null;
        int start = 0;
        int idx = 0;
        DbRow lastRow = null;
        for(DbRow row : sortedDbRows) {
            if (row.getEntity() != lastEntity) {
                start = idx;
                if(lastEntity != null && sorter.isReflexive(lastEntity)) {
                    ObjEntity objEntity = resolver.getObjEntity(lastRow.getObject().getObjectId().getEntityName());
                    List<DbRow> reflexiveSublist = sortedDbRows.subList(start, idx);
                    sorter.sortObjectsForEntity(objEntity, reflexiveSublist, lastRow instanceof DeleteDbRow);
                }
                lastEntity = row.getEntity();
            }
            lastRow = row;
            idx++;
        }
        // sort last chunk
        if(lastEntity != null && sorter.isReflexive(lastEntity)) {
            ObjEntity objEntity = resolver.getObjEntity(lastRow.getObject().getObjectId().getEntityName());
            List<DbRow> reflexiveSublist = sortedDbRows.subList(start, idx);
            sorter.sortObjectsForEntity(objEntity, reflexiveSublist, lastRow instanceof DeleteDbRow);
        }
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
            // TODO: check this in real example of meaningful PK insert/delete cycle
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
                if(leftType == DELETE) {
                    // invert order for delete
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
            return INSERT;
        }

        @Override
        public Integer visitUpdate(UpdateDbRow diffSnapshot) {
            return UPDATE;
        }

        @Override
        public Integer visitDelete(DeleteDbRow diffSnapshot) {
            return DELETE;
        }
    }
}
