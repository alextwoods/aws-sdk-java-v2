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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.HeaderErrorSerializer;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;

/**
 * Generates {@code awsJson1_0} and {@code awsJson1_1} responses: a JSON body with status 200, and errors with
 * {@code __type} and {@code message} in the body and the {@code X-Amzn-Errortype} header.
 */
@SdkInternalApi
@ThreadSafe
abstract class AwsJsonResponseGenerator implements ResponseGenerator {
    private static final JsonCodec CODEC = JsonCodec.builder().settings(JsonSettings.AWS_JSON).build();
    private static final AugmentedStruct.Augmenter ERROR_BODY = new AugmentedStruct.Augmenter(null, "__type", "message");

    private final ShapeId protocolId;
    private final String contentType;

    AwsJsonResponseGenerator(ShapeId protocolId, String contentType) {
        this.protocolId = protocolId;
        this.contentType = contentType;
    }

    @Override
    public final ShapeId protocolId() {
        return protocolId;
    }

    @Override
    public final SdkHttpFullResponse generateOutput(ApiOperation<?, ?> operation, SerializableStruct output) {
        SerializableStruct body = Responses.validateOutput(operation, output);
        return Responses.response(200, contentType, CODEC.serialize(body)).build();
    }

    @Override
    public final SdkHttpFullResponse generateError(ApiOperation<?, ?> operation, SerializableStruct modeledError) {
        ErrorTrait error = Responses.validateError(operation, modeledError);
        AugmentedStruct body = ERROR_BODY.wrap(modeledError, modeledError.sdkSchema().id().toString(),
                                               Responses.message(modeledError));
        SdkHttpFullResponse.Builder response =
            Responses.response(Responses.errorStatus(modeledError.sdkSchema()), contentType, CODEC.serialize(body));
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        HeaderErrorSerializer.AMZN_ERROR_HEADER.writeErrorType(modeledError.sdkSchema(), headers);
        headers.forEach(response::putHeader);
        return Responses.addErrorHeaders(response, operation, modeledError, error).build();
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }
}
