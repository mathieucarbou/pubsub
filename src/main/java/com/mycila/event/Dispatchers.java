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

import com.mycila.event.internal.DefaultDispatcher;

import javax.annotation.PreDestroy;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.mycila.event.internal.Ensure.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Dispatchers {

    private Dispatchers() {
    }

    /* custom */

    public static Dispatcher custom(Executor publishExecutor,
                                    Executor subscriberExecutor) {
        return custom(ErrorHandlers.rethrow(), publishExecutor, subscriberExecutor);
    }

    public static Dispatcher custom(ErrorHandler errorHandler,
                                    Executor publishExecutor,
                                    Executor subscriberExecutor) {
        return new DefaultDispatcher(errorHandler, publishExecutor, subscriberExecutor);
    }

    public static Dispatcher custom(ExecutorService publishExecutor,
                                    ExecutorService subscriberExecutor) {
        return custom(ErrorHandlers.rethrow(), publishExecutor, subscriberExecutor);
    }

    public static Dispatcher custom(ErrorHandler errorHandler,
                                    final ExecutorService publishExecutor,
                                    final ExecutorService subscriberExecutor) {
        return new DefaultDispatcher(errorHandler, publishExecutor, subscriberExecutor) {
            @Override
            @PreDestroy
            public void close() {
                publishExecutor.shutdown();
                subscriberExecutor.shutdown();
            }
        };
    }

    /* synchronousSafe */

    public static Dispatcher synchronousSafe(long blockingTimeout, TimeUnit unit) {
        return synchronousSafe(ErrorHandlers.rethrow(), blockingTimeout, unit);
    }

    public static Dispatcher synchronousSafe(ErrorHandler errorHandler, long blockingTimeout, TimeUnit unit) {
        return new DefaultDispatcher(errorHandler, Executors.blocking(blockingTimeout, unit), Executors.immediate());
    }

    public static Dispatcher synchronousSafe() {
        return synchronousSafe(ErrorHandlers.rethrow());
    }

    public static Dispatcher synchronousSafe(ErrorHandler errorHandler) {
        return new DefaultDispatcher(errorHandler, Executors.blocking(), Executors.immediate());
    }

    /* synchronousUnsafe */

    public static Dispatcher synchronousUnsafe() {
        return asynchronousUnsafe(ErrorHandlers.rethrow());
    }

    public static Dispatcher synchronousUnsafe(ErrorHandler errorHandler) {
        return new DefaultDispatcher(errorHandler, Executors.immediate(), Executors.immediate());
    }

    /* asynchronousSafe */

    public static Dispatcher asynchronousSafe() {
        return asynchronousSafe(ErrorHandlers.rethrow());
    }

    public static Dispatcher asynchronousSafe(ErrorHandler errorHandler) {
        final ExecutorService executor = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DefaultThreadFactory("AsynchronousSafe", "dispatcher", false),
                new RejectedExecutionHandler() {
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                    }
                });
        return new DefaultDispatcher(errorHandler, executor, Executors.immediate()) {
            @Override
            @PreDestroy
            public void close() {
                executor.shutdown();
            }
        };
    }

    /* asynchronousUnsafe */

    public static Dispatcher asynchronousUnsafe() {
        return asynchronousUnsafe(ErrorHandlers.rethrow());
    }

    public static Dispatcher asynchronousUnsafe(ErrorHandler errorHandler) {
        return asynchronousUnsafe(Runtime.getRuntime().availableProcessors() * 4, errorHandler);
    }

    public static Dispatcher asynchronousUnsafe(int corePoolSize) {
        return asynchronousUnsafe(corePoolSize, ErrorHandlers.rethrow());
    }

    public static Dispatcher asynchronousUnsafe(int corePoolSize, ErrorHandler errorHandler) {
        final ExecutorService executor = new ThreadPoolExecutor(
                corePoolSize, corePoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DefaultThreadFactory("AsynchronousSafe", "dispatcher", false),
                new RejectedExecutionHandler() {
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                    }
                });
        return new DefaultDispatcher(errorHandler, executor, Executors.immediate()) {
            @Override
            @PreDestroy
            public void close() {
                executor.shutdown();
            }
        };
    }

    /* broadcastOrdered */

    public static Dispatcher broadcastOrdered() {
        return broadcastOrdered(ErrorHandlers.rethrow());
    }

    public static Dispatcher broadcastOrdered(ErrorHandler errorHandler) {
        return broadcastOrdered(Runtime.getRuntime().availableProcessors() * 4, errorHandler);
    }

    public static Dispatcher broadcastOrdered(int corePoolSize) {
        return broadcastOrdered(corePoolSize, ErrorHandlers.rethrow());
    }

    public static Dispatcher broadcastOrdered(int corePoolSize, ErrorHandler errorHandler) {
        final ExecutorService publishingExecutor = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DefaultThreadFactory("BroadcastOrdered", "dispatcher", false),
                new RejectedExecutionHandler() {
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                    }
                });
        final ExecutorService subscriberExecutor = new ThreadPoolExecutor(
                corePoolSize, corePoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DefaultThreadFactory("BroadcastOrdered", "dispatcher", false),
                new RejectedExecutionHandler() {
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                    }
                });
        final SubscriberCompletionExecutor subscriberCompletionExecutor = new SubscriberCompletionExecutor(subscriberExecutor);
        return new DefaultDispatcher(errorHandler, new Executor() {
            public void execute(final Runnable command) {
                publishingExecutor.execute(new Runnable() {
                    public void run() {
                        subscriberCompletionExecutor.onPublishStarting();
                        try {
                            command.run();
                        } finally {
                            subscriberCompletionExecutor.waitForCompletion();
                        }
                    }
                });
            }
        }, subscriberCompletionExecutor) {
            @Override
            @PreDestroy
            public void close() {
                publishingExecutor.shutdown();
                subscriberExecutor.shutdown();
            }
        };
    }

    private static final class SubscriberCompletionExecutor implements Executor {

        final Executor executor;
        CompletionService<Void> completionService;
        volatile int count = 0;

        SubscriberCompletionExecutor(Executor executor) {
            this.executor = executor;
        }

        public void execute(Runnable command) {
            completionService.submit(command, null);
            count++;
        }

        void onPublishStarting() {
            completionService = new ExecutorCompletionService<Void>(executor);
            count = 0;
        }

        void waitForCompletion() {
            try {
                while (count-- > 0)
                    completionService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    /* broadcastUnordered */


    public static Dispatcher broadcastUnordered() {
        return broadcastUnordered(ErrorHandlers.rethrow());
    }

    public static Dispatcher broadcastUnordered(ErrorHandler errorHandler) {
        return broadcastUnordered(Runtime.getRuntime().availableProcessors() * 4, errorHandler);
    }

    public static Dispatcher broadcastUnordered(int corePoolSize) {
        return broadcastUnordered(corePoolSize, ErrorHandlers.rethrow());
    }

    public static Dispatcher broadcastUnordered(int corePoolSize, ErrorHandler errorHandler) {
        final ExecutorService executor = new ThreadPoolExecutor(
                corePoolSize, corePoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DefaultThreadFactory("BroadcastUnordered", "dispatcher", false),
                new RejectedExecutionHandler() {
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                    }
                });
        return new DefaultDispatcher(errorHandler, executor, executor) {
            @Override
            @PreDestroy
            public void close() {
                executor.shutdown();
            }
        };
    }

    private static final class Executors {

        private static Executor immediate() {
            return new Executor() {
                public void execute(Runnable command) {
                    command.run();
                }
            };
        }

        private static Executor blocking() {
            return new Executor() {
                public synchronized void execute(Runnable command) {
                    command.run();
                }
            };
        }

        private static Executor blocking(final long blockingTimeout, final TimeUnit unit) {
            return new Executor() {
                private final Lock lock = new ReentrantLock();

                public void execute(Runnable command) {
                    boolean acquired;
                    try {
                        acquired = lock.tryLock(blockingTimeout, unit);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw SubscriberExecutionException.wrap(e);
                    }
                    if (acquired) {
                        try {
                            command.run();
                        } finally {
                            lock.unlock();
                        }
                    } else
                        throw SubscriberExecutionException.wrap(new TimeoutException("Unable to acquire lock in " + blockingTimeout + " " + unit));
                }
            };
        }
    }

    private static final class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;
        private final String poolPrefix;
        private final boolean daemon;

        public DefaultThreadFactory(String poolPrefix, String namePrefix, boolean daemon) {
            notNull(poolPrefix, "Thread pool prefix");
            notNull(namePrefix, "Thread name prefix");
            this.daemon = daemon;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.poolPrefix = poolPrefix + "-" + poolNumber.getAndIncrement() + "-";
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(final Runnable r) {
            return newThread(namePrefix, r);
        }

        public Thread newThread(String name, final Runnable runnable) {
            notNull(runnable, "Runnable");
            final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            final Thread t = new Thread(group, new Runnable() {
                public void run() {
                    Thread.currentThread().setContextClassLoader(ccl);
                    runnable.run();
                }
            }, poolPrefix + name + "-" + threadNumber.getAndIncrement(), 0);
            t.setDaemon(daemon);
            t.setPriority(Thread.currentThread().getPriority());
            return t;
        }
    }
}
