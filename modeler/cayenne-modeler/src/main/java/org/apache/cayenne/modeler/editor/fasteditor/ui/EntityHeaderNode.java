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

package org.apache.cayenne.modeler.editor.fasteditor.ui;

import javafx.geometry.Point2D;
import org.apache.cayenne.modeler.editor.fasteditor.model.ObjEntityWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

/**
 * @since 4.2
 */
public class EntityHeaderNode extends TextNode {

    private final ObjEntityWrapper entityWrapper;

    public EntityHeaderNode(ObjEntityWrapper entityWrapper) {
        super(entityWrapper.getName());
        this.entityWrapper = entityWrapper;
    }

    @Override
    public void onDoubleClick(Renderer source, Point2D screenPoint) {
        Point2D position = new Point2D(getWorldX(), getWorldY());
        source.textInput(position, entityWrapper.getName(), str -> {
            entityWrapper.setName(str);
            setText(str);
            source.markDirty();
        });
    }
}
