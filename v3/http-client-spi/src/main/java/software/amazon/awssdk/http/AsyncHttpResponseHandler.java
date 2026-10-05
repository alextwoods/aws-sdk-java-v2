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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;

/**
 * Receives the response of an {@link SdkAsyncHttpClient} exchange. Replaces the V2
 * {@code SdkAsyncHttpResponseHandler}; the signals are the same.
 *
 * <p>Signal order, which every transport honors (Requirement 12.8): either
 * {@link #onHeaders} then {@link #onStream} (each exactly once), or {@link #onError} exactly once, possibly after
 * {@link #onHeaders}. Nothing is signalled after {@link #onError}. After {@link #onStream}, failures of the body,
 * including cancellation of the exchange, are signalled to the body subscriber through {@code onError}, never to this
 * handler. Signals are serial (never concurrent) and may run on a transport event loop, so implementations do not
 * block.
 */
public interface AsyncHttpResponseHandler {
    /** The status and headers; the response body is {@link HttpBody#empty()} here and arrives through {@link #onStream}. */
    void onHeaders(SdkHttpResponse response);

    /**
     * The response body, a Reactive Streams publisher of read-only chunks that accepts exactly one subscriber and
     * delivers no chunk without demand, never more chunks than requested, at most
     * {@link HttpClientCapabilities#maxBufferedBytesPerExchange()} bytes read ahead of demand, and exactly one
     * terminal signal. Cancelling the subscription discards the connection. A response without a body completes
     * without chunks.
     */
    void onStream(Publisher<ByteBuffer> body);

    /** The exchange failed before {@link #onStream}; {@code error} is an {@link HttpTransportException}. */
    void onError(Throwable error);
}
