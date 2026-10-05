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

package software.amazon.awssdk.awscore.internal;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.awscore.AwsClientConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.ToString;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The {@link AwsClientConfiguration} view of an {@link SdkClientConfiguration}. The typed values are read and checked
 * once, at construction. Not a public API: use {@link AwsClientConfiguration#create(SdkClientConfiguration)}.
 */
public final class DefaultAwsClientConfiguration implements AwsClientConfiguration {
    private final SdkClientConfiguration configuration;
    private final Region region;
    private final URI clientEndpoint;
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private final TimeoutConfiguration timeouts;
    private final ShapeId protocolId;

    private DefaultAwsClientConfiguration(SdkClientConfiguration configuration) {
        this.configuration = configuration;
        this.region = require(configuration, AwsClientOption.AWS_REGION);
        this.clientEndpoint = require(configuration, SdkClientOption.CLIENT_ENDPOINT);
        this.credentialsProvider = require(configuration, AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
        this.timeouts = configuration.get(SdkClientOption.TIMEOUT_CONFIGURATION).orElse(TimeoutConfiguration.none());
        this.protocolId = require(configuration, SdkClientOption.PROTOCOL_ID);
        Class<?> identityType = credentialsProvider.identityType();
        if (identityType == null || !AwsCredentialsIdentity.class.isAssignableFrom(identityType)) {
            throw SdkClientConfigurationException.builder()
                                                 .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER.name())
                                                 .message("The credentials provider must resolve "
                                                          + AwsCredentialsIdentity.class.getSimpleName()
                                                          + " identities, but resolves " + identityType + ".")
                                                 .build();
        }
    }

    public static AwsClientConfiguration create(SdkClientConfiguration configuration) {
        return new DefaultAwsClientConfiguration(Objects.requireNonNull(configuration, "configuration"));
    }

    private static <T> T require(SdkClientConfiguration configuration, ClientOption<T> option) {
        return configuration.get(option).orElseThrow(
            () -> SdkClientConfigurationException.builder()
                                                 .option(option.name())
                                                 .message("The resolved client configuration has no value for the "
                                                          + "required option " + option.name() + ".")
                                                 .build());
    }

    @Override
    public Region region() {
        return region;
    }

    @Override
    public URI clientEndpoint() {
        return clientEndpoint;
    }

    @Override
    public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
        return credentialsProvider;
    }

    @Override
    public TimeoutConfiguration timeouts() {
        return timeouts;
    }

    @Override
    public ShapeId protocolId() {
        return protocolId;
    }

    @Override
    public List<ManagedResource<?>> resources() {
        return configuration.resources();
    }

    @Override
    public <T> Optional<T> get(ClientOption<T> option) {
        return configuration.get(option);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultAwsClientConfiguration that && configuration.equals(that.configuration);
    }

    @Override
    public int hashCode() {
        return configuration.hashCode();
    }

    /** Renders non-secret settings only; the credentials provider is shown by type. */
    @Override
    public String toString() {
        return ToString.builder("AwsClientConfiguration")
                       .add("region", region)
                       .add("clientEndpoint", clientEndpoint)
                       .add("credentialsProvider", credentialsProvider.getClass().getSimpleName())
                       .add("timeouts", timeouts)
                       .add("protocolId", protocolId)
                       .add("options", configuration.options().size())
                       .build();
    }
}
