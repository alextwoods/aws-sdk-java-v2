/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.ControlledHttp;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Exchange;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Reply;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 8.4, 8.5, 8.9 and 8.10: the generated event stream methods of a synthetic restJson1 service (output,
 * input and bidirectional streams) and an awsJson1_1 service (an output stream after {@code initial-response}, an
 * input stream after {@code initial-request}) run end to end through the engine over a held fake transport. A probe
 * compiled with the generated sources calls the public API with a recording {@code <Op>ResponseHandler} and a
 * controllable {@code Publisher} of input events; response frames are encoded with the eventstream
 * {@link MessageEncoder} and request frames decoded with {@link MessageDecoder}.
 */
class GeneratedEventStreamClientTest {
    private static final String REST_PACKAGE = "software.amazon.awssdk.test.codegen.streams";
    private static final String RPC_PACKAGE = "software.amazon.awssdk.test.codegen.watch";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String PUMP_THREAD = "aws-sdk-event-stream-publisher";

    private static final String REST_MODEL = """
        $version: "2"
        namespace example.streams

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "streams")
        @aws.api#service(sdkId: "Streams", endpointPrefix: "streams")
        service Streams {
            version: "2024-01-01"
            operations: [Subscribe, Publish, Chat, SecurePublish, SecureChat, SecureSubscribe]
        }

        @auth([])
        @http(method: "POST", uri: "/subscribe")
        operation Subscribe {
            input := {
                @httpHeader("x-topic")
                topic: String
            }
            output := {
                @httpHeader("x-session")
                sessionId: String
                @httpPayload
                events: OutEvents
            }
        }

        @auth([])
        @http(method: "POST", uri: "/publish")
        operation Publish {
            input := {
                @httpHeader("x-topic")
                topic: String
                @required
                @httpPayload
                events: InEvents
            }
            output := {
                @httpHeader("x-count")
                count: Integer
            }
        }

        @http(method: "POST", uri: "/secure")
        operation SecurePublish {
            input := {
                @httpPayload
                events: InEvents
            }
        }

        @http(method: "POST", uri: "/secure-subscribe")
        operation SecureSubscribe {
            input := {
                @httpHeader("x-topic")
                topic: String
            }
            output := {
                @httpPayload
                events: OutEvents
            }
        }

        @http(method: "POST", uri: "/secure-chat")
        operation SecureChat {
            input := {
                @httpPayload
                events: InEvents
            }
            output := {
                @httpPayload
                events: OutEvents
            }
        }

        @auth([])
        @http(method: "POST", uri: "/chat")
        operation Chat {
            input := {
                @httpPayload
                events: InEvents
            }
            output := {
                @httpPayload
                events: OutEvents
            }
        }

        @streaming
        union OutEvents {
            message: MessageEvent
            failure: FailureEvent
        }

        @streaming
        union InEvents {
            message: MessageEvent
        }

        structure MessageEvent {
            text: String
        }

        @error("client")
        structure FailureEvent {
            reason: String
        }
        """;

