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

package software.amazon.awssdk.http.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.http.provider.HttpClientResolverTest.caps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
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
import software.amazon.awssdk.core.resource.ResourceOwnership;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientResolverTest.FakeAsyncClient;
import software.amazon.awssdk.http.provider.HttpClientResolverTest.FakeSyncClient;
import software.amazon.awssdk.http.provider.HttpClientResolverTest.FakeSyncService;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException.Reason;

/**
 * Property 42 (Requirements 12.11, 12.12): for any installed provider set and any enumeration order, default selection
 * yields the unique highest-priority implementation of the requested mode, or rejects a top-priority tie naming exactly
 * the tied implementations (sorted), or fails when the mode has none. The outcome, including the diagnostic, is the
 * same in every order. An explicit client or implementation always wins without consulting the installed set, and two
 * installed implementations of one mode sharing a name make the set invalid.
 *
 * <p>The oracle is computed from the generated (name, priority) specs alone: the maximum priority of the mode and the
 * sorted names at that priority.
 */
class DefaultHttpSelectionPropertyTest {
    private static final String EXPLICIT_CLIENT = "explicit-client";
    private static final String EXPLICIT_IMPLEMENTATION = "explicit-implementation";

    // Feature: aws-sdk-java-v3, Property 42: Default HTTP selection is deterministic
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.MIXIN)
    @Label("Property 42: Default HTTP selection is deterministic")
    void defaultSelectionIsDeterministic(@ForAll("scenarios") Scenario scenario) {
        Statistics.label("sync outcome").collect(scenario.duplicate() != null ? "INVALID" : kind(scenario.sync()));
        Statistics.label("async outcome").collect(scenario.duplicate() != null ? "INVALID" : kind(scenario.async()));

        List<Observed> syncOutcomes = new ArrayList<>();
        List<Observed> asyncOutcomes = new ArrayList<>();
        for (long seed : scenario.seeds()) {
            Installation installation = scenario.install(seed);
            if (scenario.duplicate() != null) {
                checkDuplicateIsInvalid(scenario, installation);
            } else {
                InstalledHttpImplementations installed = InstalledHttpImplementations.of(installation.order());
                assertEquals(sortedNames(scenario.sync()), installed.names(HttpTransportMode.SYNC));
                assertEquals(sortedNames(scenario.async()), installed.names(HttpTransportMode.ASYNC));
                assertEquals(sortedNames(scenario.sync()),
                             installed.sync().stream().map(HttpImplementation::implementationName).toList());
                assertEquals(sortedNames(scenario.async()),
                             installed.async().stream().map(HttpImplementation::implementationName).toList());

                syncOutcomes.add(check(HttpTransportMode.SYNC, scenario.sync(), scenario.async(),
                                       () -> installed.defaultSync().implementationName()));
                asyncOutcomes.add(check(HttpTransportMode.ASYNC, scenario.async(), scenario.sync(),
                                        () -> installed.defaultAsync().implementationName()));
                checkResolver(scenario, installation);
            }
            checkExplicitWins(scenario, installation);
        }
        syncOutcomes.forEach(o -> assertEquals(syncOutcomes.get(0), o, "sync outcome depends on order"));
        asyncOutcomes.forEach(o -> assertEquals(asyncOutcomes.get(0), o, "async outcome depends on order"));
    }

    /** Default selection through {@link InstalledHttpImplementations} matches the oracle. */
    private static Observed check(HttpTransportMode mode, List<Spec> specs, List<Spec> other, Supplier<String> select) {
        Observed actual = observe(select);
        Expected expected = expected(specs, other);
        assertEquals(expected.selected(), actual.selected(), () -> mode + " " + specs + ": " + actual);
        assertEquals(expected.reason(), actual.reason(), () -> mode + " " + specs + ": " + actual);
        assertEquals(expected.implementations(), actual.implementations(), () -> mode + " " + specs);
        if (expected.reason() != null) {
            assertEquals(Optional.of(mode), actual.mode());
            assertEquals(Optional.of(HttpClientResolver.clientOptionName(mode)), actual.option());
        }
        if (expected.reason() == Reason.TIED_PRIORITY) {
            assertTrue(actual.message().startsWith(expected.implementations().size() + " installed "
                                                   + InstalledHttpImplementations.label(mode) + " HTTP implementations "
                                                   + "share the highest priority " + maxPriority(specs) + ": "),
                       actual.message());
            expected.implementations().forEach(name -> assertTrue(actual.message().contains(name + " ("),
                                                                  actual.message()));
        }
        return actual;
    }

    /** Default selection through {@link HttpClientResolver} matches the oracle, consults the set once, creates once. */
    private static void checkResolver(Scenario scenario, Installation installation) {
        for (HttpTransportMode mode : HttpTransportMode.values()) {
            boolean sync = mode == HttpTransportMode.SYNC;
            Expected expected = sync ? expected(scenario.sync(), scenario.async())
                                     : expected(scenario.async(), scenario.sync());
            AtomicInteger calls = new AtomicInteger();
            Supplier<InstalledHttpImplementations> supplier = counting(calls, installation);
            Observed actual = observe(() -> sync ? HttpClientResolver.sync().installed(supplier).resolve()
                                                                     .implementationName()
                                                 : HttpClientResolver.async().installed(supplier).resolve()
                                                                     .implementationName());
            assertEquals(1, calls.get(), "default selection consults the installed set exactly once");
            assertEquals(expected.selected(), actual.selected(), () -> mode + ": " + actual);
            assertEquals(expected.reason(), actual.reason(), () -> mode + ": " + actual);
            assertEquals(expected.implementations(), actual.implementations());
            for (Created created : installation.all()) {
                int expectedCreates = created.mode() == mode && created.name().equals(expected.selected()) ? 1 : 0;
                assertEquals(expectedCreates, created.creates().getAndSet(0), created::toString);
            }
        }
    }

    /** A duplicate name within a mode makes the whole installed set invalid, in every order, and nothing is created. */
    private static void checkDuplicateIsInvalid(Scenario scenario, Installation installation) {
        Duplicate duplicate = scenario.duplicate();
        String name = scenario.duplicatedName();
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class, () -> InstalledHttpImplementations.of(installation.order()));
        assertEquals(Reason.INVALID_IMPLEMENTATION, e.reason());
        assertEquals(Optional.of(duplicate.mode()), e.mode());
        assertEquals(List.of(name), e.implementations());
        assertTrue(e.getMessage().contains("are named " + name + ": "), e.getMessage());

        for (HttpTransportMode mode : HttpTransportMode.values()) {
            AtomicInteger calls = new AtomicInteger();
            Supplier<InstalledHttpImplementations> supplier = counting(calls, installation);
            HttpImplementationSelectionException viaResolver = assertThrows(
                HttpImplementationSelectionException.class,
                () -> {
                    if (mode == HttpTransportMode.SYNC) {
                        HttpClientResolver.sync().installed(supplier).resolve();
                    } else {
                        HttpClientResolver.async().installed(supplier).resolve();
                    }
                });
            assertEquals(Reason.INVALID_IMPLEMENTATION, viaResolver.reason());
            assertEquals(List.of(name), viaResolver.implementations());
            assertEquals(1, calls.get());
        }
        installation.all().forEach(c -> assertEquals(0, c.creates().get(), c::toString));
    }

    /** An explicit client or implementation wins over every installed set without consulting it. */
    private static void checkExplicitWins(Scenario scenario, Installation installation) {
        AtomicInteger calls = new AtomicInteger();
        Supplier<InstalledHttpImplementations> supplier = counting(calls, installation);

        FakeSyncClient syncClient = new FakeSyncClient(EXPLICIT_CLIENT, caps(HttpTransportMode.SYNC));
        ResolvedHttpClient<SdkSyncHttpClient> syncResolved =
            HttpClientResolver.sync().client(syncClient).installed(supplier).resolve();
        assertSame(syncClient, syncResolved.client());
        assertEquals(EXPLICIT_CLIENT, syncResolved.implementationName());
        assertEquals(ResourceOwnership.CALLER_OWNED, syncResolved.ownership());
        syncResolved.close();
        assertEquals(0, syncClient.closed.get(), "a caller-owned client is never closed");

        FakeAsyncClient asyncClient = new FakeAsyncClient(EXPLICIT_CLIENT, caps(HttpTransportMode.ASYNC));
        ResolvedHttpClient<SdkAsyncHttpClient> asyncResolved =
            HttpClientResolver.async().client(asyncClient).installed(supplier).resolve();
        assertSame(asyncClient, asyncResolved.client());
        assertEquals(ResourceOwnership.CALLER_OWNED, asyncResolved.ownership());
        asyncResolved.close();
        assertEquals(0, asyncClient.closed.get(), "a caller-owned client is never closed");

        FakeSyncService syncImplementation = new FakeSyncService(EXPLICIT_IMPLEMENTATION, scenario.explicitPriority(),
                                                                 caps(HttpTransportMode.SYNC));
        ResolvedHttpClient<SdkSyncHttpClient> syncBuilt =
            HttpClientResolver.sync().implementation(syncImplementation).installed(supplier).resolve();
        assertEquals(EXPLICIT_IMPLEMENTATION, syncBuilt.implementationName());
        assertEquals(ResourceOwnership.SDK_OWNED, syncBuilt.ownership());
        assertEquals(1, syncImplementation.creates.get());

        CountingAsyncService asyncImplementation = new CountingAsyncService(EXPLICIT_IMPLEMENTATION,
                                                                            scenario.explicitPriority());
        ResolvedHttpClient<SdkAsyncHttpClient> asyncBuilt =
            HttpClientResolver.async().implementation(asyncImplementation).installed(supplier).resolve();
        assertEquals(EXPLICIT_IMPLEMENTATION, asyncBuilt.implementationName());
        assertEquals(ResourceOwnership.SDK_OWNED, asyncBuilt.ownership());
        assertEquals(1, asyncImplementation.creates.get());

        assertEquals(0, calls.get(), "explicit configuration never consults the installed set");
        installation.all().forEach(c -> assertEquals(0, c.creates().get(), c::toString));
    }

    // Oracle.

    private static Expected expected(List<Spec> specs, List<Spec> other) {
        if (specs.isEmpty()) {
            return new Expected(null, Reason.NO_IMPLEMENTATION, sortedNames(other));
        }
        int max = maxPriority(specs);
        List<String> top = specs.stream().filter(s -> s.priority() == max).map(Spec::name).sorted().toList();
        return top.size() == 1 ? new Expected(top.get(0), null, List.of())
                               : new Expected(null, Reason.TIED_PRIORITY, top);
    }

    private static int maxPriority(List<Spec> specs) {
        return specs.stream().mapToInt(Spec::priority).max().orElseThrow();
    }

    private static List<String> sortedNames(List<Spec> specs) {
        return specs.stream().map(Spec::name).sorted().toList();
    }

    private static String kind(List<Spec> specs) {
        Expected expected = expected(specs, List.of());
        return expected.reason() == null ? "SELECTED" : expected.reason().name();
    }

    private static Observed observe(Supplier<String> select) {
        try {
            return new Observed(select.get(), null, List.of(), Optional.empty(), Optional.empty(), null);
        } catch (HttpImplementationSelectionException e) {
            return new Observed(null, e.reason(), e.implementations(), e.mode(), e.option(), e.getMessage());
        }
    }

    private static Supplier<InstalledHttpImplementations> counting(AtomicInteger calls, Installation installation) {
        return () -> {
            calls.incrementAndGet();
            return InstalledHttpImplementations.of(installation.order());
        };
    }

    // Generators.

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Duplicate> duplicate = Combinators.combine(Arbitraries.of(HttpTransportMode.class),
                                                             Arbitraries.integers().between(0, 5), priorities())
                                                    .as(Duplicate::new);
        // About one scenario in ten has a duplicate name; the rest exercise selection.
        Arbitrary<Boolean> withDuplicate = Arbitraries.frequency(Tuple.of(9, false), Tuple.of(1, true));
        return Combinators.combine(specs(), specs(), duplicate, withDuplicate, Arbitraries.longs().list().ofSize(4),
                                   priorities())
                          .as((sync, async, dup, useDup, seeds, explicit) ->
                                  new Scenario(sync, async, useDup ? dup : null, seeds, explicit))
                          .map(Scenario::withoutImpossibleDuplicate);
    }

    /** 0-6 implementations of one mode with unique names; few priorities, so ties are common. */
    private static Arbitrary<List<Spec>> specs() {
        Arbitrary<String> names = Arbitraries.strings().withChars("abAB-.1").ofMinLength(1).ofMaxLength(3);
        return Arbitraries.integers().between(0, 6).flatMap(
            n -> Combinators.combine(names.list().uniqueElements().ofSize(n), priorities().list().ofSize(n))
                            .as((ns, ps) -> IntStream.range(0, n).mapToObj(i -> new Spec(ns.get(i), ps.get(i)))
                                                     .toList()));
    }

    private static Arbitrary<Integer> priorities() {
        return Arbitraries.frequencyOf(Tuple.of(8, Arbitraries.integers().between(0, 3)),
                                       Tuple.of(1, Arbitraries.of(Integer.MIN_VALUE, -1, Integer.MAX_VALUE)));
    }

    record Spec(String name, int priority) {
    }

    /** Adds a second implementation of {@code mode} named like the {@code index}-th (modulo) spec of the mode. */
    record Duplicate(HttpTransportMode mode, int index, int priority) {
    }

    record Scenario(List<Spec> sync, List<Spec> async, Duplicate duplicate, List<Long> seeds, int explicitPriority) {
        /** A duplicate needs an implementation of its mode to duplicate. */
        Scenario withoutImpossibleDuplicate() {
            return duplicate == null || !specsOf(duplicate.mode()).isEmpty()
                   ? this : new Scenario(sync, async, null, seeds, explicitPriority);
        }

        List<Spec> specsOf(HttpTransportMode mode) {
            return mode == HttpTransportMode.SYNC ? sync : async;
        }

        String duplicatedName() {
            List<Spec> specs = specsOf(duplicate.mode());
            return specs.get(duplicate.index() % specs.size()).name();
        }

        /** Fresh implementations for every spec (and the duplicate), in the order {@code seed} shuffles them into. */
        Installation install(long seed) {
            List<Created> all = new ArrayList<>();
            sync.forEach(s -> all.add(Created.sync(s.name(), s.priority())));
            async.forEach(s -> all.add(Created.async(s.name(), s.priority())));
            if (duplicate != null) {
                all.add(duplicate.mode() == HttpTransportMode.SYNC
                        ? Created.sync(duplicatedName(), duplicate.priority())
                        : Created.async(duplicatedName(), duplicate.priority()));
            }
            List<Created> order = new ArrayList<>(all);
            Collections.shuffle(order, new Random(seed));
            return new Installation(order);
        }
    }

    /** One enumeration order of freshly created implementations. */
    record Installation(List<Created> all) {
        List<HttpImplementation<?>> order() {
            return all.stream().<HttpImplementation<?>>map(Created::implementation).toList();
        }
    }

    /** A created implementation with its create counter. */
    record Created(HttpTransportMode mode, String name, HttpImplementation<?> implementation, AtomicInteger creates) {
        static Created sync(String name, int priority) {
            FakeSyncService service = new FakeSyncService(name, priority, caps(HttpTransportMode.SYNC));
            return new Created(HttpTransportMode.SYNC, name, service, service.creates);
        }

        static Created async(String name, int priority) {
            CountingAsyncService service = new CountingAsyncService(name, priority);
            return new Created(HttpTransportMode.ASYNC, name, service, service.creates);
        }

        @Override
        public String toString() {
            return mode + ":" + implementation;
        }
    }

    record Expected(String selected, Reason reason, List<String> implementations) {
    }

    record Observed(String selected, Reason reason, List<String> implementations, Optional<HttpTransportMode> mode,
                    Optional<String> option, String message) {
    }

    /** An async implementation that counts {@link #create} calls. */
    static final class CountingAsyncService implements SdkAsyncHttpService {
        final AtomicInteger creates = new AtomicInteger();
        private final String name;
        private final int priority;

        CountingAsyncService(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public String implementationName() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return caps(HttpTransportMode.ASYNC);
        }

        @Override
        public SdkAsyncHttpClient create(HttpClientConstruction construction) {
            creates.incrementAndGet();
            return new FakeAsyncClient(name, caps(HttpTransportMode.ASYNC));
        }

        @Override
        public String toString() {
            return name + "@" + priority;
        }
    }
}
