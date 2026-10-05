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
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.apache5.ProxyConfiguration;

/**
 * The sync TCK against the Apache HttpClient 5 classic transport ({@code apache5-client}). Deviation:
 * {@link Deviation#TLS_FAILURE_MASKED_BY_A_LATER_ADDRESS} (TCK finding).
 */
class Apache5SyncTckTest extends SyncHttpClientTck {

    static final TransportUnderTest<SdkSyncHttpClient> TRANSPORT =
        TransportUnderTest.of("apache5-client", HttpTransportMode.SYNC, Apache5SyncTckTest::create,
                              Deviation.TLS_FAILURE_MASKED_BY_A_LATER_ADDRESS);

    private static SdkSyncHttpClient create(ClientSettings settings) {
        Apache5HttpClient.Builder builder = Apache5HttpClient.builder().connectionTimeout(settings.connectTimeout());
        if (settings.readTimeout() != null) {
            builder.socketTimeout(settings.readTimeout());
        }
        if (settings.maxConnections() != null) {
            builder.maxConnections(settings.maxConnections());
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
