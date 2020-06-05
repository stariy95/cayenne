/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.di.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ServerCaseDataSourceInfoProvider implements Provider<DataSourceInfo> {

    private static Logger logger = LoggerFactory.getLogger(ServerCaseDataSourceInfoProvider.class);

    private static final String PROPERTIES_FILE = "connection.properties";
    private static final String CONNECTION_NAME_KEY = "cayenneTestConnection";

    private static final String ADAPTER_KEY_MAVEN = "cayenneAdapter";
    private static final String USER_NAME_KEY_MAVEN = "cayenneJdbcUsername";
    private static final String PASSWORD_KEY_MAVEN = "cayenneJdbcPassword";
    private static final String URL_KEY_MAVEN = "cayenneJdbcUrl";
    private static final String DRIVER_KEY_MAVEN = "cayenneJdbcDriver";

    private Map<String, DataSourceInfo> inMemoryDataSources;
    private ConnectionProperties connectionProperties;

    public ServerCaseDataSourceInfoProvider() throws IOException {

        Map<String, String> propertiesMap = new HashMap<>();

        File file = connectionPropertiesFile();
        if(file.exists()) {
            Properties properties = new Properties();
            properties.load(new FileReader(file));
            properties.forEach((k, v) -> propertiesMap.put(k.toString(), v.toString()));
        }

        this.connectionProperties = new ConnectionProperties(propertiesMap);
        logger.info("Loaded  " + connectionProperties.size() + " DataSource configurations from properties file");

        this.inMemoryDataSources = new HashMap<>();

        // preload default in-memory DataSources. Will use them as defaults if
        // nothing is configured in ~/.cayenne/connection.properties
        DataSourceInfo hsqldb = new DataSourceInfo();
        hsqldb.setAdapterClassName(HSQLDBAdapter.class.getName());
        hsqldb.setUserName("sa");
        hsqldb.setPassword("");
        hsqldb.setDataSourceUrl("jdbc:hsqldb:mem:aname");
        hsqldb.setJdbcDriver("org.hsqldb.jdbcDriver");
        hsqldb.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
        hsqldb.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        inMemoryDataSources.put("hsql", hsqldb);

        DataSourceInfo h2 = new DataSourceInfo();
        h2.setAdapterClassName(H2Adapter.class.getName());
        h2.setUserName("sa");
        h2.setPassword("");
        h2.setDataSourceUrl("jdbc:h2:mem:aname;MVCC=TRUE;DB_CLOSE_DELAY=-1");
        h2.setJdbcDriver("org.h2.Driver");
        h2.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
        h2.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        inMemoryDataSources.put("h2", h2);

        DataSourceInfo derby = new DataSourceInfo();
        derby.setAdapterClassName(DerbyAdapter.class.getName());
        derby.setUserName("sa");
        derby.setPassword("");
        derby.setDataSourceUrl("jdbc:derby:target/testdb;create=true");
        derby.setJdbcDriver("org.apache.derby.jdbc.EmbeddedDriver");
        derby.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
        derby.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        inMemoryDataSources.put("derby", derby);

        DataSourceInfo sqlite = new DataSourceInfo();
        sqlite.setAdapterClassName(SQLiteAdapter.class.getName());
        sqlite.setUserName("sa");
        sqlite.setPassword("");
        sqlite.setDataSourceUrl("jdbc:sqlite:file:memdb?mode=memory&cache=shared&date_class=text");
        sqlite.setJdbcDriver("org.sqlite.JDBC");
        sqlite.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
        sqlite.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        inMemoryDataSources.put("sqlite", sqlite);
    }

    @Override
    public DataSourceInfo get() throws ConfigurationException {

        String connectionKey = property(CONNECTION_NAME_KEY);
        if (connectionKey == null) {
            connectionKey = "hsql";
        }

        logger.info("Connection key: " + connectionKey);
        DataSourceInfo connectionInfo = connectionProperties.getConnection(connectionKey);

        // attempt default if invalid key is specified
        if (connectionInfo == null) {
            connectionInfo = inMemoryDataSources.get(connectionKey);
        }

        if (connectionInfo == null) {
            connectionInfo = checkTestContainersDataSource(connectionKey);
        }

        connectionInfo = applyOverrides(connectionInfo);

        if (connectionInfo == null) {
            throw new ConfigurationException("No connection info for key: " + connectionKey);
        }

        logger.info("loaded connection info: " + connectionInfo);
        return connectionInfo;
    }

    private DataSourceInfo checkTestContainersDataSource(String connectionKey) {
        // special case for the testcontainers profile
        if (!connectionKey.endsWith("-tc")) {
            return null;
        }

        String db = connectionKey.substring(0, connectionKey.length() - 3);
        JdbcDatabaseContainer container;
        String adapter = JdbcAdapter.class.getName();
        switch (db) {
            case "mysql":
                container = new MySQLContainer();
                container
                        .withUrlParam("useUnicode", "true")
                        .withUrlParam("characterEncoding", "UTF-8")
                        .withUrlParam("generateSimpleParameterMetadata", "true")
                        .withUrlParam("useLegacyDatetimeCode", "false")
                        .withUrlParam("serverTimezone", Calendar.getInstance().getTimeZone().getID())
                        .withCommand("--character-set-server=utf8mb4")
                        .withCommand("--collation-server=utf8mb4_unicode_ci");
                adapter = MySQLAdapter.class.getName();
                break;
            case "postgres":
                container = new PostgreSQLContainer();
                adapter = PostgresAdapter.class.getName();
                break;
            case "sqlserver":
                adapter = SQLServerAdapter.class.getName();
                // TODO: implement this
                return null;
            default:
                // TODO: could we start some generic container anyway?
                return null;
        }

        // To grab properties, should start container first
        container.start();

        DataSourceInfo sourceInfo = new DataSourceInfo();
        sourceInfo.setAdapterClassName(adapter);
        sourceInfo.setUserName(container.getUsername());
        sourceInfo.setPassword(container.getPassword());
        sourceInfo.setDataSourceUrl(container.getJdbcUrl());
        sourceInfo.setJdbcDriver(container.getDriverClassName());
        sourceInfo.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
        sourceInfo.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        return sourceInfo;
    }

    private File connectionPropertiesFile() {
        return new File(cayenneUserDir(), PROPERTIES_FILE);
    }

    private File cayenneUserDir() {
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir, ".cayenne");
    }

    private DataSourceInfo applyOverrides(DataSourceInfo connectionInfo) {
        String adapter = property(ADAPTER_KEY_MAVEN);
        String user = property(USER_NAME_KEY_MAVEN);
        String pass = property(PASSWORD_KEY_MAVEN);
        String url = property(URL_KEY_MAVEN);
        String driver = property(DRIVER_KEY_MAVEN);

        if (connectionInfo == null) {
            // only create a brand new DSI if overrides contains a DB url...
            if (url == null) {
                return null;
            }

            connectionInfo = new DataSourceInfo();
            connectionInfo.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
            connectionInfo.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        }

        connectionInfo = connectionInfo.cloneInfo();
        if (adapter != null) {
            connectionInfo.setAdapterClassName(adapter);
        }

        if (user != null) {
            connectionInfo.setUserName(user);
        }

        if (pass != null) {
            connectionInfo.setPassword(pass);
        }

        if (url != null) {
            connectionInfo.setDataSourceUrl(url);
        }

        if (driver != null) {
            connectionInfo.setJdbcDriver(driver);
        }

        return connectionInfo;
    }

    private String property(String name) {
        String p = System.getProperty(name);
        return p == null || p.startsWith("$") ? null : p;
    }
}
