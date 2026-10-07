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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.eventstream.HeaderType;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.awssdk.protocols.core.error.ErrorCodes;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.SpecificShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Event stream SERDE of one event stream union: maps each union member (event) to an event stream {@link Message} and
 * back, with the protocol's {@link EventPayloadCodec} for structured payloads.
 *
 * <p>Message mapping (Smithy event stream specification):
 * <ul>
 *   <li>an event member: {@code :message-type event}, {@code :event-type <member name>}; a modeled error member
 *       ({@code @error} target): {@code :message-type exception}, {@code :exception-type <member name>};</li>
 *   <li>{@code @eventHeader} members become typed headers (boolean, byte, short, integer/intEnum, long, blob as byte
 *       array, string/enum, timestamp), in member order after the {@code :} headers;</li>
 *   <li>an {@code @eventPayload} member is the payload: a blob raw ({@code application/octet-stream}), a string as
 *       UTF-8 ({@code text/plain}), a structure/union through the codec; otherwise the non-header members, if the
 *       event has any, are serialized as a structure through the codec; {@code :content-type} is sent with a
 *       payload only;</li>
 *   <li>RPC protocols send the non-stream input members as a first {@code initial-request} event and receive the
 *       non-stream output members as a first {@code initial-response} event;</li>
 *   <li>a received {@code exception} message is the modeled error of its {@code :exception-type} (an
 *       {@link AwsServiceException} built by the service's registered error type), a received {@code error} message a
 *       generic {@link AwsServiceException} with its {@code :error-code} and {@code :error-message}; an unknown
 *       event type is passed to the union builder as an unknown member.</li>
 * </ul>
 *
 * <p>Malformed messages (missing or mistyped {@code :} headers, header/member type mismatches, undecodable payloads)
 * fail with a located {@link SdkProtocolException} that never quotes payload values.
 *
 * <p>Immutable and thread safe; build once per event stream member of an operation.
 */
public final class EventStreamCodec {
    public static final String MESSAGE_TYPE = ":message-type";
    public static final String EVENT_TYPE = ":event-type";
    public static final String EXCEPTION_TYPE = ":exception-type";
    public static final String CONTENT_TYPE = ":content-type";
    public static final String ERROR_CODE = ":error-code";
    public static final String ERROR_MESSAGE = ":error-message";
    public static final String INITIAL_REQUEST = "initial-request";
    public static final String INITIAL_RESPONSE = "initial-response";

    private static final String WIRE_LOCATION = "event stream message";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String TEXT = "text/plain";

    private final WireContext wire;
    private final Schema union;
    private final EventPayloadCodec payloads;
    private final ModeledErrors errors;
    private final UnaryOperator<SerializableStruct> eventView;

    private EventStreamCodec(WireContext wire, Schema union, EventPayloadCodec payloads, ModeledErrors errors,
                             UnaryOperator<SerializableStruct> eventView) {
        this.wire = wire;
        this.union = union;
        this.payloads = payloads;
        this.errors = errors;
        this.eventView = eventView;
    }

    /**
     * The codec of an event stream.
     *
     * @param wire the protocol and operation, for failure locations
     * @param eventStream the event stream member, or its streaming union target
     * @param payloads the protocol's payload codec
     * @param errors the service's modeled errors, or {@code null} to build every received error generically
     * @throws IllegalArgumentException if {@code eventStream} is not a streaming union
     */
    public static EventStreamCodec of(WireContext wire, Schema eventStream, EventPayloadCodec payloads,
                                      ModeledErrors errors) {
        Objects.requireNonNull(wire, "wire");
        Objects.requireNonNull(payloads, "payloads");
        Schema union = eventStream.isMember() ? eventStream.memberTarget() : eventStream;
        if (union.type() != ShapeType.UNION || !union.hasTrait(TraitKey.STREAMING_TRAIT)) {
            throw new IllegalArgumentException(union.id() + " is not an event stream (a @streaming union)");
        }
        return new EventStreamCodec(wire, union, payloads, errors, null);
    }

    /**
     * This codec, encoding each event through {@code view} first (see {@link EventStreams.InputEventView}); decoding
     * is unchanged.
     */
    public EventStreamCodec withEventView(UnaryOperator<SerializableStruct> view) {
        return new EventStreamCodec(wire, union, payloads, errors, Objects.requireNonNull(view, "view"));
    }

    /** The event stream union. */
    public Schema union() {
        return union;
    }

    /** The protocol and operation. */
    public WireContext wire() {
        return wire;
    }

    /**
     * The message of {@code event}, a value of the event stream union.
     *
     * @throws IllegalArgumentException if {@code event} is not a value of the union or has no member set
     */
    public Message encodeEvent(SerializableStruct event) {
        Objects.requireNonNull(event, "event");
        if (eventView != null) {
            event = Objects.requireNonNull(eventView.apply(event), "eventView");
        }
        if (!event.schema().id().equals(union.id())) {
            throw new IllegalArgumentException("The event is a " + event.schema().id() + ", not a " + union.id());
        }
        SetMember set = new SetMember();
        event.serializeMembers(set);
        if (set.member == null) {
            throw new IllegalArgumentException("The " + union.id() + " event has no member set");
        }
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        boolean exception = set.member.memberTarget().hasTrait(TraitKey.ERROR_TRAIT);
        headers.put(MESSAGE_TYPE, HeaderValue.ofString(exception ? "exception" : "event"));
        headers.put(exception ? EXCEPTION_TYPE : EVENT_TYPE, HeaderValue.ofString(set.member.memberName()));
        return eventMessage(headers, set.value);
    }

    /**
     * The first message of an RPC event stream ({@code initial-request} or {@code initial-response}): the members of
     * {@code struct} other than its event stream member, through the codec.
     */
    public Message encodeInitialMessage(SerializableStruct struct, String eventType) {
        Objects.requireNonNull(struct, "struct");
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(MESSAGE_TYPE, HeaderValue.ofString("event"));
        headers.put(EVENT_TYPE, HeaderValue.ofString(eventType));
        headers.put(CONTENT_TYPE, HeaderValue.ofString(payloads.mediaType()));
        byte[] payload = payloads.serialize(struct.schema(), new FilteredStruct(struct, m -> !isEventStream(m)));
        return new Message(headers, payload);
    }

    /** A generic {@code :message-type error} message. */
    public Message encodeError(String errorCode, String errorMessage) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(MESSAGE_TYPE, HeaderValue.ofString("error"));
        headers.put(ERROR_CODE, HeaderValue.ofString(errorCode));
        headers.put(ERROR_MESSAGE, HeaderValue.ofString(errorMessage));
        return new Message(headers, new byte[0]);
    }

    private Message eventMessage(Map<String, HeaderValue> headers, SerializableStruct value) {
        Schema target = value.schema();
        EventParts parts = new EventParts();
        value.serializeMembers(parts);
        byte[] payload = null;
        String contentType = null;
        Schema payloadMember = payloadMember(target);
        if (payloadMember != null) {
            if (parts.payload != null) {
                payload = parts.payload;
                contentType = switch (payloadMember.type()) {
                    case BLOB -> mediaType(payloadMember, OCTET_STREAM);
                    case STRING, ENUM -> mediaType(payloadMember, TEXT);
                    default -> payloads.mediaType();
                };
            } else if (parts.payloadStruct != null) {
                payload = payloads.serialize(parts.payloadStruct.schema(), parts.payloadStruct);
                contentType = payloads.mediaType();
            }
        } else if (hasBodyMembers(target)) {
            payload = payloads.serialize(target, new FilteredStruct(value, m -> !isHeader(m)));
            contentType = payloads.mediaType();
        }
        if (contentType != null) {
            headers.put(CONTENT_TYPE, HeaderValue.ofString(contentType));
        }
        headers.putAll(parts.headers);
        return new Message(headers, payload != null ? payload : new byte[0]);
    }

    /**
     * The union value of the event {@code message}.
     *
     * @param response the initial HTTP response, for error metadata; may be {@code null}
     * @throws AwsServiceException for an {@code exception} or {@code error} message
     * @throws SdkProtocolException if the message is malformed
     */
    public SerializableStruct decodeEvent(Message message, SdkHttpResponse response) {
        Objects.requireNonNull(message, "message");
        String messageType = requiredString(message, MESSAGE_TYPE);
        switch (messageType) {
            case "event" -> {
                String eventType = requiredString(message, EVENT_TYPE);
                ShapeBuilder<?> builder = unionBuilder();
                return (SerializableStruct) LocatingDeserializer.deserialize(
                    wire, WIRE_LOCATION, new UnionDeserializer(eventType, message), builder, null);
            }
            case "exception" -> throw exception(requiredString(message, EXCEPTION_TYPE), message, response);
            case "error" -> throw generic(optionalString(message, ERROR_CODE), optionalString(message, ERROR_MESSAGE),
                                          response);
            default -> throw failure(union, "the " + MESSAGE_TYPE + " header is not event, exception or error");
        }
    }

    /**
     * A builder of the event union: the registered type of a generated client (whose schemas carry no builder), or
     * else the schema's own builder (dynamic schemas).
     */
    private ShapeBuilder<?> unionBuilder() {
        if (errors != null) {
            try {
                return errors.newBuilder(union.id());
            } catch (IllegalStateException e) {
                // Not registered: the schema's own builder.
            }
        }
        return union.shapeBuilder();
    }

    /**
     * Reads the RPC {@code initial-response} {@code message} into {@code builder}: the payload's members through the
     * codec, and {@code stream} as the event stream member, which stays unset if {@code stream} is {@code null}. The
     * caller builds it.
     *
     * @throws AwsServiceException if the message is an {@code exception} or {@code error} message
     * @throws SdkProtocolException if the message is not an {@code initial-response} event or is malformed
     */
    public <B extends ShapeBuilder<?>> B decodeInitialMessage(Message message, B builder, EventStream<?> stream,
                                                              SdkHttpResponse response) {
        String messageType = requiredString(message, MESSAGE_TYPE);
        if (!"event".equals(messageType)) {
            decodeEvent(message, response);
        }
        if (!INITIAL_RESPONSE.equals(requiredString(message, EVENT_TYPE))) {
            throw failure(builder.schema(), "the first message is not the " + INITIAL_RESPONSE + " event");
        }
        return LocatingDeserializer.deserializeInto(wire, WIRE_LOCATION,
                                                    new InitialDeserializer(message, stream), builder, null);
    }

    private AwsServiceException exception(String exceptionType, Message message, SdkHttpResponse response) {
        Schema member = union.member(exceptionType);
        String errorMessage = errorMessage(message);
        if (member == null || !member.memberTarget().hasTrait(TraitKey.ERROR_TRAIT)) {
            return generic(exceptionType, errorMessage, response);
        }
        Schema target = member.memberTarget();
        ShapeBuilder<?> builder = null;
        if (errors != null) {
            try {
                builder = errors.newBuilder(target.id());
            } catch (IllegalStateException e) {
                builder = null;
            }
        }
        if (builder == null) {
            builder = target.shapeBuilder();
        }
        LocatingDeserializer.deserializeInto(wire, WIRE_LOCATION, new EventStructDeserializer(message), builder,
                                             null);
        if (!(builder instanceof AwsServiceException.Builder)) {
            return generic(exceptionType, errorMessage, response);
        }
        return errors != null ? errors.complete(builder, errorResponse(exceptionType, errorMessage, response))
                              : complete(builder, errorResponse(exceptionType, errorMessage, response));
    }

    private static AwsServiceException complete(ShapeBuilder<?> builder, ModeledErrors.ErrorResponse response) {
        AwsServiceException.Builder error = (AwsServiceException.Builder) builder;
        error.awsErrorDetails(response.details()).requestId(response.requestId())
             .extendedRequestId(response.extendedRequestId()).statusCode(response.statusCode());
        return (AwsServiceException) builder.errorCorrection().build();
    }

    private AwsServiceException generic(String code, String message, SdkHttpResponse response) {
        ModeledErrors.ErrorResponse metadata = errorResponse(code, message, response);
        if (errors != null) {
            return errors.generic(metadata);
        }
        return AwsServiceException.builder().awsErrorDetails(metadata.details()).message(message)
                                  .requestId(metadata.requestId()).statusCode(metadata.statusCode()).build();
    }

    private ModeledErrors.ErrorResponse errorResponse(String code, String message, SdkHttpResponse response) {
        AwsErrorDetails details = AwsErrorDetails.builder()
                                                 .errorCode(code)
                                                 .errorMessage(message)
                                                 .serviceName(errors != null ? errors.serviceName() : null)
                                                 .sdkHttpResponse(response)
                                                 .build();
        if (response == null) {
            return new ModeledErrors.ErrorResponse(details, null, null, 200);
        }
        return new ModeledErrors.ErrorResponse(details, ErrorCodes.requestId(response.headers()).orElse(null),
                                               ErrorCodes.extendedRequestId(response.headers()).orElse(null),
                                               response.statusCode());
    }

    /** The {@code message}/{@code Message} member of a structured error payload, if it has one. */
    private String errorMessage(Message message) {
        if (message.payloadLength() == 0) {
            return null;
        }
        try {
            Document document = payloads.deserializer(message.payload()).readDocument();
            for (String name : List.of("message", "Message")) {
                Document value = document == null ? null : document.getMember(name);
                if (value != null && value.type() == ShapeType.STRING) {
                    return value.asString();
                }
            }
        } catch (RuntimeException e) {
            // Not a structured payload (or a codec without documents): the error has no message.
        }
        return null;
    }

    private String requiredString(Message message, String name) {
        HeaderValue value = message.header(name);
        if (value == null) {
            throw failure(union, "the message has no " + name + " header");
        }
        if (value.type() != HeaderType.STRING) {
            throw failure(union, "the " + name + " header is a " + value.type() + ", not a STRING");
        }
        return value.asString();
    }

    private static String optionalString(Message message, String name) {
        HeaderValue value = message.header(name);
        return value != null && value.type() == HeaderType.STRING ? value.asString() : null;
    }

    /** The failure of an RPC output event stream body that ended before its {@code initial-response} event. */
    SdkProtocolException missingInitialResponse() {
        return failure(union, "the body ended before the " + INITIAL_RESPONSE + " event");
    }

    private SdkProtocolException failure(Schema shape, String reason) {
        return wire.failure(shape.id(), List.of(), WIRE_LOCATION, reason);
    }

    private static boolean isHeader(Schema member) {
        return member.hasTrait(TraitKey.EVENT_HEADER_TRAIT);
    }

    private static boolean isEventStream(Schema member) {
        Schema target = member.memberTarget();
        return target.type() == ShapeType.UNION && target.hasTrait(TraitKey.STREAMING_TRAIT);
    }

    private static Schema payloadMember(Schema struct) {
        for (Schema member : struct.members()) {
            if (member.hasTrait(TraitKey.EVENT_PAYLOAD_TRAIT)) {
                return member;
            }
        }
        return null;
    }

    private static boolean hasBodyMembers(Schema struct) {
        for (Schema member : struct.members()) {
            if (!isHeader(member)) {
                return true;
            }
        }
        return false;
    }

    private static String mediaType(Schema member, String fallback) {
        var trait = member.memberTarget().getTrait(TraitKey.MEDIA_TYPE_TRAIT);
        return trait != null ? trait.getValue() : fallback;
    }

    /** Captures the set member of a union value. */
    private static final class SetMember extends NoOpSerializer {
        private Schema member;
        private SerializableStruct value;

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            member = schema;
            value = struct;
        }
    }

    /** Collects the header members and the explicit payload of an event structure. */
    private static final class EventParts extends NoOpSerializer {
        private final Map<String, HeaderValue> headers = new LinkedHashMap<>();
        private byte[] payload;
        private SerializableStruct payloadStruct;

        private void header(Schema schema, HeaderValue value) {
            if (isHeader(schema)) {
                headers.put(schema.memberName(), value);
            }
        }

        private static boolean isPayload(Schema schema) {
            return schema.hasTrait(TraitKey.EVENT_PAYLOAD_TRAIT);
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (isPayload(schema)) {
                payloadStruct = struct;
            }
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            header(schema, HeaderValue.ofBoolean(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            header(schema, HeaderValue.ofByte(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            header(schema, HeaderValue.ofShort(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            header(schema, HeaderValue.ofInteger(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            header(schema, HeaderValue.ofLong(value));
        }

        @Override
        public void writeString(Schema schema, String value) {
            if (isPayload(schema)) {
                payload = value.getBytes(StandardCharsets.UTF_8);
            } else {
                header(schema, HeaderValue.ofString(value));
            }
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            byte[] bytes = new byte[value.remaining()];
            value.duplicate().get(bytes);
            if (isPayload(schema)) {
                payload = bytes;
            } else {
                header(schema, HeaderValue.ofByteArray(bytes));
            }
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            header(schema, HeaderValue.ofTimestamp(value));
        }
    }

    /** A structure value that serializes only the members {@code include} accepts. */
    private static final class FilteredStruct implements SerializableStruct {
        private final SerializableStruct struct;
        private final java.util.function.Predicate<Schema> include;

        FilteredStruct(SerializableStruct struct, java.util.function.Predicate<Schema> include) {
            this.struct = struct;
            this.include = include;
        }

        @Override
        public Schema schema() {
            return struct.schema();
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            struct.serializeMembers(new MemberFilter(serializer, include));
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return struct.getMemberValue(member);
        }
    }

    /** The top-level deserializer of an event message into the union. */
    private final class UnionDeserializer extends SpecificShapeDeserializer {
        private final String eventType;
        private final Message message;

        UnionDeserializer(String eventType, Message message) {
            this.eventType = eventType;
            this.message = message;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new IllegalStateException("An event is read into a union, not " + schema.type());
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            Schema member = schema.member(eventType);
            if (member == null) {
                consumer.unknownMember(state, eventType);
                return;
            }
            consumer.accept(state, member, new EventStructDeserializer(message));
        }
    }

    /** Reads an event structure from the headers and payload of a message. */
    private final class EventStructDeserializer extends SpecificShapeDeserializer {
        private final Message message;

        EventStructDeserializer(Message message) {
            this.message = message;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new IllegalStateException("An event is a structure, not " + schema.type());
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            Schema payloadMember = null;
            boolean bodyMembers = false;
            for (Schema member : schema.members()) {
                if (isHeader(member)) {
                    HeaderValue value = message.header(member.memberName());
                    if (value != null) {
                        consumer.accept(state, member, new HeaderDeserializer(value));
                    }
                } else if (member.hasTrait(TraitKey.EVENT_PAYLOAD_TRAIT)) {
                    payloadMember = member;
                } else {
                    bodyMembers = true;
                }
            }
            if (message.payloadLength() == 0) {
                return;
            }
            if (payloadMember != null) {
                ShapeDeserializer value = switch (payloadMember.type()) {
                    case BLOB, STRING, ENUM -> new RawPayloadDeserializer(message.payload());
                    default -> payloads.deserializer(message.payload());
                };
                consumer.accept(state, payloadMember, value);
            } else if (bodyMembers) {
                payloads.deserializer(message.payload()).readStruct(schema, state,
                                                                    skipping(consumer, m -> !isHeader(m)));
            }
        }
    }

    /** Reads the initial-response members from the payload, then sets the event stream member. */
    private final class InitialDeserializer extends SpecificShapeDeserializer {
        private final Message message;
        private final EventStream<?> stream;

        InitialDeserializer(Message message, EventStream<?> stream) {
            this.message = message;
            this.stream = stream;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new IllegalStateException("An initial response is a structure, not " + schema.type());
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            if (message.payloadLength() > 0) {
                payloads.deserializer(message.payload()).readStruct(schema, state,
                                                                    skipping(consumer, m -> !isEventStream(m)));
            }
            for (Schema member : schema.members()) {
                // Without a stream (an async caller receives the events separately) the member stays unset.
                if (stream != null && isEventStream(member)) {
                    consumer.accept(state, member, new StreamValue(stream));
                }
            }
        }
    }

    private static <T> ShapeDeserializer.StructMemberConsumer<T> skipping(
        ShapeDeserializer.StructMemberConsumer<T> consumer, java.util.function.Predicate<Schema> include) {
        return new ShapeDeserializer.StructMemberConsumer<>() {
            @Override
            public void accept(T t, Schema member, ShapeDeserializer de) {
                if (include.test(member)) {
                    consumer.accept(t, member, de);
                }
            }

            @Override
            public void unknownMember(T t, String memberName) {
                consumer.unknownMember(t, memberName);
            }
        };
    }

    /** The value of an event stream member. */
    static final class StreamValue extends SpecificShapeDeserializer {
        private final EventStream<?> stream;

        StreamValue(EventStream<?> stream) {
            this.stream = stream;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public EventStream<? extends SerializableStruct> readEventStream(Schema schema) {
            return (EventStream<? extends SerializableStruct>) stream;
        }
    }

    /** A header value read as a member of the matching type. */
    private static final class HeaderDeserializer extends SpecificShapeDeserializer {
        private final HeaderValue value;

        HeaderDeserializer(HeaderValue value) {
            this.value = value;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new IllegalArgumentException("A " + value.type() + " header is not a " + schema.type());
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean readBoolean(Schema schema) {
            return value.asBoolean();
        }

        @Override
        public byte readByte(Schema schema) {
            return value.asByte();
        }

        @Override
        public short readShort(Schema schema) {
            return value.asShort();
        }

        @Override
        public int readInteger(Schema schema) {
            return value.asInteger();
        }

        @Override
        public long readLong(Schema schema) {
            return value.asLong();
        }

        @Override
        public String readString(Schema schema) {
            return value.asString();
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return ByteBuffer.wrap(value.asByteArray());
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            return value.asTimestamp();
        }
    }

    /** A blob or string payload read as raw bytes. */
    private static final class RawPayloadDeserializer extends SpecificShapeDeserializer {
        private final ByteBuffer payload;

        RawPayloadDeserializer(ByteBuffer payload) {
            this.payload = payload;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return payload.duplicate();
        }

        @Override
        public String readString(Schema schema) {
            return StandardCharsets.UTF_8.decode(payload.duplicate()).toString();
        }
    }
}
