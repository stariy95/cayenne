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

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.1
 */
class ObjPathProcessor extends PathProcessor<ObjEntity> {

    private ObjAttribute attribute;
    private EmbeddedAttribute embeddedAttribute;

    ObjPathProcessor(TranslatorContext context, ObjEntity entity, String path) {
        super(context, entity, path);
    }

    @Override
    public void process() {
        while(pathIterator.hasNext()) {
            String next = pathIterator.next();

            if (pathIterator.isAlias()) {
                processAliasedAttribute(next);
            } else {
                processNormalAttribute(next);
            }
        }
    }

    ObjAttribute getAttribute() {
        return attribute;
    }

    protected void processNormalAttribute(String next) {
        fetchAttribute(next);
        if(attribute != null) {
            processAttribute(attribute);
            return;
        }

        ObjRelationship relationship = entity.getRelationship(next);
        if(relationship != null) {
            processRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + currentDbPath.toString()
                + " (unknown '" + next + "' component)");
    }

    protected void fetchAttribute(String name) {
        if(embeddedAttribute != null) {
            attribute = embeddedAttribute.getAttribute(name);
            embeddedAttribute = null;
        } else {
            attribute = entity.getAttribute(name);
        }
    }

    protected void processAttribute(ObjAttribute attribute) {
        if(attribute instanceof EmbeddedAttribute) {
            embeddedAttribute = (EmbeddedAttribute)attribute;
            return;
        }

        Iterator<CayenneMapEntry> dbPathIterator = attribute.getDbPathIterator();
        while (dbPathIterator.hasNext()) {
            Object pathPart = dbPathIterator.next();
            if (pathPart == null) {
                throw new CayenneRuntimeException("ObjAttribute has no component: %s", attribute.getName());
            } else if (pathPart instanceof DbRelationship) {
                appendDbPathSegment(((DbRelationship) pathPart).getName());
                context.getTableTree().addJoinTable(currentDbPath.toString(), (DbRelationship) pathPart, JoinType.LEFT_OUTER);
            } else if (pathPart instanceof DbAttribute) {
                appendDbPathSegment(((DbAttribute) pathPart).getName());
                dbAttributeList.add((DbAttribute)pathPart);
            }
        }
    }

    protected void processRelationship(ObjRelationship relationship) {
        entity = relationship.getTargetEntity();
        if (!pathIterator.hasNext()) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(relationship);
        } else {
            // find and add joins ....
            for (DbRelationship dbRel : relationship.getDbRelationships()) {
                appendDbPathSegment(dbRel.getName());
                context.getTableTree().addJoinTable(currentDbPath.toString(), dbRel,
                        pathIterator.isOuterJoin() ? JoinType.LEFT_OUTER : JoinType.INNER);
            }
        }
    }

    protected void processRelTermination(ObjRelationship rel) {

        Iterator<DbRelationship> dbRels = rel.getDbRelationships().iterator();

        // scan DbRelationships
        while (dbRels.hasNext()) {
            DbRelationship dbRel = dbRels.next();
            appendDbPathSegment(dbRel.getName());

            // if this is a last relationship in the path, it needs special handling
            if (!dbRels.hasNext()) {
                processRelTermination(dbRel);
            } else {
                // find and add joins ....
                context.getTableTree().addJoinTable(currentDbPath.toString(), dbRel, JoinType.LEFT_OUTER);
            }
        }
    }

    protected void processRelTermination(DbRelationship rel) {
        this.relationship = rel;
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

    protected void processAliasedAttribute(String next) {
        currentDbPath.append(pathIterator.getNonAliasedName()).append('$');
        ObjRelationship relationship = entity.getRelationship(next);
        if(relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + next);
        }

        processRelationship(relationship);
    }

    protected void appendDbPathSegment(String pathSegment) {
        if(currentDbPath.length() > 0 && currentDbPath.charAt(currentDbPath.length() - 1) != '$') {
            currentDbPath.append('.');
        }
        currentDbPath.append(pathSegment);
        if(pathIterator.isOuterJoin()) {
            currentDbPath.append('+');
        }
    }
}
