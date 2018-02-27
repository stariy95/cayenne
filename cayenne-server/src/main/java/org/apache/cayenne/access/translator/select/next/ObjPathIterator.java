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
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.1
 */
class ObjPathIterator implements Iterator<Void> {

    private final PathIterator pathIterator;
    private final TranslatorContext context;
    private final List<DbAttribute> dbAttributeList;
    private ObjEntity entity;
    private ObjAttribute attribute;

    ObjPathIterator(TranslatorContext context, ObjEntity entity, String path, Map<String, String> pathAlias) {
        this.context = context;
        this.pathIterator = new PathIterator(path, pathAlias);
        this.entity = entity;
        this.dbAttributeList = new ArrayList<>(2);
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

    ObjAttribute getAttribute() {
        return attribute;
    }

    List<DbAttribute> getDbAttributeList() {
        return dbAttributeList;
    }

    private void processNormalAttribute(String next) {
        attribute = entity.getAttribute(next);
        if(attribute != null) {
            processAttribute(attribute);
            return;
        }

        ObjRelationship relationship = entity.getRelationship(next);
        if(relationship != null) {
            entity = relationship.getTargetEntity();
            processRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + pathIterator.currentPath());
    }

    private void processAttribute(ObjAttribute attribute) {
        Iterator<CayenneMapEntry> dbPathIterator = attribute.getDbPathIterator();
        while (dbPathIterator.hasNext()) {
            Object pathPart = dbPathIterator.next();
            if (pathPart == null) {
                throw new CayenneRuntimeException("ObjAttribute has no component: %s", attribute.getName());
            } else if (pathPart instanceof DbRelationship) {
                context.getTableTree().addJoinTable(pathIterator.currentPath(), (DbRelationship) pathPart, JoinType.INNER);
            } else if (pathPart instanceof DbAttribute) {
                dbAttributeList.add((DbAttribute)pathPart);
            }
        }
    }

    private void processRelationship(ObjRelationship relationship) {
        if (!pathIterator.hasNext()) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(relationship);
        } else {
            // find and add joins ....
            for (DbRelationship dbRel : relationship.getDbRelationships()) {
                context.getTableTree().addJoinTable(pathIterator.currentPath(), dbRel,
                        pathIterator.isOuterJoin() ? JoinType.LEFT_OUTER : JoinType.INNER);
            }
        }
    }

    protected void processRelTermination(ObjRelationship rel) {

        Iterator<DbRelationship> dbRels = rel.getDbRelationships().iterator();
        StringBuilder relPath = new StringBuilder();

        // scan DbRelationships
        while (dbRels.hasNext()) {
            DbRelationship dbRel = dbRels.next();

            if(relPath.length() > 0) {
                relPath.append(Entity.PATH_SEPARATOR);
            }
            relPath.append(dbRel.getName());

            // if this is a last relationship in the path,
            // it needs special handling
            if (!dbRels.hasNext()) {
                processRelTermination(dbRel);
            } else {
                // find and add joins ....
                context.getTableTree().addJoinTable(pathIterator.currentPath() + relPath, dbRel,
                        pathIterator.isOuterJoin() ? JoinType.LEFT_OUTER : JoinType.INNER);
            }
        }
    }

    protected void processRelTermination(DbRelationship rel) {
        // get last DbRelationship on the list
        List<DbJoin> joins = rel.getJoins();
        if (joins.size() != 1) {
            String msg = "OBJ_PATH expressions are only supported for a single-join relationships. " +
                    "This relationship has %s joins.";
            throw new CayenneRuntimeException(msg, joins.size());
        }

        DbJoin join = joins.get(0);

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

    private void processAliasedAttribute(String next) {
        ObjRelationship relationship = entity.getRelationship(next);
        if(relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + next);
        }

        // todo resolve path
    }
}
