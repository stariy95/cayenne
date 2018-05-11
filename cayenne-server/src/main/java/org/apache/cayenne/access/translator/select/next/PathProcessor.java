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
import java.util.List;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;

/**
 * @since 4.1
 */
public abstract class PathProcessor<T extends Entity> {

    protected final PathIterator pathIterator;
    protected final TranslatorContext context;
    protected final List<DbAttribute> dbAttributeList;
    protected final StringBuilder currentDbPath;

    protected T entity;
    protected DbRelationship relationship;

    public PathProcessor(TranslatorContext context, T entity, String path) {
        this.context = context;
        this.entity = entity;
        this.pathIterator = new PathIterator(path, context.getMetadata().getPathSplitAliases());
        this.currentDbPath = new StringBuilder();
        this.dbAttributeList = new ArrayList<>(1);
    }

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

    abstract protected void processAliasedAttribute(String next);

    abstract protected void processNormalAttribute(String next);

    public List<DbAttribute> getDbAttributeList() {
        return dbAttributeList;
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    public String getFinalPath() {
        return currentDbPath.toString();
    }
}
