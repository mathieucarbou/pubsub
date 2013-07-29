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

import java.io.Serializable;
import java.util.UUID;

import static com.mycila.event.internal.Ensure.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Topic extends Topics implements Serializable {

    private static final long serialVersionUID = 0;

    private final String name;

    private Topic(String name) {
        this.name = notNull(name, "Topic name");
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean matches(Topic topic) {
        return equals(topic);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Topic && ((Topic) other).name.equals(name);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public static Topic[] topics(String... names) {
        notNull(names, "Topic names");
        Topic[] topics = new Topic[names.length];
        for (int i = 0, length = topics.length; i < length; i++)
            topics[i] = topic(names[i]);
        return topics;
    }

    public static Topic topic(String name) {
        return new Topic(name);
    }

    public static Topic random() {
        return new Topic("temp/" + UUID.randomUUID().toString());
    }

}
