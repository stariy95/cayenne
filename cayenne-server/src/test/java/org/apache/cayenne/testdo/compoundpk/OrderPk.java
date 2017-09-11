package org.apache.cayenne.testdo.compoundpk;

import java.math.BigDecimal;

import org.apache.cayenne.testdo.compoundpk.auto._OrderPk;

public class OrderPk extends _OrderPk {

    private static final long serialVersionUID = 1L;

    private int paymentsCounter;

    public PaymentPk newPayment() {
        Integer nextPaymentNumber = nextPaymentNumber();
        PaymentPk payment = getObjectContext().newObject(PaymentPk.class);

        addToPayments(payment);

        // payment.setOrderNumber(getOrderNumber()); // this will be set by Cayenne
        payment.setPaymentNumber(nextPaymentNumber);
        payment.setAmount(BigDecimal.ZERO);

        return payment;
    }

    private Integer nextPaymentNumber() {
        int maxPaymentId = 0;
        for(PaymentPk paymentPk : getPayments()) {
            if(paymentPk.getPaymentNumber() > maxPaymentId) {
                maxPaymentId = paymentPk.getPaymentNumber();
            }
        }
        return maxPaymentId + 1;
    }

    @Override
    protected void onPostLoad() {

    }
}
