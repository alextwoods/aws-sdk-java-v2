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

package software.amazon.awssdk.benchmark.schema;

import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson11Protocol;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.awsquery.Ec2QueryClientProtocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.services.schemabenchec2.model.SchemaBenchEc2Exception;
import software.amazon.awssdk.services.schemabenchec2.model.SchemaBenchEc2Operations;
import software.amazon.awssdk.services.schemabenchjson10.model.SchemaBenchJson10Exception;
import software.amazon.awssdk.services.schemabenchjson10.model.SchemaBenchJson10Operations;
import software.amazon.awssdk.services.schemabenchjson11.model.SchemaBenchJson11Exception;
import software.amazon.awssdk.services.schemabenchjson11.model.SchemaBenchJson11Operations;
import software.amazon.awssdk.services.schemabenchquery.model.SchemaBenchQueryException;
import software.amazon.awssdk.services.schemabenchquery.model.SchemaBenchQueryOperations;
import software.amazon.awssdk.services.schemabenchrestjson.model.SchemaBenchRestJsonException;
import software.amazon.awssdk.services.schemabenchrestjson.model.SchemaBenchRestJsonOperations;
import software.amazon.awssdk.services.schemabenchrestxml.model.SchemaBenchRestXmlException;
import software.amazon.awssdk.services.schemabenchrestxml.model.SchemaBenchRestXmlOperations;
import software.amazon.awssdk.services.schemabenchrpcv2cbor.model.SchemaBenchRpcV2CborException;
import software.amazon.awssdk.services.schemabenchrpcv2cbor.model.SchemaBenchRpcV2CborOperations;

/**
 * The Supported_Protocols measured by {@link SchemaMarshallBenchmark} and {@link SchemaUnmarshallBenchmark}. Each one
 * is a generated benchmark service ({@code src/main/resources/schema-codegen-resources}) with the same shapes, so the
 * payloads differ only by protocol. {@code PutPayload} carries the payload in its input and {@code GetPayload} in its
 * output.
 */
public enum SchemaSerdeProtocol {
    RPCV2_CBOR(SchemaBenchRpcV2CborOperations.SERVICE, SchemaBenchRpcV2CborException::builder,
               SchemaBenchRpcV2CborOperations.PUT_PAYLOAD, SchemaBenchRpcV2CborOperations.GET_PAYLOAD,
               RpcV2CborProtocol::new),
    AWS_JSON_1_0(SchemaBenchJson10Operations.SERVICE, SchemaBenchJson10Exception::builder,
                 SchemaBenchJson10Operations.PUT_PAYLOAD, SchemaBenchJson10Operations.GET_PAYLOAD,
                 AwsJson1Protocol::new),
    AWS_JSON_1_1(SchemaBenchJson11Operations.SERVICE, SchemaBenchJson11Exception::builder,
                 SchemaBenchJson11Operations.PUT_PAYLOAD, SchemaBenchJson11Operations.GET_PAYLOAD,
                 AwsJson11Protocol::new),
    REST_JSON_1(SchemaBenchRestJsonOperations.SERVICE, SchemaBenchRestJsonException::builder,
                SchemaBenchRestJsonOperations.PUT_PAYLOAD, SchemaBenchRestJsonOperations.GET_PAYLOAD,
                RestJsonClientProtocol::new),
    REST_XML(SchemaBenchRestXmlOperations.SERVICE, SchemaBenchRestXmlException::builder,
             SchemaBenchRestXmlOperations.PUT_PAYLOAD, SchemaBenchRestXmlOperations.GET_PAYLOAD,
             RestXmlClientProtocol::new),
    AWS_QUERY(SchemaBenchQueryOperations.SERVICE, SchemaBenchQueryException::builder,
              SchemaBenchQueryOperations.PUT_PAYLOAD, SchemaBenchQueryOperations.GET_PAYLOAD,
              AwsQueryClientProtocol::new),
    EC2_QUERY(SchemaBenchEc2Operations.SERVICE, SchemaBenchEc2Exception::builder,
              SchemaBenchEc2Operations.PUT_PAYLOAD, SchemaBenchEc2Operations.GET_PAYLOAD,
              Ec2QueryClientProtocol::new);

    private final ApiService service;
    private final Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder;
    private final ApiOperation<?, ?> putPayload;
    private final ApiOperation<?, ?> getPayload;
    private final Function<ProtocolSettings, ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse>> protocolFactory;

    SchemaSerdeProtocol(ApiService service, Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder,
                        ApiOperation<?, ?> putPayload, ApiOperation<?, ?> getPayload,
                        Function<ProtocolSettings, ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse>> factory) {
        this.service = service;
        this.baseExceptionBuilder = baseExceptionBuilder;
        this.putPayload = putPayload;
        this.getPayload = getPayload;
        this.protocolFactory = factory;
    }

    /**
     * Creates the client protocol, configured as the generated client builder configures it.
     */
    public ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> createProtocol() {
        ProtocolSettings settings = ProtocolSettings.builder()
                                                    .service(service.schema().id())
                                                    .serviceSchema(service.schema())
                                                    .serviceVersion("1999-12-31")
                                                    .baseExceptionBuilder(baseExceptionBuilder)
                                                    .build();
        return protocolFactory.apply(settings);
    }

    /**
     * The operation whose input is the payload.
     */
    public ApiOperation<?, ?> putPayload() {
        return putPayload;
    }

    /**
     * The operation whose output is the payload.
     */
    public ApiOperation<?, ?> getPayload() {
        return getPayload;
    }
}
