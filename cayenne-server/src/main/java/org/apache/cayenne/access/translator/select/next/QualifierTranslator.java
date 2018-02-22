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

import java.util.Collections;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.util.CayenneMapEntry;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.value;
import static org.apache.cayenne.exp.Expression.*;

/**
 * @since 4.1
 */
public class QualifierTranslator implements TraversalHandler {

    private final TranslatorContext context;

    private Node rootNode;

    private Node currentNode;

    public QualifierTranslator(TranslatorContext context) {
        this.context = context;
        rootNode = new EmptyNode();
        currentNode = rootNode;
    }

    NodeBuilder translate(Expression qualifier) {
        if(qualifier == null) {
            return null;
        }

        qualifier.traverse(this);
        return () -> rootNode;
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {

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
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case NEGATIVE:
            case OR:
            case AND:
            case IN:
            case BETWEEN:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(StringBuilder builder) {
                        builder.append(' ').append(expToStr(node.getType())).append(' ');
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
            case NOT_EQUAL_TO:
            case LESS_THAN:
            case LESS_THAN_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL_TO:
            case LIKE:
            case LIKE_IGNORE_CASE:
            case NOT_LIKE:
            case NOT_LIKE_IGNORE_CASE:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(StringBuilder builder) {
                        builder.append(' ').append(expToStr(node.getType())).append(' ');
                    }
                };

            case OBJ_PATH:
                return new Node() {
                    @Override
                    public void append(StringBuilder buffer) {
                        ASTObjPath pathNode = (ASTObjPath)node;
                        for (PathComponent<ObjAttribute, ObjRelationship> component
                                : context.getMetadata().getObjEntity().resolvePath(pathNode, Collections.emptyMap())) {
                            ObjRelationship relationship = component.getRelationship();
                            ObjAttribute attribute = component.getAttribute();

                            if (relationship != null) {
                                // if this is a last relationship in the path, it needs special handling
                                if (component.isLast()) {
//                                    processRelTermination(relationship, component.getJoinType(), joinSplitAlias);
                                } else {
                                    // find and add joins ....
                                    for (DbRelationship dbRel : relationship.getDbRelationships()) {
//                                        queryAssembler.dbRelationshipAdded(dbRel, component.getJoinType(), joinSplitAlias);
                                    }
                                }
                            } else {
                                Iterator<CayenneMapEntry> dbPathIterator = attribute.getDbPathIterator();
                                while (dbPathIterator.hasNext()) {
                                    Object pathPart = dbPathIterator.next();

                                    if (pathPart == null) {
                                        throw new CayenneRuntimeException("ObjAttribute has no component: %s", attribute.getName());
                                    } else if (pathPart instanceof DbRelationship) {
//                                        queryAssembler.dbRelationshipAdded((DbRelationship) pathPart, JoinType.INNER, joinSplitAlias);
                                    } else if (pathPart instanceof DbAttribute) {
//                                        processColumnWithQuoteSqlIdentifiers((DbAttribute) pathPart, pathExp);
                                    }
                                }

                            }
                        }
                        buffer.append(node.getOperand(0));
                    }
                };

            case DB_PATH:
                return new Node() {
                    @Override
                    public void append(StringBuilder buffer) {
                        buffer.append(node.getOperand(0));
                    }
                };
        }

        return new Node() {
            @Override
            public void append(StringBuilder buffer) {

            }
        };
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
                return "LIKE_IGNORE_CASE";
            case NOT_BETWEEN:
                return "NOT BETWEEN";
            case NOT_IN:
                return "NOT IN";
            case NOT_LIKE:
                return "NOT LIKE";
            case NOT_LIKE_IGNORE_CASE:
                return "NOT LIKE IGNORE CASE";
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
            default:
                return "{other}";
        }
    }
}
