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

import java.beans.EventHandler;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
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
        Canvas canvas = new Canvas(1024, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawShapes(gc);

        canvas.setOnMouseDragEntered(event -> {
            double x = event.getSceneX();
            double y = event.getSceneY();
            gc.setStroke(Color.BLACK);
            gc.strokeLine(x, y, x + 1, y + 1);
        });

        canvas.setOnMouseClicked(event -> {
            double x = event.getSceneX();
            double y = event.getSceneY();
            gc.setStroke(Color.BLACK);
            gc.strokeLine(x, y, x + 1, y + 1);
        });

        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private Node createTestTree() {
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

        return node0;
    }


    private void drawShapes(GraphicsContext gc) {
        TreeParamsWalker walker = new TreeParamsWalker();

        Node node0 = createTestTree();

        walker.walk(node0, n -> {});

        int depth = walker.getMaxDepth();
        int width = walker.getMaxWidth();

        int nodeWidth  = 220;
        int nodeHeight = 50;
        int margin = 8;

        Affine transform = new Affine();
        transform.setTx(0.0);
        transform.setTy(0.0);

        int y = margin;
        for(int i=0; i<=depth; i++) {
            List<Node> nodeList = walker.getNodes(i);
            int nodesOnLevel = nodeList.size();
            int x = margin + (width - nodesOnLevel) * (nodeWidth + margin) / 2;
            for(Node nextNode : nodeList) {
                x += nodeWidth + 2 * margin;
                nextNode.setX(x);
                nextNode.setY(y);
                nextNode.setWidth(nodeWidth);
                nextNode.setHeight(nodeHeight);

                renderNode(gc, nextNode);
            }
            y += nodeHeight + 2 * margin;
        }
    }

    void renderNode(GraphicsContext gc, Node node) {
        gc.setFill(Color.rgb(175,195,220));
        gc.setStroke(Color.rgb(85, 118, 164));
        gc.setLineWidth(2);
        gc.translate(0, 0);

        gc.fillRoundRect(node.getX(), node.getY(), node.getWidth(), node.getHeight(), 5, 5);
        gc.strokeRoundRect(node.getX(), node.getY(), node.getWidth(), node.getHeight(), 5, 5);

        gc.translate(0, 0);
        gc.setFill(Color.BLACK);

        final Text text = new Text("SomeCayenneEntityName");
        double captionWidth = text.getLayoutBounds().getWidth();
        double captionHeight = text.getLayoutBounds().getHeight();

        double captionX = node.getX() + node.getWidth() / 2 - captionWidth / 2;
        double captionY = node.getY() + captionHeight + 2;

        gc.fillText("SomeCayenneEntityName", captionX, captionY);

    }

}
