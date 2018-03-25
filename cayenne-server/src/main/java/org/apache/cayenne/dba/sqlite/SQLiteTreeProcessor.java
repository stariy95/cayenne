package org.apache.cayenne.dba.sqlite;

import org.apache.cayenne.access.sqlbuilder.sqltree.ExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLimitOffsetNode;

import java.util.function.Function;

public class SQLiteTreeProcessor implements Function<Node, Node> {

    @Override
    public Node apply(Node node) {

        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {

            }

            @Override
            public void onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
                Node replacement = null;
                switch (child.getType()) {
                    case LIMIT_OFFSET:
                        LimitOffsetNode limitOffsetNode = (LimitOffsetNode)child;
                        replacement = new MysqlLimitOffsetNode(limitOffsetNode.getLimit(), limitOffsetNode.getOffset());
                        break;
                    case FUNCTION:
                        FunctionNode functionNode = (FunctionNode)child;
                        String functionName = functionNode.getFunctionName();
                        switch (functionName) {
                            case "SUBSTRING":
                                replacement = new FunctionNode("SUBSTR", functionNode.getAlias(), true);
                                break;
                            case "LOCATE":
                                replacement = new FunctionNode("INSTR", functionNode.getAlias(), true);
                                for (int i = 0; i <= 1; i++) {
                                    replacement.addChild(child.getChild(1 - i));
                                }
                                parent.replaceChild(index, replacement);
                                return;
                            case "CONCAT":
                                replacement = new ExpressionNode() {
                                    @Override
                                    public void appendChildSeparator(QuotingAppendable builder, int childInd) {
                                        builder.append("||");
                                    }
                                };
                                break;
                            case "MOD":
                                replacement = new ExpressionNode() {
                                    @Override
                                    public void appendChildSeparator(QuotingAppendable builder, int childInd) {
                                        builder.append("%");
                                    }
                                };
                                break;

                            case "CURRENT_DATE":
                            case "CURRENT_TIMESTAMP":
                            case "CURRENT_TIME":
                                replacement = new FunctionNode(functionName, functionNode.getAlias(), false);
                                break;

                            case "DAY_OF_YEAR":
                                replaceExtractFunction(parent, functionNode, index, "'%j'");
                                return;
                            case "DAY_OF_WEEK":
                                replaceExtractFunction(parent, functionNode, index, "'%w'");
                                return;
                            case "WEEK":
                                replaceExtractFunction(parent, functionNode, index, "'%W'");
                                return;
                            case "YEAR":
                                replaceExtractFunction(parent, functionNode, index, "'%Y'");
                                return;
                            case "MONTH":
                                replaceExtractFunction(parent, functionNode, index, "'%m'");
                                return;
                            case "DAY":
                            case "DAY_OF_MONTH":
                                replaceExtractFunction(parent, functionNode, index, "'%d'");
                                return;
                            case "HOUR":
                                replaceExtractFunction(parent, functionNode, index, "'%H'");
                                return;
                            case "MINUTE":
                                replaceExtractFunction(parent, functionNode, index, "'%M'");
                                return;
                            case "SECOND":
                                replaceExtractFunction(parent, functionNode, index, "'%S'");
                                return;
                        }
                        break;
                }

                if(replacement != null) {
                    for(int i=0; i<child.getChildrenCount(); i++) {
                        replacement.addChild(child.getChild(i));
                    }
                    parent.replaceChild(index, replacement);
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

        return node;
    }

    private void replaceExtractFunction(Node parent, FunctionNode original, int index, String format) {
        Node replacement = new FunctionNode("cast", original.getAlias(), true) {
            @Override
            public void appendChildSeparator(QuotingAppendable builder, int childIdx) {
                builder.append(" as ");
            }
        };

        FunctionNode strftime = new FunctionNode("strftime", null, true);
        strftime.addChild(new TextNode(format));
        strftime.addChild(original.getChild(0));
        replacement.addChild(strftime);
        replacement.addChild(new TextNode("integer"));

        parent.replaceChild(index, replacement);
    }
}
