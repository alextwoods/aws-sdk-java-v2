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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Records every {@link ExecutionInterceptor} hook it receives with what the hook's {@link Context} type exposes and the
 * names of the {@link ExecutionAttributes} keys present at that moment. It can fail one hook (after recording it) with
 * an {@link IllegalStateException}.
 *
 * <p>Each hook's context is described only through the {@code Context} interface the hook is declared with, so state an
 * interceptor cannot see from that hook (for example a previous attempt's response at {@code beforeTransmission}) is not
 * compared. Header values are left out except {@code amz-sdk-request}, because the invocation ID is random.
 */
final class HookRecordingInterceptor implements ExecutionInterceptor {
    private final List<HookRecord> records = new CopyOnWriteArrayList<>();
    private final String failingHook;

    HookRecordingInterceptor(String failingHook) {
        this.failingHook = failingHook;
    }

    List<HookRecord> records() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    static String describeException(Throwable exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        record("beforeExecution", request(context), executionAttributes);
    }

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        record("modifyRequest", request(context), executionAttributes);
        return context.request();
    }

    @Override
    public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
        record("beforeMarshalling", request(context), executionAttributes);
    }

    @Override
    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        record("afterMarshalling", httpRequest(context), executionAttributes);
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        record("modifyHttpRequest", httpRequest(context), executionAttributes);
        return context.httpRequest();
    }

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {
        record("modifyHttpContent", httpRequest(context), executionAttributes);
        return context.requestBody();
    }

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context,
                                                             ExecutionAttributes executionAttributes) {
        record("modifyAsyncHttpContent", httpRequest(context), executionAttributes);
        return context.asyncRequestBody();
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        record("beforeTransmission", httpRequest(context), executionAttributes);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        record("afterTransmission", httpResponse(context), executionAttributes);
    }

    @Override
    public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
        record("modifyHttpResponse", httpResponse(context), executionAttributes);
        return context.httpResponse();
    }

    @Override
    public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                           ExecutionAttributes executionAttributes) {
        record("modifyAsyncHttpResponseContent", httpResponse(context), executionAttributes);
        return context.responsePublisher();
    }

    @Override
    public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                           ExecutionAttributes executionAttributes) {
        record("modifyHttpResponseContent", httpResponse(context), executionAttributes);
        return context.responseBody();
    }

    @Override
    public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
        record("beforeUnmarshalling", httpResponse(context), executionAttributes);
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        record("afterUnmarshalling", response(context), executionAttributes);
    }

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        record("modifyResponse", response(context), executionAttributes);
        return context.response();
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        record("afterExecution", response(context), executionAttributes);
    }

    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        record("modifyException", failure(context), executionAttributes);
        return context.exception();
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        record("onExecutionFailure", failure(context), executionAttributes);
    }

    private void record(String hook, String context, ExecutionAttributes executionAttributes) {
        List<String> keys = executionAttributes.getAttributes().keySet().stream()
                                               .map(Object::toString)
                                               .sorted()
                                               .collect(Collectors.toList());
        records.add(new HookRecord(hook, context, keys, Thread.currentThread().getName()));
        if (hook.equals(failingHook)) {
            throw new IllegalStateException("Interceptor failure in " + hook);
        }
    }

    private static String request(Context.BeforeExecution context) {
        return "context=" + context.getClass().getSimpleName() + ", request=" + context.request();
    }

    private static String httpRequest(Context.AfterMarshalling context) {
        return request(context)
               + ", httpRequest=" + describe(context.httpRequest())
               + ", requestBody=" + context.requestBody().map(HookRecordingInterceptor::describe).orElse("none")
               + ", asyncRequestBody=" + (context.asyncRequestBody().isPresent() ? "present" : "none");
    }

    private static String httpResponse(Context.AfterTransmission context) {
        return httpRequest(context)
               + ", httpResponse=" + describe(context.httpResponse())
               + ", responseBody=" + (context.responseBody().isPresent() ? "present" : "none")
               + ", responsePublisher=" + (context.responsePublisher().isPresent() ? "present" : "none");
    }

    private static String response(Context.AfterUnmarshalling context) {
        return httpResponse(context) + ", response=" + describe(context.response());
    }

    private static String failure(Context.FailedExecution context) {
        return "context=" + context.getClass().getSimpleName()
               + ", exception=" + describeException(context.exception())
               + ", request=" + context.request()
               + ", httpRequest=" + context.httpRequest().map(HookRecordingInterceptor::describe).orElse("none")
               + ", httpResponse=" + context.httpResponse().map(HookRecordingInterceptor::describe).orElse("none")
               + ", response=" + context.response().map(HookRecordingInterceptor::describe).orElse("none");
    }

    private static String describe(SdkHttpRequest request) {
        if (request == null) {
            return "null";
        }
        return request.method() + " " + request.getUri()
               + " headers=" + headerNames(request.headers().keySet())
               + " amz-sdk-request=" + request.firstMatchingHeader("amz-sdk-request").orElse("none");
    }

    private static String describe(SdkHttpResponse response) {
        if (response == null) {
            return "null";
        }
        return response.statusCode() + " headers=" + headerNames(response.headers().keySet());
    }

    private static String describe(RequestBody body) {
        return "RequestBody(length=" + body.optionalContentLength().orElse(null) + ", type=" + body.contentType() + ")";
    }

    private static String describe(SdkResponse response) {
        if (response == null) {
            return "null";
        }
        String value = response instanceof TestResponse ? ((TestResponse) response).value() : "?";
        String httpResponse = response.sdkHttpResponse() == null ? "none"
                                                                 : String.valueOf(response.sdkHttpResponse().statusCode());
        return response.getClass().getSimpleName() + "(value=" + value + ", sdkHttpResponse=" + httpResponse + ")";
    }

    private static String headerNames(Set<String> names) {
        TreeSet<String> result = new TreeSet<>();
        names.forEach(n -> result.add(n.toLowerCase(Locale.ROOT)));
        return result.toString();
    }

    /**
     * One hook invocation: the hook, what its context exposed, the attribute keys present and the name of the thread it
     * ran on.
     */
    static final class HookRecord {
        private final String hook;
        private final String context;
        private final List<String> attributeKeys;
        private final String thread;

        private HookRecord(String hook, String context, List<String> attributeKeys, String thread) {
            this.hook = hook;
            this.context = context;
            this.attributeKeys = Collections.unmodifiableList(attributeKeys);
            this.thread = thread;
        }

        String thread() {
            return thread;
        }

        String hook() {
            return hook;
        }

        String context() {
            return context;
        }

        List<String> attributeKeys() {
            return attributeKeys;
        }

        @Override
        public String toString() {
            return hook + ": " + context + " " + attributeKeys;
        }
    }
}
