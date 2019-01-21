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
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.BeforeScopeEnd;

/**
 * A default implementation of {@link EventManager}.
 *
 * @since 3.1, completely new implementation in 4.2
 */
public class DefaultEventManager implements EventManager {

    private static final int DEFAULT_EXECUTOR_THREADS = 2;

    // every N-th event submission will launch cleanup task, that purge GC-ed listeners
    private static final long CLEANUP_TASK_THRESHOLD = 1000L;

    private static final Object NULL_SENDER = new Object();

    private final Map<EventSubject, Map<Object, Collection<EventListener>>> listenersBySubject = new ConcurrentHashMap<>();
    private final Map<String, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();
    private final AtomicLong taskSubmitCounter = new AtomicLong(0L);
    private final ExecutorService executorService;
    private final Runnable refCleanupTask;

    public DefaultEventManager() {
        this(DEFAULT_EXECUTOR_THREADS);
    }

    public DefaultEventManager(int executorThreads) {
        executorService = Executors.newFixedThreadPool(executorThreads);
        refCleanupTask = () -> listenersBySubject.values().forEach(
                bySender -> bySender.values().forEach(
                        listeners -> listeners.removeIf(listener -> listener.refTo(null))
                )
        );
    }

    @Override
    public boolean isSingleThreaded() {
        return false;
    }

    @Override
    public void addListener(Object listener, String methodName, Class<?> eventParameterClass, EventSubject subject) {
        addNonBlockingListener(listener, methodName, eventParameterClass, subject, null);
    }

    @Override
    public void addNonBlockingListener(Object listener, String methodName, Class<?> eventParameterClass, EventSubject subject) {
        addNonBlockingListener(listener, methodName, eventParameterClass, subject, null);
    }

    @Override
    public void addListener(Object listener, String methodName, Class<?> eventParameterClass, EventSubject subject, Object sender) {
        addNonBlockingListener(listener, methodName, eventParameterClass, subject, sender);
    }

    @Override
    public void addNonBlockingListener(Object object, String method, Class<?> eventClass, EventSubject subject, Object sender) {
        Reference<?> objectRef = new WeakReference<>(Objects.requireNonNull(object, "Listener is null"));
        Class<?> listenerClass = object.getClass();
        MethodHandle methodHandle = getMethodHandle(listenerClass, method, eventClass);
        EventListener listener = new EventListener(objectRef, methodHandle, sender);

        listenersBySubject
                .computeIfAbsent(subject, subj -> new ConcurrentHashMap<>())
                .computeIfAbsent(keyForSender(sender), s -> new ConcurrentLinkedQueue<>())
                .add(listener);
    }

    private Object keyForSender(Object sender) {
        if(sender == null) {
            return NULL_SENDER;
        }
        return sender;
    }

    @Override
    public boolean removeListener(Object listener) {
        boolean removed = false;
        for(Map<Object, Collection<EventListener>> listenersBySender : listenersBySubject.values()) {
            for(Collection<EventListener> listeners: listenersBySender.values()) {
                removed |= listeners.removeIf(next -> next.refTo(listener));
            }
        }
        return removed;
    }

    @Override
    public boolean removeAllListeners(EventSubject subject) {
        Map<Object, Collection<EventListener>> listeners = listenersBySubject.remove(subject);
        return listeners != null && !listeners.isEmpty();
    }

    @Override
    public boolean removeListener(Object listener, EventSubject subject) {
        listenersBySubject.values()
                .forEach(bySenders -> bySenders
                    .getOrDefault(subject, Collections.emptyList())
                    .removeIf(next -> next.refTo(listener))
                );
        return false;
    }

    @Override
    public boolean removeListener(Object listener, EventSubject subject, Object sender) {
        return removeListener(listener, subject);
    }

    @Override
    public void postEvent(CayenneEvent event, EventSubject subject) {
        postNonBlockingEvent(event, subject);
    }

    @Override
    public void postNonBlockingEvent(CayenneEvent event, EventSubject subject) {
        // TODO: send for NULL sender...
        Collection<EventListener> listeners = listenersBySubject
                .getOrDefault(subject, Collections.emptyMap())
                .getOrDefault(event.getSource(), Collections.emptyList());
        if(!listeners.isEmpty()) {
            executorService.submit(() -> listeners.removeIf(listener -> listener.apply(event)));
        }
        checkAndSubmitCleanupTask();
    }

    private void checkAndSubmitCleanupTask() {
        if(taskSubmitCounter.incrementAndGet() % CLEANUP_TASK_THRESHOLD == 0) {
            executorService.submit(refCleanupTask);
        }
    }

    private MethodHandle getMethodHandle(Class<?> listenerClass, String method, Class<?> eventClass) {
        String methodKey = listenerClass.getName() + '/' + method + '/' + eventClass.getSimpleName();
        return methodHandleCache.computeIfAbsent(methodKey, key -> {
            try {
                Method methodRef = listenerClass.getDeclaredMethod(method, eventClass);
                methodRef.setAccessible(true);
                return MethodHandles.lookup().unreflect(methodRef);
            } catch (NoSuchMethodException | IllegalAccessException ex) {
                throw new CayenneRuntimeException("Unable to find method %s() for %s", method, listenerClass);
            }
        });
    }

    @BeforeScopeEnd
    public void shutdown() {
        executorService.shutdownNow();
    }

}
