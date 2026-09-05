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
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.error.ErrorFault;
import software.amazon.smithy.java.retries.api.RetrySafety;

/**
 * Carries a failure that v2 would retry but whose own type cannot say so.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@code CallException} stores its retry safety in a mutable field with a setter, which is how
 * {@link V2ErrorEnricher} stamps v2's classification onto an error before the retry strategy reads it.
 * Two kinds of failure cannot be stamped that way:
 *
 * <ul>
 *   <li>Types that <em>override the getter with a constant</em>. {@code TransportException} and
 *       {@code ConnectTimeoutException} both {@code return RetrySafety.NO} unconditionally, ignoring
 *       whatever the field holds, and every type
 *       {@code ClientTransport.remapExceptions} produces descends from the first of those.
 *   <li>Types with no retry field at all. smithy's {@code SerializationException} is a plain
 *       {@code RuntimeException}, not a {@code CallException}, so it reaches the retry gate carrying no
 *       {@code RetryInfo} whatsoever. This is the common case in practice: it is what a truncated or
 *       otherwise unparseable response body arrives as, which v2 retries.
 * </ul>
 *
 * <p>This class is the substitution they need — a {@code CallException} that reports
 * {@code RetrySafety.YES} and holds the real failure. {@link V2ErrorEnricher} throws it in place of
 * the original, which the smithy pipeline accepts as the attempt's error, and the client boundary
 * unwraps {@link #original()} so the caller never sees this type.
 *
 * <p><b>This does not rescue transport failures</b>, despite the first bullet describing them. The
 * enricher's hook and smithy-java's retry loop both live inside {@code ClientPipeline.deserialize},
 * which a failed send never reaches, so on that path there is nothing to substitute the error
 * <em>for</em>. Throwing this from the transport instead was measured and made no difference. See
 * {@code compatability_issues.md} section 3.6; the reachable half of the first bullet is a transport
 * error that surfaces during body reading, which is a {@code SerializationException} by then anyway.
 */
@SdkProtectedApi
public final class V2RetryableError extends CallException {

    private final transient RuntimeException original;

    /**
     * @param original the failure v2 would retry. Kept as the cause so a stack trace is complete, and
     *                 separately as a field so the client boundary can rethrow the original rather
     *                 than something wrapping it.
     */
    public V2RetryableError(RuntimeException original) {
        // SERVER, not CLIENT: v2 only reaches this classification for failures that are worth another
        // attempt against the service, and a CLIENT fault would be a contradiction.
        super(original.getMessage(), original, ErrorFault.SERVER);
        this.original = original;
        isRetrySafe(RetrySafety.YES);
    }

    /** The failure this was substituted for; what a v2 caller should ultimately see. */
    public RuntimeException original() {
        return original;
    }

    @Override
    public RetrySafety isRetrySafe() {
        // Explicit rather than inherited: the whole point of this class is that inheriting the getter
        // is what went wrong, so it must not be possible for a future superclass change to make this
        // report anything else.
        return RetrySafety.YES;
    }
}
