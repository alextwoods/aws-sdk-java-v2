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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.Binding;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.BindingExcludingStruct;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.BindingFilteredStruct;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.EventStreamExt;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.InterceptingSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.serde.event.EventEncoder;
import software.amazon.awssdk.schema.serde.event.EventStreamingException;
import software.amazon.awssdk.utils.Validate;
import software.amazon.eventstream.Message;

/**
 * Encodes events into AWS event stream frames through the event stream union's Schema (Requirement 14.4):
 * {@code eventHeader} members become message headers, the {@code eventPayload} member (raw bytes for a blob, UTF-8 for
 * a string, the codec otherwise) or the remaining members (through the codec) become the body, and
 * {@code :event-type} is the union member name.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.AwsEventShapeEncoder}. SDK additions: an SDK
 * event is the union member's value, not a union (the generated event stream type is an interface), so
 * {@link #encode(SerializableStruct)} also accepts an event whose shape is the target of exactly one union member, and
 * {@link #encodeEvent(String, SerializableStruct)} takes the member name explicitly (the generated
 * {@code sdkEventType()}); a modeled exception is a {@link Throwable} that is also a {@link SerializableStruct}.
 *
 * <p>The member partition of each structure is precomputed once by {@link EventStreamSchemaExtensions} (port of
 * smithy-java {@code 8e892b96c}) instead of walking the members and calling {@code hasTrait} on every message.
 */
@SdkInternalApi
final class AwsEventShapeEncoder implements EventEncoder<AwsEventFrame> {
    private final InitialEventType initialEventType;
    private final Schema eventSchema;
    private final Codec codec;
    private final String payloadMediaType;
    private final boolean emitEmptyPayload;
    private final Set<String> possibleTypes;
    private final Map<ShapeId, Schema> possibleExceptions;
    private final Map<ShapeId, String> memberByTarget;
    private final Function<Throwable, EventStreamingException> exceptionHandler;

    AwsEventShapeEncoder(InitialEventType initialEventType, Schema eventSchema, Codec codec, String payloadMediaType,
                         boolean emitEmptyPayload, Function<Throwable, EventStreamingException> exceptionHandler) {
        this.initialEventType = Validate.paramNotNull(initialEventType, "initialEventType");
        this.eventSchema = Validate.paramNotNull(eventSchema, "eventSchema");
        this.codec = Validate.paramNotNull(codec, "codec");
        this.payloadMediaType = Validate.paramNotNull(payloadMediaType, "payloadMediaType");
        this.emitEmptyPayload = emitEmptyPayload;
        this.possibleTypes = possibleTypes(eventSchema, initialEventType.value());
        this.possibleExceptions = possibleExceptions(eventSchema);
        this.memberByTarget = uniqueMemberByTarget(eventSchema);
        this.exceptionHandler = Validate.paramNotNull(exceptionHandler, "exceptionHandler");
    }

    @Override
    public AwsEventFrame encode(SerializableStruct item) {
        Validate.paramNotNull(item, "item");
        Schema schema = item.sdkSchema();
        if (isInitialEvent(schema)) {
            return encodeMessage(item, null);
        }
        if (schema.id().equals(eventSchema.id())) {
            return encodeMessage(item, null);
        }
        String memberName = memberByTarget.get(schema.id());
        if (memberName == null) {
            throw new IllegalArgumentException("Cannot encode " + schema.id() + " as an event of " + eventSchema.id()
                                               + ": it is not the target of exactly one union member. Call "
                                               + "encodeEvent with the event type.");
        }
        return encodeEvent(memberName, item);
    }

    /**
     * Encodes an event, the value of the union member {@code eventType}.
     *
     * @param eventType the union member name, the {@code :event-type} header value
     * @param event the event
     * @return the frame
     * @throws IllegalArgumentException if the union has no member {@code eventType}
     */
    AwsEventFrame encodeEvent(String eventType, SerializableStruct event) {
        Schema member = eventSchema.member(eventType);
        if (member == null || member.hasTrait(TraitKey.ERROR_TRAIT)) {
            throw new IllegalArgumentException("Event stream " + eventSchema.id() + " has no event " + eventType);
        }
        return encodeMessage(event, member);
    }

