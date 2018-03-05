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

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.map.Entity;

/**
 * @since 4.1
 */
class PathIterator implements Iterator<String> {

    private final PathComponents components;
    private final Map<String, String> pathAlias;

    private int position;
    private boolean isAlias;
    private String nonAliasedName;
    private boolean isOuterJoin;

    PathIterator(String path, Map<String, String> pathAlias) {
        this.components = new PathComponents(path);
        this.pathAlias = pathAlias;
        this.position = 0;
    }

    public boolean isAlias() {
        return isAlias;
    }

    public boolean isOuterJoin() {
        return isOuterJoin;
    }

    @Override
    public boolean hasNext() {
        return position < components.size();
    }

    @Override
    public String next() {
        String next = components.getAll()[position++];

        if(next.endsWith(Entity.OUTER_JOIN_INDICATOR)) {
            next = next.substring(0, next.length() - Entity.OUTER_JOIN_INDICATOR.length());
            isOuterJoin = true;
        } else {
            isOuterJoin = false;
        }

        nonAliasedName = next;

        String alias = pathAlias.get(next);
        if(alias != null) {
            isAlias = true;
            next = alias;
        } else {
            isAlias = false;
        }

        return next;
    }

    public String getNonAliasedName() {
        return nonAliasedName;
    }
}
