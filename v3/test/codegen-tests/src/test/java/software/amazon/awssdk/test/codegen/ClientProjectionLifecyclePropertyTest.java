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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Property 30 (Requirements 10.4, 14.14): client projections have equivalent lifecycle.
 *
 * <p>A synthetic model with two SigV4 services sharing shapes, a restJson1 {@code Records} service (labels, query,
 * header and body bindings, a {@code 204} Unit output, a mixin) and an awsJson1_0 {@code Ledger} service, both with an
 * endpoint rule set bound to the region, the endpoint override and an input context parameter, and with modeled
 * client, throttling and retryable server errors, is generated and compiled once. Its JSON AST is loaded once as the
 * Dynamic Client's runtime model. Each try generates one finite call: the operation and its modeled input (strings,
 * integers, longs, doubles, booleans, timestamps, blobs, lists, maps, nested structures and enums; random member
 * subsets), the client configuration (region, endpoint override, retry attempts), a per-call override (header, raw
 * query parameter, credentials), up to three interceptors (observing, adding a header before signing, failing in a
 * chosen hook and attempt, or replacing the terminal failure), and a script of per-attempt replies (a modeled
 * success, a modeled error, an unmodeled retryable 5xx/throttle or non-retryable 4xx error, or a retryable or
 * non-retryable transport failure). It also generates transport mechanics that must not matter: the async completion
 * executor (caller-owned direct or SDK-owned), whether the async transport answers inline or on a fake event loop, and
 * how many chunks the async response body arrives in.
 *
 * <p>The call runs with identical configuration through the four {@link Projection}s: the generated sync client, the
 * generated async client, the Dynamic Client and the Dynamic Async Client. Each projection records a semantic trace:
 * every hook of every interceptor in order, each with the attempt number and the attempt's endpoint, auth scheme and
 * identity; the modeled request, the serialized, pre-signing and signed HTTP requests, the received response status and
 * headers, every attempt and terminal failure; every retry-strategy interaction with its normalized attempt failure and
 * decision; every exchange the transport received (method, URI, ordered headers and body); and the terminal outcome
 * (the modeled output as a {@link Document}, or the failure with its exception kind, modeled error shape and members,
 * status, error code, request ID and attempt count).
 *
 * <p>Normalization removes only transport mechanics and nondeterministic metadata: nothing is recorded about threads,
 * executors, timing or stack frames; the {@code CompletionException}/{@code ExecutionException} wrappers of async
 * futures are unwrapped; the signature-bearing {@code Authorization} header, the signing time {@code X-Amz-Date} and
 * the random {@code amz-sdk-invocation-id} are dropped (whether a request is signed is kept); request bodies and the
 * {@code x-amz-content-sha256} payload hash are compared byte for byte (generated types and the Dynamic Client both
 * write structure members in model declaration order); retry delays are zero
 * (no backoff) and not recorded; a streaming hook body is recorded as streaming, not read. Generated and dynamic
 * modeled exceptions are compared by their modeled error shape and member document rather than their Java class (the
 * Dynamic Client has no generated exception types), and outputs by their {@link Document} view.
 *
 * <p>Oracle: the generated sync trace is the reference; the other three traces must equal it. Operations with
 * {@code httpChecksum} are not generated here; Property 54 covers their wire equivalence.
 */
class ClientProjectionLifecyclePropertyTest {
    private static final String NAMESPACE = "example.projection";
    private static final String RECORDS_PACKAGE = "software.amazon.awssdk.test.codegen.projection.records";
    private static final String LEDGER_PACKAGE = "software.amazon.awssdk.test.codegen.projection.ledger";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String TRANSPORT_NAME = "ScriptedHttp";
    private static final String EVENT_LOOP = "projection-event-loop";
    private static final URI ENDPOINT_OVERRIDE = URI.create("https://override.example.test");
    private static final AwsCredentialsIdentity CLIENT_IDENTITY =
        AwsCredentialsIdentity.create("AKIDCLIENT", "client-secret-key");
    private static final AwsCredentialsIdentity OVERRIDE_IDENTITY =
        AwsCredentialsIdentity.create("AKIDOVERRIDE", "override-secret-key");
    /** Per-call nondeterministic header values: the signature, the signing time and the invocation ID. */
    private static final Set<String> NONDETERMINISTIC_HEADERS =
        Set.of("authorization", "x-amz-date", "amz-sdk-invocation-id");

    private static final String RULES = """
        @endpointRuleSet({
            version: "1.0"
            parameters: {
                Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
                Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
                Zone: { type: "string", required: false, documentation: "The zone." }
            }
            rules: [
                {
                    type: "endpoint"
                    conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
                    endpoint: { url: { ref: "Endpoint" } }
                }
                {
                    type: "endpoint"
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Zone" }] }
                        { fn: "isSet", argv: [{ ref: "Region" }] }
                    ]
                    endpoint: { url: "https://{Zone}.PREFIX.{Region}.example.test" }
                }
                {
                    type: "endpoint"
                    conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
                    endpoint: { url: "https://PREFIX.{Region}.example.test" }
                }
                { type: "error", conditions: [], error: "A region is required" }
            ]
        })
        """;

    private static final String MODEL = """
        $version: "2"
        namespace example.projection

        use aws.auth#sigv4
        use aws.protocols#awsJson1_0
        use aws.protocols#restJson1
        use smithy.rules#contextParam
        use smithy.rules#endpointRuleSet

        @restJson1
        @sigv4(name: "records")
        @aws.api#service(sdkId: "Records", endpointPrefix: "records")
        RECORDS_RULES
        service Records {
            version: "2024-01-01"
            operations: [PutRecord, GetRecord, DeleteRecord]
            errors: [Throttled, Unavailable]
        }

        @awsJson1_0
        @sigv4(name: "ledger")
        @aws.api#service(sdkId: "Ledger", endpointPrefix: "ledger")
        LEDGER_RULES
        service Ledger {
            version: "2024-01-01"
            operations: [Append, Describe]
            errors: [Throttled, Unavailable]
        }

        @http(method: "POST", uri: "/records/{id}")
        operation PutRecord {
            input := with [RecordFields] {
                @required
                @httpLabel
                id: String

                @httpQuery("limit")
                limit: Integer

                @httpHeader("X-Tag")
                tag: String

                @contextParam(name: "Zone")
                @httpHeader("X-Zone")
                zone: String
            }
            output := with [RecordFields] {
                @httpHeader("X-Version")
                version: Integer
            }
            errors: [NotFound, Conflict]
        }

        @readonly
        @http(method: "GET", uri: "/records/{id}")
        operation GetRecord {
            input := {
                @required
                @httpLabel
                id: String

                @httpQuery("v")
                version: Integer

                @httpQuery("tag")
                tags: TagList

                @contextParam(name: "Zone")
                @httpHeader("X-Zone")
                zone: String
            }
            output := with [RecordFields] {
                @httpHeader("X-Version")
                version: Integer
            }
            errors: [NotFound]
        }

        @idempotent
        @http(method: "DELETE", uri: "/records/{id}", code: 204)
        operation DeleteRecord {
            input := {
                @required
                @httpLabel
                id: String
            }
            output := {}
            errors: [NotFound, Conflict]
        }

        operation Append {
            input := {
                amount: Long

                @required
                entry: String

                tags: TagList
                nested: Nested
                when: Timestamp

                @contextParam(name: "Zone")
                zone: String
            }
            output := {
                sequence: Long
                accepted: Boolean
                tags: TagList
                created: Timestamp
                data: Blob
                attrs: AttrMap
                color: Color
            }
            errors: [NotFound, Conflict]
        }

        @readonly
        operation Describe {
            input := {}
            output := {
                name: String
                nested: Nested
            }
        }

        @mixin
        structure RecordFields {
            name: String
            count: Long
            ratio: Double
            enabled: Boolean
            created: Timestamp
            data: Blob
            tags: TagList
            attrs: AttrMap
            nested: Nested
            color: Color
        }

        structure Nested {
            label: String
            level: Integer
        }

        list TagList {
            member: String
        }

        map AttrMap {
            key: String
            value: String
        }

        enum Color {
            RED
            GREEN
            BLUE
        }

        @error("client")
        @httpError(404)
        structure NotFound {
            message: String
            id: String
        }

        @error("client")
        @httpError(409)
        structure Conflict {
            message: String
            reason: String
        }

        @error("client")
        @httpError(429)
        @retryable(throttling: true)
        structure Throttled {
            message: String
        }

        @error("server")
        @httpError(503)
        @retryable
        structure Unavailable {
            message: String
        }
        """.replace("RECORDS_RULES", RULES.replace("PREFIX", "records"))
           .replace("LEDGER_RULES", RULES.replace("PREFIX", "ledger"));

