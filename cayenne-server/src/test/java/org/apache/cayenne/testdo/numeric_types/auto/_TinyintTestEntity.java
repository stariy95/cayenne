package org.apache.cayenne.testdo.numeric_types.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

/**
 * Class _TinyintTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _TinyintTestEntity extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<Byte> TINYINT_COL = Property.create("tinyintCol", Byte.class);

    public void setTinyintCol(Byte tinyintCol) {
        writeProperty("tinyintCol", tinyintCol);
    }
    public Byte getTinyintCol() {
        return (Byte)readProperty("tinyintCol");
    }

}