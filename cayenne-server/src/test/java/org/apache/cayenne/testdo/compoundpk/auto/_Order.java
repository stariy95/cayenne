package org.apache.cayenne.testdo.compoundpk.auto;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.compoundpk.Payment;

/**
 * Class _Order was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Order extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ORDER_NUMBER_PK_COLUMN = "order_number";

    public static final Property<Integer> ORDER_NUMBER = Property.create("orderNumber", Integer.class);
    public static final Property<String> VALUE = Property.create("value", String.class);
    public static final Property<List<Payment>> PAYMENTS = Property.create("payments", List.class);

    public void setOrderNumber(Integer orderNumber) {
        writeProperty("orderNumber", orderNumber);
    }
    public Integer getOrderNumber() {
        return (Integer)readProperty("orderNumber");
    }

    public void setValue(String value) {
        writeProperty("value", value);
    }
    public String getValue() {
        return (String)readProperty("value");
    }

    public void addToPayments(Payment obj) {
        addToManyTarget("payments", obj, true);
    }
    public void removeFromPayments(Payment obj) {
        removeToManyTarget("payments", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<Payment> getPayments() {
        return (List<Payment>)readProperty("payments");
    }


}
