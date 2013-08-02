**Table of Contents**

- [Mycila PubSub](#mycila-pubsub)
	- [Maven Repository](#maven-repository)
	- [Documentation](#documentation)
		- [Sample](#sample)
		- [Usage](#usage)
			- [Subscribing](#subscribing)
			- [Publishing](#publishing)
			- [Synchronous requests](#synchronous-requests)
			- [Asynchronous requests](#asynchronous-requests)
			- [Request answers](#request-answers)
		- [Features](#features)
			- [Topics and Event types](#topics-and-event-types)
			- [In-Memory event system](#in-memory-event-system)
			- [Memory management](#memory-management)
			- [Exception handling](#exception-handling)
			- [Topic Matchers](#topic-matchers)
			- [Annotation support](#annotation-support)
				- [Subscribers](#subscribers)
				- [Publisher](#publisher)
				- [Requests](#requests)
				- [Answering requests](#answering-requests)
			- [Event dispatching strategies](#event-dispatching-strategies)
				- [Synchronous Safe Dispatching](#synchronous-safe-dispatching)
				- [Synchronous Unsafe Dispatching](#synchronous-unsafe-dispatching)
				- [Asynchronous Safe Dispatching](#asynchronous-safe-dispatching)
				- [Asynchronous Unsafe Dispatching](#asynchronous-unsafe-dispatching)
				- [Broadcast Ordered Dispatching](#broadcast-ordered-dispatching)
				- [Broadcast Unordered Dispatching](#broadcast-unordered-dispatching)
				- [Custom strategy](#custom-strategy)
			- [Integration](#integration)
				- [Google Guice](#google-guice)

# Mycila PubSub #

Mycila Event is a new powerful event framework for in-memory event management. It has a lot of features similar to [EventBus](https://eventbus.dev.java.net/) but is better written and uses Java Concurrency features to provide you with:

 * Asynchronous event dispatching
 * Multicasting events
 * Topic matchers: enable a subscriber (and a vetoers) to subscribe to many topics
 * Annotation support: you can annotate your methods to decouple your code from Mycila Event and subscribe, publish and veto
 * Memory management through hard and weak references

__Project Status:__

 - __OSGI Compliant:__ <img width="100px" src="http://www.sonatype.com/system/images/W1siZiIsIjIwMTMvMDQvMTIvMTEvNDAvMzcvMTgzL05leHVzX0ZlYXR1cmVfTWF0cml4X29zZ2lfbG9nby5wbmciXV0/Nexus-Feature-Matrix-osgi-logo.png" title="OSGI Compliant"></img>
 - __Build Status:__ [![Build Status](https://travis-ci.org/mycila/pubsub.png?branch=master)](https://travis-ci.org/mycila/pubsub)
 - __Issues:__ https://github.com/mycila/pubsub/issues

## Maven Repository ##

__Releases__

Available in Maven Central Repository: http://repo1.maven.org/maven2/com/mycila/pubsub/

__Snapshots__
 
Available in OSS Repository:  https://oss.sonatype.org/content/repositories/snapshots/com/mycila/pubsub/

__Maven dependency__

    <dependency>
        <groupId>com.mycila</groupId>
        <artifactId>mycila-pubsub</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Maven sites__

 - [5.0.ga](http://mycila.github.io/pubsub/reports/5.0.ga/index.html)

## Documentation ##

Mycila Event is a new powerful event framework for in-memory event management. It has a lot of features similar to [EventBus](https://eventbus.dev.java.net/) but is better designed, uses Java Concurrency features and has a lot of more event features than EventBus, which are really useful when you work with a complex system driven by event messaging.

### Sample ###

    import static com.mycila.event.api.topic.Topics.*;
    
    // first create an event service
    Dispatcher dispatcher = Dispatchers.synchronousSafe(ErrorHandlers.rethrowErrorsAfterPublish());
    
    // then subscribe
    TopicMatcher matcher = only("app/events/swing/button").or(topics("app/events/swing/fields/**"));
    dispatcher.subscribe(matcher, String.class, new Subscriber<String>() {
        public void onEvent(Event<String> event) throws Exception {
            System.out.println("Received: " + event.source());
        }
    });
    
    // and publish
    dispatcher.publish(topic("app/events/swing/button"), "Hello !");

When you subscribe, you need to give to which topic to subscribe and for which event type.

### Usage ###

#### Subscribing ####

Subscribing is done with the Dispatcher.subscribe method, which take the topic to subscribe to, the event type and an instance of `Subscriber<T>`, where T is the event type. The method receives an Event object, containing the timestamp (in nanoseconds) and the source.

__Example:__

    dispatcher.subscribe(only("prog/events/a").or(matching("prog/events/b/**")), String.class, new Subscriber<String>() {
        public void onEvent(Event<String> event) throws Exception {
            sequence.add(event.getSource());
        }
    });

#### Publishing ####

Publishing is done by simply sending to a topic an event object.

    dispatcher.publish(topic("prog/events/a"), "Hello for a");

#### Synchronous requests ####

An event system is asynchronous by default, but you sometimes need to wait for an answer before proceeding. This is the request/response pattern that everyone know. You can create a request and wait for its response (or wait with a timeout). The request is created through `Messages.createRequest`, where you can pass request parameters. The you call `MessageRequest.getResponse()` to obtain the response.

    MessageRequest<Integer> adddRequest = Messages.createRequest(new int[]{1, 2, 3, 4, 5}, "param2");
    dispatcher.publish(topic("system/add"), adddRequest);
    int sum = adddRequest.getResponse(1, TimeUnit.SECOND);

#### Asynchronous requests ####

You can also request in asynchronous mode by adding listeners, which will be triggered when the response will be received;

    MessageRequest<Integer> adddRequest = Messages.createRequest(new int[]{1, 2, 3, 4, 5}, "param2");
    adddRequest.addListener(new MessageListener<Integer>() {
        public void onResponse(Integer value) {
            assertEquals(15, value.intValue());
        }
    
        public void onError(Throwable t) {
            t.printStackTrace();
            fail();
        }
    });
    dispatcher.publish(topic("system/add"), adddRequest);

#### Request answers ####

To be able to respond to an answer on a topic, you simply have to subscribe, with the specific event type `MessageResponse`:

    dispatcher.subscribe(only("system/add"), MessageResponse.class, new Subscriber<MessageResponse<Integer>>() {
        public void onEvent(Event<MessageResponse<Integer>> event) throws Exception {
            int[] p = (int[]) event.getSource().getParameters()[0];
            System.out.printl(event.getSource().getParameters()[1]); // output the second parameter
            int c = 0;
            for (int i : p) c += i;
            event.getSource.reply(c);
        }
    });

The event type is a special class which enable to take the parameters and reply a response. You could also respond by an exception if an error occured in the subscriber:

    event.getSource().replyError(new ArithmeticException("Overflow !"));

### Features ###

#### Topics and Event types ####

When you subscribe, you subscribe in a Topic for a given event type. Event type subclasses are allowed to be received by a subscriber accepting its super-class. In example, if you subscribe to `Topic.topic("buttons/ok")` with event type `ActionListener.class`, you can publish any implementation of `ActionListener` and it will be received by subscribers accepting the type (and sub-types) `ActionListener`.

#### In-Memory event system ####

Mycila Event is not a JMS solution ! Like EventBus, Mycila Event resolves intra-process communication. In example, it can be used in a Swing GUI or in a complex modular framework to handle communication between plugins.

Thus, __Mycila Event must be fast, thread-safe and scalable.__

#### Memory management ####

Like EventBus, Mycila Event supports *hard and weak subscriptions*. A hard subscription will always remain and must be unregistered if not needed anymore. A weak subscription will be automatically removed when the subscriber is no longer in use.

By default, if nothing is specified, Mycila Event uses a hard reference. this is very useful when you simply bind a listener like this:

    dispatcher.subscribe(matcher, String.class, new Subscriber<String>() {
        public void onEvent(Event<String> event) throws Exception {
            System.out.println("Received: " + event.source());
        }
    });

Reachability control can be done by annotating the class with `@Reference`. In example, suppose you have a plugin class subscribing for events. You can annotate the class like this:

    @Reference(Reachability.WEAK)
    public class MyPlugin implements Subscriber<String> {
        public void onEvent(Event<String> event) throws Exception {
            System.out.println("Received: " + event.source());
        }
        public void start() {
            // start the plugin
        }
    }
    
    MyPlugin pluginLoadedByAnotherSystem = ...;
    dispatcher.subscribe(matcher, String.class, pluginLoadedByAnotherSystem);

When registering the plugin, a weak registration will be done so that if the plugin is unloaded or not used anymore, the subscription could be removed automatically.

`@Reference` can also be put on methods, when used with annotations.

#### Exception handling ####

By default, if a subscriber launches an exception, the exception is rethrown immediately, in the thread firering the event. You can change this behavior by providing your own `ErrorHandler`, or by using existing one:

 - `ErrorHandlers.ignoreErrors()`: ignore all exceptions thrown by subscribers
 - `ErrorHandlers.rethrow()`: the default behavior.

To create an Dispatcher with the appropriate event handler, you just have to set the `ErrorHandler` instance when creating it:

    Dispatcher dispatcher = Dispatchers.synchronousSafe(ErrorHandlers.ignoreErrors());

#### Topic Matchers ####

When you register a subscriber, you need to pass the type of event you want to receive and a matcher to math topics you want to listen to. TopicMatcher can be created with the `Topics` class. You can compose matchers.

 * `Topics.only(exactName)`: matches a single topic name. *Example:* `Topics.only("app/events/buttons/ok")`
 * `Topics.topics(pattern)`: matches several topic by using an Ant expression. *Example:* `Topics.topics("app/events/buttons/**")`
 * `Topics.any()`: matches any topic
 * `Topics.not(matcher)`: invert the given matcher
 * `matcher.and(matcher)`: any matcher can be composed with another matcher with an *and*. In this case, all matchers must match given topic event for the subscriber to receive it. This is quite an uncommon case.
 * `matcher.or(matcher)`: any matcher can be composed with another matcher with an *or*. In this case, the subscriber will receive events matching the two matchers. This is a common case where you would like to receive in one subscriber the same events from different topics.

In example, to set a catch-all subscriber, you could do:

    dispatcher.subscribe(Topics.any(), Object.class, new Subscriber<Object>() {
        public void onEvent(Event<Object> event) throws Exception {
            System.out.println("Received: " + event.source());
        }
    });

#### Annotation support ####

Mycila Event provides annotations to create publishers and subscribers decoupled from the Dispatcher service.

##### Subscribers #####

You can annotate any method in your class to receive events with the `@Subscribe` annotation. It takes in arguments the list of topics (an Ant expression matching topics) to listen to and the type of event.

These annotations must be placed on methods having one parameter: `Event` for listening to events.

You can use the `@Reference` annotation to control if the listeners is weak or not. This annotation can be place on the method, or, if you have in your class several annotated methods to listen for events, you can place the `@Reference` annotation in the class directly.

___By default, a subscription has a HARD reference. So be VERY CAREFUL when you register listeners implemented by your classes to make them WEAK if they have a shorter lifecycle than the Dispatcher.___

__Subscribe to events__

    class MyClass1 {
        @Subscribe(topics = "prog/events/a/**", eventType = String.class)
        private void handle(Event<String> event) {
            // do something
        }
    }

In your code, after having created a `Dispatcher`, you can use the `AnnotationProcessors` to register annotated methods like this:

    MyClass1 c1 = new ...
    
    AnnotationProcessor processor = AnnotationProcessors.create(dispatcher);
    processor.process(c1);;

##### Publisher #####

Publishers can be created using the annotation `@Publish`. You can completely decouple your code by creating interface (or abstract classes) that will be automatically generated thanks to annotations.

In example:

    private static interface B {
        @Publish(topics = "prog/events/a")
        void send(String a, int b);
    }
    
    static abstract class C {
        @Publish(topics = {"prog/events/a/a1", "prog/events/a/allA"})
        abstract void send(String a, int b);
    }

These two classes defines publishing methods. B will publish two events (a string and an integer) to one topic, and C will publish two events in two topics.

To automatically generate their implementation, you can use, after having created a Dispacther:

    B b = annotationProcessor.proxy(B.class);
    C c = annotationProcessor.proxy(C.class);
    
    b.send("Hello for a", 1);
    c.send("Hello for a1", 4);

B and C are classes in your code that you can use directly to publish events.

You can annotate a generated Publisher method by `@Multiple`. If the publishing method is given an array or collection of objects, each object will be published independently as an event.

In example:

    abstract class MyCustomPublisher2 {
        @Publish(topics = "a/topic/path")
        @Multiple
        abstract void send(int event1, String... otherEvents);
    }
    [...]
    myCustomPublisher2.send(1, "each", "string", "will", "be", "an", "event")

__Note__: you can then create abstract classes which act sa event managers: abstract methods annotated by @Publish will be generated and concrete methods annotated by @Subscribe will receive events.

##### Requests #####

Requesting methods acts the same as publishers. They differ in the annotation, which provides a way to timeout and wait for the response to come back. In example, suppose you have an additioner plugin exposing its computation method to the topic `system/add`. You can create a subscriber like this:

    interface Requestor {
        @Request(topic = "system/add", timeout = 1, unit = TimeUnit.SECONDS)
        int addNumbers(int... p);
    }
    Dispatcher dispatcher = Dispatchers.synchronousSafe();
    AnnotationProcessor processor = AnnotationProcessors.create(dispatcher);
    Requestor req = processor.proxy(Requestor.class);
    assertEquals(15, req.addNumbers(1,2,3,4,5));

Given the nature of a method call, it is obvious that the call is synchronous and will wait either indefinitely for a response or only with the given time.

##### Answering requests #####

There is two way for answering requests: as we seen, we have to register a simple subscriber. It can be done like this:

    static class DU {
        @Subscribe(topics = "system/du", eventType = MessageResponse.class)
        void duRequest(Event<MessageResponse<Integer>> event) {
            String folder = (String) event.getSource().getParameters()[0];
            System.out.println("du request on folder " + folder);
            // call du -h <folder>
            if ("notFound".equals(folder))
                event.getSource().replyError(new FileNotFoundException("du did not found folder " + folder));
            else
                event.getSource().reply(40);
        }
    }
    Dispatcher dispatcher = Dispatchers.synchronousSafe();
    AnnotationProcessor processor = AnnotationProcessors.create(dispatcher);
    DU du = new DU();
    processor.process(du);

Hopefully, there is a better way. The request sends as parameter a String, and the subscriber replies with an integer. So what if we can simply call a method ? This can be done like this:

    static class DU {
        @Answers(topics = "system/du")
        int getSize(String folder) throws FileNotFoundException {
            System.out.println("du request on folder " + folder);
            // call du -h <folder>
            if ("notFound".equals(folder))
                throw new FileNotFoundException("du did not found folder " + folder);
            return 40;
        }
    }
    Dispatcher dispatcher = Dispatchers.synchronousSafe();
    AnnotationProcessor processor = AnnotationProcessors.create(dispatcher);
    DU du = new DU();
    processor.process(du);

The method output (or the exception) will then be used as the reply if an request is made to the topic `system/du`.

#### Event dispatching strategies ####

There are several strategies regarding about how you want the order of events and the order of listeners be respected and whether or not you have multiple threads publishing events.

##### Synchronous Safe Dispatching #####

 * Listeners are called in the order they subscribed
 * Events are published __one after one__
 * Only one thread can publish at a time

This strategy guarantees the order of listeners called and that only one thread will hit the listeners at a time. Thus, your listeners don't need to be thread-safe. The publish method thus block until the previous publishing is finished.

This behavior allows you to have stateful non thread-safe subscribers. When multiple threads are publishing, this strategy is slower.

    Dispatcher dispatcher = Dispatchers.synchronousSafe();

##### Synchronous Unsafe Dispatching #####

 * Listeners are called in the order they subscribed
 * Events are published immediately
 * Several threads can publish at a time

This strategy guarantees the order of listeners called. The publish method only block for the current thread, meaning that a thread can be publishing while another thread also starting publishing an event. Thus, your subscribers can be hit at the same time by two or more threads.

Your subscribers need to be stateless and thread-safe. Multiple threads can publish at the same time.

    Dispatcher dispatcher = Dispatchers.synchronousUnsafe();

##### Asynchronous Safe Dispatching #####

 * Listeners are called in the order they subscribed
 * Events are published one after one
 * Many threads can enqueue an event at one time
 * One background thread is responsible to dequeue and fire events
 * A queue is used to enqueue events before they are processed.

This strategy guarantees the order of listeners called and the order of published events. The publish method *does not block* and the publisher thread immediately returns. The event is queued and wait for its turn to be processed by the background thread.

This strategy is useful when your publishers must execute as fast as possible, but be careful to also have fast subscribers to not fill the queue. This behavior allows you to have stateful non thread-safe subscribers since only one thread will dequeue and fire events.

Since this strategy uses an unbounded queue, be careful to have fast subscribers and to not publish events more than your subscribers can consume.

    Dispatcher dispatcher = Dispatchers.asynchronousSafe();

##### Asynchronous Unsafe Dispatching #####

 * Listeners are called in the order they subscribed
 * Several events (so listeners) are processed at the same time
 * Many threads can publish an event at one time
 * Several background threads are responsible for firing events

This strategy guarantees the order of listeners called, but not the order of event. Basically, each call to publish will return immediately. All events in the queue are handled by a thread pool.

This strategy is useful when your publishers must execute as fast as possible, and event publishing needs to be processed quickly, but by respecting listener order. This dispatcher can be seen as a concurrent event dispatcher, respecting listener order.

Since this strategy uses an unbounded queue, be careful to have fast subscribers and to not publish events more than your subscribers can consume.

Your subscribers need to be stateless and thread-safe. Multiple threads can send events at the same time.

    Dispatcher dispatcher = Dispatchers.asynchronousUnsafe();

##### Broadcast Ordered Dispatching #####

 * Listeners are called unordered
 * Events are published one after one
 * Many threads can enqueue an event at one time
 * A thread-pool is used to send each event to each subscriber

This strategy guarantees the order of events but calls listeners unordered. The goal of a broadcasting is to reach as fast as possible each listeners in the smallest amount of time. A thread-pool is used to handle subscriber's execution.

This type of dispatching is really useful when you don't care about ordering and want to publish fast and want your subscribers to be called as fast as possible.

Note that when a thread publishes an event, it is enqueued. A thread is used to enqueue events one per one and fire this event concurrently to all subscribers. The publish method returns immediately, but for another event to be processed, all concurrent firing of the previous event must have finished.

This guarantees that all subscribers will be called concurrently, but they will all receive the events in the same order.

    Dispatcher dispatcher = Dispatchers.broadcastOrdered();

##### Broadcast Unordered Dispatching #####

 * Listeners are called unordered
 * Several events are published at the same time
 * Many threads can enqueue an event at one time
 * A thread-pool is used to send each event to each subscriber

This type of dispatching is really useful when you don't care about any ordering. Since the publish method does not block, any thread will be able to publish events really fast and a thread-pool is used to process them all, unordered.

Thus, subscribers taking a long time to execute don't affect publishing of other events to other subscribers.

    Dispatcher dispatcher = Dispatchers.broadcastUnordered();

##### Custom strategy #####

You can easily implelement and control your own dispatching strategy: simply look at the source code of `Dispatchers` to have more example. You can create a custom dispacther like this:

    Dispatcher dispatcher = Dispatchers.custom(errorHandlerProvider, publishingExecutor, subscriberExecutor);

Executors are implementations of `java.util.concurrent.Executor`. The first one control the concurrency of the whole publishing process and the second one control the concurrency for calling a subscriber. The class `com.mycila.event.util.Executors` has two flavors that can help you for basic cases: 

 * `Executors.immediate()`: execute the runnable immediately
 * `Executors.blocking()`: idem, but blocks so that only one thread can execute a subscriber at a time

#### Integration ####

##### Google Guice #####

http://code.google.com/p/google-guice/

Mycila Event can be used without any IOC. But thanks to the powerful annotation support and injection listeners of [http://code.google.com/p/google-guice/ Google Guice], this dependency injection library integrates very well with Mycila Event.

__Binding generated publishers__

    public final class MyModule implements Module {
        @Override
        public void configure(Binder binder) {
            MycilaEventGuice.bindPublisher(binder, MyCustomPublisher.class).in(Singleton.class);
            MycilaEventGuice.bindPublisher(binder, MyCustomPublisher2.class).in(Singleton.class);
            MycilaEventGuice.bindPublisher(binder, MyCustomPublisher3.class).in(Singleton.class);
        }
    
        @Reference(Reachability.WEAK)
        static interface MyCustomPublisher {
            @Publish(topics = "a/topic/path")
            void send(String... messages);
        }
    
        static abstract class MyCustomPublisher2 {
            @Publish(topics = "a/topic/path")
            @Multiple
            abstract void send(int event1, String... otherEvents);
        }
    
        static abstract class MyCustomPublisher3 {
            @Publish(topics = "a/topic/path")
            @Multiple
            abstract void send(int event1, Iterable<String> events);
        }
    }
    
    injector.getInstance(MyCustomPublisher.class).send("A", "cut", "message", "containing", "bad words");
    injector.getInstance(MyCustomPublisher2.class).send(1, "A", "cut", "message", "containing", "bad words", "in varg");
    injector.getInstance(MyCustomPublisher3.class).send(1, Arrays.asList("A", "cut", "message", "containing", "bad words", "in list"));

__Automatically inject publishers and create subscriptions__

Suppose you have a class like this:

    public final class MyImpl implements MyClass {
    
        Publisher<String> publisher;
    
        @Subscribe(topics = "a/topic/path", eventType = String.class)
        void subscribe(Event<String> event) {
            System.out.println("(subscribe) Got: " + event);
        }
    
        @Subscribe(topics = "a/topic/path", eventType = String[].class)
        void subscribeToList(Event<String[]> event) {
            System.out.println("(subscribeToList) Got: " + Arrays.toString(event.source()));
        }
    
        @Subscribe(topics = "a/topic/path", eventType = Integer.class)
        void subscribeToInts(Event<Integer> event) {
            System.out.println("(subscribeToInts) Got: " + event.source());
        }
    
        @Publish(topics = "a/topic/path")
        void publisher(Publisher<String> publisher) {
            System.out.println("Publisher injected");
            publisher.publish("Hello from publisher !");
            this.publisher = publisher;
        }
    }

When configuring Guice, simply put MycileEventModule like this:

    Injector injector = Guice.createInjector(new MycilaEventGuiceModule(), new AbstractModule() {
    	@Override
    	public void configure(Binder binder) {
    		binder.bind(MyClass.class).to(MyImpl.class);
    	}        
    });

All the created instances by Guice will automatically discover MycilaEvent annotations and subscribing methods will be registered and publishers will be injected.



[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/25803a0aff184f73d36916c178ef1f2c "githalytics.com")](http://githalytics.com/mycila/pubsub)
