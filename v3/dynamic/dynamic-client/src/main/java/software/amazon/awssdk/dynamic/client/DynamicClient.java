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
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;

/**
 * A synchronous model-driven client of one service of a runtime Smithy model (Requirements 14.2-14.8, 14.11, 14.14,
 * 14.15, 14.19): the dynamic counterpart of a generated {@code <Service>Client}.
 *
 * <p>A dynamic call runs on the same pipeline as a generated call. The builder resolves the client's configuration
 * through the same {@code AwsClientBuildState} into the same {@code ClientRuntime} and {@code AwsCallEngine}; each
 * operation gets the same kind of {@code AwsOperation} binding a generated client holds (descriptor, schemas,
 * endpoint binding, modeled auth, {@code httpChecksum}), and its {@code PipelinePlan} is built once per client by the
 * runtime; and {@link #call} is one {@code AwsCallEngine.executeSync} call with that plan. Configuration, request
 * overrides, interceptors, feature plugins, Modern Auth, endpoint rules, retries and error mapping are therefore the
 * generated client's.
 *
 * <p>Finite inputs and outputs are Smithy Java {@link Document}s. Before anything of a call runs (no interceptor,
 * retry token or transmission), the operation is resolved by its exact modeled name and the input is validated
 * against the operation's exact input schema; an invalid call fails with an error identifying the invalid element.
 *
 * <p>Streams use the native types generated clients use, never buffered into documents (Requirement 14.12): a
 * streaming blob input is a {@link RequestBody} and a streaming blob output is consumed by a
 * {@link ResponseTransformer}, through the engine's streaming entry points; an event stream is the Smithy Java
 * {@code EventStream} a generated input or output holds, as an event stream document member of the input (a writer,
 * see {@link #inputEvent}) or output (a reader). Paginated operations have {@linkplain #paginator lazy pages} and
 * modeled waiters a {@linkplain #waiter() waiter}, on the sdk-core paginator and waiter runtime generated paginators
 * and waiters use (Requirement 14.15).
 *
 * <pre>{@code
 * try (DynamicClient client = DynamicClient.builder()
 *         .model(Path.of("weather.json"))
 *         .serviceId(ShapeId.from("example.weather#Weather"))
 *         .region(Region.US_WEST_2)
 *         .build()) {
 *     Document forecast = client.call("GetForecast", Document.of(Map.of("city", Document.of("Seattle"))));
 * }
 * }</pre>
 *
 * <p>Thread safe; share one client per service and configuration. {@link #close()} releases the resources the SDK
 * created for the client.
 */
public final class DynamicClient implements AutoCloseable {
    private final DynamicCalls calls;

    private DynamicClient(DynamicCalls calls) {
        this.calls = calls;
    }

    /** A builder of a dynamic client. */
    public static Builder builder() {
        return new Builder();
    }

    /** The selected service: its ID, schemas and operations by exact name. */
    public DynamicService service() {
        return calls.service();
    }

    /**
     * Invokes the operation with exactly the modeled name {@code operationName} and returns its modeled output.
     *
     * @see #call(String, Document, AwsRequestOverrideConfiguration)
     */
    public Document call(String operationName, Document input) {
        return call(operationName, input, (AwsRequestOverrideConfiguration) null);
    }

    /**
     * Invokes the operation with exactly the modeled name {@code operationName} on the calling thread, with an
     * explicit per-call override, and returns its modeled output: a document of the operation's exact output schema.
     *
     * @param operationName the exact (case-sensitive) modeled operation name
     * @param input the operation's input: a map or structure document conforming to the exact input schema; a
     *        document of a generated input type works too
     * @param override the call's override configuration, or {@code null}
     * @return the modeled output
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException naming the service, the unknown name and
     *         every operation name in lexical order, if the service has no such operation; nothing was sent
     * @throws DynamicInvocationException if the input does not conform, naming every invalid element, or the
     *         operation needs a streaming call; nothing was sent
     * @throws DynamicServiceException for a modeled error of the service
     * @throws software.amazon.awssdk.core.exception.SdkException the call's other mapped terminal failure, exactly as
     *         a generated client reports it
     */
    public Document call(String operationName, Document input, AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        DynamicCalls.Call call = calls.prepare(operationName, input);
        return calls.engine().executeSync(call.plan(), call.input(), override, null);
    }

    /**
     * Invokes the streaming-blob-input operation {@code operationName} with its request body.
     *
     * @see #call(String, Document, RequestBody, AwsRequestOverrideConfiguration)
     */
    public Document call(String operationName, Document input, RequestBody requestBody) {
        return call(operationName, input, requestBody, (AwsRequestOverrideConfiguration) null);
    }

