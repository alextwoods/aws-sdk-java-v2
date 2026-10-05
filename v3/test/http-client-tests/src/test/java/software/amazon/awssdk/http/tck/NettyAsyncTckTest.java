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

package software.amazon.awssdk.http.tck;

import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

/**
 * The async TCK against the Netty transport ({@code netty-nio-client}) in its default HTTP/1.1 protocol mode (HTTP/2
 * needs ALPN, which the TCK server does not offer). Netty declares no {@code PROXY} option, so the proxy check is
 * skipped by capability gating. No documented deviations.
 */
class NettyAsyncTckTest extends AsyncHttpClientTck {

    static final TransportUnderTest<SdkAsyncHttpClient> TRANSPORT =
        TransportUnderTest.of("netty-nio-client", HttpTransportMode.ASYNC, NettyAsyncTckTest::create);

    private static SdkAsyncHttpClient create(ClientSettings settings) {
        if (settings.proxy() != null) {
            throw new IllegalArgumentException("netty-nio-client has no PROXY");
        }
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder()
                                                                         .connectionTimeout(settings.connectTimeout());
        if (settings.readTimeout() != null) {
            builder.readTimeout(settings.readTimeout());
        }
        if (settings.maxConnections() != null) {
            builder.maxConcurrency(settings.maxConnections());
        }
        if (settings.trust() != null) {
            TckTls tls = settings.trust();
            builder.tlsTrustManagersProvider(tls::trustManagers);
        }
        return builder.build();
    }

    @Override
    TransportUnderTest<SdkAsyncHttpClient> transport() {
        return TRANSPORT;
    }
}
