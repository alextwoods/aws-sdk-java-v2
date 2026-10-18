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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * One scripted sequence of HTTP exchanges, answered in order by whichever stub sends the requests: a sync or async SDK
 * HTTP client (the legacy pipeline, or the new path's default transport over it) or a sync or async
 * {@link ClientTransport} (the new path with a configured transport). The same script therefore drives the same call
 * through every pipeline.
 *
 * <p>The stub transports report I/O failures as {@code SdkClientException("Unable to execute HTTP request: <message>")}
 * with the {@link IOException} as the cause, the mapping of the SDK HTTP client transports; any other failure is
 * passed through unchanged, as a third-party transport may do.
 */
final class ScriptedExchange {
    private final Queue<Step> steps = new ConcurrentLinkedQueue<>();
    private final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<?>> pending = new CopyOnWriteArrayList<>();

    /**
     * Answers the next request with a response with the status, the body, a {@code Content-Length} and a request ID.
     */
    ScriptedExchange respond(int status, String body) {
        steps.add(new Step(StepKind.RESPOND, status, body));
        return this;
    }

    /**
     * Fails the next request with an {@link IOException} (an HTTP client) or its SDK mapping (a stub transport).
     */
    ScriptedExchange failWithIoException(String message) {
        steps.add(new Step(StepKind.IO_FAILURE, 0, message));
        return this;
    }

    /**
     * Fails the next request with an {@link IllegalStateException}, unchanged.
     */
    ScriptedExchange failWithRuntimeException(String message) {
        steps.add(new Step(StepKind.RUNTIME_FAILURE, 0, message));
        return this;
    }

    /**
     * Leaves the next asynchronous request pending; its future is in {@link #pending()}.
     */
    ScriptedExchange leavePending() {
        steps.add(new Step(StepKind.PENDING, 0, null));
        return this;
    }

    /**
     * The requests sent so far, one per attempt.
     */
    List<SdkHttpRequest> requests() {
        return requests;
    }

    /**
     * The futures of the requests left pending, in order.
     */
    List<CompletableFuture<?>> pending() {
        return pending;
    }

    SdkHttpClient httpClient() {
        return new ScriptedHttpClient();
    }

    SdkAsyncHttpClient asyncHttpClient() {
        return new ScriptedAsyncHttpClient();
    }

    ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport() {
        return new ScriptedTransport();
    }

    AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> asyncTransport() {
        return new ScriptedAsyncTransport();
    }

    private Step next(SdkHttpRequest request) {
        requests.add(request);
        Step step = steps.poll();
        if (step == null) {
            throw new IllegalStateException("No scripted exchange for request " + requests.size());
        }
        return step;
    }

    private static SdkHttpFullResponse response(Step step) {
        byte[] body = step.text.getBytes(StandardCharsets.UTF_8);
        return SdkHttpFullResponse.builder()
                                  .statusCode(step.status)
                                  .putHeader("Content-Length", String.valueOf(body.length))
                                  .putHeader("x-amzn-RequestId", "request-" + step.status)
                                  .content(AbortableInputStream.create(new ByteArrayInputStream(body)))
                                  .build();
    }

    private static SdkClientException transportFailure(IOException cause) {
        return SdkClientException.create("Unable to execute HTTP request: " + cause.getMessage(), cause);
    }

    private enum StepKind {
        RESPOND, IO_FAILURE, RUNTIME_FAILURE, PENDING
    }

    private static final class Step {
        private final StepKind kind;
        private final int status;
        private final String text;

        private Step(StepKind kind, int status, String text) {
            this.kind = kind;
            this.status = status;
            this.text = text;
        }
    }

    private final class ScriptedHttpClient implements SdkHttpClient {
        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    Step step = next(request.httpRequest());
                    switch (step.kind) {
                        case RESPOND:
                            SdkHttpFullResponse response = response(step);
                            return HttpExecuteResponse.builder()
                                                      .response(response.toBuilder().content(null).build())
                                                      .responseBody(response.content().get())
                                                      .build();
                        case IO_FAILURE:
                            throw new IOException(step.text);
                        case RUNTIME_FAILURE:
                            throw new IllegalStateException(step.text);
                        default:
                            throw new UnsupportedOperationException("Synchronous requests cannot be left pending");
                    }
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
        }
    }

    private final class ScriptedAsyncHttpClient implements SdkAsyncHttpClient {
        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            Step step = next(request.request());
            CompletableFuture<Void> result = new CompletableFuture<>();
            switch (step.kind) {
                case RESPOND:
                    SdkHttpFullResponse response = response(step);
                    byte[] body = step.text.getBytes(StandardCharsets.UTF_8);
                    request.responseHandler().onHeaders(response.toBuilder().content(null).build());
                    request.responseHandler().onStream(
                        SdkPublisher.fromIterable(Collections.singletonList(ByteBuffer.wrap(body))));
                    result.complete(null);
                    return result;
                case IO_FAILURE:
                    return fail(request, result, new IOException(step.text));
                case RUNTIME_FAILURE:
                    return fail(request, result, new IllegalStateException(step.text));
                default:
                    pending.add(result);
                    return result;
            }
        }

        private CompletableFuture<Void> fail(AsyncExecuteRequest request, CompletableFuture<Void> result,
                                             Throwable failure) {
            request.responseHandler().onError(failure);
            result.completeExceptionally(failure);
            return result;
        }

        @Override
        public void close() {
        }
    }

    private final class ScriptedTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
            Step step = next(request);
            switch (step.kind) {
                case RESPOND:
                    return response(step);
                case IO_FAILURE:
                    throw transportFailure(new IOException(step.text));
                case RUNTIME_FAILURE:
                    throw new IllegalStateException(step.text);
                default:
                    throw new UnsupportedOperationException("Synchronous requests cannot be left pending");
            }
        }

        @Override
        public void close() {
        }
    }

    private final class ScriptedAsyncTransport implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context, SdkHttpFullRequest request,
                                                           AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
            Step step = next(request);
            CompletableFuture<SdkHttpFullResponse> result = new CompletableFuture<>();
            switch (step.kind) {
                case RESPOND:
                    SdkHttpFullResponse response = response(step);
                    SdkHttpFullResponse headers = response.toBuilder().content(null).build();
                    if (streamingHandoff != null && streamingHandoff.onHeaders(headers)) {
                        // As the SDK transport hands off a streaming body: the publisher, then the headers-only response.
                        byte[] body = step.text.getBytes(StandardCharsets.UTF_8);
                        streamingHandoff.onStream(
                            SdkPublisher.fromIterable(Collections.singletonList(ByteBuffer.wrap(body))));
                        result.complete(headers);
                    } else {
                        result.complete(response);
                    }
                    break;
                case IO_FAILURE:
                    result.completeExceptionally(transportFailure(new IOException(step.text)));
                    break;
                case RUNTIME_FAILURE:
                    result.completeExceptionally(new IllegalStateException(step.text));
                    break;
                default:
                    pending.add(result);
                    break;
            }
            return result;
        }

        @Override
        public void close() {
        }
    }
}
