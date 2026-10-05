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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 29: Pipeline lifecycle partial order (Requirements 10.2, 10.14).
 *
 * <p>For any success, failure, retry or cancellation trace, the stages and hooks a call runs respect the documented
 * lifecycle partial order, and no dependent later stage runs after its prerequisite fails. Each try runs one call of a
 * generated scenario through {@link AwsCallEngine}, recording every reported stage point ({@link StageObserver}) and
 * every interceptor invocation, and compares the trace with {@link LifecycleOracle}: a reference model of the
 * lifecycle written here as a directed acyclic graph, from the design's "Stage order and retry ownership" flowchart
 * and the hook table of "Interceptors and plugins", not from {@link PipelineStage}'s declaration order. Checks:
 * <ul>
 *   <li>the stage trace equals the oracle's walk of the graph for the scenario, stage by stage and attempt by attempt,
 *       and the call ends as the walk does: succeeded, failed, or cancelled;</li>
 *   <li>independently of the walk: within the execution and within each attempt, every pair of events respects the
 *       graph's precedence and no stage repeats;</li>
 *   <li>attempt-scoped events, and only those, carry their attempt number; attempts are contiguous, numbered 1, 2, ...
 *       in order, never more than maxAttempts, and each later attempt follows its predecessor's retry
 *       classification;</li>
 *   <li>after the failing stage of a failed attempt, none of its dependents (the stages only its success edge leads
 *       to) runs (Requirement 10.14);</li>
 *   <li>the terminal stages of a completed call run at most once each, on the success path or the failure path only,
 *       and form the trace's suffix, so nothing follows the terminal stage; a cancelled call runs none of them, and no
 *       event at all follows its cancellation, even when a late identity, response or timer then arrives;</li>
 *   <li>each hook stage point is immediately followed by its one interceptor invocation, and no interceptor runs
 *       anywhere else.</li>
 * </ul>
 *
 * <p>A scenario picks the sync or async driver; any subset of the 16 hooks (one recording interceptor per hook); the
 * response-metadata and telemetry stage points; whether the operation has an idempotency token; signed (an identity
 * resolution per attempt) or unsigned auth; maxAttempts 1-4 with one scripted outcome per attempt: success, a
 * retryable 503, a non-retryable 400, a retryable connection reset, or (signed only) a retryable identity failure;
 * whether the client has no endpoint at all; and, for the async driver, an optional caller cancellation at the k-th
 * point where the driver waits. The async driver runs on a {@link ManualExecutor} with every identity, transport and
 * retry-delay wait held until the test releases it, one at a time, so each try runs deterministically on the test
 * thread and its jqwik seed reproduces it.
 *
 * <p>Interpretations: an unresolvable endpoint is a client condition (no endpoint rules and no endpoint), not a
 * per-attempt script; it fails attempt 1 and is not retryable, so it ends the call. A caller cancellation lands
 * between driver steps (at a wait), where the design defines it; the request feature stages (compression and the
 * checksums) are in the model but never active here, since activating them requires their feature implementations.
 * Hook failures, whose traces depend on where the hook throws, are covered by {@code InterceptorDispatchTest}.
 */
class PipelineLifecyclePropertyTest {
    private static final Identity IDENTITY = new Identity() {
    };
    private static final String SCHEME_ID = "test#scripted";
    private static final AwsOperation<Struct, Struct> UNSIGNED_GET = operation("UnsignedGet", GET_INPUT, false);
    private static final AwsOperation<Struct, Struct> SIGNED_GET = operation("SignedGet", GET_INPUT, true);
    private static final AwsOperation<Struct, Struct> UNSIGNED_PUT = operation("UnsignedPut", PUT_INPUT, false);
    private static final AwsOperation<Struct, Struct> SIGNED_PUT = operation("SignedPut", PUT_INPUT, true);
    /** More waits than any scenario has: 1 + 4 attempts x (identity + transmission + retry delay). */
    private static final int MAX_WAITS = 13;

    /** The scripted outcome of one attempt. */
    enum Outcome {
        SUCCESS,
        /** HTTP 503: decoded as a service error; transient, so retryable. */
        SERVICE_UNAVAILABLE,
        /** HTTP 400: decoded as a service error; not retryable. */
        BAD_REQUEST,
        /** A connection reset while connecting: transient, so retryable. */
        CONNECTION_RESET,
        /** The identity provider fails with an I/O error: transient, so retryable. Signed scenarios only. */
        IDENTITY_UNAVAILABLE
    }

    /** How the call ended. */
    enum Status {
        SUCCEEDED, FAILED, CANCELLED
    }

    /**
     * One generated call.
     *
     * @param outcomes the outcome of attempt {@code n} at index {@code n - 1}; one per allowed attempt
     * @param cancelAtWait the wait (0 being the queued first drain) at which the caller cancels; async only, else null
     */
    record Scenario(Driver driver, Set<PipelineStage> hooks, Set<PipelineStage> features, boolean idempotent,
                    boolean signed, boolean endpointUnresolvable, List<Outcome> outcomes, Integer cancelAtWait) {
        int maxAttempts() {
            return outcomes.size();
        }

        Outcome outcome(int attempt) {
            return outcomes.get(attempt - 1);
        }

        AwsOperation<Struct, Struct> operation() {
            return idempotent ? (signed ? SIGNED_PUT : UNSIGNED_PUT) : (signed ? SIGNED_GET : UNSIGNED_GET);
        }
    }

