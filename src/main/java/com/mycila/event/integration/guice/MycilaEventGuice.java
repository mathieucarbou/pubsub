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
package com.mycila.event.integration.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.mycila.event.Dispatcher;
import com.mycila.event.MycilaEvent;
import com.mycila.event.annotation.Answers;
import com.mycila.event.annotation.Subscribe;

import javax.inject.Singleton;

import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.inject.matcher.Matchers.any;
import static com.mycila.event.internal.Reflect.annotatedBy;
import static com.mycila.event.internal.Reflect.findMethods;
import static com.mycila.event.internal.Reflect.getTargetClass;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class MycilaEventGuice {
    private MycilaEventGuice() {
    }

    public static Module mycilaEventModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bindListener(any(), new TypeListener() {
                    public <I> void hear(TypeLiteral<I> type, final TypeEncounter<I> encounter) {
                        if (!isEmpty(filter(
                                findMethods(getTargetClass(type.getRawType())),
                                or(annotatedBy(Subscribe.class), annotatedBy(Answers.class))))) {
                            final Provider<MycilaEvent> mycilaEventProvider = encounter.getProvider(MycilaEvent.class);
                            encounter.register(new InjectionListener<I>() {
                                public void afterInjection(I injectee) {
                                    mycilaEventProvider.get().register(injectee);
                                }
                            });
                        }
                    }
                });
            }

            @Provides
            @Singleton
            public MycilaEvent mycilaEvent(Dispatcher dispatcher) {
                return MycilaEvent.with(dispatcher);
            }
        };
    }

    public static <T> ScopedBindingBuilder bindPublisher(Binder binder, Class<T> clazz) {
        return binder.bind(clazz).toProvider(publisher(clazz));
    }

    public static <T> Provider<T> publisher(final Class<T> clazz) {
        return new Provider<T>() {
            @Inject
            Provider<MycilaEvent> mycilaEventProvider;
            @Inject
            Provider<Injector> injectorProvider;

            public T get() {
                T proxy = mycilaEventProvider.get().instanciate(clazz);
                injectorProvider.get().injectMembers(proxy);
                return proxy;
            }
        };
    }
}
