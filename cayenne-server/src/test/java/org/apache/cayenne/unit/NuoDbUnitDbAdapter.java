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

package org.apache.cayenne.unit;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class NuoDbUnitDbAdapter extends UnitDbAdapter {

    public NuoDbUnitDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    /**
     * Returns a map of database constraints with DbEntity names used as keys,
     * and Collections of constraint names as values.
     */
    protected Map<String, Collection<String>> getConstraints(Connection conn, DataMap map,
                                                             Collection<String> includeTables) throws SQLException {

        Map<String, Collection<String>> constraintMap = new HashMap<>();

        DatabaseMetaData metadata = conn.getMetaData();

        for (String name : includeTables) {
            DbEntity entity = map.getDbEntity(name);
            if (entity == null) {
                continue;
            }

            QuotingStrategy strategy = adapter.getQuotingStrategy();

            // Get all constraints for the table
            try (ResultSet rs = metadata.getExportedKeys(entity.getCatalog(), entity.getSchema(), entity.getName())) {
                while (rs.next()) {
                    String fk = rs.getString("FK_NAME");
                    if (fk == null || fk.isEmpty()) {
                        continue;
                    }
                    String fkTable = rs.getString("FKTABLE_NAME");
                    if (fkTable != null) {
                        constraintMap
                                .computeIfAbsent(fkTable, k -> new HashSet<>())
                                .add(strategy.quotedIdentifier(entity, fk));
                    }
                }
            }
        }

        return constraintMap;
    }

}
