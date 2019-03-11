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

package org.apache.cayenne.access.flush.v1;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.DataDomainFlushActionFactory;
import org.apache.cayenne.access.flush.OperationSorter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushActionFactory implements DataDomainFlushActionFactory {

    @Inject
    private OperationSorter operationSorter;

    @Inject
    private JdbcEventLogger jdbcEventLogger;

    @Override
    public DataDomainFlushAction createFlushAction(DataDomain dataDomain) {
//        org.apache.cayenne.access.DataDomainFlushAction dataDomainFlushAction = new org.apache.cayenne.access.DataDomainFlushAction(dataDomain);
//        dataDomainFlushAction.setJdbcEventLogger(jdbcEventLogger);
//        return dataDomainFlushAction;
        return new NewDataDomainFlushAction(dataDomain, operationSorter, jdbcEventLogger);
    }
}
