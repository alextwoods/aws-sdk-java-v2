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

package software.amazon.awssdk.awscore;

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
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.model.shapes.ShapeId;

class AwsConfigurationTest {
    private static final ContextKey<String> TRACE = ContextKey.execution("Trace", String.class);
    private static final ClientOption<Boolean> PATH_STYLE = ClientOption.create("PathStyle", Boolean.class);
    private static final ShapeId REST_XML = ShapeId.from("aws.protocols#restXml");
    private static final URI ENDPOINT = URI.create("https://s3.us-west-2.amazonaws.com");

    private static <T extends Identity> IdentityProvider<T> provider(Class<T> type, T identity) {
        return new IdentityProvider<>() {
            @Override
            public Class<T> identityType() {
                return type;
            }

            @Override
            public CompletableFuture<T> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(identity);
            }
        };
    }

    private static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS =
        provider(AwsCredentialsIdentity.class, AwsCredentialsIdentity.create("AKID", "SECRET"));

    private static SdkClientConfiguration.Builder complete() {
        return SdkClientConfiguration.builder()
                                     .option(AwsClientOption.AWS_REGION, Region.US_WEST_2)
                                     .option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT)
                                     .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, CREDENTIALS)
                                     .option(SdkClientOption.PROTOCOL_ID, REST_XML);
    }

    @Test
    void overrideCarriesInheritedDeltasAndCredentialsProvider() {
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder()
                                                                                  .putHeader("x-example", "value")
                                                                                  .apiCallAttemptTimeout(
                                                                                      Duration.ofSeconds(2))
                                                                                  .putExecutionAttribute(TRACE, "t1")
                                                                                  .credentialsProvider(CREDENTIALS)
                                                                                  .build();
        assertEquals(List.of("value"), override.headers().get("X-EXAMPLE"));
        assertEquals(Optional.of(Duration.ofSeconds(2)), override.apiCallAttemptTimeout());
        assertEquals(Optional.of("t1"), override.executionAttribute(TRACE));
        assertSame(CREDENTIALS, override.credentialsProvider().orElseThrow());
        assertSame(CREDENTIALS, override.credentialsIdentityProvider().orElseThrow());

        AwsRequestOverrideConfiguration copy = override.toBuilder().build();
        assertEquals(override, copy);
        assertEquals(override.hashCode(), copy.hashCode());
        assertNotEquals(override, override.toBuilder().credentialsProvider(null).build());
        assertEquals(Optional.empty(), AwsRequestOverrideConfiguration.builder().build().credentialsProvider());
        assertSame(CREDENTIALS, override.toBuilder().credentialsProvider());
        assertFalse(override.toString().contains("SECRET"), override.toString());
    }

    @Test
    void fromConvertsProtocolAgnosticOverrides() {
        assertNull(AwsRequestOverrideConfiguration.from(null));
        AwsRequestOverrideConfiguration aws = AwsRequestOverrideConfiguration.builder().build();
        assertSame(aws, AwsRequestOverrideConfiguration.from(aws));
        SdkRequestOverrideConfiguration sdk = SdkRequestOverrideConfiguration.builder()
                                                                             .putRawQueryParameter("q", "1")
                                                                             .apiCallTimeout(Duration.ofSeconds(9))
                                                                             .build();
        AwsRequestOverrideConfiguration converted = AwsRequestOverrideConfiguration.from(sdk);
        assertEquals(sdk.rawQueryParameters(), converted.rawQueryParameters());
        assertEquals(sdk.timeouts(), converted.timeouts());
        assertEquals(Optional.empty(), converted.credentialsProvider());
        assertNotEquals(sdk, converted, "different override types are never equal");
    }

    @Test
    void clientConfigurationExposesTypedViewOfResolvedOptions() {
        AutoCloseable http = () -> { };
        TimeoutConfiguration timeouts = TimeoutConfiguration.builder().apiCallTimeout(Duration.ofSeconds(30)).build();
        SdkClientConfiguration resolved = complete().option(SdkClientOption.TIMEOUT_CONFIGURATION, timeouts)
                                                    .option(PATH_STYLE, true)
                                                    .addResource(ManagedResource.sdkOwned(http))
                                                    .build();
        AwsClientConfiguration configuration = AwsClientConfiguration.create(resolved);
        assertSame(Region.US_WEST_2, configuration.region());
        assertEquals(ENDPOINT, configuration.clientEndpoint());
        assertSame(CREDENTIALS, configuration.credentialsProvider());
        assertEquals(timeouts, configuration.timeouts());
        assertEquals(REST_XML, configuration.protocolId());
        assertEquals(List.of(ManagedResource.sdkOwned(http)), configuration.resources());
        assertEquals(Optional.of(true), configuration.get(PATH_STYLE));
        assertEquals(configuration, AwsClientConfiguration.create(resolved));
        assertEquals(configuration.hashCode(), AwsClientConfiguration.create(resolved).hashCode());
        assertFalse(configuration.toString().contains("SECRET"), configuration.toString());

        assertEquals(TimeoutConfiguration.none(), AwsClientConfiguration.create(complete().build()).timeouts());
    }

    @Test
    void missingRequiredOptionsAreNamedInTheFailure() {
        SdkClientConfigurationException e = assertThrows(
            SdkClientConfigurationException.class,
            () -> AwsClientConfiguration.create(complete().option(AwsClientOption.AWS_REGION, null).build()));
        assertEquals(Optional.of("AwsRegion"), e.option());
        assertEquals("The resolved client configuration has no value for the required option AwsRegion.",
                     e.getMessage());
        e = assertThrows(SdkClientConfigurationException.class,
                         () -> AwsClientConfiguration.create(
                             complete().option(SdkClientOption.PROTOCOL_ID, null).build()));
        assertEquals(Optional.of("ProtocolId"), e.option());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void credentialsProviderMustResolveAwsCredentials() {
        IdentityProvider<Identity> other = provider(Identity.class, new Identity() { });
        IdentityProvider raw = other;
        SdkClientConfigurationException e = assertThrows(
            SdkClientConfigurationException.class,
            () -> AwsClientConfiguration.create(
                complete().option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, raw).build()));
        assertEquals(Optional.of("CredentialsIdentityProvider"), e.option());
        assertTrue(e.getMessage().startsWith("The credentials provider must resolve AwsCredentialsIdentity"),
                   e.getMessage());
    }
}
