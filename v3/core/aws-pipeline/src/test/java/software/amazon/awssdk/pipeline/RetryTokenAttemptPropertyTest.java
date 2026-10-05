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
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;
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
import software.amazon.awssdk.pipeline.PipelineFixtures.CountingTimer;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/**
 * Property 31: Retry tokens and attempts correspond one-to-one (Requirements 10.6, 10.7, 10.8, 13.11).
 *
 * <p>For any sequence of retry decisions, attempts are numbered {@code 1..N}, every granted retry token authorizes
 * exactly one subsequent attempt, and no attempt occurs without a token. Each try runs one call of a generated
 * scenario through {@link AwsCallEngine} with a real {@link StandardRetryStrategy} wrapped by
 * {@link RecordingRetryStrategy} (the token ledger) and by {@link TokenAudit} (which follows the token instances), and
 * compares what happened with {@link TokenLedger}: an independent state machine of the token lifecycle written from
 * the design's "Stage order and retry ownership" and "State invariants" 2 and 4, not from the engine. Checks:
 * <ul>
 *   <li>the strategy's ledger ({@code acquire#1}, {@code retry#n}, {@code stop#n}, {@code success#n},
 *       {@code failure#n}) equals the oracle's, entry by entry, so each token is consumed exactly once on every
 *       terminal path: success, stop (non-retryable or attempts exhausted), and caller cancellation;</li>
 *   <li>the attempt numbers the {@link StageObserver} reports, and the attempt context's number (Requirement 10.8),
 *       are {@code 1..N} in order, as the oracle predicts, and never more than maxAttempts;</li>
 *   <li>the transport receives exactly the oracle's transmissions, one per attempt that reaches it;</li>
 *   <li>live, when an attempt starts and when it transmits: its token is the strategy's most recently granted token
 *       instance, still {@link RetryToken.State#ACTIVE} and not yet passed back, so no attempt starts or transmits
 *       without a granted token (Requirement 10.7);</li>
 *   <li>each granted token instance authorizes exactly one attempt (Requirement 10.6), except the token a caller's
 *       cancellation during the retry delay released before its attempt began, which authorizes none; and each is
 *       passed back to the strategy exactly once and ends in a terminal state.</li>
 * </ul>
 *
 * <p>A scenario picks the sync or async driver; signed (an identity resolution per attempt) or unsigned auth;
 * maxAttempts 1-4 with one scripted outcome per attempt: success, a retryable 503 or 429 (throttling), a
 * non-retryable 400, a retryable connection reset, or (signed only) a retryable identity failure; and, for the async
 * driver, an optional caller cancellation while attempt {@code n} waits for its identity, its exchange, or the retry
 * delay after it. A cancellation point the call never reaches is a late cancellation: it must fail and change nothing.
 * The async driver runs on a {@link ManualExecutor} with every identity, exchange and retry-delay wait held until the
 * test releases it, so each try is deterministic on the test thread and its jqwik seed reproduces it.
 */
class RetryTokenAttemptPropertyTest {
    private static final Identity IDENTITY = new Identity() {
    };
    private static final String SCHEME_ID = "test#scripted";
    private static final AwsOperation<Struct, Struct> UNSIGNED_GET = operation("UnsignedGet", false);
    private static final AwsOperation<Struct, Struct> SIGNED_GET = operation("SignedGet", true);
    private static final int MAX_ATTEMPTS = 4;

    /** The scripted outcome of one attempt. */
    enum Outcome {
        SUCCESS(false),
        /** HTTP 503: a transient service error. */
        SERVICE_UNAVAILABLE(true),
        /** HTTP 429: throttling. */
        THROTTLED(true),
        /** HTTP 400: not retryable. */
        BAD_REQUEST(false),
        /** A connection reset while connecting: a transient transport failure. */
        CONNECTION_RESET(true),
        /** The identity provider fails with an I/O error: transient; nothing is transmitted. Signed only. */
        IDENTITY_UNAVAILABLE(true);

        final boolean retryable;

        Outcome(boolean retryable) {
            this.retryable = retryable;
        }
    }

    /** Where an async attempt waits. */
    enum Point {
        IDENTITY, EXCHANGE, DELAY
    }

    /** How a call ended; the cancelled endings name where the cancellation landed. */
    enum Ending {
        SUCCEEDED, NON_RETRYABLE, ATTEMPTS_EXHAUSTED, CANCELLED_AT_IDENTITY, CANCELLED_AT_EXCHANGE, CANCELLED_AT_DELAY;

