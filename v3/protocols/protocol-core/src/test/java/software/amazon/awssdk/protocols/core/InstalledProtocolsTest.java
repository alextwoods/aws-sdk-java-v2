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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceConfigurationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

class InstalledProtocolsTest {
    private static final String SERVICES = "META-INF/services/" + ProtocolProvider.class.getName();

    @Test
    void discoveryFindsRegisteredProviders() {
        InstalledProtocols installed = InstalledProtocols.discover(getClass().getClassLoader());
        assertEquals(List.of(ProtocolIds.AWS_JSON_1_0, ProtocolIds.REST_XML), installed.protocolIds());
        assertInstanceOf(TestProviders.RestXml.class, installed.provider(ProtocolIds.REST_XML).orElseThrow());
        assertInstanceOf(TestProviders.AwsJson10.class, installed.provider(ProtocolIds.AWS_JSON_1_0).orElseThrow());
        assertTrue(installed.provider(ProtocolIds.EC2_QUERY).isEmpty());
    }

    @Test
    void discoveredProvidersFeedSelection() {
        ServiceProtocols service = ServiceProtocols.builder(
            Schema.createService(ShapeId.from("com.example#S")))
                                                   .protocol(ModeledProtocol.of(ProtocolIds.REST_XML))
                                                   .build();
        SelectedProtocol selected = ProtocolSelector.select(service,
                                                            InstalledProtocols.discover(getClass().getClassLoader()));
        assertInstanceOf(TestProviders.RestXml.class, selected.provider());
    }

    @Test
    void twoProvidersOfOneProtocolAreRejectedNamingBoth(@TempDir Path dir) throws IOException {
        try (URLClassLoader loader = withServices(dir, TestProviders.DuplicateRestXml.class.getName())) {
            ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class,
                                                        () -> InstalledProtocols.discover(loader));
            assertEquals(ProtocolSelectionException.Reason.DUPLICATE_PROVIDER, e.reason());
            assertEquals(List.of(TestProviders.DuplicateRestXml.class.getName(), TestProviders.RestXml.class.getName()),
                         e.providers());
            assertTrue(e.getMessage().contains(ProtocolIds.REST_XML.toString()), e.getMessage());
        }
    }

    @Test
    void explicitDuplicatesAreRejected() {
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class, () -> InstalledProtocols.of(
            new TestProviders.RestXml(), new TestProviders.DuplicateRestXml()));
        assertEquals(ProtocolSelectionException.Reason.DUPLICATE_PROVIDER, e.reason());
    }

    @Test
    void unloadableProviderFailsDiscovery(@TempDir Path dir) throws IOException {
        try (URLClassLoader loader = withServices(dir, "com.example.MissingProvider")) {
            ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class,
                                                        () -> InstalledProtocols.discover(loader));
            assertEquals(ProtocolSelectionException.Reason.DISCOVERY_FAILED, e.reason());
            assertInstanceOf(ServiceConfigurationError.class, e.getCause());
        }
    }

    @Test
    void noneIsEmpty() {
        assertEquals(List.of(), InstalledProtocols.none().protocolIds());
    }

    private URLClassLoader withServices(Path dir, String provider) throws IOException {
        Path file = dir.resolve(SERVICES);
        Files.createDirectories(file.getParent());
        Files.writeString(file, provider + "\n");
        return new URLClassLoader(new URL[] {dir.toUri().toURL()}, getClass().getClassLoader());
    }
}
