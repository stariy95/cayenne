package org.apache.cayenne.testdo.compoundpk;

import java.math.BigDecimal;

import org.apache.cayenne.testdo.compoundpk.auto._OrderPkFk;

public class OrderPkFk extends _OrderPkFk {

    private static final long serialVersionUID = 1L;

    public PaymentPkFk newPayment() {
        Integer nextPaymentNumber = nextPaymentNumber();
        PaymentPkFk payment = getObjectContext().newObject(PaymentPkFk.class);

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
