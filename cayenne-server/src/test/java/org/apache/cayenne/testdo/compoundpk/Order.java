package org.apache.cayenne.testdo.compoundpk;

import java.math.BigDecimal;

import org.apache.cayenne.testdo.compoundpk.auto._Order;

public class Order extends _Order {

    private static final long serialVersionUID = 1L;

    public Payment newPayment() {
        Integer nextPaymentNumber = nextPaymentNumber();
        Payment payment = getObjectContext().newObject(Payment.class);

        addToPayments(payment);

        payment.setOrderNumber(getOrderNumber());
        payment.setPaymentNumber(nextPaymentNumber);
        payment.setAmount(BigDecimal.ZERO);

        return payment;
    }

    private Integer nextPaymentNumber() {
        return getPayments().size() + 1;
    }

}
