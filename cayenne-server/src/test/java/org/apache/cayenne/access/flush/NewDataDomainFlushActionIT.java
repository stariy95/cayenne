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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NewDataDomainFlushActionIT extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private ObjectContext context;

    @Test
    public void testFlattenedRelationshipProcessing() {
        ObjEntity artist = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);
        assertNotNull(artist);
        ObjRelationship relationship = artist.getRelationship("groupArray");
        assertNotNull(relationship);

        ObjectId sourceId = ObjectId.of("Artist", "ARTIST_ID", 1);
        ObjectId finalTargetId = ObjectId.of("Group", "GROUP_ID", 2);

        // trying to generate this...
        Map<Id, Snapshot> snapshots = getIdSnapshotMap(relationship, sourceId, finalTargetId);

        assertEquals(1, snapshots.size());
    }

    @Test
    public void testToManyRelationshipProcessing() {
        ObjEntity artist = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);
        assertNotNull(artist);
        ObjRelationship relationship = artist.getRelationship("paintingArray");
        assertNotNull(relationship);

        ObjectId sourceId = ObjectId.of("Artist", "ARTIST_ID", 1);
        ObjectId finalTargetId = ObjectId.of("Painting", "PAINTING_ID", 2);

        // trying to generate this...
        Map<Id, Snapshot> snapshots = getIdSnapshotMap(relationship, sourceId, finalTargetId);

        assertEquals(0, snapshots.size());
    }

    @Test
    public void testToOneRelationshipProcessing() {
        ObjEntity artist = runtime.getDataDomain().getEntityResolver().getObjEntity(Painting.class);
        assertNotNull(artist);
        ObjRelationship relationship = artist.getRelationship("toArtist");
        assertNotNull(relationship);

        ObjectId sourceId = ObjectId.of("Painting", "PAINTING_ID", 1);
        ObjectId finalTargetId = ObjectId.of("Artist", "ARTIST_ID", 2);

        // trying to generate this...
        Map<Id, Snapshot> snapshots = getIdSnapshotMap(relationship, sourceId, finalTargetId);

        assertEquals(1, snapshots.size());
    }

    private Map<Id, Snapshot> getIdSnapshotMap(ObjRelationship relationship, ObjectId sourceId, ObjectId finalTargetId) {
        Map<Id, Snapshot> snapshots = new HashMap<>();

        Id srcId = new Id(sourceId.getIdSnapshot());

        List<DbRelationship> dbRelationships = relationship.getDbRelationships();
        int size = dbRelationships.size();
        for (int i = 0; i < size; i++) {
            boolean last = i == size - 1;
            DbRelationship next = dbRelationships.get(i);
            Id nextTargetId = last ? new Id(finalTargetId.getIdSnapshot()) : new Id();
            calcSnapshot(snapshots, srcId, last, next, nextTargetId);
            srcId = nextTargetId;

        }
        return snapshots;
    }

    private void calcSnapshot(Map<Id, Snapshot> snapshots, Id srcId, boolean last, DbRelationship next, Id nextTargetId) {
        for (DbJoin join : next.getJoins()) {
            DbAttribute source = join.getSource();
            if (source.isPrimaryKey()) {
                if (!last) {
                    nextTargetId.put(join.getTargetName(), (Supplier) () -> srcId.get(join.getSourceName()));
                }
            }

            if(!source.isPrimaryKey() || (join.getTarget().isPrimaryKey() && !next.isToDependentPK())) {
                snapshots.compute(srcId, (id, old) -> {
                    if (old == null) {
                        old = new Snapshot();
                    }
                    old.put(source, (Supplier) () -> nextTargetId.get(join.getTargetName()));
                    return old;
                });
            }
        }
    }

    static class Id extends HashMap<String, Object> {
        Id() {
            super();
        }

        Id(Map<String, Object> idSnapshot) {
            super(idSnapshot);
        }
    }

    static class Snapshot extends HashMap<DbAttribute, Object> {
    }


}