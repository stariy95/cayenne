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
package org.apache.cayenne.dba.oracle;

import java.util.List;

import org.apache.cayenne.access.translator.select.JoinStack;
import org.apache.cayenne.access.translator.select.JoinTreeNode;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 3.0
 */
@Deprecated
// cloned from OpenBaseJoin stack... need better strategies of reuse...
class Oracle8JoinStack extends JoinStack {

	Oracle8JoinStack(DbAdapter dbAdapter, QueryAssembler assembler) {
		super(dbAdapter, assembler);
	}

	@Override
	protected void appendJoinsToBuilder(JoinTreeNode node) {
		DbRelationship relationship = node.getRelationship();

		if (relationship == null) {
			return;
		}

		DbEntity targetEntity = relationship.getTargetEntity();
		String targetAlias = node.getTargetTableAlias();

		selectBuilder.from(table(targetEntity.getFullyQualifiedName()).as(targetAlias));

		for (JoinTreeNode child : node.getChildren()) {
			appendJoinsToBuilder(child);
		}
	}

	@Override
	protected void appendQualifier(StringBuilder out, boolean firstQualifierElement) {
		boolean first = firstQualifierElement;
		for (JoinTreeNode node : rootNode.getChildren()) {
			if (!first) {
				out.append(" AND ");
			}
			appendQualifierSubtree(out, node);
			first = false;
		}
	}

	protected void appendQualifierSubtree(StringBuilder out, JoinTreeNode node) {
		DbRelationship relationship = node.getRelationship();

		String srcAlias = node.getSourceTableAlias();
		String targetAlias = node.getTargetTableAlias();

		List<DbJoin> joins = relationship.getJoins();
		int len = joins.size();
		for (int i = 0; i < len; i++) {
			DbJoin join = joins.get(i);

			if (i > 0) {
				out.append(" AND ");
			}

			out.append(srcAlias).append('.').append(join.getSourceName());

			switch (node.getJoinType()) {
			case INNER:
				out.append(" = ");
				break;
			case LEFT_OUTER:
				out.append(" * ");
				break;
			default:
				throw new IllegalArgumentException("Unsupported join type: " + node.getJoinType());
			}

			out.append(targetAlias).append('.').append(join.getTargetName());
		}

		for (JoinTreeNode child : node.getChildren()) {
			out.append(" AND ");
			appendQualifierSubtree(out, child);
		}
	}
}
