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
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeState;

/**
 * @since 4.2
 */
public class EntityNode extends Node {

    private final ObjEntityWrapper entityWrapper;

    public EntityNode(ObjEntityWrapper entityWrapper) {
        this.entityWrapper = entityWrapper;
        addChild(new EntityHeaderNode(entityWrapper));
        addChild(new AddAttributeNode(entityWrapper));
    }

    @Override
    public void doRender(Renderer renderer) {
        GraphicsContext context = renderer.getContext();
        context.setLineWidth(1.5);
        if(getNodeState().haveState(NodeState.STATE_SELECTED)) {
            context.setFill(Color.rgb(230, 230, 240));
        } else {
            context.setFill(Color.gray(0.9));
        }
        context.fillRoundRect(getBoundingRect().getMinX(), getBoundingRect().getMinY(),getWidth(), getHeight(), 15, 15);

    }

    private void addAttribute() {
        System.out.println("Attribute added");
    }

    private class AddAttributeNode extends Node {

        private final Image addAttributeIcon;

        public AddAttributeNode(ObjEntityWrapper entityWrapper) {
            addAttributeIcon = new Image("org/apache/cayenne/modeler/images/icon-attribute.png");
            boundingRect = new Rectangle2D(10, 20, addAttributeIcon.getWidth(), addAttributeIcon.getHeight());
        }

        @Override
        protected void doRender(Renderer renderer) {
            renderer.getContext().drawImage(addAttributeIcon, getX(), getY());
        }

        @Override
        public void onClick(Point2D screenPoint) {
            addAttribute();
        }
    }
}
