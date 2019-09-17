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

        PathCheckResult checkResult = check();
        setForeground(checkResult.getStatus().getColor());
        node.setColorized(checkResult.getStatus() != Status.NONE);
        return this;
    }

    PathCheckResult check() {
        List<DbImportNode> path = getPath();
        FilterContainer config = reverseEngineeringTree.getReverseEngineering();

        PathCheckResult result = PathCheckResult.none(config);
        for(DbImportNode segment : path) {
            result = segment.getType().getChecker().apply(segment, result.getNextStep());
            if(result.getStatus() == Status.EXCLUDED) {
                return result;
            }
        }

        return result;
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

    private static class Node {
        private Node parent;

        Node getParent() {
            return parent;
        }

        boolean isExcluded() {
            return false;
        }
    }

    private static class CatalogNode extends Node {

    }

    private static class SchemaNode extends Node {

    }

    private static class TableNode extends Node {

    }

    private static class ColumnNode extends Node {

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
        INCLUDED    (new Color(60,179,113)),
        EXCLUDED    (new Color(178, 0, 0)),
        UNKNOWN     (Color.BLUE), // TODO: BLUE for debug only, change to LIGHT_GRAY
        NONE        (Color.LIGHT_GRAY);
        private final Color color;
        Status(Color color) {
            this.color = color;
        }
        Color getColor() {
            return color;
        }
    }

    private static class PathCheckResult {

        private static final PathCheckResult EXCLUDED
                = new PathCheckResult(Status.EXCLUDED, null);

        private static final PathCheckResult UNKNOWN
                = new PathCheckResult(Status.UNKNOWN, null);

        private final Status status;
        private final Object nextStep;

        static PathCheckResult excluded() {
            return EXCLUDED;
        }

        static PathCheckResult none(Object next) {
            return new PathCheckResult(Status.NONE, next);
        }

        static PathCheckResult included(Object next) {
            return new PathCheckResult(Status.INCLUDED, next);
        }

        static PathCheckResult unknown() {
            return UNKNOWN;
        }

        private PathCheckResult(Status status, Object nextStep) {
            this.status = status;
            this.nextStep = nextStep;
        }

        public Object getNextStep() {
            return nextStep;
        }

        public Status getStatus() {
            return status;
        }
    }

    private enum ObjectType {
        UNKNOWN     ((node, config) -> PathCheckResult.unknown()),

        CATALOG     ((node, config) -> {
            if(config instanceof ReverseEngineering) {
                Collection<Catalog> catalogs = ((ReverseEngineering) config).getCatalogs();
                for(Catalog catalog : catalogs) {
                    if(catalog.getName().equals(node.getValue())) {
                        return PathCheckResult.included(catalog);
                    }
                }
                if(!catalogs.isEmpty()) {
                    return PathCheckResult.none(config);
                } else {
                    return PathCheckResult.included(config);
                }
            }
            return PathCheckResult.unknown();
        }),

        SCHEMA      ((node, config) -> {
            if(config instanceof SchemaContainer) {
                Collection<Schema> schemas = ((SchemaContainer) config).getSchemas();
                for(Schema schema : schemas) {
                    if(schema.getName().equals(node.getValue())) {
                        return PathCheckResult.included(schema);
                    }
                }
                if(!schemas.isEmpty()) {
                    return PathCheckResult.none(config);
                } else {
                    return PathCheckResult.included(config);
                }
            }
            return PathCheckResult.unknown();
        }),

        TABLE       ((node, config) -> {
            if(config instanceof FilterContainer) {
                Collection<IncludeTable> includeTables = ((FilterContainer) config).getIncludeTables();
                for(IncludeTable includeTable : includeTables) {
                    if(node.getValue().matches(includeTable.getPattern())) {
                        return PathCheckResult.included(includeTable);
                    }
                }
                if(!includeTables.isEmpty()) {
                    return PathCheckResult.none(config);
                }
                Collection<ExcludeTable> excludeTables = ((FilterContainer) config).getExcludeTables();
                for(ExcludeTable excludeTable : excludeTables) {
                    if(node.getValue().matches(excludeTable.getPattern())) {
                        return PathCheckResult.excluded();
                    }
                }
                if(!excludeTables.isEmpty()) {
                    return PathCheckResult.included(config);
                }

                return PathCheckResult.included(config);
            }
            return PathCheckResult.unknown();
        }),

        COLUMN      ((node, config) -> PathCheckResult.unknown()),

        PROCEDURE   ((node, config) -> PathCheckResult.unknown());

        private final BiFunction<DbImportNode, Object, PathCheckResult> checker;

        ObjectType(BiFunction<DbImportNode, Object, PathCheckResult> checker) {
            this.checker = checker;
        }

        public BiFunction<DbImportNode, Object, PathCheckResult> getChecker() {
            return checker;
        }
    }
}
