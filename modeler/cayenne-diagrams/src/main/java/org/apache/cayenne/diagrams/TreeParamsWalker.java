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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Node tree walker that calculates tree depth and width.
 *
 * @since 4.1
 */
public class TreeParamsWalker implements NodeTreeWalker {

    /**
     * Current depth
     */
    private int depth;

    /**
     * Max depth of tree starting from first node
     */
    private int maxDepth;

    /**
     * Define width per depth (total nodes on depth + unrelated components)
     */
    private Map<Integer, Width> width = new HashMap<>();

    private Map<Integer, List<Node>> nodePerDepth = new HashMap<>();

    /**
     * Mark visited to ignore cycles
     */
    private Set<Node> visited = new HashSet<>();

    @Override
    public void walk(Node node, Consumer<Node> consumer) {

        // add visited now to evade self-references
        visited.add(node);
        consumer.accept(node);

        nodePerDepth.computeIfAbsent(depth, d -> new ArrayList<>()).add(node);

        List<Node> children = node.getChildren().stream()
                .filter(c -> visited.add(c)).collect(Collectors.toList());
        int count = children.size();
        if(count > 0) {
            depth++;
            if (depth > maxDepth) {
                maxDepth = depth;
            }
            width.computeIfAbsent(depth, d -> new Width()).add(count);
            children.forEach(c -> walk(c, consumer));
            depth--;
        }
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getWidth(int level) {
        return width.getOrDefault(level, new Width()).getTotal();
    }

    public int getMaxWidth() {
        return width.values().stream()
                .max(Comparator.comparingInt(Width::getTotal))
                .orElse(new Width())
                .getTotal();
    }

    public List<Node> getNodes(int level) {
        return nodePerDepth.get(level);
    }

    static private class Width {
        private int total;
        private int components;

        public int getTotal() {
            return total;
        }

        void add(int count) {
            components++;
            total += count;
        }
    }
}
