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

package software.amazon.awssdk.core.client.protocol;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Lets the caller of {@link AsyncClientTransport#send} take a response body as a stream instead of having the
 * transport buffer it. Operations with a streaming output use it, so that a successful response's body goes to the
 * response transformer while an error response is still buffered and deserialized as an error.
 *
 * <p>The transport calls {@link #onHeaders} once, when the response status and headers have arrived and before it
 * reads the body. If that returns {@code true}, the transport calls {@link #onStream} once with the body, then
 * completes its future with the response that was passed to {@code onHeaders}. If it returns {@code false}, the
 * transport buffers the body and completes its future with the full response, as when no hand-off is given.
 *
 * <p>Both methods are called on a transport thread and must not block.
 *
 * <p>SDK addition (design OD-17): smithy-java's transport is synchronous only.
 *
 * @param <ResponseT> the response message type
 */
@SdkPublicApi
public interface AsyncResponseHandoff<ResponseT> {
    /**
     * Called once when the response status and headers have arrived.
     *
     * @param response the response, without its body
     * @return {@code true} to receive the body through {@link #onStream}; {@code false} to have the transport buffer it
     */
    boolean onHeaders(ResponseT response);

    /**
     * Called once with the response body, if {@link #onHeaders} returned {@code true}. Failures while reading the body
     * are signaled to the publisher's subscriber.
     *
     * @param body the response body; it supports a single subscriber
     */
    void onStream(Publisher<ByteBuffer> body);
}
