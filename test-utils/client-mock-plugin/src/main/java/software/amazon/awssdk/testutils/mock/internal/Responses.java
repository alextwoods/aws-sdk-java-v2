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
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.HttpErrorTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Helpers shared by the response generators: error status, code and fault, the SDK exception's message and request ID,
 * service traits, and response building.
 */
@SdkInternalApi
final class Responses {
    static final String CONTENT_TYPE = "Content-Type";
    static final String CONTENT_LENGTH = "Content-Length";
    static final String REQUEST_ID_HEADER = "x-amzn-RequestId";
    static final String QUERY_ERROR_HEADER = "x-amzn-query-error";

    private Responses() {
    }

    /**
     * Validates the arguments of {@code generateOutput} and returns the value to serialize: the output itself, or, for a
     * schema-backed {@link Document} such as the dynamic client's {@code StructDocument}, a view of it that serializes as
     * the structure.
     *
     * <p>A schema-backed Document's {@code serialize} writes one {@code writeDocument} (the {@link Document} rule), which
     * codecs write as a document, with a type discriminator in JSON and nothing in XML, and which HTTP bindings cannot
     * split into bound members. Its {@code serializeMembers} writes the members with their member schemas, which is what
     * {@link SerializableStruct#serialize}'s {@code writeStruct} needs.
     */
    static SerializableStruct validateOutput(ApiOperation<?, ?> operation, SerializableStruct output) {
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(output, "output");
        return output instanceof Document ? new StructView(output) : output;
    }

    /**
     * Validates the arguments of {@code generateError} and returns the error trait of the error's schema.
     */
    static ErrorTrait validateError(ApiOperation<?, ?> operation, SerializableStruct modeledError) {
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(modeledError, "modeledError");
        Schema schema = modeledError.sdkSchema();
        ErrorTrait error = schema.getTrait(TraitKey.ERROR_TRAIT);
        if (error == null) {
            throw new IllegalArgumentException("Cannot generate an error response for " + schema.id()
                                               + ": its schema has no error trait. Use generateOutput for outputs.");
        }
        return error;
    }

    /**
     * Returns the {@code httpError} code of an error schema, or 400 for a client and 500 for a server error.
     */
    static int errorStatus(Schema errorSchema) {
        HttpErrorTrait httpError = errorSchema.getTrait(TraitKey.HTTP_ERROR_TRAIT);
        return httpError != null ? httpError.getCode() : errorSchema.expectTrait(TraitKey.ERROR_TRAIT)
                                                                    .getDefaultHttpStatusCode();
    }

    /**
     * Returns the {@code awsQueryError} code of an error schema, or its shape name.
     */
    static String errorCode(Schema errorSchema) {
        AwsQueryErrorTrait queryError = errorSchema.getTrait(TraitKey.AWS_QUERY_ERROR_TRAIT);
        return queryError != null ? queryError.getCode() : errorSchema.id().getName();
    }

    /**
     * Returns the awsQuery fault of an error: {@code Sender} for a client and {@code Receiver} for a server error.
     */
    static String fault(ErrorTrait error) {
        return error.isClientError() ? "Sender" : "Receiver";
    }

    /**
     * Returns the message of an SDK exception, without the service diagnostics, or null.
     */
    static String message(SerializableStruct modeledError) {
        return modeledError instanceof SdkException ? ((SdkException) modeledError).rawMessage() : null;
    }

    /**
     * Returns the request ID of an SDK service exception, or null.
     */
    static String requestId(SerializableStruct modeledError) {
        return modeledError instanceof SdkServiceException ? ((SdkServiceException) modeledError).requestId() : null;
    }

    /**
     * Returns the service schema of an operation, or null.
     */
    static Schema serviceSchema(ApiOperation<?, ?> operation) {
        ApiService service = operation.service();
        return service != null ? service.schema() : null;
    }

    /**
     * Returns the {@code xmlNamespace} trait of an operation's service, or null.
     */
    static XmlNamespaceTrait xmlNamespace(ApiOperation<?, ?> operation) {
        Schema service = serviceSchema(operation);
        return service != null ? service.getTrait(TraitKey.XML_NAMESPACE_TRAIT) : null;
    }

    /**
     * Adds the request ID header, and, for a service with the {@code awsQueryCompatible} trait, the
     * {@code x-amzn-query-error} header ({@code Code;Sender} or {@code Code;Receiver}).
     */
    static SdkHttpFullResponse.Builder addErrorHeaders(SdkHttpFullResponse.Builder response, ApiOperation<?, ?> operation,
                                                       SerializableStruct modeledError, ErrorTrait error) {
        Schema service = serviceSchema(operation);
        if (service != null && service.hasTrait(TraitKey.AWS_QUERY_COMPATIBLE_TRAIT)) {
            response.putHeader(QUERY_ERROR_HEADER, errorCode(modeledError.sdkSchema()) + ";" + fault(error));
        }
        return addRequestIdHeader(response, modeledError);
    }

    /**
     * Adds the request ID header of an SDK service exception, if it has one.
     */
    static SdkHttpFullResponse.Builder addRequestIdHeader(SdkHttpFullResponse.Builder response,
                                                          SerializableStruct modeledError) {
        String requestId = requestId(modeledError);
        if (requestId != null) {
            response.putHeader(REQUEST_ID_HEADER, requestId);
        }
        return response;
    }

    /**
     * Creates a response with a body.
     */
    static SdkHttpFullResponse.Builder response(int status, String contentType, ByteBuffer body) {
        return response(status, contentType, BinaryUtils.copyBytesFrom(body));
    }

    /**
     * Creates a response with a body.
     */
    static SdkHttpFullResponse.Builder response(int status, String contentType, byte[] body) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(status)
                                  .putHeader(CONTENT_TYPE, contentType)
                                  .putHeader(CONTENT_LENGTH, Integer.toString(body.length))
                                  .content(AbortableInputStream.create(new ByteArrayInputStream(body)));
    }

    /**
     * A structure whose {@code serialize} is {@link SerializableStruct}'s {@code writeStruct}, over the members of a
     * schema-backed Document.
     */
    private static final class StructView implements SerializableStruct {
        private final SerializableStruct document;

        private StructView(SerializableStruct document) {
            this.document = document;
        }

        @Override
        public Schema sdkSchema() {
            return document.sdkSchema();
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            document.serializeMembers(serializer);
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return document.getMemberValue(member);
        }
    }

    /**
     * Escapes text for an XML attribute value or element content.
     */
    static String escapeXml(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&apos;");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }
}
