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
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ClassDescriptor;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.1
 */
class PrefetchNodeStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        updatePrefetchNodes(context);
        processJoint(context);
        processPrefetchQuery(context);
    }

    private void updatePrefetchNodes(TranslatorContext context) {
        if(context.getQuery().getPrefetchTree() == null) {
            return;
        }
        // Set entity name, in case MixedConversionStrategy will be used to select objects from this query
        // Note: all prefetch nodes will point to query root, it is not a problem until select query can't
        // perform some sort of union or sub-queries.
        for(PrefetchTreeNode prefetch : context.getQuery().getPrefetchTree().getChildren()) {
            prefetch.setEntityName(context.getMetadata().getObjEntity().getName());
        }
    }

    private void processJoint(TranslatorContext context) {
        PrefetchTreeNode prefetch = context.getQuery().getPrefetchTree();
        if(prefetch == null) {
            return;
        }

        // TODO: use new path translator

        ObjEntity objEntity = context.getMetadata().getObjEntity();

        for(PrefetchTreeNode node : prefetch.adjacentJointNodes()) {
            Expression prefetchExp = ExpressionFactory.exp(node.getPath());
            ASTDbPath dbPrefetch = (ASTDbPath) objEntity.translateToDbPath(prefetchExp);
            DbRelationship r = null;
            StringBuilder fullPath = new StringBuilder();

            for (PathComponent<DbAttribute, DbRelationship> component :
                    objEntity.getDbEntity().resolvePath(dbPrefetch, Collections.emptyMap())) {
                r = component.getRelationship();
                if(fullPath.length() > 0) {
                    fullPath.append('.');
                } else {
                    // add mark of prefetch to not overlap with query qualifiers
                    fullPath.append("p:");
                }
                fullPath.append(r.getName());
                context.getTableTree().addJoinTable(fullPath.toString(), r, JoinType.LEFT_OUTER);
            }

            if (r == null) {
                throw new CayenneRuntimeException("Invalid joint prefetch '%s' for entity: %s", node, objEntity.getName());
            }

            ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(objEntity);
            ClassDescriptor prefetchClassDescriptor = context.getResolver().getClassDescriptor(targetRel.getTargetEntityName());

            String labelPrefix = dbPrefetch.getPath();
            DescriptorColumnExtractor columnExtractor = new DescriptorColumnExtractor(context, prefetchClassDescriptor, labelPrefix);
            columnExtractor.extract(fullPath.toString());
        }
    }

    private void processPrefetchQuery(TranslatorContext context) {
        if(!(context.getQuery() instanceof PrefetchSelectQuery)) {
            return;
        }

        PathTranslator pathTranslator = new PathTranslator(context);
        PrefetchSelectQuery prefetchSelectQuery = (PrefetchSelectQuery)context.getQuery();
        for(String prefetchPath: prefetchSelectQuery.getResultPaths()) {
            ASTDbPath pathExp = (ASTDbPath) context.getMetadata().getClassDescriptor().getEntity()
                    .translateToDbPath(ExpressionFactory.exp(prefetchPath));

            String path = pathExp.getPath();
            PathTranslator.PathTranslationResult result = pathTranslator
                    .translatePath(context.getMetadata().getDbEntity(), path);
            result.getDbRelationship().ifPresent(r -> {
                DbEntity targetEntity = r.getTargetEntity();
                context.getTableTree().addJoinTable(path, r, JoinType.INNER);
                for (DbAttribute pk : targetEntity.getPrimaryKeys()) {
                    // note that we may select a source attribute, but label it as target for simplified snapshot processing
                    String finalPath = path + '.' + pk.getName();
                    String alias = context.getTableTree().aliasForAttributePath(finalPath);

                    Node columnNode = table(alias).column(pk).build();
                    context.addResultNode(columnNode, finalPath);
                }
            });
        }
    }
}
