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
import javafx.scene.text.Text;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class EntityHeaderNode extends Node {

    private final ObjEntityWrapper entityWrapper;
    private boolean editMode = false;

    public EntityHeaderNode(ObjEntityWrapper entityWrapper) {
        this.entityWrapper = entityWrapper;
    }

    @Override
    protected void doRender(Renderer renderer) {
        GraphicsContext gc = renderer.getContext();

        final Text text = new Text("SomeCayenneEntityName");
        double captionWidth = text.getLayoutBounds().getWidth();
        double captionHeight = text.getLayoutBounds().getHeight();

        double captionX = parent.getWidth() / 2 - captionWidth / 2;
        double captionY = captionHeight + 2;

        gc.setFill(Color.BLACK);
        gc.fillText("SomeCayenneEntityName", captionX, captionY);
    }

    @Override
    public void advanceAnimation(long delta) {

    }

    @Override
    public void onDoubleClick(Point2D screenPoint) {
        switchEditMode();
    }

    private void switchEditMode() {
        if(editMode) {
            editMode = false;
        } else {
            editMode = true;
        }
    }
}
