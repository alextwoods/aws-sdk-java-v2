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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.Binding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.DirectionBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.HeaderPlan;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.RequestBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.ResponseBinding;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Generic HTTP binding deserializer. It reads the bound members from the headers, status code, path labels, query
 * string and payload, and reads the unbound members from the body through the protocol's codec.
 *
 * <p>The top-level shape read must be a structure. Members whose header, label or query parameter is absent are not
 * passed to the consumer, so they stay unset in the builder.
 *
 * <p>Port of smithy-java {@code HttpBindingDeserializer}, reading SDK header maps. The body is read on first use, so
 * a streaming payload member (task 7.19) leaves it unread.
 */
@SdkInternalApi
public final class HttpBindingDeserializer extends SpecificShapeDeserializer {
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    private final Codec payloadCodec;
    private final SdkHttpHeaders headers;
    private final Map<String, List<String>> queryParameters;
    private final int responseStatus;
    private final Map<String, String> requestPathLabels;
    private final boolean isResponse;
    private final String payloadMediaType;
    private final InputStream bodyStream;
    private ByteBuffer body;

    private HttpBindingDeserializer(Builder builder) {
        this.payloadCodec = Validate.paramNotNull(builder.payloadCodec, "payloadCodec");
        this.headers = Validate.paramNotNull(builder.headers, "headers");
        this.isResponse = builder.isResponse;
        this.queryParameters = builder.queryParameters == null ? Collections.emptyMap() : builder.queryParameters;
        this.responseStatus = builder.responseStatus;
        this.requestPathLabels = builder.requestPathLabels == null ? Collections.emptyMap() : builder.requestPathLabels;
        this.payloadMediaType = builder.payloadMediaType;
        this.body = builder.body;
        this.bodyStream = builder.bodyStream;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected RuntimeException throwForInvalidState(Schema schema) {
        return new IllegalStateException("Expected to parse a structure for HTTP bindings, but found " + schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        // Throws IllegalStateException for a schema that is not a structure or union.
        HttpBindingSchemaExtensions.StructBindings sb = HttpBindingSchemaExtensions.structBindingsOf(schema);
        DirectionBinding direction;
        if (isResponse) {
            ResponseBinding rb = sb.response();
            direction = rb;
            readHeaderBindings(state, consumer, rb.headers);
            if (rb.statusMember != null) {
                consumer.accept(state, rb.statusMember, new ResponseStatusDeserializer(responseStatus));
            }
        } else {
            RequestBinding rb = sb.request();
            direction = rb;
            readHeaderBindings(state, consumer, rb.headers);
            readRequestBindings(state, consumer, rb);
        }
        if (direction.payloadMember != null) {
            readPayload(direction.payloadMember, state, consumer);
        }
        if (direction.hasBody) {
            readBody(schema, state, consumer, direction.bindings);
        }
    }

    private <T> void readRequestBindings(T state, StructMemberConsumer<T> consumer, RequestBinding rb) {
        for (Schema member : rb.labelMembers) {
            String labelValue = requestPathLabels.get(member.memberName());
            if (labelValue == null) {
                throw new SerializationException("Expected a value for HTTP label " + member.id());
            }
            consumer.accept(state, member, new HttpPathLabelDeserializer(labelValue));
        }
        for (int i = 0; i < rb.queryMembers.length; i++) {
            List<String> paramValues = queryParameters.get(rb.queryWireNames[i]);
            if (paramValues != null && !paramValues.isEmpty()) {
                Schema member = rb.queryMembers[i];
                consumer.accept(state, member, new HttpQueryStringDeserializer(paramValues, member));
            }
        }
        for (Schema member : rb.queryParamsMembers) {
            consumer.accept(state, member, new HttpQueryParamsDeserializer(queryParameters));
        }
    }

    private <T> void readHeaderBindings(T state, StructMemberConsumer<T> consumer, HeaderPlan plan) {
        for (int i = 0; i < plan.listHeaderMembers.length; i++) {
            List<String> values = headers.matchingHeaders(plan.listHeaderNames[i]);
            if (!values.isEmpty()) {
                Schema member = plan.listHeaderMembers[i];
                consumer.accept(state, member,
                                new HttpHeaderListDeserializer(member, HttpBindingSchemaExtensions.memberBindingOf(member),
                                                               values));
            }
        }
        for (int i = 0; i < plan.scalarHeaderMembers.length; i++) {
            Optional<String> value = headers.firstMatchingHeader(plan.scalarHeaderNames[i]);
            if (value.isPresent()) {
                Schema member = plan.scalarHeaderMembers[i];
                consumer.accept(state, member, new HttpHeaderDeserializer(value.get(), member));
            }
        }
        for (Schema member : plan.prefixHeadersMembers) {
            consumer.accept(state, member,
                            new HttpPrefixHeadersDeserializer(headers, HttpBindingSchemaExtensions.memberBindingOf(member)));
        }
    }

    private <T> void readPayload(Schema member, T state, StructMemberConsumer<T> consumer) {
        if (member.hasTrait(TraitKey.STREAMING_TRAIT)) {
            // Streaming blobs (task 7.19) and event streams (task 7.20) are handed to the caller by the protocol; the
            // body is left unread.
            return;
        }
        ByteBuffer bb = bodyAsByteBuffer();
        if (bb.remaining() == 0) {
            // An empty body leaves the payload member unset, for every target type.
            return;
        }
        ShapeType type = member.type();
        if (type == ShapeType.STRUCTURE || type == ShapeType.UNION || type == ShapeType.DOCUMENT
            || type == ShapeType.LIST || type == ShapeType.MAP) {
            validateMediaType();
        }
        consumer.accept(state, member, new PayloadDeserializer(payloadCodec, bb));
    }

    private <T> void readBody(Schema schema, T state, StructMemberConsumer<T> consumer, Binding[] bindings) {
        validateMediaType();
        try (ShapeDeserializer deserializer = payloadCodec.createDeserializer(bodyAsByteBuffer())) {
            deserializer.readStruct(schema, state, new BodyMemberConsumer<>(consumer, bindings));
        }
    }

    private ByteBuffer bodyAsByteBuffer() {
        if (body == null) {
            if (bodyStream == null) {
                body = EMPTY;
            } else {
                try {
                    body = ByteBuffer.wrap(IoUtils.toByteArray(bodyStream));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return body.duplicate();
    }

    private void validateMediaType() {
        String contentType = headers.firstMatchingHeader("Content-Type").orElse(null);
        if (compareMediaType(contentType, payloadMediaType) == -1) {
            throw new SerializationException("Unexpected Content-Type '" + contentType + "', expected "
                                             + payloadMediaType);
        }
    }

    /**
     * Compares an actual media type against an expected media type, case-insensitively. The actual media type matches
     * if it starts with the expected one and is either an exact match or followed by a semicolon or whitespace.
     *
     * @param actual the media type received (for example from a Content-Type header), or null
     * @param expected the expected media type, or null to skip validation
     * @return 1 if the media types match or {@code expected} is null, 0 if {@code actual} is null but
     *     {@code expected} is not (accepted), -1 if the media types do not match
     */
    static int compareMediaType(String actual, String expected) {
        if (expected == null) {
            return 1;
        } else if (actual == null) {
            return 0;
        }
        // Media types are ASCII (RFC 6838 section 4.2), so case folding with regionMatches is safe.
        int len = expected.length();
        if (actual.regionMatches(true, 0, expected, 0, len)) {
            if (actual.length() == len) {
                return 1;
            }
            char next = actual.charAt(len);
            if (next == ';' || Character.isWhitespace(next)) {
                return 1;
            }
        }
        return -1;
    }

    /**
     * Passes only the body members, the members whose direction-specific binding is {@code BODY}, to the consumer, so
     * a body key that matches a member bound elsewhere is read and discarded. Unknown members are forwarded.
     */
    private static final class BodyMemberConsumer<T> implements StructMemberConsumer<T> {
        private final StructMemberConsumer<T> delegate;
        private final Binding[] bindings;

        BodyMemberConsumer(StructMemberConsumer<T> delegate, Binding[] bindings) {
            this.delegate = delegate;
            this.bindings = bindings;
        }

        @Override
        public void accept(T state, Schema memberSchema, ShapeDeserializer memberDeserializer) {
            if (bindings[memberSchema.memberIndex()] == Binding.BODY) {
                delegate.accept(state, memberSchema, memberDeserializer);
            } else {
                ValueSkipper.skip(memberSchema, memberDeserializer);
            }
        }

        @Override
        public void unknownMember(T state, String memberName) {
            delegate.unknownMember(state, memberName);
        }
    }

    /**
     * Builds an {@link HttpBindingDeserializer}.
     */
    public static final class Builder {
        private Map<String, String> requestPathLabels;
        private Codec payloadCodec;
        private SdkHttpHeaders headers;
        private Map<String, List<String>> queryParameters;
        private ByteBuffer body;
        private InputStream bodyStream;
        private int responseStatus;
        private String payloadMediaType;
        private boolean isResponse;

        private Builder() {
        }

        public HttpBindingDeserializer build() {
            return new HttpBindingDeserializer(this);
        }

        /**
         * Sets the percent-decoded label values of the matched request path, by member name.
         */
        public Builder requestPathLabels(Map<String, String> requestPathLabels) {
            this.requestPathLabels = requestPathLabels;
            return this;
        }

        /**
         * Sets the codec for the body and structured payloads.
         */
        public Builder payloadCodec(Codec payloadCodec) {
            this.payloadCodec = payloadCodec;
            return this;
        }

        /**
         * Sets the message headers.
         */
        public Builder headers(SdkHttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the raw (decoded) query parameters of a request.
         */
        public Builder queryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        /**
         * Sets the whole body. Takes precedence over {@link #bodyStream}.
         */
        public Builder body(ByteBuffer body) {
            this.body = body;
            return this;
        }

        /**
         * Sets a body stream, read fully on first use and not closed.
         */
        public Builder bodyStream(InputStream bodyStream) {
            this.bodyStream = bodyStream;
            return this;
        }

        /**
         * Sets the HTTP status code of a response.
         */
        public Builder responseStatus(int responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }

        /**
         * Sets the expected media type of the body; null skips the check.
         */
        public Builder payloadMediaType(String payloadMediaType) {
            this.payloadMediaType = payloadMediaType;
            return this;
        }

        /**
         * Selects the response direction (true) or the request direction (false).
         */
        public Builder isResponse(boolean isResponse) {
            this.isResponse = isResponse;
            return this;
        }
    }
}
