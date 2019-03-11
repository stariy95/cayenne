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

package org.apache.cayenne.access.flush.v1;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff;

/**
 * @since 4.2
 */
public abstract class Operation {

    protected final ObjectId id;
    protected final Persistent object;
    protected final ObjectDiff diff;
    protected NewDataDomainFlushAction.Snapshot snapshot;

    public Operation(ObjectId id, Persistent object, ObjectDiff diff) {
        this.id = id;
        this.object = object;
        this.diff = diff;
    }

    public ObjectId getId() {
        return id;
    }

    public Persistent getObject() {
        return object;
    }

    public ObjectDiff getDiff() {
        return diff;
    }

    public void setSnapshot(NewDataDomainFlushAction.Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public NewDataDomainFlushAction.Snapshot getSnapshot() {
        return snapshot;
    }

    public abstract <T> T visit(OperationVisitor<T> visitor);
}
