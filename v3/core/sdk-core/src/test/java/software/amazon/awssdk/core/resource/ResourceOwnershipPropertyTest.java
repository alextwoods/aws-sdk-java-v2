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

package software.amazon.awssdk.core.resource;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.core.resource.ResourceOwnership.CALLER_OWNED;
import static software.amazon.awssdk.core.resource.ResourceOwnership.SDK_OWNED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.RandomDistribution;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.core.client.config.ClientConfigurationResolver;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientSetting;
import software.amazon.awssdk.core.client.config.ConfigurationSource;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.client.config.ResolvedOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.core.exception.SdkCloseException;

/**
 * Property 27: Resource ownership and close aggregation.
 *
 * <p>For any mixture of SDK-owned and caller-owned resources, any number of client closes, and any subset of close
 * failures, every SDK-owned resource receives exactly one close attempt, no caller-owned resource is closed, all
 * SDK-owned resources are attempted, and the reported failure set equals the failures thrown.
 *
 * <p>Three views, each compared with an oracle written here from the requirements rather than from
 * {@link ResourceScope}:
 * <ul>
 *     <li><b>Scope</b> ({@link #resourceOwnershipAndCloseAggregation}): 1-10 distinct resources (closeable or not),
 *     registered with mixed ownership, with extra registrations of the same instance (identity aliases) and distinct
 *     instances that are {@code equals} to each other; a subset fails on close with a {@link RuntimeException}, an
 *     {@link Error}, a checked exception or an {@link InterruptedException}; some re-enter {@code close()} from their
 *     own close; the scope is closed 1-4 times, sequentially or by 2-3 threads released together by a barrier. The
 *     oracle deduplicates by registration index (one index per instance), lets any caller-owned registration win,
 *     and expects exactly the SDK-owned closeables to be attempted once each, in reverse first-registration order;
 *     the failures, in that order, as the suppressed exceptions of one {@link SdkCloseException} seen by exactly one
 *     caller; and the interrupt flag set on the closing thread only, and only if an {@code InterruptedException} was
 *     thrown.</li>
 *     <li><b>Resolver</b> ({@link #resolvedResourceOwnership}): resources reach the configuration as builder values
 *     (caller-owned), builder values recorded SDK-owned (a builder or factory), values the SDK creates from JVM
 *     properties, environment variables, profile properties or defaults (SDK-owned), and explicitly registered
 *     resources, with aliases across all of them; {@code ResourceScope.of(config.resources())} must close exactly
 *     the resources the oracle says the SDK owns (Requirement 9.6's recording, end to end).</li>
 *     <li><b>Construction rollback</b> ({@link #constructionRollbackReleasesAcquiredResources}): Requirement 12.14's
 *     ownership semantics. sdk-core cannot depend on http-spi, where the real construction lives
 *     ({@code software.amazon.awssdk.http.provider.HttpClientResolver}, tested by {@code HttpClientResolverTest});
 *     this view models construction as acquiring resources step by step and failing at a generated step, releasing
 *     what was acquired through a {@code ResourceScope}, and checks that exactly the SDK-owned resources acquired
 *     before the failure are released, once each, as the resolver does.</li>
 * </ul>
 * Close attempts, re-entrant closes and interrupt flags are recorded per thread; every wait is bounded, so a deadlock
 * fails the try instead of hanging the build.
 *
 * <p><b>Validates: Requirements 9.6, 9.7, 9.8, 9.13, 9.14, 12.14</b>
 */
class ResourceOwnershipPropertyTest {
    private static final long WAIT_SECONDS = 10;
    private static final String INSTANCE_TEXT = "instance-";

