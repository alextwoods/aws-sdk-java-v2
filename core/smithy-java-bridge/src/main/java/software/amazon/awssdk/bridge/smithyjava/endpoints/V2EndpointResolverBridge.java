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

package software.amazon.awssdk.bridge.smithyjava.endpoints;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.bridge.smithyjava.auth.V2IdentityResolver;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.endpoints.EndpointResolverParams;

/**
 * Implements smithy-java's {@link EndpointResolver} by delegating to the AWS SDK v2 endpoint rules
 * engine.
 *
 * <h2>Why this is bridged rather than translated</h2>
 *
 * <p>Endpoint resolution is the single largest piece of per-service behavior in the SDK: FIPS,
 * dual-stack, account-ID-based routing, {@code ResourceArn}-driven routing, client/static/operation
 * context params, endpoint overrides, and any customer-supplied {@code EndpointProvider}. The v2 rules
 * engine already implements all of it, generated from the same model. Reimplementing it against
 * smithy-java's rules runtime would be a large amount of work whose only outcome would be to arrive at
 * the same answers.
 *
 * <p>The bridge is possible without extra codegen because the generated
 * {@code <Service>ResolveEndpointInterceptor.ruleParams(SdkRequest, ExecutionAttributes)} method is
 * {@code public static} and self-contained. Generated client code passes a lambda that composes it
 * with the configured provider; see {@link V2RuleParamsResolver}.
 *
 * <h2>Account-ID routing</h2>
 *
 * <p>The rules engine reads the caller's account ID out of the {@code SELECTED_AUTH_SCHEME} execution
 * attribute. That works here for free: smithy's {@code ClientPipeline} resolves the auth scheme and
 * identity <em>before</em> resolving the endpoint, and stashes the identity in
 * {@link CallContext#IDENTITY}, so this class reads the already-resolved identity out of the call
 * context rather than resolving credentials a second time.
 *
 * <h2>Cost</h2>
 *
 * <p>Each resolution allocates an {@link ExecutionAttributes} copy, a
 * {@link BusinessMetricCollection}, a {@link SelectedAuthScheme}, and a completed future. smithy
 * resolves endpoints once per <em>attempt</em> rather than once per execution, so on a retried call
 * this happens more often than under v2. A single-entry cache keyed on the resolved URI avoids
 * re-allocating the smithy {@link Endpoint} on the steady-state path, where the answer never changes.
 */
@SdkProtectedApi
public final class V2EndpointResolverBridge implements EndpointResolver {

    /**
     * Composes the generated {@code ruleParams(...)} with the configured {@code EndpointProvider}.
     *
     * <p>Generated client code supplies this as a lambda, which is what keeps this class free of any
     * service-specific types:
     * {@snippet :
     * (request, attributes) -> provider.resolveEndpoint(
     *         DynamoDbResolveEndpointInterceptor.ruleParams(request, attributes)).join()
     * }
     */
    @FunctionalInterface
    public interface V2RuleParamsResolver {
        software.amazon.awssdk.endpoints.Endpoint resolve(SdkRequest request, ExecutionAttributes attributes);
    }

    private final V2RuleParamsResolver v2Resolver;
    private final ExecutionAttributes template;
    private final AuthSchemeOption sigV4Option;

    // Single-entry memo of the last resolved endpoint. Endpoints are stable for the overwhelming
    // majority of calls, so this keeps the steady-state path from re-parsing the URI.
    private volatile URI cachedUri;
    private volatile Endpoint cachedEndpoint;

    /**
     * @param v2Config   the v2 client configuration to read endpoint builtins from.
     * @param v2Resolver composition of the generated {@code ruleParams} and the endpoint provider.
     */
    public V2EndpointResolverBridge(SdkClientConfiguration v2Config, V2RuleParamsResolver v2Resolver) {
        this.v2Resolver = v2Resolver;
        this.sigV4Option = AuthSchemeOption.builder().schemeId("aws.auth#sigv4").build();
        this.template = buildTemplate(v2Config);
    }

    // Everything the rules engine reads that does not vary per call. Built once at client construction
    // and copied per resolution, because ruleParams mutates BUSINESS_METRICS.
    private static ExecutionAttributes buildTemplate(SdkClientConfiguration v2Config) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, v2Config.option(AwsClientOption.AWS_REGION));
        attributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED,
                                v2Config.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED));
        attributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED,
                                v2Config.option(AwsClientOption.FIPS_ENDPOINT_ENABLED));
        attributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                v2Config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER));
        attributes.putAttribute(AwsExecutionAttribute.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE,
                                v2Config.option(AwsClientOption.ACCOUNT_ID_ENDPOINT_MODE));
        attributes.putAttribute(SdkExecutionAttribute.SERVICE_NAME,
                                v2Config.option(SdkClientOption.SERVICE_NAME));
        attributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS,
                                v2Config.option(SdkClientOption.CLIENT_CONTEXT_PARAMS));
        return attributes;
    }

    @Override
    public Endpoint resolveEndpoint(EndpointResolverParams params) {
        ExecutionAttributes attributes = template.copy();
        attributes.putAttribute(AwsExecutionAttribute.OPERATION_NAME, params.operation().schema().id().getName());
        // Business metrics recorded by the rules engine are collected and discarded: v2 stamps them into
        // the User-Agent, and smithy-java builds its own. See compatability_issues.md 7.2.
        attributes.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, new BusinessMetricCollection());
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme(params));

        software.amazon.awssdk.endpoints.Endpoint v2Endpoint =
                v2Resolver.resolve((SdkRequest) params.inputValue(), attributes);
        return toSmithy(v2Endpoint.url());
    }

    // Hands the already-resolved identity back to the v2 rules engine so the AccountId builtin resolves
    // without a second credential lookup. The signer is never invoked; only identity() is read.
    private SelectedAuthScheme<AwsCredentialsIdentity> selectedAuthScheme(EndpointResolverParams params) {
        var identity = params.context().get(CallContext.IDENTITY);
        AwsCredentialsIdentity v2Identity =
                identity instanceof software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity smithy
                        ? V2IdentityResolver.toV2(smithy)
                        : AwsCredentialsIdentity.builder().accessKeyId("").secretAccessKey("").build();
        return new SelectedAuthScheme<>(CompletableFuture.completedFuture(v2Identity),
                                        UnusedSigner.INSTANCE,
                                        sigV4Option);
    }

    private Endpoint toSmithy(URI url) {
        Endpoint cached = cachedEndpoint;
        if (cached != null && url.equals(cachedUri)) {
            return cached;
        }
        Endpoint resolved = Endpoint.builder().uri(url).build();
        this.cachedUri = url;
        this.cachedEndpoint = resolved;
        return resolved;
    }
}
