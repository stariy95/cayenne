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

package org.apache.cayenne.dbsync.xml;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.dbsync.reverse.dbimport.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class ConfigHandler extends NamespaceAwareNestedTagHandler {

    private static final String CONFIG_TAG = "config";
    private static final String CATALOG_TAG = "catalog";
    private static final String SCHEMA_TAG = "schema";
    private static final String TABLE_TYPE_TAG = "table-type";
    private static final String DEFAULT_PACKAGE_TAG = "default-package";
    private static final String FORCE_DATAMAP_CATALOG_TAG = "force-datamap-catalog";
    private static final String FORCE_DATAMAP_SCHEMA_TAG = "force-datamap-schema";
    private static final String MEANINGFUL_PK_TABLES_TAG = "meaningful-pk-tables";
    private static final String NAMING_STRATEGY_TAG = "naming-strategy";
    private static final String SKIP_PK_LOADING_TAG = "skip-pk-loading";
    private static final String SKIP_RELATIONSHIPS_LOADING_TAG = "skip-relationships-loading";
    private static final String STRIP_FROM_TABLE_NAMES_TAG = "strip-from-table-names";
    private static final String USE_JAVA7_TYPES_TAG = "use-java7-types";
    private static final String USE_PRIMITIVES_TAG = "use-primitives";
    private static final String INCLUDE_TABLE_TAG = "include-table";
    private static final String EXCLUDE_TABLE_TAG = "exclude-table";
    private static final String INCLUDE_COLUMN_TAG = "include-column";
    private static final String EXCLUDE_COLUMN_TAG = "exclude-column";
    private static final String INCLUDE_PROCEDURE_TAG = "include-procedure";
    private static final String EXCLUDE_PROCEDURE_TAG = "exclude-procedure";

    private static final String TRUE = "true";

    private ReverseEngineering configuration;

    public ConfigHandler(NamespaceAwareNestedTagHandler parentHandler) {
        super(parentHandler);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case CONFIG_TAG:
                createConfig();
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case CATALOG_TAG:
                    return new CatalogHandler(this, configuration);
                case SCHEMA_TAG:
                    return new SchemaHandler(this, configuration);
                case INCLUDE_TABLE_TAG:
                    return new IncludeTableHandler(this , configuration);
            }
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case TABLE_TYPE_TAG:
                createTableType(data);
                break;
            case DEFAULT_PACKAGE_TAG:
                createDefaultPackage(data);
                break;
            case FORCE_DATAMAP_CATALOG_TAG:
                createForceDatamapCatalog(data);
                break;
            case FORCE_DATAMAP_SCHEMA_TAG:
                createForceDatamapSchema(data);
                break;
            case MEANINGFUL_PK_TABLES_TAG:
                createMeaningfulPkTables(data);
                break;
            case NAMING_STRATEGY_TAG:
                createNamingStrategy(data);
                break;
            case SKIP_PK_LOADING_TAG:
                createSkipPkLoading(data);
                break;
            case SKIP_RELATIONSHIPS_LOADING_TAG:
                createSkipRelationshipsLoading(data);
                break;
            case STRIP_FROM_TABLE_NAMES_TAG:
                createStripFromTableNames(data);
                break;
            case USE_JAVA7_TYPES_TAG:
                createUseJava7Types(data);
                break;
            case USE_PRIMITIVES_TAG:
                createUsePrimitives(data);
                break;
            case EXCLUDE_TABLE_TAG:
                createExcludeTable(data);
                break;
            case INCLUDE_COLUMN_TAG:
                createIncludeColumn(data);
                break;
            case EXCLUDE_COLUMN_TAG:
                createExcludeColumn(data);
                break;
            case INCLUDE_PROCEDURE_TAG:
                createIncludeProcedure(data);
                break;
            case EXCLUDE_PROCEDURE_TAG:
                createExcludeProcedure(data);
                break;
        }
    }

    private void createExcludeProcedure(String excludeProcedure) {
        if (excludeProcedure.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.addExcludeProcedure(new ExcludeProcedure(excludeProcedure));
        }
    }

    private void createIncludeProcedure(String includeProcedure) {
        if (includeProcedure.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.addIncludeProcedure(new IncludeProcedure(includeProcedure));
        }
    }

    private void createExcludeColumn(String excludeColumn) {
        if (excludeColumn.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.addExcludeColumn(new ExcludeColumn(excludeColumn));
        }
    }

    private void createIncludeColumn(String includeColumn) {
        if (includeColumn.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.addIncludeColumn(new IncludeColumn(includeColumn));
        }
    }

    private void createExcludeTable(String excludeTable) {
        if (excludeTable.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.addExcludeTable(new ExcludeTable(excludeTable));
        }
    }

    private void createUsePrimitives(String usePrimitives) {
        if (usePrimitives.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            if (usePrimitives.equals(TRUE)) {
                configuration.setUsePrimitives(true);
            } else {
                configuration.setUsePrimitives(false);
            }
        }
    }

    private void createUseJava7Types(String useJava7Types) {
        if (useJava7Types.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            if (useJava7Types.equals(TRUE)) {
                configuration.setUseJava7Types(true);
            } else {
                configuration.setUseJava7Types(false);
            }
        }
    }

    private void createStripFromTableNames(String stripFromTableNames) {
        if (stripFromTableNames.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.setStripFromTableNames(stripFromTableNames);
        }
    }

    private void createSkipRelationshipsLoading(String skipRelationshipsLoading) {
        if (skipRelationshipsLoading.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            if (skipRelationshipsLoading.equals(TRUE)) {
                configuration.setSkipRelationshipsLoading(true);
            } else {
                configuration.setSkipRelationshipsLoading(false);
            }
        }
    }

    private void createSkipPkLoading(String skipPkLoading) {
        if (skipPkLoading.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            if (skipPkLoading.equals(TRUE)) {
                configuration.setSkipPrimaryKeyLoading(true);
            } else {
                configuration.setSkipPrimaryKeyLoading(false);
            }
        }
    }

    private void createNamingStrategy(String namingStrategy) {
        if (namingStrategy.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.setNamingStrategy(namingStrategy);
        }
    }

    private void createMeaningfulPkTables(String meaningfulPkTables) {
        if (meaningfulPkTables.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.setMeaningfulPkTables(meaningfulPkTables);
        }
    }

    private void createForceDatamapSchema(String forceDatamapSchema) {
        if (forceDatamapSchema.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            if (forceDatamapSchema.equals(TRUE)) {
                configuration.setForceDataMapSchema(true);
            } else {
                configuration.setForceDataMapSchema(false);
            }
        }
    }

    private void createForceDatamapCatalog(String forceDatamapCatalog) {
        if (forceDatamapCatalog.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            if (forceDatamapCatalog.equals(TRUE)) {
                configuration.setForceDataMapCatalog(true);
            } else {
                configuration.setForceDataMapCatalog(false);
            }
        }
    }

    private void createDefaultPackage(String defaultPackage) {
        if (defaultPackage.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.setDefaultPackage(defaultPackage);
        }
    }

    private void createTableType(String tableType) {
        if (tableType.trim().length() == 0) {
            return;
        }

        if (configuration != null) {
            configuration.addTableType(tableType);
        }
    }

    private void createConfig() {
        configuration = new ReverseEngineering();
    }
}
