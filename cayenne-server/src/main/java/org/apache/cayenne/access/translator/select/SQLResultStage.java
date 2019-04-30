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

package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DefaultScalarResultSegment;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
public class SQLResultStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        if(context.getParentContext() != null || !context.getQuery().needsResultSetMapping()) {
            return;
        }

        // optimization, resolve metadata result components here too, as it have same logic as this extractor...
        List<Object> resultSetMapping = resolveComponents(context.getSqlResult(), context.getResolver());

        context.getMetadata().setResultSetMapping(resultSetMapping);
    }

    protected List<Object> resolveComponents(SQLResult sqlResult, EntityResolver resolver) {
        if (sqlResult.getComponents() == null || sqlResult.getComponents().isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> resolvedComponents = new ArrayList<>(sqlResult.getComponents().size());

        int offset = 0;
        for (Object component : sqlResult.getComponents()) {
            if (component instanceof String) {
                resolvedComponents.add(new DefaultScalarResultSegment((String) component, offset));
                offset = offset + 1;
            } else if (component instanceof EntityResult) {
                EntityResult entityResult = (EntityResult) component;
                int fields = entityResult.getFieldsCount();
                String entityName = entityResult.getEntityName();
                if (entityName == null) {
                    entityName = resolver.getObjEntity(entityResult.getEntityClass()).getName();
                }

                ClassDescriptor classDescriptor = resolver.getClassDescriptor(entityName);
                resolvedComponents.add(new MyEntityResultSegment(classDescriptor, fields, offset));
                offset = offset + fields;
            } else {
                throw new IllegalArgumentException("Unsupported result descriptor component: " + component);
            }
        }

        return resolvedComponents;
    }

    static class MyEntityResultSegment implements EntityResultSegment {
        private final ClassDescriptor classDescriptor;
        private final int fields;
        private final int offset;

        MyEntityResultSegment(ClassDescriptor classDescriptor, int fields, int offset) {
            this.classDescriptor = classDescriptor;
            this.fields = fields;
            this.offset = offset;
        }

        @Override
        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }

        @Override
        public Map<String, String> getFields() {
            return Collections.emptyMap();
        }

        @Override
        public int getColumns() {
            return fields;
        }

        @Override
        public String getColumnPath(String resultSetLabel) {
            return resultSetLabel;
        }

        @Override
        public int getColumnOffset() {
            return offset;
        }
    }
}
