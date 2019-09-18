package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.junit.Before;
import org.junit.Test;

public class SchemaTableNodeTest extends BaseNodeTest {

    private SchemaTableNode node;

    @Before
    public void createNode() {
        CatalogNode catalogNode = new CatalogNode("catalog");
        SchemaNode schemaNode = new SchemaNode("schema", catalogNode);
        node = new SchemaTableNode("table1", schemaNode);
    }

    @Test
    public void rootInclude() {
        config = config().includeTable("table1").build();

        assertIncluded(node);
    }

    @Test
    public void rootNoInclude() {
        config = config().includeTable("table2").build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void schemaIncludeAll() {
        config = config().schema(schema("schema")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaInclude() {
        config = config().schema(schema("schema").includeTable("table1")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaIncludeGlobalNoInclude() {
        config = config()
                .includeTable("table2")
                .schema(schema("schema").includeTable("table1")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoInclude() {
        config = config().schema(schema("schema").includeTable("table2")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void schemaNoIncludeGlobalNoInclude() {
        config = config()
                .includeTable("table2")
                .schema(schema("schema")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void rootExclude() {

    }

    @Test
    public void schemaExclude() {

    }
}