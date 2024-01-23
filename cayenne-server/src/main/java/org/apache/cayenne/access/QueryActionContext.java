package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ListResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 5.0
 */
class QueryActionContext {
    final DataDomainQueryActionNew action;
    final Query query;
    final DataContext context;

    QueryResponse response;
    Map<String, List<?>> prefetchResultsByPath;
    Map<QueryEngine, Collection<Query>> queriesByNode;

    QueryActionContext(DataDomainQueryActionNew action, ObjectContext originatingContext, Query query) {
        if(originatingContext != null && !(originatingContext instanceof DataContext)) {
            throw new IllegalArgumentException("DataDomain can only work with DataContext. "
                    + "Unsupported context type: " + originatingContext);
        }
        this.action = action;
        this.context = (DataContext)originatingContext;
        this.query = query;
    }

    Query getQuery() {
        return query;
    }

    Map<QueryEngine, Collection<Query>> getQueriesByNode() {
        return queriesByNode;
    }

    DataContext getContext() {
        return context;
    }

    void addPrefetchByPath(String path, List<?> prefetchResult) {
        if(prefetchResultsByPath == null) {
            prefetchResultsByPath = new HashMap<>();
        }
        prefetchResultsByPath.put(path, prefetchResult);
    }

    void setResponse(QueryResponse response) {
        this.response = response;
    }

    /**
     * NOTE: use {@link #isGenericResponse()} and {@link #isListResponse()}
     * methods to check actual response form or {@link ClassCastException} could be thrown.
     * @return response typed to the requested form
     * @param <T> type of the response wanted by the caller
     */
    @SuppressWarnings("unchecked")
    <T extends QueryResponse> T getResponse() {
        return (T)response;
    }

    boolean isGenericResponse() {
        return response instanceof GenericResponse;
    }

    boolean isListResponse() {
        return response instanceof ListResponse;
    }
}
