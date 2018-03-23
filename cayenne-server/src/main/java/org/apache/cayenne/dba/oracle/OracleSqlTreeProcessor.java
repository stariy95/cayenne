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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.SelectBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.dba.derby.sqltree.DerbyColumnNode;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.1
 */
public class OracleSqlTreeProcessor implements Function<Node, Node> {

    @Override
    public Node apply(Node node) {
        SelectBuilder[] selectBuilder = {null};

        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {

            }

            @Override
            public void onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
                switch (child.getType()) {
                    case RESULT:
                        for(int i=0; i<child.getChildrenCount(); i++) {
                            child.replaceChild(i, aliased(child.getChild(i), "c" + i).build());
                        }
                        break;
                    case COLUMN:
                        DerbyColumnNode replacement = new DerbyColumnNode((ColumnNode)child);
                        for(int i=0; i<child.getChildrenCount(); i++) {
                            replacement.addChild(child.getChild(i));
                        }
                        parent.replaceChild(index, replacement);
                        break;
                    case FUNCTION:
                        FunctionNode oldNode = (FunctionNode) child;
                        String functionName = oldNode.getFunctionName();
                        Node functionReplacement = null;
                        switch (functionName) {
                            case "SUBSTRING":
                                functionReplacement = new FunctionNode("SUBSTR", oldNode.getAlias(), true);
                                break;
                            case "LOCATE":
                                functionReplacement = new FunctionNode("INSTR", oldNode.getAlias(), true);
                                for(int i=0; i<=1; i++) {
                                    functionReplacement.addChild(child.getChild(1-i));
                                }
                                parent.replaceChild(index, functionReplacement);
                                return;
                            case "CONCAT":
                                functionReplacement = new ExpressionNode() {
                                    @Override
                                    public void appendChildSeparator(QuotingAppendable builder, int childInd) {
                                        builder.append("||");
                                    }
                                };
                                break;
                            case "CURRENT_TIMESTAMP":
                            case "CURRENT_DATE":
                                functionReplacement = new FunctionNode(functionName, oldNode.getAlias(), false);
                                break;

                            case "CURRENT_TIME":
                                functionReplacement = new FunctionNode("{fn CURTIME()}", oldNode.getAlias(), false);
                                break;

                            case "DAY_OF_YEAR":
                            case "DAY_OF_WEEK":
                            case "WEEK":
                                functionReplacement = new FunctionNode("TO_CHAR", oldNode.getAlias(), true);
                                functionReplacement.addChild(child.getChild(0));
                                if("DAY_OF_YEAR".equals(functionName)) {
                                    functionName = "'DDD'";
                                } else if("DAY_OF_WEEK".equals(functionName)) {
                                    functionName = "'D'";
                                } else {
                                    functionName = "'IW'";
                                }
                                functionReplacement.addChild(new TextNode(functionName));
                                parent.replaceChild(index, functionReplacement);
                                return;

                            case "YEAR":
                            case "MONTH":
                            case "DAY":
                            case "DAY_OF_MONTH":
                            case "HOUR":
                            case "MINUTE":
                            case "SECOND":
                                functionReplacement = new FunctionNode("EXTRACT", oldNode.getAlias(), true) {
                                    @Override
                                    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
                                        builder.append(' ');
                                    }
                                };
                                if("DAY_OF_MONTH".equals(functionName)) {
                                    functionName = "DAY";
                                }
                                functionReplacement.addChild(new TextNode(functionName + " FROM "));
                                break;
                        }

                        if(functionReplacement != null) {
                            for(int i=0; i<child.getChildrenCount(); i++) {
                                functionReplacement.addChild(child.getChild(i));
                            }
                            parent.replaceChild(index, functionReplacement);
                        }
                        break;

                    case LIMIT_OFFSET:
                        LimitOffsetNode limitOffsetNode = (LimitOffsetNode)child;
                        if(limitOffsetNode.getLimit() > 0 || limitOffsetNode.getOffset() > 0) {
                            int limit = limitOffsetNode.getLimit();
                            int offset = limitOffsetNode.getOffset();
                            int max = (limit <= 0) ? Integer.MAX_VALUE : limit + offset;

                            /*
                             Transform query with limit/offset into following form:
                             SELECT * FROM (
                                SELECT tid.*, ROWNUM rnum
                                FROM ( MAIN_QUERY ) tid
                                WHERE ROWNUM <= OFFSET + LIMIT
                             ) WHERE rnum > OFFSET
                             */
                            selectBuilder[0] = select(all())
                                    .from(select(text("tid.*"), text("ROWNUM rnum")) // using text not column to avoid quoting
                                            .from(aliased(() -> node, "tid"))
                                            .where(exp(text("ROWNUM")).lte(value(max))))
                                    .where(exp(text("rnum")).gt(value(offset)));
                        }
                        parent.replaceChild(index, new EmptyNode());
                        break;

                    case IN:
                        InNode inNode = (InNode)child;
                        Node arg = inNode.getChild(0);
                        Node childNode = inNode.getChild(1);
                        if(childNode.getType() != NodeType.VALUE) {
                            return;
                        }

                        ValueNode valueNode = (ValueNode)childNode;
                        Object value = valueNode.getValue();
                        if(!value.getClass().isArray()) {
                            return;
                        }

                        List<Node> newChildren = new ArrayList<>();

                        // ok need to slice for batches of 1000 values
                        if(value instanceof Object[]) {
                            Object[][] slices = sliceArray((Object[])value, 1000);
                            for(Object[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof int[]) {
                            int[][] slices = sliceArray((int[])value, 1000);
                            for(int[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof long[]) {
                            long[][] slices = sliceArray((long[])value, 1000);
                            for(long[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof float[]) {
                            float[][] slices = sliceArray((float[])value, 1000);
                            for(float[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof double[]) {
                            double[][] slices = sliceArray((double[])value, 1000);
                            for(double[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof short[]) {
                            short[][] slices = sliceArray((short[])value, 1000);
                            for(short[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof char[]) {
                            char[][] slices = sliceArray((char[])value, 1000);
                            for(char[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof boolean[]) {
                            boolean[][] slices = sliceArray((boolean[])value, 1000);
                            for(boolean[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        } else if(value instanceof byte[]) {
                            byte[][] slices = sliceArray((byte[])value, 1000);
                            for(byte[] slice : slices) {
                                InNode nextNode = new InNode(inNode.isNot());
                                nextNode.addChild(arg.deepCopy());
                                nextNode.addChild(new ValueNode(slice, valueNode.getAttribute()));
                                newChildren.add(nextNode);
                            }
                        }

                        ExpressionNodeBuilder exp = exp(() -> newChildren.get(0));
                        for(int i=1; i<newChildren.size(); i++) {
                            exp = exp.or(node(newChildren.get(i)));
                        }
                        parent.replaceChild(index, exp.build());

                        break;
                }
            }

            @Override
            public void onChildNodeEnd(Node parent, Node child, int index, boolean hasMore) {

            }

            @Override
            public void onNodeEnd(Node node) {

            }
        };

        node.visit(visitor);

        if(selectBuilder[0] != null) {
            return selectBuilder[0].build();
        }

        return node;
    }

    static int[][] sliceArray(int[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new int[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        int[][] result = new int[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new int[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static long[][] sliceArray(long[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new long[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        long[][] result = new long[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new long[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static float[][] sliceArray(float[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new float[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        float[][] result = new float[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new float[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static double[][] sliceArray(double[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new double[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        double[][] result = new double[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new double[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static short[][] sliceArray(short[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new short[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        short[][] result = new short[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new short[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static char[][] sliceArray(char[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new char[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        char[][] result = new char[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new char[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static boolean[][] sliceArray(boolean[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new boolean[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        boolean[][] result = new boolean[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new boolean[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static byte[][] sliceArray(byte[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new byte[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        byte[][] result = new byte[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new byte[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    static Object[][] sliceArray(Object[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new Object[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        Object[][] result = new Object[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new Object[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }
}
