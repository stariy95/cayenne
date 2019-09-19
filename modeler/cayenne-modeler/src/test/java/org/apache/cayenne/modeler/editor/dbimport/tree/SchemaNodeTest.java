package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.junit.Before;
import org.junit.Test;

public class SchemaNodeTest extends BaseNodeTest {

    private SchemaNode node;

    @Before
    public void createNode() {
        CatalogNode catalogNode = new CatalogNode("catalog");
        node = new SchemaNode("schema", catalogNode);
    }

    @Test
    public void testIncludeEmptyConfig() {
        config = config().build();
        assertIncluded(node);
    }

    @Test
    public void testIncludeSchema() {
        config = config().schema(schema("schema")).build();
        assertIncluded(node);
    }

    @Test
    public void testIncludeMultipleSchemas() {
        config = config().schema(schema("schema")).schema(schema("schema1")).build();
        assertIncluded(node);
    }

    @Test
    public void testNoIncludeSchema() {
        config = config().schema(schema("schema1")).build();
        assertExcludedImplicitly(node);
    }
}
