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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.exception.SdkCloseException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolTransportRequirements;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 34: Protocol/transport capability validation is fail-fast (Requirement 10.13).
 *
 * <p>For any protocol and HTTP transport whose message/capability types are incompatible, building the client's
 * {@link AwsCallEngine} (the last step of client construction) fails before an engine, and so a client, is returned,
 * the failure names both the protocol ID and the transport's {@code clientName()}, every SDK-owned resource acquired
 * for the client is closed exactly once, no caller-owned resource is closed, and nothing is transmitted. For a
 * compatible pair the engine is built and nothing is closed or transmitted.
 *
 * <p>A scenario picks a protocol ID; the sync or async driver (the runtime's transport mode); which transports are
 * wired: one of the runtime's mode, one of the other mode only, or both; whether the runtime has transport
 * requirements and, if so, the HTTP versions (ALPN IDs, including {@code h3}, which no SPI transport can declare) its
 * ordinary and its event-stream operations can use, and whether the client has event-stream operations; the
 * transport's declared capabilities: its mode (usually the slot's, sometimes the other one), its HTTP versions and
 * options (options do not take part in compatibility); and whether each transport is SDK-owned or caller-owned, plus
 * up to two further SDK-owned resources and one caller-owned resource.
 *
 * <p>The oracle, {@link #expectedReasons}, is written here from the requirement and the SPI types, not from the
 * engine: a transport of the other mode only is incompatible; without transport requirements nothing else is checked;
 * with them, the transport of the runtime's mode is incompatible if its capabilities declare another mode, if it
 * supports none of the protocol's HTTP versions, or if the client has event-stream operations and it supports none of
 * their HTTP versions.
 *
 * <p>Interpretations: "construction" is {@link AwsCallEngine.Builder#build()} over a built {@link ClientRuntime}; the
 * engine closes the runtime it rejects, which is the client's rollback. A missing transport (none wired) is a missing
 * component ({@link NullPointerException}), not an incompatibility, and is not generated. The protocol-core
 * {@link ProtocolTransportRequirements.Message} has the one kind {@code HTTP}, and every SPI transport exchanges HTTP
 * messages, so a message-kind mismatch cannot be constructed.
 */
class ProtocolTransportValidationPropertyTest {
    private static final List<ShapeId> PROTOCOL_IDS = List.of(ShapeId.from("aws.protocols#restJson1"),
                                                              ShapeId.from("aws.protocols#awsJson1_0"),
                                                              ShapeId.from("aws.protocols#restXml"),
                                                              ShapeId.from("smithy.protocols#rpcv2Cbor"));
    /** The ALPN IDs a protocol may list; {@code h3} is never supported by an SPI transport. */
    private static final List<String> ALPN_IDS = List.of("http/1.1", "h2", "h3");
    /** The ALPN ID of each SPI protocol version, as the protocol traits name them. */
    private static final Map<Protocol, String> ALPN = Map.of(Protocol.HTTP1_1, "http/1.1", Protocol.HTTP2, "h2");

    /** Which transports the engine builder is given. */
    enum Wiring {
        /** A transport of the runtime's mode only. */
        MODE_ONLY,
        /** A transport of the other mode only. */
        OTHER_ONLY,
        /** One of each mode. */
        BOTH
    }

    /** Why a pair is incompatible. */
    enum Reason {
        OTHER_MODE_ONLY,
        DECLARED_MODE,
        HTTP_VERSIONS,
        EVENT_STREAM_VERSIONS
    }

    // Feature: aws-sdk-java-v3, Property 34: Protocol/transport capability validation is fail-fast
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if compatible pairs or any incompatibility reason stop being generated.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 34: Protocol/transport capability validation is fail-fast")
    void incompatiblePairsFailConstructionWithoutLeaks(@ForAll("scenarios") Scenario scenario) {
        Set<Reason> expected = expectedReasons(scenario);
        Components components = new Components(scenario);
        ClientRuntime runtime = components.runtime();
        AwsCallEngine.Builder builder = components.engineBuilder(runtime);

        AwsCallEngine engine = null;
        Throwable failure = null;
        try {
            engine = builder.build();
        } catch (Throwable t) {
            failure = t;
        }

        if (expected.isEmpty()) {
            assertThat(failure).as("a compatible pair is accepted").isNull();
            assertThat(engine.runtime()).isSameAs(runtime);
            assertThat(runtime.resources().isClosed()).as("nothing is closed prematurely").isFalse();
            components.closeables().forEach(c -> assertThat(c.closes).as(c.name + " closes").hasValue(0));
        } else {
            assertThat(engine).as("no engine, so no client, is returned").isNull();
            assertThat(failure).as("an incompatible pair is rejected")
                               .isInstanceOf(SdkClientConfigurationException.class);
            assertThat(failure.getMessage()).as("the failure names the protocol")
                                            .contains(scenario.protocolId().toString())
                                            .as("the failure names the transport")
                                            .contains(components.namedTransport().clientName());
            assertThat(failure.getMessage().contains("Event-stream"))
                .as("an event-stream reason is reported exactly when expected")
                .isEqualTo(expected.contains(Reason.EVENT_STREAM_VERSIONS));
            assertThat(runtime.resources().isClosed()).as("the rejected runtime is rolled back").isTrue();
            for (CountingCloseable closeable : components.closeables()) {
                assertThat(closeable.closes).as(closeable.name + " closes")
                                            .hasValue(closeable.sdkOwned ? 1 : 0);
            }
        }
        components.transports().forEach(t -> assertThat(t.exchanges).as(t.name + " exchanges").hasValue(0));

        // Releases a compatible client's resources, and checks that a rejected one is not closed twice.
        runtime.close();
        for (CountingCloseable closeable : components.closeables()) {
            assertThat(closeable.closes).as(closeable.name + " closes after close")
                                        .hasValue(closeable.sdkOwned ? 1 : 0);
        }

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("outcome").collect(expected.isEmpty() ? "compatible" : "incompatible");
        Statistics.label("outcome").coverage(c -> {
            c.check("compatible").percentage(p -> p >= 25);
            c.check("incompatible").percentage(p -> p >= 25);
        });
        Statistics.label("requirements").collect(scenario.requirements().isPresent());
        Statistics.label("requirements").coverage(c -> c.check(false).percentage(p -> p >= 10));
        for (Reason reason : Reason.values()) {
            String label = "rejected for " + reason;
            Statistics.label(label).collect(expected.contains(reason));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 3));
        }
        String compatibleWiring = "compatible with " + scenario.wiring();
        Statistics.label(compatibleWiring).collect(expected.isEmpty());
        if (scenario.wiring() != Wiring.OTHER_ONLY) {
            Statistics.label(compatibleWiring).coverage(c -> c.check(true).percentage(p -> p >= 20));
        }
    }

    @Example
    @Label("Property 34 (example): a modeled h2-only event-stream protocol rejects an HTTP/1.1-only transport")
    void modeledEventStreamProtocolRejectsHttp1Transport() {
        ShapeId restJson = ShapeId.from("aws.protocols#restJson1");
        ProtocolTransportRequirements requirements = ProtocolTransportRequirements.http(
            new ModeledProtocol(restJson, List.of("http/1.1", "h2"), List.of("h2"), false));
        CountingTransport transport = new CountingTransport("Http1OnlyTransport", true,
                                                            capabilities(HttpTransportMode.SYNC,
                                                                         EnumSet.of(Protocol.HTTP1_1), Set.of()));
        ClientRuntime runtime = runtimeBuilder(restJson, HttpTransportMode.SYNC, List.of(transport))
            .transportRequirements(requirements, true)
            .build();

        assertThatThrownBy(() -> AwsCallEngine.builder().runtime(runtime).syncHttpClient(transport).build())
            .isInstanceOf(SdkClientConfigurationException.class)
            .hasMessageContaining(restJson.toString())
            .hasMessageContaining("Http1OnlyTransport")
            .hasMessageContaining("Event-stream");
        assertThat(transport.closes).hasValue(1);

        ClientRuntime withoutEventStreams = runtimeBuilder(restJson, HttpTransportMode.SYNC, List.of())
            .transportRequirements(requirements, false)
            .build();
        assertThat(AwsCallEngine.builder().runtime(withoutEventStreams).syncHttpClient(transport).build()).isNotNull();
    }

    @Example
    @Label("Property 34 (example): a close failure during rollback is suppressed and other resources still close")
    void rollbackCloseFailureIsSuppressed() {
        ShapeId protocolId = PROTOCOL_IDS.get(0);
        CountingTransport transport = new CountingTransport("SyncOnlyTransport", true,
                                                            capabilities(HttpTransportMode.SYNC,
                                                                         EnumSet.of(Protocol.HTTP1_1), Set.of()));
        IllegalStateException closeFailure = new IllegalStateException("close failed");
        AutoCloseable failing = () -> {
            throw closeFailure;
        };
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .addResource(ManagedResource.sdkOwned(transport))
                                                                     .addResource(ManagedResource.sdkOwned(failing))
                                                                     .build();
        ClientRuntime runtime = ClientRuntime.builder()
                                             .configuration(configuration)
                                             .protocol(new IdProtocol(protocolId))
                                             .transportMode(HttpTransportMode.ASYNC)
                                             .completionExecutor(new ManualExecutor())
                                             .retryScheduler(new ManualScheduler())
                                             .retryStrategy(StandardRetryStrategy.create(), "things")
                                             .build();

        Throwable failure = catchThrowable(
            () -> AwsCallEngine.builder().runtime(runtime).syncHttpClient(transport).build());

        assertThat(failure).isInstanceOf(SdkClientConfigurationException.class)
                           .hasMessageContaining(protocolId.toString())
                           .hasMessageContaining("SyncOnlyTransport");
        assertThat(failure.getSuppressed()).singleElement().isInstanceOf(SdkCloseException.class);
        assertThat(failure.getSuppressed()[0].getSuppressed()).containsExactly(closeFailure);
        assertThat(transport.closes).hasValue(1);
    }

    @Example
    @Label("Property 34 (example): transport requirements of another protocol are rejected")
    void requirementsOfAnotherProtocolAreRejected() {
        ProtocolTransportRequirements other = ProtocolTransportRequirements.http(
            ModeledProtocol.of(PROTOCOL_IDS.get(1)));
        assertThatThrownBy(() -> runtimeBuilder(PROTOCOL_IDS.get(0), HttpTransportMode.SYNC, List.of())
            .transportRequirements(other, false)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(PROTOCOL_IDS.get(0).toString())
            .hasMessageContaining(PROTOCOL_IDS.get(1).toString());
    }

    /** Every reason the scenario's pair is incompatible; empty if it is compatible. */
    static Set<Reason> expectedReasons(Scenario scenario) {
        if (scenario.wiring() == Wiring.OTHER_ONLY) {
            return Set.of(Reason.OTHER_MODE_ONLY);
        }
        if (scenario.requirements().isEmpty()) {
            return Set.of();
        }
        if (scenario.declaresOtherMode()) {
            return Set.of(Reason.DECLARED_MODE);
        }
        Set<String> supported = new HashSet<>();
        scenario.transportVersions().forEach(version -> supported.add(ALPN.get(version)));
        Versions versions = scenario.requirements().get();
        Set<Reason> reasons = EnumSet.noneOf(Reason.class);
        if (Collections.disjoint(versions.http(), supported)) {
            reasons.add(Reason.HTTP_VERSIONS);
        }
        if (scenario.eventStreams() && Collections.disjoint(versions.eventStream(), supported)) {
            reasons.add(Reason.EVENT_STREAM_VERSIONS);
        }
        return reasons;
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Wiring> wirings = Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.just(Wiring.MODE_ONLY)),
                                                            Tuple.of(1, Arbitraries.just(Wiring.OTHER_ONLY)),
                                                            Tuple.of(1, Arbitraries.just(Wiring.BOTH)));
        Arbitrary<List<String>> alpnLists = Arbitraries.of(ALPN_IDS).list().uniqueElements().ofMinSize(1)
                                                       .ofMaxSize(ALPN_IDS.size());
        Arbitrary<Optional<Versions>> requirements = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(Optional.empty())),
            Tuple.of(5, Combinators.combine(alpnLists, alpnLists).as(Versions::new).map(Optional::of)));
        Arbitrary<Boolean> declaresOtherMode = Arbitraries.frequencyOf(Tuple.of(5, Arbitraries.just(false)),
                                                                       Tuple.of(1, Arbitraries.just(true)));
        Arbitrary<Set<Protocol>> versions = Arbitraries.of(Protocol.class).set().ofMinSize(1);
        Arbitrary<Set<HttpClientOption>> options = Arbitraries.of(HttpClientOption.class).set();
        Arbitrary<Head> heads = Combinators.combine(Arbitraries.of(PROTOCOL_IDS), PipelineArbitraries.drivers(),
                                                    wirings, requirements, Arbitraries.of(true, false),
                                                    declaresOtherMode, versions, options)
                                           .as(Head::new);
        return Combinators.combine(heads, Arbitraries.of(true, false), Arbitraries.integers().between(0, 2))
                          .as((head, sdkOwned, extra) -> new Scenario(head.protocolId, head.driver, head.wiring,
                                                                      head.requirements, head.eventStreams,
                                                                      head.declaresOtherMode,
                                                                      head.transportVersions, head.options,
                                                                      sdkOwned, extra));
    }

    private record Head(ShapeId protocolId, Driver driver, Wiring wiring, Optional<Versions> requirements,
                        boolean eventStreams, boolean declaresOtherMode, Set<Protocol> transportVersions,
                        Set<HttpClientOption> options) {
    }

    /** The HTTP versions, as ALPN IDs in preference order, of the protocol's ordinary and event-stream operations. */
    record Versions(List<String> http, List<String> eventStream) {
    }

    /**
     * One generated client construction.
     *
     * @param transportSdkOwned whether the wired transports are SDK-owned (otherwise caller-owned)
     * @param extraSdkResources how many further SDK-owned resources the client acquired
     */
    record Scenario(ShapeId protocolId, Driver driver, Wiring wiring, Optional<Versions> requirements,
                    boolean eventStreams, boolean declaresOtherMode, Set<Protocol> transportVersions,
                    Set<HttpClientOption> options, boolean transportSdkOwned, int extraSdkResources) {
    }

    /** The components of one scenario's client, each counting its closes and exchanges. */
    private static final class Components {
        private final Scenario scenario;
        private final CountingTransport modeTransport;
        private final CountingTransport otherTransport;
        private final List<CountingCloseable> resources = new ArrayList<>();

        Components(Scenario scenario) {
            this.scenario = scenario;
            HttpTransportMode mode = scenario.driver().mode;
            HttpTransportMode other = other(mode);
            this.modeTransport = scenario.wiring() == Wiring.OTHER_ONLY ? null : new CountingTransport(
                name(mode), scenario.transportSdkOwned(),
                capabilities(scenario.declaresOtherMode() ? other : mode, scenario.transportVersions(),
                             scenario.options()));
            this.otherTransport = scenario.wiring() == Wiring.MODE_ONLY ? null : new CountingTransport(
                name(other), scenario.transportSdkOwned(),
                capabilities(other, scenario.transportVersions(), scenario.options()));
            for (int i = 0; i < scenario.extraSdkResources(); i++) {
                resources.add(new CountingCloseable("sdk-resource-" + i, true));
            }
            resources.add(new CountingCloseable("caller-resource", false));
        }

        ClientRuntime runtime() {
            ClientRuntime.Builder builder = runtimeBuilder(scenario.protocolId(), scenario.driver().mode,
                                                           closeables());
            scenario.requirements().ifPresent(versions -> builder.transportRequirements(
                new ProtocolTransportRequirements(scenario.protocolId(), ProtocolTransportRequirements.Message.HTTP,
                                                  versions.http(), versions.eventStream()),
                scenario.eventStreams()));
            return builder.build();
        }

        AwsCallEngine.Builder engineBuilder(ClientRuntime runtime) {
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime);
            boolean sync = scenario.driver() == Driver.SYNC;
            CountingTransport syncTransport = sync ? modeTransport : otherTransport;
            CountingTransport asyncTransport = sync ? otherTransport : modeTransport;
            if (syncTransport != null) {
                builder.syncHttpClient(syncTransport);
            }
            if (asyncTransport != null) {
                builder.asyncHttpClient(asyncTransport);
            }
            return builder;
        }

        /** The transport an incompatibility names: the one of the runtime's mode, else the other one. */
        CountingTransport namedTransport() {
            return modeTransport != null ? modeTransport : otherTransport;
        }

        List<CountingTransport> transports() {
            List<CountingTransport> transports = new ArrayList<>();
            if (modeTransport != null) {
                transports.add(modeTransport);
            }
            if (otherTransport != null) {
                transports.add(otherTransport);
            }
            return transports;
        }

        /** Every resource the client acquired: its transports and further resources, in registration order. */
        List<CountingCloseable> closeables() {
            List<CountingCloseable> all = new ArrayList<>(transports());
            all.addAll(resources);
            return all;
        }

        private static HttpTransportMode other(HttpTransportMode mode) {
            return mode == HttpTransportMode.SYNC ? HttpTransportMode.ASYNC : HttpTransportMode.SYNC;
        }

        private static String name(HttpTransportMode mode) {
            return mode == HttpTransportMode.SYNC ? "CountingSyncTransport" : "CountingAsyncTransport";
        }
    }

    /**
     * A runtime of {@code mode} over a protocol with {@code protocolId} that owns {@code closeables} as their
     * ownership says; an async runtime has a completion executor and a retry scheduler.
     */
    private static ClientRuntime.Builder runtimeBuilder(ShapeId protocolId, HttpTransportMode mode,
                                                        List<? extends CountingCloseable> closeables) {
        SdkClientConfiguration.Builder configuration = SdkClientConfiguration.builder();
        for (CountingCloseable closeable : closeables) {
            configuration.addResource(closeable.sdkOwned ? ManagedResource.sdkOwned(closeable)
                                                         : ManagedResource.callerOwned(closeable));
        }
        ClientRuntime.Builder builder = ClientRuntime.builder()
                                                     .configuration(configuration.build())
                                                     .protocol(new IdProtocol(protocolId))
                                                     .transportMode(mode)
                                                     .retryStrategy(StandardRetryStrategy.create(), "things");
        if (mode == HttpTransportMode.ASYNC) {
            builder.completionExecutor(new ManualExecutor()).retryScheduler(new ManualScheduler());
        }
        return builder;
    }

    private static HttpClientCapabilities capabilities(HttpTransportMode mode, Set<Protocol> versions,
                                                       Set<HttpClientOption> options) {
        return HttpClientCapabilities.builder()
                                     .mode(mode)
                                     .addProtocol(versions.toArray(Protocol[]::new))
                                     .options(options)
                                     .maxBufferedBytesPerExchange(64 * 1024)
                                     .cancellationTimeout(Duration.ofSeconds(1))
                                     .build();
    }

    /** A resource that counts its close attempts. */
    private static class CountingCloseable implements AutoCloseable {
        final String name;
        final boolean sdkOwned;
        final AtomicInteger closes = new AtomicInteger();

        CountingCloseable(String name, boolean sdkOwned) {
            this.name = name;
            this.sdkOwned = sdkOwned;
        }

        @Override
        public void close() {
            closes.incrementAndGet();
        }
    }

    /** A transport of either SPI with fixed capabilities that counts exchanges; construction must start none. */
    private static final class CountingTransport extends CountingCloseable
        implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final AtomicInteger exchanges = new AtomicInteger();
        private final HttpClientCapabilities capabilities;

        CountingTransport(String name, boolean sdkOwned, HttpClientCapabilities capabilities) {
            super(name, sdkOwned);
            this.capabilities = capabilities;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            exchanges.incrementAndGet();
            throw new UnsupportedOperationException("construction must not transmit");
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            exchanges.incrementAndGet();
            throw new UnsupportedOperationException("construction must not transmit");
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public String clientName() {
            return name;
        }
    }

    /** A protocol with a given ID; construction never asks it for an operation plan. */
    private record IdProtocol(ShapeId protocolId) implements ClientProtocol {
        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            throw new UnsupportedOperationException("construction must not plan operations");
        }
    }
}
