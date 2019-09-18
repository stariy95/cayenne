package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

public abstract class Node<P extends Node> {
    private final String name;
    private final P parent;

    public Node(String name, P parent) {
        this.name = name;
        this.parent = parent;
    }

    P getParent() {
        return parent;
    }

    String getName() {
        return name;
    }

    public abstract Status getStatus(ReverseEngineering config);
}
