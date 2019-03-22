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

package org.apache.cayenne.access.flush.row;

import java.util.Map;
import java.util.Objects;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public abstract class BaseDbRow implements DbRow {

    protected final Persistent object;
    protected final DbEntity entity;
    // Can be ObjEntity id or a DB row id for flattened rows
    protected final ObjectId changeId;

    protected BaseDbRow(Persistent object, DbEntity entity, ObjectId id) {
        this.object = Objects.requireNonNull(object);
        this.entity = Objects.requireNonNull(entity);
        this.changeId = Objects.requireNonNull(id);
    }

    @Override
    public DbEntity getEntity() {
        return entity;
    }

    @Override
    public ObjectId getChangeId() {
        return changeId;
    }

    public Persistent getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DbRow)) return false;

        DbRow other = (DbRow) o;
        ObjectId otherChangeId = other.getChangeId();
        // TODO: Is this assumption valid?
        //  can tmp id with proper replacement map be equal to permanent id?
        if(this.changeId == otherChangeId) {
            return true;
        }

        if(this.changeId.isTemporary() != otherChangeId.isTemporary()) {
            return false;
        }

        // TODO: should this be in the ObjectIdTmp class?
        if(this.changeId.isTemporary()) {
            if(!this.changeId.getEntityName().equals(otherChangeId.getEntityName())) {
                return false;
            }

            if(changeId.isReplacementIdAttached() != otherChangeId.isReplacementIdAttached()) {
                return false;
            }

            if(changeId.isReplacementIdAttached()) {
                for(DbAttribute pk : entity.getPrimaryKeys()) {
                    if(!changeId.getReplacementIdMap().get(pk.getName())
                            .equals(otherChangeId.getReplacementIdMap().get(pk.getName()))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return changeId.equals(otherChangeId);
    }

    @Override
    public int hashCode() {
        if(changeId.isTemporary() && changeId.isReplacementIdAttached()) {
            int hashCode = changeId.getEntityName().hashCode();
            Map<String, Object> replacementIdMap = changeId.getReplacementIdMap();
            for(DbAttribute attribute : entity.getPrimaryKeys()) {
                Object value = replacementIdMap.get(attribute.getName());
                if(value != null) {
                    hashCode = 17 * hashCode + value.hashCode();
                }
            }
            return hashCode;
        }
        return changeId.hashCode();
    }

}
