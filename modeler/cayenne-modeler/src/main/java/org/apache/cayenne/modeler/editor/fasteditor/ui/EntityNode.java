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

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderObject;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

/**
 * @since 4.2
 */
public class EntityNode implements RenderObject {

    private Point2D position = Point2D.ZERO;

    private final ObjEntityWrapper entityWrapper;
    private EntityHeaderNode header;
    private final List<AttributeNode> attributes = new ArrayList<>();


    public EntityNode(ObjEntityWrapper entityWrapper) {
        this.entityWrapper = entityWrapper;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    @Override
    public void render(Renderer renderer) {
        GraphicsContext context = renderer.getContext();

        context.setLineWidth(1.5);
        context.setFill(Color.gray(0.9));
        context.setStroke(Color.gray(0.7));
        context.fillRoundRect(position.getX(), position.getY(),250, 100, 15, 15);

        attributes.forEach(a -> a.render(renderer));
    }
}
