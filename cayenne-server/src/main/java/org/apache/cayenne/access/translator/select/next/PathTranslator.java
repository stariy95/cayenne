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

import java.util.HashMap;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.1
 */
class PathTranslator {

    private final HashMap<String, PathTranslationResult> resultCache = new HashMap<>();

    private final TranslatorContext context;

    PathTranslator(TranslatorContext context) {
        this.context = context;
    }

    PathTranslationResult translatePath(ObjEntity entity, String path) {
        return resultCache.computeIfAbsent(entity.getName() + '.' + path,
                (k) -> new ObjPathProcessor(context, entity, path).process());
    }

    PathTranslationResult translatePath(DbEntity entity, String path) {
        return resultCache.computeIfAbsent(':' + entity.getName() + '.' + path,
                (k) -> new DbPathProcessor(context, entity, path).process());
    }

}