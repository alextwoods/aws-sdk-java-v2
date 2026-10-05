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

package software.amazon.awssdk.protocols.core.eventstream;

import java.util.Objects;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The event stream plan of one operation: its input and output event stream members (if any) and their codecs. The
 * HTTP binding protocols ({@code restJson1}, {@code restXml}) bind the event stream as the {@code httpPayload};
 * the RPC protocols ({@code awsJson1_0/1_1}, {@code rpcv2Cbor}) send the other input members as the
 * {@code initial-request} event and read the other output members from the {@code initial-response} event.
 *
 * <p>Immutable and thread safe.
 */
public final class OperationEventStreams {
    private final Schema inputMember;
    private final Schema outputMember;
    private final EventStreamCodec input;
    private final EventStreamCodec output;
    private final int maxMessageSize;
    private final OutputEventStream restOutput;
    private final OutputEventStream rpcOutput;

    private OperationEventStreams(WireContext wire, OperationSchemas operation, EventPayloadCodec payloads,
                                  ModeledErrors errors, int maxMessageSize) {
        this.inputMember = eventStream(operation.inputStreamMember().orElse(null));
        this.outputMember = eventStream(operation.outputStreamMember().orElse(null));
        this.input = inputMember == null ? null : EventStreamCodec.of(wire, inputMember, payloads, errors);
        this.output = outputMember == null ? null : EventStreamCodec.of(wire, outputMember, payloads, errors);
        this.maxMessageSize = maxMessageSize;
        this.restOutput = output == null ? null : new OutputEventStream(output, maxMessageSize, false);
        this.rpcOutput = output == null ? null : new OutputEventStream(output, maxMessageSize, true);
    }

    /** The event stream plan of {@code operation}, with {@link EventStreams#DEFAULT_MAX_MESSAGE_SIZE}. */
    public static OperationEventStreams of(WireContext wire, OperationSchemas operation, EventPayloadCodec payloads,
                                           ModeledErrors errors) {
        return new OperationEventStreams(Objects.requireNonNull(wire, "wire"),
                                         Objects.requireNonNull(operation, "operation"),
                                         Objects.requireNonNull(payloads, "payloads"), errors,
                                         EventStreams.DEFAULT_MAX_MESSAGE_SIZE);
    }

    private static Schema eventStream(Schema member) {
        return member != null && member.type() != ShapeType.BLOB ? member : null;
    }

    /** True if the input has an event stream member. */
    public boolean hasInput() {
        return inputMember != null;
    }

    /** True if the output has an event stream member. */
    public boolean hasOutput() {
        return outputMember != null;
    }

    /** The input event stream codec, or {@code null}. */
    public EventStreamCodec inputCodec() {
        return input;
    }

    /** The output event stream codec, or {@code null}. */
    public EventStreamCodec outputCodec() {
        return output;
    }

    /**
     * HTTP binding protocols: sets the input event stream (if set) as the request body.
     *
     * @return true if a body was set
     */
    public boolean writeRestRequest(SerializableStruct value, SdkHttpRequest.Builder request) {
        return write(value, null, request);
    }

    /**
     * RPC protocols: sets the {@code initial-request} event ({@code value} without its event stream) followed by the
     * input event stream (if set) as the request body.
     *
     * @return true if a body was set
     */
    public boolean writeRpcRequest(SerializableStruct value, SdkHttpRequest.Builder request) {
        return write(value, value, request);
    }

    private boolean write(SerializableStruct value, SerializableStruct initial, SdkHttpRequest.Builder request) {
        if (inputMember == null || EventStreams.eventStream(value, inputMember) == null) {
            return false;
        }
        EventStreams.writeRequest(value, inputMember, input, initial, request);
        return true;
    }

    /** HTTP binding protocols: the factory of the output event stream reader. */
    public HttpBindingReader.EventStreamFactory restResponse() {
        return response -> EventStreams.reader(response, output, maxMessageSize);
    }

    /**
     * HTTP binding protocols: how an async caller reads the output event stream, whose other members are bound to
     * the response headers; {@code null} if the output has no event stream.
     */
    public OutputEventStream restOutput() {
        return restOutput;
    }

    /**
     * RPC protocols: how an async caller reads the output event stream, whose first message is the
     * {@code initial-response} event; {@code null} if the output has no event stream.
     */
    public OutputEventStream rpcOutput() {
        return rpcOutput;
    }

    /** RPC protocols: reads the {@code initial-response} event and binds the rest of the body as the event stream. */
    public <T extends SerializableStruct> T readRpcResponse(SdkHttpResponse response, ShapeBuilder<T> builder) {
        return EventStreams.readInitialResponse(response, output, builder, maxMessageSize);
    }
}
