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

package software.amazon.awssdk.protocols.client.awsquery.internal;

import java.nio.ByteBuffer;
import java.util.Collections;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.client.http.HttpErrorDeserializer;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Reads the error code, message and request ID of a query protocol error body in one pass:
 * <ul>
 *     <li>awsQuery: {@code <ErrorResponse><Error><Code/><Message/></Error><RequestId/></ErrorResponse>};</li>
 *     <li>ec2Query: {@code <Response><Errors><Error><Code/><Message/></Error></Errors><RequestID/></Response>}.</li>
 * </ul>
 * As in earlier SDK clients, the request ID is the root's {@code RequestId} child, else its {@code RequestID} child,
 * and only the first {@code <Error>} of ec2Query's {@code <Errors>} is read. A body whose root element is not the
 * protocol's ({@code ErrorResponse} or {@code Response}) is rejected with an exception, after which
 * {@link HttpErrorDeserializer} uses the service's base exception without an error code, as earlier SDK clients do
 * when they find no error element.
 *
 * <p>The protocol's codec unwraps the root itself, so it cannot see the request ID next to the error element; this
 * parser reads the root's children with its own codec instead.
 */
@SdkInternalApi
@ThreadSafe
public final class QueryErrorPayloadParser implements HttpErrorDeserializer.ErrorPayloadParser {
    private static final String NAMESPACE = "software.amazon.awssdk.protocols.client.awsquery.internal";

    private static final Schema ERROR = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Error"))
                                              .putMember("Code", PreludeSchemas.STRING)
                                              .putMember("Message", PreludeSchemas.STRING)
                                              .build();

    private static final Schema ERRORS = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Errors"))
                                               .putMember("Error", ERROR)
                                               .build();

    private static final Schema ERROR_RESPONSE = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "ErrorResponse"))
                                                       .putMember("Error", ERROR)
                                                       .putMember("RequestId", PreludeSchemas.STRING)
                                                       .putMember("RequestID", PreludeSchemas.STRING)
                                                       .build();

    private static final Schema EC2_RESPONSE = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Response"))
                                                     .putMember("Errors", ERRORS)
                                                     .putMember("RequestId", PreludeSchemas.STRING)
                                                     .putMember("RequestID", PreludeSchemas.STRING)
                                                     .build();

    private static final ShapeDeserializer.StructMemberConsumer<ErrorFields> ERROR_MEMBERS =
        (fields, member, deserializer) -> {
            String value = deserializer.readString(member);
            if ("Code".equals(member.memberName())) {
                fields.code = value;
            } else {
                fields.message = value;
            }
        };

    private static final ShapeDeserializer.StructMemberConsumer<ErrorFields> IGNORED_ERROR_MEMBERS =
        (fields, member, deserializer) -> deserializer.readString(member);

    private static final ShapeDeserializer.StructMemberConsumer<ErrorFields> ERRORS_MEMBERS =
        (fields, member, deserializer) -> {
            fields.errorElements++;
            deserializer.readStruct(member, fields, fields.errorElements == 1 ? ERROR_MEMBERS : IGNORED_ERROR_MEMBERS);
        };

    private static final ShapeDeserializer.StructMemberConsumer<ErrorFields> ROOT_MEMBERS =
        (fields, member, deserializer) -> {
            switch (member.memberName()) {
                case "Error":
                    fields.errorElements++;
                    deserializer.readStruct(member, fields,
                                            fields.errorElements == 1 ? ERROR_MEMBERS : IGNORED_ERROR_MEMBERS);
                    break;
                case "Errors":
                    deserializer.readStruct(member, fields, ERRORS_MEMBERS);
                    break;
                case "RequestId":
                    fields.requestId = deserializer.readString(member);
                    break;
                default:
                    fields.requestIdUppercase = deserializer.readString(member);
                    break;
            }
        };

    /**
     * The awsQuery parser, for {@code <ErrorResponse><Error>}.
     */
    public static final QueryErrorPayloadParser AWS_QUERY =
        new QueryErrorPayloadParser("ErrorResponse", ERROR_RESPONSE);

    /**
     * The ec2Query parser, for {@code <Response><Errors><Error>}.
     */
    public static final QueryErrorPayloadParser EC2_QUERY = new QueryErrorPayloadParser("Response", EC2_RESPONSE);

    private final String rootElement;
    private final Schema rootSchema;
    private final XmlCodec rootCodec;

    private QueryErrorPayloadParser(String rootElement, Schema rootSchema) {
        this.rootElement = rootElement;
        this.rootSchema = rootSchema;
        this.rootCodec = XmlCodec.builder()
                                 .settings(XmlSettings.AWS_QUERY_RESPONSE)
                                 .wrapperElements(Collections.singletonList(rootElement))
                                 .build();
    }

    /**
     * Parses an error body. The protocol's codec is not used.
     *
     * @throws software.amazon.awssdk.schema.serde.SerializationException if the body is not XML or its root element is
     *     not the protocol's
     */
    @Override
    public HttpErrorDeserializer.ErrorPayload parse(Codec codec, SdkHttpFullResponse response, ByteBuffer body) {
        ErrorFields fields = new ErrorFields();
        try (ShapeDeserializer deserializer = rootCodec.createDeserializer(body)) {
            deserializer.readStruct(rootSchema, fields, ROOT_MEMBERS);
        }
        String requestId = fields.requestId != null ? fields.requestId : fields.requestIdUppercase;
        return HttpErrorDeserializer.ErrorPayload.create(fields.code, fields.message, requestId);
    }

    @Override
    public String toString() {
        return "QueryErrorPayloadParser(" + rootElement + ")";
    }

    /**
     * The values read from an error body.
     */
    private static final class ErrorFields {
        private String code;
        private String message;
        private String requestId;
        private String requestIdUppercase;
        private int errorElements;
    }
}
