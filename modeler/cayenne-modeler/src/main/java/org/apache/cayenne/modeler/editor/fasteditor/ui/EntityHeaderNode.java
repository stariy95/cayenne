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
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class EntityHeaderNode extends Node {

    private String name;
    private final ObjEntityWrapper entityWrapper;

    public EntityHeaderNode(ObjEntityWrapper entityWrapper) {
        this.entityWrapper = entityWrapper;
        this.name = entityWrapper.getName();
    }

    private void initBoundingRect(boolean force) {
        if(force || boundingRect == ZERO_RECT) {
            final Text text = new Text(name);
            double captionWidth = text.getLayoutBounds().getWidth();
            double captionHeight = text.getLayoutBounds().getHeight();
            double captionX = parent.getWidth() / 2 - captionWidth / 2;
            double captionY = 4;
            boundingRect = new Rectangle2D(captionX, captionY, captionWidth, captionHeight);
        }
    }

    @Override
    protected void doRender(Renderer renderer) {
        initBoundingRect(false);
        GraphicsContext gc = renderer.getContext();
        gc.setFill(Color.BLACK);
        gc.fillText(name, getX(), getY() + getHeight());
    }

    @Override
    public void onDoubleClick(Renderer source, Point2D screenPoint) {
        Point2D position = new Point2D(getX() + parent.getX(), getY() + parent.getY());
        source.textInput(position, entityWrapper.getName(), str -> {
            name = str;
            entityWrapper.setName(str);
            initBoundingRect(true);
            source.markDirty();
        });
    }
}
