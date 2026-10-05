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

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.STRING;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 35: Interceptor contexts are exact (Requirements 11.1, 11.5).
 *
 * <p>For any applicable hook, the interceptor receives the exact operation schema, scope-correct typed context, and
 * endpoint/identity values produced for that execution or attempt. Each try runs one generated call through
 * {@link AwsCallEngine} with recording interceptors on a generated subset of the 16 hooks, and compares every value
 * each hook received, by identity, with the stage outputs of that call, attempt and hook point.
 *
 * <p>The oracle captures each stage output where it is produced or consumed, independently of the hook contexts:
 * <ul>
 *   <li>the protocol fake records the modeled request it serializes, the endpoint URL it serializes against, the
 *       serialized request it returns, the response it deserializes, and the output or decoded error it returns;</li>
 *   <li>the identity fake records the identity (a new instance per resolution) or the failure it returns, and the
 *       scheme's identity provider is the one the selection must carry;</li>
 *   <li>the signing fake records the unsigned request and identity it signs and the signed request it returns;</li>
 *   <li>the transport fake records the request it transmits and the response it returns or failure it throws;</li>
 *   <li>the {@link StageObserver} records the call's options and execution context at validation, the modeled
 *       request after the idempotency-token stage, and per attempt its context, the endpoint the endpoint stage
 *       resolved and the auth scheme the selection stage chose;</li>
 *   <li>the caller records the output or failure the call surfaced.</li>
 * </ul>
 * Each hook's operation, schema, options, execution context and modeled request must be those of the call; each
 * attempt hook's attempt context must be its attempt's (the same one across the attempt, a distinct one per attempt),
 * with no endpoint, identity or auth scheme before resolution and exactly the attempt's resolved ones after; and the
 * hook-specific values (serialized, unsigned, signed and transmitted requests, received response and body, output,
 * attempt and terminal failures) must be exactly the corresponding stage outputs of that attempt.
 *
 * <p>A scenario picks the driver; one of nine operations (three shapes with distinct schemas, one with an idempotency
 * token the caller may supply, each unsigned or signed with one of two schemes); a client endpoint; the hooks to
 * observe, each with one or two interceptors; and maxAttempts 1-3 with one scripted outcome per attempt: success, a
 * retryable 503, a non-retryable 400, a retryable connection reset, or (signed only) a retryable identity failure.
 * Interceptors return every value unchanged, so each stage output flows unchanged to the next stage. The async driver
 * runs on a {@link ManualExecutor} and {@link ManualScheduler} drained by the test thread, so each try is
 * deterministic and its jqwik seed reproduces it.
 */
class InterceptorContextPropertyTest {
    private static final Schema LIST_INPUT = Schema.structureBuilder(ShapeId.from("com.example#ListThingsInput"))
                                                   .putMember("Name", STRING)
                                                   .build();
    private static final Schema LIST_OUTPUT = Schema.structureBuilder(ShapeId.from("com.example#ListThingsOutput"))
                                                    .putMember("Value", STRING)
                                                    .build();
    private static final List<String> ENDPOINTS = List.of(
        "https://things.example.com", "https://eu-west-1.things.example.net", "http://localhost",
        "https://vpce-0123.things.example.org");
    private static final Map<Shape, Map<Auth, AwsOperation<Struct, Struct>>> OPERATIONS = operations();

    /** The modeled shape of an operation. */
    enum Shape {
        GET("GetThing", GET_INPUT, GET_OUTPUT),
        /** Has an idempotency token member, so the idempotency-token stage may replace the request. */
        PUT("PutThing", PUT_INPUT, GET_OUTPUT),
        LIST("ListThings", LIST_INPUT, LIST_OUTPUT);

        final String name;
        final Schema input;
        final Schema output;

        Shape(String name, Schema input, Schema output) {
            this.name = name;
            this.input = input;
            this.output = output;
        }
    }

    /** The modeled auth of an operation: none, or one of the two schemes the client supports. */
    enum Auth {
        UNSIGNED(null),
        SCHEME_A("test#schemeA"),
        SCHEME_B("test#schemeB");

        final String schemeId;

        Auth(String schemeId) {
            this.schemeId = schemeId;
        }
    }

    /** The scripted outcome of one attempt. */
    enum Outcome {
        SUCCESS,
        /** HTTP 503: decoded as a service error; retryable. */
        SERVICE_UNAVAILABLE,
        /** HTTP 400: decoded as a service error; not retryable. */
        BAD_REQUEST,
        /** A connection reset thrown by the transport; retryable. */
        CONNECTION_RESET,
        /** The identity provider fails with an I/O error; retryable. Signed scenarios only. */
        IDENTITY_UNAVAILABLE
    }

