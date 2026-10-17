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

package software.amazon.awssdk.protocols.client.restxml;

import java.nio.ByteBuffer;
import java.util.Collections;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.client.http.HttpBindingClientProtocol;
import software.amazon.awssdk.protocols.client.http.HttpBindingErrorFactory;
import software.amazon.awssdk.protocols.client.http.HttpErrorDeserializer;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.traits.RestXmlTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.utils.Validate;

/**
 * The client protocol for {@code aws.protocols#restXml}: the
 * <a href="https://smithy.io/2.0/aws/protocols/aws-restxml-protocol.html">AWS restXml protocol</a>. Members are placed
 * by their HTTP binding traits, and the body is serialized by {@link XmlCodec} with {@link XmlSettings#REST_XML}
 * ({@code xmlName}, {@code xmlNamespace}, {@code xmlAttribute}, {@code xmlFlattened}, {@code timestampFormat},
 * {@code date-time} by default) as {@code application/xml}. A body without an {@code xmlNamespace} trait on its root
 * shape declares the service's {@code xmlNamespace}, if it has one.
 *
 * <p>Like the earlier SDK restXml marshallers:
 * <ul>
 *     <li>a body starts with the XML declaration {@code <?xml version="1.0" encoding="UTF-8"?>};</li>
 *     <li>an input without body members and without a payload member is sent without a body, and an input whose
 *     structure {@code @httpPayload} member is null is also sent without a body;</li>
 *     <li>a greedy label value is sent with its leading {@code /} ({@code /a/{key+}} with {@code key = "/b"} is
 *     {@code /a//b});</li>
 *     <li>the response {@code Content-Type} is not checked, the root element name of a response is not checked against
 *     the output shape, and the body of an output without body or payload members is not read.</li>
 * </ul>
 * Error responses are resolved from the {@code Code} element of {@code <ErrorResponse><Error>} or, for services with
 * {@code noErrorWrapping}, of a bare {@code <Error>}; both forms are accepted either way. The message comes from the
 * {@code Message} element of the error, and the request ID from a {@code RequestId} or {@code RequestID} element (next to
 * {@code <Error>} in {@code <ErrorResponse>}, else inside {@code <Error>}), then the {@code x-amzn-RequestId} and
 * {@code x-amz-request-id} headers. A modeled error's members are read through their HTTP binding traits
 * ({@link HttpBindingErrorFactory}).
 *
 * <p>Streaming payloads and event streams are not supported yet.
 *
 * <p>Instances are created once per client and are safe to use from multiple threads. Create one with
 * {@link #RestXmlClientProtocol(ProtocolSettings)}, or through {@link Factory}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol}.
 */
@SdkPublicApi
@ThreadSafe
public final class RestXmlClientProtocol extends HttpBindingClientProtocol {
    private static final String MEDIA_TYPE = "application/xml";

    private final ShapeId service;
    private final XmlCodec codec;
    private final HttpErrorDeserializer errorDeserializer;

    /**
     * Creates the protocol for a service.
     *
     * @param settings the settings of the service. The service namespace resolves relative error codes, the base
     *     exception builder creates the exception for errors that do not resolve to a modeled error, and the
     *     {@code xmlNamespace} trait of the service schema, if any, is the default namespace of request bodies
     * @throws NullPointerException if {@code settings} is null
     */
    public RestXmlClientProtocol(ProtocolSettings settings) {
        super(RestXmlTrait.ID);
        Validate.paramNotNull(settings, "settings");
        this.service = settings.service();
        Schema serviceSchema = settings.serviceSchema();
        XmlNamespaceTrait namespace = serviceSchema != null ? serviceSchema.getTrait(TraitKey.XML_NAMESPACE_TRAIT) : null;
        // Responses: tolerate a root element name that differs from the output shape (for example S3's
        // <CopyObjectResult> for CopyObjectOutput).
        this.codec = XmlCodec.builder()
                             .settings(XmlSettings.REST_XML)
                             .defaultNamespace(namespace)
                             .strictRootElement(false)
                             .xmlDeclaration(true)
                             .build();
        this.errorDeserializer = HttpErrorDeserializer.builder()
                                                      .codec(codec)
                                                      .protocolSettings(settings)
                                                      .errorPayloadParser(new XmlErrorPayloadParser())
                                                      .knownErrorFactory(new HttpBindingErrorFactory(httpBinding()))
                                                      .build();
    }

