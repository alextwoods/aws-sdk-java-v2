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

package software.amazon.awssdk.core.internal.http;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.endpoint.EndpointResolver;
import software.amazon.awssdk.core.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.EndpointResolutionStage;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * The endpoint resolution step of the schema-based request pipeline (design D.6, seam 1), with the observable
 * behavior of the legacy {@link EndpointResolutionStage}: the same skip conditions, resolver call,
 * {@link SdkInternalExecutionAttribute#RESOLVED_ENDPOINT}, re-applied interceptor auth properties, endpoint headers,
 * {@link CoreMetric#ENDPOINT_RESOLVE_DURATION} and final URI.
 *
 * <p>The resolved endpoint is applied to the request through {@link ClientProtocol#updateServiceEndpoint}, which
 * replaces the legacy URI rewrite. The step runs in two parts inside {@link RequestMutationStages}' single pass:
 * {@link #resolve} runs at the legacy stage's position (after auth scheme resolution, before the checksum and
 * {@code User-Agent} mutations, which do not read the URI) and leaves the request's path relative to the endpoint;
 * {@link #apply} moves the built request to the endpoint. When an interceptor moved the request to another host in
 * {@code modifyHttpRequest}, or when the protocol's path joining cannot reproduce the legacy path (an interceptor path
 * that does not start with the client endpoint path), the URI is set on the builder exactly as the legacy stage sets
 * it and {@link #apply} has nothing to do.
 */
@SdkInternalApi
public final class EndpointResolution {
    private EndpointResolution() {
    }

    /**
     * Resolves the call's endpoint and applies everything except the scheme, host and port to the request builder.
     *
     * @param request the request being mutated
     * @param context the call's context
     * @return the endpoint to move the built request to with {@link #apply}, or null when the request already has its
     *     final URI (resolution skipped, or the URI was set on the builder)
     */
    public static Endpoint resolve(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        ExecutionAttributes attrs = context.executionAttributes();
        if (Boolean.TRUE.equals(attrs.getAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT))
            || Boolean.TRUE.equals(attrs.getAttribute(SdkInternalExecutionAttribute.SKIP_ENDPOINT_RESOLUTION))
            || attrs.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT) != null) {
            return null;
        }
        EndpointResolver resolver = attrs.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER);
        if (resolver == null) {
            return null;
        }

        MetricCollector metricCollector = attrs.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        boolean measure = MetricUtils.collectsMetrics(metricCollector);
        long start = measure ? System.nanoTime() : 0L;
        Endpoint endpoint = resolver.resolve(context.executionContext().interceptorContext().request(), attrs);
        long end = measure ? System.nanoTime() : 0L;

        reapplyInterceptorModifiedAuthProperties(attrs);
        if (measure) {
            metricCollector.reportMetric(CoreMetric.ENDPOINT_RESOLVE_DURATION, Duration.ofNanos(end - start));
        }
        attrs.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint);

        Map<String, List<String>> headers = endpoint.headers();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((name, values) -> values.forEach(v -> request.appendHeader(name, v)));
        }

        URI clientEndpoint = attrs.getAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER).clientEndpoint();
        URI resolvedUri = endpoint.url();
        String legacyPath = legacyPath(request.encodedPath(), clientEndpoint.getRawPath(), resolvedUri.getRawPath());
        if (interceptorModifiedEndpoint(request, attrs)) {
            request.encodedPath(legacyPath);
            return null;
        }

        String relativePath = endpointRelativePath(request.encodedPath(), clientEndpoint.getRawPath(),
                                                   resolvedUri.getRawPath());
        if (relativePath != null && SdkHttpUtils.appendUri(nullToEmpty(resolvedUri.getRawPath()), relativePath)
                                                .equals(legacyPath)) {
            request.encodedPath(relativePath);
            return endpoint;
        }
        request.encodedPath(legacyPath)
               .protocol(resolvedUri.getScheme())
               .host(resolvedUri.getHost())
               .port(resolvedUri.getPort());
        return null;
    }

    /**
     * Moves a built request to the endpoint {@link #resolve} returned, through the call's protocol.
     *
     * @param request the built request, whose path is relative to the endpoint
     * @param endpoint the endpoint, or null to return the request unchanged
     * @param attributes the call's execution attributes, holding the protocol
     * @return the request addressed to the endpoint
     */
    public static SdkHttpFullRequest apply(SdkHttpFullRequest request, Endpoint endpoint, ExecutionAttributes attributes) {
        if (endpoint == null) {
            return request;
        }
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol = ProtocolMarshalling.httpProtocol(attributes);
        return protocol.updateServiceEndpoint(request, endpoint);
    }

    /**
     * The path the legacy stage gives the request: unchanged when the resolved path is the client endpoint path,
     * otherwise the resolved path followed by the request path with the client endpoint path removed.
     */
    private static String legacyPath(String requestPath, String clientEndpointPath, String resolvedUriPath) {
        if (resolvedUriPath.equals(clientEndpointPath)) {
            return requestPath;
        }
        String withoutClientPath = StringUtils.replaceOnce(requestPath, clientEndpointPath, "");
        return SdkHttpUtils.appendUri(resolvedUriPath, withoutClientPath);
    }

    /**
     * The request path relative to the endpoint: the path the protocol serialized, which the client endpoint path was
     * put in front of before {@code afterMarshalling}. Null when it cannot be determined.
     */
    private static String endpointRelativePath(String requestPath, String clientEndpointPath, String resolvedUriPath) {
        if (!resolvedUriPath.equals(clientEndpointPath)) {
            return StringUtils.replaceOnce(requestPath, clientEndpointPath, "");
        }
        if (requestPath == null || !requestPath.startsWith(clientEndpointPath)) {
            return null;
        }
        return requestPath.substring(clientEndpointPath.length());
    }

    /**
     * Same as the legacy stage: whether a {@code modifyHttpRequest} interceptor changed the host, scheme or port of the
     * request, compared with the snapshot taken before those interceptors ran.
     */
    private static boolean interceptorModifiedEndpoint(SdkHttpFullRequest.Builder request, ExecutionAttributes attrs) {
        URI preModifyUri = attrs.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY);
        if (preModifyUri == null) {
            return false;
        }
        String requestHost = request.host();
        Integer requestPort = request.port();
        return requestHost != null
               && (!requestHost.equals(preModifyUri.getHost())
                   || !String.valueOf(request.protocol()).equals(preModifyUri.getScheme())
                   || (requestPort != null && requestPort != preModifyUri.getPort()));
    }

    private static void reapplyInterceptorModifiedAuthProperties(ExecutionAttributes attrs) {
        SelectedAuthScheme<?> currentScheme = attrs.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (currentScheme == null) {
            return;
        }
        SelectedAuthScheme<?> afterInterceptors =
            attrs.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_POST_INTERCEPTORS);
        if (afterInterceptors == null) {
            return;
        }
        SelectedAuthScheme<?> beforeInterceptors =
            attrs.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_PRE_INTERCEPTORS);
        AuthSchemeResolver.applyInterceptorModifiedProperties(currentScheme, beforeInterceptors, afterInterceptors, attrs);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
