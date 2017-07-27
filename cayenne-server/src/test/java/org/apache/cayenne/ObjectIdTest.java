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

import org.apache.cayenne.util.Util;
import org.apache.commons.collections.map.LinkedMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ObjectIdTest {

    @Test
    public void testConstructor() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("e", new String[]{"a"});
        ObjectId temp1 = new ObjectId(descriptor);
        assertEquals("e", temp1.getEntityName());
        assertTrue(temp1.isTemporary());
        assertNotEquals(0, temp1.getKey());

        long key = 123;
        ObjectIdDescriptor descriptor2 = new ObjectIdDescriptor("e1", new String[]{"a"});
        ObjectId temp2 = new ObjectId(descriptor2, key);
        assertEquals("e1", temp2.getEntityName());
        assertTrue(temp2.isTemporary());
        assertSame(key, temp2.getKey());
    }

    @Test
    public void testSerializabilityTemp() throws Exception {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("e", new String[]{"a"});
        ObjectId temp1 = new ObjectId(descriptor);
        ObjectId temp2 = Util.cloneViaSerialization(temp1);

        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    @Test
    public void testSerializabilityPerm() throws Exception {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("e", new String[]{"a"});
        ObjectId perm1 = new ObjectId(descriptor, "a", "b");

        // make sure hashcode is resolved
        int h = perm1.hashCode();
        assertEquals(h, perm1.hashCode);
        assertTrue(perm1.hashCode != 0);

        ObjectId perm2 = Util.cloneViaSerialization(perm1);

        // make sure hashCode is reset to 0
        assertTrue(perm2.hashCode == 0);

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    @Test
    public void testEquals0() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("TE", new String[]{"a"});
        ObjectId oid1 = new ObjectId(descriptor);
        assertEquals(oid1, oid1);
        assertEquals(oid1.hashCode(), oid1.hashCode());
    }

    @Test
    public void testEquals1() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"a"});
        ObjectId oid1 = new ObjectId(descriptor, "a", "b");
        ObjectId oid2 = new ObjectId(descriptor, "a", "b");
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    @Test
    public void testEquals2() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"a"});
        Map<String, Object> hm = new HashMap<>();
        ObjectId oid1 = new ObjectId(descriptor, hm);
        ObjectId oid2 = new ObjectId(descriptor, hm);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    @Test
    public void testEquals3() {
        String pknm = "xyzabc";

        Map<String, Object> hm1 = new HashMap<>();
        hm1.put(pknm, "123");

        Map<String, Object> hm2 = new HashMap<>();
        hm2.put(pknm, "123");

        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{pknm});
        ObjectId oid1 = new ObjectId(descriptor, hm1);
        ObjectId oid2 = new ObjectId(descriptor, hm2);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    /**
     * This is a test case reproducing conditions for the bug "8458963".
     */
    @Test
    public void testEquals5() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"key1", "key2"});
        Map<String, Object> hm1 = new HashMap<>();
        hm1.put("key1", 1);
        hm1.put("key2", 11);

        Map<String, Object> hm2 = new HashMap<>();
        hm2.put("key1", 11);
        hm2.put("key2", 1);

        ObjectId ref = new ObjectId(descriptor, hm1);
        ObjectId oid = new ObjectId(descriptor, hm2);
        assertFalse(ref.equals(oid));
    }

    /**
     * Multiple key objectId
     */
    @Test
    public void testEquals6() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"key1", "key2"});
        Map<String, Object> hm1 = new HashMap<>();
        hm1.put("key1", 1);
        hm1.put("key2", 2);

        Map<String, Object> hm2 = new HashMap<>();
        hm2.put("key1", 1);
        hm2.put("key2", 2);

        ObjectId ref = new ObjectId(descriptor, hm1);
        ObjectId oid = new ObjectId(descriptor, hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    /**
     * Checks that hashCode works even if keys are inserted in the map in a
     * different order...
     */
    @Test
    public void testEquals7() {

        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"KEY1", "KEY2"});
        // create maps with guaranteed iteration order

        @SuppressWarnings("unchecked")
        Map<String, Object> hm1 = new LinkedMap();
        hm1.put("KEY1", 1);
        hm1.put("KEY2", 2);

        @SuppressWarnings("unchecked")
        Map<String, Object> hm2 = new LinkedMap();
        // put same keys but in different order
        hm2.put("KEY2", 2);
        hm2.put("KEY1", 1);

        ObjectId ref = new ObjectId(descriptor, hm1);
        ObjectId oid = new ObjectId(descriptor, hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    @Test
    public void testEqualsBinaryKey() {

        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"key1"});

        Map<String, Object> hm1 = new HashMap<>();
        hm1.put("key1", new byte[] { 3, 4, 10, -1 });

        Map<String, Object> hm2 = new HashMap<>();
        hm2.put("key1", new byte[] { 3, 4, 10, -1 });

        ObjectId ref = new ObjectId(descriptor, hm1);
        ObjectId oid = new ObjectId(descriptor, hm2);
        assertEquals(ref.hashCode(), oid.hashCode());
        assertTrue(ref.equals(oid));
    }

    @Test
    public void testEqualsNull() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"ARTIST_ID"});
        ObjectId o = new ObjectId(descriptor, "ARTIST_ID", new Integer(42));
        assertFalse(o.equals(null));
    }

    @Test
    public void testIdAsMapKey() {
        Map<ObjectId, Object> map = new HashMap<>();
        Object o1 = new Object();

        String pknm = "xyzabc";
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{pknm});

        Map<String, Object> hm1 = new HashMap<>();
        hm1.put(pknm, "123");

        Map<String, Object> hm2 = new HashMap<>();
        hm2.put(pknm, "123");

        ObjectId oid1 = new ObjectId(descriptor, hm1);
        ObjectId oid2 = new ObjectId(descriptor, hm2);

        map.put(oid1, o1);
        assertSame(o1, map.get(oid2));
    }

    @Test
    public void testNotEqual1() {
        ObjectIdDescriptor descriptor1 = new ObjectIdDescriptor("T1", new String[]{"a"});
        ObjectIdDescriptor descriptor2 = new ObjectIdDescriptor("T2", new String[]{"a"});
        ObjectId oid1 = new ObjectId(descriptor1);
        ObjectId oid2 = new ObjectId(descriptor2);
        assertFalse(oid1.equals(oid2));
    }

    @Test
    public void testNotEqual2() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"pk1"});

        Map<String, Object> hm1 = new HashMap<>();
        hm1.put("pk1", "123");

        Map<String, Object> hm2 = new HashMap<>();
        hm2.put("pk2", "123");

        ObjectId oid1 = new ObjectId(descriptor, hm1);
        ObjectId oid2 = new ObjectId(descriptor, hm2);
        assertFalse(oid1.equals(oid2));
    }

    /**
     * Test different numeric types.
     */
    @Test
    public void testEquals8() {

        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("T", new String[]{"KEY1", "KEY2"});
        // create maps with guaranteed iteration order

        @SuppressWarnings("unchecked")
        Map<String, Object> hm1 = new LinkedMap();
        hm1.put("KEY1", 1);
        hm1.put("KEY2", 2);

        @SuppressWarnings("unchecked")
        Map<String, Object> hm2 = new LinkedMap();
        // put same keys but in different order
        hm2.put("KEY2", new BigDecimal(2.00));
        hm2.put("KEY1", 1L);

        ObjectId ref = new ObjectId(descriptor, hm1);
        ObjectId oid = new ObjectId(descriptor, hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    @Test
    public void testToString() {
        ObjectIdDescriptor descriptor = new ObjectIdDescriptor("e1", new String[]{"a", "b"});

        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", "1");
        m1.put("b", "2");
        ObjectId i1 = new ObjectId(descriptor, m1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("b", "2");
        m2.put("a", "1");

        ObjectId i2 = new ObjectId(descriptor, m2);

        assertEquals(i1, i2);
        assertEquals(i1.toString(), i2.toString());
    }
}
