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

package org.apache.cayenne.modeler.editor.fasteditor.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @since 4.2
 */
public abstract class Renderer {

    protected GraphicsContext context;
    protected boolean isDirty;

    public void setContext(GraphicsContext context) {
        this.context = context;
    }

    public void markDirty() {
        isDirty = true;
    }

    public void render(long now, double width, double height) {
        if(!isDirty) {
            return;
        }

        clear(width, height);
        doRender();

        isDirty = false;
    }

    protected abstract void doRender();

    protected void clear(double width, double height) {
        context.clearRect(0, 0, width, height);
        context.setFill(Color.BLACK);
        context.fillText(width + "x" + height,0, 10);
    }
}
