/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import java.sql.Types;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.access.sqlbuilder.sqltree.ChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.PerAttributeChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.TrimmingColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.dba.types.GeoJson;
import org.apache.cayenne.dba.types.Wkt;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
public class BaseSQLTreeProcessorNew extends SimpleNodeTreeVisitor implements SQLTreeProcessor {

    protected static final Map<String, ChildProcessor<ColumnNode>> BY_TYPE_COLUMN_PROCESSORS = new HashMap<>();
    protected static final Map<String, ChildProcessor<ValueNode>> BY_TYPE_VALUE_PROCESSORS = new HashMap<>();
    static {
        BY_TYPE_COLUMN_PROCESSORS.put(Wkt.class.getName(), (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_AsText")));
        BY_TYPE_COLUMN_PROCESSORS.put(GeoJson.class.getName(), (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_AsGeoJSON")));

        BY_TYPE_VALUE_PROCESSORS.put(Wkt.class.getName(), (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_GeomFromText")));
        BY_TYPE_VALUE_PROCESSORS.put(GeoJson.class.getName(), (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_GeomFromGeoJSON")));
    }

    final Map<NodeType, ChildProcessor<Node>> byTypeProcessors = new EnumMap<>(NodeType.class);

    public BaseSQLTreeProcessorNew() {
        registerProcessor(NodeType.COLUMN, new PerAttributeChildProcessor<>(this::getColumnAttribute, this::getColumnProcessor));
        registerProcessor(NodeType.VALUE, new PerAttributeChildProcessor<>(this::getValueAttribute, this::getValueProcessor));
    }

    @Override
    public Node process(Node node) {
        node.visit(this);
        return node;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerProcessor(NodeType nodeType, ChildProcessor childProcessor) {
        byTypeProcessors.put(nodeType, childProcessor);
    }

    protected Optional<Node> defaultProcess(Node parent, Node child, int index) {
        return Optional.empty();
    }

    @Override
    public boolean onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
        byTypeProcessors
                .getOrDefault(child.getType(), this::defaultProcess)
                .process(parent, child, index)
                .ifPresent(node -> replaceChild(parent, index, node));
        return true;
    }

    protected DbAttribute getColumnAttribute(ColumnNode node) {
        DbAttribute attribute = node.getAttribute();
        if(attribute.getType() == Types.OTHER
                && node.getParent() != null
                && node.getParent().getType() == NodeType.RESULT) {
            return attribute;
        }
        return null;
    }

    protected ChildProcessor<ColumnNode> getColumnProcessor(DbAttribute attr) {
        String type = getObjAttributeFor(attr).map(ObjAttribute::getType).orElse("default");
        return BY_TYPE_COLUMN_PROCESSORS
                .getOrDefault(type, (p,c,i) -> Optional.of(new TrimmingColumnNode(c)));
    }

    protected DbAttribute getValueAttribute(ValueNode node) {
        DbAttribute attribute = node.getAttribute();
        if(attribute.getType() == Types.OTHER
                && node.getParent() != null
                && (node.getParent().getType() == NodeType.EQUALITY || node.getParent().getType() == NodeType.INSERT_VALUES)) {
            return attribute;
        }
        return null;
    }

    protected ChildProcessor<ValueNode> getValueProcessor(DbAttribute attr) {
        String type = getObjAttributeFor(attr).map(ObjAttribute::getType).orElse("default");
        return BY_TYPE_VALUE_PROCESSORS
                .getOrDefault(type, this::defaultProcess);
    }

    protected static void replaceChild(Node parent, int index, Node newChild) {
        Node oldChild = parent.getChild(index);
        for (int i = 0; i < oldChild.getChildrenCount(); i++) {
            newChild.addChild(oldChild.getChild(i));
        }
        parent.replaceChild(index, newChild);
    }

    protected static Node wrapInFunction(Node node, String function) {
        FunctionNode functionNode = new FunctionNode(function, null);
        functionNode.addChild(node);
        return functionNode;
    }

    protected static Optional<ObjAttribute> getObjAttributeFor(DbAttribute dbAttribute) {
        if(dbAttribute == null) {
            return Optional.empty();
        }
        DbEntity dbEntity = dbAttribute.getEntity();
        for(ObjEntity objEntity: dbEntity.getDataMap().getObjEntities()) {
            ObjAttribute objAttribute = objEntity.getAttributeForDbAttribute(dbAttribute);
            if(objAttribute != null) {
                return Optional.of(objAttribute);
            }
        }
        return Optional.empty();
    }
}
