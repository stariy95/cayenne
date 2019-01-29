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

package org.apache.cayenne.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * Base class for {@link ObjectSelect} and {@link ColumnSelect}
 *
 * @since 4.0
 */
public abstract class FluentSelect<T> extends IndirectQuery implements Select<T> {

    protected QueryRoot<?> queryRoot;

    protected Expression where;
    protected Collection<Ordering> orderings;
    protected PrefetchTreeNode prefetches;

    protected int limit;
    protected int offset;
    protected int pageSize;
    protected int statementFetchSize;

    protected QueryCacheStrategy cacheStrategy;
    protected String cacheGroup;

    protected FluentSelect() {
    }

    /**
     * Translates self to a SelectQuery.
     */
    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {

        SelectQuery<?> replacement = new SelectQuery<>();
        Object root;
        if(queryRoot == null || (root = queryRoot.resolve(resolver)) == null) {
            throw new CayenneRuntimeException("Undefined root entity of the query");
        }

        replacement.setRoot(root);
        replacement.setQualifier(where);
        replacement.addOrderings(orderings);
        replacement.setPrefetchTree(prefetches);
        replacement.setCacheStrategy(cacheStrategy);
        replacement.setCacheGroup(cacheGroup);
        replacement.setFetchLimit(limit);
        replacement.setFetchOffset(offset);
        replacement.setPageSize(pageSize);
        replacement.setStatementFetchSize(statementFetchSize);

        return replacement;
    }

    public String getCacheGroup() {
        return cacheGroup;
    }

    public QueryCacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    public int getStatementFetchSize() {
        return statementFetchSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    protected QueryRoot<?> getQueryRoot() {
        return queryRoot;
    }

    /**
     * Returns a WHERE clause Expression of this query.
     */
    public Expression getWhere() {
        return where;
    }

    public Collection<Ordering> getOrderings() {
        return orderings;
    }

    public PrefetchTreeNode getPrefetches() {
        return prefetches;
    }

    @Override
    public List<T> select(ObjectContext context) {
        return context.select(this);
    }

    @Override
    public T selectOne(ObjectContext context) {
        return context.selectOne(this);
    }

    @Override
    public void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
        context.iterate(this, callback);
    }

    @Override
    public ResultIterator<T> iterator(ObjectContext context) {
        return context.iterator(this);
    }

    @Override
    public ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
        return context.batchIterator(this, size);
    }

    interface QueryRoot<E> extends Serializable {
        E resolve(EntityResolver resolver);
    }

    static class EntityTypeQueryRoot implements QueryRoot<Class<?>> {
        final Class<?> entityType;

        EntityTypeQueryRoot(Class<?> entityType) {
            this.entityType = entityType;
        }

        @Override
        public Class<?> resolve(EntityResolver resolver) {
            return entityType;
        }
    }

    static class EntityNameQueryRoot implements QueryRoot<ObjEntity> {
        final String entityName;

        EntityNameQueryRoot(String entityName) {
            this.entityName = entityName;
        }

        @Override
        public ObjEntity resolve(EntityResolver resolver) {
            return resolver.getObjEntity(entityName);
        }
    }

    static class DbEntityNameQueryRoot implements QueryRoot<DbEntity> {
        final String entityName;

        DbEntityNameQueryRoot(String entityName) {
            this.entityName = entityName;
        }

        @Override
        public DbEntity resolve(EntityResolver resolver) {
            return resolver.getDbEntity(entityName);
        }
    }

    static class SubqueryQueryRoot implements QueryRoot<Select<?>> {

        final Select<?> subquery;

        SubqueryQueryRoot(Select<?> subquery) {
            this.subquery = subquery;
        }

        @Override
        public Select<?> resolve(EntityResolver resolver) {
            return subquery;
        }
    }
}
