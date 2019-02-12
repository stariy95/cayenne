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

    // default number of thread for async manager
    private static final int DEFAULT_EXECUTOR_THREADS = 2;
    // every N-th event submission will launch cleanup task, that purge GC-ed listeners
    private static final long CLEANUP_TASK_THRESHOLD = 100L;
    // Null sender marker
    private static final Object NULL_SENDER = new Object();

    // listeners indexed by subject and by sender
    private final Map<EventSubject, Map<Object, Collection<EventListener>>> listenersBySubject = new ConcurrentHashMap<>();
    // cache of MethodHandles as it can be expensive to lookup and their count is limited
    private final Map<String, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();
    // submitted task counter, used by cleanup task submit logic
    private final AtomicLong taskSubmitCounter = new AtomicLong(0L);
    // executor service for async case
    private final ExecutorService executorService;
    // task to mass cleanup listeners with deleted references
    private final Runnable refCleanupTask;

    public DefaultEventManager() {
        this(DEFAULT_EXECUTOR_THREADS);
    }

    public DefaultEventManager(int executorThreads) {
        executorService = Executors.newFixedThreadPool(executorThreads <= 0 ? 1 : executorThreads);
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
        Objects.requireNonNull(subject, "Subject can't be null");
        Reference<?> objectRef = new WeakReference<>(Objects.requireNonNull(object, "Listener is null"));
        Class<?> listenerClass = object.getClass();
        MethodHandle methodHandle = getMethodHandle(listenerClass, method, eventClass);
        EventListener listener = new EventListener(objectRef, methodHandle);

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
        if(subject == null) {
            return false;
        }
        boolean removed = false;
        Map<Object, Collection<EventListener>> bySenders = listenersBySubject.getOrDefault(subject, Collections.emptyMap());
        for(Collection<EventListener> listeners : bySenders.values()) {
            removed |= listeners.removeIf(next -> next.refTo(listener));
        }
        return removed;
    }

    @Override
    public boolean removeListener(Object listener, EventSubject subject, Object sender) {
        return listenersBySubject
                .getOrDefault(subject, Collections.emptyMap())
                .getOrDefault(keyForSender(sender), Collections.emptyList())
                .removeIf(next -> next.refTo(listener));
    }

    @Override
    public void postEvent(CayenneEvent event, EventSubject subject) {
        postEvent(event, subject, false);
    }

    @Override
    public void postNonBlockingEvent(CayenneEvent event, EventSubject subject) {
        postEvent(event, subject, true);
    }

    private void postEvent(CayenneEvent event, EventSubject subject, boolean async) {
        submitExecutionForSender(event, subject, NULL_SENDER, async);
        submitExecutionForSender(event, subject, event.getSource(), async);
        checkAndSubmitCleanupTask();
    }

    private void submitExecutionForSender(CayenneEvent event, EventSubject subject, Object sender, boolean sync) {
        if(sender == null) {
            return;
        }
        Collection<EventListener> listeners = listenersBySubject
                .getOrDefault(subject, Collections.emptyMap())
                .getOrDefault(sender, Collections.emptyList());
        if(!listeners.isEmpty()) {
            submit(() -> listeners.removeIf(listener -> listener.apply(event)), sync);
        }
    }

    private void checkAndSubmitCleanupTask() {
        if(taskSubmitCounter.incrementAndGet() % CLEANUP_TASK_THRESHOLD == 0) {
           submit(refCleanupTask, true);
        }
    }

    private void submit(Runnable task, boolean async) {
        if(async) {
            executorService.submit(task);
        } else {
            task.run();
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
