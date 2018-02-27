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

package org.apache.cayenne.access.translator.select.next;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 */
public class SQLGenerationVisitor implements NodeTreeVisitor {

    private static final Logger logger = LoggerFactory.getLogger(SQLGenerationVisitor.class);

    private final TranslatorContext context;

    private final StringBuilder builder;

    private int level = 0;

    public SQLGenerationVisitor(TranslatorContext context) {
        this.builder = new StringBuilder();
        this.context = context;
    }

    @Override
    public void onNodeStart(Node node) {
        StringBuilder msg = new StringBuilder();
        for(int i=0; i<level; i++) {
            msg.append("  ");
        }
        msg.append("start node {}");
        logger.info(msg.toString(), node);
        level++;

        if(node instanceof ValueNode) {
            if(context != null) {
                Object value = ((ValueNode) node).getValue();
                if(value instanceof byte[]) {
                    addValueBinding((byte[])value);
                } else if(value instanceof short[]) {
                    addValueBinding((short[])value);
                } else if(value instanceof char[]) {
                    addValueBinding((char[])value);
                } else if(value instanceof int[]) {
                    addValueBinding((int[])value);
                } else if(value instanceof long[]) {
                    addValueBinding((long[])value);
                } else if(value instanceof float[]) {
                    addValueBinding((float[])value);
                } else if(value instanceof double[]) {
                    addValueBinding((double[])value);
                } else if(value instanceof boolean[]) {
                    addValueBinding((boolean[])value);
                } else if(value instanceof Object[]) {
                    addValueBinding((Object[]) value);
                } else if(value instanceof ObjectId) {
                    addValueBinding((ObjectId)value);
                } else if(value instanceof Persistent) {
                    addValueBinding((Persistent)value);
                } else {
                    addValueBinding(value);
                }
                builder.delete(builder.length() - 1, builder.length());
            }
        } else {
            node.append(builder);
            node.appendChildrenStart(builder);
        }
    }

    private void addValueBinding(byte[] value) {
        for(byte v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(short[] value) {
        for(short v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(char[] value) {
        for(char v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(int[] value) {
        for(int v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(long[] value) {
        for(long v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(float[] value) {
        for(float v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(double[] value) {
        for(double v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(boolean[] value) {
        for(boolean v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(Object[] value) {
        for(Object v : value) {
            addValueBinding(v);
        }
    }

    private void addValueBinding(Persistent value) {
        addValueBinding(value.getObjectId());
    }

    private void addValueBinding(ObjectId value) {
        for(Object idVal: value.getIdSnapshot().values()) {
            addValueBinding(idVal);
        }
    }

    private void addValueBinding(Object value) {
        ExtendedType extendedType = value != null
                ? context.getAdapter().getExtendedTypes().getRegisteredType(value.getClass())
                : context.getAdapter().getExtendedTypes().getDefaultType();
        DbAttributeBinding binding = new DbAttributeBinding(null); //
        binding.setStatementPosition(context.getBindings().size() + 1);
        binding.setExtendedType(extendedType);
        binding.setValue(value);
        context.getBindings().add(binding);
        if(value == null) {
            builder.append(",");
            binding.setStatementPosition(-1);
        } else {
            builder.append("?,");
        }
    }

    @Override
    public void onChildNodeEnd(Node node, int index, boolean hasMore) {
        if(hasMore && node.getParent() != null) {
            node.getParent().appendChildSeparator(builder, index);
        }
    }

    @Override
    public void onNodeEnd(Node node) {
        node.appendChildrenEnd(builder);
        level--;
    }

    public String getString() {
        return builder.toString();
    }
}