    /**
     * One generated call.
     *
     * @param callerToken whether the caller supplies the idempotency token ({@link Shape#PUT} only)
     * @param interceptorsPerHook how many recording interceptors observe each hook in {@code hooks}
     * @param outcomes the outcome of attempt {@code n} at index {@code n - 1}; one per allowed attempt
     */
    record Scenario(Driver driver, Shape shape, Auth auth, boolean callerToken, String endpoint,
                    Set<PipelineStage> hooks, int interceptorsPerHook, List<Outcome> outcomes) {
        AwsOperation<Struct, Struct> operation() {
            return OPERATIONS.get(shape).get(auth);
        }

        boolean signed() {
            return auth != Auth.UNSIGNED;
        }

        Outcome outcome(int attempt) {
            return outcomes.get(attempt - 1);
        }

        /** The attempts the call makes: until a success, a non-retryable failure, or maxAttempts. */
        int expectedAttempts() {
            for (int attempt = 1; attempt < outcomes.size(); attempt++) {
                Outcome outcome = outcome(attempt);
                if (outcome == Outcome.SUCCESS || outcome == Outcome.BAD_REQUEST) {
                    return attempt;
                }
            }
            return outcomes.size();
        }

        boolean succeeds() {
            return outcome(expectedAttempts()) == Outcome.SUCCESS;
        }
    }

    /**
     * What one interceptor received at one hook invocation, with the attempt-scoped values read while the hook ran
     * (an attempt context is unreadable once its attempt completes).
     *
     * @param attempt the attempt context of an attempt hook, else null
     * @param attemptNumber the attempt number of an attempt hook, else 0
     */
    record Received(PipelineStage hook, int interceptor, HookContext context, AttemptContext attempt,
                    int attemptNumber, Optional<Endpoint> endpoint, Optional<Identity> identity,
                    Optional<SelectedAuthScheme<?>> authScheme) {
        @Override
        public String toString() {
            return hook + (attemptNumber == 0 ? "" : "#" + attemptNumber) + "/" + interceptor;
        }
    }

