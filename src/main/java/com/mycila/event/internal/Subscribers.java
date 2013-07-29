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
import com.mycila.event.EventRequest;
import com.mycila.event.Reachability;
import com.mycila.event.Referencable;
import com.mycila.event.Subscriber;
import com.mycila.event.SubscriberExecutionException;
import com.mycila.event.annotation.Reference;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.mycila.event.internal.Ensure.hasSomeArgs;
import static com.mycila.event.internal.Ensure.notNull;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Subscribers {

    private Subscribers() {
    }

    public static Subscriber<?> createSubscriber(Object instance, Method method) {
        return new MethodSubscriber(instance, method);
    }

    public static Subscriber<? extends EventRequest<?>> createResponder(Object instance, Method method) {
        return new MethodResponder(instance, method);
    }

    private static class ReferencableMethod implements Referencable {
        final Reachability reachability;
        final Object target;
        final MethodInvoker invoker;
        final Class<?>[] argTypes;

        ReferencableMethod(Object target, final Method method) {
            notNull(target, "Target object");
            notNull(method, "Method");
            this.argTypes = method.getParameterTypes();
            this.target = target;
            this.invoker = Proxy.invoker(method);
            this.reachability = method.isAnnotationPresent(Reference.class) ?
                    method.getAnnotation(Reference.class).value() :
                    Reachability.of(target.getClass());
        }

        @Override
        public final Reachability getReachability() {
            return reachability;
        }
    }

    private static final class MethodSubscriber extends ReferencableMethod implements Subscriber<Object> {
        MethodSubscriber(Object target, Method method) {
            super(target, method);
            hasSomeArgs(method);
        }

        @Override
        public void onEvent(Event<Object> event) throws Exception {
            try {
                if (argTypes.length == 1 && argTypes[0].isAssignableFrom(Event.class))
                    invoker.invoke(target, event);
                else {
                    Object o = event.getSource();
                    if (o.getClass().isArray())
                        invoker.invoke(target, (Object[]) o);
                    else
                        invoker.invoke(target, o);
                }
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof Exception)
                    throw (Exception) e.getTargetException();
                throw SubscriberExecutionException.wrap(e.getTargetException());
            }
        }
    }

    private static final class MethodResponder extends ReferencableMethod implements Subscriber<EventRequest<Object>> {

        private final int len;

        MethodResponder(Object target, Method method) {
            super(target, method);
            this.len = method.getParameterTypes().length;
        }

        @Override
        public void onEvent(Event<EventRequest<Object>> event) throws Exception {
            try {
                if (len == 0) event.getSource().reply(invoker.invoke(target));
                else event.getSource().reply(invoker.invoke(target, event.getSource().getParameters().toArray()));
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof Exception)
                    throw (Exception) e.getTargetException();
                throw SubscriberExecutionException.wrap(e.getTargetException());
            }
        }
    }

}
