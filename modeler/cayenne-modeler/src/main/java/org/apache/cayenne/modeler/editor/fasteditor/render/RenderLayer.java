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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @since 4.2
 */
public class RenderLayer implements RenderObject {

    private final LayerType type;

    private final Set<RenderObject> objectSet = new LinkedHashSet<>();

    public RenderLayer(LayerType type) {
        this.type = Objects.requireNonNull(type);
    }

    public void addRenderObject(RenderObject object) {
        objectSet.add(object);
    }

    public void removeRenderObject(RenderObject object) {
        objectSet.remove(object);
    }

    @Override
    public void render(Renderer renderer) {
        objectSet.forEach(o -> o.render(renderer));
    }

    @Override
    public void advanceAnimation(long delta) {
        objectSet.forEach(o -> o.advanceAnimation(delta));
    }
}
