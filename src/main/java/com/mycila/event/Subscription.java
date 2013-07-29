/**
 * Copyright (C) 2010 Mycila (mathieu.carbou@gmail.com)
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

import static com.mycila.event.internal.Ensure.notNull;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Subscription<E> implements Referencable {

    private final Topics matcher;
    private final Class<?> eventType;
    private final Subscriber<E> subscriber;

    private Subscription(Topics matcher, Class<?> eventType, Subscriber<E> subscriber) {
        this.matcher = matcher;
        this.eventType = eventType;
        this.subscriber = subscriber;
    }

    public Topics getTopicMatcher() {
        return matcher;
    }

    public Class<?> getEventType() {
        return eventType;
    }

    public Subscriber<E> getSubscriber() {
        return subscriber;
    }

    @Override
    public Reachability getReachability() {
        return subscriber instanceof Referencable ?
                ((Referencable) subscriber).getReachability() :
                Reachability.of(subscriber.getClass());
    }

    @Override
    public String toString() {
        return "Event " + eventType + " on " + matcher;
    }

    public static <E> Subscription<E> create(Topics matcher, Class<?> eventType, Subscriber<E> subscriber) {
        notNull(matcher, "TopicMatcher");
        notNull(eventType, "Event type");
        notNull(subscriber, "Subscriber");
        return new Subscription<E>(matcher, eventType, subscriber);
    }

}
