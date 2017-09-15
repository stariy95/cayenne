package org.apache.cayenne.crypto.db.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.Property;

/**
 * Class _Table5 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Table5 extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<Integer> CRYPTO_INT1 = Property.create("cryptoInt1", Integer.class);
    public static final Property<Integer> CRYPTO_INT3 = Property.create("cryptoInt3", Integer.class);
    public static final Property<Integer> CRYPTO_INT4 = Property.create("cryptoInt4", Integer.class);

    protected int cryptoInt1;
    protected int cryptoInt3;
    protected int cryptoInt4;


    public void setCryptoInt1(int cryptoInt1) {
        beforePropertyWrite("cryptoInt1", this.cryptoInt1, cryptoInt1);
        this.cryptoInt1 = cryptoInt1;
    }

    public int getCryptoInt1() {
        beforePropertyRead("cryptoInt1");
        return this.cryptoInt1;
    }

    public void setCryptoInt3(int cryptoInt3) {
        beforePropertyWrite("cryptoInt3", this.cryptoInt3, cryptoInt3);
        this.cryptoInt3 = cryptoInt3;
    }

    public int getCryptoInt3() {
        beforePropertyRead("cryptoInt3");
        return this.cryptoInt3;
    }

    public void setCryptoInt4(int cryptoInt4) {
        beforePropertyWrite("cryptoInt4", this.cryptoInt4, cryptoInt4);
        this.cryptoInt4 = cryptoInt4;
    }

    public int getCryptoInt4() {
        beforePropertyRead("cryptoInt4");
        return this.cryptoInt4;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "cryptoInt1":
                return this.cryptoInt1;
            case "cryptoInt3":
                return this.cryptoInt3;
            case "cryptoInt4":
                return this.cryptoInt4;
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
            case "cryptoInt1":
                this.cryptoInt1 = val == null ? 0 : (Integer)val;
                break;
            case "cryptoInt3":
                this.cryptoInt3 = val == null ? 0 : (Integer)val;
                break;
            case "cryptoInt4":
                this.cryptoInt4 = val == null ? 0 : (Integer)val;
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
        out.writeInt(this.cryptoInt1);
        out.writeInt(this.cryptoInt3);
        out.writeInt(this.cryptoInt4);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.cryptoInt1 = in.readInt();
        this.cryptoInt3 = in.readInt();
        this.cryptoInt4 = in.readInt();
    }

}
