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

package org.apache.cayenne.dba.oracle;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class OracleSqlTreeProcessorTest {

    @Test
    public void sliceArray() {
        int[] array = {1, 2, 3, 4, 5, 6, 7};

        int[][] result = OracleSqlTreeProcessor.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new int[]{1, 2}, result[0]);
        assertArrayEquals(new int[]{3, 4}, result[1]);
        assertArrayEquals(new int[]{5, 6}, result[2]);
        assertArrayEquals(new int[]{7},    result[3]);

        int[][] result2 = OracleSqlTreeProcessor.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new int[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new int[]{5, 6, 7}, result2[1]);

        int[][] result3 = OracleSqlTreeProcessor.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        int[][] result4 = OracleSqlTreeProcessor.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        int[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        int[][] result5 = OracleSqlTreeProcessor.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new int[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new int[]{5, 6, 7, 8}, result5[1]);
    }
}