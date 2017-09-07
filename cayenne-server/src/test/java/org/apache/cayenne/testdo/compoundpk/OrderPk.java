package org.apache.cayenne.testdo.compoundpk;

import java.math.BigDecimal;

import org.apache.cayenne.testdo.compoundpk.auto._OrderPk;

public class OrderPk extends _OrderPk {

    private static final long serialVersionUID = 1L;

    public PaymentPk newPayment() {
        Integer nextPaymentNumber = nextPaymentNumber();
        PaymentPk payment = getObjectContext().newObject(PaymentPk.class);

        addToPayments(payment);

//        payment.setOrderNumber(getOrderNumber()); // this will be set by Cayenne
        payment.setPaymentNumber(nextPaymentNumber);
        payment.setAmount(BigDecimal.ZERO);

        return payment;
    }

    private Integer nextPaymentNumber() {
        return getPayments().size() + 1;
    }
}
