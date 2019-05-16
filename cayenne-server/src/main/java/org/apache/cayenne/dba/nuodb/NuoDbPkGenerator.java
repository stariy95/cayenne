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

package org.apache.cayenne.dba.nuodb;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class NuoDbPkGenerator extends JdbcPkGenerator {

    public NuoDbPkGenerator() {
    }

    public NuoDbPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String pkSelectString(DbEntity entity) {
        return "SELECT #result('NEXT_ID' 'long' 'NEXT_ID') FROM AUTO_PK_SUPPORT "
                + "WHERE TABLE_NAME = '" + entity.getName() + '\'';
    }


    @Override
    protected String pkUpdateString(DbEntity entity) {
        return "UPDATE AUTO_PK_SUPPORT SET NEXT_ID = NEXT_ID + " + pkCacheSize
                + " WHERE TABLE_NAME = '" + entity.getName() + '\'';
    }

    protected String dropAutoPkString() {
        return "DROP TABLE AUTO_PK_SUPPORT IF EXISTS";
    }
}
