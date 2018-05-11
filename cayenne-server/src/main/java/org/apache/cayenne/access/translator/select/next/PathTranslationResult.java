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

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;

/**
 * Immutable object that describes result of path translation
 *
 * @since 4.1
 */
class PathTranslationResult {

    private final String finalPath;
    private final List<DbAttribute> dbAttributes;
    private final DbRelationship dbRelationship;
    // TODO: to cache result it should also contain some form of pre-compiled joins

    PathTranslationResult(PathProcessor<?> processor) {
        this.finalPath = processor.getFinalPath();
        this.dbAttributes = processor.getDbAttributeList();
        this.dbRelationship = processor.getRelationship();
    }

    Optional<DbRelationship> getDbRelationship() {
        if (dbRelationship == null) {
            return Optional.empty();
        }
        return Optional.of(dbRelationship);
    }

    List<DbAttribute> getDbAttributes() {
        return dbAttributes;
    }

    DbAttribute getLastAttribute() {
        return dbAttributes.get(dbAttributes.size() - 1);
    }

    public String getFinalPath() {
        return finalPath;
    }
}
