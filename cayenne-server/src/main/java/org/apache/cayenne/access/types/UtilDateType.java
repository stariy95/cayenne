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

package org.apache.cayenne.access.types;

import org.apache.cayenne.dba.TypesMapping;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

/**
 * Maps <code>java.util.Date</code> to any of the three database date/time types: TIME,
 * DATE, TIMESTAMP.
 */
public class UtilDateType implements ExtendedType<Date> {

    private final Calendar calendar = Calendar.getInstance();

    /**
     * Returns "java.util.Date".
     */
    @Override
    public String getClassName() {
        return Date.class.getName();
    }

    @Override
    public Date materializeObject(ResultSet rs, int index, int type) throws Exception {
        Date val;
        switch (type) {
            case Types.TIMESTAMP:
                val = rs.getTimestamp(index, calendar);
                break;
            case Types.DATE:
                val = rs.getDate(index, calendar);
                break;
            case Types.TIME:
                val = rs.getTime(index, calendar);
                break;
            default:
                val = rs.getTimestamp(index, calendar);
                break;
        }

        // return java.util.Date instead of subclass
        return val == null ? null : new Date(val.getTime());
    }

    @Override
    public Date materializeObject(CallableStatement cs, int index, int type) throws Exception {
        Date val;
        switch (type) {
            case Types.TIMESTAMP:
                val = cs.getTimestamp(index, calendar);
                break;
            case Types.DATE:
                val = cs.getDate(index, calendar);
                break;
            case Types.TIME:
                val = cs.getTime(index, calendar);
                break;
            default:
                val = cs.getTimestamp(index, calendar);
                break;
        }

        // return java.util.Date instead of subclass
        return val == null ? null : new Date(val.getTime());
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Date value,
            int pos,
            int type,
            int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        } else {
            if (type == Types.DATE) {
                statement.setDate(pos, new java.sql.Date(value.getTime()), calendar);
            } else if (type == Types.TIME) {
                Time time = new Time(value.getTime());
                statement.setTime(pos, time, calendar);
            } else if (type == Types.TIMESTAMP) {
                statement.setTimestamp(pos, new java.sql.Timestamp(value.getTime()), calendar);
            } else {
                throw new IllegalArgumentException(
                        "Only DATE, TIME or TIMESTAMP can be mapped as '" + getClassName()
                                + "', got " + TypesMapping.getSqlNameByType(type));
            }
        }
    }

    @Override
    public String toString(Date value) {
        if (value == null) {
            return "NULL";
        }

        long time = value.getTime();
        return '\'' + new java.sql.Timestamp(time).toString() + '\'';
    }
}
