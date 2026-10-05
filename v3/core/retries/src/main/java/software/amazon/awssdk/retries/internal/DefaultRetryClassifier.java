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

package software.amazon.awssdk.retries.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.exception.SdkTimeoutException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.ModeledRetryTraits;
import software.amazon.awssdk.retries.RetryClassification;
import software.amazon.awssdk.retries.RetryClassifier;

/** The table of {@link RetryClassifier#defaults()}; see its documentation for the rows. */
public final class DefaultRetryClassifier implements RetryClassifier {
    public static final DefaultRetryClassifier INSTANCE = new DefaultRetryClassifier();

    /** AWS error codes that indicate throttling. */
    public static final Set<String> THROTTLING_ERROR_CODES = Set.of(
        "Throttling", "ThrottlingException", "ThrottledException", "RequestThrottledException",
        "TooManyRequestsException", "ProvisionedThroughputExceededException", "TransactionInProgressException",
        "RequestLimitExceeded", "BandwidthLimitExceeded", "LimitExceededException", "RequestThrottled", "SlowDown",
        "EC2ThrottledException");

    /** AWS error codes of transient service errors, including definite clock skew. */
    public static final Set<String> TRANSIENT_ERROR_CODES = Set.of(
        "RequestTimeout", "RequestTimeoutException", "PriorRequestNotComplete", "InternalError",
        "RequestTimeTooSkewed", "RequestExpired", "RequestInTheFuture");

    /** HTTP status code of throttled requests. */
    public static final int THROTTLING_STATUS_CODE = 429;

    /** HTTP status codes of transient service errors. */
    public static final Set<Integer> TRANSIENT_STATUS_CODES = Set.of(500, 502, 503, 504);

    /** Bound on the cause-chain walk (the chain may be cyclic). */
    private static final int MAX_CAUSE_DEPTH = 32;

    private DefaultRetryClassifier() {
    }

    @Override
    public RetryClassification classify(AttemptFailure failure) {
        if (!failure.requestReplayable() || isTerminal(failure.failure())) {
            return RetryClassification.NON_RETRYABLE;
        }
        Throwable t = failure.failure();
        ModeledRetryTraits traits = failure.modeledTraits();
        int status = failure.statusCode().orElse(0);
        String code = failure.errorCode().orElse(null);
        SdkServiceException service = t instanceof SdkServiceException s ? s : null;

        if (traits.throttling()
            || status == THROTTLING_STATUS_CODE
            || code != null && THROTTLING_ERROR_CODES.contains(code)
            || service != null && service.isThrottlingException()) {
            return RetryClassification.THROTTLING;
        }
        if (traits.retryable()
            || TRANSIENT_STATUS_CODES.contains(status)
            || code != null && TRANSIENT_ERROR_CODES.contains(code)
            || service != null && (service.isRetryableException() || service.isClockSkewException())) {
            return RetryClassification.TRANSIENT;
        }
        HttpTransportException transport = find(t, HttpTransportException.class);
        if (transport != null) {
            return transport.category().transientByDefault() ? RetryClassification.TRANSIENT
                                                             : RetryClassification.NON_RETRYABLE;
        }
        // An API_CALL timeout anywhere in the chain was rejected by isTerminal, so this is an attempt timeout.
        if (find(t, SdkTimeoutException.class) != null
            || find(t, IOException.class) != null
            || find(t, UncheckedIOException.class) != null
            || t instanceof SdkException sdk && sdk.retryable()) {
            return RetryClassification.TRANSIENT;
        }
        return RetryClassification.NON_RETRYABLE;
    }

    /**
     * Whether {@code failure} ends the call whatever the classifier or retry predicates say: cancellation or the
     * whole-call timeout.
     */
    public static boolean isTerminal(Throwable failure) {
        int depth = 0;
        for (Throwable t = failure; t != null && depth < MAX_CAUSE_DEPTH; t = t.getCause(), depth++) {
            if (t instanceof CancellationException || t instanceof InterruptedException) {
                return true;
            }
            if (t instanceof HttpTransportException transport
                && transport.category() == HttpFailureCategory.CANCELLED) {
                return true;
            }
            if (t instanceof SdkTimeoutException timeout && timeout.scope() == SdkTimeoutException.Scope.API_CALL) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Throwable> T find(Throwable failure, Class<T> type) {
        int depth = 0;
        for (Throwable t = failure; t != null && depth < MAX_CAUSE_DEPTH; t = t.getCause(), depth++) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "RetryClassifier.defaults()";
    }
}
