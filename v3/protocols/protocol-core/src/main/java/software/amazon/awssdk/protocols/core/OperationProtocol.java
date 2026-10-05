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

package software.amazon.awssdk.protocols.core;

import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;

/**
 * The immutable, thread-safe Schema SERDE plan of one operation under one protocol, obtained once from
 * {@link ClientProtocol#operation(OperationSchemas)} and held by the operation's pipeline plan.
 *
 * <p>Every failure caused by wire input that violates the protocol or a schema is an {@link SdkProtocolException}
 * carrying the protocol, operation, shape and member path and, when known, the wire location, but never a wire value
 * (Requirement 5.9); no partial modeled value is ever returned (Requirement 5.10).
 */
public interface OperationProtocol {
    /** The operation's schemas. */
    OperationSchemas schemas();

    /**
     * Serializes {@code input} into an HTTP request addressed to {@code endpoint}.
     *
     * <p>The endpoint supplies the scheme, host, port and base path; the protocol appends the operation's path
     * (joining with a single {@code /}), applies the {@code smithy.api#endpoint} host prefix if the client enabled
     * host-prefix injection ({@link ProtocolContext#hostPrefixInjection()}), and sets every protocol header, such as
     * {@code Content-Type}. It does not set transport headers ({@code Host}, {@code Content-Length}), signing or
     * user-agent headers; those belong to later pipeline stages.
     *
     * @param input the operation input, whose schema is the operation's input schema
     * @param endpoint the resolved endpoint; an absolute {@code http}/{@code https} URI without query or fragment
     * @throws IllegalArgumentException if {@code input} is not the operation's input, or {@code endpoint} is invalid
     * <p>A streaming blob payload member becomes a {@link software.amazon.awssdk.http.HttpBody.Kind#STREAMING} body
     * over the member's {@code DataStream}, keeping its content length, content type and replayability; it is never
     * read or buffered here. An event stream member becomes a one-shot
     * {@link software.amazon.awssdk.protocols.core.eventstream.EventStreamRequestBody}.
     *
     * @throws UnsupportedOperationException if the operation has an event stream member the protocol does not support
     *         (query protocols) or a streaming blob member the protocol cannot bind
     */
    SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint);

    /**
     * True if {@code response} is an error response to be read by {@link #deserializeError}. The default treats every
     * non-2xx status code as an error.
     */
    default boolean isError(SdkHttpResponse response) {
        return !response.isSuccessful();
    }

    /**
     * Deserializes a successful response into {@code output}, a fresh builder of the operation's output, and builds
     * it.
     *
     * <p>A streaming blob payload member is a {@code DataStream} over the response body, which is not read or
     * buffered; the caller owns consuming it. Any other response body that streams is a finite message and is read
     * completely.
     *
     * @throws SdkProtocolException if the response violates the protocol or the output schema
     * @throws UnsupportedOperationException if the operation has an event stream member the protocol does not support
     *         (query protocols) or a streaming blob member the protocol cannot bind
     */
    <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output);

    /**
     * Deserializes a successful response of an operation whose output has a streaming blob payload member, for a
     * caller that consumes the payload itself (a response transformer): every member bound to the status code and
     * headers is read into {@code output}, the payload member is not set, and the response body is neither read nor
     * buffered.
     *
     * <p>The default supports no streaming blob payloads: only the HTTP binding protocols ({@code restJson1},
     * {@code restXml}) bind them.
     *
     * @throws SdkProtocolException if the response violates the protocol or the output schema
     * @throws UnsupportedOperationException if the operation has no streaming blob payload the protocol binds
     */
    default <T extends SerializableStruct> T deserializeStreamingOutput(SdkHttpResponse response,
                                                                        ShapeBuilder<T> output) {
        throw new UnsupportedOperationException("The protocol of " + schemas().id()
                                                + " does not bind streaming blob payloads");
    }

    /**
     * How an async caller reads the operation's output event stream from a live response body, if the output has an
     * event stream member the protocol serves: the codec, the message bound, and whether the output's other members
     * arrive as the RPC {@code initial-response} event or are bound to the response headers (then read by
     * {@link #deserializeStreamingOutput} without the body). The default is empty: no output event stream.
     */
    default Optional<OutputEventStream> outputEventStream() {
        return Optional.empty();
    }

    /**
     * Maps an error response to the exception to throw: the modeled error the response identifies, resolved through
     * the service's type registry and the protocol's error-code rules (including {@code awsQueryCompatible} and
     * {@code awsQueryError} codes), else a generic {@link AwsServiceException}. Either carries the wire error code,
     * message, request ID and status code in its {@link AwsServiceException#awsErrorDetails()}.
     *
     * @throws SdkProtocolException if the response identifies a modeled error but its body violates that error's
     *         schema
     */
    AwsServiceException deserializeError(SdkHttpResponse response);
}
