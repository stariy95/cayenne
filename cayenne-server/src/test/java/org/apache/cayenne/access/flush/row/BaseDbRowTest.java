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

package org.apache.cayenne.access.flush.row;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.COMPOUND_PROJECT)
public class BaseDbRowTest extends ServerCase {

    @Inject
    private DataContext context;

    @SuppressWarnings("SimplifiableJUnitAssertion")
    @Test
    public void testHashCode_TmpId() {

        ObjEntity entity = context.getEntityResolver().getObjEntity(CompoundPkTestEntity.class);
        CompoundPkTestEntity object1 = context.newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity object2 = context.newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity object3 = context.newObject(CompoundPkTestEntity.class);

        object1.getObjectId().getReplacementIdMap().put("KEY1", "11");
        object1.getObjectId().getReplacementIdMap().put("KEY2", "21");

        object2.getObjectId().getReplacementIdMap().put("KEY2", "21");
        object2.getObjectId().getReplacementIdMap().put("KEY1", "11");

        object3.getObjectId().getReplacementIdMap().put("KEY1", "12");
        object3.getObjectId().getReplacementIdMap().put("KEY2", "21");

        DbRow row1 = new InsertDbRow(object1, entity.getDbEntity(), object1.getObjectId());
        DbRow row2 = new InsertDbRow(object2, entity.getDbEntity(), object2.getObjectId());
        DbRow row3 = new InsertDbRow(object3, entity.getDbEntity(), object3.getObjectId());

        assertEquals(row1.hashCode(), row1.hashCode());
        assertEquals(row1.hashCode(), row2.hashCode());

        assertNotEquals(row1.hashCode(), row3.hashCode());
        assertNotEquals(row2.hashCode(), row3.hashCode());

        assertTrue(row1.equals(row2));
        assertTrue(row2.equals(row1));

        assertFalse(row1.equals(row3));
        assertFalse(row3.equals(row1));

        assertFalse(row2.equals(row3));
        assertFalse(row3.equals(row2));

        Map<DbRow, DbRow> map = new HashMap<>();
        map.put(row1, row1);
        map.put(row2, row2);
        map.put(row3, row3);

        assertEquals(2, map.size());
    }

}