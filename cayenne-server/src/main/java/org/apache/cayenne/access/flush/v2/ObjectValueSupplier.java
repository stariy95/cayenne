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

package org.apache.cayenne.access.flush.v2;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * @since 4.2
 */
class ObjectValueSupplier implements Supplier<Object> {

    private final AttributeProperty property;
    private final Persistent persistent;

    static Object getFor(ObjectId id, DbAttribute attribute) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(attribute);
        if(attribute.isPrimaryKey()) {
            // resolve eagerly, if value is already present
            // TODO: what if this is a meaningful part of an ID and it will change?
            Object value = id.getIdSnapshot().get(attribute.getName());
            if(value != null) {
                return value;
            }
            return new ObjectIdValueSupplier(id, attribute.getName());
        }

        // TODO: implement this case
        throw new CayenneRuntimeException("Unable to read non PK value");
//        return new ObjectValueSupplier(descriptor, null, attribute);
    }

    private ObjectValueSupplier(ClassDescriptor descriptor, Persistent object, DbAttribute attribute) {
        this.property = new ObjAttributeFinder(attribute).find(descriptor);
        this.persistent = object;
    }

    @Override
    public Object get() {
        return property.readPropertyDirectly(persistent);
    }

    static class ObjAttributeFinder implements PropertyVisitor {
        private final DbAttribute attribute;
        private AttributeProperty property;

        ObjAttributeFinder(DbAttribute attribute) {
            this.attribute = attribute;
        }

        AttributeProperty find(ClassDescriptor descriptor) {
            if(descriptor.visitAllProperties(this)) {
                throw new CayenneRuntimeException("Can't find property in descriptor %s for DbAttribute %s"
                        , descriptor, attribute);
            }
            return getProperty();
        }

        private AttributeProperty getProperty() {
            return property;
        }

        @Override
        public boolean visitAttribute(AttributeProperty property) {
            if(property.getAttribute().getDbAttribute() == attribute) {
                this.property = property;
                // stop visitor
                return false;
            }
            return true;
        }

        @Override
        public boolean visitToOne(ToOneProperty property) {
            return true;
        }

        @Override
        public boolean visitToMany(ToManyProperty property) {
            return true;
        }
    }
}
