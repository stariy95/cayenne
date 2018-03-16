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

package org.apache.cayenne.dba.h2;

import java.util.function.Function;

import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.dba.derby.sqltree.DerbyLimitOffsetNode;

/**
 * @since 4.1
 */
public class H2SqlTreeProcessor implements Function<Node, Node> {

    @Override
    public Node apply(Node node) {
        NodeTreeVisitor visitor = new NodeTreeVisitor() {
            @Override
            public void onNodeStart(Node node) {
            }

            @Override
            public void onChildNodeStart(Node node, int index, boolean hasMore) {
                if(node.getType() == NodeType.LIMIT_OFFSET) {
                    Node replacement = new DerbyLimitOffsetNode((LimitOffsetNode)node);
                    node.getParent().replaceChild(index, replacement);
                }
            }

            @Override
            public void onChildNodeEnd(Node node, int index, boolean hasMore) {
            }

            @Override
            public void onNodeEnd(Node node) {
            }
        };

        node.visit(visitor);
        return node;
    }
}
