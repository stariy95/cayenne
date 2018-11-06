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
import org.apache.cayenne.modeler.editor.fasteditor.render.node.Node;
import org.apache.cayenne.modeler.editor.fasteditor.render.node.NodeContainer;

public class DefaultState extends ControlState {

    public DefaultState(NodeContainer container) {
        super(container);
    }

    @Override
    public void onClick(Point2D screenPoint) {
        moveToState(StateType.SINGLE_SELECTION).onClick(screenPoint);
    }

    @Override
    public void onDoubleClick(Point2D screenPoint) {
        moveToState(StateType.SINGLE_SELECTION).onDoubleClick(screenPoint);
    }

    @Override
    public void onDragStart(Point2D screenPoint) {
        Node selectedNode = nodeContainer.findNode(screenPoint);
        if(selectedNode != null) {
            moveToState(StateType.DRAG).onDragStart(screenPoint);
        } else {
            moveToState(StateType.MULTI_SELECTION).onDragStart(screenPoint);
        }
    }

    @Override
    public void onStateExit(ControlState nextState) {
        // Do nothing on exit from default state
    }
}
