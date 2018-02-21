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

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SqlBuilder.*;
import static org.junit.Assert.*;


/**
 * @since 4.1
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SqlBuilderIT extends ServerCase {

    @Inject
    DataContext context;

    @Test
    public void testTranslation() {
        SelectQuery<Artist> selectQuery = SelectQuery.query(Artist.class);
        selectQuery.setQualifier(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("painting%"));
        selectQuery.select(context);
    }

    @Test
    public void selectDemo() {

        // SELECT DISTINCT
        //      a.ARTIST_ID AS a_id
        //      COUNT p.PAINTING_TITLE AS p_count
        // FROM
        //      ARTIST AS a
        //      LEFT JOIN PAINTING AS p ON a.ARTIST_ID =  p.ARTIST_ID  AND   p.ESTIMATED_PRICE >  10
        // WHERE
        //      a.ARTIST_NAME =  'Picasso'
        //      AND EXISTS SELECT* FROM GALLERY AS g WHERE g.GALLERY_ID =  p.GALLERY_ID
        //      AND   1 =  1
        //      OR  false
        // GROUP BY a.ARTIST_ID
        // ORDER BY p_count DESC

        ToStringVisitor visitor = new ToStringVisitor();

        select(table("a").column("ARTIST_ID").as("a_id"),
                count(table("p").column("PAINTING_TITLE")).as("p_count"))
                .distinct()
                .from(table("ARTIST").as("a"))
                .from(leftJoin(table("PAINTING").as("p"))
                                .on(table("a").column("ARTIST_ID")
                                        .eq(table("p").column("ARTIST_ID"))
                                        .and(table("p").column("ESTIMATED_PRICE").gt(value(10)))))
                .where(
                        table("a").column("ARTIST_NAME")
                                .eq(value("Picasso"))
                                .and(exists(select(value('*'))
                                                .from(table("GALLERY").as("g"))
                                                .where(table("g").column("GALLERY_ID").eq(table("p").column("GALLERY_ID")))))
                                .and(value(1).eq(value(1)))
                                .or(value(false)))
                .groupBy(table("a").column("ARTIST_ID"))
                .having(not(count(table("p").column("PAINTING_TITLE")).gt(value(3))))
                .orderBy(column("p_count").desc())
                .buildNode()
                .visit(visitor);

//        assertEquals("SELECT DISTINCT   a.ARTIST_ID AS a_id ," +
//                "COUNT(p.PAINTING_TITLE ) AS p_count  " +
//                "FROM ARTIST AS a  " +
//                "LEFT JOIN PAINTING AS p  ON(((a.ARTIST_ID  =  p.ARTIST_ID ) AND  (p.ESTIMATED_PRICE  >  10 ))) " +
//                "WHERE ((((a.ARTIST_NAME  =  'Picasso' ) " +
//                "AND  EXISTS(SELECT  *  FROM GALLERY AS g  WHERE (g.GALLERY_ID  =  p.GALLERY_ID ))) " +
//                "AND  (1  =  1 )) OR  false ) " +
//                "GROUP BY a.ARTIST_ID  " +
//                "HAVING (COUNT(p.PAINTING_TITLE ) >  3 ) " +
//                "ORDER BY  p_count  DESC ", visitor.getString());

    }

}