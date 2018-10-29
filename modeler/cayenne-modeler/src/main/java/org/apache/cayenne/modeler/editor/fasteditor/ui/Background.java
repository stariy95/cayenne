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

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderObject;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

/**
 * @since 4.2
 */
public class Background implements RenderObject {

    private static final int CELL_SIZE = 20;

    @Override
    public void render(Background this, Renderer renderer) {
        GraphicsContext context = renderer.getContext();
        double height = context.getCanvas().getHeight();
        double width = context.getCanvas().getWidth();

        context.setLineWidth(0.5);
        context.setStroke(Color.rgb(0xdd, 0xdd, 0xee));

        int hLines = (int)Math.floor(width / CELL_SIZE);
        for(int i = 1; i <= hLines; i++) {
            int x = i * CELL_SIZE;
            context.strokeLine(x, 0, x, height);
        }
        int vLines = (int)Math.floor(height / CELL_SIZE);
        for(int i = 1; i <= vLines; i++) {
            int y = i * CELL_SIZE;
            context.strokeLine(0, y, width, y);
        }
    }
}
