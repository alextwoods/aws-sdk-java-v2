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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.HeaderErrorSerializer;
import software.amazon.awssdk.httpbinding.HttpBinding;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;

/**
 * Generates {@code aws.protocols#restJson1} responses with the HTTP binding {@code ResponseSerializer}: bound status,
 * headers and payload, and a JSON body. Errors carry {@code X-Amzn-Errortype} ({@code HeaderErrorSerializer}) and a
 * {@code message} body member.
 *
 * <p>Port of smithy-java's {@code AwsRestJson1Protocol.serializeOutput}.
 */
@SdkInternalApi
@ThreadSafe
public final class RestJsonResponseGenerator implements ResponseGenerator {
    private static final String MEDIA_TYPE = "application/json";
    private static final JsonCodec CODEC = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();
    private static final AugmentedStruct.Augmenter ERROR_BODY = new AugmentedStruct.Augmenter(null, "message");

    /**
     * Creates the generator; called by {@link java.util.ServiceLoader}.
     */
    public RestJsonResponseGenerator() {
    }

    @Override
    public ShapeId protocolId() {
        return RestJson1Trait.ID;
    }

    @Override
    public SdkHttpFullResponse generateOutput(ApiOperation<?, ?> operation, SerializableStruct output) {
        SerializableStruct body = Responses.validateOutput(operation, output);
        return HttpBinding.create()
                          .responseSerializer()
                          .operation(operation)
                          .payloadCodec(CODEC)
                          .payloadMediaType(MEDIA_TYPE)
                          .omitEmptyPayload(true)
                          .shapeValue(body)
                          .serializeResponse();
    }

    @Override
    public SdkHttpFullResponse generateError(ApiOperation<?, ?> operation, SerializableStruct modeledError) {
        ErrorTrait error = Responses.validateError(operation, modeledError);
        SdkHttpFullResponse response = HttpBinding.create()
                                                  .responseSerializer()
                                                  .operation(operation)
                                                  .payloadCodec(CODEC)
                                                  .payloadMediaType(MEDIA_TYPE)
                                                  .omitEmptyPayload(true)
                                                  .error(true)
                                                  .headerErrorSerializer(HeaderErrorSerializer.AMZN_ERROR_HEADER)
                                                  .shapeValue(ERROR_BODY.wrap(modeledError,
                                                                              Responses.message(modeledError)))
                                                  .serializeResponse();
        return Responses.addErrorHeaders(response.toBuilder(), operation, modeledError, error).build();
    }

    @Override
    public String toString() {
        return "RestJsonResponseGenerator";
    }
}
