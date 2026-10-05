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

package software.amazon.awssdk.protocols.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/** {@link ServiceProtocols}, {@link ModeledProtocol}, transport requirements and {@link SdkProtocolException}. */
class ProtocolDeclarationTest {
    private static final ShapeId SERVICE = ShapeId.from("com.example#Widgets");
    private static final ShapeId ERROR = ShapeId.from("com.example#Throttled");

    @Test
    void serviceProtocolsAreSortedUniqueAndExposeModeledData() {
        Schema serviceSchema = Schema.createService(SERVICE, XmlNamespaceTrait.builder()
                                                                               .uri("https://example.com/doc/")
                                                                               .build());
        ServiceProtocols protocols = ServiceProtocols.builder(serviceSchema)
                                                     .protocol(ModeledProtocol.of(ProtocolIds.EC2_QUERY))
                                                     .protocol(ModeledProtocol.of(ProtocolIds.AWS_QUERY))
                                                     .awsQueryCompatible(true)
                                                     .awsQueryError(ERROR, "Throttling", 400)
                                                     .build();
        assertEquals(SERVICE, protocols.serviceId());
        assertEquals(List.of(ProtocolIds.AWS_QUERY, ProtocolIds.EC2_QUERY), protocols.protocolIds());
        assertTrue(protocols.declares(ProtocolIds.AWS_QUERY));
        assertFalse(protocols.declares(ProtocolIds.REST_XML));
        assertEquals("https://example.com/doc/", protocols.xmlNamespace().orElseThrow().getUri());
        assertTrue(protocols.awsQueryCompatible());
        assertEquals(new AwsQueryErrorCode(ERROR, "Throttling", 400), protocols.awsQueryErrors().get(ERROR));
        assertThrows(UnsupportedOperationException.class, () -> protocols.awsQueryErrors().clear());
    }

    @Test
    void duplicatesAndNonServiceSchemasAreRejected() {
        ServiceProtocols.Builder builder = ServiceProtocols.builder(Schema.createService(SERVICE))
                                                           .protocol(ModeledProtocol.of(ProtocolIds.REST_XML))
                                                           .awsQueryError(ERROR, "A", 400);
        assertThrows(IllegalArgumentException.class, () -> builder.protocol(ModeledProtocol.of(ProtocolIds.REST_XML)));
        assertThrows(IllegalArgumentException.class, () -> builder.awsQueryError(ERROR, "B", 500));
        assertThrows(IllegalArgumentException.class, () -> ServiceProtocols.builder(PreludeSchemas.STRING));
        assertEquals(Optional.empty(), builder.build().xmlNamespace());
    }

    @Test
    void queryProtocolDataIsRecordedAndValidated() {
        ShapeId member = ShapeId.from("com.example#Input$token");
        ServiceProtocols protocols = ServiceProtocols.builder(Schema.createService(SERVICE))
                                                     .serviceVersion("2020-01-08")
                                                     .ec2QueryName(member, "Token")
                                                     .build();
        assertEquals("2020-01-08", protocols.serviceVersion());
        assertEquals(java.util.Map.of(member, "Token"), protocols.ec2QueryNames());
        assertThrows(UnsupportedOperationException.class, () -> protocols.ec2QueryNames().clear());
        assertEquals("", ServiceProtocols.builder(Schema.createService(SERVICE)).build().serviceVersion());

        ServiceProtocols.Builder builder = ServiceProtocols.builder(Schema.createService(SERVICE))
                                                           .ec2QueryName(member, "Token");
        assertThrows(IllegalArgumentException.class, () -> builder.ec2QueryName(member, "Other"));
        assertThrows(IllegalArgumentException.class, () -> builder.ec2QueryName(ERROR, "NotAMember"));
        assertThrows(IllegalArgumentException.class,
                     () -> builder.ec2QueryName(ShapeId.from("com.example#Input$other"), ""));
    }