        boolean cancelled() {
            return this == CANCELLED_AT_IDENTITY || this == CANCELLED_AT_EXCHANGE || this == CANCELLED_AT_DELAY;
        }
    }

    /** A caller cancellation while {@code attempt} waits at {@code point} (for the delay: the delay after it). */
    record Cancel(int attempt, Point point) {
    }

    /**
     * One generated call.
     *
     * @param outcomes the outcome of attempt {@code n} at index {@code n - 1}; one per allowed attempt
     * @param cancel the caller's cancellation; async only, else null
     */
    record Scenario(Driver driver, boolean signed, List<Outcome> outcomes, Cancel cancel) {
        int maxAttempts() {
            return outcomes.size();
        }

        Outcome outcome(int attempt) {
            return outcomes.get(attempt - 1);
        }

        boolean cancelsAt(int attempt, Point point) {
            return cancel != null && cancel.equals(new Cancel(attempt, point));
        }
    }

    /**
     * What the oracle predicts, or what a call did.
     *
     * @param ledger the token ledger, in RecordingRetryStrategy's format
     * @param attempts the attempt numbers started, in order
     * @param transmitted the attempt number of each transmission, in order
     */
    record Run(List<String> ledger, List<Integer> attempts, List<Integer> transmitted, Ending ending) {
    }

