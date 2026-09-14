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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.http.NoopTestAwsRequest;
import software.amazon.awssdk.awscore.internal.identity.AwsRequestIdentityProviderResolver;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributesTemplate;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.AttributeMap.LazyValueSource;

/**
 * The client-constant execution attributes are seeded from a per-client template; these pin that the template is what
 * gets used, that it reads back exactly as the per-attribute puts did, and that request-level overrides still layer
 * onto it.
 */
class ClientConstantExecutionAttributesTest {
    private static final EndpointProvider CLIENT_ENDPOINT_PROVIDER = mock(EndpointProvider.class);
    private static final AuthSchemeProvider CLIENT_AUTH_SCHEME_PROVIDER = mock(AuthSchemeProvider.class);
    private static final IdentityProviders CLIENT_IDENTITY_PROVIDERS =
        IdentityProviders.builder()
                         .putIdentityProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("ak", "sk")))
                         .build();

    private static SdkClientConfiguration.Builder baseConfig() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.emptyList())
                                     .option(SdkClientOption.SERVICE_NAME, "svc")
                                     .option(AwsClientOption.AWS_REGION, Region.EU_WEST_2)
                                     .option(AwsClientOption.SERVICE_SIGNING_NAME, "svc")
                                     .option(AwsClientOption.SIGNING_REGION, Region.EU_WEST_2)
                                     .option(AwsClientOption.FIPS_ENDPOINT_ENABLED, true)
                                     .option(SdkClientOption.ENDPOINT_PROVIDER, CLIENT_ENDPOINT_PROVIDER)
                                     .option(SdkClientOption.AUTH_SCHEME_PROVIDER, CLIENT_AUTH_SCHEME_PROVIDER)
                                     .option(SdkClientOption.AUTH_SCHEMES, Collections.emptyMap())
                                     .option(SdkClientOption.IDENTITY_PROVIDERS, CLIENT_IDENTITY_PROVIDERS)
                                     .option(SdkClientOption.EXECUTION_ATTRIBUTES, new ExecutionAttributes());
    }

    private static ClientExecutionParams<SdkRequest, SdkResponse> params(AwsRequest request) {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(request)
            .withFullDuplex(false)
            .withOperationName("TestOperation");
    }

    private static ExecutionAttributes attributesFor(SdkClientConfiguration config, AwsRequest request) {
        return AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(params(request), config)
                                         .executionAttributes();
    }

    @Test
    void templateRegisteredAsLazyOption_isWhatSeedsTheAttributes() {
        SdkClientConfiguration config = baseConfig()
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();
        ExecutionAttributesTemplate template = config.option(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES);
        assertThat(template).isNotNull();

        ExecutionAttributes attrs = attributesFor(config, NoopTestAwsRequest.builder().build());
        assertThat(attrs.getAttribute(AwsExecutionAttribute.AWS_REGION)).isEqualTo(Region.EU_WEST_2);
        assertThat(attrs.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("svc");
        assertThat(attrs.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED)).isTrue();
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER)).isSameAs(CLIENT_ENDPOINT_PROVIDER);
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER))
            .isSameAs(CLIENT_AUTH_SCHEME_PROVIDER);
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS)).isSameAs(CLIENT_IDENTITY_PROVIDERS);
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_RESOLVER))
            .isSameAs(AwsRequestIdentityProviderResolver.create());
    }

    @Test
    void templateOptionPresent_isConsulted_ratherThanRederived() {
        // A template built from a different configuration: if it is used, its values show up.
        SdkClientConfiguration other = baseConfig().option(SdkClientOption.SERVICE_NAME, "from-template").build();
        ExecutionAttributesTemplate template = AwsExecutionContextBuilder.clientConstantExecutionAttributes(
            new LazyValueSource() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(AttributeMap.Key<T> sourceKey) {
                    return other.option((ClientOption<T>) sourceKey);
                }
            });
        SdkClientConfiguration config = baseConfig()
            .option(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES, template)
            .build();

        ExecutionAttributes attrs = attributesFor(config, NoopTestAwsRequest.builder().build());
        assertThat(attrs.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("from-template");
    }

    @Test
    void noTemplateOption_derivesTheSameAttributesOnTheSpot() {
        SdkClientConfiguration withTemplate = baseConfig()
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();
        SdkClientConfiguration without = baseConfig().build();

        ExecutionAttributes a = attributesFor(withTemplate, NoopTestAwsRequest.builder().build());
        ExecutionAttributes b = attributesFor(without, NoopTestAwsRequest.builder().build());

        // Everything but the per-call collections (business metrics, user-agent metadata) must be identical.
        for (ExecutionAttributes attrs : new ExecutionAttributes[] {a, b}) {
            attrs.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, null);
            attrs.putAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA, null);
            attrs.putAttribute(SdkInternalExecutionAttribute.SIGNING_METHOD_UPDATER, null);
            attrs.putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR, null);
            // The legacy signing-name/region writes build a fresh SelectedAuthScheme (no equals) per call.
            attrs.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
            attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_PRE_INTERCEPTORS, null);
        }
        assertThat(a).isEqualTo(b);
    }

    @Test
    void templateWins_overClientAndRequestExecutionAttributeOverrides_asThePutsDid() {
        ExecutionAttributes clientOverrides = new ExecutionAttributes()
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, "client-override");
        AwsRequest request = NoopTestAwsRequest.builder()
            .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                .putExecutionAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1)
                .build())
            .build();
        SdkClientConfiguration config = baseConfig()
            .option(SdkClientOption.EXECUTION_ATTRIBUTES, clientOverrides)
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();

        ExecutionAttributes attrs = attributesFor(config, request);
        assertThat(attrs.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("svc");
        assertThat(attrs.getAttribute(AwsExecutionAttribute.AWS_REGION)).isEqualTo(Region.EU_WEST_2);
    }

    @Test
    void requestLevelEndpointAndAuthSchemeProviders_overrideTheClientValues() {
        EndpointProvider requestEndpointProvider = mock(EndpointProvider.class);
        AuthSchemeProvider requestAuthSchemeProvider = mock(AuthSchemeProvider.class);
        AwsRequest request = NoopTestAwsRequest.builder()
            .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                .endpointProvider(requestEndpointProvider)
                .authSchemeProvider(requestAuthSchemeProvider)
                .build())
            .build();
        SdkClientConfiguration config = baseConfig()
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();

        ExecutionAttributes attrs = attributesFor(config, request);
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER)).isSameAs(requestEndpointProvider);
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER)).isSameAs(requestAuthSchemeProvider);
        // Untouched by that override: the client's identity providers, same instance.
        assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS)).isSameAs(CLIENT_IDENTITY_PROVIDERS);
    }

    @Test
    void requestLevelCredentialsProvider_isAddedToACopyOfTheClientIdentityProviders() {
        StaticCredentialsProvider requestCredentials =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("request-ak", "request-sk"));
        AwsRequest request = NoopTestAwsRequest.builder()
            .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                .credentialsProvider(requestCredentials)
                .build())
            .build();
        SdkClientConfiguration config = baseConfig()
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();

        IdentityProviders resolved = attributesFor(config, request)
            .getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);
        assertThat(resolved).isNotSameAs(CLIENT_IDENTITY_PROVIDERS);
        assertThat(resolved.identityProvider(AwsCredentialsIdentity.class)).isSameAs(requestCredentials);
        assertThat(CLIENT_IDENTITY_PROVIDERS.identityProvider(AwsCredentialsIdentity.class))
            .as("the client's providers are not mutated").isNotSameAs(requestCredentials);
    }

    @Test
    void lazyTemplate_isRecomputedWhenAnOptionItDependsOnChanges() {
        SdkClientConfiguration config = baseConfig()
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();
        ExecutionAttributesTemplate before = config.option(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES);

        // What a request-level plugin does: copy the configuration and change an option.
        SdkClientConfiguration changed = config.toBuilder().option(AwsClientOption.AWS_REGION, Region.US_WEST_2).build();
        ExecutionAttributesTemplate after = changed.option(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES);

        assertThat(after).isNotSameAs(before);
        assertThat(attributesFor(changed, NoopTestAwsRequest.builder().build())
                       .getAttribute(AwsExecutionAttribute.AWS_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(config.option(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES))
            .as("the original configuration keeps its template").isSameAs(before);
    }

    @Test
    void lazyTemplate_seesAnOptionAddedAfterItWasFirstResolved() {
        // The generated clients add SDK_CLIENT (and API_METADATA) to the configuration the builder produced; a template
        // resolved before that must be recomputed to carry it.
        SdkClientConfiguration built = baseConfig()
            .lazyOption(AwsInternalClientOption.CLIENT_EXECUTION_ATTRIBUTES,
                        AwsExecutionContextBuilder::clientConstantExecutionAttributes)
            .build();
        assertThat(attributesFor(built, NoopTestAwsRequest.builder().build())
                       .getAttribute(SdkInternalExecutionAttribute.SDK_CLIENT)).isNull();

        SdkClient sdkClient = mock(SdkClient.class);
        SdkClientConfiguration withClient = built.toBuilder().option(SdkClientOption.SDK_CLIENT, sdkClient).build();
        assertThat(attributesFor(withClient, NoopTestAwsRequest.builder().build())
                       .getAttribute(SdkInternalExecutionAttribute.SDK_CLIENT)).isSameAs(sdkClient);
    }
}
