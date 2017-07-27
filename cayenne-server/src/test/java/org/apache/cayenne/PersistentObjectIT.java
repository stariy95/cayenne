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

import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PersistentObjectIT extends ServerCase {

    @Test
    public void testObjectContext() {
        ObjectContext context = mock(ObjectContext.class);
        PersistentObject object = new MockPersistentObject();

        assertNull(object.getObjectContext());
        object.setObjectContext(context);
        assertSame(context, object.getObjectContext());
    }

    @Test
    public void testPersistenceState() {
        PersistentObject object = new MockPersistentObject();
        assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());
        object.setPersistenceState(PersistenceState.DELETED);
        assertEquals(PersistenceState.DELETED, object.getPersistenceState());
    }

    @Test
    public void testObjectID() {
        ObjectId id = new ObjectId(new ObjectIdDescriptor("test"));

        PersistentObject object = new MockPersistentObject();

        assertNull(object.getObjectId());
        object.setObjectId(id);
        assertSame(id, object.getObjectId());
    }

}
