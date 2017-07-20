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

package org.apache.cayenne;

import java.math.BigDecimal;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.compoundpk.Order;
import org.apache.cayenne.testdo.compoundpk.Payment;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
@UseServerRuntime(CayenneProjects.NESTED_CONTEXT_BUG)
public class CompoundPkFkNestedContextIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private ServerRuntime runtime;

    @Test
    public void nestedContextLocalObjectCompound() throws Exception {
        Order order = context.newObject(Order.class);
        order.setValue("Order");

        order.newPayment();
        Payment payment = order.getPayments().get(0);

        assertNull(payment.readPropertyDirectly("orderNumber"));
        assertNotNull(payment.readPropertyDirectly("order"));
        assertSame(order, payment.getOrder());
        assertEquals(1, order.getPayments().size());
        assertSame(payment, order.getPayments().get(0));

        ObjectContext childContext = runtime.newContext(context);
        Payment localPayment = childContext.localObject(payment);
        localPayment.setAmount(new BigDecimal(30));

        assertNull(localPayment.readPropertyDirectly("orderNumber"));
        assertNull(localPayment.readPropertyDirectly("order"));
        assertNull(localPayment.getOrder());

        assertEquals(1, order.getPayments().size());
        assertNotEquals(payment, localPayment);
        assertSame(payment, order.getPayments().get(0));
    }

}
