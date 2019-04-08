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
import org.apache.cayenne.access.flush.row.DbRowOpSorter;
import org.apache.cayenne.access.flush.row.DbRowOp;
import org.apache.cayenne.access.flush.row.DbRowOpVisitor;
import org.apache.cayenne.access.flush.row.DeleteDbRowOp;
import org.apache.cayenne.access.flush.row.InsertDbRowOp;
import org.apache.cayenne.access.flush.row.UpdateDbRowOp;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
public class DefaultDbRowOpSorter implements DbRowOpSorter {

    // This is default order of operations
    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;

    private final Provider<DataDomain> dataDomainProvider;

    private volatile Comparator<DbRowOp> comparator;

    public DefaultDbRowOpSorter(@Inject Provider<DataDomain> dataDomainProvider) {
        this.dataDomainProvider = dataDomainProvider;
    }

    @Override
    public List<DbRowOp> sort(Collection<DbRowOp> dbRows) {

        List<DbRowOp> sortedDbRows = new ArrayList<>(dbRows);
        // sort by id, operation type and entity relations
        sortedDbRows.sort(getComparator());
        // sort reflexively dependent objects
        sortReflexive(sortedDbRows);

        return sortedDbRows;
    }

    private void sortReflexive(List<DbRowOp> sortedDbRows) {
        DataDomain dataDomain = dataDomainProvider.get();
        EntitySorter sorter = dataDomain.getEntitySorter();
        EntityResolver resolver = dataDomain.getEntityResolver();

        DbEntity lastEntity = null;
        int start = 0;
        int idx = 0;
        DbRowOp lastRow = null;
        for(DbRowOp row : sortedDbRows) {
            if (row.getEntity() != lastEntity) {
                start = idx;
                if(lastEntity != null && sorter.isReflexive(lastEntity)) {
                    ObjEntity objEntity = resolver.getObjEntity(lastRow.getObject().getObjectId().getEntityName());
                    List<DbRowOp> reflexiveSublist = sortedDbRows.subList(start, idx);
                    sorter.sortObjectsForEntity(objEntity, reflexiveSublist, lastRow instanceof DeleteDbRowOp);
                }
                lastEntity = row.getEntity();
            }
            lastRow = row;
            idx++;
        }
        // sort last chunk
        if(lastEntity != null && sorter.isReflexive(lastEntity)) {
            ObjEntity objEntity = resolver.getObjEntity(lastRow.getObject().getObjectId().getEntityName());
            List<DbRowOp> reflexiveSublist = sortedDbRows.subList(start, idx);
            sorter.sortObjectsForEntity(objEntity, reflexiveSublist, lastRow instanceof DeleteDbRowOp);
        }
    }

    private Comparator<DbRowOp> getComparator() {
        Comparator<DbRowOp> local = comparator;
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

    static class DbRowComparator implements Comparator<DbRowOp> {

        private final EntitySorter entitySorter;
        private final Function<DbRowOp, Integer> typeExtractor;

        DbRowComparator(EntitySorter entitySorter) {
            this.entitySorter = entitySorter;
            this.typeExtractor = row -> row.accept(DbRowTypeVisitor.INSTANCE);
        }

        @Override
        public int compare(DbRowOp left, DbRowOp right) {
            // 1. sort by op type
            int leftType = typeExtractor.apply(left);
            int rightType = typeExtractor.apply(right);
            int result = Integer.compare(leftType, rightType);
            if(result != 0
                    && left.getChangeId().getIdSnapshot().equals(right.getChangeId().getIdSnapshot())) {
                // need rearrange order of inserts/deletes for same IDs
                result = -result;
            }

            if(result != 0) {
                return result;
            }

            // 2. sort by entity relations
            result = entitySorter.getDbEntityComparator().compare(left.getEntity(), right.getEntity());
            if(result != 0) {
                return leftType == DELETE ? -result : result;
            }

            // TODO: 3. sort updates by changed and null attributes to batch it better...
            return 0;
        }
    }

    private static class DbRowTypeVisitor implements DbRowOpVisitor<Integer> {

        private static final DbRowTypeVisitor INSTANCE = new DbRowTypeVisitor();

        @Override
        public Integer visitInsert(InsertDbRowOp diffSnapshot) {
            return INSERT;
        }

        @Override
        public Integer visitUpdate(UpdateDbRowOp diffSnapshot) {
            return UPDATE;
        }

        @Override
        public Integer visitDelete(DeleteDbRowOp diffSnapshot) {
            return DELETE;
        }
    }
}
