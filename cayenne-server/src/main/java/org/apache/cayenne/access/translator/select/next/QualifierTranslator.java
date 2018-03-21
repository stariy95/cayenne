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

import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.sqlbuilder.ColumnNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.apache.cayenne.exp.Expression.*;

/**
 * @since 4.1
 */
class QualifierTranslator implements TraversalHandler {

    private final TranslatorContext context;
    private final PathTranslator pathTranslator;

    private Set<Object> expressionsToSkip;
    private Node currentNode;

    private boolean forceJoin;
    private String topLevelAlias;

    QualifierTranslator(TranslatorContext context) {
        this.context = context;
        this.pathTranslator = new PathTranslator(context);
    }

    Node translate(Property<?> property) {
        if(property == null) {
            return null;
        }

        topLevelAlias = property.getAlias();
        Node result = translate(property.getExpression());
        topLevelAlias = null;
        return result;
    }

    Node translate(Expression qualifier) {
        if(qualifier == null) {
            return null;
        }

        Node rootNode = new EmptyNode();
        this.currentNode = rootNode;
        this.expressionsToSkip = new HashSet<>();

        qualifier.traverse(this);
        return rootNode;
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {
        if(expressionsToSkip.contains(node) || expressionsToSkip.contains(parentNode)) {
            return;
        }
        Node nextNode = expressionNodeToSqlNode(node, parentNode);
        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
        currentNode = nextNode;
    }

    private Node expressionNodeToSqlNode(Expression node, Expression parentNode) {
        switch (node.getType()) {
            case NOT_IN:
                return new InNode(true);
            case IN:
                return new InNode(false);

            case NOT_BETWEEN:
            case BETWEEN:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
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
                    public void append(QuotingAppendable buffer) {
                        buffer.append("NOT");
                    }
                };

            case BITWISE_NOT:
                return new ExpressionNode() {
                    @Override
                    public void append(QuotingAppendable buffer) {
                        buffer.append("!");
                    }
                };

            case EQUAL_TO:
                return new EqualNode();
            case NOT_EQUAL_TO:
                return new NotEqualNode();

            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case NEGATIVE:
            case BITWISE_AND:
            case BITWISE_LEFT_SHIFT:
            case BITWISE_OR:
            case BITWISE_RIGHT_SHIFT:
            case BITWISE_XOR:
            case OR:
            case AND:
            case LESS_THAN:
            case LESS_THAN_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL_TO:
                return new ExpressionNode() {
                    @Override
                    public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
                        builder.append(' ').append(expToStr(node.getType())).append(' ');
                    }
                };

            case LIKE:
            case NOT_LIKE:
            case LIKE_IGNORE_CASE:
            case NOT_LIKE_IGNORE_CASE:
                PatternMatchNode patternMatchNode = (PatternMatchNode)node;
                boolean not = node.getType() == NOT_LIKE || node.getType() == NOT_LIKE_IGNORE_CASE;
                return new LikeNode(patternMatchNode.isIgnoringCase(), not, patternMatchNode.getEscapeChar());

            case OBJ_PATH:
                String path = (String)node.getOperand(0);
                PathTranslator.PathTranslationResult result = pathTranslator.translatePath(context.getMetadata().getObjEntity(), path);
                return processPathTranslationResult(node, parentNode, result);

            case DB_PATH:
                String dbPath = (String)node.getOperand(0);
                PathTranslator.PathTranslationResult dbResult = pathTranslator.translatePath(context.getMetadata().getDbEntity(), dbPath);
                return processPathTranslationResult(node, parentNode, dbResult);

            case TRUE:
            case FALSE:
                return new TextNode(expToStr(node.getType()));

            case FUNCTION_CALL:
                ASTFunctionCall functionCall = (ASTFunctionCall)node;
                String alias = topLevelAlias;
                topLevelAlias = null;
                return function(functionCall.getFunctionName()).as(alias).build();

            case ASTERISK:
                return new TextNode("*");
        }

