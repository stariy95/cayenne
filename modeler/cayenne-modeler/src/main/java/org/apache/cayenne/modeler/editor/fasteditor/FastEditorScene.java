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

package org.apache.cayenne.modeler.editor.fasteditor;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import org.apache.cayenne.modeler.editor.fasteditor.render.CanvasEventListener;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.ResizableCanvas;

/**
 * @since 4.2
 */
public class FastEditorScene extends Scene {

    private final ResizableCanvas canvas;

    public FastEditorScene() {
        super(new Group());
        canvas = new ResizableCanvas(new Renderer() {
            @Override
            protected void doRender() {
                context.fillRect(10, 10, 100, 100);
            }
        }, new CanvasEventListener() {
            @Override
            public void onClick(Point2D screenPoint) {
            }

            @Override
            public void onMouseUp(Point2D screenPoint) {
            }

            @Override
            public void onDragStart(Point2D screenPoint) {
            }

            @Override
            public void onDragMove(Point2D screenPoint) {
            }
        });
        ((Group)getRoot()).getChildren().add(canvas);

        canvas.startRenderer();
    }


}
