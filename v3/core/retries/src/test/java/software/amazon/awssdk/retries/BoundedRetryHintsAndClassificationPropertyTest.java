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

package software.amazon.awssdk.retries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.exception.SdkTimeoutException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/**
 * Property 49: Retry hints and classification are bounded (Requirements 13.12, 13.13, 13.14, 13.15).
 *
 * <p>For any response metadata (status code, AWS error code, {@code x-amz-retry-after} hint), modeled retry traits
 * (from an error Schema or set explicitly), failure cause chain and replayability, the default classifier and the
 * standard and adaptive strategies agree with a table-driven oracle written here, independent of the runtime's
 * classifier and hint parser:
 * <ul>
 *   <li>classification follows the ordered 7-row table of {@link RetryClassifier#defaults()}, reproduced below as
 *       literal code/status/category tables, so both Schema traits and response metadata can make a failure
 *       retryable or throttling (13.15);</li>
 *   <li>the attempt delay is the classification's backoff ({@code throttlingBackoffStrategy} for throttling, the
 *       deterministic full-jitter formula computed here) bounded by the hint: a valid in-bound hint is used (13.12), an
 *       above-bound hint is capped at backoff + 5 s (13.13), and an absent or invalid hint (anything other than 1 to 18
 *       ASCII digits of milliseconds) is ignored (13.14). A valid hint below the backoff yields the backoff.</li>
 * </ul>
 * The adaptive mode classifies identically and never waits less than the bounded delay (its rate limiter can only
 * lengthen it). {@link RetryHints#boundedDelay} and {@link RetryHints#parseRetryAfter} are also checked directly
 * against the band table with hints drawn relative to the backoff, including the exact boundaries.
 */
class BoundedRetryHintsAndClassificationPropertyTest {
    private static final String SCOPE = "property-49";

    // ---- Oracle tables: literal values of the documented classification and hint rules. ----

    private static final Set<String> ORACLE_THROTTLING_CODES = Set.of(
        "Throttling", "ThrottlingException", "ThrottledException", "RequestThrottledException",
        "TooManyRequestsException", "ProvisionedThroughputExceededException", "TransactionInProgressException",
        "RequestLimitExceeded", "BandwidthLimitExceeded", "LimitExceededException", "RequestThrottled", "SlowDown",
        "EC2ThrottledException");

    private static final Set<String> ORACLE_TRANSIENT_CODES = Set.of(
        "RequestTimeout", "RequestTimeoutException", "PriorRequestNotComplete", "InternalError",
        "RequestTimeTooSkewed", "RequestExpired", "RequestInTheFuture");

    private static final int ORACLE_THROTTLING_STATUS = 429;

    private static final Set<Integer> ORACLE_TRANSIENT_STATUSES = Set.of(500, 502, 503, 504);

    private static final Map<HttpFailureCategory, Boolean> ORACLE_TRANSPORT_TRANSIENT =
        new EnumMap<>(HttpFailureCategory.class);

