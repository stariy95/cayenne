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
public class SchemaHandler extends NamespaceAwareNestedTagHandler {

    private static final String SCHEMA_TAG = "schema";
    private static final String SCHEMA_NAME_TAG = "name";
    private static final String INCLUDE_TABLE_TAG = "include-table";
    private static final String EXCLUDE_TABLE_TAG = "exclude-table";
    private static final String INCLUDE_COLUMN_TAG = "include-column";
    private static final String EXCLUDE_COLUMN_TAG = "exclude-column";
    private static final String INCLUDE_PROCEDURE_TAG = "include-procedure";
    private static final String EXCLUDE_PROCEDURE_TAG = "exclude-procedure";

    private SchemaContainer entity;

    private Schema schema;

    public SchemaHandler(NamespaceAwareNestedTagHandler parentHandler, SchemaContainer entity) {
        super(parentHandler);
        this.entity = entity;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case SCHEMA_TAG:
                createSchema();
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case INCLUDE_TABLE_TAG:
                    return new IncludeTableHandler(this, schema);
            }
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case SCHEMA_NAME_TAG:
                createSchemaName(data);
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

        if (schema != null) {
            schema.addExcludeProcedure(new ExcludeProcedure(excludeProcedure));
        }
    }

    private void createIncludeProcedure(String includeProcedure) {
        if (includeProcedure.trim().length() == 0) {
            return;
        }

        if (schema != null) {
            schema.addIncludeProcedure(new IncludeProcedure(includeProcedure));
        }
    }

    private void createExcludeColumn(String excludeColumn) {
        if (excludeColumn.trim().length() == 0) {
            return;
        }

        if (schema != null) {
            schema.addExcludeColumn(new ExcludeColumn(excludeColumn));
        }
    }

    private void createIncludeColumn(String includeColumn) {
        if (includeColumn.trim().length() == 0) {
            return;
        }

        if (schema != null) {
            schema.addIncludeColumn(new IncludeColumn(includeColumn));
        }
    }

    private void createExcludeTable(String excludeTable) {
        if (excludeTable.trim().length() == 0) {
            return;
        }

        if (schema != null) {
            schema.addExcludeTable(new ExcludeTable(excludeTable));
        }
    }

    private void createSchemaName(String schemaName) {
        if (schemaName.trim().length() == 0) {
            return;
        }

        if (schema != null) {
            schema.setName(schemaName);
        }
    }

    private void createSchema() {
        schema = new Schema();
        entity.addSchema(schema);
    }
}
