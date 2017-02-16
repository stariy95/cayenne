package org.apache.cayenne.testdo.return_types.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

/**
 * Class _ReturnTypesMapLobs1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ReturnTypesMapLobs1 extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String AAAID_PK_COLUMN = "AAAID";

    public static final Property<String> CLOB_COLUMN = Property.create("clobColumn", String.class);
    public static final Property<String> NCLOB_COLUMN = Property.create("nclobColumn", String.class);

    public void setClobColumn(String clobColumn) {
        writeProperty("clobColumn", clobColumn);
    }
    public String getClobColumn() {
        return (String)readProperty("clobColumn");
    }

    public void setNclobColumn(String nclobColumn) {
        writeProperty("nclobColumn", nclobColumn);
    }
    public String getNclobColumn() {
        return (String)readProperty("nclobColumn");
    }

}