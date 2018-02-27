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

package org.apache.cayenne.access.sqlbuilder.sqltree;

/**
 * @since 4.1
 */
public class ValueNode extends Node {

    private final Object value;

    public ValueNode(Object value) {
        this.value = value;
        this.type = NodeType.VALUE;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void append(StringBuilder buffer) {
        appendValue(value, buffer);
    }

    private void appendValue(Object val, StringBuilder buffer) {
        if(val == null) {
            return;
        }

        boolean isString = val instanceof CharSequence;
        boolean isArray = val.getClass().isArray();

        if(isArray) {
            if(val instanceof byte[]) {
                appendValue((byte[])val, buffer);
            } else if(val instanceof short[]) {
                appendValue((short[])val, buffer);
            } else if(val instanceof char[]) {
                appendValue((char[])val, buffer);
            } else if(val instanceof int[]) {
                appendValue((int[])val, buffer);
            } else if(val instanceof long[]) {
                appendValue((long[])val, buffer);
            } else if(val instanceof float[]) {
                appendValue((float[])val, buffer);
            } else if(val instanceof double[]) {
                appendValue((double[])val, buffer);
            } else if(val instanceof boolean[]) {
                appendValue((boolean[])val, buffer);
            } else if(val instanceof Object[]) {
                appendValue((Object[])val, buffer);
            }
        } else {
            if (isString) {
                buffer.append('\'');
            }
            buffer.append(String.valueOf(val));
            if (isString) {
                buffer.append('\'');
            }
        }
    }

    private void appendValue(byte[] val, StringBuilder buffer) {
        boolean first = true;
        for(byte i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(short[] val, StringBuilder buffer) {
        boolean first = true;
        for(short i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(char[] val, StringBuilder buffer) {
        boolean first = true;
        for(char i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(int[] val, StringBuilder buffer) {
        boolean first = true;
        for(int i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(long[] val, StringBuilder buffer) {
        boolean first = true;
        for(long i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(float[] val, StringBuilder buffer) {
        boolean first = true;
        for(float i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(double[] val, StringBuilder buffer) {
        boolean first = true;
        for(double i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(boolean[] val, StringBuilder buffer) {
        boolean first = true;
        for(boolean i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }

    private void appendValue(Object[] val, StringBuilder buffer) {
        boolean first = true;
        for(Object i : val) {
            if(first) {
                first = false;
            } else {
                buffer.append(',');
            }
            appendValue(i, buffer);
        }
    }
}