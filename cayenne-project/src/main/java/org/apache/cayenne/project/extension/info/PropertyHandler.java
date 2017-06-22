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

package org.apache.cayenne.project.extension.info;

import org.apache.cayenne.configuration.xml.DataMapHandler;
import org.apache.cayenne.configuration.xml.DbEntityHandler;
import org.apache.cayenne.configuration.xml.DbRelationshipHandler;
import org.apache.cayenne.configuration.xml.EmbeddableHandler;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.configuration.xml.ObjEntityHandler;
import org.apache.cayenne.configuration.xml.ObjRelationshipHandler;
import org.apache.cayenne.configuration.xml.ProcedureHandler;
import org.apache.cayenne.configuration.xml.QueryDescriptorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class PropertyHandler extends NamespaceAwareNestedTagHandler {

    static final String PROPERTY_TAG = "property";

    private static final Logger logger = LoggerFactory.getLogger(PropertyHandler.class);

    InfoStorage storage;

    public PropertyHandler(NamespaceAwareNestedTagHandler parentHandler, String targetNamespace, InfoStorage storage) {
        super(parentHandler, targetNamespace);
        this.storage = storage;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case PROPERTY_TAG:
                Object parentObject = getParentObject();
                String name = attributes.getValue("name");
                if(parentObject != null) {
                    String oldValue = storage.putInfo(parentObject, name, attributes.getValue("value"));
                    if(oldValue != null) {
                        logger.warn("Property {} defined more than one for object", name);
                    }
                }
                logger.info("Loaded property for {}: {} = {}", parentObject, name, attributes.getValue("value"));
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    private Object getParentObject() {
        if(parentHandler instanceof DataMapHandler) {
            return ((DataMapHandler) parentHandler).getDataMap();
        } else if(parentHandler instanceof DbEntityHandler) {
            return ((DbEntityHandler) parentHandler).getEntity();
        } else if(parentHandler instanceof ObjEntityHandler) {
            return ((ObjEntityHandler) parentHandler).getEntity();
        } else if(parentHandler instanceof EmbeddableHandler) {
            return ((EmbeddableHandler) parentHandler).getEmbeddable();
        } else if(parentHandler instanceof QueryDescriptorHandler) {
            return "query"; // TODO: how to get query descriptor?
        } else if(parentHandler instanceof ProcedureHandler) {
            return ((ProcedureHandler) parentHandler).getProcedure();
        } else if(parentHandler instanceof DbRelationshipHandler) {
            return ((DbRelationshipHandler) parentHandler).getDbRelationship();
        } else if(parentHandler instanceof ObjRelationshipHandler) {
            return ((ObjRelationshipHandler) parentHandler).getObjRelationship();
        }

        if(parentHandler instanceof NamespaceAwareNestedTagHandler) {
            ContentHandler parentParentHandler = ((NamespaceAwareNestedTagHandler) parentHandler).getParentHandler();
            if(parentParentHandler instanceof DbEntityHandler) {
                return ((DbEntityHandler) parentParentHandler).getLastAttribute();
            } else if(parentParentHandler instanceof ObjEntityHandler) {
                return ((ObjEntityHandler) parentParentHandler).getLastAttribute();
            } else {
                logger.info("Parent class unknown: {} -> {}", parentParentHandler.getClass().getName(), parentHandler.getClass().getName());
            }
        } else {
            logger.info("Parent class unknown: {}", parentHandler.getClass().getName());
        }
        return null;
    }
}
