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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;

/**
 * Generates {@code awsQuery} and {@code ec2Query} XML responses in the service's {@code xmlNamespace}.
 *
 * <ul>
 *     <li>awsQuery output: {@code <OpResponse><OpResult>members</OpResult></OpResponse>}.</li>
 *     <li>ec2Query output: {@code <OpResponse>members</OpResponse>}.</li>
 *     <li>awsQuery error: {@code <ErrorResponse><Error><Type/><Code/><Message/>members</Error><RequestId/>}.</li>
 *     <li>ec2Query error: {@code <Response><Errors><Error><Code/><Message/>members</Error></Errors><RequestID/>}.</li>
 * </ul>
 *
 * <p>The code is the error's {@code awsQueryError} code, or its shape name. Envelopes are schemas built once per
 * operation or error, serialized with the XML codec.
 */
@SdkInternalApi
@ThreadSafe
abstract class QueryResponseGenerator implements ResponseGenerator {
    private static final String MEDIA_TYPE = "text/xml";
    private static final String NAMESPACE = "software.amazon.awssdk.testutils.mock.internal";
    private static final String NO_NAMESPACE = "";

    private final ShapeId protocolId;
    private final boolean ec2;
    private final AugmentedStruct.Augmenter errorBody;
    private final Map<Schema, Schema> envelopes = Collections.synchronizedMap(new IdentityHashMap<>());
    private final ConcurrentMap<String, AugmentedStruct.Augmenter> ec2Outputs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, XmlCodec> codecs = new ConcurrentHashMap<>();

    QueryResponseGenerator(ShapeId protocolId, boolean ec2) {
        this.protocolId = protocolId;
        this.ec2 = ec2;
        this.errorBody = ec2 ? new AugmentedStruct.Augmenter("Error", "Code", "Message")
                             : new AugmentedStruct.Augmenter("Error", "Type", "Code", "Message");
    }

    @Override
    public final ShapeId protocolId() {
        return protocolId;
    }

    @Override
    public final SdkHttpFullResponse generateOutput(ApiOperation<?, ?> operation, SerializableStruct output) {
        SerializableStruct value = Responses.validateOutput(operation, output);
        String responseName = operation.name() + "Response";
        SerializableStruct body;
        if (ec2) {
            body = ec2Outputs.computeIfAbsent(responseName, n -> new AugmentedStruct.Augmenter(n)).wrap(value);
        } else {
            // Built per call: an output schema may be shared by operations of different names.
            Schema envelope = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, responseName))
                                    .putMember(operation.name() + "Result", value.sdkSchema())
                                    .build();
            body = new EnvelopeStruct(envelope, value);
        }
        return Responses.response(200, MEDIA_TYPE, codec(operation).serialize(body)).build();
    }

    @Override
    public final SdkHttpFullResponse generateError(ApiOperation<?, ?> operation, SerializableStruct modeledError) {
        ErrorTrait error = Responses.validateError(operation, modeledError);
        Schema schema = modeledError.sdkSchema();
        String code = Responses.errorCode(schema);
        String message = Responses.message(modeledError);
        AugmentedStruct errorElement = ec2 ? errorBody.wrap(modeledError, code, message)
                                           : errorBody.wrap(modeledError, Responses.fault(error), code, message);
        Schema envelope = envelopes.computeIfAbsent(schema, s -> errorEnvelope(errorElement.sdkSchema()));
        String requestId = Responses.requestId(modeledError);
        SerializableStruct body;
        if (ec2) {
            Schema errors = envelope.member("Errors").memberTarget();
            body = new EnvelopeStruct(envelope, new EnvelopeStruct(errors, errorElement), requestId);
        } else {
            body = new EnvelopeStruct(envelope, errorElement, requestId);
        }
        SdkHttpFullResponse.Builder response =
            Responses.response(Responses.errorStatus(schema), MEDIA_TYPE, codec(operation).serialize(body));
        // The query protocols carry the code in the body, so x-amzn-query-error is not added.
        return Responses.addRequestIdHeader(response, modeledError).build();
    }

    private Schema errorEnvelope(Schema errorElement) {
        if (ec2) {
            Schema errors = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Errors"))
                                  .putMember("Error", errorElement)
                                  .build();
            return Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Response"))
                         .putMember("Errors", errors)
                         .putMember("RequestID", PreludeSchemas.STRING)
                         .build();
        }
        return Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "ErrorResponse"))
                     .putMember("Error", errorElement)
                     .putMember("RequestId", PreludeSchemas.STRING)
                     .build();
    }

    private XmlCodec codec(ApiOperation<?, ?> operation) {
        XmlNamespaceTrait namespace = Responses.xmlNamespace(operation);
        String key = namespace == null ? NO_NAMESPACE : namespace.getUri() + "|" + namespace.getPrefix().orElse("");
        return codecs.computeIfAbsent(key, k -> {
            XmlCodec.Builder builder = XmlCodec.builder().settings(XmlSettings.AWS_QUERY_RESPONSE);
            if (namespace != null) {
                builder.defaultNamespace(namespace);
            }
            return builder.build();
        });
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }
}
