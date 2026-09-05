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

package software.amazon.awssdk.bridge.smithyjava.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.bridge.smithyjava.auth.V2IdentityResolver;
import software.amazon.awssdk.bridge.smithyjava.endpoints.V2EndpointResolverBridge;
import software.amazon.awssdk.bridge.smithyjava.error.V2ErrorEnricher;
import software.amazon.awssdk.bridge.smithyjava.interceptors.V2InterceptorBridge;
import software.amazon.awssdk.bridge.smithyjava.transport.V2TransportBridge;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.retry.RetryPolicyAdapter;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4Settings;
import software.amazon.smithy.java.aws.sdkv2.retries.SdkRetryStrategy;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.core.schema.ApiService;

/**
 * Builds a configured {@link SmithyBridgeClient} out of a v2 {@link SdkClientConfiguration}.
 *
 * <p>This is where the "thin veneer over smithy-java" idea becomes concrete: the customer configures a
 * v2 client builder exactly as before, and this class decides, option by option, whether that option is
 * <em>bridged</em> (the v2 component is handed to smithy-java and keeps running),
 * <em>translated</em> (the value is copied onto smithy's equivalent), or dropped.
 *
 * <table>
 *   <caption>Disposition of the options this translator reads</caption>
 *   <tr><th>v2 option</th><th>Disposition</th></tr>
 *   <tr><td>{@code SYNC_HTTP_CLIENT}</td><td>bridged via {@link V2TransportBridge}</td></tr>
 *   <tr><td>{@code CREDENTIALS_IDENTITY_PROVIDER}</td><td>bridged via {@link V2IdentityResolver}</td></tr>
 *   <tr><td>{@code RETRY_STRATEGY} / {@code RETRY_POLICY}</td><td>bridged via {@link SdkRetryStrategy}</td></tr>
 *   <tr><td>{@code EXECUTION_INTERCEPTORS}</td><td>bridged via {@link V2InterceptorBridge}, lazily</td></tr>
 *   <tr><td>endpoint provider</td><td>bridged via {@link V2EndpointResolverBridge}</td></tr>
 *   <tr><td>{@code SIGNING_REGION}, {@code SERVICE_SIGNING_NAME}</td><td>translated to SigV4 settings</td></tr>
 *   <tr><td>everything else</td><td>see {@code compatability_issues.md} section 9</td></tr>
 * </table>
 */
@SdkProtectedApi
public final class V2ConfigTranslator {

    /**
     * Measurement knobs: replace an individual bridge with smithy-java's own component.
     *
     * <p>These exist to answer "what does bridging cost?" with one binary and one code path. Setting
     * {@code awssdk.bridge.stripAll} yields a client that is smithy-java everywhere except the HTTP
     * transport, so the difference against the fully bridged client is the bridging tax, measured on
     * the same jar, the same JIT profile, and the same server.
     *
     * <p>They are not a supported feature: stripping changes behavior (a fixed endpoint instead of the
     * rules engine, smithy's retry policy instead of the configured one, no v2 interceptors, no AWS
     * error metadata on exceptions). Read once per client construction, never on the call path.
     */
    private static final String STRIP_ALL = "awssdk.bridge.stripAll";
    private static final String STRIP_ENDPOINTS = "awssdk.bridge.stripEndpoints";
    private static final String STRIP_RETRIES = "awssdk.bridge.stripRetries";
    private static final String STRIP_INTERCEPTORS = "awssdk.bridge.stripInterceptors";
    private static final String STRIP_ERROR_ENRICHER = "awssdk.bridge.stripErrorEnricher";

    /**
     * Measurement knob: keep provably inert interceptors in the bridged chain.
     *
     * <p>Set this to measure what {@link #inert} saves. Without it, an AWS client that configures no
     * interceptors of its own installs no interceptor bridge at all; with it, the three interceptors
     * {@code AwsDefaultClientBuilder} puts on every client are bridged even though none can act.
     */
    private static final String KEEP_INERT_INTERCEPTORS = "awssdk.bridge.keepInertInterceptors";

    /**
     * The six v2 hooks {@link V2InterceptorBridge} actually invokes.
     *
     * <p>An interceptor that overrides none of them cannot observe or change anything through the bridge,
     * so bridging it buys nothing and costs a per-attempt {@code InterceptorContext} and
     * {@code ExecutionAttributes}. Names only: every one of these takes {@code (Context.X,
     * ExecutionAttributes)}, so arity is enough to tell an override from an unrelated method.
     */
    private static final Set<String> BRIDGED_HOOKS = Set.of(
            "beforeExecution", "modifyRequest", "modifyHttpRequest", "modifyHttpContent",
            "afterExecution", "onExecutionFailure");

