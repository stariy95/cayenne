/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.access.types.ByteArrayTypeTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class IDUtilTest {

    @Test
    public void testPseudoUniqueByteSequence1() throws Exception {
        try {
            IDUtil.pseudoUniqueByteSequence(6);
            fail("must throw an exception on short sequences");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testPseudoUniqueByteSequence2() throws Exception {
        byte[] byte16 = IDUtil.pseudoUniqueByteSequence(16);
        assertNotNull(byte16);
        assertEquals(16, byte16.length);

        try {
            ByteArrayTypeTest.assertByteArraysEqual(
                byte16,
                IDUtil.pseudoUniqueByteSequence(16));
            fail("Same byte array..");
        } catch (Throwable th) {

        }
    }

    @Test
    public void testPseudoUniqueByteSequence3() throws Exception {
        byte[] byte17 = IDUtil.pseudoUniqueByteSequence(17);
        assertNotNull(byte17);
        assertEquals(17, byte17.length);

        byte[] byte123 = IDUtil.pseudoUniqueByteSequence(123);
        assertNotNull(byte123);
        assertEquals(123, byte123.length);
    }

    @Test
    public void testUniqueness() {
        Set<Key> keys = new HashSet<>();
        for(int i=0; i<100000; i++) {
            assertTrue(keys.add(new Key(IDUtil.pseudoUniqueByteSequence8())));
        }
    }

    static class Key {
        final byte[] data;

        Key(byte[] data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;
            return Arrays.equals(data, key.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}
