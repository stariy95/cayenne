package org.apache.cayenne.modeler.editor.dbimport.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

public class CatalogTableNode extends TableNode<CatalogNode> {

    public CatalogTableNode(String name, CatalogNode parent) {
        super(name, parent);
    }

    @Override
    List<FilterContainer> getContainers(ReverseEngineering config) {
        List<FilterContainer> containers = new ArrayList<>();
        if(getParent() != null) {
            Status parentStatus = getParent().getStatus(config);
            if(parentStatus == Status.INCLUDED) {
                // path via catalog
                Catalog catalog = getParent().getCatalog(config);
                containers.add(catalog);
            }
        }
        containers.add(config);
        return containers;
    }
}
