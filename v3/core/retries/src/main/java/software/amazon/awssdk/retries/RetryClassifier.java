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

package software.amazon.awssdk.retries;

import software.amazon.awssdk.retries.internal.DefaultRetryClassifier;

/** Classifies the retryability of one failed attempt. Implementations are pure and thread safe. */
@FunctionalInterface
public interface RetryClassifier {
    /** The classification of {@code failure}. */
    RetryClassification classify(AttemptFailure failure);

    /**
     * The AWS classification table, evaluated in order; the first matching row wins:
     * <table>
     *   <caption>Default classification</caption>
     *   <tr><th>#</th><th>Condition</th><th>Classification</th></tr>
     *   <tr><td>1</td><td>{@link AttemptFailure#requestReplayable()} is false</td><td>NON_RETRYABLE</td></tr>
     *   <tr><td>2</td><td>cancellation: an {@code HttpTransportException} of category {@code CANCELLED}, a
     *       {@code CancellationException} or an {@code InterruptedException} in the cause chain; or the whole-call
     *       timeout ({@code SdkTimeoutException} of scope {@code API_CALL})</td><td>NON_RETRYABLE</td></tr>
     *   <tr><td>3</td><td>throttling: modeled {@code @retryable(throttling: true)}; status 429; a throttling error
     *       code (for example {@code Throttling}, {@code ThrottlingException}, {@code SlowDown},
     *       {@code TooManyRequestsException}, {@code ProvisionedThroughputExceededException}); or
     *       {@code SdkServiceException.isThrottlingException()}</td><td>THROTTLING</td></tr>
     *   <tr><td>4</td><td>transient service error: modeled {@code @retryable}; status 500, 502, 503 or 504; a
     *       transient error code ({@code RequestTimeout}, {@code RequestTimeoutException},
     *       {@code PriorRequestNotComplete}, {@code InternalError}) or definite clock-skew code
     *       ({@code RequestTimeTooSkewed}, {@code RequestExpired}, {@code RequestInTheFuture}); or
     *       {@code SdkServiceException.isRetryableException()} / {@code isClockSkewException()}</td>
     *       <td>TRANSIENT</td></tr>
     *   <tr><td>5</td><td>transport: an {@code HttpTransportException} in the cause chain, by
     *       {@code HttpFailureCategory.transientByDefault()} (connection acquisition, connect, timeout, request
     *       transmission and response consumption are transient; proxy, TLS and protocol violations are not)</td>
     *       <td>TRANSIENT or NON_RETRYABLE</td></tr>
     *   <tr><td>6</td><td>an attempt timeout ({@code SdkTimeoutException} of scope {@code API_CALL_ATTEMPT}), an
     *       {@code IOException} or {@code UncheckedIOException} in the cause chain, or
     *       {@code SdkException.retryable()}</td><td>TRANSIENT</td></tr>
     *   <tr><td>7</td><td>anything else</td><td>NON_RETRYABLE</td></tr>
     * </table>
     * Rows 3 and 4 combine the Schema traits of a modeled error with the response metadata (Requirement 13.15):
     * either source can make a failure retryable, and either can mark it throttling.
     */
    static RetryClassifier defaults() {
        return DefaultRetryClassifier.INSTANCE;
    }
}
