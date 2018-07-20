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

package org.apache.cayenne.query;

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NullOrderingIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    UnitDbAdapter dbAdapter;

    @Before
    public void initData() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();
        tArtist
                .insert(0, "artist0", new java.sql.Date(dateBase))
                .insert(1, "artist1", null)
                .insert(2, "artist2", new java.sql.Date(dateBase + 24 * 3600 * 1000))
                .insert(3, "artist3", null)
                .insert(4, "artist4", new java.sql.Date(dateBase + 48 * 3600 * 1000));
    }

    @Test
    public void sortAscNullFirst() {
        if(!dbAdapter.supportsNullsOrdering()) {
            return;
        }

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.DATE_OF_BIRTH.asc().nullsFirst())
                .select(context);

        assertEquals(5, artists.size());
        assertNull(artists.get(0).getDateOfBirth());
        assertNull(artists.get(1).getDateOfBirth());
        assertNotNull(artists.get(2).getDateOfBirth());
        assertEquals("artist0", artists.get(2).getArtistName());
        assertNotNull(artists.get(3).getDateOfBirth());
        assertEquals("artist2", artists.get(3).getArtistName());
        assertNotNull(artists.get(4).getDateOfBirth());
        assertEquals("artist4", artists.get(4).getArtistName());
    }

    @Test
    public void sortDescNullFirst() {
        if(!dbAdapter.supportsNullsOrdering()) {
            return;
        }

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.DATE_OF_BIRTH.desc().nullsFirst())
                .select(context);

        assertEquals(5, artists.size());
        assertNull(artists.get(0).getDateOfBirth());
        assertNull(artists.get(1).getDateOfBirth());
        assertNotNull(artists.get(2).getDateOfBirth());
        assertEquals("artist4", artists.get(2).getArtistName());
        assertNotNull(artists.get(3).getDateOfBirth());
        assertEquals("artist2", artists.get(3).getArtistName());
        assertNotNull(artists.get(4).getDateOfBirth());
        assertEquals("artist0", artists.get(4).getArtistName());
    }

    @Test
    public void sortAscNullLast() {
        if(!dbAdapter.supportsNullsOrdering()) {
            return;
        }

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.DATE_OF_BIRTH.asc().nullsLast())
                .select(context);

        assertEquals(5, artists.size());
        assertNotNull(artists.get(0).getDateOfBirth());
        assertEquals("artist0", artists.get(0).getArtistName());
        assertNotNull(artists.get(1).getDateOfBirth());
        assertEquals("artist2", artists.get(1).getArtistName());
        assertNotNull(artists.get(2).getDateOfBirth());
        assertEquals("artist4", artists.get(2).getArtistName());
        assertNull(artists.get(3).getDateOfBirth());
        assertNull(artists.get(4).getDateOfBirth());
    }

    @Test
    public void sortDescNullLast() {
        if(!dbAdapter.supportsNullsOrdering()) {
            return;
        }

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.DATE_OF_BIRTH.desc().nullsLast())
                .select(context);

        assertEquals(5, artists.size());
        assertNotNull(artists.get(0).getDateOfBirth());
        assertEquals("artist4", artists.get(0).getArtistName());
        assertNotNull(artists.get(1).getDateOfBirth());
        assertEquals("artist2", artists.get(1).getArtistName());
        assertNotNull(artists.get(2).getDateOfBirth());
        assertEquals("artist0", artists.get(2).getArtistName());
        assertNull(artists.get(3).getDateOfBirth());
        assertNull(artists.get(4).getDateOfBirth());
    }
}
