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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class InsertDbRow extends BaseDbRow implements DbRowWithValues {

    protected final Values values;

    protected InsertDbRow(Persistent object, DbEntity entity, ObjectId id) {
        super(object, entity, id);
        values = new Values(this, true);
    }

    @Override
    public <T> T accept(DbRowVisitor<T> visitor) {
        return visitor.visitInsert(this);
    }

    @Override
    public Values getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        // TODO: here go troubles with transitivity
        //   insert = update, update = delete, delete != insert
        //   though we need this only to store in a hash map, so it should be ok...
        if(!(o instanceof DbRowWithValues)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public boolean isSameBatch(DbRow row) {
        if(!(row instanceof InsertDbRow)) {
            return false;
        }
        InsertDbRow other = (InsertDbRow)row;
        return values.isSameBatch(other.values);
    }
}
