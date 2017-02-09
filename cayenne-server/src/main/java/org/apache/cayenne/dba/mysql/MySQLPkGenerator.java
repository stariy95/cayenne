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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.sybase.SybasePkGenerator;

/**
 */
public class MySQLPkGenerator extends SybasePkGenerator {

	MySQLPkGenerator(JdbcAdapter adapter) {
		super(adapter);
	}

    @Override
    protected String pkTableCreateString() {
        return "CREATE TABLE IF NOT EXISTS AUTO_PK_SUPPORT " +
                "(TABLE_NAME CHAR(100) NOT NULL, NEXT_ID BIGINT NOT NULL, PRIMARY KEY (TABLE_NAME)) ENGINE=InnoDB";
    }

	@Override
	protected String safePkTableDrop() {
		return "DROP TABLE IF EXISTS AUTO_PK_SUPPORT";
	}

    @Override
    protected String getCallProcedureSQL() {
        return "{call auto_pk_for_table(?, ?)}";
    }

    /**
     * CREATE PROCEDURE auto_pk_for_table (IN tname VARCHAR(255), IN pkbatchsize INT)
     * BEGIN
     *     DECLARE exit HANDLER FOR sqlexception
     *     BEGIN
     *          ROLLBACK;
     *     END;
     *
     *     DECLARE exit HANDLER FOR sqlwarning
     *     BEGIN
     *          ROLLBACK;
     *     END;
     *
     *     START TRANSACTION;
	 *     	   -- UPDATE will lock TABLE_NAME record utill transaction end
     *         UPDATE AUTO_PK_SUPPORT SET NEXT_ID = NEXT_ID + pkbatchsize WHERE TABLE_NAME = tname;
     *         SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = tname;
     *     COMMIT;
     * END
     *
     */
	protected String unsafePkProcCreate() {
		return  "CREATE PROCEDURE auto_pk_for_table (IN tname VARCHAR(100), IN pkbatchsize INT) BEGIN " +
                " DECLARE exit HANDLER FOR sqlexception BEGIN ROLLBACK; END; " +
                " DECLARE exit HANDLER FOR sqlwarning BEGIN ROLLBACK; END; " +
                " START TRANSACTION; " +
                "   UPDATE AUTO_PK_SUPPORT SET NEXT_ID = NEXT_ID + pkbatchsize WHERE TABLE_NAME = tname; " +
                "   SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = tname; " +
                " COMMIT; " +
                "END";
	}

	@Override
	protected String safePkProcDrop() {
		return "DROP PROCEDURE IF EXISTS auto_pk_for_table";
	}
}
