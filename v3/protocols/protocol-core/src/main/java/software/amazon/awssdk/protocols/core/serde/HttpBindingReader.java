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

package software.amazon.awssdk.protocols.core.serde;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.awssdk.protocols.core.binding.HttpBindingLocation;
import software.amazon.awssdk.protocols.core.binding.HttpMemberBinding;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.SpecificShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Reads an HTTP response bound with the Smithy HTTP binding traits (via {@link HttpMessageBindings}) into a
 * {@link ShapeBuilder} of an operation output or error: {@code httpHeader}, {@code httpPrefixHeaders},
 * {@code httpResponseCode}, and the body, which is either the {@code httpPayload} member or a protocol document of the
 * {@link HttpBindingLocation#BODY} members. For {@code restJson1} and {@code restXml}; the body document and
 * structured payloads are read by the protocol's codec through a {@link BodyDeserializerFactory}, so this reader is
 * codec-agnostic.
 *
 * <p>Value rules follow the Smithy HTTP binding specification:
 * <ul>
 *   <li>header scalars: booleans {@code true}/{@code false}, numbers in their text form (floats also {@code NaN},
 *       {@code Infinity}, {@code -Infinity}), blobs and {@code mediaType} strings base64, timestamps in the member's
 *       {@code timestampFormat}, else {@code http-date}; a header present several times is read as its values joined
 *       by {@code ", "};</li>
 *   <li>list headers: every field of the name, split at commas outside double quotes, items trimmed, quoted items
 *       unquoted and unescaped ({@code \"}, {@code \\}); {@code http-date} timestamps, which contain a comma, are split
 *       at every second comma;</li>
 *   <li>prefix headers: every header whose name starts with the prefix (case-insensitively), keyed by the rest of the
 *       name as received, first occurrence only; an empty map if no header matches (the official
 *       {@code HttpPrefixHeadersAreNotPresent} restXml response case), never absent;</li>
 *   <li>payload: blob as the raw bytes, string/enum as the UTF-8 text, structure/union/document through the codec;
 *       an empty body leaves the payload member absent;</li>
 *   <li>body members: read through the codec from a non-empty body, members bound elsewhere are ignored; an empty body
 *       has no body members.</li>
 * </ul>
 *
 * <p>Every failure is a located {@link SdkProtocolException}: bound members are reported at their header or the
 * status code, body members at the body with their member path (via {@link LocatingDeserializer}). No partial value
 * is built.
 *
 * <p>A streaming blob payload is read without buffering: the payload member is a {@code DataStream} over the response
 * body ({@link StreamingPayloads#readResponse}), and only headers and the status code are bound around it. For every
 * other payload kind a {@link HttpBody.Kind#STREAMING} response body is a finite message and is buffered first.
 * An event stream payload is likewise never buffered: the payload member is the event stream the caller's
 * {@link EventStreamFactory} creates over the response (see {@code protocols.core.eventstream.EventStreams}).
 *
 * <p>Immutable and thread safe; build once per response structure.
 */
public final class HttpBindingReader {
    /** Creates the protocol codec's deserializer of a message body. */
    @FunctionalInterface
    public interface BodyDeserializerFactory {
        /**
         * A deserializer positioned at the root value of {@code body}.
         *
         * @param body the complete, non-empty body; read-only, owned by the deserializer
         */
        ShapeDeserializer create(ByteBuffer body);
    }

    /** Creates the event stream value of an event stream payload member over the response body. */
    @FunctionalInterface
    public interface EventStreamFactory {
        EventStream<? extends SerializableStruct> create(SdkHttpResponse response);
    }

    private final HttpMessageBindings bindings;
    private final HttpMemberBinding[] byIndex;
    private final boolean hasBodyMembers;

    private HttpBindingReader(HttpMessageBindings bindings) {
        this.bindings = bindings;
        List<Schema> members = bindings.structure().members();
        this.byIndex = new HttpMemberBinding[members.size()];
        for (HttpMemberBinding binding : bindings.bindings()) {
            byIndex[binding.member().memberIndex()] = binding;
        }
        this.hasBodyMembers = !bindings.members(HttpBindingLocation.BODY).isEmpty();
    }

    /**
     * The reader of {@code bindings}.
     *
     * @throws IllegalArgumentException if {@code bindings} are not {@link HttpMessageBindings.Direction#RESPONSE}
     *         bindings
     */
    public static HttpBindingReader of(HttpMessageBindings bindings) {
        Objects.requireNonNull(bindings, "bindings");
        if (bindings.direction() != HttpMessageBindings.Direction.RESPONSE) {
            throw new IllegalArgumentException("HttpBindingReader reads responses, not " + bindings.direction()
                                               + " bindings of " + bindings.structure().id());
        }
        return new HttpBindingReader(bindings);
    }

    /** The bindings this reader reads. */
    public HttpMessageBindings bindings() {
        return bindings;
    }

    /**
     * Reads {@code response} into {@code builder}, then error-corrects and builds it.
     *
     * @param context the protocol and operation, for failure locations
     * @param response the response; a streaming body is buffered unless the payload is a streaming blob
     * @param body the codec's body deserializer factory
     * @param builder a fresh builder of {@link #bindings()}' structure
     * @param bodyTimestamps the timestamp reader of the codec's body values, or {@code null} to use the codec's; never
     *        applied to header values
     * @return the built value
     * @throws SdkProtocolException if the response violates the protocol or schema
     * @throws UnsupportedOperationException if the payload is an event stream (use the overload with an
     *         {@link EventStreamFactory})
     */
    public <T extends SerializableShape> T read(WireContext context, SdkHttpResponse response,
                                                BodyDeserializerFactory body, ShapeBuilder<T> builder,
                                                LocatingDeserializer.TimestampReader bodyTimestamps) {
        return read(context, response, body, builder, bodyTimestamps, null);
    }

    /**
     * {@link #read}, with {@code eventStreams} creating the value of an event stream payload member.
     *
     * @param eventStreams the event stream factory, or {@code null} if the payload is not an event stream
     */
    public <T extends SerializableShape> T read(WireContext context, SdkHttpResponse response,
                                                BodyDeserializerFactory body, ShapeBuilder<T> builder,
                                                LocatingDeserializer.TimestampReader bodyTimestamps,
                                                EventStreamFactory eventStreams) {
        return LocatingDeserializer.build(context, WireContext.BODY,
                                          readInto(context, response, body, builder, bodyTimestamps, eventStreams));
    }

    /**
     * Reads {@code response} into {@code builder} without building it, so that the caller can add protocol-derived
     * values (for example error metadata) first. See {@link #read}.
     *
     * @return {@code builder}
     */
    public <B extends ShapeBuilder<?>> B readInto(WireContext context, SdkHttpResponse response,
                                                  BodyDeserializerFactory body, B builder,
                                                  LocatingDeserializer.TimestampReader bodyTimestamps) {
        return readInto(context, response, body, builder, bodyTimestamps, null);
    }

    /**
     * {@link #read} of a streaming-output response whose payload the caller consumes: every member is bound except
     * the streaming blob or event stream payload member, which is not set on {@code builder}; the response body is
     * not read.
     *
     * @throws IllegalArgumentException if these bindings have no streaming blob or event stream payload
     */
    public <T extends SerializableShape> T readWithoutStreamingPayload(
        WireContext context, SdkHttpResponse response, BodyDeserializerFactory body, ShapeBuilder<T> builder,
        LocatingDeserializer.TimestampReader bodyTimestamps) {
        if (bindings.payloadKind() != HttpMessageBindings.PayloadKind.STREAMING_BLOB
            && bindings.payloadKind() != HttpMessageBindings.PayloadKind.EVENT_STREAM) {
            throw new IllegalArgumentException(bindings.structure().id() + " has no streaming payload");
        }
        return LocatingDeserializer.build(context, WireContext.BODY,
                                          readInto(context, response, body, builder, bodyTimestamps, null, true));
    }

    /** {@link #readInto}, with {@code eventStreams} creating the value of an event stream payload member. */
    public <B extends ShapeBuilder<?>> B readInto(WireContext context, SdkHttpResponse response,
                                                  BodyDeserializerFactory body, B builder,
                                                  LocatingDeserializer.TimestampReader bodyTimestamps,
                                                  EventStreamFactory eventStreams) {
        return readInto(context, response, body, builder, bodyTimestamps, eventStreams, false);
    }

    private <B extends ShapeBuilder<?>> B readInto(WireContext context, SdkHttpResponse response,
                                                   BodyDeserializerFactory body, B builder,
                                                   LocatingDeserializer.TimestampReader bodyTimestamps,
                                                   EventStreamFactory eventStreams, boolean skipStreamingPayload) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(builder, "builder");
        if (!builder.schema().id().equals(bindings.structure().id())) {
            throw new IllegalArgumentException("The builder builds " + builder.schema().id() + ", not "
                                               + bindings.structure().id());
        }
        HttpMessageBindings.PayloadKind kind = bindings.payloadKind();
        if (kind == HttpMessageBindings.PayloadKind.EVENT_STREAM && eventStreams == null && !skipStreamingPayload) {
            throw new UnsupportedOperationException("The event stream payload of " + bindings.structure().id()
                                                    + " needs an EventStreamFactory");
        }
        SdkHttpResponse message = kind == HttpMessageBindings.PayloadKind.STREAMING_BLOB
                                  || kind == HttpMessageBindings.PayloadKind.EVENT_STREAM
                                  ? response : buffered(response);
        LocatingDeserializer.TimestampReader timestamps = (schema, delegate) -> {
            if (delegate instanceof BoundValue || bodyTimestamps == null) {
                return delegate.readTimestamp(schema);
            }
            return bodyTimestamps.read(schema, delegate);
        };
        return LocatingDeserializer.deserializeInto(context, WireContext.BODY,
                                                    new MessageDeserializer(context, message, body, eventStreams,
                                                                            skipStreamingPayload),
                                                    builder,
                                                    timestamps);
    }

    /** {@code response} with its body buffered, if it streams (see {@link HttpBody#buffered()}). */
    public static SdkHttpResponse buffered(SdkHttpResponse response) {
        return response.body().kind() == HttpBody.Kind.STREAMING
               ? response.toBuilder().body(response.body().buffered()).build() : response;
    }

    private boolean isBodyMember(Schema member) {
        int index = member.memberIndex();
        return index >= 0 && index < byIndex.length && byIndex[index] != null
               && byIndex[index].location() == HttpBindingLocation.BODY;
    }

    /** The top-level deserializer of a response structure. */
    private final class MessageDeserializer extends SpecificShapeDeserializer {
        private final WireContext context;
        private final SdkHttpResponse response;
        private final BodyDeserializerFactory factory;
        private final EventStreamFactory eventStreams;
        private final boolean skipStreamingPayload;

        MessageDeserializer(WireContext context, SdkHttpResponse response, BodyDeserializerFactory factory,
                            EventStreamFactory eventStreams, boolean skipStreamingPayload) {
            this.context = context;
            this.response = response;
            this.factory = factory;
            this.eventStreams = eventStreams;
            this.skipStreamingPayload = skipStreamingPayload;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new IllegalStateException("HTTP bindings are read into a structure, not " + schema.type());
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            HttpHeaders headers = response.headers();
            for (HttpMemberBinding binding : bindings.members(HttpBindingLocation.HEADER)) {
                List<String> values = headers.allValues(binding.name());
                if (values.isEmpty()) {
                    continue;
                }
                Schema member = binding.member();
                ShapeDeserializer value = member.type() == ShapeType.LIST
                                          ? new BoundList(splitHeaderList(member, values))
                                          : new BoundValue(values.size() == 1 ? values.get(0)
                                                                              : String.join(", ", values));
                bound(state, consumer, binding, value, WireContext.header(binding.name()));
            }
            for (HttpMemberBinding binding : bindings.members(HttpBindingLocation.PREFIX_HEADERS)) {
                // Always present, empty when no header matches: the official restXml response case
                // HttpPrefixHeadersAreNotPresent expects fooMap: {} for a response without x-foo-* headers.
                bound(state, consumer, binding, new BoundMap(prefixHeaders(headers, binding.name())),
                      WireContext.header(binding.name() + "*"));
            }
            for (HttpMemberBinding binding : bindings.members(HttpBindingLocation.STATUS_CODE)) {
                bound(state, consumer, binding, new BoundValue(Integer.toString(response.statusCode())),
                      WireContext.STATUS_CODE);
            }
            if (bindings.payloadKind() == HttpMessageBindings.PayloadKind.STREAMING_BLOB) {
                // The only body value; never buffered, and left to the caller when it consumes the payload.
                if (!skipStreamingPayload) {
                    Schema member = bindings.payload().orElseThrow().member();
                    consumer.accept(state, member, new StreamPayload(StreamingPayloads.readResponse(response)));
                }
                return;
            }
            if (bindings.payloadKind() == HttpMessageBindings.PayloadKind.EVENT_STREAM) {
                // The only body value; read incrementally by the event stream reader, or left to an async caller.
                if (!skipStreamingPayload) {
                    Schema member = bindings.payload().orElseThrow().member();
                    consumer.accept(state, member, new EventStreamPayload(eventStreams.create(response)));
                }
                return;
            }
            ByteBuffer body = response.body().asByteBuffer();
            if (!body.hasRemaining()) {
                return;
            }
            bindings.payload().ifPresent(payload -> readPayload(state, consumer, payload.member(), body));
            if (hasBodyMembers) {
                factory.create(body).readStruct(schema, state, new StructMemberConsumer<T>() {
                    @Override
                    public void accept(T t, Schema member, ShapeDeserializer de) {
                        if (isBodyMember(member)) {
                            consumer.accept(t, member, de);
                        }
                    }

                    @Override
                    public void unknownMember(T t, String memberName) {
                        consumer.unknownMember(t, memberName);
                    }
                });
            }
        }

        private <T> void readPayload(T state, StructMemberConsumer<T> consumer, Schema member, ByteBuffer body) {
            switch (bindings.payloadKind()) {
                case BLOB, STRING -> consumer.accept(state, member, new PayloadValue(body));
                case STRUCTURED -> consumer.accept(state, member, factory.create(body));
                default -> throw new IllegalStateException("Unexpected payload kind " + bindings.payloadKind());
            }
        }

        /** Reads one bound member, reporting any failure at its wire location. */
        private <T> void bound(T state, StructMemberConsumer<T> consumer, HttpMemberBinding binding,
                               ShapeDeserializer value, String wireLocation) {
            try {
                consumer.accept(state, binding.member(), value);
            } catch (SdkProtocolException e) {
                throw e;
            } catch (RuntimeException e) {
                Schema target = binding.member().memberTarget();
                throw context.failure(target.id(), List.of(binding.memberName()), wireLocation,
                                      "the value is not a valid " + target.type() + " ("
                                      + e.getClass().getSimpleName() + ")");
            }
        }
    }

    /**
     * The items of a list header: every field value split at commas outside double quotes (at every second comma for
     * {@code http-date} timestamps), trimmed and unquoted.
     */
    static List<String> splitHeaderList(Schema listMember, List<String> values) {
        Schema element = listMember.memberTarget().listMember();
        boolean httpDates = element.type() == ShapeType.TIMESTAMP
                            && TimestampFormats.formatOf(element, TimestampFormats.formatOf(
                                listMember, TimestampFormatTrait.Format.HTTP_DATE))
                               == TimestampFormatTrait.Format.HTTP_DATE;
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (httpDates) {
                splitHttpDates(value, out);
            } else {
                splitQuoted(value, out);
            }
        }
        return out;
    }

    private static void splitQuoted(String value, List<String> out) {
        StringBuilder item = new StringBuilder();
        boolean quoted = false;
        boolean wasQuoted = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quoted) {
                if (c == '\\' && i + 1 < value.length()) {
                    item.append(value.charAt(++i));
                } else if (c == '"') {
                    quoted = false;
                } else {
                    item.append(c);
                }
            } else if (c == '"') {
                if (!wasQuoted && item.toString().isBlank()) {
                    item.setLength(0);
                }
                quoted = true;
                wasQuoted = true;
            } else if (c == ',') {
                out.add(wasQuoted ? item.toString() : item.toString().trim());
                item.setLength(0);
                wasQuoted = false;
            } else if (!wasQuoted) {
                item.append(c);
            }
        }
        if (quoted) {
            throw new SerializationException("Unterminated quoted header list item");
        }
        out.add(wasQuoted ? item.toString() : item.toString().trim());
    }

    private static void splitHttpDates(String value, List<String> out) {
        int commas = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == ',' && ++commas % 2 == 0) {
                out.add(value.substring(start, i).trim());
                start = i + 1;
            }
        }
        out.add(value.substring(start).trim());
    }

    private static Map<String, String> prefixHeaders(HttpHeaders headers, String prefix) {
        Map<String, String> out = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            if (name.length() >= prefix.length() && name.regionMatches(true, 0, prefix, 0, prefix.length())
                && seen.add(name.toLowerCase(Locale.ROOT))) {
                out.put(name.substring(prefix.length()), headers.value(i));
            }
        }
        return out;
    }

    /** One header or status-code value as text. */
    private static final class BoundValue implements ShapeDeserializer {
        private final String value;

        BoundValue(String value) {
            this.value = value;
        }

        @Override
        public boolean readBoolean(Schema schema) {
            return switch (value) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new SerializationException("Not a boolean");
            };
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return ByteBuffer.wrap(Base64.getDecoder().decode(value));
        }

        @Override
        public byte readByte(Schema schema) {
            return Byte.parseByte(value);
        }

        @Override
        public short readShort(Schema schema) {
            return Short.parseShort(value);
        }

        @Override
        public int readInteger(Schema schema) {
            return Integer.parseInt(value);
        }

        @Override
        public long readLong(Schema schema) {
            return Long.parseLong(value);
        }

        @Override
        public float readFloat(Schema schema) {
            return (float) readDouble(schema);
        }

        @Override
        public double readDouble(Schema schema) {
            return switch (value) {
                case "NaN" -> Double.NaN;
                case "Infinity" -> Double.POSITIVE_INFINITY;
                case "-Infinity" -> Double.NEGATIVE_INFINITY;
                default -> {
                    requireDecimal();
                    yield Double.parseDouble(value);
                }
            };
        }

        /** Rejects Java-only forms such as hexadecimal, {@code 1f} or {@code 1d}. */
        private void requireDecimal() {
            new BigDecimal(value);
        }

        @Override
        public BigInteger readBigInteger(Schema schema) {
            return new BigInteger(value);
        }

        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            return new BigDecimal(value);
        }

        @Override
        public String readString(Schema schema) {
            if (schema.hasTrait(TraitKey.MEDIA_TYPE_TRAIT)) {
                return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            }
            return value;
        }

        @Override
        public Document readDocument() {
            throw new SerializationException("Documents cannot be bound to headers");
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            return TimestampFormats.read(value, TimestampFormats.formatOf(schema,
                                                                          TimestampFormatTrait.Format.HTTP_DATE));
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            throw new SerializationException("Structures cannot be bound to headers");
        }

        @Override
        public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
            throw new SerializationException("A list header is read as a list member");
        }

        @Override
        public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
            throw new SerializationException("Maps cannot be bound to headers");
        }

        @Override
        public boolean isNull() {
            return false;
        }
    }

    /** The items of a list header. */
    private static final class BoundList extends SpecificShapeDeserializer {
        private final List<String> items;

        BoundList(List<String> items) {
            this.items = items;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new SerializationException("A list header cannot be read as a " + schema.type());
        }

        @Override
        public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
            for (String item : items) {
                consumer.accept(state, new BoundValue(item));
            }
        }

        @Override
        public int containerSize() {
            return items.size();
        }

        @Override
        public boolean isNull() {
            return false;
        }
    }

    /** The entries of a prefix-headers map. */
    private static final class BoundMap extends SpecificShapeDeserializer {
        private final Map<String, String> entries;

        BoundMap(Map<String, String> entries) {
            this.entries = entries;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new SerializationException("Prefix headers cannot be read as a " + schema.type());
        }

        @Override
        public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                consumer.accept(state, entry.getKey(), new BoundValue(entry.getValue()));
            }
        }

        @Override
        public int containerSize() {
            return entries.size();
        }

        @Override
        public boolean isNull() {
            return false;
        }
    }

    /** A streaming blob payload. */
    /** The event stream value of an event stream payload member. */
    private static final class EventStreamPayload extends SpecificShapeDeserializer {
        private final EventStream<? extends SerializableStruct> stream;

        EventStreamPayload(EventStream<? extends SerializableStruct> stream) {
            this.stream = stream;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public EventStream<? extends SerializableStruct> readEventStream(Schema schema) {
            return stream;
        }
    }

    private static final class StreamPayload extends SpecificShapeDeserializer {
        private final DataStream stream;

        StreamPayload(DataStream stream) {
            this.stream = stream;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new SerializationException("A streaming payload cannot be read as a " + schema.type());
        }

        @Override
        public DataStream readDataStream(Schema schema) {
            return stream;
        }

        @Override
        public boolean isNull() {
            return false;
        }
    }

    /** A blob or text payload: the whole body. */
    private static final class PayloadValue extends SpecificShapeDeserializer {
        private final ByteBuffer body;

        PayloadValue(ByteBuffer body) {
            this.body = body;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new SerializationException("A raw payload cannot be read as a " + schema.type());
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return body.duplicate();
        }

        @Override
        public String readString(Schema schema) {
            return StandardCharsets.UTF_8.decode(body.duplicate()).toString();
        }

        @Override
        public boolean isNull() {
            return false;
        }
    }
}
