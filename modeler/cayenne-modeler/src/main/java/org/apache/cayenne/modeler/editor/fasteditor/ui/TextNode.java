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

import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class TextNode extends Node {

    private String text;

    public TextNode(String text) {
        setText(text);
    }

    @Override
    protected void doRender(Renderer renderer) {
        renderer.getContext().setFill(Color.BLACK);
        renderer.getContext().fillText(text, getX(), getY() + getHeight());
    }

    public void setText(String text) {
        this.text = text;
        final Text tmpText = new Text(text);
        double captionWidth = tmpText.getLayoutBounds().getWidth();
        double captionHeight = tmpText.getLayoutBounds().getHeight();
        boundingRect = new Rectangle2D(0, 0, captionWidth, captionHeight);
    }
}
