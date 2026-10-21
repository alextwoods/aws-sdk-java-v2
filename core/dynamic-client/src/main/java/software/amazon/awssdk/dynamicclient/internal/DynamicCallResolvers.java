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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.endpoint.EndpointResolver;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeParams;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeResolver;
import software.amazon.awssdk.dynamicclient.DynamicEndpointParams;
import software.amazon.awssdk.dynamicclient.DynamicEndpointProvider;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
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
 * <p>Not done here yet, unlike a generated client's {@code resolveEndpoint}: the host prefix of the operation's
 * {@code endpoint} trait, the signer properties of the endpoint's {@code authSchemes}, and the endpoint's metric
 * values. They are part of running dynamic calls through the shared pipeline features (Requirement 30.7).
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
     * is synchronous.
     */
    private static Endpoint endpoint(SdkRequest request, ExecutionAttributes attributes) {
        EndpointProvider provider = attributes.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        DynamicEndpointProvider endpointProvider = Validate.isInstanceOf(
            DynamicEndpointProvider.class, provider,
            "The endpoint provider of a dynamic client call must be a DynamicEndpointProvider, but it is %s",
            provider == null ? null : provider.getClass().getName());
        DynamicEndpointParams params = DynamicEndpointParams.builder()
                                                            .operation(operation(attributes))
                                                            .input((SerializableStruct) request)
                                                            .executionAttributes(attributes)
                                                            .build();
        return CompletableFutureUtils.joinLikeSync(endpointProvider.resolveEndpoint(params));
    }

    private static ApiOperation<?, ?> operation(ExecutionAttributes attributes) {
        return attributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
    }
}
