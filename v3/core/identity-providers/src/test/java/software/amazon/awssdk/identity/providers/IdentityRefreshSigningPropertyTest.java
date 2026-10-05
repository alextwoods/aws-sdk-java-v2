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

package software.amazon.awssdk.identity.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityRefreshException;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;

/**
 * Property 50: Identity refresh changes subsequent signing identity.
 *
 * <p>For any rejected/expired refreshable identity followed by successful refresh, V3 requests refresh before an allowed
 * later attempt/call and signs it with the refreshed identity; if refresh fails, no later transmission occurs and the
 * refresh failure remains the cause.
 *
 * <p>Each try generates a script of loader outcomes (temporary session credentials expiring at a chosen offset from the
 * load, or a load failure) and a trace of steps: clock advances to exactly the current identity's prefetch time, to just
 * before or exactly at its stale time, or by a fixed amount; calls of up to {@value #MAX_ATTEMPTS} attempts whose service
 * responses are success, an identity rejection ({@code ExpiredToken}) or a throttle; overlapping callers that resolve
 * before any load runs; and runs of the pending background refresh. The identity comes from a real
 * {@link CachingIdentityProvider} (the base of the STS, SSO, container and instance-metadata providers, with their STS
 * stale/prefetch times) over a manual executor, so every load runs at a point the trace chooses and the run is
 * deterministic.
 *
 * <p>The pipeline is not built yet, so the test models its documented per-attempt contract with the real auth-api types:
 * resolve the identity through {@link SelectedAuthScheme#resolveIdentity()}, sign with a recording signer that wraps the
 * real SigV4 signer, transmit to a counting transport, and on a rejection report the identity with
 * {@link SelectedAuthScheme#invalidate} and retry. The oracle is an independent refresh-state machine (value, stale and
 * prefetch times, invalidated flag, pending load). After every attempt the identity the signer received, the
 * {@code Authorization} credential scope, the session-token header and the signature (recomputed from the oracle's
 * expected credentials) must match the oracle; a rejected identity must never be signed with again and must be
 * refreshed before the next attempt; a failed required refresh must fail the attempt with an
 * {@link IdentityRefreshException} whose cause is the exact loader failure, with zero transmissions; and loader
 * invocations, transmissions and pending background loads must equal the oracle's after every step.
 *
 * <p><b>Validates: Requirements 13.16, 13.17, 13.19</b>
 */
class IdentityRefreshSigningPropertyTest {
    private static final int MAX_ATTEMPTS = 3;
    private static final Instant START = Instant.parse("2025-01-01T00:00:00Z");
    private static final String PROVIDER_NAME = "ScriptedStsProvider";
    private static final String ACCESS_KEY_PREFIX = "AKIDLOAD";
    private static final SdkHttpRequest UNSIGNED = SdkHttpRequest.builder()
                                                                 .method("GET")
                                                                 .host("svc.us-east-1.amazonaws.com")
                                                                 .encodedPath("/resource")
                                                                 .build();
    /** Credential lifetimes from the load: never expires, already stale, prefetch at once, and ordinary STS lifetimes. */
    private static final List<Optional<Duration>> LIFETIMES =
        List.of(Optional.empty(), Optional.of(Duration.ofSeconds(30)), Optional.of(Duration.ofMinutes(3)),
                Optional.of(Duration.ofMinutes(10)), Optional.of(Duration.ofHours(1)));

    @Property(tries = 1_000)
    void refreshedIdentitySignsEverySubsequentAttempt(@ForAll("traces") Trace trace) {
        Run run = new Run(trace.loads());
        for (Step step : trace.steps()) {
            run.apply(step);
            run.assertStateMatchesOracle(step);
        }
        Statistics.label("signed after rejection refresh").collect(run.signedAfterRejection);
        Statistics.label("signed after expiry refresh").collect(run.signedAfterExpiry);
        Statistics.label("failed required refresh blocked").collect(run.blockedByRefreshFailure);
    }

