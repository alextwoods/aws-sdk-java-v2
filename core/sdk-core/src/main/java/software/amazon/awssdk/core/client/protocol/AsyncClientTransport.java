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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Sends a request serialized by a {@link ClientProtocol} and completes a future with the response, without blocking
 * the calling thread.
 *
 * <p>The SDK wraps every {@code SdkAsyncHttpClient} in a transport whose {@link #messageExchange()} is
 * {@link HttpMessageExchange#INSTANCE}. A client can only be built with a protocol and a transport whose message
 * exchanges are equal.
 *
 * <p>A transport delivers the request exactly as the protocol produced it and returns the response exactly as it was
 * received. Timeouts, retries, metrics and signing stay in the request pipeline. Implementations must be safe to use
 * from multiple threads.
 *
 * <p>SDK addition (design OD-17): smithy-java's transport is synchronous only. The asynchronous transport completes a
 * {@link CompletableFuture} and can hand a streaming response body to the caller through an
 * {@link AsyncResponseHandoff}.
 *
 * @param <RequestT> the request message type
 * @param <ResponseT> the response message type
 * @see ClientTransport
 */
@SdkPublicApi
public interface AsyncClientTransport<RequestT, ResponseT> extends SdkAutoCloseable {
    /**
     * Returns the request and response message types of the transport.
     *
     * @return the message exchange, for example {@link HttpMessageExchange#INSTANCE}
     */
    MessageExchange<RequestT, ResponseT> messageExchange();

    /**
     * Sends a request.
     *
     * @param context the attributes of the current call
     * @param request the request to send
     * @param streamingHandoff receives the response body as a stream if it asks for it, or null to always buffer the
     *     body into the response
     * @return a future completed with the response, including error responses. It completes exceptionally with an
     *     {@link SdkClientException} if the request could not be sent or the response could not be received. The SDK's
     *     transports raise the same exception, with the same retryable classification, that the SDK raises today for
     *     the failure, with the underlying failure as the cause
     */
    CompletableFuture<ResponseT> send(ExecutionAttributes context,
                                      RequestT request,
                                      AsyncResponseHandoff<ResponseT> streamingHandoff);

    /**
     * Releases the resources of the transport. A transport that wraps a client supplied by the user leaves that client
     * open.
     */
    @Override
    void close();
}