    @Test
    void modeledProtocolValidatesOptions() {
        ModeledProtocol json = new ModeledProtocol(ProtocolIds.AWS_JSON_1_1, List.of("h2", "http/1.1"), List.of("h2"),
                                                   false);
        assertEquals(Optional.of("1.1"), json.awsJsonVersion());
        assertEquals(Optional.of("1.0"), ModeledProtocol.of(ProtocolIds.AWS_JSON_1_0).awsJsonVersion());
        assertEquals(Optional.empty(), ModeledProtocol.of(ProtocolIds.REST_JSON_1).awsJsonVersion());
        assertTrue(new ModeledProtocol(ProtocolIds.REST_XML, List.of(), List.of(), true).noErrorWrapping());
        assertThrows(IllegalArgumentException.class,
                     () -> new ModeledProtocol(ProtocolIds.REST_JSON_1, List.of(), List.of(), true));
        assertThrows(IllegalArgumentException.class, () -> ModeledProtocol.of(ShapeId.from("a#b$c")));
        assertThrows(IllegalArgumentException.class, () -> new AwsQueryErrorCode(ERROR, "", 400));
        assertThrows(IllegalArgumentException.class, () -> new AwsQueryErrorCode(ERROR, "X", 99));
    }

    @Test
    void transportRequirementsFollowTheModeledHttpVersions() {
        ProtocolTransportRequirements plain = ProtocolTransportRequirements.http(ModeledProtocol.of(
            ProtocolIds.REST_JSON_1));
        assertEquals(List.of("http/1.1"), plain.httpVersions());
        assertEquals(List.of("http/1.1"), plain.eventStreamHttpVersions());

        ProtocolTransportRequirements h2 = ProtocolTransportRequirements.http(new ModeledProtocol(
            ProtocolIds.REST_JSON_1, List.of("http/1.1", "h2"), List.of("h2"), false));
        assertEquals(List.of(), h2.incompatibilities("apache", ProtocolTransportRequirements.Message.HTTP,
                                                     Set.of("http/1.1"), false));
        List<String> reasons = h2.incompatibilities("apache", ProtocolTransportRequirements.Message.HTTP,
                                                    Set.of("http/1.1"), true);
        assertEquals(1, reasons.size());
        assertTrue(reasons.get(0).contains(ProtocolIds.REST_JSON_1.toString()) && reasons.get(0).contains("apache"),
                   reasons.get(0));
        assertEquals(List.of(), h2.incompatibilities("netty", ProtocolTransportRequirements.Message.HTTP,
                                                     Set.of("h2"), true));
        assertEquals(1, plain.incompatibilities("h2-only", ProtocolTransportRequirements.Message.HTTP,
                                                Set.of("h2"), false).size());
    }

    @Test
    void protocolExceptionIsLocatedAndNotRetryable() {
        SdkProtocolException e = SdkProtocolException.builder()
                                                     .protocolId(ProtocolIds.REST_JSON_1)
                                                     .operationId(ShapeId.from("com.example#GetWidget"))
                                                     .shapeId(ShapeId.from("com.example#Size"))
                                                     .memberPath(List.of("details", "sizes", "[2]", "attrs", "{k}"))
                                                     .wireLocation("body")
                                                     .message("expected an integer")
                                                     .build();
        assertEquals("details.sizes[2].attrs{k}", e.renderedMemberPath());
        assertEquals(Optional.of("body"), e.wireLocation());
        assertFalse(e.retryable());
        String message = e.getMessage();
        assertTrue(message.contains("com.example#GetWidget") && message.contains("com.example#Size")
                   && message.contains("details.sizes[2].attrs{k}") && message.contains("expected an integer"),
                   message);
        SdkProtocolException copy = e.toBuilder().build();
        assertEquals(message, copy.getMessage());
        assertEquals(e.memberPath(), copy.memberPath());

        SdkProtocolException root = SdkProtocolException.builder()
                                                        .protocolId(ProtocolIds.AWS_JSON_1_0)
                                                        .operationId(ShapeId.from("com.example#Op"))
                                                        .shapeId(ShapeId.from("com.example#OpOutput"))
                                                        .message("unparseable body")
                                                        .build();
        assertEquals("<root>", root.renderedMemberPath());
        assertThrows(NullPointerException.class, () -> SdkProtocolException.builder().message("m").build());
    }
}
