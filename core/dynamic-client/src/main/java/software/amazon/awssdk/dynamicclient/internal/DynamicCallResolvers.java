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

import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointProviderUtils;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.endpoint.EndpointResolver;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeParams;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeResolver;
import software.amazon.awssdk.dynamicclient.DynamicEndpointParams;
import software.amazon.awssdk.dynamicclient.DynamicEndpointProvider;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * The auth scheme options resolver and endpoint resolver of a dynamic client call, the counterparts of a generated
 * client's {@code resolveAuthSchemeOptions} and {@code resolveEndpoint} methods.
 *
 * <p>Both read the operation from {@link SdkExecutionAttribute#API_OPERATION}, the instance the protocol serializes.
 * The request's override configuration can replace the client's auth scheme resolver or endpoint provider, as for a
 * generated client; the replacement must be a {@link DynamicAuthSchemeResolver} or {@link DynamicEndpointProvider}.
 *
 * <p>After the endpoint provider returns, the endpoint resolver does what a generated {@code resolveEndpoint} does with
 * the endpoint:
 * <ul>
 *     <li>adds the host prefix of the operation's {@code endpoint} trait, with the host labels of the request, unless
 *     host prefix injection is disabled;</li>
 *     <li>copies the signer properties of the endpoint's {@code authSchemes} entry that matches the selected auth scheme
 *     (signing name, signing region or region set, double URL encoding) to the selected auth scheme;</li>
 *     <li>adds the endpoint's metric values to the call's business metrics.</li>
 * </ul>
 * When the client endpoint is overridden, it also records the endpoint override business metric, as the generated
 * endpoint parameters do when they bind {@code SDK::Endpoint}.
 */
@SdkInternalApi
public final class DynamicCallResolvers {
    private static final AuthSchemeOptionsResolver AUTH_SCHEME_OPTIONS = DynamicCallResolvers::authSchemeOptions;
    private static final EndpointResolver ENDPOINT = DynamicCallResolvers::endpoint;

    private DynamicCallResolvers() {
    }

    /**
     * Returns the auth scheme options resolver of dynamic calls.
     *
     * @return the resolver
     */
    public static AuthSchemeOptionsResolver authSchemeOptionsResolver() {
        return AUTH_SCHEME_OPTIONS;
    }

    /**
     * Returns the endpoint resolver of dynamic calls.
     *
     * @return the resolver
     */
    public static EndpointResolver endpointResolver() {
        return ENDPOINT;
    }

    private static List<AuthSchemeOption> authSchemeOptions(SdkRequest request, ExecutionAttributes attributes) {
        AuthSchemeProvider provider =
            request.overrideConfiguration()
                   .flatMap(c -> c.authSchemeProvider())
                   .orElseGet(() -> attributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER));
        DynamicAuthSchemeResolver resolver = Validate.isInstanceOf(
            DynamicAuthSchemeResolver.class, provider,
            "The auth scheme provider of a dynamic client call must be a DynamicAuthSchemeResolver, but it is %s",
            provider == null ? null : provider.getClass().getName());
        ClientProtocol<?, ?> protocol = attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL);
        return resolver.resolveAuthScheme(DynamicAuthSchemeParams.builder()
                                                                 .protocolId(protocol.id())
                                                                 .operation(operation(attributes))
                                                                 .executionAttributes(attributes)
                                                                 .build());
    }

    /**
     * Waits for the endpoint, as a generated client's {@code resolveEndpoint} does: the pipeline's endpoint resolver
     * is synchronous. Then applies the host prefix, the endpoint's auth scheme signer properties and its metric
     * values.
     */
    private static Endpoint endpoint(SdkRequest request, ExecutionAttributes attributes) {
        EndpointProvider provider = attributes.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        DynamicEndpointProvider endpointProvider = Validate.isInstanceOf(
            DynamicEndpointProvider.class, provider,
            "The endpoint provider of a dynamic client call must be a DynamicEndpointProvider, but it is %s",
            provider == null ? null : provider.getClass().getName());
        if (AwsEndpointProviderUtils.endpointIsOverridden(attributes)) {
            addMetric(attributes, BusinessMetricFeatureId.ENDPOINT_OVERRIDE.value());
        }
        SerializableStruct input = (SerializableStruct) request;
        DynamicEndpointParams params = DynamicEndpointParams.builder()
                                                            .operation(operation(attributes))
                                                            .input(input)
                                                            .executionAttributes(attributes)
                                                            .build();
        Endpoint endpoint = CompletableFutureUtils.joinLikeSync(endpointProvider.resolveEndpoint(params));

        DynamicOperationMetadata metadata = attributes.getAttribute(DynamicOperationMetadata.METADATA);
        if (metadata != null && !AwsEndpointProviderUtils.disableHostPrefixInjection(attributes)) {
            String hostPrefix = metadata.resolvedHostPrefix(input);
            if (hostPrefix != null) {
                endpoint = AwsEndpointProviderUtils.addHostPrefix(endpoint, hostPrefix);
            }
        }

        List<EndpointAuthScheme> endpointAuthSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        SelectedAuthScheme<?> selectedAuthScheme = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (endpointAuthSchemes != null && selectedAuthScheme != null) {
            attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                    withEndpointSignerProperties(endpointAuthSchemes, selectedAuthScheme, attributes));
        }

        List<String> metricValues = endpoint.attribute(AwsEndpointAttribute.METRIC_VALUES);
        if (metricValues != null) {
            metricValues.forEach(v -> addMetric(attributes, v));
        }
        return endpoint;
    }

    /**
     * The selected auth scheme with the signer properties of the endpoint auth scheme that has its scheme ID, as the
     * generated {@code <Service>EndpointResolverUtils.authSchemeWithEndpointSignerProperties} of a service whose auth
     * schemes include sigv4a. A region set configured on the client ({@code AWS_SIGV4A_SIGNING_REGION_SET}) is kept;
     * otherwise the endpoint's region set replaces the one the auth scheme resolver derived from the region.
     *
     * @throws IllegalArgumentException if the matching endpoint auth scheme is neither sigv4 nor sigv4a
     */
    static <T extends Identity> SelectedAuthScheme<T> withEndpointSignerProperties(
        List<EndpointAuthScheme> endpointAuthSchemes, SelectedAuthScheme<T> selectedAuthScheme,
        ExecutionAttributes attributes) {
        for (EndpointAuthScheme endpointAuthScheme : endpointAuthSchemes) {
            if (!endpointAuthScheme.schemeId().equals(selectedAuthScheme.authSchemeOption().schemeId())) {
                continue;
            }
            AuthSchemeOption.Builder option = selectedAuthScheme.authSchemeOption().toBuilder();
            if (endpointAuthScheme instanceof SigV4AuthScheme) {
                SigV4AuthScheme v4 = (SigV4AuthScheme) endpointAuthScheme;
                if (v4.isDisableDoubleEncodingSet()) {
                    option.putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !v4.disableDoubleEncoding());
                }
                if (v4.signingRegion() != null) {
                    option.putSignerProperty(AwsV4HttpSigner.REGION_NAME, v4.signingRegion());
                }
                if (v4.signingName() != null) {
                    option.putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, v4.signingName());
                }
                return new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build());
            }
            if (endpointAuthScheme instanceof SigV4aAuthScheme) {
                SigV4aAuthScheme v4a = (SigV4aAuthScheme) endpointAuthScheme;
                if (v4a.isDisableDoubleEncodingSet()) {
                    option.putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, !v4a.disableDoubleEncoding());
                }
                Set<String> configuredRegionSet = attributes.getAttribute(AwsExecutionAttribute.AWS_SIGV4A_SIGNING_REGION_SET);
                if (CollectionUtils.isNullOrEmpty(configuredRegionSet)
                    && !CollectionUtils.isNullOrEmpty(v4a.signingRegionSet())) {
                    option.putSignerProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create(v4a.signingRegionSet()));
                }
                if (v4a.signingName() != null) {
                    option.putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, v4a.signingName());
                }
                return new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build());
            }
            throw new IllegalArgumentException("Endpoint auth scheme '" + endpointAuthScheme.name() + "' cannot be mapped "
                                               + "to the SDK auth scheme. Was it declared in the service's model?");
        }
        return selectedAuthScheme;
    }

    private static void addMetric(ExecutionAttributes attributes, String metric) {
        BusinessMetricCollection metrics = attributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        if (metrics != null) {
            metrics.addMetric(metric);
        }
    }

    private static ApiOperation<?, ?> operation(ExecutionAttributes attributes) {
        return attributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
    }
}
