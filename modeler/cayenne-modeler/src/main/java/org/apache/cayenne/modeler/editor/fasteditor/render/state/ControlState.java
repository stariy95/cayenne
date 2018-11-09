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
import org.apache.cayenne.modeler.editor.fasteditor.render.CanvasEventListener;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderLayer;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

public abstract class ControlState implements CanvasEventListener {

    protected final RenderLayer nodeContainer;

    public ControlState(RenderLayer container) {
        this.nodeContainer = container;
    }

    @Override
    public void onClick(Point2D screenPoint) {
    }

    @Override
    public void onDoubleClick(Renderer source, Point2D screenPoint) {
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

    public abstract void onStateExit(ControlState nextState);

    protected ControlState moveToState(StateType stateType) {
        System.out.println("move to state " + stateType);
        ControlState nextState = nodeContainer.moveToState(stateType);
        onStateExit(nextState);
        return nextState;
    }

}
