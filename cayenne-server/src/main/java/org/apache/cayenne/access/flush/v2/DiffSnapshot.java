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

package org.apache.cayenne.access.flush.v2;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public abstract class DiffSnapshot {

    protected final Persistent object;
    protected final DbEntity entity;
    // Can be not ObjEntity id but a DB row id
    protected ObjectId changeId;

    protected DiffSnapshot(Persistent object, DbEntity entity) {
        this.object = object;
        this.entity = entity;
    }

    public abstract <T> T accept(DiffSnapshotVisitor<T> visitor);

    public DbEntity getEntity() {
        return entity;
    }

    public ObjectId getChangeId() {
        return changeId;
    }

    public Persistent getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiffSnapshot snapshot = (DiffSnapshot) o;
        return changeId.equals(snapshot.changeId);
    }

    @Override
    public int hashCode() {
        return changeId.hashCode();
    }
}
