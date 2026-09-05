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

package software.amazon.awssdk.bridge.smithyjava.error;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.bridge.smithyjava.retry.V2RetryClassification;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.error.ErrorFault;

/**
 * Carries a v2 exception built from an error response that matched no entry in the operation's
 * {@code TypeRegistry} — the unmodeled counterpart to {@link V2ModeledError}.
 *
 * <h2>Why this exists</h2>
 *
 * <p>smithy-java's error deserialization has exactly two outcomes: the {@code __type} on the wire
 * resolves to a registered error shape and a modeled exception is built, or it does not and the
 * protocol throws a bare {@link CallException} whose only content is a synthesized message
 * ({@code "Server HTTP/1.1 503 response from operation ..."}). The second outcome is common in
 * practice, not exceptional — a 503 from a load balancer has no AWS error payload at all, an empty
 * 5xx has no body to parse, and an error the service models but the <em>operation</em> does not
 * declare is absent from a per-operation registry.
 *
 * <p>Left alone, that path loses everything a v2 caller depends on. The status code, request ID and
 * {@code awsErrorDetails} never reach the exception, so {@code statusCode()} reads 0 and
 * {@code requestId()} reads null. Worse, {@link V2RetryClassification} is driven off the v2 exception,
 * so an unclassified error is never stamped {@code RetrySafety.YES} — and
 * {@code SdkRetryStrategy.of(...)} retries only on {@code YES}. A 503 would therefore be returned to
 * the caller on the first attempt, silently, where v2 retries it.
 *
 * <p>This class closes that gap. {@link V2ErrorEnricher} builds the service's base exception from the
 * HTTP response, wraps it here, and throws this in place of the bare {@code CallException}; the smithy
 * pipeline treats a replacement error thrown from {@code modifyBeforeAttemptCompletion} as the
 * attempt's error, so the retry strategy sees the classification stamped on it. The client boundary
 * then unwraps {@link #v2Exception()}, exactly as it does for a modeled error.
 *
 * <p>What this does <em>not</em> recover is the concrete exception <em>type</em>: an error the service
 * models but the operation does not declare still arrives as the service's base exception rather than
 * as its own class. See {@code compatability_issues.md} section 1.3.
 */
@SdkProtectedApi
public final class V2UnmodeledError extends CallException {

    private final transient AwsServiceException v2;

    /**
     * @param v2    the v2 exception built from the error response.
     * @param cause the smithy exception this replaces, kept so the smithy-side detail is not lost. Not
     *              narrowed to {@code CallException}: an error response whose payload failed to parse
     *              arrives as a plain {@code SerializationException}, and that case needs this
     *              treatment most, since it is the one with no v2 metadata of its own at all.
     */
    public V2UnmodeledError(AwsServiceException v2, Throwable cause) {
        // The fault is derived from the status code rather than copied from the cause: the cause's fault
        // is what smithy inferred without a parsed payload, and ofHttpStatusCode is the same rule.
        super(v2.getMessage(), cause, ErrorFault.ofHttpStatusCode(v2.statusCode()));
        this.v2 = v2;
        V2RetryClassification.applyTo(this, v2);
    }

    /**
     * The v2 exception the client boundary rethrows.
     *
     * @return the v2 exception.
     */
    public AwsServiceException v2Exception() {
        return v2;
    }
}
