package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
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

    static class SchemaBuilder {
        Schema schema;

        SchemaBuilder(String name) {
            schema = new Schema(name);
        }

        SchemaBuilder includeTable(String name) {
            schema.addIncludeTable(new IncludeTable(name));
            return this;
        }

        SchemaBuilder excludeTable(String name) {
            schema.addExcludeTable(new ExcludeTable(name));
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

        CatalogBuilder excludeTable(String name) {
            catalog.addExcludeTable(new ExcludeTable(name));
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

        ConfigBuilder excludeTable(String name) {
            config.addExcludeTable(new ExcludeTable(name));
            return this;
        }

        ReverseEngineering build() {
            return config;
        }
    }
}