    @Override
    public Codec payloadCodec() {
        return codec;
    }

    @Override
    protected String payloadMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    protected HttpErrorDeserializer errorDeserializer() {
        return errorDeserializer;
    }

    /**
     * Returns true: an input without body or payload members is sent without a body.
     */
    @Override
    protected boolean omitEmptyPayload() {
        return true;
    }

    @Override
    public String toString() {
        return "RestXmlClientProtocol(" + service + ")";
    }

    /**
     * Reads the error code, message and request ID of a restXml error body: {@code <ErrorResponse><Error>...</Error>
     * <RequestId>...</RequestId></ErrorResponse>}, or a bare {@code <Error>} with the request ID inside it.
     */
    private static final class XmlErrorPayloadParser implements HttpErrorDeserializer.ErrorPayloadParser {
        private static final String ERROR_RESPONSE = "ErrorResponse";
        private static final String NAMESPACE = "software.amazon.awssdk.protocols.client.restxml";

        private static final Schema ERROR = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Error"))
                                                  .putMember("Code", PreludeSchemas.STRING)
                                                  .putMember("Message", PreludeSchemas.STRING)
                                                  .putMember("RequestId", PreludeSchemas.STRING)
                                                  .putMember("RequestID", PreludeSchemas.STRING)
                                                  .build();

