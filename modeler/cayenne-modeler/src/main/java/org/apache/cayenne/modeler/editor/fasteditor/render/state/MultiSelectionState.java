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

package org.apache.cayenne.modeler.editor.fasteditor.render.state;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import org.apache.cayenne.modeler.editor.fasteditor.render.LayerType;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderObject;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeContainer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeState;

public class MultiSelectionState extends ControlState {

    private List<Node> selectedNodes = new ArrayList<>();
    private Point2D startPoint;
    private Rectangle2D selectionRect;
    private RenderObject selectionRenderObject;

    public MultiSelectionState(NodeContainer container) {
        super(container);
        selectionRenderObject = renderer -> {
            if(selectionRect != null) {
                renderer.getContext().setStroke(Color.rgb(0xcc, 0xcc, 0xcc));
                renderer.getContext().strokeRect(
                        selectionRect.getMinX(),
                        selectionRect.getMinY(),
                        selectionRect.getWidth(),
                        selectionRect.getHeight());
            }
        };
    }

    @Override
    public void onDragMove(Point2D screenPoint) {
        // selection rectangle
        double x = Math.min(startPoint.getX(), screenPoint.getX());
        double y = Math.min(startPoint.getY(), screenPoint.getY());
        double w = Math.abs(startPoint.getX() - screenPoint.getX());
        double h = Math.abs(startPoint.getY() - screenPoint.getY());
        selectionRect = new Rectangle2D(x, y, w, h);
    }

    @Override
    public void onDragStart(Point2D screenPoint) {
        startPoint = screenPoint;
        selectionRect = new Rectangle2D(screenPoint.getX(), screenPoint.getY(), 1, 1);
        nodeContainer.getRenderer().addObject(LayerType.UI, selectionRenderObject);
    }

    @Override
    public void onMouseUp(Point2D screenPoint) {
        moveToState(StateType.DEFAULT);
    }

    @Override
    public void onStateExit(ControlState nextState) {
        nodeContainer.getRenderer().removeObject(LayerType.UI, selectionRenderObject);
        selectedNodes.forEach(n -> n.getNodeState().removeState(NodeState.STATE_MULTI_SELECTED));
        selectedNodes.clear();
    }
}
