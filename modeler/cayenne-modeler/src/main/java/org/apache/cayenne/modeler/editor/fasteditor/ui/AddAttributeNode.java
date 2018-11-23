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
import javafx.scene.image.Image;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class AddAttributeNode extends Node {

    private final Image addAttributeIcon;

    public AddAttributeNode() {
        addAttributeIcon = new Image("org/apache/cayenne/modeler/images/icon-attribute.png");
        boundingRect = new Rectangle2D(10, 0, addAttributeIcon.getWidth(), addAttributeIcon.getHeight());
    }

    @Override
    protected void doRender(Renderer renderer) {
        renderer.getContext().drawImage(addAttributeIcon, getX(), getY());
    }

    @Override
    public void onClick(Renderer source, Point2D screenPoint) {
        ((EntityNode) parent).addAttribute(this, source);
    }
}
