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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 8.2 and 8.4-8.13 through the generated async client of a synthetic restJson1 service with a plain,
 * a streaming-input, a streaming-output, a paginated and a waitable operation. Each call runs over a controllable
 * transport whose exchanges are held until the test answers them on a fake event-loop thread, so the tests observe
 * the returned futures before terminal completion, the exact demand on request and response bodies and paginator
 * pages, the threads of continuations, interceptor hooks and transformer callbacks, causal failures and
 * cancellation of exchanges, body subscriptions, retry delays, paginator calls and waiter polls and delays.
 * Engine-level races and stage ordering are covered by the {@code AwsCallEngine*Test}s of {@code aws-pipeline}.
 */
class GeneratedAsyncBehaviorTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.items";
    private static final String EVENT_LOOP = "fake-event-loop";
    private static final String RECORDING = "recording-completion";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final String MODEL = """
        $version: "2"
        namespace example.items

        use smithy.waiters#waitable

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "items")
        @aws.api#service(sdkId: "Items", endpointPrefix: "items")
        service Items {
            version: "2024-01-01"
            operations: [GetItem, PutBlob, GetBlob, ListItems]
        }

        @auth([])
        @readonly
        @http(method: "GET", uri: "/items/{id}")
        @waitable(
            ItemReady: {
                minDelay: 2
                acceptors: [
                    {
                        state: "success"
                        matcher: { output: { path: "value", expected: "READY", comparator: "stringEquals" } }
                    }
                    {
                        state: "retry"
                        matcher: { errorType: "ItemMissing" }
                    }
                ]
            }
        )
        operation GetItem {
            input := {
                @required
                @httpLabel
                id: String
            }
            output := {
                value: String
            }
            errors: [ItemMissing]
        }

        @error("client")
        @httpError(404)
        structure ItemMissing {
            message: String
        }

        @auth([])
        @http(method: "PUT", uri: "/blobs")
        operation PutBlob {
            input := {
                @httpHeader("X-Name")
                name: String

                @httpPayload
                @required
                body: StreamingBlob
            }
            output := {}
        }

        @auth([])
        @readonly
        @http(method: "GET", uri: "/blobs/{name}")
        operation GetBlob {
            input := {
                @required
                @httpLabel
                name: String
            }
            output := {
                @httpHeader("X-Kind")
                kind: String

                @httpPayload
                @required
                body: StreamingBlob
            }
        }

        @streaming
        blob StreamingBlob

        @auth([])
        @readonly
        @http(method: "GET", uri: "/items")
        @paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items")
        operation ListItems {
            input := {
                @httpQuery("next")
                nextToken: String
            }
            output := {
                nextToken: String
                items: ItemList
            }
        }

        list ItemList {
            member: String
        }
        """;

    /** Thin adapters over the generated public API; the test drives the returned futures and publishers. */
    private static final String PROBE = """
        package example.probe;

        import java.lang.reflect.Field;
        import java.net.URI;
        import java.nio.ByteBuffer;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Executor;
        import org.reactivestreams.Publisher;
        import software.amazon.awssdk.core.async.AsyncRequestBody;
        import software.amazon.awssdk.core.async.AsyncResponseTransformer;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.pipeline.ExecutionInterceptor;
        import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.retries.RetryScheduler;
        import software.amazon.awssdk.retries.RetryStrategy;
        import PKG.ItemsAsyncClient;
        import PKG.ItemsAsyncClientBuilder;
        import PKG.model.GetBlobRequest;
        import PKG.model.GetBlobResponse;
        import PKG.model.GetItemRequest;
        import PKG.model.GetItemResponse;
        import PKG.model.ListItemsRequest;
        import PKG.model.PutBlobRequest;
        import PKG.waiters.ItemsAsyncWaiter;

        public final class AsyncProbe {
            private AsyncProbe() {
            }

            public static AutoCloseable client(SdkAsyncHttpClient http, Executor executor, RetryStrategy strategy,
                                               RetryScheduler scheduler, ExecutionInterceptor interceptor)
                throws Exception {
                ItemsAsyncClientBuilder builder = ItemsAsyncClient.builder().region(Region.of("us-east-1"))
                    .endpointOverride(URI.create("https://items.test")).httpClient(http)
                    .addExecutionInterceptor(interceptor);
                if (executor != null) {
                    builder.asyncCompletionExecutor(executor);
                }
                if (strategy != null) {
                    builder.retryStrategy(strategy);
                }
                if (scheduler != null) {
                    // The public builder has no retry-scheduler setter; its default forwards to the build state.
                    Field state = builder.getClass().getDeclaredField("state");
                    state.setAccessible(true);
                    ((AwsClientBuildState) state.get(builder)).retryScheduler(scheduler);
                }
                return builder.build();
            }

            public static CompletableFuture<?> getItem(Object client, String id) {
                return ((ItemsAsyncClient) client).getItem(GetItemRequest.builder().id(id).build());
            }

            public static String value(Object response) {
                return ((GetItemResponse) response).value();
            }

            public static CompletableFuture<?> putBlob(Object client, Publisher<ByteBuffer> body) {
                return ((ItemsAsyncClient) client).putBlob(PutBlobRequest.builder().name("n").build(),
                                                           AsyncRequestBody.fromPublisher(body));
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            public static CompletableFuture<?> getBlob(Object client, AsyncResponseTransformer<?, ?> transformer) {
                return ((ItemsAsyncClient) client).getBlob(GetBlobRequest.builder().name("b").build(),
                                                           (AsyncResponseTransformer<GetBlobResponse, Object>)
                                                               (AsyncResponseTransformer) transformer);
            }

            public static Publisher<?> listItems(Object client) {
                return ((ItemsAsyncClient) client).listItemsPaginator(ListItemsRequest.builder().build());
            }

            public static CompletableFuture<?> waitUntilItemReady(Object client, RetryScheduler scheduler, String id) {
                ItemsAsyncWaiter waiter = ItemsAsyncWaiter.builder().client((ItemsAsyncClient) client)
                                                          .retryScheduler(scheduler).build();
                return waiter.waitUntilItemReady(r -> r.id(id));
            }
        }
        """.replace("PKG", PACKAGE);

    @TempDir
    static Path temp;

    private static URLClassLoader loader;
    private static Class<?> probe;

    private final ControlledHttp http = new ControlledHttp();
    private final RecordingExecutor recording = new RecordingExecutor();
    private final ThreadRecordingInterceptor hooks = new ThreadRecordingInterceptor();
    private final List<AutoCloseable> clients = new ArrayList<>();

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("items.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("example.items#Items", PACKAGE, models,
                                                                            temp.resolve("out")), List.of());
        Path sources = temp.resolve("sources");
        copyTree(result.sourceRoot(), sources);
        Path file = Files.createDirectories(sources.resolve("example/probe")).resolve("AsyncProbe.java");
        Files.writeString(file, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
        probe = Class.forName("example.probe.AsyncProbe", true, loader);
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @AfterEach
    void closeClients() throws Exception {
        for (AutoCloseable client : clients) {
            client.close();
        }
        http.eventLoop.shutdownNow();
        recording.delegate.shutdownNow();
    }

    // -------------------------------------------------------------------------------------------------------------
    // 8.2, 8.8, 8.12: returned before completion; continuations on the configured executor, never the event loop

    @Test
    void aPlainCallReturnsIncompleteAndContinuesOnTheConfiguredExecutorNeverOnTheEventLoop() throws Exception {
        Object client = client(recording, null, null);
        CompletableFuture<?> future = call("getItem", client, "a");
        assertFalse(future.isDone(), "returned before the transport was answered");
        Exchange exchange = http.next();
        AtomicReference<Thread> completedOn = new AtomicReference<>();
        future.whenComplete((r, e) -> completedOn.set(Thread.currentThread()));
        await(() -> hooks.contains("beforeTransmission"));
        assertFalse(future.isDone(), "still incomplete while the exchange is held");

        exchange.respond(Reply.json(200, "{\"value\":\"v\"}"));
        assertEquals("v", call("value", join(future)));
        await(() -> completedOn.get() != null);

        assertTrue(completedOn.get().getName().startsWith(RECORDING), "completed on " + completedOn.get());
        assertTrue(recording.tasks.get() > 0);
        hooks.assertNoneOn(EVENT_LOOP);
        for (String hook : List.of("afterTransmission", "beforeDeserialization", "afterAttempt", "afterExecution")) {
            assertTrue(hooks.threadOf(hook).getName().startsWith(RECORDING), hook + " ran on " + hooks.threadOf(hook));
        }
    }

    @Test
    void withoutAConfiguredExecutorNoSdkWorkRunsOnTheCommonPoolOrTheEventLoop() throws Exception {
        // SDK-owned completion executor and DefaultRetryScheduler: one retried call, a stream and a paginator.
        AtomicInteger getItems = new AtomicInteger();
        http.auto = request -> {
            if (request.encodedPath().startsWith("/items/")) {
                return getItems.incrementAndGet() == 1 ? Reply.json(500, "{\"message\":\"busy\"}")
                                                       : Reply.json(200, "{\"value\":\"v\"}");
            }
            if (request.encodedPath().startsWith("/blobs/")) {
                return Reply.stream(new LoopPublisher(http.eventLoop, "x", "y"));
            }
            return Reply.json(200, "{\"items\":[\"i\"]}");
        };
        Object client = client(null, null, null);
        List<Thread> threads = new CopyOnWriteArrayList<>();

        CompletableFuture<?> item = call("getItem", client, "a");
        item.whenComplete((r, e) -> threads.add(Thread.currentThread()));
        assertEquals("v", call("value", join(item)));
        assertEquals(2, getItems.get(), "retried once through the SDK-owned retry scheduler");

        DemandTransformer transformer = new DemandTransformer();
        transformer.autoRequest = true;
        CompletableFuture<?> blob = call("getBlob", client, transformer);
        blob.whenComplete((r, e) -> threads.add(Thread.currentThread()));
        assertEquals("xy", join(blob));

        PageSubscriber pages = new PageSubscriber();
        ((Publisher<?>) call("listItems", client)).subscribe(pages);
        pages.subscription().request(1);
        await(() -> pages.terminal.get() != null);
        threads.addAll(pages.threads);

        threads.addAll(hooks.threads());
        threads.addAll(transformer.callbackThreads);
        assertFalse(threads.isEmpty());
        for (Thread thread : threads) {
            assertFalse(thread instanceof ForkJoinWorkerThread worker && worker.getPool() == ForkJoinPool.commonPool(),
                        "SDK work ran on the common pool: " + thread);
        }
        hooks.assertNoneOn(EVENT_LOOP);
        assertTrue(transformer.callbackThreads.stream().noneMatch(t -> t.getName().equals(EVENT_LOOP)));
        assertTrue(pages.threads.stream().noneMatch(t -> t.getName().equals(EVENT_LOOP)));
    }

    // -------------------------------------------------------------------------------------------------------------
    // 8.4, 8.5, 8.6: exact demand

    @Test
    void theStreamingRequestBodyIsPulledOnlyAsTheTransportRequests() throws Exception {
        Object client = client(recording, null, null);
        SourcePublisher body = new SourcePublisher("a", "b", "c");
        CompletableFuture<?> future = call("putBlob", client, body);
        Exchange exchange = http.next();
        settle();
        assertEquals(0, body.delivered.get(), "nothing is read before the transport demands");

        exchange.pullBody(1);
        assertEquals(1, body.delivered.get());
        assertEquals(1, exchange.bodyChunks.size());
        exchange.pullBody(1);
        assertEquals(2, body.delivered.get());
        assertEquals(2, exchange.bodyChunks.size());
        assertFalse(exchange.bodyComplete.get());
        exchange.pullBody(5);
        await(exchange.bodyComplete::get);
        assertEquals("abc", exchange.bodyText());
        assertTrue(body.requested.get() <= exchange.pulled.get(), "the source saw no more demand than the transport");
        assertFalse(future.isDone(), "incomplete until the transport answers");

        exchange.respond(Reply.json(200, "{}"));
        assertNotNull(join(future));
        hooks.assertNoneOn(EVENT_LOOP);
    }

    @Test
    void theStreamingResponseIsDeliveredOnlyAsTheTransformerRequests() throws Exception {
        Object client = client(recording, null, null);
        DemandTransformer transformer = new DemandTransformer();
        CompletableFuture<?> future = call("getBlob", client, transformer);
        Exchange exchange = http.next();
        LoopPublisher payload = new LoopPublisher(http.eventLoop, "x", "y", "z");
        exchange.respond(Reply.stream(payload));
        await(() -> transformer.subscription != null);
        settle();
        assertEquals(0, payload.delivered.get(), "nothing is delivered before the transformer demands");

        transformer.subscription.request(1);
        await(() -> transformer.received.size() == 1);
        settle();
        assertEquals(1, payload.delivered.get());
        assertEquals(1, transformer.received.size());
        assertFalse(future.isDone());

        transformer.subscription.request(2);
        await(() -> transformer.received.size() == 3);
        settle();
        assertEquals(3, payload.delivered.get());
        transformer.subscription.request(1);
        assertEquals("xyz", join(future));
        assertTrue(transformer.callbackThreads.stream().allMatch(t -> t.getName().startsWith(RECORDING)),
                   "onResponse and onStream ran on " + transformer.callbackThreads);
        hooks.assertNoneOn(EVENT_LOOP);
    }

    @Test
    void thePaginatorPublisherCallsOncePerRequestedPage() throws Exception {
        http.auto = request -> Reply.json(200, "{\"nextToken\":\"t" + http.calls.get() + "\",\"items\":[\"i\"]}");
        Object client = client(recording, null, null);
        PageSubscriber pages = new PageSubscriber();
        ((Publisher<?>) call("listItems", client)).subscribe(pages);
        Subscription subscription = pages.subscription();
        settle();
        assertEquals(0, http.calls.get(), "no call before demand");

        subscription.request(1);
        await(() -> pages.pages.size() == 1);
        settle();
        assertEquals(1, http.calls.get());
        subscription.request(1);
        await(() -> pages.pages.size() == 2);
        settle();
        assertEquals(2, http.calls.get(), "no page is fetched after demand reaches zero");
        assertTrue(pages.threads.stream().allMatch(t -> t.getName().startsWith(RECORDING)),
                   "pages on " + pages.threads);
        subscription.cancel();
    }

    // -------------------------------------------------------------------------------------------------------------
    // 8.11: causal failures

    @Test
    void aTransportFailureIsReachableInTheCauseChain() throws Exception {
        Object client = client(recording, StandardRetryStrategy.doNotRetry(), null);
        CompletableFuture<?> future = call("getItem", client, "a");
        IOException reset = new IOException("connection reset");
        http.next().fail(reset);
        Throwable failure = failure(future);
        assertTrue(chain(failure).stream().anyMatch(t -> t == reset), "cause chain " + chain(failure));
    }

    @Test
    void aModeledServiceErrorCompletesTheFutureWithTheModeledException() throws Exception {
        Object client = client(recording, StandardRetryStrategy.doNotRetry(), null);
        CompletableFuture<?> future = call("getItem", client, "a");
        http.next().respond(new Reply(404, Map.of("X-Amzn-ErrorType", "ItemMissing"),
                                      "{\"message\":\"gone\"}", null));
        Throwable failure = failure(future);
        Throwable modeled = chain(failure).stream()
                                          .filter(t -> t.getClass().getSimpleName().startsWith("ItemMissing"))
                                          .findFirst().orElseThrow(() -> new AssertionError("chain " + chain(failure)));
        assertTrue(modeled instanceof AwsServiceException);
        assertTrue(modeled.getMessage().contains("gone"), modeled.getMessage());
    }

    @Test
    void aResponseTransformerExceptionIsReachableInTheCauseChain() throws Exception {
        Object client = client(recording, StandardRetryStrategy.doNotRetry(), null);
        IllegalStateException boom = new IllegalStateException("boom");
        DemandTransformer transformer = new DemandTransformer();
        transformer.onResponseFailure = boom;
        CompletableFuture<?> future = call("getBlob", client, transformer);
        http.next().respond(Reply.stream(new LoopPublisher(http.eventLoop, "x")));
        Throwable failure = failure(future);
        assertTrue(chain(failure).stream().anyMatch(t -> t == boom), "cause chain " + chain(failure));
    }

    // -------------------------------------------------------------------------------------------------------------
    // 8.9, 8.10: cancellation

    @Test
    void cancellingDuringTransmissionCancelsTheExchangeAndRunsNothingAfterwards() throws Exception {
        Object client = client(recording, null, null);
        CompletableFuture<?> future = call("getItem", client, "a");
        Exchange exchange = http.next();
        await(() -> hooks.contains("beforeTransmission"));
        settle();
        int hooksBefore = hooks.size();

        assertTrue(future.cancel(true));
        await(() -> exchange.cancels.get() == 1);
        assertEquals(List.of(CancellationReason.CALLER_CANCELLED), exchange.reasons);
        exchange.respond(Reply.json(200, "{\"value\":\"late\"}"));
        settle();
        assertTrue(future.isCancelled());
        assertEquals(1, exchange.cancels.get());
        assertEquals(1, http.calls.get(), "no later attempt");
        assertEquals(hooksBefore, hooks.size(), "hooks after cancellation: " + hooks.names());
    }

    @Test
    void cancellingDuringTheRequestBodyCancelsTheBodySubscriptionAndTheExchange() throws Exception {
        Object client = client(recording, null, null);
        SourcePublisher body = new SourcePublisher("a", "b", "c");
        CompletableFuture<?> future = call("putBlob", client, body);
        Exchange exchange = http.next();
        exchange.pullBody(1);
        assertEquals(1, body.delivered.get());

        assertTrue(future.cancel(true));
        await(() -> exchange.cancels.get() == 1);
        assertEquals(List.of(CancellationReason.CALLER_CANCELLED), exchange.reasons);
        await(body.cancelled::get);
        exchange.pullBody(5);
        settle();
        assertEquals(1, body.delivered.get(), "no body consumption after cancellation");
        assertEquals(1, http.calls.get());
    }

    @Test
    void cancellingDuringTheResponseStreamCancelsTheStreamSubscriptionAndTheExchange() throws Exception {
        Object client = client(recording, null, null);
        DemandTransformer transformer = new DemandTransformer();
        CompletableFuture<?> future = call("getBlob", client, transformer);
        Exchange exchange = http.next();
        LoopPublisher payload = new LoopPublisher(http.eventLoop, "x", "y", "z");
        exchange.respond(Reply.stream(payload));
        await(() -> transformer.subscription != null);
        transformer.subscription.request(1);
        await(() -> transformer.received.size() == 1);

        assertTrue(future.cancel(true));
        await(payload.cancelled::get);
        await(() -> exchange.cancels.get() == 1);
        transformer.subscription.request(5);
        settle();
        assertEquals(1, payload.delivered.get(), "no body delivery after cancellation");
        assertEquals(1, transformer.received.size());
        assertEquals(1, http.calls.get());
    }

    @Test
    void cancellingDuringARetryDelayCancelsTheDelayAndStartsNoLaterAttempt() throws Exception {
        HeldScheduler scheduler = new HeldScheduler();
        Object client = client(recording, null, scheduler);
        CompletableFuture<?> future = call("getItem", client, "a");
        http.next().respond(Reply.json(500, "{\"message\":\"busy\"}"));
        CompletableFuture<Void> delay = scheduler.next();
        assertFalse(future.isDone());

        assertTrue(future.cancel(true));
        // The engine cancels the pending delay from its cancellation handler, which may run asynchronously.
        await(delay::isCancelled);
        delay.complete(null);
        settle();
        assertEquals(1, http.calls.get(), "no later attempt");
        assertTrue(http.exchanges.isEmpty());
    }

    @Test
    void cancellingThePaginatorSubscriptionCancelsItsInFlightCall() throws Exception {
        Object client = client(recording, null, null);
        PageSubscriber pages = new PageSubscriber();
        ((Publisher<?>) call("listItems", client)).subscribe(pages);
        pages.subscription().request(1);
        Exchange exchange = http.next();

        pages.subscription().cancel();
        await(() -> exchange.cancels.get() == 1);
        exchange.respond(Reply.json(200, "{\"nextToken\":\"t\",\"items\":[\"i\"]}"));
        pages.subscription().request(1);
        settle();
        assertTrue(pages.pages.isEmpty(), "no page after cancellation");
        assertEquals(1, http.calls.get());
    }

    @Test
    void cancellingAnAsyncWaiterCancelsItsPendingPollOrDelay() throws Exception {
        HeldScheduler scheduler = new HeldScheduler();
        Object client = client(recording, null, null);

        CompletableFuture<?> polling = call("waitUntilItemReady", client, scheduler, "a");
        assertFalse(polling.isDone(), "the waiter returns before polling completes");
        Exchange poll = http.next();
        assertTrue(polling.cancel(true));
        await(() -> poll.cancels.get() == 1);
        poll.respond(Reply.json(200, "{\"value\":\"READY\"}"));
        settle();
        assertEquals(1, http.calls.get());
        assertTrue(scheduler.delays.isEmpty(), "no delay after a cancelled poll");

        CompletableFuture<?> delaying = call("waitUntilItemReady", client, scheduler, "b");
        http.next().respond(new Reply(404, Map.of("X-Amzn-ErrorType", "ItemMissing"), "{}", null));
        CompletableFuture<Void> delay = scheduler.next();
        assertFalse(delaying.isDone());
        assertTrue(delaying.cancel(true));
        assertTrue(delay.isCancelled(), "the pending waiter delay is cancelled");
        delay.complete(null);
        settle();
        assertEquals(2, http.calls.get(), "no poll after cancellation");
    }

    // -------------------------------------------------------------------------------------------------------------
    // Support

    private Object client(Executor executor, RetryStrategy strategy, RetryScheduler scheduler) throws Exception {
        Method method = probe.getMethod("client", SdkAsyncHttpClient.class, Executor.class, RetryStrategy.class,
                                        RetryScheduler.class, ExecutionInterceptor.class);
        AutoCloseable client = (AutoCloseable) invoke(method, http, executor, strategy, scheduler, hooks);
        clients.add(client);
        return client;
    }

    @SuppressWarnings("unchecked")
    private static <T> T call(String name, Object... arguments) throws Exception {
        for (Method method : probe.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                return (T) invoke(method, arguments);
            }
        }
        throw new AssertionError("no probe method " + name);
    }

    private static Object invoke(Method method, Object... arguments) throws Exception {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            throw new AssertionError("AsyncProbe." + method.getName() + " failed", e.getCause());
        }
    }

    private static Object join(CompletableFuture<?> future) throws Exception {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new AssertionError("the call failed", e.getCause());
        }
    }

    private static Throwable failure(CompletableFuture<?> future) {
        ExecutionException e = assertThrows(ExecutionException.class,
                                            () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertTrue(future.isCompletedExceptionally());
        assertFalse(future.isCancelled());
        return e.getCause() instanceof CompletionException c && c.getCause() != null ? c.getCause() : e.getCause();
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
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not reached within " + TIMEOUT);
            }
            Thread.sleep(1);
        }
    }

    /** Drains the event loop and lets the executors run anything still queued. */
    private void settle() throws Exception {
        for (int i = 0; i < 3; i++) {
            http.eventLoop.submit(() -> { }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Thread.sleep(20);
        }
    }

    private static void copyTree(Path from, Path to) throws Exception {
        try (var walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    private static ByteBuffer utf8(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String text(List<ByteBuffer> chunks) {
        StringBuilder text = new StringBuilder();
        for (ByteBuffer chunk : chunks) {
            text.append(StandardCharsets.UTF_8.decode(chunk.duplicate()));
        }
        return text.toString();
    }

    /** A canned response: a buffered JSON body, or a stream that completes the exchange when it terminates. */
    record Reply(int status, Map<String, String> headers, String json, LoopPublisher stream) {
        static Reply json(int status, String json) {
            return new Reply(status, Map.of("Content-Type", "application/json"), json, null);
        }

        static Reply stream(LoopPublisher stream) {
            return new Reply(200, Map.of("X-Kind", "k", "Content-Type", "application/octet-stream"), null, stream);
        }
    }

    /**
     * An async transport whose exchanges are held until the test answers them; every response callback runs on one
     * fake event-loop thread. With {@link #auto}, each exchange is answered as soon as it starts.
     */
    static final class ControlledHttp implements SdkAsyncHttpClient {
        final ExecutorService eventLoop = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, EVENT_LOOP);
            thread.setDaemon(true);
            return thread;
        });
        final BlockingQueue<Exchange> exchanges = new LinkedBlockingQueue<>();
        final AtomicInteger calls = new AtomicInteger();
        volatile Function<SdkHttpRequest, Reply> auto;

        Exchange next() throws InterruptedException {
            Exchange exchange = exchanges.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull(exchange, "no exchange started");
            return exchange;
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            calls.incrementAndGet();
            Exchange exchange = new Exchange(this, request, handler);
            Function<SdkHttpRequest, Reply> responder = auto;
            if (responder != null) {
                eventLoop.execute(() -> exchange.answer(responder.apply(request)));
            } else {
                exchanges.add(exchange);
            }
            return exchange.completion;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "ControlledHttp";
        }

        @Override
        public void close() {
        }
    }

    /** One held exchange: the test pulls its request body and answers or fails it on the event loop. */
    static final class Exchange {
        final ControlledHttp http;
        final SdkHttpRequest request;
        final AsyncHttpResponseHandler handler;
        final AsyncExchangeCompletion completion;
        final AtomicInteger cancels = new AtomicInteger();
        final List<CancellationReason> reasons = new CopyOnWriteArrayList<>();
        final List<ByteBuffer> bodyChunks = new CopyOnWriteArrayList<>();
        final AtomicBoolean bodyComplete = new AtomicBoolean();
        final AtomicLong pulled = new AtomicLong();
        private Subscription body;

        Exchange(ControlledHttp http, SdkHttpRequest request, AsyncHttpResponseHandler handler) {
            this.http = http;
            this.request = request;
            this.handler = handler;
            // As the SPI requires of a transport, a cancelled exchange reports its failure to the handler and
            // cancels the transport's request-body subscription, which the SDK's body adapter forwards upstream.
            this.completion = new AsyncExchangeCompletion("ControlledHttp", (reason, failure) -> {
                cancels.incrementAndGet();
                reasons.add(reason);
                http.eventLoop.execute(() -> {
                    if (body != null) {
                        body.cancel();
                    }
                });
                handler.onError(failure);
            });
        }

        void pullBody(long n) throws Exception {
            onLoop(() -> {
                if (body == null) {
                    assertEquals(HttpBody.Kind.STREAMING, request.body().kind());
                    request.body().streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            body = s;
                        }

                        @Override
                        public void onNext(ByteBuffer chunk) {
                            bodyChunks.add(chunk);
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onComplete() {
                            bodyComplete.set(true);
                        }
                    });
                }
                pulled.addAndGet(n);
                body.request(n);
            });
        }

        String bodyText() {
            return text(bodyChunks);
        }

        void respond(Reply reply) throws Exception {
            onLoop(() -> answer(reply));
        }

        void fail(Throwable failure) throws Exception {
            onLoop(() -> {
                handler.onError(failure);
                completion.fail(failure);
            });
        }

        /** Runs on the event loop. */
        void answer(Reply reply) {
            if (completion.isDone()) {
                return;
            }
            SdkHttpResponse.Builder headers = SdkHttpResponse.builder().statusCode(reply.status());
            reply.headers().forEach(headers::addHeader);
            handler.onHeaders(headers.body(HttpBody.empty()).build());
            if (reply.stream() != null) {
                reply.stream().onTerminal = completion::complete;
                handler.onStream(reply.stream());
            } else {
                handler.onStream(new LoopPublisher(http.eventLoop, reply.json()));
                completion.complete();
            }
        }

        private void onLoop(Runnable task) throws Exception {
            http.eventLoop.submit(task).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** A request-body source emitting one chunk per unit of demand on the requesting thread. */
    static final class SourcePublisher implements Publisher<ByteBuffer> {
        final List<String> chunks;
        final AtomicLong requested = new AtomicLong();
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();

        SourcePublisher(String... chunks) {
            this.chunks = List.of(chunks);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    requested.addAndGet(n);
                    for (long i = 0; i < n && !done && !cancelled.get(); i++) {
                        int index = delivered.get();
                        if (index == chunks.size()) {
                            done = true;
                            subscriber.onComplete();
                            return;
                        }
                        delivered.incrementAndGet();
                        subscriber.onNext(utf8(chunks.get(index)));
                    }
                    if (!done && !cancelled.get() && delivered.get() == chunks.size()) {
                        done = true;
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
    }

    /** A response stream emitting one chunk per unit of demand, always on the event loop. */
    static final class LoopPublisher implements Publisher<ByteBuffer> {
        final ExecutorService eventLoop;
        final List<String> chunks;
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();
        volatile Runnable onTerminal = () -> { };

        LoopPublisher(ExecutorService eventLoop, String... chunks) {
            this.eventLoop = eventLoop;
            this.chunks = List.of(chunks);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    eventLoop.execute(() -> {
                        for (long i = 0; i < n && !done && !cancelled.get(); i++) {
                            int index = delivered.getAndIncrement();
                            subscriber.onNext(utf8(chunks.get(index)));
                            if (delivered.get() == chunks.size()) {
                                break;
                            }
                        }
                        if (!done && !cancelled.get() && delivered.get() == chunks.size()) {
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

    /** A response transformer that requests only what the test asks for, recording callback threads. */
    static final class DemandTransformer implements AsyncResponseTransformer<Object, String> {
        final List<ByteBuffer> received = new CopyOnWriteArrayList<>();
        final List<Thread> callbackThreads = new CopyOnWriteArrayList<>();
        volatile Subscription subscription;
        volatile boolean autoRequest;
        volatile RuntimeException onResponseFailure;
        private volatile CompletableFuture<String> result;

        @Override
        public CompletableFuture<String> prepare() {
            result = new CompletableFuture<>();
            return result;
        }

        @Override
        public void onResponse(Object response) {
            callbackThreads.add(Thread.currentThread());
            if (onResponseFailure != null) {
                throw onResponseFailure;
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            callbackThreads.add(Thread.currentThread());
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    if (autoRequest) {
                        s.request(Long.MAX_VALUE);
                    }
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    received.add(chunk);
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    result.complete(text(received));
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            CompletableFuture<String> current = result;
            if (current != null) {
                current.completeExceptionally(error);
            }
        }
    }

    /** A paginator subscriber that requests only what the test asks for. */
    static final class PageSubscriber implements Subscriber<Object> {
        final List<Object> pages = new CopyOnWriteArrayList<>();
        final List<Thread> threads = new CopyOnWriteArrayList<>();
        final AtomicReference<String> terminal = new AtomicReference<>();
        private final CompletableFuture<Subscription> subscription = new CompletableFuture<>();

        Subscription subscription() throws Exception {
            return subscription.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription.complete(s);
        }

        @Override
        public void onNext(Object page) {
            threads.add(Thread.currentThread());
            pages.add(page);
            terminal.compareAndSet(null, "page");
        }

        @Override
        public void onError(Throwable t) {
            terminal.set("error:" + t);
        }

        @Override
        public void onComplete() {
            terminal.compareAndSet(null, "complete");
        }
    }

    /** A retry scheduler whose delays complete only when the test completes them. */
    static final class HeldScheduler implements RetryScheduler {
        final BlockingQueue<CompletableFuture<Void>> delays = new LinkedBlockingQueue<>();

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            delays.add(future);
            return future;
        }

        CompletableFuture<Void> next() throws InterruptedException {
            CompletableFuture<Void> delay = delays.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull(delay, "no delay was scheduled");
            return delay;
        }
    }

    /** An executor counting its tasks, running them on named threads. */
    static final class RecordingExecutor implements Executor {
        final AtomicInteger tasks = new AtomicInteger();
        final ExecutorService delegate = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, RECORDING);
            thread.setDaemon(true);
            return thread;
        });

        @Override
        public void execute(Runnable command) {
            tasks.incrementAndGet();
            delegate.execute(command);
        }
    }

    /** Records the thread of every observational hook. */
    static final class ThreadRecordingInterceptor implements ExecutionInterceptor {
        private final List<Map.Entry<String, Thread>> calls = new CopyOnWriteArrayList<>();

        private void record(String hook) {
            calls.add(Map.entry(hook, Thread.currentThread()));
        }

        boolean contains(String hook) {
            return calls.stream().anyMatch(e -> e.getKey().equals(hook));
        }

        int size() {
            return calls.size();
        }

        List<String> names() {
            return calls.stream().map(Map.Entry::getKey).toList();
        }

        List<Thread> threads() {
            return calls.stream().map(Map.Entry::getValue).toList();
        }

        Thread threadOf(String hook) {
            return calls.stream().filter(e -> e.getKey().equals(hook)).map(Map.Entry::getValue).findFirst()
                        .orElseThrow(() -> new AssertionError(hook + " never ran: " + names()));
        }

        void assertNoneOn(String threadName) {
            for (Map.Entry<String, Thread> call : calls) {
                assertFalse(call.getValue().getName().equals(threadName), call.getKey() + " ran on " + threadName);
            }
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            record("beforeExecution");
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            record("beforeAttempt");
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            record("afterSerialization");
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            record("beforeTransmission");
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            record("afterTransmission");
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            record("beforeDeserialization");
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            record("afterAttempt");
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            record("afterExecution");
        }
    }
}
