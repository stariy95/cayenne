package org.apache.cayenne.testdo.enum_test.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.enum_test.Enum1;

/**
 * Class _EnumEntity3 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _EnumEntity3 extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<Enum1> ENUM_ATTRIBUTE = Property.create("enumAttribute", Enum1.class);

    protected Enum1 enumAttribute;


    public void setEnumAttribute(Enum1 enumAttribute) {
        beforePropertyWrite("enumAttribute", this.enumAttribute, enumAttribute);
        this.enumAttribute = enumAttribute;
    }

    public Enum1 getEnumAttribute() {
        beforePropertyRead("enumAttribute");
        return this.enumAttribute;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "enumAttribute":
                return this.enumAttribute;
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
            case "enumAttribute":
                this.enumAttribute = (Enum1)val;
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
        out.writeObject(this.enumAttribute);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.enumAttribute = (Enum1)in.readObject();
    }

}
