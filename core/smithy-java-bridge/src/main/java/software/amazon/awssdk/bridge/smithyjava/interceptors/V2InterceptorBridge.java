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

package software.amazon.awssdk.bridge.smithyjava.interceptors;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;

/**
 * Runs a customer's v2 {@link ExecutionInterceptor} chain from inside smithy-java's pipeline.
 *
 * <p>This is what lets customer-authored interceptors — and the SDK's own classpath-discovered ones —
 * keep working when the pipeline underneath is smithy-java. The whole v2 chain is driven through
 * {@link ExecutionInterceptorChain} so that v2's ordering rules (forward for {@code before*}, reverse
 * for {@code after*}) and its result validation are preserved rather than re-implemented.
 *
 * <h2>Installed lazily</h2>
 *
 * <p>Bridging is not free: it materializes a v2 {@link InterceptorContext}, an
 * {@link ExecutionAttributes} copy, and — on the signing hook — a v2 {@link SdkHttpRequest} converted
 * from and back to smithy's representation. So the config translator only installs this interceptor
 * when the client actually has interceptors that smithy does not already replace. A default client
 * pays nothing.
 *
 * <h2>Hook mapping</h2>
 *
 * <pre>
 *   smithy-java ClientInterceptor      AWS SDK v2 ExecutionInterceptor
 *   ----------------------------       -------------------------------
 *   readBeforeExecution          --&gt;   beforeExecution
 *   modifyBeforeSerialization    --&gt;   modifyRequest
 *   modifyBeforeSigning          --&gt;   modifyHttpRequest / modifyHttpContent
 *   readAfterExecution           --&gt;   afterExecution | onExecutionFailure
 * </pre>
 *
 * <p>{@code modifyRequest} maps to {@code modifyBeforeSerialization} and {@code modifyHttpRequest} to
 * {@code modifyBeforeSigning} because that is where each sits relative to serialization and signing in
 * v2: a header added by {@code modifyHttpRequest} must end up inside the signature, as it does in v2.
 *
 * <p>The remaining twelve v2 hooks are not bridged; see {@code compatability_issues.md} section 2.1 for
 * the full list and what depends on each.
 */
@SdkProtectedApi
public final class V2InterceptorBridge implements ClientInterceptor {

    // Per-call state, threaded through the smithy call context because the v2 chain is stateful across
    // hooks (each hook sees the InterceptorContext the previous one produced).
    private static final software.amazon.smithy.java.context.Context.Key<CallState> STATE =
            software.amazon.smithy.java.context.Context.key("v2InterceptorBridgeState");

    private final ExecutionInterceptorChain chain;
    private final ExecutionAttributes template;
    private final URI placeholderEndpoint;

    /**
     * @param interceptors the v2 interceptors to run, in v2 order.
     * @param v2Config     client configuration, read once to seed the execution attributes.
     */
    public V2InterceptorBridge(List<ExecutionInterceptor> interceptors, SdkClientConfiguration v2Config) {
        this.chain = new ExecutionInterceptorChain(interceptors);
        this.template = buildTemplate(v2Config);
        this.placeholderEndpoint = clientEndpoint(v2Config);
    }

    /**
     * The scheme/host/port shown to v2 interceptors, because smithy has not resolved one yet.
     *
     * <p>smithy resolves the endpoint <em>after</em> {@code modifyBeforeSigning}, so the request URI at
     * that point is smithy's unresolved placeholder: no scheme, no host, path {@code "/"}. v2's
     * {@code modifyHttpRequest} is specified to see a complete URI, and a v2 {@code SdkHttpFullRequest}
     * cannot even be built without a protocol. The client endpoint is the closest available answer; it
     * matches the resolved endpoint for a client with an endpoint override, and differs from it whenever
     * the endpoint rules rewrite the host (accounts endpoints, FIPS/dualstack, host prefixes).
     * See {@code compatability_issues.md} section 2.1.
     */
    private static URI clientEndpoint(SdkClientConfiguration v2Config) {
        ClientEndpointProvider provider = v2Config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (provider != null && provider.clientEndpoint() != null) {
            return provider.clientEndpoint();
        }
        return URI.create("https://unresolved.invalid");
    }