    /**
     * Thin adapters over the generated public sync and async APIs: builders configured exactly as the dynamic
     * builders (external configuration sources disabled through the build state, which the public builders do not
     * expose) and one typed call per operation, whose request is the generated type read from the input document.
     */
    private static final String PROBE = """
        package example.probe;

        import java.lang.reflect.Field;
        import java.net.URI;
        import java.util.List;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Executor;
        import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
        import software.amazon.awssdk.core.client.config.ConfigurationSources;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
        import software.amazon.awssdk.pipeline.ExecutionInterceptor;
        import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.retries.RetryStrategy;
        import software.amazon.smithy.java.core.serde.document.Document;
        import RPKG.RecordsAsyncClient;
        import RPKG.RecordsAsyncClientBuilder;
        import RPKG.RecordsClient;
        import RPKG.RecordsClientBuilder;
        import RPKG.model.DeleteRecordRequest;
        import RPKG.model.GetRecordRequest;
        import RPKG.model.PutRecordRequest;
        import LPKG.LedgerAsyncClient;
        import LPKG.LedgerAsyncClientBuilder;
        import LPKG.LedgerClient;
        import LPKG.LedgerClientBuilder;
        import LPKG.model.AppendRequest;
        import LPKG.model.DescribeRequest;

        public final class ProjectionProbe {
            private ProjectionProbe() {
            }

            public static AutoCloseable syncClient(String service, SdkSyncHttpClient http, Region region,
                                                   URI endpointOverride,
                                                   IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                                   RetryStrategy retryStrategy, List<ExecutionInterceptor> interceptors)
                throws Exception {
                if (service.equals("Records")) {
                    RecordsClientBuilder builder = RecordsClient.builder().region(region)
                        .credentialsProvider(credentials).retryStrategy(retryStrategy).httpClient(http);
                    if (endpointOverride != null) {
                        builder.endpointOverride(endpointOverride);
                    }
                    interceptors.forEach(builder::addExecutionInterceptor);
                    isolate(builder);
                    return builder.build();
                }
                LedgerClientBuilder builder = LedgerClient.builder().region(region)
                    .credentialsProvider(credentials).retryStrategy(retryStrategy).httpClient(http);
                if (endpointOverride != null) {
                    builder.endpointOverride(endpointOverride);
                }
                interceptors.forEach(builder::addExecutionInterceptor);
                isolate(builder);
                return builder.build();
            }

            public static AutoCloseable asyncClient(String service, SdkAsyncHttpClient http, Executor executor,
                                                    Region region, URI endpointOverride,
                                                    IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                                    RetryStrategy retryStrategy,
                                                    List<ExecutionInterceptor> interceptors) throws Exception {
                if (service.equals("Records")) {
                    RecordsAsyncClientBuilder builder = RecordsAsyncClient.builder().region(region)
                        .credentialsProvider(credentials).retryStrategy(retryStrategy).httpClient(http);
                    if (endpointOverride != null) {
                        builder.endpointOverride(endpointOverride);
                    }
                    if (executor != null) {
                        builder.asyncCompletionExecutor(executor);
                    }
                    interceptors.forEach(builder::addExecutionInterceptor);
                    isolate(builder);
                    return builder.build();
                }
                LedgerAsyncClientBuilder builder = LedgerAsyncClient.builder().region(region)
                    .credentialsProvider(credentials).retryStrategy(retryStrategy).httpClient(http);
                if (endpointOverride != null) {
                    builder.endpointOverride(endpointOverride);
                }
                if (executor != null) {
                    builder.asyncCompletionExecutor(executor);
                }
                interceptors.forEach(builder::addExecutionInterceptor);
                isolate(builder);
                return builder.build();
            }

            public static Object call(Object client, String operation, Document input,
                                      AwsRequestOverrideConfiguration override) {
                return switch (operation) {
                    case "PutRecord" -> ((RecordsClient) client)
                        .putRecord(input.asShape(PutRecordRequest.builder()), override);
                    case "GetRecord" -> ((RecordsClient) client)
                        .getRecord(input.asShape(GetRecordRequest.builder()), override);
                    case "DeleteRecord" -> ((RecordsClient) client)
                        .deleteRecord(input.asShape(DeleteRecordRequest.builder()), override);
                    case "Append" -> ((LedgerClient) client)
                        .append(input.asShape(AppendRequest.builder()), override);
                    case "Describe" -> ((LedgerClient) client)
                        .describe(input.asShape(DescribeRequest.builder()), override);
                    default -> throw new IllegalArgumentException(operation);
                };
            }

            public static CompletableFuture<?> callAsync(Object client, String operation, Document input,
                                                         AwsRequestOverrideConfiguration override) {
                return switch (operation) {
                    case "PutRecord" -> ((RecordsAsyncClient) client)
                        .putRecord(input.asShape(PutRecordRequest.builder()), override);
                    case "GetRecord" -> ((RecordsAsyncClient) client)
                        .getRecord(input.asShape(GetRecordRequest.builder()), override);
                    case "DeleteRecord" -> ((RecordsAsyncClient) client)
                        .deleteRecord(input.asShape(DeleteRecordRequest.builder()), override);
                    case "Append" -> ((LedgerAsyncClient) client)
                        .append(input.asShape(AppendRequest.builder()), override);
                    case "Describe" -> ((LedgerAsyncClient) client)
                        .describe(input.asShape(DescribeRequest.builder()), override);
                    default -> throw new IllegalArgumentException(operation);
                };
            }

            /** Disables the external configuration sources, as the dynamic builders' configurationSources(none()). */
            private static void isolate(Object builder) throws Exception {
                Field state = builder.getClass().getDeclaredField("state");
                state.setAccessible(true);
                ((AwsClientBuildState) state.get(builder)).configurationSources(ConfigurationSources.none());
            }
        }
        """.replace("RPKG", RECORDS_PACKAGE).replace("LPKG", LEDGER_PACKAGE);

