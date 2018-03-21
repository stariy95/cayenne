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
import org.apache.cayenne.access.sqlbuilder.OrderingNodeBuilder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.query.Ordering;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.1
 */
class OrderingStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        QualifierTranslator qualifierTranslator = new QualifierTranslator(context);
        for(Ordering ordering : context.getQuery().getOrderings()) {
            processOrdering(qualifierTranslator, context, ordering);
        }
    }

    private void processOrdering(QualifierTranslator qualifierTranslator, TranslatorContext context, Ordering ordering) {
        Expression exp = ordering.getSortSpec();
        NodeBuilder nodeBuilder = () -> qualifierTranslator.translate(exp);

        if(ordering.isCaseInsensitive()) {
            nodeBuilder = function("UPPER", nodeBuilder);
        }

        // If query is DISTINCT than we need to add all ORDER BY clauses as result columns
        if(shouldAddToResult(context, ordering)) {
            // TODO: need to check duplicates
            context.addResultNode(nodeBuilder.build().deepCopy());
        }

        OrderingNodeBuilder orderingNodeBuilder = order(nodeBuilder);
        if(ordering.isDescending()) {
            orderingNodeBuilder.desc();
        }
        context.getSelectBuilder().orderBy(orderingNodeBuilder);
    }

    private boolean shouldAddToResult(TranslatorContext context, Ordering ordering) {
        if(context.isDistinctSuppression()) {
           return false;
        }
        if(ordering.getSortSpec() instanceof ASTAggregateFunctionCall) {
            return false;
        }
        return true;
    }

}
