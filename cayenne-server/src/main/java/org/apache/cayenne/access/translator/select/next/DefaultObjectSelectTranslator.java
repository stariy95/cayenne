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

package org.apache.cayenne.access.translator.select.next;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 4.1
 */
public class DefaultObjectSelectTranslator implements SelectTranslator {

    private static final List<TranslationStage> TRANSLATION_STAGES = Arrays.asList(
            new ColumnExtractorStage(),
            new PrefetchNodeStage(),
            new OrderingStage(),
            new QualifierTranslationStage(),
            new HavingTranslationStage(),
            new GroupByStage(),
            new DistinctStage(),
            new LimitOffsetStage(),
            new SQLPreparationStage(),
            new SQLGenerationStage()
    );

    private final TranslatorContext context;

    public DefaultObjectSelectTranslator(SelectQuery<?> query, DbAdapter adapter, EntityResolver entityResolver, DefaultObjectSelectTranslator parent) {
        this.context = new TranslatorContext(query, adapter, entityResolver, parent == null ? null : parent.context);
    }

    public DefaultObjectSelectTranslator(SelectQuery<?> query, DbAdapter adapter, EntityResolver entityResolver) {
        this(query, adapter, entityResolver, null);
    }

    @Override
    public String getSql() {
        for(TranslationStage stage : TRANSLATION_STAGES) {
            stage.perform(context);
        }

        return context.getFinalSQL();
    }

    @Override
    public DbAttributeBinding[] getBindings() {
        return context.getBindings().toArray(new DbAttributeBinding[0]);
    }

    @Override
    public Map<ObjAttribute, ColumnDescriptor> getAttributeOverrides() {
        return Collections.emptyMap();
    }

    @Override
    public ColumnDescriptor[] getResultColumns() {
        return context.getColumnDescriptors().toArray(new ColumnDescriptor[0]);
    }

    @Override
    public boolean isSuppressingDistinct() {
        return context.isDistinctSuppression();
    }

    @Override
    public boolean hasJoins() {
        return context.getTableCount() > 1;
    }
}