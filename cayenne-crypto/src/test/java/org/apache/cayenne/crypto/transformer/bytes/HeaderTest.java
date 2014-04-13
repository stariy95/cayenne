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
package org.apache.cayenne.crypto.transformer.bytes;

import static org.junit.Assert.assertEquals;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.junit.Test;

public class HeaderTest {

    @Test
    public void testCreate_WithKeyName() {

        assertEquals("bcd", Header.create("bcd").getKeyName());
        assertEquals("bc", Header.create("bc").getKeyName());
        assertEquals("b", Header.create("b").getKeyName());
    }

    @Test(expected = CayenneCryptoException.class)
    public void testCreate_WithKeyName_TooLong() {

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < Byte.MAX_VALUE + 1; i++) {
            buf.append("a");
        }

        Header.create(buf.toString());
    }

    @Test
    public void testCreate_WithData() {
        byte[] input1 = { 'C', 'C', '1', 9, 0, 'a', 'b', 'c', 'd', 'e' };
        assertEquals("abcd", Header.create(input1, 0).getKeyName());
        
        byte[] input2 = { 0, 'C', 'C', '1', 9, 0, 'a', 'b', 'c', 'd', 'e' };
        assertEquals("abcd", Header.create(input2, 1).getKeyName());
    }
}