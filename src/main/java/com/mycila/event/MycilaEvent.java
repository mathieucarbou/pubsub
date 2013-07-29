/**
 * Copyright (C) 2010 Mycila <mathieu.carbou@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycila.event;

import com.google.common.collect.Iterables;
import com.mycila.event.annotation.Answers;
import com.mycila.event.annotation.Subscribe;
import com.mycila.event.internal.EventQueue;
import com.mycila.event.internal.Message;
import com.mycila.event.internal.Proxy;
import com.mycila.event.internal.PublisherInterceptor;
import com.mycila.event.internal.Subscribers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;
import static com.mycila.event.internal.Ensure.notNull;
import static com.mycila.event.internal.Reflect.annotatedBy;
import static com.mycila.event.internal.Reflect.findMethods;
import static com.mycila.event.internal.Reflect.getTargetClass;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class MycilaEvent {

    private final Dispatcher dispatcher;

    private MycilaEvent(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public <T> BlockingQueue<T> createSynchronousQueue(Topics topics, Class<T> eventType) {
        EventQueue<T> queue = new EventQueue<T>(new SynchronousQueue<T>());
        dispatcher.subscribe(topics, eventType, queue);
        return queue;
    }

    public <T> BlockingQueue<T> createBoundedQueue(Topics topics, Class<T> eventType, int capacity) {
        EventQueue<T> queue = new EventQueue<T>(new ArrayBlockingQueue<T>(capacity));
        dispatcher.subscribe(topics, eventType, queue);
        return queue;
    }

    public <T> BlockingQueue<T> createUnboundedQueue(Topics topics, Class<T> eventType) {
        EventQueue<T> queue = new EventQueue<T>(new LinkedBlockingQueue<T>());
        dispatcher.subscribe(topics, eventType, queue);
        return queue;
    }

    public <T> BlockingQueue<T> createPriorityQueue(Topics topics, Class<T> eventType) {
        EventQueue<T> queue = new EventQueue<T>(new PriorityBlockingQueue<T>());
        dispatcher.subscribe(topics, eventType, queue);
        return queue;
    }

    public Requestor createRequestor(final Topic topic) {
        return new Requestor() {
            @Override
            public Topic getTopic() {
                return topic;
            }

            @Override
            public <R> SendableRequest<R> createRequest() {
                return createRequest(Collections.<Object>emptyList());
            }

            @Override
            public <R> SendableRequest<R> createRequest(Object parameter) {
                return createRequest(Arrays.asList(parameter));
            }

            @Override
            public <R> SendableRequest<R> createRequest(Object... parameters) {
                return createRequest(Arrays.asList(parameters));
            }

            @Override
            public <R> SendableRequest<R> createRequest(final List<?> parameters) {
                checkNotNull(topic, "Missing topic");
                return new SendableRequest<R>() {
                    final Collection<FutureListener<R>> listeners = new CopyOnWriteArrayList<FutureListener<R>>();

                    @Override
                    public List<?> getParameters() {
                        return parameters;
                    }

                    @Override
                    public SendableRequest<R> addListener(FutureListener<R> listener) {
                        listeners.add(listener);
                        return this;
                    }

                    @Override
                    public Topic getTopic() {
                        return topic;
                    }

                    @Override
                    public Future<R> send() {
                        Message<R> msg = new Message<R>(listeners, parameters);
                        dispatcher.publish(topic, msg);
                        return msg;
                    }

                    @Override
                    public String toString() {
                        return "Request on " + topic;
                    }
                };
            }

            @Override
            public String toString() {
                return "Requestor on " + topic;
            }
        };
    }

    public Publisher createPublisher(final Iterable<Topic> topics) {
        return createPublisher(Iterables.toArray(checkNotNull(topics, "Missing topics"), Topic.class));
    }

    public Publisher createPublisher(final Topic... topics) {
        checkNotNull(topics, "Missing topics");
        return new Publisher() {
            @Override
            public Topic[] getTopics() {
                return topics;
            }

            @Override
            public void publish(Object event) {
                for (Topic topic : topics) {
                    dispatcher.publish(topic, event);
                }
            }

            @Override
            public String toString() {
                return "Publisher on " + Arrays.toString(topics);
            }
        };
    }

    public <T> T instanciate(Class<T> abstractClassOrInterface) {
        notNull(abstractClassOrInterface, "Abstract class or interface");
        T t = Proxy.proxy(abstractClassOrInterface, new PublisherInterceptor(this, abstractClassOrInterface));
        register(t);
        return t;
    }

    public void register(Object instance) {
        notNull(instance, "Instance");
        final Iterable<Method> methods = findMethods(getTargetClass(instance.getClass()));
        for (Method method : filter(methods, annotatedBy(Subscribe.class))) {
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            dispatcher.subscribe(
                    Topic.anyOf(subscribe.topics()),
                    subscribe.eventType(),
                    Subscribers.createSubscriber(instance, method));
        }
        for (Method method : filter(methods, annotatedBy(Answers.class))) {
            Answers answers = method.getAnnotation(Answers.class);
            dispatcher.subscribe(
                    Topic.anyOf(answers.topics()),
                    EventRequest.class,
                    Subscribers.createResponder(instance, method));
        }
    }

    /* STATIC CTOR */

    public static MycilaEvent with(Dispatcher dispatcher) {
        return new MycilaEvent(dispatcher);
    }

}
