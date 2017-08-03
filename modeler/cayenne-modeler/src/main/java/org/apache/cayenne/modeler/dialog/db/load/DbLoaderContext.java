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

import java.io.File;
import java.sql.Connection;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0
 */
public class DbLoaderContext {

    private static Logger LOGGER = LoggerFactory.getLogger(DbLoaderContext.class);

    private DbImportConfiguration config;
    private Connection connection;
    private ProjectController projectController;
    private boolean existingMap;
    private DataMap dataMap;
    private boolean stopping;
    private String loadStatusNote;

    private DataChannelMetaData metaData;

    public DbLoaderContext(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    DataMap getDataMap() {
        return dataMap;
    }

    boolean isExistingDataMap() {
        return existingMap;
    }

    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    }

    ProjectController getProjectController() {
        return projectController;
    }

    void setConfig(DbImportConfiguration config) {
        this.config = config;
    }

    DbImportConfiguration getConfig() {
        return config;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isStopping() {
        return stopping;
    }

    public DataChannelMetaData getMetaData() {
        return metaData;
    }

    void setStopping(boolean stopping) {
        this.stopping = stopping;
    }

    String getStatusNote() {
        return loadStatusNote;
    }

    void setStatusNote(String loadStatusNote) {
        this.loadStatusNote = loadStatusNote;
    }

    public boolean buildConfig(DataSourceWizard connectionWizard, DbActionOptionsDialog dialog) {
        if (dialog == null || connectionWizard == null) {
            return false;
        }
        if (dialog instanceof AdvancedConfigurationDialog) {
            return buildConfig(connectionWizard, (AdvancedConfigurationDialog) dialog);
        } else if (dialog instanceof DbLoaderOptionsDialog) {
            return buildConfig(connectionWizard, (DbLoaderOptionsDialog) dialog);
        }

        return false;
    }

    // Fill config from metadata reverseEngineering
    private void fillConfig(DbImportConfiguration config, DataSourceWizard connectionWizard,
                             ReverseEngineering reverseEngineering) {
        FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(reverseEngineering);
        DBConnectionInfo connectionInfo = connectionWizard.getConnectionInfo();
        config.setAdapter(connectionWizard.getAdapter().getClass().getName());
        config.setUsername(connectionInfo.getUserName());
        config.setPassword(connectionInfo.getPassword());
        config.setDriver(connectionInfo.getJdbcDriver());
        config.setUrl(connectionInfo.getUrl());
        config.getDbLoaderConfig().setFiltersConfig(filtersConfigBuilder.build());
        config.setMeaningfulPkTables(reverseEngineering.getMeaningfulPkTables());
        config.setNamingStrategy(reverseEngineering.getNamingStrategy());
        config.setDefaultPackage(reverseEngineering.getDefaultPackage());
        config.setStripFromTableNames(reverseEngineering.getStripFromTableNames());
        config.setUsePrimitives(reverseEngineering.isUsePrimitives());
        config.setUseJava7Types(reverseEngineering.isUseJava7Types());
        config.setForceDataMapCatalog(reverseEngineering.isForceDataMapCatalog());
        config.setForceDataMapSchema(reverseEngineering.isForceDataMapSchema());
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
    }

    // Fill config from DbLoaderOptionDialog
    private void fillConfig(DbImportConfiguration config, DataSourceWizard connectionWizard, ReverseEngineering reverseEngineering,
                            DbLoaderOptionsDialog dialog) {
        FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(reverseEngineering);
        DBConnectionInfo connectionInfo = connectionWizard.getConnectionInfo();
        config.setAdapter(connectionWizard.getAdapter().getClass().getName());
        config.setUsername(connectionInfo.getUserName());
        config.setPassword(connectionInfo.getPassword());
        config.setDriver(connectionInfo.getJdbcDriver());
        config.setUrl(connectionInfo.getUrl());
        config.getDbLoaderConfig().setFiltersConfig(filtersConfigBuilder.build());
        config.setMeaningfulPkTables(dialog.getMeaningfulPk());
        config.setNamingStrategy(dialog.getNamingStrategy());
        config.setUsePrimitives(dialog.isUsePrimitives());
        config.setUseJava7Types(dialog.isUseJava7Typed());
    }

    private void fillReverseEngineeringFromDialog(ReverseEngineering reverseEngineering, AdvancedConfigurationDialog dialog) {
        reverseEngineering.setUsePrimitives(dialog.isUsePrimitives());
        reverseEngineering.setUseJava7Types(dialog.isUseJava7Typed());
        reverseEngineering.setForceDataMapCatalog(dialog.isForceDataMapCatalog());
        reverseEngineering.setForceDataMapSchema(dialog.isForceDataMapSchema());
        reverseEngineering.setSkipRelationshipsLoading(dialog.isSkipRelationshipsLoading());
        reverseEngineering.setSkipPrimaryKeyLoading(dialog.isSkipPrimaryKeyLoading());
        reverseEngineering.setMeaningfulPkTables(dialog.getMeaningfulPk());
        reverseEngineering.setNamingStrategy(dialog.getNamingStrategy());
        reverseEngineering.setStripFromTableNames(dialog.getStripFromTableNames());
        reverseEngineering.setDefaultPackage(dialog.getDefaultPackage());
    }

    private boolean buildConfig(DataSourceWizard connectionWizard, AdvancedConfigurationDialog dialog) {
        if (dialog == null || connectionWizard == null) {
            return false;
        }

        // Build reverse engineering from metadata and dialog values
        ReverseEngineering metaReverseEngineering = metaData.get(getProjectController().getCurrentDataMap(), ReverseEngineering.class);
        fillReverseEngineeringFromDialog(metaReverseEngineering, dialog);
        // Create copy of metaReverseEngineering
        ReverseEngineering reverseEngineering = new ReverseEngineering(metaReverseEngineering);

        DbImportConfiguration config = new DbImportConfiguration() {
            @Override
            public DbLoaderDelegate createLoaderDelegate() {
                return new LoaderDelegate(DbLoaderContext.this);
            }
        };
        fillConfig(config, connectionWizard, reverseEngineering);
        setConfig(config);

        prepareDataMap();

        return true;
    }

    // Clear dbimport metadata
    private void resetReverseEngineering(ReverseEngineering reverseEngineering) {
        reverseEngineering.clearIncludeTables();
        reverseEngineering.clearExcludeTables();
        reverseEngineering.clearIncludeColumns();
        reverseEngineering.clearExcludeColumns();
        reverseEngineering.clearIncludeProcedures();
        reverseEngineering.clearExcludeProcedures();
        reverseEngineering.getCatalogs().clear();
        reverseEngineering.getSchemas().clear();
        reverseEngineering.setDefaultPackage("");
        reverseEngineering.setStripFromTableNames("");
        reverseEngineering.setForceDataMapCatalog(false);
        reverseEngineering.setForceDataMapSchema(false);
        reverseEngineering.setSkipRelationshipsLoading(false);
        reverseEngineering.setSkipPrimaryKeyLoading(false);
    }

    private boolean buildConfig(DataSourceWizard connectionWizard, DbLoaderOptionsDialog dialog) {
        if (dialog == null || connectionWizard == null) {
            return false;
        }
        ReverseEngineering metaReverseEngineering = metaData.get(getProjectController().getCurrentDataMap(), ReverseEngineering.class);
        if (metaReverseEngineering == null) {
            metaReverseEngineering = new ReverseEngineering();
        }
        resetReverseEngineering(metaReverseEngineering);

        // Build filters
        ReverseEngineering reverseEngineering = new ReverseEngineering();
        IncludeTable includeTable = new IncludeTable(dialog.getTableIncludePattern());
        ExcludeTable excludeTable = new ExcludeTable(dialog.getTableExcludePattern());
        IncludeProcedure includeProcedure = new IncludeProcedure(dialog.getProcedureNamePattern());
        // Add patterns in metadata if text fields is not empty
        if (dialog.getTableIncludePattern() != null) {
            metaReverseEngineering.addIncludeTable(new IncludeTable(includeTable));
        }
        if (dialog.getTableExcludePattern() != null) {
            metaReverseEngineering.addExcludeTable(new ExcludeTable(excludeTable));
        }
        if (dialog.getProcedureNamePattern() != null) {
            metaReverseEngineering.addIncludeProcedure(new IncludeProcedure(includeProcedure));
        }
        metaReverseEngineering.setUsePrimitives(dialog.isUsePrimitives());
        metaReverseEngineering.setUseJava7Types(dialog.isUseJava7Typed());
        metaReverseEngineering.setMeaningfulPkTables(dialog.getMeaningfulPk());
        metaReverseEngineering.setNamingStrategy(dialog.getNamingStrategy());
        reverseEngineering.addIncludeTable(includeTable);
        reverseEngineering.addExcludeTable(excludeTable);
        reverseEngineering.addIncludeProcedure(includeProcedure);
        reverseEngineering.addCatalog(new Catalog(dialog.getSelectedCatalog()));
        reverseEngineering.addSchema(new Schema(dialog.getSelectedSchema()));

        // Add here auto_pk_support table
        reverseEngineering.addExcludeTable(new ExcludeTable("auto_pk_support|AUTO_PK_SUPPORT"));
        reverseEngineering.addIncludeProcedure(new IncludeProcedure(dialog.getProcedureNamePattern()));

        DbImportConfiguration config = new DbImportConfiguration() {
            @Override
            public DbLoaderDelegate createLoaderDelegate() {
                return new LoaderDelegate(DbLoaderContext.this);
            }
        };
        fillConfig(config, connectionWizard, reverseEngineering, dialog);
        setConfig(config);

        prepareDataMap();

        return true;
    }

    private void prepareDataMap() {
        dataMap = getProjectController().getCurrentDataMap();
        existingMap = dataMap != null;

        if (!existingMap) {
            ConfigurationNode root = getProjectController().getProject().getRootNode();
            dataMap = new DataMap();
            dataMap.setName(NameBuilder.builder(dataMap, root).name());
        }
        if (dataMap.getConfigurationSource() != null) {
            getConfig().setTargetDataMap(new File(dataMap.getConfigurationSource().getURL().getPath()));
        }
    }

    public void processWarn(final Throwable th, final String message) {
        LOGGER.warn(message, Util.unwindException(th));
    }

    public void processException(final Throwable th, final String message) {
        LOGGER.info("Exception on reverse engineering", Util.unwindException(th));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(Application.getFrame(), th.getMessage(), message,
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
