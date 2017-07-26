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

package org.apache.cayenne;

import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A portable global identifier for persistent objects. ObjectId can be
 * temporary (used for transient or new uncommitted objects) or permanent (used
 * for objects that have been already stored in DB). A temporary ObjectId stores
 * object entity name and a pseudo-unique binary key; permanent id stores a map
 * of values from an external persistent store (aka "primary key").
 * 
 */
public class ObjectId implements Serializable {

	private static final long serialVersionUID = -2265029098344119323L;

	private static final AtomicLong TMP_ID_SEQUENCE = new AtomicLong(1);

	// this two fields are constant per ObjEntity thus can be hidden and optimized
	protected String entityName;
	private String[] keys;

	// PK values
	private Object[] values;

	// key which is used for temporary ObjectIds only
	protected long tmpKey;

	// New values for keys in this ObjectId
	protected Map<String, Object> replacementIdMap;

	// hash code is transient to make sure id is portable across VM
	transient int hashCode;

	// exists for deserialization with Hessian and similar
	@SuppressWarnings("unused")
	private ObjectId() {
	}

	/**
	 * Creates a TEMPORARY ObjectId. Assigns a generated unique key.
	 * 
	 * @since 1.2
	 */
	public ObjectId(String entityName) {
		this(entityName, getNextTmpId());
	}

	/**
	 * @return next id
	 */
	static long getNextTmpId() {
		if(TMP_ID_SEQUENCE.get() == Long.MAX_VALUE) {
			TMP_ID_SEQUENCE.set(1);
		}

		return TMP_ID_SEQUENCE.getAndIncrement();
	}

	/**
	 * Creates a TEMPORARY id with a specified entity name and a binary key. It
	 * is a caller responsibility to provide a globally unique binary key.
	 * 
	 * @since 1.2
	 */
	public ObjectId(String entityName, long tmpKey) {
		this.entityName = entityName;
		this.tmpKey = tmpKey;
	}

	/**
	 * Creates a portable permanent ObjectId.
	 * 
	 * @param entityName
	 *            The entity name which this object id is for
	 * @param key
	 *            A key describing this object id, usually the attribute name
	 *            for the primary key
	 * @param value
	 *            The unique value for this object id
	 * @since 1.2
	 */
	public ObjectId(String entityName, String key, int value) {
		this(entityName, key, Integer.valueOf(value));
	}

	/**
	 * Creates a portable permanent ObjectId.
	 * 
	 * @param entityName
	 *            The entity name which this object id is for
	 * @param key
	 *            A key describing this object id, usually the attribute name
	 *            for the primary key
	 * @param value
	 *            The unique value for this object id
	 * @since 1.2
	 */
	public ObjectId(String entityName, String key, Object value) {
		this.entityName = entityName;

		this.keys = new String[]{key};
		this.values = new Object[]{value};
	}

	/**
	 * Creates a portable permanent ObjectId as a compound primary key.
	 * 
	 * @param entityName
	 *            The entity name which this object id is for
	 * @param idMap
	 *            Keys are usually the attribute names for each part of the
	 *            primary key. Values are unique when taken as a whole.
	 * @since 1.2
	 */
	public ObjectId(String entityName, Map<String, ?> idMap) {
		this.entityName = entityName;

		if (idMap == null || idMap.size() == 0) {
			return;
		}

		keys = new String[idMap.size()];
		values = new Object[idMap.size()];

		int idx = 0;
		for(Map.Entry<String, ?> e : idMap.entrySet()) {
			keys[idx] = e.getKey();
			values[idx] = e.getValue();
			idx++;
		}
	}

	/**
	 * Is this is temporary object id (used for objects which are not yet
	 * persisted to the data store).
	 */
	public boolean isTemporary() {
		return tmpKey != 0;
	}

	/**
	 * @since 1.2
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * Get the binary temporary object id. Null if this object id is permanent
	 * (persisted to the data store).
	 * @since 4.1 this method returns long
	 */
	public long getKey() {
		return tmpKey;
	}

