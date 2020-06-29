package org.apache.cayenne.exp.property;

import org.apache.cayenne.dba.types.Json;
import org.apache.cayenne.exp.Expression;

public class JsonProperty<E extends Json> extends BaseProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param name       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     * @see PropertyFactory#createBase(String, Expression, Class)
     */
    protected JsonProperty(String name, Expression expression, Class<E> type) {
        super(name, expression, type);
    }


}
