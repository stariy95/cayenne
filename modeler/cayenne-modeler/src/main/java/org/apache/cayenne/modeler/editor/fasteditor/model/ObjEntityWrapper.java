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

package org.apache.cayenne.modeler.editor.fasteditor.model;

import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;

/**
 * @since 4.2
 */
public class ObjEntityWrapper extends ObjEntity implements ObjAttributeListener, ObjRelationshipListener {

    private ChangeListener listener;

    @Override
    public void addAttribute(Attribute attribute) {
        if(!(attribute instanceof ObjAttributeWrapper)) {
            throw new IllegalArgumentException();
        }
        super.addAttribute(attribute);
    }

    public ObjEntity toEntity() {
        ObjEntity result = new ObjEntity(getName());
        result.setClassName(getClassName());

        return result;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        if(listener != null) {
            listener.onChange(ChangeType.NAME_CHANGE, this, name);
        }
    }

    public ObjAttributeWrapper getWrapperAttribute(String name) {
        return (ObjAttributeWrapper)getAttribute(name);
    }

    @SuppressWarnings("unchecked")
    public Collection<ObjAttributeWrapper> getWrapperAttributes() {
        return (Collection<ObjAttributeWrapper>)(Collection)getAttributes();
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void objAttributeChanged(AttributeEvent e) {
        if(e.getEntity() == this && listener != null) {
            listener.onChange(ChangeType.ATTRIBUTE_CHANGE, this, e.getAttribute().getName());
        }
    }

    @Override
    public void objAttributeAdded(AttributeEvent e) {
        if(e.getEntity() == this && listener != null) {
            listener.onChange(ChangeType.ATTRIBUTE_ADD, this, e.getAttribute().getName());
        }
    }

    @Override
    public void objAttributeRemoved(AttributeEvent e) {
        if(e.getEntity() == this && listener != null) {
            listener.onChange(ChangeType.ATTRIBUTE_REMOVE, this, e.getAttribute().getName());
        }
    }

    @Override
    public void objRelationshipChanged(RelationshipEvent e) {
        if(e.getEntity() == this && listener != null) {
            listener.onChange(ChangeType.RELATIONSHIP_CHANGE, this, e.getRelationship().getName());
        }
    }

    @Override
    public void objRelationshipAdded(RelationshipEvent e) {
        if(e.getEntity() == this && listener != null) {
            listener.onChange(ChangeType.RELATIONSHIP_ADD, this, e.getRelationship().getName());
        }
    }

    @Override
    public void objRelationshipRemoved(RelationshipEvent e) {
        if(e.getEntity() == this && listener != null) {
            listener.onChange(ChangeType.RELATIONSHIP_REMOVE, this, e.getRelationship().getName());
        }
    }

    public enum ChangeType {
        ATTRIBUTE_ADD,
        ATTRIBUTE_CHANGE,
        ATTRIBUTE_REMOVE,
        RELATIONSHIP_ADD,
        RELATIONSHIP_CHANGE,
        RELATIONSHIP_REMOVE,
        NAME_CHANGE
    }

    public interface ChangeListener {
        void onChange(ChangeType type, ObjEntityWrapper wrapper, String name);
    }
}
