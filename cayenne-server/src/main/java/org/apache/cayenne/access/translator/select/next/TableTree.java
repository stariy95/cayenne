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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.1
 */
class TableTree {
    /**
     * Tables mapped by db path it's spawned by.
     * Can be following:
     * - query root table
     * - tables from flattened attributes (including all intermediate tables)
     * - tables from attributes used in expressions (WHERE, HAVING, ORDER BY)
     * - tables from prefetches
     */
    private Map<String, TableTreeNode> tableNodes;

    private TableTreeNode rootNode;

    private int tableAliasSequence;

    TableTree(int tableAliasSequence) {
        this.tableAliasSequence = tableAliasSequence;
        this.tableNodes = new HashMap<>();
    }

    void addRootTable(DbEntity root) {
        rootNode = new TableTreeNode();
        rootNode.attributePath = "";
        rootNode.entity = root;
        rootNode.tableAlias = nextTableAlias();
    }

    void addJoinTable(String path, DbRelationship relationship, JoinType joinType) {
        if (tableNodes.get(path) != null) {
            return;
        }

        TableTreeNode node = new TableTreeNode();
        node.attributePath = path;
        node.entity = relationship.getTargetEntity();
        node.tableAlias = nextTableAlias();
        node.relationship = relationship;
        node.joinType = joinType;

        tableNodes.put(path, node);
    }

    String aliasForAttributePath(String attributePath) {
        int lastSeparator = attributePath.lastIndexOf('.');
        if (lastSeparator == -1) {
            return rootNode.tableAlias;
        }
        String table = attributePath.substring(0, lastSeparator);
        TableTreeNode node = tableNodes.get(table);
        if (node == null) {
            throw new CayenneRuntimeException("No table for attribute '%s' found", attributePath);
        }
        return node.tableAlias;
    }

    String nextTableAlias() {
        return 't' + String.valueOf(tableAliasSequence++);
    }

    public int getNodeCount() {
        return tableNodes.size() + 1;
    }

    public void visit(TableNodeVisitor visitor) {
        visitor.visit(rootNode);
        for(TableTreeNode node : tableNodes.values()) {
            visitor.visit(node);
        }
    }

    @FunctionalInterface
    interface TableNodeVisitor {

        void visit(TableTreeNode node);

    }
}
