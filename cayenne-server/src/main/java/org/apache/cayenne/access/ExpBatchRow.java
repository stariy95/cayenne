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

package org.apache.cayenne.access;

import java.util.Map;

import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.1
 */
class ExpBatchRow {
    private final int type; //
    private final ClassDescriptor descriptor; // ?
    private final Map<String, Object> objectIdSnapshot; // or just ObjectId?
    private Map<String, Object> fullSnapshot;

    ExpBatchRow(int type, ClassDescriptor descriptor, Map<String, Object> objectIdSnapshot) {
        this.type = type;
        this.descriptor = descriptor;
        this.objectIdSnapshot = objectIdSnapshot;
    }

    void setFullSnapshot(Map<String, Object> fullSnapshot) {
        this.fullSnapshot = fullSnapshot;
    }

    ClassDescriptor getDescriptor() {
        return descriptor;
    }

    int getType() {
        return type;
    }

    Map<String, Object> getFullSnapshot() {
        return fullSnapshot;
    }

    Map<String, Object> getObjectIdSnapshot() {
        return objectIdSnapshot;
    }
}
