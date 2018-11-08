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
import java.util.Optional;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;

/**
 * @since 4.2
 */
public class Renderer implements CanvasEventListener {

    protected final EnumMap<LayerType, RenderLayer> layerMap = new EnumMap<>(LayerType.class);
    protected /*final*/ GraphicsContext context;
    protected volatile boolean isDirty = true;
    protected long lastFrameTime;
    protected final FocusController focusController;

    public Renderer() {
        this.focusController = new FocusController();
        for(LayerType type: LayerType.values()) {
            layerMap.put(type, new RenderLayer(this, type));
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

    public void requestFocus(Node node) {
        focusController.setFocus(node);
    }

    public void resetFocus() {
        focusController.resetFocus();
    }

    protected void advanceAnimation(long delta) {
        layerMap.values().forEach(l -> l.advanceAnimation(delta));
    }

    protected void doRender() {
        layerMap.values().forEach(l -> l.render(this));
    }

    protected void clear(double width, double height) {
        context.setTransform(1, 0, 0, 1, 0, 0);
        context.clearRect(0, 0, width, height);
        context.setFill(Color.BLACK);
        context.fillText(width + "x" + height,0, 10);
    }

    public void addObject(LayerType layer, RenderObject object) {
        layerMap.get(layer).addRenderObject(object);
        markDirty();
    }

    public void removeObject(LayerType layer, RenderObject object) {
        layerMap.get(layer).removeRenderObject(object);
        markDirty();
    }

    private Optional<Node> findActiveNode(Point2D screenPoint) {
        LayerType[] types = LayerType.values();
        for(int i=types.length - 1; i >= 0; i--) {
            Node node = layerMap.get(types[i]).findNode(screenPoint);
            if(node != null) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    @Override
    public void onClick(Point2D screenPoint) {
        layerMap.get(LayerType.SCENE_BACK).onClick(screenPoint);
        findActiveNode(screenPoint).ifPresent(n -> n.onClick(screenPoint));
    }

    @Override
    public void onMouseUp(Point2D screenPoint) {
        layerMap.get(LayerType.SCENE_BACK).onMouseUp(screenPoint);
        findActiveNode(screenPoint).ifPresent(n -> n.onMouseUp(screenPoint));
    }

    @Override
    public void onDragStart(Point2D screenPoint) {
        layerMap.get(LayerType.SCENE_BACK).onDragStart(screenPoint);
        findActiveNode(screenPoint).ifPresent(n -> n.onDragStart(screenPoint));
    }

    @Override
    public void onDragMove(Point2D screenPoint) {
        layerMap.get(LayerType.SCENE_BACK).onDragMove(screenPoint);
        findActiveNode(screenPoint).ifPresent(n -> n.onDragMove(screenPoint));
    }

    @Override
    public void onDoubleClick(Point2D screenPoint) {
        layerMap.get(LayerType.SCENE_BACK).onDoubleClick(screenPoint);
        findActiveNode(screenPoint).ifPresent(n -> n.onDoubleClick(screenPoint));
    }
}
