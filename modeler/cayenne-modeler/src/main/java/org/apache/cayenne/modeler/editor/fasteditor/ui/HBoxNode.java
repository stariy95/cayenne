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
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class HBoxNode extends Node {

    private final int padding;

    private boolean isDirty;

    public HBoxNode(int padding) {
        this.padding = padding;
        isDirty = true;
    }

    protected void updateChildrenLayout() {
        double startX = boundingRect == ZERO_RECT ? padding : boundingRect.getMinX();
        double startY = boundingRect == ZERO_RECT ? padding : boundingRect.getMinY();
        double width = padding;
        double height = padding;
        for(Node node : children) {
            Rectangle2D oldRect = node.getBoundingRect();
            Rectangle2D rect = new Rectangle2D(width, oldRect.getMinY(), oldRect.getWidth(), oldRect.getHeight());
            node.setBoundingRect(rect);
            width += oldRect.getWidth() + padding;
            height = Math.max(height, oldRect.getHeight());
        }
        setBoundingRect(new Rectangle2D(startX, startY, width, height));
    }

    @Override
    public void addChild(Node node) {
        super.addChild(node);
        updateChildrenLayout();
    }

    @Override
    public void addChild(int idx, Node node) {
        super.addChild(idx, node);
        updateChildrenLayout();
    }

    @Override
    public void removeChild(Node node) {
        super.removeChild(node);
        updateChildrenLayout();
    }

    @Override
    protected void doRender(Renderer renderer) {
        // do nothing
        if(isDirty) {
            updateChildrenLayout();
            isDirty = false;
        }
    }
}
