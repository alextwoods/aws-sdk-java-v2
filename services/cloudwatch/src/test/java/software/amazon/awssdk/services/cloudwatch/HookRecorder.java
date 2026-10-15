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

package software.amazon.awssdk.services.cloudwatch;

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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Records every {@link ExecutionInterceptor} hook with what the hook's {@link Context} type exposes and the names of the
 * {@link ExecutionAttributes} keys present at that moment. Header values are left out because some (the invocation ID,
 * the signature, the date) change per call.
 */
final class HookRecorder implements ExecutionInterceptor {
    private final List<Record> records = new CopyOnWriteArrayList<>();

    List<Record> records() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        record("beforeExecution", request(context), null, executionAttributes);
    }

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        record("modifyRequest", request(context), null, executionAttributes);
        return context.request();
    }

    @Override
    public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
        record("beforeMarshalling", request(context), null, executionAttributes);
    }

    @Override
    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        record("afterMarshalling", httpRequest(context), null, executionAttributes);
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        record("modifyHttpRequest", httpRequest(context), null, executionAttributes);
        return context.httpRequest();
    }

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {
        record("modifyHttpContent", httpRequest(context), null, executionAttributes);
        return context.requestBody();
    }

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context,
                                                             ExecutionAttributes executionAttributes) {
        record("modifyAsyncHttpContent", httpRequest(context), null, executionAttributes);
        return context.asyncRequestBody();
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        record("beforeTransmission", httpRequest(context), null, executionAttributes);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        record("afterTransmission", httpResponse(context), null, executionAttributes);
    }

    @Override
    public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
        record("modifyHttpResponse", httpResponse(context), null, executionAttributes);
        return context.httpResponse();
    }

    @Override
    public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                           ExecutionAttributes executionAttributes) {
        record("modifyAsyncHttpResponseContent", httpResponse(context), null, executionAttributes);
        return context.responsePublisher();
    }

    @Override
    public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                           ExecutionAttributes executionAttributes) {
        record("modifyHttpResponseContent", httpResponse(context), null, executionAttributes);
        return context.responseBody();
    }

    @Override
    public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
        record("beforeUnmarshalling", httpResponse(context), null, executionAttributes);
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        record("afterUnmarshalling", response(context), context.response(), executionAttributes);
    }

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        record("modifyResponse", response(context), context.response(), executionAttributes);
        return context.response();
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        record("afterExecution", response(context), context.response(), executionAttributes);
    }

    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        record("modifyException", failure(context), context.response().orElse(null), executionAttributes);
        return context.exception();
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        record("onExecutionFailure", failure(context), context.response().orElse(null), executionAttributes);
    }

    private void record(String hook, String context, SdkResponse response, ExecutionAttributes executionAttributes) {
        List<String> keys = executionAttributes.getAttributes().keySet().stream()
                                               .map(Object::toString)
                                               .sorted()
                                               .collect(Collectors.toList());
        Integer httpResponseOnOutput = response == null || response.sdkHttpResponse() == null
                                       ? null : response.sdkHttpResponse().statusCode();
        records.add(new Record(hook, context, httpResponseOnOutput, keys));
    }

    private static String request(Context.BeforeExecution context) {
        return "context=" + context.getClass().getSimpleName() + ", request=" + context.request();
    }

    private static String httpRequest(Context.AfterMarshalling context) {
        return request(context)
               + ", httpRequest=" + describe(context.httpRequest())
               + ", requestBody=" + context.requestBody().map(HookRecorder::describe).orElse("none")
               + ", asyncRequestBody=" + (context.asyncRequestBody().isPresent() ? "present" : "none");
    }

    private static String httpResponse(Context.AfterTransmission context) {
        return httpRequest(context)
               + ", httpResponse=" + describe(context.httpResponse())
               + ", responseBody=" + (context.responseBody().isPresent() ? "present" : "none")
               + ", responsePublisher=" + (context.responsePublisher().isPresent() ? "present" : "none");
    }

    private static String response(Context.AfterUnmarshalling context) {
        return httpResponse(context) + ", response=" + context.response();
    }

    private static String failure(Context.FailedExecution context) {
        return "context=" + context.getClass().getSimpleName()
               + ", exception=" + context.exception().getClass().getSimpleName()
               + ", request=" + context.request()
               + ", httpRequest=" + context.httpRequest().map(HookRecorder::describe).orElse("none")
               + ", httpResponse=" + context.httpResponse().map(HookRecorder::describe).orElse("none")
               + ", response=" + context.response().map(Object::toString).orElse("none");
    }

    private static String describe(SdkHttpRequest request) {
        return request.method() + " " + request.getUri() + " headers=" + headerNames(request.headers().keySet());
    }

    private static String describe(SdkHttpResponse response) {
        return response.statusCode() + " headers=" + headerNames(response.headers().keySet());
    }

    private static String describe(RequestBody body) {
        return "RequestBody(length=" + body.optionalContentLength().orElse(null) + ", type=" + body.contentType() + ")";
    }

    private static String headerNames(Set<String> names) {
        TreeSet<String> result = new TreeSet<>();
        names.forEach(n -> result.add(n.toLowerCase(Locale.ROOT)));
        return result.toString();
    }

    /**
     * One hook invocation: the hook, what its context exposed, the status code of the HTTP response held by the output
     * the hook sees (null when it has none) and the attribute keys present.
     */
    static final class Record {
        private final String hook;
        private final String context;
        private final Integer httpResponseOnOutput;
        private final List<String> attributeKeys;

        private Record(String hook, String context, Integer httpResponseOnOutput, List<String> attributeKeys) {
            this.hook = hook;
            this.context = context;
            this.httpResponseOnOutput = httpResponseOnOutput;
            this.attributeKeys = Collections.unmodifiableList(attributeKeys);
        }

        String hook() {
            return hook;
        }

        String context() {
            return context;
        }

        Integer httpResponseOnOutput() {
            return httpResponseOnOutput;
        }

        List<String> attributeKeys() {
            return attributeKeys;
        }

        @Override
        public String toString() {
            return hook + ": " + context + " sdkHttpResponse=" + httpResponseOnOutput + " " + attributeKeys;
        }
    }
}