        return new EmptyNode();
    }

    private Node processPathTranslationResult(Expression node, Expression parentNode, PathTranslator.PathTranslationResult result) {
        StringBuilder path = new StringBuilder(result.getFinalPath());
        DbAttribute[] lastDbAttribute = new DbAttribute[]{result.getLastAttribute()};
        result.getDbRelationship().ifPresent(r -> {
            if(r.isToMany() || !r.isToPK() || forceJoin) {
                context.getTableTree().addJoinTable(path.toString(), r, JoinType.LEFT_OUTER);
                path.append('.').append(lastDbAttribute[0].getName());
            }
        });

        if(result.getDbAttributes().size() > 1) {
            return createMultiAttributeMatch(node, parentNode, result);
        } else if(result.getDbAttributes().isEmpty()) {
            return new EmptyNode();
        } else {
            String alias = topLevelAlias;
            topLevelAlias = null;
            ColumnNodeBuilder column = table(context.getTableTree().aliasForAttributePath(path.toString()))
                    .column(lastDbAttribute[0]).as(alias);
            return column.build();
        }
    }

    private Node createMultiAttributeMatch(Expression node, Expression parentNode, PathTranslator.PathTranslationResult result) {
        DbRelationship relationship = result.getDbRelationship().orElseThrow(() -> new CayenneRuntimeException("No relationship found"));
        ObjectId objectId = null;
        expressionsToSkip.add(node);
        expressionsToSkip.add(parentNode);

        int siblings = parentNode.getOperandCount();
        for(int i=0; i<siblings; i++) {
            Object operand = parentNode.getOperand(i);
            if(node == operand) {
                continue;
            }

            if(operand instanceof Persistent) {
                objectId = ((Persistent) operand).getObjectId();
                break;
            } else if(operand instanceof ObjectId) {
                objectId = (ObjectId)operand;
                break;
            } else if(operand instanceof ASTObjPath) {
                // TODO: support comparision of multi attribute ObjPath with other multi attribute ObjPath
                throw new UnsupportedOperationException("Comparision of multiple attributes not supported for ObjPath");
            }
        }

        if(objectId == null) {
            throw new CayenneRuntimeException("Multi attribute ObjPath isn't matched with valid value. " +
                    "List or Persistent object required.");
        }

        ExpressionNodeBuilder expressionNodeBuilder = null;
        ExpressionNodeBuilder eq;

        if(relationship.isToMany()) {
            String alias = context.getTableTree().aliasForPath(relationship.getName());
            for (DbAttribute attribute : relationship.getTargetEntity().getPrimaryKeys()) {
                Object nextValue = objectId.getIdSnapshot().get(attribute.getName());
                eq = table(alias).column(attribute).eq(value(nextValue));
                if (expressionNodeBuilder == null) {
                    expressionNodeBuilder = eq;
                } else {
                    expressionNodeBuilder = expressionNodeBuilder.and(eq);
                }
            }
        } else {
            String path = (String)node.getOperand(0);
            String alias = context.getTableTree().aliasForAttributePath(path);
            for (DbJoin join : relationship.getJoins()) {
                Object nextValue = objectId.getIdSnapshot().get(join.getTargetName());
                eq = table(alias).column(join.getSource()).eq(value(nextValue));
                if (expressionNodeBuilder == null) {
                    expressionNodeBuilder = eq;
                } else {
                    expressionNodeBuilder = expressionNodeBuilder.and(eq);
                }
            }
        }

        return expressionNodeBuilder.build();
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        if(currentNode.getParent() != null) {
            currentNode = currentNode.getParent();
        }
    }

    @Override
    public void objectNode(Object leaf, Expression parentNode) {
        if(expressionsToSkip.contains(parentNode)) {
            return;
        }
        if(parentNode.getType() == Expression.OBJ_PATH || parentNode.getType() == Expression.DB_PATH) {
            return;
        }

        Node nextNode = value(leaf).attribute(findDbAttribute(parentNode)).build();

        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
    }

    protected DbAttribute findDbAttribute(Expression node) {
        int len = node.getOperandCount();
        if (len != 2) {
            if (node instanceof SimpleNode) {
                Expression parent = (Expression) ((SimpleNode) node).jjtGetParent();
                if (parent != null) {
                    node = parent;
                } else {
                    return null;
                }
            }
        }

        PathTranslator.PathTranslationResult result = null;
        for(int i=0; i<node.getOperandCount(); i++) {
            Object op = node.getOperand(i);
            // TODO: here is double translation of paths we already saw or going to translate soon
            if(op instanceof ASTObjPath) {
                result = pathTranslator.translatePath(context.getMetadata().getObjEntity(), ((ASTObjPath) op).getPath());
                break;
            } else if(op instanceof ASTDbPath) {
                result = pathTranslator.translatePath(context.getMetadata().getDbEntity(), ((ASTDbPath) op).getPath());
                break;
            }
        }

        if(result == null) {
            return null;
        }

        return result.getLastAttribute();
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
    }

    public void setForceJoin(boolean forceJoin) {
        this.forceJoin = forceJoin;
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
            case BITWISE_AND:
                return "&";
            case BITWISE_OR:
                return "|";
            case BITWISE_XOR:
                return "^";
            case BITWISE_NOT:
                return "!";
            case BITWISE_LEFT_SHIFT:
                return "<<";
            case BITWISE_RIGHT_SHIFT:
                return ">>";
            default:
                return "{other}";
        }
    }

    private static class EqualNode extends ExpressionNode {
        @Override
        public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
            String expStr = " = ";
            Node child = getChild(1);
            if(child.getType() == NodeType.VALUE && ((ValueNode)child).getValue() == null) {
                expStr = " IS NULL";
            }
            builder.append(expStr);
        }

        @Override
        public NodeType getType() {
            return NodeType.EQUALITY;
        }
    }

    private static class NotEqualNode extends ExpressionNode {
        @Override
        public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
            String expStr = " <> ";
            Node child = getChild(1);
            if(child.getType() == NodeType.VALUE && ((ValueNode)child).getValue() == null) {
                expStr = " IS NOT NULL";
            }
            builder.append(expStr);
        }

        @Override
        public NodeType getType() {
            return NodeType.EQUALITY;
        }
    }

    private class InNode extends Node {
        private final boolean not;

        public InNode(boolean not) {
            this.not = not;
        }

        @Override
        public void append(QuotingAppendable buffer) {
        }

        @Override
        public void appendChildSeparator(QuotingAppendable builder, int childInd) {
            if(childInd == 0) {
                builder.append(' ');
                if(not) {
                    builder.append("NOT ");
                }
                builder.append("IN (");
            }
        }

        @Override
        public void appendChildrenEnd(QuotingAppendable builder) {
            builder.append(')');
        }

        @Override
        public Node copy() {
            return new InNode(not);
        }
    }
}
