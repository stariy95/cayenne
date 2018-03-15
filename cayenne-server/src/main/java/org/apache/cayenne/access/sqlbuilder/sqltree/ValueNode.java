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

import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.1
 */
public class ValueNode extends Node {

    private final Object value;

    // Used as hint for type of this value
    private final DbAttribute attribute;

    public ValueNode(Object value, DbAttribute attribute) {
        this.value = value;
        this.attribute = attribute;
        this.type = NodeType.VALUE;
    }

    public Object getValue() {
        return value;
    }

    public DbAttribute getAttribute() {
        return attribute;
    }

    @Override
    public void append(QuotingAppendable buffer) {
        appendValue(value, buffer);
    }

    private void appendValue(Object val, QuotingAppendable buffer) {
        if(val == null) {
            return;
        }

        boolean isString = val instanceof CharSequence;
        boolean isArray = val.getClass().isArray();

        if(isArray) {
            if(val instanceof short[]) {
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
            } else {
                appendObjectValue(buffer, val);
            }
        } else {
            if(isString) {
                appendStringValue(buffer, (CharSequence)val);
            } else {
                appendObjectValue(buffer, val);
            }
        }
    }

    protected void appendObjectValue(QuotingAppendable buffer, Object value) {
        if(value == null) {
            return;
        }
        buffer.append('?');
    }

    protected void appendStringValue(QuotingAppendable buffer, CharSequence value) {
        buffer.append('?');
    }

    private void appendValue(short[] val, QuotingAppendable buffer) {
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

    private void appendValue(char[] val, QuotingAppendable buffer) {
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

    private void appendValue(int[] val, QuotingAppendable buffer) {
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

    private void appendValue(long[] val, QuotingAppendable buffer) {
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

    private void appendValue(float[] val, QuotingAppendable buffer) {
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

    private void appendValue(double[] val, QuotingAppendable buffer) {
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

    private void appendValue(boolean[] val, QuotingAppendable buffer) {
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

    private void appendValue(Object[] val, QuotingAppendable buffer) {
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
