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

package org.apache.cayenne.access.flush;

import java.sql.Date;
import java.sql.Types;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class InsertSnapshotHandlerIT extends ServerCase {

    @Inject
    DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPaintings;

    @Before
    public void createArtistsDataSet() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST")
                .setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DATE);
        tPaintings = new TableHelper(dbHelper, "PAINTING")
                .setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
    }

    @Test
    public void testInsert() {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("Artist");

        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("Painting");
        painting.setToArtist(artist);

        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
    }

    @Test
    public void testDelete() throws Exception {
        tArtist.insert(2, "artist", null);

        Artist artist = SelectById.query(Artist.class, 2).selectOne(context);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        context.deleteObject(artist);
        context.commitChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
    }

    @Test
    public void testUpdate() throws Exception {
        tArtist.insert(2, "artist", null);

        Artist artist = SelectById.query(Artist.class, 2).selectOne(context);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        artist.setDateOfBirth(new Date(System.currentTimeMillis()));
        artist.setArtistName("new name");
        assertEquals(PersistenceState.MODIFIED, artist.getPersistenceState());

        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
    }
}