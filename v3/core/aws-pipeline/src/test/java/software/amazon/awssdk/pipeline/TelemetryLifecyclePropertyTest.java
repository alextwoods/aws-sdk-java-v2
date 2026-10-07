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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
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
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
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
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryOptions;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * Property 67: Telemetry records correspond to lifecycle (Requirements 21.2, 21.3, 21.4).
 *
 * <p>For any call with N attempts and configured telemetry, exactly one terminal call record and N terminal attempt
 * records are emitted, each containing all required call/attempt dimensions. Each try builds one client with
 * telemetry (one to three publishers, a generated service ID and HTTP implementation name, and a standard, adaptive
 * or custom retry strategy of maxAttempts 1-5 without backoff) and runs one to three sequential calls of generated
 * operations through {@link AwsCallEngine} on the sync or async driver. Each call scripts one transport outcome per
 * allowed attempt: success, a retryable 503 or 429 (throttling), a non-retryable modeled 400, a retryable connection
 * reset, an unmodeled transport failure (an {@link IllegalStateException}), or (async only) an exchange that never
 * completes, which the caller then cancels.
 *
 * <p>The lifecycle trace is recorded independently of the telemetry code: a test interceptor records each attempt's
 * {@code beforeAttempt} and {@code afterTransmission}, the test transport records the outcome it delivered for each
 * transmission, and the caller records how the call ended (output, surfaced failure, or cancellation). The expected
 * records are derived from that trace alone; the checks:
 * <ul>
 *   <li>every publisher receives exactly one {@link CallRecord} per call, last, and exactly one
 *       {@link AttemptRecord} per attempt the trace shows, numbered {@code 1..N} in order (21.2, 21.3);</li>
 *   <li>each attempt record is emitted after its attempt began and before the next one began, and a call that
 *       completes has its call record before the caller observes its result;</li>
 *   <li>every record carries the client's service ID, the operation name, the HTTP implementation and the retry mode
 *       ({@code standard}, {@code adaptive} or the custom strategy's class name), its outcome, the attempt number,
 *       a non-negative duration, and the call record the attempt count and its attempt records (21.4); the attempt's
 *       HTTP status and the V2 error category also match the transport's outcome.</li>
 * </ul>
 * The async driver completes inline ({@code Runnable::run}) with zero-delay retries, so each try is deterministic on
 * the test thread and its jqwik seed reproduces it.
 */
class TelemetryLifecyclePropertyTest {
    private static final int MAX_ATTEMPTS = 5;
    private static final String RETRY_SCOPE = "things";

    /** The scripted transport outcome of one attempt. */
    enum Outcome {
        SUCCESS(OptionalInt.of(200), Optional.empty()),
        /** HTTP 503: a retryable service error. */
        SERVICE_UNAVAILABLE(OptionalInt.of(503), Optional.of("ServerError")),
        /** HTTP 429: throttling, retryable. */
        THROTTLED(OptionalInt.of(429), Optional.of("Throttling")),
        /** HTTP 400: a modeled, non-retryable service error. */
        BAD_REQUEST(OptionalInt.of(400), Optional.of("ServerError")),
        /** A connection reset while connecting: a retryable transport failure; no response. */
        CONNECTION_RESET(OptionalInt.empty(), Optional.of("IO")),
        /** An unmodeled failure of the transport component; no response. */
        UNMODELED_FAILURE(OptionalInt.empty(), Optional.of("Other")),
        /** Async only: the exchange never completes; the caller cancels the call. */
        HANG(OptionalInt.empty(), Optional.empty());

        /** The HTTP status the attempt receives; empty when it receives no response. */
        final OptionalInt status;
        /** The V2 error category of the attempt's failure (SdkErrorType); empty for success and cancellation. */
        final Optional<String> errorType;

        Outcome(OptionalInt status, Optional<String> errorType) {
            this.status = status;
            this.errorType = errorType;
        }
    }

    /** The client's retry strategy kind. */
    enum RetryKind {
        STANDARD, ADAPTIVE, CUSTOM
    }

    /** How a call ended, from its lifecycle trace; for the statistics. */
    enum Ending {
        SUCCEEDED, STOPPED, ATTEMPTS_EXHAUSTED, CANCELLED
    }

    /**
     * One generated call.
     *
     * @param operation the operation name
     * @param script the transport outcome of attempt {@code n} at index {@code n - 1}; one per allowed attempt
     */
    record CallSpec(String operation, List<Outcome> script) {
    }

    /** One generated client and the calls made on it, in order. */
    record Scenario(Driver driver, RetryKind retry, int maxAttempts, String service, String httpImplementation,
                    int publishers, List<CallSpec> calls) {
    }

    /**
     * The lifecycle trace of one call, recorded without the telemetry code.
     *
     * @param attempts the attempt numbers whose {@code beforeAttempt} ran, in order
     * @param delivered the transport outcome of each transmission, in order
     * @param transmittedStatuses the status each {@code afterTransmission} observed, or empty for a failure
     * @param outcome how the caller observed the call to end
     */
    record Trace(List<Integer> attempts, List<Outcome> delivered, List<OptionalInt> transmittedStatuses,
                 TelemetryOutcome outcome) {
    }

    // Feature: aws-sdk-java-v3, Property 67: Telemetry records correspond to lifecycle
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if any ending, retries, retry kind or outcome kind stop being generated.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 67: Telemetry records correspond to lifecycle")
    void telemetryRecordsCorrespondToLifecycle(@ForAll("scenarios") Scenario scenario) {
        Client client = new Client(scenario);
        for (CallSpec spec : scenario.calls()) {
            int[] marks = client.marks();
            Trace trace = client.run(spec);
            String description = scenario + "\n  call: " + spec + "\n  trace: " + trace + "\n  timeline: "
                                 + client.timeline;

            assertTraceIsALifecycle(scenario, spec, trace, description);
            List<Object> first = client.publishers.get(0).since(marks[0]);
            for (int p = 1; p < client.publishers.size(); p++) {
                assertThat(client.publishers.get(p).since(marks[p])).as("publisher " + p + " of " + description)
                                                                    .containsExactlyElementsOf(first);
            }
            assertRecordsMatch(scenario, spec, trace, first, description);
            assertTimeline(trace, client.timelineSince(marks[marks.length - 1]), description);
            if (trace.outcome() != TelemetryOutcome.CANCELLED) {
                assertThat(client.callRecordsAtCompletion).as("call records when the caller observed " + description)
                                                          .isEqualTo(1);
            }

            Ending ending = ending(scenario, trace);
            Statistics.label("ending").collect(ending);
            Statistics.label("ending").coverage(c -> {
                c.check(Ending.SUCCEEDED).percentage(p -> p >= 15);
                c.check(Ending.STOPPED).percentage(p -> p >= 5);
                c.check(Ending.ATTEMPTS_EXHAUSTED).percentage(p -> p >= 5);
                c.check(Ending.CANCELLED).percentage(p -> p >= 2);
            });
            Statistics.label("attempts").collect(trace.attempts().size());
            Statistics.label("retried").collect(trace.attempts().size() > 1);
            Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 20));
            for (Outcome outcome : Outcome.values()) {
                String label = "an attempt's outcome was " + outcome;
                Statistics.label(label).collect(trace.delivered().contains(outcome));
                Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 2));
            }
        }
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("retry").collect(scenario.retry());
        Statistics.label("retry").coverage(c -> {
            for (RetryKind kind : RetryKind.values()) {
                c.check(kind).percentage(p -> p >= 15);
            }
        });
        Statistics.label("publishers").collect(scenario.publishers());
        Statistics.label("calls").collect(scenario.calls().size());
    }

    /** The trace itself is a lifecycle: attempts 1..N, one transmission each, the script's prefix, N <= max. */
    private static void assertTraceIsALifecycle(Scenario scenario, CallSpec spec, Trace trace, String description) {
        int n = trace.attempts().size();
        assertThat(n).as("attempts of " + description).isBetween(1, scenario.maxAttempts());
        assertThat(trace.attempts()).as("attempt numbers of " + description).containsExactlyElementsOf(oneTo(n));
        assertThat(trace.delivered()).as("transmissions of " + description)
                                     .containsExactlyElementsOf(spec.script().subList(0, n));
        List<OptionalInt> responded = trace.delivered().stream().filter(o -> o != Outcome.HANG)
                                           .map(o -> o.status).toList();
        assertThat(trace.transmittedStatuses()).as("afterTransmission of " + description)
                                               .containsExactlyElementsOf(responded);
    }

    /** Exactly the records the trace implies, attempt records first, in order, then the call record. */
    private static void assertRecordsMatch(Scenario scenario, CallSpec spec, Trace trace, List<Object> records,
                                           String description) {
        int n = trace.attempts().size();
        assertThat(records).as("records of " + description).hasSize(n + 1);
        String retryMode = retryMode(scenario.retry());
        List<AttemptRecord> attempts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            assertThat(records.get(i)).as("record " + i + " of " + description).isInstanceOf(AttemptRecord.class);
            AttemptRecord attempt = (AttemptRecord) records.get(i);
            attempts.add(attempt);
            Outcome delivered = trace.delivered().get(i);
            TelemetryOutcome outcome = delivered == Outcome.SUCCESS ? TelemetryOutcome.SUCCEEDED
                                       : delivered == Outcome.HANG ? TelemetryOutcome.CANCELLED
                                       : TelemetryOutcome.FAILED;
            assertThat(attempt).as("attempt " + (i + 1) + " of " + description)
                               .extracting(AttemptRecord::service, AttemptRecord::operation,
                                           AttemptRecord::attemptNumber, AttemptRecord::outcome,
                                           AttemptRecord::httpImplementation, AttemptRecord::retryMode,
                                           AttemptRecord::httpStatusCode, AttemptRecord::errorType)
                               .containsExactly(scenario.service(), spec.operation(), trace.attempts().get(i), outcome,
                                                scenario.httpImplementation(), retryMode, delivered.status,
                                                delivered.errorType);
            assertThat(attempt.duration()).as("duration of attempt " + (i + 1) + " of " + description)
                                          .isGreaterThanOrEqualTo(Duration.ZERO);
        }
        assertThat(records.get(n)).as("last record of " + description).isInstanceOf(CallRecord.class);
        CallRecord call = (CallRecord) records.get(n);
        Optional<String> errorType = trace.outcome() == TelemetryOutcome.FAILED
                                     ? trace.delivered().get(n - 1).errorType : Optional.empty();
        assertThat(call).as("call record of " + description)
                        .extracting(CallRecord::service, CallRecord::operation, CallRecord::outcome,
                                    CallRecord::attemptCount, CallRecord::httpImplementation, CallRecord::retryMode,
                                    CallRecord::errorType, CallRecord::attempts)
                        .containsExactly(scenario.service(), spec.operation(), trace.outcome(), n,
                                         scenario.httpImplementation(), retryMode, errorType, attempts);
        Duration attemptsTotal = attempts.stream().map(AttemptRecord::duration).reduce(Duration.ZERO, Duration::plus);
        assertThat(call.duration()).as("call duration of " + description).isGreaterThanOrEqualTo(attemptsTotal);
    }

    /** Each attempt record lies between its attempt's start and the next attempt's start; the call record is last. */
    private static void assertTimeline(Trace trace, List<String> timeline, String description) {
        int n = trace.attempts().size();
        List<String> expected = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            expected.add("begin#" + i);
            expected.add("record#" + i);
        }
        expected.add("call");
        assertThat(timeline).as("timeline of " + description).containsExactlyElementsOf(expected);
    }

    private static Ending ending(Scenario scenario, Trace trace) {
        return switch (trace.outcome()) {
            case SUCCEEDED -> Ending.SUCCEEDED;
            case CANCELLED -> Ending.CANCELLED;
            case FAILED -> trace.attempts().size() == scenario.maxAttempts() ? Ending.ATTEMPTS_EXHAUSTED
                                                                            : Ending.STOPPED;
        };
    }

    /** The documented retry mode dimension of each strategy kind. */
    private static String retryMode(RetryKind kind) {
        return switch (kind) {
            case STANDARD -> "standard";
            case ADAPTIVE -> "adaptive";
            case CUSTOM -> CustomRetryStrategy.class.getName();
        };
    }

    private static List<Integer> oneTo(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        return Combinators.combine(PipelineArbitraries.drivers(), Arbitraries.of(RetryKind.class),
                                   Arbitraries.integers().between(1, MAX_ATTEMPTS), names, names,
                                   Arbitraries.integers().between(1, 3))
                          .flatAs((driver, retry, maxAttempts, service, http, publishers) ->
                                      callSpecs(driver, maxAttempts, names).list().ofMinSize(1).ofMaxSize(3)
                                          .map(calls -> new Scenario(driver, retry, maxAttempts, service, http,
                                                                     publishers, calls)));
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

    /** A custom strategy (neither standard nor adaptive): standard retries, without backoff, by delegation. */
    private static final class CustomRetryStrategy implements RetryStrategy {
        private final RetryStrategy delegate;

        CustomRetryStrategy(int maxAttempts) {
            this.delegate = PipelineFixtures.retries(maxAttempts);
        }

        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            return delegate.acquireInitialToken(request);
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            return delegate.refreshRetryToken(token, failure);
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            return delegate.recordSuccess(token);
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            return delegate.recordFailure(token);
        }
    }

    /** Records every callback of one publisher, in order; the first publisher also writes the shared timeline. */
    private static final class RecordingPublisher implements TelemetryPublisher {
        final List<Object> records = new ArrayList<>();
        private final List<String> timeline;

        RecordingPublisher(List<String> timeline) {
            this.timeline = timeline;
        }

        @Override
        public void attemptCompleted(AttemptRecord attempt) {
            records.add(attempt);
            if (timeline != null) {
                timeline.add("record#" + attempt.attemptNumber());
            }
        }

        @Override
        public void callCompleted(CallRecord call) {
            records.add(call);
            if (timeline != null) {
                timeline.add("call");
            }
        }

        List<Object> since(int mark) {
            return List.copyOf(records.subList(mark, records.size()));
        }

        long calls() {
            return records.stream().filter(CallRecord.class::isInstance).count();
        }
    }

    /**
     * One client of a scenario: its engine, publishers, lifecycle interceptor and scripted transport (one outcome per
     * transmission, from the current call's script), and the current call's trace.
     */
    private static final class Client implements SdkSyncHttpClient, SdkAsyncHttpClient, ExecutionInterceptor {
        final List<String> timeline = new ArrayList<>();
        final List<RecordingPublisher> publishers = new ArrayList<>();
        /** The call records the first publisher had received when the caller observed the call's result. */
        long callRecordsAtCompletion;
        private final Scenario scenario;
        private final AwsCallEngine engine;
        private List<Outcome> script;
        private List<Integer> attempts;
        private List<Outcome> delivered;
        private List<OptionalInt> transmittedStatuses;
        private long callRecordsBefore;

        Client(Scenario scenario) {
            this.scenario = scenario;
            TelemetryConfiguration.Builder telemetry = TelemetryConfiguration.builder();
            for (int p = 0; p < scenario.publishers(); p++) {
                RecordingPublisher publisher = new RecordingPublisher(p == 0 ? timeline : null);
                publishers.add(publisher);
                telemetry.addPublisher(publisher);
            }
            SdkClientConfiguration configuration =
                SdkClientConfiguration.builder()
                                      .option(SdkClientOption.CLIENT_ENDPOINT, PipelineFixtures.ENDPOINT)
                                      .option(TelemetryOptions.TELEMETRY, telemetry.build())
                                      .build();
            ClientRuntime.Builder runtime =
                ClientRuntime.builder()
                             .configuration(configuration)
                             .protocol(new HeaderProtocol())
                             .transportMode(scenario.driver().mode)
                             .retryStrategy(retryStrategy(scenario), RETRY_SCOPE)
                             .addInterceptor(this)
                             .telemetryDimensions(scenario.service(), scenario.httpImplementation());
            if (scenario.driver() == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            this.engine = (scenario.driver() == Driver.SYNC ? builder.syncHttpClient(this)
                                                            : builder.asyncHttpClient(this)).build();
        }

        private static RetryStrategy retryStrategy(Scenario scenario) {
            return switch (scenario.retry()) {
                case STANDARD -> PipelineFixtures.retries(scenario.maxAttempts());
                case ADAPTIVE -> {
                    // A clock that advances ten seconds per read, so the rate limiter never makes a sync call sleep.
                    AtomicLong clock = new AtomicLong();
                    yield AdaptiveRetryStrategy.builder()
                                               .maxAttempts(scenario.maxAttempts())
                                               .backoffStrategy(BackoffStrategy.none())
                                               .throttlingBackoffStrategy(BackoffStrategy.none())
                                               .nanoTimeSource(() -> clock.addAndGet(Duration.ofSeconds(10).toNanos()))
                                               .build();
                }
                case CUSTOM -> new CustomRetryStrategy(scenario.maxAttempts());
            };
        }

        /** The current record count of each publisher, then the timeline's length. */
        int[] marks() {
            int[] marks = new int[publishers.size() + 1];
            for (int p = 0; p < publishers.size(); p++) {
                marks[p] = publishers.get(p).records.size();
            }
            marks[publishers.size()] = timeline.size();
            return marks;
        }

        List<String> timelineSince(int mark) {
            return List.copyOf(timeline.subList(mark, timeline.size()));
        }

        /** Runs one call of {@code spec} and returns its lifecycle trace. */
        Trace run(CallSpec spec) {
            script = spec.script();
            attempts = new ArrayList<>();
            delivered = new ArrayList<>();
            transmittedStatuses = new ArrayList<>();
            callRecordsBefore = publishers.get(0).calls();
            callRecordsAtCompletion = -1;
            AwsOperation<Struct, Struct> operation =
                PipelineFixtures.operation("com.example#" + spec.operation(), GET_INPUT, GET_OUTPUT);
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(operation);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            TelemetryOutcome outcome;
            if (scenario.driver() == Driver.SYNC) {
                try {
                    engine.executeSync(plan, request, null, null);
                    outcome = TelemetryOutcome.SUCCEEDED;
                } catch (RuntimeException failure) {
                    outcome = TelemetryOutcome.FAILED;
                }
                observedCompletion();
            } else {
                CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
                future.whenComplete((output, failure) -> observedCompletion());
                if (!future.isDone()) {
                    // The call waits on a hanging exchange: the caller cancels it.
                    assertThat(delivered.get(delivered.size() - 1)).as("the waiting attempt of " + spec)
                                                                   .isEqualTo(Outcome.HANG);
                    assertThat(future.cancel(true)).as("cancellation of " + spec).isTrue();
                }
                outcome = future.isCancelled() ? TelemetryOutcome.CANCELLED
                          : future.isCompletedExceptionally() ? TelemetryOutcome.FAILED : TelemetryOutcome.SUCCEEDED;
            }
            return new Trace(List.copyOf(attempts), List.copyOf(delivered), List.copyOf(transmittedStatuses),
                             outcome);
        }

        private void observedCompletion() {
            if (callRecordsAtCompletion < 0) {
                callRecordsAtCompletion = publishers.get(0).calls() - callRecordsBefore;
            }
        }

        // The lifecycle interceptor.

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            attempts.add(context.attemptNumber());
            timeline.add("begin#" + context.attemptNumber());
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            transmittedStatuses.add(context.httpResponse().map(r -> OptionalInt.of(r.statusCode()))
                                           .orElse(OptionalInt.empty()));
        }

        // The scripted transport.

        /** The next scripted outcome, recorded as delivered; a single-outcome transport for all but a hang. */
        private Outcome next() {
            Outcome outcome = script.get(delivered.size());
            delivered.add(outcome);
            return outcome;
        }

        private static ScriptedHttp transmission(Outcome outcome) {
            ScriptedHttp http = new ScriptedHttp();
            return switch (outcome) {
                case SUCCESS -> http.respond(200, "v");
                case SERVICE_UNAVAILABLE -> http.respond(503, null);
                case THROTTLED -> http.respond(429, null);
                case BAD_REQUEST -> http.respond(400, null);
                case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                    HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset")));
                case UNMODELED_FAILURE -> http.fail(new IllegalStateException("transport component bug"));
                case HANG -> throw new IllegalStateException("a sync call cannot hang");
            };
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
            return scenario.httpImplementation();
        }

        @Override
        public void close() {
        }
    }
}
