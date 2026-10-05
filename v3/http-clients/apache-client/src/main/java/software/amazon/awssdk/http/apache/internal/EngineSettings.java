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

package software.amazon.awssdk.http.apache.internal;

import java.time.Duration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

/**
 * The fully resolved configuration of an {@link ApacheEngine}: every default applied, every value validated.
 * {@code connectionTimeToLive} zero means unlimited; {@code httpProxy}/{@code httpsProxy} are {@code null} when
 * requests of that scheme go direct. {@link #toString()} renders no key material or proxy credentials.
 */
public record EngineSettings(Duration connectionTimeout,
                             Duration socketTimeout,
                             Duration connectionAcquisitionTimeout,
                             Duration connectionMaxIdleTime,
                             Duration connectionTimeToLive,
                             int maxConnections,
                             boolean tcpKeepAlive,
                             boolean useIdleConnectionReaper,
                             boolean expectContinueEnabled,
                             KeyManager[] keyManagers,
                             TrustManager[] trustManagers,
                             boolean trustAllCertificates,
                             ProxySettings httpProxy,
                             ProxySettings httpsProxy) {

    /** The proxy for a target of {@code scheme} and {@code host}, or {@code null} to connect directly. */
    public ProxySettings proxyFor(String scheme, String host) {
        ProxySettings proxy = "https".equalsIgnoreCase(scheme) ? httpsProxy : httpProxy;
        return proxy != null && proxy.appliesTo(host) ? proxy : null;
    }

    @Override
    public String toString() {
        return "EngineSettings[connectionTimeout=" + connectionTimeout + ", socketTimeout=" + socketTimeout
               + ", connectionAcquisitionTimeout=" + connectionAcquisitionTimeout + ", connectionMaxIdleTime="
               + connectionMaxIdleTime + ", connectionTimeToLive=" + connectionTimeToLive + ", maxConnections="
               + maxConnections + ", tcpKeepAlive=" + tcpKeepAlive + ", useIdleConnectionReaper="
               + useIdleConnectionReaper + ", expectContinueEnabled=" + expectContinueEnabled + ", keyManagers="
               + (keyManagers != null) + ", trustManagers=" + (trustManagers != null) + ", trustAllCertificates="
               + trustAllCertificates + ", httpProxy=" + httpProxy + ", httpsProxy=" + httpsProxy + "]";
    }
}
