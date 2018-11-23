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

import org.apache.cayenne.modeler.editor.fasteditor.model.ObjAttributeWrapper;
import org.apache.cayenne.modeler.editor.fasteditor.render.Renderer;

/**
 * @since 4.2
 */
public class AttributeNode extends HBoxNode {

    private final static String REMOVE_ICON = "org/apache/cayenne/modeler/images/icon-trash.png";

    private final ObjAttributeWrapper objAttribute;

    public AttributeNode(ObjAttributeWrapper objAttribute) {
        super(4);
        this.objAttribute = objAttribute;

        addChild(new IconNode(REMOVE_ICON));
        addChild(new TextNode(objAttribute.getName()));
    }

    @Override
    protected void doRender(Renderer renderer) {
        super.doRender(renderer);
    }
}