    @Override
    public AwsEventFrame encodeFailure(Throwable exception) {
        Schema exceptionSchema = exception instanceof SerializableStruct
                                 ? possibleExceptions.get(((SerializableStruct) exception).sdkSchema().id())
                                 : null;
        if (exceptionSchema != null) {
            HeadersBuilder headers = HeadersBuilder.forException().exceptionType(exceptionSchema.memberName());
            Holder<String> contentTypeHolder = new Holder<>(payloadMediaType);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ((SerializableStruct) exception).serialize(createSerializer(out, headers, contentTypeHolder));
            setContentType(headers, contentTypeHolder);
            return new AwsEventFrame(new Message(headers.build(), out.toByteArray()));
        }
        EventStreamingException es = exceptionHandler.apply(exception);
        HeadersBuilder headers = HeadersBuilder.forError().errorCode(es.getErrorCode()).errorMessage(es.getMessage());
        return new AwsEventFrame(new Message(headers.build(), new byte[0]));
    }

    /**
     * Encodes an event ({@code member} not null), a union value or the initial request or response.
     *
     * <p>The content type can change with an {@code eventPayload} blob or string member; if nothing is written to the
     * payload, the holder is set to null and no {@code :content-type} header is written.
     */
    private AwsEventFrame encodeMessage(SerializableStruct item, Schema member) {
        Holder<String> typeHolder = new Holder<>();
        Holder<String> contentTypeHolder = new Holder<>(payloadMediaType);
        HeadersBuilder headers = HeadersBuilder.forEvent();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ShapeSerializer baseSerializer = createSerializer(out, headers, contentTypeHolder);
        if (member != null) {
            typeHolder.set(member.memberName());
            baseSerializer.writeStruct(member, item);
        } else if (isInitialEvent(item.sdkSchema())) {
            // The initial event is serialized fully instead of a single member as for events.
            typeHolder.set(initialEventType.value());
            Schema schema = item.sdkSchema();
            Binding[] bindings = EventStreamSchemaExtensions.extensionOf(schema).bindings();
            new BindingExcludingStruct(schema, item, bindings, Binding.STREAMING).serialize(baseSerializer);
        } else {
            item.serializeMembers(new SpecificShapeSerializer() {
                @Override
                public void writeStruct(Schema schema, SerializableStruct struct) {
                    String memberName = schema.memberName();
                    if (possibleTypes.contains(memberName) && typeHolder.setIfAbsent(memberName)) {
                        baseSerializer.writeStruct(schema, struct);
                    }
                }
            });
        }
        if (typeHolder.get() == null) {
            throw new SerializationException("The event stream union value of " + eventSchema.id() + " has no member");
        }
        headers.eventType(typeHolder.get());
        setContentType(headers, contentTypeHolder);
        return new AwsEventFrame(new Message(headers.build(), out.toByteArray()));
    }

    private static void setContentType(HeadersBuilder headers, Holder<String> contentTypeHolder) {
        if (contentTypeHolder.get() != null) {
            headers.contentType(contentTypeHolder.get());
        }
    }

    private ShapeSerializer createSerializer(OutputStream out, HeadersBuilder headers, Holder<String> contentType) {
        return new EventSerializer(out, codec, new EventHeaderSerializer(headers), emitEmptyPayload, contentType);
    }

    static Set<String> possibleTypes(Schema eventSchema, String initialEventType) {
        Set<String> result = new HashSet<>();
        for (Schema member : eventSchema.members()) {
            result.add(member.memberName());
        }
        result.add(initialEventType);
        return Collections.unmodifiableSet(result);
    }

