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

import org.apache.cayenne.CayenneRuntimeException;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * helper class to generate pseudo-GUID sequences.
 *
 */
public class IDUtil {

    private static class Holder {
        static final SecureRandom numberGenerator = new SecureRandom();
    }

    /**
     * Prints a byte value to a StringBuffer as a double digit hex value.
     *
     * @since 1.2 Since 3.0 signature has changed to take Appendable argument.
     */
    public static void appendFormattedByte(Appendable buffer, byte byteValue) {
        final String digits = "0123456789ABCDEF";

        try {
            buffer.append(digits.charAt((byteValue >>> 4) & 0xF));
            buffer.append(digits.charAt(byteValue & 0xF));
        }
        catch (IOException e) {
            throw new CayenneRuntimeException("Error appending data to buffer", e);
        }
    }

    /**
     * @param length the length of returned byte[]
     * @return A pseudo-unique byte array of the specified length. Length must be at least
     *         8 bytes, or an exception is thrown.
     * @since 1.0.2
     */
    public static byte[] pseudoUniqueByteSequence(int length) {
        return pseudoUniqueSecureByteSequence(length);
    }

    public static byte[] pseudoUniqueSecureByteSequence(int length) {
        if (length < 8) {
            throw new IllegalArgumentException(
                    "Can't generate unique byte sequence shorter than 16 bytes: "
                            + length);
        }

        byte[] randomBytes = new byte[length];
        SecureRandom ng = Holder.numberGenerator;
        ng.nextBytes(randomBytes);
        return randomBytes;
    }

    public static byte[] pseudoUniqueByteSequence8() {
        return pseudoUniqueSecureByteSequence(8);
    }

    /**
     * @return A pseudo unique 16-byte array.
     */
    public static byte[] pseudoUniqueByteSequence16() {
        return pseudoUniqueSecureByteSequence(16);
    }

    /**
     * @return A pseudo unique digested 16-byte array.
     */
    public static byte[] pseudoUniqueSecureByteSequence16() {
        return pseudoUniqueSecureByteSequence(16);
    }

    private IDUtil() {
    }
}
