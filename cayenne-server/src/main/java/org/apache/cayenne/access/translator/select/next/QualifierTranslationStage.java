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

import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.1
 */
class QualifierTranslationStage extends TranslationStage {

    QualifierTranslationStage(TranslatorContext context) {
        super(context);
    }

    @Override
    void perform() {
        QualifierTranslator translator = new QualifierTranslator(context);

        Expression expression = context.getQuery().getWhere();

        ObjEntity entity = context.getMetadata().getObjEntity();
        if (entity != null) {
            ClassDescriptor descriptor = context.getMetadata().getClassDescriptor();
            Expression entityQualifier = descriptor.getEntityInheritanceTree().qualifierForEntityAndSubclasses();
            if (entityQualifier != null) {
                expression = (expression != null) ? expression.andExp(entityQualifier) : entityQualifier;
            }
        }

        // Attaching root Db entity's qualifier
        DbEntity dbEntity = context.getMetadata().getDbEntity();
        if (dbEntity != null) {
            Expression dbQualifier = dbEntity.getQualifier();
            if (dbQualifier != null) {
                dbQualifier = dbQualifier.transform(node -> {
                    if (node instanceof ASTObjPath) {
                        return new ASTDbPath(((SimpleNode) node).getOperand(0));
                    }
                    return node;
                });

                expression = expression == null ? dbQualifier : expression.andExp(dbQualifier);
            }
        }

        NodeBuilder qualifierBuilder = translator.translate(expression);

        if(qualifierBuilder != null) {
            context.getSelectBuilder().where(qualifierBuilder);
        }
    }
}
