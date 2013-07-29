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

import com.mycila.event.Event;
import com.mycila.event.Reachability;
import com.mycila.event.Referencable;
import com.mycila.event.Subscriber;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class EventQueue<T> implements BlockingQueue<T>, Subscriber<T>, Referencable {

    private final BlockingQueue<T> delegate;

    public EventQueue(BlockingQueue<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Reachability getReachability() {
        return Reachability.WEAK;
    }

    @Override
    public void onEvent(Event<T> e) throws Exception {
        offer(e.getSource());
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventQueue that = (EventQueue) o;
        return delegate.equals(that.delegate);
    }

    /* AUTO GENERATED DELEGATES */

    @Override
    public boolean add(T t) {
        return delegate.add(t);
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        return delegate.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        return delegate.drainTo(c, maxElements);
    }

    @Override
    public boolean offer(T t) {
        return delegate.offer(t);
    }

    @Override
    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.offer(t, timeout, unit);
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.poll(timeout, unit);
    }

    @Override
    public void put(T t) throws InterruptedException {
        delegate.put(t);
    }

    @Override
    public int remainingCapacity() {
        return delegate.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public T take() throws InterruptedException {
        return delegate.take();
    }

    @Override
    public T element() {
        return delegate.element();
    }

    @Override
    public T peek() {
        return delegate.peek();
    }

    @Override
    public T poll() {
        return delegate.poll();
    }

    @Override
    public T remove() {
        return delegate.remove();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return delegate.addAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

}
