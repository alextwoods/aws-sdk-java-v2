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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpPrefixHeadersTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Pre-computes HTTP binding data on Schema objects so the per-request path avoids trait lookups, type checks and
 * dispatch on a generic switch. One value is computed per Schema instance and cached by {@link Schema#getExtension}
 * (Requirement 15.4).
 *
 * <ul>
 *     <li>A member schema gets a {@code MemberBinding}: its binding kind, wire name and location timestamp format.</li>
 *     <li>A structure or union gets a {@code StructBindings} whose request and response plans are built lazily on
 *     first use in each direction.</li>
 *     <li>An operation with {@code @http} gets an {@code OperationBinding}: the method, the parsed URI pattern and
 *     its query literals.</li>
 * </ul>
 *
 * <p>Port of smithy-java {@code HttpBindingSchemaExtensions}. Registered in
 * {@code META-INF/services/software.amazon.awssdk.schema.SchemaExtensionProvider}.
 */
@SdkInternalApi
public final class HttpBindingSchemaExtensions
    implements SchemaExtensionProvider<HttpBindingSchemaExtensions.HttpBindingExt> {
    // CHECKSTYLE:ON

    /**
     * Extension key for HTTP binding pre-computed data.
     */
    public static final SchemaExtensionKey<HttpBindingExt> KEY = new SchemaExtensionKey<>();

    private static final Schema[] NO_SCHEMAS = new Schema[0];
    private static final String[] NO_STRINGS = new String[0];

    /**
     * Returns whether the input structure has an {@code @httpPayload} member that targets a structure.
     *
     * @param inputSchema the operation input schema
     * @return true if the request body is a single codec-serialized structure
     */
    public static boolean hasStructPayload(Schema inputSchema) {
        return structBindingsOf(inputSchema).request().hasStructPayload;
    }

    /**
     * Returns whether the structure has an {@code @httpPayload} member with {@code @streaming} in the given direction.
     *
     * @param schema the input, output or error schema
     * @param response true for the response direction
     * @return true if the payload member is streaming
     */
    public static boolean hasStreamingPayload(Schema schema, boolean response) {
        HttpBindingExt ext = schema.getExtension(KEY);
        if (!(ext instanceof StructBindings)) {
            return false;
        }
        StructBindings sb = (StructBindings) ext;
        Schema payload = response ? sb.response().payloadMember : sb.request().payloadMember;
        return payload != null && payload.hasTrait(TraitKey.STREAMING_TRAIT);
    }

    /**
     * Look up the {@link MemberBinding} for a member schema. Throws if no binding or not a member.
     */
    static MemberBinding memberBindingOf(Schema schema) {
        HttpBindingExt ext = schema.getExtension(KEY);
        if (!(ext instanceof MemberBinding)) {
            throw new IllegalStateException("Schema " + schema.id() + " has no HTTP member binding");
        }
        return (MemberBinding) ext;
    }

    /**
     * Look up the {@link OperationBinding} for an operation schema. Throws if no binding or not an operation.
     */
    static OperationBinding operationBindingOf(Schema schema) {
        HttpBindingExt ext = schema.getExtension(KEY);
        if (!(ext instanceof OperationBinding)) {
            throw new IllegalStateException("Schema " + schema.id() + " has no HTTP operation binding; an operation "
                                            + "schema with the smithy.api#http trait is required");
        }
        return (OperationBinding) ext;
    }

    /**
     * Look up the {@link StructBindings} for a structure schema. Throws if no binding or not a structure.
     */
    static StructBindings structBindingsOf(Schema schema) {
        HttpBindingExt ext = schema.getExtension(KEY);
        if (!(ext instanceof StructBindings)) {
            throw new IllegalStateException("Schema " + schema.id() + " has no HTTP structure binding");
        }
        return (StructBindings) ext;
    }

    @Override
    public SchemaExtensionKey<HttpBindingExt> key() {
        return KEY;
    }

    @Override
    public HttpBindingExt provide(Schema schema) {
        if (schema.isMember()) {
            return forMember(schema);
        }
        ShapeType type = schema.type();
        if (type == ShapeType.STRUCTURE || type == ShapeType.UNION) {
            return new StructBindings(schema);
        }
        if (type == ShapeType.OPERATION && schema.hasTrait(TraitKey.HTTP_TRAIT)) {
            return forOperation(schema);
        }
        return null;
    }

    private static MemberBinding forMember(Schema m) {
        HttpHeaderTrait hdr = m.getTrait(TraitKey.HTTP_HEADER_TRAIT);
        if (hdr != null) {
            boolean isList = isListMember(m);
            return new MemberBinding(Binding.HEADER, hdr.getValue(), isList, m.hasTrait(TraitKey.MEDIA_TYPE_TRAIT),
                                     timestampFormatter(isList ? m.listMember() : m, TimestampFormatTrait.Format.HTTP_DATE));
        }

        HttpQueryTrait q = m.getTrait(TraitKey.HTTP_QUERY_TRAIT);
        if (q != null) {
            boolean isList = isListMember(m);
            return new MemberBinding(Binding.QUERY, q.getValue(), isList, false,
                                     timestampFormatter(isList ? m.listMember() : m, TimestampFormatTrait.Format.DATE_TIME));
        }

        // An empty prefix is permitted by the trait: every header goes into the map.
        HttpPrefixHeadersTrait pfx = m.getTrait(TraitKey.HTTP_PREFIX_HEADERS_TRAIT);
        if (pfx != null) {
            return new MemberBinding(Binding.PREFIX_HEADERS, pfx.getValue(), false, false, null);
        }

        if (m.hasTrait(TraitKey.HTTP_LABEL_TRAIT)) {
            return new MemberBinding(Binding.LABEL, m.memberName(), false, false,
                                     timestampFormatter(m, TimestampFormatTrait.Format.DATE_TIME));
        }

        if (m.hasTrait(TraitKey.HTTP_QUERY_PARAMS_TRAIT)) {
            return new MemberBinding(Binding.QUERY_PARAMS, null, false, false, null);
        }

        if (m.hasTrait(TraitKey.HTTP_PAYLOAD_TRAIT)) {
            return new MemberBinding(Binding.PAYLOAD, null, false, m.hasTrait(TraitKey.MEDIA_TYPE_TRAIT), null);
        }

        if (m.hasTrait(TraitKey.HTTP_RESPONSE_CODE_TRAIT)) {
            return new MemberBinding(Binding.STATUS, null, false, false, null);
        }

        return new MemberBinding(Binding.BODY, null, false, false, null);
    }

    private static boolean isListMember(Schema m) {
        return m.type() == ShapeType.LIST;
    }

    private static TimestampFormatter timestampFormatter(Schema schema, TimestampFormatTrait.Format defaultFormat) {
        return TimestampFormatter.of(schema, defaultFormat);
    }

    /**
     * Builds the request-direction plan for one structure or union schema.
     */
    static RequestBinding buildRequestBinding(Schema struct) {
        List<Schema> headers = new ArrayList<>();
        List<Schema> queries = new ArrayList<>();
        List<Schema> labels = new ArrayList<>();
        List<Schema> queryParams = new ArrayList<>();
        List<Schema> prefixHeaders = new ArrayList<>();
        Schema payload = null;
        boolean hasBody = false;

        int memberCount = struct.members().size();
        Binding[] bindings = new Binding[memberCount];
        MemberBinding[] memberBindings = new MemberBinding[memberCount];

        for (Schema m : struct.members()) {
            MemberBinding ext = memberBindingOf(m);
            int idx = m.memberIndex();
            memberBindings[idx] = ext;
            switch (ext.kind()) {
                case HEADER:
                    headers.add(m);
                    bindings[idx] = Binding.HEADER;
                    break;
                case PREFIX_HEADERS:
                    prefixHeaders.add(m);
                    bindings[idx] = Binding.PREFIX_HEADERS;
                    break;
                case PAYLOAD:
                    payload = m;
                    bindings[idx] = Binding.PAYLOAD;
                    break;
                case QUERY:
                    queries.add(m);
                    bindings[idx] = Binding.QUERY;
                    break;
                case QUERY_PARAMS:
                    queryParams.add(m);
                    bindings[idx] = Binding.QUERY_PARAMS;
                    break;
                case LABEL:
                    labels.add(m);
                    bindings[idx] = Binding.LABEL;
                    break;
                case STATUS:
                case BODY:
                default:
                    // STATUS has no meaning request-side: fold into BODY.
                    hasBody = true;
                    bindings[idx] = Binding.BODY;
                    break;
            }
        }

        Schema[] queryArr = toArray(queries);
        boolean hasStructPayload = payload != null && payload.type() == ShapeType.STRUCTURE;
        return new RequestBinding(bindings, memberBindings, new HeaderPlan(headers, prefixHeaders), queryArr,
                                  wireNames(queryArr), toArray(labels), toArray(queryParams), payload, hasBody,
                                  hasStructPayload);
    }

    /**
     * Builds the response-direction plan for one structure or union schema.
     */
    static ResponseBinding buildResponseBinding(Schema struct) {
        List<Schema> headers = new ArrayList<>();
        List<Schema> prefixHeaders = new ArrayList<>();
        Schema payload = null;
        Schema statusMember = null;
        boolean hasBody = false;

        int memberCount = struct.members().size();
        Binding[] bindings = new Binding[memberCount];
        MemberBinding[] memberBindings = new MemberBinding[memberCount];

        for (Schema m : struct.members()) {
            MemberBinding ext = memberBindingOf(m);
            int idx = m.memberIndex();
            memberBindings[idx] = ext;
            switch (ext.kind()) {
                case HEADER:
                    headers.add(m);
                    bindings[idx] = Binding.HEADER;
                    break;
                case PREFIX_HEADERS:
                    prefixHeaders.add(m);
                    bindings[idx] = Binding.PREFIX_HEADERS;
                    break;
                case PAYLOAD:
                    payload = m;
                    bindings[idx] = Binding.PAYLOAD;
                    break;
                case STATUS:
                    statusMember = m;
                    bindings[idx] = Binding.STATUS;
                    break;
                case QUERY:
                case QUERY_PARAMS:
                case LABEL:
                case BODY:
                default:
                    // Request-only kinds are folded into BODY in the response direction.
                    hasBody = true;
                    bindings[idx] = Binding.BODY;
                    break;
            }
        }

        // Default response status from @httpError or @error, or -1 when neither is present.
        int defaultStatus;
        if (struct.hasTrait(TraitKey.HTTP_ERROR_TRAIT)) {
            defaultStatus = struct.expectTrait(TraitKey.HTTP_ERROR_TRAIT).getCode();
        } else if (struct.hasTrait(TraitKey.ERROR_TRAIT)) {
            defaultStatus = struct.expectTrait(TraitKey.ERROR_TRAIT).getDefaultHttpStatusCode();
        } else {
            defaultStatus = -1;
        }

        return new ResponseBinding(bindings, memberBindings, new HeaderPlan(headers, prefixHeaders), payload,
                                   statusMember, hasBody, defaultStatus);
    }

    private static Schema[] toArray(List<Schema> list) {
        return list.isEmpty() ? NO_SCHEMAS : list.toArray(new Schema[0]);
    }

    private static String[] wireNames(Schema[] members) {
        if (members.length == 0) {
            return NO_STRINGS;
        }
        String[] names = new String[members.length];
        for (int i = 0; i < members.length; i++) {
            names[i] = memberBindingOf(members[i]).wireName();
        }
        return names;
    }

    private static OperationBinding forOperation(Schema schema) {
        HttpTrait httpTrait = schema.expectTrait(TraitKey.HTTP_TRAIT);
        UriPattern uriPattern = UriPattern.parse(httpTrait.getUri());
        Map<String, String> literals = uriPattern.queryLiterals();
        String[] keys = literals.isEmpty() ? NO_STRINGS : new String[literals.size()];
        String[] values = literals.isEmpty() ? NO_STRINGS : new String[literals.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : literals.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        return new OperationBinding(httpTrait, uriPattern, keys, values);
    }

    /**
     * Binding kind for a single member, derived from the traits applied to it.
     *
     * <p>The serializer and deserializer use direction-specific arrays which collapse {@link #STATUS} to {@link #BODY}
     * for the request direction and {@link #LABEL}, {@link #QUERY} and {@link #QUERY_PARAMS} to {@link #BODY} for the
     * response direction.
     */
    enum Binding {
        HEADER,
        QUERY,
        PAYLOAD,
        BODY,
        LABEL,
        STATUS,
        PREFIX_HEADERS,
        QUERY_PARAMS
    }

    /**
     * Marker for HTTP binding extension data attached to a schema.
     */
    public interface HttpBindingExt {
    }

    /**
     * Pre-computed binding data for a single member schema.
     */
    static final class MemberBinding implements HttpBindingExt {
        private final Binding kind;
        private final String wireName;
        private final String canonicalName;
        private final boolean isList;
        private final boolean hasMediaType;
        private final TimestampFormatter timestampFormatter;

        /**
         * @param kind the member's binding kind
         * @param wireName the header name for {@code @httpHeader} as written in the model, the query parameter name for
         *     {@code @httpQuery}, the prefix for {@code @httpPrefixHeaders}, the member name for {@code @httpLabel},
         *     or null
         * @param isList whether the member targets a list
         * @param hasMediaType whether the member carries {@code @mediaType}
         * @param timestampFormatter the timestamp format for this location with any {@code @timestampFormat} trait
         *     resolved: {@code http-date} for headers, {@code date-time} for query parameters and labels, null
         *     elsewhere. For a list member, the format of the list's elements
         */
        MemberBinding(Binding kind, String wireName, boolean isList, boolean hasMediaType,
                      TimestampFormatter timestampFormatter) {
            this.kind = kind;
            this.wireName = wireName;
            this.canonicalName = wireName == null ? null : wireName.toLowerCase(Locale.ROOT);
            this.isList = isList;
            this.hasMediaType = hasMediaType;
            this.timestampFormatter = timestampFormatter;
        }

        Binding kind() {
            return kind;
        }

        String wireName() {
            return wireName;
        }

        /**
         * The wire name in lower case, for case-insensitive header comparisons.
         */
        String canonicalName() {
            return canonicalName;
        }

        boolean isList() {
            return isList;
        }

        boolean hasMediaType() {
            return hasMediaType;
        }

        TimestampFormatter timestampFormatter() {
            return timestampFormatter;
        }
    }

    /**
     * HTTP binding extension data for a structure or union schema, with request and response plans built lazily.
     *
     * <p>The volatile lazy fields use a benign race: two threads may build a plan, and either result is equivalent.
     */
    static final class StructBindings implements HttpBindingExt {
        private final Schema schema;
        private volatile RequestBinding request;
        private volatile ResponseBinding response;

        StructBindings(Schema schema) {
            this.schema = schema;
        }

        RequestBinding request() {
            RequestBinding r = request;
            if (r == null) {
                r = buildRequestBinding(schema);
                request = r;
            }
            return r;
        }

        ResponseBinding response() {
            ResponseBinding r = response;
            if (r == null) {
                r = buildResponseBinding(schema);
                response = r;
            }
            return r;
        }
    }

    /**
     * The header members of one direction, split by target shape.
     */
    static final class HeaderPlan {
        final Schema[] scalarHeaderMembers;
        final String[] scalarHeaderNames;
        final Schema[] listHeaderMembers;
        final String[] listHeaderNames;
        final Schema[] prefixHeadersMembers;
        // Lower-case names of every @httpHeader member. A prefix-header entry with one of these names is skipped.
        final Set<String> headerCanonicalNames;
        // Whether Content-Type is bound by an @httpHeader member.
        final boolean contentTypeHeader;

        HeaderPlan(List<Schema> headers, List<Schema> prefixHeaders) {
            List<Schema> scalar = new ArrayList<>();
            List<Schema> list = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (Schema m : headers) {
                MemberBinding binding = memberBindingOf(m);
                names.add(binding.canonicalName());
                (binding.isList() ? list : scalar).add(m);
            }
            this.scalarHeaderMembers = toArray(scalar);
            this.scalarHeaderNames = wireNames(scalarHeaderMembers);
            this.listHeaderMembers = toArray(list);
            this.listHeaderNames = wireNames(listHeaderMembers);
            this.prefixHeadersMembers = toArray(prefixHeaders);
            this.headerCanonicalNames = names.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(names);
            this.contentTypeHeader = names.contains("content-type");
        }
    }

    /**
     * The codec output for a structure with no body members, cached for the last codec that produced it.
     */
    private static final class EmptyBody {
        private final Codec codec;
        private final byte[] bytes;

        EmptyBody(Codec codec, byte[] bytes) {
            this.codec = codec;
            this.bytes = bytes;
        }
    }

    /**
     * State shared by the request and response plans.
     */
    abstract static class DirectionBinding {
        // Per-member direction-specific binding kind, indexed by member index.
        final Binding[] bindings;
        // Direction-neutral MemberBinding metadata, indexed by member index.
        final MemberBinding[] memberBindings;
        final HeaderPlan headers;
        final Schema payloadMember;
        final boolean hasBody;
        private volatile EmptyBody emptyBody;

        DirectionBinding(Binding[] bindings, MemberBinding[] memberBindings, HeaderPlan headers, Schema payloadMember,
                         boolean hasBody) {
            this.bindings = bindings;
            this.memberBindings = memberBindings;
            this.headers = headers;
            this.payloadMember = payloadMember;
            this.hasBody = hasBody;
        }

        boolean hasPayload() {
            return payloadMember != null;
        }

        boolean writeBody(boolean omitEmptyPayload) {
            return hasBody || (!omitEmptyPayload && payloadMember == null);
        }

        /**
         * Returns the codec output for this structure without members. The bytes depend on the codec and the schema, so
         * they are cached for the codec instance that last produced them; callers must not modify the result.
         */
        byte[] emptyBody(Codec codec, Schema schema) {
            EmptyBody cached = emptyBody;
            if (cached != null && cached.codec == codec) {
                return cached.bytes;
            }
            ByteBufferOutputStream out = new ByteBufferOutputStream(16);
            try (ShapeSerializer serializer = codec.createSerializer(out)) {
                serializer.writeStruct(schema, new EmptyStruct(schema));
            }
            byte[] bytes = new byte[out.size()];
            System.arraycopy(out.array(), 0, bytes, 0, bytes.length);
            emptyBody = new EmptyBody(codec, bytes);
            return bytes;
        }
    }

    /**
     * Pre-computed request-direction plan for one structure or union schema.
     */
    static final class RequestBinding extends DirectionBinding {
        final Schema[] queryMembers;
        final String[] queryWireNames;
        final Schema[] labelMembers;
        final Schema[] queryParamsMembers;
        // True iff the @httpPayload member targets a STRUCTURE.
        final boolean hasStructPayload;
        private volatile PathSerializer pathSerializer;

        RequestBinding(Binding[] bindings, MemberBinding[] memberBindings, HeaderPlan headers, Schema[] queryMembers,
                       String[] queryWireNames, Schema[] labelMembers, Schema[] queryParamsMembers, Schema payloadMember,
                       boolean hasBody, boolean hasStructPayload) {
            super(bindings, memberBindings, headers, payloadMember, hasBody);
            this.queryMembers = queryMembers;
            this.queryWireNames = queryWireNames;
            this.labelMembers = labelMembers;
            this.queryParamsMembers = queryParamsMembers;
            this.hasStructPayload = hasStructPayload;
        }

        PathSerializer pathSerializer(UriPattern uriPattern, Schema inputSchema) {
            PathSerializer p = pathSerializer;
            if (p == null || p.uriPattern() != uriPattern) {
                p = new PathSerializer(uriPattern, inputSchema);
                pathSerializer = p;
            }
            return p;
        }
    }

    /**
     * Pre-computed response-direction plan for one structure or union schema.
     */
    static final class ResponseBinding extends DirectionBinding {
        final Schema statusMember;
        // Default response status from @httpError or @error, or -1 when neither is present.
        final int defaultStatus;

        ResponseBinding(Binding[] bindings, MemberBinding[] memberBindings, HeaderPlan headers, Schema payloadMember,
                        Schema statusMember, boolean hasBody, int defaultStatus) {
            super(bindings, memberBindings, headers, payloadMember, hasBody);
            this.statusMember = statusMember;
            this.defaultStatus = defaultStatus;
        }
    }

    /**
     * Pre-computed HTTP binding data for an operation schema.
     */
    static final class OperationBinding implements HttpBindingExt {
        private final HttpTrait httpTrait;
        private final UriPattern uriPattern;
        private final String[] queryLiteralKeys;
        private final String[] queryLiteralValues;
        // Null when the method is empty or not an SdkHttpMethod; sdkHttpMethod() then resolves it again.
        private final SdkHttpMethod sdkHttpMethod;

        OperationBinding(HttpTrait httpTrait, UriPattern uriPattern, String[] queryLiteralKeys,
                         String[] queryLiteralValues) {
            this.httpTrait = httpTrait;
            this.uriPattern = uriPattern;
            this.queryLiteralKeys = queryLiteralKeys;
            this.queryLiteralValues = queryLiteralValues;
            this.sdkHttpMethod = resolveMethod(httpTrait.getMethod());
        }

        private static SdkHttpMethod resolveMethod(String method) {
            try {
                return SdkHttpMethod.fromValue(method);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        HttpTrait httpTrait() {
            return httpTrait;
        }

        /**
         * The {@code @http} method as an {@link SdkHttpMethod}, resolved once. A method that
         * {@link SdkHttpMethod#fromValue} rejects is resolved again on each call, so it throws (or, when empty, returns
         * null) where it did before.
         */
        SdkHttpMethod sdkHttpMethod() {
            return sdkHttpMethod != null ? sdkHttpMethod : SdkHttpMethod.fromValue(httpTrait.getMethod());
        }

        UriPattern uriPattern() {
            return uriPattern;
        }

        /**
         * Raw (unencoded) keys of the {@code @http} URI query literals, for example {@code x-id}.
         */
        String[] queryLiteralKeys() {
            return queryLiteralKeys;
        }

        /**
         * Raw values parallel to {@link #queryLiteralKeys()}; null for a literal without {@code =}.
         */
        String[] queryLiteralValues() {
            return queryLiteralValues;
        }

        int defaultResponseStatus() {
            return httpTrait.getCode();
        }
    }

    /**
     * A structure value without members, used to produce the codec's empty-body bytes.
     */
    private static final class EmptyStruct implements SerializableStruct {
        private final Schema schema;

        EmptyStruct(Schema schema) {
            this.schema = schema;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            // no members
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }
}