    private static URLClassLoader loader;
    private static Method syncClient;
    private static Method asyncClient;
    private static Method call;
    private static Method callAsync;
    private static DynamicModel runtimeModel;
    private static ExecutorService eventLoop;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "client-projection")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Path modelFile = models.resolve("projection.smithy");
        Files.writeString(modelFile, MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult records = AwsCodegen.generate(
            new AwsCodegenRequest(NAMESPACE + "#Records", RECORDS_PACKAGE, models, work.resolve("records")), List.of());
        AwsCodegenResult ledger = AwsCodegen.generate(
            new AwsCodegenRequest(NAMESPACE + "#Ledger", LEDGER_PACKAGE, models, work.resolve("ledger")), List.of());
        // One source tree: both generated services and the probe compile and load together.
        Path sources = work.resolve("sources");
        copyTree(records.sourceRoot(), sources);
        copyTree(ledger.sourceRoot(), sources);
        Path probeFile = Files.createDirectories(sources.resolve("example/probe")).resolve("ProjectionProbe.java");
        Files.writeString(probeFile, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, work.resolve("classes")).loader();
        Class<?> probe = Class.forName("example.probe.ProjectionProbe", true, loader);
        syncClient = probe.getMethod("syncClient", String.class, SdkSyncHttpClient.class, Region.class, URI.class,
                                     IdentityProvider.class, RetryStrategy.class, List.class);
        asyncClient = probe.getMethod("asyncClient", String.class, SdkAsyncHttpClient.class, Executor.class,
                                      Region.class, URI.class, IdentityProvider.class, RetryStrategy.class,
                                      List.class);
        call = probe.getMethod("call", Object.class, String.class, Document.class,
                               AwsRequestOverrideConfiguration.class);
        callAsync = probe.getMethod("callAsync", Object.class, String.class, Document.class,
                                    AwsRequestOverrideConfiguration.class);
        runtimeModel = DynamicModel.fromJsonAst(jsonAst(modelFile), "projection.json");
        eventLoop = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, EVENT_LOOP);
            thread.setDaemon(true);
            return thread;
        });
    }

    @AfterContainer
    static void close() throws Exception {
        if (eventLoop != null) {
            eventLoop.shutdownNow();
        }
        if (loader != null) {
            loader.close();
        }
    }

    @Property(tries = 500, edgeCases = EdgeCasesMode.MIXIN)
    void everyProjectionHasTheGeneratedSyncLifecycle(@ForAll("scenarios") Scenario scenario) throws Exception {
        Trace reference = run(Projection.GENERATED_SYNC, scenario);
        Statistics.label("operation").collect(scenario.operation());
        Statistics.label("exchanges").collect(reference.exchanges.size());
        Statistics.label("terminal").collect(reference.terminalKind());
        assertTrue(reference.events.stream().anyMatch(e -> e.contains(":beforeExecution")),
                   "the reference call never entered the pipeline:\n" + reference.render());
        for (Projection projection : List.of(Projection.GENERATED_ASYNC, Projection.DYNAMIC_SYNC,
                                             Projection.DYNAMIC_ASYNC)) {
            Trace trace = run(projection, scenario);
            assertEquals(reference.render(), trace.render(),
                         projection + " diverges from " + Projection.GENERATED_SYNC + " for " + scenario);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Generators

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.of(Operation.values()).flatMap(operation -> Combinators.combine(
            operation.input(),
            script(operation),
            Arbitraries.frequency(Tuple.of(1, 1), Tuple.of(2, 2), Tuple.of(3, 3), Tuple.of(2, 4)),
            interceptors(),
            overrides(),
            Arbitraries.of(Region.US_EAST_1, Region.US_WEST_2, Region.EU_CENTRAL_1),
            Arbitraries.of(false, false, true),
            mechanics())
            .as((input, script, maxAttempts, interceptors, override, region, endpointOverride, mechanics) ->
                    new Scenario(operation, input, script, maxAttempts, interceptors, override,
                                 region, endpointOverride, mechanics)));
    }

    private static Arbitrary<List<Reply>> script(Operation operation) {
        Arbitrary<Reply> success = operation.output().map(Reply.Success::new);
        Arbitrary<Reply> modeled = Arbitraries.of(operation.errors()).flatMap(error -> error.errorMembers().map(
            members -> new Reply.ModeledError(error, members)));
        Arbitrary<Reply> unmodeled = Arbitraries.of(UnmodeledError.values()).map(Reply.UnmodeledError::new);
        Arbitrary<Reply> transport = Arbitraries.of(HttpPhase.CONNECT, HttpPhase.REQUEST_TRANSMISSION,
                                                    HttpPhase.TLS_HANDSHAKE)
                                                .map(Reply.TransportFailure::new);
        Arbitrary<Reply> reply = Arbitraries.frequencyOf(Tuple.of(3, success), Tuple.of(3, modeled),
                                                         Tuple.of(3, unmodeled), Tuple.of(2, transport));
        return reply.list().ofMinSize(1).ofMaxSize(4);
    }

    private static Arbitrary<List<InterceptorSpec>> interceptors() {
        Arbitrary<InterceptorSpec> observe = Arbitraries.just(new InterceptorSpec(Action.OBSERVE, "-", 0, "-"));
        Arbitrary<InterceptorSpec> header = token().map(v -> new InterceptorSpec(Action.ADD_HEADER,
                                                                                  "modifyBeforeSigning", 0, v));
        Arbitrary<InterceptorSpec> fail = Combinators.combine(Arbitraries.of(Hooks.FAILABLE),
                                                              Arbitraries.integers().between(1, 2))
                                                     .as((hook, attempt) -> new InterceptorSpec(Action.THROW, hook,
                                                                                                attempt, "-"));
        Arbitrary<InterceptorSpec> replace = Arbitraries.just(new InterceptorSpec(Action.REPLACE_FAILURE,
                                                                                  "modifyException", 0, "-"));
        return Arbitraries.frequencyOf(Tuple.of(3, observe), Tuple.of(3, header), Tuple.of(2, fail),
                                       Tuple.of(1, replace))
                          .list().ofMaxSize(3);
    }

    private static Arbitrary<OverrideSpec> overrides() {
        return Combinators.combine(token().injectNull(0.6), token().injectNull(0.6), Arbitraries.of(false, false, true))
                          .as(OverrideSpec::new);
    }

    private static Arbitrary<Mechanics> mechanics() {
        return Combinators.combine(Arbitraries.of(CompletionExecutor.values()), Arbitraries.of(Delivery.values()),
                                   Arbitraries.integers().between(1, 3))
                          .as(Mechanics::new);
    }

    /** A structure value: the required members always, each optional member with probability 0.6. */
    private static Arbitrary<Map<String, Object>> members(Member... members) {
        List<Arbitrary<Object>> values = new ArrayList<>();
        for (Member member : members) {
            Arbitrary<Object> value = member.values().map(v -> (Object) v);
            values.add(member.required() ? value : value.injectNull(0.4));
        }
        return Combinators.combine(values).as(list -> {
            Map<String, Object> out = new LinkedHashMap<>();
            for (int i = 0; i < members.length; i++) {
                if (list.get(i) != null) {
                    out.put(members[i].name(), list.get(i));
                }
            }
            return out;
        });
    }

    private static Member optional(String name, Arbitrary<?> values) {
        return new Member(name, values, false);
    }

    private static Member required(String name, Arbitrary<?> values) {
        return new Member(name, values, true);
    }

    /** Body text, including characters JSON escapes, non-ASCII and URI-reserved characters. */
    private static Arbitrary<String> text() {
        return Arbitraries.strings().withChars("abcXYZ019 _-\u00e9\"\\/").ofMaxLength(8);
    }

    /** Header- and query-safe tokens. */
    private static Arbitrary<String> token() {
        return Arbitraries.strings().withChars("abcxyz0129-").ofMinLength(1).ofMaxLength(8);
    }

    /** URI label values, including characters percent-encoding changes. */
    private static Arbitrary<String> label() {
        return Arbitraries.strings().withChars("abc019-_~ ").ofMinLength(1).ofMaxLength(8);
    }

    /** Valid host labels for the endpoint rules' zone. */
    private static Arbitrary<String> zone() {
        return Arbitraries.strings().withChars("abcdefxyz").ofMinLength(1).ofMaxLength(6);
    }

    private static Arbitrary<Integer> ints() {
        return Arbitraries.integers().between(-1000, 1000);
    }

    private static Arbitrary<Long> longs() {
        return Arbitraries.longs().between(-1_000_000_000_000L, 1_000_000_000_000L);
    }

    private static Arbitrary<Double> doubles() {
        return Arbitraries.of(0.0, 0.5, -1.25, 3.0, 1.0E10, 0.0025);
    }

    private static Arbitrary<Instant> instants() {
        return Combinators.combine(Arbitraries.longs().between(0, 4_000_000_000L),
                                   Arbitraries.integers().between(0, 999))
                          .as((seconds, millis) -> Instant.ofEpochSecond(seconds).plusMillis(millis));
    }

    private static Arbitrary<byte[]> blobs() {
        return Arbitraries.bytes().array(byte[].class).ofMaxSize(6);
    }

    private static Arbitrary<List<String>> tags() {
        return text().list().ofMaxSize(3);
    }

    private static Arbitrary<Map<String, String>> attrs() {
        return Arbitraries.maps(token(), text()).ofMaxSize(3).map(LinkedHashMap::new);
    }

    private static Arbitrary<Map<String, Object>> nested() {
        return members(optional("label", text()), optional("level", ints()));
    }

    private static Arbitrary<String> colors() {
        return Arbitraries.of("RED", "GREEN", "BLUE");
    }

    private static Member[] recordFields() {
        return new Member[] {
            optional("name", text()), optional("count", longs()), optional("ratio", doubles()),
            optional("enabled", Arbitraries.of(true, false)), optional("created", instants()),
            optional("data", blobs()), optional("tags", tags()), optional("attrs", attrs()),
            optional("nested", nested()), optional("color", colors())
        };
    }

    private static Member[] concat(Member[] first, Member... second) {
        Member[] out = new Member[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Scenario

    /** How a call is made; the generated sync client is the reference. */
    enum Projection {
        GENERATED_SYNC(false, false),
        GENERATED_ASYNC(true, false),
        DYNAMIC_SYNC(false, true),
        DYNAMIC_ASYNC(true, true);

        private final boolean async;
        private final boolean dynamic;

        Projection(boolean async, boolean dynamic) {
            this.async = async;
            this.dynamic = dynamic;
        }
    }

    enum Service {
        RECORDS("Records", true), LEDGER("Ledger", false);

        private final String name;
        private final boolean restJson;

        Service(String name, boolean restJson) {
            this.name = name;
            this.restJson = restJson;
        }

        ShapeId id() {
            return ShapeId.fromParts(NAMESPACE, name);
        }
    }

    enum ModeledError {
        NOT_FOUND("NotFound", 404), CONFLICT("Conflict", 409), THROTTLED("Throttled", 429),
        UNAVAILABLE("Unavailable", 503);

        private final String name;
        private final int status;

        ModeledError(String name, int status) {
            this.name = name;
            this.status = status;
        }

        Arbitrary<Map<String, Object>> errorMembers() {
            return switch (this) {
                case NOT_FOUND -> members(optional("message", text()), optional("id", token()));
                case CONFLICT -> members(optional("message", text()), optional("reason", text()));
                case THROTTLED, UNAVAILABLE -> members(optional("message", text()));
            };
        }
    }

    /** Errors that name no modeled shape: retryable server and throttling errors, and non-retryable client errors. */
    enum UnmodeledError {
        INTERNAL(500, "InternalFailure"), SERVICE_UNAVAILABLE(503, "ServiceUnavailable"),
        THROTTLING(400, "ThrottlingException"), ACCESS_DENIED(403, "AccessDeniedException"),
        VALIDATION(400, "ValidationException");

        private final int status;
        private final String code;

        UnmodeledError(int status, String code) {
            this.status = status;
            this.code = code;
        }
    }

    enum Operation {
        PUT_RECORD("PutRecord", Service.RECORDS, 200, List.of(ModeledError.NOT_FOUND, ModeledError.CONFLICT)),
        GET_RECORD("GetRecord", Service.RECORDS, 200, List.of(ModeledError.NOT_FOUND)),
        DELETE_RECORD("DeleteRecord", Service.RECORDS, 204, List.of(ModeledError.NOT_FOUND, ModeledError.CONFLICT)),
        APPEND("Append", Service.LEDGER, 200, List.of(ModeledError.NOT_FOUND, ModeledError.CONFLICT)),
        DESCRIBE("Describe", Service.LEDGER, 200, List.of());

        private final String name;
        private final Service service;
        private final int successStatus;
        private final List<ModeledError> errors;

        Operation(String name, Service service, int successStatus, List<ModeledError> operationErrors) {
            this.name = name;
            this.service = service;
            this.successStatus = successStatus;
            List<ModeledError> all = new ArrayList<>(operationErrors);
            all.add(ModeledError.THROTTLED);
            all.add(ModeledError.UNAVAILABLE);
            this.errors = List.copyOf(all);
        }

        List<ModeledError> errors() {
            return errors;
        }

        Arbitrary<Map<String, Object>> input() {
            return switch (this) {
                case PUT_RECORD -> members(concat(new Member[] {
                    required("id", label()), optional("limit", ints()), optional("tag", token()),
                    optional("zone", zone())}, recordFields()));
                case GET_RECORD -> members(required("id", label()), optional("version", ints()),
                                           optional("tags", token().list().ofMaxSize(2)),
                                           optional("zone", zone()));
                case DELETE_RECORD -> members(required("id", label()));
                case APPEND -> members(required("entry", text()), optional("amount", longs()),
                                       optional("tags", tags()), optional("nested", nested()),
                                       optional("when", instants()), optional("zone", zone()));
                case DESCRIBE -> Arbitraries.just(Map.of());
            };
        }

        /** The modeled success output as it is on the wire: header-bound members and the JSON body members. */
        Arbitrary<Wire> output() {
            return switch (this) {
                case PUT_RECORD, GET_RECORD -> members(concat(recordFields(), optional("version", ints()))).map(
                    body -> {
                        Map<String, Object> members = new LinkedHashMap<>(body);
                        Object version = members.remove("version");
                        Map<String, String> headers = new LinkedHashMap<>();
                        if (version != null) {
                            headers.put("X-Version", version.toString());
                        }
                        return new Wire(headers, json(members));
                    });
                case DELETE_RECORD -> Arbitraries.just(new Wire(Map.of(), ""));
                case APPEND -> members(optional("sequence", longs()), optional("accepted", Arbitraries.of(true, false)),
                                       optional("tags", tags()), optional("created", instants()),
                                       optional("data", blobs()), optional("attrs", attrs()),
                                       optional("color", colors()))
                    .map(body -> new Wire(Map.of(), json(body)));
                case DESCRIBE -> members(optional("name", text()), optional("nested", nested()))
                    .map(body -> new Wire(Map.of(), json(body)));
            };
        }
    }

    record Member(String name, Arbitrary<?> values, boolean required) {
    }

    /** Header-bound response members and the response body text. */
    record Wire(Map<String, String> headers, String body) {
    }

    /** One scripted reply of the transport to one attempt. */
    sealed interface Reply {
        record Success(Wire wire) implements Reply {
        }

        record ModeledError(ClientProjectionLifecyclePropertyTest.ModeledError error, Map<String, Object> members)
            implements Reply {
        }

        record UnmodeledError(ClientProjectionLifecyclePropertyTest.UnmodeledError error) implements Reply {
        }

        record TransportFailure(HttpPhase phase) implements Reply {
        }
    }

    enum Action {
        /** Records its hooks only. */
        OBSERVE,
        /** Adds {@code X-Probe-<index>} in {@code modifyBeforeSigning}. */
        ADD_HEADER,
        /** Throws in {@code hook} (in attempt {@code attempt}, for an attempt hook). */
        THROW,
        /** Replaces the terminal failure in {@code modifyException}. */
        REPLACE_FAILURE
    }

    record InterceptorSpec(Action action, String hook, int attempt, String value) {
    }

    /** The per-call override: a header, a raw query parameter and override credentials, each optional. */
    record OverrideSpec(String header, String query, boolean credentials) {
        AwsRequestOverrideConfiguration build() {
            if (header == null && query == null && !credentials) {
                return null;
            }
            AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
            if (header != null) {
                builder.putHeader("X-Override", header);
            }
            if (query != null) {
                builder.putRawQueryParameter("override", query);
            }
            if (credentials) {
                builder.credentialsProvider(new StaticCredentials(OVERRIDE_IDENTITY));
            }
            return builder.build();
        }
    }

    enum CompletionExecutor {
        CALLER_DIRECT, SDK_OWNED
    }

    enum Delivery {
        INLINE, EVENT_LOOP
    }

    /** Async transport mechanics, which every trace normalizes away. */
    record Mechanics(CompletionExecutor executor, Delivery delivery, int chunks) {
    }

    record Scenario(Operation operation, Map<String, Object> input, List<Reply> script, int maxAttempts,
                    List<InterceptorSpec> interceptors, OverrideSpec override, Region region,
                    boolean endpointOverride, Mechanics mechanics) {
    }

    // -------------------------------------------------------------------------------------------------------------
    // One projection's call

    private static Trace run(Projection projection, Scenario scenario) throws Exception {
        Trace trace = new Trace();
        ScriptedHttp http = new ScriptedHttp(projection.async ? HttpTransportMode.ASYNC : HttpTransportMode.SYNC,
                                             scenario, trace);
        List<ExecutionInterceptor> interceptors = new ArrayList<>();
        for (int i = 0; i < scenario.interceptors().size(); i++) {
            interceptors.add(new ScriptedInterceptor(i + 1, scenario.interceptors().get(i), trace));
        }
        interceptors.add(new Recorder(trace));
        RetryStrategy retryStrategy = new RecordingRetryStrategy(
            StandardRetryStrategy.builder().maxAttempts(scenario.maxAttempts())
                                 .backoffStrategy(BackoffStrategy.none())
                                 .throttlingBackoffStrategy(BackoffStrategy.none()).build(), trace);
        Executor executor = scenario.mechanics().executor() == CompletionExecutor.CALLER_DIRECT ? Runnable::run
                                                                                                 : null;
        Operation operation = scenario.operation();
        Service service = operation.service;
        URI endpoint = scenario.endpointOverride() ? ENDPOINT_OVERRIDE : null;
        StaticCredentials credentials = new StaticCredentials(CLIENT_IDENTITY);
        AwsRequestOverrideConfiguration override = scenario.override().build();
        Document input = toDocument(scenario.input());

        try (AutoCloseable client = projection.dynamic
                                    ? dynamicClient(projection, service, http, executor, scenario, endpoint,
                                                    credentials, retryStrategy, interceptors)
                                    : projection.async
                                      ? (AutoCloseable) invoke(asyncClient, service.name, http, executor,
                                                               scenario.region(), endpoint, credentials, retryStrategy,
                                                               interceptors)
                                      : (AutoCloseable) invoke(syncClient, service.name, http, scenario.region(),
                                                               endpoint, credentials, retryStrategy, interceptors)) {
            Object result;
            try {
                if (projection.async) {
                    result = await(projection.dynamic ? callDynamicAsync((DynamicAsyncClient) client, operation.name,
                                                                         input, override)
                                                      : (CompletableFuture<?>) invoke(callAsync, client,
                                                                                      operation.name, input,
                                                                                      override));
                } else if (projection.dynamic) {
                    try {
                        result = ((DynamicClient) client).call(operation.name, input, override);
                    } catch (RuntimeException e) {
                        throw new TerminalFailure(e);
                    }
                } else {
                    result = invoke(call, client, operation.name, input, override);
                }
            } catch (TerminalFailure failure) {
                trace.terminal = "failure " + failure(failure.getCause());
                return trace;
            }
            // Both outputs are structures (a generated type or the dynamic schema-guided document): both are viewed
            // through the same Document conversion the hooks use.
            trace.terminal = "success " + render(Document.of((SerializableStruct) result));
        }
        return trace;
    }

    private static AutoCloseable dynamicClient(Projection projection, Service service, ScriptedHttp http,
                                               Executor executor, Scenario scenario, URI endpoint,
                                               StaticCredentials credentials, RetryStrategy retryStrategy,
                                               List<ExecutionInterceptor> interceptors) {
        if (projection.async) {
            DynamicAsyncClient.Builder builder = DynamicAsyncClient.builder().model(runtimeModel)
                .serviceId(service.id()).region(scenario.region()).credentialsProvider(credentials)
                .retryStrategy(retryStrategy).configurationSources(ConfigurationSources.none()).httpClient(http);
            if (endpoint != null) {
                builder.endpointOverride(endpoint);
            }
            if (executor != null) {
                builder.asyncCompletionExecutor(executor);
            }
            interceptors.forEach(builder::addExecutionInterceptor);
            return builder.build();
        }
        DynamicClient.Builder builder = DynamicClient.builder().model(runtimeModel)
            .serviceId(service.id()).region(scenario.region()).credentialsProvider(credentials)
            .retryStrategy(retryStrategy).configurationSources(ConfigurationSources.none()).httpClient(http);
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        interceptors.forEach(builder::addExecutionInterceptor);
        return builder.build();
    }

    /** The terminal failure of a call, as the caller observes it: the sync exception or the future's cause. */
    private static final class TerminalFailure extends Exception {
        private static final long serialVersionUID = 1L;

        TerminalFailure(Throwable cause) {
            super(cause);
        }
    }

    /** Invokes a probe method; a failure of the call itself is the terminal failure, not a test failure. */
    private static Object invoke(Method method, Object... arguments) throws TerminalFailure {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            throw new TerminalFailure(e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** A failure of the async call method itself (rather than of its future) is the terminal failure too. */
    private static CompletableFuture<?> callDynamicAsync(DynamicAsyncClient client, String operation, Document input,
                                                         AwsRequestOverrideConfiguration override)
        throws TerminalFailure {
        try {
            return client.callAsync(operation, input, override);
        } catch (RuntimeException e) {
            throw new TerminalFailure(e);
        }
    }

    private static Object await(CompletableFuture<?> future) throws TerminalFailure, InterruptedException {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new TerminalFailure(e.getCause());
        } catch (TimeoutException e) {
            throw new AssertionError("the async call did not complete within " + TIMEOUT);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Trace and normalization

    /** One projection's normalized semantic trace. */
    static final class Trace {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<String> exchanges = new CopyOnWriteArrayList<>();
        volatile String terminal = "none";

        String terminalKind() {
            if (terminal.startsWith("success")) {
                return "success";
            }
            int brace = terminal.indexOf('{');
            int colon = terminal.indexOf(':');
            int end = brace < 0 ? colon : colon < 0 ? brace : Math.min(brace, colon);
            return end < 0 ? terminal : terminal.substring(0, end);
        }

        String render() {
            return String.join("\n", events) + "\n-- exchanges\n" + String.join("\n", exchanges)
                   + "\n-- terminal\n" + terminal;
        }
    }

    /** The canonical text of a document: its shape type and value, structure members by name, entries in order. */
    static String render(Document document) {
        if (document == null) {
            return "null";
        }
        return switch (document.type()) {
            case STRUCTURE, UNION, MAP -> {
                StringBuilder out = new StringBuilder(document.type().toString()).append('{');
                String separator = "";
                // Structure members are unordered in the Smithy data model, and a Smithy Java document iterates them
                // in schema member-index order (required members first) or in serialization order, depending on its
                // origin; the wire order of members is compared on the transmitted bodies instead. Map entries keep
                // their order.
                Map<String, Document> members = document.type() == ShapeType.MAP
                                                ? document.asStringMap()
                                                : new TreeMap<>(document.asStringMap());
                for (Map.Entry<String, Document> member : members.entrySet()) {
                    out.append(separator).append(member.getKey()).append('=').append(render(member.getValue()));
                    separator = ", ";
                }
                yield out.append('}').toString();
            }
            case LIST, SET -> {
                List<String> items = new ArrayList<>();
                for (Document item : document.asList()) {
                    items.add(render(item));
                }
                yield document.type() + items.toString();
            }
            case BLOB -> "BLOB:" + Base64.getEncoder().encodeToString(bytes(document.asBlob()));
            case TIMESTAMP -> "TIMESTAMP:" + document.asTimestamp();
            case STRING, ENUM -> document.type() + ":" + document.asString();
            case BOOLEAN -> "BOOLEAN:" + document.asBoolean();
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL ->
                document.type() + ":" + document.asNumber();
            default -> document.type() + ":" + document;
        };
    }

    /**
     * The canonical text of a failure and its causes. Service exceptions are compared by their AWS error data and
     * modeled error document, not their class; async {@code CompletionException} wrappers are skipped.
     */
    static String failure(Throwable failure) {
        StringBuilder out = new StringBuilder();
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 8; depth++) {
            if (current instanceof CompletionException && current.getCause() != null) {
                current = current.getCause();
                continue;
            }
            if (out.length() > 0) {
                out.append(" <- ");
            }
            if (current instanceof AwsServiceException service) {
                AwsErrorDetails details = service.awsErrorDetails();
                out.append("AwsServiceException{status=").append(service.statusCode())
                   .append(", code=").append(details == null ? null : details.errorCode())
                   .append(", errorMessage=").append(details == null ? null : details.errorMessage())
                   .append(", service=").append(details == null ? null : details.serviceName())
                   .append(", requestId=").append(service.requestId())
                   .append(", attempts=").append(service.numAttempts())
                   .append(", retryable=").append(service.retryable())
                   .append(", throttling=").append(service.isThrottlingException());
                if (current instanceof SerializableStruct modeled) {
                    out.append(", modeled=").append(modeled.schema().id()).append(' ')
                       .append(render(Document.of(modeled)));
                } else {
                    out.append(", unmodeled");
                }
                out.append(", message=").append(service.getMessage()).append('}');
            } else {
                out.append(current.getClass().getName()).append(": ").append(current.getMessage());
                if (current instanceof SdkException sdk) {
                    out.append(" {attempts=").append(sdk.numAttempts()).append(", retryable=")
                       .append(sdk.retryable()).append('}');
                }
                if (current instanceof HttpTransportException transport) {
                    out.append(" {category=").append(transport.category()).append(", phase=")
                       .append(transport.phase()).append('}');
                }
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return out.toString();
    }

    /** The text of an HTTP request: method, URI, ordered deterministic headers and the transmitted body. */
    static String request(SdkHttpRequest request, boolean readStream) {
        byte[] bytes = switch (request.body().kind()) {
            case EMPTY -> new byte[0];
            case BYTES -> request.body().asByteArray();
            case STREAMING -> readStream ? request.body().buffered().asByteArray() : null;
        };
        String body = bytes == null ? "<streaming>" : new String(bytes, StandardCharsets.UTF_8);
        return request.method() + " " + request.scheme() + "://" + request.authority() + request.rawTarget() + " "
               + headers(request.headers()) + " body=" + body;
    }

    static String response(SdkHttpResponse response) {
        return response.statusCode() + " " + headers(response.headers());
    }

    static String headers(HttpHeaders headers) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            if (!NONDETERMINISTIC_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                out.add(name + ": " + headers.value(i));
            }
        }
        return out.toString();
    }

    static String attempt(HookContext context) {
        if (!(context instanceof HookContext.Attempt attempt)) {
            return "";
        }
        return " [attempt=" + attempt.attemptNumber()
               + " endpoint=" + attempt.endpoint().map(e -> e.url().toString()).orElse("-")
               + " auth=" + attempt.authScheme().map(s -> s.scheme().schemeId()).orElse("-")
               + " identity=" + attempt.identity().map(ClientProjectionLifecyclePropertyTest::identity).orElse("-")
               + "]";
    }

    static String identity(Identity identity) {
        return identity instanceof AwsCredentialsIdentity credentials ? credentials.accessKeyId()
                                                                      : identity.getClass().getSimpleName();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] out = new byte[view.remaining()];
        view.get(out);
        return out;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Values: documents of the input, JSON of the response bodies

    @SuppressWarnings("unchecked")
    static Document toDocument(Object value) {
        if (value instanceof String s) {
            return Document.of(s);
        } else if (value instanceof Byte b) {
            return Document.of(b.byteValue());
        } else if (value instanceof Short s) {
            return Document.of(s.shortValue());
        } else if (value instanceof Integer i) {
            return Document.of(i.intValue());
        } else if (value instanceof Long l) {
            return Document.of(l.longValue());
        } else if (value instanceof Float f) {
            return Document.of(f.floatValue());
        } else if (value instanceof Double d) {
            return Document.of(d.doubleValue());
        } else if (value instanceof java.math.BigInteger b) {
            return Document.of(b);
        } else if (value instanceof BigDecimal b) {
            return Document.of(b);
        } else if (value instanceof Boolean b) {
            return Document.of(b.booleanValue());
        } else if (value instanceof Instant t) {
            return Document.of(t);
        } else if (value instanceof byte[] b) {
            return Document.of(b);
        } else if (value instanceof List<?> list) {
            List<Document> out = new ArrayList<>();
            list.forEach(item -> out.add(toDocument(item)));
            return Document.of(out);
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Document> out = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((k, v) -> out.put(k, toDocument(v)));
            return Document.of(out);
        }
        throw new IllegalArgumentException(String.valueOf(value));
    }

    static String json(Object value) {
        if (value instanceof String s) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            return out.append('"').toString();
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Instant t) {
            return BigDecimal.valueOf(t.toEpochMilli(), 3).toPlainString();
        } else if (value instanceof byte[] b) {
            return json(Base64.getEncoder().encodeToString(b));
        } else if (value instanceof List<?> list) {
            List<String> items = new ArrayList<>();
            list.forEach(item -> items.add(json(item)));
            return "[" + String.join(",", items) + "]";
        } else if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            map.forEach((k, v) -> entries.add(json(k) + ":" + json(v)));
            return "{" + String.join(",", entries) + "}";
        }
        throw new IllegalArgumentException(String.valueOf(value));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Interceptors, retry strategy, credentials and the scripted transport

    /** The hooks a {@link Action#THROW} interceptor may fail in. */
    static final class Hooks {
        static final List<String> FAILABLE = List.of("beforeExecution", "modifyRequest", "beforeAttempt",
                                                     "afterSerialization", "modifyBeforeSigning",
                                                     "beforeTransmission", "afterTransmission",
                                                     "beforeDeserialization", "afterAttempt", "modifyResponse",
                                                     "afterExecution");

        private Hooks() {
        }
    }

    /** Records every hook it sees as {@code <index>:<hook> [attempt...]} and performs its scripted action. */
    static final class ScriptedInterceptor implements ExecutionInterceptor {
        private final int index;
        private final InterceptorSpec spec;
        private final Trace trace;

        ScriptedInterceptor(int index, InterceptorSpec spec, Trace trace) {
            this.index = index;
            this.spec = spec;
            this.trace = trace;
        }

        private void hook(String hook, HookContext context) {
            trace.events.add(index + ":" + hook + attempt(context));
            if (spec.action() == Action.THROW && spec.hook().equals(hook)
                && (!(context instanceof HookContext.Attempt a) || a.attemptNumber() == spec.attempt())) {
                throw new IllegalStateException("interceptor " + index + " failed in " + hook);
            }
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            hook("beforeExecution", context);
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            hook("modifyRequest", context);
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            hook("beforeAttempt", context);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            hook("afterSerialization", context);
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            hook("modifyBeforeSigning", context);
            if (spec.action() == Action.ADD_HEADER) {
                return context.httpRequest().toBuilder().putHeader("X-Probe-" + index, spec.value()).build();
            }
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            hook("modifyAfterSigning", context);
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            hook("beforeTransmission", context);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            hook("afterTransmission", context);
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            hook("modifyHttpResponse", context);
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            hook("modifyResponseBody", context);
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            hook("beforeDeserialization", context);
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            hook("modifyAttemptException", context);
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            hook("afterAttempt", context);
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            hook("modifyResponse", context);
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            hook("modifyException", context);
            if (spec.action() == Action.REPLACE_FAILURE) {
                return new IllegalStateException("replaced by interceptor " + index, context.failure());
            }
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            hook("afterExecution", context);
        }
    }

    /**
     * The last interceptor: records every hook with the attempt's resolved endpoint, auth scheme and identity, and the
     * hook's semantic values (modeled request and response, HTTP requests and responses, failures).
     */
    static final class Recorder implements ExecutionInterceptor {
        private final Trace trace;

        Recorder(Trace trace) {
            this.trace = trace;
        }

        private void record(String hook, HookContext context, String detail) {
            trace.events.add("recorder:" + hook + attempt(context) + (detail.isEmpty() ? "" : " " + detail));
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            record("beforeExecution", context, "operation=" + context.operationSchema().id() + " request="
                                               + render(Document.of(context.request())));
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            record("modifyRequest", context, "request=" + render(Document.of(context.request())));
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            record("beforeAttempt", context, "");
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            record("afterSerialization", context, request(context.httpRequest(), false));
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            record("modifyBeforeSigning", context, request(context.httpRequest(), false));
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            record("modifyAfterSigning", context, signed(context.signedRequest()));
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            record("beforeTransmission", context, signed(context.signedRequest()));
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            record("afterTransmission", context,
                   context.httpResponse().map(r -> "response=" + response(r))
                          .orElseGet(() -> "failure=" + failure(context.failure().orElse(null))));
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            record("modifyHttpResponse", context, response(context.httpResponse()));
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            // The body's representation (buffered or streaming) is a transport mechanic; its bytes reach the
            // deserializer and so the recorded outputs and failures.
            record("modifyResponseBody", context, response(context.httpResponse()));
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            record("beforeDeserialization", context, response(context.httpResponse()));
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            record("modifyAttemptException", context, "failure=" + failure(context.failure()));
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            record("afterAttempt", context, outcome(context.response(), context.failure()));
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            record("modifyResponse", context, "response=" + render(Document.of(context.response())));
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            record("modifyException", context, "failure=" + failure(context.failure()));
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            record("afterExecution", context, outcome(context.response(), context.failure()));
        }

        private static String signed(SignedRequest signed) {
            return request(signed.request(), false) + " signed=" + signed.signatureValue().isPresent();
        }

        private static String outcome(Optional<SerializableStruct> response, Optional<Throwable> failure) {
            return response.map(r -> "response=" + render(Document.of(r)))
                           .orElseGet(() -> "failure=" + failure(failure.orElse(null)));
        }
    }

    /** The standard strategy without backoff, recording every interaction with its normalized inputs and decision. */
    static final class RecordingRetryStrategy implements RetryStrategy {
        private final RetryStrategy delegate;
        private final Trace trace;

        RecordingRetryStrategy(RetryStrategy delegate, Trace trace) {
            this.delegate = delegate;
            this.trace = trace;
        }

        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            trace.events.add("retry:acquireInitialToken");
            return delegate.acquireInitialToken(request);
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            RetryDecision decision = delegate.refreshRetryToken(token, failure);
            String outcome = decision instanceof RetryDecision.Stop stop
                             ? "stop " + stop.reason() + " " + stop.classification()
                             : "retry " + decision.classification();
            trace.events.add("retry:refreshRetryToken status=" + failure.statusCode() + " code="
                             + failure.errorCode() + " traits=" + failure.modeledTraits() + " replayable="
                             + failure.requestReplayable() + " failure=" + failure(failure.failure()) + " -> "
                             + outcome);
            return decision;
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            trace.events.add("retry:recordSuccess");
            return delegate.recordSuccess(token);
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            trace.events.add("retry:recordFailure");
            return delegate.recordFailure(token);
        }
    }

    static final class StaticCredentials implements IdentityProvider<AwsCredentialsIdentity> {
        private final AwsCredentialsIdentity identity;

        StaticCredentials(AwsCredentialsIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(identity);
        }
    }

    /**
     * A sync or async transport answering attempt {@code n} with the scenario's {@code n}th scripted reply (the last
     * one for later attempts) and recording every received request with its body. The async mode answers inline or on
     * the fake event loop, with the body in the scenario's number of chunks.
     */
    static final class ScriptedHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        private final HttpTransportMode mode;
        private final Scenario scenario;
        private final Trace trace;
        private final AtomicInteger exchanges = new AtomicInteger();

        ScriptedHttp(HttpTransportMode mode, Scenario scenario, Trace trace) {
            this.mode = mode;
            this.scenario = scenario;
            this.trace = trace;
        }

        /** Records the request and returns the scripted reply: a response, or a transport failure to raise. */
        private Object answer(SdkHttpRequest request) {
            int index = exchanges.getAndIncrement();
            trace.exchanges.add(request(request, true));
            Reply reply = scenario.script().get(Math.min(index, scenario.script().size() - 1));
            String requestId = "req-" + (index + 1);
            boolean restJson = scenario.operation().service.restJson;
            String contentType = restJson ? "application/json" : "application/x-amz-json-1.0";
            return switch (reply) {
                case Reply.Success success -> {
                    SdkHttpResponse.Builder response = SdkHttpResponse.builder()
                        .statusCode(scenario.operation().successStatus)
                        .addHeader("x-amzn-RequestId", requestId);
                    if (!success.wire().body().isEmpty()) {
                        response.addHeader("Content-Type", contentType);
                    }
                    success.wire().headers().forEach(response::addHeader);
                    yield response.body(HttpBody.fromUtf8(success.wire().body())).build();
                }
                case Reply.ModeledError error -> errorResponse(error.error().status, error.error().name,
                                                               NAMESPACE + "#" + error.error().name, error.members(),
                                                               requestId, restJson, contentType);
                case Reply.UnmodeledError error -> errorResponse(error.error().status, error.error().code,
                                                                 error.error().code,
                                                                 Map.of("message", "unmodeled " + error.error().code),
                                                                 requestId, restJson, contentType);
                case Reply.TransportFailure failure -> HttpTransportException.builder()
                    .phase(failure.phase())
                    .category(HttpFailureCategory.of(failure.phase(), HttpFailureCategory.Kind.IO))
                    .clientName(TRANSPORT_NAME)
                    .message("scripted " + failure.phase() + " failure")
                    .cause(new IOException("connection reset"))
                    .build();
            };
        }

        private static SdkHttpResponse errorResponse(int status, String restType, String jsonType,
                                                     Map<String, Object> members, String requestId,
                                                     boolean restJson, String contentType) {
            SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(status)
                                                              .addHeader("Content-Type", contentType)
                                                              .addHeader("x-amzn-RequestId", requestId);
            Map<String, Object> body = new LinkedHashMap<>();
            if (restJson) {
                response.addHeader("X-Amzn-ErrorType", restType);
            } else {
                body.put("__type", jsonType);
            }
            body.putAll(members);
            return response.body(HttpBody.fromUtf8(json(body))).build();
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    Object answer = answer(request);
                    if (answer instanceof HttpTransportException failure) {
                        throw failure;
                    }
                    return (SdkHttpResponse) answer;
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            AsyncExchangeCompletion completion =
                new AsyncExchangeCompletion(TRANSPORT_NAME, (reason, failure) -> handler.onError(failure));
            Runnable deliver = () -> {
                Object answer = answer(request);
                if (answer instanceof HttpTransportException failure) {
                    handler.onError(failure);
                    completion.fail(failure);
                    return;
                }
                SdkHttpResponse response = (SdkHttpResponse) answer;
                handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
                handler.onStream(new ChunkPublisher(response.body().asByteArray(), scenario.mechanics().chunks()));
                completion.complete();
            };
            if (scenario.mechanics().delivery() == Delivery.INLINE) {
                deliver.run();
            } else {
                eventLoop.execute(deliver);
            }
            return completion;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return TRANSPORT_NAME;
        }

        @Override
        public void close() {
        }
    }

    /** A response body in up to {@code chunks} buffers, one per unit of demand, on the requesting thread. */
    static final class ChunkPublisher implements Publisher<ByteBuffer> {
        private final List<ByteBuffer> chunks = new ArrayList<>();

        ChunkPublisher(byte[] body, int count) {
            int size = Math.max(1, (body.length + count - 1) / Math.max(1, count));
            for (int offset = 0; offset < body.length; offset += size) {
                chunks.add(ByteBuffer.wrap(body, offset, Math.min(size, body.length - offset)).slice()
                                     .asReadOnlyBuffer());
            }
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private long demand;
                private int next;
                private boolean emitting;
                private boolean done;

                @Override
                public void request(long n) {
                    if (done) {
                        return;
                    }
                    demand = n == Long.MAX_VALUE || demand + n < 0 ? Long.MAX_VALUE : demand + n;
                    if (emitting) {
                        return;
                    }
                    emitting = true;
                    try {
                        while (!done && demand > 0 && next < chunks.size()) {
                            demand--;
                            subscriber.onNext(chunks.get(next++));
                        }
                        if (!done && next == chunks.size()) {
                            done = true;
                            subscriber.onComplete();
                        }
                    } finally {
                        emitting = false;
                    }
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Fixture setup helpers

    /** The IDL model file as a Smithy JSON AST, the Dynamic Client's runtime model format. */
    static String jsonAst(Path idl) {
        ClassLoader classLoader = ClientProjectionLifecyclePropertyTest.class.getClassLoader();
        Model model = Model.assembler(classLoader).discoverModels(classLoader)
                           .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                           .addImport(idl)
                           .assemble().unwrap();
        return Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model));
    }

    static void copyTree(Path from, Path to) throws IOException {
        try (var walk = Files.walk(from)) {
            for (Path source : (Iterable<Path>) walk::iterator) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else if (!Files.exists(target)) {
                    Files.copy(source, target);
                } else {
                    assertFalse(source.toString().endsWith(".java") && !Files.readString(source)
                                                                            .equals(Files.readString(target)),
                                "the two generated services disagree on the shared source " + target);
                }
            }
        }
    }
}
