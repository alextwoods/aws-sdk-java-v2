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

package software.amazon.awssdk.httpbinding.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.httpbinding.HeaderErrorSerializer;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.Binding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.DirectionBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.HeaderPlan;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.MemberBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.OperationBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.RequestBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.ResponseBinding;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Generic HTTP binding serializer. It writes the bound members to the path, query string, headers, status code and
 * payload, and hands the unbound members to the protocol's codec as the body.
 *
 * <p>The top-level shape written must be a structure; anything else throws {@link IllegalStateException}. The results
 * are read through the accessors after {@link SerializableStruct#serialize} returns. One instance serializes one
 * message.
 *
 * <p>Port of smithy-java {@code HttpBindingSerializer}, writing into SDK header and raw query maps instead of
 * {@code ModifiableHttpHeaders}/{@code QueryStringBuilder}.
 */
@SdkInternalApi
public final class HttpBindingSerializer extends SpecificShapeSerializer {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String DEFAULT_BLOB_CONTENT_TYPE = "application/octet-stream";
    private static final String DEFAULT_STRING_CONTENT_TYPE = "text/plain";
    private static final int DEFAULT_ERROR_STATUS = 500;

    private final OperationBinding operationBinding;
    private final Codec payloadCodec;
    private final String payloadMediaType;
    private final boolean isResponse;
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private boolean omitEmptyPayload;
    private boolean allowEmptyStructPayload;
    private boolean trimGreedyLabelLeadingSlash;
    private HeaderErrorSerializer errorSerializer;
    private ByteBufferOutputStream bodyBuffer;

    private Map<String, List<String>> rawQueryParameters;
    // Names written by @httpQuery members and query literals; @httpQueryParams entries with these names are dropped.
    private Set<String> httpQueryKeys;
    // @httpQueryParams entries as (name, value) pairs, applied after every member is written.
    private List<String> deferredQueryParams;

    private SerializableStruct outerStruct;
    private Binding[] activeBindings;
    private MemberBinding[] memberBindings;
    private HeaderPlan headerPlan;
    private PathSerializer pathSerializer;
    private Schema payloadMember;
    private PayloadSerializer payloadSerializer;
    private Schema blobPayloadSchema;

    private ContentStreamProvider body;
    private long bodyLength = -1;
    private int responseStatus;

    /**
     * @param operationSchema the operation schema, which carries {@code smithy.api#http}
     * @param payloadCodec the codec for the body and structured payloads
     * @param payloadMediaType the media type of codec output, sent as {@code Content-Type}
     * @param isResponse true to write a response (status code, response bindings), false for a request
     */
    public HttpBindingSerializer(Schema operationSchema, Codec payloadCodec, String payloadMediaType,
                                 boolean isResponse) {
        this.operationBinding = HttpBindingSchemaExtensions.operationBindingOf(operationSchema);
        this.payloadCodec = payloadCodec;
        this.payloadMediaType = payloadMediaType;
        this.isResponse = isResponse;
        this.responseStatus = operationBinding.defaultResponseStatus();
    }

    /**
     * When true, no body is written for a structure without body members or payload member.
     */
    public HttpBindingSerializer omitEmptyPayload(boolean omitEmptyPayload) {
        this.omitEmptyPayload = omitEmptyPayload;
        return this;
    }

    /**
     * When true, the codec's empty structure is written as the body even when the structure has a payload member; a
     * non-null payload replaces it.
     */
    public HttpBindingSerializer allowEmptyStructPayload(boolean allowEmptyStructPayload) {
        this.allowEmptyStructPayload = allowEmptyStructPayload;
        return this;
    }

    /**
     * When true, one leading {@code /} of a greedy label value is dropped before it is encoded, as the earlier restJson1
     * marshallers do. SDK addition.
     */
    public HttpBindingSerializer trimGreedyLabelLeadingSlash(boolean trimGreedyLabelLeadingSlash) {
        this.trimGreedyLabelLeadingSlash = trimGreedyLabelLeadingSlash;
        return this;
    }

    /**
     * Marks the message as an error response: the status comes from {@code @httpError} or {@code @error}, and
     * {@code errorSerializer} writes the error type header.
     */
    public HttpBindingSerializer error(HeaderErrorSerializer errorSerializer) {
        this.errorSerializer = errorSerializer;
        return this;
    }

    /**
     * Sets the buffer the codec writes the body members into. When unset, a new buffer is used.
     */
    public HttpBindingSerializer bodyBuffer(ByteBufferOutputStream bodyBuffer) {
        this.bodyBuffer = bodyBuffer;
        return this;
    }

    @Override
    protected RuntimeException throwForInvalidState(String message, Schema schema) {
        return new IllegalStateException("Expected a structure for HTTP bindings, but found " + schema);
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        HttpBindingSchemaExtensions.StructBindings bindings = HttpBindingSchemaExtensions.structBindingsOf(schema);
        outerStruct = struct;
        DirectionBinding direction;
        if (isResponse) {
            ResponseBinding resp = bindings.response();
            direction = resp;
            if (resp.defaultStatus != -1) {
                responseStatus = resp.defaultStatus;
            }
        } else {
            RequestBinding req = bindings.request();
            direction = req;
            pathSerializer = req.pathSerializer(operationBinding.uriPattern(), schema);
            addQueryLiterals();
        }
        activeBindings = direction.bindings;
        memberBindings = direction.memberBindings;
        headerPlan = direction.headers;
        payloadMember = direction.payloadMember;

        if (allowEmptyStructPayload || direction.writeBody(omitEmptyPayload)) {
            if (direction.hasBody) {
                serializeBody(schema, struct);
            } else {
                // No body members: the codec's fixed empty-structure bytes, cached on the plan.
                byte[] empty = direction.emptyBody(payloadCodec, schema);
                setBody(ContentStreamProvider.fromByteArrayUnsafe(empty), empty.length);
            }
            putHeader(CONTENT_TYPE, payloadMediaType);
        }

        if (errorSerializer != null) {
            prepareError(direction);
        }

        struct.serializeMembers(new BindingSerializer());
        flushPayload();
        flushQueryParams();
    }

    private void serializeBody(Schema schema, SerializableStruct struct) {
        ByteBufferOutputStream out = bodyBuffer != null ? bodyBuffer : new ByteBufferOutputStream();
        try (ShapeSerializer serializer = payloadCodec.createSerializer(out)) {
            serializer.writeStruct(schema, new StructBodyProxy(struct, activeBindings));
        }
        setBody(out.toContentStreamProvider(), out.size());
    }

    private void prepareError(DirectionBinding direction) {
        int status = ((ResponseBinding) direction).defaultStatus;
        responseStatus = status == -1 ? DEFAULT_ERROR_STATUS : status;
        errorSerializer.writeErrorType(outerStruct.sdkSchema(), headers);
    }

    private void addQueryLiterals() {
        String[] keys = operationBinding.queryLiteralKeys();
        String[] values = operationBinding.queryLiteralValues();
        for (int i = 0; i < keys.length; i++) {
            addQuery(keys[i], values[i]);
        }
    }

    private void addQuery(String name, String value) {
        if (rawQueryParameters == null) {
            rawQueryParameters = new LinkedHashMap<>();
            httpQueryKeys = new HashSet<>();
        }
        rawQueryParameters.computeIfAbsent(name, k -> new ArrayList<>(1)).add(value);
        httpQueryKeys.add(name);
    }

    private void addQueryParam(String name, String value) {
        if (deferredQueryParams == null) {
            deferredQueryParams = new ArrayList<>();
        }
        deferredQueryParams.add(name);
        deferredQueryParams.add(value);
    }

    // @httpQuery wins over @httpQueryParams regardless of member order (Requirement 15.6).
    private void flushQueryParams() {
        if (deferredQueryParams == null) {
            return;
        }
        if (rawQueryParameters == null) {
            rawQueryParameters = new LinkedHashMap<>();
        }
        for (int i = 0; i < deferredQueryParams.size(); i += 2) {
            String name = deferredQueryParams.get(i);
            if (httpQueryKeys == null || !httpQueryKeys.contains(name)) {
                rawQueryParameters.computeIfAbsent(name, k -> new ArrayList<>(1)).add(deferredQueryParams.get(i + 1));
            }
        }
    }

    private void putHeader(String name, String value) {
        headers.put(name, Collections.singletonList(value));
    }

    private void setBody(ContentStreamProvider provider, long length) {
        this.body = provider;
        this.bodyLength = length;
    }

    void writePayloadContentType() {
        putHeader(CONTENT_TYPE, payloadMediaType);
    }

    void setBlobPayload(Schema schema, ByteBuffer value) {
        // Held by reference, as the earlier marshallers hold SdkBytes and smithy-java DataStream.ofByteBuffer does.
        ByteBufferContentStreamProvider provider = ByteBufferContentStreamProvider.create(value);
        setBody(provider, provider.length());
        blobPayloadSchema = schema;
    }

    private PayloadSerializer payload() {
        if (payloadSerializer == null) {
            payloadSerializer = new PayloadSerializer(this, payloadCodec);
        }
        return payloadSerializer;
    }

    private void flushPayload() {
        if (payloadSerializer == null) {
            return;
        }
        if (!payloadSerializer.isPayloadWritten()) {
            ByteBufferOutputStream out = payloadSerializer.output();
            if (out == null) {
                return;
            }
            setBody(out.toContentStreamProvider(), out.size());
            blobPayloadSchema = payloadMember;
        }
        // A raw (blob, string or scalar) payload: Content-Type from @mediaType, else the type's default, unless a
        // header member or a codec payload already set it.
        if (blobPayloadSchema != null && !headers.containsKey(CONTENT_TYPE)) {
            MediaTypeTrait mediaType = blobPayloadSchema.getTrait(TraitKey.MEDIA_TYPE_TRAIT);
            String contentType;
            if (mediaType != null) {
                contentType = mediaType.getValue();
            } else {
                contentType = blobPayloadSchema.type() == ShapeType.BLOB ? DEFAULT_BLOB_CONTENT_TYPE
                                                                          : DEFAULT_STRING_CONTENT_TYPE;
            }
            putHeader(CONTENT_TYPE, contentType);
        }
    }

    /**
     * Returns the operation's HTTP method from {@code smithy.api#http}, for example {@code GET}.
     */
    public String httpMethod() {
        return operationBinding.httpTrait().getMethod();
    }

    /**
     * Returns the headers written, keyed case-insensitively in the case the model declares.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns the raw (not percent-encoded) query parameters, in the order written. A query literal without a value
     * maps to a null element. The SDK request percent-encodes them with the RFC 3986 unreserved set.
     */
    public Map<String, List<String>> rawQueryParameters() {
        return rawQueryParameters == null ? Collections.emptyMap() : rawQueryParameters;
    }

    /**
     * Returns the percent-encoded request path, starting with {@code /}.
     *
     * @throws software.amazon.awssdk.schema.serde.SerializationException if a label member is null or empty
     */
    public String path() {
        return pathSerializer.serialize(outerStruct, trimGreedyLabelLeadingSlash);
    }

    /**
     * Returns the body, or null when there is none.
     */
    public ContentStreamProvider body() {
        return body;
    }

    /**
     * Returns the length of the body in bytes, or -1 when there is none.
     */
    public long bodyLength() {
        return bodyLength;
    }

    /**
     * Returns the response status code.
     */
    public int responseStatus() {
        return responseStatus;
    }

    /**
     * Quotes a string element of a list header when it contains a comma or a double quote, as Smithy's
     * {@code httpHeader} list rules require, escaping {@code "} and {@code \} inside the quotes. An element with a
     * backslash is quoted too, so the reader's escape handling gives the element back unchanged.
     */
    static String quoteHeaderListElement(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length() + 4).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.append('"').toString();
    }

    private static String base64Utf8(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Dispatches each member write by its direction-specific binding. Body members were already written by the codec
     * and are skipped here, as are labels (read through {@link SerializableStruct#getMemberValue} by the path
     * serializer).
     */
    private final class BindingSerializer extends SpecificShapeSerializer {
        private Binding binding(Schema schema) {
            return activeBindings[schema.memberIndex()];
        }

        // Only header and query members are converted to text; body and label members are skipped before any work.
        private boolean isText(Binding binding) {
            return binding == Binding.HEADER || binding == Binding.QUERY;
        }

        private void writeText(Schema schema, Binding binding, String text) {
            MemberBinding member = memberBindings[schema.memberIndex()];
            if (binding == Binding.HEADER) {
                putHeader(member.wireName(), text);
            } else {
                addQuery(member.wireName(), text);
            }
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeBoolean(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, Boolean.toString(value));
            }
        }

        @Override
        public void writeShort(Schema schema, short value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeShort(schema, value);
            } else if (b == Binding.STATUS) {
                responseStatus = value;
            } else if (isText(b)) {
                writeText(schema, b, Short.toString(value));
            }
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeByte(schema, value);
            } else if (b == Binding.STATUS) {
                responseStatus = value;
            } else if (isText(b)) {
                writeText(schema, b, Byte.toString(value));
            }
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeInteger(schema, value);
            } else if (b == Binding.STATUS) {
                responseStatus = value;
            } else if (isText(b)) {
                writeText(schema, b, Integer.toString(value));
            }
        }

        @Override
        public void writeLong(Schema schema, long value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeLong(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, Long.toString(value));
            }
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeFloat(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, Float.toString(value));
            }
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeDouble(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, Double.toString(value));
            }
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeBigInteger(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, value.toString());
            }
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeBigDecimal(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, value.toString());
            }
        }

        @Override
        public void writeString(Schema schema, String value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeString(schema, value);
            } else if (b == Binding.HEADER) {
                MemberBinding header = memberBindings[schema.memberIndex()];
                putHeader(header.wireName(), header.hasMediaType() ? base64Utf8(value) : value);
            } else if (b == Binding.QUERY) {
                writeText(schema, b, value);
            }
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            Binding b = binding(schema);
            if (b == Binding.PAYLOAD) {
                payload().writeBlob(schema, value);
            } else if (isText(b)) {
                writeText(schema, b, ByteBufferUtils.base64Encode(value));
            }
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            int idx = schema.memberIndex();
            switch (activeBindings[idx]) {
                case HEADER:
                case QUERY:
                    writeText(schema, activeBindings[idx], memberBindings[idx].timestampFormatter().writeString(value));
                    break;
                case PAYLOAD:
                    payload().writeTimestamp(schema, value);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (binding(schema) == Binding.PAYLOAD) {
                payload().writeStruct(schema, struct);
            }
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            int idx = schema.memberIndex();
            switch (activeBindings[idx]) {
                case HEADER:
                    HeaderListElementSerializer elements = new HeaderListElementSerializer(memberBindings[idx], size);
                    consumer.accept(listState, elements);
                    elements.finish();
                    break;
                case QUERY:
                    consumer.accept(listState, new QueryListElementSerializer(memberBindings[idx]));
                    break;
                case PAYLOAD:
                    payload().writeList(schema, listState, size, consumer);
                    break;
                default:
                    break;
            }
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            int idx = schema.memberIndex();
            switch (activeBindings[idx]) {
                case PREFIX_HEADERS:
                    String prefix = memberBindings[idx].wireName();
                    consumer.accept(mapState, new StringMapEntrySerializer((key, value) -> {
                        String name = prefix + key;
                        // A statically named @httpHeader member takes precedence over a prefix-header entry.
                        if (!headerPlan.headerCanonicalNames.contains(name.toLowerCase(Locale.ROOT))) {
                            putHeader(name, value);
                        }
                    }));
                    break;
                case QUERY_PARAMS:
                    consumer.accept(mapState, new StringMapEntrySerializer(HttpBindingSerializer.this::addQueryParam));
                    break;
                case PAYLOAD:
                    payload().writeMap(schema, mapState, size, consumer);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            if (binding(schema) == Binding.PAYLOAD) {
                payload().writeDocument(schema, value);
            }
        }

        @Override
        public void writeNull(Schema schema) {
            // Nulls are dropped from the wire for every binding kind.
        }
    }

    /**
     * Collects the elements of a list-typed {@code @httpHeader} member into one header value joined with
     * {@code ", "} (Requirement 15.6). String elements are quoted when needed; timestamps use the element format
     * ({@code http-date} by default, written unquoted as Smithy specifies).
     *
     * <p>An empty list is sent as an empty header value ({@code RestJsonNullAndEmptyHeaders}). Null and blank string
     * elements are left out, as the earlier SDK marshallers do, and a list whose elements are all left out sends no
     * header.
     */
    private final class HeaderListElementSerializer extends SpecificShapeSerializer {
        private final MemberBinding binding;
        private final int size;
        private final StringBuilder joined = new StringBuilder();
        private boolean any;

        HeaderListElementSerializer(MemberBinding binding, int size) {
            this.binding = binding;
            this.size = size;
        }

        private void append(String value) {
            if (any) {
                joined.append(", ");
            }
            any = true;
            joined.append(value);
        }

        void finish() {
            if (any || size == 0) {
                putHeader(binding.wireName(), joined.toString());
            }
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            append(Boolean.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            append(Short.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            append(Byte.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            append(Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            append(Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            append(Float.toString(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            append(Double.toString(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            append(value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            append(value.toString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            if (StringUtils.isBlank(value)) {
                return;
            }
            append(quoteHeaderListElement(binding.hasMediaType() ? base64Utf8(value) : value));
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            append(ByteBufferUtils.base64Encode(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            append(binding.timestampFormatter().writeString(value));
        }

        @Override
        public void writeNull(Schema schema) {
            // Sparse null elements are dropped.
        }
    }

    /**
     * Writes each element of a list-typed {@code @httpQuery} member as its own parameter, in list order.
     */
    private final class QueryListElementSerializer extends SpecificShapeSerializer {
        private final MemberBinding binding;

        QueryListElementSerializer(MemberBinding binding) {
            this.binding = binding;
        }

        private void add(String value) {
            addQuery(binding.wireName(), value);
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            add(Boolean.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            add(Short.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            add(Byte.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            add(Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            add(Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            add(Float.toString(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            add(Double.toString(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            add(value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            add(value.toString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            add(value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            add(ByteBufferUtils.base64Encode(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            add(binding.timestampFormatter().writeString(value));
        }

        @Override
        public void writeNull(Schema schema) {
            // Sparse null elements are dropped.
        }
    }
}
