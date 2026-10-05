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

import java.net.URI;
import software.amazon.awssdk.http.crt.ProxyConfiguration;

/** Settings mapping shared by the CRT sync and async executions. */
final class CrtTck {
    /** The CRT stream window (and so the declared buffer bound) used by the TCK executions. */
    static final long READ_BUFFER = 256 * 1024;

    private CrtTck() {
    }

    static void rejectUnsupported(ClientSettings settings) {
        if (settings.readTimeout() != null) {
            throw new IllegalArgumentException("aws-crt-client has no READ_TIMEOUT");
        }
    }

    static ProxyConfiguration proxy(URI endpoint) {
        return ProxyConfiguration.builder().scheme(endpoint.getScheme()).host(endpoint.getHost())
                                 .port(endpoint.getPort()).build();
    }
}
