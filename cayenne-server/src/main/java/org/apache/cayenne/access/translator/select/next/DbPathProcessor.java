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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.1
 */
public class DbPathProcessor extends PathProcessor<DbEntity> {

    DbPathProcessor(TranslatorContext context, DbEntity entity, String path) {
        super(context, entity, path);
    }

    @Override
    protected void processNormalAttribute(String next) {
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

        throw new IllegalStateException("Unable to resolve path: " + currentDbPath.toString());
    }

    @Override
    protected void processAliasedAttribute(String next) {
        DbRelationship relationship = entity.getRelationship(next);
        if(relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + next);
        }

        // todo resolve path
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
        appendCurrentPath(rel.getName());

        if (rel.isToMany() || !rel.isToPK()) {
            // match on target PK
            dbAttributeList.addAll(rel.getTargetEntity().getPrimaryKeys());
        } else {
            for(DbJoin join : rel.getJoins()) {
                dbAttributeList.add(join.getSource());
            }
        }
    }

    private void appendCurrentPath(String nextSegment) {
        if(currentDbPath.length() > 0 && currentDbPath.charAt(currentDbPath.length() - 1) != '$') {
            currentDbPath.append('.');
        }
        currentDbPath.append(nextSegment);
        if(pathIterator.isOuterJoin()) {
            currentDbPath.append('+');
        }
    }

}
