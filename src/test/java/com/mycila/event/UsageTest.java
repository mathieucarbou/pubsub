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

import org.junit.Ignore;

import static com.mycila.event.Topic.match;
import static com.mycila.event.Topic.only;
import static com.mycila.event.Topic.topic;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Ignore
final class UsageTest {
    public static void main(String... args) {
        // first createPublisher an event service
        Dispatcher dispatcher = Dispatchers.synchronousUnsafe(ErrorHandlers.rethrow());

        // then subscribe
        Topics matcher = only("app/events/swing/button").or(match("app/events/swing/fields/**"));
        dispatcher.subscribe(matcher, String.class, new Subscriber<String>() {
            public void onEvent(Event<String> event) throws Exception {
                System.out.println("Received: " + event.getSource());
            }
        });

        // and publish
        dispatcher.publish(topic("app/events/swing/button"), "Hello !");
    }
}
