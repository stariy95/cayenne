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

package org.apache.cayenne.modeler.action.fasteditor;

import java.awt.event.ActionEvent;
import java.sql.Types;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.2
 */
public class SyncModelAction extends CayenneAction {

    private static final Logger logger = LoggerFactory.getLogger(SyncModelAction.class);

    public static String getActionName() {
        return "Sync with DataMap";
    }

    public SyncModelAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-datamap.png";
    }

    @Override
    public void performAction(ActionEvent e) {
        application.getProject().getRootNode().acceptVisitor(new BaseConfigurationNodeVisitor<Void>() {
            @Override
            public Void visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
                channelDescriptor.getDataMaps().forEach(SyncModelAction.this::processDataMap);
                return null;
            }
        });
    }

    private void processDataMap(DataMap dataMap) {
        logger.info("Syncing map " + dataMap.getName());
        dataMap.getObjEntities().forEach(this::processObjEntity);
    }

    private void processObjEntity(ObjEntity entity) {
        if(entity.getDbEntityName() == null) {
            String name = Util.javaToUnderscored(entity.getName());
            DbEntity dbEntity = new DbEntity(name);
            DbAttribute pk = new DbAttribute(name + "_id");
            pk.setType(Types.INTEGER);
            pk.setPrimaryKey(true);
            pk.setMandatory(true);
            pk.setGenerated(true);
            dbEntity.addAttribute(pk);
            entity.getDataMap().addDbEntity(dbEntity);
            entity.setDbEntity(dbEntity);
            application.getFrameController().getProjectController()
                    .fireDbEntityEvent(new EntityEvent(this, dbEntity, MapEvent.ADD));
        }
        entity.getAttributes().forEach(this::processObjAttribute);
        entity.getRelationships().forEach(this::processObjRelationship);
    }

    private void processObjAttribute(ObjAttribute objAttribute) {
        DbEntity dbEntity = objAttribute.getEntity().getDbEntity();
        if(Util.isBlank(objAttribute.getDbAttributePath())) {
            String name = Util.javaToUnderscored(objAttribute.getName());
            DbAttribute dbAttribute = new DbAttribute(name);
            dbAttribute.setMandatory(objAttribute.isMandatory());
            dbAttribute.setType(TypesMapping.getSqlTypeByJava(objAttribute.getJavaClass()));
            if(dbAttribute.getType() == Types.VARCHAR) {
                dbAttribute.setMaxLength(255);
            }
            dbEntity.addAttribute(dbAttribute);
            objAttribute.setDbAttributePath(name);
        }
    }

    private void processObjRelationship(ObjRelationship objRelationship) {
        DbEntity sourceDbEntity = objRelationship.getSourceEntity().getDbEntity();
        DbEntity targetDbEntity = objRelationship.getTargetEntity().getDbEntity();
    }
}
