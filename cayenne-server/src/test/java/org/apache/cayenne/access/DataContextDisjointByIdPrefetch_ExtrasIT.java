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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.things.Bag;
import org.apache.cayenne.testdo.things.Ball;
import org.apache.cayenne.testdo.things.Box;
import org.apache.cayenne.testdo.things.Thing;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;

@UseServerRuntime(CayenneProjects.THINGS_PROJECT)
public class DataContextDisjointByIdPrefetch_ExtrasIT extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    private ServerRuntime runtime;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    protected TableHelper tBag;
    protected TableHelper tBall;
    protected TableHelper tBox;
    protected TableHelper tBoxInfo;
    protected TableHelper tBoxThing;
    protected TableHelper tThing;

    @Before
    public void setUp() throws Exception {
        tBag = new TableHelper(dbHelper, "BAG");
        tBag.setColumns("ID", "NAME");

        tBall = new TableHelper(dbHelper, "BALL");
        tBall.setColumns("ID", "BOX_ID", "THING_VOLUME", "THING_WEIGHT");

        tBox = new TableHelper(dbHelper, "BOX");
        tBox.setColumns("ID", "BAG_ID", "NAME");

        tBoxInfo = new TableHelper(dbHelper, "BOX_INFO");
        tBoxInfo.setColumns("ID" ,"BOX_ID", "COLOR");

        tBoxThing = new TableHelper(dbHelper, "BOX_THING");
        tBoxThing.setColumns("BOX_ID", "THING_VOLUME", "THING_WEIGHT");

        tThing = new TableHelper(dbHelper, "THING");
        tThing.setColumns("ID", "VOLUME", "WEIGHT");
    }

    private void createBagWithTwoBoxesAndPlentyOfBallsDataSet() throws Exception {

        // because of SQLServer need to enable identity inserts per transaction,
        // inserting these objects via Cayenne, and then flushing the cache
        // http://technet.microsoft.com/en-us/library/ms188059.aspx

        tBag.insert(1, "b1");
        tBox.insert(1, 1, "big");
        tBoxInfo.insert(1, 1, "red");
        tBox.insert(2, 1, "small");
        tBoxInfo.insert(2, 2, "green");
        tThing.insert(1, 10, 10);
        tBall.insert(1, 1, 10, 10);
        tThing.insert(2, 20, 20);
        tBall.insert(2, 1, 20, 20);
        tThing.insert(3, 30, 30);
        tBall.insert(3, 2, 30, 30);
        tThing.insert(4, 40, 40);
        tBall.insert(4, 2, 40, 40);
        tThing.insert(5, 20, 10);
        tBall.insert(5, 2, 20, 10);
        tThing.insert(6, 40, 30);
        tBall.insert(6, 2, 40, 30);

        tBoxThing.insert(1, 10, 10);
        tBoxThing.insert(1, 20, 20);
        tBoxThing.insert(2, 30, 30);
        tBoxThing.insert(1, 40, 40);
        tBoxThing.insert(1, 20, 10);
        tBoxThing.insert(1, 40, 30);

    }

    @Test
    public void testFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery<Bag> query = SelectQuery.query(Bag.class);
        query.addPrefetch(Bag.BALLS.disjointById());

        final List<Bag> result = query.select(context);

        // SELECT
        //      t0.THING_VOLUME ,t0.THING_WEIGHT ,t0.BOX_ID ,t0.ID
        //      ,t2.ID
        // FROM
        //      BALL t0
        //      JOIN BOX t1  ON ((t0.BOX_ID  =  t1.ID ))
        //      JOIN BAG t2  ON ((t1.BAG_ID  =  t2.ID ))
        // WHERE t1.BAG_ID = ?  [bind: 1->BAG_ID:1]

        // SELECT DISTINCT
        //      t0.THING_VOLUME ,t0.THING_WEIGHT ,t0.BOX_ID ,t0.ID
        //      ,t2.ID
        // FROM
        //      BALL t0
        //      JOIN BOX t1  ON ((t0.BOX_ID  =  t1.ID ))
        //      JOIN BAG t2  ON ((t1.BAG_ID  =  t2.ID ))
        // WHERE  (t1.BAG_ID  = ? )   [bind: 1->BAG_ID:1]

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Bag b1 = result.get(0);
            @SuppressWarnings("unchecked")
            List<Ball> balls = (List<Ball>) b1.readPropertyDirectly(Bag.BALLS.getName());
            assertNotNull(balls);
            assertFalse(((ValueHolder) balls).isFault());
            assertEquals(6, balls.size());

            List<Integer> volumes = new ArrayList<>();
            for (Ball b : balls) {
                assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
                volumes.add(b.getThingVolume());
            }
            assertThat(volumes, hasItems(10, 20, 30, 40, 20, 40));
        });
    }

    @Test
    public void testFlattenedMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Box.class);
        query.addPrefetch(Box.THINGS.disjointById());
        final List<Box> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                List<Integer> volumes = new ArrayList<Integer>();
                for (Box box : result) {
                    List<Thing> things = (List<Thing>) box.readPropertyDirectly(Box.THINGS.getName());
                    assertNotNull(things);
                    assertFalse(((ValueHolder) things).isFault());
                    for (Thing t : things) {
                        assertEquals(PersistenceState.COMMITTED, t.getPersistenceState());
                        volumes.add(t.getVolume());
                    }
                }
                assertEquals(6, volumes.size());
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40)));
            }
        });
    }

    @Test
    public void testLongFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.THINGS.disjointById());
        final List<Bag> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                Bag b1 = result.get(0);
                List<Thing> things = (List<Thing>) b1.readPropertyDirectly(Bag.THINGS.getName());
                assertNotNull(things);
                assertFalse(((ValueHolder) things).isFault());
                assertEquals(6, things.size());

                List<Integer> volumes = new ArrayList<Integer>();
                for (Thing t : things) {
                    assertEquals(PersistenceState.COMMITTED, t.getPersistenceState());
                    volumes.add(t.getVolume());
                }
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 20, 30, 40, 40)));
            }
        });
    }

    @Test
    public void testMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery<Ball> query = SelectQuery.query(Ball.class);
        query.orQualifier(Ball.THING_VOLUME.eq(40).andExp(Ball.THING_WEIGHT.eq(30)));
        query.orQualifier(Ball.THING_VOLUME.eq(20).andExp(Ball.THING_WEIGHT.eq(10)));

        query.addPrefetch(Ball.THING.disjointById());

        final List<Ball> balls = query.select(context);


        // SELECT
        //      t0.THING_VOLUME ,t0.THING_WEIGHT ,t0.BOX_ID ,
        //      t0.THING_VOLUME ,t0.THING_WEIGHT ,t0.ID
        // FROM BALL t0
        // WHERE  (((t0.THING_VOLUME  = ? ) AND (t0.THING_WEIGHT  = ? )) OR ((t0.THING_VOLUME  = ? ) AND (t0.THING_WEIGHT  = ? )))   [bind: 1->THING_VOLUME:40, 2->THING_WEIGHT:30, 3->THING_VOLUME:20, 4->THING_WEIGHT:10]

        // SELECT DISTINCT
        //      t0.VOLUME ,t0.WEIGHT ,
        //      t0.VOLUME ,t0.WEIGHT ,t0.ID ,
        //      t1.ID
        // FROM
        //      THING t0
        //      JOIN BALL t1  ON (((t0.VOLUME  =  t1.THING_VOLUME ) AND  (t0.WEIGHT  =  t1.THING_WEIGHT )))
        // WHERE  (((t0.VOLUME  = ? ) AND (t0.WEIGHT  = ? )) OR ((t0.VOLUME  = ? ) AND (t0.WEIGHT  = ? )))   [bind: 1->VOLUME:20, 2->WEIGHT:10, 3->VOLUME:40, 4->WEIGHT:30]

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertEquals(2, balls.size());
            for(Ball next : balls) {
                assertNotNull(balls.get(0).getThing());
                next.getThing().getVolume();
            }
        });
    }

    @Test
    public void testJointPrefetchInParent() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Box.class);
        query.addPrefetch(Box.BALLS.disjointById());
        query.addPrefetch(Box.BALLS.dot(Ball.THING).disjointById());
        final List<Box> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                List<Integer> volumes = new ArrayList<Integer>();
                for (Box box : result) {
                    List<Ball> balls = (List<Ball>) box.readPropertyDirectly(Box.BALLS.getName());
                    assertNotNull(balls);
                    assertFalse(((ValueHolder) balls).isFault());
                    for (Ball ball : balls) {
                        Thing thing = (Thing) ball.readPropertyDirectly(Ball.THING.getName());
                        assertNotNull(thing);
                        assertEquals(PersistenceState.COMMITTED, thing.getPersistenceState());
                        volumes.add(thing.getVolume());
                    }
                }
                assertEquals(6, volumes.size());
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40)));
            }
        });
    }

    @Test
    public void testJointPrefetchInChild() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery<Bag> query = new SelectQuery<Bag>(Bag.class);
        query.addPrefetch(Bag.BOXES.disjointById());
        query.addPrefetch(Bag.BOXES.dot(Box.BALLS).joint());
        final List<Bag> result = context.select(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

        	@Override
            public void execute() {
                assertFalse(result.isEmpty());

                Bag bag = result.get(0);
                List<Box> boxes = (List<Box>) bag.readPropertyDirectly(Bag.BOXES.getName());
                assertNotNull(boxes);
                assertFalse(((ValueHolder) boxes).isFault());
                assertEquals(2, boxes.size());

                Box big = null;
                List<String> names = new ArrayList<String>();
                for (Box box : boxes) {
                    assertEquals(PersistenceState.COMMITTED, box.getPersistenceState());
                    names.add(box.getName());
                    if (box.getName().equals("big")) {
                        big = box;
                    }
                }
                assertTrue(names.contains("big"));
                assertTrue(names.contains("small"));

                List<Ball> balls = (List<Ball>) big.readPropertyDirectly(Box.BALLS.getName());
                assertNotNull(balls);
                assertFalse(((ValueHolder) balls).isFault());
                assertEquals(2, balls.size());
                List<Integer> volumes = new ArrayList<Integer>();
                for (Ball ball : balls) {
                    assertEquals(PersistenceState.COMMITTED, ball.getPersistenceState());
                    volumes.add(ball.getThingVolume());
                }
                assertTrue(volumes.containsAll(Arrays.asList(10, 20)));
            }
        });
    }
}