    static {
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.CONNECTION_ACQUISITION, true);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.CONNECT, true);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.PROXY, false);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.TLS, false);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.TIMEOUT, true);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.CANCELLED, false);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.REQUEST_TRANSMISSION, true);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.RESPONSE_CONSUMPTION, true);
        ORACLE_TRANSPORT_TRANSIENT.put(HttpFailureCategory.PROTOCOL, false);
    }

    /** A valid hint: 1 to 18 ASCII digits ({@code [0-9]} is ASCII-only in Java regex). */
    private static final Pattern ORACLE_VALID_HINT = Pattern.compile("[0-9]{1,18}");

    private static final Duration ORACLE_HINT_EXTENSION = Duration.ofSeconds(5);

    private static final String ORACLE_HINT_HEADER = "x-amz-retry-after";

    /** Jitter factors are {@code k / 1024}, so {@code ceiling * factor} is exact in double arithmetic. */
    private static final int JITTER_SCALE = 1024;

    /** Where a hint falls relative to the backoff {@code b} and bound {@code b + e}. */
    enum Band {
        IGNORED, BELOW_BACKOFF, WITHIN_BOUND, ABOVE_BOUND
    }

    // ---- Properties ----

    @Property(tries = 3_000, edgeCases = EdgeCasesMode.MIXIN)
    @Label("Property 49: Retry hints and classification are bounded")
    void classificationAndAttemptDelayFollowTheOracle(@ForAll("scenarios") Scenario scenario) {
        AttemptFailure failure = scenario.attemptFailure();
        RetryClassification expected = expectedClassification(scenario);

        assertEquals(expected, RetryClassifier.defaults().classify(failure), scenario::toString);

        Duration transientBackoff = scenario.transientBackoff().expected(scenario.failedAttempt());
        Duration throttlingBackoff = scenario.throttlingBackoff().expected(scenario.failedAttempt());
        Duration backoff = expected == RetryClassification.THROTTLING ? throttlingBackoff : transientBackoff;
        Duration expectedDelay = oracleDelay(backoff, scenario.hint(), ORACLE_HINT_EXTENSION);

        RetryDecision standard = refreshAt(scenario, StandardRetryStrategy.builder(), failure);
        assertDecision(scenario, expected, standard);
        if (expected.isRetryable()) {
            assertEquals(expectedDelay, standard.delay(), scenario::toString);
        }

        RetryDecision adaptive = refreshAt(scenario, AdaptiveRetryStrategy.builder(), failure);
        assertDecision(scenario, expected, adaptive);
        if (expected.isRetryable()) {
            assertTrue(adaptive.delay().compareTo(expectedDelay) >= 0,
                       () -> "adaptive " + adaptive.delay() + " < bounded " + expectedDelay + " for " + scenario);
        }

        Band band = band(backoff, oracleParse(scenario.hint()), ORACLE_HINT_EXTENSION);
        Statistics.label("classification").collect(expected);
        Statistics.label("hint band").collect(band);
        Statistics.label("throttling backoff observable")
                  .collect(expected == RetryClassification.THROTTLING && band != Band.WITHIN_BOUND
                           && !throttlingBackoff.equals(transientBackoff));
        Statistics.label("classification").coverage(c -> {
            for (RetryClassification value : RetryClassification.values()) {
                c.check(value).count(n -> n > 0);
            }
        });
        Statistics.label("hint band").coverage(c -> {
            for (Band value : Band.values()) {
                c.check(value).count(n -> n > 0);
            }
        });
        Statistics.label("throttling backoff observable").coverage(c -> c.check(true).count(n -> n > 0));
    }

    @Property(tries = 2_000, edgeCases = EdgeCasesMode.MIXIN)
    @Label("Property 49: RetryHints.boundedDelay follows the hint band table")
    void boundedDelayFollowsTheBandTable(@ForAll("hintCases") HintCase hintCase) {
        Duration backoff = hintCase.backoff();
        String raw = hintCase.rawHint();
        Optional<Duration> expectedHint = oracleParse(raw);

        assertEquals(expectedHint, RetryHints.parseRetryAfter(raw), hintCase::toString);

        Duration withDefault = RetryHints.boundedDelay(backoff, raw);
        assertEquals(oracleDelay(backoff, raw, ORACLE_HINT_EXTENSION), withDefault, hintCase::toString);

        Duration extension = hintCase.extension();
        Duration withExtension = RetryHints.boundedDelay(backoff, RetryHints.parseRetryAfter(raw), extension);
        assertEquals(oracleDelay(backoff, raw, extension), withExtension, hintCase::toString);
        assertTrue(withExtension.compareTo(backoff) >= 0 && withExtension.compareTo(backoff.plus(extension)) <= 0,
                   hintCase::toString);

        Band band = band(backoff, expectedHint, extension);
        Statistics.label("band").collect(band);
        Statistics.label("band").coverage(c -> {
            for (Band value : Band.values()) {
                c.check(value).count(n -> n > 0);
            }
        });
    }

    @Example
    void oracleTablesMatchTheRuntimeConstants() {
        assertEquals(EnumSet.allOf(HttpFailureCategory.class), ORACLE_TRANSPORT_TRANSIENT.keySet());
        assertEquals(ORACLE_HINT_EXTENSION, RetryHints.HINT_EXTENSION);
        assertEquals(ORACLE_HINT_HEADER, RetryHints.X_AMZ_RETRY_AFTER);
    }

    @Example
    void boundedDelayAnchors() {
        Duration b = Duration.ofSeconds(1);
        assertEquals(Duration.ofMillis(1500), RetryHints.boundedDelay(b, "1500"));
        assertEquals(Duration.ofMillis(1500), RetryHints.boundedDelay(b, "000000000000001500"));
        assertEquals(Duration.ofSeconds(6), RetryHints.boundedDelay(b, "6000"));
        assertEquals(Duration.ofSeconds(6), RetryHints.boundedDelay(b, "999999999999999999"));
        assertEquals(b, RetryHints.boundedDelay(b, "10"));
        assertEquals(b, RetryHints.boundedDelay(b, "0000000000000001500"));
        assertEquals(b, RetryHints.boundedDelay(b, "1.5"));
        assertEquals(b, RetryHints.boundedDelay(b, " 1500"));
        assertEquals(b, RetryHints.boundedDelay(b, ""));
        assertEquals(b, RetryHints.boundedDelay(b, null));
    }

    // ---- Oracle ----

    /** The ordered classification table; the first matching row wins. */
    static RetryClassification expectedClassification(Scenario s) {
        // Row 1: a non-replayable request is never retried.
        if (!s.replayable()) {
            return RetryClassification.NON_RETRYABLE;
        }
        List<Node> chain = s.chain();
        // Row 2: cancellation or the whole-call timeout anywhere in the cause chain.
        for (Node n : chain) {
            if (n.kind() == Kind.CANCELLATION || n.kind() == Kind.INTERRUPTED
                || n.kind() == Kind.TRANSPORT && n.category() == HttpFailureCategory.CANCELLED
                || n.kind() == Kind.TIMEOUT && n.scope() == SdkTimeoutException.Scope.API_CALL) {
                return RetryClassification.NON_RETRYABLE;
            }
        }
        Node top = chain.get(0);
        boolean modeledRetryable;
        boolean modeledThrottling;
        if (s.traitsOverride() != null) {
            modeledRetryable = s.traitsOverride().retryable();
            modeledThrottling = s.traitsOverride().throttling();
        } else if (top.kind() == Kind.MODELED) {
            modeledRetryable = top.schema().retryable;
            modeledThrottling = top.schema().throttling;
        } else {
            modeledRetryable = false;
            modeledThrottling = false;
        }
        // The response status wins; otherwise the status of a service exception that is the failure itself.
        int status = s.responseStatus() != 0 ? s.responseStatus() : top.isService() ? top.status() : 0;
        String code = s.errorCode();
        // Row 3: throttling from the Schema traits or the response metadata.
        if (modeledThrottling
            || status == ORACLE_THROTTLING_STATUS
            || code != null && ORACLE_THROTTLING_CODES.contains(code)
            || top.isService() && top.status() == ORACLE_THROTTLING_STATUS) {
            return RetryClassification.THROTTLING;
        }
        // Row 4: transient service error from the Schema traits or the response metadata.
        if (modeledRetryable
            || ORACLE_TRANSIENT_STATUSES.contains(status)
            || code != null && ORACLE_TRANSIENT_CODES.contains(code)
            || top.hasServiceFlags() && (top.retryableFlag() || top.clockSkewFlag())) {
            return RetryClassification.TRANSIENT;
        }
        // Row 5: a transport failure in the chain, by category.
        for (Node n : chain) {
            if (n.kind() == Kind.TRANSPORT) {
                return ORACLE_TRANSPORT_TRANSIENT.get(n.category()) ? RetryClassification.TRANSIENT
                                                                     : RetryClassification.NON_RETRYABLE;
            }
        }
        // Row 6: attempt timeout or I/O anywhere in the chain, or a failure that says it is retryable.
        for (Node n : chain) {
            if (n.kind() == Kind.TIMEOUT || n.kind() == Kind.IO || n.kind() == Kind.UNCHECKED_IO) {
                return RetryClassification.TRANSIENT;
            }
        }
        if (top.kind() == Kind.RETRYABLE_CLIENT) {
            return RetryClassification.TRANSIENT;
        }
        // Row 7.
        return RetryClassification.NON_RETRYABLE;
    }

    static Optional<Duration> oracleParse(String raw) {
        if (raw == null || !ORACLE_VALID_HINT.matcher(raw).matches()) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(Long.parseLong(raw)));
    }

    static Band band(Duration backoff, Optional<Duration> hint, Duration extension) {
        if (hint.isEmpty()) {
            return Band.IGNORED;
        }
        Duration h = hint.get();
        if (h.compareTo(backoff) < 0) {
            return Band.BELOW_BACKOFF;
        }
        return h.compareTo(backoff.plus(extension)) > 0 ? Band.ABOVE_BOUND : Band.WITHIN_BOUND;
    }

    /** The attempt delay by band: ignored or below-backoff hints use the backoff, in-bound hints are used, others cap. */
    static Duration oracleDelay(Duration backoff, String raw, Duration extension) {
        Optional<Duration> hint = oracleParse(raw);
        return switch (band(backoff, hint, extension)) {
            case IGNORED, BELOW_BACKOFF -> backoff;
            case WITHIN_BOUND -> hint.orElseThrow();
            case ABOVE_BOUND -> backoff.plus(extension);
        };
    }

    // ---- Strategy driving ----

    private static <B extends RetryStrategy.Builder<B, S>, S extends RetryStrategy> RetryDecision refreshAt(
        Scenario s, B builder, AttemptFailure failure) {
        RetryStrategy strategy = builder.maxAttempts(s.failedAttempt() + 1)
                                        .backoffStrategy(s.transientBackoff().strategy())
                                        .throttlingBackoffStrategy(s.throttlingBackoff().strategy())
                                        .build();
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
        AttemptFailure prior = AttemptFailure.of(SdkServiceException.builder().message("prior").statusCode(503)
                                                                    .build());
        for (int attempt = 1; attempt < s.failedAttempt(); attempt++) {
            token = assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(token, prior)).token();
        }
        assertEquals(s.failedAttempt(), token.attempt());
        return strategy.refreshRetryToken(token, failure);
    }

    private static void assertDecision(Scenario s, RetryClassification expected, RetryDecision decision) {
        assertEquals(expected, decision.classification(), s::toString);
        if (expected.isRetryable()) {
            RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class, decision, s::toString);
            assertEquals(s.failedAttempt() + 1, retry.token().attempt(), s::toString);
        } else {
            RetryDecision.Stop stop = assertInstanceOf(RetryDecision.Stop.class, decision, s::toString);
            assertEquals(RetryDecision.StopReason.NON_RETRYABLE, stop.reason(), s::toString);
            assertEquals(Duration.ZERO, stop.delay(), s::toString);
        }
    }

    // ---- Model ----

    enum Kind {
        WRAP_RUNTIME, WRAP_COMPLETION, WRAP_SDK_CLIENT,
        SERVICE, FLAGGED_SERVICE, MODELED,
        TRANSPORT, TIMEOUT, IO, UNCHECKED_IO, CANCELLATION, INTERRUPTED, RETRYABLE_CLIENT, PLAIN_CLIENT, OTHER
    }

    /** The error Schemas and the traits the oracle expects of each (Smithy {@code @retryable}). */
    enum ErrorSchema {
        PLAIN(ModeledTestError.PLAIN, false, false),
        RETRYABLE(ModeledTestError.RETRYABLE, true, false),
        THROTTLING(ModeledTestError.THROTTLING, true, true);

        final Schema schema;
        final boolean retryable;
        final boolean throttling;

        ErrorSchema(Schema schema, boolean retryable, boolean throttling) {
            this.schema = schema;
            this.retryable = retryable;
            this.throttling = throttling;
        }
    }

    /** One link of a failure's cause chain; the chain is listed outermost first. */
    record Node(Kind kind, int status, boolean retryableFlag, boolean clockSkewFlag, ErrorSchema schema,
                HttpFailureCategory category, SdkTimeoutException.Scope scope) {
        static Node simple(Kind kind) {
            return new Node(kind, 0, false, false, null, null, null);
        }

        static Node service(int status) {
            return new Node(Kind.SERVICE, status, false, false, null, null, null);
        }

        static Node flagged(int status, boolean retryable, boolean clockSkew) {
            return new Node(Kind.FLAGGED_SERVICE, status, retryable, clockSkew, null, null, null);
        }

        static Node modeled(ErrorSchema schema, int status, boolean retryable, boolean clockSkew) {
            return new Node(Kind.MODELED, status, retryable, clockSkew, schema, null, null);
        }

        static Node transport(HttpFailureCategory category) {
            return new Node(Kind.TRANSPORT, 0, false, false, null, category, null);
        }

        static Node timeout(SdkTimeoutException.Scope scope) {
            return new Node(Kind.TIMEOUT, 0, false, false, null, null, scope);
        }

        boolean isService() {
            return kind == Kind.SERVICE || hasServiceFlags();
        }

        boolean hasServiceFlags() {
            return kind == Kind.FLAGGED_SERVICE || kind == Kind.MODELED;
        }

        Throwable create(Throwable cause) {
            return switch (kind) {
                case WRAP_RUNTIME -> new RuntimeException("wrapper", cause);
                case WRAP_COMPLETION -> new CompletionException("wrapper", cause);
                case WRAP_SDK_CLIENT, PLAIN_CLIENT -> SdkClientException.create("client", cause);
                case SERVICE -> SdkServiceException.builder().message("service").statusCode(status).cause(cause)
                                                   .build();
                case FLAGGED_SERVICE -> new FlaggedServiceError(status, retryableFlag, clockSkewFlag, cause);
                case MODELED -> new ModeledServiceError(schema.schema, status, retryableFlag, clockSkewFlag, cause);
                case TRANSPORT -> HttpTransportException.builder().category(category)
                                                        .phase(HttpPhase.RESPONSE_CONSUMPTION)
                                                        .message("transport").cause(cause).build();
                case TIMEOUT -> SdkTimeoutException.builder().scope(scope).message("timeout").cause(cause).build();
                case IO -> new IOException("io", cause);
                case UNCHECKED_IO -> new UncheckedIOException("unchecked io", new IOException("io", cause));
                case CANCELLATION -> withCause(new CancellationException("cancelled"), cause);
                case INTERRUPTED -> withCause(new InterruptedException("interrupted"), cause);
                case RETRYABLE_CLIENT -> new RetryableClientError(cause);
                case OTHER -> new IllegalStateException("other", cause);
            };
        }

        private static Throwable withCause(Throwable t, Throwable cause) {
            return cause == null ? t : t.initCause(cause);
        }

        @Override
        public String toString() {
            return switch (kind) {
                case SERVICE -> "SERVICE(" + status + ")";
                case FLAGGED_SERVICE -> "FLAGGED_SERVICE(" + status + ", retryable=" + retryableFlag + ", clockSkew="
                                        + clockSkewFlag + ")";
                case MODELED -> "MODELED(" + schema + ", " + status + ", retryable=" + retryableFlag + ", clockSkew="
                                + clockSkewFlag + ")";
                case TRANSPORT -> "TRANSPORT(" + category + ")";
                case TIMEOUT -> "TIMEOUT(" + scope + ")";
                default -> kind.name();
            };
        }
    }

    /** Exponential full-jitter backoff with a constant jitter factor {@code jitter / 1024}. */
    record Backoff(long baseMillis, long maxMillis, int jitter) {
        BackoffStrategy strategy() {
            double factor = jitter / (double) JITTER_SCALE;
            return BackoffStrategy.exponentialDelay(Duration.ofMillis(baseMillis), Duration.ofMillis(maxMillis),
                                                    () -> factor);
        }

        /** {@code floor(min(max, base * 2^(n - 1)) * jitter / 1024)} in nanoseconds. */
        Duration expected(int failedAttempt) {
            long ceiling = Math.min(TimeUnit.MILLISECONDS.toNanos(maxMillis),
                                    TimeUnit.MILLISECONDS.toNanos(baseMillis) * (1L << (failedAttempt - 1)));
            return Duration.ofNanos(ceiling * jitter / JITTER_SCALE);
        }
    }

    /** Response metadata and request state of the failed attempt. */
    record Metadata(int responseStatus, boolean viaResponse, String headerName, String hint, boolean decoyRetryAfter,
                    String errorCode, ModeledRetryTraits traitsOverride, boolean replayable) {
    }

    record Scenario(List<Node> chain, Metadata metadata, Backoff transientBackoff, Backoff throttlingBackoff,
                    int failedAttempt) {
        int responseStatus() {
            return metadata.responseStatus();
        }

        String hint() {
            return metadata.hint();
        }

        String errorCode() {
            return metadata.errorCode();
        }

        ModeledRetryTraits traitsOverride() {
            return metadata.traitsOverride();
        }

        boolean replayable() {
            return metadata.replayable();
        }

        Throwable failure() {
            Throwable t = null;
            for (int i = chain.size() - 1; i >= 0; i--) {
                t = chain.get(i).create(t);
            }
            return t;
        }

        AttemptFailure attemptFailure() {
            AttemptFailure.Builder builder = AttemptFailure.builder().failure(failure())
                                                           .requestReplayable(metadata.replayable());
            if (metadata.viaResponse() && metadata.responseStatus() != 0) {
                SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(metadata.responseStatus());
                if (metadata.decoyRetryAfter()) {
                    // An unsupported hint header: must not affect the delay.
                    response.addHeader("Retry-After", "30000");
                }
                if (metadata.hint() != null) {
                    response.addHeader(metadata.headerName(), metadata.hint());
                }
                builder.response(response.build());
            } else {
                if (metadata.responseStatus() != 0) {
                    builder.statusCode(metadata.responseStatus());
                }
                builder.retryAfterHint(metadata.hint());
            }
            if (metadata.errorCode() != null) {
                builder.errorCode(metadata.errorCode());
            }
            if (metadata.traitsOverride() != null) {
                builder.modeledTraits(metadata.traitsOverride());
            }
            return builder.build();
        }
    }

    record HintCase(Duration backoff, String rawHint, Duration extension) {
    }

    // ---- Generators ----

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Combinators.combine(chains(), metadata(), backoffs(), backoffs(), Arbitraries.integers().between(1, 4))
                          .as(Scenario::new);
    }

    @Provide
    Arbitrary<HintCase> hintCases() {
        Arbitrary<Long> backoffMillis = Arbitraries.longs().between(0, 30_000);
        Arbitrary<Long> subMillisNanos = Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(0L)),
            Tuple.of(1, Arbitraries.longs().between(1, 999_999)));
        Arbitrary<Duration> extension = Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(ORACLE_HINT_EXTENSION)),
            Tuple.of(1, Arbitraries.longs().between(0, 10_000).map(Duration::ofMillis)));
        return Combinators.combine(backoffMillis, subMillisNanos, extension)
                          .flatAs((ms, nanos, ext) -> relativeHints(ms, ext.toMillis())
                              .map(h -> new HintCase(Duration.ofMillis(ms).plusNanos(nanos), h, ext)));
    }

    /** Hints relative to a backoff of {@code b} ms and extension {@code e} ms: every band and both boundaries. */
    private static Arbitrary<String> relativeHints(long b, long e) {
        Arbitrary<Long> millis = Arbitraries.frequencyOf(
            Tuple.of(2, Arbitraries.longs().between(0, b)),
            Tuple.of(1, Arbitraries.just(b)),
            Tuple.of(3, Arbitraries.longs().between(b, b + e)),
            Tuple.of(1, Arbitraries.just(b + e)),
            Tuple.of(1, Arbitraries.just(b + e + 1)),
            Tuple.of(2, Arbitraries.longs().between(b + e + 1, 999_999_999_999_999_999L)));
        return Arbitraries.frequencyOf(
            Tuple.of(6, millis.map(String::valueOf)),
            Tuple.of(1, millis.map(BoundedRetryHintsAndClassificationPropertyTest::leadingZeros)),
            Tuple.of(3, invalidHints()))
                          .injectNull(0.08);
    }

    private static Arbitrary<List<Node>> chains() {
        Arbitrary<List<Node>> wrappers = Arbitraries.of(Kind.WRAP_RUNTIME, Kind.WRAP_COMPLETION, Kind.WRAP_SDK_CLIENT)
                                                    .map(Node::simple).list().ofMaxSize(3);
        return Combinators.combine(wrappers, primaryNodes(), innerNodes().injectNull(0.6))
                          .as((outer, primary, inner) -> {
                              List<Node> chain = new ArrayList<>(outer);
                              chain.add(primary);
                              // At most one transport failure per chain, so the transport row is unambiguous.
                              if (inner != null && !(inner.kind() == Kind.TRANSPORT
                                                     && primary.kind() == Kind.TRANSPORT)) {
                                  chain.add(inner);
                              }
                              return chain;
                          });
    }

    private static Arbitrary<Node> primaryNodes() {
        return Arbitraries.frequencyOf(
            Tuple.of(3, statuses().map(Node::service)),
            Tuple.of(2, Combinators.combine(statuses(), Arbitraries.of(true, false), Arbitraries.of(true, false))
                                   .as(Node::flagged)),
            Tuple.of(4, Combinators.combine(Arbitraries.of(ErrorSchema.class), statuses(),
                                            Arbitraries.frequency(Tuple.of(4, false), Tuple.of(1, true)),
                                            Arbitraries.frequency(Tuple.of(4, false), Tuple.of(1, true)))
                                   .as(Node::modeled)),
            Tuple.of(3, Arbitraries.of(HttpFailureCategory.class).map(Node::transport)),
            Tuple.of(2, Arbitraries.of(SdkTimeoutException.Scope.class).map(Node::timeout)),
            Tuple.of(4, Arbitraries.of(Kind.IO, Kind.UNCHECKED_IO, Kind.CANCELLATION, Kind.INTERRUPTED,
                                       Kind.RETRYABLE_CLIENT, Kind.PLAIN_CLIENT, Kind.OTHER).map(Node::simple)));
    }

    private static Arbitrary<Node> innerNodes() {
        return Arbitraries.frequencyOf(
            Tuple.of(2, Arbitraries.of(HttpFailureCategory.class).map(Node::transport)),
            Tuple.of(1, Arbitraries.of(SdkTimeoutException.Scope.class).map(Node::timeout)),
            Tuple.of(3, Arbitraries.of(Kind.IO, Kind.CANCELLATION, Kind.INTERRUPTED, Kind.RETRYABLE_CLIENT,
                                       Kind.OTHER).map(Node::simple)));
    }

    private static Arbitrary<Metadata> metadata() {
        Arbitrary<String> headerNames = Arbitraries.of("x-amz-retry-after", "X-Amz-Retry-After", "X-AMZ-RETRY-AFTER");
        Arbitrary<ModeledRetryTraits> traits = Arbitraries.of(ModeledRetryTraits.NONE,
                                                              new ModeledRetryTraits(true, false),
                                                              new ModeledRetryTraits(true, true))
                                                          .injectNull(0.7);
        return Combinators.combine(statuses(), Arbitraries.of(true, false), headerNames, hints(),
                                   Arbitraries.frequency(Tuple.of(4, false), Tuple.of(1, true)), errorCodes(),
                                   traits, Arbitraries.frequency(Tuple.of(6, true), Tuple.of(1, false)))
                          .as(Metadata::new);
    }

    private static Arbitrary<Backoff> backoffs() {
        Arbitrary<Long> base = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(0L)),
            Tuple.of(6, Arbitraries.longs().between(1, 2_000)));
        return Combinators.combine(base, Arbitraries.longs().between(0, 20_000),
                                   Arbitraries.integers().between(0, JITTER_SCALE - 1))
                          .as(Backoff::new);
    }

    private static Arbitrary<Integer> statuses() {
        return Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(0)),
            Tuple.of(6, Arbitraries.of(400, 403, 404, 408, 413, 429, 500, 501, 502, 503, 504, 509)),
            Tuple.of(1, Arbitraries.integers().between(100, 999)));
    }

    private static Arbitrary<String> errorCodes() {
        List<String> known = new ArrayList<>(ORACLE_THROTTLING_CODES);
        known.addAll(ORACLE_TRANSIENT_CODES);
        Arbitrary<String> variants = Arbitraries.of(known).flatMap(c -> Arbitraries.of(
            c.toLowerCase(Locale.ROOT), c.toUpperCase(Locale.ROOT), " " + c, c + " ", c + "X"));
        Arbitrary<String> unknown = Arbitraries.oneOf(
            Arbitraries.of("", "AccessDenied", "ValidationException", "InternalFailure", "ServiceUnavailable",
                           "ResourceNotFoundException"),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20));
        return Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.of(ORACLE_THROTTLING_CODES.toArray(new String[0]))),
            Tuple.of(3, Arbitraries.of(ORACLE_TRANSIENT_CODES.toArray(new String[0]))),
            Tuple.of(2, variants),
            Tuple.of(2, unknown))
                          .injectNull(0.4);
    }

    /** Valid in-range, leading-zero and very large hints, invalid hints, and (null) absent hints. */
    private static Arbitrary<String> hints() {
        Arbitrary<Long> inRange = Arbitraries.longs().between(0, 40_000);
        return Arbitraries.frequencyOf(
            Tuple.of(5, inRange.map(String::valueOf)),
            Tuple.of(1, inRange.map(BoundedRetryHintsAndClassificationPropertyTest::leadingZeros)),
            Tuple.of(1, Arbitraries.longs().between(40_001, 999_999_999_999_999_999L).map(String::valueOf)),
            Tuple.of(4, invalidHints()))
                          .injectNull(0.15);
    }

    private static Arbitrary<String> invalidHints() {
        Arbitrary<Long> millis = Arbitraries.longs().between(0, 40_000);
        return Arbitraries.oneOf(
            Arbitraries.of("", " ", "+100", "-100", "-0", "1.5", "1,000", "100ms", "5s", "1e3", "0x1F",
                           "\uFF11\uFF12\uFF13", "\u0661\u0662\u0663", "12 34", "abc", "NaN", "Infinity", "\t100",
                           "1_000", "0000000000000000001500"),
            // More than 18 digits, possibly with leading zeros.
            Arbitraries.strings().numeric().ofMinLength(19).ofMaxLength(25),
            // A valid value decorated with a sign, whitespace, fraction or unit.
            Combinators.combine(millis, Arbitraries.of("+%d", "-%d", " %d", "%d ", "\t%d", "%d.0", "%d.5", "%dms",
                                                       "%ds", "%d%%"))
                       .as((v, format) -> String.format(Locale.ROOT, format, v)));
    }

    /** {@code millis} left-padded with zeros to 18 digits (still valid). */
    private static String leadingZeros(long millis) {
        return String.format(Locale.ROOT, "%018d", millis);
    }

    // ---- Failure types ----

    /** A service exception whose {@code isRetryableException()} / {@code isClockSkewException()} are set. */
    private static class FlaggedServiceError extends SdkServiceException {
        private static final long serialVersionUID = 1L;

        private final boolean retryable;
        private final boolean clockSkew;

        FlaggedServiceError(int status, boolean retryable, boolean clockSkew, Throwable cause) {
            super(SdkServiceException.builder().message("flagged").statusCode(status).cause(cause));
            this.retryable = retryable;
            this.clockSkew = clockSkew;
        }

        @Override
        public boolean isRetryableException() {
            return retryable;
        }

        @Override
        public boolean isClockSkewException() {
            return clockSkew;
        }
    }

    /** A generated-style modeled error over an error Schema, whose retry traits are derived from the Schema. */
    private static final class ModeledServiceError extends FlaggedServiceError implements SerializableStruct {
        private static final long serialVersionUID = 1L;

        private final transient Schema schema;

        ModeledServiceError(Schema schema, int status, boolean retryable, boolean clockSkew, Throwable cause) {
            super(status, retryable, clockSkew, cause);
            this.schema = schema;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    /** A client failure that reports {@link SdkClientException#retryable()} as {@code true}. */
    private static final class RetryableClientError extends SdkClientException {
        private static final long serialVersionUID = 1L;

        RetryableClientError(Throwable cause) {
            super(SdkClientException.builder().message("retryable client").cause(cause));
        }

        @Override
        public boolean retryable() {
            return true;
        }
    }
}
