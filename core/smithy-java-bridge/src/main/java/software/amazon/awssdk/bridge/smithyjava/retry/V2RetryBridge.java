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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Bridges AWS SDK v2 retry configuration ({@link RetryStrategy} or legacy {@link RetryPolicy})
 * to a retry configuration that the smithy-java pipeline can consume.
 *
 * <p>The bridge extracts:
 * <ul>
 *   <li><b>Maximum attempts</b> from the v2 retry config</li>
 *   <li><b>Error classification</b> for throttling and transient server errors</li>
 * </ul>
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@link SdkClientOption#RETRY_STRATEGY} — modern v2 {@link RetryStrategy}</li>
 *   <li>{@link SdkClientOption#RETRY_POLICY} — legacy v2 {@link RetryPolicy}</li>
 *   <li>Default: 3 max attempts (standard retry mode)</li>
 * </ol>
 */
@SdkInternalApi
public final class V2RetryBridge {

    /**
     * Default maximum attempts when no explicit retry config is present.
     * Matches the standard retry mode default (3 attempts = initial + 2 retries).
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    /**
     * HTTP status codes classified as throttling errors.
     */
    private static final Set<Integer> THROTTLING_STATUS_CODES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(429))
    );

    /**
     * HTTP status codes classified as transient server errors.
     */
    private static final Set<Integer> TRANSIENT_ERROR_STATUS_CODES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(500, 502, 503, 504))
    );

    private final int maxAttempts;

    private V2RetryBridge(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Creates a {@link V2RetryBridge} from the given v2 client configuration.
     *
     * <p>The method attempts to extract retry configuration in the following order:
     * <ol>
     *   <li>If {@link SdkClientOption#RETRY_STRATEGY} is set, use its {@code maxAttempts()}</li>
     *   <li>If {@link SdkClientOption#RETRY_POLICY} is set, use {@code numRetries() + 1} as max attempts</li>
     *   <li>Otherwise, default to {@value #DEFAULT_MAX_ATTEMPTS} max attempts</li>
     * </ol>
     *
     * @param config the v2 client configuration
     * @return a new {@link V2RetryBridge} instance with the resolved retry configuration
     */
    public static V2RetryBridge fromV2Config(SdkClientConfiguration config) {
        // Try modern RetryStrategy first
        RetryStrategy retryStrategy = config.option(SdkClientOption.RETRY_STRATEGY);
        if (retryStrategy != null) {
            return new V2RetryBridge(retryStrategy.maxAttempts());
        }

        // Fall back to legacy RetryPolicy
        RetryPolicy retryPolicy = config.option(SdkClientOption.RETRY_POLICY);
        if (retryPolicy != null && retryPolicy.numRetries() != null) {
            // RetryPolicy.numRetries() is the number of retries (not including the initial attempt)
            return new V2RetryBridge(retryPolicy.numRetries() + 1);
        }

        // Default to standard retry mode
        return new V2RetryBridge(DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Returns the maximum number of attempts (initial attempt + retries).
     *
     * @return the configured maximum attempts
     */
    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * Determines if the given HTTP status code indicates a throttling error.
     * Throttling errors are retryable and use the throttling backoff strategy.
     *
     * @param statusCode the HTTP response status code
     * @return {@code true} if the status code indicates throttling (HTTP 429)
     */
    public boolean isThrottlingError(int statusCode) {
        return THROTTLING_STATUS_CODES.contains(statusCode);
    }

    /**
     * Determines if the given HTTP status code indicates a transient server error.
     * Transient errors are retryable and use the standard backoff strategy.
     *
     * @param statusCode the HTTP response status code
     * @return {@code true} if the status code indicates a transient server error (HTTP 500, 502, 503, 504)
     */
    public boolean isTransientError(int statusCode) {
        return TRANSIENT_ERROR_STATUS_CODES.contains(statusCode);
    }

    /**
     * Determines if the given HTTP status code is retryable (either throttling or transient).
     *
     * @param statusCode the HTTP response status code
     * @return {@code true} if the status code is classified as retryable
     */
    public boolean isRetryableStatusCode(int statusCode) {
        return isThrottlingError(statusCode) || isTransientError(statusCode);
    }
}
