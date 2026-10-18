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

package software.amazon.awssdk.protocols.client.http.eventstream;

import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.event.EventEncoder;
import software.amazon.awssdk.schema.serde.event.EventEncoderFactory;
import software.amazon.awssdk.schema.serde.event.EventStreamingException;
import software.amazon.awssdk.schema.serde.event.FrameEncoder;
import software.amazon.awssdk.utils.Validate;

/**
 * Creates encoders of the AWS event stream encoding ({@code application/vnd.amazon.eventstream}) for an operation's
 * input or output event stream.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.AwsEventEncoderFactory}. The frame encoder is
 * the SDK's {@code eventstream} library, and outbound frames are signed by the SDK's existing event stream signer on
 * the encoded bytes, so there is no {@code withFrameProcessor}.
 */
@SdkPublicApi
public final class AwsEventEncoderFactory implements EventEncoderFactory<AwsEventFrame> {
    /**
     * The media type of an AWS event stream body, {@code application/vnd.amazon.eventstream}.
     */
    public static final String CONTENT_TYPE = "application/vnd.amazon.eventstream";

    private final InitialEventType initialEventType;
    private final Schema schema;
    private final Codec codec;
    private final String payloadMediaType;
    private final boolean emitEmptyPayload;
    private final Function<Throwable, EventStreamingException> exceptionHandler;

    private AwsEventEncoderFactory(InitialEventType initialEventType, Schema schema, Codec codec,
                                   String payloadMediaType, boolean emitEmptyPayload,
                                   Function<Throwable, EventStreamingException> exceptionHandler) {
        this.initialEventType = Validate.paramNotNull(initialEventType, "initialEventType");
        Validate.paramNotNull(schema, "schema");
        this.schema = schema.isMember() ? schema.memberTarget() : schema;
        this.codec = Validate.paramNotNull(codec, "codec");
        this.payloadMediaType = Validate.paramNotNull(payloadMediaType, "payloadMediaType");
        this.emitEmptyPayload = emitEmptyPayload;
        this.exceptionHandler = Validate.paramNotNull(exceptionHandler, "exceptionHandler");
    }

    /**
     * Creates an encoder factory for the operation's input event stream. Events without payload members have no body
     * and no {@code :content-type}, as REST protocols send them.
     *
     * @param operation the operation, which has an input event stream
     * @param codec the codec of the payloads
     * @param payloadMediaType the {@code :content-type} of codec-encoded payloads
     * @param exceptionHandler converts an exception that is not a modeled event stream error into an error message
     * @return the factory
     */
    public static AwsEventEncoderFactory forInputStream(ApiOperation<?, ?> operation, Codec codec,
                                                        String payloadMediaType,
                                                        Function<Throwable, EventStreamingException> exceptionHandler) {
        return forInputStream(operation, codec, payloadMediaType, false, exceptionHandler);
    }

    /**
     * Creates an encoder factory for the operation's input event stream.
     *
     * @param operation the operation, which has an input event stream
     * @param codec the codec of the payloads
     * @param payloadMediaType the {@code :content-type} of codec-encoded payloads
     * @param emitEmptyPayload whether an event without payload members gets a codec-encoded empty structure, as RPC
     *                         protocols send it
     * @param exceptionHandler converts an exception that is not a modeled event stream error into an error message
     * @return the factory
     */
    public static AwsEventEncoderFactory forInputStream(ApiOperation<?, ?> operation, Codec codec,
                                                        String payloadMediaType, boolean emitEmptyPayload,
                                                        Function<Throwable, EventStreamingException> exceptionHandler) {
        return new AwsEventEncoderFactory(InitialEventType.INITIAL_REQUEST, streamMember(operation.inputStreamMember()),
                                          codec, payloadMediaType, emitEmptyPayload, exceptionHandler);
    }

    /**
     * Creates an encoder factory for the operation's output event stream.
     *
     * @param operation the operation, which has an output event stream
     * @param codec the codec of the payloads
     * @param payloadMediaType the {@code :content-type} of codec-encoded payloads
     * @param exceptionHandler converts an exception that is not a modeled event stream error into an error message
     * @return the factory
     */
    public static AwsEventEncoderFactory forOutputStream(ApiOperation<?, ?> operation, Codec codec,
                                                         String payloadMediaType,
                                                         Function<Throwable, EventStreamingException> exceptionHandler) {
        return forOutputStream(operation, codec, payloadMediaType, false, exceptionHandler);
    }

    /**
     * Creates an encoder factory for the operation's output event stream.
     *
     * @param operation the operation, which has an output event stream
     * @param codec the codec of the payloads
     * @param payloadMediaType the {@code :content-type} of codec-encoded payloads
     * @param emitEmptyPayload whether an event without payload members gets a codec-encoded empty structure
     * @param exceptionHandler converts an exception that is not a modeled event stream error into an error message
     * @return the factory
     */
    public static AwsEventEncoderFactory forOutputStream(ApiOperation<?, ?> operation, Codec codec,
                                                         String payloadMediaType, boolean emitEmptyPayload,
                                                         Function<Throwable, EventStreamingException> exceptionHandler) {
        return new AwsEventEncoderFactory(InitialEventType.INITIAL_RESPONSE,
                                          streamMember(operation.outputStreamMember()),
                                          codec, payloadMediaType, emitEmptyPayload, exceptionHandler);
    }

    static Schema streamMember(Schema member) {
        if (member == null || member.memberTarget().type() != ShapeType.UNION) {
            throw new IllegalArgumentException("The operation has no event stream member");
        }
        return member;
    }

    @Override
    public AwsEventEncoder newEventEncoder() {
        return new AwsEventEncoder(new AwsEventShapeEncoder(initialEventType, schema, codec, payloadMediaType,
                                                            emitEmptyPayload, exceptionHandler));
    }

    @Override
    public FrameEncoder<AwsEventFrame> newFrameEncoder() {
        return AwsFrameCodec.ENCODER;
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    /**
     * The event encoder of the factory, with an SDK addition: {@link #encodeEvent(String, SerializableStruct)}
     * encodes an SDK event, the value of a named union member.
     */
    @SdkPublicApi
    public static final class AwsEventEncoder implements EventEncoder<AwsEventFrame> {
        private final AwsEventShapeEncoder delegate;

        private AwsEventEncoder(AwsEventShapeEncoder delegate) {
            this.delegate = delegate;
        }

        @Override
        public AwsEventFrame encode(SerializableStruct item) {
            return delegate.encode(item);
        }

        /**
         * Encodes an event, the value of the event stream union member {@code eventType}.
         *
         * @param eventType the union member name, the {@code :event-type} header value
         * @param event the event
         * @return the frame
         * @throws IllegalArgumentException if the union has no event member {@code eventType}
         */
        public AwsEventFrame encodeEvent(String eventType, SerializableStruct event) {
            return delegate.encodeEvent(Validate.paramNotNull(eventType, "eventType"),
                                        Validate.paramNotNull(event, "event"));
        }

        @Override
        public AwsEventFrame encodeFailure(Throwable exception) {
            return delegate.encodeFailure(exception);
        }
    }
}
