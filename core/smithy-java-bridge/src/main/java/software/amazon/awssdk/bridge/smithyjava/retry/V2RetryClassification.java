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

import java.io.IOException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.retries.api.RetrySafety;

/**
 * Derives smithy-java {@link software.amazon.smithy.java.retries.api.RetryInfo} from an AWS SDK v2
 * exception, using v2's own notion of what is retryable.
 *
 * <h2>Why this is needed</h2>
 *
 * <p>{@code SdkRetryStrategy.of(v2Strategy)} — the adapter that lets a v2 {@code RetryStrategy} drive
 * smithy's pipeline — deliberately <em>replaces</em> the delegate's {@code retryOnException} and
 * {@code treatAsThrottling} predicates with ones that read smithy's {@code RetryInfo}. v2's own
 * conditions ({@code RetryOnStatusCodeCondition}, {@code RetryOnExceptionsCondition},
 * {@code RetryOnClockSkewCondition}, and {@code AwsRetryStrategy}'s error-code lists) never run.
 *
 * <p>Separately, smithy's own model-driven classifier ({@code ApplyModelRetryInfoPlugin}) reads the
 * {@code @retryable}, {@code @readonly}, and {@code @idempotent} traits off the operation and error
 * schemas — and the schemas generated on this branch carry no traits at all.
 *
 * <p>So neither side classifies anything on its own. This class closes the gap by computing
 * {@code RetryInfo} from the v2 exception, reproducing v2's <em>default</em> classification. It does
 * not reproduce customer-supplied retry conditions; see {@code compatability_issues.md} section 3.2.
 */
@SdkProtectedApi
public final class V2RetryClassification {

    private V2RetryClassification() {
    }

    /**
     * Stamps retry information derived from {@code v2} onto the smithy exception that carries it.
     *
     * @param target the smithy exception the retry strategy will inspect.
     * @param v2     the v2 exception to classify.
     */
    public static void applyTo(CallException target, SdkException v2) {
        target.isRetrySafe(isRetrySafe(v2));
        target.isThrottle(isThrottle(v2));
    }

    /**
     * v2's default retry classification, in the order {@code AwsRetryStrategy} applies it.
     *
     * @param v2 exception to classify.
     * @return whether smithy is allowed to retry it.
     */
    public static RetrySafety isRetrySafe(SdkException v2) {
        if (v2 instanceof RetryableException) {
            return RetrySafety.YES;
        }
        if (v2.retryable()) {
            return RetrySafety.YES;
        }
        if (v2 instanceof SdkServiceException service) {
            if (service.isThrottlingException()
                    || service.isClockSkewException()
                    || service.isRetryableException()
                    || isRetryableStatusCode(service.statusCode())) {
                return RetrySafety.YES;
            }
            // A modeled service error that v2 would not retry is definitively not retryable: retrying
            // it would burn an attempt and a retry token for nothing.
            return RetrySafety.NO;
        }
        if (hasCause(v2, IOException.class)) {
            return RetrySafety.YES;
        }
        return RetrySafety.MAYBE;
    }

    /**
     * @param v2 exception to classify.
     * @return true if v2 would count this against the throttling side of the retry token bucket.
     */
    public static boolean isThrottle(SdkException v2) {
        return v2 instanceof SdkServiceException service && service.isThrottlingException();
    }

    // Mirrors SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES.
    private static boolean isRetryableStatusCode(int statusCode) {
        switch (statusCode) {
            case HttpStatusCode.INTERNAL_SERVER_ERROR:
            case HttpStatusCode.BAD_GATEWAY:
            case HttpStatusCode.SERVICE_UNAVAILABLE:
            case HttpStatusCode.GATEWAY_TIMEOUT:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }
}
