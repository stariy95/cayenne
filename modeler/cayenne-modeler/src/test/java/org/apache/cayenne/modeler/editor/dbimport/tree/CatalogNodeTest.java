package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.junit.Before;
import org.junit.Test;

public class CatalogNodeTest extends BaseNodeTest {

    private CatalogNode node;

    @Before
    public void createNode() {
        node = new CatalogNode("catalog");
    }

    @Test
    public void testIncludeEmptyConfig() {
        config = config().build();
        assertIncluded(node);
    }

    @Test
    public void testIncludeCatalog() {
        config = config().catalog(catalog("catalog")).build();
        assertIncluded(node);
    }

    @Test
    public void testNoIncludeCatalog() {
        config = config().catalog(catalog("catalog1")).build();
        assertExcludedImplicitly(node);
    }
}
