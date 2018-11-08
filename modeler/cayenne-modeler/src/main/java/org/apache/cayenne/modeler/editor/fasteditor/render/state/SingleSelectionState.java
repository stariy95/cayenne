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
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderLayer;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeState;

public class SingleSelectionState extends ControlState {

    private Node selectedNode;

    public SingleSelectionState(RenderLayer container) {
        super(container);
    }

    @Override
    public void onClick(Point2D screenPoint) {
        resetSelectedNode();
        selectedNode = nodeContainer.findNode(screenPoint);
        if(selectedNode != null) {
            System.out.println("Select node " + selectedNode);
            selectedNode.getNodeState().addState(NodeState.STATE_SELECTED);
        } else {
            moveToState(StateType.DEFAULT);
        }
    }

    @Override
    public void onDoubleClick(Point2D screenPoint) {
        onClick(screenPoint);
        if(selectedNode != null) {
            System.out.println("Double click");
        }
    }

    @Override
    public void onDragStart(Point2D screenPoint) {
        Node node = nodeContainer.findNode(screenPoint);
        if(node != null) {
            moveToState(StateType.DRAG).onDragStart(screenPoint);
        } else {
            moveToState(StateType.MULTI_SELECTION).onDragStart(screenPoint);
        }
    }

    @Override
    public void onStateExit(ControlState nextState) {
        resetSelectedNode();
    }

    private void resetSelectedNode() {
        if(selectedNode != null) {
            System.out.println("Reset node " + selectedNode);
            selectedNode.getNodeState().removeState(NodeState.STATE_SELECTED);
            selectedNode = null;
        }
    }
}
