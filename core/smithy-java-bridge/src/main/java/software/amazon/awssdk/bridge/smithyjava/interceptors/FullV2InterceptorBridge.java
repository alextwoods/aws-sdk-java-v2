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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;

/**
 * Bridges a full AWS SDK v2 {@link ExecutionInterceptor} into smithy-java's pipeline by implementing
 * smithy-java's {@link ClientInterceptor} and forwarding each hook to the corresponding v2 interceptor method.
 *
 * <p>This class maintains a shared {@link ExecutionAttributes} instance across all hooks within a single
 * execution, pre-populating it with the operation name from the smithy-java hook context. All 17
 * smithy-java hooks are mapped to the corresponding v2 interceptor hooks, preserving ordering and
 * mutation semantics.
 *
 * <p>Unlike the simplified {@link V2InterceptorBridge} (which maps only 4 hooks), this bridge provides
 * full fidelity for customer v2 interceptors running under the smithy-java engine.
 */
@SdkInternalApi
public final class FullV2InterceptorBridge implements ClientInterceptor {

    private final ExecutionInterceptor v2Interceptor;
    private final ExecutionAttributes executionAttributes;

    public FullV2InterceptorBridge(ExecutionInterceptor v2Interceptor) {
        this.v2Interceptor = v2Interceptor;
        this.executionAttributes = new ExecutionAttributes();
    }

    /**
     * Returns the underlying v2 {@link ExecutionInterceptor} for testing/inspection.
     */
    public ExecutionInterceptor v2Interceptor() {
        return v2Interceptor;
    }

    /**
     * Returns the shared {@link ExecutionAttributes} for testing/inspection.
     */
    public ExecutionAttributes executionAttributes() {
        return executionAttributes;
    }

    // -- Forward hooks (registration order) --

