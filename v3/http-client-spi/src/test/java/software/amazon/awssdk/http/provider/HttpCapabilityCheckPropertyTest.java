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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.provider.HttpClientResolverTest.FakeSyncClient;
import software.amazon.awssdk.http.provider.HttpClientResolverTest.FakeSyncService;
import software.amazon.awssdk.http.provider.HttpClientResolverTest.Tracked;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException.Reason;

/**
 * Property 43 (Requirement 12.13): for any implementation and any configured option set, resolution succeeds iff the
 * implementation's declared capabilities and the capabilities of the client it builds both support every configured
 * option. Otherwise it fails before a client is returned with {@link Reason#UNSUPPORTED_OPTION}, listing exactly the
 * unsupported options in {@link HttpClientOption} declaration order and naming each one (by its V2 configuration name)
 * and the implementation.
 *
 * <p>A declared-capability failure happens before {@code create}, so nothing is acquired. A built-client failure (the
 * client declares fewer options than its implementation's metadata) closes the client and every SDK-owned resource
 * registered during {@code create} exactly once, in reverse order, and leaves caller-owned resources alone. An explicit
 * client is checked the same way and never closed.
 */
class HttpCapabilityCheckPropertyTest {
    private static final List<HttpClientOption> OPTIONS = List.of(HttpClientOption.values());

