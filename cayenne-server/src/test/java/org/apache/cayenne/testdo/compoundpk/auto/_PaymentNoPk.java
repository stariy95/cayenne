package org.apache.cayenne.testdo.compoundpk.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.compoundpk.OrderNoPK;

/**
 * Class _PaymentNoPk was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _PaymentNoPk extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ORDER_NUMBER_PK_COLUMN = "order_number";
    public static final String PAYMENT_NUMBER_PK_COLUMN = "payment_number";

    public static final Property<BigDecimal> AMOUNT = Property.create("amount", BigDecimal.class);
    public static final Property<OrderNoPK> ORDER = Property.create("order", OrderNoPK.class);

    protected BigDecimal amount;

    protected Object order;

    public void setAmount(BigDecimal amount) {
        beforePropertyWrite("amount", this.amount, amount);
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        beforePropertyRead("amount");
        return this.amount;
    }

    public void setOrder(OrderNoPK order) {
        setToOneTarget("order", order, true);
    }

    public OrderNoPK getOrder() {
        return (OrderNoPK)readProperty("order");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "amount":
                return this.amount;
            case "order":
                return this.order;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "amount":
                this.amount = (BigDecimal)val;
                break;
            case "order":
                this.order = val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.amount);
        out.writeObject(this.order);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.amount = (BigDecimal)in.readObject();
        this.order = in.readObject();
    }

}
