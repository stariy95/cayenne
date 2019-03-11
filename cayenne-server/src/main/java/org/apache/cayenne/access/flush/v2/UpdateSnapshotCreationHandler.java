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

import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
class UpdateSnapshotCreationHandler extends SnapshotCreationHandler {

    UpdateSnapshotCreationHandler(ObjectStore store, ClassDescriptor descriptor) {
        super(store, descriptor);
    }

    @Override
    protected UpdateDiffSnapshot createSnapshot(DbEntity entity) {
        return new UpdateDiffSnapshot(entity);
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
    }
}
