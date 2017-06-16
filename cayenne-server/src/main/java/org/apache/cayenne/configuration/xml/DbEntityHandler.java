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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class DbEntityHandler extends NamespaceAwareNestedTagHandler {

    private static final String DB_ENTITY_TAG = "db-entity";
    private static final String DB_ATTRIBUTE_TAG = "db-attribute";
    private static final String DB_KEY_GENERATOR_TAG = "db-key-generator";
    private static final String DB_GENERATOR_TYPE_TAG = "db-generator-type";
    private static final String DB_GENERATOR_NAME_TAG = "db-generator-name";
    private static final String DB_KEY_CACHE_SIZE_TAG = "db-key-cache-size";
    private static final String QUALIFIER_TAG = "qualifier";

    private DbEntity entity;

    private DataMap dataMap;

    public DbEntityHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap dataMap) {
        super(parentHandler);
        this.dataMap = dataMap;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case DB_ENTITY_TAG:
                createDbEntity(attributes);
                return true;

            case DB_ATTRIBUTE_TAG:
                createDbAttribute(attributes);
                return true;

            case DB_KEY_GENERATOR_TAG:
                createDbKeyGenerator();
                return true;

            case DB_GENERATOR_NAME_TAG:
            case DB_GENERATOR_TYPE_TAG:
            case DB_KEY_CACHE_SIZE_TAG:
            case QUALIFIER_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case DB_GENERATOR_TYPE_TAG:
                setDbGeneratorType(data);
                break;

            case DB_GENERATOR_NAME_TAG:
                setDbGeneratorName(data);
                break;

            case DB_KEY_CACHE_SIZE_TAG:
                setDbKeyCacheSize(data);
                break;

            case QUALIFIER_TAG:
                createQualifier(data);
                break;
        }
    }

    private void createDbEntity(Attributes attributes) {
        String name = attributes.getValue("name");
        entity = new DbEntity(name);
        entity.setSchema(attributes.getValue("schema"));
        entity.setCatalog(attributes.getValue("catalog"));
        dataMap.addDbEntity(entity);
    }

    private void createDbAttribute(Attributes attributes) {
        String name = attributes.getValue("name");
        String type = attributes.getValue("type");

        DbAttribute attrib = new DbAttribute(name);
        attrib.setType(TypesMapping.getSqlTypeByName(type));
        entity.addAttribute(attrib);

        String length = attributes.getValue("length");
        if (length != null) {
            attrib.setMaxLength(Integer.parseInt(length));
        }

        String precision = attributes.getValue("attributePrecision");
        if (precision != null) {
            attrib.setAttributePrecision(Integer.parseInt(precision));
        }

        // this is an obsolete 1.2 'precision' attribute that really meant 'scale'
        String pseudoPrecision = attributes.getValue("precision");
        if (pseudoPrecision != null) {
            attrib.setScale(Integer.parseInt(pseudoPrecision));
        }

        String scale = attributes.getValue("scale");
        if (scale != null) {
            attrib.setScale(Integer.parseInt(scale));
        }

        attrib.setPrimaryKey(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("isPrimaryKey")));
        attrib.setMandatory(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("isMandatory")));
        attrib.setGenerated(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("isGenerated")));
    }

    private void createDbKeyGenerator() {
        entity.setPrimaryKeyGenerator(new DbKeyGenerator());
    }

    private void setDbGeneratorType(String type) {
        if (entity == null) {
            return;
        }
        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        pkGenerator.setGeneratorType(type);
        if (pkGenerator.getGeneratorType() == null) {
            entity.setPrimaryKeyGenerator(null);
        }
    }

    private void setDbGeneratorName(String name) {
        if (entity == null) {
            return;
        }
        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        if (pkGenerator == null) {
            return;
        }
        pkGenerator.setGeneratorName(name);
    }

    private void setDbKeyCacheSize(String size) {
        if (entity == null) {
            return;
        }
        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        if (pkGenerator == null) {
            return;
        }
        try {
            pkGenerator.setKeyCacheSize(new Integer(size.trim()));
        } catch (Exception ex) {
            pkGenerator.setKeyCacheSize(null);
        }
    }

    private void createQualifier(String qualifier) {
        if (qualifier.trim().length() == 0) {
            return;
        }

        // qualifier can belong to ObjEntity, DbEntity or a query
        if (entity != null) {
            entity.setQualifier(ExpressionFactory.exp(qualifier));
        }
    }

    public DbEntity getEntity() {
        return entity;
    }
}
