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

import java.util.EnumMap;
import java.util.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @since 4.2
 */
public class Renderer {

    protected final EnumMap<LayerType, RenderLayer> layerMap = new EnumMap<>(LayerType.class);
    protected /*final*/ GraphicsContext context;
    protected volatile boolean isDirty = true;
    protected long lastFrameTime;

    public Renderer() {
        for(LayerType type: LayerType.values()) {
            layerMap.put(type, new RenderLayer(type));
        }
        lastFrameTime = System.currentTimeMillis();
    }

    public void setContext(GraphicsContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public GraphicsContext getContext() {
        return context;
    }

    public void markDirty() {
        isDirty = true;
    }

    public void render(long now, double width, double height) {
        advanceAnimation(lastFrameTime - now);
        lastFrameTime = now;
        if(isDirty) {
            clear(width, height);
            doRender();
            isDirty = false;
        }
    }

    protected void advanceAnimation(long delta) {
        layerMap.values().forEach(l -> l.advanceAnimation(delta));
    }

    protected void doRender() {
        layerMap.values().forEach(l -> l.render(this));
    }

    protected void clear(double width, double height) {
        context.clearRect(0, 0, width, height);
        context.setFill(Color.BLACK);
        context.fillText(width + "x" + height,0, 10);
    }

    public void addObject(LayerType layer, RenderObject object) {
        layerMap.get(layer).addRenderObject(object);
        markDirty();
    }
}
