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

package software.amazon.awssdk.bridge.smithyjava.retry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

class V2RetryBridgeTest {

    @Test
    void fromV2Config_noRetryConfig_defaultsToThreeMaxAttempts() {
        SdkClientConfiguration config = SdkClientConfiguration.builder().build();

        V2RetryBridge bridge = V2RetryBridge.fromV2Config(config);

        assertThat(bridge.maxAttempts()).isEqualTo(3);
    }

    @Test
    void fromV2Config_withRetryStrategy_bridgesMaxAttempts() {
        StandardRetryStrategy strategy = StandardRetryStrategy.builder()
            .maxAttempts(5)
            .build();

        SdkClientConfiguration config = SdkClientConfiguration.builder()
            .option(SdkClientOption.RETRY_STRATEGY, strategy)
            .build();

        V2RetryBridge bridge = V2RetryBridge.fromV2Config(config);

        assertThat(bridge.maxAttempts()).isEqualTo(5);
    }

    @Test
    void fromV2Config_withRetryStrategy_singleAttempt() {
        StandardRetryStrategy strategy = StandardRetryStrategy.builder()
            .maxAttempts(1)
            .build();

        SdkClientConfiguration config = SdkClientConfiguration.builder()
            .option(SdkClientOption.RETRY_STRATEGY, strategy)
            .build();

        V2RetryBridge bridge = V2RetryBridge.fromV2Config(config);

        assertThat(bridge.maxAttempts()).isEqualTo(1);
    }

    @Test
    void fromV2Config_withLegacyRetryPolicy_bridgesNumRetries() {
        RetryPolicy policy = RetryPolicy.builder()
            .numRetries(4)
            .build();

        SdkClientConfiguration config = SdkClientConfiguration.builder()
            .option(SdkClientOption.RETRY_POLICY, policy)
            .build();

        V2RetryBridge bridge = V2RetryBridge.fromV2Config(config);

        // numRetries(4) means 4 retries + 1 initial attempt = 5 max attempts
        assertThat(bridge.maxAttempts()).isEqualTo(5);
    }

    @Test
    void fromV2Config_withRetryStrategyAndRetryPolicy_prefersRetryStrategy() {
        StandardRetryStrategy strategy = StandardRetryStrategy.builder()
            .maxAttempts(7)
            .build();
        RetryPolicy policy = RetryPolicy.builder()
            .numRetries(2)
            .build();

        SdkClientConfiguration config = SdkClientConfiguration.builder()
            .option(SdkClientOption.RETRY_STRATEGY, strategy)
            .option(SdkClientOption.RETRY_POLICY, policy)
            .build();

        V2RetryBridge bridge = V2RetryBridge.fromV2Config(config);

        // RetryStrategy takes precedence over RetryPolicy
        assertThat(bridge.maxAttempts()).isEqualTo(7);
    }

    @Test
    void isThrottlingError_http429_returnsTrue() {
        V2RetryBridge bridge = V2RetryBridge.fromV2Config(SdkClientConfiguration.builder().build());

        assertThat(bridge.isThrottlingError(429)).isTrue();
    }

    @Test
    void isThrottlingError_nonThrottlingCode_returnsFalse() {
        V2RetryBridge bridge = V2RetryBridge.fromV2Config(SdkClientConfiguration.builder().build());

        assertThat(bridge.isThrottlingError(500)).isFalse();
        assertThat(bridge.isThrottlingError(200)).isFalse();
        assertThat(bridge.isThrottlingError(403)).isFalse();
    }

    @Test
    void isTransientError_serverErrors_returnsTrue() {
        V2RetryBridge bridge = V2RetryBridge.fromV2Config(SdkClientConfiguration.builder().build());

        assertThat(bridge.isTransientError(500)).isTrue();
        assertThat(bridge.isTransientError(502)).isTrue();
        assertThat(bridge.isTransientError(503)).isTrue();
        assertThat(bridge.isTransientError(504)).isTrue();
    }

    @Test
    void isTransientError_nonTransientCode_returnsFalse() {
        V2RetryBridge bridge = V2RetryBridge.fromV2Config(SdkClientConfiguration.builder().build());

        assertThat(bridge.isTransientError(200)).isFalse();
        assertThat(bridge.isTransientError(400)).isFalse();
        assertThat(bridge.isTransientError(404)).isFalse();
        assertThat(bridge.isTransientError(429)).isFalse();
        assertThat(bridge.isTransientError(501)).isFalse();
    }

    @Test
    void isRetryableStatusCode_throttlingAndTransient_returnsTrue() {
        V2RetryBridge bridge = V2RetryBridge.fromV2Config(SdkClientConfiguration.builder().build());

        // Throttling
        assertThat(bridge.isRetryableStatusCode(429)).isTrue();
        // Transient
        assertThat(bridge.isRetryableStatusCode(500)).isTrue();
        assertThat(bridge.isRetryableStatusCode(502)).isTrue();
        assertThat(bridge.isRetryableStatusCode(503)).isTrue();
        assertThat(bridge.isRetryableStatusCode(504)).isTrue();
    }

    @Test
    void isRetryableStatusCode_nonRetryable_returnsFalse() {
        V2RetryBridge bridge = V2RetryBridge.fromV2Config(SdkClientConfiguration.builder().build());

        assertThat(bridge.isRetryableStatusCode(200)).isFalse();
        assertThat(bridge.isRetryableStatusCode(400)).isFalse();
        assertThat(bridge.isRetryableStatusCode(403)).isFalse();
        assertThat(bridge.isRetryableStatusCode(404)).isFalse();
    }
}
