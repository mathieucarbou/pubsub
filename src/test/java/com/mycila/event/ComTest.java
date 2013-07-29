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

import com.mycila.event.annotation.Answers;
import com.mycila.event.annotation.Request;
import com.mycila.event.annotation.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.mycila.event.Topic.*;
import static org.junit.Assert.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class ComTest {

    static boolean throwExcepiton = false;

    @Test
    public void test_async_request() throws Exception {
        Dispatcher dispatcher = Dispatchers.synchronousSafe(ErrorHandlers.rethrow());
        MycilaEvent processor = MycilaEvent.with(dispatcher);
        processor.instanciate(DU.class);

        final CountDownLatch finished = new CountDownLatch(2);
        SendableRequest<Integer> req = processor.createRequestor(topic("system/add")).<Integer>createRequest("my sum", new int[]{1, 2, 3, 4, 5})
                .addListener(new FutureListener<Integer>() {
                    public void onResponse(Integer value) {
                        assertEquals(15, value.intValue());
                        finished.countDown();
                    }

                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }
                });
        System.out.println(req);
        req.send();

        SendableRequest<Integer> req2 = processor.createRequestor(topic("system/rm")).<Integer>createRequest("err")
                .addListener(new FutureListener<Integer>() {
                    public void onResponse(Integer value) {
                        fail();
                    }

                    public void onError(Throwable t) {
                        //t.printStackTrace();
                        assertTrue(t instanceof FileNotFoundException);
                        finished.countDown();
                    }
                });
        try {
            req2.send();
        } catch (Exception e) {
            assertEquals(e.getClass(), FileNotFoundException.class);
            assertEquals("rm did not found folder err", e.getMessage());
        }

        assertTrue(finished.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void test_args() throws Exception {
        Dispatcher dispatcher = Dispatchers.synchronousSafe(ErrorHandlers.rethrow());
        MycilaEvent processor = MycilaEvent.with(dispatcher);

        DU du = processor.instanciate(DU.class);
        DU2 du2 = processor.instanciate(DU2.class);

        assertEquals(30, du.mult(5, 6));
        //assertEquals(15, du.add(1, 2, 3, 4, 5));

        assertEquals("hello", du2.reqHello());
    }

    @Test
    public void test() throws Exception {
        Dispatcher dispatcher = Dispatchers.synchronousSafe(ErrorHandlers.rethrow());

        dispatcher.subscribe(only("system/df"), EventRequest.class, new Subscriber<EventRequest<Integer>>() {
            public void onEvent(Event<EventRequest<Integer>> event) throws Exception {
                String folder = (String) event.getSource().getParameters().get(0);
                System.out.println("df request on folder " + folder);
                // call df -h <folder>
                if ("inexisting".equals(folder))
                    event.getSource().replyError(new FileNotFoundException("df did not found folder " + folder));
                else
                    event.getSource().reply(45);
            }
        });

        // through annotations
        MycilaEvent processor = MycilaEvent.with(dispatcher);
        DU du = processor.instanciate(DU.class);
        DU2 du2 = processor.instanciate(DU2.class);

        System.out.println(Integer.toHexString(du.hashCode()));
        System.out.println(Integer.toHexString(du2.hashCode()));
        System.out.println(du);
        System.out.println(du2);

        assertEquals(40, du.getSize("root").intValue());
        try {
            du.getSize("notFound");
            fail();
        } catch (Exception e) {
            assertEquals(SubscriberExecutionException.class, e.getClass());
            assertEquals("du did not found folder notFound", e.getMessage());
        }

        assertEquals(40, du2.getSize("root").intValue());
        try {
            du2.getSize("notFound");
            fail();
        } catch (Exception e) {
            assertEquals(SubscriberExecutionException.class, e.getClass());
            assertEquals("du did not found folder notFound", e.getMessage());
        }

        assertEquals(111, du.rm("root"));
        try {
            du.rm("err");
            fail();
        } catch (Exception e) {
            assertEquals(SubscriberExecutionException.class, e.getClass());
            assertEquals("rm did not found folder err", e.getMessage());
        }

        assertEquals(222, du.rm());
        try {
            throwExcepiton = true;
            du.rm();
            fail();
        } catch (Exception e) {
            assertEquals(SubscriberExecutionException.class, e.getClass());
            assertEquals("rm2 did not found folder", e.getMessage());
        }

        // manually
        SendableRequest<Integer> req1 = processor.createRequestor(topic("system/df")).createRequest("home");
        SendableRequest<Integer> req2 = processor.createRequestor(topic("system/df")).createRequest("inexisting");

        assertEquals(45, req1.send().get(5, TimeUnit.SECONDS).intValue());
        try {
            req2.send().get();
            fail();
        } catch (Exception e) {
            assertEquals("df did not found folder inexisting", e.getMessage());
        }
    }

    static abstract class DU {
        @Request(topic = "system/du", timeout = 5, unit = TimeUnit.SECONDS)
        abstract Integer getSize(String folder);

        @Subscribe(topics = "system/du", eventType = EventRequest.class)
        void duRequest(Event<EventRequest<Integer>> event) {
            String folder = (String) event.getSource().getParameters().get(0);
            System.out.println("du request on folder " + folder);
            // call du -h <folder>
            if ("notFound".equals(folder))
                event.getSource().replyError(new FileNotFoundException("du did not found folder " + folder));
            else
                event.getSource().reply(40);
        }

        @Request(topic = "system/rm")
        abstract int rm(String folder);

        @Answers(topics = "system/rm")
        int rmRequest(String folder) throws FileNotFoundException {
            System.out.println("rm request on folder " + folder);
            if ("err".equals(folder))
                throw new FileNotFoundException("rm did not found folder " + folder);
            else return 111;
        }

        @Request(topic = "system/rm2")
        abstract int rm();

        @Answers(topics = "system/rm2")
        int rmRequest() throws FileNotFoundException {
            System.out.println("rm2 request on folder");
            if (throwExcepiton)
                throw new FileNotFoundException("rm2 did not found folder");
            else return 222;
        }

        @Request(topic = "system/mult")
        abstract int mult(int p1, int p2);

        @Answers(topics = "system/mult")
        int multRequest(int p1, int p2) {
            return p1 * p2;
        }

        @Request(topic = "system/add")
        abstract int add(int... p);

        @Answers(topics = "hello")
        String hello() {
            return "hello";
        }

        @Answers(topics = "system/add")
        int addRequest(String str, int... p) {
            System.out.println("add: " + str);
            int c = 0;
            for (int i : p) c += i;
            return c;
        }
    }

    interface DU2 {
        @Request(topic = "system/du", timeout = 5, unit = TimeUnit.SECONDS)
        Integer getSize(String folder);

        @Request(topic = "hello")
        String reqHello();
    }

}