    @Override
    public void readBeforeExecution(InputHook<?, ?> hook) {
        String operationName = hook.operation().schema().id().getName();
        executionAttributes.putAttribute(SdkExecutionAttribute.OPERATION_NAME, operationName);

        SdkRequest sdkRequest = (SdkRequest) hook.input();
        InterceptorContext context = InterceptorContext.builder()
            .request(sdkRequest)
            .build();
        v2Interceptor.beforeExecution(context, executionAttributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends SerializableStruct> I modifyBeforeSerialization(InputHook<I, ?> hook) {
        SdkRequest sdkRequest = (SdkRequest) hook.input();
        InterceptorContext context = InterceptorContext.builder()
            .request(sdkRequest)
            .build();
        SdkRequest modified = v2Interceptor.modifyRequest(context, executionAttributes);
        return (I) modified;
    }

    @Override
    public void readBeforeSerialization(InputHook<?, ?> hook) {
        SdkRequest sdkRequest = (SdkRequest) hook.input();
        InterceptorContext context = InterceptorContext.builder()
            .request(sdkRequest)
            .build();
        v2Interceptor.beforeMarshalling(context, executionAttributes);
    }

    @Override
    public void readAfterSerialization(RequestHook<?, ?, ?> hook) {
        SdkRequest sdkRequest = (SdkRequest) hook.input();
        SdkHttpRequest httpRequest = toSdkHttpRequest(hook.request());
        InterceptorContext context = InterceptorContext.builder()
            .request(sdkRequest)
            .httpRequest(httpRequest)
            .build();
        v2Interceptor.afterMarshalling(context, executionAttributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
        if (hook.request() instanceof HttpRequest httpRequest) {
            SdkRequest sdkRequest = (SdkRequest) hook.input();
            SdkHttpRequest sdkHttpRequest = toSdkHttpRequest(httpRequest);
            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(sdkHttpRequest)
                .build();
            SdkHttpRequest modified = v2Interceptor.modifyHttpRequest(context, executionAttributes);

            // Rebuild the smithy-java ModifiableHttpRequest from the v2 modified SdkHttpRequest.
            // This ensures all mutations (URI changes, header additions/removals) are applied.
            ModifiableHttpRequest modifiable = HttpRequest.create()
                .setMethod(modified.method().name())
                .setUri(modified.getUri());

            modified.forEachHeader((name, values) -> {
                for (String value : values) {
                    modifiable.headers().addHeader(name, value);
                }
            });

            // Carry over the body from the original request
            modifiable.setBody(httpRequest.body());

            return (RequestT) modifiable;
        }
        return hook.request();
    }

    @Override
    public void readBeforeTransmit(RequestHook<?, ?, ?> hook) {
        if (hook.request() instanceof HttpRequest httpRequest) {
            SdkRequest sdkRequest = (SdkRequest) hook.input();
            SdkHttpRequest sdkHttpRequest = toSdkHttpRequest(httpRequest);
            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(sdkHttpRequest)
                .build();
            v2Interceptor.beforeTransmission(context, executionAttributes);
        }
    }

    @Override
    public void readAfterTransmit(ResponseHook<?, ?, ?, ?> hook) {
        if (hook.request() instanceof HttpRequest httpRequest
                && hook.response() instanceof HttpResponse httpResponse) {
            SdkRequest sdkRequest = (SdkRequest) hook.input();
            SdkHttpRequest sdkHttpRequest = toSdkHttpRequest(httpRequest);
            SdkHttpResponse sdkHttpResponse = toSdkHttpResponse(httpResponse);
            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(sdkHttpRequest)
                .httpResponse(sdkHttpResponse)
                .build();
            v2Interceptor.afterTransmission(context, executionAttributes);
        }
    }

    // -- Completion hooks --

    @Override
    @SuppressWarnings("unchecked")
    public <ResponseT> ResponseT modifyBeforeDeserialization(ResponseHook<?, ?, ?, ResponseT> hook) {
        if (hook.response() instanceof HttpResponse httpResponse && hook.input() instanceof SdkRequest sdkRequest) {
            SdkHttpResponse v2HttpResponse = toSdkHttpResponse(httpResponse);
            SdkHttpRequest v2HttpRequest = hook.request() instanceof HttpRequest httpReq
                ? toSdkHttpRequest(httpReq) : null;

            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(v2HttpRequest)
                .httpResponse(v2HttpResponse)
                .build();

            SdkHttpResponse modified = v2Interceptor.modifyHttpResponse(context, executionAttributes);
            if (modified != v2HttpResponse) {
                // Build a new smithy-java HttpResponse from the modified v2 response
                return (ResponseT) toSmithyHttpResponse(modified, httpResponse);
            }
        }
        return hook.response();
    }

    @Override
    public void readBeforeDeserialization(ResponseHook<?, ?, ?, ?> hook) {
        if (hook.response() instanceof HttpResponse httpResponse && hook.input() instanceof SdkRequest sdkRequest) {
            SdkHttpResponse v2HttpResponse = toSdkHttpResponse(httpResponse);
            SdkHttpRequest v2HttpRequest = hook.request() instanceof HttpRequest httpReq
                ? toSdkHttpRequest(httpReq) : null;

            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(v2HttpRequest)
                .httpResponse(v2HttpResponse)
                .build();

            v2Interceptor.beforeUnmarshalling(context, executionAttributes);
        }
    }

    @Override
    public void readAfterDeserialization(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {
        if (hook.input() instanceof SdkRequest sdkRequest) {
            SdkHttpResponse v2HttpResponse = hook.response() instanceof HttpResponse httpResponse
                ? toSdkHttpResponse(httpResponse) : null;
            SdkHttpRequest v2HttpRequest = hook.request() instanceof HttpRequest httpReq
                ? toSdkHttpRequest(httpReq) : null;
            SdkResponse sdkResponse = hook.output() instanceof SdkResponse resp ? resp : null;

            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(v2HttpRequest)
                .httpResponse(v2HttpResponse)
                .response(sdkResponse)
                .build();

            v2Interceptor.afterUnmarshalling(context, executionAttributes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends SerializableStruct> O modifyBeforeCompletion(OutputHook<?, O, ?, ?> hook, RuntimeException error) {
        if (error != null) {
            return hook.forward(error);
        }
        if (hook.input() instanceof SdkRequest sdkRequest && hook.output() instanceof SdkResponse sdkResponse) {
            SdkHttpResponse v2HttpResponse = hook.response() instanceof HttpResponse httpResponse
                ? toSdkHttpResponse(httpResponse) : null;
            SdkHttpRequest v2HttpRequest = hook.request() instanceof HttpRequest httpReq
                ? toSdkHttpRequest(httpReq) : null;

            InterceptorContext context = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(v2HttpRequest)
                .httpResponse(v2HttpResponse)
                .response(sdkResponse)
                .build();

            SdkResponse modified = v2Interceptor.modifyResponse(context, executionAttributes);
            if (modified != sdkResponse && modified instanceof SerializableStruct) {
                return hook.asOutputType((SerializableStruct) modified);
            }
        }
        return hook.forward(error);
    }

    @Override
    public void readAfterExecution(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {
        if (hook.input() instanceof SdkRequest sdkRequest) {
            SdkHttpResponse v2HttpResponse = hook.response() instanceof HttpResponse httpResponse
                ? toSdkHttpResponse(httpResponse) : null;
            SdkHttpRequest v2HttpRequest = hook.request() instanceof HttpRequest httpReq
                ? toSdkHttpRequest(httpReq) : null;
            SdkResponse sdkResponse = hook.output() instanceof SdkResponse resp ? resp : null;

            if (error == null) {
                // Success path: call afterExecution
                InterceptorContext context = InterceptorContext.builder()
                    .request(sdkRequest)
                    .httpRequest(v2HttpRequest)
                    .httpResponse(v2HttpResponse)
                    .response(sdkResponse)
                    .build();

                v2Interceptor.afterExecution(context, executionAttributes);
            } else {
                // Failure path: call onExecutionFailure
                InterceptorContext interceptorContext = InterceptorContext.builder()
                    .request(sdkRequest)
                    .httpRequest(v2HttpRequest)
                    .httpResponse(v2HttpResponse)
                    .response(sdkResponse)
                    .build();

                Context.FailedExecution failedContext = DefaultFailedExecutionContext.builder()
                    .interceptorContext(interceptorContext)
                    .exception(error)
                    .build();

                v2Interceptor.onExecutionFailure(failedContext, executionAttributes);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends SerializableStruct> O modifyBeforeAttemptCompletion(
            OutputHook<?, O, ?, ?> hook,
            RuntimeException error
    ) {
        if (error != null && hook.input() instanceof SdkRequest sdkRequest) {
            SdkHttpResponse v2HttpResponse = hook.response() instanceof HttpResponse httpResponse
                ? toSdkHttpResponse(httpResponse) : null;
            SdkHttpRequest v2HttpRequest = hook.request() instanceof HttpRequest httpReq
                ? toSdkHttpRequest(httpReq) : null;
            SdkResponse sdkResponse = hook.output() instanceof SdkResponse resp ? resp : null;

            InterceptorContext interceptorContext = InterceptorContext.builder()
                .request(sdkRequest)
                .httpRequest(v2HttpRequest)
                .httpResponse(v2HttpResponse)
                .response(sdkResponse)
                .build();

            Context.FailedExecution failedContext = DefaultFailedExecutionContext.builder()
                .interceptorContext(interceptorContext)
                .exception(error)
                .build();

            Throwable modified = v2Interceptor.modifyException(failedContext, executionAttributes);
            if (modified instanceof RuntimeException runtimeEx) {
                return hook.forward(runtimeEx);
            } else if (modified != null && modified != error) {
                return hook.forward(new RuntimeException(modified));
            }
        }
        return hook.forward(error);
    }

    // -- Conversion helpers --

    /**
     * Converts a smithy-java {@link HttpRequest} to a v2 {@link SdkHttpRequest}.
     *
     * <p>Handles the case where the URI has no scheme (e.g., during the readAfterSerialization hook
     * which fires before endpoint resolution). In that case, we use "https" as a default protocol
     * so the SdkHttpRequest can be constructed without throwing NPE.
     */
    private static SdkHttpRequest toSdkHttpRequest(Object request) {
        if (request instanceof HttpRequest httpRequest) {
            java.net.URI uri = httpRequest.uri().toURI();
            // If the scheme is null (endpoint not yet resolved), use a synthetic URI with https
            if (uri.getScheme() == null) {
                try {
                    uri = new java.net.URI("https", uri.getAuthority() != null ? uri.getAuthority() : "localhost",
                        uri.getPath(), uri.getQuery(), uri.getFragment());
                } catch (java.net.URISyntaxException e) {
                    // Fallback: construct a minimal valid URI
                    uri = java.net.URI.create("https://localhost" + (uri.getPath() != null ? uri.getPath() : "/"));
                }
            }
            SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .uri(uri)
                .method(SdkHttpMethod.fromValue(httpRequest.method()));

            for (Map.Entry<String, List<String>> entry : httpRequest.headers().map().entrySet()) {
                builder.putHeader(entry.getKey(), entry.getValue());
            }
            return builder.build();
        }
        return null;
    }

    /**
     * Converts a smithy-java {@link HttpResponse} to a v2 {@link SdkHttpResponse}.
     */
    private static SdkHttpResponse toSdkHttpResponse(Object response) {
        if (response instanceof HttpResponse httpResponse) {
            SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder()
                .statusCode(httpResponse.statusCode())
                .headers(httpResponse.headers().map());
            return builder.build();
        }
        return null;
    }

    /**
     * Converts a v2 {@link SdkHttpResponse} back to a smithy-java {@link HttpResponse},
     * preserving the body from the original smithy-java response.
     */
    private static HttpResponse toSmithyHttpResponse(SdkHttpResponse v2Response, HttpResponse originalSmithyResponse) {
        HttpHeaders headers = HttpHeaders.of(v2Response.headers());
        return HttpResponse.of(
            HttpVersion.HTTP_1_1,
            v2Response.statusCode(),
            headers,
            originalSmithyResponse.body()
        );
    }
}
