package org.apache.cayenne.access;

@FunctionalInterface
public interface QueryActionStage {

    boolean DONE = true;

    boolean perform(QueryActionContext context);

}
