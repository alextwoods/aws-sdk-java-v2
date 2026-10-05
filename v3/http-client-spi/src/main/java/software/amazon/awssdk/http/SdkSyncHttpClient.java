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
 * A blocking HTTP transport (Requirement 12.1; design, "HTTP SPI"). Separate from {@link SdkAsyncHttpClient}: the two
 * share the immutable message, header, body, option, capability and failure types but not an execution interface,
 * so neither mode adapts through the other. Renamed from the V2 {@code SdkHttpClient}.
 *
 * <p>Execution: {@link #prepare} returns a {@link SyncHttpExchange} without I/O; {@link SyncHttpExchange#execute()}
 * acquires a connection, transmits the request and returns once the status and headers are read. Request bodies are
 * read from {@link SdkHttpRequest#body()}: buffered bytes directly, a {@link HttpBody.Kind#STREAMING} body through one
 * {@link StreamingBody#openStream()} per exchange, which the transport closes when transmission ends or fails.
 *
 * <p>Ownership:
 * <ul>
 *   <li>The client is closed by whoever built it (see {@code ResourceOwnership}): a client passed to an SDK client
 *       builder is caller owned, one the SDK creates is SDK owned. {@link #close()} is idempotent; it releases pooled
 *       connections, and exchanges in flight fail with {@link HttpFailureCategory#CANCELLED}.</li>
 *   <li>The caller of {@link #prepare} owns the exchange and closes it ({@link SyncHttpExchange}).</li>
 *   <li>The transport never closes or replays a request body it did not open, and never retains the request after
 *       the exchange ends.</li>
 * </ul>
 *
 * <p>Failures are thrown as {@link HttpTransportException}. Implementations are thread safe: exchanges may be
 * prepared and executed concurrently.
 */
public interface SdkSyncHttpClient extends AutoCloseable {
    /**
     * A new exchange of {@code request}; no I/O happens until {@link SyncHttpExchange#execute()}.
     *
     * @throws IllegalStateException if the client is closed
     */
    SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options);

    /** What this transport supports; the same value for the client's lifetime. */
    HttpClientCapabilities capabilities();

    /** A stable diagnostic name of the implementation, for example {@code ApacheHttpClient}. */
    String clientName();

    /** Releases the transport's resources; idempotent. */
    @Override
    void close();
}
