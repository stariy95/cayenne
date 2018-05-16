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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.1
 */
@UseServerRuntime(CayenneProjects.RELATIONSHIPS_FLATTENED_PROJECT)
public class ObjPathProcessorIT3 extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    private ObjPathProcessor pathProcessor;

    @Before
    public void prepareTranslationContext() {
        TranslatorContext translatorContext = new TranslatorContext(
                new SelectQuery<>(),
                Mockito.mock(DbAdapter.class),
                context.getEntityResolver(),
                null
        );
        ObjEntity entity = context.getEntityResolver().getObjEntity("FlattenedTest5");
        pathProcessor = new ObjPathProcessor(translatorContext, entity, null);
    }

    @Test
    public void testSimpleAttributePathTranslation() {
        PathTranslationResult result = pathProcessor.process("name");
        assertEquals(1, result.getDbAttributes().size());
        assertEquals(1, result.getAttributePaths().size());

        assertEquals("", result.getLastAttributePath());
        assertEquals("NAME", result.getLastAttribute().getName());
    }

    @Test
    public void testFlattenedRelationshipPathTranslation() {
        PathTranslationResult result = pathProcessor.process("toFT1");
        assertEquals(2, result.getDbAttributes().size());
        assertEquals(2, result.getAttributePaths().size());

        assertEquals("complexJoin2", result.getAttributePaths().get(0));
        assertEquals("PK", result.getDbAttributes().get(0).getName());

        assertEquals("complexJoin2", result.getAttributePaths().get(1));
        assertEquals("FT1_FK", result.getDbAttributes().get(1).getName());
    }

}