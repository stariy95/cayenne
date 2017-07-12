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

package org.apache.cayenne.modeler.dialog.db.load;

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

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @since 4.1
 */
public class DefaultPopUpMenu extends JPopupMenu {

    protected JMenuItem rename;
    protected JMenuItem delete;
    protected DbImportTreeNode selectedElement;
    protected DbImportTreeNode parentElement;
    protected JTree tree;

    public DefaultPopUpMenu() {
        rename = new JMenuItem("Rename");
        delete = new JMenuItem("Delete");
        this.add(rename);
        this.add(delete);
        initListeners();
    }

    private void removePatternParams(FilterContainer container, Object selectedObject) {
        container.getExcludeTables().remove(selectedObject);
        container.getIncludeColumns().remove(selectedObject);
        container.getExcludeColumns().remove(selectedObject);
        container.getIncludeProcedures().remove(selectedObject);
        container.getExcludeProcedures().remove(selectedObject);
    }

    private void deleteChilds(Catalog catalog) {
        Object selectedObject = this.selectedElement.getUserObject();
        if (selectedObject instanceof Schema) {
            catalog.getSchemas().remove(selectedObject);
        } else if (selectedObject instanceof IncludeTable) {
            catalog.getIncludeTables().remove(selectedObject);
        } else if (selectedObject instanceof PatternParam) {
            removePatternParams(catalog, selectedObject);
        }
    }

    private void deleteChilds(Schema schema) {
        Object selectedObject = this.selectedElement.getUserObject();
        if (selectedObject instanceof IncludeTable) {
            schema.getIncludeTables().remove(selectedObject);
        } else if (selectedObject instanceof PatternParam) {
            removePatternParams(schema, selectedObject);
        }
    }

    private void deleteChilds(IncludeTable includeTable) {
        Object selectedObject = this.selectedElement.getUserObject();
        includeTable.getIncludeColumns().remove(selectedObject);
        includeTable.getExcludeColumns().remove(selectedObject);
    }

    private void deleteChilds(ReverseEngineering reverseEngineering) {
        Object selectedObject = this.selectedElement.getUserObject();
        if (selectedObject instanceof Catalog) {
            reverseEngineering.getCatalogs().remove(selectedObject);
        } else if (selectedObject instanceof Schema) {
            reverseEngineering.getSchemas().remove(selectedObject);
        } else if (selectedObject instanceof IncludeTable) {
            reverseEngineering.getIncludeTables().remove(selectedObject);
        } else if (selectedObject instanceof ExcludeTable) {
            reverseEngineering.getExcludeTables().remove(selectedObject);
        } else if (selectedObject instanceof IncludeColumn) {
            reverseEngineering.getIncludeColumns().remove(selectedObject);
        } else if (selectedObject instanceof ExcludeColumn) {
            reverseEngineering.getExcludeColumns().remove(selectedObject);
        } else if (selectedObject instanceof IncludeProcedure) {
            reverseEngineering.getIncludeProcedures().remove(selectedObject);
        } else if (selectedObject instanceof ExcludeProcedure) {
            reverseEngineering.getExcludeProcedures().remove(selectedObject);
        }
    }

    private void removeFromParent() {
        Object parentUserObject = parentElement.getUserObject();
        if (parentUserObject instanceof ReverseEngineering) {
            ReverseEngineering reverseEngineering = (ReverseEngineering) parentUserObject;
            deleteChilds(reverseEngineering);
        } else if (parentUserObject instanceof Catalog) {
            Catalog catalog = (Catalog) parentUserObject;
            deleteChilds(catalog);
        } else if (parentUserObject instanceof Schema) {
            Schema schema  = (Schema) parentUserObject;
            deleteChilds(schema);
        } else if (parentUserObject instanceof IncludeTable) {
            IncludeTable includeTable = (IncludeTable) parentUserObject;
            deleteChilds(includeTable);
        }
    }

    protected String getNewName(String oldValue) {
        String name = JOptionPane.showInputDialog(this, "Name:", oldValue != null ? oldValue : "");
        return name != null ? name : "";
    }

    protected String getNewName() {
        String name = JOptionPane.showInputDialog(this, "Name:");
        return name != null ? name : "";
    }

    protected void updateModel() {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.reload(selectedElement);
    }

    private void renameElement() {
        Object selectedObject = this.selectedElement.getUserObject();
        String name = getNewName(selectedElement.getSimpleNodeName());
        if (!name.equals("")) {
            if (selectedObject instanceof FilterContainer) {
                ((FilterContainer) selectedObject).setName(name);
            } else if (selectedObject instanceof PatternParam) {
                ((PatternParam) selectedObject).setPattern(name);
            }
            updateModel();
        }
    }

    private void updateParentChilds() {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        model.removeNodeFromParent(selectedElement);
        model.reload(parentElement);
    }

    private void initListeners() {
        rename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((selectedElement != null) && (parentElement != null)) {
                    renameElement();
                }
            }
        });
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((selectedElement != null) && (parentElement != null)) {
                    removeFromParent();
                    updateParentChilds();
                }
            }
        });
    }

    public void setSelectedElement(DbImportTreeNode selectedElement) {
        this.selectedElement = selectedElement;
    }

    public void setParentElement(DbImportTreeNode parentElement) {
        this.parentElement = parentElement;
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }
}
