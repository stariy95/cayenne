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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

	// descriptor for ObjEntity PKs
	private ObjectIdDescriptor descriptor;

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
	public ObjectId(ObjectIdDescriptor descriptor) {
		this(descriptor, getNextTmpId());
	}

	/**
	 * @return next id
	 */
	static long getNextTmpId() {
		TMP_ID_SEQUENCE.compareAndSet(Long.MAX_VALUE, 1);
		return TMP_ID_SEQUENCE.getAndIncrement();
	}

	/**
	 * Creates a TEMPORARY id with a specified entity name and a binary key. It
	 * is a caller responsibility to provide a globally unique binary key.
	 * 
	 * @since 1.2
	 * @since 4.1 tmpKey is long
	 */
	public ObjectId(ObjectIdDescriptor descriptor, long tmpKey) {
		this.descriptor = descriptor;
		this.tmpKey = tmpKey;
	}

	/**
	 * Creates a portable permanent ObjectId.
	 * 
	 * @param descriptor
	 *            The entity ID descriptor which this object id is for
	 * @param key
	 *            A key describing this object id, usually the attribute name
	 *            for the primary key
	 * @param value
	 *            The unique value for this object id
	 * @since 1.2
	 */
	public ObjectId(ObjectIdDescriptor descriptor, String key, int value) {
		this(descriptor, key, Integer.valueOf(value));
	}

	/**
	 * Creates a portable permanent ObjectId.
	 * 
	 * @param descriptor
	 *            The entity name which this object id is for
	 * @param key
	 *            A key describing this object id, usually the attribute name
	 *            for the primary key
	 * @param value
	 *            The unique value for this object id
	 * @since 1.2
	 */
	public ObjectId(ObjectIdDescriptor descriptor, String key, Object value) {
		this.descriptor = descriptor;
		this.values = new Object[]{value};
	}

	/**
	 * Creates a portable permanent ObjectId as a compound primary key.
	 * 
	 * @param descriptor
	 *            The entity name which this object id is for
	 * @param idMap
	 *            Keys are usually the attribute names for each part of the
	 *            primary key. Values are unique when taken as a whole.
	 * @since 1.2
	 */
	public ObjectId(ObjectIdDescriptor descriptor, Map<String, ?> idMap) {
		this.descriptor = descriptor;

		if (idMap == null || idMap.size() == 0) {
			return;
		}

		int pkLength = descriptor.getPkNames().length;
		values = new Object[pkLength];
		for(int i=0; i<pkLength; i++) {
			values[i] = idMap.get(descriptor.getPkNames()[i]);
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
		return descriptor.getEntityName();
	}

	/**
	 * @since 4.1
	 */
	public ObjectIdDescriptor getDescriptor() {
		return descriptor;
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

		if(values == null) {
			return Collections.emptyMap();
		}

		return new SnapshotView();
	}

	public Collection<Object> getValues() {
		return Collections.unmodifiableCollection(Arrays.asList(values));
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

		if (!Util.nullSafeEquals(descriptor, id.descriptor)) {
			return false;
		}

		if (isTemporary()) {
			return tmpKey == id.tmpKey;
		}

		if(values == null) {
			return id.values == null;
		}

		if(id.values == null || values.length != id.values.length) {
			return false;
		}

		for(int i=0; i<values.length; i++) {
			if (values[i] instanceof Number) {
				if(!(id.values[i] instanceof Number) ||
						((Number) values[i]).longValue() != ((Number) id.values[i]).longValue()) {
					return false;
				}
			} else if (values[i].getClass().isArray()) {
				if(!new EqualsBuilder().append( values[i], id.values[i]).isEquals()) {
					return false;
				}
			} else if (!Util.nullSafeEquals(values[i], id.values[i])) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		if (this.hashCode == 0) {
			HashCodeBuilder builder = new HashCodeBuilder(3, 5);
			builder.append(descriptor);
			if(tmpKey != 0) {
				builder.append(tmpKey);
			} else if(values != null){
				for (Object value : values) {
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
		return new ObjectId(descriptor, newIdMap);
	}

	/**
	 * Returns true if there is full or partial replacement id attached to this
	 * id. This method is preferable to "!getReplacementIdMap().isEmpty()" as
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

		buffer.append("<ObjectId:").append(descriptor.getEntityName());

		if (isTemporary()) {
			buffer.append(", TEMP:").append(tmpKey);
		} else if(values != null) {
			// ensure consistent order of the keys, so that toString could be
			// used as a unique key, just like id itself
			int idx = 0;
			for (String key : descriptor.getPkNames()) {
				buffer.append(", ");
				buffer.append(String.valueOf(key)).append("=").append(values[idx++]);
			}
		}

		buffer.append(">");
		return buffer.toString();
	}

	/**
	 * Custom Map implementation used to export snapshot to outer world.
	 * This class is immutable as it is just a "view" over ObjectId internals.
	 * Note also that it doesn't check "values" of ObjectId, the caller must make sure it's not null.
	 * This class uses full array scan to find keys and values, that shouldn't be a problem as there is hardly
	 * can be over 10 of PK in an object.
	 */
	private class SnapshotView implements Map<String, Object> {

        @Override
        public int size() {
            return values.length;
        }

        @Override
        public boolean isEmpty() {
            return values.length > 0;
        }

        @Override
        public boolean containsKey(Object key) {
            for(String pk : descriptor.getPkNames()) {
                if(pk.equals(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            for(Object v : values) {
                if(value == null) {
					if(v == null) {
                    	return true;
					}
                } else if(value.equals(v)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object get(Object key) {
            for(int i=0; i<descriptor.getPkNames().length; i++) {
                if(descriptor.getPkNames()[i].equals(key)) {
                    return values[i];
                }
            }
            return null;
        }

		@Override
		public Set<String> keySet() {
			return new HashSet<>(Arrays.asList(descriptor.getPkNames()));
		}

		@Override
		public Collection<Object> values() {
			return Collections.unmodifiableCollection(Arrays.asList(values));
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}

			if (!(o instanceof Map)) {
				return false;
			}
			Map<?,?> m = (Map<?,?>) o;
			if (m.size() != size()) {
				return false;
			}

			try {
				for(int i=0; i<descriptor.getPkNames().length; i++) {
					String key = descriptor.getPkNames()[i];
					Object value = values[i];
					if (value == null) {
						if (!(m.get(key) == null && m.containsKey(key))) {
							return false;
						}
					} else if (!value.equals(m.get(key))) {
						return false;
					}
				}
			} catch (ClassCastException | NullPointerException unused) {
				return false;
			}

			return true;
		}

		/**
		 * This method should return same hashCode as will for example HashMap with same content.
		 * @return hash code
		 */
		@Override
		public int hashCode() {
        	int hashCode = 0;
			for(int i=0; i<descriptor.getPkNames().length; i++) {
				String key = descriptor.getPkNames()[i];
				hashCode += Objects.hashCode(key) ^ Objects.hashCode(values[i]);
			}
			return hashCode;
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			// not used for now, so it's unimplemented
			throw new UnsupportedOperationException();
		}

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}
