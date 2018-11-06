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

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;

public class ResizableCanvas extends Canvas {

    private final CanvasEventListener eventListener;
    private final Renderer renderer;
    private final AnimationTimer timer;

    public ResizableCanvas(Renderer renderer, CanvasEventListener eventListener) {
        super(700, 500);

        this.renderer = renderer;
        this.eventListener = eventListener;
        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(now);
            }
        };

        renderer.setContext(getGraphicsContext2D());
        setupListeners();
    }

    private void setupListeners() {
        setOnDragDetected(event -> {
            eventListener.onDragStart(new Point2D(event.getSceneX(), event.getSceneY()));
            renderer.markDirty();
        });

        setOnMouseDragged(event -> {
            eventListener.onDragMove(new Point2D(event.getSceneX(), event.getSceneY()));
            renderer.markDirty();
        });

        setOnMouseReleased(event -> {
            eventListener.onMouseUp(new Point2D(event.getSceneX(), event.getSceneY()));
            renderer.markDirty();
        });

        setOnMousePressed(event -> {
            Point2D screenPoint = new Point2D(event.getSceneX(), event.getSceneY());
            System.out.println(event);
            switch (event.getClickCount()) {
                case 2:
                    eventListener.onDoubleClick(screenPoint);
                    break;
                default:
                    eventListener.onClick(screenPoint);
            }
        });

        widthProperty().addListener(evt -> renderer.markDirty());
        heightProperty().addListener(evt -> renderer.markDirty());
    }

    private void draw(long now) {
        renderer.render(now, getWidth(), getHeight());
    }

    public void startRenderer() {
        timer.start();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

}
