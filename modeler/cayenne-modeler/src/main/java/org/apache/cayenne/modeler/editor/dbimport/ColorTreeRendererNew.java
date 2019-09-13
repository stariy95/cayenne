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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
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
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

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

        Status status = getStatus();
        setForeground(status.getColor());
        node.setColorized(status != Status.NONE);
        return this;
    }

    Status getStatus() {
        List<DbImportNode> path = getPath();
        FilterContainer config = reverseEngineeringTree.getReverseEngineering();

        for(DbImportNode segment : path) {
            Status status = segment.getType().getChecker().apply(segment, config);
            if(status == Status.EXCLUDED) {
                return status;
            }
        }

        return Status.NONE;
    }

    private List<DbImportNode> getPath() {
        List<DbImportNode> path = new ArrayList<>();
        path.add(toImportNode(node.getUserObject()));
        DbImportTreeNode parent = node.getParent();
        while (parent != null) {
            path.add(toImportNode(parent.getUserObject()));
            parent = parent.getParent();
        }
        Collections.reverse(path);
        return path;
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

    private enum Status {
        INCLUDED(new Color(60,179,113)),
        EXCLUDED(new Color(178, 0, 0)),
        NONE(Color.LIGHT_GRAY);
        private final Color color;
        Status(Color color) {
            this.color = color;
        }
        Color getColor() {
            return color;
        }
    }

    private enum ObjectType {
        UNKNOWN     ((node, config) -> Status.NONE),

        CATALOG     ((node, config) -> {
            if(config instanceof ReverseEngineering) {
                Collection<Catalog> catalogs = ((ReverseEngineering) config).getCatalogs();
                for(Catalog catalog : catalogs) {
                    if(catalog.getName().equals(node.getValue())) {
                        return Status.INCLUDED;
                    }
                }
                if(!catalogs.isEmpty()) {
                    return Status.EXCLUDED;
                }
            }
            return Status.NONE;
        }),

        SCHEMA      ((node, config) -> {
            if(config instanceof SchemaContainer) {
                Collection<Schema> schemas = ((SchemaContainer) config).getSchemas();
                for(Schema schema : schemas) {
                    if(schema.getName().equals(node.getValue())) {
                        return Status.INCLUDED;
                    }
                }
                if(!schemas.isEmpty()) {
                    return Status.EXCLUDED;
                }
            }
            return Status.NONE;
        }),

        TABLE       ((node, config) -> Status.NONE),

        COLUMN      ((node, config) -> Status.NONE),

        PROCEDURE   ((node, config) -> Status.NONE);

        private final BiFunction<DbImportNode, Object, Status> checker;

        ObjectType(BiFunction<DbImportNode, Object, Status> checker) {
            this.checker = checker;
        }

        public BiFunction<DbImportNode, Object, Status> getChecker() {
            return checker;
        }
    }
}
