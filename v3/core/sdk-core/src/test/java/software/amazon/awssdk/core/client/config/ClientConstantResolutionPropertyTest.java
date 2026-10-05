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

package software.amazon.awssdk.core.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
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
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.override.CallOption;
import software.amazon.awssdk.core.override.CallOptions;

/**
 * Property 26: Client-constant resolution is once per client.
 *
 * <p>For any positive number of calls through one client, every client-constant derived value is resolved no more than
 * once for that client (and every per-operation constant no more than once for that client/operation).
 *
 * <p><b>What is exercised for real.</b> Each scenario builds 1&ndash;4 clients with {@link ClientConfigurationResolver}
 * over a shared pool of {@link ClientSetting} definitions (so the same parser and default functions serve several
 * clients). Each client uses 1&ndash;5 of them, and each setting's value comes from the builder, a JVM property, an
 * environment variable, a profile property, a fixed default or a derived default, with lower-precedence sources also
 * present or declared-but-absent. Every {@link SettingSource} lookup, parser call and default call is counted per
 * <em>building client</em> and per source name, parser or default; attribution is by a thread-local set only while that
 * client builds, so anything invoked outside a build (by a call, on any thread) lands in an {@code outside-build}
 * bucket. Calls go through one {@link CallOptions#of} per client and a per-call {@link CallOptions#withOverrides} with
 * generated request-embedded and explicit replacing overrides; they are interleaved with other clients' builds and
 * calls, then repeated concurrently from 2&ndash;4 threads started together at a barrier (all waits bounded).
 *
 * <p><b>Oracle.</b> An expected-invocation map derived independently from the precedence rules
 * ({@link #expectedResolution}): each client build must read each needed source name of a setting (declared names in
 * precedence order down to the first with a value, none under a builder value) exactly once, may read an unneeded
 * declared name at most once, parses exactly once when an external source wins, and calls the default exactly once
 * when no builder or external value exists; nothing else may be invoked, so no name, parser or default runs twice in a
 * build and the {@code outside-build} bucket stays empty. The counter map is frozen after each build and must be
 * unchanged before every later call, after the sequential phase and after the concurrent phase, so neither a client's
 * calls nor other clients' builds add to its counts. Every build also yields the expected value and provenance, and
 * every call sees that value or its override.
 *
 * <p><b>Seam, not the real plan factory.</b> Requirement 10.11 is about per-operation stage graphs, interceptor plans,
 * auth/endpoint metadata and user-agent fragments. No V3 code creates those yet: {@code v3/core/aws-pipeline} holds only
 * a package-info. {@link ModeledClient} therefore stands in for the call engine with a test-owned, instrumented
 * per-operation plan factory memoized per client ({@link ModeledClient#plans}); its counter oracle (exactly one creation
 * per client/operation that received a call, including operations first called concurrently) checks the modeled
 * contract, not production code. Real plan-factory coverage belongs to task 9.1 ({@code ClientRuntime} /
 * {@code PipelinePlan}, cached stage metadata) and task 9.6 (instrumented proof of no per-call plan construction), with
 * task 9.2 ({@code AwsCallEngine}) and the codegen client tasks as the callers; they should replace this seam.
 *
 * <p><b>Validates: Requirements 9.5, 10.11</b>
 */
class ClientConstantResolutionPropertyTest {
    private static final int DEFINITIONS = 6;
    private static final int MAX_OPERATIONS = 3;
    private static final long TIMEOUT_SECONDS = 30;
    private static final String OUTSIDE_BUILD = "outside-build";
    private static final int REQUIRED = 1;
    private static final int PERMITTED = 0;
    private static final List<ConfigurationSource> EXTERNAL = List.of(ConfigurationSource.JVM_PROPERTY,
                                                                      ConfigurationSource.ENVIRONMENT,
                                                                      ConfigurationSource.PROFILE);

