package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;

import static org.junit.Assert.assertEquals;

class BaseNodeTest {

    ReverseEngineering config;

    void assertIncluded(Node<?> node) {
        assertEquals(Status.INCLUDED, node.getStatus(config));
    }

    void assertExcludedImplicitly(Node<?> node) {
        assertEquals(Status.EXCLUDED_IMPLICIT, node.getStatus(config));
    }

    void assertExcludedExplicitly(Node<?> node) {
        assertEquals(Status.EXCLUDED_EXPLICIT, node.getStatus(config));
    }

    static ConfigBuilder config() {
        return new ConfigBuilder();
    }

    static SchemaBuilder schema(String name) {
        return new SchemaBuilder(name);
    }

    static CatalogBuilder catalog(String name) {
        return new CatalogBuilder(name);
    }

    static IncludeTableBuilder table(String name) {
        return new IncludeTableBuilder(name);
    }

    static class IncludeTableBuilder {
        final IncludeTable table;

        IncludeTableBuilder(String name) {
            table = new IncludeTable(name);
        }

        IncludeTableBuilder includeColumn(String name) {
            table.addIncludeColumn(new IncludeColumn(name));
            return this;
        }

        IncludeTableBuilder excludeColumn(String name) {
            table.addExcludeColumn(new ExcludeColumn(name));
            return this;
        }

        IncludeTable build() {
            return table;
        }
    }

    static class SchemaBuilder {
        Schema schema;

        SchemaBuilder(String name) {
            schema = new Schema(name);
        }

        SchemaBuilder includeTable(String name) {
            schema.addIncludeTable(new IncludeTable(name));
            return this;
        }

        SchemaBuilder includeTable(IncludeTableBuilder tableBuilder) {
            schema.addIncludeTable(tableBuilder.build());
            return this;
        }

        SchemaBuilder excludeTable(String name) {
            schema.addExcludeTable(new ExcludeTable(name));
            return this;
        }

        SchemaBuilder includeColumn(String name) {
            schema.addIncludeColumn(new IncludeColumn(name));
            return this;
        }

        SchemaBuilder excludeColumn(String name) {
            schema.addExcludeColumn(new ExcludeColumn(name));
            return this;
        }

        Schema build() {
            return schema;
        }
    }

    static class CatalogBuilder {
        Catalog catalog;

        protected CatalogBuilder(String name) {
            catalog = new Catalog(name);
        }

        CatalogBuilder schema(SchemaBuilder schemaBuilder) {
            catalog.addSchema(schemaBuilder.build());
            return this;
        }

        CatalogBuilder includeTable(String name) {
            catalog.addIncludeTable(new IncludeTable(name));
            return this;
        }

        CatalogBuilder includeTable(IncludeTableBuilder tableBuilder) {
            catalog.addIncludeTable(tableBuilder.build());
            return this;
        }

        CatalogBuilder excludeTable(String name) {
            catalog.addExcludeTable(new ExcludeTable(name));
            return this;
        }

        CatalogBuilder includeColumn(String name) {
            catalog.addIncludeColumn(new IncludeColumn(name));
            return this;
        }

        CatalogBuilder excludeColumn(String name) {
            catalog.addExcludeColumn(new ExcludeColumn(name));
            return this;
        }

        Catalog build() {
            return catalog;
        }
    }

    static class ConfigBuilder {
        ReverseEngineering config;

        protected ConfigBuilder() {
            config = new ReverseEngineering();
        }

        ConfigBuilder schema(SchemaBuilder schemaBuilder) {
            config.addSchema(schemaBuilder.build());
            return this;
        }

        ConfigBuilder catalog(CatalogBuilder catalogBuilder) {
            config.addCatalog(catalogBuilder.build());
            return this;
        }

        ConfigBuilder includeTable(String name) {
            config.addIncludeTable(new IncludeTable(name));
            return this;
        }

        ConfigBuilder includeTable(IncludeTableBuilder tableBuilder) {
            config.addIncludeTable(tableBuilder.build());
            return this;
        }

        ConfigBuilder excludeTable(String name) {
            config.addExcludeTable(new ExcludeTable(name));
            return this;
        }

        ConfigBuilder includeColumn(String name) {
            config.addIncludeColumn(new IncludeColumn(name));
            return this;
        }

        ConfigBuilder excludeColumn(String name) {
            config.addExcludeColumn(new ExcludeColumn(name));
            return this;
        }

        ReverseEngineering build() {
            return config;
        }
    }
}
