package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

public class ColumnNode extends Node<TableNode<?>> {

    public ColumnNode(String name, TableNode<?> parent) {
        super(name, parent);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        return Status.NONE;
    }
}
