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

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.OperationShape;

/**
 * {@link DynamicEndpointParams} and {@link DynamicAuthSchemeParams}, and the consumer overloads of
 * {@link DynamicEndpointProvider} and {@link DynamicAuthSchemeResolver}.
 */
class DynamicResolverParamsTest {
    private static final ShapeId REST_JSON = ShapeId.from("aws.protocols#restJson1");
    private static final ExecutionAttribute<String> MARKER = new ExecutionAttribute<>("DynamicResolverParamsTest");

    @Test
    void endpointParams_equalsAndHashCode_useEveryField() {
        EqualsVerifier.forClass(DynamicEndpointParams.class)
                      .withNonnullFields("operation", "input", "executionAttributes")
                      .withPrefabValues(ExecutionAttributes.class, attributes("red"), attributes("blue"))
                      .verify();
    }

    @Test
    void authSchemeParams_equalsAndHashCode_useEveryField() {
        EqualsVerifier.forClass(DynamicAuthSchemeParams.class)
                      .withNonnullFields("protocolId", "operation", "executionAttributes")
                      .withPrefabValues(ExecutionAttributes.class, attributes("red"), attributes("blue"))
                      .withPrefabValues(ShapeId.class, REST_JSON, ShapeId.from("aws.protocols#awsJson1_0"))
                      .verify();
    }

    @Test
    void endpointParams_build_keepsTheValuesAndRoundTripsThroughToBuilder() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();
        SerializableStruct input = input(operation);
        ExecutionAttributes attributes = attributes("value");

        DynamicEndpointParams params = DynamicEndpointParams.builder()
                                                            .operation(operation)
                                                            .input(input)
                                                            .executionAttributes(attributes)
                                                            .build();

        assertThat(params.operation()).isSameAs(operation);
        assertThat(params.input()).isSameAs(input);
        assertThat(params.executionAttributes()).isSameAs(attributes);
        assertThat(params.toBuilder().build()).isEqualTo(params);
        assertThat(params.toString())
            .isEqualTo("DynamicEndpointParams(operation=test.weather#GetForecast)");
    }

    @Test
    void endpointParams_withoutAttributes_haveEmptyAttributes() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();
        DynamicEndpointParams params = DynamicEndpointParams.builder().operation(operation).input(input(operation)).build();

        assertThat(params.executionAttributes()).isEqualTo(new ExecutionAttributes());
    }

    @Test
    void endpointParams_withoutRequiredValues_throw() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();

        assertThatThrownBy(() -> DynamicEndpointParams.builder().input(input(operation)).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("operation");
        assertThatThrownBy(() -> DynamicEndpointParams.builder().operation(operation).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("input");
    }

    @Test
    void authSchemeParams_build_keepsTheValuesAndRoundTripsThroughToBuilder() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();
        ExecutionAttributes attributes = attributes("value");

        DynamicAuthSchemeParams params = DynamicAuthSchemeParams.builder()
                                                                .protocolId(REST_JSON)
                                                                .operation(operation)
                                                                .executionAttributes(attributes)
                                                                .build();

        assertThat(params.protocolId()).isEqualTo(REST_JSON);
        assertThat(params.operation()).isSameAs(operation);
        assertThat(params.executionAttributes()).isSameAs(attributes);
        assertThat(params.toBuilder().build()).isEqualTo(params);
        assertThat(params.toString())
            .isEqualTo("DynamicAuthSchemeParams(protocolId=aws.protocols#restJson1, operation=test.weather#GetForecast)");
    }

    @Test
    void authSchemeParams_withoutRequiredValues_throw() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();

        assertThatThrownBy(() -> DynamicAuthSchemeParams.builder().operation(operation).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("protocolId");
        assertThatThrownBy(() -> DynamicAuthSchemeParams.builder().protocolId(REST_JSON).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("operation");
        assertThat(DynamicAuthSchemeParams.builder().protocolId(REST_JSON).operation(operation).build()
                                          .executionAttributes())
            .isEqualTo(new ExecutionAttributes());
    }

    @Test
    void endpointProvider_resolveEndpointWithAConsumer_buildsTheParams() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();
        SerializableStruct input = input(operation);
        AtomicReference<DynamicEndpointParams> seen = new AtomicReference<>();
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://example.com")).build();
        DynamicEndpointProvider provider = params -> {
            seen.set(params);
            return CompletableFuture.completedFuture(endpoint);
        };

        assertThat(provider.resolveEndpoint(p -> p.operation(operation).input(input)).join()).isSameAs(endpoint);
        assertThat(seen.get().operation()).isSameAs(operation);
        assertThat(seen.get().input()).isSameAs(input);
    }

    @Test
    void authSchemeResolver_resolveAuthSchemeWithAConsumer_buildsTheParams() {
        ApiOperation<StructDocument, StructDocument> operation = getForecast();
        AtomicReference<DynamicAuthSchemeParams> seen = new AtomicReference<>();
        List<AuthSchemeOption> options =
            Collections.singletonList(AuthSchemeOption.builder().schemeId("smithy.api#noAuth").build());
        DynamicAuthSchemeResolver resolver = params -> {
            seen.set(params);
            return options;
        };

        assertThat(resolver.resolveAuthScheme(p -> p.protocolId(REST_JSON).operation(operation))).isSameAs(options);
        assertThat(seen.get().protocolId()).isEqualTo(REST_JSON);
        assertThat(seen.get().operation()).isSameAs(operation);
    }

    /**
     * The GetForecast operation of the weather model, assembled from converted schemas. Dynamic operations are task
     * 13.7; the params only hold the operation.
     */
    private static ApiOperation<StructDocument, StructDocument> getForecast() {
        try (DynamicClient client = DynamicClient.builder()
                                                 .model(DynamicTestModels.weather())
                                                 .region(Region.US_WEST_2)
                                                 .credentialsProvider(TestCredentials.PROVIDER)
                                                 .httpClient(new StubHttpClients.Sync())
                                                 .httpClient(new StubHttpClients.Async())
                                                 .build()) {
            ApiService service = client.service();
            SchemaConverter converter = client.schemaConverter();
            OperationShape shape = client.model().expectShape(
                software.amazon.smithy.model.shapes.ShapeId.from("test.weather#GetForecast"), OperationShape.class);
            Schema operation = converter.getSchema(shape);
            Schema inputSchema = converter.getSchema(shape.getInputShape());
            Schema outputSchema = converter.getSchema(shape.getOutputShape());
            return SimpleApiOperation.<StructDocument, StructDocument>builder()
                                     .schema(operation)
                                     .inputSchema(() -> inputSchema)
                                     .outputSchema(() -> outputSchema)
                                     .errorRegistry(TypeRegistry.empty())
                                     .service(service)
                                     .build();
        }
    }

    private static SerializableStruct input(ApiOperation<StructDocument, StructDocument> operation) {
        return operation.inputBuilder().build();
    }

    private static ExecutionAttributes attributes(String value) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(MARKER, value);
        return attributes;
    }
}
