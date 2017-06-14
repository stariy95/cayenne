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

import java.util.Map;
import java.util.TreeMap;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class DataMapHandler extends NamespaceAwareNestedTagHandler {

    /* This constants must be in sync with dataMap.xsd schema */
    private static final String DATA_MAP_TAG         = "data-map";
    private static final String PROPERTY_TAG         = "property";
    private static final String DB_ENTITY_TAG        = "db-entity";
    private static final String OBJ_ENTITY_TAG       = "obj-entity";
    private static final String DB_RELATIONSHIP_TAG  = "db-relationship";
    private static final String OBJ_RELATIONSHIP_TAG = "obj-relationship";
    private static final String EMBEDDABLE_TAG       = "embeddable";
    private static final String PROCEDURE_TAG        = "procedure";
    private static final String QUERY_TAG            = "query";

    public static final String TRUE = "true";

    private DataChannelDescriptor descriptor;

    private DataMap map;

    private Map<String, Object> mapProperties;

    public DataMapHandler(NamespaceAwareNestedTagHandler parentHandler) {
        super(parentHandler);
    }

    public DataMapHandler(SAXNestedTagHandler parentHandler, HandlerFactory factory, DataChannelDescriptor descriptor) {
        // TODO: do we need to define current namespace somewhere else to have better control over it?
        super(parentHandler, "http://cayenne.apache.org/schema/10/modelMap", factory);
        this.descriptor = descriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName,
                                     Attributes attributes) throws SAXException {
        switch (localName) {
            case PROPERTY_TAG:
                addProperty(attributes);
                return true;

            case DATA_MAP_TAG:
                this.map = new DataMap();
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if(namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case DB_ENTITY_TAG:
                    return new DbEntityHandler(this, map);

                case OBJ_ENTITY_TAG:
                    return new ObjEntityHandler(this, map);

                case DB_RELATIONSHIP_TAG:
                    return new DbRelationshipHandler(this, map);

                case OBJ_RELATIONSHIP_TAG:
                    return new ObjRelationshipHandler(this, map);

                case PROCEDURE_TAG:
                    return new ProcedureHandler(this, map);

                case QUERY_TAG:
                    return new QueryDescriptorHandler(this, map);

                case EMBEDDABLE_TAG:
                    return new EmbeddableHandler(this, map);
            }
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected void beforeScopeEnd() {
        map.initWithProperties(mapProperties);
        if(descriptor != null) {
            descriptor.getDataMaps().add(map);
        }
    }

    private void addProperty(Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (null == name) {
            throw new SAXException("MapLoader::processStartDataMapProperty(), no property name.");
        }

        String value = attributes.getValue("value");
        if (null == value) {
            throw new SAXException("MapLoader::processStartDataMapProperty(), no property value.");
        }

        // special meaning for <property name="name" .../>
        if("name".equals(name)) {
            map.setName(value);
            return;
        }

        if (mapProperties == null) {
            mapProperties = new TreeMap<>();
        }

        mapProperties.put(name, value);
    }

    public DataMap getMap() {
        return map;
    }
}
