package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryRouter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataDomainQueryActionNew implements QueryRouter, OperationObserver {

    final DataDomain domain;
    final List<QueryActionStage> stages;

    DataDomainQueryActionNew(DataDomain domain) {
        this.domain = domain;
        this.stages = List.of(

        );
    }

    QueryResponse execute(ObjectContext originatingContext, Query query) {
        QueryActionContext actionContext = new QueryActionContext(this, originatingContext, query);

        boolean done = false;
        for(QueryActionStage stage: stages) {
            if(stage.perform(actionContext) == QueryActionStage.DONE) {
                done = true;
                break;
            }
        }

        if(!done) {
            runQueryInTransaction(actionContext);
        }

        return actionContext.getResponse();
    }

    void runQueryInTransaction(QueryActionContext actionContext) {
        domain.getTransactionManager().performInTransaction(() -> {
            runQuery(actionContext);
            return null;
        });
    }

    private void runQuery(QueryActionContext actionContext) {
        // categorize queries by node and by "executable" query...
        actionContext.getQuery().route(this, domain.getEntityResolver(), null);

        // run categorized queries
        if (actionContext.getQueriesByNode() != null) {
            for (Map.Entry<QueryEngine, Collection<Query>> entry : actionContext.getQueriesByNode().entrySet()) {
                QueryEngine nextNode = entry.getKey();
                Collection<Query> nodeQueries = entry.getValue();
                nextNode.performQueries(nodeQueries, this);
            }
        }
    }

    public DataDomain getDomain() {
        return domain;
    }

    // QueryRouter methods

    @Override
    public void route(QueryEngine engine, Query query, Query substitutedQuery) {
        // TODO: what is this actually doing?
//        Collection<Query> queries = null;
//        if (queriesByNode == null) {
//            queriesByNode = new HashMap<>();
//        } else {
//            queries = queriesByNode.get(engine);
//        }
//
//        if (queries == null) {
//            queries = new ArrayList<>(5);
//            queriesByNode.put(engine, queries);
//        }
//        queries.add(query);
    }

    @Override
    public QueryEngine engineForName(String name) {
        QueryEngine node;
        if (name == null) {
            node = domain.getDefaultNode();
            if (node == null) {
                throw new CayenneRuntimeException("No default DataNode exists.");
            }
        } else {
            node = domain.getDataNode(name);
            if (node == null) {
                throw new CayenneRuntimeException("No DataNode exists for name %s", name);
            }
        }
        return node;
    }

    @Override
    public QueryEngine engineForDataMap(DataMap map) {
        Objects.requireNonNull(map, "Null DataMap, can't determine DataNode.");
        QueryEngine node = domain.lookupDataNode(map);
        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap %s", map);
        }
        return node;
    }

    // OperationObserver + OperationHints methods

    @Override
    public boolean isIteratedResult() {
        return false;
    }


    @Override
    public void nextCount(Query query, int resultCount) {

    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {

    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {

    }

    @Override
    public void nextRows(Query q, ResultIterator<?> it) {

    }

    @Override
    public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {

    }

    @Override
    public void nextQueryException(Query query, Exception ex) {

    }

    @Override
    public void nextGlobalException(Exception ex) {

    }
}
