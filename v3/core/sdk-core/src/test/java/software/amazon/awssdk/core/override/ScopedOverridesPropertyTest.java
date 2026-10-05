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

package software.amazon.awssdk.core.override;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.core.override.CallOptionsTest.INTERCEPTORS;
import static software.amazon.awssdk.core.override.CallOptionsTest.INTERCEPTORS_CALL;
import static software.amazon.awssdk.core.override.CallOptionsTest.RETRY_CALL;
import static software.amazon.awssdk.core.override.CallOptionsTest.RETRY_STRATEGY;
import static software.amazon.awssdk.core.override.CallOptionsTest.TRACE;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.override.CallOptionsTest.Interceptor;
import software.amazon.awssdk.core.override.CallOptionsTest.Retry;

/**
 * Property 25: Overrides are isolated and scoped.
 *
 * <p>For any calls sharing one client and carrying arbitrary distinct request overrides, each call observes only its
 * own sparse deltas plus base configuration, and all non-overridden calls observe the unchanged base configuration.
 *
 * <p>Each try generates a base client configuration and 2–8 calls with distinct deltas. A call has a request-embedded
 * override, an explicit operation-overload override, both, or neither, and each may also be {@code null} or empty. The
 * deltas cover every call-option rule through stand-in keys (replacing {@link CallOptionsTest#RETRY_CALL}, appending
 * {@link CallOptionsTest#INTERCEPTORS_CALL}, merging {@link #LABELS_CALL}) plus {@link SdkCallOption#TIMEOUT_CONFIGURATION},
 * headers with case variants, raw query parameters and execution attributes. Two client-only options
 * ({@link SdkClientOption#CLIENT_ENDPOINT} and {@link #REGION}) have no call option and must always read the base value.
 *
 * <p>The oracle ({@link #expected}) computes each call's view from the generated specs with plain maps only: the explicit
 * override wins over the embedded one, which wins over the base; appending accumulates base, embedded, explicit; timeouts
 * and labels merge field by field. It never touches {@link CallOptions}.
 *
 * <p>All calls run against one {@link CallOptions#of} instance on {@code calls.size()} threads for {@link #ROUNDS}
 * rounds, rotating so every thread runs every call's delta. A barrier after each round's views are built proves that
 * every call holds its view at once before any view is checked, and every wait is bounded.
 */
class ScopedOverridesPropertyTest {
    private static final int ROUNDS = 3;
    private static final long TIMEOUT_SECONDS = 10;

    /** Stand-in for an option merged field by field; the call's entries win. */
    record Labels(Map<String, String> entries) {
        Labels {
            entries = Map.copyOf(entries);
        }

        static Labels merge(Labels client, Labels call) {
            Map<String, String> merged = new HashMap<>(client.entries());
            merged.putAll(call.entries());
            return new Labels(merged);
        }
    }

    static final ClientOption<Labels> LABELS = ClientOption.create("Labels", Labels.class);
    static final CallOption<Labels> LABELS_CALL = CallOption.merging(LABELS, Labels::merge);
    /** Client-only: no call option exists for it. */
    static final ClientOption<String> REGION = ClientOption.create("Region", String.class);
    static final ContextKey<Integer> COUNT = ContextKey.execution("Count", Integer.class);

    /** Every option the property reads; the last two are client-only. */
    private static final List<ClientOption<?>> OPTIONS = List.of(RETRY_STRATEGY, INTERCEPTORS, LABELS,
                                                                 SdkClientOption.TIMEOUT_CONFIGURATION,
                                                                 SdkClientOption.CLIENT_ENDPOINT, REGION);
    private static final List<ContextKey<?>> ATTRIBUTES = List.of(TRACE, COUNT);

