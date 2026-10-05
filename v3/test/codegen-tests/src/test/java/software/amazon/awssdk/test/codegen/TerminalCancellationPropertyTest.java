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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.ControlledHttp;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Exchange;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.LoopPublisher;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.RecordingExecutor;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.SourcePublisher;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Property 23 (Requirements 8.9, 8.10, 10.15, 14.10, 14.13): cancellation is terminal and comprehensive.
 *
 * <p>A synthetic restJson1 service (a signed plain/waitable operation, an unsigned streaming-input operation and a
 * signed streaming-output operation) is generated and compiled once. Each try generates an operation surface (plain
 * call, streaming input, streaming output, async waiter), a cancellation point, zero to two retried attempts
 * ({@code 500}) before it, and a racing callback. The cancellation points:
 * <ul>
 *   <li>start: the caller cancels right after the call method returns, while the driver runs ahead on the
 *       completion executor up to the transport;</li>
 *   <li>identity: the attempt's identity future is pending;</li>
 *   <li>retry delay: the engine's retry delay is pending on the retry scheduler;</li>
 *   <li>transport: the exchange has started and has no headers;</li>
 *   <li>request body: the transport has read one or two request-body chunks;</li>
 *   <li>response body: the response transformer has received one or two of the payload's chunks;</li>
 *   <li>waiter delay: the waiter's delay between two polls is pending.</li>
 * </ul>
 * The racing callback runs on its own thread, released together with the cancellation through a barrier and skewed by
 * a generated spin on either side: none (the held stage is completed only afterwards, as a stale callback), a second
 * {@code cancel}, or the held stage's own completion toward the scripted success or the scripted failure (identity
 * failure, retry-delay failure, transport {@link IOException}, response-stream {@link IOException}, waiter-delay
 * failure, or for the start point the transport answering {@code 400}). Every exchange after the cancellation point
 * is answered with the scripted outcome, and the retry strategy allows exactly the scripted attempts.
 *
 * <p>Oracle: a single-winner reference state machine ({@link Reference}) is {@code ACTIVE} until its first terminal
 * transition and rejects every later one. The observed race is linearized into it, the winner first: a successful
 * cancellation, or the racer's scripted completion; a cancellation with no racing completion must win. The call must
 * then be in the reference's state ({@code CANCELLED} only if {@code cancel} succeeded), its future must have signalled
 * exactly once, with the scripted value or failure (by identity) if the cancellation lost. For the work, the reference
 * holds the script's continuation from the cancellation point (the calls, attempts, identity resolutions, exchanges,
 * retry and waiter delays and {@code afterExecution} hooks it still contains): a call that completed did exactly that
 * work; a cancelled call did no more of it, and none at all if nothing raced the cancellation. After a cancellation
 * every handle has terminated: every exchange is done and cancelled at most once; every retry and waiter delay is done;
 * a subscribed request body or response stream is cancelled or was consumed to its end; the transformer's result is
 * complete and {@code exceptionOccurred} ran at most once. With nothing racing, the held handle itself was cancelled,
 * an exchange exactly once for {@link CancellationReason#CALLER_CANCELLED}. A call that completed cancelled no
 * exchange for the caller. Finally, stale
 * callbacks (the held identity and every delay completing, the transport pulling more request body, the response
 * stream resuming, the transformer requesting more) start no call, attempt, identity resolution, exchange, delay,
 * hook or body work, the reference rejects the stale completion, and the future's outcome and single signal stand.
 *
 * <p>Call surfaces: {@link CallSurface} is the extension point for the Dynamic Client (Requirements 14.10, 14.13), as
 * in {@link EarlyAsyncReturnPropertyTest}. Only {@link CallSurface#GENERATED} exists today, because the Dynamic Client
 * (task 12) is not implemented yet; once it is, a {@code DYNAMIC} constant declaring the {@link Operation}s its async
 * entry point supports, with a branch in {@link Fixture#start}, joins the generator unchanged, because both surfaces
 * drive the same {@code AwsCallEngine} async driver. Synchronous calls have no caller cancellation handle. Paginator
 * subscription cancellation is not a returned-future cancellation and is covered by {@link GeneratedAsyncBehaviorTest};
 * engine-level cancellation races on a manual executor are covered by {@code AwsCallEngineCancellationTest} in
 * {@code aws-pipeline}.
 */
class TerminalCancellationPropertyTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.cancellation";
    private static final String RACER = "cancellation-racer";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /** Spin iterations per unit of generated skew. */
    private static final int SPIN = 2_000;
    private static final List<String> REQUEST_CHUNKS = List.of("a", "b", "c");
    private static final List<String> RESPONSE_CHUNKS = List.of("x", "y", "z");
    private static final AwsCredentialsIdentity IDENTITY =
        AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

    private static final String MODEL = """
        $version: "2"
        namespace example.cancellation

        use smithy.waiters#waitable

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "items")
        @aws.api#service(sdkId: "Items", endpointPrefix: "items")
        service Items {
            version: "2024-01-01"
            operations: [GetItem, PutBlob, GetBlob]
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
        """;

    /** Thin adapters over the generated public async API. */
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
        import software.amazon.awssdk.core.waiters.WaiterResponse;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
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
        import PKG.model.PutBlobRequest;
        import PKG.waiters.ItemsAsyncWaiter;

        public final class CancellationProbe {
            private CancellationProbe() {
            }

            public static AutoCloseable client(SdkAsyncHttpClient http, Executor executor,
                                               IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                               RetryScheduler scheduler, RetryStrategy strategy,
                                               ExecutionInterceptor interceptor) throws Exception {
                ItemsAsyncClientBuilder builder = ItemsAsyncClient.builder().region(Region.of("us-east-1"))
                    .endpointOverride(URI.create("https://items.test")).httpClient(http)
                    .credentialsProvider(credentials).asyncCompletionExecutor(executor).retryStrategy(strategy)
                    .addExecutionInterceptor(interceptor);
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
    private static ExecutorService racer;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "terminal-cancellation")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Files.writeString(models.resolve("cancellation.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("example.cancellation#Items", PACKAGE, models, work.resolve("out")), List.of());
        Path file = Files.createDirectories(result.sourceRoot().resolve("example/probe"))
                         .resolve("CancellationProbe.java");
        Files.writeString(file, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(result.sourceRoot(), work.resolve("classes")).loader();
        probe = Class.forName("example.probe.CancellationProbe", true, loader);
        racer = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, RACER);
            thread.setDaemon(true);
            return thread;
        });
    }

    @AfterContainer
    static void close() throws Exception {
        if (racer != null) {
            racer.shutdownNow();
        }
        if (loader != null) {
            loader.close();
        }
    }

    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    void aSuccessfulCancellationIsTheOneTerminalTransitionAndEndsEveryHandle(@ForAll("scenarios") Scenario scenario)
        throws Exception {
        Statistics.label("point").collect(scenario.point());
        Statistics.label("point").coverage(c -> {
            for (Point point : Point.values()) {
                c.check(point).count(n -> n >= 5);
            }
        });
        Statistics.label("racer").collect(scenario.racer());
        Statistics.label("surface/operation").collect(scenario.surface(), scenario.operation());

        try (Fixture fixture = new Fixture(scenario)) {
            Counts before = fixture.reachCancellationPoint();
            Chunks chunksBefore = fixture.chunks();
            boolean won = fixture.cancelRacing();
            Statistics.label("cancellation").collect(scenario.racer().completion() == null ? "unraced"
                                                     : won ? "won the race" : "lost the race");
            fixture.quiesce();

            Reference reference = new Reference(scenario);
            reference.linearize(won);
            assertEquals(reference.status(), Status.of(fixture.future), "the call's terminal state: " + scenario);
            assertEquals(1, fixture.terminalSignals.get(), "terminal signals of " + scenario);
            fixture.assertSurfaced(reference.status());

            Counts settled = fixture.counts();
            reference.assertWork(before, settled);
            fixture.assertHandles(reference.status() == Status.CANCELLED);
            Chunks chunksSettled = fixture.chunks();
            if (reference.status() == Status.CANCELLED && scenario.racer().completion() == null
                && scenario.point() != Point.START) {
                assertEquals(chunksBefore, chunksSettled, "body work after the cancellation of " + scenario);
            }

            fixture.staleCallbacks();
            fixture.settle();
            assertFalse(reference.apply(Status.SUCCEEDED), "the reference accepted a stale completion");
            assertEquals(settled, fixture.counts(), "work scheduled by stale callbacks of " + scenario);
            assertEquals(chunksSettled, fixture.chunks(), "body work after the terminal transition of " + scenario);
            assertEquals(1, fixture.terminalSignals.get(), "a second terminal signal of " + scenario);
            assertEquals(reference.status(), Status.of(fixture.future), "the terminal state changed: " + scenario);
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        // The point is drawn first, so every point is equally likely whatever the operations it applies to.
        return Arbitraries.of(Point.values()).flatMap(point -> Arbitraries.of(CallSurface.values()).flatMap(
            surface -> {
                Set<Operation> operations = EnumSet.noneOf(Operation.class);
                for (Operation operation : surface.operations()) {
                    if (operation.points().contains(point)) {
                        operations.add(operation);
                    }
                }
                return Arbitraries.of(operations).flatMap(operation -> Combinators.combine(
                    Arbitraries.of(Racer.values()),
                    Arbitraries.integers().between(0, maxRetries(operation, point)),
                    Arbitraries.integers().between(point.usesChunks() ? 1 : 0, point.usesChunks() ? 2 : 0),
                    Arbitraries.integers().between(-3, 3))
                    .as((racing, retries, chunks, skew) ->
                            new Scenario(surface, operation, point, racing, retries, chunks, skew)));
            }));
    }

    /**
     * A non-replayable request body is never retried, the start point precedes any attempt, and a held retry delay
     * follows one more failed attempt; the retry strategy then allows exactly the scripted attempts.
     */
    private static int maxRetries(Operation operation, Point point) {
        if (operation == Operation.STREAMING_INPUT || point == Point.START) {
            return 0;
        }
        return point == Point.RETRY_DELAY ? 1 : 2;
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

    /** The operation surface and the cancellation points it has; {@code PutBlob} is unsigned and not retried. */
    enum Operation {
        PLAIN(true, EnumSet.of(Point.START, Point.IDENTITY, Point.RETRY_DELAY, Point.TRANSPORT)),
        STREAMING_INPUT(false, EnumSet.of(Point.START, Point.TRANSPORT, Point.REQUEST_BODY)),
        STREAMING_OUTPUT(true, EnumSet.of(Point.START, Point.IDENTITY, Point.RETRY_DELAY, Point.TRANSPORT,
                                          Point.RESPONSE_BODY)),
        WAITER(true, EnumSet.of(Point.START, Point.IDENTITY, Point.RETRY_DELAY, Point.TRANSPORT, Point.WAITER_DELAY));

        private final boolean signed;
        private final Set<Point> points;

        Operation(boolean signed, Set<Point> points) {
            this.signed = signed;
            this.points = points;
        }

        Set<Point> points() {
            return points;
        }
    }

    /** Where the call is when the caller cancels it. */
    enum Point {
        START, IDENTITY, RETRY_DELAY, TRANSPORT, REQUEST_BODY, RESPONSE_BODY, WAITER_DELAY;

        boolean usesChunks() {
            return this == REQUEST_BODY || this == RESPONSE_BODY;
        }
    }

    /** The callback racing the cancellation, and the terminal state it drives the call to if it wins. */
    enum Racer {
        NONE(null), CANCEL(null), SUCCESS(Status.SUCCEEDED), FAILURE(Status.FAILED);

        private final Status completion;

        Racer(Status completion) {
            this.completion = completion;
        }

        /** The terminal state the racer completes the call with, or {@code null} if it completes nothing. */
        Status completion() {
            return completion;
        }
    }

    /**
     * One try. {@code chunks} is the number of request (or response) body chunks moved before the cancellation point,
     * for {@link Point#REQUEST_BODY} (or {@link Point#RESPONSE_BODY}); {@code skew} spins the cancellation (positive,
     * by {@code SPIN * 4^skew} iterations) or the racer (negative, by {@code -skew * SPIN}) after the barrier.
     */
    record Scenario(CallSurface surface, Operation operation, Point point, Racer racer, int retries, int chunks,
                    int skew) {
        /** Attempts answered {@code 500} before the cancellation point: a held retry delay follows one more. */
        int failedAttempts() {
            return point == Point.RETRY_DELAY ? retries + 1 : retries;
        }

        /** Grows geometrically, so a delayed cancellation can also land after the racer has completed the call. */
        int cancelSpins() {
            return skew > 0 ? SPIN << (2 * skew) : 0;
        }

        int racerSpins() {
            return Math.max(-skew, 0) * SPIN;
        }

        @Override
        public String toString() {
            return surface + " " + operation + " cancelled at " + point
                   + (point.usesChunks() ? " after " + chunks + " chunks" : "") + " after " + failedAttempts()
                   + " failed attempts, racing " + racer + " with skew " + skew;
        }
    }

    /** A call's state in the reference: active until its one terminal transition. */
    enum Status {
        ACTIVE, SUCCEEDED, FAILED, CANCELLED;

        static Status of(CompletableFuture<?> future) {
            if (!future.isDone()) {
                return ACTIVE;
            }
            if (future.isCancelled()) {
                return CANCELLED;
            }
            return future.isCompletedExceptionally() ? FAILED : SUCCEEDED;
        }
    }

    /** The work a call has done so far, as counted by the test's interceptor, identity provider, transport and delays. */
    record Counts(int executions, int attempts, int identities, int exchanges, int retryDelays, int waiterDelays,
                  int afterExecutions) {
        static final Counts ZERO = new Counts(0, 0, 0, 0, 0, 0, 0);

        Counts plus(Counts other) {
            return new Counts(executions + other.executions, attempts + other.attempts,
                              identities + other.identities, exchanges + other.exchanges,
                              retryDelays + other.retryDelays, waiterDelays + other.waiterDelays,
                              afterExecutions + other.afterExecutions);
        }

        /** Whether every count is at most {@code other}'s. */
        boolean within(Counts other) {
            return executions <= other.executions && attempts <= other.attempts && identities <= other.identities
                   && exchanges <= other.exchanges && retryDelays <= other.retryDelays
                   && waiterDelays <= other.waiterDelays && afterExecutions <= other.afterExecutions;
        }
    }

    /** The request-body chunks the source delivered and the response-body chunks the streams delivered. */
    record Chunks(int request, int response) {
    }

    // -------------------------------------------------------------------------------------------------------------
    // Oracle

    /**
     * The single-winner reference state machine of one call: {@code ACTIVE} until its first terminal transition, which
     * every later transition loses. With the scenario's script it predicts the work the call may still do from the
     * cancellation point.
     */
    static final class Reference {
        private final Scenario scenario;
        private Status status = Status.ACTIVE;

        Reference(Scenario scenario) {
            this.scenario = scenario;
        }

        Status status() {
            return status;
        }

        /** Makes the terminal transition to {@code to}; false (changing nothing) if the call already ended. */
        boolean apply(Status to) {
            if (status != Status.ACTIVE) {
                return false;
            }
            status = to;
            return true;
        }

        /**
         * Linearizes the observed race, the winner first: the cancellation if {@code cancel} succeeded, otherwise the
         * racer's scripted completion. A cancellation that nothing completes can race must win.
         */
        void linearize(boolean cancelWon) {
            Status completion = scenario.racer().completion();
            if (cancelWon) {
                assertTrue(apply(Status.CANCELLED));
                if (completion != null) {
                    assertFalse(apply(completion), "a second terminal transition");
                }
                return;
            }
            assertNotNull(completion, "the cancellation failed although nothing could complete the call: "
                                      + scenario);
            assertTrue(apply(completion));
            assertFalse(apply(Status.CANCELLED), "a second terminal transition");
        }

        /**
         * The work in the script after the cancellation point: what the driver may do before reaching the held
         * transport (start point), plus what the racer's completion drives, ending the call (and so running its
         * {@code afterExecution} hooks) once. Nothing completes without a completing racer.
         */
        Counts continuation() {
            boolean completes = scenario.racer().completion() != null;
            boolean success = scenario.racer() == Racer.SUCCESS;
            int ends = completes ? 1 : 0;
            int signed = scenario.operation().signed ? 1 : 0;
            return switch (scenario.point()) {
                case START -> new Counts(1, 1, signed, 1, 0, 0, ends);
                case IDENTITY -> new Counts(0, 0, 0, success ? 1 : 0, 0, 0, ends);
                case RETRY_DELAY -> success ? new Counts(0, 1, 1, 1, 0, 0, 1) : new Counts(0, 0, 0, 0, 0, 0, ends);
                case TRANSPORT, REQUEST_BODY, RESPONSE_BODY -> new Counts(0, 0, 0, 0, 0, 0, ends);
                // A failing waiter delay fails the waiter itself, with no further call.
                case WAITER_DELAY -> success ? new Counts(1, 1, 1, 1, 0, 0, 1) : Counts.ZERO;
            };
        }

        /**
         * A completed call did exactly the scripted work; a cancelled call did none beyond it, and with no completing
         * racer none after the cancellation point at all.
         */
        void assertWork(Counts before, Counts settled) {
            Counts complete = before.plus(continuation());
            if (status != Status.CANCELLED) {
                assertEquals(complete, settled, "the work of the completed " + scenario);
                return;
            }
            assertTrue(before.within(settled) && settled.within(complete),
                       "the cancelled " + scenario + " did " + settled + ", outside [" + before + ", " + complete
                       + "]");
            if (scenario.racer().completion() == null && scenario.point() != Point.START) {
                assertEquals(before, settled, "work after the cancellation of " + scenario);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Fixture: one client over a controlled transport, held at the cancellation point, with the racing callback

    private static final class Fixture implements AutoCloseable {
        private final Scenario scenario;
        private final Throwable injected;
        private final RaceHttp http = new RaceHttp();
        private final RecordingExecutor executor = new RecordingExecutor();
        private final CountingCredentials credentials;
        private final RecordingScheduler retryScheduler;
        private final RecordingScheduler waiterScheduler;
        private final CountingInterceptor hooks = new CountingInterceptor();
        private final SourcePublisher requestBody = new SourcePublisher(REQUEST_CHUNKS.toArray(String[]::new));
        private final CountingTransformer transformer = new CountingTransformer();
        private final List<ChunkStream> streams = new CopyOnWriteArrayList<>();
        private final AtomicInteger terminalSignals = new AtomicInteger();
        private final AtomicBoolean transportFailed = new AtomicBoolean();
        private final AutoCloseable client;
        private CompletableFuture<?> future;
        private Exchange held;
        private ChunkStream heldStream;

        Fixture(Scenario scenario) throws Exception {
            this.scenario = scenario;
            this.injected = switch (scenario.point()) {
                case START -> null;
                case IDENTITY -> SdkClientException.create("injected identity failure");
                case RETRY_DELAY, WAITER_DELAY -> new IllegalStateException("injected delay failure");
                case TRANSPORT, REQUEST_BODY, RESPONSE_BODY -> new IOException("injected transport failure");
            };
            Point point = scenario.point();
            this.credentials = new CountingCredentials(point == Point.IDENTITY ? scenario.failedAttempts() + 1 : 0);
            this.retryScheduler = new RecordingScheduler(point == Point.RETRY_DELAY ? scenario.failedAttempts() : 0);
            this.waiterScheduler = new RecordingScheduler(point == Point.WAITER_DELAY ? 1 : 0);
            StandardRetryStrategy strategy = StandardRetryStrategy.builder()
                                                                  .maxAttempts(scenario.failedAttempts() + 1)
                                                                  .backoffStrategy(a -> Duration.ofMillis(10))
                                                                  .build();
            this.client = call("client", http, executor, credentials, retryScheduler, strategy, hooks);
        }

        /**
         * Starts the call and holds it at the cancellation point, answering the retried attempts; returns the work
         * done before the point (for the start point, before the call). From then on every new exchange is answered
         * with the scripted outcome, if the racer completes the call.
         */
        Counts reachCancellationPoint() throws Exception {
            if (scenario.point() == Point.START) {
                Counts before = counts();
                if (scenario.racer().completion() != null) {
                    http.auto = this::answerScripted;
                }
                start();
                return before;
            }
            start();
            for (int i = 0; i < scenario.failedAttempts(); i++) {
                respond(http.next(), 500, "{\"message\":\"busy\"}").get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
            switch (scenario.point()) {
                case IDENTITY -> credentials.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                case RETRY_DELAY -> retryScheduler.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                case TRANSPORT -> held = http.next();
                case REQUEST_BODY -> {
                    held = http.next();
                    held.pullBody(scenario.chunks());
                    await(() -> requestBody.delivered.get() == scenario.chunks());
                }
                case RESPONSE_BODY -> {
                    held = http.next();
                    heldStream = stream(scenario.chunks(), scenario.racer() == Racer.FAILURE ? injected : null);
                    respondStream(held, heldStream).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    heldStream.reachedHold.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    await(() -> transformer.received.get() == scenario.chunks());
                }
                case WAITER_DELAY -> {
                    respond(http.next(), 200, "{\"value\":\"PENDING\"}").get(TIMEOUT.toMillis(),
                                                                             TimeUnit.MILLISECONDS);
                    waiterScheduler.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                }
                default -> throw new IllegalStateException(scenario.point().name());
            }
            settle();
            Counts before = counts();
            if (scenario.racer() == Racer.SUCCESS) {
                http.auto = this::answerScripted;
            }
            return before;
        }

        /** Invokes the call method and counts the terminal signals of the future it returns. */
        void start() throws Exception {
            future = switch (scenario.surface()) {
                case GENERATED -> startGenerated();
            };
            future.whenComplete((result, failure) -> terminalSignals.incrementAndGet());
        }

        private CompletableFuture<?> startGenerated() throws Exception {
            return switch (scenario.operation()) {
                case PLAIN -> call("getItem", client);
                case STREAMING_INPUT -> call("putBlob", client, requestBody);
                case STREAMING_OUTPUT -> call("getBlob", client, transformer);
                case WAITER -> call("waitUntilItemReady", client, waiterScheduler);
            };
        }

        /**
         * Cancels the call while the racer runs, both released by one barrier and skewed by spinning; true if a
         * cancellation succeeded.
         */
        boolean cancelRacing() throws Exception {
            if (scenario.racer() == Racer.NONE) {
                spin(scenario.cancelSpins());
                return future.cancel(true);
            }
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<Boolean> racing = racer.submit(() -> {
                barrier.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                spin(scenario.racerSpins());
                return race();
            });
            barrier.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            spin(scenario.cancelSpins());
            boolean cancelled = future.cancel(true);
            boolean raced;
            try {
                raced = racing.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("the racer failed", e.getCause());
            }
            return cancelled || raced;
        }

        /** The racing callback; true if it was a cancellation that succeeded. */
        private boolean race() throws Exception {
            if (scenario.racer() == Racer.CANCEL) {
                return future.cancel(true);
            }
            boolean success = scenario.racer() == Racer.SUCCESS;
            switch (scenario.point()) {
                // The driver, running ahead to the auto-answered transport, is the racer.
                case START -> { }
                case IDENTITY -> complete(credentials.held.join(), IDENTITY);
                case RETRY_DELAY -> complete(retryScheduler.held.join(), null);
                case WAITER_DELAY -> complete(waiterScheduler.held.join(), null);
                case TRANSPORT -> {
                    if (success) {
                        answerSuccess(held).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    } else {
                        fail(held, injected).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
                case REQUEST_BODY -> {
                    if (success) {
                        held.pullBody(REQUEST_CHUNKS.size() + 1);
                        // The rest of the body arrives, or the cancellation ends the exchange first.
                        await(() -> held.bodyComplete.get() || held.completion.isDone());
                        respond(held, 200, "{}").get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    } else {
                        fail(held, injected).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
                case RESPONSE_BODY -> heldStream.release();
                default -> throw new IllegalStateException(scenario.point().name());
            }
            return false;
        }

        /** Completes a held future with {@code value} for a succeeding racer, or with the injected failure. */
        private <T> void complete(CompletableFuture<T> pending, T value) {
            if (scenario.racer() == Racer.SUCCESS) {
                pending.complete(value);
            } else {
                pending.completeExceptionally(injected);
            }
        }

        /** Answers an exchange after the cancellation point with the scripted outcome, without blocking. */
        private void answerScripted(Exchange exchange) {
            if (scenario.racer() == Racer.FAILURE) {
                respond(exchange, 400, "{\"message\":\"rejected\"}");
            } else {
                answerSuccess(exchange);
            }
        }

        private Future<?> answerSuccess(Exchange exchange) {
            return switch (scenario.operation()) {
                case PLAIN, WAITER -> respond(exchange, 200, "{\"value\":\"READY\"}");
                case STREAMING_INPUT -> respond(exchange, 200, "{}");
                case STREAMING_OUTPUT -> respondStream(exchange, stream(-1, null));
            };
        }

        /** A response stream holding after {@code holdAfter} chunks (-1: never), failing with {@code failure}. */
        private ChunkStream stream(int holdAfter, Throwable failure) {
            ChunkStream stream = new ChunkStream(http.transport.eventLoop, holdAfter, failure);
            streams.add(stream);
            return stream;
        }

        /** As a transport does, fails the exchange and signals the handler only if the failure terminates it. */
        private Future<?> fail(Exchange exchange, Throwable failure) {
            return exchange.http.eventLoop.submit(() -> {
                if (exchange.completion.fail(failure)) {
                    transportFailed.set(true);
                    exchange.handler.onError(failure);
                }
            });
        }

        void quiesce() throws Exception {
            await(future::isDone);
            settle();
        }

        /** Drains the event loop and lets the executors run anything still queued. */
        void settle() throws Exception {
            for (int i = 0; i < 3; i++) {
                http.transport.eventLoop.submit(() -> { }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                Thread.sleep(10);
            }
        }

        Counts counts() {
            return new Counts(hooks.executions.get(), hooks.attempts.get(), credentials.resolutions.get(),
                              http.all.size(), retryScheduler.delays.size(), waiterScheduler.delays.size(),
                              hooks.afterExecutions.get());
        }

        Chunks chunks() {
            return new Chunks(requestBody.delivered.get(), streams.stream().mapToInt(s -> s.delivered.get()).sum());
        }

        /** The future's outcome: the scripted value or failure if the cancellation lost, its cancellation if it won. */
        void assertSurfaced(Status status) throws Exception {
            switch (status) {
                case CANCELLED -> assertThrows(CancellationException.class, () -> future.get(0, TimeUnit.SECONDS));
                case FAILED -> {
                    ExecutionException e = assertThrows(ExecutionException.class,
                                                        () -> future.get(0, TimeUnit.SECONDS));
                    Set<Throwable> reachable = reachable(e.getCause());
                    if (injected != null) {
                        assertTrue(reachable.contains(injected), "the scripted " + injected + " in " + reachable);
                    } else {
                        assertTrue(reachable.stream().anyMatch(t -> t instanceof AwsServiceException
                                                                    && t.getMessage().contains("400")),
                                   "the scripted 400 in " + reachable);
                    }
                }
                case SUCCEEDED -> {
                    Object result = future.get(0, TimeUnit.SECONDS);
                    switch (scenario.operation()) {
                        case PLAIN -> assertEquals("READY", call("value", result));
                        case WAITER -> assertEquals("READY", call("waiterValue", result));
                        case STREAMING_INPUT -> assertNotNull(result);
                        case STREAMING_OUTPUT -> assertEquals(String.join("", RESPONSE_CHUNKS), result);
                        default -> throw new IllegalStateException(scenario.operation().name());
                    }
                }
                default -> throw new AssertionError("the call did not end: " + scenario);
            }
        }

        /**
         * Every handle of the call has terminated. After a cancellation, an exchange is cancelled only for the caller
         * and a body is cancelled unless it was consumed (or its transport failed); with nothing racing, the held
         * handle itself was cancelled. A call that completed cancelled no exchange for the caller.
         */
        void assertHandles(boolean cancelled) {
            for (Exchange exchange : http.all) {
                assertTrue(exchange.completion.isDone(), "an open exchange after " + scenario);
                assertTrue(exchange.cancels.get() <= 1, "an exchange cancelled twice: " + exchange.reasons);
                if (!cancelled) {
                    assertFalse(exchange.reasons.contains(CancellationReason.CALLER_CANCELLED),
                                "the completed call cancelled an exchange: " + scenario);
                }
                // A cancellation racing a failing response may find the payload already abandoned
                // (RESPONSE_ABANDONED); with nothing racing, the reason is checked exactly below.
            }
            Stream.concat(retryScheduler.delays.stream(), waiterScheduler.delays.stream()).forEach(
                delay -> assertTrue(delay.isDone(), "a pending delay after " + scenario));
            assertTrue(transformer.exceptions.get() <= 1, "exceptionOccurred " + transformer.exceptions.get()
                                                          + " times");
            if (!cancelled) {
                return;
            }
            if (requestBody.requested.get() > 0) {
                assertTrue(requestBody.cancelled.get() || requestBody.delivered.get() == REQUEST_CHUNKS.size()
                           || transportFailed.get(), "the request body was abandoned: " + scenario);
            }
            for (ChunkStream stream : streams) {
                assertTrue(!stream.subscribed.get() || stream.cancelled.get() || stream.terminated.get(),
                           "a response stream was abandoned: " + scenario);
            }
            CompletableFuture<String> result = transformer.result;
            assertTrue(result == null || result.isDone(), "the transformer's result is pending: " + scenario);

            if (scenario.racer().completion() != null) {
                return;
            }
            switch (scenario.point()) {
                case START -> http.all.forEach(e -> assertEquals(1, e.cancels.get(), "the held exchange"));
                case TRANSPORT -> assertEquals(List.of(CancellationReason.CALLER_CANCELLED), held.reasons);
                case REQUEST_BODY -> {
                    assertEquals(List.of(CancellationReason.CALLER_CANCELLED), held.reasons);
                    assertTrue(requestBody.cancelled.get(), "the request body subscription");
                }
                case RESPONSE_BODY -> {
                    assertEquals(List.of(CancellationReason.CALLER_CANCELLED), held.reasons);
                    assertTrue(heldStream.cancelled.get(), "the response stream subscription");
                    assertEquals(1, transformer.exceptions.get(), "exceptionOccurred");
                    assertTrue(transformer.exception instanceof CancellationException,
                               "exceptionOccurred with " + transformer.exception);
                }
                case RETRY_DELAY -> assertTrue(retryScheduler.held.join().isCancelled(), "the retry delay");
                case WAITER_DELAY -> assertTrue(waiterScheduler.held.join().isCancelled(), "the waiter delay");
                case IDENTITY -> {
                    // The engine cancels only its copy of the provider's future; the stale identity proves it.
                }
                default -> throw new IllegalStateException(scenario.point().name());
            }
        }

        /** Late callbacks after the terminal transition: each must be dropped. */
        void staleCallbacks() throws Exception {
            credentials.held.thenAccept(pending -> pending.complete(IDENTITY));
            Stream.concat(retryScheduler.delays.stream(), waiterScheduler.delays.stream())
                  .forEach(delay -> delay.complete(null));
            if (held != null && scenario.point() == Point.REQUEST_BODY && !transportFailed.get()) {
                // The transport keeps requesting on its (cancelled or completed) request-body subscription. A
                // transport that failed its exchange stops reading the body, as a real one does.
                held.pullBody(REQUEST_CHUNKS.size() + 1);
            }
            for (Exchange exchange : http.all) {
                // A no-op for every terminated exchange.
                respond(exchange, 200, "{\"value\":\"stale\"}").get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
            streams.forEach(ChunkStream::release);
            Subscription subscription = transformer.subscription;
            if (subscription != null) {
                subscription.request(RESPONSE_CHUNKS.size());
            }
        }

        @Override
        public void close() throws Exception {
            // Unblock anything still held, so a failed try leaves nothing pending behind.
            credentials.held.thenAccept(pending -> pending.complete(IDENTITY));
            retryScheduler.held.thenAccept(pending -> pending.complete(null));
            waiterScheduler.held.thenAccept(pending -> pending.complete(null));
            streams.forEach(ChunkStream::release);
            try {
                client.close();
            } finally {
                http.transport.eventLoop.shutdownNow();
                executor.delegate.shutdownNow();
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Controlled stages

    /**
     * {@link ControlledHttp} recording every exchange it starts: each is held for the test, or with {@link #auto}
     * answered as soon as it starts. The calls of a try are sequential, so the exchange just queued by the transport is
     * the one this call started.
     */
    static final class RaceHttp implements SdkAsyncHttpClient {
        final ControlledHttp transport = new ControlledHttp();
        final List<Exchange> all = new CopyOnWriteArrayList<>();
        final BlockingQueue<Exchange> held = new LinkedBlockingQueue<>();
        volatile Consumer<Exchange> auto;

        Exchange next() throws InterruptedException {
            Exchange exchange = held.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull(exchange, "no exchange started");
            return exchange;
        }

        @Override
        public synchronized AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                                      HttpExecutionOptions options) {
            AsyncHttpExchange started = transport.execute(request, handler, options);
            Exchange exchange = transport.exchanges.poll();
            assertNotNull(exchange, "the transport queued no exchange");
            all.add(exchange);
            Consumer<Exchange> responder = auto;
            if (responder != null) {
                responder.accept(exchange);
            } else {
                held.add(exchange);
            }
            return started;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return transport.capabilities();
        }

        @Override
        public String clientName() {
            return transport.clientName();
        }

        @Override
        public void close() {
        }
    }

    /**
     * As a transport does, answers on the event loop only if the response terminates the exchange: a buffered
     * response completes it before its headers and body are signalled, so a racing cancellation either wins or
     * cancels nothing.
     */
    private static Future<?> respond(Exchange exchange, int status, String json) {
        return exchange.http.eventLoop.submit(() -> {
            if (!exchange.completion.complete()) {
                return;
            }
            SdkHttpResponse.Builder headers = SdkHttpResponse.builder().statusCode(status)
                                                             .addHeader("Content-Type", "application/json");
            if (status == 400) {
                headers.addHeader("X-Amzn-ErrorType", "Rejected");
            }
            exchange.handler.onHeaders(headers.body(HttpBody.empty()).build());
            exchange.handler.onStream(new LoopPublisher(exchange.http.eventLoop, json));
        });
    }

    /** Answers {@code 200} headers and a body stream that completes or fails the exchange when it terminates. */
    private static Future<?> respondStream(Exchange exchange, ChunkStream stream) {
        return exchange.http.eventLoop.submit(() -> {
            if (exchange.completion.isDone()) {
                return;
            }
            stream.onTerminal = failure -> {
                if (failure == null) {
                    exchange.completion.complete();
                } else {
                    exchange.completion.fail(failure);
                }
            };
            exchange.handler.onHeaders(SdkHttpResponse.builder().statusCode(200).addHeader("X-Kind", "k")
                                                      .addHeader("Content-Type", "application/octet-stream")
                                                      .body(HttpBody.empty()).build());
            exchange.handler.onStream(stream);
        });
    }

    /** Static credentials counting resolutions; the {@code holdAt}-th (1-based; 0: none) stays pending. */
    static final class CountingCredentials implements IdentityProvider<AwsCredentialsIdentity> {
        final CompletableFuture<CompletableFuture<AwsCredentialsIdentity>> held = new CompletableFuture<>();
        final AtomicInteger resolutions = new AtomicInteger();
        private final int holdAt;

        CountingCredentials(int holdAt) {
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

    /** A scheduler recording every delay; the {@code holdAt}-th (1-based; 0: none) stays pending, others elapse. */
    static final class RecordingScheduler implements RetryScheduler {
        final CompletableFuture<CompletableFuture<Void>> held = new CompletableFuture<>();
        final List<CompletableFuture<Void>> delays = new CopyOnWriteArrayList<>();
        private final AtomicInteger count = new AtomicInteger();
        private final int holdAt;

        RecordingScheduler(int holdAt) {
            this.holdAt = holdAt;
        }

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            CompletableFuture<Void> pending = new CompletableFuture<>();
            delays.add(pending);
            if (count.incrementAndGet() == holdAt) {
                held.complete(pending);
            } else {
                pending.complete(null);
            }
            return pending;
        }
    }

    /** Counts the calls, attempts and {@code afterExecution} hooks the pipeline runs. */
    static final class CountingInterceptor implements ExecutionInterceptor {
        final AtomicInteger executions = new AtomicInteger();
        final AtomicInteger attempts = new AtomicInteger();
        final AtomicInteger afterExecutions = new AtomicInteger();

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            executions.incrementAndGet();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            attempts.incrementAndGet();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            afterExecutions.incrementAndGet();
        }
    }

    /**
     * A response body on the event loop that, on demand, delivers {@code holdAfter} chunks (-1: all) and then holds
     * until released; after the release it delivers the rest and completes, or fails with {@code failure}. Records
     * its subscription, cancellation, termination and delivered chunks.
     */
    static final class ChunkStream implements Publisher<ByteBuffer> {
        final CompletableFuture<Void> reachedHold = new CompletableFuture<>();
        final AtomicBoolean subscribed = new AtomicBoolean();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean terminated = new AtomicBoolean();
        final AtomicInteger delivered = new AtomicInteger();
        volatile Consumer<Throwable> onTerminal = f -> { };
        private final ExecutorService eventLoop;
        private final int holdAfter;
        private final Throwable failure;
        // Confined to the event loop.
        private Subscriber<? super ByteBuffer> subscriber;
        private long demand;
        private boolean released;
        private boolean done;

        ChunkStream(ExecutorService eventLoop, int holdAfter, Throwable failure) {
            this.eventLoop = eventLoop;
            this.holdAfter = holdAfter;
            this.failure = failure;
            this.released = holdAfter < 0;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscribed.set(true);
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
                        cancelled.set(true);
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
                if (cancelled.get()) {
                    done = true;
                    return;
                }
                if (!released && delivered.get() == holdAfter) {
                    reachedHold.complete(null);
                    return;
                }
                if (released && failure != null) {
                    done = true;
                    terminated.set(true);
                    subscriber.onError(failure);
                    onTerminal.accept(failure);
                    return;
                }
                if (delivered.get() == RESPONSE_CHUNKS.size()) {
                    done = true;
                    terminated.set(true);
                    subscriber.onComplete();
                    onTerminal.accept(null);
                    return;
                }
                if (demand == 0) {
                    return;
                }
                demand--;
                String chunk = RESPONSE_CHUNKS.get(delivered.getAndIncrement());
                subscriber.onNext(ByteBuffer.wrap(chunk.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    /**
     * A response transformer requesting the whole payload, collecting it, and counting {@code exceptionOccurred}.
     */
    static final class CountingTransformer implements AsyncResponseTransformer<Object, String> {
        final AtomicInteger received = new AtomicInteger();
        final AtomicInteger exceptions = new AtomicInteger();
        final StringBuffer text = new StringBuffer();
        volatile Throwable exception;
        volatile Subscription subscription;
        volatile CompletableFuture<String> result;

        @Override
        public CompletableFuture<String> prepare() {
            result = new CompletableFuture<>();
            return result;
        }

        @Override
        public void onResponse(Object response) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            CompletableFuture<String> current = result;
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    text.append(StandardCharsets.UTF_8.decode(chunk.duplicate()));
                    received.incrementAndGet();
                }

                @Override
                public void onError(Throwable t) {
                    current.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    current.complete(text.toString());
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            exceptions.incrementAndGet();
            exception = error;
            CompletableFuture<String> current = result;
            if (current != null) {
                current.completeExceptionally(error);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Support

    /** Every throwable reachable from {@code root} through causes and suppressed exceptions, by identity. */
    private static Set<Throwable> reachable(Throwable root) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>(List.of(root));
        while (!pending.isEmpty()) {
            Throwable t = pending.pop();
            if (!seen.add(t)) {
                continue;
            }
            if (t.getCause() != null) {
                pending.push(t.getCause());
            }
            for (Throwable suppressed : t.getSuppressed()) {
                pending.push(suppressed);
            }
        }
        return seen;
    }

    private static void spin(int iterations) {
        for (int i = 0; i < iterations; i++) {
            Thread.onSpinWait();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T call(String name, Object... arguments) throws Exception {
        for (Method method : probe.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                try {
                    return (T) method.invoke(null, arguments);
                } catch (InvocationTargetException e) {
                    throw new AssertionError("CancellationProbe." + name + " failed", e.getCause());
                }
            }
        }
        throw new AssertionError("no probe method " + name);
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
