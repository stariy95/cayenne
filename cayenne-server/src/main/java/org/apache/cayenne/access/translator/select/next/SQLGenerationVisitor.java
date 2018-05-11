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

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeTreeVisitor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 */
public class SQLGenerationVisitor implements NodeTreeVisitor {

    // TODO: remove debug-related stuff
//    private static final Logger logger = LoggerFactory.getLogger(SQLGenerationVisitor.class);
//    private static final boolean debug = false;
//    private int level = 0;

    private final StringBuilderAppendable delegate;

    public SQLGenerationVisitor(TranslatorContext context) {
        if(context == null) {
            this.delegate = new StringBuilderAppendable(null);
        } else {
            this.delegate = new DefaultQuotingAppendable(context);
        }
    }

    @Override
    public boolean onNodeStart(Node node) {
//        if(debug) {
//            StringBuilder msg = new StringBuilder();
//            for (int i = 0; i < level; i++) {
//                msg.append('\t');
//            }
//            msg.append("start node {}");
//            logger.info(msg.toString(), node);
//            level++;
//        }
        node.append(delegate);
        node.appendChildrenStart(delegate);
        return true;
    }

    @Override
    public boolean onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
        return true;
    }

    @Override
    public void onChildNodeEnd(Node parent, Node child, int index, boolean hasMore) {
        if(hasMore && parent != null) {
            parent.appendChildrenSeparator(delegate, index);
        }
    }

    @Override
    public void onNodeEnd(Node node) {
        node.appendChildrenEnd(delegate);
//        if(debug) {
//            level--;
//        }
    }

    public String getSQLString() {
        return delegate.toString();
    }

}