    record Base(Retry retry, List<Interceptor> interceptors, Labels labels, boolean hasTimeouts, Duration apiCallTimeout,
                Duration apiCallAttemptTimeout, URI endpoint, String region) {
        SdkClientConfiguration build() {
            SdkClientConfiguration.Builder builder = SdkClientConfiguration.builder();
            if (retry != null) {
                builder.option(RETRY_STRATEGY, retry);
            }
            if (interceptors != null) {
                builder.option(INTERCEPTORS, List.copyOf(interceptors));
            }
            if (labels != null) {
                builder.option(LABELS, labels);
            }
            if (hasTimeouts) {
                builder.option(SdkClientOption.TIMEOUT_CONFIGURATION, timeouts(apiCallTimeout, apiCallAttemptTimeout));
            }
            if (endpoint != null) {
                builder.option(SdkClientOption.CLIENT_ENDPOINT, endpoint);
            }
            if (region != null) {
                builder.option(REGION, region);
            }
            return builder.build();
        }

        /** The base values, written out from the spec rather than read from a configuration. */
        Map<ClientOption<?>, Object> values() {
            Map<ClientOption<?>, Object> values = new HashMap<>();
            putIfPresent(values, RETRY_STRATEGY, retry);
            putIfPresent(values, INTERCEPTORS, interceptors == null ? null : List.copyOf(interceptors));
            putIfPresent(values, LABELS, labels);
            putIfPresent(values, SdkClientOption.TIMEOUT_CONFIGURATION,
                         hasTimeouts ? timeouts(apiCallTimeout, apiCallAttemptTimeout) : null);
            putIfPresent(values, SdkClientOption.CLIENT_ENDPOINT, endpoint);
            putIfPresent(values, REGION, region);
            return values;
        }
    }

    record Header(String name, List<String> values) {
    }

    record Query(String name, List<String> values) {
    }

    /** Call-option deltas of one override; {@code null} means unset. */
    record OptionDelta(Retry retry, List<Interceptor> interceptors, boolean interceptorsElementWise, Labels labels,
                       Duration apiCallTimeout, Duration apiCallAttemptTimeout) {
    }

    /** Header, query and execution-attribute deltas of one override, applied in order. */
    record ContextDelta(List<Header> headers, List<Query> query, String trace, Integer count) {
    }

    enum Kind { NULL, EMPTY, DELTA }

    /** One override: {@code null}, built with nothing set, or built from the deltas. */
    record Layer(Kind kind, OptionDelta options, ContextDelta context) {
        RequestOverrideConfiguration build() {
            if (kind == Kind.NULL) {
                return null;
            }
            SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
            if (kind == Kind.EMPTY) {
                return builder.build();
            }
            if (options.retry() != null) {
                builder.putCallOption(RETRY_CALL, options.retry());
            }
            if (options.interceptors() != null) {
                if (options.interceptorsElementWise() && !options.interceptors().isEmpty()) {
                    options.interceptors().forEach(i -> builder.addCallOptionElement(INTERCEPTORS_CALL, i));
                } else {
                    builder.putCallOption(INTERCEPTORS_CALL, options.interceptors());
                }
            }
            if (options.labels() != null) {
                builder.putCallOption(LABELS_CALL, options.labels());
            }
            if (options.apiCallTimeout() != null) {
                builder.apiCallTimeout(options.apiCallTimeout());
            }
            if (options.apiCallAttemptTimeout() != null) {
                builder.apiCallAttemptTimeout(options.apiCallAttemptTimeout());
            }
            context.headers().forEach(h -> builder.putHeader(h.name(), h.values()));
            context.query().forEach(q -> builder.putRawQueryParameter(q.name(), q.values()));
            builder.putExecutionAttribute(TRACE, context.trace());
            builder.putExecutionAttribute(COUNT, context.count());
            return builder.build();
        }
    }

    record Call(Layer embedded, Layer explicit) {
    }

    /** What a call must observe, computed without {@link CallOptions}. */
    record Expected(Map<ClientOption<?>, Object> values, Map<ClientOption<?>, Object> overridden,
                    Map<String, List<String>> headers, Map<String, List<String>> query,
                    Map<ContextKey<?>, Object> attributes) {
        boolean hasOverrides() {
            return !overridden.isEmpty() || !headers.isEmpty() || !query.isEmpty() || !attributes.isEmpty();
        }
    }

