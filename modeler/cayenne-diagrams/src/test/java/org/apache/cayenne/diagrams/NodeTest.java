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

package org.apache.cayenne.diagrams;

import org.junit.Test;

/**
 * @since 4.1
 */
public class NodeTest {

    @Test
    public void testDepth() {
        TreeParamsWalker walker = new TreeParamsWalker();

        Node node0 = new Node();
        Node node1 = new Node();
        node0.addChild(node0);
        node0.addChild(node1);
        node1.addChild(new Node());
        node1.addChild(new Node());

        Node node2 = new Node();
        node0.addChild(node2);

        Node node3 = new Node();
        node2.addChild(node3);
        node3.addChild(new Node());
        node3.addChild(new Node());
        node3.addChild(new Node());
        node3.addChild(node1);

        walker.walk(node0, n -> {});

        int depth = walker.getMaxDepth();
        int width = walker.getMaxWidth();

        System.out.println("Tree depth: " + depth + ", tree width: " + width);
    }


}