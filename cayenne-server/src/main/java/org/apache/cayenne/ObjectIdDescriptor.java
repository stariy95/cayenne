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

package org.apache.cayenne;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Shared structure used by ObjectId describing ObjEntity PKs.
 * This structure allows move constant data out of each ObjectId.
 *
 * @since 4.1
 */
public class ObjectIdDescriptor implements Serializable {

    private static final long serialVersionUID = -9086247557105553929L;
    /**
     * ObjEntity name
     */
    private final String entityName;

    /**
     * Sorted array of PK attributes' names
     */
    private final String[] pkNames;

    public ObjectIdDescriptor(String entityName, String... pkNames) {
        this.entityName = Objects.requireNonNull(entityName);
        this.pkNames = Objects.requireNonNull(pkNames);
    }

    public String getEntityName() {
        return entityName;
    }

    public String[] getPkNames() {
        return pkNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectIdDescriptor that = (ObjectIdDescriptor) o;
        return entityName.equals(that.entityName) && Arrays.equals(pkNames, that.pkNames);
    }

    @Override
    public int hashCode() {
        return 31 * entityName.hashCode() + Arrays.hashCode(pkNames);
    }
}