    // Client-scoped attributes, built once and copied per call. This is not the full set v2 populates —
    // metric collectors, checksum specs, and the auth-scheme attributes are absent. See 2.1.
    private static ExecutionAttributes buildTemplate(SdkClientConfiguration v2Config) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, v2Config.option(AwsClientOption.AWS_REGION));
        attributes.putAttribute(AwsExecutionAttribute.ENDPOINT_PREFIX,
                                v2Config.option(AwsClientOption.ENDPOINT_PREFIX));
        attributes.putAttribute(SdkExecutionAttribute.SERVICE_NAME, v2Config.option(SdkClientOption.SERVICE_NAME));
        attributes.putAttribute(SdkExecutionAttribute.CLIENT_TYPE, v2Config.option(SdkClientOption.CLIENT_TYPE));
        return attributes;
    }

    @Override
    public void readBeforeExecution(InputHook<?, ?> hook) {
        if (!(hook.input() instanceof SdkRequest request)) {
            return;
        }
        ExecutionAttributes attributes = template.copy();
        attributes.putAttribute(SdkExecutionAttribute.OPERATION_NAME,
                                hook.operation().schema().id().getName());
        CallState state = new CallState(InterceptorContext.builder().request(request).build(), attributes);
        hook.context().put(STATE, state);
        chain.beforeExecution(state.context, state.attributes);
    }

    @Override
    public <I extends SerializableStruct> I modifyBeforeSerialization(InputHook<I, ?> hook) {
        CallState state = hook.context().get(STATE);
        if (state == null) {
            return hook.input();
        }
        InterceptorContext result = chain.modifyRequest(state.context, state.attributes);
        state.context = result;
        SdkRequest modified = result.request();
        // Reference equality: the chain only copies the context when an interceptor actually swapped the
        // request, so an unmodified request costs nothing here.
        return modified == hook.input() ? hook.input() : hook.asInputType((SerializableStruct) modified);
    }

    @Override
    public <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
        CallState state = hook.context().get(STATE);
        if (state == null || !(hook.request() instanceof HttpRequest smithyRequest)) {
            return hook.request();
        }

        SdkHttpRequest before = toV2Request(smithyRequest);
        state.context = state.context.copy(b -> b.httpRequest(before));
        InterceptorContext result = chain.modifyHttpRequestAndHttpContent(state.context, state.attributes);
        state.context = result;

        SdkHttpRequest after = result.httpRequest();
        if (after == before) {
            return hook.request();
        }
        // A RequestBody swapped in by modifyHttpContent is dropped: smithy owns serialization and the
        // body is already a DataStream. See compatability_issues.md 2.1.
        ModifiableHttpRequest modifiable = smithyRequest.toModifiable();
        modifiable.setMethod(after.method().name());
        modifiable.setUri(writeBackUri(after));
        modifiable.setHeaders(after.headers());
        return hook.asRequestType(modifiable);
    }

    @Override
    public void readAfterExecution(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {
        CallState state = hook.context().get(STATE);
        if (state == null) {
            return;
        }
        if (hook.response() instanceof HttpResponse response) {
            SdkHttpResponse v2Response = toV2Response(response);
            state.context = state.context.copy(b -> b.httpResponse(v2Response));
        }
        if (error != null) {
            chain.onExecutionFailure(new FailedExecution(state.context, error), state.attributes);
            return;
        }
        if (hook.output() instanceof SdkResponse response) {
            state.context = state.context.copy(b -> b.response(response));
        }
        chain.afterExecution(state.context, state.attributes);
    }

    // ---- conversions ---------------------------------------------------------

    /**
     * The URI to hand back to smithy after the v2 chain has run.
     *
     * <p>Whatever host the interceptors saw is discarded: smithy resolves the endpoint after this hook
     * and {@code setServiceEndpoint} overwrites scheme, host and port, so a host change made here would
     * be silently dropped anyway (see {@code compatability_issues.md} 2.1). Only the path and query
     * survive — and the path is made relative again, because {@code setServiceEndpoint} concatenates the
     * endpoint's path in front of it and would otherwise duplicate the prefix.
     */
    private URI writeBackUri(SdkHttpRequest after) {
        String prefix = placeholderEndpoint.getRawPath();
        String path = after.encodedPath();
        if (prefix != null && !prefix.isEmpty() && !prefix.equals("/") && path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        String query = SdkHttpUtils.encodeAndFlattenQueryParameters(after.rawQueryParameters()).orElse(null);
        return URI.create(query == null ? path : path + "?" + query);
    }

    private SdkHttpRequest toV2Request(HttpRequest request) {
        URI uri = request.uri().toURI();
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                              .method(SdkHttpMethod.fromValue(request.method()));
        if (uri.getScheme() == null || uri.getHost() == null) {
            // Unresolved placeholder: keep the path smithy serialized, and borrow the scheme, host and
            // port from the client endpoint. Set piecewise rather than through uri(URI), which appends
            // paths in the opposite order. See clientEndpoint().
            builder.protocol(placeholderEndpoint.getScheme())
                   .host(placeholderEndpoint.getHost())
                   .port(placeholderEndpoint.getPort())
                   .encodedPath(SdkHttpUtils.appendUri(placeholderEndpoint.getRawPath(), uri.getRawPath()));
        } else {
            builder.uri(uri);
        }
        // Builder.uri(URI) does not carry query parameters across, so copy them explicitly. Guarded
        // because uriParams throws NPE on a URI with no query string, which is every awsJson request.
        if (uri.getRawQuery() != null) {
            for (Map.Entry<String, List<String>> param : SdkHttpUtils.uriParams(uri).entrySet()) {
                builder.putRawQueryParameter(param.getKey(), param.getValue());
            }
        }
        for (Map.Entry<String, List<String>> header : request.headers().map().entrySet()) {
            builder.putHeader(header.getKey(), header.getValue());
        }
        return builder.build();
    }

    private static SdkHttpResponse toV2Response(HttpResponse response) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(response.statusCode())
                                  .headers(response.headers().map())
                                  .build();
    }

    private static final class CallState {
        private InterceptorContext context;
        private final ExecutionAttributes attributes;

        private CallState(InterceptorContext context, ExecutionAttributes attributes) {
            this.context = context;
            this.attributes = attributes;
        }
    }

    /**
     * A {@link Context.FailedExecution} over whatever the bridge managed to capture before the failure.
     *
     * <p>Implemented here rather than reusing v2's {@code DefaultFailedExecutionContext} to keep the
     * bridge off of {@code sdk-core} internals.
     */
    private static final class FailedExecution implements Context.FailedExecution {
        private final InterceptorContext context;
        private final Throwable exception;

        private FailedExecution(InterceptorContext context, Throwable exception) {
            this.context = context;
            this.exception = exception;
        }

        @Override
        public Throwable exception() {
            return exception;
        }

        @Override
        public SdkRequest request() {
            return context.request();
        }

        @Override
        public Optional<SdkHttpRequest> httpRequest() {
            return Optional.ofNullable(context.httpRequest());
        }

        @Override
        public Optional<SdkHttpResponse> httpResponse() {
            return Optional.ofNullable(context.httpResponse());
        }

        @Override
        public Optional<SdkResponse> response() {
            return Optional.ofNullable(context.response());
        }
    }
}
