
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

package org.apache.cayenne.modeler.editor.fasteditor.render.node;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.apache.cayenne.modeler.editor.fasteditor.render.CanvasEventListener;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderObject;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

public abstract class Node implements RenderObject, CanvasEventListener {

    protected final List<Node> children;
    protected Node parent;
    protected Rectangle2D boundingRect;
    protected NodeState nodeState;

    public Node() {
        children = new ArrayList<>();
        nodeState = new NodeState();
    }

    public void addChild(Node node) {
        children.add(node);
        node.parent = this;
    }

    public int getChildrenCount() {
        return children.size();
    }

    public List<Node> getChildren() {
        return children;
    }

    public Node getChild(int idx) {
        return children.get(idx);
    }

    public double getX() {
        return boundingRect.getMinX();
    }

    public double getY() {
        return boundingRect.getMinY();
    }

    public double getWidth() {
        return boundingRect.getWidth();
    }

    public double getHeight() {
        return boundingRect.getHeight();
    }

    public void setBoundingRect(Rectangle2D boundingRect) {
        this.boundingRect = boundingRect;
    }

    public Rectangle2D getBoundingRect() {
        return boundingRect;
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    @Override
    public void render(Renderer renderer) {
        doRender(renderer);
        renderer.getContext().strokeRect(getX(), getY(), getWidth(), getHeight());
        if(!children.isEmpty()) {
            children.forEach(c -> {
                renderer.getContext().setTransform(1, 0, 0, 1, boundingRect.getMinX(), boundingRect.getMinY());
                c.render(renderer);
            });
        }
    }

    abstract protected void doRender(Renderer renderer);

    @Override
    public void onClick(Point2D screenPoint) {
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

    @Override
    public void onDoubleClick(Point2D screenPoint) {
        Point2D childPoint = screenPoint.subtract(getX(), getY());
        System.out.println(screenPoint + " : " + childPoint + " : " + getBoundingRect());
        for(Node child: children) {
            System.out.println(child.getBoundingRect());
            if(child.getBoundingRect() != null && child.getBoundingRect().contains(childPoint)) {
                child.onDoubleClick(childPoint);
            }
        }
    }

    public void onFocusLost() {

    }

    public void onFocus() {
    }
}
