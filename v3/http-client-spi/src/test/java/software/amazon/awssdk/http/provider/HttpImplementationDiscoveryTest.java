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

package software.amazon.awssdk.http.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceConfigurationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkSyncHttpClient;

/**
 * {@link HttpImplementationDiscovery} finds the {@code META-INF/services} registrations of both SPIs (this module's
 * test resources register {@link DiscoverableSyncService}) and reports a broken registration as a discovery failure.
 */
class HttpImplementationDiscoveryTest {
    @Test
    void discoveryFindsRegisteredImplementations() {
        InstalledHttpImplementations installed =
            HttpImplementationDiscovery.discover(HttpImplementationDiscoveryTest.class.getClassLoader());
        assertEquals(List.of("test-discoverable"), installed.names(HttpTransportMode.SYNC));
        assertEquals(List.of(), installed.names(HttpTransportMode.ASYNC));
        assertEquals("test-discoverable", installed.defaultSync().implementationName());
    }

    @Test
    void theDefaultResolverDiscoversWithTheSpiClassLoader() {
        assertEquals("test-discoverable", HttpClientResolver.sync().resolve().implementationName());
    }

    @Test
    void anUnloadableRegistrationIsADiscoveryFailure(@TempDir Path root) throws IOException {
        Path services = Files.createDirectories(root.resolve("META-INF/services"));
        Files.writeString(services.resolve(SdkAsyncHttpService.class.getName()), "com.example.Missing\n",
                          StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[] {root.toUri().toURL()},
                                                        HttpImplementationDiscoveryTest.class.getClassLoader())) {
            HttpImplementationSelectionException e = assertThrows(HttpImplementationSelectionException.class,
                                                                  () -> HttpImplementationDiscovery.discover(loader));
            assertEquals(HttpImplementationSelectionException.Reason.DISCOVERY_FAILED, e.reason());
            assertInstanceOf(ServiceConfigurationError.class, e.getCause());
        }
    }

    /** Registered in this module's test resources. */
    public static final class DiscoverableSyncService implements SdkHttpService {
        @Override
        public String implementationName() {
            return "test-discoverable";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientResolverTest.caps(HttpTransportMode.SYNC);
        }

        @Override
        public SdkSyncHttpClient create(HttpClientConstruction construction) {
            return new HttpClientResolverTest.FakeSyncClient(implementationName(), capabilities());
        }
    }
}
