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

package software.amazon.awssdk.dynamic.client;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;

/**
 * An asynchronous model-driven client of one service of a runtime Smithy model (Requirements 14.2-14.7, 14.9-14.11,
 * 14.14, 14.15, 14.19): the dynamic counterpart of a generated {@code <Service>AsyncClient}, on the same
 * {@code AwsClientBuildState}, {@code ClientRuntime}, plans and {@code AwsCallEngine} (see {@link DynamicClient}).
 *
 * <p>{@link #callAsync} returns its {@code CompletableFuture} before the call completes: it is the engine's own call
 * future, so it completes exactly once, on the client's completion executor, with the modeled output document or the
 * call's SDK exception, and cancelling it cancels the call as for a generated async call. A call rejected before
 * anything of it runs (an unknown operation name, streaming arguments that do not match the operation's streaming
 * blob members, or an input that does not conform to the operation's input schema) returns an already failed future
 * with that error; nothing is sent.
 *
 * <p>Streams, paginators and waiters are those of {@link DynamicClient}, in their async forms: an
 * {@link AsyncRequestBody} and an {@link AsyncResponseTransformer} through the engine's async streaming entry points
 * (the request body is pulled under the transport's demand and the transformer's subscription drives the response's
 * demand), an {@link EventStreamResponseHandler} for an output event stream (decoded only under its subscriber's
 * demand, never buffered into the output document, so the handler overload is the only async call of such an
 * operation), a demand-driven {@link DynamicPagePublisher} and a non-blocking {@link DynamicAsyncWaiter}. Every
 * future and subscription they return is, or cancels, the engine's own call future, so cancellation propagates
 * through the shared pipeline exactly as for generated async calls (Requirements 14.12, 14.13, 14.15).
 *
 * <p>Thread safe; share one client per service and configuration. {@link #close()} releases the resources the SDK
 * created for the client.
 */
public final class DynamicAsyncClient implements AutoCloseable {
    private final DynamicCalls calls;

    private DynamicAsyncClient(DynamicCalls calls) {
        this.calls = calls;
    }

    /** A builder of a dynamic async client. */
    public static Builder builder() {
        return new Builder();
    }

    /** The selected service: its ID, schemas and operations by exact name. */
    public DynamicService service() {
        return calls.service();
    }

    /**
     * Starts the operation with exactly the modeled name {@code operationName}.
     *
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration)
     */
    public CompletableFuture<Document> callAsync(String operationName, Document input) {
        return callAsync(operationName, input, (AwsRequestOverrideConfiguration) null);
    }

    /**
     * Starts the operation with exactly the modeled name {@code operationName}, with an explicit per-call override,
     * and returns the future of its modeled output before the call completes.
     *
     * @param operationName the exact (case-sensitive) modeled operation name
     * @param input the operation's input: a map or structure document conforming to the exact input schema; a
     *        document of a generated input type works too
     * @param override the call's override configuration, or {@code null}
     * @return the future of the modeled output: a document of the operation's exact output schema. It fails with a
     *     {@link software.amazon.awssdk.dynamic.model.DynamicModelException} for an unknown operation name, a
     *     {@link DynamicInvocationException} for an operation that needs a streaming call (including one whose
     *     output has an event stream, which needs the {@link EventStreamResponseHandler} overload) or a
     *     nonconforming input (in these cases nothing was sent), a {@link DynamicServiceException} for a modeled
     *     error of the service, or the call's other mapped terminal {@link SdkException}, exactly as a generated
     *     async client reports it
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Document> callAsync(String operationName, Document input,
                                                 AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        DynamicCalls.Call call;
        try {
            call = calls.prepare(operationName, input);
            call.operation().requireNoAsyncOutputEventStream();
        } catch (SdkException rejected) {
            return CompletableFuture.failedFuture(rejected);
        }
        CompletableFuture<StructDocument> future = calls.engine().executeAsync(call.plan(), call.input(), override,
                                                                               null);
        // The engine's call future itself, not a dependent stage, so cancelling it cancels the call. Its values are
        // StructDocuments, which are Documents.
        return (CompletableFuture<Document>) (CompletableFuture<?>) future;
    }

    /**
     * Starts the streaming-blob-input operation {@code operationName} with its request body.
     *
     * @see #callAsync(String, Document, AsyncRequestBody, AwsRequestOverrideConfiguration)
     */
    public CompletableFuture<Document> callAsync(String operationName, Document input, AsyncRequestBody requestBody) {
        return callAsync(operationName, input, requestBody, (AwsRequestOverrideConfiguration) null);
    }

