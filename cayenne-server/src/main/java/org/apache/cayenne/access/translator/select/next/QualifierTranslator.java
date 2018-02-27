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

import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.map.DbAttribute;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.apache.cayenne.exp.Expression.*;

/**
 * @since 4.1
 */
public class QualifierTranslator implements TraversalHandler {

    private final TranslatorContext context;
    private final PathTranslator pathTranslator;

    private Node rootNode;
    private Node currentNode;

    public QualifierTranslator(TranslatorContext context) {
        this.context = context;
        this.pathTranslator = new PathTranslator(context);
        this.rootNode = new EmptyNode();
        this.currentNode = rootNode;
    }

    NodeBuilder translate(Expression qualifier) {
        if(qualifier == null) {
            return null;
        }

        qualifier.traverse(this);
        return () -> rootNode;
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {
        Node nextNode = expressionNodeToSqlNode(node);
        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
        currentNode = nextNode;
    }

    private Node expressionNodeToSqlNode(Expression node) {
        switch (node.getType()) {
            case NOT_IN:
            case IN:
                return new Node() {
                    @Override
                    public void append(StringBuilder buffer) {
                    }

                    @Override
                    public void appendChildSeparator(StringBuilder builder, int childInd) {
                        if(childInd == 0) {
                            builder.append(' ').append(expToStr(node.getType())).append(" (");
                        }
                    }

                    @Override
                    public void appendChildrenEnd(StringBuilder builder) {
                        builder.append(')');
                    }
                };

            case NOT_BETWEEN:
            case BETWEEN:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(StringBuilder builder, int childIdx) {
                        if(childIdx == 0) {
                            builder.append(expToStr(node.getType())).append(' ');
                        } else {
                            builder.append(" AND ");
                        }
                    }
                };

            case NOT:
                return new ExpressionNode() {
                    @Override
                    public void append(StringBuilder buffer) {
                        buffer.append("NOT");
                    }
                };

            case EQUAL_TO:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(StringBuilder builder, int childIdx) {
                        String expStr = " = ";
                        if(node.getOperand(1) == null) {
                            expStr = " IS NULL";
                        }
                        builder.append(expStr);
                    }
                };
            case NOT_EQUAL_TO:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(StringBuilder builder, int childIdx) {
                        String expStr = " <> ";
                        if(node.getOperand(1) == null) {
                            expStr = " IS NOT NULL";
                        }
                        builder.append(expStr);
                    }
                };

            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case NEGATIVE:
            case OR:
            case AND:
            case LESS_THAN:
            case LESS_THAN_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL_TO:
            case LIKE:
            case NOT_LIKE:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(StringBuilder builder, int childIdx) {
                        builder.append(' ').append(expToStr(node.getType())).append(' ');
                    }
                };


            case LIKE_IGNORE_CASE:
            case NOT_LIKE_IGNORE_CASE:
                return new ExpressionNode() {
                    @Override
                    public void append(StringBuilder buffer) {
                    }

                    @Override
                    public void appendChildrenStart(StringBuilder builder) {
                        builder.append("UPPER(");
                    }

                    @Override
                    public void appendChildSeparator(StringBuilder builder, int childIdx) {
                        builder.append(") ").append(expToStr(node.getType())).append(" UPPER(");
                    }

                    @Override
                    public void appendChildrenEnd(StringBuilder builder) {
                        builder.append(")");
                    }
                };

            case OBJ_PATH:
                String path = (String)node.getOperand(0);
                PathTranslator.PathTranslationResult result = pathTranslator.translatePath(context.getMetadata().getObjEntity(), path);
                DbAttribute lastAttribute = result.getDbAttributes().get(result.getDbAttributes().size() - 1);
                return table(context.getTableTree().aliasForAttributePath(path)).column(lastAttribute.getName()).build();

            case DB_PATH:
                // TODO proper db path translation
                String dbPath = (String)node.getOperand(0);
                String attr = dbPath;
                return table(context.getTableTree().aliasForAttributePath(dbPath)).column(attr).build();

            case TRUE:
            case FALSE:
                return new Node() {
                    @Override
                    public void append(StringBuilder buffer) {
                        buffer.append(expToStr(node.getType()));
                    }
                };


        }

        return new EmptyNode();
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        if(currentNode.getParent() != null) {
            currentNode = currentNode.getParent();
        }
    }

    @Override
    public void objectNode(Object leaf, Expression parentNode) {
        if(parentNode.getType() == Expression.OBJ_PATH || parentNode.getType() == Expression.DB_PATH) {
            return;
        }
        Node nextNode = value(leaf).build();
        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
    }

    private String expToStr(int type) {
        switch (type) {
            case AND:
                return "AND";
            case OR:
                return "OR";
            case NOT:
                return "NOT";
            case EQUAL_TO:
                return "=";
            case NOT_EQUAL_TO:
                return "<>";
            case LESS_THAN:
                return "<";
            case LESS_THAN_EQUAL_TO:
                return "<=";
            case GREATER_THAN:
                return ">";
            case GREATER_THAN_EQUAL_TO:
                return ">=";
            case BETWEEN:
                return "BETWEEN";
            case IN:
                return "IN";
            case LIKE:
                return "LIKE";
            case LIKE_IGNORE_CASE:
                return "LIKE";
            case NOT_BETWEEN:
                return "NOT BETWEEN";
            case NOT_IN:
                return "NOT IN";
            case NOT_LIKE:
                return "NOT LIKE";
            case NOT_LIKE_IGNORE_CASE:
                return "NOT LIKE";
            case ADD:
                return "+";
            case SUBTRACT:
                return "-";
            case MULTIPLY:
                return "*";
            case DIVIDE:
                return "/";
            case NEGATIVE:
                return "-";
            case TRUE:
                return "1=1";
            case FALSE:
                return "1=0";
            default:
                return "{other}";
        }
    }
}
