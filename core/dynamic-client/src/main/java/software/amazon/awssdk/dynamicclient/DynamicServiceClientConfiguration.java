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

package software.amazon.awssdk.dynamicclient;

import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceClientConfigurationBuilder;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;

/**
 * The settings of a {@link DynamicClient}, as seen by the user and by {@code SdkPlugin}s. The counterpart of a generated
 * client's {@code <Service>ServiceClientConfiguration}, with {@link #authSchemeResolver()} in place of its
 * {@code authSchemeProvider()}.
 */
@SdkPublicApi
public final class DynamicServiceClientConfiguration extends AwsServiceClientConfiguration {
    private final DynamicAuthSchemeResolver authSchemeResolver;

    /**
     * Creates the configuration from a builder.
     *
     * @param builder the builder
     */
    public DynamicServiceClientConfiguration(Builder builder) {
        super(builder);
        this.authSchemeResolver = builder.authSchemeResolver();
    }

    /**
     * Creates a builder that is not attached to a client.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new DynamicServiceClientConfigurationBuilder();
    }

    /**
     * Returns the configured auth scheme resolver.
     *
     * @return the auth scheme resolver, or null when the client uses the auth schemes of its model
     */
    public DynamicAuthSchemeResolver authSchemeResolver() {
        return authSchemeResolver;
    }

    /**
     * The builder of {@link DynamicServiceClientConfiguration}. The builder that a client passes to
     * {@code SdkPlugin.configureClient} writes through to the client's configuration.
     */
    public interface Builder extends AwsServiceClientConfiguration.Builder {
        @Override
        Builder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration);

        @Override
        ClientOverrideConfiguration overrideConfiguration();

        @Override
        Builder endpointOverride(URI endpointOverride);

        @Override
        URI endpointOverride();

        @Override
        Builder endpointProvider(EndpointProvider endpointProvider);

        @Override
        EndpointProvider endpointProvider();

        @Override
        Builder region(Region region);

        @Override
        Region region();

        @Override
        Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider);

        @Override
        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider();

        @Override
        Builder putAuthScheme(AuthScheme<?> authScheme);

        @Override
        Map<String, AuthScheme<?>> authSchemes();

        /**
         * Sets the auth scheme resolver.
         *
         * @param authSchemeResolver the resolver, or null to use the auth schemes of the model
         * @return this builder
         */
        Builder authSchemeResolver(DynamicAuthSchemeResolver authSchemeResolver);

        /**
         * Returns the auth scheme resolver.
         *
         * @return the resolver, or null when none is set
         */
        DynamicAuthSchemeResolver authSchemeResolver();

        @Override
        DynamicServiceClientConfiguration build();
    }
}
