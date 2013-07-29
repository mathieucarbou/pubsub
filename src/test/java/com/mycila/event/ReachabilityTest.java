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

import com.mycila.event.annotation.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class ReachabilityTest {

    @Test
    public void test() throws Exception {
        assertEquals(Reachability.of(new Object()), Reachability.HARD);
        assertEquals(Reachability.of(new Subscriber() {
            public void onEvent(Event event) throws Exception {
            }
        }), Reachability.HARD);
        assertEquals(Reachability.of(new S()), Reachability.WEAK);
    }

    @Reference(Reachability.WEAK)
    private static class S implements Subscriber {
        public void onEvent(Event event) throws Exception {
        }
    }
}
