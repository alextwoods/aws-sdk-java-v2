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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.metrics.CoreMetric;
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
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryFailure;
import software.amazon.awssdk.telemetry.spi.TelemetryFailureListener;
import software.amazon.awssdk.telemetry.spi.TelemetryMetric;
import software.amazon.awssdk.telemetry.spi.TelemetryOptions;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 71: Telemetry failures are isolated (Requirements 21.14, 21.15).
 *
 * <p>For any telemetry callback failure and any underlying success or causal SDK failure, the call's terminal result
 * remains the underlying result while the telemetry failure appears in the telemetry-failure channel. Each try builds
 * one client with one to three publishers, each either a Telemetry SPI {@link TelemetryPublisher} or a V2
 * {@link MetricPublisher} (through {@link TelemetryConfiguration.Builder#addMetricPublisher}), and runs one or two
 * calls on the sync or async driver with a standard retry strategy of maxAttempts 1-4 without backoff. Each call
 * scripts one transport outcome per allowed attempt: success, a retryable 503 or 429, a non-retryable modeled 400, a
 * retryable connection reset, an unmodeled transport failure, or (async only) an exchange that never completes, which
 * the caller cancels.
 *
 * <p>The failure plan is generated per publisher: the attempt numbers whose {@code attemptCompleted} throws (SPI
 * publishers only; the V2 bridge has no attempt callback), whether {@code callCompleted} (for a V2 publisher,
 * {@code publish}) throws, and what it throws: a {@link RuntimeException}, an {@link Error} that is not a
 * {@link VirtualMachineError}, or a checked exception thrown sneakily. The failure listener records every failure
 * and either returns, or throws a {@link RuntimeException} or an {@link Error} itself. {@link VirtualMachineError}s
 * are outside the property: they are not callback failures but fatal JVM conditions, which the pipeline rethrows
 * rather than isolate (the telemetry failure rule of the design's error taxonomy concerns callback failures).
 *
 * <p>The oracle comes from the failure plan and a lifecycle trace recorded without the telemetry code (a test
 * interceptor's {@code beforeAttempt}, and the outcomes the test transport delivered). The transport and a test
 * protocol create a fresh result instance per attempt: the output of a success, the modeled service exception of an
 * error status, or the transport's own failure. Each call runs on the failing client and on a twin client with the
 * same script whose publishers and listener never throw. The checks:
 * <ul>
 *   <li>the caller's terminal result is the underlying result of the last attempt in the trace (21.14): the same
 *       output instance, the same service or transport exception instance with the trace's attempt count, an
 *       {@link SdkClientException} whose cause is the unmodeled transport failure instance, or cancellation; and it
 *       matches the twin's result: kind, exception type, message and attempt count, and the same attempts;</li>
 *   <li>every publisher, failing or not, receives the same records as its twin, one per attempt and one per call:
 *       a failure neither stops later callbacks nor changes any record;</li>
 *   <li>the failure listener receives exactly the planned failures, once each, in callback order, with the failing
 *       publisher's identity (the bridge, for a V2 publisher), the callback, the call's service and operation, and
 *       the thrown instance as the cause (21.15); the twin's listener receives none;</li>
 *   <li>no thrown instance (of a publisher or of the listener) reaches the caller, as the terminal failure or in its
 *       cause chain or suppressed failures.</li>
 * </ul>
 * The async driver completes inline ({@code Runnable::run}) with zero-delay retries, so each try is deterministic on
 * the test thread and its jqwik seed reproduces it.
 */
class TelemetryFailureIsolationPropertyTest {
    private static final int MAX_ATTEMPTS = 4;
    private static final String RETRY_SCOPE = "things";
    private static final String UNMODELED_MESSAGE = "transport component bug";

    /** The scripted transport outcome of one attempt. */
    enum Outcome {
        SUCCESS, SERVICE_UNAVAILABLE, THROTTLED, BAD_REQUEST, CONNECTION_RESET, UNMODELED_FAILURE, HANG
    }

    /** What a failing callback or listener throws; never a {@link VirtualMachineError}. */
    enum Fault {
        RUNTIME, ERROR, CHECKED;

        Throwable create(String where) {
            return switch (this) {
                case RUNTIME -> new IllegalStateException("telemetry failure in " + where);
                case ERROR -> new AssertionError("telemetry failure in " + where);
                case CHECKED -> new IOException("telemetry failure in " + where);
            };
        }
    }

    enum PublisherKind {
        SPI, METRIC_PUBLISHER
    }

    /** The failure listener's behavior after it records a failure. */
    enum ListenerKind {
        RETURNS, THROWS_RUNTIME, THROWS_ERROR
    }

    /** How a call ended, from its trace; for the statistics. */
    enum Ending {
        SUCCEEDED, FAILED, CANCELLED
    }

    /**
     * The failure plan of one publisher, applied to every call.
     *
     * @param failingAttempts the attempt numbers whose {@code attemptCompleted} throws; empty for a V2 publisher
     * @param failsCall whether {@code callCompleted} (a V2 publisher's {@code publish}) throws
     */
    record PublisherPlan(PublisherKind kind, Set<Integer> failingAttempts, boolean failsCall, Fault fault) {
        boolean healthy() {
            return failingAttempts.isEmpty() && !failsCall;
        }
    }

    /**
     * One generated call.
     *
     * @param script the transport outcome of attempt {@code n} at index {@code n - 1}; one per allowed attempt
     */
    record CallSpec(String operation, List<Outcome> script) {
    }

    record Scenario(Driver driver, int maxAttempts, String service, List<PublisherPlan> publishers,
                    ListenerKind listener, List<CallSpec> calls) {
    }

    /**
     * What one call of one client showed.
     *
     * @param attempts the attempt numbers whose {@code beforeAttempt} ran, in order
     * @param delivered the transport outcome of each transmission, in order
     * @param made the underlying result instance the test created for each transmission (null for a hang)
     * @param output the caller's output, or null
     * @param failure the caller's failure, or null
     * @param received per publisher, the projections of the records it received during the call
     * @param thrown per publisher, the instances it threw during the call
     * @param failures what the failure listener received during the call
     * @param listenerThrown the instances the failure listener threw during the call
     */
    record Observation(List<Integer> attempts, List<Outcome> delivered, List<Object> made, Object output,
                       Throwable failure, boolean cancelled, List<List<String>> received,
                       List<List<Throwable>> thrown, List<TelemetryFailure> failures, List<Throwable> listenerThrown) {
    }

    /** One planned telemetry failure. */
    record ExpectedFailure(TelemetryPublisher publisher, TelemetryFailure.Callback callback, Throwable cause) {
    }

    // Feature: aws-sdk-java-v3, Property 71: Telemetry failures are isolated
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if any ending, fault, failing callback or listener kind stops being generated.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 71: Telemetry failures are isolated")
    void telemetryFailuresAreIsolated(@ForAll("scenarios") Scenario scenario) {
        Client failing = new Client(scenario, true);
        Client twin = new Client(scenario, false);
        for (CallSpec spec : scenario.calls()) {
            Observation actual = failing.run(spec);
            Observation expected = twin.run(spec);
            String description = scenario + "\n  call: " + spec + "\n  observed: " + actual + "\n  twin: " + expected;

            assertUnderlyingResult(actual, description);
            assertUnderlyingResult(expected, "twin of " + description);
            assertSameAsTwin(actual, expected, description);
            assertRecordCounts(scenario, actual, description);
            List<ExpectedFailure> planned = expectedFailures(scenario, failing, actual);
            assertThat(actual.failures()).as("failure channel of " + description).hasSameSizeAs(planned);
            assertFailuresDelivered(scenario, spec, planned, actual.failures(), description);
            assertThat(expected.failures()).as("failure channel of the twin of " + description).isEmpty();
            assertNeverReachesCaller(actual, description);

            collectStatistics(scenario, actual);
        }
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("listener").collect(scenario.listener());
        Statistics.label("listener").coverage(c -> {
            for (ListenerKind kind : ListenerKind.values()) {
                c.check(kind).percentage(p -> p >= 15);
            }
        });
        Statistics.label("publishers").collect(scenario.publishers().size());
    }

    /** The caller's result is the underlying result of the trace's last attempt (Requirement 21.14). */
    private static void assertUnderlyingResult(Observation observed, String description) {
        int n = observed.attempts().size();
        assertThat(n).as("attempts of " + description).isBetween(1, MAX_ATTEMPTS);
        assertThat(observed.delivered()).as("transmissions of " + description).hasSize(n);
        Outcome last = observed.delivered().get(n - 1);
        Object underlying = observed.made().get(n - 1);
        switch (last) {
            case SUCCESS -> {
                assertThat(observed.failure()).as("failure of " + description).isNull();
                assertThat(observed.output()).as("output of " + description).isSameAs(underlying);
            }
            case HANG -> {
                assertThat(observed.cancelled()).as("cancellation of " + description).isTrue();
                assertThat(observed.failure()).as("failure of " + description)
                                              .isInstanceOf(CancellationException.class);
            }
            case UNMODELED_FAILURE -> {
                assertThat(observed.failure()).as("failure of " + description)
                                              .isExactlyInstanceOf(SdkClientException.class);
                assertThat(observed.failure().getCause()).as("cause of the failure of " + description)
                                                         .isSameAs(underlying);
                assertThat(((SdkException) observed.failure()).numAttempts()).as("attempts of the failure of "
                                                                                 + description).isEqualTo(n);
            }
            default -> {
                assertThat(observed.failure()).as("failure of " + description).isSameAs(underlying);
                assertThat(((SdkException) observed.failure()).numAttempts()).as("attempts of the failure of "
                                                                                 + description).isEqualTo(n);
            }
        }
        if (last != Outcome.HANG) {
            assertThat(observed.cancelled()).as("cancellation of " + description).isFalse();
        }
    }

    /** The failing client's call is indistinguishable from its twin's, apart from the failure channel. */
    private static void assertSameAsTwin(Observation actual, Observation twin, String description) {
        assertThat(actual.attempts()).as("attempts against the twin of " + description)
                                     .containsExactlyElementsOf(twin.attempts());
        assertThat(actual.delivered()).as("transmissions against the twin of " + description)
                                      .containsExactlyElementsOf(twin.delivered());
        assertThat(actual.cancelled()).as("cancellation against the twin of " + description)
                                      .isEqualTo(twin.cancelled());
        assertThat(actual.output() == null).as("output presence against the twin of " + description)
                                           .isEqualTo(twin.output() == null);
        if (actual.output() != null) {
            assertThat(value(actual.output())).as("output against the twin of " + description)
                                              .isEqualTo(value(twin.output()));
        }
        assertThat(actual.failure() == null).as("failure presence against the twin of " + description)
                                            .isEqualTo(twin.failure() == null);
        if (actual.failure() != null) {
            assertThat(actual.failure().getClass()).as("failure type against the twin of " + description)
                                                   .isEqualTo(twin.failure().getClass());
            assertThat(actual.failure().getMessage()).as("failure message against the twin of " + description)
                                                     .isEqualTo(twin.failure().getMessage());
            if (actual.failure() instanceof SdkException sdk) {
                assertThat(sdk.numAttempts()).as("failure attempts against the twin of " + description)
                                             .isEqualTo(((SdkException) twin.failure()).numAttempts());
            }
            assertThat(actual.failure().getSuppressed()).as("suppressed against the twin of " + description)
                                                        .hasSameSizeAs(twin.failure().getSuppressed());
        }
        for (int p = 0; p < actual.received().size(); p++) {
            assertThat(actual.received().get(p)).as("records of publisher " + p + " against the twin of "
                                                    + description)
                                                .containsExactlyElementsOf(twin.received().get(p));
        }
    }

    /** Every publisher still receives every callback: one per attempt (SPI) and one per call. */
    private static void assertRecordCounts(Scenario scenario, Observation actual, String description) {
        int n = actual.attempts().size();
        for (int p = 0; p < scenario.publishers().size(); p++) {
            int expected = scenario.publishers().get(p).kind() == PublisherKind.SPI ? n + 1 : 1;
            assertThat(actual.received().get(p)).as("records of publisher " + p + " of " + description)
                                                .hasSize(expected);
        }
    }

    /**
     * The planned failures of a call with the trace's attempts, in callback order: each attempt's callbacks in
     * publisher order, then each publisher's call callback; each cause is the instance its publisher threw.
     */
    private static List<ExpectedFailure> expectedFailures(Scenario scenario, Client client, Observation actual) {
        List<List<TelemetryFailure.Callback>> perPublisher = new ArrayList<>();
        List<ExpectedFailure> order = new ArrayList<>();
        List<Integer> orderSlots = new ArrayList<>();
        for (int p = 0; p < scenario.publishers().size(); p++) {
            perPublisher.add(new ArrayList<>());
        }
        for (int attempt : actual.attempts()) {
            for (int p = 0; p < scenario.publishers().size(); p++) {
                PublisherPlan plan = scenario.publishers().get(p);
                if (plan.kind() == PublisherKind.SPI && plan.failingAttempts().contains(attempt)) {
                    order.add(new ExpectedFailure(client.slots.get(p), TelemetryFailure.Callback.ATTEMPT_COMPLETED,
                                                  null));
                    orderSlots.add(p);
                }
            }
        }
        for (int p = 0; p < scenario.publishers().size(); p++) {
            if (scenario.publishers().get(p).failsCall()) {
                order.add(new ExpectedFailure(client.slots.get(p), TelemetryFailure.Callback.CALL_COMPLETED, null));
                orderSlots.add(p);
            }
        }
        // Each publisher threw exactly its planned failures; its k-th planned failure's cause is its k-th throw.
        int[] next = new int[scenario.publishers().size()];
        List<ExpectedFailure> expected = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            int p = orderSlots.get(i);
            List<Throwable> thrown = actual.thrown().get(p);
            Throwable cause = next[p] < thrown.size() ? thrown.get(next[p]) : null;
            next[p]++;
            expected.add(new ExpectedFailure(order.get(i).publisher(), order.get(i).callback(), cause));
        }
        for (int p = 0; p < next.length; p++) {
            assertThat(actual.thrown().get(p)).as("throws of publisher " + p + " of " + scenario + " in " + actual)
                                              .hasSize(next[p]);
        }
        return expected;
    }

    /** Exactly the planned failures reach the channel, once each, in order (Requirement 21.15). */
    private static void assertFailuresDelivered(Scenario scenario, CallSpec spec, List<ExpectedFailure> expected,
                                                List<TelemetryFailure> delivered, String description) {
        for (int i = 0; i < expected.size(); i++) {
            TelemetryFailure failure = delivered.get(i);
            ExpectedFailure planned = expected.get(i);
            String which = "failure " + i + " of " + description;
            assertThat(failure.publisher()).as("publisher of " + which).isSameAs(planned.publisher());
            assertThat(failure.callback()).as("callback of " + which).isEqualTo(planned.callback());
            assertThat(failure.cause()).as("cause of " + which).isSameAs(planned.cause());
            assertThat(failure.service()).as("service of " + which).isEqualTo(scenario.service());
            assertThat(failure.operation()).as("operation of " + which).isEqualTo(spec.operation());
        }
    }

    /** No thrown instance appears in what the caller observed. */
    private static void assertNeverReachesCaller(Observation actual, String description) {
        Set<Throwable> thrown = Collections.newSetFromMap(new IdentityHashMap<>());
        actual.thrown().forEach(thrown::addAll);
        thrown.addAll(actual.listenerThrown());
        Set<Throwable> visible = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(actual.failure(), visible);
        visible.retainAll(thrown);
        assertThat(visible).as("telemetry failures the caller observed in " + description).isEmpty();
    }

    /** {@code failure}, its causes and its suppressed failures, transitively. */
    private static void collect(Throwable failure, Set<Throwable> into) {
        if (failure == null || !into.add(failure)) {
            return;
        }
        collect(failure.getCause(), into);
        for (Throwable suppressed : failure.getSuppressed()) {
            collect(suppressed, into);
        }
    }

    private static void collectStatistics(Scenario scenario, Observation actual) {
        Outcome last = actual.delivered().get(actual.delivered().size() - 1);
        Ending ending = last == Outcome.SUCCESS ? Ending.SUCCEEDED : last == Outcome.HANG ? Ending.CANCELLED
                                                                                          : Ending.FAILED;
        Statistics.label("ending").collect(ending);
        Statistics.label("ending").coverage(c -> {
            c.check(Ending.SUCCEEDED).percentage(p -> p >= 15);
            c.check(Ending.FAILED).percentage(p -> p >= 15);
            c.check(Ending.CANCELLED).percentage(p -> p >= 2);
        });
        Statistics.label("retried").collect(actual.attempts().size() > 1);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 20));
        for (Outcome outcome : Outcome.values()) {
            String label = "the call ended with " + outcome;
            Statistics.label(label).collect(last == outcome);
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 2));
        }
        Statistics.label("telemetry failures in the call").collect(actual.failures().size());
        boolean attemptFailed = actual.failures().stream()
                                      .anyMatch(f -> f.callback() == TelemetryFailure.Callback.ATTEMPT_COMPLETED);
        boolean callFailed = actual.failures().stream()
                                   .anyMatch(f -> f.callback() == TelemetryFailure.Callback.CALL_COMPLETED);
        Statistics.label("an attempt callback failed").collect(attemptFailed);
        Statistics.label("an attempt callback failed").coverage(c -> c.check(true).percentage(p -> p >= 30));
        Statistics.label("a call callback failed").collect(callFailed);
        Statistics.label("a call callback failed").coverage(c -> c.check(true).percentage(p -> p >= 30));
        boolean v2Failed = actual.failures().stream().anyMatch(f -> !(f.publisher() instanceof SpiPublisher));
        Statistics.label("a V2 metric publisher failed").collect(v2Failed);
        Statistics.label("a V2 metric publisher failed").coverage(c -> c.check(true).percentage(p -> p >= 10));
        Set<Class<?>> faults = actual.failures().stream().map(f -> f.cause().getClass()).collect(Collectors.toSet());
        for (Fault fault : Fault.values()) {
            String label = "a callback threw " + fault;
            Statistics.label(label).collect(faults.contains(fault.create("").getClass()));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 10));
        }
        Statistics.label("healthy publisher alongside a failing one")
                  .collect(scenario.publishers().stream().anyMatch(PublisherPlan::healthy)
                           && !actual.failures().isEmpty());
    }

    private static Object value(Object output) {
        return ((Struct) output).getMemberValue(GET_OUTPUT.member("Value"));
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        return Combinators.combine(PipelineArbitraries.drivers(), Arbitraries.integers().between(1, MAX_ATTEMPTS),
                                   names, publisherPlans().list().ofMinSize(1).ofMaxSize(3),
                                   Arbitraries.of(ListenerKind.class))
                          .flatAs((driver, maxAttempts, service, publishers, listener) ->
                                      callSpecs(driver, maxAttempts, names).list().ofMinSize(1).ofMaxSize(2)
                                          .map(calls -> new Scenario(driver, maxAttempts, service, publishers,
                                                                     listener, calls)));
    }

    private static Arbitrary<PublisherPlan> publisherPlans() {
        Arbitrary<Set<Integer>> attempts = Arbitraries.integers().between(1, MAX_ATTEMPTS).set().ofMaxSize(MAX_ATTEMPTS);
        Arbitrary<Fault> faults = Arbitraries.of(Fault.class);
        Arbitrary<PublisherPlan> spi = Combinators.combine(attempts, Arbitraries.of(true, false), faults)
                                                  .as((failing, call, fault) ->
                                                          new PublisherPlan(PublisherKind.SPI, failing, call, fault));
        Arbitrary<PublisherPlan> v2 = Combinators.combine(Arbitraries.of(true, false), faults)
                                                 .as((call, fault) -> new PublisherPlan(PublisherKind.METRIC_PUBLISHER,
                                                                                        Set.of(), call, fault));
        Arbitrary<PublisherPlan> healthy = Arbitraries.of(PublisherKind.class)
                                                      .map(kind -> new PublisherPlan(kind, Set.of(), false,
                                                                                     Fault.RUNTIME));
        return Arbitraries.frequencyOf(Tuple.of(5, spi), Tuple.of(3, v2), Tuple.of(2, healthy));
    }

    private static Arbitrary<CallSpec> callSpecs(Driver driver, int maxAttempts, Arbitrary<String> names) {
        return Combinators.combine(names, outcomes(driver).list().ofSize(maxAttempts)).as(CallSpec::new);
    }

    private static Arbitrary<Outcome> outcomes(Driver driver) {
        List<Tuple.Tuple2<Integer, Outcome>> frequencies = new ArrayList<>(List.of(
            Tuple.of(4, Outcome.SUCCESS), Tuple.of(2, Outcome.SERVICE_UNAVAILABLE), Tuple.of(2, Outcome.THROTTLED),
            Tuple.of(1, Outcome.BAD_REQUEST), Tuple.of(2, Outcome.CONNECTION_RESET),
            Tuple.of(1, Outcome.UNMODELED_FAILURE)));
        if (driver == Driver.ASYNC) {
            frequencies.add(Tuple.of(1, Outcome.HANG));
        }
        return Arbitraries.frequency(frequencies);
    }

    /** Throws {@code failure}, checked or not, without declaring it. */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }

    /** A Telemetry SPI publisher that records every record it receives, then throws as its plan says. */
    private static final class SpiPublisher implements TelemetryPublisher {
        final List<String> received = new ArrayList<>();
        final List<Throwable> thrown = new ArrayList<>();
        private final PublisherPlan plan;
        private final boolean failing;

        SpiPublisher(PublisherPlan plan, boolean failing) {
            this.plan = plan;
            this.failing = failing;
        }

        @Override
        public void attemptCompleted(AttemptRecord attempt) {
            received.add(project(attempt));
            if (failing && plan.failingAttempts().contains(attempt.attemptNumber())) {
                throw fail("attemptCompleted#" + attempt.attemptNumber());
            }
        }

        @Override
        public void callCompleted(CallRecord call) {
            received.add("call:" + call.outcome() + ":" + call.attemptCount() + ":" + call.errorType() + ":"
                         + call.attempts().stream().map(SpiPublisher::project).toList());
            if (failing && plan.failsCall()) {
                throw fail("callCompleted");
            }
        }

        private RuntimeException fail(String where) {
            Throwable failure = plan.fault().create(where);
            thrown.add(failure);
            return sneakyThrow(failure);
        }

        private static String project(AttemptRecord attempt) {
            return "attempt#" + attempt.attemptNumber() + ":" + attempt.outcome() + ":" + attempt.httpStatusCode()
                   + ":" + attempt.errorType();
        }
    }

    /** A V2 metric publisher that records every collection it receives, then throws as its plan says. */
    private static final class V2Publisher implements MetricPublisher {
        final List<String> received = new ArrayList<>();
        final List<Throwable> thrown = new ArrayList<>();
        private final PublisherPlan plan;
        private final boolean failing;

        V2Publisher(PublisherPlan plan, boolean failing) {
            this.plan = plan;
            this.failing = failing;
        }

        @Override
        public void publish(MetricCollection collection) {
            received.add(collection.name() + ":success=" + collection.metricValues(CoreMetric.API_CALL_SUCCESSFUL)
                         + ":retries=" + collection.metricValues(CoreMetric.RETRY_COUNT)
                         + ":error=" + collection.metricValues(CoreMetric.ERROR_TYPE)
                         + ":attempts=" + collection.children().stream()
                                                   .map(c -> c.metricValues(TelemetryMetric.HTTP_STATUS_CODE) + "/"
                                                             + c.metricValues(CoreMetric.ERROR_TYPE))
                                                   .toList());
            if (failing && plan.failsCall()) {
                Throwable failure = plan.fault().create("publish");
                thrown.add(failure);
                throw sneakyThrow(failure);
            }
        }

        @Override
        public void close() {
        }
    }

    /** The telemetry failure channel: records every failure, then returns or throws as its kind says. */
    private static final class Listener implements TelemetryFailureListener {
        final List<TelemetryFailure> failures = new ArrayList<>();
        final List<Throwable> thrown = new ArrayList<>();
        private final ListenerKind kind;

        Listener(ListenerKind kind) {
            this.kind = kind;
        }

        @Override
        public void telemetryFailed(TelemetryFailure failure) {
            failures.add(failure);
            Throwable own = switch (kind) {
                case RETURNS -> null;
                case THROWS_RUNTIME -> new IllegalStateException("failure listener failure");
                case THROWS_ERROR -> new AssertionError("failure listener failure");
            };
            if (own != null) {
                thrown.add(own);
                throw sneakyThrow(own);
            }
        }
    }

    /**
     * One client of a scenario: its engine, publishers and failure listener, its lifecycle interceptor, its scripted
     * transport and its test protocol, which return the fresh underlying result instance of each transmission.
     */
    private static final class Client implements SdkSyncHttpClient, SdkAsyncHttpClient, ExecutionInterceptor,
                                                 ClientProtocol {
        /** Per publisher, the configured {@link TelemetryPublisher} (the bridge, for a V2 publisher). */
        final List<TelemetryPublisher> slots;
        private final List<SpiPublisher> spi = new ArrayList<>();
        private final List<V2Publisher> v2 = new ArrayList<>();
        private final Listener listener;
        private final Scenario scenario;
        private final AwsCallEngine engine;
        private List<Outcome> script;
        private List<Integer> attempts;
        private List<Outcome> delivered;
        private List<Object> made;

        Client(Scenario scenario, boolean failing) {
            this.scenario = scenario;
            this.listener = new Listener(failing ? scenario.listener() : ListenerKind.RETURNS);
            TelemetryConfiguration.Builder telemetry = TelemetryConfiguration.builder().failureListener(listener);
            for (PublisherPlan plan : scenario.publishers()) {
                if (plan.kind() == PublisherKind.SPI) {
                    SpiPublisher publisher = new SpiPublisher(plan, failing);
                    spi.add(publisher);
                    v2.add(null);
                    telemetry.addPublisher(publisher);
                } else {
                    V2Publisher publisher = new V2Publisher(plan, failing);
                    spi.add(null);
                    v2.add(publisher);
                    telemetry.addMetricPublisher(publisher);
                }
            }
            TelemetryConfiguration configuration = telemetry.build();
            this.slots = configuration.publishers();
            SdkClientConfiguration clientConfiguration =
                SdkClientConfiguration.builder()
                                      .option(SdkClientOption.CLIENT_ENDPOINT, PipelineFixtures.ENDPOINT)
                                      .option(TelemetryOptions.TELEMETRY, configuration)
                                      .build();
            ClientRuntime.Builder runtime =
                ClientRuntime.builder()
                             .configuration(clientConfiguration)
                             .protocol(this)
                             .transportMode(scenario.driver().mode)
                             .retryStrategy(PipelineFixtures.retries(scenario.maxAttempts()), RETRY_SCOPE)
                             .addInterceptor(this)
                             .telemetryDimensions(scenario.service(), "ScriptedHttp");
            if (scenario.driver() == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            this.engine = (scenario.driver() == Driver.SYNC ? builder.syncHttpClient(this)
                                                            : builder.asyncHttpClient(this)).build();
        }

        /** Runs one call of {@code spec} and returns what it showed. */
        Observation run(CallSpec spec) {
            script = spec.script();
            attempts = new ArrayList<>();
            delivered = new ArrayList<>();
            made = new ArrayList<>();
            int[] receivedMarks = new int[slots.size()];
            int[] thrownMarks = new int[slots.size()];
            for (int p = 0; p < slots.size(); p++) {
                receivedMarks[p] = received(p).size();
                thrownMarks[p] = thrown(p).size();
            }
            int failureMark = listener.failures.size();
            int listenerThrownMark = listener.thrown.size();

            AwsOperation<Struct, Struct> operation =
                PipelineFixtures.operation("com.example#" + spec.operation(), GET_INPUT, GET_OUTPUT);
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(operation);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            Object output = null;
            Throwable failure = null;
            boolean cancelled = false;
            if (scenario.driver() == Driver.SYNC) {
                try {
                    output = engine.executeSync(plan, request, null, null);
                } catch (Throwable thrown) {
                    failure = thrown;
                }
            } else {
                CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
                if (!future.isDone()) {
                    // The call waits on a hanging exchange: the caller cancels it.
                    assertThat(delivered.get(delivered.size() - 1)).as("the waiting attempt of " + spec)
                                                                   .isEqualTo(Outcome.HANG);
                    assertThat(future.cancel(true)).as("cancellation of " + spec).isTrue();
                }
                assertThat(future).as("completion of " + spec).isDone();
                cancelled = future.isCancelled();
                AtomicReference<Object> value = new AtomicReference<>();
                AtomicReference<Throwable> error = new AtomicReference<>();
                future.handle((o, t) -> {
                    value.set(o);
                    error.set(t);
                    return null;
                });
                output = value.get();
                failure = error.get();
            }
            List<List<String>> received = new ArrayList<>();
            List<List<Throwable>> thrown = new ArrayList<>();
            for (int p = 0; p < slots.size(); p++) {
                received.add(List.copyOf(received(p).subList(receivedMarks[p], received(p).size())));
                thrown.add(List.copyOf(thrown(p).subList(thrownMarks[p], thrown(p).size())));
            }
            return new Observation(List.copyOf(attempts), List.copyOf(delivered),
                                   Collections.unmodifiableList(new ArrayList<>(made)), output, failure, cancelled,
                                   received, thrown,
                                   List.copyOf(listener.failures.subList(failureMark, listener.failures.size())),
                                   List.copyOf(listener.thrown.subList(listenerThrownMark, listener.thrown.size())));
        }

        private List<String> received(int p) {
            return spi.get(p) != null ? spi.get(p).received : v2.get(p).received;
        }

        private List<Throwable> thrown(int p) {
            return spi.get(p) != null ? spi.get(p).thrown : v2.get(p).thrown;
        }

        // The lifecycle interceptor.

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            attempts.add(context.attemptNumber());
        }

        // The scripted transport: each transmission creates its underlying result instance.

        private Outcome next() {
            Outcome outcome = script.get(delivered.size());
            delivered.add(outcome);
            return outcome;
        }

        private ScriptedHttp transmission(Outcome outcome) {
            ScriptedHttp http = new ScriptedHttp();
            switch (outcome) {
                case SUCCESS -> {
                    made.add(new Struct(GET_OUTPUT, Map.of("Value", "v" + delivered.size())));
                    http.respond(200, "v");
                }
                case SERVICE_UNAVAILABLE -> respond(http, 503);
                case THROTTLED -> respond(http, 429);
                case BAD_REQUEST -> respond(http, 400);
                case CONNECTION_RESET -> {
                    HttpTransportException failure = HttpTransportException.create(
                        HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset"));
                    made.add(failure);
                    http.fail(failure);
                }
                case UNMODELED_FAILURE -> {
                    IllegalStateException failure = new IllegalStateException(UNMODELED_MESSAGE);
                    made.add(failure);
                    http.fail(failure);
                }
                case HANG -> throw new IllegalStateException("a sync call cannot hang");
            }
            return http;
        }

        /** An error status, whose modeled exception instance the protocol returns. */
        private void respond(ScriptedHttp http, int status) {
            made.add(AwsServiceException.builder()
                                        .statusCode(status)
                                        .message("Service error " + status)
                                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("Error" + status).build())
                                        .build());
            http.respond(status, null);
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return transmission(next()).prepare(request, options);
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            Outcome outcome = next();
            if (outcome == Outcome.HANG) {
                made.add(null);
                // Completes only when cancelled, failing the handler as a real transport does.
                return new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            }
            return transmission(outcome).execute(request, handler, options);
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "ScriptedHttp";
        }

        @Override
        public void close() {
        }

        // The test protocol: returns the current transmission's underlying result instance.

        @Override
        public ShapeId protocolId() {
            return ShapeId.from("aws.protocols#awsJson1_0");
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            return new OperationProtocol() {
                @Override
                public OperationSchemas schemas() {
                    return operation;
                }

                @Override
                public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
                    return SdkHttpRequest.builder()
                                         .method("POST")
                                         .scheme(endpoint.getScheme())
                                         .host(endpoint.getHost())
                                         .body(HttpBody.fromUtf8("a"))
                                         .build();
                }

                @Override
                @SuppressWarnings("unchecked")
                public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response,
                                                                            ShapeBuilder<T> output) {
                    return (T) made.get(made.size() - 1);
                }

                @Override
                public AwsServiceException deserializeError(SdkHttpResponse response) {
                    return (AwsServiceException) made.get(made.size() - 1);
                }
            };
        }
    }
}
