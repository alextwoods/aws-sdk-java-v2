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

package software.amazon.awssdk.core.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.resource.ManagedResource;

class SdkClientConfigurationTest {
    private static final ClientOption<String> SECRET = ClientOption.create("Secret", String.class);
    private static final URI ENDPOINT = URI.create("https://example.test");

    @Test
    void valuesAreReadBackByTypedKey() {
        TimeoutConfiguration timeouts = TimeoutConfiguration.builder().apiCallTimeout(Duration.ofSeconds(5)).build();
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT)
                                                                     .option(SdkClientOption.TIMEOUT_CONFIGURATION,
                                                                             timeouts)
                                                                     .build();
        assertEquals(Optional.of(ENDPOINT), configuration.get(SdkClientOption.CLIENT_ENDPOINT));
        assertEquals(Optional.of(timeouts), configuration.get(SdkClientOption.TIMEOUT_CONFIGURATION));
        assertEquals(Optional.empty(), configuration.get(SdkClientOption.PROTOCOL_ID));
        assertTrue(configuration.contains(SdkClientOption.CLIENT_ENDPOINT));
        assertFalse(configuration.contains(SdkClientOption.PROTOCOL_ID));
        assertEquals(List.of(SdkClientOption.CLIENT_ENDPOINT, SdkClientOption.TIMEOUT_CONFIGURATION),
                     List.copyOf(configuration.options()));
    }

    @Test
    void keysAreComparedByIdentityNotName() {
        ClientOption<String> sameName = ClientOption.create("Secret", String.class);
        SdkClientConfiguration configuration = SdkClientConfiguration.builder().option(SECRET, "s").build();
        assertEquals(Optional.empty(), configuration.get(sameName));
    }

    @Test
    void builtConfigurationIsImmutableAndIndependentOfItsBuilder() {
        AutoCloseable closeable = () -> { };
        SdkClientConfiguration.Builder builder = SdkClientConfiguration.builder()
                                                                       .option(SECRET, "s")
                                                                       .addResource(ManagedResource.sdkOwned(closeable));
        SdkClientConfiguration configuration = builder.build();
        builder.option(SECRET, null).addResource(ManagedResource.callerOwned("other"));
        assertEquals(Optional.of("s"), configuration.get(SECRET));
        assertEquals(List.of(ManagedResource.sdkOwned(closeable)), configuration.resources());
        assertThrows(UnsupportedOperationException.class, () -> configuration.resources().clear());
        assertThrows(UnsupportedOperationException.class, () -> configuration.options().clear());
        assertThrows(UnsupportedOperationException.class, () -> builder.resources().clear());
    }

    @Test
    void builderRoundTripsAndEquality() {
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(SECRET, "s")
                                                                     .resources(List.of(ManagedResource.callerOwned("r")))
                                                                     .build();
        SdkClientConfiguration copy = configuration.toBuilder().build();
        assertEquals(configuration, copy);
        assertEquals(configuration.hashCode(), copy.hashCode());
        assertEquals("s", configuration.toBuilder().option(SECRET));
        assertNotEquals(configuration, configuration.copy(b -> b.option(SECRET, "t")));
        assertNull(configuration.toBuilder().option(SECRET, null).option(SECRET));
        assertSame(SdkClientConfiguration.empty(), SdkClientConfiguration.builder().build());
        assertSame(SdkClientConfiguration.empty(),
                   configuration.toBuilder().option(SECRET, null).resources(List.of()).build());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wronglyTypedValuesAreRejectedWithTheOptionName() {
        ClientOption raw = SECRET;
        ClassCastException e = assertThrows(ClassCastException.class,
                                            () -> SdkClientConfiguration.builder().option(raw, 42));
        assertEquals("ClientOption(Secret: String) requires a java.lang.String value, but was given a "
                     + "java.lang.Integer.", e.getMessage());
    }

    @Test
    void toStringRendersOptionNamesButNotValues() {
        String rendered = SdkClientConfiguration.builder().option(SECRET, "hunter2").build().toString();
        assertTrue(rendered.contains("Secret"), rendered);
        assertFalse(rendered.contains("hunter2"), rendered);
    }
}
