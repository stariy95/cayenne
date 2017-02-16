package org.apache.cayenne.testdo.db2.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.db1.CrossdbM1E1;
import org.apache.cayenne.testdo.db2.CrossdbM2E1;

/**
 * Class _CrossdbM2E2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _CrossdbM2E2 extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<String> NAME = Property.create("name", String.class);
    public static final Property<CrossdbM1E1> TO_M1E1 = Property.create("toM1E1", CrossdbM1E1.class);
    public static final Property<CrossdbM2E1> TO_M2E1 = Property.create("toM2E1", CrossdbM2E1.class);

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void setToM1E1(CrossdbM1E1 toM1E1) {
        setToOneTarget("toM1E1", toM1E1, true);
    }

    public CrossdbM1E1 getToM1E1() {
        return (CrossdbM1E1)readProperty("toM1E1");
    }


    public void setToM2E1(CrossdbM2E1 toM2E1) {
        setToOneTarget("toM2E1", toM2E1, true);
    }

    public CrossdbM2E1 getToM2E1() {
        return (CrossdbM2E1)readProperty("toM2E1");
    }


}