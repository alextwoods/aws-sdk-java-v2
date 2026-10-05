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

package software.amazon.awssdk.awscore.override;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsClientConfiguration;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.model.shapes.ShapeId;

class AwsCallOptionTest {
    private static final IdentityProvider<AwsCredentialsIdentity> CLIENT = provider("CLIENT-SECRET");
    private static final IdentityProvider<AwsCredentialsIdentity> CALL = provider("CALL-SECRET");

    private static IdentityProvider<AwsCredentialsIdentity> provider(String secret) {
        AwsCredentialsIdentity identity = AwsCredentialsIdentity.create("AKID", secret);
        return new IdentityProvider<>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(identity);
            }
        };
    }

    private static SdkClientConfiguration base() {
        return SdkClientConfiguration.builder()
                                     .option(AwsClientOption.AWS_REGION, Region.US_WEST_2)
                                     .option(SdkClientOption.CLIENT_ENDPOINT,
                                             URI.create("https://example.test"))
                                     .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, CLIENT)
                                     .option(SdkClientOption.PROTOCOL_ID, ShapeId.from("aws.protocols#restJson1"))
                                     .build();
    }

    @Test
    void perCallCredentialsReplaceTheClientProviderForThatCallOnly() {
        SdkClientConfiguration base = base();
        CallOptions client = CallOptions.of(base);
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder()
                                                                                  .credentialsProvider(CALL)
                                                                                  .build();
        assertEquals(Map.of(AwsCallOption.CREDENTIALS_IDENTITY_PROVIDER, CALL), override.callOptions());

        CallOptions call = client.withOverrides(null, override);
        assertSame(CALL, call.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).orElseThrow());
        assertSame(CLIENT, client.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).orElseThrow());
        assertSame(CLIENT, AwsClientConfiguration.create(base).credentialsProvider());
        assertEquals(Optional.of(Region.US_WEST_2), call.get(AwsClientOption.AWS_REGION));
        assertFalse(call.toString().contains("SECRET"), call.toString());
        assertFalse(override.toString().contains("SECRET"), override.toString());
    }

    @Test
    void consumerOverloadAndProtocolAgnosticOverridesLayerTheSameWay() {
        // What a generated getItem(request, Consumer<AwsRequestOverrideConfiguration.Builder>) overload builds.
        Consumer<AwsRequestOverrideConfiguration.Builder> mutation = o -> o.credentialsProvider(CALL)
                                                                           .apiCallTimeout(Duration.ofSeconds(3));
        AwsRequestOverrideConfiguration explicit = AwsRequestOverrideConfiguration.builder()
                                                                                  .applyMutation(mutation)
                                                                                  .build();
        SdkRequestOverrideConfiguration embedded = SdkRequestOverrideConfiguration.builder()
                                                                                  .apiCallTimeout(Duration.ofSeconds(9))
                                                                                  .apiCallAttemptTimeout(
                                                                                      Duration.ofSeconds(1))
                                                                                  .build();
        CallOptions call = CallOptions.of(base()).withOverrides(embedded, explicit);
        assertSame(CALL, call.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).orElseThrow());
        assertEquals(Optional.of(Duration.ofSeconds(3)), call.timeouts().apiCallTimeout());
        assertEquals(Optional.of(Duration.ofSeconds(1)), call.timeouts().apiCallAttemptTimeout());

        AwsRequestOverrideConfiguration converted = AwsRequestOverrideConfiguration.from(embedded);
        assertEquals(embedded.callOptions(), converted.callOptions());
        assertEquals(Optional.empty(), converted.credentialsProvider());
    }
}
