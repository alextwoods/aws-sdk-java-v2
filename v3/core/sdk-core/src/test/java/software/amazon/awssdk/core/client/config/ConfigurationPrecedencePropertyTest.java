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
import static software.amazon.awssdk.core.client.config.ConfigurationSource.BUILDER;
import static software.amazon.awssdk.core.client.config.ConfigurationSource.DEFAULT;
import static software.amazon.awssdk.core.client.config.ConfigurationSource.ENVIRONMENT;
import static software.amazon.awssdk.core.client.config.ConfigurationSource.JVM_PROPERTY;
import static software.amazon.awssdk.core.client.config.ConfigurationSource.PROFILE;
import static software.amazon.awssdk.core.client.config.ConfigurationSource.REQUEST_OVERRIDE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.RandomDistribution;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.override.CallOption;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.core.resource.ResourceOwnership;

/**
 * Property 24: Configuration precedence is total.
 *
 * <p>For any subset of request override, builder, JVM property, environment, profile, and default values for one key,
 * resolution chooses the value from the highest-priority present source.
 *
 * <p>Each scenario supplies one option from a subset of the six sources: the five client-level sources through
 * {@link ClientConfigurationResolver#resolve} (builder values plus injected, read-counting {@link SettingSource}s and a
 * call-counting default), and the request override end to end through {@link CallOptions#withOverrides}. The selected
 * value and provenance at client level, at call level and from {@link ClientConfigurationResolver#select} are compared
 * with {@link #PRECEDENCE}, a fixed list written out here rather than derived from {@link ConfigurationSource}'s
 * declaration order. Every source's value is distinct, so choosing any other source changes the observed value. Values
 * vary in type (strings, integers, durations) and parser; a present but empty string is still present.
 *
 * <p>The property generates subsets with jqwik; {@link #everySubsetOfTheSixSources()} additionally enumerates all
 * 2<sup>6</sup> subsets (including the empty one) for every kind and every empty-string position, so each subset is
 * covered on every run.
 *
 * <p><b>Validates: Requirements 9.3</b>
 */
class ConfigurationPrecedencePropertyTest {
    /** Requirement 9.3's precedence, highest first. Bit {@code i} of a subset mask selects {@code PRECEDENCE.get(i)}. */
    private static final List<ConfigurationSource> PRECEDENCE =
        List.of(REQUEST_OVERRIDE, BUILDER, JVM_PROPERTY, ENVIRONMENT, PROFILE, DEFAULT);
    private static final int SUBSETS = 1 << PRECEDENCE.size();
    private static final Set<ConfigurationSource> EXTERNAL = EnumSet.of(JVM_PROPERTY, ENVIRONMENT, PROFILE);
    /** Values the caller passes are caller-owned; values the SDK creates from text or defaults are SDK-owned. */
    private static final Map<ConfigurationSource, ResourceOwnership> OWNERSHIP =
        Map.of(REQUEST_OVERRIDE, ResourceOwnership.CALLER_OWNED,
               BUILDER, ResourceOwnership.CALLER_OWNED,
               JVM_PROPERTY, ResourceOwnership.SDK_OWNED,
               ENVIRONMENT, ResourceOwnership.SDK_OWNED,
               PROFILE, ResourceOwnership.SDK_OWNED,
               DEFAULT, ResourceOwnership.SDK_OWNED);
    /** Text every source also holds under an unrelated name; parsing it fails, and reading it fails the count check. */
    private static final String NOISE_SUFFIX = ".unrelated";

