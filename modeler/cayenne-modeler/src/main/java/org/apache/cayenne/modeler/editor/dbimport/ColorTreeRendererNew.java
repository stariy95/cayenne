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

package org.apache.cayenne.modeler.editor.dbimport;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTree;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.CatalogNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.CatalogTableNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.ColumnNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.Node;
import org.apache.cayenne.modeler.editor.dbimport.tree.SchemaNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.SchemaTableNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.Status;
import org.apache.cayenne.modeler.editor.dbimport.tree.TableNode;

/**
 * @since 4.1
 */
public class ColorTreeRendererNew extends DbImportTreeCellRenderer {

    private DbImportTree reverseEngineeringTree;

    public ColorTreeRendererNew() {
        super();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (this.node.isLabel() || this.selected) {
            setForeground(Color.BLACK);
            return this;
        }

        Node<?> path = getLogicalTreeNode();
        if(path == null) {
            return this;
        }
        Status status = path.getStatus(reverseEngineeringTree.getReverseEngineering());
        setForeground(status.getColor());
        node.setColorized(status != Status.NONE);
        return this;
    }

    private Node<?> getLogicalTreeNode() {
        List<DbImportNode> path = new ArrayList<>();
        path.add(toImportNode(node.getUserObject()));
        DbImportTreeNode parent = node.getParent();
        while (parent != null) {
            path.add(toImportNode(parent.getUserObject()));
            parent = parent.getParent();
        }
        Collections.reverse(path);

        Node<?> logicalParent = null;
        for(DbImportNode node : path) {
            logicalParent = toLogicalNode(node, logicalParent);
        }
        return logicalParent;
    }

    private Node<?> toLogicalNode(DbImportNode node, Node<?> logicalParent) {
        switch (node.getType()) {
            case CATALOG:
                return new CatalogNode(node.getValue());
            case SCHEMA:
                return new SchemaNode(node.getValue(), (CatalogNode)logicalParent);
            case TABLE:
                if(logicalParent instanceof CatalogNode) {
                    return new CatalogTableNode(node.getValue(), (CatalogNode)logicalParent);
                } else {
                    return new SchemaTableNode(node.getValue(), (SchemaNode)logicalParent);
                }
            case COLUMN:
                return new ColumnNode(node.getValue(), (TableNode<?>) logicalParent);
            case PROCEDURE:
                return null;
            default:
                return null;
        }
    }

    private DbImportNode toImportNode(Object object) {
        return new DbImportNode(getObjectType(object), getObjectValue(object));
    }

    private ObjectType getObjectType(Object object) {
        if(object instanceof Catalog) {
            return ObjectType.CATALOG;
        } else if(object instanceof Schema) {
            return ObjectType.SCHEMA;
        } else if(object instanceof IncludeTable || object instanceof ExcludeTable) {
            return ObjectType.TABLE;
        } else if(object instanceof IncludeColumn || object instanceof ExcludeColumn) {
            return ObjectType.COLUMN;
        } else if(object instanceof IncludeProcedure || object instanceof ExcludeProcedure) {
            return ObjectType.PROCEDURE;
        }
        return ObjectType.UNKNOWN;
    }

    private String getObjectValue(Object object) {
        if(object instanceof FilterContainer) {
            return ((FilterContainer) object).getName();
        } else if(object instanceof PatternParam) {
            return ((PatternParam) object).getPattern();
        }
        return "";
    }

    public void setReverseEngineeringTree(DbImportTree reverseEngineeringTree) {
        this.reverseEngineeringTree = reverseEngineeringTree;
    }

    // TODO: use as a UserObject directly
    private static class DbImportNode {
        private final ObjectType type;
        private final String value;

        private DbImportNode(ObjectType type, String value) {
            this.type = type;
            this.value = value;
        }

        ObjectType getType() {
            return type;
        }

        String getValue() {
            return value;
        }
    }

    private enum ObjectType {
        UNKNOWN,
        CATALOG,
        SCHEMA,
        TABLE,
        COLUMN,
        PROCEDURE;
    }
}
