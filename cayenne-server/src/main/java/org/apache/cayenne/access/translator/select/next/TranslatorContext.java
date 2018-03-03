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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.SelectBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 4.1
 */
class TranslatorContext {

    private final TableTree tableTree;

    /**
     * Result columns, can be following:
     * - root object attributes (including flattened)
     * - root object additional db attributes (PKs and FKs)
     * - flattened attributes additional PKs
     * - prefetched objects attributes and additional db attributes (PKs and FKs)
     * - order by expressions if query is distinct?
     */
    private final Set<ColumnDescriptor> columnDescriptors;

    /**
     * Scalar values bindings in order of appearance in final SQL,
     * may be should be filled by SQL node visitor.
     * <p>
     * Can be from expressions encountered in:
     * - attributes
     * - order by expressions
     * - where expression (including qualifiers from all used DbEntities and ObjEntities)
     */
    private final Collection<DbAttributeBinding> bindings;

    private final SelectBuilder selectBuilder;

    private final SelectQuery<?> query;

    private final QueryMetadata metadata;

    private final EntityResolver resolver;

    private final DbAdapter adapter;

    TranslatorContext(SelectQuery<?> query, DbAdapter adapter, EntityResolver resolver, TranslatorContext parentContext) {
        this.query = query;
        this.adapter = adapter;
        this.resolver = resolver;
        this.metadata = query.getMetaData(resolver);
        this.tableTree = new TableTree(parentContext == null ? 0 : parentContext.getTableCount());
        this.columnDescriptors = new LinkedHashSet<>(8);
        this.bindings = new ArrayList<>(4);
        this.selectBuilder = SQLBuilder.select();
    }

    SelectBuilder getSelectBuilder() {
        return selectBuilder;
    }

    Collection<ColumnDescriptor> getColumnDescriptors() {
        return columnDescriptors;
    }

    Collection<DbAttributeBinding> getBindings() {
        return bindings;
    }

    TableTree getTableTree() {
        return tableTree;
    }

    int getTableCount() {
        return tableTree.getNodeCount();
    }

    SelectQuery<?> getQuery() {
        return query;
    }

    QueryMetadata getMetadata() {
        return metadata;
    }

    EntityResolver getResolver() {
        return resolver;
    }

    DbAdapter getAdapter() {
        return adapter;
    }
}