    /** A fixed identity's contract does not permit refresh: rejecting it changes nothing. */
    @Example
    void fixedIdentityIsNotRefreshable() {
        AwsCredentialsIdentity fixed = AwsCredentialsIdentity.create("AKIDFIXED", "fixed-secret");
        SelectedAuthScheme<AwsCredentialsIdentity> selected =
            new SelectedAuthScheme<>(AwsV4AuthScheme.create(), option(Clock.fixed(START, ZoneOffset.UTC)),
                                     StaticCredentialsProvider.create(fixed));
        assertFalse(selected.invalidate(fixed));
        assertSame(fixed, selected.resolveIdentity().join());
    }

    @Provide
    Arbitrary<Trace> traces() {
        Arbitrary<LoadOutcome> loaded = Arbitraries.of(LIFETIMES).map(Loaded::new);
        Arbitrary<LoadOutcome> fails = Arbitraries.just(new Fails());
        Arbitrary<LoadOutcome> load = Arbitraries.frequencyOf(Tuple.of(3, loaded), Tuple.of(1, fails));

        Arbitrary<Response> response = Arbitraries.frequency(Tuple.of(3, Response.OK),
                                                             Tuple.of(2, Response.IDENTITY_REJECTED),
                                                             Tuple.of(1, Response.THROTTLED));
        Arbitrary<Step> call = response.list().ofSize(MAX_ATTEMPTS).map(Call::new);
        Arbitrary<Step> advance = Combinators.combine(Arbitraries.of(Target.class),
                                                      Arbitraries.of(1L, 30L, 90L, 240L, 600L, 3_600L))
                                             .as(Advance::new);
        Arbitrary<Step> overlapping = Arbitraries.integers().between(2, 4).map(Overlapping::new);
        Arbitrary<Step> background = Arbitraries.just(new RunBackground());
        Arbitrary<Step> step = Arbitraries.frequencyOf(Tuple.of(4, call), Tuple.of(4, advance),
                                                       Tuple.of(1, overlapping), Tuple.of(1, background));

        return Combinators.combine(load.list().ofMinSize(1).ofMaxSize(6), step.list().ofMinSize(1).ofMaxSize(25))
                          .as(Trace::new);
    }