    /**
     * Starts the operation {@code operationName}, whose input has a streaming blob member, with
     * {@code requestBody} as that member, as the generated request-body overload does: the body is subscribed to
     * and pulled only under the transport's demand, and cancelling the returned future (the engine's own call
     * future) cancels the body subscription and the exchange.
     *
     * @param input the operation's input document without its streaming blob member
     * @param requestBody the streaming blob member's body
     * @return the future of the modeled output; already failed with a {@link DynamicInvocationException}, with
     *     nothing sent, if the operation has no streaming blob input member, its output has one, or the input does
     *     not conform
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration) for the other outcomes
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Document> callAsync(String operationName, Document input, AsyncRequestBody requestBody,
                                                 AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(requestBody, "requestBody");
        DynamicCalls.Call call;
        try {
            call = calls.prepare(operationName, input, true, false);
            call.operation().requireNoAsyncOutputEventStream();
        } catch (SdkException rejected) {
            return CompletableFuture.failedFuture(rejected);
        }
        return (CompletableFuture<Document>) (CompletableFuture<?>) calls.engine().executeAsync(
            call.plan(), call.input(), override, requestBody);
    }

    /**
     * Starts the streaming-blob-output operation {@code operationName}, handing its streaming response body to
     * {@code responseTransformer}.
     *
     * @see #callAsync(String, Document, AsyncRequestBody, AwsRequestOverrideConfiguration, AsyncResponseTransformer)
     */
    public <R> CompletableFuture<R> callAsync(String operationName, Document input,
                                              AsyncResponseTransformer<Document, R> responseTransformer) {
        return callAsync(operationName, input, null, null, responseTransformer);
    }

