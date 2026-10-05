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
import java.time.Duration;

/**
 * The transport-neutral settings a TCK check asks a {@link TransportUnderTest} factory to apply. {@code null} means
 * "leave the transport default". A check only sets a value after gating on the matching declared capability, so a
 * factory may reject a value its transport does not support.
 *
 * @param connectTimeout {@code CONNECTION_TIMEOUT}; always set (short) so refused/unreachable checks stay fast
 * @param readTimeout {@code READ_TIMEOUT}
 * @param trust the test certificate to trust ({@code TLS_TRUST_MANAGERS} or a documented equivalent)
 * @param proxy an {@code http://host:port} proxy endpoint ({@code PROXY})
 * @param maxConnections {@code MAX_CONNECTIONS}
 */
record ClientSettings(Duration connectTimeout, Duration readTimeout, TckTls trust, URI proxy, Integer maxConnections) {

    static ClientSettings defaults() {
        return new ClientSettings(Duration.ofSeconds(2), null, null, null, null);
    }

    ClientSettings readTimeout(Duration timeout) {
        return new ClientSettings(connectTimeout, timeout, trust, proxy, maxConnections);
    }

    ClientSettings connectTimeout(Duration timeout) {
        return new ClientSettings(timeout, readTimeout, trust, proxy, maxConnections);
    }

    ClientSettings trust(TckTls tls) {
        return new ClientSettings(connectTimeout, readTimeout, tls, proxy, maxConnections);
    }

    ClientSettings proxy(URI endpoint) {
        return new ClientSettings(connectTimeout, readTimeout, trust, endpoint, maxConnections);
    }

    ClientSettings maxConnections(int max) {
        return new ClientSettings(connectTimeout, readTimeout, trust, proxy, max);
    }
}
