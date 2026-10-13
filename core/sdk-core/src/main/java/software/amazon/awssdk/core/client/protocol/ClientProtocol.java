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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * A wire protocol, such as {@code aws.protocols#restJson1}: turns an operation input into a transport request and a
 * transport response into an operation output or an exception, using only the operation's schemas.
 *
 * <p>Clients that use schema-based serialization, generated or dynamic, delegate all serialization to a client
 * protocol. The SDK provides one implementation for each supported protocol; generated clients pick a default and
 * accept any other instance through their builder's {@code protocol(...)} setting. A third party can implement this
 * interface, usually by extending one of the SDK's HTTP protocol base classes, and use it with any such client without
 * regenerating code. To make a protocol discoverable by its protocol ID, register a {@link ClientProtocolFactory}.
 *
 * <p>The interface is transport-agnostic. Its message types come from {@link #messageExchange()}, which must equal the
 * {@link ClientTransport#messageExchange() message exchange} of the transport the client uses; HTTP protocols use
 * {@link HttpMessageExchange}.
 *
 * <p>Implementations are created once per client, must be immutable after construction and safe to use from multiple
 * threads, and must return the same {@link #id()}, {@link #payloadCodec()} and {@link #messageExchange()} on every
 * call.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.core.ClientProtocol}. The call context is
 * {@link ExecutionAttributes}, shared with interceptors and transports; {@code createRequest} and
 * {@code setServiceEndpoint} are named {@link #serializeRequest} and {@link #updateServiceEndpoint}; and
 * {@link #deserializeResponse} throws SDK exceptions.
 *
 * @param <RequestT> the transport request type
 * @param <ResponseT> the transport response type
 */
@SdkPublicApi
public interface ClientProtocol<RequestT, ResponseT> {
    /**
     * Returns the ID of the protocol.
     *
     * @return the shape ID of the protocol definition trait the protocol implements, for example
     *     {@code aws.protocols#restJson1}
     */
    ShapeId id();

    /**
     * Returns the codec that serializes and deserializes request and response payloads.
     *
     * @return the payload codec, configured with the protocol's codec settings, or null if the protocol has no single
     *     payload codec (for example {@code aws.protocols#awsQuery} and {@code aws.protocols#ec2Query})
     */
    Codec payloadCodec();

    /**
     * Returns the request and response message types of the protocol.
     *
     * @return the message exchange, for example {@link HttpMessageExchange#INSTANCE}
     */
    MessageExchange<RequestT, ResponseT> messageExchange();

    /**
     * Serializes an operation input into a transport request.
     *
     * <p>The endpoint is where the request would be sent if no endpoint were resolved later. The request pipeline
     * calls {@link #updateServiceEndpoint} with the resolved endpoint afterwards.
     *
     * @param operation the operation being called
     * @param input the operation input
     * @param context the attributes of the current call
     * @param endpoint the endpoint to send the request to
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @return the transport request
     * @throws SdkClientException if the input cannot be serialized under the protocol, for example when a required
     *     {@code httpLabel} member is null or empty. The message names the operation and the cause, and no request is
     *     returned
     */
    <I extends SerializableStruct, O extends SerializableStruct> RequestT serializeRequest(ApiOperation<I, O> operation,
                                                                                           I input,
                                                                                           ExecutionAttributes context,
                                                                                           URI endpoint);

    /**
     * Returns a copy of a serialized request that is sent to a new endpoint.
     *
     * <p>The endpoint is the root of the request's destination. How it combines with the request is protocol-specific;
     * HTTP protocols take the scheme, host and port from the endpoint, append the request's path to the endpoint's
     * path, and keep the query, headers and body.
     *
     * @param request the serialized request, which is not modified
     * @param endpoint the endpoint to send the request to
     * @return the updated request
     */
    RequestT updateServiceEndpoint(RequestT request, Endpoint endpoint);

    /**
     * Deserializes a transport response into the operation output, or throws the error it describes.
     *
     * <p>For an error response, the protocol resolves the error's shape ID from the response and creates the modeled
     * exception through the error registry. When the registry does not contain the shape ID, or none can be resolved,
     * the protocol throws the service's base exception from {@link ProtocolSettings#baseExceptionBuilder()} instead,
     * carrying the status code and, when present, the raw error code and message.
     *
     * @param operation the operation being called
     * @param context the attributes of the current call
     * @param errorRegistry the registry used to create modeled errors
     * @param request the request that was sent
     * @param response the response to deserialize
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @return the operation output, built through the operation's output builder
     * @throws SdkServiceException for an error response: the modeled exception or the service's base exception, with
     *     the response status code
     * @throws SdkClientException if a successful response's body cannot be parsed. The message names the operation and
     *     the cause, and no partial output is returned
     */
    <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(ApiOperation<I, O> operation,
                                                                                       ExecutionAttributes context,
                                                                                       TypeRegistry errorRegistry,
                                                                                       RequestT request,
                                                                                       ResponseT response);
}
