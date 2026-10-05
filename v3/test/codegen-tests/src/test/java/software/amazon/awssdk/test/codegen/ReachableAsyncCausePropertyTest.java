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
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.pipeline.InterceptorException;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.codegen.EarlyAsyncReturnPropertyTest.GateScheduler;
import software.amazon.awssdk.test.codegen.EarlyAsyncReturnPropertyTest.HeldCredentials;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.ControlledHttp;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Exchange;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.LoopPublisher;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.RecordingExecutor;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.Reply;
import software.amazon.awssdk.test.codegen.GeneratedAsyncBehaviorTest.SourcePublisher;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Property 22 (Requirement 8.11): async causal failures remain reachable.
 *
 * <p>A synthetic restJson1 service (a signed plain/waitable operation, an unsigned streaming-input operation, a
 * signed streaming-output operation and a signed paginated operation, each with a modeled {@code ItemLocked} error)
 * is generated and compiled once. Each try generates the async child stage that fails, the injected throwable, the
 * operation surface (plain call, streaming input, streaming output, paginator, waiter) and zero to two retried
 * attempts ({@code 500}) before the failure; a paginator may also deliver one page before the failing page. The
 * stages and their injections:
 * <ul>
 *   <li>identity: the credentials provider's identity future fails ({@link SdkClientException} or a raw
 *       {@link RuntimeException});</li>
 *   <li>interceptor: one of six generated hooks throws in the failing attempt (or call, for
 *       {@code beforeExecution});</li>
 *   <li>request body: the streaming request body publisher signals {@code onError} after zero to two chunks, which the
 *       transport reports as its failure;</li>
 *   <li>transport: the exchange fails ({@link IOException} or {@link HttpTransportException});</li>
 *   <li>response body: the response stream signals {@code onError} after zero to two chunks;</li>
 *   <li>response transformer: it throws in {@code onResponse} or {@code onStream}, or fails its future after the
 *       first chunk;</li>
 *   <li>modeled error: the service answers the modeled {@code ItemLocked} (no injected instance: the SDK creates
 *       it);</li>
 *   <li>retry delay: the retry scheduler's delay future fails.</li>
 * </ul>
 * The waiter's acceptors match only a successful {@code READY} poll, so every poll failure is one no acceptor matches.
 * The retry strategy allows exactly the attempts the try scripts, so a retryable injection ends the call.
 *
 * <p>Oracle: the surfaced failure (the future's failure, unwrapped from a {@link CompletionException} completion,
 * which must be the cause {@code get()} reports; or the paginator subscriber's {@code onError} throwable) is
 * traversed through causes and suppressed exceptions, and the injected instance is reachable by identity, with no
 * {@link CompletionException} or {@link ExecutionException} anywhere in that graph. The call-level failure (the
 * surfaced one, or for a waiter the cause of its single "no acceptor matched" {@link SdkClientException}) follows the
 * one mapping path of {@code CallStages.fail}: an injected {@link SdkException} is that very instance, anything else
 * is exactly one {@link SdkClientException} whose direct cause is the injected instance; a hook failure is exactly one
 * {@link InterceptorException} naming the hook, with the thrown instance as its direct cause (Requirement 11.13); a
 * modeled error is the generated {@code ItemLocked} itself. The future (or subscriber) fails exactly once, never
 * completes normally, a paginator delivers only the pages before the failure, and no exchange starts beyond the
 * scripted ones.
 *
 * <p>Call surfaces: {@link CallSurface} is the extension point for the Dynamic Client, as in
 * {@link EarlyAsyncReturnPropertyTest}. Only {@link CallSurface#GENERATED} exists today, because the Dynamic Client
 * (task 12) is not implemented yet; once it is, a {@code DYNAMIC} constant declaring the {@link Operation}s its async
 * entry point supports, with a branch in {@link Fixture#start}, joins the generator unchanged. The engine-level
 * mapping is covered by {@code InterceptorFailurePropertyTest} and {@code AwsCallEngineLifecycleTest} in
 * {@code aws-pipeline}; this property covers the generated async surfaces end to end over
 * {@link GeneratedAsyncBehaviorTest}'s transport.
 */
class ReachableAsyncCausePropertyTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.causes";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String LOCKED = "locked-marker";
    private static final List<String> REQUEST_CHUNKS = List.of("a", "b", "c");
    private static final List<String> RESPONSE_CHUNKS = List.of("x", "y", "z");

    private static final String MODEL = """
        $version: "2"
        namespace example.causes

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
            errors: [ItemLocked]
        }

        @error("client")
        @httpError(409)
        structure ItemLocked {
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
            errors: [ItemLocked]
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
            errors: [ItemLocked]
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
            errors: [ItemLocked]
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
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Executor;
        import org.reactivestreams.Publisher;
        import software.amazon.awssdk.core.async.AsyncRequestBody;
        import software.amazon.awssdk.core.async.AsyncResponseTransformer;
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
        import PKG.model.ListItemsRequest;
        import PKG.model.PutBlobRequest;
        import PKG.waiters.ItemsAsyncWaiter;

        public final class CauseProbe {
            private CauseProbe() {
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

            public static CompletableFuture<?> waitUntilItemReady(Object client, RetryScheduler scheduler) {
                ItemsAsyncWaiter waiter = ItemsAsyncWaiter.builder().client((ItemsAsyncClient) client)
                                                          .retryScheduler(scheduler).build();
                return waiter.waitUntilItemReady(r -> r.id("a"));
            }
        }
        """.replace("PKG", PACKAGE);

    private static URLClassLoader loader;
    private static Class<?> probe;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "reachable-async-cause")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Files.writeString(models.resolve("causes.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("example.causes#Items", PACKAGE, models, work.resolve("out")), List.of());
        Path file = Files.createDirectories(result.sourceRoot().resolve("example/probe")).resolve("CauseProbe.java");
        Files.writeString(file, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(result.sourceRoot(), work.resolve("classes")).loader();
        probe = Class.forName("example.probe.CauseProbe", true, loader);
    }

    @AfterContainer
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    void theInjectedAsyncFailureRemainsReachableThroughOneMappingPath(@ForAll("scenarios") Scenario scenario)
        throws Exception {
        Statistics.label("stage").collect(scenario.stage());
        Statistics.label("stage").coverage(c -> {
            for (Stage stage : Stage.values()) {
                c.check(stage).count(n -> n >= 5);
            }
        });
        Statistics.label("surface/operation").collect(scenario.surface(), scenario.operation());
        Statistics.label("injection").collect(scenario.injection());
        Statistics.label("retried first").collect(scenario.failedAttempts() > 0);

        try (Fixture fixture = new Fixture(scenario)) {
            fixture.start();
            fixture.drive();
            Throwable surfaced;
            try {
                surfaced = fixture.terminal.failure.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("the call did not fail: " + scenario, e.getCause());
            } catch (TimeoutException e) {
                throw new AssertionError("the call did not end within " + TIMEOUT + ": " + scenario);
            }
            fixture.settle();
            fixture.assertTerminatedOnce(surfaced);
            fixture.assertCause(surfaced);
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        // The stage is drawn first, so every stage is equally likely whatever the operations it applies to.
        return Arbitraries.of(Stage.values()).flatMap(stage -> Arbitraries.of(CallSurface.values()).flatMap(
            surface -> {
                Set<Operation> operations = EnumSet.copyOf(stage.operations());
                operations.retainAll(surface.operations());
                return Arbitraries.of(operations).flatMap(operation -> Arbitraries.of(stage.hooks()).flatMap(hook ->
                    Combinators.combine(
                        Arbitraries.of(stage.injections()),
                        Arbitraries.integers().between(0, maxRetries(operation, stage, hook)),
                        Arbitraries.integers().between(0, operation == Operation.PAGINATOR ? 1 : 0),
                        Arbitraries.of(stage.modes()),
                        Arbitraries.integers().between(0, stage.usesChunks() ? 2 : 0))
                        .as((injection, retries, pagesBefore, mode, chunks) ->
                                new Scenario(surface, operation, stage, injection, retries, pagesBefore, hook, mode,
                                             chunks))));
            }));
    }

    /**
     * A non-replayable request body is never retried, a once-per-call hook runs before any retry, and a failing retry
     * delay follows one more failed attempt; the standard strategy's three attempts bound the rest.
     */
    private static int maxRetries(Operation operation, Stage stage, Hook hook) {
        if (operation == Operation.STREAMING_INPUT || hook == Hook.BEFORE_EXECUTION) {
            return 0;
        }
        return stage == Stage.RETRY_DELAY ? 1 : 2;
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
        PLAIN, STREAMING_INPUT, STREAMING_OUTPUT, PAGINATOR, WAITER
    }

    /** The async child stage that fails. {@code PutBlob} is unsigned, so it has no identity stage. */
    enum Stage {
        IDENTITY(EnumSet.complementOf(EnumSet.of(Operation.STREAMING_INPUT)),
                 EnumSet.of(Injection.RAW_RUNTIME, Injection.SDK_CLIENT)),
        INTERCEPTOR(EnumSet.allOf(Operation.class), EnumSet.of(Injection.RAW_RUNTIME, Injection.SDK_CLIENT)),
        REQUEST_BODY(EnumSet.of(Operation.STREAMING_INPUT),
                     EnumSet.of(Injection.RAW_RUNTIME, Injection.SDK_CLIENT, Injection.IO)),
        TRANSPORT(EnumSet.allOf(Operation.class), EnumSet.of(Injection.IO, Injection.HTTP_TRANSPORT)),
        RESPONSE_BODY(EnumSet.of(Operation.STREAMING_OUTPUT), EnumSet.of(Injection.IO, Injection.HTTP_TRANSPORT)),
        TRANSFORMER(EnumSet.of(Operation.STREAMING_OUTPUT), EnumSet.of(Injection.RAW_RUNTIME, Injection.SDK_CLIENT)),
        MODELED_ERROR(EnumSet.allOf(Operation.class), EnumSet.of(Injection.NONE)),
        RETRY_DELAY(EnumSet.complementOf(EnumSet.of(Operation.STREAMING_INPUT)),
                    EnumSet.of(Injection.RAW_RUNTIME, Injection.SDK_CLIENT));

        private final Set<Operation> operations;
        private final Set<Injection> injections;

        Stage(Set<Operation> operations, Set<Injection> injections) {
            this.operations = operations;
            this.injections = injections;
        }

        Set<Operation> operations() {
            return operations;
        }

        Set<Injection> injections() {
            return injections;
        }

        Set<Hook> hooks() {
            return this == INTERCEPTOR ? EnumSet.complementOf(EnumSet.of(Hook.NONE)) : EnumSet.of(Hook.NONE);
        }

        Set<TransformerMode> modes() {
            return this == TRANSFORMER ? EnumSet.complementOf(EnumSet.of(TransformerMode.NONE))
                                       : EnumSet.of(TransformerMode.NONE);
        }

        boolean usesChunks() {
            return this == REQUEST_BODY || this == RESPONSE_BODY;
        }
    }

    /** The injected throwable; each try creates a fresh instance, so identity is meaningful. */
    enum Injection {
        RAW_RUNTIME, SDK_CLIENT, IO, HTTP_TRANSPORT, NONE;

        Throwable create(Stage stage) {
            String message = "injected at " + stage;
            return switch (this) {
                case RAW_RUNTIME -> new IllegalStateException(message);
                case SDK_CLIENT -> SdkClientException.create(message);
                case IO -> new IOException(message);
                case HTTP_TRANSPORT -> HttpTransportException.create(
                    stage == Stage.RESPONSE_BODY ? HttpPhase.RESPONSE_CONSUMPTION : HttpPhase.CONNECT,
                    HttpFailureCategory.Kind.IO, message, null);
                case NONE -> null;
            };
        }
    }

    /** The generated hook that throws; the per-attempt hooks after transmission need a received response. */
    enum Hook {
        NONE(null, false),
        BEFORE_EXECUTION("beforeExecution", false),
        BEFORE_ATTEMPT("beforeAttempt", false),
        AFTER_SERIALIZATION("afterSerialization", false),
        BEFORE_TRANSMISSION("beforeTransmission", false),
        AFTER_TRANSMISSION("afterTransmission", true),
        BEFORE_DESERIALIZATION("beforeDeserialization", true);

        private final String method;
        private final boolean transmitted;

        Hook(String method, boolean transmitted) {
            this.method = method;
            this.transmitted = transmitted;
        }
    }

    enum TransformerMode {
        NONE, THROW_ON_RESPONSE, THROW_ON_STREAM, FAIL_FUTURE
    }

    /**
     * One try. {@code chunks} is the number of body chunks moved before a request- or response-body {@code onError};
     * {@code pagesBefore} the number of paginator pages delivered before the failing page.
     */
    record Scenario(CallSurface surface, Operation operation, Stage stage, Injection injection, int retries,
                    int pagesBefore, Hook hook, TransformerMode mode, int chunks) {
        /** Attempts of the failing call answered {@code 500} first: a failing retry delay follows one more. */
        int failedAttempts() {
            return stage == Stage.RETRY_DELAY ? retries + 1 : retries;
        }

        /** The 1-based index, across the paginator's calls, of the attempt that fails. */
        int failingAttempt() {
            return pagesBefore + failedAttempts() + 1;
        }

        /** Whether the failing attempt starts an exchange. */
        boolean transmits() {
            return switch (stage) {
                case IDENTITY, RETRY_DELAY -> false;
                case INTERCEPTOR -> hook.transmitted;
                default -> true;
            };
        }

        int exchanges() {
            return pagesBefore + failedAttempts() + (transmits() ? 1 : 0);
        }

        @Override
        public String toString() {
            return surface + " " + operation + " failing at " + stage
                   + (hook == Hook.NONE ? "" : " " + hook.method)
                   + (mode == TransformerMode.NONE ? "" : " " + mode)
                   + (stage.usesChunks() ? " after " + chunks + " chunks" : "")
                   + " with " + injection + " after " + failedAttempts() + " failed attempts"
                   + (operation == Operation.PAGINATOR ? " and " + pagesBefore + " pages" : "");
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Fixture: one client over a controlled transport, with the scripted attempts and the injected failure

    private static final class Fixture implements AutoCloseable {
        private final Scenario scenario;
        private final Throwable injected;
        private final ControlledHttp http = new ControlledHttp();
        private final RecordingExecutor executor = new RecordingExecutor();
        private final HeldCredentials credentials;
        private final GateScheduler retryScheduler;
        private final FailingTransformer transformer;
        private final Publisher<ByteBuffer> requestBody;
        private final Terminal terminal = new Terminal();
        private final AutoCloseable client;
        private CompletableFuture<?> future;

        Fixture(Scenario scenario) throws Exception {
            this.scenario = scenario;
            this.injected = scenario.injection().create(scenario.stage());
            Stage stage = scenario.stage();
            this.credentials = new HeldCredentials(stage == Stage.IDENTITY ? scenario.failingAttempt() : 0);
            this.retryScheduler = new GateScheduler(stage == Stage.RETRY_DELAY ? scenario.failedAttempts() : 0);
            this.transformer = new FailingTransformer(scenario.mode(), injectedRuntime(Stage.TRANSFORMER));
            this.requestBody = stage == Stage.REQUEST_BODY
                               ? new FailingBody(scenario.chunks(), injected)
                               : new SourcePublisher(REQUEST_CHUNKS.toArray(String[]::new));
            FailingInterceptor interceptor = new FailingInterceptor(scenario.hook(), scenario.failingAttempt(),
                                                                    scenario.pagesBefore() + 1,
                                                                    injectedRuntime(Stage.INTERCEPTOR));
            StandardRetryStrategy strategy = StandardRetryStrategy.builder()
                                                                  .maxAttempts(scenario.failedAttempts() + 1)
                                                                  .build();
            this.client = call("client", http, executor, credentials, retryScheduler, strategy, interceptor);
        }

        private RuntimeException injectedRuntime(Stage stage) {
            return scenario.stage() == stage ? (RuntimeException) injected : null;
        }

        /** Invokes the call method and observes how the call ends. */
        void start() throws Exception {
            switch (scenario.surface()) {
                case GENERATED -> startGenerated();
                default -> throw new IllegalStateException(scenario.surface().name());
            }
        }

        private void startGenerated() throws Exception {
            switch (scenario.operation()) {
                case PLAIN -> observe(call("getItem", client));
                case STREAMING_INPUT -> observe(call("putBlob", client, requestBody));
                case STREAMING_OUTPUT -> observe(call("getBlob", client, transformer));
                case WAITER -> observe(call("waitUntilItemReady", client, (RetryScheduler) d -> CompletableFuture
                    .completedFuture(null)));
                case PAGINATOR -> {
                    ((Publisher<?>) call("listItems", client)).subscribe(terminal);
                    terminal.subscription.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                                         .request(scenario.pagesBefore() + 1);
                }
                default -> throw new IllegalStateException(scenario.operation().name());
            }
        }

        private void observe(CompletableFuture<?> call) {
            future = call;
            call.whenComplete((result, failure) -> {
                if (failure == null) {
                    terminal.onComplete();
                } else {
                    terminal.onError(failure);
                }
            });
        }

        /** Answers the pages before and the failed attempts, then injects the failure at the scenario's stage. */
        void drive() throws Exception {
            for (int i = 0; i < scenario.pagesBefore(); i++) {
                http.next().respond(Reply.json(200, "{\"nextToken\":\"t1\",\"items\":[\"i0\"]}"));
            }
            for (int i = 0; i < scenario.failedAttempts(); i++) {
                http.next().respond(Reply.json(500, "{\"message\":\"busy\"}"));
            }
            switch (scenario.stage()) {
                case IDENTITY -> credentials.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                                                 .completeExceptionally(injected);
                case RETRY_DELAY -> retryScheduler.held.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                                                       .completeExceptionally(injected);
                case INTERCEPTOR -> {
                    if (scenario.hook().transmitted) {
                        answerSuccess(http.next());
                    }
                }
                case TRANSPORT -> http.next().fail(injected);
                case MODELED_ERROR -> {
                    Exchange exchange = http.next();
                    drainRequestBody(exchange);
                    exchange.respond(new Reply(409, Map.of("Content-Type", "application/json",
                                                           "X-Amzn-ErrorType", "ItemLocked"),
                                               "{\"message\":\"" + LOCKED + "\"}", null));
                }
                case REQUEST_BODY -> forwardBodyFailure(http.next());
                case RESPONSE_BODY -> {
                    Exchange exchange = http.next();
                    answerStream(exchange, new FailingStream(http, exchange, scenario.chunks(), injected));
                }
                case TRANSFORMER -> answerSuccess(http.next());
                default -> throw new IllegalStateException(scenario.stage().name());
            }
        }

        private void answerSuccess(Exchange exchange) throws Exception {
            switch (scenario.operation()) {
                case PLAIN, WAITER -> exchange.respond(Reply.json(200, "{\"value\":\"READY\"}"));
                case STREAMING_INPUT -> {
                    drainRequestBody(exchange);
                    exchange.respond(Reply.json(200, "{}"));
                }
                case STREAMING_OUTPUT -> exchange.respond(
                    Reply.stream(new LoopPublisher(http.eventLoop, RESPONSE_CHUNKS.toArray(String[]::new))));
                case PAGINATOR -> exchange.respond(Reply.json(200, "{\"items\":[\"i1\"]}"));
                default -> throw new IllegalStateException(scenario.operation().name());
            }
        }

        private void drainRequestBody(Exchange exchange) throws Exception {
            if (scenario.operation() == Operation.STREAMING_INPUT) {
                exchange.pullBody(REQUEST_CHUNKS.size() + 1);
                await(exchange.bodyComplete::get);
            }
        }

        /** As a transport does, reads the request body and reports its {@code onError} as the exchange's failure. */
        private void forwardBodyFailure(Exchange exchange) throws Exception {
            Publisher<ByteBuffer> body = exchange.request.body().streamingBody();
            http.eventLoop.submit(() -> body.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                }

                @Override
                public void onError(Throwable t) {
                    exchange.handler.onError(t);
                    exchange.completion.fail(t);
                }

                @Override
                public void onComplete() {
                    AssertionError unexpected = new AssertionError("the failing request body completed");
                    exchange.handler.onError(unexpected);
                    exchange.completion.fail(unexpected);
                }
            })).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        /** Answers {@code 200} headers and the given body stream on the event loop. */
        private void answerStream(Exchange exchange, Publisher<ByteBuffer> stream) throws Exception {
            http.eventLoop.submit(() -> {
                exchange.handler.onHeaders(SdkHttpResponse.builder().statusCode(200).addHeader("X-Kind", "k")
                                                          .addHeader("Content-Type", "application/octet-stream")
                                                          .body(HttpBody.empty()).build());
                exchange.handler.onStream(stream);
            }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        /** Drains the event loop and lets the executors run anything still queued. */
        void settle() throws Exception {
            for (int i = 0; i < 2; i++) {
                http.eventLoop.submit(() -> { }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                Thread.sleep(5);
            }
        }

        void assertTerminatedOnce(Throwable surfaced) throws Exception {
            assertEquals(1, terminal.failures.get(), "failure signals of " + scenario);
            assertEquals(0, terminal.completions.get(), "normal completions of " + scenario);
            if (future != null) {
                assertTrue(future.isCompletedExceptionally(), "completed exceptionally");
                assertFalse(future.isCancelled(), "cancelled");
                ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(0, TimeUnit.SECONDS));
                assertSame(surfaced, e.getCause(), "get() reports the surfaced failure");
            } else {
                assertEquals(scenario.pagesBefore(), terminal.pages.size(), "pages before the failure");
            }
            assertEquals(scenario.exchanges(), http.calls.get(), "exchanges of " + scenario);
            assertTrue(http.exchanges.isEmpty(), "no exchange beyond the scripted ones");
        }

        void assertCause(Throwable surfaced) {
            String chain = describe(surfaced);
            Set<Throwable> reachable = reachable(surfaced);
            for (Throwable t : reachable) {
                assertFalse(t instanceof CompletionException || t instanceof ExecutionException,
                            "a wrapper-only failure in " + chain);
            }
            if (injected != null) {
                assertTrue(reachable.contains(injected), "the injected " + injected + " is unreachable from " + chain);
            }

            Throwable callFailure = surfaced;
            if (scenario.operation() == Operation.WAITER) {
                assertTrue(surfaced instanceof SdkClientException
                           && surfaced.getMessage().contains("no acceptor matched"),
                           "the waiter's no-acceptor failure: " + chain);
                callFailure = surfaced.getCause();
                assertNotNull(callFailure, "the waiter's cause: " + chain);
            }
            switch (scenario.stage()) {
                case MODELED_ERROR -> {
                    assertTrue(callFailure instanceof AwsServiceException
                               && callFailure.getClass().getSimpleName().startsWith("ItemLocked"),
                               "the modeled error itself: " + chain);
                    assertTrue(callFailure.getMessage().contains(LOCKED), chain);
                }
                case INTERCEPTOR -> {
                    assertTrue(callFailure instanceof InterceptorException, "one interceptor failure: " + chain);
                    assertSame(injected, callFailure.getCause(), "the thrown instance as its cause: " + chain);
                    assertEquals(scenario.hook().method, ((InterceptorException) callFailure).hook(), chain);
                }
                default -> {
                    if (injected instanceof SdkException) {
                        assertSame(injected, callFailure, "the injected SdkException itself: " + chain);
                    } else {
                        assertTrue(callFailure instanceof SdkClientException, "one SdkClientException: " + chain);
                        assertSame(injected, callFailure.getCause(), "the injected instance as its cause: " + chain);
                    }
                }
            }
        }

        @Override
        public void close() throws Exception {
            // Unblock anything still held, so a failed try leaves nothing pending behind.
            credentials.held.thenAccept(f -> f.completeExceptionally(new IllegalStateException("closed")));
            retryScheduler.held.thenAccept(f -> f.completeExceptionally(new IllegalStateException("closed")));
            try {
                client.close();
            } finally {
                http.eventLoop.shutdownNow();
                executor.delegate.shutdownNow();
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Failing stages

    /** Throws {@code failure} from {@code hook} in the failing attempt (or, for {@code beforeExecution}, call). */
    static final class FailingInterceptor implements ExecutionInterceptor {
        private final Hook hook;
        private final int failingAttempt;
        private final int failingExecution;
        private final RuntimeException failure;
        // Hooks run on one call's driver at a time: the calls of a try are sequential.
        private final AtomicInteger executions = new AtomicInteger();
        private final AtomicInteger attempts = new AtomicInteger();

        FailingInterceptor(Hook hook, int failingAttempt, int failingExecution, RuntimeException failure) {
            this.hook = hook;
            this.failingAttempt = failingAttempt;
            this.failingExecution = failingExecution;
            this.failure = failure;
        }

        private void maybeThrow(Hook current, int index, int failing) {
            if (current == hook && index == failing) {
                throw failure;
            }
        }

        private void inAttempt(Hook current) {
            maybeThrow(current, attempts.get(), failingAttempt);
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            maybeThrow(Hook.BEFORE_EXECUTION, executions.incrementAndGet(), failingExecution);
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            maybeThrow(Hook.BEFORE_ATTEMPT, attempts.incrementAndGet(), failingAttempt);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            inAttempt(Hook.AFTER_SERIALIZATION);
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            inAttempt(Hook.BEFORE_TRANSMISSION);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            inAttempt(Hook.AFTER_TRANSMISSION);
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            inAttempt(Hook.BEFORE_DESERIALIZATION);
        }
    }

    /** A request body that emits {@code chunks} chunks on demand, then signals {@code onError(failure)}. */
    static final class FailingBody implements Publisher<ByteBuffer> {
        private final int chunks;
        private final Throwable failure;

        FailingBody(int chunks, Throwable failure) {
            this.chunks = chunks;
            this.failure = failure;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private int delivered;
                private boolean done;

                @Override
                public void request(long n) {
                    for (long i = 0; i < n && !done && delivered < chunks; i++) {
                        delivered++;
                        subscriber.onNext(utf8("b" + delivered));
                    }
                    if (!done && delivered == chunks) {
                        done = true;
                        subscriber.onError(failure);
                    }
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        }
    }

    /**
     * A response body on the event loop that delivers {@code chunks} chunks on demand, then signals
     * {@code onError(failure)} and fails the exchange, as a transport does when the response stream breaks.
     */
    static final class FailingStream implements Publisher<ByteBuffer> {
        private final ControlledHttp http;
        private final Exchange exchange;
        private final int chunks;
        private final Throwable failure;

        FailingStream(ControlledHttp http, Exchange exchange, int chunks, Throwable failure) {
            this.http = http;
            this.exchange = exchange;
            this.chunks = chunks;
            this.failure = failure;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                // Confined to the event loop.
                private int delivered;
                private boolean done;

                @Override
                public void request(long n) {
                    http.eventLoop.execute(() -> {
                        for (long i = 0; i < n && !done && delivered < chunks; i++) {
                            subscriber.onNext(utf8(RESPONSE_CHUNKS.get(delivered++)));
                        }
                        if (!done && delivered == chunks) {
                            done = true;
                            subscriber.onError(failure);
                            exchange.completion.fail(failure);
                        }
                    });
                }

                @Override
                public void cancel() {
                    http.eventLoop.execute(() -> done = true);
                }
            });
        }
    }

    /**
     * A response transformer that, by {@code mode}, throws {@code failure} from {@code onResponse} or
     * {@code onStream}, or fails its future with it after the first chunk; with {@link TransformerMode#NONE} it
     * collects the body.
     */
    static final class FailingTransformer implements AsyncResponseTransformer<Object, String> {
        private final TransformerMode mode;
        private final RuntimeException failure;
        private final List<ByteBuffer> received = new CopyOnWriteArrayList<>();
        private volatile CompletableFuture<String> result;

        FailingTransformer(TransformerMode mode, RuntimeException failure) {
            this.mode = mode;
            this.failure = failure;
        }

        @Override
        public CompletableFuture<String> prepare() {
            result = new CompletableFuture<>();
            received.clear();
            return result;
        }

        @Override
        public void onResponse(Object response) {
            if (mode == TransformerMode.THROW_ON_RESPONSE) {
                throw failure;
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            if (mode == TransformerMode.THROW_ON_STREAM) {
                throw failure;
            }
            CompletableFuture<String> current = result;
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    s.request(mode == TransformerMode.FAIL_FUTURE ? 1 : Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    if (mode == TransformerMode.FAIL_FUTURE) {
                        subscription.cancel();
                        current.completeExceptionally(failure);
                        return;
                    }
                    received.add(chunk);
                }

                @Override
                public void onError(Throwable t) {
                    current.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    StringBuilder text = new StringBuilder();
                    received.forEach(c -> text.append(StandardCharsets.UTF_8.decode(c.duplicate())));
                    current.complete(text.toString());
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

    /**
     * Records how a call ended: the failure signals and normal completions of its future, or the pages, errors and
     * completions of a paginator subscription. {@link #failure} holds the first failure, or fails on a completion.
     */
    static final class Terminal implements Subscriber<Object> {
        final CompletableFuture<Subscription> subscription = new CompletableFuture<>();
        final CompletableFuture<Throwable> failure = new CompletableFuture<>();
        final List<Object> pages = new CopyOnWriteArrayList<>();
        final AtomicInteger failures = new AtomicInteger();
        final AtomicInteger completions = new AtomicInteger();

        @Override
        public void onSubscribe(Subscription s) {
            subscription.complete(s);
        }

        @Override
        public void onNext(Object page) {
            pages.add(page);
        }

        @Override
        public void onError(Throwable t) {
            failures.incrementAndGet();
            // A dependent completion wraps the failure in a CompletionException; get() reports its cause.
            failure.complete(t instanceof CompletionException && t.getCause() != null ? t.getCause() : t);
        }

        @Override
        public void onComplete() {
            completions.incrementAndGet();
            failure.completeExceptionally(new AssertionError("completed normally"));
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

    private static String describe(Throwable root) {
        StringBuilder text = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = root; t != null && seen.add(t); t = t.getCause()) {
            text.append(text.length() == 0 ? "" : " <- ").append(t.getClass().getName()).append(": ")
                .append(t.getMessage());
            for (Throwable suppressed : t.getSuppressed()) {
                text.append(" [suppressed ").append(suppressed).append(']');
            }
        }
        return text.toString();
    }

    private static ByteBuffer utf8(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static <T> T call(String name, Object... arguments) throws Exception {
        for (Method method : probe.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                try {
                    return (T) method.invoke(null, arguments);
                } catch (InvocationTargetException e) {
                    throw new AssertionError("CauseProbe." + name + " failed", e.getCause());
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