	/**
	 * Returns an unmodifiable Map of persistent id values, essentially a
	 * primary key map. For temporary id returns replacement id, if it was
	 * already created. Otherwise returns an empty map.
	 */
	public Map<String, Object> getIdSnapshot() {
		if (isTemporary()) {
			return (replacementIdMap == null) ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(replacementIdMap);
		}

		if(keys == null) {
			return Collections.emptyMap();
		}

		// TODO: can we get rid of this map?
		Map<String, Object> snapshot = new HashMap<>();
		for(int i=0; i<keys.length; i++) {
			snapshot.put(keys[i], values[i]);
		}
		return Collections.unmodifiableMap(snapshot);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof ObjectId)) {
			return false;
		}

		ObjectId id = (ObjectId) object;

		if (!Util.nullSafeEquals(entityName, id.entityName)) {
			return false;
		}

		if (isTemporary()) {
			return new EqualsBuilder().append(tmpKey, id.tmpKey).isEquals();
		}

		if(keys == null) {
			return id.keys == null;
		}

		if(id.keys == null) {
			return false;
		}

		if(keys.length != id.keys.length) {
			return false;
		}

		for(int i=0; i<keys.length; i++) {
			int idx = find(id.keys, keys[i]);
			if(idx < 0) {
				return false;
			}

			if (values[i] instanceof Number) {
				if(!(id.values[idx] instanceof Number) ||
						((Number) values[i]).longValue() != ((Number) id.values[idx]).longValue()) {
					return false;
				}
			} else if (values[i].getClass().isArray()) {
				if(!new EqualsBuilder().append( values[i], id.values[idx]).isEquals()) {
					return false;
				}
			} else if (!Util.nullSafeEquals(values[i], id.values[idx])) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Linear scan search, good for small count of elements.
	 *
	 * @return index of element in array or -1 if it's not found
	 */
	static int find(String[] values, String key) {
		if(values == null) {
			return -1;
		}

		for(int i=0; i<values.length; i++) {
			if((key == null && values[i] == null)
					|| values[i].equals(key)) {
				return i;
			}
		}

		return -1;
	}

	@Override
	public int hashCode() {
		if (this.hashCode == 0) {
			HashCodeBuilder builder = new HashCodeBuilder(3, 5);
			builder.append(entityName.hashCode());
			if(tmpKey != 0) {
				builder.append(tmpKey);
			} else if(keys != null){
				Map<String, ?> snapshot = getIdSnapshot();
				List<String> keys = new ArrayList<>(snapshot.keySet());
				Collections.sort(keys);
				for (String key : keys) {
					Object value = snapshot.get(key);
					if(value instanceof Number) {
						builder.append(((Number) value).longValue());
					} else {
						builder.append(value);
					}
				}
			}

			this.hashCode = builder.toHashCode();
			assert hashCode != 0 : "Generated zero hashCode";
		}

		return hashCode;
	}

	/**
	 * Returns a non-null mutable map that can be used to append replacement id
	 * values. This allows to incrementally build a replacement GlobalID.
	 * 
	 * @since 1.2
	 */
	public Map<String, Object> getReplacementIdMap() {
		if (replacementIdMap == null) {
			replacementIdMap = new HashMap<>();
		}

		return replacementIdMap;
	}

	/**
	 * Creates and returns a replacement ObjectId. No validation of ID is done.
	 * 
	 * @since 1.2
	 */
	public ObjectId createReplacementId() {
		// merge existing and replaced ids to handle a replaced subset of
		// a compound primary key
		Map<String, Object> newIdMap = new HashMap<>(getIdSnapshot());
		if (replacementIdMap != null) {
			newIdMap.putAll(replacementIdMap);
		}
		return new ObjectId(getEntityName(), newIdMap);
	}

	/**
	 * Returns true if there is full or partial replacement id attached to this
	 * id. This method is preferrable to "!getReplacementIdMap().isEmpty()" as
	 * it avoids unneeded replacement id map creation.
	 */
	public boolean isReplacementIdAttached() {
		return replacementIdMap != null && !replacementIdMap.isEmpty();
	}

	/**
	 * A standard toString method used for debugging. It is guaranteed to
	 * produce the same string if two ObjectIds are equal.
	 */
	@Override
	public String toString() {

		StringBuilder buffer = new StringBuilder();

		buffer.append("<ObjectId:").append(entityName);

		if (isTemporary()) {
			buffer.append(", TEMP:").append(tmpKey);
		} else if(keys != null) {
			// ensure consistent order of the keys, so that toString could be
			// used as a unique key, just like id itself
			Map<String, ?> snapshot = getIdSnapshot();
			List<String> keys = new ArrayList<>(snapshot.keySet());
			Collections.sort(keys);
			for (String key : keys) {
				buffer.append(", ");
				buffer.append(String.valueOf(key)).append("=").append(snapshot.get(key));
			}
		}

		buffer.append(">");
		return buffer.toString();
	}
}
