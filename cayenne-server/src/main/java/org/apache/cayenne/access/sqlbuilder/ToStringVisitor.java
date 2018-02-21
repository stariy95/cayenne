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

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 */
public class ToStringVisitor implements NodeTreeVisitor {

    private static final Logger logger = LoggerFactory.getLogger(ToStringVisitor.class);

    private StringBuilder builder;

    private int level = 0;

    public ToStringVisitor() {
        builder = new StringBuilder();
    }

    @Override
    public void onNodeStart(Node node) {
        StringBuilder msg = new StringBuilder();
        for(int i=0; i<level; i++) {
            msg.append('\t');
        }
        msg.append("start node {}");
        logger.info(msg.toString(), node);
        level++;
        node.append(builder);
        node.appendChildrenStart(builder);
    }

    @Override
    public void onChildNodeEnd(Node node, int index, boolean hasMore) {
        if(hasMore && node.getParent() != null) {
            node.getParent().appendChildSeparator(builder);
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