    // Feature: aws-sdk-java-v3, Property 31: Retry tokens and attempts correspond one-to-one
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if any terminal path, retries or any outcome kind stop being generated.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 31: Retry tokens and attempts correspond one-to-one")
    void retryTokensAndAttemptsCorrespondOneToOne(@ForAll("scenarios") Scenario scenario) {
        Run expected = TokenLedger.predict(scenario);

        Call call = new Call(scenario);
        Run observed = call.run();

        String description = scenario + "\n  expected: " + expected + "\n  observed: " + observed;
        assertThat(call.violations).as("live attempt violations of " + description).isEmpty();
        assertThat(call.audit.violations).as("live token violations of " + description).isEmpty();
        assertThat(observed.ledger()).as("ledger of " + description).containsExactlyElementsOf(expected.ledger());
        assertThat(observed.attempts()).as("attempts of " + description)
                                       .containsExactlyElementsOf(expected.attempts())
                                       .containsExactlyElementsOf(oneTo(observed.attempts().size()));
        assertThat(observed.attempts().size()).as("attempts of " + description)
                                              .isLessThanOrEqualTo(scenario.maxAttempts());
        assertThat(observed.transmitted()).as("transmissions of " + description)
                                          .containsExactlyElementsOf(expected.transmitted());
        assertThat(observed.ending()).as("ending of " + description).isEqualTo(expected.ending());
        call.audit.assertCorrespondence(call.startedTokens, observed.ending(), description);

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("ending").collect(expected.ending());
        Statistics.label("ending").coverage(c -> {
            c.check(Ending.SUCCEEDED).percentage(p -> p >= 15);
            c.check(Ending.NON_RETRYABLE).percentage(p -> p >= 5);
            c.check(Ending.ATTEMPTS_EXHAUSTED).percentage(p -> p >= 5);
            c.check(Ending.CANCELLED_AT_IDENTITY).percentage(p -> p >= 1);
            c.check(Ending.CANCELLED_AT_EXCHANGE).percentage(p -> p >= 3);
            c.check(Ending.CANCELLED_AT_DELAY).percentage(p -> p >= 2);
        });
        Statistics.label("attempts").collect(expected.attempts().size());
        Statistics.label("retried").collect(expected.attempts().size() > 1);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("late cancellation").collect(scenario.cancel() != null && !expected.ending().cancelled());
        Statistics.label("late cancellation").coverage(c -> c.check(true).percentage(p -> p >= 3));
        for (Outcome outcome : Outcome.values()) {
            List<Outcome> reached = scenario.outcomes().subList(0, expected.attempts().size());
            String label = "an attempt's outcome was " + outcome;
            Statistics.label(label).collect(reached.contains(outcome));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 3));
        }
    }

    @Example
    @Label("Property 31 (oracle): matches the engine examples' ledgers")
    void oracleMatchesTheEngineExamples() {
        List<Outcome> outcomes = List.of(Outcome.SERVICE_UNAVAILABLE, Outcome.SUCCESS, Outcome.SUCCESS);
        Run delay = TokenLedger.predict(new Scenario(Driver.ASYNC, false, outcomes, new Cancel(1, Point.DELAY)));
        assertThat(delay.ledger()).containsExactly("acquire#1", "retry#1", "failure#2");
        assertThat(delay.attempts()).containsExactly(1);
        Run exchange = TokenLedger.predict(new Scenario(Driver.ASYNC, false, outcomes, new Cancel(1, Point.EXCHANGE)));
        assertThat(exchange.ledger()).containsExactly("acquire#1", "failure#1");
        Run retried = TokenLedger.predict(new Scenario(Driver.SYNC, false, outcomes, null));
        assertThat(retried.ledger()).containsExactly("acquire#1", "retry#1", "success#2");
        assertThat(retried.transmitted()).containsExactly(1, 2);
        Run stopped = TokenLedger.predict(new Scenario(Driver.SYNC, false, List.of(Outcome.BAD_REQUEST), null));
        assertThat(stopped.ledger()).containsExactly("acquire#1", "stop#1");
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.of(true, false).flatMap(signed -> outcomes(signed).list().ofMinSize(1)
            .ofMaxSize(MAX_ATTEMPTS).flatMap(outcomes -> Combinators.combine(
                PipelineArbitraries.drivers(), cancels(signed, outcomes.size()))
                .as((driver, cancel) -> new Scenario(driver, signed, outcomes,
                                                     driver == Driver.ASYNC ? cancel : null))));
    }

    private static Arbitrary<Outcome> outcomes(boolean signed) {
        List<Tuple.Tuple2<Integer, Outcome>> frequencies = new ArrayList<>(List.of(
            Tuple.of(3, Outcome.SUCCESS), Tuple.of(2, Outcome.SERVICE_UNAVAILABLE), Tuple.of(2, Outcome.THROTTLED),
            Tuple.of(1, Outcome.BAD_REQUEST), Tuple.of(2, Outcome.CONNECTION_RESET)));
        if (signed) {
            frequencies.add(Tuple.of(2, Outcome.IDENTITY_UNAVAILABLE));
        }
        return Arbitraries.frequency(frequencies);
    }

    private static Arbitrary<Cancel> cancels(boolean signed, int maxAttempts) {
        // The delay is weighted double and fewer calls go uncancelled: a delay cancellation lands only after a
        // retryable attempt that is not the last, so with uniform points it sat just above its 2% coverage minimum.
        Arbitrary<Point> points = signed
                                  ? Arbitraries.frequency(Tuple.of(1, Point.IDENTITY), Tuple.of(1, Point.EXCHANGE),
                                                          Tuple.of(2, Point.DELAY))
                                  : Arbitraries.frequency(Tuple.of(1, Point.EXCHANGE), Tuple.of(2, Point.DELAY));
        return Combinators.combine(Arbitraries.integers().between(1, maxAttempts), points).as(Cancel::new)
                          .injectNull(0.25);
    }

    private static List<Integer> oneTo(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    private static AwsOperation<Struct, Struct> operation(String name, boolean signed) {
        String id = "com.example#" + name;
        ModeledAuth auth = signed ? ModeledAuth.builder().operationId(id).addServiceScheme(SCHEME_ID).build() : null;
        return PipelineFixtures.operation(id, GET_INPUT, GET_OUTPUT, auth);
    }

    /**
     * The reference token lifecycle: at most one token is outstanding (granted and not yet passed back); the initial
     * acquisition grants the token of attempt 1; an attempt may start only with the outstanding token, and only once;
     * passing the token back as {@code success}, {@code stop} or {@code retry} requires that its attempt ran, and
     * {@code retry} grants the token of the next attempt; {@code failure} (a cancellation's release) consumes the
     * outstanding token whether or not its attempt began. A call ends with no token outstanding. Each transition
     * enforces its precondition, so the oracle cannot predict a run that breaks the invariants it checks.
     */
    static final class TokenLedger {
        private final List<String> entries = new ArrayList<>();
        private final List<Integer> attempts = new ArrayList<>();
        /** The attempt number of the most recently granted token; 0 before the acquisition. */
        private int granted;
        private boolean outstanding;
        private boolean used;

        /** The walk of a call of {@code scenario} through the token lifecycle. */
        static Run predict(Scenario scenario) {
            TokenLedger ledger = new TokenLedger();
            List<Integer> transmitted = new ArrayList<>();
            ledger.acquire();
            for (int n = 1; ; n++) {
                ledger.start(n);
                Outcome outcome = scenario.outcome(n);
                if (scenario.signed() && scenario.cancelsAt(n, Point.IDENTITY)) {
                    return ledger.end("failure", transmitted, Ending.CANCELLED_AT_IDENTITY);
                }
                if (outcome != Outcome.IDENTITY_UNAVAILABLE) {
                    transmitted.add(n);
                    if (scenario.cancelsAt(n, Point.EXCHANGE)) {
                        return ledger.end("failure", transmitted, Ending.CANCELLED_AT_EXCHANGE);
                    }
                    if (outcome == Outcome.SUCCESS) {
                        return ledger.end("success", transmitted, Ending.SUCCEEDED);
                    }
                }
                if (!outcome.retryable) {
                    return ledger.end("stop", transmitted, Ending.NON_RETRYABLE);
                }
                if (n == scenario.maxAttempts()) {
                    return ledger.end("stop", transmitted, Ending.ATTEMPTS_EXHAUSTED);
                }
                ledger.consume("retry");
                if (scenario.cancelsAt(n, Point.DELAY)) {
                    return ledger.end("failure", transmitted, Ending.CANCELLED_AT_DELAY);
                }
            }
        }

        private void acquire() {
            require(granted == 0, "one initial acquisition");
            granted = 1;
            outstanding = true;
            used = false;
            entries.add("acquire#1");
        }

        private void start(int attempt) {
            require(outstanding && !used && attempt == granted, "attempt " + attempt + " has its own token");
            used = true;
            attempts.add(attempt);
        }

        private void consume(String kind) {
            require(outstanding, kind + " consumes an outstanding token");
            require(used || kind.equals("failure"), kind + " follows the token's attempt");
            entries.add(kind + "#" + granted);
            outstanding = false;
            if (kind.equals("retry")) {
                granted++;
                outstanding = true;
                used = false;
            }
        }

        private Run end(String kind, List<Integer> transmitted, Ending ending) {
            consume(kind);
            require(!outstanding, "no token outlives the call");
            return new Run(List.copyOf(entries), List.copyOf(attempts), List.copyOf(transmitted), ending);
        }

        private static void require(boolean condition, String invariant) {
            if (!condition) {
                throw new IllegalStateException("Token lifecycle violated: " + invariant);
            }
        }
    }

    /**
     * Wraps the recording strategy to follow the token instances: the order they were granted in, and how many times
     * each was passed back. Called only on the test thread.
     */
    private static final class TokenAudit implements RetryStrategy {
        final RecordingRetryStrategy recording;
        final List<RetryToken> granted = new ArrayList<>();
        final Map<RetryToken, Integer> passedBack = new IdentityHashMap<>();
        final List<String> violations = new ArrayList<>();

        TokenAudit(RecordingRetryStrategy recording) {
            this.recording = recording;
        }

        /** The most recently granted token, or null before the acquisition. */
        RetryToken latest() {
            return granted.isEmpty() ? null : granted.get(granted.size() - 1);
        }

        boolean outstanding(RetryToken token) {
            return token == latest() && !passedBack.containsKey(token) && token.state() == RetryToken.State.ACTIVE;
        }

        @Override
        public int maxAttempts() {
            return recording.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            if (!granted.isEmpty()) {
                violations.add("a second initial acquisition");
            }
            AcquireInitialTokenResponse response = recording.acquireInitialToken(request);
            granted.add(response.token());
            return response;
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            passBack(token, "refresh");
            RetryDecision decision = recording.refreshRetryToken(token, failure);
            if (decision instanceof RetryDecision.Retry retry) {
                granted.add(retry.token());
            }
            return decision;
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            passBack(token, "success");
            return recording.recordSuccess(token);
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            passBack(token, "failure");
            return recording.recordFailure(token);
        }

        private void passBack(RetryToken token, String how) {
            if (!outstanding(token)) {
                violations.add(how + " of " + token + ", not the outstanding token; granted " + granted);
            }
            passedBack.merge(token, 1, Integer::sum);
        }

        /**
         * Each granted token was passed back exactly once and ended in a terminal state, and authorized exactly one
         * attempt, in grant order, except a last token a cancellation during its retry delay released.
         */
        void assertCorrespondence(List<RetryToken> started, Ending ending, String description) {
            for (RetryToken token : granted) {
                assertThat(passedBack.get(token)).as("pass-backs of " + token + " in " + description).isEqualTo(1);
                assertThat(token.state()).as("state of " + token + " in " + description)
                                         .isNotEqualTo(RetryToken.State.ACTIVE);
            }
            List<RetryToken> authorizing = ending == Ending.CANCELLED_AT_DELAY
                                           ? granted.subList(0, granted.size() - 1) : granted;
            assertThat(started).as("tokens that started attempts in " + description)
                               .usingElementComparator((a, b) -> a == b ? 0 : 1)
                               .containsExactlyElementsOf(authorizing);
        }
    }

    /** A held async wait: the event that ends it, and which attempt waits at which point. */
    private record Held(Point point, int attempt, Runnable release) {
    }

    /**
     * One call of a scenario: its engine, its scripted identity provider and transport (one outcome per attempt, the
     * attempt read from the stage observer), and what it observed. For the async driver every wait is held in
     * {@link #pending} until {@link #run} releases it.
     */
    private static final class Call implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<String> violations = new ArrayList<>();
        final List<RetryToken> startedTokens = new ArrayList<>();
        final TokenAudit audit;
        private final Scenario scenario;
        private final boolean async;
        private final RecordingRetryStrategy recording;
        private final List<Integer> attempts = new ArrayList<>();
        private final List<Integer> transmitted = new ArrayList<>();
        private final Deque<Held> pending = new ArrayDeque<>();
        private final ManualExecutor executor = new ManualExecutor();
        private final ManualScheduler scheduler = new ManualScheduler();
        private int heldTimers;
        private int attempt;

        Call(Scenario scenario) {
            this.scenario = scenario;
            this.async = scenario.driver() == Driver.ASYNC;
            this.recording = new RecordingRetryStrategy(retryStrategy());
            this.audit = new TokenAudit(recording);
        }

        Run run() {
            ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(audit, scenario.driver().mode);
            if (scenario.signed()) {
                runtime.authSchemeSelector(AuthSchemeSelector.create(List.of(new ScriptedScheme(this::identity))));
            }
            if (async) {
                runtime.completionExecutor(executor).retryScheduler(scheduler);
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                         .stageObserver(this::entered);
            AwsCallEngine engine = (async ? builder.asyncHttpClient(this) : builder.syncHttpClient(this)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(scenario.signed() ? SIGNED_GET : UNSIGNED_GET);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            if (!async) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return observed(Ending.SUCCEEDED);
                } catch (RuntimeException failure) {
                    return observed(stopEnding());
                }
            }
            return runAsync(engine.executeAsync(plan, request, null, null));
        }

        /**
         * Drives the async call wait by wait. At the scenario's cancellation point the caller cancels instead of
         * releasing the wait; then the late events of every held wait are delivered, and nothing may change. A
         * cancellation point the call never reached is tried after the outcome: it fails and changes nothing.
         */
        private Run runAsync(CompletableFuture<Struct> future) {
            drain();
            Ending cancelled = null;
            while (!future.isDone()) {
                assertThat(pending).as("held waits of " + scenario + " at " + recording.ledger).hasSize(1);
                Held held = pending.remove();
                if (new Cancel(held.attempt(), held.point()).equals(scenario.cancel())) {
                    assertThat(future.cancel(true)).as("cancellation at " + held + " of " + scenario).isTrue();
                    drain();
                    Run frozen = observed(null);
                    held.release().run();
                    drain();
                    while (!pending.isEmpty()) {
                        pending.remove().release().run();
                        drain();
                    }
                    assertThat(observed(null)).as("late events after cancelling " + scenario).isEqualTo(frozen);
                    cancelled = switch (held.point()) {
                        case IDENTITY -> Ending.CANCELLED_AT_IDENTITY;
                        case EXCHANGE -> Ending.CANCELLED_AT_EXCHANGE;
                        case DELAY -> Ending.CANCELLED_AT_DELAY;
                    };
                    break;
                }
                held.release().run();
                drain();
            }
            if (cancelled != null) {
                assertThat(future).as(scenario.toString()).isCancelled();
                return observed(cancelled);
            }
            assertThat(pending).as("waits outliving " + scenario).isEmpty();
            if (scenario.cancel() != null) {
                Run settled = observed(null);
                assertThat(future.cancel(true)).as("late cancellation of " + scenario).isFalse();
                drain();
                assertThat(observed(null)).as("after a late cancellation of " + scenario).isEqualTo(settled);
            }
            return observed(future.isCompletedExceptionally() ? stopEnding() : Ending.SUCCEEDED);
        }

        /** The ending of a call that stopped: exhausted if its last attempt's failure was retryable. */
        private Ending stopEnding() {
            return scenario.outcome(attempt).retryable ? Ending.ATTEMPTS_EXHAUSTED : Ending.NON_RETRYABLE;
        }

        private Run observed(Ending ending) {
            return new Run(List.copyOf(recording.ledger), List.copyOf(attempts), List.copyOf(transmitted), ending);
        }

        /** Runs the queued driver work, then holds each retry delay it scheduled. */
        private void drain() {
            executor.runAll();
            for (; heldTimers < scheduler.timers.size(); heldTimers++) {
                CountingTimer timer = scheduler.timers.get(heldTimers);
                pending.add(new Held(Point.DELAY, attempt, () -> timer.complete(null)));
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

        /** The stage observer: detects each attempt's start and checks it holds the outstanding token. */
        private void entered(CallState<?, ?> call, AttemptState attemptState, PipelineStage stage) {
            if (attemptState == null || attemptState.number() == attempt) {
                return;
            }
            int number = attemptState.number();
            String at = "attempt " + number + " at " + stage + " after " + recording.ledger;
            if (number != attempt + 1) {
                violations.add("attempt " + number + " follows attempt " + attempt);
            }
            if (attemptState.context().attemptNumber() != number) {
                violations.add("the context of " + at + " has number " + attemptState.context().attemptNumber());
            }
            RetryToken token = attemptState.token();
            if (!audit.outstanding(token)) {
                violations.add(at + " started without the outstanding granted token: " + token);
            }
            if (startedTokens.stream().anyMatch(started -> started == token)) {
                violations.add(at + " reuses the token of an earlier attempt: " + token);
            }
            String grant = number == 1 ? "acquire#1" : "retry#" + (number - 1);
            if (recording.ledger.isEmpty() || !recording.ledger.get(recording.ledger.size() - 1).equals(grant)) {
                violations.add(at + " does not follow its grant " + grant);
            }
            startedTokens.add(token);
            attempts.add(number);
            attempt = number;
        }

        /** Records one transmission of the current attempt, which must still hold its outstanding token. */
        private void transmitting() {
            RetryToken token = startedTokens.isEmpty() ? null : startedTokens.get(startedTokens.size() - 1);
            if (token == null || !audit.outstanding(token)) {
                violations.add("transmission of attempt " + attempt + " without its outstanding token, after "
                               + recording.ledger);
            }
            transmitted.add(attempt);
        }

        /** Delivers {@code event} now (sync), or holds it until the test releases the driver's wait (async). */
        private void hold(Point point, Runnable event) {
            if (async) {
                pending.add(new Held(point, attempt, event));
            } else {
                event.run();
            }
        }

        private CompletableFuture<Identity> identity() {
            CompletableFuture<Identity> identity = new CompletableFuture<>();
            if (scenario.outcome(attempt) == Outcome.IDENTITY_UNAVAILABLE) {
                UncheckedIOException failure =
                    new UncheckedIOException(new IOException("The credentials endpoint is unreachable"));
                hold(Point.IDENTITY, () -> identity.completeExceptionally(failure));
            } else {
                hold(Point.IDENTITY, () -> identity.complete(IDENTITY));
            }
            return identity;
        }

        /** A transport scripted with the current attempt's outcome. */
        private ScriptedHttp transmission() {
            ScriptedHttp http = new ScriptedHttp();
            return switch (scenario.outcome(attempt)) {
                case SERVICE_UNAVAILABLE -> http.respond(503, null);
                case THROTTLED -> http.respond(429, null);
                case BAD_REQUEST -> http.respond(400, null);
                case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                    HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset")));
                // An attempt whose identity failed is never transmitted; if it were, the transmissions would show it.
                case SUCCESS, IDENTITY_UNAVAILABLE -> http.respond(200, "v");
            };
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            transmitting();
            return transmission().prepare(request, options);
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            transmitting();
            ScriptedHttp transmission = transmission();
            hold(Point.EXCHANGE, () -> transmission.execute(request, handler, options));
            return new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "RetryTokenAttemptPropertyTest";
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
}
