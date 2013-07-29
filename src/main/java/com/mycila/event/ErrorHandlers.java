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

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class ErrorHandlers {

    private ErrorHandlers() {
    }

    public static ErrorHandler ignoreErrors() {
        return SILENT;
    }

    public static ErrorHandler rethrow() {
        return RETHROW;
    }

    private static final ErrorHandler SILENT = new ErrorHandler() {
        @Override
        public <E> void onError(Subscription<E> subscription, Event<E> event, Exception e) {
        }
    };


    private static final ErrorHandler RETHROW = new ErrorHandler() {
        @Override
        public <E> void onError(Subscription<E> subscription, Event<E> event, Exception e) {
            SubscriberExecutionException ee = SubscriberExecutionException.wrap(e);
            if (event.getSource() instanceof EventRequest)
                ((EventRequest) event.getSource()).replyError(ee.getCause());
            else
                throw ee;
        }
    };

}
