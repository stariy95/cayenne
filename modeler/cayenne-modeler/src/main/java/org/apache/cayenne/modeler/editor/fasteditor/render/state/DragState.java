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

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderLayer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeState;

public class DragState extends ControlState {

    private Node dragNode;
    private Point2D offset;

    public DragState(RenderLayer container) {
        super(container);
    }

    @Override
    public void onDragStart(Point2D screenPoint) {
        resetDragNode();
        dragNode = nodeContainer.findNode(screenPoint);
        if(dragNode == null) {
            moveToState(StateType.MULTI_SELECTION).onDragStart(screenPoint);
        } else {
            dragNode.getNodeState().addState(NodeState.STATE_DRAG);
            offset = new Point2D(dragNode.getX(), dragNode.getY()).subtract(screenPoint);
        }
    }

    @Override
    public void onDragMove(Point2D screenPoint) {
        dragNode.setBoundingRect(new Rectangle2D(
                screenPoint.getX() + offset.getX(),
                screenPoint.getY() + offset.getY(),
                dragNode.getWidth(),
                dragNode.getHeight()
        ));
    }

    @Override
    public void onMouseUp(Point2D screenPoint) {
        moveToState(StateType.DEFAULT);
    }

    private void resetDragNode() {
        if(dragNode != null) {
            dragNode.getNodeState().removeState(NodeState.STATE_DRAG);
            dragNode = null;
        }
        offset = null;
    }

    @Override
    public void onStateExit(ControlState nextState) {
        resetDragNode();
    }
}
