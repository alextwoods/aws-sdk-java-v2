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

package software.amazon.awssdk.bridge.smithyjava.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson11Protocol;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;

class ProtocolResolverTest {

    private ProtocolResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ProtocolResolver();
    }

    @Test
    void resolve_awsJson1_0_returnsAwsJson1Protocol() {
        ApiOperation<?, ?> operation = operationWithServiceProtocol(
            ShapeId.from("com.example#MyService"),
            AwsJson1_0Trait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> protocol = resolver.resolve(operation);

        assertThat(protocol).isInstanceOf(AwsJson1Protocol.class);
    }

    @Test
    void resolve_awsJson1_1_returnsAwsJson11Protocol() {
        ApiOperation<?, ?> operation = operationWithServiceProtocol(
            ShapeId.from("com.example#JsonService"),
            AwsJson1_1Trait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> protocol = resolver.resolve(operation);

        assertThat(protocol).isInstanceOf(AwsJson11Protocol.class);
    }

    @Test
    void resolve_restJson1_returnsRestJsonClientProtocol() {
        ApiOperation<?, ?> operation = operationWithServiceProtocol(
            ShapeId.from("com.example#RestJsonService"),
            RestJson1Trait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> protocol = resolver.resolve(operation);

        assertThat(protocol).isInstanceOf(RestJsonClientProtocol.class);
    }

    @Test
    void resolve_restXml_returnsRestXmlClientProtocol() {
        ApiOperation<?, ?> operation = operationWithServiceProtocol(
            ShapeId.from("com.example#RestXmlService"),
            RestXmlTrait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> protocol = resolver.resolve(operation);

        assertThat(protocol).isInstanceOf(RestXmlClientProtocol.class);
    }

    @Test
    void resolve_awsQuery_throwsUnsupportedOperationException() {
        ApiOperation<?, ?> operation = operationWithServiceProtocol(
            ShapeId.from("com.example#QueryService"),
            new AwsQueryTrait()
        );

        assertThatThrownBy(() -> resolver.resolve(operation))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("awsQuery")
            .hasMessageContaining("not yet supported");
    }

    @Test
    void resolve_rpcV2Cbor_returnsRpcV2CborProtocol() {
        ApiOperation<?, ?> operation = operationWithServiceProtocol(
            ShapeId.from("com.example#CborService"),
            Rpcv2CborTrait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> protocol = resolver.resolve(operation);

        assertThat(protocol).isInstanceOf(RpcV2CborProtocol.class);
    }

    @Test
    void resolve_noRecognizedProtocol_throwsSdkClientException() {
        ShapeId serviceId = ShapeId.from("com.example#NoProtocolService");
        // Create a service schema with no protocol traits
        Schema serviceSchema = Schema.createService(serviceId);

        ApiService apiService = mock(ApiService.class);
        when(apiService.schema()).thenReturn(serviceSchema);

        ApiOperation<?, ?> operation = mock(ApiOperation.class);
        when(operation.service()).thenReturn(apiService);

        assertThatThrownBy(() -> resolver.resolve(operation))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No recognized protocol trait")
            .hasMessageContaining("com.example#NoProtocolService");
    }

    @Test
    void resolve_sameService_returnsCachedInstance() {
        ShapeId serviceId = ShapeId.from("com.example#CachedService");
        ApiOperation<?, ?> operation1 = operationWithServiceProtocol(
            serviceId,
            AwsJson1_0Trait.builder().build()
        );
        ApiOperation<?, ?> operation2 = operationWithServiceProtocol(
            serviceId,
            AwsJson1_0Trait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> first = resolver.resolve(operation1);
        ClientProtocol<HttpRequest, HttpResponse> second = resolver.resolve(operation2);

        assertThat(first).isSameAs(second);
    }

    @Test
    void resolve_differentServices_returnsDifferentInstances() {
        ApiOperation<?, ?> operation1 = operationWithServiceProtocol(
            ShapeId.from("com.example#ServiceA"),
            AwsJson1_0Trait.builder().build()
        );
        ApiOperation<?, ?> operation2 = operationWithServiceProtocol(
            ShapeId.from("com.example#ServiceB"),
            AwsJson1_0Trait.builder().build()
        );

        ClientProtocol<HttpRequest, HttpResponse> first = resolver.resolve(operation1);
        ClientProtocol<HttpRequest, HttpResponse> second = resolver.resolve(operation2);

        assertThat(first).isNotSameAs(second);
    }

    /**
     * Helper that creates a mock ApiOperation whose service schema carries the given protocol trait.
     */
    private static ApiOperation<?, ?> operationWithServiceProtocol(
            ShapeId serviceId,
            software.amazon.smithy.model.traits.Trait protocolTrait) {

        Schema serviceSchema = Schema.createService(serviceId, protocolTrait);

        ApiService apiService = mock(ApiService.class);
        when(apiService.schema()).thenReturn(serviceSchema);

        ApiOperation<?, ?> operation = mock(ApiOperation.class);
        when(operation.service()).thenReturn(apiService);

        return operation;
    }
}
