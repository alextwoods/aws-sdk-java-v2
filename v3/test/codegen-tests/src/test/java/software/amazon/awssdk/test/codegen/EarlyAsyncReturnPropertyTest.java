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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.ControlledHttp;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.DemandTransformer;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Exchange;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.RecordingExecutor;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Reply;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.SourcePublisher;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Property 21 (Requirements 8.2, 14.9): async calls return before terminal completion.
 *
 * <p>A synthetic restJson1 service (a signed plain/waitable operation, an unsigned streaming-input operation, a
 * signed streaming-output operation and a signed paginated operation) is generated and compiled once. Each try
 * generates an operation surface (plain call, streaming input, streaming output, a paginator's first page, an async
 * waiter), the stage at which the call is held (a credentials provider's identity future, a retry delay on a held
 * {@link RetryScheduler}, the transport exchange before headers, the request body's transport demand after a few
 * chunks, or the response body mid-stream), zero to two retried attempts ({@code 500}) before the held stage, a
 * caller-owned or SDK-owned completion executor, and a scripted outcome (success, or a modeled-protocol {@code 400}
 * or, mid-stream, a transport {@link IOException}).
 *
 * <p>Latch oracle: the call method runs on a dedicated caller thread while the test drives the retried attempts, and
 * must return within a bounded join while the stage is still held, so a blocking implementation fails rather than
 * hangs. The returned future (the waiter future, or a future completed by the paginator's first {@code onNext} or
 * {@code onError}) is incomplete when the call method returns and stays incomplete after a settle while the stage is
 * held. After the release it completes exactly as predicted: the scripted success value or the scripted failure in
 * the cause chain, after exactly {@code retries + 1} (or {@code + 2} for a held retry delay) exchanges.
 *
 * <p>Call surfaces: {@link CallSurface} is the extension point for the Dynamic Client (Requirement 14.9). Only
 * {@link CallSurface#GENERATED} exists today, because the Dynamic Client (task 12) is not implemented yet; once it
 * is, a {@code DYNAMIC} constant that invokes the same model's operations through the Dynamic Client's async entry
 * point (and declares which {@link Operation}s it supports) joins the generator unchanged. Engine-level early return
 * is covered by {@code AwsCallEngineAsyncTest} and {@code PipelineLifecyclePropertyTest} in {@code aws-pipeline};
 * this property covers the generated surface end to end over {@link GeneratedAsyncBehaviorTest}'s transport.
 */
class EarlyAsyncReturnPropertyTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.early";
    private static final String CALLER = "early-return-caller";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /** The bound on the call method itself: far below {@link #TIMEOUT}, far above any non-blocking return. */
    private static final Duration CALL_BOUND = Duration.ofSeconds(5);
    private static final String FAILURE_MESSAGE = "scripted-failure";
    private static final List<String> REQUEST_CHUNKS = List.of("a", "b", "c");
    private static final List<String> RESPONSE_CHUNKS = List.of("x", "y", "z");
    private static final AwsCredentialsIdentity IDENTITY =
        AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

    private static final String MODEL = """
        $version: "2"
        namespace example.early

        use smithy.waiters#waitable

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "items")
        @aws.api#service(sdkId: "Items", endpointPrefix: "items")
        service Items {
            version: "2024-01-01"
            operations: [GetItem, PutBlob, GetBlob, ListItems]
        }

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

    /** Thin adapters over the generated public async API. */
    private static final String PROBE = """
        package example.probe;

        import java.lang.reflect.Field;
        import java.net.URI;
        import java.nio.ByteBuffer;
        import java.util.List;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Executor;
        import org.reactivestreams.Publisher;
        import software.amazon.awssdk.core.async.AsyncRequestBody;
        import software.amazon.awssdk.core.async.AsyncResponseTransformer;
        import software.amazon.awssdk.core.waiters.WaiterResponse;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
        import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.retries.RetryScheduler;
        import PKG.ItemsAsyncClient;
        import PKG.ItemsAsyncClientBuilder;
        import PKG.model.GetBlobRequest;
        import PKG.model.GetBlobResponse;
        import PKG.model.GetItemRequest;
        import PKG.model.GetItemResponse;
        import PKG.model.ListItemsRequest;
        import PKG.model.ListItemsResponse;
        import PKG.model.PutBlobRequest;
        import PKG.waiters.ItemsAsyncWaiter;

        public final class EarlyProbe {
            private EarlyProbe() {
            }

            public static AutoCloseable client(SdkAsyncHttpClient http, Executor executor,
                                               IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                               RetryScheduler scheduler) throws Exception {
                ItemsAsyncClientBuilder builder = ItemsAsyncClient.builder().region(Region.of("us-east-1"))
                    .endpointOverride(URI.create("https://items.test")).httpClient(http)
                    .credentialsProvider(credentials);
                if (executor != null) {
                    builder.asyncCompletionExecutor(executor);
                }
                // The public builder has no retry-scheduler setter; its default forwards to the build state.
                Field state = builder.getClass().getDeclaredField("state");
                state.setAccessible(true);
                ((AwsClientBuildState) state.get(builder)).retryScheduler(scheduler);
                return builder.build();
            }

            public static CompletableFuture<?> getItem(Object client) {
                return ((ItemsAsyncClient) client).getItem(GetItemRequest.builder().id("a").build());
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

            public static List<String> items(Object page) {
                return ((ListItemsResponse) page).items();
            }

            public static CompletableFuture<?> waitUntilItemReady(Object client, RetryScheduler scheduler) {
                ItemsAsyncWaiter waiter = ItemsAsyncWaiter.builder().client((ItemsAsyncClient) client)
                                                          .retryScheduler(scheduler).build();
                return waiter.waitUntilItemReady(r -> r.id("a"));
            }

            @SuppressWarnings("unchecked")
            public static String waiterValue(Object response) {
                return ((WaiterResponse<GetItemResponse>) response).matched().response().get().value();
            }
        }
        """.replace("PKG", PACKAGE);

    private static URLClassLoader loader;
    private static Class<?> probe;
    private static ExecutorService caller;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "early-async-return")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Files.writeString(models.resolve("early.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("example.early#Items", PACKAGE, models, work.resolve("out")), List.of());
        Path file = Files.createDirectories(result.sourceRoot().resolve("example/probe")).resolve("EarlyProbe.java");
        Files.writeString(file, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(result.sourceRoot(), work.resolve("classes")).loader();
        probe = Class.forName("example.probe.EarlyProbe", true, loader);
        caller = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, CALLER);
            thread.setDaemon(true);
            return thread;
        });
    }

    @AfterContainer
    static void close() throws Exception {
        if (caller != null) {
            caller.shutdownNow();
        }
        if (loader != null) {
            loader.close();
        }
    }

    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    void asyncCallsReturnAnIncompleteFutureBeforeTheHeldStageIsReleased(@ForAll("scenarios") Scenario scenario)
        throws Exception {
        Statistics.label("surface/operation").collect(scenario.surface(), scenario.operation());
        Statistics.label("block").collect(scenario.block());
        try (Fixture fixture = new Fixture(scenario)) {
            Future<Returned> call = caller.submit(fixture::start);
            fixture.driveRetriedAttempts();
            fixture.awaitHeldStage();

            Returned returned;
            try {
                returned = call.get(CALL_BOUND.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new AssertionError("the call method did not return while " + scenario.block() + " was held");
            } catch (ExecutionException e) {
                throw new AssertionError("the call method threw", e.getCause());
            }
            assertEquals(CALLER, returned.thread().getName(), "the call method ran on the calling thread");
            assertFalse(returned.releasedAtReturn(), "the call method returned before the release");
            assertFalse(returned.doneAtReturn(), "the call returned an already complete future");

            fixture.settle();
            assertFalse(returned.future().isDone(), "the future completed while " + scenario.block() + " was held");

            fixture.release();
            fixture.assertOutcome(returned.future());
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<CallSurface> surfaces = Arbitraries.of(CallSurface.values());
        return surfaces.flatMap(surface -> Arbitraries.of(surface.operations()).flatMap(operation -> {
            Arbitrary<Block> blocks = Arbitraries.of(operation.blocks());
            return blocks.flatMap(block -> Combinators.combine(
                Arbitraries.integers().between(0, maxRetries(operation, block)),
                Arbitraries.integers().between(0, REQUEST_CHUNKS.size() - 1),
                Arbitraries.of(CompletionExecutor.values()),
                Arbitraries.of(Outcome.values()))
                .as((retries, heldAfter, executor, outcome) ->
                        new Scenario(surface, operation, block, retries, heldAfter, executor, outcome)));
        }));
    }

    /** The standard strategy allows three attempts; a non-replayable request body is never retried. */
    private static int maxRetries(Operation operation, Block block) {
        if (operation == Operation.STREAMING_INPUT) {
            return 0;
        }
        return block == Block.RETRY_DELAY ? 1 : 2;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Scenario

    /**
     * How the call is made. Only the generated async client exists; the Dynamic Client (task 12) adds {@code DYNAMIC}
     * here, with the operations its async entry point supports and a branch in {@link Fixture#start}.
     */
    enum CallSurface {
        GENERATED(EnumSet.allOf(Operation.class));

        private final Set<Operation> operations;

        CallSurface(Set<Operation> operations) {
            this.operations = operations;
        }

        Set<Operation> operations() {
            return operations;
        }
    }

    enum Operation {
        PLAIN(EnumSet.of(Block.IDENTITY, Block.RETRY_DELAY, Block.TRANSPORT)),
        STREAMING_INPUT(EnumSet.of(Block.TRANSPORT, Block.REQUEST_DEMAND)),
        STREAMING_OUTPUT(EnumSet.of(Block.IDENTITY, Block.RETRY_DELAY, Block.TRANSPORT, Block.RESPONSE_BODY)),
        PAGINATOR_FIRST_PAGE(EnumSet.of(Block.IDENTITY, Block.RETRY_DELAY, Block.TRANSPORT)),
        WAITER(EnumSet.of(Block.IDENTITY, Block.RETRY_DELAY, Block.TRANSPORT));

        private final Set<Block> blocks;

        Operation(Set<Block> blocks) {
            this.blocks = blocks;
        }

        Set<Block> blocks() {
            return blocks;
        }
    }

    /** The stage held when the call method returns. {@code PutBlob} is unsigned, so it has no identity stage. */
    enum Block {
        IDENTITY, RETRY_DELAY, TRANSPORT, REQUEST_DEMAND, RESPONSE_BODY
    }

    enum CompletionExecutor {
        CALLER_OWNED, SDK_OWNED
    }

    enum Outcome {
        SUCCESS, FAILURE
    }

    /**
     * One try. {@code heldAfter} is the number of request (or response) body chunks moved before the hold, for
     * {@link Block#REQUEST_DEMAND} (or {@link Block#RESPONSE_BODY}); other blocks ignore it.
     */
    record Scenario(CallSurface surface, Operation operation, Block block, int retries, int heldAfter,
                    CompletionExecutor executor, Outcome outcome) {
        /** Attempts answered {@code 500} before the held stage: a held retry delay follows one more. */
        int failedAttempts() {
            return block == Block.RETRY_DELAY ? retries + 1 : retries;
        }

        @Override
        public String toString() {
            return surface + " " + operation + " held at " + block + " after " + retries + " retries"
                   + (block == Block.REQUEST_DEMAND || block == Block.RESPONSE_BODY ? " and " + heldAfter + " chunks"
                                                                                    : "")
                   + ", " + executor + " executor, " + outcome;
        }
    }

    record Returned(CompletableFuture<?> future, boolean doneAtReturn, boolean releasedAtReturn, Thread thread) {
    }

    // -------------------------------------------------------------------------------------------------------------
    // Fixture: one client over a controlled transport, with the held stage and its release

    private static final class Fixture implements AutoCloseable {
        private final Scenario scenario;
        private final ControlledHttp http = new ControlledHttp();
        private final RecordingExecutor recording = new RecordingExecutor();
        private final HeldCredentials credentials;
        private final GateScheduler retryScheduler;
        private final SourcePublisher requestBody = new SourcePublisher(REQUEST_CHUNKS.toArray(String[]::new));
        private final DemandTransformer transformer = new DemandTransformer();
        private final AutoCloseable client;
        private volatile boolean released;
        private Exchange held;
        private HeldStream heldStream;

        Fixture(Scenario scenario) throws Exception {
            this.scenario = scenario;
            this.credentials = new HeldCredentials(scenario.block() == Block.IDENTITY ? scenario.retries() + 1 : 0);
            this.retryScheduler = new GateScheduler(scenario.block() == Block.RETRY_DELAY ? scenario.retries() + 1
                                                                                         : 0);
            this.transformer.autoRequest = true;
            this.client = call("client", http, scenario.executor() == CompletionExecutor.CALLER_OWNED ? recording
                                                                                                       : null,
                               credentials, retryScheduler);
        }

        /** Runs on the caller thread: invokes the call method and records what it returned. */
        Returned start() throws Exception {
            CompletableFuture<?> future = switch (scenario.surface()) {
                case GENERATED -> startGenerated();
            };
            return new Returned(future, future.isDone(), released, Thread.currentThread());
        }

        private CompletableFuture<?> startGenerated() throws Exception {
            return switch (scenario.operation()) {
                case PLAIN -> call("getItem", client);
                case STREAMING_INPUT -> call("putBlob", client, requestBody);
                case STREAMING_OUTPUT -> call("getBlob", client, transformer);
                case WAITER -> call("waitUntilItemReady", client, (RetryScheduler) d -> CompletableFuture
                    .completedFuture(null));
                case PAGINATOR_FIRST_PAGE -> {
                    FirstPage first = new FirstPage();
                    ((Publisher<?>) call("listItems", client)).subscribe(first);
                    first.subscription.get(CALL_BOUND.toMillis(), TimeUnit.MILLISECONDS).request(1);
                    yield first.signal;
                }
            };
        }

        void driveRetriedAttempts() throws Exception {
            for (int i = 0; i < scenario.failedAttempts(); i++) {
                http.next().respond(Reply.json(500, "{\"message\":\"busy\"}"));
            }
        }

        void awaitHeldStage() throws Exception {
            switch (scenario.block()) {
                case IDENTITY -> credentials.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                case RETRY_DELAY -> retryScheduler.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                case TRANSPORT -> held = http.next();
                case REQUEST_DEMAND -> {
                    held = http.next();
                    if (scenario.heldAfter() > 0) {
                        held.pullBody(scenario.heldAfter());
                    }
                    await(() -> held.bodyChunks.size() == scenario.heldAfter());
                }
                case RESPONSE_BODY -> {
                    held = http.next();
                    heldStream = new HeldStream(http.eventLoop, scenario.heldAfter(),
                                                scenario.outcome() == Outcome.FAILURE ? new IOException("reset")
                                                                                      : null);
                    answerWithStream(held, heldStream);
                    heldStream.reachedHold.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    await(() -> transformer.received.size() == scenario.heldAfter());
                }
                default -> throw new IllegalStateException(scenario.block().name());
            }
            assertEquals(scenario.failedAttempts() + (scenario.block() == Block.IDENTITY
                                                      || scenario.block() == Block.RETRY_DELAY ? 0 : 1),
                         http.calls.get(), "exchanges started before the held stage");
        }

        /** Drains the event loop and lets the executors run anything still queued. */
        void settle() throws Exception {
            for (int i = 0; i < 2; i++) {
                http.eventLoop.submit(() -> { }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                Thread.sleep(5);
            }
            if (scenario.block() == Block.REQUEST_DEMAND) {
                assertEquals(scenario.heldAfter(), requestBody.delivered.get(), "body read beyond transport demand");
            }
        }

        void release() throws Exception {
            released = true;
            switch (scenario.block()) {
                case IDENTITY -> {
                    credentials.held.join().complete(IDENTITY);
                    answer(http.next());
                }
                case RETRY_DELAY -> {
                    retryScheduler.held.join().complete(null);
                    answer(http.next());
                }
                case TRANSPORT, REQUEST_DEMAND -> answer(held);
                case RESPONSE_BODY -> heldStream.release();
                default -> throw new IllegalStateException(scenario.block().name());
            }
        }

        private void answer(Exchange exchange) throws Exception {
            if (scenario.operation() == Operation.STREAMING_INPUT) {
                exchange.pullBody(REQUEST_CHUNKS.size() + 1);
                await(exchange.bodyComplete::get);
            }
            if (scenario.outcome() == Outcome.FAILURE) {
                exchange.respond(new Reply(400, Map.of("Content-Type", "application/json",
                                                       "X-Amzn-ErrorType", "Rejected"),
                                           "{\"message\":\"" + FAILURE_MESSAGE + "\"}", null));
                return;
            }
            switch (scenario.operation()) {
                case PLAIN, WAITER -> exchange.respond(Reply.json(200, "{\"value\":\"READY\"}"));
                case STREAMING_INPUT -> exchange.respond(Reply.json(200, "{}"));
                case PAGINATOR_FIRST_PAGE -> exchange.respond(Reply.json(200, "{\"items\":[\"i1\"]}"));
                case STREAMING_OUTPUT -> answerWithStream(exchange, new HeldStream(http.eventLoop, -1, null));
                default -> throw new IllegalStateException(scenario.operation().name());
            }
        }

        /** Answers {@code 200} headers and a body stream that completes or fails the exchange when it terminates. */
        private void answerWithStream(Exchange exchange, HeldStream stream) throws Exception {
            stream.onTerminal = failure -> {
                if (failure == null) {
                    exchange.completion.complete();
                } else {
                    exchange.completion.fail(failure);
                }
            };
            http.eventLoop.submit(() -> {
                exchange.handler.onHeaders(SdkHttpResponse.builder().statusCode(200).addHeader("X-Kind", "k")
                                                          .addHeader("Content-Type", "application/octet-stream")
                                                          .body(HttpBody.empty()).build());
                exchange.handler.onStream(stream);
            }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        void assertOutcome(CompletableFuture<?> future) throws Exception {
            int exchanges = scenario.failedAttempts() + 1;
            if (scenario.outcome() == Outcome.FAILURE) {
                ExecutionException e = assertThrows(ExecutionException.class,
                                                    () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
                assertFalse(future.isCancelled());
                List<Throwable> chain = chain(e.getCause());
                if (scenario.block() == Block.RESPONSE_BODY) {
                    assertTrue(chain.contains(heldStream.failure), "the scripted stream failure in " + chain);
                } else {
                    assertTrue(chain.stream().anyMatch(t -> t instanceof AwsServiceException
                                                            && t.getMessage().contains(FAILURE_MESSAGE)
                                                            && t.getMessage().contains("400")),
                               "the scripted service failure in " + chain);
                }
                assertEquals(exchanges, http.calls.get(), "exchanges");
                return;
            }
            Object result;
            try {
                result = future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("the call failed", e.getCause());
            }
            switch (scenario.operation()) {
                case PLAIN -> assertEquals("READY", call("value", result));
                case WAITER -> assertEquals("READY", call("waiterValue", result));
                case STREAMING_INPUT -> {
                    assertNotNull(result);
                    assertEquals(String.join("", REQUEST_CHUNKS), held.bodyText(), "the transmitted body");
                }
                case STREAMING_OUTPUT -> assertEquals(String.join("", RESPONSE_CHUNKS), result);
                case PAGINATOR_FIRST_PAGE -> assertEquals(List.of("i1"), call("items", result));
                default -> throw new IllegalStateException(scenario.operation().name());
            }
            assertEquals(exchanges, http.calls.get(), "exchanges");
        }

        @Override
        public void close() throws Exception {
            // Unblock anything still held, so a failed try leaves no stuck caller thread behind.
            released = true;
            credentials.held.thenAccept(f -> f.complete(IDENTITY));
            retryScheduler.held.thenAccept(f -> f.complete(null));
            if (heldStream != null) {
                heldStream.release();
            }
            try {
                client.close();
            } finally {
                http.eventLoop.shutdownNow();
                recording.delegate.shutdownNow();
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Held stages

    /** Static credentials whose {@code holdAt}-th resolution (1-based; 0: none) stays pending until released. */
    static final class HeldCredentials implements IdentityProvider<AwsCredentialsIdentity> {
        final CompletableFuture<CompletableFuture<AwsCredentialsIdentity>> held = new CompletableFuture<>();
        private final int holdAt;
        private final AtomicInteger resolutions = new AtomicInteger();

        HeldCredentials(int holdAt) {
            this.holdAt = holdAt;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            if (resolutions.incrementAndGet() == holdAt) {
                CompletableFuture<AwsCredentialsIdentity> pending = new CompletableFuture<>();
                held.complete(pending);
                return pending;
            }
            return CompletableFuture.completedFuture(IDENTITY);
        }
    }

    /** A retry scheduler whose {@code holdAt}-th delay (1-based; 0: none) stays pending; others elapse at once. */
    static final class GateScheduler implements RetryScheduler {
        final CompletableFuture<CompletableFuture<Void>> held = new CompletableFuture<>();
        private final int holdAt;
        private final AtomicInteger delays = new AtomicInteger();

        GateScheduler(int holdAt) {
            this.holdAt = holdAt;
        }

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            if (delays.incrementAndGet() == holdAt) {
                CompletableFuture<Void> pending = new CompletableFuture<>();
                held.complete(pending);
                return pending;
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * A response body on the event loop that, on demand, delivers {@code holdAfter} chunks (-1: all) and then holds
     * until released; after the release it delivers the rest and completes, or fails with {@code failure}.
     */
    static final class HeldStream implements Publisher<ByteBuffer> {
        final CompletableFuture<Void> reachedHold = new CompletableFuture<>();
        final IOException failure;
        private final ExecutorService eventLoop;
        private final int holdAfter;
        volatile Consumer<Throwable> onTerminal = f -> { };
        // Confined to the event loop.
        private Subscriber<? super ByteBuffer> subscriber;
        private long demand;
        private int delivered;
        private boolean released;
        private boolean done;

        HeldStream(ExecutorService eventLoop, int holdAfter, IOException failure) {
            this.eventLoop = eventLoop;
            this.holdAfter = holdAfter;
            this.failure = failure;
            this.released = holdAfter < 0;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            eventLoop.execute(() -> {
                subscriber = s;
                s.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        eventLoop.execute(() -> {
                            demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                            drain();
                        });
                    }

                    @Override
                    public void cancel() {
                        eventLoop.execute(() -> done = true);
                    }
                });
            });
        }

        void release() {
            eventLoop.execute(() -> {
                released = true;
                if (subscriber != null) {
                    drain();
                }
            });
        }

        private void drain() {
            while (!done) {
                if (!released && delivered == holdAfter) {
                    reachedHold.complete(null);
                    return;
                }
                if (released && failure != null) {
                    done = true;
                    subscriber.onError(failure);
                    onTerminal.accept(failure);
                    return;
                }
                if (delivered == RESPONSE_CHUNKS.size()) {
                    done = true;
                    subscriber.onComplete();
                    onTerminal.accept(null);
                    return;
                }
                if (demand == 0) {
                    return;
                }
                demand--;
                subscriber.onNext(ByteBuffer.wrap(RESPONSE_CHUNKS.get(delivered++).getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    /** Completes {@link #signal} with the paginator's first page, or its first error. */
    static final class FirstPage implements Subscriber<Object> {
        final CompletableFuture<Subscription> subscription = new CompletableFuture<>();
        final CompletableFuture<Object> signal = new CompletableFuture<>();

        @Override
        public void onSubscribe(Subscription s) {
            subscription.complete(s);
        }

        @Override
        public void onNext(Object page) {
            signal.complete(page);
        }

        @Override
        public void onError(Throwable t) {
            signal.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            signal.completeExceptionally(new AssertionError("the paginator completed without a page"));
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Support

    @SuppressWarnings("unchecked")
    private static <T> T call(String name, Object... arguments) throws Exception {
        for (Method method : probe.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                try {
                    return (T) method.invoke(null, arguments);
                } catch (InvocationTargetException e) {
                    throw new AssertionError("EarlyProbe." + name + " failed", e.getCause());
                }
            }
        }
        throw new AssertionError("no probe method " + name);
    }

    private static List<Throwable> chain(Throwable failure) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = failure; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
            chain.addAll(List.of(t.getSuppressed()));
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
}
