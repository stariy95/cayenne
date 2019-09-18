package org.apache.cayenne.modeler.editor.dbimport.tree;

import java.util.List;

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

public abstract class TableNode<T extends Node> extends Node<T> {

    TableNode(String name, T parent) {
        super(name, parent);
    }
    
    @Override
    public Status getStatus(ReverseEngineering config) {
        for(FilterContainer container : getContainers(config)) {
            if(container == null) {
                continue;
            }
            Status status = includesTable(container);
            if(status != Status.NONE) {
                return status;
            }
        }

        return Status.NONE;
    }
    
    abstract List<FilterContainer> getContainers(ReverseEngineering config);

    Status includesTable(FilterContainer container) {
        if(container.getIncludeTables().isEmpty() && container.getExcludeTables().isEmpty()) {
            return Status.INCLUDED;
        }

        if(!container.getIncludeTables().isEmpty()) {
            if(getIncludeTable(container) != null) {
                return Status.INCLUDED;
            } else {
                return Status.NONE;
            }
        }

        if(!container.getExcludeTables().isEmpty()) {
            if(getExcludeTable(container) != null) {
                return Status.EXCLUDED;
            } else {
                return Status.INCLUDED;
            }
        }

        return Status.NONE;
    }

    private IncludeTable getIncludeTable(FilterContainer container) {
        for(IncludeTable table : container.getIncludeTables()) {
            if(getName().matches(table.getPattern())) {
                return table;
            }
        }
        return null;
    }

    private ExcludeTable getExcludeTable(FilterContainer container) {
        for(ExcludeTable table : container.getExcludeTables()) {
            if(getName().matches(table.getPattern())) {
                return table;
            }
        }
        return null;
    }
}
