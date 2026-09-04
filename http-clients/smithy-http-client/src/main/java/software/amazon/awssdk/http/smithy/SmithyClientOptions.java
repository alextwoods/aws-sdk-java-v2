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

package software.amazon.awssdk.http.smithy;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;

/**
 * Applies resolved {@link SdkHttpConfigurationOption} values to a smithy {@link HttpClient.Builder}.
 *
 * <p>Kept separate so the synchronous and asynchronous clients configure smithy identically; a difference
 * between them would make their measurements incomparable.
 */
@SdkInternalApi
final class SmithyClientOptions {

    private SmithyClientOptions() {
    }

    static HttpClient.Builder apply(HttpClient.Builder builder, Boolean http2Enabled, AttributeMap options) {
        applyIfPositive(options.get(SdkHttpConfigurationOption.READ_TIMEOUT), builder::readTimeout);
        applyIfPositive(options.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT), builder::connectTimeout);
        applyIfPositive(options.get(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT), builder::acquireTimeout);
        applyIfPositive(options.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT), builder::maxIdleTime);

        Integer maxConnections = options.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);
        if (maxConnections != null && maxConnections > 0) {
            // The SDK expresses one pool-wide ceiling. smithy has both a total and a per-route ceiling, and a
            // per-route limit below the total would throttle the single-endpoint case the SDK cares about, so
            // both are set to the same value.
            builder.maxTotalConnections(maxConnections);
            builder.maxConnectionsPerRoute(maxConnections);
        }

        if (http2Enabled != null) {
            builder.httpVersionPolicy(http2Enabled ? HttpVersionPolicy.AUTOMATIC
                                                   : HttpVersionPolicy.ENFORCE_HTTP_1_1);
        }

        SSLContext sslContext = sslContext(options);
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }

        // Note on TRUST_ALL_CERTIFICATES: the trust manager above makes any issuer acceptable, but hostname
        // verification stays on. smithy's JdkTlsProvider forces setEndpointIdentificationAlgorithm("HTTPS")
        // after copying any caller-supplied SSLParameters, so passing parameters here cannot switch it off.
        // Disabling it would mean supplying a custom TlsProvider, and a provider that did not report
        // supportsEpoll() would silently drop this client onto its slower non-epoll transport. So trustAll is
        // honoured for issuer trust only, and a certificate whose name does not match the host still fails.

        return builder;
    }

    private static void applyIfPositive(Duration duration, java.util.function.Consumer<Duration> setter) {
        // The SDK uses zero to mean "no timeout"; smithy rejects non-positive durations, so leave its default
        // in place rather than passing a value it will refuse.
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            setter.accept(duration);
        }
    }

    /**
     * Builds an {@link SSLContext} from the SDK's TLS options, or null to leave smithy's default in place.
     */
    private static SSLContext sslContext(AttributeMap options) {
        Boolean trustAll = options.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES);
        TlsTrustManagersProvider trustProvider = options.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER);
        TlsKeyManagersProvider keyProvider = options.get(SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER);

        boolean trustAllEnabled = Boolean.TRUE.equals(trustAll);

        // Trusting everything and supplying a specific trust store are contradictory instructions; silently
        // honouring one would quietly weaken or ignore the caller's TLS intent.
        if (trustAllEnabled && trustProvider != null) {
            throw new IllegalArgumentException("A TLS trust managers provider cannot be given at the same time as "
                                               + "trustAll, because they specify conflicting trust behaviour.");
        }

        KeyManager[] keyManagers = keyProvider == null ? null : keyProvider.keyManagers();

        if (!trustAllEnabled && trustProvider == null && keyManagers == null) {
            return null;
        }

        TrustManager[] trustManagers;
        if (trustAllEnabled) {
            trustManagers = new TrustManager[] {TrustAllManager.INSTANCE};
        } else if (trustProvider != null) {
            trustManagers = trustProvider.trustManagers();
        } else {
            trustManagers = null;
        }

        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, null);
            return context;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Could not create an SSLContext for the smithy HTTP client", e);
        }
    }

    /**
     * Accepts every certificate. Only reachable through
     * {@link SdkHttpConfigurationOption#TRUST_ALL_CERTIFICATES}, which the SDK documents as test-only.
     */
    private static final class TrustAllManager implements X509TrustManager {
        private static final TrustAllManager INSTANCE = new TrustAllManager();

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
