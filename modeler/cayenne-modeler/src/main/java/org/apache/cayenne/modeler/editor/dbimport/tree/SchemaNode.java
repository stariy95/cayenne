package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;

public class SchemaNode extends Node<CatalogNode> {
    public SchemaNode(String name, CatalogNode parent) {
        super(name, parent);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        // check via parent path
        if(getParent() != null) {
            Status parentStatus = getParent().getStatus(config);
            if(parentStatus == Status.INCLUDED) {
                Catalog parentCatalog = getParent().getCatalog(config);
                if(includesSchema(parentCatalog) == Status.INCLUDED) {
                    return Status.INCLUDED;
                }
            }
        }

        // check root
        return includesSchema(config);
    }

    Status includesSchema(SchemaContainer container) {
        if(container.getSchemas().isEmpty()) {
            return Status.INCLUDED;
        }
        if(getSchema(container) != null) {
            return Status.INCLUDED;
        }
        return Status.NONE;
    }

    Schema getSchema(SchemaContainer container) {
        for(Schema schema : container.getSchemas()) {
            if(schema.getName().equals(getName())) {
                return schema;
            }
        }
        return null;
    }
}
