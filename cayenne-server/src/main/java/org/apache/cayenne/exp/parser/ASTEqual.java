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

package org.apache.cayenne.exp.parser;

import java.util.Collection;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ValueInjector;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.CayenneMapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Equal To" expression.
 * 
 * @since 1.1
 */
public class ASTEqual extends ConditionNode implements ValueInjector {

	private static final long serialVersionUID = 1211234198602067833L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ASTEqual.class);

	/**
	 * Constructor used by expression parser. Do not invoke directly.
	 */
	ASTEqual(int id) {
		super(id);
	}

	public ASTEqual() {
		super(ExpressionParserTreeConstants.JJTEQUAL);
	}

	/**
	 * Creates "Equal To" expression.
	 */
	public ASTEqual(SimpleNode path, Object value) {
		super(ExpressionParserTreeConstants.JJTEQUAL);
		jjtAddChild(path, 0);
		jjtAddChild(new ASTScalar(value), 1);
		connectChildren();
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 2;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		Object o2 = evaluatedChildren[1];
		return evaluateImpl(o, o2) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Compares two objects, if one of them is array, 'in' operation is
	 * performed
	 */
	static boolean evaluateImpl(Object o1, Object o2) {
		// TODO: maybe we need a comparison "strategy" here, instead of
		// a switch of all possible cases? ... there were other requests for
		// more relaxed type-unsafe comparison (e.g. numbers to strings)

		if (o1 == null && o2 == null) {
			return true;
		} else if (o1 != null) {
			// Per CAY-419 we perform 'in' comparison if one object is a list, and other is not
			if (o2 instanceof Collection) {
				for (Object element : ((Collection<?>) o2)) {
					if (element != null && Evaluator.evaluator(element).eq(element, o1)) {
						return true;
					}
				}
				return false;
			}

			return Evaluator.evaluator(o1).eq(o1, o2);
		}
		return false;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTEqual(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "=";
	}

	@Override
	protected String getEJBQLExpressionOperator(int index) {
		if (jjtGetChild(1) instanceof ASTScalar && ((ASTScalar) jjtGetChild(1)).getValue() == null) {
			// for ejbql, we need "is null" instead of "= null"
			return "is";
		}
		return getExpressionOperator(index);
	}

	@Override
	public int getType() {
		return Expression.EQUAL_TO;
	}

	public void injectValue(Object o) {
		// try to inject value, if one of the operands is scalar, and other is a
		// path

		Node[] args = new Node[] { jjtGetChild(0), jjtGetChild(1) };

		int scalarIndex = -1;
		if (args[0] instanceof ASTScalar) {
			scalarIndex = 0;
		} else if (args[1] instanceof ASTScalar) {
			scalarIndex = 1;
		}

		if (scalarIndex != -1 && args[1 - scalarIndex] instanceof ASTObjPath) {
			// inject
			ASTObjPath path = (ASTObjPath) args[1 - scalarIndex];
			try {
				Object value = evaluateChild(scalarIndex, o);
				value = dynamicCastValue(o, path, value);
				path.injectValue(o, value);
			} catch (Exception ex) {
				LOGGER.warn("Failed to inject value " + " on path " + path.getPath() + " to " + o, ex);
			}
		}
	}

	/**
	 * Try to cast value to attribute type.
	 * For now it can only cast String into Enum value.
	 * TODO: can we cast some other types here, like String to Number?
	 */
	@SuppressWarnings("unchecked")
	private Object dynamicCastValue(Object object, ASTObjPath path, Object value) {
		if(object instanceof DataObject) {
            ObjectContext context = ((DataObject) object).getObjectContext();
            ObjectId objectId = ((DataObject) object).getObjectId();
            if(context != null && objectId != null) {
                ObjEntity entity = context.getEntityResolver().getObjEntity(objectId.getEntityName());
                CayenneMapEntry entry = path.evaluateEntityNode(entity);
                if(entry instanceof ObjAttribute
						&& ((ObjAttribute) entry).getJavaClass().isEnum()
						&& value instanceof String) {
					// finally...
					value = Enum.valueOf((Class<Enum>)((ObjAttribute) entry).getJavaClass(), (String)value);
                }
            }
        }
		return value;
	}
}
