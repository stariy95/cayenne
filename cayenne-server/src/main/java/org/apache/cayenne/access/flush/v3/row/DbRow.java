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

package org.apache.cayenne.access.flush.v3.row;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public interface DbRow extends DataObject {

    <T> T accept(DbRowVisitor<T> visitor);

    DbEntity getEntity();

    ObjectId getChangeId();

    Persistent getObject();

    boolean isSameBatch(DbRow row);

    default ObjectId getObjectId() {
        return getObject().getObjectId();
    }

    default void setObjectId(ObjectId id) {
        getObject().setObjectId(id);
    }

    default int getPersistenceState() {
        return getObject().getPersistenceState();
    }

    default void setPersistenceState(int state) {
        getObject().setPersistenceState(state);
    }

    default ObjectContext getObjectContext() {
        return getObject().getObjectContext();
    }

    default void setObjectContext(ObjectContext objectContext) {
        getObject().setObjectContext(objectContext);
    }

    default Object readPropertyDirectly(String propertyName) {
        return ((DataObject)getObject()).readPropertyDirectly(propertyName);
    }

    default Object readProperty(String propertyName) {
        return ((DataObject)getObject()).readProperty(propertyName);
    }

    default Object readNestedProperty(String path) {
        return ((DataObject)getObject()).readNestedProperty(path);
    }

    default void writePropertyDirectly(String propertyName, Object val) {
        throw new UnsupportedOperationException();
    }

    default void writeProperty(String propertyName, Object value) {
        throw new UnsupportedOperationException();
    }

    default void addToManyTarget(
            String relationshipName,
            DataObject target,
            boolean setReverse) {
        throw new UnsupportedOperationException();
    }

    default void removeToManyTarget(
            String relationshipName,
            DataObject target,
            boolean unsetReverse) {
        throw new UnsupportedOperationException();
    }

    default void setToOneTarget(
            String relationshipName,
            DataObject value,
            boolean setReverse) {
        throw new UnsupportedOperationException();
    }

    default long getSnapshotVersion() {
        throw new UnsupportedOperationException();
    }

    default void setSnapshotVersion(long snapshotVersion) {
        throw new UnsupportedOperationException();
    }
}
