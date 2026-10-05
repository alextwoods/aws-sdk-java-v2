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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

class ProtocolSelectorTest {
    private static final ShapeId SERVICE = ShapeId.from("com.example#Widgets");
    private static final ShapeId CUSTOM = ShapeId.from("com.example#customProtocol");

    private static ServiceProtocols declaring(ShapeId... protocols) {
        ServiceProtocols.Builder builder = ServiceProtocols.builder(Schema.createService(SERVICE));
        for (ShapeId protocol : protocols) {
            builder.protocol(ModeledProtocol.of(protocol));
        }
        return builder.build();
    }

    private static InstalledProtocols installing(ShapeId... protocols) {
        return InstalledProtocols.of(Arrays.stream(protocols)
                                                     .map(id -> TestProviders.of(id, false))
                                                     .toList());
    }

    @Test
    void singleDeclaredInstalledProtocolIsSelectedByDefault() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_JSON_1_0);
        InstalledProtocols installed = installing(ProtocolIds.AWS_JSON_1_0, ProtocolIds.REST_XML);
        SelectedProtocol selected = ProtocolSelector.select(service, installed);
        assertEquals(ProtocolIds.AWS_JSON_1_0, selected.protocolId());
        assertSame(service.protocol(ProtocolIds.AWS_JSON_1_0).orElseThrow(), selected.declaration());
        assertSame(installed.provider(ProtocolIds.AWS_JSON_1_0).orElseThrow(), selected.provider());
        assertEquals(SelectedProtocol.Source.MODEL_DEFAULT, selected.source());
    }

    @Test
    void defaultSelectionFollowsTheFixedPrecedenceNotDeclarationOrInstallationOrder() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_QUERY, ProtocolIds.RPC_V2_CBOR, ProtocolIds.AWS_JSON_1_0);
        assertEquals(ProtocolIds.RPC_V2_CBOR, ProtocolSelector.select(service, installing(
            ProtocolIds.AWS_QUERY, ProtocolIds.AWS_JSON_1_0, ProtocolIds.RPC_V2_CBOR)).protocolId());
        assertEquals(ProtocolIds.AWS_JSON_1_0, ProtocolSelector.select(service, installing(
            ProtocolIds.AWS_QUERY, ProtocolIds.AWS_JSON_1_0)).protocolId());
        assertEquals(ProtocolIds.AWS_QUERY, ProtocolSelector.select(service, installing(
            ProtocolIds.AWS_QUERY, ProtocolIds.REST_XML)).protocolId());
    }

    @Test
    void protocolsOutsideThePrecedenceRankLastByShapeId() {
        ShapeId other = ShapeId.from("com.example#anotherProtocol");
        ServiceProtocols service = declaring(CUSTOM, other, ProtocolIds.EC2_QUERY);
        assertEquals(ProtocolIds.EC2_QUERY,
                     ProtocolSelector.select(service, installing(CUSTOM, other, ProtocolIds.EC2_QUERY)).protocolId());
        assertEquals(other, ProtocolSelector.select(service, installing(CUSTOM, other)).protocolId());
    }

    @Test
    void defaultSelectionRejectsWhenNoDeclaredProtocolIsInstalled() {
        ServiceProtocols service = declaring(ProtocolIds.REST_JSON_1, ProtocolIds.AWS_JSON_1_1);
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class, () -> ProtocolSelector.select(
            service, installing(ProtocolIds.REST_XML)));
        assertEquals(ProtocolSelectionException.Reason.NO_INSTALLED_DECLARED_PROTOCOL, e.reason());
        assertEquals(Optional.of(SERVICE), e.serviceId());
        assertEquals(Optional.empty(), e.requested());
        assertEquals(List.of(ProtocolIds.AWS_JSON_1_1, ProtocolIds.REST_JSON_1), e.declared());
        assertEquals(List.of(ProtocolIds.REST_XML), e.installed());
        assertFalse(e.retryable());
        assertInstanceOf(SdkClientConfigurationException.class, e);
        assertEquals(Optional.empty(), e.option());
        ProtocolSelectionException copy = e.toBuilder().option("protocol").build();
        assertEquals(Optional.of("protocol"), copy.option());
        assertEquals(e.reason(), copy.reason());
        assertTrue(e.getMessage().contains(SERVICE.toString()), e.getMessage());
        assertThrows(ProtocolSelectionException.class, () -> ProtocolSelector.select(service,
                                                                                     InstalledProtocols.none()));
    }

    @Test
    void explicitIdTakesPrecedenceOverTheDefault() {
        ServiceProtocols service = declaring(ProtocolIds.RPC_V2_CBOR, ProtocolIds.AWS_JSON_1_0);
        InstalledProtocols installed = installing(ProtocolIds.RPC_V2_CBOR, ProtocolIds.AWS_JSON_1_0);
        SelectedProtocol selected = ProtocolSelector.select(service, installed, ProtocolIds.AWS_JSON_1_0);
        assertEquals(ProtocolIds.AWS_JSON_1_0, selected.protocolId());
        assertEquals(SelectedProtocol.Source.EXPLICIT_ID, selected.source());
    }

    @Test
    void explicitIdMustBeDeclared() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_JSON_1_0);
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class, () -> ProtocolSelector.select(
            service, installing(ProtocolIds.AWS_JSON_1_0, ProtocolIds.REST_XML), ProtocolIds.REST_XML));
        assertEquals(ProtocolSelectionException.Reason.NOT_DECLARED, e.reason());
        assertEquals(Optional.of(ProtocolIds.REST_XML), e.requested());
        assertEquals(List.of(ProtocolIds.AWS_JSON_1_0), e.declared());
        assertTrue(e.getMessage().contains("not declared"), e.getMessage());
    }

    @Test
    void explicitIdMustBeInstalled() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_JSON_1_0, ProtocolIds.RPC_V2_CBOR);
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class, () -> ProtocolSelector.select(
            service, installing(ProtocolIds.AWS_JSON_1_0), ProtocolIds.RPC_V2_CBOR));
        assertEquals(ProtocolSelectionException.Reason.NOT_INSTALLED, e.reason());
        assertEquals(Optional.of(ProtocolIds.RPC_V2_CBOR), e.requested());
        assertEquals(List.of(ProtocolIds.AWS_JSON_1_0), e.installed());
    }

    @Test
    void explicitProviderIsUsedWithoutInstalledRuntimes() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_JSON_1_0, CUSTOM);
        ProtocolProvider custom = TestProviders.of(CUSTOM, true);
        SelectedProtocol selected = ProtocolSelector.select(service, custom);
        assertSame(custom, selected.provider());
        assertEquals(SelectedProtocol.Source.EXPLICIT_PROVIDER, selected.source());
    }

    @Test
    void explicitProviderMustImplementADeclaredProtocol() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_JSON_1_0);
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class, () -> ProtocolSelector.select(
            service, TestProviders.of(ProtocolIds.AWS_JSON_1_1, false)));
        assertEquals(ProtocolSelectionException.Reason.NOT_DECLARED, e.reason());
        assertEquals(Optional.of(ProtocolIds.AWS_JSON_1_1), e.requested());
        assertEquals(List.of(ProtocolIds.AWS_JSON_1_1), e.installed());
    }

    @Test
    void runtimeValidationRunsOnTheSelectedProtocol() {
        ServiceProtocols service = declaring(ProtocolIds.REST_XML);
        ProtocolProvider strict = new TestProviders.Fixed(ProtocolIds.REST_XML, true) {
            @Override
            public void validate(ServiceProtocols s, ModeledProtocol declaration) {
                if (s.xmlNamespace().isEmpty()) {
                    throw ProtocolSelectionException.builder()
                                                    .reason(ProtocolSelectionException.Reason.UNSUPPORTED_BY_RUNTIME)
                                                    .serviceId(s.serviceId())
                                                    .message("restXml needs an XML namespace")
                                                    .build();
                }
            }
        };
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class,
                                                    () -> ProtocolSelector.select(service, InstalledProtocols.of(strict)));
        assertEquals(ProtocolSelectionException.Reason.UNSUPPORTED_BY_RUNTIME, e.reason());
    }

    @Test
    void selectionIsRepeatable() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_QUERY, ProtocolIds.AWS_JSON_1_0);
        InstalledProtocols installed = installing(ProtocolIds.AWS_JSON_1_0, ProtocolIds.AWS_QUERY);
        SelectedProtocol first = ProtocolSelector.select(service, installed);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, ProtocolSelector.select(service, installed));
        }
    }

    @Test
    void selectedProtocolRejectsMismatchedParts() {
        ServiceProtocols service = declaring(ProtocolIds.AWS_JSON_1_0);
        ModeledProtocol declared = service.protocol(ProtocolIds.AWS_JSON_1_0).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> new SelectedProtocol(
            service, declared, TestProviders.of(ProtocolIds.REST_XML, true), SelectedProtocol.Source.EXPLICIT_PROVIDER));
        assertThrows(IllegalArgumentException.class, () -> new SelectedProtocol(
            service, ModeledProtocol.of(ProtocolIds.AWS_JSON_1_0), TestProviders.of(ProtocolIds.AWS_JSON_1_0, false),
            SelectedProtocol.Source.EXPLICIT_PROVIDER));
    }
}
