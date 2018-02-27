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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.1
 */
class PathTranslator {

    static class PathTranslationResult {
        private final List<DbAttribute> dbAttributes;
        private final ObjAttribute objAttribute;

        PathTranslationResult(List<DbAttribute> dbAttributes, ObjAttribute objAttribute) {
            this.dbAttributes = dbAttributes;
            this.objAttribute = objAttribute;
        }

        Optional<ObjAttribute> getObjAttribute() {
            if(objAttribute == null) {
                return Optional.empty();
            }
            return Optional.of(objAttribute);
        }

        List<DbAttribute> getDbAttributes() {
            return dbAttributes;
        }
    }

    private final TranslatorContext context;

    PathTranslator(TranslatorContext context) {
        this.context = context;
    }

    PathTranslationResult translatePath(ObjEntity entity, String path) {
        ObjPathIterator iterator = new ObjPathIterator(context, entity, path, Collections.emptyMap());
        while(iterator.hasNext()) {
            iterator.next();
        }

        return new PathTranslationResult(iterator.getDbAttributeList(), iterator.getAttribute());
    }

}
