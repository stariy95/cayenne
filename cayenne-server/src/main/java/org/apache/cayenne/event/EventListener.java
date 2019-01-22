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

package org.apache.cayenne.event;

import java.lang.invoke.MethodHandle;
import java.lang.ref.Reference;
import java.util.function.Function;

/**
 * Wrapper around object that subscribed for events.
 * @since 4.2
 */
class EventListener implements Function<CayenneEvent, Boolean> {
    /**
     * Reference to the object that will receive event
     */
    private final Reference<?> objectRef;

    /**
     * Method that processes event
     */
    private final MethodHandle handle;

    EventListener(Reference<?> objectRef, MethodHandle handle) {
        this.objectRef = objectRef;
        this.handle = handle;
    }

    /**
     * @param event to process
     * @return true if this listener's reference is null and it should be removed from collection of listeners
     */
    @Override
    public Boolean apply(CayenneEvent event) {
        try {
            // listener is dead, should be removed
            Object object = objectRef.get();
            if (object == null) {
                return true;
            }
            handle.invoke(object, event);
        } catch (Throwable ex) {
            // do nothing...
        }
        return false;
    }

    boolean refTo(Object object) {
        return object == objectRef.get();
    }
}
