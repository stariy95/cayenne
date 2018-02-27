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

package org.apache.cayenne.access.translator.select.next;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DefaultObjectSelectTranslatorIT extends ServerCase {

    @Inject
    DataContext context;

    @Inject
    DbAdapter adapter;

    @Test
    public void simpleSql() throws Exception {
//        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class);
        SelectQuery<Artist> select = SelectQuery.query(Artist.class);
        DefaultObjectSelectTranslator translator = new DefaultObjectSelectTranslator(select, adapter, context.getEntityResolver());

        String sql = translator.getSql();
        assertEquals("SELECT  t0.ARTIST_NAME ,t0.DATE_OF_BIRTH ,t0.ARTIST_ID  FROM ARTIST t0 ", sql);
    }

    @Test
    public void sqlWhere() throws Exception {
//        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class)
//                .where(Artist.ARTIST_NAME.eq("artist"))
//                .and(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).eq("painting"));
        SelectQuery<Artist> select = SelectQuery.query(Artist.class, Artist.ARTIST_NAME.eq("artist")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).eq("painting")));

        DefaultObjectSelectTranslator translator = new DefaultObjectSelectTranslator(select, adapter, context.getEntityResolver());

        String sql = translator.getSql();
        assertEquals("SELECT  t0.ARTIST_NAME ,t0.DATE_OF_BIRTH ,t0.ARTIST_ID  " +
                "FROM ARTIST t0  JOIN PAINTING t1  ON ((t0.ARTIST_ID  =  t1.ARTIST_ID )) " +
                "WHERE  ((t0.ARTIST_NAME  = 'artist' ) AND (t1.PAINTING_TITLE  = 'painting' ))", sql);
    }

    @Test
    public void sqlWithToOneResult() throws Exception {
        SelectQuery<Painting> select = SelectQuery.query(Painting.class);

        DefaultObjectSelectTranslator translator = new DefaultObjectSelectTranslator(select, adapter, context.getEntityResolver());

        String sql = translator.getSql();
        assertEquals("SELECT  t0.ESTIMATED_PRICE ,t0.PAINTING_DESCRIPTION ,t0.PAINTING_TITLE ,t0.ARTIST_ID ,t0.GALLERY_ID ,t0.PAINTING_ID  FROM PAINTING t0 ", sql);
    }
}