    private static final String RPC_MODEL = """
        $version: "2"
        namespace example.watch

        @aws.protocols#awsJson1_1
        @aws.auth#sigv4(name: "watch")
        @aws.api#service(sdkId: "Watch", endpointPrefix: "watch")
        service Watch {
            version: "2024-01-01"
            operations: [WatchChanges, Upload]
        }

        @auth([])
        operation WatchChanges {
            input := { name: String }
            output := {
                status: String
                events: WatchEvents
            }
        }

        @auth([])
        operation Upload {
            input := {
                name: String
                events: UploadEvents
            }
            output := { count: Integer }
        }

        @streaming
        union WatchEvents {
            change: ChangeEvent
        }

        @streaming
        union UploadEvents {
            chunk: ChunkEvent
        }

        structure ChangeEvent {
            path: String
        }

        structure ChunkEvent {
            data: String
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.util.List;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.atomic.AtomicReference;
        import org.reactivestreams.Publisher;
        import org.reactivestreams.Subscriber;
        import org.reactivestreams.Subscription;
        import software.amazon.awssdk.core.async.SdkPublisher;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
        import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.smithy.java.core.serde.event.EventStream;
        import REST.StreamsAsyncClient;
        import REST.model.ChatRequest;
        import REST.model.ChatResponse;
        import REST.model.ChatResponseHandler;
        import REST.model.SecureChatRequest;
        import REST.model.SecureChatResponse;
        import REST.model.SecureChatResponseHandler;
        import REST.model.SecureSubscribeResponse;
        import REST.model.SecureSubscribeResponseHandler;
        import REST.model.InEvents;
        import REST.model.MessageEvent;
        import REST.model.OutEvents;
        import REST.model.PublishRequest;
        import REST.model.SubscribeRequest;
        import REST.model.SubscribeResponse;
        import REST.model.SubscribeResponseHandler;
        import RPC.WatchAsyncClient;
        import RPC.model.ChunkEvent;
        import RPC.model.UploadEvents;
        import RPC.model.WatchChangesResponse;
        import RPC.model.WatchChangesResponseHandler;
        import RPC.model.WatchEvents;

        public final class EventStreamProbe {
            private EventStreamProbe() {
            }

            public static AutoCloseable streams(SdkAsyncHttpClient http, boolean failingCredentials) {
                var builder = StreamsAsyncClient.builder().region(Region.of("us-east-1"))
                    .endpointOverride(URI.create("https://streams.test")).httpClient(http);
                if (failingCredentials) {
                    builder.credentialsProvider(new IdentityProvider<AwsCredentialsIdentity>() {
                        @Override
                        public Class<AwsCredentialsIdentity> identityType() {
                            return AwsCredentialsIdentity.class;
                        }

                        @Override
                        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest r) {
                            return CompletableFuture.failedFuture(new IllegalStateException("no credentials"));
                        }
                    });
                }
                return builder.build();
            }

            /** A streams client with static credentials, for the SigV4 signed calls. */
            public static AutoCloseable secureStreams(SdkAsyncHttpClient http) {
                AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("AKIDEXAMPLE", "SECRETEXAMPLE");
                return StreamsAsyncClient.builder().region(Region.of("us-east-1"))
                    .endpointOverride(URI.create("https://streams.test")).httpClient(http)
                    .credentialsProvider(new IdentityProvider<AwsCredentialsIdentity>() {
                        @Override
                        public Class<AwsCredentialsIdentity> identityType() {
                            return AwsCredentialsIdentity.class;
                        }

                        @Override
                        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest r) {
                            return CompletableFuture.completedFuture(credentials);
                        }
                    }).build();
            }

            public static AutoCloseable watch(SdkAsyncHttpClient http) {
                return WatchAsyncClient.builder().region(Region.of("us-east-1"))
                    .endpointOverride(URI.create("https://watch.test")).httpClient(http).build();
            }

            /** The input events: each text becomes a message event. */
            public static Publisher<InEvents> messages(Publisher<String> texts) {
                return map(texts, t -> InEvents.fromMessage(MessageEvent.builder().text(t).build()));
            }

            public static CompletableFuture<Void> subscribe(Object client, List<String> log,
                                                            AtomicReference<Subscription> subscription) {
                return ((StreamsAsyncClient) client).subscribe(r -> r.topic("t"), new SubscribeResponseHandler() {
                    @Override
                    public void responseReceived(SubscribeResponse response) {
                        log.add("response:" + response.sessionId());
                    }

                    @Override
                    public void onEventStream(SdkPublisher<OutEvents> publisher) {
                        publisher.subscribe(recorder(log, subscription, e -> e.message().text()));
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        log.add("exception:" + throwable.getClass().getSimpleName());
                    }

                    @Override
                    public void complete() {
                        log.add("complete");
                    }
                });
            }

            public static CompletableFuture<?> publish(Object client, Publisher<String> texts) {
                // The engine's own future: cancelling it cancels the call.
                return ((StreamsAsyncClient) client).publish(r -> r.topic("t"), messages(texts));
            }

            public static CompletableFuture<?> securePublish(Object client, Publisher<String> texts) {
                return ((StreamsAsyncClient) client).securePublish(r -> { }, messages(texts));
            }

            public static CompletableFuture<Void> secureChat(Object client, Publisher<String> texts,
                                                             List<String> log,
                                                             AtomicReference<Subscription> subscription) {
                return ((StreamsAsyncClient) client).secureChat(SecureChatRequest.builder().build(), messages(texts),
                                                                new SecureChatResponseHandler() {
                    @Override
                    public void responseReceived(SecureChatResponse response) {
                        log.add("response");
                    }

                    @Override
                    public void onEventStream(SdkPublisher<OutEvents> publisher) {
                        publisher.subscribe(recorder(log, subscription, e -> e.message().text()));
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        log.add("exception:" + throwable.getClass().getSimpleName());
                    }

                    @Override
                    public void complete() {
                        log.add("complete");
                    }
                });
            }

            public static CompletableFuture<Void> secureSubscribe(Object client, List<String> log,
                                                                  AtomicReference<Subscription> subscription) {
                return ((StreamsAsyncClient) client).secureSubscribe(r -> r.topic("t"),
                                                                     new SecureSubscribeResponseHandler() {
                    @Override
                    public void responseReceived(SecureSubscribeResponse response) {
                        log.add("response");
                    }

                    @Override
                    public void onEventStream(SdkPublisher<OutEvents> publisher) {
                        publisher.subscribe(recorder(log, subscription, e -> e.message().text()));
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        log.add("exception:" + throwable.getClass().getSimpleName());
                    }

                    @Override
                    public void complete() {
                        log.add("complete");
                    }
                });
            }

            public static CompletableFuture<Void> chat(Object client, Publisher<String> texts, List<String> log,
                                                       AtomicReference<Subscription> subscription) {
                return ((StreamsAsyncClient) client).chat(ChatRequest.builder().build(), messages(texts),
                                                          new ChatResponseHandler() {
                    @Override
                    public void responseReceived(ChatResponse response) {
                        log.add("response");
                    }

                    @Override
                    public void onEventStream(SdkPublisher<OutEvents> publisher) {
                        publisher.subscribe(recorder(log, subscription, e -> e.message().text()));
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        log.add("exception:" + throwable.getClass().getSimpleName());
                    }

                    @Override
                    public void complete() {
                        log.add("complete");
                    }
                });
            }

            public static CompletableFuture<Void> watchChanges(Object client, List<String> log,
                                                               AtomicReference<Subscription> subscription) {
                return ((WatchAsyncClient) client).watchChanges(r -> r.name("n"), new WatchChangesResponseHandler() {
                    @Override
                    public void responseReceived(WatchChangesResponse response) {
                        log.add("response:" + response.status());
                    }

                    @Override
                    public void onEventStream(SdkPublisher<WatchEvents> publisher) {
                        publisher.subscribe(recorder(log, subscription, e -> e.change().path()));
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        log.add("exception:" + throwable.getClass().getSimpleName());
                    }

                    @Override
                    public void complete() {
                        log.add("complete");
                    }
                });
            }

            public static CompletableFuture<?> upload(Object client, Publisher<String> data) {
                return ((WatchAsyncClient) client).upload(r -> r.name("n"),
                    map(data, d -> UploadEvents.fromChunk(ChunkEvent.builder().data(d).build())));
            }

            public static Integer count(Object response) {
                return response instanceof REST.model.PublishResponse p ? p.count()
                                                                        : ((RPC.model.UploadResponse) response).count();
            }

            /** toBuilder keeps the event stream member; equals and hashCode ignore it. */
            public static String roundTrip() {
                EventStream<InEvents> events = EventStream.newWriter();
                PublishRequest plain = PublishRequest.builder().topic("t").build();
                PublishRequest.Builder builder = plain.toBuilder();
                builder.setMemberValue(plain.schema().member("events"), events);
                PublishRequest withStream = builder.build();
                Object kept = withStream.toBuilder().build().getMemberValue(plain.schema().member("events"));
                return (kept == events) + "," + withStream.equals(plain) + ","
                       + (withStream.hashCode() == plain.hashCode()) + ","
                       + (plain.getMemberValue(plain.schema().member("events")) == null);
            }

            private static <E> Subscriber<E> recorder(List<String> log, AtomicReference<Subscription> subscription,
                                                      java.util.function.Function<E, String> describe) {
                return new Subscriber<E>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription.set(s);
                    }

                    @Override
                    public void onNext(E event) {
                        log.add("event:" + event.getClass().getSimpleName() + ":" + describe.apply(event));
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.add("onError:" + t.getClass().getSimpleName());
                    }

                    @Override
                    public void onComplete() {
                        log.add("onComplete");
                    }
                };
            }

            private static <A, B> Publisher<B> map(Publisher<A> source, java.util.function.Function<A, B> f) {
                return subscriber -> source.subscribe(new Subscriber<A>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        subscriber.onSubscribe(s);
                    }

                    @Override
                    public void onNext(A a) {
                        subscriber.onNext(f.apply(a));
                    }

                    @Override
                    public void onError(Throwable t) {
                        subscriber.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        }
        """.replace("REST.", REST_PACKAGE + ".").replace("RPC.", RPC_PACKAGE + ".");

