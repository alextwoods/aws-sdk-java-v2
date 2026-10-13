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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Sends a request serialized by a {@link ClientProtocol} and returns the response, blocking the calling thread.
 *
 * <p>The SDK wraps every {@code SdkHttpClient} in a transport whose {@link #messageExchange()} is
 * {@link HttpMessageExchange#INSTANCE}. A client can only be built with a protocol and a transport whose message
 * exchanges are equal.
 *
 * <p>A transport delivers the request exactly as the protocol produced it and returns the response exactly as it was
 * received. Timeouts, retries, metrics and signing stay in the request pipeline. Implementations must be safe to use
 * from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.core.ClientTransport}. It extends
 * {@link SdkAutoCloseable}, takes {@link ExecutionAttributes} as its call context, and has no
 * {@code remapExceptions}: failures surface as the SDK exceptions the SDK raises today.
 *
 * @param <RequestT> the request message type
 * @param <ResponseT> the response message type
 * @see AsyncClientTransport
 */
@SdkPublicApi
public interface ClientTransport<RequestT, ResponseT> extends SdkAutoCloseable {
    /**
     * Returns the request and response message types of the transport.
     *
     * @return the message exchange, for example {@link HttpMessageExchange#INSTANCE}
     */
    MessageExchange<RequestT, ResponseT> messageExchange();

    /**
     * Sends a request and waits for its response.
     *
     * @param context the attributes of the current call
     * @param request the request to send
     * @return the response; any response the transport receives, including error responses
     * @throws SdkClientException if the request could not be sent or the response could not be received. The SDK's
     *     transports raise the same exception, with the same retryable classification, that the SDK raises today for
     *     the failure, with the underlying failure as the cause
     */
    ResponseT send(ExecutionAttributes context, RequestT request);

    /**
     * Releases the resources of the transport. A transport that wraps a client supplied by the user leaves that client
     * open.
     */
    @Override
    void close();
}