    /** A stage point the engine reported, with its attempt (0 for an execution-scoped stage), or a hook invocation. */
    record Event(PipelineStage stage, int attempt, boolean hook) {
        static Event entered(PipelineStage stage, int attempt) {
            return new Event(stage, attempt, false);
        }

        static Event invoked(PipelineStage hook) {
            return new Event(hook, 0, true);
        }

        @Override
        public String toString() {
            return hook ? "hook:" + stage : attempt == 0 ? stage.name() : stage + "#" + attempt;
        }
    }

    /**
     * What one call did.
     *
     * @param trace the stage points and hook invocations, in order
     * @param waits for the async driver, the number of stage points reported before each wait
     */
    record Observed(List<Event> trace, List<Integer> waits, Status status) {
        List<Event> stages() {
            return trace.stream().filter(e -> !e.hook()).toList();
        }
    }

    /**
     * What the oracle predicts for a call that is not cancelled.
     *
     * @param waits for the async driver, the number of stage points reported before each wait
     */
    record Prediction(List<Event> stages, List<Integer> waits, Status status) {
        int attempts() {
            return stages.stream().mapToInt(Event::attempt).max().orElse(0);
        }
    }

    // Feature: aws-sdk-java-v3, Property 29: Pipeline lifecycle partial order
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if cancellation, retries, failures or any failing stage stop being generated.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 29: Pipeline lifecycle partial order")
    void stagesAndHooksFollowTheLifecyclePartialOrder(@ForAll("scenarios") Scenario scenario) {
        Prediction full = LifecycleOracle.predict(scenario);
        Integer cancelAt = scenario.cancelAtWait();
        boolean cancelled = cancelAt != null && cancelAt < full.waits().size();

        Observed observed = new Call(scenario).run();

        List<Event> stages = observed.stages();
        String description = scenario + "\n  trace: " + observed.trace();
        assertPartialOrder(stages, description);
        assertAttempts(stages, scenario, description);
        assertDependentsSkipped(stages, scenario, observed.status(), description);
        assertTerminal(stages, observed.status(), description);
        assertHooks(observed.trace(), scenario, description);
        List<Event> expected = cancelled ? full.stages().subList(0, full.waits().get(cancelAt)) : full.stages();
        assertThat(stages).as(description).containsExactlyElementsOf(expected);
        assertThat(observed.status()).as(description).isEqualTo(cancelled ? Status.CANCELLED : full.status());
        if (scenario.driver() == Driver.ASYNC) {
            assertThat(observed.waits()).as("waits of " + description)
                                        .isEqualTo(cancelled ? full.waits().subList(0, cancelAt + 1) : full.waits());
        }

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("status").collect(observed.status());
        Statistics.label("status").coverage(c -> {
            c.check(Status.SUCCEEDED).percentage(p -> p >= 20);
            c.check(Status.FAILED).percentage(p -> p >= 20);
            c.check(Status.CANCELLED).percentage(p -> p >= 5);
        });
        Statistics.label("attempts").collect(full.attempts());
        Statistics.label("retried").collect(full.attempts() > 1);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Set<LifecycleOracle.Node> failedAt = new HashSet<>();
        for (int attempt = 1; attempt <= full.attempts(); attempt++) {
            Optional.ofNullable(LifecycleOracle.failingNode(scenario, attempt)).ifPresent(failedAt::add);
        }
        for (LifecycleOracle.Node failing : LifecycleOracle.FAILING_NODES) {
            String label = "an attempt failed at " + failing.stage;
            Statistics.label(label).collect(failedAt.contains(failing));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 3));
        }
    }

    @Example
    @Label("Property 29 (oracle): the lifecycle model is a DAG over every stage, linearly extended by PipelineStage")
    void lifecycleModelIsADagOverEveryStage() {
        Set<PipelineStage> modelled = EnumSet.noneOf(PipelineStage.class);
        for (LifecycleOracle.Node node : LifecycleOracle.Node.values()) {
            if (node.stage != null) {
                modelled.add(node.stage);
                assertThat(LifecycleOracle.precedes(node.stage, node.stage)).as(node.name()).isFalse();
            }
            assertThat(LifecycleOracle.closure(LifecycleOracle.Node.VALIDATE)).as("reachable").contains(node);
        }
        assertThat(modelled).containsExactlyInAnyOrder(PipelineStage.values());
        // The declaration order PipelineStage documents is consistent with the model: a linear extension of it.
        for (PipelineStage earlier : PipelineStage.values()) {
            for (PipelineStage later : PipelineStage.values()) {
                if (LifecycleOracle.precedes(earlier, later)) {
                    assertThat(earlier.ordinal()).as(earlier + " before " + later).isLessThan(later.ordinal());
                }
            }
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.of(true, false).flatMap(signed -> Combinators.combine(
            PipelineArbitraries.drivers(),
            PipelineArbitraries.hookSubsets(),
            Arbitraries.subsetOf(PipelineStage.RESPONSE_METADATA, PipelineStage.TELEMETRY),
            Arbitraries.of(true, false),
            Arbitraries.frequency(Tuple.of(1, true), Tuple.of(7, false)),
            outcomes(signed).list().ofMinSize(1).ofMaxSize(4),
            Arbitraries.integers().between(0, MAX_WAITS - 1).injectNull(0.4))
            .as((driver, hooks, features, idempotent, endpointUnresolvable, outcomes, cancelAt) -> new Scenario(
                driver, hooks, PipelineArbitraries.enumSet(features), idempotent, signed, endpointUnresolvable,
                outcomes, driver == Driver.ASYNC ? cancelAt : null)));
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

    // ---- Checks of the observed trace that do not use the oracle's walk ----

    /** Within the execution and within each attempt, no stage repeats and no later event must precede an earlier. */
    private static void assertPartialOrder(List<Event> stages, String description) {
        for (int i = 0; i < stages.size(); i++) {
            for (int j = i + 1; j < stages.size(); j++) {
                Event earlier = stages.get(i);
                Event later = stages.get(j);
                if (earlier.attempt() != later.attempt()) {
                    continue;
                }
                String pair = earlier + " then " + later + " in " + description;
                assertThat(later.stage()).as(pair).isNotEqualTo(earlier.stage());
                assertThat(LifecycleOracle.precedes(later.stage(), earlier.stage())).as(pair).isFalse();
            }
        }
    }

    /**
     * Attempt-scoped stages, and only those, carry an attempt; attempts are contiguous, numbered from 1 in order, each
     * later one following its predecessor's retry classification, at most maxAttempts of them; once a terminal stage
     * ran, no attempt stage follows.
     */
    private static void assertAttempts(List<Event> stages, Scenario scenario, String description) {
        int current = 0;
        PipelineStage lastOfAttempt = null;
        boolean terminal = false;
        for (Event event : stages) {
            boolean attemptScoped = LifecycleOracle.ATTEMPT_STAGES.contains(event.stage());
            String at = event + " in " + description;
            assertThat(event.attempt() != 0).as("attempt of " + at).isEqualTo(attemptScoped);
            if (attemptScoped) {
                assertThat(terminal).as("attempt stage after a terminal stage: " + at).isFalse();
                if (event.attempt() != current) {
                    assertThat(event.attempt()).as("next attempt: " + at).isEqualTo(current + 1);
                    if (current > 0) {
                        assertThat(lastOfAttempt).as("end of attempt " + current + " before " + at)
                                                 .isEqualTo(PipelineStage.CLASSIFY_RETRY);
                    }
                    current = event.attempt();
                }
                lastOfAttempt = event.stage();
            } else if (current > 0 || LifecycleOracle.TERMINAL_STAGES.contains(event.stage())) {
                assertThat(LifecycleOracle.TERMINAL_STAGES).as("execution stage after attempts: " + at)
                                                           .contains(event.stage());
                terminal = true;
            }
        }
        assertThat(current).as("attempts of " + description).isLessThanOrEqualTo(scenario.maxAttempts());
    }

    /**
     * Requirement 10.14: after the failing stage of a failed attempt, no dependent of it runs in the rest of the
     * attempt, nor, for the call's last attempt, in the rest of the call.
     */
    private static void assertDependentsSkipped(List<Event> stages, Scenario scenario, Status status,
                                                String description) {
        int attempts = stages.stream().mapToInt(Event::attempt).max().orElse(0);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            LifecycleOracle.Node failing = LifecycleOracle.failingNode(scenario, attempt);
            if (failing == null) {
                continue;
            }
            int at = stages.indexOf(Event.entered(failing.stage, attempt));
            if (at < 0) {
                // Only a cancellation stops an attempt before the stage its script fails.
                assertThat(status).as(failing.stage + "#" + attempt + " in " + description)
                                  .isEqualTo(Status.CANCELLED);
                continue;
            }
            Set<PipelineStage> dependents = LifecycleOracle.dependents(failing);
            for (int i = at + 1; i < stages.size(); i++) {
                Event event = stages.get(i);
                if (event.attempt() > attempt) {
                    break;
                }
                assertThat(dependents).as(event + " after " + stages.get(at) + " failed, in " + description)
                                      .doesNotContain(event.stage());
            }
        }
    }

    /**
     * A completed call runs each terminal stage at most once, on one path, as the trace's suffix (so nothing follows
     * its terminal stage), and a failed call maps its failure exactly once; a cancelled call runs no terminal stage.
     */
    private static void assertTerminal(List<Event> stages, Status status, String description) {
        List<PipelineStage> terminal = stages.stream().map(Event::stage)
                                             .filter(LifecycleOracle.TERMINAL_STAGES::contains).toList();
        if (status == Status.CANCELLED) {
            assertThat(terminal).as("terminal stages of a cancelled call: " + description).isEmpty();
            return;
        }
        assertThat(terminal).as("terminal stages of " + description).doesNotHaveDuplicates();
        assertThat(stages.subList(stages.size() - terminal.size(), stages.size()))
            .as("the terminal stages end " + description)
            .extracting(Event::stage).containsExactlyElementsOf(terminal);
        if (status == Status.FAILED) {
            assertThat(LifecycleOracle.FAILURE_PATH).as("terminal path of " + description).containsAll(terminal);
            assertThat(terminal).as("the one mapping path of " + description).contains(PipelineStage.MAP_EXCEPTION);
        } else {
            assertThat(LifecycleOracle.SUCCESS_PATH).as("terminal path of " + description).containsAll(terminal);
        }
    }

    /** Each hook stage point is immediately followed by its one invocation, and invocations occur nowhere else. */
    private static void assertHooks(List<Event> trace, Scenario scenario, String description) {
        for (int i = 0; i < trace.size(); i++) {
            Event event = trace.get(i);
            String at = event + " at " + i + " of " + description;
            if (event.hook()) {
                assertThat(i > 0 && trace.get(i - 1).equals(Event.entered(event.stage(), trace.get(i - 1).attempt())))
                    .as("hook invoked at its stage point: " + at).isTrue();
            } else if (scenario.hooks().contains(event.stage())) {
                assertThat(i + 1 < trace.size() && trace.get(i + 1).equals(Event.invoked(event.stage())))
                    .as("stage point invokes its hook: " + at).isTrue();
            }
        }
    }

    private static AwsOperation<Struct, Struct> operation(String name, Schema input, boolean signed) {
        String id = "com.example#" + name;
        ModeledAuth auth = signed ? ModeledAuth.builder().operationId(id).addServiceScheme(SCHEME_ID).build() : null;
        return PipelineFixtures.operation(id, input, GET_OUTPUT, auth);
    }

    /**
     * The reference lifecycle: a graph of nodes (each the point of one {@link PipelineStage} on one path of the
     * documented lifecycle, so a stage that runs on two paths, such as {@code afterAttempt}, has two nodes) and edges
     * guarded by the {@link Branch} that takes them. Without the one retry edge, which starts the next attempt, the
     * graph is acyclic. A call walks it from {@link Node#VALIDATE} to {@link Node#END}, reporting each node whose stage
     * is active in the scenario; {@link #precedes} and {@link #dependents} are reachability in it.
     */
    static final class LifecycleOracle {
        /** The core stages: active in every plan. */
        static final Set<PipelineStage> CORE = EnumSet.of(
            PipelineStage.VALIDATE, PipelineStage.ACQUIRE_RETRY_TOKEN, PipelineStage.RESOLVE_ENDPOINT,
            PipelineStage.SELECT_AUTH_SCHEME, PipelineStage.RESOLVE_IDENTITY, PipelineStage.SERIALIZE,
            PipelineStage.SIGN, PipelineStage.TRANSMIT, PipelineStage.DESERIALIZE, PipelineStage.CLASSIFY_RETRY,
            PipelineStage.MAP_EXCEPTION);
        /** The failures the default classifier retries (transient service, transport and I/O failures). */
        static final Set<Outcome> RETRYABLE = EnumSet.of(
            Outcome.SERVICE_UNAVAILABLE, Outcome.CONNECTION_RESET, Outcome.IDENTITY_UNAVAILABLE);

        /** Which edge leaves a node. */
        enum Branch {
            ALWAYS,
            SIGNED, UNSIGNED,
            ENDPOINT_RESOLVED, ENDPOINT_FAILED,
            IDENTITY_RESOLVED, IDENTITY_FAILED,
            RESPONSE_RECEIVED, TRANSPORT_FAILED,
            OUTPUT, ERROR_RESPONSE,
            /** A retry token was granted: the next attempt; the graph's only back edge. */
            RETRY,
            STOP
        }

        /** A point of the lifecycle; {@code attempt} if it runs within an attempt (inside the retry owner). */
        enum Node {
            VALIDATE(PipelineStage.VALIDATE, false),
            BEFORE_EXECUTION(PipelineStage.BEFORE_EXECUTION, false),
            MODIFY_REQUEST(PipelineStage.MODIFY_REQUEST, false),
            IDEMPOTENCY_TOKEN(PipelineStage.IDEMPOTENCY_TOKEN, false),
            ACQUIRE_RETRY_TOKEN(PipelineStage.ACQUIRE_RETRY_TOKEN, false),
            BEFORE_ATTEMPT(PipelineStage.BEFORE_ATTEMPT, true),
            RESOLVE_ENDPOINT(PipelineStage.RESOLVE_ENDPOINT, true),
            SELECT_AUTH_SCHEME(PipelineStage.SELECT_AUTH_SCHEME, true),
            RESOLVE_IDENTITY(PipelineStage.RESOLVE_IDENTITY, true),
            SERIALIZE(PipelineStage.SERIALIZE, true),
            AFTER_SERIALIZATION(PipelineStage.AFTER_SERIALIZATION, true),
            REQUEST_COMPRESSION(PipelineStage.REQUEST_COMPRESSION, true),
            REQUEST_CHECKSUM(PipelineStage.REQUEST_CHECKSUM, true),
            MODIFY_BEFORE_SIGNING(PipelineStage.MODIFY_BEFORE_SIGNING, true),
            SIGN(PipelineStage.SIGN, true),
            MODIFY_AFTER_SIGNING(PipelineStage.MODIFY_AFTER_SIGNING, true),
            BEFORE_TRANSMISSION(PipelineStage.BEFORE_TRANSMISSION, true),
            TRANSMIT(PipelineStage.TRANSMIT, true),
            AFTER_TRANSMISSION(PipelineStage.AFTER_TRANSMISSION, true),
            MODIFY_HTTP_RESPONSE(PipelineStage.MODIFY_HTTP_RESPONSE, true),
            MODIFY_RESPONSE_BODY(PipelineStage.MODIFY_RESPONSE_BODY, true),
            RESPONSE_CHECKSUM(PipelineStage.RESPONSE_CHECKSUM, true),
            BEFORE_DESERIALIZATION(PipelineStage.BEFORE_DESERIALIZATION, true),
            DESERIALIZE(PipelineStage.DESERIALIZE, true),
            OUTPUT_METADATA(PipelineStage.RESPONSE_METADATA, true),
            AFTER_SUCCEEDED_ATTEMPT(PipelineStage.AFTER_ATTEMPT, true),
            /** {@code afterTransmission} observing the transport's failure. */
            TRANSPORT_FAILURE_OBSERVED(PipelineStage.AFTER_TRANSMISSION, true),
            /** The response metadata of a decoded error. */
            ERROR_METADATA(PipelineStage.RESPONSE_METADATA, true),
            MODIFY_ATTEMPT_EXCEPTION(PipelineStage.MODIFY_ATTEMPT_EXCEPTION, true),
            AFTER_FAILED_ATTEMPT(PipelineStage.AFTER_ATTEMPT, true),
            CLASSIFY_RETRY(PipelineStage.CLASSIFY_RETRY, true),
            MODIFY_RESPONSE(PipelineStage.MODIFY_RESPONSE, false),
            AFTER_SUCCEEDED_EXECUTION(PipelineStage.AFTER_EXECUTION, false),
            SUCCESS_TELEMETRY(PipelineStage.TELEMETRY, false),
            MAP_EXCEPTION(PipelineStage.MAP_EXCEPTION, false),
            MODIFY_EXCEPTION(PipelineStage.MODIFY_EXCEPTION, false),
            AFTER_FAILED_EXECUTION(PipelineStage.AFTER_EXECUTION, false),
            FAILURE_TELEMETRY(PipelineStage.TELEMETRY, false),
            END(null, false);

            final PipelineStage stage;
            final boolean attempt;

            Node(PipelineStage stage, boolean attempt) {
                this.stage = stage;
                this.attempt = attempt;
            }
        }

        record Edge(Branch branch, Node to) {
        }

        private static final Map<Node, List<Edge>> EDGES = new EnumMap<>(Node.class);

        static {
            chain(Node.VALIDATE, Node.BEFORE_EXECUTION, Node.MODIFY_REQUEST, Node.IDEMPOTENCY_TOKEN,
                  Node.ACQUIRE_RETRY_TOKEN, Node.BEFORE_ATTEMPT, Node.RESOLVE_ENDPOINT);
            fork(Node.RESOLVE_ENDPOINT, Branch.ENDPOINT_RESOLVED, Node.SELECT_AUTH_SCHEME,
                 Branch.ENDPOINT_FAILED, Node.MODIFY_ATTEMPT_EXCEPTION);
            edge(Node.SELECT_AUTH_SCHEME, Branch.SIGNED, Node.RESOLVE_IDENTITY);
            edge(Node.SELECT_AUTH_SCHEME, Branch.UNSIGNED, Node.SERIALIZE);
            fork(Node.RESOLVE_IDENTITY, Branch.IDENTITY_RESOLVED, Node.SERIALIZE,
                 Branch.IDENTITY_FAILED, Node.MODIFY_ATTEMPT_EXCEPTION);
            chain(Node.SERIALIZE, Node.AFTER_SERIALIZATION, Node.REQUEST_COMPRESSION, Node.REQUEST_CHECKSUM,
                  Node.MODIFY_BEFORE_SIGNING, Node.SIGN, Node.MODIFY_AFTER_SIGNING, Node.BEFORE_TRANSMISSION,
                  Node.TRANSMIT);
            fork(Node.TRANSMIT, Branch.RESPONSE_RECEIVED, Node.AFTER_TRANSMISSION,
                 Branch.TRANSPORT_FAILED, Node.TRANSPORT_FAILURE_OBSERVED);
            chain(Node.AFTER_TRANSMISSION, Node.MODIFY_HTTP_RESPONSE, Node.MODIFY_RESPONSE_BODY, Node.RESPONSE_CHECKSUM,
                  Node.BEFORE_DESERIALIZATION, Node.DESERIALIZE);
            fork(Node.DESERIALIZE, Branch.OUTPUT, Node.OUTPUT_METADATA, Branch.ERROR_RESPONSE, Node.ERROR_METADATA);
            chain(Node.OUTPUT_METADATA, Node.AFTER_SUCCEEDED_ATTEMPT, Node.MODIFY_RESPONSE,
                  Node.AFTER_SUCCEEDED_EXECUTION, Node.SUCCESS_TELEMETRY, Node.END);
            chain(Node.TRANSPORT_FAILURE_OBSERVED, Node.MODIFY_ATTEMPT_EXCEPTION);
            chain(Node.ERROR_METADATA, Node.MODIFY_ATTEMPT_EXCEPTION, Node.AFTER_FAILED_ATTEMPT, Node.CLASSIFY_RETRY);
            edge(Node.CLASSIFY_RETRY, Branch.RETRY, Node.BEFORE_ATTEMPT);
            edge(Node.CLASSIFY_RETRY, Branch.STOP, Node.MAP_EXCEPTION);
            chain(Node.MAP_EXCEPTION, Node.MODIFY_EXCEPTION, Node.AFTER_FAILED_EXECUTION, Node.FAILURE_TELEMETRY,
                  Node.END);
        }

        /** The nodes where an attempt fails, each by its failure branch. */
        static final List<Node> FAILING_NODES =
            List.of(Node.RESOLVE_ENDPOINT, Node.RESOLVE_IDENTITY, Node.TRANSMIT, Node.DESERIALIZE);
        static final Set<PipelineStage> ATTEMPT_STAGES = stagesOf(n -> n.attempt, EnumSet.allOf(Node.class));
        /** The execution-scoped stages after the attempts begin: the call's terminal stages. */
        static final Set<PipelineStage> TERMINAL_STAGES = stagesOf(n -> !n.attempt, reach(Node.BEFORE_ATTEMPT));
        static final Set<PipelineStage> SUCCESS_PATH = stagesOf(n -> !n.attempt, reach(Node.AFTER_SUCCEEDED_ATTEMPT));
        static final Set<PipelineStage> FAILURE_PATH = stagesOf(n -> !n.attempt, reach(Node.CLASSIFY_RETRY));

        private LifecycleOracle() {
        }

        private static void chain(Node... nodes) {
            for (int i = 0; i + 1 < nodes.length; i++) {
                edge(nodes[i], Branch.ALWAYS, nodes[i + 1]);
            }
        }

        private static void fork(Node at, Branch success, Node next, Branch failure, Node failed) {
            edge(at, success, next);
            edge(at, failure, failed);
        }

        private static void edge(Node from, Branch branch, Node to) {
            EDGES.computeIfAbsent(from, ignored -> new ArrayList<>()).add(new Edge(branch, to));
        }

        /**
         * The nodes reachable from {@code from} without the retry edge, excluding {@code from} itself.
         *
         * @throws IllegalStateException if the graph without the retry edge has a cycle
         */
        static Set<Node> reach(Node from) {
            Set<Node> reached = EnumSet.noneOf(Node.class);
            visit(from, EnumSet.noneOf(Node.class), reached);
            return reached;
        }

        private static void visit(Node node, Set<Node> path, Set<Node> reached) {
            if (!path.add(node)) {
                throw new IllegalStateException("The lifecycle has a cycle through " + node);
            }
            for (Edge edge : EDGES.getOrDefault(node, List.of())) {
                if (edge.branch() != Branch.RETRY) {
                    reached.add(edge.to());
                    visit(edge.to(), path, reached);
                }
            }
            path.remove(node);
        }

        /** Whether {@code earlier} must precede {@code later} within one execution or attempt. */
        static boolean precedes(PipelineStage earlier, PipelineStage later) {
            for (Node node : Node.values()) {
                if (node.stage == earlier && stagesOf(n -> true, reach(node)).contains(later)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The dependents of {@code failing} (Requirement 10.14): the stages its success edge leads to and its failure
         * edge does not, so a failure there must skip them.
         */
        static Set<PipelineStage> dependents(Node failing) {
            List<Edge> edges = EDGES.get(failing);
            Set<PipelineStage> dependents = stagesOf(n -> true, closure(edges.get(0).to()));
            dependents.removeAll(stagesOf(n -> true, closure(edges.get(1).to())));
            return dependents;
        }

        /** {@code node} and the nodes reachable from it without the retry edge. */
        static Set<Node> closure(Node node) {
            Set<Node> nodes = reach(node);
            nodes.add(node);
            return nodes;
        }

        /** The node where attempt {@code attempt} of {@code scenario} fails, or null if it succeeds. */
        static Node failingNode(Scenario scenario, int attempt) {
            if (scenario.endpointUnresolvable()) {
                return Node.RESOLVE_ENDPOINT;
            }
            return switch (scenario.outcome(attempt)) {
                case SUCCESS -> null;
                case IDENTITY_UNAVAILABLE -> Node.RESOLVE_IDENTITY;
                case CONNECTION_RESET -> Node.TRANSMIT;
                case SERVICE_UNAVAILABLE, BAD_REQUEST -> Node.DESERIALIZE;
            };
        }

        /**
         * The walk of an uncancelled call of {@code scenario}. For the async driver it also records where the driver
         * waits: for its first drain after validation, for each identity and each response, and for each retry delay.
         */
        static Prediction predict(Scenario scenario) {
            boolean async = scenario.driver() == Driver.ASYNC;
            List<Event> stages = new ArrayList<>();
            List<Integer> waits = new ArrayList<>();
            int attempt = 0;
            Node node = Node.VALIDATE;
            Status status = Status.SUCCEEDED;
            while (node != Node.END) {
                if (node == Node.BEFORE_ATTEMPT) {
                    attempt++;
                }
                if (node == Node.MAP_EXCEPTION) {
                    status = Status.FAILED;
                }
                if (isActive(scenario, node.stage)) {
                    stages.add(Event.entered(node.stage, node.attempt ? attempt : 0));
                }
                Edge edge = next(scenario, node, attempt);
                boolean waitsHere = node == Node.VALIDATE || node == Node.RESOLVE_IDENTITY || node == Node.TRANSMIT
                                    || edge.branch() == Branch.RETRY;
                if (async && waitsHere) {
                    waits.add(stages.size());
                }
                node = edge.to();
            }
            return new Prediction(List.copyOf(stages), List.copyOf(waits), status);
        }

        private static boolean isActive(Scenario scenario, PipelineStage stage) {
            return CORE.contains(stage) || scenario.hooks().contains(stage) || scenario.features().contains(stage)
                   || stage == PipelineStage.IDEMPOTENCY_TOKEN && scenario.idempotent();
        }

        /** The one edge leaving {@code node} whose branch holds for the scenario's attempt. */
        private static Edge next(Scenario scenario, Node node, int attempt) {
            List<Edge> taken = EDGES.get(node).stream().filter(e -> holds(e.branch(), scenario, attempt)).toList();
            if (taken.size() != 1) {
                throw new IllegalStateException(node + " has " + taken + " for " + scenario);
            }
            return taken.get(0);
        }

        private static boolean holds(Branch branch, Scenario scenario, int attempt) {
            return switch (branch) {
                case ALWAYS -> true;
                case SIGNED -> scenario.signed();
                case UNSIGNED -> !scenario.signed();
                case ENDPOINT_FAILED -> scenario.endpointUnresolvable();
                case ENDPOINT_RESOLVED -> !scenario.endpointUnresolvable();
                case IDENTITY_FAILED -> scenario.outcome(attempt) == Outcome.IDENTITY_UNAVAILABLE;
                case IDENTITY_RESOLVED -> scenario.outcome(attempt) != Outcome.IDENTITY_UNAVAILABLE;
                case TRANSPORT_FAILED -> scenario.outcome(attempt) == Outcome.CONNECTION_RESET;
                case RESPONSE_RECEIVED -> scenario.outcome(attempt) != Outcome.CONNECTION_RESET;
                case ERROR_RESPONSE -> scenario.outcome(attempt) != Outcome.SUCCESS;
                case OUTPUT -> scenario.outcome(attempt) == Outcome.SUCCESS;
                case RETRY -> retries(scenario, attempt);
                case STOP -> !retries(scenario, attempt);
            };
        }

        /** One granted token per retryable failure, while attempts remain; the retry quota is never exhausted here. */
        private static boolean retries(Scenario scenario, int attempt) {
            return !scenario.endpointUnresolvable() && RETRYABLE.contains(scenario.outcome(attempt))
                   && attempt < scenario.maxAttempts();
        }

        private static Set<PipelineStage> stagesOf(Predicate<Node> filter, Set<Node> nodes) {
            Set<PipelineStage> stages = EnumSet.noneOf(PipelineStage.class);
            for (Node node : nodes) {
                if (node.stage != null && filter.test(node)) {
                    stages.add(node.stage);
                }
            }
            return stages;
        }
    }

    /**
     * One call of a scenario: its engine, its scripted identity provider and transport (one outcome per attempt, the
     * attempt read from the stage observer), and the trace it records. For the async driver every wait is held in
     * {@link #pending} until {@link #run} releases it.
     */
    private static final class Call implements SdkSyncHttpClient, SdkAsyncHttpClient {
        private final Scenario scenario;
        private final boolean async;
        private final List<Event> trace = new ArrayList<>();
        private final Deque<Runnable> pending = new ArrayDeque<>();
        private final ManualExecutor executor = new ManualExecutor();
        private final ManualScheduler scheduler = new ManualScheduler();
        private int heldTimers;
        private int attempt;

        Call(Scenario scenario) {
            this.scenario = scenario;
            this.async = scenario.driver() == Driver.ASYNC;
        }

        Observed run() {
            ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(retryStrategy(), scenario.driver().mode);
            if (scenario.endpointUnresolvable()) {
                runtime.configuration(SdkClientConfiguration.empty());
            }
            if (scenario.signed()) {
                runtime.authSchemeSelector(AuthSchemeSelector.create(List.of(new ScriptedScheme(this::identity))));
            }
            for (PipelineStage hook : scenario.hooks()) {
                runtime.addInterceptor(PipelineFixtures.hookRecorder(hook, () -> trace.add(Event.invoked(hook))));
            }
            if (!scenario.features().isEmpty()) {
                runtime.addPlugin(new FeatureStages(scenario.features()));
            }
            if (async) {
                runtime.completionExecutor(executor).retryScheduler(scheduler);
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                         .stageObserver(this::entered);
            AwsCallEngine engine = (async ? builder.asyncHttpClient(this) : builder.syncHttpClient(this)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(scenario.operation());
            Struct request = PipelineFixtures.request(scenario.idempotent() ? PUT_INPUT : GET_INPUT, "a");
            if (!async) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return new Observed(trace, List.of(), Status.SUCCEEDED);
                } catch (RuntimeException failure) {
                    return new Observed(trace, List.of(), Status.FAILED);
                }
            }
            return runAsync(engine.executeAsync(plan, request, null, null));
        }

        /**
         * Drives the async call wait by wait: the first drain, then per wait the one held event and the drain it
         * queues. At the scenario's wait the caller cancels instead; then the late events of every held wait are
         * delivered, and nothing may follow.
         */
        private Observed runAsync(CompletableFuture<Struct> future) {
            List<Integer> waits = new ArrayList<>();
            for (int wait = 0; !future.isDone(); wait++) {
                assertThat(wait).as("waits of " + scenario).isLessThan(MAX_WAITS);
                waits.add((int) trace.stream().filter(e -> !e.hook()).count());
                if (Objects.equals(scenario.cancelAtWait(), wait)) {
                    assertThat(future.cancel(true)).as("cancellation at wait " + wait + " of " + scenario).isTrue();
                    int frozen = trace.size();
                    drain();
                    while (!pending.isEmpty()) {
                        pending.remove().run();
                        drain();
                    }
                    assertThat(trace).as("events after the cancellation of " + scenario).hasSize(frozen);
                    break;
                }
                if (wait > 0) {
                    assertThat(pending).as("held events at wait " + wait + " of " + scenario + ": " + trace)
                                       .hasSize(1);
                    pending.remove().run();
                }
                drain();
            }
            if (!future.isCancelled()) {
                assertThat(pending).as("waits outliving " + scenario).isEmpty();
                if (scenario.cancelAtWait() != null) {
                    // A cancellation after the outcome loses and changes nothing.
                    int size = trace.size();
                    assertThat(future.cancel(true)).as("late cancellation of " + scenario).isFalse();
                    drain();
                    assertThat(trace).as("events after a late cancellation of " + scenario).hasSize(size);
                }
            }
            Status status = future.isCancelled() ? Status.CANCELLED
                                                 : future.isCompletedExceptionally() ? Status.FAILED
                                                                                     : Status.SUCCEEDED;
            return new Observed(trace, waits, status);
        }

        /** Runs the queued driver work, then holds each retry delay it scheduled. */
        private void drain() {
            executor.runAll();
            for (; heldTimers < scheduler.timers.size(); heldTimers++) {
                PipelineFixtures.CountingTimer timer = scheduler.timers.get(heldTimers);
                pending.add(() -> timer.complete(null));
            }
        }

        private RetryStrategy retryStrategy() {
            if (!async) {
                return PipelineFixtures.retries(scenario.maxAttempts());
            }
            // A nonzero delay, so the async driver waits for each retry; the manual scheduler holds it.
            BackoffStrategy minute = BackoffStrategy.fixedDelay(Duration.ofMinutes(1));
            return StandardRetryStrategy.builder().maxAttempts(scenario.maxAttempts()).backoffStrategy(minute)
                                        .throttlingBackoffStrategy(minute).build();
        }

        private void entered(CallState<?, ?> call, AttemptState attemptState, PipelineStage stage) {
            if (attemptState != null) {
                attempt = attemptState.number();
            }
            trace.add(Event.entered(stage, attemptState == null ? 0 : attemptState.number()));
        }

        /** Delivers {@code event} now (sync), or holds it until the test releases the driver's wait (async). */
        private void hold(Runnable event) {
            if (async) {
                pending.add(event);
            } else {
                event.run();
            }
        }

        private CompletableFuture<Identity> identity() {
            CompletableFuture<Identity> identity = new CompletableFuture<>();
            if (scenario.outcome(attempt) == Outcome.IDENTITY_UNAVAILABLE) {
                UncheckedIOException failure =
                    new UncheckedIOException(new IOException("The credentials endpoint is unreachable"));
                hold(() -> identity.completeExceptionally(failure));
            } else {
                hold(() -> identity.complete(IDENTITY));
            }
            return identity;
        }

        /** A transport scripted with the current attempt's outcome. */
        private ScriptedHttp transmission() {
            ScriptedHttp http = new ScriptedHttp();
            return switch (scenario.outcome(attempt)) {
                case SERVICE_UNAVAILABLE -> http.respond(503, null);
                case BAD_REQUEST -> http.respond(400, null);
                case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                    HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset")));
                // An attempt whose identity failed is never transmitted; if it were, the trace would show it.
                case SUCCESS, IDENTITY_UNAVAILABLE -> http.respond(200, "v");
            };
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return transmission().prepare(request, options);
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            ScriptedHttp transmission = transmission();
            pending.add(() -> transmission.execute(request, handler, options));
            return new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "PipelineLifecyclePropertyTest";
        }

        @Override
        public void close() {
        }
    }

    /** A scheme whose identity provider returns the scripted identity of the current attempt; signs nothing. */
    private static final class ScriptedScheme implements AuthScheme<Identity> {
        private final Supplier<CompletableFuture<Identity>> identities;

        ScriptedScheme(Supplier<CompletableFuture<Identity>> identities) {
            this.identities = identities;
        }

        @Override
        public String schemeId() {
            return SCHEME_ID;
        }

        @Override
        public Class<Identity> identityType() {
            return Identity.class;
        }

        @Override
        public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
            return Optional.of(new IdentityProvider<>() {
                @Override
                public Class<Identity> identityType() {
                    return Identity.class;
                }

                @Override
                public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
                    return identities.get();
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return request -> SignedRequest.unsigned(request.request());
        }
    }

    /** Activates the given feature stage points, which need no feature implementation. */
    private record FeatureStages(Set<PipelineStage> stages) implements AwsFeaturePlugin {
        @Override
        public ShapeId id() {
            return ShapeId.from("test#FeatureStages");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            return true;
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            stages.forEach(plan::activate);
        }
    }
}