    // Feature: aws-sdk-java-v3, Property 25: Overrides are isolated and scoped
    @Property(tries = 1_000)
    @Label("Property 25: Overrides are isolated and scoped")
    void overridesAreIsolatedAndScoped(@ForAll("bases") Base baseSpec, @ForAll("calls") List<Call> calls)
        throws Exception {
        Statistics.label("calls").collect(calls.size());
        calls.forEach(c -> Statistics.label("layers").collect(c.embedded().kind() + "/" + c.explicit().kind()));

        SdkClientConfiguration base = baseSpec.build();
        CallOptions client = CallOptions.of(base);
        Expected plain = expected(baseSpec, null, null);
        List<Expected> expected = new ArrayList<>();
        for (Call call : calls) {
            expected.add(expected(baseSpec, call.embedded(), call.explicit()));
        }

        int simultaneous = calls.size();
        CyclicBarrier viewsBuilt = new CyclicBarrier(simultaneous);
        CyclicBarrier viewsChecked = new CyclicBarrier(simultaneous);
        AtomicInteger live = new AtomicInteger();
        AtomicInteger operations = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(simultaneous);
        List<Callable<Void>> workers = new ArrayList<>();
        for (int worker = 0; worker < simultaneous; worker++) {
            int workerId = worker;
            workers.add(() -> {
                try {
                    for (int round = 0; round < ROUNDS; round++) {
                        int index = (workerId + round) % simultaneous;
                        Call spec = calls.get(index);
                        RequestOverrideConfiguration embedded = spec.embedded().build();
                        RequestOverrideConfiguration explicit = spec.explicit().build();
                        CallOptions view = client.withOverrides(embedded, explicit);
                        // A call without overrides reuses the client's instance and reads plain base values.
                        CallOptions noOverride = client.withOverrides(null, null);
                        live.incrementAndGet();
                        viewsBuilt.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        // Every call of this round holds its view now: the calls overlap.
                        assertEquals(simultaneous, live.get());
                        assertView(expected.get(index), base, client, view, "call " + index);
                        assertSame(client, noOverride);
                        assertView(plain, base, client, noOverride, "no-override call");
                        // Merging did not change the overrides themselves.
                        assertEquals(spec.embedded().build(), embedded);
                        assertEquals(spec.explicit().build(), explicit);
                        viewsChecked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        live.decrementAndGet();
                        operations.incrementAndGet();
                    }
                    return null;
                } catch (Throwable t) {
                    // Interrupt the other workers so they stop at once instead of waiting out a barrier timeout.
                    executor.shutdownNow();
                    throw t;
                }
            });
        }
        runBounded(executor, workers);

        assertEquals(simultaneous * ROUNDS, operations.get());
        assertTrue(operations.get() > simultaneous);
        // The base is unchanged, and the shared client view still reads it plainly.
        assertEquals(baseSpec.build(), base);
        assertEquals(baseSpec.values(), snapshot(base));
        assertView(plain, base, client, client, "client after calls");
        assertView(plain, base, client, client.withOverrides(SdkRequestOverrideConfiguration.builder().build(), null),
                   "empty override after calls");
    }

