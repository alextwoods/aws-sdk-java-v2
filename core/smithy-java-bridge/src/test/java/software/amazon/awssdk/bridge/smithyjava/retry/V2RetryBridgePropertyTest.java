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

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Property-based tests for {@link V2RetryBridge} verifying retry configuration bridge preservation.
 *
 * <p><b>Validates: Requirements 7.2</b>
 *
 * <p><b>Property 9: Retry Configuration Bridge Preservation</b> — For any v2 RetryStrategy with a
 * given maxAttempts() value, the V2RetryBridge SHALL produce a smithy-java retry configuration with
 * the same maximum attempt count.
 */
class V2RetryBridgePropertyTest {

    /**
     * Verifies that for any v2 RetryStrategy with a random maxAttempts value,
     * the V2RetryBridge preserves the same maximum attempt count.
     *
     * <p><b>Validates: Requirements 7.2</b>
     */
    @Property(tries = 100)
    void retryStrategy_maxAttemptsIsPreserved(@ForAll("maxAttemptsValues") int maxAttempts) {
        RetryStrategy v2Strategy = StandardRetryStrategy.builder()
                                                        .maxAttempts(maxAttempts)
                                                        .build();

        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.RETRY_STRATEGY, v2Strategy)
                                                              .build();

        V2RetryBridge bridge = V2RetryBridge.fromV2Config(config);

        assertThat(bridge.maxAttempts()).isEqualTo(maxAttempts);
    }

    @Provide
    Arbitrary<Integer> maxAttemptsValues() {
        // Range 1–100 covers typical retry counts and ensures 100 random iterations
        return Arbitraries.integers().between(1, 100);
    }
}
