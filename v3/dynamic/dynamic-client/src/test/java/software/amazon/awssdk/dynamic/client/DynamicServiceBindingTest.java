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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.pipeline.client.AwsServiceClientDefinition;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The pipeline binding of a runtime service and its operations carries the same per-service and per-operation facts
 * generated services emit (Requirements 14.14, 14.15): protocols and protocol data, type registry, endpoint rules
 * and bindings, modeled auth, the {@code httpChecksum} trait, and the default credentials of AWS-credential services.
 */
class DynamicServiceBindingTest {

    private static final String MODEL = """
        $version: "2"
        namespace example.bind

        use aws.auth#sigv4
        use aws.auth#unsignedPayload
        use aws.protocols#awsJson1_0
        use aws.protocols#awsQueryCompatible
        use aws.protocols#awsQueryError
        use aws.protocols#httpChecksum
        use smithy.rules#contextParam
        use smithy.rules#endpointRuleSet
        use smithy.rules#operationContextParams
        use smithy.rules#staticContextParams

        @awsJson1_0
        @awsQueryCompatible
        @sigv4(name: "binder")
        @httpBearerAuth
        @auth([sigv4, httpBearerAuth])
        @aws.api#service(sdkId: "Bind", endpointPrefix: "bindy")
        @endpointRuleSet({
            version: "1.0"
            parameters: {
                Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
                Bucket: { type: "string", required: false, documentation: "The bucket." }
                Flag: { type: "boolean", required: false, documentation: "A flag." }
                Arns: { type: "stringArray", required: false, documentation: "The ARNs." }
            }
            rules: [
                {
                    type: "endpoint"
                    conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
                    endpoint: { url: "https://bind.{Region}.example.com" }
                }
                { type: "error", conditions: [], error: "A region is required" }
            ]
        })
        service Bind {
            version: "2021-01-01"
            operations: [Put, Get]
            errors: [Gone]
        }

        @staticContextParams(Flag: { value: true })
        @operationContextParams(Arns: { path: "items[*].arn" })
        @endpoint(hostPrefix: "{account}.")
        @optionalAuth
        @unsignedPayload
        @auth([sigv4])
        @httpChecksum(requestChecksumRequired: true, requestAlgorithmMember: "algorithm")
        operation Put {
            input := {
                @required
                @hostLabel
                account: String
                @contextParam(name: "Bucket")
                bucket: String
                items: Items
                algorithm: ChecksumAlgorithm
            }
        }

        operation Get {}

        list Items {
            member: Item
        }

        structure Item {
            arn: String
        }

        enum ChecksumAlgorithm {
            CRC32
            CRC32C
            SHA1
            SHA256
        }

        @error("client")
        @awsQueryError(code: "Gone", httpResponseCode: 410)
        structure Gone {}
        """;

    private static DynamicService service;
    private static DynamicServiceBinding binding;

    @BeforeAll
    static void load() {
        service = TestSupport.load(MODEL).service();
        binding = DynamicServiceBinding.of(service);
    }

    @Test
    void theDefinitionCarriesTheServiceFactsGeneratedServicesDeclare() {
        AwsServiceClientDefinition definition = binding.definition();
        assertEquals(ShapeId.from("example.bind#Bind"), definition.serviceId());
        assertEquals("bindy", definition.serviceName());
        assertTrue(definition.rules().isPresent());
        assertTrue(definition.defaultCredentialsFactory().isPresent(), "SigV4 services default to the chain");

        ServiceProtocols protocols = definition.protocols();
        assertSame(service.schema(), protocols.serviceSchema());
        assertEquals(List.of(ShapeId.from("aws.protocols#awsJson1_0")), protocols.protocolIds());
        assertTrue(protocols.awsQueryCompatible());
        assertEquals("Gone", protocols.awsQueryErrors().get(ShapeId.from("example.bind#Gone")).code());

        SchemaTypeRegistry registry = definition.typeRegistry();
        assertEquals(DynamicServiceException.class, registry.getShapeClass(ShapeId.from("example.bind#Gone")));
        assertEquals(StructDocument.class, registry.getShapeClass(ShapeId.from("example.bind#Item")));
        assertTrue(registry.contains(ShapeId.from("example.bind#PutInput")));
    }

    @Test
    void operationBindingsCarryEndpointAuthAndChecksumMetadata() {
        DynamicOperationBinding put = binding.operation("Put");
        assertSame(put, binding.operation("Put"), "one binding per client and operation");
        AwsOperation<StructDocument, StructDocument> operation = put.pipeline();
        assertSame(service.operation("Put").descriptor(), operation.descriptor());

        EndpointBinding endpoint = operation.endpointBinding();
        assertEquals("Put", endpoint.operationName());
        assertEquals(Map.of("Flag", true), endpoint.staticContextParams());
        assertEquals(Map.of("Bucket", "bucket"), endpoint.contextParams());
        assertEquals(List.of("Arns"), List.copyOf(endpoint.operationContextParams().keySet()));
        assertTrue(endpoint.hostPrefix().isPresent());
        assertFalse(endpoint.discovery().isPresent());

        ModeledAuth auth = operation.auth().orElseThrow();
        assertEquals("example.bind#Put", auth.operationId());
        // The operation's @auth, then smithy.api#noAuth for @optionalAuth.
        assertEquals(List.of("aws.auth#sigv4", "smithy.api#noAuth"), auth.effectiveSchemeIds());
        assertTrue(auth.optionalAuth());
        assertTrue(auth.unsignedPayload());

        HttpChecksumMetadata checksum = operation.httpChecksum().orElseThrow();
        assertTrue(checksum.requestChecksumRequired());
        assertEquals(Optional.of("algorithm"), checksum.requestAlgorithmMember());

        AwsOperation<StructDocument, StructDocument> get = binding.operation("Get").pipeline();
        assertEquals(List.of("aws.auth#sigv4", "smithy.api#httpBearerAuth"),
                     get.auth().orElseThrow().effectiveSchemeIds());
        assertFalse(get.auth().orElseThrow().optionalAuth());
        assertFalse(get.httpChecksum().isPresent());
        assertEquals(Map.of(), get.endpointBinding().staticContextParams());
    }
}
