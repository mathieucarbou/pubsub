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

import com.mycila.event.MycilaEvent;
import com.mycila.event.Publisher;
import com.mycila.event.Requestor;
import com.mycila.event.Topic;
import com.mycila.event.annotation.Group;
import com.mycila.event.annotation.Multiple;
import com.mycila.event.annotation.Publish;
import com.mycila.event.annotation.Request;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.collect.Iterables.filter;
import static com.mycila.event.internal.Ensure.hasSomeArgs;
import static com.mycila.event.internal.Reflect.annotatedBy;
import static com.mycila.event.internal.Reflect.findMethods;
import static com.mycila.event.internal.Reflect.getTargetClass;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class PublisherInterceptor implements MethodInterceptor {
    private final Map<Signature, Publisher> publisherCache = new HashMap<Signature, Publisher>();
    private final Map<Signature, TimedRequestor> requestorCache = new HashMap<Signature, TimedRequestor>();
    private final Object delegate;

    public PublisherInterceptor(MycilaEvent mycilaEvent, final Class<?> c) {
        Iterable<Method> allMethods = findMethods(getTargetClass(c));
        // find publishers
        for (Method method : filter(allMethods, annotatedBy(Publish.class))) {
            hasSomeArgs(method);
            Publish annotation = method.getAnnotation(Publish.class);
            Publisher publisher = mycilaEvent.createPublisher(Topic.topics(annotation.topics()));
            publisherCache.put(new Signature(method), publisher);
        }
        // find requestors
        for (Method method : filter(allMethods, annotatedBy(Request.class))) {
            Request annotation = method.getAnnotation(Request.class);
            requestorCache.put(new Signature(method), new TimedRequestor(mycilaEvent.createRequestor(Topic.topic(annotation.topic())), annotation));
        }
        delegate = !c.isInterface() ? null : new Object() {
            @Override
            public String toString() {
                return c.getName() + "$$byMycila@" + Integer.toHexString(hashCode());
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Signature methodSignature = new Signature(invocation.getMethod());
        Publisher publisher = publisherCache.get(methodSignature);
        if (publisher != null)
            return handlePublishing(publisher, invocation);
        TimedRequestor r = requestorCache.get(methodSignature);
        if (r != null) {
            try {
                return r.request.timeout() <= Request.INFINITE ?
                        r.requestor.createRequest(invocation.getArguments()).send().get() :
                        r.requestor.createRequest(invocation.getArguments()).send().get(r.request.timeout(), r.request.unit());
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }
        return delegate == null ?
                invocation.proceed() :
                invocation.getMethod().invoke(delegate, invocation.getArguments());
    }

    private static Object handlePublishing(Publisher publisher, MethodInvocation invocation) {
        boolean group = invocation.getMethod().isAnnotationPresent(Group.class);
        if (group) {
            publisher.publish(invocation.getArguments());
        } else {
            boolean requiresSplit = invocation.getMethod().isAnnotationPresent(Multiple.class);
            for (Object arg : invocation.getArguments()) {
                if (!requiresSplit)
                    publisher.publish(arg);
                else if (arg.getClass().isArray())
                    for (Object event : (Object[]) arg)
                        publisher.publish(event);
                else if (arg instanceof Iterable)
                    for (Object event : (Iterable) arg)
                        publisher.publish(event);
                else
                    publisher.publish(arg);
            }
        }
        return null;
    }

    private static final class TimedRequestor {
        private final Requestor requestor;
        private final Request request;

        private TimedRequestor(Requestor requestor, Request request) {
            this.requestor = requestor;
            this.request = request;
        }
    }
}
