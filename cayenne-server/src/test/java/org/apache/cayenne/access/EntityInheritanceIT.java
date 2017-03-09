/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.Fault;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance.BaseEntity;
import org.apache.cayenne.testdo.inheritance.RelatedEntity;
import org.apache.cayenne.testdo.inheritance.SubEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.INHERITANCE_PROJECT)
public class EntityInheritanceIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    /**
     * Test for CAY-1008: Reverse relationships may not be correctly set if inheritance is
     * used.
     */
    @Test
    public void testCAY1008() {
        RelatedEntity related = context.newObject(RelatedEntity.class);

        BaseEntity base = context.newObject(BaseEntity.class);
        base.setToRelatedEntity(related);

        assertEquals(1, related.getBaseEntities().size());
        assertEquals(0, related.getSubEntities().size());

        SubEntity sub = context.newObject(SubEntity.class);
        sub.setToRelatedEntity(related);

        assertEquals(2, related.getBaseEntities().size());

        // TODO: andrus 2008/03/28 - this fails...
        // assertEquals(1, related.getSubEntities().size());
    }

    /**
     * Test for CAY-1009: Bogus runtime relationships can mess up commit.
     */
    @Test
    public void testCAY1009() {

        // We should have only one relationship. DirectToSubEntity -> SubEntity.

        // this fails as a result of 'EntityResolver().applyObjectLayerDefaults()'
        // creating incorrect relationships
        // assertEquals(1, context
        // .getEntityResolver()
        // .getObjEntity("DirectToSubEntity")
        // .getRelationships()
        // .size());

        // We should still just have the one mapped relationship, but we in fact now have
        // two:
        // DirectToSubEntity -> BaseEntity and DirectToSubEntity -> SubEntity.

        // TODO: andrus 2008/03/28 - this fails...
        // assertEquals(1, context.getEntityResolver().getObjEntity("DirectToSubEntity")
        // .getRelationships().size());
        //
        // DirectToSubEntity direct = context.newObject(DirectToSubEntity.class);
        //
        // SubEntity sub = context.newObject(SubEntity.class);
        // sub.setToDirectToSubEntity(direct);
        //
        // assertEquals(1, direct.getSubEntities().size());
        //
        // context.deleteObject(sub);
        // assertEquals(0, direct.getSubEntities().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCAY_2257() throws Exception {
        createTestData();

        int[] semantics = {
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS,
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS
        };

        int[] expectedCountSubEntity = {3, 1, 0};
        int[] expectedCountBaseEntity = {4, 2, 0, 3, 1, 0};
        for(int s : semantics) {
            validateResult(context.performQuery(createQuery(BaseEntity.class, s)), expectedCountBaseEntity);
            validateResult(context.performQuery(createQuery(SubEntity.class, s)), expectedCountSubEntity);
        }
    }

    private void createTestData() throws Exception {
        new TableHelper(dbHelper, "RELATED_ENTITY").setColumns("RELATED_ENTITY_ID").insert(1);

        TableHelper master = new TableHelper(dbHelper, "BASE_ENTITY");
        master.setColumns("BASE_ENTITY_ID", "ENTITY_TYPE", "RELATED_ENTITY_ID")
                .insert(1, "", 1).insert(2, "", 1).insert(3, "", 1)
                .insert(4, "sub", 1).insert(5, "sub", 1).insert(6, "sub", 1);

        TableHelper toManyRelatedEntity = new TableHelper(dbHelper, "TO_MANY_RELATED_ENTITY");
        toManyRelatedEntity.setColumns("ID", "BASE_ENTITY_ID")
                .insert(1, 1).insert(2, 1).insert(3, 1).insert(4, 1)
                .insert(5, 2).insert(6, 2)
                .insert(7, 4).insert(8, 4).insert(9, 4)
                .insert(10, 5);
    }

    private <T> SelectQuery<T> createQuery(Class<T> type, int prefetch) {
        SelectQuery<T> query = SelectQuery.query(type);
        query.addOrdering(new Ordering("db:" + BaseEntity.BASE_ENTITY_ID_PK_COLUMN));
        query.addPrefetch("toManyRelatedEntities").setSemantics(prefetch);
        return query;
    }

    private void validateResult(List<BaseEntity> result, int[] expectedCount) {
        assertEquals(expectedCount.length, result.size());
        for(int i=0; i<expectedCount.length; i++) {
            validatePrefetchState(result.get(i), expectedCount[i]);
        }
    }

    private void validatePrefetchState(BaseEntity entity, int expectedCount) {
        Object value = entity.readPropertyDirectly("toManyRelatedEntities");
        assertFalse(value instanceof Fault);
        assertTrue(value instanceof List);
        assertEquals(expectedCount, ((List)value).size());
    }
}
