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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class SubscriberExecutionException extends DispatcherException {
    private SubscriberExecutionException(Throwable cause) {
        super(cause);
    }

    public static SubscriberExecutionException wrap(Throwable throwable) {
        while (throwable instanceof InvocationTargetException
                || throwable instanceof ExecutionException
                || throwable instanceof SubscriberExecutionException)
            throwable = throwable instanceof InvocationTargetException ?
                    ((InvocationTargetException) throwable).getTargetException() :
                    throwable.getCause();
        return new SubscriberExecutionException(throwable);
    }
}
