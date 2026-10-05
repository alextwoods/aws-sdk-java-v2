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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.UriEncoding;
import software.amazon.awssdk.protocols.core.binding.HttpBindingLocation;
import software.amazon.awssdk.protocols.core.binding.HttpMemberBinding;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.awssdk.protocols.core.binding.HttpOperationBindings;
import software.amazon.awssdk.protocols.core.binding.UriSegment;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Writes the HTTP-bound members of a request (Smithy HTTP binding traits, via {@link HttpMessageBindings}) into an
 * {@link SdkHttpRequest.Builder}: URI labels, the {@code http} trait's query literals, {@code httpQuery} and
 * {@code httpQueryParams} parameters, {@code httpHeader} and {@code httpPrefixHeaders} fields. For {@code restJson1}
 * and {@code restXml}; the body ({@link HttpBindingLocation#BODY}/{@link HttpBindingLocation#PAYLOAD} members) is
 * written by the protocol's codec, for example through {@link #bodyMembers}.
 *
 * <p>Value rendering follows the Smithy HTTP binding rules: booleans and numbers in their canonical text (floats
 * {@code NaN}, {@code Infinity}, {@code -Infinity}), blobs and {@code mediaType} strings in headers as base64,
 * timestamps in the member's {@code timestampFormat} or the location default ({@link TimestampFormats#defaultFormat}),
 * list headers joined by {@code ", "} with values containing {@code ,} or {@code "} quoted, labels percent-encoded
 * (greedy labels keep {@code /}), and {@code httpQuery} parameters taking precedence over {@code httpQueryParams}
 * entries of the same name.
 *
 * <p>Immutable and thread safe; build once per operation.
 */
public final class HttpBindingWriter {
    private final HttpOperationBindings plan;
    private final HttpMemberBinding[] byIndex;
    private final Set<String> queryNames;
    private final Set<String> headerNames;

    private HttpBindingWriter(HttpOperationBindings plan) {
        this.plan = plan;
        HttpMessageBindings request = plan.request();
        List<Schema> members = request.structure().members();
        this.byIndex = new HttpMemberBinding[members.size()];
        for (HttpMemberBinding binding : request.bindings()) {
            byIndex[binding.member().memberIndex()] = binding;
        }
        Set<String> names = new HashSet<>();
        for (HttpMemberBinding binding : request.members(HttpBindingLocation.QUERY)) {
            names.add(binding.name());
        }
        this.queryNames = Set.copyOf(names);
        Set<String> headers = new HashSet<>();
        for (HttpMemberBinding binding : request.members(HttpBindingLocation.HEADER)) {
            headers.add(binding.name().toLowerCase(Locale.ROOT));
        }
        this.headerNames = Set.copyOf(headers);
    }

    /** The writer of {@code plan}'s request bindings. */
    public static HttpBindingWriter of(HttpOperationBindings plan) {
        return new HttpBindingWriter(plan);
    }

    /**
     * Writes {@code input}'s bound members into {@code request}: sets its encoded path from the {@code http} trait's
     * URI (joined to {@code basePath}), appends query parameters and headers.
     *
     * @throws IllegalArgumentException if a label member is absent or empty (its value is never included)
     */
    public void write(SerializableStruct input, String basePath, SdkHttpRequest.Builder request) {
        Collector collector = new Collector();
        input.serializeMembers(collector);
        StringBuilder path = new StringBuilder();
        for (UriSegment segment : plan.path()) {
            path.append('/');
            if (!segment.isLabel()) {
                path.append(segment.content());
                continue;
            }
            String value = collector.labels.get(segment.content());
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("The URI label member " + segment.content() + " of "
                                                   + input.schema().id() + " is absent or empty");
            }
            path.append(UriEncoding.encode(value, segment.kind() == UriSegment.Kind.GREEDY_LABEL));
        }
        request.encodedPath(RequestTargets.joinPath(basePath, path.isEmpty() ? "/" : path.toString()));
        for (Map.Entry<String, String> literal : plan.queryLiterals().entrySet()) {
            request.addQueryParameter(literal.getKey(), literal.getValue().isEmpty() ? null : literal.getValue());
        }
        for (String[] parameter : collector.query) {
            request.addQueryParameter(parameter[0], parameter[1]);
        }
        for (String[] parameter : collector.queryParams) {
            if (!queryNames.contains(parameter[0])) {
                request.addQueryParameter(parameter[0], parameter[1]);
            }
        }
        for (String[] header : collector.headers) {
            request.addHeader(header[0], header[1]);
        }
    }

    /**
     * A view of {@code input} that serializes only its {@link HttpBindingLocation#BODY} members, for a codec to write
     * as the request body document.
     */
    public SerializableStruct bodyMembers(SerializableStruct input) {
        return new FilteredMembers(input, byIndex, HttpBindingLocation.BODY);
    }

    /**
     * A view of {@code input} that serializes only its {@code httpPayload} member (nothing if it has none or it is
     * absent), for a protocol to write as the request body: the member's value is written directly to the serializer,
     * for example {@code writeStruct(member, value)} or {@code writeBlob(member, bytes)}.
     */
    public SerializableStruct payloadMember(SerializableStruct input) {
        return new FilteredMembers(input, byIndex, HttpBindingLocation.PAYLOAD);
    }

    private HttpMemberBinding binding(Schema member) {
        int index = member.memberIndex();
        return index >= 0 && index < byIndex.length ? byIndex[index] : null;
    }

    /** Header list rendering: quote values containing a comma or double quote. */
    static String headerListItem(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0) {
            return value;
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /** Collects the bound values of one input. */
    private final class Collector extends SpecificShapeSerializer {
        private final Map<String, String> labels = new HashMap<>();
        private final List<String[]> query = new ArrayList<>();
        private final List<String[]> queryParams = new ArrayList<>();
        private final List<String[]> headers = new ArrayList<>();

        private void scalar(Schema member, String text) {
            HttpMemberBinding binding = binding(member);
            if (binding == null) {
                return;
            }
            switch (binding.location()) {
                case LABEL -> labels.put(binding.name(), text);
                case QUERY -> query.add(new String[] {binding.name(), text});
                case HEADER -> headers.add(new String[] {binding.name(), text});
                default -> {
                    // Body, payload and status-code members are not written here.
                }
            }
        }

        private void timestamp(Schema member, Instant value) {
            HttpMemberBinding binding = binding(member);
            if (binding != null && isBound(binding)) {
                scalar(member, formatTimestamp(member, binding.location(), value));
            }
        }

        private boolean isBound(HttpMemberBinding binding) {
            return binding.location() == HttpBindingLocation.LABEL || binding.location() == HttpBindingLocation.QUERY
                   || binding.location() == HttpBindingLocation.HEADER;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            // Structured members are body or payload members.
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            // Document members are body or payload members.
        }

        @Override
        public void writeNull(Schema schema) {
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            scalar(schema, Boolean.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            scalar(schema, Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            scalar(schema, Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            scalar(schema, Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            scalar(schema, Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            scalar(schema, formatFloat(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            scalar(schema, formatDouble(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            scalar(schema, value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            scalar(schema, value.toPlainString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            HttpMemberBinding binding = binding(schema);
            if (binding != null && binding.location() == HttpBindingLocation.HEADER
                && schema.hasTrait(TraitKey.MEDIA_TYPE_TRAIT)) {
                scalar(schema, Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
            } else {
                scalar(schema, value);
            }
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            HttpMemberBinding binding = binding(schema);
            if (binding != null && isBound(binding)) {
                scalar(schema, base64(value));
            }
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            timestamp(schema, value);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            HttpMemberBinding binding = binding(schema);
            if (binding == null) {
                return;
            }
            if (binding.location() == HttpBindingLocation.QUERY) {
                List<String> values = new ArrayList<>();
                consumer.accept(listState, new TextCollector(values, schema, binding.location()));
                for (String value : values) {
                    query.add(new String[] {binding.name(), value});
                }
            } else if (binding.location() == HttpBindingLocation.HEADER) {
                List<String> values = new ArrayList<>();
                consumer.accept(listState, new TextCollector(values, schema, binding.location()));
                StringBuilder joined = new StringBuilder();
                for (String value : values) {
                    if (!joined.isEmpty()) {
                        joined.append(", ");
                    }
                    boolean timestamps = schema.listMember().memberTarget().type() == ShapeType.TIMESTAMP;
                    joined.append(timestamps ? value : headerListItem(value));
                }
                headers.add(new String[] {binding.name(), joined.toString()});
            }
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            HttpMemberBinding binding = binding(schema);
            if (binding == null) {
                return;
            }
            HttpBindingLocation location = binding.location();
            if (location != HttpBindingLocation.QUERY_PARAMS && location != HttpBindingLocation.PREFIX_HEADERS) {
                return;
            }
            Map<String, List<String>> entries = new LinkedHashMap<>();
            consumer.accept(mapState, new MapSerializer() {
                @Override
                public <V> void writeEntry(Schema keySchema, String key, V state,
                                           BiConsumer<V, ShapeSerializer> valueSerializer) {
                    List<String> values = new ArrayList<>();
                    valueSerializer.accept(state, new TextCollector(values, schema.mapValueMember(), location));
                    entries.put(key, values);
                }
            });
            for (Map.Entry<String, List<String>> entry : entries.entrySet()) {
                for (String value : entry.getValue()) {
                    if (location == HttpBindingLocation.QUERY_PARAMS) {
                        queryParams.add(new String[] {entry.getKey(), value});
                    } else {
                        String name = binding.name() + entry.getKey();
                        // An httpHeader member of the same name takes precedence over the prefix-headers entry.
                        if (!headerNames.contains(name.toLowerCase(Locale.ROOT))) {
                            headers.add(new String[] {name, value});
                        }
                    }
                }
            }
        }
    }

    /** Renders scalar (and nested list) values as text. */
    private static final class TextCollector extends SpecificShapeSerializer {
        private final List<String> out;
        private final Schema container;
        private final HttpBindingLocation location;

        TextCollector(List<String> out, Schema container, HttpBindingLocation location) {
            this.out = out;
            this.container = container;
            this.location = location;
        }

        @Override
        public void writeNull(Schema schema) {
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            out.add(Boolean.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            out.add(Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            out.add(Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            out.add(Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            out.add(Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            out.add(formatFloat(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            out.add(formatDouble(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            out.add(value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            out.add(value.toPlainString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            out.add(value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            out.add(base64(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            out.add(formatTimestamp(schema.isMember() ? schema : container, location, value));
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            consumer.accept(listState, new TextCollector(out, schema.listMember(), location));
        }
    }

    private static String formatTimestamp(Schema member, HttpBindingLocation location, Instant value) {
        TimestampFormatTrait.Format format = TimestampFormats.formatOf(member, TimestampFormats.defaultFormat(
            location == HttpBindingLocation.PREFIX_HEADERS ? HttpBindingLocation.HEADER : location));
        return TimestampFormats.write(value, format);
    }

    /** A float in its shortest decimal form ({@code 1.1f} is {@code 1.1}, not its widened double value). */
    static String formatFloat(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return formatDouble(value);
        }
        // -0.0 keeps its sign ("-0.0"): only other integral values are written as integers.
        if (value == Math.rint(value) && Math.abs(value) < 1e15 && Float.compare(value, -0.0f) != 0) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    static String formatDouble(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        // -0.0 keeps its sign ("-0.0"): only other integral values are written as integers.
        if (value == Math.rint(value) && Math.abs(value) < 1e15 && Double.compare(value, -0.0) != 0) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String base64(ByteBuffer value) {
        ByteBuffer view = value.duplicate();
        byte[] bytes = new byte[view.remaining()];
        view.get(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Serializes only the members of a struct bound to one location. */
    private static final class FilteredMembers implements SerializableStruct {
        private final SerializableStruct delegate;
        private final HttpMemberBinding[] byIndex;
        private final HttpBindingLocation location;

        FilteredMembers(SerializableStruct delegate, HttpMemberBinding[] byIndex, HttpBindingLocation location) {
            this.delegate = delegate;
            this.byIndex = byIndex;
            this.location = location;
        }

        @Override
        public Schema schema() {
            return delegate.schema();
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            delegate.serializeMembers(new MemberFilter(serializer, byIndex, location));
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return delegate.getMemberValue(member);
        }
    }

    /** Forwards writes of the members bound to one location only. */
    private static final class MemberFilter implements ShapeSerializer {
        private final ShapeSerializer delegate;
        private final HttpMemberBinding[] byIndex;
        private final HttpBindingLocation location;

        MemberFilter(ShapeSerializer delegate, HttpMemberBinding[] byIndex, HttpBindingLocation location) {
            this.delegate = delegate;
            this.byIndex = byIndex;
            this.location = location;
        }

        /** Members without a binding (not of the bound structure) are treated as body members. */
        private boolean body(Schema member) {
            int index = member.memberIndex();
            if (index < 0 || index >= byIndex.length || byIndex[index] == null) {
                return location == HttpBindingLocation.BODY;
            }
            return byIndex[index].location() == location;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (body(schema)) {
                delegate.writeStruct(schema, struct);
            }
        }

        @Override
        public <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
            if (body(schema)) {
                delegate.writeList(schema, state, size, consumer);
            }
        }

        @Override
        public <T> void writeMap(Schema schema, T state, int size, BiConsumer<T, MapSerializer> consumer) {
            if (body(schema)) {
                delegate.writeMap(schema, state, size, consumer);
            }
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            if (body(schema)) {
                delegate.writeBoolean(schema, value);
            }
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            if (body(schema)) {
                delegate.writeByte(schema, value);
            }
        }

        @Override
        public void writeShort(Schema schema, short value) {
            if (body(schema)) {
                delegate.writeShort(schema, value);
            }
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            if (body(schema)) {
                delegate.writeInteger(schema, value);
            }
        }

        @Override
        public void writeLong(Schema schema, long value) {
            if (body(schema)) {
                delegate.writeLong(schema, value);
            }
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            if (body(schema)) {
                delegate.writeFloat(schema, value);
            }
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            if (body(schema)) {
                delegate.writeDouble(schema, value);
            }
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            if (body(schema)) {
                delegate.writeBigInteger(schema, value);
            }
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            if (body(schema)) {
                delegate.writeBigDecimal(schema, value);
            }
        }

        @Override
        public void writeString(Schema schema, String value) {
            if (body(schema)) {
                delegate.writeString(schema, value);
            }
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            if (body(schema)) {
                delegate.writeBlob(schema, value);
            }
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            if (body(schema)) {
                delegate.writeTimestamp(schema, value);
            }
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            if (body(schema)) {
                delegate.writeDocument(schema, value);
            }
        }

        @Override
        public void writeNull(Schema schema) {
            if (body(schema)) {
                delegate.writeNull(schema);
            }
        }

        @Override
        public void writeDataStream(Schema schema, DataStream value) {
            if (body(schema)) {
                delegate.writeDataStream(schema, value);
            }
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
            if (body(schema)) {
                delegate.writeEventStream(schema, value);
            }
        }
    }
}
