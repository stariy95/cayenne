
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.apache.cayenne.modeler.editor.fasteditor.render.CanvasEventListener;
import org.apache.cayenne.modeler.editor.fasteditor.render.RenderObject;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

public abstract class Node implements RenderObject, CanvasEventListener {

    protected static final Rectangle2D ZERO_RECT = new Rectangle2D(0, 0, 0, 0);

    protected final List<Node> children;
    protected Node parent;
    protected Rectangle2D boundingRect;
    protected NodeState nodeState;

    public Node() {
        children = new ArrayList<>();
        nodeState = new NodeState();
        boundingRect = ZERO_RECT;
    }

    public void addChild(Node node) {
        children.add(node);
        node.parent = this;
    }

    public void addChild(int idx, Node node) {
        children.add(idx, node);
        node.parent = this;
    }

    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Node findChild(Point2D screenPoint) {
        Point2D childPoint = screenPoint.subtract(getX(), getY());
        for(Node child : children) {
            if(child.getBoundingRect() != null && child.getBoundingRect().contains(childPoint)) {
                return child.findChild(childPoint);
            }
        }
        return this;
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
        this.boundingRect = Objects.requireNonNull(boundingRect);
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
//        renderer.getContext().strokeRect(getX(), getY(), getWidth(), getHeight());
        if(!children.isEmpty()) {
            children.forEach(c -> {
                renderer.getContext().setTransform(1, 0, 0, 1, boundingRect.getMinX(), boundingRect.getMinY());
                c.render(renderer);
            });
        }
    }

    @Override
    public void advanceAnimation(long delta) {
        children.forEach(o -> o.advanceAnimation(delta));
    }

    abstract protected void doRender(Renderer renderer);

    @Override
    public void onClick(Renderer source, Point2D screenPoint) {
    }

    @Override
    public void onMouseUp(Renderer source, Point2D screenPoint) {
    }

    @Override
    public void onDragStart(Renderer source, Point2D screenPoint) {
    }

    @Override
    public void onDragMove(Renderer source, Point2D screenPoint) {
    }

    @Override
    public void onDoubleClick(Renderer source, Point2D screenPoint) {
    }

    public void onFocusLost() {
    }

    public void onFocus() {
    }
}
