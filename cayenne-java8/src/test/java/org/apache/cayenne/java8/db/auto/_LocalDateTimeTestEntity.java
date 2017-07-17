package org.apache.cayenne.java8.db.auto;

import java.time.LocalDateTime;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

/**
 * Class _LocalDateTimeTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _LocalDateTimeTestEntity extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final Property<LocalDateTime> TIMESTAMP = Property.create("timestamp", LocalDateTime.class);

    public void setTimestamp(LocalDateTime timestamp) {
        writeProperty("timestamp", timestamp);
    }

    public LocalDateTime getTimestamp() {
        return (LocalDateTime)readProperty("timestamp");
    }

}
