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

package software.amazon.awssdk.http.nio.netty;

import java.time.Duration;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.SdkAsyncHttpService;

/**
 * The Netty async provider ({@code META-INF/services/software.amazon.awssdk.http.provider.SdkAsyncHttpService}).
 *
 * <p>Priority {@link #PRIORITY} = 300: V2 made Netty the default async client, so it is the highest async default.
 * Other async providers (CRT async, Apache 5 async) must use distinct lower values, so no tie is possible; the
 * design keeps the final numbers evidence-dependent (release decision 2).
 *
 * <p>Configuration values: timeouts are {@link Duration}s; {@code MAX_CONNECTIONS} (V2 {@code maxConcurrency}),
 * {@code MAX_PENDING_CONNECTION_ACQUIRES} and the HTTP/2 settings are {@link Integer}s; {@code TCP_KEEP_ALIVE} a
 * {@link Boolean}; {@code PROTOCOL_VERSION} a {@link Protocol}; {@code TLS_KEY_MANAGERS} a
 * {@link TlsKeyManagersProvider}; {@code TLS_TRUST_MANAGERS} a {@link TlsTrustManagersProvider}.
 *
 * <p>Ownership: the SDK-created event loop group is registered as SDK-owned as soon as it exists, then the
 * connection pools; a failed construction releases them in reverse order.
 */
public final class NettyNioAsyncHttpService implements SdkAsyncHttpService {
    /** The implementation name. */
    public static final String NAME = "netty-nio-client";
    /** The default-selection priority. */
    public static final int PRIORITY = 300;

    @Override
    public String implementationName() {
        return NAME;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return NettyNioAsyncHttpClient.capabilities(Http2Configuration.DEFAULT_INITIAL_WINDOW_SIZE);
    }

    @Override
    public SdkAsyncHttpClient create(HttpClientConstruction construction) {
        HttpClientConfiguration c = construction.configuration();
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();
        c.value(HttpClientOption.CONNECTION_TIMEOUT, Duration.class).ifPresent(builder::connectionTimeout);
        c.value(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT, Duration.class)
         .ifPresent(builder::connectionAcquisitionTimeout);
        c.value(HttpClientOption.READ_TIMEOUT, Duration.class).ifPresent(builder::readTimeout);
        c.value(HttpClientOption.WRITE_TIMEOUT, Duration.class).ifPresent(builder::writeTimeout);
        c.value(HttpClientOption.TLS_NEGOTIATION_TIMEOUT, Duration.class).ifPresent(builder::tlsNegotiationTimeout);
        c.value(HttpClientOption.TLS_KEY_MANAGERS, TlsKeyManagersProvider.class)
         .ifPresent(builder::tlsKeyManagersProvider);
        c.value(HttpClientOption.TLS_TRUST_MANAGERS, TlsTrustManagersProvider.class)
         .ifPresent(builder::tlsTrustManagersProvider);
        c.value(HttpClientOption.MAX_CONNECTIONS, Integer.class).ifPresent(builder::maxConcurrency);
        c.value(HttpClientOption.MAX_PENDING_CONNECTION_ACQUIRES, Integer.class)
         .ifPresent(builder::maxPendingConnectionAcquires);
        c.value(HttpClientOption.CONNECTION_TIME_TO_LIVE, Duration.class).ifPresent(builder::connectionTimeToLive);
        c.value(HttpClientOption.CONNECTION_MAX_IDLE_TIME, Duration.class).ifPresent(builder::connectionMaxIdleTime);
        c.value(HttpClientOption.TCP_KEEP_ALIVE, Boolean.class).ifPresent(builder::tcpKeepAlive);
        c.value(HttpClientOption.PROTOCOL_VERSION, Protocol.class).ifPresent(builder::protocol);
        Http2Configuration.Builder h2 = Http2Configuration.builder();
        c.value(HttpClientOption.HTTP2_MAX_STREAMS, Integer.class).ifPresent(h2::maxStreams);
        c.value(HttpClientOption.HTTP2_INITIAL_WINDOW_SIZE, Integer.class).ifPresent(h2::initialWindowSize);
        builder.http2Configuration(h2.build());
        NettyNioAsyncHttpClient client = builder.build(construction::sdkOwned);
        construction.sdkOwned(client.connectionPools());
        return client;
    }
}