    /**
     * Starts the operation {@code operationName}, whose output has a streaming blob member, and returns the future
     * of {@code responseTransformer}'s result before the call completes, as the generated response-transformer
     * overload does: the transformer gets {@code prepare}, {@code onResponse} with the output document (without the
     * streaming member) and {@code onStream} with the live payload on the client's completion executor; its own
     * subscription drives the transport's demand and nothing is buffered. Once it has the response the call is
     * never retried. Cancelling the returned future (the engine's own call future) cancels the call, the exchange
     * and the payload subscription, and the transformer is told through {@code exceptionOccurred}.
     *
     * @param input the operation's input document, without a streaming blob input member
     * @param requestBody the streaming blob input member's body, if the input has one, else {@code null}
     * @param override the call's override configuration, or {@code null}
     * @param responseTransformer consumes the payload and produces the result
     * @return the future of the transformer's result; already failed with a {@link DynamicInvocationException},
     *     with nothing sent, if the streaming arguments do not match the operation's streaming blob members or the
     *     input does not conform
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration) for the other outcomes
     */
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> callAsync(String operationName, Document input, AsyncRequestBody requestBody,
                                              AwsRequestOverrideConfiguration override,
                                              AsyncResponseTransformer<Document, R> responseTransformer) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(responseTransformer, "responseTransformer");
        DynamicCalls.Call call;
        try {
            call = calls.prepare(operationName, input, requestBody != null, true);
        } catch (SdkException rejected) {
            return CompletableFuture.failedFuture(rejected);
        }
        // The transformer only consumes the output, so one of Documents accepts the StructDocument output.
        AsyncResponseTransformer<StructDocument, R> transformer =
            (AsyncResponseTransformer<StructDocument, R>) (AsyncResponseTransformer<?, R>) responseTransformer;
        return calls.engine().executeAsync(call.plan(), call.input(), override, requestBody, transformer);
    }

    /**
     * Starts the output-event-stream operation {@code operationName}, handing its output and events to
     * {@code handler}.
     *
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration, EventStreamResponseHandler)
     */
    public CompletableFuture<Void> callAsync(String operationName, Document input,
                                             EventStreamResponseHandler<Document, Document> handler) {
        return callAsync(operationName, input, (AwsRequestOverrideConfiguration) null, handler);
    }

    /**
     * Starts the operation {@code operationName}, whose output has an event stream member, as the generated
     * event-stream overload does, through the engine's event stream entry point: {@code handler} receives the output
     * document (without its event stream member) once, as soon as the response headers, or for an RPC protocol the
     * {@code initial-response} event, arrive, and then the publisher of the event documents, which decodes events
     * from the live response body only as its subscriber requests them; nothing is buffered beyond one body chunk
     * and one partial message. The call then ends exactly once: the subscriber's {@code onComplete} and
     * {@code handler.complete()}, or the call's mapped failure (including a modeled exception event) to the
     * subscriber's {@code onError} and {@code handler.exceptionOccurred}. Once the handler has the output the call is
     * never retried. Cancelling the event subscription or the returned future (the engine's own call future)
     * cancels the call and the exchange.
     *
     * <p>{@link #callAsync(String, Document, AwsRequestOverrideConfiguration)} of such an operation still returns
     * the output document with an event stream reader, after the whole response has arrived.
     *
     * @param input the operation's input document
     * @param override the call's override configuration, or {@code null}
     * @param handler receives the output document and the event stream
     * @return the future of the call's end, completed after the event stream completes; already failed with a
     *     {@link DynamicInvocationException}, with nothing sent, if the operation has no output event stream or the
     *     input does not conform
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration) for the other outcomes
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> callAsync(String operationName, Document input,
                                             AwsRequestOverrideConfiguration override,
                                             EventStreamResponseHandler<Document, Document> handler) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(handler, "handler");
        DynamicCalls.Call call;
        try {
            call = calls.prepare(operationName, input);
            call.operation().requireOutputEventStream();
        } catch (SdkException rejected) {
            return CompletableFuture.failedFuture(rejected);
        }
        // The handler only consumes the output and the events, so one of Documents accepts StructDocuments.
        EventStreamResponseHandler<StructDocument, Document> documents =
            (EventStreamResponseHandler<StructDocument, Document>) (EventStreamResponseHandler<?, Document>) handler;
        return calls.engine().executeAsync(call.plan(), call.input(), override, null, documents);
    }

    /**
     * The pages of the paginated operation {@code operationName} starting from {@code input}.
     *
     * @see #paginator(String, Document, AwsRequestOverrideConfiguration)
     */
    public DynamicPagePublisher paginator(String operationName, Document input) {
        return paginator(operationName, input, null);
    }

    /**
     * The demand-driven pages of the paginated operation {@code operationName} starting from {@code input}, each
     * page call with {@code override}, following the modeled output token; nothing is sent before a subscriber
     * requests a page.
     *
     * @param operationName the exact modeled name of an operation with modeled input and output tokens
     * @param input the first page's input document; validated now
     * @param override the override configuration of every page call, or {@code null}
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException for an unknown or unpaginated operation,
     *         listing every operation or paginated operation name in lexical order
     * @throws DynamicInvocationException if the input does not conform or the operation needs a streaming call
     */
    public DynamicPagePublisher paginator(String operationName, Document input,
                                          AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        return new DynamicPagePublisher(calls, calls.paginate(operationName, input), override);
    }

    /**
     * A waiter of this client's service's modeled waiters that polls this client and schedules its delays on this
     * client's retry scheduler, as a generated async client's {@code waiter()} does; it borrows both.
     */
    public DynamicAsyncWaiter waiter() {
        return DynamicAsyncWaiter.builder().client(this).build();
    }

    /**
     * {@code event} as one event to write to the input event stream of the operation {@code operationName}; see
     * {@link DynamicClient#inputEvent}.
     *
     * @throws DynamicInvocationException if the operation has no input event stream or the event does not conform
     */
    public StructDocument inputEvent(String operationName, Document event) {
        Objects.requireNonNull(operationName, "operationName");
        return calls.inputEvent(operationName, event);
    }

    DynamicCalls calls() {
        return calls;
    }

    /** The exact, immutable configuration resolved for this client and read by its calls. */
    public SdkClientConfiguration serviceClientConfiguration() {
        return calls.configuration();
    }

    /** Closes the resources the SDK created for this client; idempotent. */
    @Override
    public void close() {
        calls.close();
    }

    @Override
    public String toString() {
        return "DynamicAsyncClient[" + calls.service().id() + "]";
    }

    /** Builder of a {@link DynamicAsyncClient}; not thread safe. */
    public static final class Builder extends DynamicClientBuilder<Builder, DynamicAsyncClient> {
        private Builder() {
        }

        /** A built async HTTP client: caller-owned, never closed by the dynamic client. */
        public Builder httpClient(SdkAsyncHttpClient httpClient) {
            state.asyncHttpClient(httpClient);
            return this;
        }

        /** The executor SDK-owned continuations run on: caller-owned. Without one, the SDK creates and owns one. */
        public Builder asyncCompletionExecutor(Executor executor) {
            state.completionExecutor(executor);
            return this;
        }

        @Override
        public DynamicAsyncClient build() {
            DynamicServiceBinding binding = resolve();
            return new DynamicAsyncClient(new DynamicCalls(binding, state.buildAsync(binding.definition())));
        }
    }
}
