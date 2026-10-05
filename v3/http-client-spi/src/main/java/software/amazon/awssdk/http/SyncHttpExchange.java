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

package software.amazon.awssdk.http;

/**
 * One blocking request/response exchange of a {@link SdkSyncHttpClient}. Replaces the V2 {@code ExecutableHttpRequest}
 * and {@code AbortableInputStream} pair.
 *
 * <p>Response body: {@link #execute()} returns the status and headers with a {@link HttpBody.Kind#STREAMING} body
 * (or {@link HttpBody.Kind#EMPTY} when the response has none) backed by the connection: a one-shot
 * {@link StreamingBody} whose {@link StreamingBody#openStream()} stream reads the remaining response bytes, with at
 * most {@link HttpClientCapabilities#maxBufferedBytesPerExchange()} bytes buffered ahead of the reader.
 *
 * <p>Ownership and termination, exactly one of which releases the connection:
 * <ul>
 *   <li>closing the response stream after reading it to its end returns the connection to the pool;</li>
 *   <li>closing the response stream early, {@link #abort()}, or {@link #close()} before the body is consumed discards
 *       the connection rather than draining an unbounded body;</li>
 *   <li>{@link #close()} is idempotent, may follow any of the above, and must always be called by the owner (for
 *       example in try-with-resources); it also closes a response stream that was never opened.</li>
 * </ul>
 *
 * <p>{@link #abort()} may be called from any thread, at any time, any number of times: an {@link #execute()} or
 * response read that is blocked, or starts later, fails with {@link HttpTransportException} of category
 * {@link HttpFailureCategory#CANCELLED} within {@link HttpClientCapabilities#cancellationTimeout()}. Every other method
 * is called by the owning thread only.
 */
public interface SyncHttpExchange extends AutoCloseable {
    /**
     * Transmits the request and returns the response status and headers; called at most once.
     *
     * @throws HttpTransportException if acquisition, connection, TLS, proxy, transmission or reading the headers
     *     fails, times out, or is aborted
     * @throws IllegalStateException if called again or after {@link #close()}
     */
    SdkHttpResponse execute();

    /** Aborts the exchange, discarding its connection; idempotent and thread safe. */
    void abort();

    /** Releases the exchange (see the ownership rules above); idempotent. */
    @Override
    void close();
}
