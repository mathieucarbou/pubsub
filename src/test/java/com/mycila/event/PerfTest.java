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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Ignore
final class PerfTest {

    private static final int N_SUBS = 5;
    private static final AtomicLong published = new AtomicLong(0);
    private static final AtomicLong[] consumed = new AtomicLong[N_SUBS];
    private static final ConcurrentLinkedQueue<StatEvent> events = new ConcurrentLinkedQueue<StatEvent>();
    private static final Random random = new Random(System.nanoTime());

    static {
        for (int i = 0, length = consumed.length; i < length; i++)
            consumed[i] = new AtomicLong(0);
    }

    public static void main(String... args) throws Exception {
        Map<String, Dispatcher> dispatchers = new LinkedHashMap<String, Dispatcher>(6) {
            {
                put("SynchronousSafe", Dispatchers.synchronousSafe(ErrorHandlers.rethrow()));
                put("SynchronousUnsafe", Dispatchers.synchronousUnsafe(ErrorHandlers.rethrow()));
                put("AsynchronousSafe", Dispatchers.asynchronousSafe(ErrorHandlers.rethrow()));
                put("AsynchronousUnsafe", Dispatchers.asynchronousUnsafe(ErrorHandlers.rethrow()));
                put("BroadcastOrdered", Dispatchers.broadcastOrdered(ErrorHandlers.rethrow()));
                put("BroadcastUnordered", Dispatchers.broadcastUnordered(ErrorHandlers.rethrow()));
            }
        };
        for (Map.Entry<String, Dispatcher> entry : dispatchers.entrySet()) {
            System.out.println("\n===== " + entry.getKey() + " Statistics =====\n");
            for (int i = 0, length = N_SUBS * 2; i < length; i++) {
                final int index = i % N_SUBS;
                System.out.println("Adding consumer to: stats" + index);
                entry.getValue().subscribe(Topic.only("stats" + index), StatEvent.class, new Subscriber<StatEvent>() {
                    public void onEvent(Event<StatEvent> statEvent) throws Exception {
                        events.offer(statEvent.getSource().received());
                        consumed[index].incrementAndGet();
                    }
                });
            }
            test(entry.getValue(), 50, 50);
            test(entry.getValue(), 100, 100);
            test(entry.getValue(), 1000, 1000);
            test(entry.getValue(), 2, 225000);
            entry.getValue().close();
        }
        System.out.println("\nFinished!");
    }

    static void test(final Dispatcher dispatcher, int nPublishers, int nEvents) throws Exception {
        System.out.println("\n > Testing " + nPublishers + " publishers sending " + nEvents + " events each\n");

        long start = System.nanoTime();
        publish(dispatcher, nPublishers, nEvents);
        long end = System.nanoTime();

        waitAndReset();

        long latency = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        int s = events.size();
        while (!events.isEmpty()) {
            StatEvent event = events.poll();
            latency += event.latency();
            if (event.latency() > max)
                max = event.latency();
            else if (event.latency() < min)
                min = event.latency();
        }
        latency /= s;

        System.out.println("- publish time: " + (end - start) + "ns = " + TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS) + "ms");
        System.out.println("- avg. latency: " + latency + "ns = " + TimeUnit.MILLISECONDS.convert(latency, TimeUnit.NANOSECONDS) + "ms");
        System.out.println("- min. latency: " + min + "ns = " + TimeUnit.MILLISECONDS.convert(min, TimeUnit.NANOSECONDS) + "ms");
        System.out.println("- max. latency: " + max + "ns = " + TimeUnit.MILLISECONDS.convert(max, TimeUnit.NANOSECONDS) + "ms");
    }

    private static void publish(final Dispatcher dispatcher, final int nPublishers, final int nEvents) throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(nPublishers + 1);
        for (int i = 0; i < nPublishers; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        for (int i = 0; i < nEvents; i++) {
                            dispatcher.publish(Topic.topic("stats" + random.nextInt(N_SUBS)), new StatEvent());
                            published.getAndIncrement();
                        }
                        barrier.await();
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.start();
        }
        barrier.await();
        barrier.await();
    }

    private static void waitAndReset() throws Exception {
        long p, s, t;
        long[] c = new long[5];
        do {
            t = 0;
            Thread.sleep(5000);
            p = published.get();
            for (int i = 0, length = consumed.length; i < length; i++) {
                c[i] = consumed[i].get();
                t += c[i];
            }
            s = events.size();
            System.out.println("Published: " + p);
            System.out.println("Queue: " + s);
            for (int i = 0, length = c.length; i < length; i++)
                System.out.println("Consumed by " + i + ": " + c[i]);
        } while (t != s || p * 2 != s);
        published.set(0);
        for (int i = 0, length = consumed.length; i < length; i++)
            consumed[i].set(0);
    }

    private static final class StatEvent {
        final long start;
        long latency;

        StatEvent() {
            this.start = System.nanoTime();
        }

        StatEvent received() {
            latency = System.nanoTime() - start;
            return this;
        }

        long latency() {
            return latency;
        }
    }
}