    // Feature: aws-sdk-java-v3, Property 27: Resource ownership and close aggregation
    // Embedded edge cases are disabled: they are mostly the one-resource, no-alias, sequential scenario. The
    // statistics coverage checks below fail the property if aliases, twins, failure kinds, re-entry or concurrency
    // stop being generated.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 27: Resource ownership and close aggregation")
    void resourceOwnershipAndCloseAggregation(@ForAll("scenarios") Scenario scenario) throws Exception {
        Thread.interrupted();
        Run run = new Run();
        List<Tracked> instances = instantiate(scenario.specs(), run);
        List<Registration> registrations = scenario.registrations();
        List<ManagedResource<?>> managed = new ArrayList<>();
        registrations.forEach(r -> managed.add(ManagedResource.of(instances.get(r.resource()), r.ownership())));
        Expected expected = Expected.of(registrations, instances);
        collectStatistics(scenario, registrations, expected);

        ResourceScope scope = ResourceScope.of(managed);
        run.scope = scope;

        // The ownership set: identity-deduplicated, first-registration order, caller ownership wins.
        List<ManagedResource<?>> resources = scope.resources();
        assertEquals(expected.order().size(), resources.size(), "distinct resources");
        for (int i = 0; i < resources.size(); i++) {
            int index = expected.order().get(i);
            assertSame(instances.get(index), resources.get(i).resource(), "resource " + i);
            assertEquals(expected.ownership().get(index), resources.get(i).ownership(), "ownership of " + index);
        }
        for (Tracked instance : instances) {
            assertEquals(Optional.of(expected.ownership().get(instance.index)), scope.ownership(instance));
            Tracked twin = instance.twin();
            assertEquals(instance, twin);
            assertEquals(Optional.empty(), scope.ownership(twin), "an equal but distinct instance is not in scope");
        }
        assertFalse(scope.isClosed());
        assertTrue(run.attempts.isEmpty(), "nothing is closed before close()");

        // Close: concurrently (all closers released together), then sequential repeats.
        List<Outcome> concurrent = closeConcurrently(scope, run, scenario.closers());
        List<Outcome> sequential = new ArrayList<>();
        for (int i = 0; i < scenario.sequentialCloses(); i++) {
            sequential.add(closeOnce(scope, run));
        }
        assertTrue(scope.isClosed());

        List<Attempt> attempts = List.copyOf(run.attempts);
        assertEquals(expected.attempts(), attempts.stream().map(Attempt::resource).toList(), "close attempts");
        Thread closingThread = attempts.isEmpty() ? null : attempts.get(0).thread();
        attempts.forEach(a -> assertSame(closingThread, a.thread(), "every attempt is made by the first closer"));
        assertCallerOwnedNeverClosed(instances, expected.ownership(), Set.copyOf(expected.attempts()));

        Outcome first;
        if (scenario.closers() == 0) {
            first = sequential.get(0);
        } else {
            first = concurrent.stream().filter(o -> o.thread() == closingThread).findFirst().orElse(null);
            if (closingThread != null) {
                assertNotNull(first, "the closing thread is one of the concurrent closers");
            }
        }
        List<Outcome> outcomes = new ArrayList<>(concurrent);
        outcomes.addAll(sequential);
        for (Outcome outcome : outcomes) {
            boolean isFirst = outcome == first;
            if (isFirst && !expected.failures().isEmpty()) {
                assertAggregated(expected.failures(), outcome.thrown());
            } else {
                assertNull(outcome.thrown(), "only the first close reports failures");
            }
            assertEquals(isFirst && expected.interrupted(), outcome.interrupted(), "interrupt flag");
            assertEquals(expected.attempts().size(), outcome.attemptsAtReturn(),
                         "a close returns only after every attempt");
        }
        assertEquals(expected.failures().isEmpty() ? 0 : 1, outcomes.stream().filter(o -> o.thrown() != null).count(),
                     "exactly one caller observes the failures");

        // Re-entrant closes: one per attempted re-entrant resource, on the closing thread, returning at once.
        long reentrant = expected.attempts().stream().filter(i -> scenario.specs().get(i).reentrant()).count();
        List<Reentry> reentries = List.copyOf(run.reentries);
        assertEquals(reentrant, reentries.size(), "re-entrant closes");
        for (Reentry reentry : reentries) {
            assertSame(closingThread, reentry.thread());
            assertTrue(reentry.closedSeen(), "a re-entrant close sees the scope closed");
            assertNull(reentry.thrown(), "a re-entrant close throws nothing");
        }
        boolean overlapped = concurrent.stream().anyMatch(o -> o != first && o.attemptsAtEntry()
                                                                             < expected.attempts().size());
        Statistics.label("concurrent closer waited").collect(overlapped);
    }