    // Feature: aws-sdk-java-v3, Property 35: Interceptor contexts are exact
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if either driver, signed or unsigned calls, retries (and hooks of a retry),
    // failures, any outcome or any observed hook stop being generated.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 35: Interceptor contexts are exact")
    void interceptorContextsAreExact(@ForAll("scenarios") Scenario scenario) {
        Call call = new Call(scenario);
        call.run();

        call.assertOracle();
        for (Received received : call.received) {
            call.assertReceived(received);
        }

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("driver").coverage(c -> {
            c.check(Driver.SYNC).percentage(p -> p >= 30);
            c.check(Driver.ASYNC).percentage(p -> p >= 30);
        });
        Statistics.label("auth").collect(scenario.auth());
        Statistics.label("signed").collect(scenario.signed());
        Statistics.label("signed").coverage(c -> {
            c.check(true).percentage(p -> p >= 30);
            c.check(false).percentage(p -> p >= 20);
        });
        // A retry's attempt hooks must see that attempt's own (distinct) context and resolved values.
        Statistics.label("an attempt context of a retry was checked")
                  .collect(call.received.stream().anyMatch(r -> r.attemptNumber() > 1));
        Statistics.label("an attempt context of a retry was checked")
                  .coverage(c -> c.check(true).percentage(p -> p >= 10));
        Statistics.label("shape").collect(scenario.shape());
        Statistics.label("status").collect(scenario.succeeds() ? "succeeded" : "failed");
        Statistics.label("status").coverage(c -> {
            c.check("succeeded").percentage(p -> p >= 20);
            c.check("failed").percentage(p -> p >= 20);
        });
        Statistics.label("retried").collect(scenario.expectedAttempts() > 1);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Set<Outcome> outcomes = scenario.outcomes().subList(0, scenario.expectedAttempts()).stream()
                                        .collect(Collectors.toSet());
        for (Outcome outcome : Outcome.values()) {
            String label = "an attempt ended with " + outcome;
            Statistics.label(label).collect(outcomes.contains(outcome));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 5));
        }
        Set<PipelineStage> observed = call.received.stream().map(Received::hook).collect(Collectors.toSet());
        for (PipelineStage hook : PipelineArbitraries.HOOK_STAGES) {
            String label = "a context of " + hook + " was checked";
            Statistics.label(label).collect(observed.contains(hook));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 10));
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.of(Auth.class).flatMap(auth -> Combinators.combine(
            PipelineArbitraries.drivers(),
            Arbitraries.of(Shape.class),
            Arbitraries.of(true, false),
            Arbitraries.of(ENDPOINTS),
            PipelineArbitraries.hookSubsets(),
            Arbitraries.integers().between(1, 2),
            outcomes(auth != Auth.UNSIGNED).list().ofMinSize(1).ofMaxSize(3))
            .as((driver, shape, callerToken, endpoint, hooks, perHook, outcomes) -> new Scenario(
                driver, shape, auth, shape == Shape.PUT && callerToken, endpoint, hooks, perHook, outcomes)));
    }

    private static Arbitrary<Outcome> outcomes(boolean signed) {
        List<Tuple.Tuple2<Integer, Outcome>> frequencies = new ArrayList<>(List.of(
            Tuple.of(3, Outcome.SUCCESS), Tuple.of(3, Outcome.SERVICE_UNAVAILABLE), Tuple.of(1, Outcome.BAD_REQUEST),
            Tuple.of(2, Outcome.CONNECTION_RESET)));
        if (signed) {
            frequencies.add(Tuple.of(2, Outcome.IDENTITY_UNAVAILABLE));
        }
        return Arbitraries.frequency(frequencies);
    }

    private static Map<Shape, Map<Auth, AwsOperation<Struct, Struct>>> operations() {
        Map<Shape, Map<Auth, AwsOperation<Struct, Struct>>> operations = new EnumMap<>(Shape.class);
        for (Shape shape : Shape.values()) {
            Map<Auth, AwsOperation<Struct, Struct>> byAuth = new EnumMap<>(Auth.class);
            for (Auth auth : Auth.values()) {
                String id = "com.example#" + shape.name + auth.name();
                ModeledAuth modeled = auth.schemeId == null
                                      ? null
                                      : ModeledAuth.builder().operationId(id).addServiceScheme(auth.schemeId).build();
                byAuth.put(auth, PipelineFixtures.operation(id, shape.input, shape.output, modeled));
            }
            operations.put(shape, byAuth);
        }
        return operations;
    }

    /** The stage outputs of one attempt, as the fakes and the stage observer captured them. */
    private static final class AttemptTrace {
        final int number;
        final AttemptContext context;
        /**
         * The attempt number its context reported as the attempt started. Read then, because the context closes when
         * the attempt ends and rejects every read afterwards (Requirement 11.7); only its identity outlives it.
         */
        final int contextNumber;
        Endpoint endpoint;
        SelectedAuthScheme<?> authScheme;
        Identity identity;
        Throwable identityFailure;
        SerializableStruct serializedInput;
        URI serializedEndpoint;
        SdkHttpRequest serialized;
        SdkHttpRequest unsigned;
        Identity signerIdentity;
        SignedRequest signed;
        SdkHttpRequest transmitted;
        SdkHttpResponse transportResponse;
        Throwable transportFailure;
        SdkHttpResponse deserialized;
        SerializableStruct output;
        AwsServiceException error;
        /** For an unsigned call, the one signed-request wrapper the attempt's hooks saw; it has no other source. */
        SignedRequest unsignedWrapper;

        AttemptTrace(int number, AttemptContext context) {
            this.number = number;
            this.context = context;
            this.contextNumber = context.attemptNumber();
        }

        /** The attempt's failure: of identity resolution, of the transport, or the decoded error response. */
        Throwable failure() {
            return identityFailure != null ? identityFailure : transportFailure != null ? transportFailure : error;
        }
    }

    /** An identity of one resolution; compared by identity only. */
    private static final class TestIdentity implements Identity {
        private final String name;

        TestIdentity(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * One call of a scenario: its engine and the fakes of its protocol, schemes, identities and transport, which record
     * each stage output into the current attempt's {@link AttemptTrace}; and every context its interceptors received.
     */
    private static final class Call implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final Scenario scenario;
        final AwsOperation<Struct, Struct> operation;
        /** The client endpoint: a fresh instance per call, so identity comparisons are meaningful. */
        final URI endpoint;
        final Struct input;
        final List<AttemptTrace> attempts = new ArrayList<>();
        final List<Received> received = new ArrayList<>();
        final Map<String, RecordingScheme> schemes = new HashMap<>();
        private final ManualExecutor executor = new ManualExecutor();
        private final ManualScheduler scheduler = new ManualScheduler();
        CallOptions options;
        ExecutionContext execution;
        SerializableStruct executionRequest;
        SerializableStruct output;
        Throwable failure;
        private AttemptTrace current;

        Call(Scenario scenario) {
            this.scenario = scenario;
            this.operation = scenario.operation();
            this.endpoint = URI.create(scenario.endpoint());
            Map<String, Object> members = new HashMap<>();
            members.put("Name", "thing-" + scenario.shape().name().toLowerCase());
            if (scenario.callerToken()) {
                members.put("ClientToken", "caller-token");
            }
            this.input = new Struct(scenario.shape().input, members);
            for (Auth auth : Auth.values()) {
                if (auth.schemeId != null) {
                    schemes.put(auth.schemeId, new RecordingScheme(auth.schemeId));
                }
            }
        }

        void run() {
            boolean async = scenario.driver() == Driver.ASYNC;
            ClientRuntime.Builder runtime =
                ClientRuntime.builder()
                             .configuration(SdkClientConfiguration.builder()
                                                                  .option(SdkClientOption.CLIENT_ENDPOINT, endpoint)
                                                                  .build())
                             .protocol(new RecordingProtocol())
                             .transportMode(scenario.driver().mode)
                             .retryStrategy(PipelineFixtures.retries(scenario.outcomes().size()), "things")
                             .authSchemeSelector(AuthSchemeSelector.create(List.copyOf(schemes.values())));
            for (PipelineStage hook : scenario.hooks()) {
                for (int i = 0; i < scenario.interceptorsPerHook(); i++) {
                    runtime.addInterceptor(recorder(hook, i));
                }
            }
            if (async) {
                runtime.completionExecutor(executor).retryScheduler(scheduler);
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                         .stageObserver(this::entered);
            AwsCallEngine engine = (async ? builder.asyncHttpClient(this) : builder.syncHttpClient(this)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(operation);
            if (!async) {
                try {
                    output = engine.executeSync(plan, input, null, null);
                } catch (RuntimeException e) {
                    failure = e;
                }
                return;
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, input, null, null);
            for (int round = 0; !future.isDone(); round++) {
                assertThat(round).as("drain rounds of " + scenario).isLessThan(100);
                executor.runAll();
                scheduler.timers.forEach(timer -> timer.complete(null));
            }
            try {
                output = future.join();
            } catch (CompletionException e) {
                failure = e.getCause();
            }
        }

        private void entered(CallState<?, ?> call, AttemptState attempt, PipelineStage stage) {
            switch (stage) {
                case VALIDATE -> {
                    options = call.options();
                    execution = call.executionContext();
                }
                case ACQUIRE_RETRY_TOKEN -> executionRequest = call.request();
                case RESOLVE_ENDPOINT -> {
                    current = new AttemptTrace(attempt.number(), attempt.context());
                    attempts.add(current);
                }
                // The output of the endpoint stage, as the next stage starts.
                case SELECT_AUTH_SCHEME -> current.endpoint = attempt.endpoint().orElseThrow();
                // The output of the selection stage, as identity resolution starts (signed calls only).
                case RESOLVE_IDENTITY -> current.authScheme = attempt.authScheme().orElseThrow();
                default -> {
                }
            }
        }

        private String description() {
            return scenario + "\n  received: " + received;
        }

        // ---- The oracle's own consistency: the fakes saw the stage outputs flow as the scenario scripts ----

        void assertOracle() {
            String description = description();
            assertThat(options).as(description).isNotNull();
            assertThat(execution).as(description).isNotNull();
            assertThat(executionRequest).as(description).isNotNull();
            if (scenario.shape() != Shape.PUT) {
                assertThat(executionRequest).as("request without an idempotency token, " + description)
                                            .isSameAs(input);
            }
            assertThat(attempts).as("attempts of " + description).hasSize(scenario.expectedAttempts());
            for (int i = 0; i < attempts.size(); i++) {
                AttemptTrace attempt = attempts.get(i);
                String of = "attempt " + (i + 1) + " of " + description;
                assertThat(attempt.number).as(of).isEqualTo(i + 1);
                assertThat(attempt.contextNumber).as(of).isEqualTo(i + 1);
                // Every attempt has ended, so its context is closed (isOpen and identity are its only safe reads).
                assertThat(attempt.context.isOpen()).as(of).isFalse();
                for (int j = 0; j < i; j++) {
                    assertThat(attempt.context).as(of).isNotSameAs(attempts.get(j).context);
                }
                assertThat(attempt.endpoint.url()).as(of).isSameAs(endpoint);
                assertThat(attempt.authScheme == null).as(of).isEqualTo(!scenario.signed());
                if (attempt.authScheme != null) {
                    RecordingScheme scheme = schemes.get(scenario.auth().schemeId);
                    assertThat(attempt.authScheme.scheme()).as(of).isSameAs(scheme);
                    assertThat(attempt.authScheme.identityProvider()).as(of).isSameAs(scheme.provider);
                }
                Outcome outcome = scenario.outcome(attempt.number);
                assertThat(attempt.identityFailure != null).as(of).isEqualTo(outcome == Outcome.IDENTITY_UNAVAILABLE);
                if (outcome == Outcome.IDENTITY_UNAVAILABLE) {
                    assertThat(attempt.serialized).as(of).isNull();
                    continue;
                }
                assertThat(attempt.serializedInput).as(of).isSameAs(executionRequest);
                assertThat(attempt.serializedEndpoint).as(of).isSameAs(attempt.endpoint.url());
                if (scenario.signed()) {
                    assertThat(attempt.unsigned).as(of).isSameAs(attempt.serialized);
                    assertThat(attempt.signerIdentity).as(of).isSameAs(attempt.identity);
                    assertThat(attempt.transmitted).as(of).isSameAs(attempt.signed.request());
                } else {
                    assertThat(attempt.signed).as(of).isNull();
                    assertThat(attempt.transmitted).as(of).isSameAs(attempt.serialized);
                }
                // The attempt transmitted to exactly its resolved endpoint (Requirement 11.5).
                assertThat(attempt.transmitted.scheme()).as(of).isEqualTo(attempt.endpoint.url().getScheme());
                assertThat(attempt.transmitted.host()).as(of).isEqualTo(attempt.endpoint.url().getHost());
                assertThat(attempt.transportFailure != null).as(of).isEqualTo(outcome == Outcome.CONNECTION_RESET);
                if (outcome == Outcome.CONNECTION_RESET) {
                    assertThat(attempt.deserialized).as(of).isNull();
                    continue;
                }
                if (scenario.driver() == Driver.SYNC) {
                    assertThat(attempt.deserialized).as(of).isSameAs(attempt.transportResponse);
                }
                assertThat(attempt.output != null).as(of).isEqualTo(outcome == Outcome.SUCCESS);
                assertThat(attempt.error != null).as(of).isEqualTo(outcome != Outcome.SUCCESS);
            }
            AttemptTrace last = attempts.get(attempts.size() - 1);
            if (scenario.succeeds()) {
                assertThat(failure).as(description).isNull();
                assertThat(output).as(description).isSameAs(last.output);
            } else {
                assertThat(output).as(description).isNull();
                assertThat(failure).as(description).isNotNull();
                // Surfaced as is, or (a failure that is not an SDK exception) as the cause of the one mapped failure.
                Throwable surfaced = failure == last.failure() ? failure : failure.getCause();
                assertThat(surfaced).as(description).isSameAs(last.failure());
            }
        }

        // ---- The property: each received context holds exactly the stage outputs of its call, attempt and hook ----

        void assertReceived(Received received) {
            HookContext context = received.context();
            String of = received + " of " + description();
            assertThat(context.operation()).as(of).isSameAs(operation);
            assertThat(context.operationSchema()).as(of).isSameAs(operation.schemas().operation());
            assertThat(context.options()).as(of).isSameAs(options);
            assertThat(context.execution()).as(of).isSameAs(execution);
            boolean beforeToken = received.hook() == PipelineStage.BEFORE_EXECUTION
                                  || received.hook() == PipelineStage.MODIFY_REQUEST;
            assertThat(context.request()).as(of).isSameAs(beforeToken ? input : executionRequest);
            if (!(context instanceof HookContext.Attempt)) {
                assertThat(received.attempt()).as(of).isNull();
                assertExecutionValues(context, of);
                return;
            }
            assertThat(received.attemptNumber()).as(of).isBetween(1, attempts.size());
            AttemptTrace attempt = attempts.get(received.attemptNumber() - 1);
            assertThat(received.attempt()).as(of).isSameAs(attempt.context);
            assertThat(received.attempt().execution()).as(of).isSameAs(execution);
            if (received.hook() == PipelineStage.BEFORE_ATTEMPT) {
                assertThat(received.endpoint()).as(of).isEmpty();
                assertThat(received.identity()).as(of).isEmpty();
                assertThat(received.authScheme()).as(of).isEmpty();
            } else {
                assertThat(received.endpoint()).as(of).containsSame(attempt.endpoint);
                assertPresentAs(received.identity(), attempt.identity, of);
                assertPresentAs(received.authScheme(), attempt.authScheme, of);
            }
            assertAttemptValues(context, attempt, of);
        }

        private void assertExecutionValues(HookContext context, String of) {
            switch (context) {
                case HookContext.ModifyResponse c -> assertThat(c.response()).as(of).isSameAs(output);
                case HookContext.ModifyException c -> assertThat(c.failure()).as(of).isSameAs(failure);
                case HookContext.AfterExecution c -> {
                    assertPresentAs(c.response(), output, of);
                    assertPresentAs(c.failure(), failure, of);
                }
                default -> {
                }
            }
        }

        private void assertAttemptValues(HookContext context, AttemptTrace attempt, String of) {
            switch (context) {
                case HookContext.AfterSerialization c ->
                    assertThat(c.httpRequest()).as(of).isSameAs(attempt.serialized);
                case HookContext.ModifyBeforeSigning c ->
                    assertThat(c.httpRequest()).as(of).isSameAs(attempt.serialized);
                case HookContext.ModifyAfterSigning c -> assertSigned(c.signedRequest(), attempt, of);
                case HookContext.BeforeTransmission c -> {
                    assertSigned(c.signedRequest(), attempt, of);
                    assertThat(c.signedRequest().request()).as(of).isSameAs(attempt.transmitted);
                }
                case HookContext.AfterTransmission c -> {
                    assertSigned(c.signedRequest(), attempt, of);
                    assertPresentAs(c.httpResponse(), attempt.deserialized, of);
                    assertPresentAs(c.failure(), attempt.transportFailure, of);
                    assertThat(c.httpResponse().isPresent() ^ c.failure().isPresent()).as(of).isTrue();
                }
                case HookContext.ModifyHttpResponse c ->
                    assertThat(c.httpResponse()).as(of).isSameAs(attempt.deserialized);
                case HookContext.ModifyResponseBody c -> {
                    assertThat(c.httpResponse()).as(of).isSameAs(attempt.deserialized);
                    assertThat(c.body()).as(of).isSameAs(attempt.deserialized.body());
                }
                case HookContext.BeforeDeserialization c ->
                    assertThat(c.httpResponse()).as(of).isSameAs(attempt.deserialized);
                case HookContext.ModifyAttemptException c ->
                    assertThat(c.failure()).as(of).isNotNull().isSameAs(attempt.failure());
                case HookContext.AfterAttempt c -> {
                    assertPresentAs(c.response(), attempt.output, of);
                    assertPresentAs(c.failure(), attempt.output == null ? attempt.failure() : null, of);
                }
                default -> {
                }
            }
        }

        /** The signed request the attempt's signer returned; unsigned, the one wrapper of its serialized request. */
        private void assertSigned(SignedRequest signed, AttemptTrace attempt, String of) {
            if (scenario.signed()) {
                assertThat(signed).as(of).isSameAs(attempt.signed);
                return;
            }
            assertThat(signed.request()).as(of).isSameAs(attempt.serialized);
            assertThat(signed.signatureValue()).as(of).isEmpty();
            if (attempt.unsignedWrapper == null) {
                attempt.unsignedWrapper = signed;
            }
            assertThat(signed).as(of).isSameAs(attempt.unsignedWrapper);
        }

        /** {@code actual} holds exactly {@code expected}, or is empty when it is null. */
        private static <T> void assertPresentAs(Optional<? extends T> actual, T expected, String of) {
            if (expected == null) {
                assertThat(actual).as(of).isEmpty();
            } else {
                assertThat(actual).as(of).isPresent();
                assertThat(actual.get()).as(of).isSameAs(expected);
            }
        }

        // ---- Recording interceptors: one class per hook, so a plan activates exactly the observed hooks ----

        private void receive(PipelineStage hook, int interceptor, HookContext context) {
            if (context instanceof HookContext.Attempt attempt) {
                received.add(new Received(hook, interceptor, context, attempt.attempt(), attempt.attemptNumber(),
                                          attempt.endpoint(), attempt.identity(), attempt.authScheme()));
            } else {
                received.add(new Received(hook, interceptor, context, null, 0, Optional.empty(), Optional.empty(),
                                          Optional.empty()));
            }
        }

        private ExecutionInterceptor recorder(PipelineStage hook, int i) {
            return switch (hook) {
                case BEFORE_EXECUTION -> new ExecutionInterceptor() {
                    @Override
                    public void beforeExecution(HookContext.BeforeExecution context) {
                        receive(hook, i, context);
                    }
                };
                case MODIFY_REQUEST -> new ExecutionInterceptor() {
                    @Override
                    public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
                        receive(hook, i, context);
                        return context.request();
                    }
                };
                case BEFORE_ATTEMPT -> new ExecutionInterceptor() {
                    @Override
                    public void beforeAttempt(HookContext.BeforeAttempt context) {
                        receive(hook, i, context);
                    }
                };
                case AFTER_SERIALIZATION -> new ExecutionInterceptor() {
                    @Override
                    public void afterSerialization(HookContext.AfterSerialization context) {
                        receive(hook, i, context);
                    }
                };
                case MODIFY_BEFORE_SIGNING -> new ExecutionInterceptor() {
                    @Override
                    public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
                        receive(hook, i, context);
                        return context.httpRequest();
                    }
                };
                case MODIFY_AFTER_SIGNING -> new ExecutionInterceptor() {
                    @Override
                    public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
                        receive(hook, i, context);
                        return context.signedRequest();
                    }
                };
                case BEFORE_TRANSMISSION -> new ExecutionInterceptor() {
                    @Override
                    public void beforeTransmission(HookContext.BeforeTransmission context) {
                        receive(hook, i, context);
                    }
                };
                case AFTER_TRANSMISSION -> new ExecutionInterceptor() {
                    @Override
                    public void afterTransmission(HookContext.AfterTransmission context) {
                        receive(hook, i, context);
                    }
                };
                case MODIFY_HTTP_RESPONSE -> new ExecutionInterceptor() {
                    @Override
                    public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
                        receive(hook, i, context);
                        return context.httpResponse();
                    }
                };
                case MODIFY_RESPONSE_BODY -> new ExecutionInterceptor() {
                    @Override
                    public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
                        receive(hook, i, context);
                        return context.body();
                    }
                };
                case BEFORE_DESERIALIZATION -> new ExecutionInterceptor() {
                    @Override
                    public void beforeDeserialization(HookContext.BeforeDeserialization context) {
                        receive(hook, i, context);
                    }
                };
                case MODIFY_ATTEMPT_EXCEPTION -> new ExecutionInterceptor() {
                    @Override
                    public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
                        receive(hook, i, context);
                        return context.failure();
                    }
                };
                case AFTER_ATTEMPT -> new ExecutionInterceptor() {
                    @Override
                    public void afterAttempt(HookContext.AfterAttempt context) {
                        receive(hook, i, context);
                    }
                };
                case MODIFY_RESPONSE -> new ExecutionInterceptor() {
                    @Override
                    public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
                        receive(hook, i, context);
                        return context.response();
                    }
                };
                case MODIFY_EXCEPTION -> new ExecutionInterceptor() {
                    @Override
                    public Throwable modifyException(HookContext.ModifyException context) {
                        receive(hook, i, context);
                        return context.failure();
                    }
                };
                case AFTER_EXECUTION -> new ExecutionInterceptor() {
                    @Override
                    public void afterExecution(HookContext.AfterExecution context) {
                        receive(hook, i, context);
                    }
                };
                default -> throw new IllegalArgumentException(hook + " is not a hook stage");
            };
        }

        // ---- The identity and signing fakes ----

        /** Resolves a new identity per attempt, or fails as scripted; signs by recording what it signs. */
        private final class RecordingScheme implements AuthScheme<Identity> {
            final IdentityProvider<Identity> provider = new IdentityProvider<>() {
                @Override
                public Class<Identity> identityType() {
                    return Identity.class;
                }

                @Override
                public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
                    return identity();
                }
            };
            private final String id;

            RecordingScheme(String id) {
                this.id = id;
            }

            @Override
            public String schemeId() {
                return id;
            }

            @Override
            public Class<Identity> identityType() {
                return Identity.class;
            }

            @Override
            public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
                return Optional.of(provider);
            }

            @Override
            public HttpSigner<Identity> signer() {
                return this::sign;
            }

            private SignedRequest sign(SignRequest<? extends Identity> request) {
                current.unsigned = request.request();
                current.signerIdentity = request.identity();
                String signature = id + ":" + request.identity();
                SignedRequest signed = new SignedRequest(
                    request.request().toBuilder().putHeader("Authorization", signature).build(), signature);
                current.signed = signed;
                return signed;
            }
        }

        private CompletableFuture<Identity> identity() {
            if (scenario.outcome(current.number) == Outcome.IDENTITY_UNAVAILABLE) {
                UncheckedIOException unavailable =
                    new UncheckedIOException(new IOException("The credentials endpoint is unreachable"));
                current.identityFailure = unavailable;
                return CompletableFuture.failedFuture(unavailable);
            }
            Identity identity = new TestIdentity("identity#" + current.number);
            current.identity = identity;
            return CompletableFuture.completedFuture(identity);
        }

        // ---- The protocol fake ----

        /** Serializes {@code Name} to a header and the body; deserializes the body to {@code Value}. */
        private final class RecordingProtocol implements ClientProtocol {
            @Override
            public ShapeId protocolId() {
                return ShapeId.from("aws.protocols#awsJson1_0");
            }

            @Override
            public OperationProtocol operation(OperationSchemas schemas) {
                return new RecordingOperation(schemas);
            }
        }

        private final class RecordingOperation implements OperationProtocol {
            private final OperationSchemas schemas;

            RecordingOperation(OperationSchemas schemas) {
                this.schemas = schemas;
            }

            @Override
            public OperationSchemas schemas() {
                return schemas;
            }

            @Override
            public SdkHttpRequest serializeRequest(SerializableStruct request, URI url) {
                current.serializedInput = request;
                current.serializedEndpoint = url;
                String name = request.getMemberValue(schemas.input().member("Name"));
                SdkHttpRequest.Builder serialized = SdkHttpRequest.builder()
                                                                  .method("POST")
                                                                  .scheme(url.getScheme())
                                                                  .host(url.getHost())
                                                                  .putHeader("X-Name", name)
                                                                  .body(HttpBody.fromUtf8(name));
                Schema token = schemas.input().member("ClientToken");
                if (token != null && request.getMemberValue(token) != null) {
                    serialized.putHeader("X-Token", request.getMemberValue(token));
                }
                current.serialized = serialized.build();
                return current.serialized;
            }

            @Override
            public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response,
                                                                        ShapeBuilder<T> builder) {
                current.deserialized = response;
                builder.setMemberValue(builder.schema().member("Value"), response.body().asUtf8String());
                T deserialized = builder.build();
                current.output = deserialized;
                return deserialized;
            }

            @Override
            public AwsServiceException deserializeError(SdkHttpResponse response) {
                current.deserialized = response;
                AwsServiceException error =
                    AwsServiceException.builder()
                                       .statusCode(response.statusCode())
                                       .message("Service error " + response.statusCode())
                                       .awsErrorDetails(AwsErrorDetails.builder()
                                                                       .errorCode("Error" + response.statusCode())
                                                                       .build())
                                       .build();
                current.error = error;
                return error;
            }
        }

        // ---- The transport fake, for both drivers ----

        /** The current attempt's scripted response, or null for a connection reset (recorded as its failure). */
        private SdkHttpResponse transmission(SdkHttpRequest request) {
            current.transmitted = request;
            Outcome outcome = scenario.outcome(current.number);
            assertThat(outcome).as("transmission of an attempt whose identity failed").isNotEqualTo(
                Outcome.IDENTITY_UNAVAILABLE);
            if (outcome == Outcome.CONNECTION_RESET) {
                current.transportFailure = HttpTransportException.create(
                    HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset"));
                return null;
            }
            int status = outcome == Outcome.SUCCESS ? 200 : outcome == Outcome.BAD_REQUEST ? 400 : 503;
            current.transportResponse = SdkHttpResponse.builder()
                                                       .statusCode(status)
                                                       .body(HttpBody.fromUtf8("body#" + current.number))
                                                       .build();
            return current.transportResponse;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    SdkHttpResponse response = transmission(request);
                    if (response == null) {
                        throw (RuntimeException) current.transportFailure;
                    }
                    return response;
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
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            SdkHttpResponse response = transmission(request);
            if (response == null) {
                exchange.fail(current.transportFailure);
                handler.onError(current.transportFailure);
                return exchange;
            }
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(bodyOf(response.body().asUtf8String().getBytes(StandardCharsets.UTF_8)));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "InterceptorContextPropertyTest";
        }

        @Override
        public void close() {
        }
    }

    /** A response body publisher of {@code bytes}, delivered on the first request. */
    private static Publisher<ByteBuffer> bodyOf(byte[] bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes).asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }
}
