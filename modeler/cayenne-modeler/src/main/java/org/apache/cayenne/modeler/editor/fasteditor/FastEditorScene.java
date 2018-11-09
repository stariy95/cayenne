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

package org.apache.cayenne.modeler.editor.fasteditor;

import java.util.Collections;

import javafx.scene.Group;
import javafx.scene.Scene;
import org.apache.cayenne.modeler.editor.fasteditor.layout.DefaultLayout;
import org.apache.cayenne.modeler.editor.fasteditor.layout.Layout;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.LayerType;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;
import org.apache.cayenne.modeler.editor.fasteditor.render.ResizableCanvas;
import org.apache.cayenne.modeler.editor.fasteditor.ui.Background;
import org.apache.cayenne.modeler.editor.fasteditor.ui.EntityNode;

/**
 * @since 4.2
 */
public class FastEditorScene extends Scene {

    private final ResizableCanvas canvas;
    private final Renderer renderer;
    private final Layout layout;

    public FastEditorScene() {
        super(new Group());
        renderer = new Renderer();
        layout = new DefaultLayout();
        canvas = new ResizableCanvas(renderer);
        ((Group)getRoot()).getChildren().add(canvas);
        canvas.startRenderer();
        renderer.addObject(LayerType.BACKGROUND, new Background());
        canvas.requestFocus();
    }

    public void addEntity(ObjEntityWrapper entity) {
        EntityNode entityNode = new EntityNode(entity);
        layout.doLayout(Collections.emptyList(), entityNode);
        renderer.addObject(LayerType.SCENE_BACK, entityNode);
    }
}
