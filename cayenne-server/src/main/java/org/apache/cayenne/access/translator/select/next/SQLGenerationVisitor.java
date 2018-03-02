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
import org.apache.cayenne.map.DbAttribute;
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
                DbAttribute attribute = ((ValueNode) node).getAttribute();
                if(value instanceof short[]) {
                    addValueBinding((short[])value, attribute);
                } else if(value instanceof char[]) {
                    addValueBinding((char[])value, attribute);
                } else if(value instanceof int[]) {
                    addValueBinding((int[])value, attribute);
                } else if(value instanceof long[]) {
                    addValueBinding((long[])value, attribute);
                } else if(value instanceof float[]) {
                    addValueBinding((float[])value, attribute);
                } else if(value instanceof double[]) {
                    addValueBinding((double[])value, attribute);
                } else if(value instanceof boolean[]) {
                    addValueBinding((boolean[])value, attribute);
                } else if(value instanceof Object[]) {
                    addValueBinding((Object[]) value, attribute);
                } else if(value instanceof ObjectId) {
                    addValueBinding((ObjectId)value, attribute);
                } else if(value instanceof Persistent) {
                    addValueBinding((Persistent)value, attribute);
                } else {
                    addValueBinding(value, attribute);
                }
                builder.delete(builder.length() - 1, builder.length());
            }
        } else {
            node.append(builder);
            node.appendChildrenStart(builder);
        }
    }

    private void addValueBinding(short[] value, DbAttribute attribute) {
        for(short v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(char[] value, DbAttribute attribute) {
        for(char v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(int[] value, DbAttribute attribute) {
        for(int v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(long[] value, DbAttribute attribute) {
        for(long v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(float[] value, DbAttribute attribute) {
        for(float v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(double[] value, DbAttribute attribute) {
        for(double v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(boolean[] value, DbAttribute attribute) {
        for(boolean v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(Object[] value, DbAttribute attribute) {
        for(Object v : value) {
            addValueBinding(v, attribute);
        }
    }

    private void addValueBinding(Persistent value, DbAttribute attribute) {
        addValueBinding(value.getObjectId(), attribute);
    }

    private void addValueBinding(ObjectId value, DbAttribute attribute) {
        for(Object idVal: value.getIdSnapshot().values()) {
            addValueBinding(idVal, attribute);
        }
    }

    private void addValueBinding(Object value, DbAttribute attribute) {
        ExtendedType extendedType = value != null
                ? context.getAdapter().getExtendedTypes().getRegisteredType(value.getClass())
                : context.getAdapter().getExtendedTypes().getDefaultType();
        if(value == null) {
            builder.append(",");
        } else {
            DbAttributeBinding binding = new DbAttributeBinding(attribute);
            binding.setStatementPosition(context.getBindings().size() + 1);
            binding.setExtendedType(extendedType);
            binding.setValue(value);
            context.getBindings().add(binding);
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
