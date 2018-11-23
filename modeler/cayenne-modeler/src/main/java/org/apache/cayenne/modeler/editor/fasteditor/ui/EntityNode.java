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

package org.apache.cayenne.modeler.editor.fasteditor.ui;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjAttributeWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeState;
import org.apache.cayenne.util.Util;

/**
 * @since 4.2
 */
public class EntityNode extends VBoxNode implements ObjEntityWrapper.ChangeListener {

    private final ObjEntityWrapper entityWrapper;

    public EntityNode(ObjEntityWrapper entityWrapper) {
        super(4);
        this.entityWrapper = entityWrapper;
        entityWrapper.setListener(this);
        addChild(new EntityHeaderNode(entityWrapper));
        for(ObjAttributeWrapper attribute : entityWrapper.getWrapperAttributes()) {
            addChild(new AttributeNode(attribute));
        }
        addChild(new AddAttributeNode());
    }

    @Override
    public void doRender(Renderer renderer) {
        super.doRender(renderer);
        GraphicsContext context = renderer.getContext();
        context.setLineWidth(1.5);
        if(getNodeState().haveState(NodeState.STATE_SELECTED)) {
            context.setFill(Color.rgb(230, 230, 240));
        } else {
            context.setFill(Color.gray(0.9));
        }
        context.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 15, 15);

    }

    public void addAttribute(AddAttributeNode addAttributeNode, Renderer source) {
        Point2D point2D = new Point2D(addAttributeNode.getWorldX(), addAttributeNode.getWorldY());
        source.textInput(point2D, "newAttribute", name -> {
            if(!Util.isBlank(name)) {
                ObjAttributeWrapper attribute = new ObjAttributeWrapper(name);
                entityWrapper.addAttribute(attribute);
                AttributeEvent event = new AttributeEvent(source, attribute, entityWrapper, MapEvent.ADD);
                source.getProjectController().fireObjAttributeEvent(event);
                source.markDirty();
            }
        });
    }

    @Override
    public void onChange(ObjEntityWrapper.ChangeType type, ObjEntityWrapper wrapper, String name) {
        switch (type) {
            case ATTRIBUTE_ADD:
                addChild(children.size() - 1, new AttributeNode(wrapper.getWrapperAttribute(name)));
                break;
            case ATTRIBUTE_CHANGE:
            case ATTRIBUTE_REMOVE:
            case RELATIONSHIP_ADD:
            case RELATIONSHIP_CHANGE:
            case RELATIONSHIP_REMOVE:
        }
    }

}
