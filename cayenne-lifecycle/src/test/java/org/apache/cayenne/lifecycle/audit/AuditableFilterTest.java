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
package org.apache.cayenne.lifecycle.audit;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ObjectIdDescriptor;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.lifecycle.db.Auditable1;
import org.apache.cayenne.lifecycle.db.AuditableChildUuid;
import org.apache.cayenne.lifecycle.id.IdCoder;
import org.apache.cayenne.lifecycle.relationship.ObjectIdRelationshipHandler;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Deprecated
public class AuditableFilterTest {

    private AuditableProcessor processor;
    private ServerRuntime runtime;
    private AuditableFilter filter;

    @Before
    public void setUp() throws Exception {

        EntityResolver resolver = mock(EntityResolver.class);

        ObjEntity objectEntity = new ObjEntity("CayenneDataObject");
        when(resolver.lookupObjEntity(any(Object.class))).thenReturn(objectEntity);
        when(resolver.getObjEntity(any(Persistent.class))).thenReturn(objectEntity);
        when(resolver.getObjEntity(any(Class.class))).thenReturn(objectEntity);

        DataChannel channel = mock(DataChannel.class);
        when(channel.getEntityResolver()).thenReturn(resolver);

        this.processor = mock(AuditableProcessor.class);
        this.runtime = ServerRuntime.builder().addConfig("cayenne-lifecycle.xml").build();
        this.filter = new AuditableFilter(processor);
        this.filter.init(channel);
    }

    @After
    public void tearDown() throws Exception {
        runtime.shutdown();
    }

    @Test
    public void testInsertAudit() {

        Persistent audited = mock(Persistent.class);
        filter.insertAudit(audited);
        filter.postSync();

        verify(processor).audit(audited, AuditableOperation.INSERT);
    }

    @Test
    public void testDeleteAudit() {

        Persistent audited = mock(Persistent.class);
        filter.deleteAudit(audited);
        filter.postSync();

        verify(processor).audit(audited, AuditableOperation.DELETE);
    }

    @Test
    public void testUpdateAudit() {

        Persistent audited = mock(Persistent.class);
        filter.updateAudit(audited);
        filter.postSync();

        verify(processor).audit(audited, AuditableOperation.UPDATE);
    }

    @Test
    public void testUpdateAuditChild() {

        Persistent auditedParent = mock(Persistent.class);
        DataObject audited = new MockAuditableChild();
        audited.setObjectId(new ObjectId(new ObjectIdDescriptor("MockAuditableChild", "a"), "a", 1));
        audited.writeProperty("parent", auditedParent);
        filter.updateAuditChild(audited);
        filter.postSync();

        verify(processor).audit(auditedParent, AuditableOperation.UPDATE);
    }

    @Test
    public void testUpdateAuditChildByObjectIdRelationship() {

        ObjectContext context = runtime.newContext();
        Auditable1 auditedParent = context.newObject(Auditable1.class);
        AuditableChildUuid audited = context.newObject(AuditableChildUuid.class);

        IdCoder refHandler = new IdCoder(context.getEntityResolver());
        ObjectIdRelationshipHandler handler = new ObjectIdRelationshipHandler(refHandler);
        handler.relate(audited, auditedParent);
        context.commitChanges();

        filter.updateAuditChild(audited);
        filter.postSync();
        verify(processor).audit(auditedParent, AuditableOperation.UPDATE);
    }

    @Test
    public void testOnSyncPassThrough() {

        ObjectContext context = mock(ObjectContext.class);
        GraphDiff changes = mock(GraphDiff.class);

        DataChannelFilterChain chain = mock(DataChannelFilterChain.class);

        filter.onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC, chain);
        verify(chain).onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC);

        filter.onSync(context, changes, DataChannel.ROLLBACK_CASCADE_SYNC, chain);
        verify(chain).onSync(context, changes, DataChannel.ROLLBACK_CASCADE_SYNC);
    }

    @Test
    public void testOnSyncAuditEventsCollapse() {

        ObjectContext context = mock(ObjectContext.class);
        GraphDiff changes = mock(GraphDiff.class);

        final DataObject auditedParent1 = new CayenneDataObject();
        final DataObject audited11 = new MockAuditableChild();
        audited11.writeProperty("parent", auditedParent1);
        final DataObject audited12 = new MockAuditableChild();
        audited12.writeProperty("parent", auditedParent1);
        final DataObject audited13 = new MockAuditableChild();
        audited13.writeProperty("parent", auditedParent1);

        DataChannelFilterChain chain = mock(DataChannelFilterChain.class);
        when(chain.onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC)).thenAnswer(new Answer<GraphDiff>() {

            public GraphDiff answer(InvocationOnMock invocation) throws Throwable {
                filter.updateAudit(auditedParent1);
                filter.updateAuditChild(audited11);
                filter.updateAuditChild(audited12);
                filter.updateAuditChild(audited13);
                return mock(GraphDiff.class);
            }
        });

        filter.onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC, chain);

        verify(chain).onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC);
        verify(processor).audit(auditedParent1, AuditableOperation.UPDATE);
    }
}
