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
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @since 4.1
 */
public class RootPopUpMenu extends DefaultPopUpMenu {

    private static final int FIRST_POSITION = 0;

    protected JMenuItem addItem;
    protected JMenuItem addCatalog;
    protected JMenuItem addSchema;
    protected JMenuItem addIncludeTable;
    protected JMenuItem addExcludeTable;
    protected JMenuItem addIncludeColumn;
    protected JMenuItem addExcludeColumn;
    protected JMenuItem addIncludeProcedure;
    protected JMenuItem addExcludeProcedure;

    public RootPopUpMenu() {
        initPopUpMenuElements();
        initListeners();
        this.add(addItem, FIRST_POSITION);
        delete.setVisible(false);
        rename.setVisible(false);
    }

    private void addNewCatalog() {
        String name = getNewName();
        if (!name.equals("")) {
            Catalog newCatalog = new Catalog(name);
            ((ReverseEngineering) selectedElement.getUserObject()).addCatalog(newCatalog);
            selectedElement.add(new DbImportTreeNode(newCatalog));
            updateModel();
        }
    }

    private void addNewSchema() {
        String name = getNewName();
        if (!name.equals("")) {
            Schema newSchema = new Schema(name);
            ((SchemaContainer) selectedElement.getUserObject()).addSchema(newSchema);
            selectedElement.add(new DbImportTreeNode(newSchema));
            updateModel();
        }
    }

    private void addNewIncludeTable() {
        String name = getNewName();
        if (!name.equals("")) {
            IncludeTable newTable = new IncludeTable(name);
            ((FilterContainer) selectedElement.getUserObject()).addIncludeTable(newTable);
            selectedElement.add(new DbImportTreeNode(newTable));
            updateModel();
        }
    }

    private void addPatternParamToContainer(Class paramClass, Object selectedObject, String name) {
        FilterContainer container = (FilterContainer) selectedObject;
        PatternParam element = null;
        if (paramClass == ExcludeTable.class) {
            element = new ExcludeTable(name);
            container.addExcludeTable((ExcludeTable) element);
        } else if (paramClass == IncludeColumn.class) {
            element = new IncludeColumn(name);
            container.addIncludeColumn((IncludeColumn) element);
        } else if (paramClass == ExcludeColumn.class) {
            element = new ExcludeColumn(name);
            container.addExcludeColumn((ExcludeColumn) element);
        } else if (paramClass == IncludeProcedure.class) {
            element = new IncludeProcedure(name);
            container.addIncludeProcedure((IncludeProcedure) element);
        } else if (paramClass == ExcludeProcedure.class) {
            element = new ExcludeProcedure(name);
            container.addExcludeProcedure((ExcludeProcedure) element);
        }
        selectedElement.add(new DbImportTreeNode(element));
    }

    private void addPatternParamToIncludeTable(Class paramClass, Object selectedObject, String name) {
        IncludeTable includeTable = (IncludeTable) selectedObject;
        PatternParam element = null;
        if (paramClass == IncludeColumn.class) {
            element = new IncludeColumn(name);
            includeTable.addIncludeColumn((IncludeColumn) element);

        } else if (paramClass == ExcludeColumn.class) {
            element = new ExcludeColumn(name);
            includeTable.addExcludeColumn((ExcludeColumn) element);
        }
        selectedElement.add(new DbImportTreeNode(element));
    }

    private void addNewPatternParam(Class paramClass) {
        Object selectedObject = selectedElement.getUserObject();
        String name = getNewName();
        if (!name.equals("")) {
            if (selectedObject instanceof FilterContainer) {
                addPatternParamToContainer(paramClass, selectedObject, name);
            } else if (selectedObject instanceof IncludeTable) {
                addPatternParamToIncludeTable(paramClass, selectedObject, name);
            }
            updateModel();
        }
    }

    private void initListeners() {
        addCatalog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewCatalog();
            }
        });
        addSchema.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewSchema();
            }
        });
        addIncludeTable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewIncludeTable();
                System.out.println(selectedElement.getUserObject());
            }
        });
        addExcludeTable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewPatternParam(ExcludeTable.class);
            }
        });
        addIncludeColumn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewPatternParam(IncludeColumn.class);
            }
        });
        addExcludeColumn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewPatternParam(ExcludeColumn.class);
            }
        });
        addIncludeProcedure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewPatternParam(IncludeProcedure.class);
            }
        });
        addExcludeProcedure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewPatternParam(ExcludeProcedure.class);
            }
        });
    }

    private void initPopUpMenuElements() {
        addItem = new JMenu("Add");
        addCatalog = new JMenuItem("Catalog");
        addSchema = new JMenuItem("Schema");
        addIncludeTable = new JMenuItem("Include Table");
        addExcludeTable = new JMenuItem("Exclude Table");
        addIncludeColumn = new JMenuItem("Include Column");
        addExcludeColumn = new JMenuItem("Exclude Column");
        addIncludeProcedure = new JMenuItem("Include Procedure");
        addExcludeProcedure = new JMenuItem("Exclude Procedure");

        addItem.add(addSchema);
        addItem.add(addCatalog);
        addItem.add(addIncludeTable);
        addItem.add(addExcludeTable);
        addItem.add(addIncludeColumn);
        addItem.add(addExcludeColumn);
        addItem.add(addIncludeProcedure);
        addItem.add(addExcludeProcedure);
    }
}
