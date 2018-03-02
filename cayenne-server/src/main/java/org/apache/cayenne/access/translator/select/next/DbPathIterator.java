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

package org.apache.cayenne.access.translator.select.next;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.1
 */
public class DbPathIterator implements Iterator<Void> {

    private final PathIterator pathIterator;
    private final TranslatorContext context;
    private final List<DbAttribute> dbAttributeList;
    private final StringBuilder currentDbPath;

    private DbRelationship relationship;
    private DbEntity entity;

    DbPathIterator(TranslatorContext context, DbEntity entity, String path, Map<String, String> pathAlias) {
        this.pathIterator = new PathIterator(path, pathAlias);
        this.context = context;
        this.dbAttributeList = new ArrayList<>(1);
        this.entity = entity;
        this.currentDbPath = new StringBuilder();
    }

    @Override
    public boolean hasNext() {
        return pathIterator.hasNext();
    }

    @Override
    public Void next() {
        String next = pathIterator.next();

        if(pathIterator.isAlias()) {
            processAliasedAttribute(next);
        } else {
            processNormalAttribute(next);
        }

        return null;
    }

    private void processNormalAttribute(String next) {
        DbAttribute dbAttribute = entity.getAttribute(next);
        if(dbAttribute != null) {
            processAttribute(dbAttribute);
            return;
        }

        DbRelationship relationship = entity.getRelationship(next);
        if(relationship != null) {
            entity = relationship.getTargetEntity();
            processRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + pathIterator.currentPath());
    }

    private void processAttribute(DbAttribute attribute) {
        appendCurrentPath(attribute.getName());
        dbAttributeList.add(attribute);
    }

    private void processRelationship(DbRelationship relationship) {
        if (!pathIterator.hasNext()) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(relationship);
        } else {
            appendCurrentPath(relationship.getName());
            context.getTableTree().addJoinTable(currentDbPath.toString(), relationship,
                    pathIterator.isOuterJoin() ? JoinType.LEFT_OUTER : JoinType.INNER);
        }
    }

    protected void processRelTermination(DbRelationship rel) {
        this.relationship = rel;
        appendCurrentPath(relationship.getName());

        for(DbJoin join : rel.getJoins()) {
            DbAttribute attribute;
            if (rel.isToMany()) {
                DbEntity ent = join.getRelationship().getTargetEntity();
                Collection<DbAttribute> pk = ent.getPrimaryKeys();
                if (pk.size() != 1) {
                    String msg = "DB_NAME expressions can only support targets with a single column PK. " +
                            "This entity has %d columns in primary key.";
                    throw new CayenneRuntimeException(msg, pk.size());
                }

                attribute = pk.iterator().next();
            } else {
                attribute = join.getSource();
            }

            dbAttributeList.add(attribute);
        }
    }

    private void processAliasedAttribute(String next) {
        DbRelationship relationship = entity.getRelationship(next);
        if(relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + next);
        }

        // todo resolve path
    }

    public List<DbAttribute> getDbAttributeList() {
        return dbAttributeList;
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    private void appendCurrentPath(String nextSegment) {
        if(currentDbPath.length() > 0) {
            currentDbPath.append('.');
        }
        currentDbPath.append(nextSegment);
    }

    public String getFinalPath() {
        return currentDbPath.toString();
    }
}