    // Feature: aws-sdk-java-v3, Property 26: Client-constant resolution is once per client
    @Property(tries = 1_000)
    @Label("Property 26: Client-constant resolution is once per client")
    void clientConstantsResolveOncePerClient(@ForAll("scenarios") Scenario scenario) throws Exception {
        Ledger ledger = new Ledger();
        Pool pool = new Pool(scenario.definitions(), ledger);
        int clientCount = scenario.clients().size();
        ModeledClient[] clients = new ModeledClient[clientCount];
        Map<String, Integer> expectedResolution = new TreeMap<>();
        Map<String, Integer> expectedPlans = new TreeMap<>();
        Map<String, Integer> afterBuilds = Map.of();
        int[] callSequence = {0};

        // Sequential phase: a shuffled interleaving where a client's first token builds it and later tokens call it.
        List<Integer> tokens = new ArrayList<>();
        List<List<Integer>> pendingCalls = new ArrayList<>();
        for (int c = 0; c < clientCount; c++) {
            List<Integer> calls = new ArrayList<>();
            List<Integer> perOperation = scenario.clients().get(c).callsPerOperation();
            for (int op = 0; op < perOperation.size(); op++) {
                calls.addAll(Collections.nCopies(perOperation.get(op), op));
            }
            Collections.shuffle(calls, new Random(scenario.seed() + c));
            pendingCalls.add(calls);
            tokens.addAll(Collections.nCopies(calls.size() + 1, c));
        }
        Collections.shuffle(tokens, new Random(scenario.seed()));
        for (int c : tokens) {
            if (clients[c] == null) {
                clients[c] = build(c, scenario, pool, ledger);
                expectedResolution.putAll(expectedResolution(c, scenario));
                assertResolution(expectedResolution, ledger.resolutionCounts(), "after building client " + c);
                afterBuilds = ledger.resolutionCounts();
            } else {
                assertEquals(afterBuilds, ledger.resolutionCounts(), "before a call to client " + c);
                int op = pendingCalls.get(c).remove(0);
                call(clients[c], op, callSequence[0]++, scenario, pool);
                expectedPlans.put(Ledger.planKey(c, op), 1);
            }
        }
        assertEquals(afterBuilds, ledger.resolutionCounts(), "after the sequential calls");
        assertEquals(expectedPlans, ledger.planCounts(), "plans after the sequential calls");

        // Concurrent phase: every thread first calls an operation no one called before, then generated calls.
        int threads = scenario.threads();
        List<List<int[]>> threadCalls = new ArrayList<>();
        Random random = new Random(~scenario.seed());
        for (int t = 0; t < threads; t++) {
            List<int[]> calls = new ArrayList<>();
            for (int c = 0; c < clientCount; c++) {
                calls.add(new int[] {c, MAX_OPERATIONS});
            }
            for (int i = 0; i < scenario.callsPerThread(); i++) {
                int c = random.nextInt(clientCount);
                calls.add(new int[] {c, random.nextInt(MAX_OPERATIONS + 1)});
            }
            Collections.shuffle(calls, random);
            for (int[] call : calls) {
                expectedPlans.put(Ledger.planKey(call[0], call[1]), 1);
            }
            threadCalls.add(calls);
        }
        int firstConcurrentCall = callSequence[0];
        CyclicBarrier start = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Void>> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                List<int[]> calls = threadCalls.get(t);
                int base = firstConcurrentCall + t * 1_000;
                workers.add(() -> {
                    start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    for (int i = 0; i < calls.size(); i++) {
                        call(clients[calls.get(i)[0]], calls.get(i)[1], base + i, scenario, pool);
                    }
                    return null;
                });
            }
            for (Future<Void> result : executor.invokeAll(workers, TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
        assertEquals(afterBuilds, ledger.resolutionCounts(), "after the concurrent calls");
        assertEquals(expectedPlans, ledger.planCounts(), "plans after the concurrent calls");
    }

    private static ModeledClient build(int c, Scenario scenario, Pool pool, Ledger ledger) {
        List<Usage> usages = scenario.clients().get(c).usages();
        Map<String, String> jvm = new HashMap<>();
        Map<String, String> env = new HashMap<>();
        Map<String, String> profile = new HashMap<>();
        SdkClientConfiguration.Builder builderValues = SdkClientConfiguration.builder();
        ClientConfigurationResolver.Builder resolver = ClientConfigurationResolver.builder();
        for (Usage usage : usages) {
            Definition definition = scenario.definitions().get(usage.definition());
            resolver.addSetting(pool.settings.get(usage.definition()));
            if (usage.builder()) {
                builderValues.option(pool.options.get(usage.definition()), builderValue(c, usage.definition()));
            }
            if (definition.jvm() && usage.jvm()) {
                jvm.put(name(ConfigurationSource.JVM_PROPERTY, usage.definition()), text(ConfigurationSource.JVM_PROPERTY,
                                                                                        c, usage.definition()));
            }
            if (definition.env() && usage.env()) {
                env.put(name(ConfigurationSource.ENVIRONMENT, usage.definition()), text(ConfigurationSource.ENVIRONMENT,
                                                                                       c, usage.definition()));
            }
            if (definition.profile() && usage.profile()) {
                profile.put(name(ConfigurationSource.PROFILE, usage.definition()), text(ConfigurationSource.PROFILE,
                                                                                       c, usage.definition()));
            }
            Statistics.label("winning source").collect(expectedValue(c, usages, usages.indexOf(usage), scenario, pool)
                                                           .map(v -> v.source().toString() + (
                                                               v.value().startsWith("derived") ? " (derived)" : ""))
                                                           .orElse("none"));
        }
        ledger.building.set(c);
        try {
            SdkClientConfiguration config =
                resolver.sources(ConfigurationSources.builder()
                                                     .jvmProperties(ledger.counting(ConfigurationSource.JVM_PROPERTY,
                                                                                    jvm))
                                                     .environment(ledger.counting(ConfigurationSource.ENVIRONMENT, env))
                                                     .profile(ledger.counting(ConfigurationSource.PROFILE, profile))
                                                     .build())
                        .build()
                        .resolve(builderValues.build());
            ModeledClient client = new ModeledClient(c, config, ledger);
            for (int i = 0; i < usages.size(); i++) {
                ClientOption<String> option = pool.options.get(usages.get(i).definition());
                assertEquals(expectedValue(c, usages, i, scenario, pool), config.resolved(option), "client " + c);
            }
            return client;
        } finally {
            ledger.building.remove();
        }
    }

    /** One call: per-call overrides only merge values; the per-operation plan comes from the client's memo. */
    private static void call(ModeledClient client, int op, int callId, Scenario scenario, Pool pool) {
        List<Usage> usages = scenario.clients().get(client.index).usages();
        ClientOption<String> embeddedOption = pool.options.get(usages.get(callId % usages.size()).definition());
        ClientOption<String> explicitOption = pool.options.get(usages.get((callId / 2) % usages.size()).definition());
        RequestOverrideConfiguration embedded = null;
        RequestOverrideConfiguration explicit = null;
        if (callId % 3 != 0) {
            embedded = SdkRequestOverrideConfiguration.builder()
                                                      .putCallOption(pool.callOptions.get(embeddedOption),
                                                                     "embedded-" + callId)
                                                      .build();
        }
        if (callId % 3 == 2) {
            explicit = SdkRequestOverrideConfiguration.builder()
                                                      .putCallOption(pool.callOptions.get(explicitOption),
                                                                     "explicit-" + callId)
                                                      .build();
        }
        ModeledClient.Call result = client.call(op, embedded, explicit);
        assertSame(result.plan(), client.plans.get(op));
        assertSame(client.config, result.options().clientConfiguration());
        for (Usage usage : usages) {
            ClientOption<String> option = pool.options.get(usage.definition());
            Optional<String> expected = client.config.get(option);
            if (explicit != null && option == explicitOption) {
                expected = Optional.of("explicit-" + callId);
            } else if (embedded != null && option == embeddedOption) {
                expected = Optional.of("embedded-" + callId);
            }
            assertEquals(expected, result.options().get(option), "call " + callId + " option " + option.name());
            assertEquals(client.config.get(option), Optional.ofNullable(result.plan().constant(option)));
        }
    }

    /** The resolved value for the {@code i}th setting of client {@code c}, from the precedence rules. */
    private static Optional<ResolvedOption<String>> expectedValue(int c, List<Usage> usages, int i, Scenario scenario,
                                                                Pool pool) {
        Usage usage = usages.get(i);
        Definition definition = scenario.definitions().get(usage.definition());
        ClientOption<String> option = pool.options.get(usage.definition());
        if (usage.builder()) {
            return Optional.of(ResolvedOption.of(option, builderValue(c, usage.definition()), ConfigurationSource.BUILDER));
        }
        for (ConfigurationSource source : EXTERNAL) {
            if (definition.declares(source) && usage.present(source)) {
                return Optional.of(ResolvedOption.of(option, parsed(text(source, c, usage.definition())), source));
            }
        }
        switch (definition.defaultKind()) {
            case FIXED:
                return Optional.of(ResolvedOption.of(option, "default-d" + usage.definition(),
                                                     ConfigurationSource.DEFAULT));
            case DERIVED:
                int earlier = 0;
                for (int j = 0; j < i; j++) {
                    earlier += expectedValue(c, usages, j, scenario, pool).isPresent() ? 1 : 0;
                }
                return Optional.of(ResolvedOption.of(option, derived(usage.definition(), earlier),
                                                     ConfigurationSource.DEFAULT));
            default:
                return Optional.empty();
        }
    }

    /**
     * The invocations client {@code c}'s build may make: {@link #REQUIRED} ones exactly once, {@link #PERMITTED} ones
     * at most once. A setting's needed source reads (each declared name down to the first that has a value, when the
     * builder does not set it) are required, and so are its parse exactly when an external source wins and its default
     * exactly when nothing else does. Reading a declared name the precedence rules make unnecessary (below the winner,
     * or under a builder value) is permitted once; parsing or defaulting a value that loses is not permitted at all.
     */
    private static Map<String, Integer> expectedResolution(int c, Scenario scenario) {
        Map<String, Integer> expected = new TreeMap<>();
        for (Usage usage : scenario.clients().get(c).usages()) {
            Definition definition = scenario.definitions().get(usage.definition());
            boolean found = usage.builder();
            for (ConfigurationSource source : EXTERNAL) {
                if (definition.declares(source)) {
                    expected.put(Ledger.key(c, "read " + source + " " + name(source, usage.definition())),
                                 found ? PERMITTED : REQUIRED);
                    if (!found && usage.present(source)) {
                        expected.put(Ledger.key(c, "parse d" + usage.definition()), REQUIRED);
                        found = true;
                    }
                }
            }
            if (!found && definition.defaultKind() != DefaultKind.NONE) {
                expected.put(Ledger.key(c, "default d" + usage.definition()), REQUIRED);
            }
        }
        return expected;
    }

    /** Every invocation is expected and at most once; every required invocation happened. */
    private static void assertResolution(Map<String, Integer> expected, Map<String, Integer> actual, String when) {
        actual.forEach((key, count) -> {
            assertTrue(expected.containsKey(key), when + ": unexpected invocation " + key + " in " + actual);
            assertTrue(count <= 1, when + ": " + key + " invoked " + count + " times");
        });
        expected.forEach((key, kind) -> {
            if (kind == REQUIRED) {
                assertEquals(1, actual.getOrDefault(key, 0), when + ": " + key + " in " + actual);
            }
        });
    }

    private static String name(ConfigurationSource source, int definition) {
        return switch (source) {
            case JVM_PROPERTY -> "p26.d" + definition;
            case ENVIRONMENT -> "P26_D" + definition;
            case PROFILE -> "p26_d" + definition;
            default -> throw new IllegalArgumentException(source.toString());
        };
    }

    private static String text(ConfigurationSource source, int client, int definition) {
        return source + "-c" + client + "-d" + definition;
    }

    private static String parsed(String text) {
        return "parsed(" + text + ")";
    }

    private static String derived(int definition, int earlierResolved) {
        return "derived-d" + definition + "-" + earlierResolved;
    }

    private static String builderValue(int client, int definition) {
        return "builder-c" + client + "-d" + definition;
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Definition> definition =
            Combinators.combine(Arbitraries.of(true, false), Arbitraries.of(true, false), Arbitraries.of(true, false),
                                Arbitraries.of(DefaultKind.class))
                       .as(Definition::new);
        Arbitrary<Usage> usage =
            Combinators.combine(Arbitraries.integers().between(0, DEFINITIONS - 1),
                                Arbitraries.integers().between(0, 4).map(i -> i == 0),
                                Arbitraries.of(true, false), Arbitraries.of(true, false), Arbitraries.of(true, false))
                       .as(Usage::new);
        Arbitrary<ClientSpec> client =
            Combinators.combine(usage.list().ofMinSize(1).ofMaxSize(5).uniqueElements(Usage::definition),
                                Arbitraries.integers().between(1, 6).list().ofMinSize(1).ofMaxSize(MAX_OPERATIONS))
                       .as(ClientSpec::new);
        return Combinators.combine(definition.list().ofSize(DEFINITIONS),
                                   client.list().ofMinSize(1).ofMaxSize(4),
                                   Arbitraries.longs(),
                                   Arbitraries.integers().between(2, 4),
                                   Arbitraries.integers().between(1, 8))
                          .as(Scenario::new);
    }

    enum DefaultKind { NONE, FIXED, DERIVED }

    /** A setting shared by every client that uses it: which external sources it names, and its default. */
    record Definition(boolean jvm, boolean env, boolean profile, DefaultKind defaultKind) {
        boolean declares(ConfigurationSource source) {
            return switch (source) {
                case JVM_PROPERTY -> jvm;
                case ENVIRONMENT -> env;
                case PROFILE -> profile;
                default -> false;
            };
        }
    }

    /** One client's use of a definition: whether the builder sets it and which named sources hold a value. */
    record Usage(int definition, boolean builder, boolean jvm, boolean env, boolean profile) {
        boolean present(ConfigurationSource source) {
            return switch (source) {
                case JVM_PROPERTY -> jvm;
                case ENVIRONMENT -> env;
                case PROFILE -> profile;
                default -> false;
            };
        }
    }

    /** A client's settings, in resolution order, and the positive number of calls to each of its operations. */
    record ClientSpec(List<Usage> usages, List<Integer> callsPerOperation) {
    }

    record Scenario(List<Definition> definitions, List<ClientSpec> clients, long seed, int threads,
                    int callsPerThread) {
    }

    /** Invocation counters keyed by building client (or {@value #OUTSIDE_BUILD}) and event. */
    static final class Ledger {
        private final ThreadLocal<Integer> building = new ThreadLocal<>();
        private final Map<String, AtomicInteger> resolution = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> plans = new ConcurrentHashMap<>();

        static String key(int client, String event) {
            return "client " + client + ": " + event;
        }

        static String planKey(int client, int operation) {
            return "client " + client + ": plan op" + operation;
        }

        void record(String event) {
            Integer client = building.get();
            String key = client == null ? OUTSIDE_BUILD + ": " + event : key(client, event);
            resolution.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        }

        void recordPlan(int client, int operation) {
            plans.computeIfAbsent(planKey(client, operation), k -> new AtomicInteger()).incrementAndGet();
        }

        SettingSource counting(ConfigurationSource kind, Map<String, String> values) {
            Map<String, String> copy = Map.copyOf(values);
            return name -> {
                record("read " + kind + " " + name);
                return Optional.ofNullable(copy.get(name));
            };
        }

        Map<String, Integer> resolutionCounts() {
            return snapshot(resolution);
        }

        Map<String, Integer> planCounts() {
            return snapshot(plans);
        }

        private static Map<String, Integer> snapshot(Map<String, AtomicInteger> counters) {
            Map<String, Integer> result = new TreeMap<>();
            counters.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }
    }

    /** The option, call option and instrumented setting of every definition, shared by all clients of a scenario. */
    static final class Pool {
        private final List<ClientOption<String>> options = new ArrayList<>();
        private final Map<ClientOption<String>, CallOption<String>> callOptions = new HashMap<>();
        private final List<ClientSetting<String>> settings = new ArrayList<>();

        Pool(List<Definition> definitions, Ledger ledger) {
            for (int d = 0; d < definitions.size(); d++) {
                int id = d;
                Definition definition = definitions.get(d);
                ClientOption<String> option = ClientOption.create("P26Setting" + d, String.class);
                options.add(option);
                callOptions.put(option, CallOption.replacing(option));
                ClientSetting.Builder<String> setting = ClientSetting.builder(option);
                if (definition.jvm()) {
                    setting.jvmProperty(name(ConfigurationSource.JVM_PROPERTY, d));
                }
                if (definition.env()) {
                    setting.environmentVariable(name(ConfigurationSource.ENVIRONMENT, d));
                }
                if (definition.profile()) {
                    setting.profileProperty(name(ConfigurationSource.PROFILE, d));
                }
                setting.parser(text -> {
                    ledger.record("parse d" + id);
                    return parsed(text);
                });
                if (definition.defaultKind() == DefaultKind.FIXED) {
                    setting.derivedDefault(resolved -> {
                        ledger.record("default d" + id);
                        return "default-d" + id;
                    });
                } else if (definition.defaultKind() == DefaultKind.DERIVED) {
                    setting.derivedDefault(resolved -> {
                        ledger.record("default d" + id);
                        return derived(id, resolved.options().size());
                    });
                }
                settings.add(setting.build());
            }
        }
    }

    /**
     * Test-owned model of a client (not production code): one resolved configuration and one {@link CallOptions#of}
     * per client, a per-call {@link CallOptions#withOverrides}, and an instrumented per-operation plan factory memoized
     * per client. It stands in for task 9.1's {@code ClientRuntime}/{@code PipelinePlan} until they exist.
     */
    static final class ModeledClient {
        private final int index;
        private final SdkClientConfiguration config;
        private final CallOptions clientOptions;
        private final Ledger ledger;
        private final Map<Integer, OperationPlan> plans = new ConcurrentHashMap<>();

        ModeledClient(int index, SdkClientConfiguration config, Ledger ledger) {
            this.index = index;
            this.config = config;
            this.clientOptions = CallOptions.of(config);
            this.ledger = ledger;
        }

        Call call(int operation, RequestOverrideConfiguration embedded, RequestOverrideConfiguration explicit) {
            OperationPlan plan = plans.computeIfAbsent(operation, this::newPlan);
            return new Call(plan, clientOptions.withOverrides(embedded, explicit));
        }

        private OperationPlan newPlan(int operation) {
            ledger.recordPlan(index, operation);
            Map<ClientOption<?>, Object> constants = new HashMap<>();
            for (ClientOption<?> option : config.options()) {
                constants.put(option, config.get(option).orElseThrow());
            }
            return new OperationPlan(operation, Map.copyOf(constants));
        }

        record Call(OperationPlan plan, CallOptions options) {
        }

        /** A modeled per-client, per-operation constant derived from the resolved client configuration. */
        record OperationPlan(int operation, Map<ClientOption<?>, Object> constants) {
            Object constant(ClientOption<?> option) {
                return constants.get(option);
            }
        }
    }
}