    /**
     * Invokes the operation {@code operationName}, whose input has a streaming blob member, on the calling thread,
     * with {@code requestBody} as that member, as the generated request-body overload does: the body is read only as
     * the transport sends it, and a call whose body is not replayable is not retried after transmission started.
     *
     * @param input the operation's input document without its streaming blob member
     * @param requestBody the streaming blob member's body
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#NO_STREAMING_MEMBER}) if the
     *         operation has no streaming blob input member, ({@link DynamicInvocationException.Reason
     *         #REQUIRES_STREAMING_CALL}) if its output has one, or for an invalid input; nothing was sent
     * @see #call(String, Document, AwsRequestOverrideConfiguration) for the other failures
     */
    public Document call(String operationName, Document input, RequestBody requestBody,
                         AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(requestBody, "requestBody");
        DynamicCalls.Call call = calls.prepare(operationName, input, true, false);
        return calls.engine().executeSync(call.plan(), call.input(), override, requestBody);
    }

    /**
     * Invokes the streaming-blob-output operation {@code operationName}, handing its streaming response body to
     * {@code responseTransformer}.
     *
     * @see #call(String, Document, RequestBody, AwsRequestOverrideConfiguration, ResponseTransformer)
     */
    public <R> R call(String operationName, Document input, ResponseTransformer<Document, R> responseTransformer) {
        return call(operationName, input, null, null, responseTransformer);
    }

    /**
     * Invokes the operation {@code operationName}, whose output has a streaming blob member, on the calling thread,
     * and returns the result of {@code responseTransformer}, as the generated response-transformer overload does:
     * the transformer receives the output document (without the streaming member) and the live payload stream,
     * never buffered by the SDK; once it starts the call is never retried; the exchange is released when it
     * returns, unless it {@linkplain ResponseTransformer#needsConnectionLeftOpen() keeps the connection open}, in
     * which case closing the stream releases it.
     *
     * @param input the operation's input document, without a streaming blob input member
     * @param requestBody the streaming blob input member's body, if the input has one, else {@code null}
     * @param override the call's override configuration, or {@code null}
     * @param responseTransformer consumes the payload and produces the result
     * @return the transformer's result
     * @throws DynamicInvocationException if the streaming arguments do not match the operation's streaming blob
     *         members or the input does not conform; nothing was sent
     * @see #call(String, Document, AwsRequestOverrideConfiguration) for the other failures
     */
    @SuppressWarnings("unchecked")
    public <R> R call(String operationName, Document input, RequestBody requestBody,
                      AwsRequestOverrideConfiguration override, ResponseTransformer<Document, R> responseTransformer) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(responseTransformer, "responseTransformer");
        DynamicCalls.Call call = calls.prepare(operationName, input, requestBody != null, true);
        // The transformer only consumes the output, so one of Documents accepts the StructDocument output.
        ResponseTransformer<StructDocument, R> transformer =
            (ResponseTransformer<StructDocument, R>) (ResponseTransformer<?, R>) responseTransformer;
        return calls.engine().executeSync(call.plan(), call.input(), override, requestBody, transformer);
    }

    /**
     * The pages of the paginated operation {@code operationName} starting from {@code input}.
     *
     * @see #paginator(String, Document, AwsRequestOverrideConfiguration)
     */
    public DynamicPages paginator(String operationName, Document input) {
        return paginator(operationName, input, null);
    }

    /**
     * The lazily fetched pages of the paginated operation {@code operationName} starting from {@code input}, each
     * page call with {@code override}, following the modeled output token; nothing is sent until the first pull.
     *
     * @param operationName the exact modeled name of an operation with modeled input and output tokens
     * @param input the first page's input document; validated now
     * @param override the override configuration of every page call, or {@code null}
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException for an unknown or unpaginated operation,
     *         listing every operation or paginated operation name in lexical order
     * @throws DynamicInvocationException if the input does not conform or the operation needs a streaming call
     */
    public DynamicPages paginator(String operationName, Document input, AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(input, "input");
        return new DynamicPages(calls, calls.paginate(operationName, input), override);
    }

    /** A waiter of this client's service's modeled waiters that polls this client; it borrows the client. */
    public DynamicWaiter waiter() {
        return DynamicWaiter.builder().client(this).build();
    }

    /**
     * {@code event} as one event to write to the input event stream of the operation {@code operationName}: the
     * event stream member of an input document is {@code Document.ofObject(writer)} of a Smithy Java
     * {@code EventStream.newWriter()}, and each written event is a document of the event stream union, which this
     * validates and converts.
     *
     * @param event a map or structure document with exactly one member of the event stream union
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
        return "DynamicClient[" + calls.service().id() + "]";
    }

    /** Builder of a {@link DynamicClient}; not thread safe. */
    public static final class Builder extends DynamicClientBuilder<Builder, DynamicClient> {
        private Builder() {
        }

        /** A built HTTP client: caller-owned, never closed by the dynamic client. */
        public Builder httpClient(SdkSyncHttpClient httpClient) {
            state.httpClient(httpClient);
            return this;
        }

        @Override
        public DynamicClient build() {
            DynamicServiceBinding binding = resolve();
            return new DynamicClient(new DynamicCalls(binding, state.buildSync(binding.definition())));
        }
    }
}
