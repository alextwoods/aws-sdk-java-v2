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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;
import software.amazon.awssdk.schema.serde.event.EventDecoder;
import software.amazon.awssdk.schema.serde.event.EventStreamingException;
import software.amazon.awssdk.schema.serde.event.EventStreamingProtocolException;
import software.amazon.awssdk.utils.Validate;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

/**
 * Decodes AWS event stream frames into events through the event stream union's Schema (Requirements 14.5–14.7).
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.AwsEventShapeDecoder}. SDK deviations:
 * <ul>
 *     <li>An event is the union member's value, built by {@code memberBuilders} (the generated per-member event class)
 *     or the member target's builder; it is wrapped in a union only when a {@code unionBuilder} is given (smithy-java's
 *     form).</li>
 *     <li>An unknown {@code :event-type} returns {@code unknownEvent} (the generated visitor's unknown-event value,
 *     Requirement 14.8) when one is given, instead of failing.</li>
 *     <li>A {@code :message-type: exception} message is thrown as the modeled exception built from the error member's
 *     target (Requirement 14.6) instead of returned as a union value.</li>
 *     <li>An {@code eventPayload} blob or string member is read from the raw payload bytes, as the encoder writes it,
 *     instead of through the codec.</li>
 *     <li>An empty payload is not given to the codec.</li>
 * </ul>
 */
@SdkInternalApi
final class AwsEventShapeDecoder implements EventDecoder<AwsEventFrame> {
    private final Supplier<? extends ShapeBuilder<?>> initialEventBuilder;
    private final Supplier<? extends ShapeBuilder<?>> unionBuilder;
    private final Function<String, ? extends ShapeBuilder<?>> memberBuilders;
    private final Supplier<?> unknownEvent;
    private final Schema eventSchema;
    private final Codec codec;

    AwsEventShapeDecoder(Supplier<? extends ShapeBuilder<?>> initialEventBuilder,
                         Supplier<? extends ShapeBuilder<?>> unionBuilder,
                         Function<String, ? extends ShapeBuilder<?>> memberBuilders,
                         Supplier<?> unknownEvent,
                         Schema eventSchema,
                         Codec codec) {
        this.initialEventBuilder = Validate.paramNotNull(initialEventBuilder, "initialEventBuilder");
        this.unionBuilder = unionBuilder;
        this.memberBuilders = memberBuilders;
        this.unknownEvent = unknownEvent;
        this.eventSchema = Validate.paramNotNull(eventSchema, "eventSchema");
        this.codec = Validate.paramNotNull(codec, "codec");
    }

    @Override
    public Object decode(AwsEventFrame frame) {
        Message message = frame.unwrap();
        String messageType = expectHeader(message, HeadersBuilder.MESSAGE_TYPE).getString();
        switch (messageType) {
            case "error":
                throw decodeError(message);
            case "event":
                return decodeEvent(expectHeader(message, HeadersBuilder.EVENT_TYPE).getString(), message);
            case "exception":
                throw decodeModeledException(message);
            default:
                throw new EventStreamingProtocolException("Unknown message type: " + messageType);
        }
    }

    private Object decodeEvent(String eventType, Message message) {
        Schema memberSchema = eventSchema.member(eventType);
        if (memberSchema == null || memberSchema.hasTrait(TraitKey.ERROR_TRAIT)) {
            if (unknownEvent != null) {
                return unknownEvent.get();
            }
            throw new EventStreamingProtocolException("Unsupported event type: " + eventType);
        }
        SerializableShape value = decodeMember(memberSchema, message);
        if (unionBuilder == null) {
            return value;
        }
        ShapeBuilder<?> builder = unionBuilder.get();
        builder.setMemberValue(memberSchema, value);
        return builder.build();
    }

    private RuntimeException decodeModeledException(Message message) {
        String exceptionType = expectHeader(message, HeadersBuilder.EXCEPTION_TYPE).getString();
        Schema memberSchema = eventSchema.member(exceptionType);
        if (memberSchema == null || !memberSchema.hasTrait(TraitKey.ERROR_TRAIT)) {
            throw new EventStreamingProtocolException("Unsupported exception type: " + exceptionType);
        }
        SerializableShape value = decodeMember(memberSchema, message);
        if (!(value instanceof RuntimeException)) {
            throw new IllegalStateException("The builder of " + memberSchema.memberTarget().id()
                                            + " did not build an exception: " + value.getClass().getName());
        }
        return (RuntimeException) value;
    }