    /**
     * One option value type with its setting parser. {@code value} yields distinct values for distinct inputs;
     * {@code render} is the text an external source holds for a value; {@code parsed} states independently of
     * {@code parser} what parsing that text yields; {@code empty} is the present-but-empty value, if the type has one.
     */
    record Kind<T>(String name, ClientOption<T> option, CallOption<T> callOption, Function<String, T> parser,
                   IntFunction<T> value, Function<T, String> render, Function<T, T> parsed, T empty) {
        static <T> Kind<T> of(String name, Class<T> type, Function<String, T> parser, IntFunction<T> value,
                              Function<T, String> render, Function<T, T> parsed, T empty) {
            ClientOption<T> option = ClientOption.create("P24" + name, type);
            return new Kind<>(name, option, CallOption.replacing(option), parser, value, render, parsed, empty);
        }

        String jvmProperty() {
            return "test.p24." + name;
        }

        String environmentVariable() {
            return "TEST_P24_" + name.toUpperCase();
        }

        String profileProperty() {
            return "test_p24_" + name;
        }

        String nameIn(ConfigurationSource source) {
            return switch (source) {
                case JVM_PROPERTY -> jvmProperty();
                case ENVIRONMENT -> environmentVariable();
                case PROFILE -> profileProperty();
                default -> throw new IllegalArgumentException(source.toString());
            };
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<Kind<?>> KINDS = List.of(
        Kind.of("string", String.class, text -> text, n -> "v" + n, v -> v, v -> v, ""),
        Kind.of("tagged", String.class, text -> "parsed:" + text, n -> "v" + n, v -> v, v -> "parsed:" + v, ""),
        Kind.of("integer", Integer.class, Integer::valueOf, n -> n, String::valueOf, v -> v, null),
        Kind.of("duration", Duration.class, Duration::parse, Duration::ofMillis, Duration::toString, v -> v, null));

    /**
     * One try: {@code mask} selects the present sources; {@code emptySource}, if present, supplies the empty value;
     * {@code constantDefault} uses {@code defaultValue(T)} instead of a counted derived default; {@code emptyOverride}
     * passes an empty request override instead of {@code null} when no request override is present.
     */
    record Scenario(Kind<?> kind, int mask, int seed, ConfigurationSource emptySource, boolean constantDefault,
                    boolean emptyOverride, long shuffleSeed) {
        Set<ConfigurationSource> present() {
            Set<ConfigurationSource> present = EnumSet.noneOf(ConfigurationSource.class);
            for (int i = 0; i < PRECEDENCE.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    present.add(PRECEDENCE.get(i));
                }
            }
            return present;
        }
    }

    // Feature: aws-sdk-java-v3, Property 24: Configuration precedence is total
    // Embedded edge cases are disabled: mixed in, they made about a fifth of the tries the empty subset. The boundary
    // subsets (empty, full, single sources) are covered by everySubsetOfTheSixSources on every run.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 24: Configuration precedence is total")
    void configurationPrecedenceIsTotal(@ForAll("scenarios") Scenario scenario) {
        Statistics.label("kind").collect(scenario.kind().name());
        Statistics.label("winner").collect(winner(scenario.present()).map(Enum::name).orElse("none"));
        Statistics.label("empty value").collect(scenario.emptySource() != null);
        check(scenario);
    }

    /** Every subset of the six sources, for every kind and every empty-string position, deterministically. */
    @Example
    void everySubsetOfTheSixSources() {
        int scenarios = 0;
        Set<String> covered = new HashSet<>();
        for (Kind<?> kind : KINDS) {
            for (int mask = 0; mask < SUBSETS; mask++) {
                Scenario plain = new Scenario(kind, mask, mask + 1, null, mask % 2 == 0, mask % 3 == 0, mask);
                List<ConfigurationSource> emptyPositions = new ArrayList<>();
                emptyPositions.add(null);
                if (kind.empty() != null) {
                    emptyPositions.addAll(plain.present());
                }
                for (ConfigurationSource empty : emptyPositions) {
                    check(new Scenario(kind, mask, mask + 1, empty, mask % 2 == 0, mask % 3 == 0, mask));
                    covered.add(kind.name() + "/" + mask);
                    scenarios++;
                }
            }
        }
        assertEquals(KINDS.size() * SUBSETS, covered.size());
        // 64 subsets have 192 present sources in total: each string kind adds one scenario per present source.
        assertEquals(KINDS.size() * SUBSETS + 2 * (SUBSETS / 2) * PRECEDENCE.size(), scenarios);
    }

    /** The oracle names every source exactly once, and the enum's pairwise ranking agrees with it. */
    @Example
    void oracleCoversEverySourceAndAgreesWithTheRanking() {
        assertEquals(EnumSet.allOf(ConfigurationSource.class), EnumSet.copyOf(PRECEDENCE));
        assertEquals(PRECEDENCE.size(), new HashSet<>(PRECEDENCE).size());
        for (int i = 0; i < PRECEDENCE.size(); i++) {
            for (int j = 0; j < PRECEDENCE.size(); j++) {
                assertEquals(i < j, PRECEDENCE.get(i).outranks(PRECEDENCE.get(j)),
                             PRECEDENCE.get(i) + " vs " + PRECEDENCE.get(j));
            }
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Combinators.combine(Arbitraries.of(KINDS),
                                   // Uniform, so every subset is equally likely (the default biases the range).
                                   Arbitraries.integers().between(0, SUBSETS - 1)
                                              .withDistribution(RandomDistribution.uniform()),
                                   Arbitraries.integers().between(-100_000_000, 100_000_000),
                                   Arbitraries.integers().between(0, PRECEDENCE.size()),
                                   Arbitraries.of(true, false),
                                   Arbitraries.of(true, false),
                                   Arbitraries.longs())
                          .as((kind, mask, seed, emptyIndex, constantDefault, emptyOverride, shuffleSeed) -> {
                              ConfigurationSource empty = null;
                              if (kind.empty() != null && emptyIndex < PRECEDENCE.size()
                                  && (mask & (1 << emptyIndex)) != 0) {
                                  empty = PRECEDENCE.get(emptyIndex);
                              }
                              return new Scenario(kind, mask, seed, empty, constantDefault, emptyOverride,
                                                  shuffleSeed);
                          });
    }

    /** The oracle: the first source of {@link #PRECEDENCE} that is present. */
    private static Optional<ConfigurationSource> winner(Set<ConfigurationSource> present) {
        for (ConfigurationSource source : PRECEDENCE) {
            if (present.contains(source)) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }

    private static void check(Scenario scenario) {
        checkTyped(scenario.kind(), scenario);
    }

    private static <T> void checkTyped(Kind<T> kind, Scenario scenario) {
        Set<ConfigurationSource> present = scenario.present();
        ClientOption<T> option = kind.option();

        // What each present source supplies (typed values, or text for external sources) and the value it yields.
        Map<ConfigurationSource, T> supplied = new EnumMap<>(ConfigurationSource.class);
        Map<ConfigurationSource, T> expected = new EnumMap<>(ConfigurationSource.class);
        for (ConfigurationSource source : present) {
            T value = source == scenario.emptySource()
                      ? kind.empty()
                      : kind.value().apply(scenario.seed() * 8 + PRECEDENCE.indexOf(source));
            supplied.put(source, value);
            expected.put(source, EXTERNAL.contains(source) ? kind.parsed().apply(value) : value);
        }
        assertEquals(expected.size(), new HashSet<>(expected.values()).size(), "fixture values must be distinct");

        // Client level: builder value, instrumented external sources, counted parser and default.
        Map<ConfigurationSource, CountingSource> sources = new EnumMap<>(ConfigurationSource.class);
        for (ConfigurationSource source : EXTERNAL) {
            sources.put(source, new CountingSource(kind, source, present.contains(source)
                                                                 ? kind.render().apply(supplied.get(source))
                                                                 : null));
        }
        AtomicInteger parses = new AtomicInteger();
        AtomicInteger defaults = new AtomicInteger();
        ClientSetting.Builder<T> setting = ClientSetting.builder(option)
                                                        .jvmProperty(kind.jvmProperty())
                                                        .environmentVariable(kind.environmentVariable())
                                                        .profileProperty(kind.profileProperty())
                                                        .parser(text -> {
                                                            parses.incrementAndGet();
                                                            return kind.parser().apply(text);
                                                        });
        T defaultValue = supplied.get(DEFAULT);
        if (scenario.constantDefault()) {
            setting.defaultValue(defaultValue);
        } else {
            setting.derivedDefault(resolvedSoFar -> {
                defaults.incrementAndGet();
                return defaultValue;
            });
        }
        ClientConfigurationResolver resolver =
            ClientConfigurationResolver.builder()
                                       .sources(ConfigurationSources.builder()
                                                                    .jvmProperties(sources.get(JVM_PROPERTY))
                                                                    .environment(sources.get(ENVIRONMENT))
                                                                    .profile(sources.get(PROFILE))
                                                                    .build())
                                       .addSetting(setting.build())
                                       .build();
        SdkClientConfiguration.Builder builderValues = SdkClientConfiguration.builder();
        if (present.contains(BUILDER)) {
            builderValues.option(option, supplied.get(BUILDER));
        }
        SdkClientConfiguration config = resolver.resolve(builderValues.build());

        Set<ConfigurationSource> clientLevel = EnumSet.noneOf(ConfigurationSource.class);
        clientLevel.addAll(present);
        clientLevel.remove(REQUEST_OVERRIDE);
        Optional<ConfigurationSource> clientWinner = winner(clientLevel);
        Optional<ResolvedOption<T>> expectedClient = clientWinner.map(w -> expectedOption(option, expected, w));
        assertEquals(expectedClient, config.resolved(option), "client-level selection");
        assertEquals(expectedClient.map(ResolvedOption::value), config.get(option));
        assertEquals(expectedClient.isPresent(), config.contains(option));

        // Lower-precedence sources are not consulted once a higher client-level source supplies the value.
        for (ConfigurationSource source : EXTERNAL) {
            int expectedReads = noneRankedAbove(clientLevel, source) ? 1 : 0;
            CountingSource counting = sources.get(source);
            assertEquals(expectedReads, counting.reads(kind.nameIn(source)), source + " reads of the setting");
            assertEquals(expectedReads, counting.totalReads(), source + " reads of any name");
        }
        assertEquals(clientWinner.filter(EXTERNAL::contains).isPresent() ? 1 : 0, parses.get(), "parser calls");
        if (!scenario.constantDefault()) {
            assertEquals(noneRankedAbove(clientLevel, DEFAULT) ? 1 : 0, defaults.get(), "default calls");
        }

        // Call level: the request override layered over the resolved client configuration.
        RequestOverrideConfiguration override = null;
        if (present.contains(REQUEST_OVERRIDE)) {
            override = SdkRequestOverrideConfiguration.builder()
                                                      .putCallOption(kind.callOption(), supplied.get(REQUEST_OVERRIDE))
                                                      .build();
        } else if (scenario.emptyOverride()) {
            override = SdkRequestOverrideConfiguration.builder().build();
        }
        CallOptions call = CallOptions.of(config).withOverrides(null, override);
        Optional<ConfigurationSource> callWinner = winner(present);
        assertEquals(callWinner.map(expected::get), call.get(option), "call-level value");
        assertEquals(callWinner.equals(Optional.of(REQUEST_OVERRIDE)), call.isOverridden(option));
        Optional<ConfigurationSource> callProvenance = call.isOverridden(option)
                                                       ? Optional.of(REQUEST_OVERRIDE)
                                                       : call.clientConfiguration().resolved(option)
                                                             .map(ResolvedOption::source);
        assertEquals(callWinner, callProvenance, "call-level provenance");
        assertSame(config, call.clientConfiguration());
        assertEquals(expectedClient, config.resolved(option), "a call leaves the client configuration unchanged");

        // The precedence rule itself, over every present candidate in any order, with null elements ignored.
        List<ResolvedOption<T>> candidates = new ArrayList<>();
        for (ConfigurationSource source : present) {
            candidates.add(ResolvedOption.of(option, expected.get(source), source));
        }
        Random random = new Random(scenario.shuffleSeed());
        for (int i = random.nextInt(3); i > 0; i--) {
            candidates.add(null);
        }
        Collections.shuffle(candidates, random);
        assertEquals(callWinner.map(w -> expectedOption(option, expected, w)),
                     ClientConfigurationResolver.select(candidates), "select over " + candidates);
        List<ResolvedOption<T>> clientCandidates = new ArrayList<>(candidates);
        clientCandidates.removeIf(c -> c != null && c.source() == REQUEST_OVERRIDE);
        assertEquals(config.resolved(option), ClientConfigurationResolver.select(clientCandidates),
                     "select agrees with resolve");
    }

    /** Whether no source of {@code present} ranks above {@code source}, apart from the request override. */
    private static boolean noneRankedAbove(Set<ConfigurationSource> present, ConfigurationSource source) {
        for (ConfigurationSource higher : PRECEDENCE.subList(0, PRECEDENCE.indexOf(source))) {
            if (higher != REQUEST_OVERRIDE && present.contains(higher)) {
                return false;
            }
        }
        return true;
    }

    private static <T> ResolvedOption<T> expectedOption(ClientOption<T> option, Map<ConfigurationSource, T> expected,
                                                        ConfigurationSource source) {
        return new ResolvedOption<>(option, expected.get(source), source, OWNERSHIP.get(source));
    }

    /**
     * A {@link SettingSource#fromMap} snapshot that counts lookups per name. It defines the kind's name for its source
     * when {@code text} is non-null, and always an unrelated name that resolution must never read.
     */
    private static final class CountingSource implements SettingSource {
        private final SettingSource delegate;
        private final Map<String, AtomicInteger> reads = new ConcurrentHashMap<>();

        CountingSource(Kind<?> kind, ConfigurationSource source, String text) {
            String name = kind.nameIn(source);
            Map<String, String> settings = new HashMap<>();
            settings.put(name + NOISE_SUFFIX, "not a valid " + kind.name());
            if (text != null) {
                settings.put(name, text);
            }
            this.delegate = SettingSource.fromMap(settings);
        }

        @Override
        public Optional<String> setting(String name) {
            reads.computeIfAbsent(Objects.requireNonNull(name, "name"), n -> new AtomicInteger()).incrementAndGet();
            return delegate.setting(name);
        }

        int reads(String name) {
            AtomicInteger count = reads.get(name);
            return count == null ? 0 : count.get();
        }

        int totalReads() {
            return reads.values().stream().mapToInt(AtomicInteger::get).sum();
        }
    }
}
