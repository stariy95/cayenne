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

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SqlBuilder.*;
import static org.junit.Assert.*;


/**
 * @since 4.1
 */
public class SqlBuilderTest {

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
                count(table("p").column("PAINTING_TITLE").as("p_count")))
                .distinct()
                .from(
                        table("ARTIST").as("a"),
                        leftJoin(table("PAINTING").as("p"))
                                .on(table("a").column("ARTIST_ID")
                                        .eq(table("p").column("ARTIST_ID"))
                                        .and(table("p").column("ESTIMATED_PRICE").gt(value(10)))))
                .groupBy(table("a").column("ARTIST_ID"))
                .where(
                        table("a").column("ARTIST_NAME")
                                .eq(value("Picasso"))
                                .and(exists(select(value('*'))
                                                .from(table("GALLERY").as("g"))
                                                .where(table("g").column("GALLERY_ID").eq(table("p").column("GALLERY_ID")))))
                                .and(value(1).eq(value(1)))
                                .or(value(false)))
                .orderBy(column("p_count").desc())
                .buildNode()
                .visit(visitor);

        assertEquals("SELECT DISTINCT  " +
                "a.ARTIST_ID AS a_id " +
                "COUNT p.PAINTING_TITLE AS p_count " +
                "FROM ARTIST AS a " +
                "JOIN PAINTING AS p ON a.ARTIST_ID =  p.ARTIST_ID  AND   p.ESTIMATED_PRICE >  10  " +
                "WHERE a.ARTIST_NAME =  'Picasso'  " +
                "GROUP BY a.ARTIST_ID " +
                "ORDER BY p_count DESC ", visitor.getString());


    }
}