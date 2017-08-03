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

import com.jgoodies.forms.builder.DefaultFormBuilder;
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
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @since 4.1
 */
public class AdvancedConfigurationDialog extends DbActionOptionsDialog {

    private JScrollPane scrollPane;
    private JTree includeTables;
    private JTextField meaningfulPk;
    private JTextField stripFromTableNames;
    private JTextField defaultPackage;
    private JComboBox<String> strategyCombo;
    private JCheckBox skipRelationshipsLoading;
    private JCheckBox skipPrimaryKeyLoading;
    private JCheckBox forceDataMapCatalog;
    private JCheckBox forceDataMapSchema;
    private JCheckBox usePrimitives;
    private JCheckBox useJava7Types;
    private JButton returnButton;
    protected String strategy;

    private ReverseEngineering reverseEngineering;
    private Map<Class, DefaultPopUpMenu> popups;

    public AdvancedConfigurationDialog(Collection<String> catalogs, Collection<String> schemas,
                                       String currentCatalog, String currentSchema,
                                       DbLoaderContext context) throws HeadlessException {
        super(Application.getFrame(), "Reengineer DB Schema: Advanced Options", catalogs, schemas, currentCatalog, currentSchema);
        DataMap dataMap = context.getProjectController().getCurrentDataMap();
        reverseEngineering = context.getMetaData().get(dataMap, ReverseEngineering.class);
        if (reverseEngineering == null) {
            reverseEngineering = new ReverseEngineering();
            context.getMetaData().add(dataMap, reverseEngineering);
        }
        initializePopUpMenus();
        initializeTextFields();
        fillCheckboxes();
        translateReverseEngineeringToTree();
    }

    private void initializePopUpMenus() {
        popups = new HashMap<>();
        popups.put(Catalog.class, new CatalogPopUpMenu());
        popups.put(Schema.class, new SchemaPopUpMenu());
        popups.put(ReverseEngineering.class, new RootPopUpMenu());
        popups.put(IncludeTable.class, new IncludeTablePopUpMenu());
        popups.put(ExcludeTable.class, new DefaultPopUpMenu());
        popups.put(IncludeColumn.class, new DefaultPopUpMenu());
        popups.put(ExcludeColumn.class, new DefaultPopUpMenu());
        popups.put(IncludeProcedure.class, new DefaultPopUpMenu());
        popups.put(ExcludeProcedure.class, new DefaultPopUpMenu());
    }

    private void fillCheckboxes() {
        skipRelationshipsLoading.setSelected(reverseEngineering.getSkipRelationshipsLoading());
        skipPrimaryKeyLoading.setSelected(reverseEngineering.getSkipPrimaryKeyLoading());
        forceDataMapCatalog.setSelected(reverseEngineering.isForceDataMapCatalog());
        forceDataMapSchema.setSelected(reverseEngineering.isForceDataMapSchema());
        usePrimitives.setSelected(reverseEngineering.isUsePrimitives());
        useJava7Types.setSelected(reverseEngineering.isUseJava7Types());
    }

    private <T extends PatternParam> void printParams(Collection<T> collection, DbImportTreeNode parent) {
        for (T element : collection) {
            parent.add(new DbImportTreeNode(element));
        }
    }

    private void printIncludeTables(Collection<IncludeTable> collection, DbImportTreeNode parent) {
        for (IncludeTable includeTable : collection) {
            DbImportTreeNode node = new DbImportTreeNode(includeTable);
            printParams(includeTable.getIncludeColumns(), node);
            printParams(includeTable.getExcludeColumns(), node);
            parent.add(node);
        }
    }

    private void printChildren(FilterContainer container, DbImportTreeNode parent) {
        printIncludeTables(container.getIncludeTables(), parent);
        printParams(container.getExcludeTables(), parent);
        printParams(container.getIncludeColumns(), parent);
        printParams(container.getExcludeColumns(), parent);
        printParams(container.getIncludeProcedures(), parent);
        printParams(container.getExcludeProcedures(), parent);
    }

    private void printSchemas(Collection<Schema> schemas, DbImportTreeNode parent) {
        for (Schema schema : schemas) {
            DbImportTreeNode node = new DbImportTreeNode(schema);
            printChildren(schema, node);
            parent.add(node);
        }
    }

    private void printCatalogs(Collection<Catalog> catalogs, DbImportTreeNode parent) {
        for (Catalog catalog : catalogs) {
            DbImportTreeNode node = new DbImportTreeNode(catalog);
            printSchemas(catalog.getSchemas(), node);
            printChildren(catalog, node);
            parent.add(node);
        }
    }

    private void initializeTextFields() {
        meaningfulPk.setText(reverseEngineering.getMeaningfulPkTables());
        stripFromTableNames.setText(reverseEngineering.getStripFromTableNames());
        defaultPackage.setText(reverseEngineering.getDefaultPackage());
    }

    private void translateReverseEngineeringToTree() {
        DefaultTreeModel model = (DefaultTreeModel)includeTables.getModel();
        DbImportTreeNode root = (DbImportTreeNode) model.getRoot();
        includeTables.removeAll();
        root.setUserObject(reverseEngineering);
        printCatalogs(reverseEngineering.getCatalogs(), root);
        printSchemas(reverseEngineering.getSchemas(), root);
        printIncludeTables(reverseEngineering.getIncludeTables(), root);
        printParams(reverseEngineering.getExcludeTables(), root);
        printParams(reverseEngineering.getIncludeColumns(), root);
        printParams(reverseEngineering.getExcludeColumns(), root);
        printParams(reverseEngineering.getIncludeProcedures(), root);
        printParams(reverseEngineering.getExcludeProcedures(), root);
        model.reload();
    }

