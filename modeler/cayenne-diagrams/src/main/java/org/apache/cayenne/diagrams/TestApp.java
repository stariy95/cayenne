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

package org.apache.cayenne.diagrams;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @since 4.1
 */
public class TestApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Drawing Operations Test");
        Group root = new Group();
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawShapes(gc);
        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private void drawShapes(GraphicsContext gc) {
        TreeParamsWalker walker = new TreeParamsWalker();

        Node node0 = new Node();
        Node node1 = new Node();
        node0.addChild(node0);
        node0.addChild(node1);
        node1.addChild(new Node());
        node1.addChild(new Node());

        Node node2 = new Node();
        node0.addChild(node2);

        Node node3 = new Node();
        node2.addChild(node3);
        node3.addChild(new Node());
        node3.addChild(new Node());
        node3.addChild(new Node());
        node3.addChild(node1);

        walker.walk(node0, n -> {});

        int depth = walker.getMaxDepth();
        int width = walker.getMaxWidth();

        int nodeWidth  = 100;
        int nodeHeight = 50;

        int margin = 5;

        int x = margin;
        int y = margin;

        int centerX = 200;
        int centerY = 0;

        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2);

        for(int i=0; i<=depth; i++) {
            List<Node> nodeList = walker.getNodes(i);
            for(int j=0; j<nodeList.size(); j++) {
                gc.strokeRoundRect(x, y, nodeWidth, nodeHeight, 5, 5);
                x += nodeWidth + 2 * margin;
            }
            x = margin;
            y += nodeHeight + 2 * margin;
        }

    }

}
