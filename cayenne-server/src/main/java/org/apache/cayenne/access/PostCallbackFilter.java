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

package org.apache.cayenne.access;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataChannelSyncCallbackAction;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * @since 4.0
 */
public class PostCallbackFilter implements DataChannelFilter {
    @Override
    public void init(DataChannel channel) {
        // noop
    }

    @Override
    public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType, DataChannelFilterChain filterChain) {
        DataChannelSyncCallbackAction callbackAction = DataChannelSyncCallbackAction.getCallbackAction(
                originatingContext.getChannel().getEntityResolver().getCallbackRegistry(),
                originatingContext.getGraphManager(),
                changes, syncType);

        GraphDiff result = filterChain.onSync(originatingContext, changes, syncType);

        callbackAction.applyPostCommit();

        return result;
    }
}
