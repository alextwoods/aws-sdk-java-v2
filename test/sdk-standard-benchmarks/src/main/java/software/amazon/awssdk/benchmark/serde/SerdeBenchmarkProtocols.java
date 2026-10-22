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

package software.amazon.awssdk.benchmark.serde;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.services.jsonrpc10dataplane.model.JsonRpc10DataPlaneException;
import software.amazon.awssdk.services.jsonrpc10dataplane.model.JsonRpc10DataPlaneOperations;
import software.amazon.awssdk.services.querydataplane.model.QueryDataPlaneException;
import software.amazon.awssdk.services.querydataplane.model.QueryDataPlaneOperations;
import software.amazon.awssdk.services.restjsondataplane.model.RestJsonDataPlaneException;
import software.amazon.awssdk.services.restjsondataplane.model.RestJsonDataPlaneOperations;
import software.amazon.awssdk.services.restxmldataplane.model.RestXmlDataPlaneException;
import software.amazon.awssdk.services.restxmldataplane.model.RestXmlDataPlaneOperations;
import software.amazon.awssdk.services.rpccbordataplane.model.RpcCborDataPlaneException;
import software.amazon.awssdk.services.rpccbordataplane.model.RpcCborDataPlaneOperations;

/**
 * The schema-based client protocol and operations of each service measured by the {@code serde} benchmarks.
 *
 * <p>Each protocol is created from {@link ProtocolSettings} the way the generated client builder creates it (see
 * {@code benchmark.schema.SchemaSerdeProtocol}). The benchmarks resolve the protocol and the {@link ApiOperation} of
 * their test case once per trial, and call {@link #serialize} or {@link #deserialize} in the timed method. Neither
 * signs nor sends.
 */
final class SerdeBenchmarkProtocols {
    static final SerdeBenchmarkProtocols JSON_RPC_10 = new SerdeBenchmarkProtocols(
        JsonRpc10DataPlaneOperations.SERVICE, JsonRpc10DataPlaneException::builder, AwsJson1Protocol::new,
        Arrays.asList(JsonRpc10DataPlaneOperations.GET_ITEM, JsonRpc10DataPlaneOperations.GET_METRIC_DATA,
                      JsonRpc10DataPlaneOperations.HEALTHCHECK, JsonRpc10DataPlaneOperations.PUT_ITEM,
                      JsonRpc10DataPlaneOperations.PUT_METRIC_DATA));

    static final SerdeBenchmarkProtocols REST_JSON = new SerdeBenchmarkProtocols(
        RestJsonDataPlaneOperations.SERVICE, RestJsonDataPlaneException::builder, RestJsonClientProtocol::new,
        Arrays.asList(RestJsonDataPlaneOperations.COPY_OBJECT, RestJsonDataPlaneOperations.GET_METRIC_DATA,
                      RestJsonDataPlaneOperations.GET_OBJECT, RestJsonDataPlaneOperations.HEALTHCHECK,
                      RestJsonDataPlaneOperations.PUT_METRIC_DATA, RestJsonDataPlaneOperations.PUT_OBJECT));

    static final SerdeBenchmarkProtocols REST_XML = new SerdeBenchmarkProtocols(
        RestXmlDataPlaneOperations.SERVICE, RestXmlDataPlaneException::builder, RestXmlClientProtocol::new,
        Arrays.asList(RestXmlDataPlaneOperations.COPY_OBJECT, RestXmlDataPlaneOperations.GET_METRIC_DATA,
                      RestXmlDataPlaneOperations.GET_OBJECT, RestXmlDataPlaneOperations.HEALTHCHECK,
                      RestXmlDataPlaneOperations.PUT_METRIC_DATA, RestXmlDataPlaneOperations.PUT_OBJECT));

    static final SerdeBenchmarkProtocols QUERY = new SerdeBenchmarkProtocols(
        QueryDataPlaneOperations.SERVICE, QueryDataPlaneException::builder, AwsQueryClientProtocol::new,
        Arrays.asList(QueryDataPlaneOperations.GET_ITEM, QueryDataPlaneOperations.GET_METRIC_DATA,
                      QueryDataPlaneOperations.HEALTHCHECK, QueryDataPlaneOperations.PUT_ITEM,
                      QueryDataPlaneOperations.PUT_METRIC_DATA));

    static final SerdeBenchmarkProtocols RPC_V2_CBOR = new SerdeBenchmarkProtocols(
        RpcCborDataPlaneOperations.SERVICE, RpcCborDataPlaneException::builder, RpcV2CborProtocol::new,
        Arrays.asList(RpcCborDataPlaneOperations.GET_ITEM, RpcCborDataPlaneOperations.GET_METRIC_DATA,
                      RpcCborDataPlaneOperations.HEALTHCHECK, RpcCborDataPlaneOperations.PUT_ITEM,
                      RpcCborDataPlaneOperations.PUT_METRIC_DATA));

    private final ApiService service;
    private final Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder;
    private final Function<ProtocolSettings, ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse>> protocolFactory;
    private final Map<String, ApiOperation<?, ?>> operations;

    private SerdeBenchmarkProtocols(ApiService service,
                                    Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder,
                                    Function<ProtocolSettings, ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse>>
                                        protocolFactory,
                                    List<ApiOperation<?, ?>> operations) {
        this.service = service;
        this.baseExceptionBuilder = baseExceptionBuilder;
        this.protocolFactory = protocolFactory;
        Map<String, ApiOperation<?, ?>> byName = new LinkedHashMap<>();
        operations.forEach(operation -> byName.put(operation.name(), operation));
        this.operations = Collections.unmodifiableMap(byName);
    }

    /**
     * Creates the client protocol, configured as the generated client builder configures it.
     */
    ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> createProtocol() {
        ProtocolSettings settings = ProtocolSettings.builder()
                                                    .service(service.schema().id())
                                                    .serviceSchema(service.schema())
                                                    .serviceVersion("1999-12-31")
                                                    .baseExceptionBuilder(baseExceptionBuilder)
                                                    .build();
        return protocolFactory.apply(settings);
    }

    /**
     * Returns the operation of a test case.
     *
     * @param operationName the operation name of the test case, for example {@code PutObject}
     * @throws IllegalArgumentException if the service has no such operation
     */
    ApiOperation<?, ?> operation(String operationName) {
        ApiOperation<?, ?> operation = operations.get(operationName);
        if (operation == null) {
            throw new IllegalArgumentException("Operation " + operationName + " is not an operation of "
                                               + service.name() + "; known operations: " + operations.keySet());
        }
        return operation;
    }

    /**
     * Serializes an input with {@link ClientProtocol#serializeRequest}.
     */
    @SuppressWarnings("unchecked")
    static <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest serialize(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol, ApiOperation<?, ?> operation,
        SerializableStruct input, ExecutionAttributes attributes, URI endpoint) {
        ApiOperation<I, O> typed = (ApiOperation<I, O>) operation;
        return protocol.serializeRequest(typed, (I) input, attributes, endpoint);
    }

    /**
     * Deserializes a response with {@link ClientProtocol#deserializeResponse}, with the operation's error registry and
     * no request.
     */
    @SuppressWarnings("unchecked")
    static <I extends SerializableStruct, O extends SerializableStruct> O deserialize(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol, ApiOperation<?, ?> operation,
        ExecutionAttributes attributes, SdkHttpFullResponse response) {
        ApiOperation<I, O> typed = (ApiOperation<I, O>) operation;
        return protocol.deserializeResponse(typed, attributes, typed.errorRegistry(), null, response);
    }
}