        private static final Schema WRAPPED_ERROR =
            Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, ERROR_RESPONSE))
                  .putMember("Error", ERROR)
                  .putMember("RequestId", PreludeSchemas.STRING)
                  .putMember("RequestID", PreludeSchemas.STRING)
                  .build();

        private static final ShapeDeserializer.StructMemberConsumer<ErrorFields> ERROR_MEMBERS =
            (fields, member, deserializer) -> {
                String value = deserializer.readString(member);
                switch (member.memberName()) {
                    case "Code":
                        fields.code = value;
                        break;
                    case "Message":
                        fields.message = value;
                        break;
                    default:
                        fields.nestedRequestId(value);
                        break;
                }
            };

        private static final ShapeDeserializer.StructMemberConsumer<ErrorFields> WRAPPED_MEMBERS =
            (fields, member, deserializer) -> {
                if ("Error".equals(member.memberName())) {
                    deserializer.readStruct(member, fields, ERROR_MEMBERS);
                } else {
                    fields.topLevelRequestId(deserializer.readString(member));
                }
            };

        /**
         * Reads the children of the {@code <ErrorResponse>} root: the {@code <Error>} element and the request ID next
         * to it. The response codec unwraps {@code <ErrorResponse>} itself, so it cannot see that request ID.
         */
        private final XmlCodec wrappedCodec = XmlCodec.builder()
                                                      .settings(XmlSettings.REST_XML)
                                                      .wrapperElements(Collections.singletonList(ERROR_RESPONSE))
                                                      .build();

        @Override
        public HttpErrorDeserializer.ErrorPayload parse(Codec codec, SdkHttpFullResponse response, ByteBuffer body) {
            ErrorFields fields = new ErrorFields();
            boolean wrapped = hasRootElement(body, ERROR_RESPONSE);
            try (ShapeDeserializer deserializer = (wrapped ? wrappedCodec : codec).createDeserializer(body)) {
                deserializer.readStruct(wrapped ? WRAPPED_ERROR : ERROR, fields,
                                        wrapped ? WRAPPED_MEMBERS : ERROR_MEMBERS);
            }
            return HttpErrorDeserializer.ErrorPayload.create(fields.code, fields.message, fields.requestId());
        }

        /**
         * Returns whether the document's root element is {@code name}, without a namespace prefix. Only a UTF-8 byte
         * order mark, whitespace and processing instructions (such as the XML declaration) may come before it; a body
         * with anything else first (a comment, a doctype) is reported as not matching, and is then read without the
         * request ID next to {@code <Error>}.
         */
        private static boolean hasRootElement(ByteBuffer body, String name) {
            int limit = body.limit();
            int i = body.position();
            if (limit - i >= 3 && (body.get(i) & 0xFF) == 0xEF && (body.get(i + 1) & 0xFF) == 0xBB
                && (body.get(i + 2) & 0xFF) == 0xBF) {
                i += 3;
            }
            while (true) {
                i = skipWhitespace(body, i, limit);
                if (i + 1 >= limit || body.get(i) != '<' || body.get(i + 1) != '?') {
                    break;
                }
                i = endOfProcessingInstruction(body, i + 2, limit);
                if (i < 0) {
                    return false;
                }
            }
            if (i >= limit || body.get(i) != '<') {
                return false;
            }
            i++;
            for (int c = 0; c < name.length(); c++, i++) {
                if (i >= limit || body.get(i) != name.charAt(c)) {
                    return false;
                }
            }
            if (i >= limit) {
                return false;
            }
            byte next = body.get(i);
            return next == '>' || next == '/' || isWhitespace(next);
        }

        private static int skipWhitespace(ByteBuffer body, int from, int limit) {
            int i = from;
            while (i < limit && isWhitespace(body.get(i))) {
                i++;
            }
            return i;
        }

        /**
         * Returns the index after the {@code ?>} that ends a processing instruction whose content starts at
         * {@code from}, or -1 if there is none.
         */
        private static int endOfProcessingInstruction(ByteBuffer body, int from, int limit) {
            for (int i = from; i + 1 < limit; i++) {
                if (body.get(i) == '?' && body.get(i + 1) == '>') {
                    return i + 2;
                }
            }
            return -1;
        }

        private static boolean isWhitespace(byte b) {
            return b == ' ' || b == '\t' || b == '\r' || b == '\n';
        }

        @Override
        public String toString() {
            return "XmlErrorPayloadParser";
        }
    }

    /**
     * The values read from an error body.
     */
    private static final class ErrorFields {
        private String code;
        private String message;
        private String topLevelRequestId;
        private String nestedRequestId;

        void topLevelRequestId(String value) {
            if (topLevelRequestId == null) {
                topLevelRequestId = value;
            }
        }

        void nestedRequestId(String value) {
            if (nestedRequestId == null) {
                nestedRequestId = value;
            }
        }

        String requestId() {
            return topLevelRequestId != null ? topLevelRequestId : nestedRequestId;
        }
    }

    /**
     * Creates {@link RestXmlClientProtocol}s. Generated clients use {@link #INSTANCE} directly; the factory is also
     * registered for {@link software.amazon.awssdk.core.client.protocol.ClientProtocolFactories#forId(ShapeId)}.
     */
    @SdkPublicApi
    @ThreadSafe
    public static final class Factory implements ClientProtocolFactory<RestXmlTrait> {
        /**
         * The shared factory instance.
         */
        public static final Factory INSTANCE = new Factory();

        /**
         * Creates a factory. Used by {@link java.util.ServiceLoader}; prefer {@link #INSTANCE}.
         */
        public Factory() {
        }

        /**
         * Returns {@link RestXmlTrait#ID}.
         *
         * @return {@code aws.protocols#restXml}
         */
        @Override
        public ShapeId id() {
            return RestXmlTrait.ID;
        }

        /**
         * Creates the protocol. The trait has no settings that change the protocol's behavior: error responses are read
         * with and without the {@code ErrorResponse} wrapping element whatever its {@code noErrorWrapping} value, and its
         * {@code http} and {@code eventStreamHttp} lists are for the HTTP client.
         *
         * @param settings the settings of the service
         * @param protocolTrait the protocol trait; may be null
         * @return a new {@link RestXmlClientProtocol}
         * @throws NullPointerException if {@code settings} is null
         */
        @Override
        public RestXmlClientProtocol createProtocol(ProtocolSettings settings, RestXmlTrait protocolTrait) {
            return new RestXmlClientProtocol(settings);
        }
    }
}
