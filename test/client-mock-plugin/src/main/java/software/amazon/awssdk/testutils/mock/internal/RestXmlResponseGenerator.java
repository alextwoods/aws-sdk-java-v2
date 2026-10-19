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

package software.amazon.awssdk.testutils.mock.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.HeaderErrorSerializer;
import software.amazon.awssdk.httpbinding.HttpBinding;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.RestXmlTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Generates {@code aws.protocols#restXml} responses with the HTTP binding {@code ResponseSerializer}: bound status,
 * headers and payload, and an XML body in the service's {@code xmlNamespace}.
 *
 * <p>Errors are {@code <ErrorResponse><Error><Type/><Code/><Message/>members</Error><RequestId/></ErrorResponse>}, or
 * the bare {@code <Error>} element when the service's {@code restXml} trait sets {@code noErrorWrapping}. The
 * {@code <Error>} element and its HTTP bindings come from the {@code ResponseSerializer}; the {@code <ErrorResponse>}
 * wrapper is written around its body.
 */
@SdkInternalApi
@ThreadSafe
public final class RestXmlResponseGenerator implements ResponseGenerator {
    private static final String MEDIA_TYPE = "application/xml";
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String NO_NAMESPACE = "";
    private static final AugmentedStruct.Augmenter ERROR_BODY =
        new AugmentedStruct.Augmenter("Error", "Type", "Code", "Message");
    private static final XmlCodec WRAPPED_ERROR_CODEC = XmlCodec.builder().settings(XmlSettings.REST_XML).build();

    private final ConcurrentMap<String, XmlCodec> codecs = new ConcurrentHashMap<>();

    /**
     * Creates the generator; called by {@link java.util.ServiceLoader}.
     */
    public RestXmlResponseGenerator() {
    }

    @Override
    public ShapeId protocolId() {
        return RestXmlTrait.ID;
    }

    @Override
    public SdkHttpFullResponse generateOutput(ApiOperation<?, ?> operation, SerializableStruct output) {
        Responses.validateOutput(operation, output);
        return HttpBinding.create()
                          .responseSerializer()
                          .operation(operation)
                          .payloadCodec(codec(Responses.xmlNamespace(operation)))
                          .payloadMediaType(MEDIA_TYPE)
                          .omitEmptyPayload(true)
                          .shapeValue(output)
                          .serializeResponse();
    }

    @Override
    public SdkHttpFullResponse generateError(ApiOperation<?, ?> operation, SerializableStruct modeledError) {
        ErrorTrait error = Responses.validateError(operation, modeledError);
        Schema schema = modeledError.sdkSchema();
        XmlNamespaceTrait namespace = Responses.xmlNamespace(operation);
        boolean wrapped = !noErrorWrapping(operation);
        SerializableStruct body = ERROR_BODY.wrap(modeledError, Responses.fault(error), Responses.errorCode(schema),
                                                  Responses.message(modeledError));
        SdkHttpFullResponse response = HttpBinding.create()
                                                  .responseSerializer()
                                                  .operation(operation)
                                                  .payloadCodec(wrapped ? WRAPPED_ERROR_CODEC : codec(namespace))
                                                  .payloadMediaType(MEDIA_TYPE)
                                                  .error(true)
                                                  .headerErrorSerializer(HeaderErrorSerializer.NONE)
                                                  .shapeValue(body)
                                                  .serializeResponse();
        SdkHttpFullResponse.Builder builder = response.toBuilder();
        if (wrapped) {
            wrap(builder, response.content(), namespace, Responses.requestId(modeledError));
        }
        return Responses.addErrorHeaders(builder, operation, modeledError, error).build();
    }

    private static boolean noErrorWrapping(ApiOperation<?, ?> operation) {
        Schema service = Responses.serviceSchema(operation);
        RestXmlTrait restXml = service != null ? service.getTrait(TraitKey.REST_XML_TRAIT) : null;
        return restXml != null && restXml.isNoErrorWrapping();
    }

    private static void wrap(SdkHttpFullResponse.Builder builder, Optional<AbortableInputStream> content,
                             XmlNamespaceTrait namespace, String requestId) {
        StringBuilder xml = new StringBuilder(XML_DECLARATION).append("<ErrorResponse");
        if (namespace != null) {
            xml.append(" xmlns=\"").append(Responses.escapeXml(namespace.getUri())).append('"');
        }
        xml.append('>');
        if (content.isPresent()) {
            xml.append(readUtf8(content.get()));
        }
        if (requestId != null) {
            xml.append("<RequestId>").append(Responses.escapeXml(requestId)).append("</RequestId>");
        }
        byte[] bytes = xml.append("</ErrorResponse>").toString().getBytes(StandardCharsets.UTF_8);
        builder.putHeader(Responses.CONTENT_TYPE, MEDIA_TYPE)
               .putHeader(Responses.CONTENT_LENGTH, Integer.toString(bytes.length))
               .content(AbortableInputStream.create(new ByteArrayInputStream(bytes)));
    }

    private static String readUtf8(InputStream content) {
        try (InputStream in = content) {
            return new String(IoUtils.toByteArray(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the serialized error body", e);
        }
    }

    private XmlCodec codec(XmlNamespaceTrait namespace) {
        String key = namespace == null ? NO_NAMESPACE : namespace.getUri() + "|" + namespace.getPrefix().orElse("");
        return codecs.computeIfAbsent(key, k -> {
            XmlCodec.Builder builder = XmlCodec.builder().settings(XmlSettings.REST_XML).xmlDeclaration(true);
            if (namespace != null) {
                builder.defaultNamespace(namespace);
            }
            return builder.build();
        });
    }

    @Override
    public String toString() {
        return "RestXmlResponseGenerator";
    }
}