    // Feature: aws-sdk-java-v3, Property 27: Resource ownership and close aggregation
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 27 (resolver view): Resource ownership and close aggregation")
    void resolvedResourceOwnership(@ForAll("resolverScenarios") ResolverScenario scenario) {
        Thread.interrupted();
        Run run = new Run();
        List<Tracked> instances = instantiate(scenario.pool(), run);
        int pool = instances.size();

        SdkClientConfiguration.Builder values = SdkClientConfiguration.builder();
        ClientConfigurationResolver.Builder resolver = ClientConfigurationResolver.builder();
        Map<String, String> jvm = new HashMap<>();
        Map<String, String> environment = new HashMap<>();
        Map<String, String> profile = new HashMap<>();
        List<ClientOption<Object>> keys = new ArrayList<>();
        // The oracle: explicit registrations and closeable option values, caller ownership winning.
        Map<Integer, ResourceOwnership> ownership = new LinkedHashMap<>();
        for (ExplicitResource explicit : scenario.explicit()) {
            int index = explicit.instance() % pool;
            values.addResource(ManagedResource.of(instances.get(index), explicit.ownership()));
            ownership.merge(index, explicit.ownership(), ResourceOwnershipPropertyTest::callerWins);
        }
        for (int i = 0; i < scenario.options().size(); i++) {
            OptionSupply supply = scenario.options().get(i);
            int index = supply.instance() % pool;
            Tracked value = instances.get(index);
            ClientOption<Object> key = ClientOption.create("p27.option" + i, Object.class);
            keys.add(key);
            String text = INSTANCE_TEXT + index;
            switch (supply.supply()) {
                case BUILDER -> values.option(key, value);
                case BUILDER_SDK_OWNED -> values.option(new ResolvedOption<>(key, value, ConfigurationSource.BUILDER,
                                                                             SDK_OWNED));
                case JVM_PROPERTY -> jvm.put(jvmProperty(i), text);
                case ENVIRONMENT -> environment.put(environmentVariable(i), text);
                case PROFILE -> profile.put(profileProperty(i), text);
                case DEFAULT -> { }
                default -> throw new IllegalStateException(supply.supply().toString());
            }
            if (supply.supply().external() || supply.setting()) {
                ClientSetting.Builder<Object> setting =
                    ClientSetting.builder(key)
                                 .jvmProperty(jvmProperty(i))
                                 .environmentVariable(environmentVariable(i))
                                 .profileProperty(profileProperty(i))
                                 .parser(t -> instances.get(Integer.parseInt(t.substring(INSTANCE_TEXT.length()))));
                if (supply.supply() == Supply.DEFAULT) {
                    setting.defaultValue(value);
                }
                resolver.addSetting(setting.build());
            }
            if (value instanceof AutoCloseable) {
                ownership.merge(index, supply.supply().ownership(), ResourceOwnershipPropertyTest::callerWins);
            }
        }
        SdkClientConfiguration config =
            resolver.sources(ConfigurationSources.builder()
                                                 .jvmProperties(SettingSource.fromMap(jvm))
                                                 .environment(SettingSource.fromMap(environment))
                                                 .profile(SettingSource.fromMap(profile))
                                                 .build())
                    .build()
                    .resolve(values.build());

        for (int i = 0; i < keys.size(); i++) {
            OptionSupply supply = scenario.options().get(i);
            ResolvedOption<Object> resolved = config.resolved(keys.get(i)).orElseThrow();
            assertSame(instances.get(supply.instance() % pool), resolved.value(), "value of option " + i);
            assertEquals(supply.supply().source(), resolved.source(), "source of option " + i);
            assertEquals(supply.supply().ownership(), resolved.ownership(), "ownership of option " + i);
        }

        ResourceScope scope = ResourceScope.of(config.resources());
        run.scope = scope;
        for (Tracked instance : instances) {
            assertEquals(Optional.ofNullable(ownership.get(instance.index)), scope.ownership(instance),
                         "ownership of " + instance);
        }
        Set<Integer> owned = new HashSet<>();
        ownership.forEach((index, o) -> {
            if (o == SDK_OWNED && instances.get(index) instanceof AutoCloseable) {
                owned.add(index);
            }
        });

        Outcome outcome = closeOnce(scope, run);
        Outcome again = closeOnce(scope, run);
        List<Integer> attempts = run.attempts.stream().map(Attempt::resource).toList();
        assertEquals(owned, Set.copyOf(attempts), "exactly the SDK-owned resources are attempted");
        assertEquals(owned.size(), attempts.size(), "each SDK-owned resource is attempted once");
        List<Integer> reverseRegistration = new ArrayList<>();
        List<ManagedResource<?>> registered = scope.resources();
        for (int i = registered.size() - 1; i >= 0; i--) {
            if (registered.get(i).ownership() == SDK_OWNED && registered.get(i).resource() instanceof AutoCloseable) {
                reverseRegistration.add(((Tracked) registered.get(i).resource()).index);
            }
        }
        assertEquals(reverseRegistration, attempts, "attempts are in reverse registration order");
        assertCallerOwnedNeverClosed(instances, ownership, owned);
        List<Throwable> failures = new ArrayList<>();
        attempts.forEach(i -> Optional.ofNullable(instances.get(i).failure()).ifPresent(failures::add));
        if (failures.isEmpty()) {
            assertNull(outcome.thrown());
        } else {
            assertAggregated(failures, outcome.thrown());
        }
        assertEquals(failures.stream().anyMatch(InterruptedException.class::isInstance), outcome.interrupted());
        assertNull(again.thrown(), "a second close reports nothing");
        assertEquals(attempts.size(), again.attemptsAtReturn(), "a second close attempts nothing");
        assertFalse(again.interrupted(), "a second close sets no interrupt flag");

        Statistics.label("supplies").collect(scenario.options().stream().map(o -> o.supply().name()).distinct()
                                                     .count());
        Statistics.label("mixed ownership alias").collect(mixedOwnershipAlias(scenario, pool));
        Statistics.label("mixed ownership alias").coverage(c -> c.check(true).percentage(p -> p >= 10));
        Statistics.label("sdk-owned attempted").collect(!owned.isEmpty());
        Statistics.label("sdk-owned attempted").coverage(c -> c.check(true).percentage(p -> p >= 50));
    }

