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

package org.apache.cayenne.access.flush;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.access.flush.v1.Operation;
import org.apache.cayenne.access.flush.v2.DiffSnapshot;

/**
 * @since 4.2
 * TODO: remove default implementations once v1 src is gone...
 */
public interface SnapshotSorter {

    default List<Operation> sort(List<Operation> operations) {
        return operations;
    }

    default List<DiffSnapshot> sortSnapshots(Collection<DiffSnapshot> snapshots) {
        if(snapshots instanceof List) {
            return (List<DiffSnapshot>)snapshots;
        }
        return new ArrayList<>(snapshots);
    }

}
