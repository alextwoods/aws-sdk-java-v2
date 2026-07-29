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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
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
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;

/**
 * Property-based test for {@link ProtocolResolver}.
 *
 * <p><b>Property 10: Protocol Trait Resolution</b></p>
 * <p>For any {@code ApiOperation} whose service schema carries exactly one of the six recognized
 * protocol traits, the {@code ProtocolResolver} SHALL return the corresponding {@code ClientProtocol}
 * implementation, and subsequent calls for the same service SHALL return the same instance.</p>
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.9</b></p>
 */
class ProtocolResolverPropertyTest {

    /**
     * Enum representing the supported protocol traits (excluding awsQuery which is currently
     * unsupported and throws UnsupportedOperationException).
     */
    enum SupportedProtocol {
        AWS_JSON_1_0(AwsJson1_0Trait.builder().build(), AwsJson1Protocol.class),
        AWS_JSON_1_1(AwsJson1_1Trait.builder().build(), AwsJson11Protocol.class),
        REST_JSON_1(RestJson1Trait.builder().build(), RestJsonClientProtocol.class),
        REST_XML(RestXmlTrait.builder().build(), RestXmlClientProtocol.class),
        RPC_V2_CBOR(Rpcv2CborTrait.builder().build(), RpcV2CborProtocol.class);

        private final Trait trait;
        private final Class<? extends ClientProtocol> expectedProtocolClass;

        SupportedProtocol(Trait trait, Class<? extends ClientProtocol> expectedProtocolClass) {
            this.trait = trait;
            this.expectedProtocolClass = expectedProtocolClass;
        }

        Trait trait() {
            return trait;
        }

        Class<? extends ClientProtocol> expectedProtocolClass() {
            return expectedProtocolClass;
        }
    }

    @Provide
    Arbitrary<SupportedProtocol> supportedProtocols() {
        return Arbitraries.of(SupportedProtocol.values());
    }

    @Provide
    Arbitrary<String> serviceNamespaces() {
        return Arbitraries.of(
            "com.amazonaws.dynamodb",
            "com.amazonaws.s3",
            "com.amazonaws.lambda",
            "com.amazonaws.sqs",
            "com.amazonaws.sns",
            "com.amazonaws.iam",
            "com.amazonaws.ec2",
            "com.amazonaws.sts",
            "com.amazonaws.kinesis",
            "com.amazonaws.cloudwatch"
        );
    }

    @Provide
    Arbitrary<String> serviceNames() {
        return Arbitraries.of(
            "MyService",
            "DynamoDB",
            "S3",
            "Lambda",
            "SQS",
            "SNS",
            "IAM",
            "EC2",
            "STS",
            "Kinesis"
        );
    }

    /**
     * Property: For any supported protocol trait, the ProtocolResolver returns
     * the correct ClientProtocol implementation type.
     */
    @Property(tries = 100)
    void resolveReturnsCorrectProtocolForSupportedTraits(
            @ForAll("supportedProtocols") SupportedProtocol protocol,
            @ForAll("serviceNamespaces") String namespace,
            @ForAll("serviceNames") String serviceName) {

        ShapeId serviceShapeId = ShapeId.from(namespace + "#" + serviceName);
        Schema serviceSchema = Schema.createService(serviceShapeId, protocol.trait());
        ApiOperation<?, ?> operation = mockOperation(serviceSchema);

        ProtocolResolver resolver = new ProtocolResolver();
        ClientProtocol<HttpRequest, HttpResponse> result = resolver.resolve(operation);

        assertThat(result).isInstanceOf(protocol.expectedProtocolClass());
    }

    /**
     * Property: For any service, calling resolve() twice with operations on the same service
     * returns the same cached instance (reference equality).
     */
    @Property(tries = 100)
    void resolveReturnsCachedInstanceForSameService(
            @ForAll("supportedProtocols") SupportedProtocol protocol,
            @ForAll("serviceNamespaces") String namespace,
            @ForAll("serviceNames") String serviceName) {

        ShapeId serviceShapeId = ShapeId.from(namespace + "#" + serviceName);
        Schema serviceSchema = Schema.createService(serviceShapeId, protocol.trait());

        // Create two different operation mocks that share the same service schema
        ApiOperation<?, ?> operation1 = mockOperation(serviceSchema);
        ApiOperation<?, ?> operation2 = mockOperation(serviceSchema);

        ProtocolResolver resolver = new ProtocolResolver();
        ClientProtocol<HttpRequest, HttpResponse> first = resolver.resolve(operation1);
        ClientProtocol<HttpRequest, HttpResponse> second = resolver.resolve(operation2);

        assertThat(first).isSameAs(second);
    }

    /**
     * Property: AwsQuery protocol trait throws UnsupportedOperationException
     * (not yet supported by the bridge).
     */
    @Property(tries = 100)
    void resolveThrowsForAwsQueryProtocol(
            @ForAll("serviceNamespaces") String namespace,
            @ForAll("serviceNames") String serviceName) {

        ShapeId serviceShapeId = ShapeId.from(namespace + "#" + serviceName);
        Schema serviceSchema = Schema.createService(serviceShapeId, new AwsQueryTrait());
        ApiOperation<?, ?> operation = mockOperation(serviceSchema);

        ProtocolResolver resolver = new ProtocolResolver();

        try {
            resolver.resolve(operation);
            assertThat(false).as("Expected UnsupportedOperationException to be thrown").isTrue();
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("awsQuery");
            assertThat(e.getMessage()).contains(serviceShapeId.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private ApiOperation<?, ?> mockOperation(Schema serviceSchema) {
        ApiOperation<?, ?> operation = mock(ApiOperation.class);
        ApiService service = mock(ApiService.class);
        when(operation.service()).thenReturn(service);
        when(service.schema()).thenReturn(serviceSchema);
        return operation;
    }
}