    // Feature: aws-sdk-java-v3, Property 27: Resource ownership and close aggregation
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 27 (construction rollback view): Resource ownership and close aggregation")
    void constructionRollbackReleasesAcquiredResources(@ForAll("rollbackScenarios") RollbackScenario scenario) {
        Thread.interrupted();
        Run run = new Run();
        List<Tracked> instances = instantiate(scenario.steps(), run);
        int failAt = scenario.failAt() % (instances.size() + 1); // == size: construction succeeds
        boolean fails = failAt < instances.size();

        List<ManagedResource<?>> acquired = new ArrayList<>();
        RuntimeException constructionFailure = null;
        ResourceScope client = null;
        boolean interrupted;
        try {
            client = construct(instances, scenario.ownership(), failAt, acquired, scenario.tiedPriority());
        } catch (RuntimeException e) {
            constructionFailure = e;
        } finally {
            interrupted = Thread.interrupted();
        }

        // The oracle: the SDK-owned closeables acquired before the failure, released in reverse acquisition order.
        List<Integer> expected = new ArrayList<>();
        for (int i = failAt - 1; i >= 0; i--) {
            if (scenario.ownership().get(i) == SDK_OWNED && scenario.steps().get(i).closeable()) {
                expected.add(i);
            }
        }
        List<Throwable> failures = new ArrayList<>();
        expected.forEach(i -> Optional.ofNullable(instances.get(i).failure()).ifPresent(failures::add));
        List<Integer> attempts = run.attempts.stream().map(Attempt::resource).toList();
        if (fails) {
            assertNotNull(constructionFailure, "construction fails");
            assertEquals(scenario.tiedPriority() ? "tied provider priority" : "unsupported option",
                         constructionFailure.getMessage());
            assertEquals(expected, attempts, "the SDK-owned resources acquired before the failure are released");
            if (failures.isEmpty()) {
                assertEquals(0, constructionFailure.getSuppressed().length);
            } else {
                assertEquals(1, constructionFailure.getSuppressed().length);
                assertAggregated(failures, constructionFailure.getSuppressed()[0]);
            }
            assertEquals(failures.stream().anyMatch(InterruptedException.class::isInstance), interrupted);
        } else {
            assertNull(constructionFailure);
            assertEquals(List.of(), attempts, "a constructed client releases nothing until it is closed");
            Outcome outcome = closeOnce(client, run);
            attempts = run.attempts.stream().map(Attempt::resource).toList();
            assertEquals(expected, attempts, "closing the client releases every SDK-owned resource");
            if (failures.isEmpty()) {
                assertNull(outcome.thrown());
            } else {
                assertAggregated(failures, outcome.thrown());
            }
        }
        Map<Integer, ResourceOwnership> ownership = new HashMap<>();
        for (int i = 0; i < instances.size(); i++) {
            ownership.put(i, scenario.ownership().get(i));
        }
        assertCallerOwnedNeverClosed(instances, ownership, Set.copyOf(expected));
        Statistics.label("construction").collect(fails ? "fails" : "succeeds");
        Statistics.label("construction").coverage(c -> {
            c.check("fails").percentage(p -> p >= 30);
            c.check("succeeds").percentage(p -> p >= 5);
        });
    }