    // Feature: aws-sdk-java-v3, Property 43: HTTP options are capability checked
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.MIXIN)
    @Label("Property 43: HTTP options are capability checked")
    void httpOptionsAreCapabilityChecked(@ForAll("scenarios") Scenario scenario) {
        List<HttpClientOption> preUnsupported = unsupported(scenario.configured().keySet(), scenario.declared());
        List<HttpClientOption> postUnsupported = unsupported(scenario.configured().keySet(), scenario.built());
        Statistics.label("outcome").collect(!preUnsupported.isEmpty() ? "declared unsupported"
                                            : !postUnsupported.isEmpty() ? "built client unsupported" : "supported");
        Statistics.label("path").collect(scenario.path());

        checkConstruction(scenario, preUnsupported, postUnsupported);
        checkExplicitClient(scenario);
    }

    private static void checkConstruction(Scenario scenario, List<HttpClientOption> preUnsupported,
                                          List<HttpClientOption> postUnsupported) {
        List<String> closes = new ArrayList<>();
        AtomicReference<HttpClientConstruction> construction = new AtomicReference<>();
        AtomicReference<AutoCloseable> built = new AtomicReference<>();
        Function<HttpClientConstruction, AutoCloseable> factory = c -> {
            construction.set(c);
            scenario.resources().forEach(r -> {
                Tracked resource = new Tracked(r.label(), closes, null);
                if (r.sdkOwned()) {
                    c.sdkOwned(resource);
                } else {
                    c.callerOwned(resource);
                }
            });
            AutoCloseable client = scenario.mode() == HttpTransportMode.SYNC
                                   ? new FakeSyncClient(scenario.name(), capsOf(scenario.mode(), scenario.built()),
                                                        closes)
                                   : new ClosingAsyncClient(scenario.name(), capsOf(scenario.mode(), scenario.built()),
                                                            closes);
            built.set(client);
            return client;
        };
        Subject subject = Subject.of(scenario, factory);
        AtomicInteger discoveries = new AtomicInteger();
        Supplier<ResolvedHttpClient<?>> resolve = () -> subject.resolve(scenario, discoveries);

        if (!preUnsupported.isEmpty()) {
            HttpImplementationSelectionException e = assertThrows(HttpImplementationSelectionException.class,
                                                                  resolve::get);
            checkFailure(e, scenario, preUnsupported, "implementation");
            assertEquals(0, subject.creates().get(), "create never runs when the declared capabilities fail");
            assertEquals(List.of(), closes);
        } else if (!postUnsupported.isEmpty()) {
            HttpImplementationSelectionException e = assertThrows(HttpImplementationSelectionException.class,
                                                                  resolve::get);
            checkFailure(e, scenario, postUnsupported, "client");
            assertEquals(1, subject.creates().get());
            assertEquals(expectedReleases(scenario), closes,
                         "the client, then every SDK-owned registration in reverse, each once; caller-owned untouched");
            assertEquals(0, e.getSuppressed().length);
            assertEquals(scenario.configuration(), construction.get().configuration());
        } else {
            ResolvedHttpClient<?> resolved = resolve.get();
            assertSame(built.get(), resolved.client());
            assertEquals(scenario.name(), resolved.implementationName());
            assertEquals(ResourceOwnership.SDK_OWNED, resolved.ownership());
            assertEquals(1, subject.creates().get());
            assertEquals(scenario.configuration(), construction.get().configuration(),
                         "the configured values reach the implementation");
            assertEquals(List.of(), closes, "nothing is released on success");
            resolved.close();
            assertEquals(expectedReleases(scenario), closes);
        }
        assertEquals(scenario.path() == Path.DEFAULT ? 1 : 0, discoveries.get());
        assertEquals(0, subject.otherCreates().get(), "a lower-priority implementation is never a fallback");
    }

    /** An explicit client whose capabilities are the generated built set: checked, and never closed by the SDK. */
    private static void checkExplicitClient(Scenario scenario) {
        List<String> closes = new ArrayList<>();
        HttpClientCapabilities capabilities = capsOf(scenario.mode(), scenario.built());
        List<HttpClientOption> expected = unsupported(scenario.configured().keySet(), scenario.built());
        Supplier<InstalledHttpImplementations> noDiscovery = () -> {
            throw new AssertionError("an explicit client never consults the installed set");
        };
        AutoCloseable explicit;
        Supplier<ResolvedHttpClient<?>> resolve;
        if (scenario.mode() == HttpTransportMode.SYNC) {
            FakeSyncClient client = new FakeSyncClient(scenario.name(), capabilities, closes);
            explicit = client;
            resolve = () -> HttpClientResolver.sync().client(client).configuration(scenario.configuration())
                                              .installed(noDiscovery).resolve();
        } else {
            ClosingAsyncClient client = new ClosingAsyncClient(scenario.name(), capabilities, closes);
            explicit = client;
            resolve = () -> HttpClientResolver.async().client(client).configuration(scenario.configuration())
                                              .installed(noDiscovery).resolve();
        }
        if (expected.isEmpty()) {
            ResolvedHttpClient<?> resolved = resolve.get();
            assertSame(explicit, resolved.client());
            assertEquals(ResourceOwnership.CALLER_OWNED, resolved.ownership());
            resolved.close();
        } else {
            HttpImplementationSelectionException e = assertThrows(HttpImplementationSelectionException.class,
                                                                  resolve::get);
            checkFailure(e, scenario, expected, "client");
        }
        assertEquals(List.of(), closes, "a caller-owned client is never closed");
    }

    private static void checkFailure(HttpImplementationSelectionException e, Scenario scenario,
                                     List<HttpClientOption> expected, String what) {
        assertEquals(Reason.UNSUPPORTED_OPTION, e.reason());
        assertEquals(Optional.of(scenario.mode()), e.mode());
        assertEquals(expected, e.unsupportedOptions(), "exactly the unsupported options, in declaration order");
        assertEquals(List.of(scenario.name()), e.implementations());
        List<String> names = expected.stream().map(HttpClientOption::configurationName).toList();
        assertEquals(names.size() == 1 ? Optional.of(names.get(0)) : Optional.empty(), e.option());

        String message = e.getMessage();
        String prefix = "The " + InstalledHttpImplementations.label(scenario.mode()) + " HTTP " + what + " "
                        + scenario.name() + " does not support the configured option" + (names.size() == 1 ? " "
                                                                                                           : "s ");
        assertTrue(message.startsWith(prefix), () -> message + " should start with " + prefix);
        String listed = message.substring(prefix.length(), message.indexOf(". Remove ", prefix.length()));
        assertEquals(names, Arrays.asList(listed.split(", ")), message);
    }

    // Oracle.

    /** The options of {@code configured} not in {@code supported}, in declaration order. */
    private static List<HttpClientOption> unsupported(Set<HttpClientOption> configured,
                                                      Set<HttpClientOption> supported) {
        return OPTIONS.stream().filter(o -> configured.contains(o) && !supported.contains(o)).toList();
    }

    /** What a closed construction releases: the client, then SDK-owned registrations newest first. */
    private static List<String> expectedReleases(Scenario scenario) {
        List<String> out = new ArrayList<>();
        out.add("client:" + scenario.name());
        List<Resource> resources = scenario.resources();
        for (int i = resources.size() - 1; i >= 0; i--) {
            if (resources.get(i).sdkOwned()) {
                out.add(resources.get(i).label());
            }
        }
        return out;
    }

    private static HttpClientCapabilities capsOf(HttpTransportMode mode, Set<HttpClientOption> options) {
        return caps(mode, options.toArray(new HttpClientOption[0]));
    }

    // Generators.

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Set<HttpClientOption>> optionSets =
            Arbitraries.of(HttpClientOption.class).set().ofMaxSize(OPTIONS.size());
        // Most built clients match their metadata; the rest declare a random subset of it (the removed options).
        Arbitrary<Set<HttpClientOption>> removed =
            Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.just(Set.<HttpClientOption>of())), Tuple.of(2, optionSets));
        // Shapes the configured options relative to the capabilities; the oracle does not depend on the shaping.
        Arbitrary<Tuple.Tuple2<Shaping, Integer>> shaping =
            Combinators.combine(Arbitraries.of(Shaping.class), Arbitraries.integers().between(0, 7)).as(Tuple::of);
        Arbitrary<Resource> resource =
            Combinators.combine(Arbitraries.of(true, false), Arbitraries.integers().between(0, 99))
                       .as((sdk, id) -> new Resource((sdk ? "sdk-" : "caller-") + id, sdk));
        Arbitrary<Tuple.Tuple2<HttpTransportMode, Path>> modeAndPath =
            Combinators.combine(Arbitraries.of(HttpTransportMode.class), Arbitraries.of(Path.class)).as(Tuple::of);
        return Combinators.combine(modeAndPath,
                                   Arbitraries.of("apache", "url-connection", "netty", "crt", "impl-x"),
                                   optionSets, removed, shaping, configured(),
                                   resource.list().ofMaxSize(4).map(HttpCapabilityCheckPropertyTest::uniqueLabels))
                          .as((modePath, name, declared, fewer, shape, configured, resources) -> {
                              Set<HttpClientOption> built = EnumSet.noneOf(HttpClientOption.class);
                              built.addAll(declared);
                              built.removeAll(fewer);
                              Map<HttpClientOption, Object> options = new EnumMap<>(HttpClientOption.class);
                              options.putAll(configured);
                              if (shape.get1() != Shaping.RANDOM) {

                                  options.keySet().retainAll(declared);

                              }

                              if (shape.get1() == Shaping.BUILT_DROPS_ONE && !options.isEmpty()) {

                                  built.remove(List.copyOf(options.keySet()).get(shape.get2() % options.size()));

                              }
                              return new Scenario(modePath.get1(), name, Set.copyOf(declared), Set.copyOf(built),
                                                  Map.copyOf(options), resources, modePath.get2());
                          });
    }

    /** Configured options, biased toward few, with values of the kinds real options take. */
    private static Arbitrary<Map<HttpClientOption, Object>> configured() {
        Arbitrary<Object> values = Arbitraries.oneOf(Arbitraries.longs().between(1, 60_000).map(Duration::ofMillis),
                                                     Arbitraries.integers().between(1, 500),
                                                     Arbitraries.of(true, false),
                                                     Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8));
        return Arbitraries.maps(Arbitraries.of(HttpClientOption.class), values).ofMaxSize(8);
    }

    private static List<Resource> uniqueLabels(List<Resource> resources) {
        return IntStream.range(0, resources.size())
                        .mapToObj(i -> new Resource(resources.get(i).label() + "#" + i, resources.get(i).sdkOwned()))
                        .toList();
    }

    /** How the configured options relate to the capabilities, to reach every branch often. */
    enum Shaping {
        /** Independent of the capabilities. */
        RANDOM,
        /** Only declared options, so create runs. */
        WITHIN_DECLARED,
        /** Only declared options, and the built client drops one of them. */
        BUILT_DROPS_ONE
    }

    /** How the implementation is chosen. */
    enum Path {
        /** The only implementation of the mode at the highest priority, next to a lower one supporting everything. */
        DEFAULT,
        /** Configured explicitly (the V2 builder role). */
        EXPLICIT_IMPLEMENTATION
    }

    record Resource(String label, boolean sdkOwned) {
    }

    record Scenario(HttpTransportMode mode, String name, Set<HttpClientOption> declared, Set<HttpClientOption> built,
                    Map<HttpClientOption, Object> configured, List<Resource> resources, Path path) {
        HttpClientConfiguration configuration() {
            HttpClientConfiguration.Builder builder = HttpClientConfiguration.builder();
            configured.forEach(builder::put);
            return builder.build();
        }
    }

    /** The implementation under test, a lower-priority implementation supporting every option, and their counters. */
    record Subject(HttpImplementation<?> implementation, HttpImplementation<?> other, AtomicInteger creates,
                   AtomicInteger otherCreates) {
        static Subject of(Scenario scenario, Function<HttpClientConstruction, AutoCloseable> factory) {
            HttpClientCapabilities declared = capsOf(scenario.mode(), scenario.declared());
            HttpClientCapabilities everything = capsOf(scenario.mode(), EnumSet.allOf(HttpClientOption.class));
            if (scenario.mode() == HttpTransportMode.SYNC) {
                FakeSyncService service = new FakeSyncService(scenario.name(), 10, declared,
                                                              c -> (FakeSyncClient) factory.apply(c));
                FakeSyncService other = new FakeSyncService("fallback", 1, everything);
                return new Subject(service, other, service.creates, other.creates);
            }
            FactoryAsyncService service = new FactoryAsyncService(scenario.name(), 10, declared,
                                                                  c -> (SdkAsyncHttpClient) factory.apply(c));
            FactoryAsyncService other = new FactoryAsyncService(
                "fallback", 1, everything, c -> new ClosingAsyncClient("fallback", everything, new ArrayList<>()));
            return new Subject(service, other, service.creates, other.creates);
        }

        ResolvedHttpClient<?> resolve(Scenario scenario, AtomicInteger discoveries) {
            Supplier<InstalledHttpImplementations> installed = () -> {
                discoveries.incrementAndGet();
                return InstalledHttpImplementations.of(implementation, other);
            };
            boolean explicit = scenario.path() == Path.EXPLICIT_IMPLEMENTATION;
            if (scenario.mode() == HttpTransportMode.SYNC) {
                HttpClientResolver<?, SdkHttpService> resolver = HttpClientResolver.sync();
                if (explicit) {
                    resolver.implementation((SdkHttpService) implementation);
                }
                return resolver.configuration(scenario.configuration()).installed(installed).resolve();
            }
            HttpClientResolver<?, SdkAsyncHttpService> resolver = HttpClientResolver.async();
            if (explicit) {
                resolver.implementation((SdkAsyncHttpService) implementation);
            }
            return resolver.configuration(scenario.configuration()).installed(installed).resolve();
        }
    }

    /** An async implementation built from a factory, counting {@link #create} calls. */
    static final class FactoryAsyncService implements SdkAsyncHttpService {
        final AtomicInteger creates = new AtomicInteger();
        private final String name;
        private final int priority;
        private final HttpClientCapabilities capabilities;
        private final Function<HttpClientConstruction, SdkAsyncHttpClient> factory;

        FactoryAsyncService(String name, int priority, HttpClientCapabilities capabilities,
                            Function<HttpClientConstruction, SdkAsyncHttpClient> factory) {
            this.name = name;
            this.priority = priority;
            this.capabilities = capabilities;
            this.factory = factory;
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
            return capabilities;
        }

        @Override
        public SdkAsyncHttpClient create(HttpClientConstruction construction) {
            creates.incrementAndGet();
            return factory.apply(construction);
        }
    }

    /** An async client that records its close as {@code client:<name>}. */
    static final class ClosingAsyncClient implements SdkAsyncHttpClient {
        private final String name;
        private final HttpClientCapabilities capabilities;
        private final List<String> closes;

        ClosingAsyncClient(String name, HttpClientCapabilities capabilities, List<String> closes) {
            this.name = name;
            this.capabilities = capabilities;
            this.closes = closes;
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public String clientName() {
            return name;
        }

        @Override
        public void close() {
            closes.add("client:" + name);
        }
    }
}