    private static final String LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME";

    private V2ConfigTranslator() {
    }

    private static boolean stripped(String property) {
        return Boolean.getBoolean(STRIP_ALL) || Boolean.getBoolean(property);
    }

    /**
     * Translates a v2 client configuration into a ready-to-build smithy-java client.
     *
     * <p>Generated code supplies the parameters that this module cannot know: the service and protocol
     * come from the generated {@code ApiService}, the endpoint resolver closes over the generated
     * {@code ruleParams} method, and the fallback error factory is the service's base exception.
     *
     * @param v2Config             the v2 client configuration.
     * @param service              the generated service shape.
     * @param protocol             the wire protocol, e.g. {@code AwsJson1Protocol} or
     *                             {@code RestXmlClientProtocol}. Generated code picks it, because the
     *                             protocol is a fact about the model and this module has no model.
     * @param endpointResolver     composition of the generated {@code ruleParams} and the endpoint provider.
     * @param replacedInterceptors v2 interceptor classes whose job smithy-java now does, and which must
     *                             therefore not be run twice. Generated code passes the auth-scheme,
     *                             endpoint-resolve, and endpoint-set interceptors.
     * @param baseExceptionBuilder the service's base exception builder, e.g. {@code DynamoDbException::builder},
     *                             used for any failure with no more specific v2 type.
     * @return a builder with everything configured; callers may override before building.
     */
    public static SmithyBridgeClient.Builder newClientBuilder(
            SdkClientConfiguration v2Config,
            ApiService service,
            ClientProtocol<?, ?> protocol,
            V2EndpointResolverBridge.V2RuleParamsResolver endpointResolver,
            Set<Class<? extends ExecutionInterceptor>> replacedInterceptors,
            Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder
    ) {
        SmithyBridgeClient.Builder builder = SmithyBridgeClient.builder();

        builder.service(service)
               .protocol(protocol)
               .transport(new V2TransportBridge(v2Config.option(SdkClientOption.SYNC_HTTP_CLIENT)))
               .baseExceptionBuilder(baseExceptionBuilder);

        if (stripped(STRIP_ENDPOINTS)) {
            builder.endpoint(v2Config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint().toString());
        } else {
            builder.endpointResolver(new V2EndpointResolverBridge(v2Config, endpointResolver));
        }

        // SigV4AuthScheme.getSignerProperties does context.expect(REGION), so the region must be present
        // in the client context or every call fails. The signing region, not the client region: they
        // differ for global endpoints.
        Region signingRegion = v2Config.option(AwsClientOption.SIGNING_REGION);
        if (signingRegion == null) {
            signingRegion = v2Config.option(AwsClientOption.AWS_REGION);
        }
        if (signingRegion != null) {
            builder.putConfig(SigV4Settings.REGION, signingRegion.id());
        }
        builder.putSupportedAuthSchemes(new SigV4AuthScheme(v2Config.option(AwsClientOption.SERVICE_SIGNING_NAME)));

        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider =
                v2Config.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
        if (credentialsProvider != null) {
            builder.addIdentityResolver(new V2IdentityResolver(credentialsProvider));
        }

        // Fills in the AWS error code and HTTP metadata that v2's retry classification needs, per
        // attempt, before the retry strategy sees the error.
        if (!stripped(STRIP_ERROR_ENRICHER)) {
            builder.addInterceptor(new V2ErrorEnricher(v2Config.option(SdkClientOption.SERVICE_NAME),
                                                      baseExceptionBuilder));
        }

        software.amazon.awssdk.retries.api.RetryStrategy v2RetryStrategy =
                stripped(STRIP_RETRIES) ? null : resolveRetryStrategy(v2Config);
        if (v2RetryStrategy != null) {
            // `of`, not `ofPrepared`: it rewires the strategy's predicates to read the RetryInfo that
            // V2RetryClassification stamps onto errors. See compatability_issues.md 3.1.
            builder.retryStrategy(SdkRetryStrategy.of(v2RetryStrategy));
        }

        List<ExecutionInterceptor> bridgeable = stripped(STRIP_INTERCEPTORS)
                ? List.of()
                : bridgeableInterceptors(v2Config, replacedInterceptors);
        if (!bridgeable.isEmpty()) {
            builder.addInterceptor(new V2InterceptorBridge(bridgeable, v2Config));
        }

        return builder;
    }