    /**
     * Stand-in for HTTP client construction (the real one is http-spi's HttpClientResolver): acquires
     * {@code instances} in order, and at step {@code failAt} fails as an unsupported option or tied provider priority would, first releasing everything
     * acquired so far through a {@link ResourceScope}; a release failure is attached to the construction failure.
     */
    private static ResourceScope construct(List<Tracked> instances, List<ResourceOwnership> ownership, int failAt,
                                           List<ManagedResource<?>> acquired, boolean tiedPriority) {
        for (int i = 0; i < instances.size(); i++) {
            if (i == failAt) {
                IllegalStateException failure =
                    new IllegalStateException(tiedPriority ? "tied provider priority" : "unsupported option");
                try {
                    ResourceScope.of(acquired).close();
                } catch (SdkCloseException e) {
                    failure.addSuppressed(e);
                }
                throw failure;
            }
            acquired.add(ManagedResource.of(instances.get(i), ownership.get(i)));
        }
        return ResourceScope.of(acquired);
    }

    // Checks.

    private static void assertAggregated(List<Throwable> expected, Throwable thrown) {
        SdkCloseException exception = assertInstanceOf(SdkCloseException.class, thrown);
        List<Throwable> failures = exception.failures();
        Throwable[] suppressed = exception.getSuppressed();
        assertEquals(expected.size(), failures.size(), "failures");
        assertEquals(expected.size(), suppressed.length, "suppressed");
        for (int i = 0; i < expected.size(); i++) {
            assertSame(expected.get(i), failures.get(i), "failure " + i);
            assertSame(expected.get(i), suppressed[i], "suppressed " + i);
        }
        assertNull(exception.getCause());
    }

    private static void assertCallerOwnedNeverClosed(List<Tracked> instances, Map<Integer, ResourceOwnership> ownership,
                                                     Set<Integer> attempted) {
        for (Tracked instance : instances) {
            if (instance instanceof CloseableResource closeable) {
                assertEquals(attempted.contains(instance.index) ? 1 : 0, closeable.closes.get(),
                             "closes of " + instance + " (" + ownership.get(instance.index) + ")");
            }
        }
    }

    private static Outcome closeOnce(ResourceScope scope, Run run) {
        int atEntry = run.attempts.size();
        Throwable thrown = null;
        try {
            scope.close();
        } catch (Throwable t) {
            thrown = t;
        }
        boolean interrupted = Thread.interrupted();
        return new Outcome(Thread.currentThread(), atEntry, thrown, interrupted, run.attempts.size());
    }

