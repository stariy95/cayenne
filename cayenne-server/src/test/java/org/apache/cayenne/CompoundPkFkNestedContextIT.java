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
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.testdo.compoundpk.OrderPk;
import org.apache.cayenne.testdo.compoundpk.OrderPkFk;
import org.apache.cayenne.testdo.compoundpk.PaymentPk;
import org.apache.cayenne.testdo.compoundpk.PaymentPkFk;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Ignore;
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

    /**
     * This is the worst case where both PK and FK are mapped as attributes,
     * root object not committed, linked object created in child context.
     *
     * Two variants:
     *   - root object do have id
     *   - root object do not have id
     *
     * Both variants works now.
     */
    @Test
    public void case1() throws Exception {
        OrderPkFk order = context.newObject(OrderPkFk.class);
        order.setOrderNumber(12345); // this is PK
        order.setValue("value");

        // switch to nested context
        ObjectContext childContext = runtime.newContext(context);
        OrderPkFk localOrder = childContext.localObject(order);
        PaymentPkFk localPayment = localOrder.newPayment();

        // local payment should have order and orderNumber set
        assertEquals(12345, localPayment.readPropertyDirectly("orderNumber"));
        assertSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertEquals(new BigDecimal(0), localPayment.readPropertyDirectly("amount"));

        // mutate local payment
        localPayment.setAmount(new BigDecimal(123));

        assertEquals(12345, localPayment.readPropertyDirectly("orderNumber"));
        assertSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertEquals(new BigDecimal(123), localPayment.readPropertyDirectly("amount"));

        localPayment.setPaymentNumber(54321);

        assertEquals(12345, localPayment.readPropertyDirectly("orderNumber"));
        assertSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertEquals(new BigDecimal(123), localPayment.readPropertyDirectly("amount"));
        assertNotNull(localOrder.getPayments().get(0));

        childContext.commitChangesToParent();

        assertEquals(new Integer(54321), order.getPayments().get(0).getPaymentNumber());

        context.commitChanges();
    }

    /**
     * This is the worst case where both PK and FK are mapped as attributes,
     * root object not committed, linked object created in child context.
     *
     * Two variants (both are failing):
     *   - root object do have id
     *   - root object do not have id
     */
    @Test
    @Ignore("Failing as meaningful FK doesn't supported properly")
    public void case2() throws Exception {
        OrderPkFk order = context.newObject(OrderPkFk.class);
        order.setOrderNumber(12345); // this call can be commented to get another result
        order.setValue("value");

        PaymentPkFk payment = order.newPayment();
        payment.setAmount(new BigDecimal(123));

        // switch to nested context
        ObjectContext childContext = runtime.newContext(context);

        OrderPkFk localOrder = childContext.localObject(order);
        PaymentPkFk localPayment = childContext.localObject(payment);

        // local payment should have order and orderNumber set
        assertEquals(null, localPayment.readPropertyDirectly("orderNumber"));
        assertSame(null, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new BigDecimal(123), localPayment.readPropertyDirectly("amount"));

        // mutate local payment
        localPayment.setAmount(new BigDecimal(321));

//        assertEquals(12345, localPayment.readPropertyDirectly("orderNumber"));
        assertNotSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());

        // this either will try to resolve Order fault object (in case orderNumber set),
        // or throw NPE (in case orderNumber is null)
        assertEquals("value", localPayment.getOrder().getValue());
        assertEquals(new BigDecimal(321), localPayment.readPropertyDirectly("amount"));

        localPayment.setPaymentNumber(54321);

        assertEquals(12345, localPayment.readPropertyDirectly("orderNumber"));
        assertNotSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new BigDecimal(321), localPayment.readPropertyDirectly("amount"));
        assertNotNull(localOrder.getPayments().get(0));

        childContext.commitChangesToParent();

        assertEquals(new Integer(54321), order.getPayments().get(0).getPaymentNumber());

        context.commitChanges();
    }

    /**
     * This is same case as case1, but this time only PKs mapped as ObjAttributes.
     *
     * Two variants:
     *   - root object do have id
     *   - root object do not have id
     */
    @Test
    public void case3() throws Exception {
        OrderPk order = context.newObject(OrderPk.class);
        order.setOrderNumber(12345); // this is PK
        order.setValue("value");

        // switch to nested context
        ObjectContext childContext = runtime.newContext(context);
        OrderPk localOrder = childContext.localObject(order);
        PaymentPk localPayment = localOrder.newPayment();

        // local payment should have order and orderNumber set
        assertSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertEquals(new BigDecimal(0), localPayment.readPropertyDirectly("amount"));

        // mutate local payment
        localPayment.setAmount(new BigDecimal(123));

        assertSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertEquals(new BigDecimal(123), localPayment.readPropertyDirectly("amount"));

        localPayment.setPaymentNumber(54321);

        assertSame(localOrder, localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertEquals(new BigDecimal(123), localPayment.readPropertyDirectly("amount"));
        assertNotNull(localOrder.getPayments().get(0));

        childContext.commitChangesToParent();

        assertEquals(new Integer(54321), order.getPayments().get(0).getPaymentNumber());

        context.commitChanges();
    }

    /**
     * This is same case as case2, but this time only PKs mapped as ObjAttributes.
     *
     * Two variants:
     *   - root object do have id
     *   - root object do not have id
     */
    @Test
    public void case4() throws Exception {
        // new order with custom PK value
        OrderPk order = context.newObject(OrderPk.class);
        order.setOrderNumber(12345);
        order.setValue("value");

        // new payment, paymentNumber will be set
        PaymentPk payment = order.newPayment();
        payment.setAmount(new BigDecimal(123));

        // switch to nested context
        ObjectContext childContext = runtime.newContext(context);
        OrderPk localOrder = childContext.localObject(order);
        PaymentPk localPayment = childContext.localObject(payment);

        // local payment should be hollow
        assertNull(localPayment.readPropertyDirectly("order"));
        assertNull(localPayment.readPropertyDirectly("amount"));

        // mutate local payment
        localPayment.setAmount(new BigDecimal(321));
        localPayment.setPaymentNumber(54321);

        // check that all data now resolved from parent context
        assertEquals(new BigDecimal(321), localPayment.readPropertyDirectly("amount"));
        assertSame(localOrder, localPayment.getOrder());
        assertEquals(new Integer(12345), localPayment.getOrder().getOrderNumber());
        assertSame(localPayment, localOrder.getPayments().get(0));

        childContext.commitChangesToParent();

        assertEquals(new Integer(54321), order.getPayments().get(0).getPaymentNumber());

        // logs should show insert with all proper IDs
        context.commitChanges();

        OrderPk orderFromDb = SelectById.query(OrderPk.class, 12345)
                .prefetch(OrderPk.PAYMENTS.joint()).selectOne(context);
        assertEquals("value", orderFromDb.getValue());
        assertEquals(new BigDecimal(321), orderFromDb.getPayments().get(0).getAmount());
    }

    @Test
    public void nestedContextLocalObjectCompound() throws Exception {
        OrderPkFk order = context.newObject(OrderPkFk.class);
        order.setOrderNumber(123);
        order.setValue("Order");

        order.newPayment();
        PaymentPkFk payment = order.getPayments().get(0);

        assertEquals(123, payment.readPropertyDirectly("orderNumber"));
        assertNotNull(payment.readPropertyDirectly("order"));
        assertSame(order, payment.getOrder());
        assertEquals(1, order.getPayments().size());
        assertSame(payment, order.getPayments().get(0));

        ObjectContext childContext = runtime.newContext(context);
        PaymentPkFk localPayment = childContext.localObject(payment);

        assertNull(localPayment.readPropertyDirectly("orderNumber"));
        assertNull(localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder()); // this is reset

        localPayment.setAmount(new BigDecimal(30));

        assertEquals(123, localPayment.readPropertyDirectly("orderNumber"));
        assertNotNull(localPayment.readPropertyDirectly("order"));
        assertNotNull(localPayment.getOrder());

        assertEquals(1, order.getPayments().size());
        assertNotEquals(payment, localPayment);
        assertSame(payment, order.getPayments().get(0));

        childContext.commitChangesToParent();
    }

}
