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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
class ObjectSelectMetadata extends BaseQueryMetadata {

	private static final long serialVersionUID = -4936484509363047672L;

	private Map<String, String> pathSplitAliases;

	@Override
	void copyFromInfo(QueryMetadata info) {
		super.copyFromInfo(info);
		this.pathSplitAliases = new HashMap<>(info.getPathSplitAliases());
	}

	boolean resolve(Object root, EntityResolver resolver, ObjectSelect<?> query) {

		if (super.resolve(root, resolver)) {
			// generate unique cache key, but only if we are caching..
			if (cacheStrategy != null && cacheStrategy != QueryCacheStrategy.NO_CACHE) {
				this.cacheKey = makeCacheKey(query, resolver);
			}

			resolveAutoAliases(query);
			return true;
		}

		return false;
	}

	private String makeCacheKey(FluentSelect<?> query, EntityResolver resolver) {

		// create a unique key based on entity or columns, qualifier, ordering, fetch offset and limit

		StringBuilder key = new StringBuilder();
		// handler to create string out of expressions, created lazily
		TraversalHandler traversalHandler = null;

		ObjEntity entity = getObjEntity();
		if (entity != null) {
			key.append(entity.getName());
		} else if (dbEntity != null) {
			key.append("db:").append(dbEntity.getName());
		}

		if (query.getWhere() != null) {
			key.append('/');
            traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			query.getWhere().traverse(traversalHandler);
		}

		if (query.getOrderings() != null && !query.getOrderings().isEmpty()) {
			for (Ordering o : query.getOrderings()) {
				key.append('/').append(o.getSortSpecString());
				if (!o.isAscending()) {
					key.append(":d");
				}

				if (o.isCaseInsensitive()) {
					key.append(":i");
				}
			}
		}

		if (fetchLimit > 0 || fetchOffset > 0) {
			key.append('/');
			if (fetchOffset > 0) {
				key.append('o').append(fetchOffset);
			}
			if (fetchLimit > 0) {
				key.append('l').append(fetchLimit);
			}
		}

		// add prefetch to cache key per CAY-2349
		if(prefetchTree != null) {
			prefetchTree.traverse(new ToCacheKeyPrefetchProcessor(key));
		}

		return key.toString();
	}

	private void resolveAutoAliases(ObjectSelect<?> query) {
		resolveQualifierAliases(query);
        resolveOrderingAliases(query);
	}

	private void resolveQualifierAliases(ObjectSelect<?> query) {
		Expression qualifier = query.getWhere();
		if (qualifier != null) {
			resolveAutoAliases(qualifier);
		}
	}


	private void resolveOrderingAliases(ObjectSelect<?> query) {
        Collection<Ordering> orderings = query.getOrderings();
        if(orderings != null) {
            for(Ordering ordering : orderings) {
                Expression sortSpec = ordering.getSortSpec();
                if(sortSpec != null) {
                    resolveAutoAliases(sortSpec);
                }
            }
        }
    }

	private void resolveAutoAliases(Expression expression) {
		Map<String, String> aliases = expression.getPathAliases();
		if (!aliases.isEmpty()) {
			if (pathSplitAliases == null) {
				pathSplitAliases = new HashMap<>();
			}

			for(Map.Entry<String, String> entry : aliases.entrySet()) {
				pathSplitAliases.compute(entry.getKey(), (key, value) -> {
					if(value != null && !value.equals(entry.getValue())){
						throw new CayenneRuntimeException("Can't add the same alias to different path segments.");
					} else {
						return entry.getValue();
					}
				});
			}
		}

		int len = expression.getOperandCount();
		for (int i = 0; i < len; i++) {
			Object operand = expression.getOperand(i);
			if (operand instanceof Expression) {
				resolveAutoAliases((Expression) operand);
			}
		}
	}

	@Override
	public Map<String, String> getPathSplitAliases() {
		return pathSplitAliases != null ? pathSplitAliases : Collections.emptyMap();
	}

	/**
	 * Expression traverse handler to create cache key string out of Expression.
	 * {@link Expression#appendAsString(Appendable)} where previously used for that,
	 * but it can't handle custom value objects properly (see CAY-2210).
	 *
	 * @see ValueObjectTypeRegistry
	 */
	static class ToCacheKeyTraversalHandler implements TraversalHandler {

		private ValueObjectTypeRegistry registry;
		private StringBuilder out;

		ToCacheKeyTraversalHandler(ValueObjectTypeRegistry registry, StringBuilder out) {
			this.registry = registry;
			this.out = out;
		}

		@Override
		public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
			out.append(',');
		}

		@Override
		public void startNode(Expression node, Expression parentNode) {
			if(node.getType() == Expression.FUNCTION_CALL) {
				out.append(((ASTFunctionCall)node).getFunctionName()).append('(');
			} else {
				out.append(node.getType()).append('(');
			}
		}

		@Override
		public void endNode(Expression node, Expression parentNode) {
			out.append(')');
		}

		@Override
		public void objectNode(Object leaf, Expression parentNode) {
			if(leaf == null) {
				out.append("null");
				return;
			}

			if(leaf instanceof ASTScalar) {
				leaf = ((ASTScalar) leaf).getValue();
			} else if(leaf instanceof Object[]) {
				for(Object value : (Object[])leaf) {
					objectNode(value, parentNode);
					out.append(',');
				}
				return;
			}

			if (leaf instanceof Persistent) {
				ObjectId id = ((Persistent) leaf).getObjectId();
				Object encode = (id != null) ? id : leaf;
				out.append(encode);
			} else if (leaf instanceof Enum<?>) {
				Enum<?> e = (Enum<?>) leaf;
				out.append("e:").append(leaf.getClass().getName()).append(':').append(e.ordinal());
			} else {
				ValueObjectType<Object, ?> valueObjectType;
				if (registry == null || (valueObjectType = registry.getValueType(leaf.getClass())) == null) {
					// Registry will be null in cayenne-client context.
					// Maybe we shouldn't create cache key at all in that case...
					out.append(leaf);
				} else {
					out.append(valueObjectType.toCacheKey(leaf));
				}
			}
		}
	}

	/**
	 * Prefetch processor that append prefetch tree into cache key.
	 * @since 4.0
	 */
	static class ToCacheKeyPrefetchProcessor implements PrefetchProcessor {

		StringBuilder out;

		ToCacheKeyPrefetchProcessor(StringBuilder out) {
			this.out = out;
		}

		@Override
		public boolean startPhantomPrefetch(PrefetchTreeNode node) {
			return true;
		}

		@Override
		public boolean startDisjointPrefetch(PrefetchTreeNode node) {
			out.append("/pd:").append(node.getPath());
			return true;
		}

		@Override
		public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
			out.append("/pi:").append(node.getPath());
			return true;
		}

		@Override
		public boolean startJointPrefetch(PrefetchTreeNode node) {
			out.append("/pj:").append(node.getPath());
			return true;
		}

		@Override
		public boolean startUnknownPrefetch(PrefetchTreeNode node) {
			out.append("/pu:").append(node.getPath());
			return true;
		}

		@Override
		public void finishPrefetch(PrefetchTreeNode node) {
		}
	}
}
