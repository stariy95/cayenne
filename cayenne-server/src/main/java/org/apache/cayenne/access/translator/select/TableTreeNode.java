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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Select;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 4.2
 */
class TableTreeNode {

    // path that spawned this node
    private final PathComponents attributePath;
    private final DbEntity entity;
    private final Select<?> subquery;
    private final String tableAlias;
    private final JoinType joinType;

    // relationship that connects this node with parent (or null if this is root)
    private final DbRelationship relationship;

    TableTreeNode(DbEntity entity, String tableAlias) {
        this.attributePath = new PathComponents("");
        this.entity = entity;
        this.subquery = null;
        this.tableAlias = tableAlias;
        this.relationship = null;
        this.joinType = null;
    }

    TableTreeNode(Select<?> subquery, String tableAlias) {
        this.attributePath = new PathComponents("");
        this.entity = null;
        this.subquery = subquery;
        this.tableAlias = tableAlias;
        this.relationship = null;
        this.joinType = null;
    }

    TableTreeNode(String path, DbRelationship relationship, String tableAlias, JoinType joinType) {
        this.attributePath = new PathComponents(path);
        this.entity = relationship.getTargetEntity();
        this.subquery = null;
        this.tableAlias = tableAlias;
        this.relationship = relationship;
        this.joinType = joinType;
    }

    PathComponents getAttributePath() {
        return attributePath;
    }

    DbEntity getEntity() {
        return entity;
    }

    boolean isSubquery() {
        return subquery != null;
    }

    TranslatableQueryWrapper getSubquery() {
        if(subquery instanceof ObjectSelect) {
            return new ObjectSelectWrapper((ObjectSelect<?>)subquery);
        }
        if(subquery instanceof ColumnSelect) {
            return new ColumnSelectWrapper((ColumnSelect<?>)subquery);
        }
        if(subquery instanceof SelectQuery) {
            return new SelectQueryWrapper((SelectQuery<?>)subquery);
        }

        throw new CayenneRuntimeException("Unsupported subquery type %s", subquery.getClass().getName());
    }

    String getTableAlias() {
        return tableAlias;
    }

    JoinType getJoinType() {
        return joinType;
    }

    DbRelationship getRelationship() {
        return relationship;
    }
}