    static Map<ShapeId, Schema> possibleExceptions(Schema eventSchema) {
        Map<ShapeId, Schema> result = new HashMap<>();
        for (Schema member : eventSchema.members()) {
            if (member.hasTrait(TraitKey.ERROR_TRAIT)) {
                ShapeId targetId = member.memberTarget().id();
                if (result.put(targetId, member) != null) {
                    throw new IllegalStateException("Duplicate key: " + targetId);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * The union member names by target shape, for the targets that exactly one non-error member targets.
     */
    private static Map<ShapeId, String> uniqueMemberByTarget(Schema eventSchema) {
        Map<ShapeId, String> result = new HashMap<>();
        Set<ShapeId> duplicates = new HashSet<>();
        for (Schema member : eventSchema.members()) {
            if (!member.hasTrait(TraitKey.ERROR_TRAIT)
                && result.put(member.memberTarget().id(), member.memberName()) != null) {
                duplicates.add(member.memberTarget().id());
            }
        }
        result.keySet().removeAll(duplicates);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns true if the schema is of the initial request or response: a structure with an event stream member.
     */
    private static boolean isInitialEvent(Schema schema) {
        // Not extensionOf: a schema that is not a structure or union has no partition, and is not an initial event.
        EventStreamExt ext = schema.getExtension(EventStreamSchemaExtensions.KEY);
        return ext != null && ext.isInitialEvent();
    }

    /**
     * Writes an event into the headers and the payload of the message, by the event serialization traits.
     */
    static final class EventSerializer extends SpecificShapeSerializer {
        private final OutputStream out;
        private final Codec codec;
        private final EventHeaderSerializer headerSerializer;
        private final boolean emitEmptyPayload;
        private final Holder<String> contentTypeHolder;

        EventSerializer(OutputStream out, Codec codec, EventHeaderSerializer headerSerializer,
                        boolean emitEmptyPayload, Holder<String> contentTypeHolder) {
            this.out = out;
            this.codec = codec;
            this.headerSerializer = headerSerializer;
            this.emitEmptyPayload = emitEmptyPayload;
            this.contentTypeHolder = contentTypeHolder;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            // The body choice comes from the schema written (the union member's target), the member filters from the
            // structure's own schema, whose members it writes, as the trait walk this replaces did. Both are the same
            // partition unless the event passed for the member has another shape.
            EventStreamExt ext = EventStreamSchemaExtensions.extensionOf(schema);
            EventStreamExt structExt = EventStreamSchemaExtensions.extensionOf(struct.sdkSchema());
            Binding[] bindings = structExt.bindings();
            if (ext.hasEventPayload()) {
                try (EventPayloadSerializer serializer = new EventPayloadSerializer(out, codec, contentTypeHolder)) {
                    new BindingFilteredStruct(schema, struct, bindings, Binding.EVENT_PAYLOAD)
                        .serializeMembers(serializer);
                }
            } else if (emitEmptyPayload || ext.hasPayloadMembers()) {
                // Keeps writing through the codec serializer into the frame's stream (upstream serializes to a buffer
                // with codec.serialize() and copies it): the serializer takes a pooled buffer for this sink (FU-17).
                try (ShapeSerializer serializer = codec.createSerializer(out)) {
                    new BindingFilteredStruct(schema, struct, bindings, Binding.PAYLOAD).serialize(serializer);
                }
            } else {
                contentTypeHolder.set(null);
            }
            if (structExt.hasHeaderMembers()) {
                new BindingFilteredStruct(schema, struct, bindings, Binding.HEADER).serialize(headerSerializer);
            }
        }
    }

    /**
     * Writes the {@code eventPayload} member as the payload: a blob's bytes unencoded with
     * {@code application/octet-stream}, a string's UTF-8 bytes with {@code text/plain}, anything else through the
     * codec with the protocol's media type.
     */
    static final class EventPayloadSerializer extends InterceptingSerializer {
        private final OutputStream out;
        private final Codec codec;
        private final Holder<String> contentTypeHolder;
        private ShapeSerializer codecSerializer;

        EventPayloadSerializer(OutputStream out, Codec codec, Holder<String> contentTypeHolder) {
            this.out = out;
            this.codec = codec;
            this.contentTypeHolder = contentTypeHolder;
        }

        @Override
        protected ShapeSerializer before(Schema schema) {
            switch (schema.type()) {
                case BLOB:
                    return new SpecificShapeSerializer() {
                        @Override
                        public void writeBlob(Schema blobSchema, ByteBuffer value) {
                            contentTypeHolder.set("application/octet-stream");
                            ByteBuffer copy = value.duplicate();
                            byte[] bytes = new byte[copy.remaining()];
                            copy.get(bytes);
                            write(bytes);
                        }
                    };
                case STRING:
                    return new SpecificShapeSerializer() {
                        @Override
                        public void writeString(Schema stringSchema, String value) {
                            contentTypeHolder.set("text/plain");
                            write(value.getBytes(StandardCharsets.UTF_8));
                        }
                    };
                default:
                    codecSerializer = codec.createSerializer(out);
                    return codecSerializer;
            }
        }

        private void write(byte[] bytes) {
            try {
                out.write(bytes);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void flush() {
            if (codecSerializer != null) {
                codecSerializer.flush();
            }
        }

        @Override
        public void close() {
            if (codecSerializer != null) {
                codecSerializer.close();
            }
        }
    }
}
