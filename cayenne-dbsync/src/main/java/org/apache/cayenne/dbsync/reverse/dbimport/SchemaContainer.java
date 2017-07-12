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

package org.apache.cayenne.dbsync.reverse.dbimport;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0
 */
public abstract class SchemaContainer extends FilterContainer implements Cloneable {

    private final Collection<Schema> schemaCollection = new LinkedList<>();

    public Collection<Schema> getSchemas() {
        return schemaCollection;
    }

    public void addSchema(Schema schema) {
        this.schemaCollection.add(schema);
    }

    public SchemaContainer(){
    }

    public SchemaContainer(SchemaContainer original) {
        super(original);
        for (Schema schema : original.getSchemas()) {
            this.addSchema(new Schema(schema));
        }
    }

    @Override
    public boolean isEmptyContainer() {
        if (!super.isEmptyContainer()) {
            return false;
        }

        if (schemaCollection.isEmpty()) {
            return true;
        }

        for (Schema schema : schemaCollection) {
            if (!schema.isEmptyContainer()) {
                return false;
            }
        }
        return true;
    }

    public StringBuilder toString(StringBuilder res, String prefix) {
        if (!isBlank(schemaCollection)) {
            for (Schema schema : schemaCollection) {
                schema.toString(res, prefix);
            }
        }

        return super.toString(res, prefix + "  ");
    }

    /*@Override
    public SchemaContainer clone() throws CloneNotSupportedException {
        SchemaContainer schemaContainer = (SchemaContainer) super.clone();
        Collection<Schema> schemas = new LinkedList<>(this.getSchemas());
        for (Schema schema : schemas) {
            schemaContainer.addSchema(schema.clone());
        }
        return schemaContainer;
    }*/
}
