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
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

/**
 * The sync TCK against the AWS CRT transport ({@code aws-crt-client}). The read buffer (the declared buffer bound) is
 * lowered from the 16 MiB default to {@link CrtTck#READ_BUFFER} so that the larger response bodies exercise window
 * updates. Documented deviations:
 * unknown-length request bodies fail fast, and the test certificate is trusted through the PEM option.
 */
class CrtSyncTckTest extends SyncHttpClientTck {

    static final TransportUnderTest<SdkSyncHttpClient> TRANSPORT =
        TransportUnderTest.of("aws-crt-client-sync", HttpTransportMode.SYNC, CrtSyncTckTest::create,
                              Deviation.UNKNOWN_LENGTH_REQUEST_BODY_FAILS_FAST, Deviation.TEST_TRUST_BY_PEM,
                              Deviation.RELEASES_FULLY_RECEIVED_RESPONSE_FOR_REUSE);

    private static SdkSyncHttpClient create(ClientSettings settings) {
        CrtTck.rejectUnsupported(settings);
        AwsCrtHttpClient.Builder builder = AwsCrtHttpClient.builder().connectionTimeout(settings.connectTimeout())
                                                           .readBufferSizeInBytes(CrtTck.READ_BUFFER);
        if (settings.maxConnections() != null) {
            builder.maxConcurrency(settings.maxConnections());
        }
        if (settings.trust() != null) {
            builder.tlsTrustedCertificatesPem(settings.trust().certificatePem());
        }
        if (settings.proxy() != null) {
            builder.proxyConfiguration(CrtTck.proxy(settings.proxy()));
        }
        return builder.build();
    }

    @Override
    TransportUnderTest<SdkSyncHttpClient> transport() {
        return TRANSPORT;
    }
}
