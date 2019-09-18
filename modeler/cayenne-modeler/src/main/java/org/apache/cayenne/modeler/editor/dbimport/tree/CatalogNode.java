package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

public class CatalogNode extends Node<Node> {

    public CatalogNode(String name) {
        super(name, null);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        if(config.getCatalogs().isEmpty()) {
            return Status.INCLUDED;
        }

        if(getCatalog(config) != null) {
            return Status.INCLUDED;
        }

        return Status.NONE;
    }

    Catalog getCatalog(ReverseEngineering config) {
        for(Catalog catalog : config.getCatalogs()) {
            if(catalog.getName().equals(getName())) {
                return catalog;
            }
        }
        return null;
    }
}
