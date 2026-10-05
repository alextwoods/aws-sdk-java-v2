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
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.urlconnection.ProxyConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * The sync TCK against the JDK URLConnection transport ({@code url-connection-client}). Documented deviations (see
 * the {@code UrlConnectionHttpClient} javadoc): the JDK keep-alive cleaner may reuse an early-closed connection, and
 * abort cannot interrupt a body read blocked inside the JDK.
 */
class UrlConnectionSyncTckTest extends SyncHttpClientTck {

    static final TransportUnderTest<SdkSyncHttpClient> TRANSPORT =
        TransportUnderTest.of("url-connection-client", HttpTransportMode.SYNC, UrlConnectionSyncTckTest::create,
                              Deviation.JDK_KEEP_ALIVE_CLEANER_MAY_REUSE_EARLY_CLOSED_CONNECTION,
                              Deviation.ABORT_CANNOT_INTERRUPT_BLOCKED_BODY_READ);

    private static SdkSyncHttpClient create(ClientSettings settings) {
        if (settings.maxConnections() != null) {
            throw new IllegalArgumentException("url-connection-client has no MAX_CONNECTIONS");
        }
        UrlConnectionHttpClient.Builder builder =
            UrlConnectionHttpClient.builder().connectionTimeout(settings.connectTimeout());
        if (settings.readTimeout() != null) {
            builder.socketTimeout(settings.readTimeout());
        }
        if (settings.trust() != null) {
            TckTls tls = settings.trust();
            builder.tlsTrustManagersProvider(tls::trustManagers);
        }
        if (settings.proxy() != null) {
            builder.proxyConfiguration(ProxyConfiguration.builder().endpoint(settings.proxy()).build());
        }
        return builder.build();
    }

    @Override
    TransportUnderTest<SdkSyncHttpClient> transport() {
        return TRANSPORT;
    }
}
