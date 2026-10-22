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
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;

/**
 * Generates {@code smithy.protocols#rpcv2Cbor} responses: a CBOR body with the {@code smithy-protocol: rpc-v2-cbor}
 * header, and errors with {@code __type} and {@code message} in the body.
 *
 * <p>Port of the response half of smithy-java's {@code AbstractRpcV2ServerProtocol.serializeOutput}, which writes
 * the status, the {@code smithy-protocol} header and the codec body. The SDK adds {@code __type} and {@code message},
 * which smithy-java's modeled exceptions serialize as members.
 */
@SdkInternalApi
@ThreadSafe
public final class RpcV2CborResponseGenerator implements ResponseGenerator {
    private static final String SMITHY_PROTOCOL_HEADER = "smithy-protocol";
    private static final String SMITHY_PROTOCOL_VALUE = "rpc-v2-cbor";
    private static final String MEDIA_TYPE = "application/cbor";
    private static final Rpcv2CborCodec CODEC = Rpcv2CborCodec.builder().settings(CborSettings.RPCV2).build();
    private static final AugmentedStruct.Augmenter ERROR_BODY = new AugmentedStruct.Augmenter(null, "__type", "message");

    /**
     * Creates the generator; called by {@link java.util.ServiceLoader}.
     */
    public RpcV2CborResponseGenerator() {
    }

    @Override
    public ShapeId protocolId() {
        return Rpcv2CborTrait.ID;
    }

    @Override
    public SdkHttpFullResponse generateOutput(ApiOperation<?, ?> operation, SerializableStruct output) {
        SerializableStruct body = Responses.validateOutput(operation, output);
        return Responses.response(200, MEDIA_TYPE, CODEC.serialize(body))
                        .putHeader(SMITHY_PROTOCOL_HEADER, SMITHY_PROTOCOL_VALUE)
                        .build();
    }

    @Override
    public SdkHttpFullResponse generateError(ApiOperation<?, ?> operation, SerializableStruct modeledError) {
        ErrorTrait error = Responses.validateError(operation, modeledError);
        AugmentedStruct body = ERROR_BODY.wrap(modeledError, modeledError.sdkSchema().id().toString(),
                                               Responses.message(modeledError));
        SdkHttpFullResponse.Builder response =
            Responses.response(Responses.errorStatus(modeledError.sdkSchema()), MEDIA_TYPE, CODEC.serialize(body))
                     .putHeader(SMITHY_PROTOCOL_HEADER, SMITHY_PROTOCOL_VALUE);
        return Responses.addErrorHeaders(response, operation, modeledError, error).build();
    }

    @Override
    public String toString() {
        return "RpcV2CborResponseGenerator";
    }
}
