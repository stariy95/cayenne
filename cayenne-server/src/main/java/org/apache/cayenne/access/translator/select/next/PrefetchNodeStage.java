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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.1
 */
class PrefetchNodeStage extends TranslationStage {

    PrefetchNodeStage(TranslatorContext context) {
        super(context);
    }

    @Override
    void perform() {
        PrefetchTreeNode prefetch = context.getQuery().getPrefetchTree();
        if(prefetch == null) {
            return;
        }

        ObjEntity objEntity = context.getMetadata().getObjEntity();

        for(PrefetchTreeNode node : prefetch.adjacentJointNodes()) {
            Expression prefetchExp = ExpressionFactory.exp(node.getPath());
            ASTDbPath dbPrefetch = (ASTDbPath) objEntity.translateToDbPath(prefetchExp);
            DbRelationship r = null;

            for (PathComponent<DbAttribute, DbRelationship> component :
                    objEntity.getDbEntity().resolvePath(dbPrefetch, Collections.emptyMap())) {
                r = component.getRelationship();
                context.getTableTree().addJoinTable(node.getPath(), r, JoinType.LEFT_OUTER);
            }

            if (r == null) {
                throw new CayenneRuntimeException("Invalid joint prefetch '%s' for entity: %s", node, objEntity.getName());
            }

            ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(objEntity);
            ClassDescriptor prefetchClassDescriptor = context.getResolver().getClassDescriptor(targetRel.getTargetEntityName());

            String labelPrefix = dbPrefetch.getPath();
            DescriptorColumnExtractor columnExtractor = new DescriptorColumnExtractor(context, prefetchClassDescriptor);
            columnExtractor.extract(labelPrefix);
        }
    }
}
