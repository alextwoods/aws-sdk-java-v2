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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_PREFIX;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_SUFFIX;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ADDITIONAL_HTTP_HEADERS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CLIENT_USER_AGENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;

class UserAgentHeaderCacheTest {

    @Test
    void constants_resolvedFromClientConfiguration() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("custom"));
        UserAgentHeaderCache.Snapshot snapshot = new UserAgentHeaderCache().forConfiguration(
            SdkClientConfiguration.builder()
                                  .option(CLIENT_USER_AGENT, "sdk/1")
                                  .option(USER_AGENT_PREFIX, " pre ")
                                  .option(USER_AGENT_SUFFIX, " suf ")
                                  .option(ADDITIONAL_HTTP_HEADERS, headers)
                                  .build());
        assertThat(snapshot.constantPrefix()).isEqualTo("pre sdk/1");
        assertThat(snapshot.constantSuffix()).isEqualTo("suf");
        assertThat(snapshot.userAgentInAdditionalHeaders()).isTrue();
    }

    @Test
    void constants_noPrefixNoSuffix_prefixIsClientUserAgentAndSuffixNull() {
        UserAgentHeaderCache.Snapshot snapshot = snapshot();
        assertThat(snapshot.constantPrefix()).isEqualTo("sdk/1");
        assertThat(snapshot.constantSuffix()).isNull();
        assertThat(snapshot.userAgentInAdditionalHeaders()).isFalse();
    }

    @Test
    void forConfiguration_sameConfigurationInstance_returnsSameSnapshot() {
        UserAgentHeaderCache cache = new UserAgentHeaderCache();
        SdkClientConfiguration config = config("sdk/1");
        UserAgentHeaderCache.Snapshot first = cache.forConfiguration(config);
        assertThat(cache.forConfiguration(config)).isSameAs(first);
    }

    @Test
    void forConfiguration_differentConfiguration_rederivesConstantsAndDropsCachedHeader() {
        // The dependencies the client is built with do not carry the service's api metadata; the per-call
        // configuration does. The first call must not be stuck with constants derived from the wrong configuration.
        UserAgentHeaderCache cache = new UserAgentHeaderCache();
        AtomicInteger builds = new AtomicInteger();
        UserAgentHeaderCache.Snapshot first = cache.forConfiguration(config("sdk/1"));
        first.headerValue(Arrays.asList("A"), null, () -> "ua-" + builds.incrementAndGet());

        SdkClientConfiguration perCall = config("sdk/1 api/svc#1");
        UserAgentHeaderCache.Snapshot second = cache.forConfiguration(perCall);
        assertThat(second).isNotSameAs(first);
        assertThat(second.constantPrefix()).isEqualTo("sdk/1 api/svc#1");
        assertThat(second.headerValue(Arrays.asList("A"), null, () -> "ua-" + builds.incrementAndGet())).isEqualTo("ua-2");
        // And the new one is what later calls with the same configuration get.
        assertThat(cache.forConfiguration(perCall)).isSameAs(second);
    }

    @Test
    void forConfiguration_consistentMisses_stopsRetaining() {
        UserAgentHeaderCache cache = new UserAgentHeaderCache();
        for (int i = 0; i < UserAgentHeaderCache.MAX_CONSECUTIVE_MISSES; i++) {
            cache.forConfiguration(config("sdk/" + i));
        }
        assertThat(cache.hasSnapshot()).isTrue();
        SdkClientConfiguration tooMany = config("sdk/x");
        UserAgentHeaderCache.Snapshot fresh = cache.forConfiguration(tooMany);
        assertThat(fresh.constantPrefix()).isEqualTo("sdk/x");
        // Not retained: the same configuration again is a miss.
        assertThat(cache.forConfiguration(tooMany)).isNotSameAs(fresh);
    }

    @Test
    void forConfiguration_hitAfterMisses_resetsTheMissCount() {
        UserAgentHeaderCache cache = new UserAgentHeaderCache();
        SdkClientConfiguration steady = config("sdk/steady");
        for (int i = 0; i < UserAgentHeaderCache.MAX_CONSECUTIVE_MISSES - 1; i++) {
            cache.forConfiguration(config("sdk/" + i));
        }
        UserAgentHeaderCache.Snapshot retained = cache.forConfiguration(steady);
        assertThat(cache.forConfiguration(steady)).isSameAs(retained);
        // A run of misses after the hit is counted from zero again, so a new steady configuration is still retained.
        for (int i = 0; i < UserAgentHeaderCache.MAX_CONSECUTIVE_MISSES - 1; i++) {
            cache.forConfiguration(config("sdk/again" + i));
        }
        SdkClientConfiguration next = config("sdk/next");
        assertThat(cache.forConfiguration(next)).isSameAs(cache.forConfiguration(next));
    }

    @Test
    void headerValue_sameMetricsAndMetadata_returnsCachedInstanceWithoutRebuilding() {
        UserAgentHeaderCache.Snapshot snapshot = snapshot();
        AtomicInteger builds = new AtomicInteger();
        List<AdditionalMetadata> metadata = Arrays.asList(metadata("rb", "b"));

        String first = snapshot.headerValue(Arrays.asList("A", "B"), metadata, () -> "ua-" + builds.incrementAndGet());
        // Fresh (but equal) lists on the second call, as a real call would produce.
        String second = snapshot.headerValue(new ArrayList<>(Arrays.asList("A", "B")),
                                             Arrays.asList(metadata("rb", "b")),
                                             () -> "ua-" + builds.incrementAndGet());

        assertThat(second).isSameAs(first);
        assertThat(builds.get()).isEqualTo(1);
    }

    @Test
    void headerValue_metricsDiffer_rebuilds() {
        UserAgentHeaderCache.Snapshot snapshot = snapshot();
        AtomicInteger builds = new AtomicInteger();
        snapshot.headerValue(Arrays.asList("A", "B"), null, () -> "ua-" + builds.incrementAndGet());
        String second = snapshot.headerValue(Arrays.asList("A", "C"), null, () -> "ua-" + builds.incrementAndGet());
        assertThat(second).isEqualTo("ua-2");
        // Order matters: the header lists metrics in collection order.
        String third = snapshot.headerValue(Arrays.asList("C", "A"), null, () -> "ua-" + builds.incrementAndGet());
        assertThat(third).isEqualTo("ua-3");
    }

    @Test
    void headerValue_metadataDiffers_rebuilds() {
        UserAgentHeaderCache.Snapshot snapshot = snapshot();
        AtomicInteger builds = new AtomicInteger();
        snapshot.headerValue(Arrays.asList("A"), null, () -> "ua-" + builds.incrementAndGet());
        snapshot.headerValue(Arrays.asList("A"), Arrays.asList(metadata("rb", "b")), () -> "ua-" + builds.incrementAndGet());
        snapshot.headerValue(Arrays.asList("A"), Arrays.asList(metadata("rb", "f")), () -> "ua-" + builds.incrementAndGet());
        assertThat(builds.get()).isEqualTo(3);
    }

    @Test
    void headerValue_callerMutatesItsListAfterwards_cacheIsUnaffected() {
        UserAgentHeaderCache.Snapshot snapshot = snapshot();
        AtomicInteger builds = new AtomicInteger();
        List<String> scratch = new ArrayList<>(Arrays.asList("A"));
        String first = snapshot.headerValue(scratch, null, () -> "ua-" + builds.incrementAndGet());
        scratch.add("B");
        String again = snapshot.headerValue(Arrays.asList("A"), null, () -> "ua-" + builds.incrementAndGet());
        assertThat(again).isSameAs(first);
    }

    private static UserAgentHeaderCache.Snapshot snapshot() {
        return new UserAgentHeaderCache().forConfiguration(config("sdk/1"));
    }

    private static SdkClientConfiguration config(String clientUserAgent) {
        return SdkClientConfiguration.builder().option(CLIENT_USER_AGENT, clientUserAgent).build();
    }

    private static AdditionalMetadata metadata(String name, String value) {
        return AdditionalMetadata.builder().name(name).value(value).build();
    }
}