    private SerializableShape decodeMember(Schema memberSchema, Message message) {
        ShapeBuilder<?> shapeBuilder = memberBuilders != null ? memberBuilders.apply(memberSchema.memberName()) : null;
        if (shapeBuilder == null) {
            shapeBuilder = memberSchema.memberTarget().shapeBuilder();
        }
        if (shapeBuilder == null) {
            throw new IllegalStateException("The schema of " + memberSchema.memberTarget().id() + " has no builder");
        }
        shapeBuilder.deserialize(new EventStreamDeserializer(codec, message));
        return shapeBuilder.build();
    }

    private static EventStreamingProtocolException decodeError(Message message) {
        String errorCode = expectHeader(message, HeadersBuilder.ERROR_CODE).getString();
        String errorMessage = expectHeader(message, HeadersBuilder.ERROR_MESSAGE).getString();
        return new EventStreamingException(errorCode, errorMessage);
    }

    @Override
    public SerializableStruct decodeInitialEvent(AwsEventFrame frame) {
        ShapeBuilder<?> builder = initialEventBuilder.get();
        builder.deserialize(new EventStreamDeserializer(codec, frame.unwrap()));
        Object result = builder.build();
        return (SerializableStruct) result;
    }

    private static HeaderValue expectHeader(Message message, String headerName) {
        HeaderValue header = message.getHeaders().get(headerName);
        if (header == null) {
            throw new EventStreamingProtocolException("expected headers to have '" + headerName + "' header");
        }
        return header;
    }

    /**
     * Reads {@code eventHeader} members from the message headers, the {@code eventPayload} member or the remaining
     * members from the payload.
     */
    static final class EventStreamDeserializer extends SpecificShapeDeserializer {
        private final Codec codec;
        private final Message message;
        private final HeadersDeserializer headersDeserializer;

        EventStreamDeserializer(Codec codec, Message message) {
            this.codec = codec;
            this.message = message;
            this.headersDeserializer = new HeadersDeserializer(message.getHeaders());
        }

        @Override
        public <T> void readStruct(Schema schema, T builder, StructMemberConsumer<T> consumer) {
            boolean payloadWritten = false;
            for (Schema member : schema.members()) {
                if (member.hasTrait(TraitKey.EVENT_HEADER_TRAIT)
                    && headersDeserializer.headers.containsKey(member.memberName())) {
                    consumer.accept(builder, member, headersDeserializer);
                } else if (member.hasTrait(TraitKey.EVENT_PAYLOAD_TRAIT)) {
                    if (message.getPayload().length > 0) {
                        consumer.accept(builder, member, payloadDeserializer(member));
                    }
                    payloadWritten = true;
                }
            }
            // Deserialize from the payload if still needed.
            if (!payloadWritten && message.getPayload().length > 0) {
                try (ShapeDeserializer codecDeserializer = codec.createDeserializer(message.getPayload())) {
                    codecDeserializer.readStruct(schema, builder, consumer);
                }
            }
        }

        private ShapeDeserializer payloadDeserializer(Schema member) {
            if (member.type() == ShapeType.BLOB) {
                return new SpecificShapeDeserializer() {
                    @Override
                    public ByteBuffer readBlob(Schema schema) {
                        return ByteBuffer.wrap(message.getPayload());
                    }
                };
            }
            if (member.type() == ShapeType.STRING) {
                return new SpecificShapeDeserializer() {
                    @Override
                    public String readString(Schema schema) {
                        return new String(message.getPayload(), StandardCharsets.UTF_8);
                    }
                };
            }
            return codec.createDeserializer(message.getPayload());
        }
    }

    /**
     * Reads {@code eventHeader} members from the typed message headers named after the member.
     */
    static final class HeadersDeserializer extends SpecificShapeDeserializer {
        private final Map<String, HeaderValue> headers;

        HeadersDeserializer(Map<String, HeaderValue> headers) {
            this.headers = headers;
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return header(schema).getByteBuffer();
        }

        @Override
        public byte readByte(Schema schema) {
            return header(schema).getByte();
        }

        @Override
        public short readShort(Schema schema) {
            return header(schema).getShort();
        }

        @Override
        public int readInteger(Schema schema) {
            return header(schema).getInteger();
        }

        @Override
        public long readLong(Schema schema) {
            return header(schema).getLong();
        }

        @Override
        public String readString(Schema schema) {
            return header(schema).getString();
        }

        @Override
        public boolean readBoolean(Schema schema) {
            return header(schema).getBoolean();
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            return header(schema).getTimestamp();
        }

        @Override
        public boolean isNull() {
            return false;
        }

        private HeaderValue header(Schema member) {
            HeaderValue value = headers.get(member.memberName());
            if (value == null) {
                throw new EventStreamingProtocolException("expected headers to have '" + member.memberName()
                                                          + "' header");
            }
            return value;
        }
    }
}
