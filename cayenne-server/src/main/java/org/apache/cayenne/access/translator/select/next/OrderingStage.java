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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.OrderingNodeBuilder;
import org.apache.cayenne.query.Ordering;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.1
 */
class OrderingStage extends TranslationStage {

    private final QualifierTranslator qualifierTranslator;

    OrderingStage(TranslatorContext context) {
        super(context);
        qualifierTranslator = new QualifierTranslator(context);
    }

    @Override
    void perform() {
        for(Ordering ordering : context.getQuery().getOrderings()) {
            processOrdering(ordering);
        }
    }

    private void processOrdering(Ordering ordering) {
        NodeBuilder orderingNode = qualifierTranslator.translate(ordering.getSortSpec());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(context);
        orderingNode.build().visit(visitor);
        String exp = visitor.getSQLString();
        ColumnDescriptor descriptor = new ColumnDescriptor(exp, Integer.MIN_VALUE);

        context.getColumnDescriptors().add(descriptor);

        if(ordering.isCaseInsensitive()) {
            orderingNode = function("UPPER", orderingNode);
        }

        OrderingNodeBuilder orderingNodeBuilder = order(orderingNode);
        if(ordering.isDescending()) {
            orderingNodeBuilder.desc();
        }
        context.getSelectBuilder().orderBy(orderingNodeBuilder);
    }
}