    private static List<Outcome> closeConcurrently(ResourceScope scope, Run run, int closers) throws Exception {
        if (closers == 0) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(closers);
        try {
            CyclicBarrier barrier = new CyclicBarrier(closers);
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < closers; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await(WAIT_SECONDS, SECONDS);
                    return closeOnce(scope, run);
                }));
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(WAIT_SECONDS, SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(WAIT_SECONDS, SECONDS), "closer threads terminate");
        }
    }

    private static ResourceOwnership callerWins(ResourceOwnership a, ResourceOwnership b) {
        return a == CALLER_OWNED || b == CALLER_OWNED ? CALLER_OWNED : SDK_OWNED;
    }

    private static boolean mixedOwnershipAlias(ResolverScenario scenario, int pool) {
        Map<Integer, Set<ResourceOwnership>> seen = new HashMap<>();
        scenario.explicit().forEach(e -> seen.computeIfAbsent(e.instance() % pool, k -> new HashSet<>())
                                             .add(e.ownership()));
        scenario.options().forEach(o -> seen.computeIfAbsent(o.instance() % pool, k -> new HashSet<>())
                                            .add(o.supply().ownership()));
        return seen.values().stream().anyMatch(s -> s.size() == 2);
    }

    private static void collectStatistics(Scenario scenario, List<Registration> registrations, Expected expected) {
        Map<Integer, Set<ResourceOwnership>> seen = new HashMap<>();
        registrations.forEach(r -> seen.computeIfAbsent(r.resource(), k -> new HashSet<>()).add(r.ownership()));
        boolean mixedAlias = seen.values().stream().anyMatch(s -> s.size() == 2);
        Set<String> groups = new HashSet<>();
        boolean twins = false;
        for (Spec spec : scenario.specs()) {
            twins |= !groups.add(spec.closeable() + "/" + spec.equalsGroup());
        }
        Set<Failure> failed = new HashSet<>();
        expected.attempts().forEach(i -> failed.add(scenario.specs().get(i).failure()));
        boolean reentered = expected.attempts().stream().anyMatch(i -> scenario.specs().get(i).reentrant());
        boolean plain = scenario.specs().stream().anyMatch(s -> !s.closeable());

        Statistics.label("closers").collect(scenario.closers() == 0 ? "sequential" : "concurrent");
        Statistics.label("closers").coverage(c -> c.check("concurrent").percentage(p -> p >= 25));
        Statistics.label("close calls").collect(scenario.closers() + scenario.sequentialCloses());
        Statistics.label("mixed-ownership alias").collect(mixedAlias);
        Statistics.label("mixed-ownership alias").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("equal distinct instances").collect(twins);
        Statistics.label("equal distinct instances").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("non-closeable resource").collect(plain);
        Statistics.label("non-closeable resource").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("re-entrant close").collect(reentered);
        Statistics.label("re-entrant close").coverage(c -> c.check(true).percentage(p -> p >= 10));
        Statistics.label("no failure").collect(expected.failures().isEmpty());
        Statistics.label("no failure").coverage(c -> {
            c.check(true).percentage(p -> p >= 10);
            c.check(false).percentage(p -> p >= 20);
        });
        for (Failure failure : List.of(Failure.RUNTIME, Failure.ERROR, Failure.CHECKED, Failure.INTERRUPTED)) {
            String label = "failure " + failure;
            Statistics.label(label).collect(failed.contains(failure));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 5));
        }
    }

    private static String jvmProperty(int option) {
        return "test.p27.option" + option;
    }

    private static String environmentVariable(int option) {
        return "TEST_P27_OPTION_" + option;
    }

    private static String profileProperty(int option) {
        return "test_p27_option_" + option;
    }

    // Generators.

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Boolean> healthy = percent(20);
        return Combinators.combine(specs(1, 10), healthy).as((s, h) -> h ? withoutFailures(s) : s).flatMap(specs -> {
            Arbitrary<List<ResourceOwnership>> primary = ownership().list().ofSize(specs.size());
            Arbitrary<List<Alias>> aliases = Combinators.combine(uniform(0, 9), ownership(), uniform(0, 20))
                                                        .as(Alias::new)
                                                        .list().ofMaxSize(6)
                                                        .withSizeDistribution(RandomDistribution.uniform());
            Arbitrary<Integer> closers = Arbitraries.frequency(Tuple.of(50, 0), Tuple.of(30, 2), Tuple.of(20, 3));
            return Combinators.combine(primary, aliases, closers, uniform(0, 3))
                              // 1-4 close calls in total: 1-4 sequential, or 2-3 concurrent plus up to 4 - k repeats.
                              .as((p, a, k, extra) -> new Scenario(specs, p, a, k, k == 0 ? 1 + extra
                                                                                          : extra % (5 - k)));
        });
    }

    @Provide
    Arbitrary<ResolverScenario> resolverScenarios() {
        Arbitrary<OptionSupply> option = Combinators.combine(uniform(0, 5), Arbitraries.of(Supply.class), percent(50))
                                                    .as(OptionSupply::new);
        Arbitrary<ExplicitResource> explicit = Combinators.combine(uniform(0, 5), ownership())
                                                          .as(ExplicitResource::new);
        return Combinators.combine(specs(1, 6),
                                   option.list().ofMinSize(1).ofMaxSize(6)
                                         .withSizeDistribution(RandomDistribution.uniform()),
                                   explicit.list().ofMaxSize(4).withSizeDistribution(RandomDistribution.uniform()))
                          .as(ResolverScenario::new);
    }

    @Provide
    Arbitrary<RollbackScenario> rollbackScenarios() {
        // No re-entry: construction has no client scope to re-enter yet.
        return specs(1, 8).map(ResourceOwnershipPropertyTest::withoutReentry).flatMap(
            steps -> Combinators.combine(ownership().list().ofSize(steps.size()), uniform(0, 9), percent(50))
                                .as((o, failAt, tied) -> new RollbackScenario(steps, o, failAt, tied)));
    }

    private static Arbitrary<List<Spec>> specs(int min, int max) {
        Arbitrary<Failure> failure = Arbitraries.frequency(Tuple.of(40, Failure.NONE), Tuple.of(15, Failure.RUNTIME),
                                                           Tuple.of(15, Failure.ERROR), Tuple.of(15, Failure.CHECKED),
                                                           Tuple.of(15, Failure.INTERRUPTED));
        return Combinators.combine(percent(80), failure, percent(25), uniform(0, 2),
                                   percent(30))
                          .as(Spec::new)
                          .list().ofMinSize(min).ofMaxSize(max)
                          // Uniform: the default size bias makes most tries one or two resources.
                          .withSizeDistribution(RandomDistribution.uniform());
    }

    private static List<Spec> withoutFailures(List<Spec> specs) {
        return specs.stream()
                    .map(s -> new Spec(s.closeable(), Failure.NONE, s.reentrant(), s.equalsGroup(), s.slow()))
                    .toList();
    }

    private static List<Spec> withoutReentry(List<Spec> specs) {
        return specs.stream()
                    .map(s -> new Spec(s.closeable(), s.failure(), false, s.equalsGroup(), s.slow()))
                    .toList();
    }

    private static Arbitrary<ResourceOwnership> ownership() {
        return Arbitraries.of(ResourceOwnership.class);
    }

    private static Arbitrary<Boolean> percent(int percent) {
        return uniform(0, 99).map(p -> p < percent);
    }

    // jqwik biases integers towards the shrinking target by default, which skews percentages and indices.
    private static Arbitrary<Integer> uniform(int min, int max) {
        return Arbitraries.integers().between(min, max).withDistribution(RandomDistribution.uniform());
    }

    private static List<Tracked> instantiate(List<Spec> specs, Run run) {
        List<Tracked> instances = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            Spec spec = specs.get(i);
            instances.add(spec.closeable()
                          ? new CloseableResource(i, spec.equalsGroup(), failure(spec.failure(), i), spec.reentrant(),
                                                  spec.slow(), run)
                          : new Plain(i, spec.equalsGroup()));
        }
        return instances;
    }

    private static Throwable failure(Failure failure, int index) {
        String message = "close failure " + index;
        return switch (failure) {
            case NONE -> null;
            case RUNTIME -> new IllegalStateException(message);
            case ERROR -> new CloseError(message);
            case CHECKED -> index % 2 == 0 ? new IOException(message) : new Exception(message);
            case INTERRUPTED -> new InterruptedException(message);
        };
    }

    // Scenario values.

    enum Failure { NONE, RUNTIME, ERROR, CHECKED, INTERRUPTED }

    /**
     * One distinct resource: whether it is {@link AutoCloseable}, how its close fails, whether its close re-enters the
     * scope's {@code close()}, its {@code equals} group, and whether its close is slow enough for other closers to
     * arrive while it runs.
     */
    record Spec(boolean closeable, Failure failure, boolean reentrant, int equalsGroup, boolean slow) {
    }

    /** An extra registration of resource {@code resource % n}, inserted at {@code position % (size + 1)}. */
    record Alias(int resource, ResourceOwnership ownership, int position) {
    }

    record Registration(int resource, ResourceOwnership ownership) {
    }

    /**
     * One try of the scope view: the resources, the ownership of each one's primary registration (in index order),
     * the aliases inserted among them, the number of concurrent closers (0, 2 or 3) and the sequential closes after.
     */
    record Scenario(List<Spec> specs, List<ResourceOwnership> primary, List<Alias> aliases, int closers,
                    int sequentialCloses) {
        List<Registration> registrations() {
            List<Registration> registrations = new ArrayList<>();
            for (int i = 0; i < specs.size(); i++) {
                registrations.add(new Registration(i, primary.get(i)));
            }
            for (Alias alias : aliases) {
                registrations.add(alias.position() % (registrations.size() + 1),
                                  new Registration(alias.resource() % specs.size(), alias.ownership()));
            }
            return registrations;
        }
    }

    /** How a resource reaches the resolved configuration, with the provenance and ownership it must get. */
    enum Supply {
        BUILDER(ConfigurationSource.BUILDER, CALLER_OWNED),
        BUILDER_SDK_OWNED(ConfigurationSource.BUILDER, SDK_OWNED),
        JVM_PROPERTY(ConfigurationSource.JVM_PROPERTY, SDK_OWNED),
        ENVIRONMENT(ConfigurationSource.ENVIRONMENT, SDK_OWNED),
        PROFILE(ConfigurationSource.PROFILE, SDK_OWNED),
        DEFAULT(ConfigurationSource.DEFAULT, SDK_OWNED);

        private final ConfigurationSource source;
        private final ResourceOwnership ownership;

        Supply(ConfigurationSource source, ResourceOwnership ownership) {
            this.source = source;
            this.ownership = ownership;
        }

        ConfigurationSource source() {
            return source;
        }

        ResourceOwnership ownership() {
            return ownership;
        }

        /** Whether the value comes through a setting rather than the builder. */
        boolean external() {
            return source != ConfigurationSource.BUILDER;
        }
    }

    /** One option whose value is pool instance {@code instance % n}; {@code setting} also registers a setting. */
    record OptionSupply(int instance, Supply supply, boolean setting) {
    }

    record ExplicitResource(int instance, ResourceOwnership ownership) {
    }

    record ResolverScenario(List<Spec> pool, List<OptionSupply> options, List<ExplicitResource> explicit) {
    }

    /** Construction steps, who owns each step's resource, the failing step ({@code % (n + 1)}) and its cause. */
    record RollbackScenario(List<Spec> steps, List<ResourceOwnership> ownership, int failAt, boolean tiedPriority) {
    }

    /** The oracle of the scope view. Resources are identified by registration index: one index per instance. */
    record Expected(List<Integer> order, Map<Integer, ResourceOwnership> ownership, List<Integer> attempts,
                    List<Throwable> failures, boolean interrupted) {
        static Expected of(List<Registration> registrations, List<Tracked> instances) {
            Map<Integer, ResourceOwnership> ownership = new LinkedHashMap<>();
            registrations.forEach(r -> ownership.merge(r.resource(), r.ownership(),
                                                       ResourceOwnershipPropertyTest::callerWins));
            List<Integer> order = List.copyOf(ownership.keySet());
            List<Integer> attempts = new ArrayList<>();
            for (int i = order.size() - 1; i >= 0; i--) {
                int index = order.get(i);
                if (ownership.get(index) == SDK_OWNED && instances.get(index) instanceof AutoCloseable) {
                    attempts.add(index);
                }
            }
            List<Throwable> failures = new ArrayList<>();
            attempts.forEach(i -> Optional.ofNullable(instances.get(i).failure()).ifPresent(failures::add));
            boolean interrupted = failures.stream().anyMatch(InterruptedException.class::isInstance);
            return new Expected(order, ownership, attempts, failures, interrupted);
        }
    }

    // Runtime values.

    record Attempt(int resource, Thread thread) {
    }

    record Reentry(Thread thread, boolean closedSeen, Throwable thrown) {
    }

    /** One close call: its thread, attempts made when it started and returned, what it threw, the interrupt flag. */
    record Outcome(Thread thread, int attemptsAtEntry, Throwable thrown, boolean interrupted, int attemptsAtReturn) {
    }

    /** What one try's resources observed. */
    static final class Run {
        final Queue<Attempt> attempts = new ConcurrentLinkedQueue<>();
        final Queue<Reentry> reentries = new ConcurrentLinkedQueue<>();
        volatile ResourceScope scope;

        void reenter() {
            boolean closedSeen = scope.isClosed();
            Throwable thrown = null;
            try {
                scope.close();
            } catch (Throwable t) {
                thrown = t;
            }
            reentries.add(new Reentry(Thread.currentThread(), closedSeen, thrown));
        }
    }

    /** A resource equal to every other resource of its class and group: equal instances are not the same one. */
    abstract static class Tracked {
        final int index;
        final int group;

        Tracked(int index, int group) {
            this.index = index;
            this.group = group;
        }

        abstract Tracked twin();

        Throwable failure() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && o.getClass() == getClass() && ((Tracked) o).group == group;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass().getName(), group);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "#" + index;
        }
    }

    /** A resource that is not {@link AutoCloseable}: nothing can close it. */
    static final class Plain extends Tracked {
        Plain(int index, int group) {
            super(index, group);
        }

        @Override
        Tracked twin() {
            return new Plain(-1, group);
        }
    }

    /** A closeable resource that records each close attempt, optionally re-enters the scope, and optionally fails. */
    static final class CloseableResource extends Tracked implements AutoCloseable {
        final AtomicInteger closes = new AtomicInteger();
        private final Throwable failure;
        private final boolean reentrant;
        private final boolean slow;
        private final Run run;

        CloseableResource(int index, int group, Throwable failure, boolean reentrant, boolean slow, Run run) {
            super(index, group);
            this.failure = failure;
            this.reentrant = reentrant;
            this.slow = slow;
            this.run = run;
        }

        @Override
        Tracked twin() {
            return new CloseableResource(-1, group, null, false, false, run);
        }

        @Override
        Throwable failure() {
            return failure;
        }

        @Override
        public void close() throws Exception {
            closes.incrementAndGet();
            run.attempts.add(new Attempt(index, Thread.currentThread()));
            if (slow) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            if (reentrant) {
                run.reenter();
            }
            if (failure instanceof Exception exception) {
                throw exception;
            }
            if (failure instanceof Error error) {
                throw error;
            }
        }
    }

    /** An {@link Error} thrown by a close; distinct from the test's own assertion errors. */
    static final class CloseError extends Error {
        private static final long serialVersionUID = 1L;

        CloseError(String message) {
            super(message);
        }
    }
}
