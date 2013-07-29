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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.mycila.event.Ref;
import com.mycila.event.Subscription;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class SubscriptionList implements Iterable<Subscription<?>> {

    private final CopyOnWriteArrayList<Ref<? extends Subscription<?>>> subscriptions = new CopyOnWriteArrayList<Ref<? extends Subscription<?>>>();

    public boolean add(Subscription<?> subscription) {
        Ref<? extends Subscription<?>> ref = subscription.getReachability().wrap(subscription);
        return subscriptions.add(ref);
    }

    public boolean isEmpty() {
        return subscriptions.isEmpty();
    }

    public int size() {
        return subscriptions.size();
    }

    public Iterator<Subscription<?>> iterator() {
        return filter(transform(subscriptions.iterator(), TRANSFORMER), FILTER);
    }

    public void remove(Subscription<?> subscription) {
        for (Ref<? extends Subscription<?>> ref : subscriptions)
            if (subscription.equals(ref.get()))
                subscriptions.remove(ref);
    }

    private static final Predicate<Subscription<?>> FILTER = new Predicate<Subscription<?>>() {
        @Override
        public boolean apply(Subscription<?> input) {
            return input != null;
        }
    };

    private final Function<Ref<? extends Subscription<?>>, Subscription<?>> TRANSFORMER = new Function<Ref<? extends Subscription<?>>, Subscription<?>>() {
        @Override
        public Subscription<?> apply(Ref<? extends Subscription<?>> ref) {
            Subscription<?> next = ref.get();
            if (next == null)
                subscriptions.remove(ref);
            return next;
        }
    };
}
