package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_prefetch.Dep1;
import org.apache.cayenne.testdo.inheritance_prefetch.Dep2;
import org.apache.cayenne.testdo.inheritance_prefetch.Root;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.INHERITANCE_PREFETCH)
public class PrefetchWithInheritanceIT extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected ServerRuntime runtime;


    @Test
    public void testInsert_Root() throws Exception {
        Dep1 dep1 = context.newObject(Dep1.class);
        Root root = context.newObject(Root.class);
        root.setDep1(dep1);
        root.setName("test");

        context.commitChanges();
    }

    @Test
    public void selectNoData() {
        List<Dep2> data = ObjectSelect.query(Dep2.class).prefetch(Dep2.SUBS.joint()).select(context);
        assertEquals(0, data.size());
    }

    @Test
    public void selectMixedData() throws Exception {
        TableHelper rootTable = new TableHelper(dbHelper, "IP_ROOT");
        rootTable.setColumns("ID", "NAME", "TYPE", "DEP1_ID", "DEP2_ID");

        TableHelper dep1Table = new TableHelper(dbHelper, "IP_DEP1");
        dep1Table.setColumns("ID", "NAME");

        TableHelper dep2Table = new TableHelper(dbHelper, "IP_DEP2");
        dep2Table.setColumns("ID", "NAME");

        dep1Table.insert(1, "dep1-1");
        dep2Table.insert(1, "dep2-1");

        rootTable.insert(1, "root-1", 0, 1, null);
        rootTable.insert(2, "sub1-1", 1, null, 1);
        rootTable.insert(3, "sub1-2", 1, 1, null);
        rootTable.insert(4, "sub2-1", 2, null, 1);
        rootTable.insert(5, "sub2-2", 2, 1, 1);

        List<Dep1> data1 = ObjectSelect.query(Dep1.class).prefetch(Dep1.ROOTS.joint()).select(context);
        assertEquals(1, data1.size());
        assertEquals(3, data1.get(0).getRoots().size());

        List<Dep2> data2 = ObjectSelect.query(Dep2.class)
                .prefetch(Dep2.SUBS.joint())
                .select(context);
        assertEquals(1, data2.size());

        assertEquals(2, data2.get(0).getSubs().size());
    }

}
