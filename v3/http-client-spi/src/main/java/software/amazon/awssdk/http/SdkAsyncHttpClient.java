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
 * A non-blocking HTTP transport (Requirement 12.1; design, "HTTP SPI"). Separate from {@link SdkSyncHttpClient};
 * V2 name kept.
 *
 * <p>Execution: {@link #execute} starts the exchange and returns at once. The request body is read from
 * {@link SdkHttpRequest#body()}: buffered bytes directly, a {@link HttpBody.Kind#STREAMING} body by subscribing to its
 * {@link StreamingBody} publisher once per exchange and requesting chunks only as the connection can write them
 * (Requirement 12.5), with at most {@link HttpClientCapabilities#maxBufferedBytesPerExchange()} bytes buffered. The
 * response is delivered to the {@link AsyncHttpResponseHandler}.
 *
 * <p>Threading: {@link #execute} never blocks. Handler methods and response-body signals may run on a transport
 * event loop; the pipeline hands them to its completion executor before running codec, interceptor or user code, so
 * a handler must not block. The transport never calls user code other than the handler and the subscribers it is
 * given.
 *
 * <p>Ownership:
 * <ul>
 *   <li>The client is closed by whoever built it (see {@code ResourceOwnership}); {@link #close()} is idempotent and
 *       cancels exchanges in flight with {@link CancellationReason#CLIENT_CLOSED}.</li>
 *   <li>The caller owns the returned {@link AsyncHttpExchange} and must either let it terminate or cancel it.</li>
 *   <li>The subscriber of the response body owns its subscription; cancelling it discards the connection.</li>
 *   <li>The transport cancels its request-body subscription when the exchange fails or is cancelled, and never
 *       resubscribes to a request body within one exchange.</li>
 * </ul>
 *
 * <p>Implementations are thread safe.
 */
public interface SdkAsyncHttpClient extends AutoCloseable {
    /**
     * Starts an exchange of {@code request}, reporting its response to {@code handler}.
     *
     * @throws IllegalStateException if the client is closed
     */
    AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler, HttpExecutionOptions options);

    /** What this transport supports; the same value for the client's lifetime. */
    HttpClientCapabilities capabilities();

    /** A stable diagnostic name of the implementation, for example {@code NettyNioAsyncHttpClient}. */
    String clientName();

    /** Releases the transport's resources; idempotent. */
    @Override
    void close();
}