    @Override
    protected void initForm(DefaultFormBuilder builder) {

        super.initForm(builder);

        includeTables = new JTree(new DbImportTreeNode());
        includeTables.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {

                    int row = includeTables.getClosestRowForLocation(e.getX(), e.getY());
                    includeTables.setSelectionRow(row);
                    DbImportTreeNode selectedElement
                            = (DbImportTreeNode) includeTables.getSelectionPath().getLastPathComponent();
                    DefaultPopUpMenu popupMenu = popups.get(selectedElement.getUserObject().getClass());
                    if (popupMenu != null) {
                        popupMenu.setSelectedElement(selectedElement);
                        popupMenu.setParentElement((DbImportTreeNode) selectedElement.getParent());
                        popupMenu.setTree(includeTables);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) includeTables.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        scrollPane = new JScrollPane(includeTables);

        strategyCombo = new JComboBox<>();
        strategyCombo.setEditable(true);

        returnButton = new JButton("Simple configurations");
        returnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnToSimpleConfig();
            }
        });
        buttons.add(returnButton);

        meaningfulPk = new JTextField();
        meaningfulPk.setToolTipText("<html>Regular expression to filter tables with meaningful primary keys.<br>" +
                "Multiple expressions divided by comma can be used.<br>" +
                "Example: <b>^table1|^table2|^prefix.*|table_name</b></html>");

        stripFromTableNames = new JTextField();
        stripFromTableNames.setToolTipText("<html>Regex that matches the part of the table name that needs to be stripped off " +
                "when generating ObjEntity name</html>");

        defaultPackage = new JTextField();
        defaultPackage.setToolTipText("<html>A Java package that will be set as the imported DataMap default and a package " +
                "of all the persistent Java classes. This is a required attribute if the \"map\"<br> itself does not " +
                "already contain a default package, as otherwise all the persistent classes will be mapped with no " +
                "package, and will not compile.</html>");

        skipRelationshipsLoading = new JCheckBox();
        skipRelationshipsLoading.setToolTipText("<html>Whether to load relationships.</html>");

        skipPrimaryKeyLoading = new JCheckBox();
        skipPrimaryKeyLoading.setToolTipText("<html>Whether to load primary keys.</html>");

        forceDataMapCatalog = new JCheckBox();
        forceDataMapCatalog.setToolTipText("<html>Automatically tagging each DbEntity with the actual DB catalog/schema" +
                "(default behavior) may sometimes be undesirable.<br>  If this is the case then setting <b>forceDataMapCatalog</b> " +
                "to <b>true</b> will set DbEntity catalog to one in the DataMap.</html>");

        forceDataMapSchema = new JCheckBox();
        forceDataMapSchema.setToolTipText("<html>Automatically tagging each DbEntity with the actual DB catalog/schema " +
                "(default behavior) may sometimes be undesirable.<br> If this is the case then setting <b>forceDataMapSchema</b> " +
                "to <b>true</b> will set DbEntity schema to one in the DataMap.</html>");

        usePrimitives = new JCheckBox();
        usePrimitives.setToolTipText("<html>Use primitive types (e.g. int) or Object types (e.g. java.lang.Integer)</html>");

        useJava7Types = new JCheckBox();
        useJava7Types.setToolTipText("<html>Use <b>java.util.Date</b> for all columns with <i>DATE/TIME/TIMESTAMP</i> types.<br>" +
                "By default <b>java.time.*</b> types will be used.</html>");

        builder.append("Naming Strategy:", strategyCombo, true);
        builder.append(scrollPane, 3);
        builder.append("Tables with Meaningful PK Pattern:", meaningfulPk);
        builder.append("Strip from table names:", stripFromTableNames);
        builder.append("Default package:", defaultPackage);
        builder.append("Skip relationships loading:", skipRelationshipsLoading);
        builder.append("Skip primary key loading:", skipPrimaryKeyLoading);
        builder.append("Force datamap catalog:", forceDataMapCatalog);
        builder.append("Force datamap schema:", forceDataMapSchema);
        builder.append("Use Java primitive types:", usePrimitives);
        builder.append("Use old java.util.Date type:", useJava7Types);
    }

    private void returnToSimpleConfig() {
        choice = SIMPLE_CONFIG;
        setVisible(false);
    }

    @Override
    protected void initFromModel(Collection<String> catalogs, Collection<String> schemas, String currentCatalog, String currentSchema) {
        super.initFromModel(catalogs, schemas, currentCatalog, currentSchema);

        Vector<String> arr = NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies();
        strategyCombo.setModel(new DefaultComboBoxModel<>(arr));
    }

    boolean isSkipRelationshipsLoading() {
        return skipRelationshipsLoading.isSelected();
    }

    boolean isSkipPrimaryKeyLoading() {
        return skipPrimaryKeyLoading.isSelected();
    }

    boolean isForceDataMapCatalog() {
        return forceDataMapCatalog.isSelected();
    }

    boolean isForceDataMapSchema() {
        return forceDataMapSchema.isSelected();
    }

    boolean isUsePrimitives() {
        return usePrimitives.isSelected();
    }

    boolean isUseJava7Typed() {
        return useJava7Types.isSelected();
    }

    String getMeaningfulPk() {
        return "".equals(meaningfulPk.getText()) ? null : meaningfulPk
                .getText();
    }

    String getNamingStrategy() {
        return (String) strategyCombo.getSelectedItem();
    }

    String getStripFromTableNames() {
        return stripFromTableNames.getText();
    }

    String getDefaultPackage() {
        return defaultPackage.getText();
    }
}
