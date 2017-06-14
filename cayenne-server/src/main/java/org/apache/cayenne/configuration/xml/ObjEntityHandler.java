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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class ObjEntityHandler extends NamespaceAwareNestedTagHandler {

    private static final String OBJ_ENTITY_TAG = "obj-entity";
    private static final String OBJ_ATTRIBUTE_TAG = "obj-attribute";
    public static final String TRUE = "true";

    private DataMap map;

    private ObjEntity entity;

    public ObjEntityHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case OBJ_ATTRIBUTE_TAG:
                createObjAttribute(attributes);
                return true;

            case OBJ_ENTITY_TAG:
                createObjEntity(attributes);
                return true;
        }

        return false;
    }

    private void createObjAttribute(Attributes attributes) {
        String dbPath = attributes.getValue("db-attribute-path");
        if (dbPath == null) {
            dbPath = attributes.getValue("db-attribute-name");
        }

        ObjAttribute oa = new ObjAttribute(attributes.getValue("name"));
        oa.setType(attributes.getValue("type"));
        oa.setUsedForLocking(TRUE.equalsIgnoreCase(attributes.getValue("lock")));
        oa.setDbAttributePath(dbPath);
        entity.addAttribute(oa);
    }

    private void createObjEntity(Attributes attributes) {
        entity = new ObjEntity(attributes.getValue("name"));
        entity.setClassName(attributes.getValue("className"));
        entity.setClientClassName(attributes.getValue("clientClassName"));
        entity.setAbstract(TRUE.equalsIgnoreCase(attributes.getValue("abstract")));
        entity.setReadOnly(TRUE.equalsIgnoreCase(attributes.getValue("readOnly")));
        entity.setServerOnly(TRUE.equalsIgnoreCase(attributes.getValue("serverOnly")));
        entity.setExcludingSuperclassListeners(TRUE.equalsIgnoreCase(attributes.getValue("exclude-superclass-listeners")));
        entity.setExcludingDefaultListeners(TRUE.equalsIgnoreCase(attributes.getValue("exclude-default-listeners")));
        if ("optimistic".equals(attributes.getValue("", "lock-type"))) {
            entity.setDeclaredLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        }

        String superEntityName = attributes.getValue("", "superEntityName");
        if (superEntityName != null) {
            entity.setSuperEntityName(superEntityName);
        } else {
            entity.setSuperClassName(attributes.getValue("", "superClassName"));
            entity.setClientSuperClassName(attributes.getValue("", "clientSuperClassName"));
        }
        entity.setDbEntityName(attributes.getValue("", "dbEntityName"));

        map.addObjEntity(entity);
    }
}
