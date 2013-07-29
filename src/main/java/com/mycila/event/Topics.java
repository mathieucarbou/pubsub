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

import com.mycila.event.internal.TopicsMatcher;

import java.io.Serializable;
import java.util.Arrays;

import static com.mycila.event.Topic.topic;
import static com.mycila.event.internal.Ensure.notNull;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public abstract class Topics {

    public abstract boolean matches(Topic t);

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public final Topics or(Topics other) {
        return new OrMatcher(this, notNull(other, "TopicMatcher"));
    }

    public final Topics and(Topics other) {
        return new AndMatcher(this, notNull(other, "TopicMatcher"));
    }

    /* FACTORIES */

    public static Topics match(final Topics matcher) {
        return new Delegate(matcher);
    }

    public static Topics match(String pattern) {
        return match(TopicsMatcher.forPattern(pattern));
    }

    public static Topics any() {
        return ANY;
    }

    public static Topics not(Topics matcher) {
        return new Not(matcher);
    }

    public static Topics only(String exactTopicName) {
        return only(topic(exactTopicName));
    }

    public static Topics only(Topic topic) {
        return new Only(notNull(topic, "Topic"));
    }

    public static Topics anyOf(String... patterns) {
        notNull(patterns, "Topic patterns");
        Topics[] matchers = new Topics[patterns.length];
        for (int i = 0, length = matchers.length; i < length; i++)
            matchers[i] = match(patterns[i]);
        return anyOf(matchers);
    }

    public static Topics anyOf(Topics... matchers) {
        return new AnyOf(matchers);
    }

    /* PRIVATE */

    private static final Topics ANY = match("**");

    private static class AndMatcher extends Topics implements Serializable {
        private static final long serialVersionUID = 0;
        private final Topics a, b;

        public AndMatcher(Topics a, Topics b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean matches(Topic t) {
            notNull(t, "Topic");
            return a.matches(t) && b.matches(t);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof AndMatcher
                    && ((AndMatcher) other).a.equals(a)
                    && ((AndMatcher) other).b.equals(b);
        }

        @Override
        public int hashCode() {
            return 41 * (a.hashCode() ^ b.hashCode());
        }

        @Override
        public String toString() {
            return "and(" + a + ", " + b + ")";
        }
    }

    private static class OrMatcher extends Topics implements Serializable {
        private static final long serialVersionUID = 0;
        private final Topics a, b;

        public OrMatcher(Topics a, Topics b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean matches(Topic t) {
            notNull(t, "Topic");
            return a.matches(t) || b.matches(t);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof OrMatcher
                    && ((OrMatcher) other).a.equals(a)
                    && ((OrMatcher) other).b.equals(b);
        }

        @Override
        public int hashCode() {
            return 37 * (a.hashCode() ^ b.hashCode());
        }

        @Override
        public String toString() {
            return "or(" + a + ", " + b + ")";
        }
    }

    private static final class Delegate extends Topics implements Serializable {
        private static final long serialVersionUID = 0;
        final Topics matcher;

        private Delegate(Topics matcher) {
            this.matcher = notNull(matcher, "TopicMatcher");
        }

        @Override
        public boolean matches(Topic t) {
            return matcher.matches(notNull(t, "Topic"));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Delegate && ((Delegate) other).matcher.equals(matcher);
        }

        @Override
        public int hashCode() {
            return matcher.hashCode();
        }

        @Override
        public String toString() {
            return matcher.toString();
        }
    }

    private static final class Not extends Topics implements Serializable {
        private static final long serialVersionUID = 0;
        final Topics matcher;

        private Not(Topics matcher) {
            this.matcher = notNull(matcher, "TopicMatcher");
        }

        @Override
        public boolean matches(Topic t) {
            return !matcher.matches(notNull(t, "Topic"));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Not && ((Not) other).matcher.equals(matcher);
        }

        @Override
        public int hashCode() {
            return -matcher.hashCode();
        }

        @Override
        public String toString() {
            return "not(" + matcher + ")";
        }
    }

    private static class Only extends Topics implements Serializable {
        private static final long serialVersionUID = 0;
        private final Topic topic;

        public Only(Topic topic) {
            this.topic = topic;
        }

        @Override
        public boolean matches(Topic other) {
            return topic.equals(other);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Only && ((Only) other).topic.equals(topic);
        }

        @Override
        public int hashCode() {
            return 37 * topic.hashCode();
        }

        @Override
        public String toString() {
            return topic.getName();
        }
    }

    private static final class AnyOf extends Topics implements Serializable {
        private static final long serialVersionUID = 0;
        final Topics[] matchers;

        private AnyOf(Topics... matchers) {
            this.matchers = notNull(matchers, "TopicMatcher list");
        }

        @Override
        public boolean matches(Topic t) {
            notNull(t, "Topic");
            for (Topics matcher : matchers)
                if (matcher.matches(t))
                    return true;
            return false;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof AnyOf && Arrays.equals(((AnyOf) other).matchers, matchers);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(matchers);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("anyOf(");
            int len = matchers.length;
            if (len > 0) {
                sb.append(matchers[0]);
                for (int i = 1; i < len; i++)
                    sb.append(", ").append(matchers[i]);
            }
            return sb.append(")").toString();
        }
    }

}
