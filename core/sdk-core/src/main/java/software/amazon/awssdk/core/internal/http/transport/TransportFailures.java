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

package software.amazon.awssdk.core.internal.http.transport;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Maps HTTP client failures to the exceptions the legacy pipeline raises for them.
 *
 * <p>The legacy pipeline lets the raw failure reach {@code RetryableStageHelper.setLastException}, which unwraps
 * {@link CompletionException}s, keeps {@link SdkException}s, and wraps anything else in
 * {@code SdkClientException("Unable to execute HTTP request: " + message, cause)}. The transports apply the same mapping
 * themselves, so the message, the type and the cause (which drives the retryable classification) are unchanged.
 */
@SdkInternalApi
final class TransportFailures {
    private static final int MAX_UNWRAP_DEPTH = 100;

    private TransportFailures() {
    }

    /**
     * Wraps a failure of the synchronous HTTP client, which can only throw {@code IOException}s as checked exceptions.
     */
    static SdkClientException httpRequestFailure(Exception failure) {
        return SdkClientException.create("Unable to execute HTTP request: " + failure.getMessage(), failure);
    }

    /**
     * Maps a failure of the asynchronous HTTP client: {@link CompletionException}s are unwrapped, {@link SdkException}s
     * and {@link Error}s are returned unchanged, and any other failure is wrapped as in
     * {@link #httpRequestFailure(Exception)}.
     */
    static Throwable asyncFailure(Throwable failure) {
        Throwable cause = failure;
        for (int i = 0; i < MAX_UNWRAP_DEPTH && cause instanceof CompletionException && cause.getCause() != null; i++) {
            cause = cause.getCause();
        }
        if (cause instanceof SdkException || cause instanceof Error) {
            return cause;
        }
        return SdkClientException.create("Unable to execute HTTP request: " + cause.getMessage(), cause);
    }
}
