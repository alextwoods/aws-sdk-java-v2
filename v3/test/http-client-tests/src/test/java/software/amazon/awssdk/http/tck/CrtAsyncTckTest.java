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
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;

/**
 * The async TCK against the AWS CRT transport ({@code aws-crt-client}), with the read buffer lowered to
 * {@link CrtTck#READ_BUFFER} as in {@link CrtSyncTckTest}. Documented deviations: unknown-length request bodies fail
 * fast, and the test certificate is trusted through the PEM option.
 */
class CrtAsyncTckTest extends AsyncHttpClientTck {

    static final TransportUnderTest<SdkAsyncHttpClient> TRANSPORT =
        TransportUnderTest.of("aws-crt-client-async", HttpTransportMode.ASYNC, CrtAsyncTckTest::create,
                              Deviation.UNKNOWN_LENGTH_REQUEST_BODY_FAILS_FAST, Deviation.TEST_TRUST_BY_PEM);

    private static SdkAsyncHttpClient create(ClientSettings settings) {
        CrtTck.rejectUnsupported(settings);
        AwsCrtAsyncHttpClient.Builder builder = AwsCrtAsyncHttpClient.builder()
                                                                     .connectionTimeout(settings.connectTimeout())
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
    TransportUnderTest<SdkAsyncHttpClient> transport() {
        return TRANSPORT;
    }
}
