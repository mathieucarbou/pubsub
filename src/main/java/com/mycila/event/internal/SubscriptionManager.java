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
package com.mycila.event.internal;

import com.google.common.base.Predicate;
import com.mycila.event.Event;
import com.mycila.event.Subscriber;
import com.mycila.event.Subscription;
import com.mycila.event.Topic;
import com.mycila.event.Topics;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Iterators.filter;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class SubscriptionManager {

    private final SubscriptionList subscriptions = new SubscriptionList();
    private final ConcurrentHashMap<Topic, SubscriptionList> mappedSubscriptions = new ConcurrentHashMap<Topic, SubscriptionList>();

    void addSubscription(Subscription<?> subscription) {
        subscriptions.add(subscription);
        for (Map.Entry<Topic, SubscriptionList> entry : mappedSubscriptions.entrySet())
            if (subscription.getTopicMatcher().matches(entry.getKey()))
                entry.getValue().add(subscription);
    }

    void removeSubscriber(Subscriber<?> subscriber) {
        for (Subscription<?> subscription : subscriptions)
            if (subscription.getSubscriber().equals(subscriber)) {
                subscriptions.remove(subscription);
                for (Map.Entry<Topic, SubscriptionList> entry : mappedSubscriptions.entrySet())
                    if (subscription.getTopicMatcher().matches(entry.getKey()))
                        entry.getValue().remove(subscription);
            }
    }

    void removeSubscriber(Topics matcher, Subscriber<?> subscriber) {
        for (Subscription<?> subscription : subscriptions)
            if (subscription.getSubscriber().equals(subscriber) && subscription.getTopicMatcher().equals(matcher)) {
                subscriptions.remove(subscription);
                for (Map.Entry<Topic, SubscriptionList> entry : mappedSubscriptions.entrySet())
                    if (subscription.getTopicMatcher().matches(entry.getKey()))
                        entry.getValue().remove(subscription);
            }
    }

    <E> Iterator<Subscription<E>> getSubscriptions(final Event<E> event) {
        final Topic topic = event.getTopic();
        SubscriptionList subscriptionList = mappedSubscriptions.get(topic);
        if (subscriptionList == null) {
            subscriptionList = new SubscriptionList();
            for (Subscription<?> subscription : this.subscriptions)
                if (subscription.getTopicMatcher().matches(topic))
                    subscriptionList.add(subscription);
            final SubscriptionList old = mappedSubscriptions.putIfAbsent(topic, subscriptionList);
            if (old != null) subscriptionList = old;
        }
        return (Iterator) filter(subscriptionList.iterator(), new Predicate<Subscription<?>>() {
            final Class<?> eventType = event.getSource().getClass();

            @Override
            public boolean apply(Subscription<?> input) {
                return input.getEventType().isAssignableFrom(eventType);
            }
        });
    }

}
