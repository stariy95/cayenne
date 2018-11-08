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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javafx.geometry.Point2D;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;
import org.apache.cayenne.modeler.editor.fasteditor.render.state.ControlState;
import org.apache.cayenne.modeler.editor.fasteditor.render.state.DefaultState;
import org.apache.cayenne.modeler.editor.fasteditor.render.state.DragState;
import org.apache.cayenne.modeler.editor.fasteditor.render.state.MultiSelectionState;
import org.apache.cayenne.modeler.editor.fasteditor.render.state.SingleSelectionState;
import org.apache.cayenne.modeler.editor.fasteditor.render.state.StateType;

/**
 * @since 4.2
 */
public class RenderLayer implements RenderObject, CanvasEventListener {

    private final LayerType type;
    private final Renderer renderer;
    private final Map<StateType, ControlState> stateMap;

    private ControlState currentState;

    private final Set<RenderObject> objectSet = new LinkedHashSet<>();

    public RenderLayer(Renderer renderer, LayerType type) {
        this.type = Objects.requireNonNull(type);
        this.renderer = Objects.requireNonNull(renderer);
        this.stateMap = new EnumMap<>(StateType.class);
        moveToState(StateType.DEFAULT);
    }

    public void addRenderObject(RenderObject object) {
        objectSet.add(object);
    }

    public void removeRenderObject(RenderObject object) {
        objectSet.remove(object);
    }

    @Override
    public void render(Renderer renderer) {
        objectSet.forEach(o -> {
            renderer.getContext().setTransform(1, 0, 0, 1, 0, 0);
            o.render(renderer);
        });
    }

    @Override
    public void advanceAnimation(long delta) {
        objectSet.forEach(o -> o.advanceAnimation(delta));
    }

    public ControlState moveToState(StateType stateType) {
        switch (stateType) {
            case DEFAULT:
                return currentState = stateMap.computeIfAbsent(stateType, st -> new DefaultState(this));

            case SINGLE_SELECTION:
                return currentState = stateMap.computeIfAbsent(stateType, st -> new SingleSelectionState(this));

            case MULTI_SELECTION:
                return currentState = stateMap.computeIfAbsent(stateType, st -> new MultiSelectionState(this));

            case DRAG:
                return currentState = stateMap.computeIfAbsent(stateType, st -> new DragState(this));
        }

        throw new IllegalArgumentException("Unknown state : " + stateType);
    }

    public Node findNode(Point2D point) {
        for (RenderObject object: objectSet) {
            if(object instanceof Node) {
                Node node = (Node) object;
                if (node.getBoundingRect().contains(point)) {
                    return node;
                }
            }
        }
        return null;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public void onClick(Point2D screenPoint) {
        currentState.onClick(screenPoint);
    }

    @Override
    public void onMouseUp(Point2D screenPoint) {
        currentState.onMouseUp(screenPoint);
    }

    @Override
    public void onDragStart(Point2D screenPoint) {
        currentState.onDragStart(screenPoint);
    }

    @Override
    public void onDragMove(Point2D screenPoint) {
        currentState.onDragMove(screenPoint);
    }

    @Override
    public void onDoubleClick(Point2D screenPoint) {
        currentState.onDoubleClick(screenPoint);
    }

}
