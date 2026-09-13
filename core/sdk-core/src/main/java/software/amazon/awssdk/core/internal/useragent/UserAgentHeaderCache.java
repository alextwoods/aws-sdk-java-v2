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

package software.amazon.awssdk.core.internal.useragent;

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SPACE;
import static software.amazon.awssdk.utils.StringUtils.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Per-client state for building the {@code User-Agent} header: the parts of it that are constants of the client
 * configuration, and a single-entry cache of the last complete header value keyed by the two per-call inputs it was
 * built from.
 *
 * <p>The header is around 600 bytes and, for a given client, almost always identical from one call to the next: the
 * prefix is a function of the client configuration and the business metrics and body/transformer metadata that follow
 * it are decided by the client configuration and the operation shape. Rebuilding it per call was a {@code StringBuilder},
 * a {@code String.join} and a {@code toString} for a value that had not changed. The cache compares the metric list and
 * the metadata list by equality (a handful of short, mostly interned strings) and hands back the previous header when
 * they match; a request that supplies its own {@code ApiName}s bypasses the cache, since those are per request by
 * definition.
 *
 * <p>The pipeline is assembled per call from a {@link software.amazon.awssdk.core.internal.http.HttpClientDependencies}
 * carrying that call's {@link SdkClientConfiguration}, which is the client's own configuration unless the request has
 * plugins, in which case it is a fresh instance with them applied. The constants are therefore keyed on the identity of
 * the configuration they were derived from and re-derived when a different one shows up. A client whose requests carry
 * plugins on every call would miss on every call, so, like
 * {@link software.amazon.awssdk.core.internal.http.auth.AuthSchemeResolutionCache}, the cache stops storing after
 * {@link #MAX_CONSECUTIVE_MISSES} misses in a row and such a client pays only what it paid before.
 *
 * <p>Racing threads compute equal values for equal inputs, so a lost write costs one rebuild.
 */
@SdkInternalApi
public final class UserAgentHeaderCache {
    public static final String HEADER_USER_AGENT = "User-Agent";

    static final int MAX_CONSECUTIVE_MISSES = 8;

    private static final Logger LOG = Logger.loggerFor(UserAgentHeaderCache.class);

    private volatile Snapshot snapshot;

    /**
     * Written only on a miss (at most {@link #MAX_CONSECUTIVE_MISSES} times in a row) and on the first hit after a
     * miss, so the steady-state hit path does one volatile read of it and never writes.
     */
    private volatile int consecutiveMisses;

    /**
     * The constants and cached header for this call's client configuration: the retained snapshot if it was derived
     * from the same configuration instance, otherwise a fresh one (retained unless this client misses consistently).
     */
    public Snapshot forConfiguration(SdkClientConfiguration clientConfig) {
        Snapshot cached = snapshot;
        if (cached != null && cached.clientConfig == clientConfig) {
            if (consecutiveMisses != 0) {
                consecutiveMisses = 0;
            }
            return cached;
        }
        Snapshot fresh = new Snapshot(clientConfig);
        int misses = consecutiveMisses;
        if (misses < MAX_CONSECUTIVE_MISSES) {
            consecutiveMisses = misses + 1;
            snapshot = fresh;
        }
        return fresh;
    }

    /**
     * Test hook: whether a snapshot is currently retained.
     */
    boolean hasSnapshot() {
        return snapshot != null;
    }

    /**
     * The user-agent constants of one client configuration, plus the last header value built from them.
     */
    public static final class Snapshot {
        private final SdkClientConfiguration clientConfig;
        private final String constantPrefix;
        private final String constantSuffix;
        private final boolean userAgentInAdditionalHeaders;

        private volatile Entry entry;

        Snapshot(SdkClientConfiguration clientConfig) {
            this.clientConfig = clientConfig;
            this.constantPrefix = buildConstantPrefix(clientConfig);
            String suffix = trim(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX));
            this.constantSuffix = StringUtils.isEmpty(suffix) ? null : suffix;
            Map<String, List<String>> additionalHeaders = clientConfig.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS);
            this.userAgentInAdditionalHeaders = additionalHeaders != null && additionalHeaders.containsKey(HEADER_USER_AGENT);
        }

        private static String buildConstantPrefix(SdkClientConfiguration clientConfig) {
            String clientUserAgent = clientConfig.option(SdkClientOption.CLIENT_USER_AGENT);
            if (clientUserAgent == null) {
                LOG.warn(() -> "Client user agent configuration is missing, so request user agent will be incomplete.");
                clientUserAgent = "";
            }
            String userPrefix = trim(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_PREFIX));
            if (StringUtils.isEmpty(userPrefix)) {
                return clientUserAgent;
            }
            return userPrefix + SPACE + clientUserAgent;
        }

        /**
         * The client-level user agent (with any configured prefix) that every header value starts with.
         */
        public String constantPrefix() {
            return constantPrefix;
        }

        /**
         * The configured, trimmed user agent suffix, or {@code null} if there is none.
         */
        public String constantSuffix() {
            return constantSuffix;
        }

        /**
         * Whether the client's additional HTTP headers already carry a {@code User-Agent}, in which case the stage does
         * not set one.
         */
        public boolean userAgentInAdditionalHeaders() {
            return userAgentInAdditionalHeaders;
        }

        /**
         * The header value for these business metrics and this metadata: the cached one if they match the last call,
         * otherwise {@code build} is invoked and its result cached. The lists are copied when cached, since the callers'
         * lists are per-call scratch.
         */
        public String headerValue(List<String> businessMetrics, List<AdditionalMetadata> metadata, Supplier<String> build) {
            Entry cached = entry;
            if (cached != null
                && cached.businessMetrics.equals(businessMetrics)
                && Objects.equals(cached.metadata, metadata)) {
                return cached.value;
            }
            String value = build.get();
            entry = new Entry(new ArrayList<>(businessMetrics), metadata == null ? null : new ArrayList<>(metadata), value);
            return value;
        }
    }

    private static final class Entry {
        private final List<String> businessMetrics;
        private final List<AdditionalMetadata> metadata;
        private final String value;

        private Entry(List<String> businessMetrics, List<AdditionalMetadata> metadata, String value) {
            this.businessMetrics = businessMetrics;
            this.metadata = metadata;
            this.value = value;
        }
    }
}
