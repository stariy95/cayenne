package org.apache.cayenne.testdo.meaningful_pk.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKTest1;

/**
 * Class _MeaningfulPKDep was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MeaningfulPKDep extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String PK_ATTRIBUTE_PK_COLUMN = "PK_ATTRIBUTE";

    public static final StringProperty<String> DESCR = PropertyFactory.createString("descr", String.class);
    public static final NumericProperty<Integer> PK = PropertyFactory.createNumeric("pk", Integer.class);
    public static final EntityProperty<MeaningfulPKTest1> TO_MEANINGFUL_PK = PropertyFactory.createEntity("toMeaningfulPK", MeaningfulPKTest1.class);

    protected String descr;
    protected int pk;

    protected Object toMeaningfulPK;

    public void setDescr(String descr) {
        beforePropertyWrite("descr", this.descr, descr);
        this.descr = descr;
    }

    public String getDescr() {
        beforePropertyRead("descr");
        return this.descr;
    }

    public void setPk(int pk) {
        beforePropertyWrite("pk", this.pk, pk);
        this.pk = pk;
    }

    public int getPk() {
        beforePropertyRead("pk");
        return this.pk;
    }

    public void setToMeaningfulPK(MeaningfulPKTest1 toMeaningfulPK) {
        setToOneTarget("toMeaningfulPK", toMeaningfulPK, true);
    }

    public MeaningfulPKTest1 getToMeaningfulPK() {
        return (MeaningfulPKTest1)readProperty("toMeaningfulPK");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "descr":
                return this.descr;
            case "pk":
                return this.pk;
            case "toMeaningfulPK":
                return this.toMeaningfulPK;
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
            case "descr":
                this.descr = (String)val;
                break;
            case "pk":
                this.pk = val == null ? 0 : (int)val;
                break;
            case "toMeaningfulPK":
                this.toMeaningfulPK = val;
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
        out.writeObject(this.descr);
        out.writeInt(this.pk);
        out.writeObject(this.toMeaningfulPK);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.descr = (String)in.readObject();
        this.pk = in.readInt();
        this.toMeaningfulPK = in.readObject();
    }

}
