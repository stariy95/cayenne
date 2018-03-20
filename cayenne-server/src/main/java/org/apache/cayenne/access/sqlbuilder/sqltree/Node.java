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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.translator.select.next.QuotingAppendable;
import org.apache.cayenne.access.translator.select.next.StringBuilderAppendable;

/**
 * @since 4.1
 */
public abstract class Node {

    protected Node parent;

    protected List<Node> children;

    protected NodeType type = NodeType.UNDEFINED;

    public Node() {
        this.children = new ArrayList<>(2);
    }

    public Node addChild(Node node) {
        children.add(node);
        node.setParent(this);
        return this;
    }

    public Node getChild(int idx) {
        return children.get(idx);
    }

    public int getChildrenCount() {
        return children.size();
    }

    public void removeChild(int idx) {
        children.remove(idx).setParent(null);
    }

    public void replaceChild(int idx, Node node) {
        children.set(idx, node).setParent(null);
        node.setParent(this);
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void visit(NodeTreeVisitor visitor) {
        visitor.onNodeStart(this);
        int count = getChildrenCount();
        for(int i=0; i<count; i++) {
            visitor.onChildNodeStart(this, getChild(i), i, i < (count - 1));
            getChild(i).visit(visitor);
            visitor.onChildNodeEnd(this, getChild(i), i, i < (count - 1));
        }
        visitor.onNodeEnd(this);
    }

    @Override
    public String toString() {
        QuotingAppendable sb = new StringBuilderAppendable(null);
        append(sb);
        return "Node {" + sb.toString() + "}";
    }

    public NodeType getType() {
        return type;
    }

    public abstract void append(QuotingAppendable buffer);

    public void appendChildSeparator(QuotingAppendable builder, int childInd) {
        builder.append(' ');
    }

    public void appendChildrenStart(QuotingAppendable builder) {
        builder.append(' ');
    }

    public void appendChildrenEnd(QuotingAppendable builder) {
    }
}
