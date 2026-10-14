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

package software.amazon.awssdk.protocol.smithytests;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@link ProtocolTestHarnessFactory} that generated protocol tests run on: request tests capture the request with a
 * {@link CapturingClientTransport}, response tests are answered by a {@link FixedResponseTransport}. No network
 * connection is opened.
 *
 * <p>For each call the harness builds a fresh client from the generated test's builder and configuration, then sets the
 * endpoint and installs the transport twice:
 * <ul>
 *     <li>as {@link SdkClientOption#CLIENT_TRANSPORT} and {@link SdkClientOption#ASYNC_CLIENT_TRANSPORT}, which calls on
 *     the schema-based request pipeline send through; and</li>
 *     <li>as the client's HTTP client, through an adapter that hands each request to the same transport, for calls that
 *     still run on the legacy request pipeline.</li>
 * </ul>
 * Either way the request reaches the transport exactly as the client serialized and signed it.
 *
 * <p>{@code captureRequest} also fixes the idempotency token for the duration of the call
 * ({@link FixedIdempotencyToken}). Clients must be built by an {@link SdkDefaultClientBuilder}, as every generated client
 * is: there is no public builder method for the transport options, so the harness sets them in the builder's client
 * configuration directly.
 *
 * <p>Register it for a test class path in
 * {@code META-INF/services/software.amazon.awssdk.protocol.smithytests.ProtocolTestHarnessFactory}.
 */
public final class TransportProtocolTestHarnessFactory implements ProtocolTestHarnessFactory {
    /**
     * The endpoint of clients built by {@code respondWith}. Response test cases do not depend on it.
     */
    static final String RESPONSE_ENDPOINT = "https://example.com";

    private static final Duration ASYNC_SEND_TIMEOUT = Duration.ofSeconds(10);

    @Override
    public <C, B extends SdkClientBuilder<B, C>> ProtocolTestHarness<C> create(String protocol,
                                                                              Supplier<B> clientBuilder,
                                                                              Consumer<? super B> configuration) {
        return new Harness<>(protocol, clientBuilder, configuration);
    }

    private static final class Harness<C, B extends SdkClientBuilder<B, C>> implements ProtocolTestHarness<C> {
        private final String protocol;
        private final Supplier<B> clientBuilder;
        private final Consumer<? super B> configuration;

        private Harness(String protocol, Supplier<B> clientBuilder, Consumer<? super B> configuration) {
            this.protocol = Validate.paramNotBlank(protocol, "protocol");
            this.clientBuilder = Validate.paramNotNull(clientBuilder, "clientBuilder");
            this.configuration = Validate.paramNotNull(configuration, "configuration");
        }

        @Override
        public CapturedRequest captureRequest(Consumer<? super C> call, String endpoint) {
            Validate.paramNotNull(call, "call");
            Validate.paramNotBlank(endpoint, "endpoint");
            CapturingClientTransport transport = CapturingClientTransport.create();
            try (FixedIdempotencyToken token = FixedIdempotencyToken.install()) {
                B builder = configuredBuilder(endpoint, transport, transport);
                boolean async = builder instanceof SdkAsyncClientBuilder;
                return withClient(builder.build(), client -> {
                    try {
                        call.accept(client);
                    } catch (RuntimeException e) {
                        // RequestCaptured, or the SDK exception wrapping it. Anything thrown before the send propagates.
                        if (!transport.firstRequest().isPresent()) {
                            throw e;
                        }
                    }
                    if (async) {
                        // An async call may send on another thread after it returns its future; wait before the client
                        // is closed.
                        return transport.awaitFirstRequest(ASYNC_SEND_TIMEOUT);
                    }
                    return transport.firstRequest()
                                    .orElseThrow(() -> new AssertionError("The " + protocol + " client sent no request"));
                });
            }
        }

        @Override
        public <T> T respondWith(StubbedResponse response, Function<? super C, ? extends T> call) {
            Validate.paramNotNull(response, "response");
            Validate.paramNotNull(call, "call");
            FixedResponseTransport transport = FixedResponseTransport.create(response);
            return withClient(configuredBuilder(RESPONSE_ENDPOINT, transport, transport).build(), call);
        }

        private B configuredBuilder(String endpoint,
                                    ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport,
                                    AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> asyncTransport) {
            B builder = Validate.notNull(clientBuilder.get(), "The client builder supplier returned null");
            configuration.accept(builder);
            builder.endpointOverride(URI.create(endpoint));
            SdkDefaultClientBuilder<?, ?> sdkBuilder = defaultBuilder(builder);
            SdkClientConfiguration.Builder options = clientConfiguration(sdkBuilder);
            options.option(SdkClientOption.CLIENT_TRANSPORT, transport);
            options.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, asyncTransport);
            if (builder instanceof SdkAsyncClientBuilder) {
                sdkBuilder.httpClient(TransportHttpClients.async(asyncTransport));
            } else {
                sdkBuilder.httpClient(TransportHttpClients.sync(transport));
            }
            return builder;
        }
    }

    /**
     * Runs {@code body} with {@code client} and closes the client afterwards, keeping a failure of {@code body} as the
     * primary exception.
     */
    private static <C, T> T withClient(C client, Function<? super C, ? extends T> body) {
        Throwable primary = null;
        try {
            return body.apply(client);
        } catch (Throwable t) {
            primary = t;
            throw t;
        } finally {
            close(client, primary);
        }
    }

    private static void close(Object client, Throwable primary) {
        if (!(client instanceof AutoCloseable)) {
            return;
        }
        try {
            ((AutoCloseable) client).close();
        } catch (Exception e) {
            if (primary != null) {
                primary.addSuppressed(e);
            } else {
                throw new IllegalStateException("Unable to close the protocol test client", e);
            }
        }
    }

    private static SdkDefaultClientBuilder<?, ?> defaultBuilder(Object builder) {
        if (!(builder instanceof SdkDefaultClientBuilder)) {
            throw new IllegalArgumentException("The protocol test harness needs a client builder that extends "
                                               + SdkDefaultClientBuilder.class.getName() + ", but got "
                                               + builder.getClass().getName());
        }
        return (SdkDefaultClientBuilder<?, ?>) builder;
    }

    /**
     * The builder's protected {@code clientConfiguration}. {@link SdkClientOption#CLIENT_TRANSPORT} and
     * {@link SdkClientOption#ASYNC_CLIENT_TRANSPORT} have no builder or override-configuration setter.
     */
    private static SdkClientConfiguration.Builder clientConfiguration(SdkDefaultClientBuilder<?, ?> builder) {
        try {
            Field field = SdkDefaultClientBuilder.class.getDeclaredField("clientConfiguration");
            field.setAccessible(true);
            return (SdkClientConfiguration.Builder) field.get(builder);
        } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
            throw new IllegalStateException("Unable to set the client transport on " + builder.getClass().getName()
                                            + ": cannot access SdkDefaultClientBuilder.clientConfiguration", e);
        }
    }
}
