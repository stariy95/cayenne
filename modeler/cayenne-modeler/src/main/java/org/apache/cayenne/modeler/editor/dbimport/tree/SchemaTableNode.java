package org.apache.cayenne.modeler.editor.dbimport.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

public class SchemaTableNode extends TableNode<SchemaNode> {

    public SchemaTableNode(String name, SchemaNode parent) {
        super(name, parent);
    }

    @Override
    List<FilterContainer> getContainers(ReverseEngineering config) {
        List<FilterContainer> containers = new ArrayList<>();
        if(getParent() != null) {
            Status parentStatus = getParent().getStatus(config);
            if(parentStatus == Status.INCLUDED) {
                if(getParent().getParent() == null) {
                    // no catalog
                    containers.add(getParent().getSchema(config));
                } else {
                    // path via catalog
                    Catalog catalog = getParent().getParent().getCatalog(config);
                    containers.add(getParent().getSchema(catalog));
                    containers.add(catalog);
                }
            }
        }
        containers.add(config);
        return containers;
    }
}
