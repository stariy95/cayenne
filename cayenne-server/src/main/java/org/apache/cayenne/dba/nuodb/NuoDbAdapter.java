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

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.DefaultQuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * @since 4.2
 */
public class NuoDbAdapter extends JdbcAdapter {

    public NuoDbAdapter(@Inject RuntimeProperties runtimeProperties,
                        @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                        @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                        @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                        @Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator,
                        @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);
        setSupportsBatchUpdates(true);
    }

    @Override
    protected PkGenerator createPkGenerator() {
        return new NuoDbPkGenerator(this);
    }

    @Override
    protected QuotingStrategy createQuotingStrategy() {
        return new NuoDbQuotingStrategy("\"", "\"");
    }

    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ').append(getType(this, column));

        sqlBuffer.append(sizeAndPrecision(this, column));
        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");
    }

    @Override
    public String createFkConstraint(DbRelationship rel) {

        if(true) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        String srcName = quotingStrategy.quotedFullyQualifiedName(rel.getSourceEntity());
        String dstName = quotingStrategy.quotedFullyQualifiedName(rel.getTargetEntity());

        buf.append("ALTER TABLE ");
        buf.append(srcName);

        // hsqldb requires the ADD CONSTRAINT statement
        buf.append(" ADD CONSTRAINT ");

        String name = "U_" + rel.getSourceEntity().getName() + "_"
                + (long) (System.currentTimeMillis() / (Math.random() * 100000));

        DbEntity sourceEntity = rel.getSourceEntity();

        buf.append(quotingStrategy.quotedIdentifier(sourceEntity, sourceEntity.getSchema(), name));
        buf.append(" FOREIGN KEY (");

        boolean first = true;
        for (DbJoin join : rel.getJoins()) {
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            } else {
                first = false;
            }

            buf.append(quotingStrategy.quotedSourceName(join));
            refBuf.append(quotingStrategy.quotedTargetName(join));
        }

        buf.append(") REFERENCES ");
        buf.append(dstName);
        buf.append(" (");
        buf.append(refBuf.toString());
        buf.append(')');

        // also make sure we delete dependent FKs
        buf.append(" ON DELETE CASCADE");

        return buf.toString();
    }

    @Override
    public String createTable(DbEntity entity) {

        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("CREATE TABLE IF NOT EXISTS ");
        sqlBuffer.append(quotingStrategy.quotedFullyQualifiedName(entity));

        sqlBuffer.append(" (");
        // columns
        Iterator<DbAttribute> it = entity.getAttributes().iterator();
        if (it.hasNext()) {
            boolean first = true;
            while (it.hasNext()) {
                if (first) {
                    first = false;
                } else {
                    sqlBuffer.append(", ");
                }

                DbAttribute column = it.next();

                // attribute may not be fully valid, do a simple check
                if (column.getType() == TypesMapping.NOT_DEFINED) {
                    throw new CayenneRuntimeException("Undefined type for attribute '%s.%s'."
                            , entity.getFullyQualifiedName(), column.getName());
                }

                createTableAppendColumn(sqlBuffer, column);
            }

            createTableAppendPKClause(sqlBuffer, entity);
        }

        sqlBuffer.append(')');
        return sqlBuffer.toString();
    }
}
