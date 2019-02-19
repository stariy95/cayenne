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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.DynamicJoin;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;

/**
 * @since 4.2
 */
public class ColumnSelectWrapper implements TranslatableQueryWrapper {

    private final ColumnSelect<?> columnSelect;

    public ColumnSelectWrapper(ColumnSelect<?> columnSelect) {
        this.columnSelect = Objects.requireNonNull(columnSelect);
    }

    @Override
    public boolean isDistinct() {
        return columnSelect.isDistinct();
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return columnSelect.getMetaData(resolver);
    }

    @Override
    public PrefetchTreeNode getPrefetchTree() {
        return columnSelect.getPrefetches();
    }

    @Override
    public Expression getQualifier() {
        return columnSelect.getWhere();
    }

    @Override
    public Collection<Ordering> getOrderings() {
        return columnSelect.getOrderings();
    }

    @Override
    public Collection<BaseProperty<?>> getColumns() {
        return columnSelect.getColumns();
    }

    @Override
    public Expression getHavingQualifier() {
        return columnSelect.getHaving();
    }

    @Override
    public Select<?> unwrap() {
        return columnSelect;
    }

    @Override
    public Map<String, DynamicJoin> getDynamicJoins() {
        return columnSelect.getDynamicJoins();
    }
}
