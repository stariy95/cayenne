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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.flush.row.DbRow;
import org.apache.cayenne.access.flush.row.DbRowVisitor;
import org.apache.cayenne.access.flush.row.DeleteDbRow;
import org.apache.cayenne.access.flush.row.InsertDbRow;
import org.apache.cayenne.access.flush.row.UpdateDbRow;

/**
 * @since 4.2
 */
class PostprocessVisitor implements DbRowVisitor<Void> {

    private final DataContext context;
    private Map<ObjectId, DataRow> updatedSnapshots;
    private Collection<ObjectId> deletedIds;

    PostprocessVisitor(DataContext context) {
        this.context = context;
    }

    @Override
    public Void visitInsert(InsertDbRow dbRow) {
        processObjectChange(dbRow);
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRow dbRow) {
        processObjectChange(dbRow);
        return null;
    }

    private void processObjectChange(DbRow dbRow) {
        if (dbRow.getChangeId().getEntityName().startsWith(PermanentObjectIdVisitor.DB_ID_PREFIX)) {
            return;
        }

        DataRow dataRow = context.currentSnapshot(dbRow.getObject());

        if (dbRow.getObject() instanceof DataObject) {
            DataObject dataObject = (DataObject) dbRow.getObject();
            dataRow.setReplacesVersion(dataObject.getSnapshotVersion());
            dataObject.setSnapshotVersion(dataRow.getVersion());
        }

        if (updatedSnapshots == null) {
            updatedSnapshots = new HashMap<>();
        }
        updatedSnapshots.put(dbRow.getObject().getObjectId(), dataRow);
    }

    @Override
    public Void visitDelete(DeleteDbRow dbRow) {
        if (dbRow.getChangeId().getEntityName().startsWith(PermanentObjectIdVisitor.DB_ID_PREFIX)) {
            return null;
        }
        if (deletedIds == null) {
            deletedIds = new HashSet<>();
        }
        deletedIds.add(dbRow.getChangeId());
        return null;
    }

    Collection<ObjectId> getDeletedIds() {
        return deletedIds == null ? Collections.emptyList() : deletedIds;
    }

    Map<ObjectId, DataRow> getUpdatedSnapshots() {
        return updatedSnapshots == null ? Collections.emptyMap() : updatedSnapshots;
    }
}