    /**
     * The v2 retry strategy to bridge, or null to let smithy-java use its own default.
     *
     * <p>v2 always resolves {@code RETRY_STRATEGY}, but when the customer configured a legacy
     * {@code RetryPolicy} the resolved value is a {@code RetryPolicyAdapter}, which cannot be bridged:
     * its builder throws {@code UnsupportedOperationException} from the very setters
     * {@code SdkRetryStrategy.of} calls, and it needs a per-request {@code RetryPolicyContext} that
     * smithy's pipeline never supplies. Such a client silently gets smithy's {@code StandardRetryStrategy}
     * instead of the configured policy. See {@code compatability_issues.md} section 3.4.
     */
    private static software.amazon.awssdk.retries.api.RetryStrategy resolveRetryStrategy(
            SdkClientConfiguration v2Config
    ) {
        software.amazon.awssdk.retries.api.RetryStrategy strategy = v2Config.option(SdkClientOption.RETRY_STRATEGY);
        return strategy instanceof RetryPolicyAdapter ? null : strategy;
    }

    /**
     * The interceptors worth bridging: everything the customer or the classpath contributed, minus the
     * generated built-ins whose work smithy-java now does itself, minus those that provably cannot act.
     *
     * <p>Running the endpoint or auth-scheme interceptors here would be actively wrong — they would
     * resolve an endpoint and an auth scheme into execution attributes that nothing downstream reads,
     * and pay for a second credential resolution to do it.
     */
    private static List<ExecutionInterceptor> bridgeableInterceptors(
            SdkClientConfiguration v2Config,
            Collection<Class<? extends ExecutionInterceptor>> replaced
    ) {
        List<ExecutionInterceptor> configured = v2Config.option(SdkClientOption.EXECUTION_INTERCEPTORS);
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        boolean keepInert = Boolean.getBoolean(KEEP_INERT_INTERCEPTORS);
        List<ExecutionInterceptor> result = new ArrayList<>(configured.size());
        for (ExecutionInterceptor interceptor : configured) {
            if (replaced.contains(interceptor.getClass())) {
                continue;
            }
            if (!keepInert && inert(interceptor)) {
                continue;
            }
            result.add(interceptor);
        }
        return result;
    }

    /**
     * Whether this interceptor can be dropped from the bridged chain without changing behavior.
     *
     * <p>Worth the trouble because {@code AwsDefaultClientBuilder.awsInterceptors()} puts three
     * interceptors on <em>every</em> AWS client, so without this a client that configures nothing still
     * pays to build a v2 {@code InterceptorContext} and {@code ExecutionAttributes} at four hook points
     * per attempt — measured at 6–8% of app CPU on a small DynamoDB operation. Two of the three cannot
     * do anything in a bridged sync client and the third cannot outside Lambda:
     *
     * <ul>
     *   <li>{@code HelpfulUnknownHostExceptionInterceptor} implements only {@code modifyException}, a
     *       hook the bridge never invokes — so it is already dead weight today, and the general
     *       {@link #BRIDGED_HOOKS} check catches it. That the bridge cannot rewrite exceptions at all
     *       is its own gap; see {@code compatability_issues.md} 2.1.
     *   <li>{@code EventStreamInitialRequestInterceptor} acts only when {@code HAS_INITIAL_REQUEST_EVENT}
     *       is set, which only an async event-stream operation does. Out of scope here, and named
     *       explicitly so that widening scope to async trips over this comment.
     *   <li>{@code TraceIdExecutionInterceptor} gates every one of its four hooks on the
     *       {@code AWS_LAMBDA_FUNCTION_NAME} environment variable, which cannot change while the JVM
     *       runs — so reading it once here is as good as reading it per call.
     * </ul>
     *
     * <p>The general check is deliberately first: a customer interceptor that overrides only unbridged
     * hooks is silently ineffective either way, and dropping it at least does not charge for it.
     */
    private static boolean inert(ExecutionInterceptor interceptor) {
        Class<?> type = interceptor.getClass();
        if (!overridesBridgedHook(type)) {
            return true;
        }
        String name = type.getName();
        if (name.equals("software.amazon.awssdk.awscore.eventstream.EventStreamInitialRequestInterceptor")) {
            return true;
        }
        if (name.equals("software.amazon.awssdk.awscore.interceptor.TraceIdExecutionInterceptor")) {
            // CHECKSTYLE:OFF - the interceptor itself reads the environment variable and nothing else.
            return System.getenv(LAMBDA_FUNCTION_NAME) == null;
            // CHECKSTYLE:ON
        }
        return false;
    }

    private static boolean overridesBridgedHook(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                if (method.getParameterCount() == 2 && BRIDGED_HOOKS.contains(method.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
