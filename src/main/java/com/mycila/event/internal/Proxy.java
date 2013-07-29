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

package com.mycila.event.internal;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodProxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Proxy {

    private Proxy() {
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T proxy(Class<T> c, MethodInterceptor interceptor) {
        if (c.isInterface()) {
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                    c.getClassLoader(),
                    new Class<?>[]{c},
                    toJDK(interceptor));
        }
        Enhancer enhancer = BytecodeGen.newEnhancer(c, BytecodeGen.Visibility.PUBLIC);
        enhancer.setCallback(toCGLIB(interceptor));
        return (T) enhancer.create();
    }

    static MethodInvoker invoker(final Method method) {
        return INVOKER_CACHE.get(method);
    }

    /* PRIVATE */

    private static final WeakCache<Method, MethodInvoker> INVOKER_CACHE = new WeakCache<Method, MethodInvoker>(new WeakCache.Provider<Method, MethodInvoker>() {
        @Override
        public MethodInvoker get(final Method method) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers)) {
                try {
                    final net.sf.cglib.reflect.FastMethod fastMethod = BytecodeGen.newFastClass(method.getDeclaringClass(), BytecodeGen.Visibility.forMember(method)).getMethod(method);
                    return new MethodInvoker() {
                        @Override
                        public Object invoke(Object target, Object... parameters) throws IllegalAccessException, InvocationTargetException {
                            return fastMethod.invoke(target, parameters);
                        }
                    };
                } catch (net.sf.cglib.core.CodeGenerationException e) {/* fall-through */}
            }
            if (!Modifier.isPublic(modifiers) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            return new MethodInvoker() {
                @Override
                public Object invoke(Object target, Object... parameters) throws IllegalAccessException, InvocationTargetException {
                    return method.invoke(target, parameters);
                }
            };
        }
    });

    private static InvocationHandler toJDK(final MethodInterceptor interceptor) {
        return new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, Object[] args) throws Throwable {
                final Object[] arguments = args == null ? new Object[0] : args;
                return interceptor.invoke(new MethodInvocation() {
                    public Method getMethod() {
                        return method;
                    }

                    public Object[] getArguments() {
                        return arguments;
                    }

                    public Object proceed() throws Throwable {
                        return method.invoke(proxy, arguments);
                    }

                    public Object getThis() {
                        return proxy;
                    }

                    public AccessibleObject getStaticPart() {
                        return method;
                    }
                });
            }
        };
    }

    private static net.sf.cglib.proxy.MethodInterceptor toCGLIB(final MethodInterceptor interceptor) {
        return new net.sf.cglib.proxy.MethodInterceptor() {
            public Object intercept(final Object obj, final Method method, Object[] args, final MethodProxy proxy) throws Throwable {
                final Object[] arguments = args == null ? new Object[0] : args;
                return interceptor.invoke(new MethodInvocation() {
                    public Method getMethod() {
                        return method;
                    }

                    public Object[] getArguments() {
                        return arguments;
                    }

                    public Object proceed() throws Throwable {
                        return proxy.invokeSuper(obj, arguments);
                    }

                    public Object getThis() {
                        return obj;
                    }

                    public AccessibleObject getStaticPart() {
                        return method;
                    }
                });
            }
        };
    }

}