    @TempDir
    static Path temp;

    private static URLClassLoader loader;
    private static Class<?> probe;

    private ControlledHttp http;
    private final List<AutoCloseable> clients = new ArrayList<>();

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path sources = temp.resolve("sources");
        generate("streams", REST_MODEL, "example.streams#Streams", REST_PACKAGE, sources);
        generate("watch", RPC_MODEL, "example.watch#Watch", RPC_PACKAGE, sources);
        Path file = Files.createDirectories(sources.resolve("example/probe")).resolve("EventStreamProbe.java");
        Files.writeString(file, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
        probe = Class.forName("example.probe.EventStreamProbe", true, loader);
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @BeforeEach
    void transport() {
        http = new ControlledHttp();
    }

    @AfterEach
    void closeClients() throws Exception {
        for (AutoCloseable client : clients) {
            client.close();
        }
        http.eventLoop.shutdownNow();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Output event streams

    @Test
    void anOutputStreamIsHandedToTheHandlerUnderDemandAndCompletes() throws Exception {
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("subscribe", streams(false), log, subscription);
        Exchange exchange = http.next();
        assertEquals("t", exchange.request.headers().firstValue("x-topic").orElse(null));
        FramePublisher frames = new FramePublisher(http.eventLoop, message("a"), message("b"), message("c"));
        respond(exchange, Map.of("x-session", "s1"), frames);
        await(() -> subscription.get() != null);
        assertEquals(List.of("response:s1"), log);

        subscription.get().request(2);
        await(() -> events(log).size() == 2);
        settle();
        assertEquals(List.of("event:OutEvents:a", "event:OutEvents:b"), events(log));
        assertTrue(frames.delivered.get() <= 3, "no read-ahead beyond one chunk: " + frames.delivered.get());
        assertFalse(future.isDone());

        subscription.get().request(5);
        assertEquals(null, future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(List.of("response:s1", "event:OutEvents:a", "event:OutEvents:b", "event:OutEvents:c",
                             "onComplete", "complete"), log);
    }

    @Test
    void aModeledExceptionEventFailsTheCallOnce() throws Exception {
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("subscribe", streams(false), log, subscription);
        respond(http.next(), Map.of(), new FramePublisher(http.eventLoop, message("a"),
                                                           exception("failure", "{\"reason\":\"no\"}")));
        await(() -> subscription.get() != null);
        subscription.get().request(10);
        Throwable failure = failure(future);
        assertTrue(chain(failure).stream().anyMatch(t -> t.getClass().getSimpleName().contains("Failure")),
                   "cause chain " + chain(failure));
        assertEquals(1, log.stream().filter(l -> l.startsWith("exception:")).count(), log.toString());
        assertTrue(log.contains("event:OutEvents:a"), log.toString());
        assertFalse(log.contains("complete"), log.toString());
    }

    @Test
    void anErrorFrameFailsTheCallOnce() throws Exception {
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("subscribe", streams(false), log, subscription);
        respond(http.next(), Map.of(), new FramePublisher(http.eventLoop, error("Boom", "bad")));
        await(() -> subscription.get() != null);
        subscription.get().request(10);
        Throwable failure = failure(future);
        assertTrue(chain(failure).stream().anyMatch(t -> String.valueOf(t.getMessage()).contains("bad")),
                   "cause chain " + chain(failure));
        assertEquals(1, log.stream().filter(l -> l.startsWith("exception:")).count(), log.toString());
        assertFalse(log.contains("complete"), log.toString());
    }

    @Test
    void cancellingTheFutureCancelsTheExchange() throws Exception {
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("subscribe", streams(false), log, subscription);
        Exchange exchange = http.next();
        FramePublisher frames = new FramePublisher(http.eventLoop, message("a"), message("b"));
        respond(exchange, Map.of(), frames);
        await(() -> subscription.get() != null);
        subscription.get().request(1);
        await(() -> events(log).size() == 1);

        assertTrue(future.cancel(true));
        await(() -> exchange.reasons.contains(CancellationReason.CALLER_CANCELLED));
        await(frames.cancelled::get);
    }

    @Test
    void anRpcOutputStreamStartsWithTheInitialResponse() throws Exception {
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("watchChanges", watch(), log, subscription);
        Exchange exchange = http.next();
        respond(exchange, Map.of(), new FramePublisher(http.eventLoop,
                                                       event("initial-response", "{\"status\":\"ok\"}"),
                                                       event("change", "{\"path\":\"/p\"}")));
        await(() -> subscription.get() != null);
        assertEquals(List.of("response:ok"), log);
        subscription.get().request(5);
        assertEquals(null, future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(List.of("response:ok", "event:WatchEvents:/p", "onComplete", "complete"), log);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Input event streams

    @Test
    void theInputEventsAreTheRequestBodysFramesUnderTransportDemand() throws Exception {
        EventSource texts = new EventSource("a", "b", "c");
        texts.completeWhenDrained = true;
        CompletableFuture<?> future = call("publish", streams(false), texts);
        Exchange exchange = http.next();
        assertEquals("application/vnd.amazon.eventstream",
                     exchange.request.headers().firstValue("Content-Type").orElse(null));
        assertFalse(texts.subscribed.get(), "the publisher is not subscribed before the body is opened");

        exchange.pullBody(1);
        await(() -> exchange.bodyChunks.size() == 1);
        assertTrue(texts.subscribed.get());
        exchange.pullBody(16);
        await(exchange.bodyComplete::get);
        assertEquals(1, texts.maxOutstanding.get(), "events are requested one at a time");
        List<Message> frames = frames(exchange);
        assertEquals(List.of("message:{\"text\":\"a\"}", "message:{\"text\":\"b\"}", "message:{\"text\":\"c\"}"),
                     describe(frames));

        exchange.respond(new Reply(200, Map.of("x-count", "3", "Content-Type", "application/json"), "{}", null));
        assertEquals(3, (Integer) call("count", future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)));
    }

    @Test
    void thePublishersErrorFailsTheCall() throws Exception {
        EventSource texts = new EventSource("a");
        CompletableFuture<?> future = call("publish", streams(false), texts);
        Exchange exchange = http.next();
        exchange.pullBody(1);
        await(() -> exchange.bodyChunks.size() == 1);
        IllegalStateException boom = new IllegalStateException("publisher boom");
        texts.fail(boom);
        exchange.pullBody(4);
        // The transport sees the body fail; as a transport does, it fails the exchange with the body's failure.
        settle();
        assertFalse(exchange.bodyComplete.get(), "the stream must not end normally");
        exchange.fail(new java.io.IOException("request body failed", boom));
        Throwable failure = failure(future);
        assertTrue(chain(failure).contains(boom), "cause chain " + chain(failure));
    }

    @Test
    void cancellingTheCallCancelsASilentPublisherPromptly() throws Exception {
        EventSource texts = new EventSource("a");
        CompletableFuture<?> future = call("publish", streams(false), texts);
        Exchange exchange = http.next();
        exchange.pullBody(1);
        await(() -> exchange.bodyChunks.size() == 1);
        await(() -> texts.requested.get() >= 2);
        assertTrue(pumpThreads() > 0, "the pump waits for the silent publisher");

        long start = System.nanoTime();
        assertTrue(future.cancel(true));
        awaitWithin(Duration.ofSeconds(1), texts.cancelled::get);
        assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1));
        awaitWithin(Duration.ofSeconds(1), () -> pumpThreads() == 0);
        await(() -> exchange.reasons.contains(CancellationReason.CALLER_CANCELLED));
    }

    @Test
    void aCallFailingBeforeSendingNeverSubscribesThePublisherAndLeavesNoPump() throws Exception {
        int before = pumpThreads();
        EventSource texts = new EventSource("a");
        CompletableFuture<?> future = call("securePublish", streams(true), texts);
        Throwable failure = failure(future);
        assertTrue(chain(failure).stream().anyMatch(t -> "no credentials".equals(t.getMessage())),
                   "cause chain " + chain(failure));
        assertEquals(0, http.calls.get(), "nothing is sent");
        assertFalse(texts.subscribed.get(), "the publisher is never subscribed");
        assertTrue(pumpThreads() <= before, "no pump thread is left");
    }

    @Test
    void aBidirectionalCallStreamsBothWays() throws Exception {
        EventSource texts = new EventSource("hi");
        texts.completeWhenDrained = true;
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("chat", streams(false), texts, log, subscription);
        Exchange exchange = http.next();
        exchange.pullBody(16);
        await(exchange.bodyComplete::get);
        assertEquals(List.of("message:{\"text\":\"hi\"}"), describe(frames(exchange)));
        respond(exchange, Map.of(), new FramePublisher(http.eventLoop, message("yo")));
        await(() -> subscription.get() != null);
        subscription.get().request(5);
        assertEquals(null, future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(List.of("response", "event:OutEvents:yo", "onComplete", "complete"), log);
    }

    @Test
    void aSigV4InputStreamIsSignedAndItsFramesAreChainSignedEnvelopes() throws Exception {
        EventSource texts = new EventSource("a", "b");
        texts.completeWhenDrained = true;
        CompletableFuture<?> future = call("securePublish", secureStreams(), texts);
        Exchange exchange = http.next();
        String seed = assertSignedEventStreamRequest(exchange.request);
        exchange.pullBody(16);
        await(exchange.bodyComplete::get);
        List<Message> envelopes = frames(exchange);
        assertEquals(List.of("message:{\"text\":\"a\"}", "message:{\"text\":\"b\"}"),
                     describe(verifyChain(seed, "streams", envelopes)));
        exchange.respond(new Reply(200, Map.of("Content-Type", "application/json"), "{}", null));
        future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Test
    void aSigV4BidirectionalCallSignsItsInputFramesAndStreamsBothWays() throws Exception {
        EventSource texts = new EventSource("hi");
        texts.completeWhenDrained = true;
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("secureChat", secureStreams(), texts, log, subscription);
        Exchange exchange = http.next();
        String seed = assertSignedEventStreamRequest(exchange.request);
        exchange.pullBody(16);
        await(exchange.bodyComplete::get);
        assertEquals(List.of("message:{\"text\":\"hi\"}"), describe(verifyChain(seed, "streams", frames(exchange))));
        respond(exchange, Map.of(), new FramePublisher(http.eventLoop, message("yo")));
        await(() -> subscription.get() != null);
        subscription.get().request(5);
        assertEquals(null, future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(List.of("response", "event:OutEvents:yo", "onComplete", "complete"), log);
    }

    @Test
    void aSigV4InputStreamAtAFixedSigningClockHasExactHeadersFrameDatesAndDeterministicBytes() throws Exception {
        java.time.Instant at = java.time.Instant.parse("2024-05-01T12:34:56.789Z");
        java.time.Clock clock = java.time.Clock.fixed(at, java.time.ZoneOffset.UTC);
        List<byte[]> bodies = new ArrayList<>();
        for (int run = 0; run < 2; run++) {
            Object client = secureStreams();
            GeneratedDynamicWireEquivalencePropertyTest.fixSigningClock(client, clock);
            EventSource texts = new EventSource("a", "b");
            texts.completeWhenDrained = true;
            CompletableFuture<?> future = call("securePublish", client, texts);
            Exchange exchange = http.next();
            SdkHttpRequest request = exchange.request;
            assertEquals("STREAMING-AWS4-HMAC-SHA256-EVENTS",
                         request.headers().firstValue("x-amz-content-sha256").orElse(null));
            assertEquals("20240501T123456Z", request.headers().firstValue("X-Amz-Date").orElse(null));
            String authorization = request.headers().firstValue("Authorization").orElseThrow();
            String prefix = "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20240501/us-east-1/streams/aws4_request, "
                            + "SignedHeaders=";
            assertTrue(authorization.startsWith(prefix), authorization);
            String signedHeaders = authorization.substring(prefix.length(), authorization.indexOf(", Signature="));
            String seed = authorization.substring(authorization.indexOf("Signature=") + "Signature=".length());
            assertEquals(expectedRequestSignature(request, signedHeaders, "20240501T123456Z"), seed,
                         "the request signature, recomputed independently");
            assertEquals(prefix + signedHeaders + ", Signature=" + seed, authorization);

            exchange.pullBody(16);
            await(exchange.bodyComplete::get);
            List<Message> envelopes = frames(exchange);
            assertEquals(3, envelopes.size(), "two events and the closing frame");
            for (Message envelope : envelopes) {
                assertEquals(at, envelope.header(":date").asTimestamp(), "every frame is dated at the signing clock");
            }
            assertEquals(List.of("message:{\"text\":\"a\"}", "message:{\"text\":\"b\"}"),
                         describe(verifyChain(seed, "streams", envelopes)));
            assertEquals(0, envelopes.get(2).payloadBytes().length, "the empty closing frame");
            bodies.add(bodyBytes(exchange));
            exchange.respond(new Reply(200, Map.of("Content-Type", "application/json"), "{}", null));
            future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        assertEquals(java.util.HexFormat.of().formatHex(bodies.get(0)), java.util.HexFormat.of().formatHex(bodies.get(1)),
                     "two identical calls at a fixed signing clock send byte-identical bodies");
    }

    @Test
    void aSigV4OutputOnlyStreamSignsItsRequestWithTheBodyHash() throws Exception {
        List<String> log = new CopyOnWriteArrayList<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        CompletableFuture<?> future = call("secureSubscribe", secureStreams(), log, subscription);
        Exchange exchange = http.next();
        SdkHttpRequest request = exchange.request;
        byte[] body = switch (request.body().kind()) {
            case EMPTY -> new byte[0];
            case BYTES -> request.body().asByteArray();
            case STREAMING -> throw new AssertionError("an output-only stream has no streaming request body");
        };
        assertEquals(java.util.HexFormat.of().formatHex(sha256(body)),
                     request.headers().firstValue("x-amz-content-sha256").orElse(null),
                     "a normal payload hash, not the streaming events marker");
        String authorization = request.headers().firstValue("Authorization").orElseThrow();
        assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/"), authorization);
        respond(exchange, Map.of(), new FramePublisher(http.eventLoop, message("yo")));
        await(() -> subscription.get() != null);
        subscription.get().request(5);
        assertEquals(null, future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(List.of("response", "event:OutEvents:yo", "onComplete", "complete"), log);
    }

    @Test
    void aDynamicSigV4InputStreamIsChainSignedAndByteIdenticalToTheGeneratedCall() throws Exception {
        java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2024-05-01T12:34:56.789Z"),
                                                      java.time.ZoneOffset.UTC);
        Object generated = secureStreams();
        GeneratedDynamicWireEquivalencePropertyTest.fixSigningClock(generated, clock);
        EventSource texts = new EventSource("a", "b");
        texts.completeWhenDrained = true;
        CompletableFuture<?> generatedCall = call("securePublish", generated, texts);
        Exchange reference = http.next();
        reference.pullBody(16);
        await(reference.bodyComplete::get);
        reference.respond(new Reply(200, Map.of("Content-Type", "application/json"), "{}", null));
        generatedCall.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        software.amazon.awssdk.dynamic.client.DynamicAsyncClient dynamic =
            software.amazon.awssdk.dynamic.client.DynamicAsyncClient.builder()
                .model(software.amazon.awssdk.dynamic.model.DynamicModel.fromJsonAst(
                    ClientProjectionLifecyclePropertyTest.jsonAst(temp.resolve("model-streams/streams.smithy")),
                    "streams.json"))
                .region(software.amazon.awssdk.regions.Region.of("us-east-1"))
                .endpointOverride(java.net.URI.create("https://streams.test"))
                .credentialsProvider(new ClientProjectionLifecyclePropertyTest.StaticCredentials(
                    software.amazon.awssdk.identity.spi.AwsCredentialsIdentity.create("AKIDEXAMPLE", "SECRETEXAMPLE")))
                .configurationSources(software.amazon.awssdk.core.client.config.ConfigurationSources.none())
                .httpClient(http).build();
        clients.add(dynamic);
        GeneratedDynamicWireEquivalencePropertyTest.fixSigningClock(dynamic, clock);
        software.amazon.smithy.java.core.serde.event.EventStream<software.amazon.smithy.java.core.schema.SerializableStruct>
            writer = software.amazon.smithy.java.core.serde.event.EventStream.newWriter();
        CompletableFuture<?> dynamicCall = dynamic.callAsync(
            "SecurePublish", software.amazon.smithy.java.core.serde.document.Document.of(
                Map.of("events", software.amazon.smithy.java.core.serde.document.Document.ofObject(writer))));
        Exchange exchange = http.next();
        String seed = assertSignedEventStreamRequest(exchange.request);
        // The writer hands each event to the transport's demand, so a producer thread writes while the body is pulled.
        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
            for (String text : List.of("a", "b")) {
                writer.asWriter().write(dynamic.inputEvent("SecurePublish", software.amazon.smithy.java.core.serde
                    .document.Document.of(Map.of("message", software.amazon.smithy.java.core.serde.document.Document
                        .of(Map.of("text", software.amazon.smithy.java.core.serde.document.Document.of(text)))))));
            }
            writer.close();
        });
        exchange.pullBody(16);
        await(exchange.bodyComplete::get);
        producer.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertEquals(List.of("message:{\"text\":\"a\"}", "message:{\"text\":\"b\"}"),
                     describe(verifyChain(seed, "streams", frames(exchange))));
        assertEquals(reference.request.headers().firstValue("Authorization").orElse(null),
                     exchange.request.headers().firstValue("Authorization").orElse(null),
                     "the same request signature (chain seed)");
        assertEquals(java.util.HexFormat.of().formatHex(bodyBytes(reference)),
                     java.util.HexFormat.of().formatHex(bodyBytes(exchange)),
                     "the dynamic call's signed frames are byte-identical to the generated call's");
        exchange.respond(new Reply(200, Map.of("Content-Type", "application/json"), "{}", null));
        dynamicCall.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * The SigV4 signature of {@code request} recomputed from the SigV4 specification: the canonical request of the
     * transmitted method, path, query and {@code signedHeaders}, with the streaming events payload marker.
     */
    private static String expectedRequestSignature(SdkHttpRequest request, String signedHeaders, String dateTime) {
        StringBuilder canonicalHeaders = new StringBuilder();
        for (String name : signedHeaders.split(";")) {
            String value = request.headers().firstValue(name).orElseThrow(() -> new AssertionError("no " + name));
            canonicalHeaders.append(name).append(':').append(value.trim().replaceAll(" +", " ")).append('\n');
        }
        String target = request.rawTarget();
        int query = target.indexOf('?');
        String path = query < 0 ? target : target.substring(0, query);
        String canonicalQuery = query < 0 ? "" : target.substring(query + 1);
        String canonical = request.method() + "\n" + path + "\n" + canonicalQuery + "\n" + canonicalHeaders + "\n"
                           + signedHeaders + "\nSTREAMING-AWS4-HMAC-SHA256-EVENTS";
        String date = dateTime.substring(0, 8);
        String stringToSign = "AWS4-HMAC-SHA256\n" + dateTime + "\n" + date + "/us-east-1/streams/aws4_request\n"
                              + java.util.HexFormat.of().formatHex(sha256(canonical.getBytes(StandardCharsets.UTF_8)));
        return java.util.HexFormat.of().formatHex(hmac(sigV4Key("SECRETEXAMPLE", date, "us-east-1", "streams"),
                                                       stringToSign));
    }

    private static byte[] bodyBytes(Exchange exchange) {
        int size = exchange.bodyChunks.stream().mapToInt(ByteBuffer::remaining).sum();
        ByteBuffer all = ByteBuffer.allocate(size);
        exchange.bodyChunks.forEach(chunk -> all.put(chunk.duplicate()));
        return all.array();
    }

    private static byte[] sha256(byte[] data) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(data);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Asserts the SigV4 event stream request headers and returns the seed (request) signature. */
    private static String assertSignedEventStreamRequest(SdkHttpRequest request) {
        assertEquals("STREAMING-AWS4-HMAC-SHA256-EVENTS",
                     request.headers().firstValue("x-amz-content-sha256").orElse(null));
        assertEquals("application/vnd.amazon.eventstream", request.headers().firstValue("Content-Type").orElse(null));
        String authorization = request.headers().firstValue("Authorization").orElseThrow();
        assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/"), authorization);
        return authorization.substring(authorization.indexOf("Signature=") + "Signature=".length());
    }

    /**
     * Verifies that every envelope is signed in order from {@code seedHex} (the last is the empty closing frame), by
     * an independent SigV4 key derivation and the eventstream module's frame signature primitive, and returns the
     * inner messages.
     */
    private static List<Message> verifyChain(String seedHex, String service, List<Message> envelopes) {
        assertTrue(envelopes.size() >= 1, "at least the closing frame");
        byte[] prior = java.util.HexFormat.of().parseHex(seedHex);
        var algorithm = software.amazon.awssdk.eventstream.SigV4FrameSignatureAlgorithm.create(
            "us-east-1", service, date -> sigV4Key("SECRETEXAMPLE", date.format(
                java.time.format.DateTimeFormatter.BASIC_ISO_DATE), "us-east-1", service));
        List<Message> inner = new ArrayList<>();
        for (int i = 0; i < envelopes.size(); i++) {
            Message envelope = envelopes.get(i);
            assertEquals(List.of(":date", ":chunk-signature"), List.copyOf(envelope.headers().keySet()));
            java.time.Instant date = envelope.header(":date").asTimestamp();
            Map<String, HeaderValue> dateHeader = new LinkedHashMap<>();
            dateHeader.put(":date", envelope.header(":date"));
            byte[] expected = algorithm.sign(prior, date, MessageEncoder.encodeHeaders(dateHeader),
                                             ByteBuffer.wrap(envelope.payloadBytes()));
            byte[] actual = envelope.header(":chunk-signature").asByteArray();
            assertEquals(java.util.HexFormat.of().formatHex(expected), java.util.HexFormat.of().formatHex(actual),
                         "frame " + i + " chains from the prior signature");
            prior = actual;
            if (i < envelopes.size() - 1) {
                inner.add(new MessageDecoder().decode(ByteBuffer.wrap(envelope.payloadBytes())).get(0));
            } else {
                assertEquals(0, envelope.payloadBytes().length, "the stream ends with the empty signed frame");
            }
        }
        return inner;
    }

    static byte[] sigV4Key(String secret, String date, String region, String service) {
        byte[] key = hmac(("AWS4" + secret).getBytes(StandardCharsets.UTF_8), date);
        key = hmac(key, region);
        key = hmac(key, service);
        return hmac(key, "aws4_request");
    }

    static byte[] hmac(byte[] key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theRpcInitialRequestCarriesTheNonStreamMembers() throws Exception {
        EventSource data = new EventSource("x");
        data.completeWhenDrained = true;
        CompletableFuture<?> future = call("upload", watch(), data);
        Exchange exchange = http.next();
        exchange.pullBody(16);
        await(exchange.bodyComplete::get);
        assertEquals(List.of("initial-request:{\"name\":\"n\"}", "chunk:{\"data\":\"x\"}"),
                     describe(frames(exchange)));
        exchange.respond(Reply.json(200, "{\"count\":1}"));
        assertEquals(1, (Integer) call("count", future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Model types

    @Test
    void theEventStreamMemberSurvivesToBuilderAndIsNotPartOfTheValue() throws Exception {
        assertEquals("true,true,true,true", call("roundTrip"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Support

    private Object streams(boolean failingCredentials) throws Exception {
        AutoCloseable client = call("streams", http, failingCredentials);
        clients.add(client);
        return client;
    }

    private Object secureStreams() throws Exception {
        AutoCloseable client = call("secureStreams", http);
        clients.add(client);
        return client;
    }

    private Object watch() throws Exception {
        AutoCloseable client = call("watch", http);
        clients.add(client);
        return client;
    }

    @SuppressWarnings("unchecked")
    private static <T> T call(String name, Object... arguments) throws Exception {
        for (Method method : probe.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                try {
                    return (T) method.invoke(null, arguments);
                } catch (InvocationTargetException e) {
                    throw new AssertionError("EventStreamProbe." + name + " failed", e.getCause());
                }
            }
        }
        throw new AssertionError("no probe method " + name);
    }

    private static void generate(String name, String model, String service, String pkg, Path sources)
        throws Exception {
        Path models = Files.createDirectories(temp.resolve("model-" + name));
        Files.writeString(models.resolve(name + ".smithy"), model, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(service, pkg, models,
                                                                            temp.resolve("out-" + name)), List.of());
        try (var walk = Files.walk(result.sourceRoot())) {
            for (Path source : walk.toList()) {
                Path target = sources.resolve(result.sourceRoot().relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else if (!Files.exists(target)) {
                    Files.copy(source, target);
                }
            }
        }
    }

    /** Answers {@code exchange} with an event stream response whose body is {@code frames}. */
    private static void respond(Exchange exchange, Map<String, String> headers, FramePublisher frames)
        throws Exception {
        Map<String, String> all = new LinkedHashMap<>(headers);
        all.put("Content-Type", "application/vnd.amazon.eventstream");
        exchange.http.eventLoop.submit(() -> {
            frames.onTerminal = exchange.completion::complete;
            software.amazon.awssdk.http.SdkHttpResponse.Builder response =
                software.amazon.awssdk.http.SdkHttpResponse.builder().statusCode(200);
            all.forEach(response::addHeader);
            exchange.handler.onHeaders(response.body(software.amazon.awssdk.http.HttpBody.empty()).build());
            exchange.handler.onStream(frames);
        }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static List<String> events(List<String> log) {
        return log.stream().filter(l -> l.startsWith("event:")).toList();
    }

    private static List<Message> frames(Exchange exchange) {
        int size = exchange.bodyChunks.stream().mapToInt(ByteBuffer::remaining).sum();
        ByteBuffer all = ByteBuffer.allocate(size);
        exchange.bodyChunks.forEach(chunk -> all.put(chunk.duplicate()));
        all.flip();
        return new MessageDecoder().decode(all);
    }

    /** {@code event-type:payload} of each event frame; any other frame fails the test. */
    private static List<String> describe(List<Message> frames) {
        List<String> described = new ArrayList<>();
        for (Message frame : frames) {
            assertEquals("event", frame.header(":message-type").asString(), frame.toString());
            described.add(frame.header(":event-type").asString() + ":"
                          + new String(frame.payloadBytes(), StandardCharsets.UTF_8));
        }
        return described;
    }

    private static ByteBuffer message(String text) {
        return event("message", "{\"text\":\"" + text + "\"}");
    }

    private static ByteBuffer event(String type, String json) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("event"));
        headers.put(":event-type", HeaderValue.ofString(type));
        headers.put(":content-type", HeaderValue.ofString("application/json"));
        return MessageEncoder.encode(new Message(headers, json.getBytes(StandardCharsets.UTF_8)));
    }

    private static ByteBuffer exception(String type, String json) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("exception"));
        headers.put(":exception-type", HeaderValue.ofString(type));
        headers.put(":content-type", HeaderValue.ofString("application/json"));
        return MessageEncoder.encode(new Message(headers, json.getBytes(StandardCharsets.UTF_8)));
    }

    private static ByteBuffer error(String code, String message) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("error"));
        headers.put(":error-code", HeaderValue.ofString(code));
        headers.put(":error-message", HeaderValue.ofString(message));
        return MessageEncoder.encode(new Message(headers, new byte[0]));
    }

    /** The live threads named as the input event pump, virtual threads included (a JSON thread dump). */
    private static int pumpThreads() {
        Path dump = temp.resolve("threads-" + System.nanoTime() + ".json");
        String text;
        try {
            ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                             .dumpThreads(dump.toString(), HotSpotDiagnosticMXBean.ThreadDumpFormat.JSON);
            text = Files.readString(dump);
            Files.delete(dump);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        int count = 0;
        for (int i = text.indexOf("\"" + PUMP_THREAD + "\""); i >= 0;
             i = text.indexOf("\"" + PUMP_THREAD + "\"", i + 1)) {
            count++;
        }
        return count;
    }

    private static Throwable failure(CompletableFuture<?> future) {
        ExecutionException e = assertThrows(ExecutionException.class,
                                            () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertNotNull(e.getCause());
        return e.getCause();
    }

    private static List<Throwable> chain(Throwable failure) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = failure; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
            for (Throwable suppressed : t.getSuppressed()) {
                chain.add(suppressed);
            }
        }
        return chain;
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        awaitWithin(TIMEOUT, condition);
    }

    private static void awaitWithin(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not reached within " + timeout);
            }
            Thread.sleep(1);
        }
    }

    private void settle() throws Exception {
        for (int i = 0; i < 3; i++) {
            http.eventLoop.submit(() -> { }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Thread.sleep(20);
        }
    }

    /** A response body emitting one encoded frame per unit of demand, on the event loop. */
    static final class FramePublisher implements Publisher<ByteBuffer> {
        final ExecutorService eventLoop;
        final List<ByteBuffer> frames;
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();
        volatile Runnable onTerminal = () -> { };

        FramePublisher(ExecutorService eventLoop, ByteBuffer... frames) {
            this.eventLoop = eventLoop;
            this.frames = List.of(frames);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    eventLoop.execute(() -> {
                        for (long i = 0; i < n && !done && !cancelled.get() && delivered.get() < frames.size(); i++) {
                            subscriber.onNext(frames.get(delivered.getAndIncrement()).duplicate());
                        }
                        if (!done && !cancelled.get() && delivered.get() == frames.size()) {
                            done = true;
                            subscriber.onComplete();
                            onTerminal.run();
                        }
                    });
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
    }

    /**
     * The caller's input events: emits its texts under demand on the requesting thread, then stays silent (or
     * completes with {@link #completeWhenDrained}); records subscription, demand and cancellation.
     */
    static final class EventSource implements Publisher<String> {
        final List<String> texts;
        final AtomicBoolean subscribed = new AtomicBoolean();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicLong requested = new AtomicLong();
        final AtomicLong maxOutstanding = new AtomicLong();
        volatile boolean completeWhenDrained;
        private volatile Subscriber<? super String> subscriber;

        EventSource(String... texts) {
            this.texts = List.of(texts);
        }

        void fail(Throwable failure) {
            subscriber.onError(failure);
        }

        @Override
        public void subscribe(Subscriber<? super String> s) {
            subscribed.set(true);
            subscriber = s;
            AtomicLong outstanding = new AtomicLong();
            s.onSubscribe(new Subscription() {
                private int next;
                private boolean done;

                @Override
                public synchronized void request(long n) {
                    requested.addAndGet(n);
                    maxOutstanding.accumulateAndGet(outstanding.addAndGet(n), Math::max);
                    while (!done && !cancelled.get() && outstanding.get() > 0 && next < texts.size()) {
                        outstanding.decrementAndGet();
                        s.onNext(texts.get(next++));
                    }
                    if (!done && completeWhenDrained && next == texts.size()) {
                        done = true;
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
    }
}
