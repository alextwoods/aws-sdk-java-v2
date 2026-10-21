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

package software.amazon.awssdk.dynamicclient.internal;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeResolver;
import software.amazon.awssdk.dynamicclient.DynamicServiceClientConfiguration;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link DynamicServiceClientConfiguration.Builder} that reads and writes an {@link SdkClientConfiguration.Builder},
 * so that the changes a plugin makes reach the client's configuration. A copy of the generated
 * {@code <Service>ServiceClientConfigurationBuilder}.
 */
@SdkInternalApi
public final class DynamicServiceClientConfigurationBuilder implements DynamicServiceClientConfiguration.Builder {
    private final SdkClientConfiguration.Builder config;

    public DynamicServiceClientConfigurationBuilder() {
        this(SdkClientConfiguration.builder());
    }

    public DynamicServiceClientConfigurationBuilder(SdkClientConfiguration.Builder config) {
        this.config = Validate.paramNotNull(config, "config");
    }

    @Override
    public DynamicServiceClientConfiguration.Builder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        config.putAll(overrideConfiguration);
        return this;
    }

    @Override
    public ClientOverrideConfiguration overrideConfiguration() {
        return config.asOverrideConfigurationBuilder().build();
    }

    @Override
    public DynamicServiceClientConfiguration.Builder endpointOverride(URI endpointOverride) {
        if (endpointOverride != null) {
            config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, ClientEndpointProvider.forEndpointOverride(endpointOverride));
        } else {
            config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, null);
        }
        return this;
    }

    @Override
    public URI endpointOverride() {
        ClientEndpointProvider clientEndpoint = config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (clientEndpoint != null && clientEndpoint.isEndpointOverridden()) {
            return clientEndpoint.clientEndpoint();
        }
        return null;
    }

    @Override
    public DynamicServiceClientConfiguration.Builder endpointProvider(EndpointProvider endpointProvider) {
        config.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public EndpointProvider endpointProvider() {
        return config.option(SdkClientOption.ENDPOINT_PROVIDER);
    }

    @Override
    public DynamicServiceClientConfiguration.Builder region(Region region) {
        config.option(AwsClientOption.AWS_REGION, region);
        return this;
    }

    @Override
    public Region region() {
        return config.option(AwsClientOption.AWS_REGION);
    }

    @Override
    public DynamicServiceClientConfiguration.Builder credentialsProvider(
        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        config.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, credentialsProvider);
        return this;
    }

    @Override
    public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
        return config.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
    }

    @Override
    public DynamicServiceClientConfiguration.Builder putAuthScheme(AuthScheme<?> authScheme) {
        config.computeOptionIfAbsent(SdkClientOption.AUTH_SCHEMES, HashMap::new).put(authScheme.schemeId(), authScheme);
        return this;
    }

    @Override
    public Map<String, AuthScheme<?>> authSchemes() {
        Map<String, AuthScheme<?>> authSchemes = config.option(SdkClientOption.AUTH_SCHEMES);
        return Collections.unmodifiableMap(authSchemes == null ? Collections.emptyMap() : authSchemes);
    }

    @Override
    public DynamicServiceClientConfiguration.Builder authSchemeResolver(DynamicAuthSchemeResolver authSchemeResolver) {
        config.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeResolver);
        return this;
    }

    @Override
    public DynamicAuthSchemeResolver authSchemeResolver() {
        AuthSchemeProvider result = config.option(SdkClientOption.AUTH_SCHEME_PROVIDER);
        if (result == null) {
            return null;
        }
        return Validate.isInstanceOf(DynamicAuthSchemeResolver.class, result,
                                     "Expected an instance of " + DynamicAuthSchemeResolver.class.getSimpleName());
    }

    @Override
    public DynamicServiceClientConfiguration build() {
        return new DynamicServiceClientConfiguration(this);
    }
}