    /** Runs {@code workers} on {@code executor} with bounded waits and rethrows the first worker's own failure. */
    private static void runBounded(ExecutorService executor, List<Callable<Void>> workers) throws Exception {
        try {
            List<Future<Void>> results = executor.invokeAll(workers, 3 * TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Throwable first = null;
            for (Future<Void> result : results) {
                try {
                    result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    // Prefer the failing worker's own error over the interrupts and broken barriers it caused.
                    if (first == null || first instanceof BrokenBarrierException
                        || first instanceof InterruptedException) {
                        first = e.getCause();
                    }
                } catch (CancellationException e) {
                    if (first == null) {
                        first = new AssertionError("worker did not finish within the bound", e);
                    }
                }
            }
            if (first instanceof Error error) {
                throw error;
            }
            if (first instanceof Exception exception) {
                throw exception;
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    /** Asserts {@code view} is exactly {@code expected}: values, the overridden key set, headers, query, attributes. */
    private static void assertView(Expected expected, SdkClientConfiguration base, CallOptions client, CallOptions view,
                                   String what) {
        for (ClientOption<?> option : OPTIONS) {
            assertEquals(Optional.ofNullable(expected.values().get(option)), view.get(option),
                         what + " " + option.name());
            assertEquals(expected.overridden().containsKey(option), view.isOverridden(option),
                         what + " overrides " + option.name());
        }
        assertEquals(expected.overridden(), view.overriddenOptions(), what + " overridden options");
        // Client-only options are never overridden per call.
        assertFalse(view.isOverridden(SdkClientOption.CLIENT_ENDPOINT), what);
        assertFalse(view.isOverridden(REGION), what);
        TimeoutConfiguration timeouts = (TimeoutConfiguration) expected.values()
                                                                       .get(SdkClientOption.TIMEOUT_CONFIGURATION);
        assertEquals(timeouts == null ? TimeoutConfiguration.none() : timeouts, view.timeouts(), what + " timeouts");

        assertEquals(expected.headers(), view.headers(), what + " headers");
        // Names keep the casing of the last layer that set them, and lookups ignore case.
        assertEquals(expected.headers().keySet(), new HashSet<>(view.headers().keySet()), what + " header names");
        expected.headers().forEach((name, values) -> {
            assertEquals(values, view.headers().get(name.toUpperCase(Locale.ROOT)), what + " header " + name);
            assertEquals(values, view.headers().get(name.toLowerCase(Locale.ROOT)), what + " header " + name);
        });
        assertEquals(expected.query(), view.rawQueryParameters(), what + " query");
        assertEquals(expected.attributes(), view.executionAttributes(), what + " attributes");
        for (ContextKey<?> key : ATTRIBUTES) {
            assertEquals(Optional.ofNullable(expected.attributes().get(key)), view.executionAttribute(key),
                         what + " " + key);
        }
        assertEquals(expected.hasOverrides(), view.hasOverrides(), what + " hasOverrides");
        assertSame(base, view.clientConfiguration(), what);
        if (expected.hasOverrides()) {
            assertNotSame(client, view, what);
        } else {
            assertSame(client, view, what);
        }
    }

    /**
     * The independent oracle: base values plus the embedded and then the explicit layer, with plain maps. Replacing
     * options take the last layer's value, appending options concatenate base, embedded, explicit, and merging options
     * (labels, timeouts) combine entry by entry with the later layer winning.
     */
    static Expected expected(Base base, Layer embedded, Layer explicit) {
        Map<ClientOption<?>, Object> values = base.values();
        Map<ClientOption<?>, Object> overridden = new HashMap<>();
        Duration apiCallTimeout = base.hasTimeouts() ? base.apiCallTimeout() : null;
        Duration apiCallAttemptTimeout = base.hasTimeouts() ? base.apiCallAttemptTimeout() : null;
        boolean timeoutsOverridden = false;
        Map<String, Map.Entry<String, List<String>>> headersByLowerName = new LinkedHashMap<>();
        Map<String, List<String>> query = new HashMap<>();
        Map<ContextKey<?>, Object> attributes = new HashMap<>();

        for (Layer layer : new Layer[] {embedded, explicit}) {
            if (layer == null || layer.kind() != Kind.DELTA) {
                continue;
            }
            OptionDelta options = layer.options();
            if (options.retry() != null) {
                values.put(RETRY_STRATEGY, options.retry());
                overridden.put(RETRY_STRATEGY, options.retry());
            }
            if (options.interceptors() != null) {
                List<Object> all = new ArrayList<>((List<?>) values.getOrDefault(INTERCEPTORS, List.of()));
                all.addAll(options.interceptors());
                values.put(INTERCEPTORS, List.copyOf(all));
                overridden.put(INTERCEPTORS, List.copyOf(all));
            }
            if (options.labels() != null) {
                Map<String, String> all = new HashMap<>();
                Labels current = (Labels) values.get(LABELS);
                if (current != null) {
                    all.putAll(current.entries());
                }
                all.putAll(options.labels().entries());
                values.put(LABELS, new Labels(all));
                overridden.put(LABELS, new Labels(all));
            }
            if (options.apiCallTimeout() != null) {
                apiCallTimeout = options.apiCallTimeout();
                timeoutsOverridden = true;
            }
            if (options.apiCallAttemptTimeout() != null) {
                apiCallAttemptTimeout = options.apiCallAttemptTimeout();
                timeoutsOverridden = true;
            }
            ContextDelta context = layer.context();
            for (Header header : context.headers()) {
                headersByLowerName.put(header.name().toLowerCase(Locale.ROOT),
                                       Map.entry(header.name(), List.copyOf(header.values())));
            }
            for (Query parameter : context.query()) {
                query.put(parameter.name(), List.copyOf(parameter.values()));
            }
            putIfPresent(attributes, TRACE, context.trace());
            putIfPresent(attributes, COUNT, context.count());
        }
        if (timeoutsOverridden) {
            TimeoutConfiguration merged = timeouts(apiCallTimeout, apiCallAttemptTimeout);
            values.put(SdkClientOption.TIMEOUT_CONFIGURATION, merged);
            overridden.put(SdkClientOption.TIMEOUT_CONFIGURATION, merged);
        }
        Map<String, List<String>> headers = new HashMap<>();
        headersByLowerName.values().forEach(e -> headers.put(e.getKey(), e.getValue()));
        return new Expected(values, overridden, headers, query, attributes);
    }

    private static TimeoutConfiguration timeouts(Duration apiCallTimeout, Duration apiCallAttemptTimeout) {
        return TimeoutConfiguration.builder()
                                   .apiCallTimeout(apiCallTimeout)
                                   .apiCallAttemptTimeout(apiCallAttemptTimeout)
                                   .build();
    }

    private static <K> void putIfPresent(Map<K, Object> map, K key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static Map<ClientOption<?>, Object> snapshot(SdkClientConfiguration configuration) {
        Map<ClientOption<?>, Object> values = new HashMap<>();
        for (ClientOption<?> option : configuration.options()) {
            values.put(option, configuration.get(option).orElseThrow());
        }
        return values;
    }

    @Provide
    Arbitrary<Base> bases() {
        return Combinators.combine(retries(), interceptorLists(), labels(), Arbitraries.of(true, false), durations(),
                                   durations(),
                                   Arbitraries.of("https://a.example.test", "https://b.example.test")
                                              .map(URI::create).injectNull(0.3),
                                   Arbitraries.of("us-east-1", "eu-west-1").injectNull(0.3))
                          .as(Base::new);
    }

    /** 2–8 calls whose embedded/explicit pairs are all distinct. */
    @Provide
    Arbitrary<List<Call>> calls() {
        return Combinators.combine(layers(), layers()).as(Call::new)
                          .list().ofMinSize(2).ofMaxSize(8).uniqueElements();
    }

    private static Arbitrary<Layer> layers() {
        Arbitrary<OptionDelta> options = Combinators.combine(retries(), interceptorLists(), Arbitraries.of(true, false),
                                                             labels(), durations(), durations())
                                                    .as(OptionDelta::new);
        Arbitrary<Header> header = Combinators.combine(Arbitraries.of("x-a", "X-A", "x-b", "X-B", "X-Trace-Id"),
                                                       headerValues())
                                              .as(Header::new);
        // Query names are case-sensitive: "q" and "Q" are different parameters.
        Arbitrary<Query> query = Combinators.combine(Arbitraries.of("q", "Q", "page"), headerValues()).as(Query::new);
        Arbitrary<ContextDelta> context = Combinators.combine(header.list().ofMaxSize(3), query.list().ofMaxSize(2),
                                                              Arbitraries.of("t1", "t2", "t3").injectNull(0.6),
                                                              Arbitraries.integers().between(0, 9).injectNull(0.6))
                                                     .as(ContextDelta::new);
        return Combinators.combine(Arbitraries.frequency(Tuple.of(1, Kind.NULL), Tuple.of(1, Kind.EMPTY),
                                                         Tuple.of(6, Kind.DELTA)),
                                   options, context)
                          .as(Layer::new);
    }

    private static Arbitrary<List<String>> headerValues() {
        return Arbitraries.of("1", "2", "3").list().ofMaxSize(2);
    }

    private static Arbitrary<Retry> retries() {
        return Arbitraries.integers().between(0, 99).map(Retry::new).injectNull(0.5);
    }

    private static Arbitrary<List<Interceptor>> interceptorLists() {
        return Arbitraries.of("a", "b", "c", "d").map(Interceptor::new).list().ofMaxSize(3).injectNull(0.5);
    }

    private static Arbitrary<Labels> labels() {
        return Arbitraries.maps(Arbitraries.of("k1", "k2", "k3"), Arbitraries.of("v1", "v2"))
                          .ofMaxSize(2).map(Labels::new).injectNull(0.6);
    }

    private static Arbitrary<Duration> durations() {
        return Arbitraries.longs().between(1, 5_000).map(Duration::ofMillis).injectNull(0.6);
    }
}
