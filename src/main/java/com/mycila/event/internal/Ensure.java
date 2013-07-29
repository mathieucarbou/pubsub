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

import java.lang.reflect.Method;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Ensure {
    private Ensure() {
    }

    public static <T> T notNull(T arg, String name) {
        if (arg == null)
            throw new IllegalArgumentException(name + " cannot be null");
        return arg;
    }

    public static void hasSomeArgs(Method method) {
        if (method.getParameterTypes().length == 0)
            throw new IllegalArgumentException("Method " + method + " is not valid: must have one or many parameters matching all events types to publish");
    }

}