/*

On DELL laptopp inspiron 6400:

===== SynchronousSafe Statistics =====
Testing 1000 consumers sending 500 events each
- exec. time: 166894057ns = 166ms
- avg. latency: 6427ns = 0ms
- min. latency: 3074ns = 0ms
- max. latency: 374637ns = 0ms
Testing 1000 consumers sending 1000 events each
- exec. time: 167090732ns = 167ms
- avg. latency: 475583ns = 0ms
- min. latency: 3073ns = 0ms
- max. latency: 4131488ns = 4ms
===== SynchronousUnsafe Statistics =====
Testing 1000 consumers sending 500 events each
- exec. time: 162852777ns = 162ms
- avg. latency: 3958ns = 0ms
- min. latency: 3003ns = 0ms
- max. latency: 144993ns = 0ms
Testing 1000 consumers sending 1000 events each
- exec. time: 308882709ns = 308ms
- avg. latency: 6039ns = 0ms
- min. latency: 4330ns = 0ms
- max. latency: 286775ns = 0ms
===== AsynchronousSafe Statistics =====
Testing 1000 consumers sending 500 events each
- exec. time: 154075405ns = 154ms
- avg. latency: 153753ns = 0ms
- min. latency: 4959ns = 0ms
- max. latency: 1908249ns = 1ms
Testing 1000 consumers sending 1000 events each
- exec. time: 163709151ns = 163ms
- avg. latency: 90922ns = 0ms
- min. latency: 5028ns = 0ms
- max. latency: 1452856ns = 1ms
===== AsynchronousUnsafe Statistics =====
Testing 1000 consumers sending 500 events each
- exec. time: 171095881ns = 171ms
- avg. latency: 51983ns = 0ms
- min. latency: 4261ns = 0ms
- max. latency: 2767426ns = 2ms
Testing 1000 consumers sending 1000 events each
- exec. time: 179646737ns = 179ms
- avg. latency: 108732ns = 0ms
- min. latency: 4191ns = 0ms
- max. latency: 50084653ns = 50ms
===== BroadcastOrdered Statistics =====
Testing 1000 consumers sending 500 events each
- exec. time: 184653243ns = 184ms
- avg. latency: 11617597ns = 11ms
- min. latency: 30102ns = 0ms
- max. latency: 25190883ns = 25ms
Testing 1000 consumers sending 1000 events each
- exec. time: 195372864ns = 195ms
- avg. latency: 5811118ns = 5ms
- min. latency: 18788ns = 0ms
- max. latency: 9696398ns = 9ms
===== BroadcastUnordered Statistics =====
Testing 1000 consumers sending 500 events each
- exec. time: 288626478ns = 288ms
- avg. latency: 309981ns = 0ms
- min. latency: 12711ns = 0ms
- max. latency: 6885810ns = 6ms
Testing 1000 consumers sending 1000 events each
- exec. time: 195777764ns = 195ms
- avg. latency: 1837885ns = 1ms
- min. latency: 34781ns = 0ms
- max. latency: 73457209ns = 73ms

*/