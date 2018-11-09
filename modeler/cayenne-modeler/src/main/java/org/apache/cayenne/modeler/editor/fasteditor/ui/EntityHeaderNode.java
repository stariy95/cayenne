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
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class EntityHeaderNode extends Node {

    private StringBuilder name;
    private final ObjEntityWrapper entityWrapper;
    private boolean editMode = false;
    private boolean defaultName = true;

    public EntityHeaderNode(ObjEntityWrapper entityWrapper) {
        this.entityWrapper = entityWrapper;
        this.name = new StringBuilder(getDefaultName()); // TODO:
    }

    private void initBoundingRect(boolean force) {
        if(force || boundingRect == ZERO_RECT) {
            System.out.println("Name " + name.toString());
            final Text text = new Text(name.toString());
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
        if(editMode) {
            gc.setFill(Color.BLUE);
        } else {
            gc.setFill(Color.BLACK);
        }
        gc.fillText(name.toString(), getX(), getY() + getHeight());
    }

    @Override
    public void onDoubleClick(Renderer source, Point2D screenPoint) {
        source.requestFocus(this);
    }

    @Override
    public void onFocus() {
        editMode = true;
        if(defaultName) {
            name.delete(0, name.length());
            defaultName = false;
        }
    }

    @Override
    public void onFocusLost() {
        editMode = false;
        if(name.length() == 0) {
            name.append(getDefaultName());
            initBoundingRect(true);
        }
    }

    @Override
    public void onKey(String character, KeyCode code) {
        if(code == KeyCode.BACK_SPACE) {
            System.out.println("Backspace");
            name.deleteCharAt(name.length() - 1);
        } else if(code == KeyCode.ENTER) {
            System.out.println("Enter");
            onFocusLost();
            entityWrapper.setName(name.toString());
        } else if(code.isLetterKey() || code.isDigitKey() || code == KeyCode.UNDERSCORE) {
            System.out.println("Append " + character);
            name.append(character);
            initBoundingRect(true);
        }
    }

    private String getDefaultName() {
        return "ObjEntity1";
    }
}