    private static AuthSchemeOption option(Clock clock) {
        return AuthSchemeOption.builder()
                               .schemeId(AwsV4AuthScheme.SCHEME_ID)
                               .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
                               .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "svc")
                               .putSignerProperty(HttpSigner.SIGNING_CLOCK, clock)
                               .build();
    }

    private static String accessKeyId(int load) {
        return ACCESS_KEY_PREFIX + load;
    }

    private static String sessionToken(int load) {
        return "token-" + load;
    }

    /** The credentials the {@code load}th loader invocation returns. */
    private static AwsSessionCredentialsIdentity credentials(int load, Instant expiration) {
        return AwsSessionCredentialsIdentity.builder()
                                            .accessKeyId(accessKeyId(load))
                                            .secretAccessKey("secret-" + load)
                                            .sessionToken(sessionToken(load))
                                            .expirationTime(expiration)
                                            .providerName(PROVIDER_NAME)
                                            .build();
    }

    /** One generated try. */
    record Trace(List<LoadOutcome> loads, List<Step> steps) {
    }

    /** What one loader invocation does; invocations take the script's outcomes in order, cycling. */
    sealed interface LoadOutcome permits Loaded, Fails {
    }

    /** The loader returns new session credentials expiring {@code lifetime} after the load (never, if empty). */
    record Loaded(Optional<Duration> lifetime) implements LoadOutcome {
    }

    /** The loader throws. */
    record Fails() implements LoadOutcome {
    }

    sealed interface Step permits Advance, Call, Overlapping, RunBackground {
    }

    /** Advances the clock to a point relative to the cached identity's refresh times, or by {@code seconds}. */
    record Advance(Target target, long seconds) implements Step {
    }

    enum Target {
        TO_PREFETCH, JUST_BEFORE_STALE, TO_STALE, BY_SECONDS
    }

    /** One call; attempt {@code i} receives {@code responses[i]}; the call ends at the first success. */
    record Call(List<Response> responses) implements Step {
    }

    /** {@code callers} calls resolve before any load runs, then each makes one successful attempt. */
    record Overlapping(int callers) implements Step {
    }

    /** The pending background (prefetch) load, if any, runs. */
    record RunBackground() implements Step {
    }

    enum Response {
        OK,
        /** The service rejected the identity, for example {@code ExpiredToken} or {@code InvalidClientTokenId}. */
        IDENTITY_REJECTED,
        /** A retryable error unrelated to the identity. */
        THROTTLED
    }

    /** Why the oracle expects a resolution to return the cached identity or to wait for a load. */
    enum Reason {
        FRESH, PREFETCH, NO_VALUE, STALE, INVALIDATED;

        boolean usable() {
            return this == FRESH || this == PREFETCH;
        }
    }

    /** The oracle's expected result of a resolution: an identity (by load number) or a failure of a load. */
    record Expected(Integer identity, Integer failedLoad) {
        static Expected identity(int load) {
            return new Expected(load, null);
        }

        static Expected failure(int load) {
            return new Expected(null, load);
        }
    }

    /** The system under test for one try, with its oracle. */
    private static final class Run {
        private final MutableClock clock = new MutableClock(START);
        private final ManualExecutor executor = new ManualExecutor();
        private final RecordingSigner signer = new RecordingSigner();
        private final CountingTransport transport = new CountingTransport();
        private final ScriptedProvider provider;
        private final SelectedAuthScheme<AwsCredentialsIdentity> selected;
        private final Oracle oracle;
        private final Set<String> rejected = new HashSet<>();
        private boolean signedAfterRejection;
        private boolean signedAfterExpiry;
        private boolean blockedByRefreshFailure;

        Run(List<LoadOutcome> loads) {
            this.provider = new ScriptedProvider(loads, clock, executor);
            this.oracle = new Oracle(loads);
            this.selected = new SelectedAuthScheme<>(new RecordingScheme(signer), option(clock), provider);
        }

        void apply(Step step) {
            switch (step) {
                case Advance advance -> clock.set(oracle.advance(advance));
                case Call call -> call(call.responses());
                case Overlapping overlapping -> overlapping(overlapping.callers());
                case RunBackground background -> {
                    executor.runAll();
                    oracle.runLoad();
                }
            }
        }

        void assertStateMatchesOracle(Step step) {
            assertEquals(oracle.loads, provider.invocations, "loader invocations after " + step);
            assertEquals(oracle.transmissions, transport.transmitted.size(), "transmissions after " + step);
            assertEquals(transport.transmitted.size(), signer.inputs.size(), "every signed request is transmitted");
            assertEquals(oracle.loadPending ? 1 : 0, executor.pending(), "pending loads after " + step);
        }

        private void call(List<Response> responses) {
            boolean previousRejected = false;
            for (Response response : responses) {
                Reason reason = oracle.classify();
                CompletableFuture<? extends AwsCredentialsIdentity> future = resolve();
                if (previousRejected) {
                    assertFalse(future.isDone(), "a rejected identity must be refreshed before the next attempt");
                }
                Expected expected = awaitIfPending(future);
                AwsCredentialsIdentity identity = checkResolution(future, expected);
                if (identity == null) {
                    // A failed required refresh ends the call: nothing is signed or transmitted.
                    blockedByRefreshFailure = true;
                    return;
                }
                signedAfterRejection |= previousRejected;
                signedAfterExpiry |= reason == Reason.STALE;
                signAndTransmit(identity);
                if (response == Response.OK) {
                    return;
                }
                previousRejected = response == Response.IDENTITY_REJECTED;
                if (previousRejected) {
                    assertTrue(selected.invalidate(identity), "the provider's contract permits refresh");
                    oracle.invalidate(expected.identity());
                    rejected.add(identity.accessKeyId());
                }
            }
        }

        private void overlapping(int callers) {
            List<CompletableFuture<? extends AwsCredentialsIdentity>> futures = new ArrayList<>();
            List<Integer> immediate = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                Reason reason = oracle.classify();
                futures.add(resolve());
                immediate.add(reason.usable() ? oracle.cachedLoad() : null);
            }
            int loadsBefore = provider.invocations;
            Expected waiters = null;
            if (immediate.contains(null)) {
                executor.runAll();
                oracle.runLoad();
                waiters = oracle.waiterOutcome();
            }
            assertTrue(provider.invocations - loadsBefore <= 1, "overlapping callers share one load");
            for (int i = 0; i < callers; i++) {
                Expected expected = immediate.get(i) == null ? waiters : Expected.identity(immediate.get(i));
                AwsCredentialsIdentity identity = checkResolution(futures.get(i), expected);
                if (identity != null) {
                    signAndTransmit(identity);
                }
            }
        }

        /** Starts one resolution in both the provider and the oracle; asserts it waits exactly when required. */
        private CompletableFuture<? extends AwsCredentialsIdentity> resolve() {
            CompletableFuture<? extends AwsCredentialsIdentity> future = selected.resolveIdentity();
            Reason reason = oracle.resolve();
            Statistics.label("resolution").collect(reason);
            assertEquals(!reason.usable(), !future.isDone(), "whether resolution waits for a load (" + reason + ")");
            return future;
        }

        /** Runs the load an unfinished resolution waits for, and returns the oracle's expected result. */
        private Expected awaitIfPending(CompletableFuture<? extends AwsCredentialsIdentity> future) {
            if (future.isDone()) {
                return Expected.identity(oracle.cachedLoad());
            }
            executor.runAll();
            oracle.runLoad();
            return oracle.waiterOutcome();
        }

        /** The resolved identity, or null after asserting the resolution failed with the expected load failure. */
        private AwsCredentialsIdentity checkResolution(CompletableFuture<? extends AwsCredentialsIdentity> future,
                                                       Expected expected) {
            assertTrue(future.isDone(), "the load ran");
            if (expected.identity() != null) {
                AwsCredentialsIdentity identity = future.join();
                assertEquals(accessKeyId(expected.identity()), identity.accessKeyId(), "resolved identity");
                oracle.transmissions++;
                return identity;
            }
            CompletionException failure = assertThrows(CompletionException.class, future::join);
            IdentityRefreshException refresh = assertInstanceOf(IdentityRefreshException.class, failure.getCause());
            assertEquals(PROVIDER_NAME, refresh.providerName());
            assertSame(provider.failures.get(expected.failedLoad()), refresh.getCause(),
                       "the refresh failure is the loader's failure");
            return null;
        }

        private void signAndTransmit(AwsCredentialsIdentity identity) {
            assertFalse(rejected.contains(identity.accessKeyId()), "a rejected identity was signed with again");
            int load = Integer.parseInt(identity.accessKeyId().substring(ACCESS_KEY_PREFIX.length()));
            SignedRequest signed = selected.sign(UNSIGNED, identity);
            assertSame(identity, signer.inputs.getLast(), "the signer's input identity");

            SdkHttpRequest wire = signed.request();
            String authorization = wire.headers().firstValue("Authorization").orElseThrow();
            assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 Credential=" + accessKeyId(load) + "/"),
                       authorization);
            assertEquals(sessionToken(load), wire.headers().firstValue("X-Amz-Security-Token").orElseThrow());
            SignRequest<AwsCredentialsIdentity> independent =
                SignRequest.<AwsCredentialsIdentity>builder(credentials(load, null))
                           .request(UNSIGNED)
                           .putProperties(selected.option().signerProperties())
                           .build();
            assertEquals(AwsV4HttpSigner.create().sign(independent).signature(), signed.signature(),
                         "the signature uses the expected identity's secret");
            transport.transmit(wire);
        }
    }

    /**
     * The refresh-state machine: the cached identity (by load number) with its stale and prefetch times, whether it was
     * invalidated, and whether a load is pending. Expected times follow the STS convention the caching providers use:
     * stale one minute and prefetched five minutes before expiration; a value that arrives already stale is usable for
     * one second.
     */
    private static final class Oracle {
        private final List<LoadOutcome> script;
        private Instant now = START;
        private Integer cached;
        private Instant stale;
        private Instant prefetch;
        private boolean invalidated;
        private boolean loadPending;
        private int loads;
        private int transmissions;

        Oracle(List<LoadOutcome> script) {
            this.script = script;
        }

        Instant advance(Advance advance) {
            Instant goal = switch (advance.target()) {
                case TO_PREFETCH -> cached == null ? null : prefetch;
                case JUST_BEFORE_STALE -> cached == null || stale == null ? null : stale.minusMillis(1);
                case TO_STALE -> cached == null ? null : stale;
                case BY_SECONDS -> now.plusSeconds(advance.seconds());
            };
            if (goal != null && goal.isAfter(now)) {
                now = goal;
            }
            return now;
        }

        Reason classify() {
            if (cached == null) {
                return Reason.NO_VALUE;
            }
            if (invalidated) {
                return Reason.INVALIDATED;
            }
            if (stale != null && !now.isBefore(stale)) {
                return Reason.STALE;
            }
            if (prefetch != null && !now.isBefore(prefetch)) {
                return Reason.PREFETCH;
            }
            return Reason.FRESH;
        }

        /** One resolution: a prefetch or required load starts unless one is pending. */
        Reason resolve() {
            Reason reason = classify();
            if (reason != Reason.FRESH) {
                loadPending = true;
            }
            return reason;
        }

        int cachedLoad() {
            return cached;
        }

        /** Runs the pending load, if any, at the current time. */
        void runLoad() {
            if (!loadPending) {
                return;
            }
            loadPending = false;
            int load = ++loads;
            if (script.get((load - 1) % script.size()) instanceof Loaded loaded) {
                Instant expiration = loaded.lifetime().map(now::plus).orElse(null);
                cached = load;
                invalidated = false;
                stale = expiration == null ? null : expiration.minus(Duration.ofMinutes(1));
                prefetch = expiration == null ? null : expiration.minus(Duration.ofMinutes(5));
                if (stale != null && !now.isBefore(stale)) {
                    stale = now.plusSeconds(1);
                }
            }
        }

        /** What a resolution that waited for the load just run gets: a usable identity or that load's failure. */
        Expected waiterOutcome() {
            return classify().usable() ? Expected.identity(cached) : Expected.failure(loads);
        }

        void invalidate(int load) {
            if (cached != null && cached == load) {
                invalidated = true;
            }
        }
    }

    /** A caching provider whose loader follows the script; every load runs on the manual executor. */
    private static final class ScriptedProvider extends CachingIdentityProvider<AwsCredentialsIdentity> {
        private final List<LoadOutcome> script;
        private final Map<Integer, RuntimeException> failures = new HashMap<>();
        private int invocations;

        ScriptedProvider(List<LoadOutcome> script, Clock clock, Executor executor) {
            super(AwsCredentialsIdentity.class, PROVIDER_NAME, clock, executor, IdentityCache.StaleValueBehavior.STRICT);
            this.script = script;
        }

        @Override
        protected RefreshResult<AwsCredentialsIdentity> load() {
            int load = ++invocations;
            if (script.get((load - 1) % script.size()) instanceof Loaded loaded) {
                Instant now = clock.instant();
                Instant expiration = loaded.lifetime().map(now::plus).orElse(null);
                return stsStyle(credentials(load, expiration), expiration);
            }
            RuntimeException failure = new IllegalStateException("Scripted load " + load + " failed");
            failures.put(load, failure);
            throw failure;
        }
    }

    /** SigV4 with a signer that records every identity it signs with. */
    private record RecordingScheme(RecordingSigner signer) implements AuthScheme<AwsCredentialsIdentity> {
        @Override
        public String schemeId() {
            return AwsV4AuthScheme.SCHEME_ID;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }
    }

    private static final class RecordingSigner implements HttpSigner<AwsCredentialsIdentity> {
        private final List<AwsCredentialsIdentity> inputs = new ArrayList<>();

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            inputs.add(request.identity());
            return AwsV4HttpSigner.create().sign(request);
        }
    }

    private static final class CountingTransport {
        private final List<SdkHttpRequest> transmitted = new ArrayList<>();

        void transmit(SdkHttpRequest request) {
            transmitted.add(request);
        }
    }

    /** Runs submitted loads only when the test says so. */
    private static final class ManualExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            Runnable task;
            while ((task = tasks.poll()) != null) {
                task.run();
            }
        }

        int pending() {
            return tasks.size();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
