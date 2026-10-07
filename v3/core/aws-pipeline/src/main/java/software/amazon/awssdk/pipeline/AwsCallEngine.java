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

package software.amazon.awssdk.pipeline;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.RequestBodyAdapter;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.protocols.core.ProtocolTransportRequirements;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The one production invocation engine of the AWS pipeline (design "Plan and drivers"; Requirements 10.1, 10.4,
 * 10.5): a generated or dynamic client hands it an operation's shared {@link PipelinePlan} and the call's input, and
 * it runs the call through the plan's {@link PipelineStage}s to its terminal outcome.
 *
 * <p>Sync and async calls are two drivers over the same package-private stage functions ({@code CallStages}), not
 * two semantic pipelines: every validation, idempotency, retry-token, endpoint, auth, serialization, feature,
 * signing, checksum, deserialization, classification and exception-mapping decision is made by the same code. The
 * drivers differ only in transport mechanics: how identity is awaited, which transport transmits, and how retry
 * delays are honored. {@link #executeSync} is a direct loop that blocks only on identity resolution, the sync
 * transport and retry delays. {@link #executeAsync} drives one compact per-call state machine that never blocks and
 * continues on the runtime's completion executor and retry scheduler. There is no fallback to any other execution
 * path.
 *
 * <p>One engine per client, over the client's {@link ClientRuntime}; it accepts only that runtime's plans. The
 * engine does not own its transport: the runtime's resource scope closes SDK-owned transports. Thread safe; holds no
 * per-call state.
 */
public final class AwsCallEngine {
    private final ClientRuntime runtime;
    private final SdkSyncHttpClient syncHttpClient;
    private final SdkAsyncHttpClient asyncHttpClient;
    private final Executor completionExecutor;
    private final RetryScheduler retryScheduler;
    private final CallStages stages;
    private final boolean signingOnly;

    private AwsCallEngine(Builder builder) {
        this.runtime = Objects.requireNonNull(builder.runtime, "runtime");
        this.syncHttpClient = builder.syncHttpClient;
        this.asyncHttpClient = builder.asyncHttpClient;
        this.signingOnly = builder.signingOnly;
        if (!signingOnly) {
            rejectIncompatibleTransport(runtime, syncHttpClient, asyncHttpClient);
        }
        if (signingOnly) {
            if (syncHttpClient != null || asyncHttpClient != null) {
                throw new IllegalArgumentException("A signing-only engine has no transport");
            }
            this.completionExecutor = null;
            this.retryScheduler = null;
        } else if (runtime.transportMode() == HttpTransportMode.SYNC) {
            if (syncHttpClient == null) {
                throw new NullPointerException("A sync client runtime requires a syncHttpClient");
            }
            this.completionExecutor = null;
            this.retryScheduler = null;
        } else {
            if (asyncHttpClient == null) {
                throw new NullPointerException("An async client runtime requires an asyncHttpClient");
            }
            this.completionExecutor = runtime.completionExecutor().orElseThrow(
                () -> new NullPointerException("An async client runtime requires a completionExecutor"));
            this.retryScheduler = runtime.retryScheduler().orElseThrow(
                () -> new NullPointerException("An async client runtime requires a retryScheduler"));
        }
        // The transport's name is the engine-constant transport fact of every response and surfaced exception.
        String httpClientName = syncHttpClient != null ? syncHttpClient.clientName()
                                : asyncHttpClient != null ? asyncHttpClient.clientName() : null;
        this.stages = new CallStages(runtime, builder.identityProviders, builder.authOptions, builder.stageObserver,
                                     httpClientName);
    }

    /**
     * Fails construction, before the engine and so the client exist, if the transport cannot carry the runtime's
     * protocol (design "PipelinePlan"; Requirement 10.13): a transport of the other mode only, or, when the runtime
     * has {@link ClientRuntime#transportRequirements() transport requirements}, a transport whose capabilities declare
     * another mode or none of the HTTP versions the protocol (and its event-stream operations, if any) can use. The
     * failure names the protocol and the transport. A rejected runtime is closed, so every SDK-owned resource
     * acquired for the client is released and caller-owned ones stay open; nothing has been transmitted.
     */
    private static void rejectIncompatibleTransport(ClientRuntime runtime, SdkSyncHttpClient sync,
                                                    SdkAsyncHttpClient async) {
        HttpTransportMode mode = runtime.transportMode();
        boolean syncMode = mode == HttpTransportMode.SYNC;
        ShapeId protocolId = runtime.protocol().protocolId();
        List<String> reasons = new ArrayList<>();
        if (syncMode ? sync == null && async != null : async == null && sync != null) {
            String other = syncMode ? async.clientName() : sync.clientName();
            reasons.add("Protocol " + protocolId + " of a " + mode + " client cannot use transport " + other
                        + ", which is a " + (syncMode ? HttpTransportMode.ASYNC : HttpTransportMode.SYNC)
                        + " transport");
        } else if (runtime.transportRequirements().isPresent() && (syncMode ? sync : async) != null) {
            ProtocolTransportRequirements requirements = runtime.transportRequirements().get();
            String name = syncMode ? sync.clientName() : async.clientName();
            HttpClientCapabilities capabilities = syncMode ? sync.capabilities() : async.capabilities();
            if (capabilities.mode() != mode) {
                reasons.add("Protocol " + protocolId + " of a " + mode + " client cannot use transport " + name
                            + ", whose capabilities declare mode " + capabilities.mode());
            } else {
                reasons.addAll(requirements.incompatibilities(name, ProtocolTransportRequirements.Message.HTTP,
                                                              alpnIds(capabilities.protocols()),
                                                              runtime.eventStreamOperations()));
            }
        }
        if (reasons.isEmpty()) {
            return;
        }
        SdkClientConfigurationException failure = SdkClientConfigurationException.create(
            "The transport cannot carry the client's protocol: " + String.join("; ", reasons));
        try {
            runtime.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
        throw failure;
    }

    /** The ALPN protocol IDs of {@code protocols}, as protocol traits name HTTP versions. */
    private static Set<String> alpnIds(Set<Protocol> protocols) {
        Set<String> ids = new HashSet<>();
        for (Protocol protocol : protocols) {
            ids.add(switch (protocol) {
                case HTTP1_1 -> ProtocolTransportRequirements.HTTP_1_1;
                case HTTP2 -> ProtocolTransportRequirements.HTTP_2;
            });
        }
        return ids;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The runtime of the client this engine executes calls for. */
    public ClientRuntime runtime() {
        return runtime;
    }

    /**
     * Executes one call of {@code plan} on the calling thread and returns its output.
     *
     * <p>The loop: validation and override merging, the idempotency token (once per call), the initial retry token;
     * then per attempt endpoint, auth scheme and identity, serialization, request features, signing, transmission,
     * response checksum validation and deserialization. A failed attempt is classified once; a granted retry token
     * permits exactly one further attempt after its delay, and otherwise the failure takes the one exception-mapping
     * path. Interrupting the calling thread while it waits for an identity or a delay cancels the call.
     *
     * <p>For an operation whose output has an event stream member, the output's event stream reads the live body
     * one message at a time after this returns; the exchange stays open until the caller drains the stream to its end
     * or closes it, which releases the exchange. Once returned, the call is never retried.
     *
     * @param plan the operation's plan, from this engine's runtime
     * @param input the operation's input
     * @param override the call's explicit override configuration, or {@code null}
     * @param inputBody the operation's streaming input body, or {@code null} for an operation without one
     * @return the modeled output
     * @throws IllegalArgumentException if the plan belongs to another client or the input is not the operation's
     * @throws IllegalStateException if the client executes through an async transport
     * @throws software.amazon.awssdk.core.exception.SdkException the call's mapped terminal failure
     */
    @SuppressWarnings("unchecked")
    public <I extends SerializableStruct, O extends SerializableStruct> O executeSync(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override, RequestBody inputBody) {
        requireSync();
        CallState<I, O> call = stages.begin(plan, input, override);
        HttpBody body = inputBody == null ? null : RequestBodyAdapter.toHttpBody(inputBody);
        // Without a transformer the call's result is its output.
        return (O) runSync(call, body, null);
    }

    /**
     * Executes one call of a streaming-output {@code plan} on the calling thread, handing the live response payload
     * to {@code responseTransformer}, and returns the transformer's result.
     *
     * <p>The same loop as {@link #executeSync(PipelinePlan, SerializableStruct, AwsRequestOverrideConfiguration,
     * RequestBody)} up to the response: an error response is still read in full, classified and possibly retried. A
     * successful response is deserialized without reading its payload (the protocol binds the streaming blob member
     * to the live body), and the transformer receives that output and a stream of the payload, validated while it
     * is read if response checksum validation applies. Once the transformer starts, the call is never retried: its
     * failure, or any later one, takes the call's one exception-mapping path. The exchange is closed after the
     * transformer returns unless {@link ResponseTransformer#needsConnectionLeftOpen()}, in which case closing the
     * payload stream releases it.
     *
     * @param plan the plan of an operation whose output has a streaming blob member
     * @param input the operation's input
     * @param override the call's explicit override configuration, or {@code null}
     * @param inputBody the operation's streaming input body, or {@code null} for an operation without one
     * @param responseTransformer consumes the payload and produces the result
     * @return the transformer's result
     * @throws IllegalArgumentException if the plan belongs to another client, the input is not the operation's, or
     *     the operation has no streaming blob output member
     * @throws IllegalStateException if the client executes through an async transport
     * @throws software.amazon.awssdk.core.exception.SdkException the call's mapped terminal failure
     */
    @SuppressWarnings("unchecked")
    public <I extends SerializableStruct, O extends SerializableStruct, R> R executeSync(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override, RequestBody inputBody,
        ResponseTransformer<O, R> responseTransformer) {
        requireSync();
        Objects.requireNonNull(responseTransformer, "responseTransformer");
        requireStreamingOutput(plan);
        CallState<I, O> call = stages.begin(plan, input, override);
        HttpBody body = inputBody == null ? null : RequestBodyAdapter.toHttpBody(inputBody);
        return (R) runSync(call, body, responseTransformer);
    }

    private void requireSync() {
        requireTransport();
        if (runtime.transportMode() != HttpTransportMode.SYNC) {
            throw new IllegalStateException("The client executes through an " + runtime.transportMode()
                                            + " transport; executeSync requires a sync transport");
        }
    }

    /**
     * The sync loop of one begun call: its output, or with a {@code transformer} the transformer's result.
     */
    private <I extends SerializableStruct, O extends SerializableStruct> Object runSync(
        CallState<I, O> call, HttpBody body, ResponseTransformer<O, ?> transformer) {
        RetryToken token;
        try {
            AcquireInitialTokenResponse initial = stages.beforeAttempts(call);
            sleep(initial.delay());
            token = initial.token();
        } catch (Throwable failure) {
            throw terminal(call, failure);
        }
        while (true) {
            AttemptState attempt;
            try {
                attempt = stages.startAttempt(call, token);
            } catch (Throwable failure) {
                throw terminal(call, failure);
            }
            boolean eventStreamOutput = transformer == null && hasOutputEventStream(call.plan());
            boolean transmitted = false;
            boolean transforming = false;
            boolean leaveOpen = false;
            SyncHttpExchange exchange = null;
            RetryDecision decision;
            Throwable failure;
            try {
                stages.resolveEndpoint(call, attempt);
                SelectedAuthScheme<?> scheme = stages.selectAuthScheme(call, attempt);
                if (scheme != null) {
                    stages.identityResolved(attempt, await(call, stages.resolveIdentity(call, attempt, scheme)));
                }
                stages.serialize(call, attempt, body);
                stages.requestFeatures(call, attempt);
                SdkHttpRequest request = stages.sign(call, attempt);
                stages.transmit(call, attempt);
                exchange = syncHttpClient.prepare(request, HttpExecutionOptions.defaults());
                SyncHttpExchange active = exchange;
                attempt.cancellation().register(reason -> active.abort());
                transmitted = true;
                SdkHttpResponse response = stages.responseReceived(call, attempt, exchange.execute());
                if (transformer == null) {
                    boolean holdOpen = eventStreamOutput && response.body().kind() == HttpBody.Kind.STREAMING
                                       && !call.plan().protocol().isError(response);
                    if (!holdOpen) {
                        return stages.succeed(call, attempt, stages.deserialize(call, attempt, response));
                    }
                    // The output's event stream reads the live body after the call returns: the exchange stays open
                    // until the caller drains or closes the stream, which releases it.
                    O output = stages.deserialize(call, attempt, releasingOnEnd(response, exchange));
                    O succeeded = stages.succeed(call, attempt, output);
                    leaveOpen = true;
                    return succeeded;
                }
                O output = stages.deserializeStreamingOutput(call, attempt, response);
                // From here the payload is being consumed, so no failure is retried.
                transforming = true;
                boolean keepOpen = transformer.needsConnectionLeftOpen();
                Object result = transformer.transform(output, payload(response, keepOpen ? exchange : null));
                stages.succeed(call, attempt, output);
                leaveOpen = keepOpen;
                return result;
            } catch (Throwable attemptFailure) {
                failure = attemptFailure;
                if (transforming || failure instanceof CallInterruptedException || failure instanceof Error) {
                    // An interrupted wait, an Error, or a failure once the payload is being consumed is terminal,
                    // not an attempt failure to classify.
                    throw terminal(call, failure);
                }
                try {
                    decision = stages.classify(call, attempt, failure, transmitted);
                    // The failure the attempt's hooks left, which the classifier received.
                    failure = attempt.failure().orElse(failure);
                } catch (Throwable classification) {
                    classification.addSuppressed(failure);
                    throw stages.fail(call, classification);
                }
            } finally {
                if (exchange != null && !leaveOpen) {
                    exchange.close();
                }
            }
            try {
                sleep(decision.delay());
            } catch (Throwable delayFailure) {
                throw terminal(call, delayFailure);
            }
            if (!(decision instanceof RetryDecision.Retry retry)) {
                throw stages.fail(call, failure);
            }
            // The granted token authorizes exactly one more attempt.
            token = retry.token();
        }
    }

    /**
     * Starts one call of {@code plan} and returns its future before the call completes.
     *
     * <p>The same stages as {@link #executeSync}, in the same order, driven by one per-call state machine instead of a
     * loop: it waits for identities, the async transport and retry delays without blocking any thread, and runs every
     * stage and completes the future on the runtime's completion executor, never on a transport event loop or the
     * common pool. Each granted retry token authorizes exactly one further transmission. Cancelling the returned
     * future succeeds only before the call reaches another outcome, and then cancels the call: the identity wait,
     * active exchange, body subscription or pending delay is cancelled once, the unconsumed retry token is released,
     * and no further stage, hook, attempt or transmission runs. The future always reports the call's one outcome.
     *
     * <p>This driver reads a successful response in full before deserializing it, so it does not accept an operation
     * whose output has an event stream: an event stream is never buffered (Requirements 5.11, 14.12), and such an
     * operation is called with an {@link EventStreamResponseHandler} through
     * {@link #executeAsync(PipelinePlan, SerializableStruct, AwsRequestOverrideConfiguration, AsyncRequestBody,
     * EventStreamResponseHandler)} instead.
     *
     * @param plan the operation's plan, from this engine's runtime
     * @param input the operation's input
     * @param override the call's explicit override configuration, or {@code null}
     * @param inputBody the operation's streaming input body, or {@code null} for an operation without one
     * @return the future of the modeled output; it fails with the call's mapped terminal
     *     {@link software.amazon.awssdk.core.exception.SdkException}
     * @throws IllegalArgumentException if the plan belongs to another client, the input is not the operation's, or
     *     the operation has an output event stream
     * @throws IllegalStateException if the client executes through a sync transport
     */
    public <I extends SerializableStruct, O extends SerializableStruct> CompletableFuture<O> executeAsync(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override, AsyncRequestBody inputBody) {
        requireTransport();
        if (runtime.transportMode() != HttpTransportMode.ASYNC) {
            throw new IllegalStateException("The client executes through a " + runtime.transportMode()
                                            + " transport; executeAsync requires an async transport");
        }
        if (hasOutputEventStream(plan)) {
            throw new IllegalArgumentException(plan.operation().id() + " has an output event stream, which is read "
                                               + "only under demand; call it with an EventStreamResponseHandler");
        }
        CallState<I, O> call = stages.begin(plan, input, override);
        HttpBody body = inputBody == null ? null : RequestBodyAdapter.toHttpBody(inputBody);
        return new AsyncCall<>(stages, asyncHttpClient, completionExecutor, retryScheduler, call, body, null, null,
                               null).start();
    }

    /**
     * Starts one call of a streaming-output {@code plan}, handing the live response payload to
     * {@code responseTransformer}, and returns the future of the transformer's result before the call completes.
     *
     * <p>The same state machine as {@link #executeAsync(PipelinePlan, SerializableStruct,
     * AwsRequestOverrideConfiguration, AsyncRequestBody)} up to the response: an error response is still read in
     * full, classified and possibly retried. For a successful response the driver calls the transformer's
     * {@link AsyncResponseTransformer#prepare() prepare} and {@link AsyncResponseTransformer#onResponse onResponse}
     * with the output deserialized from its status and headers, then
     * {@link AsyncResponseTransformer#onStream onStream} with the transport's body: the transformer's subscription
     * drives the transport's demand, and nothing is buffered. Once the transformer has the response the call is never
     * retried, and the future completes with the transformer's result, or with the call's one mapped failure.
     * Cancelling the future cancels the call, including the exchange and the payload subscription, and the
     * transformer is told through {@link AsyncResponseTransformer#exceptionOccurred exceptionOccurred}.
     *
     * @param plan the plan of an operation whose output has a streaming blob member
     * @param input the operation's input
     * @param override the call's explicit override configuration, or {@code null}
     * @param inputBody the operation's streaming input body, or {@code null} for an operation without one
     * @param responseTransformer consumes the payload and produces the result
     * @return the future of the transformer's result
     * @throws IllegalArgumentException if the plan belongs to another client, the input is not the operation's, or
     *     the operation has no streaming blob output member
     * @throws IllegalStateException if the client executes through a sync transport
     */
    public <I extends SerializableStruct, O extends SerializableStruct, R> CompletableFuture<R> executeAsync(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override, AsyncRequestBody inputBody,
        AsyncResponseTransformer<O, R> responseTransformer) {
        requireTransport();
        if (runtime.transportMode() != HttpTransportMode.ASYNC) {
            throw new IllegalStateException("The client executes through a " + runtime.transportMode()
                                            + " transport; executeAsync requires an async transport");
        }
        Objects.requireNonNull(responseTransformer, "responseTransformer");
        requireStreamingOutput(plan);
        CallState<I, O> call = stages.begin(plan, input, override);
        HttpBody body = inputBody == null ? null : RequestBodyAdapter.toHttpBody(inputBody);
        return new AsyncCall<>(stages, asyncHttpClient, completionExecutor, retryScheduler, call, body,
                               responseTransformer, null, null).start();
    }

    /**
     * Starts one call of an output-event-stream {@code plan}, handing the output and then its live event stream to
     * {@code handler}, and returns the call's future before the call completes (design "Event Stream publishers use
     * the same demand and cancellation contract"; Requirements 5.11, 8.4, 8.5, 8.9, 14.12).
     *
     * <p>The same state machine as {@link #executeAsync(PipelinePlan, SerializableStruct,
     * AwsRequestOverrideConfiguration, AsyncRequestBody)} up to the response: an error response is still read in
     * full, classified and possibly retried. For a successful response the body is never buffered: the driver calls
     * {@link EventStreamResponseHandler#responseReceived} with the output without its event stream member, read from
     * the headers (HTTP binding protocols) or from the {@code initial-response} event (RPC protocols, the one message
     * read without demand), then {@link EventStreamResponseHandler#onEventStream} with the publisher of the events,
     * which decodes events from the transport's body only as its subscriber requests them: at most one body chunk and
     * one partial message are buffered, and no decoded event is queued. Once the handler has the output the call is
     * never retried.
     *
     * <p>The call ends when the stream does, exactly once: on completion the subscriber's {@code onComplete}, then
     * {@link EventStreamResponseHandler#complete()}, then the future with {@code null}; on failure (a failed
     * response, a modeled exception event, an {@code error} frame, a malformed or truncated stream, a body failure)
     * the call's one mapped failure goes to the subscriber's {@code onError}, to
     * {@link EventStreamResponseHandler#exceptionOccurred} and to the future. Cancelling the event subscription or the
     * future cancels the call ({@link software.amazon.awssdk.http.CancellationReason#CALLER_CANCELLED}): the exchange
     * and the body subscription are cancelled and no further event is decoded. The callbacks and the future run on
     * the runtime's completion executor; events are delivered on the thread that drives the subscriber's demand or
     * the transport's body.
     *
     * @param plan the plan of an operation whose output has an event stream member
     * @param input the operation's input
     * @param override the call's explicit override configuration, or {@code null}
     * @param inputBody the operation's streaming input body, or {@code null} for an operation without one
     * @param handler receives the output and the event stream
     * @param <E> the event (union) type of the output event stream
     * @return the future of the call's end, completed after the event stream completes
     * @throws IllegalArgumentException if the plan belongs to another client, the input is not the operation's, or
     *     the operation has no output event stream its protocol serves
     * @throws IllegalStateException if the client executes through a sync transport
     */
    public <I extends SerializableStruct, O extends SerializableStruct, E> CompletableFuture<Void> executeAsync(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override, AsyncRequestBody inputBody,
        EventStreamResponseHandler<O, E> handler) {
        requireTransport();
        if (runtime.transportMode() != HttpTransportMode.ASYNC) {
            throw new IllegalStateException("The client executes through a " + runtime.transportMode()
                                            + " transport; executeAsync requires an async transport");
        }
        Objects.requireNonNull(handler, "handler");
        OutputEventStream eventStream = requireEventStreamOutput(plan);
        CallState<I, O> call = stages.begin(plan, input, override);
        HttpBody body = inputBody == null ? null : RequestBodyAdapter.toHttpBody(inputBody);
        // The driver only hands the handler events of the stream's union, so it holds it with its event type erased.
        @SuppressWarnings("unchecked")
        EventStreamResponseHandler<O, Object> events = (EventStreamResponseHandler<O, Object>) handler;
        return new AsyncCall<>(stages, asyncHttpClient, completionExecutor, retryScheduler, call, body, null,
                               events, eventStream).start();
    }

    /** True if the operation of {@code plan} has an output event stream (a streaming member other than a blob). */
    public static boolean hasOutputEventStream(PipelinePlan<?, ?> plan) {
        Objects.requireNonNull(plan, "plan");
        return plan.operation().schemas().outputStreamMember().filter(m -> m.type() != ShapeType.BLOB).isPresent();
    }

    private static OutputEventStream requireEventStreamOutput(PipelinePlan<?, ?> plan) {
        if (!hasOutputEventStream(plan)) {
            throw new IllegalArgumentException(plan.operation().id() + " has no output event stream");
        }
        return plan.protocol().outputEventStream().orElseThrow(
            () -> new IllegalArgumentException("The protocol of " + plan.operation().id()
                                               + " serves no output event stream"));
    }

    private void requireTransport() {
        if (signingOnly) {
            throw new IllegalStateException("A signing-only engine presigns calls and has no transport to execute "
                                            + "them");
        }
    }

    /**
     * Presigns one call of {@code plan} on the calling thread: the call is signed for later transmission by whoever
     * holds the result, and is never transmitted (design "Paginators, waiters, presigners, and utilities";
     * Requirements 7.10, 18.3). Any engine presigns; a presigner's engine is {@linkplain Builder#signingOnly()
     * signing-only}, without a transport.
     *
     * <p>The same stage functions as {@link #executeSync}, up to signing: validation and override merging, the
     * {@code beforeExecution} and {@code modifyRequest} hooks, the idempotency token and the initial retry token;
     * then for its one attempt endpoint resolution, auth scheme selection with the selected option adjusted by
     * {@link PresignSpec#authOption()}, identity resolution, serialization, and signing with its hooks. The payload
     * request features (compression and request checksums) do not run: the presigned request carries no payload,
     * which its sender supplies. The call then ends {@link TerminalState.Status#PRESIGNED}: no transmission,
     * response, retry or response hook. A failure takes the call's one exception-mapping path.
     *
     * @param plan the operation's plan, from this engine's runtime
     * @param input the operation's input
     * @param override the call's explicit override configuration, or {@code null}
     * @param presign the expiration and the auth option adjustment
     * @return the presigned request
     * @throws IllegalArgumentException if the plan belongs to another client or the input is not the operation's
     * @throws software.amazon.awssdk.core.exception.SdkException the call's mapped terminal failure, including
     *     an operation that is not signed
     */
    public <I extends SerializableStruct, O extends SerializableStruct> PresignedHttpRequest presign(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override, PresignSpec presign) {
        Objects.requireNonNull(presign, "presign");
        CallState<I, O> call = stages.begin(plan, input, override);
        try {
            // The initial token's delay paces transmissions, and a presign call transmits nothing.
            RetryToken token = stages.beforeAttempts(call).token();
            AttemptState attempt = stages.startAttempt(call, token);
            stages.resolveEndpoint(call, attempt);
            SelectedAuthScheme<?> scheme = stages.selectPresignAuthScheme(call, attempt, presign);
            stages.identityResolved(attempt, await(call, stages.resolveIdentity(call, attempt, scheme)));
            stages.serialize(call, attempt, null);
            stages.sign(call, attempt);
            return stages.presigned(call, attempt, presign);
        } catch (Throwable failure) {
            throw terminal(call, failure);
        }
    }

    private static void requireStreamingOutput(PipelinePlan<?, ?> plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan.operation().schemas().outputStreamMember().filter(m -> m.type() == ShapeType.BLOB).isEmpty()) {
            throw new IllegalArgumentException(plan.operation().id() + " has no streaming blob output member");
        }
    }

    /**
     * The payload of a successful streaming-output {@code response} as one blocking stream; the live body, never
     * buffered here. With an {@code exchange} left open for the caller, closing the stream also closes the exchange.
     */
    private static InputStream payload(SdkHttpResponse response, SyncHttpExchange exchange) {
        HttpBody body = response.body();
        InputStream stream = switch (body.kind()) {
            case EMPTY -> InputStream.nullInputStream();
            case BYTES -> new ByteArrayInputStream(body.asByteArray());
            case STREAMING -> body.streamingBody().openStream();
        };
        if (exchange == null) {
            return stream;
        }
        return new FilterInputStream(stream) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    exchange.close();
                }
            }
        };
    }

    /**
     * {@code response} with its streaming body replaced by a one-shot body over the same stream that closes
     * {@code exchange} once, when the stream is closed or reaches its end: the lifetime of a sync output event stream,
     * whose reader pulls messages from the live body after the call returns.
     */
    private static SdkHttpResponse releasingOnEnd(SdkHttpResponse response, SyncHttpExchange exchange) {
        StreamingBody live = response.body().streamingBody();
        InputStream released = new ExchangeReleasingStream(live.openStream(), exchange);
        StreamingBody body = StreamingBodies.fromInputStream(released, live.contentLength().orElse(-1),
                                                             live.contentType().orElse(null));
        return response.toBuilder().body(HttpBody.streaming(body)).build();
    }

    /** A live response stream that closes its exchange, once, at its end or when closed. */
    private static final class ExchangeReleasingStream extends FilterInputStream {
        private final SyncHttpExchange exchange;
        private boolean released;

        private ExchangeReleasingStream(InputStream in, SyncHttpExchange exchange) {
            super(in);
            this.exchange = exchange;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b < 0) {
                release();
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = super.read(buffer, offset, length);
            if (n < 0) {
                release();
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                release();
            }
        }

        private void release() {
            if (!released) {
                released = true;
                exchange.close();
            }
        }
    }

    /** A terminal failure of the sync driver, through the call's one exception-mapping path. */
    private RuntimeException terminal(CallState<?, ?> call, Throwable failure) {
        Throwable cause = failure instanceof CallInterruptedException interrupted ? interrupted.getCause() : failure;
        return stages.fail(call, cause);
    }

    /** Blocks for {@code delay}; an interruption is terminal and cancels the call. */
    private static void sleep(Duration delay) {
        if (delay.isZero()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis(), delay.toNanosPart() % 1_000_000);
        } catch (InterruptedException e) {
            throw new CallInterruptedException(e);
        }
    }

    /**
     * Waits for the identity; an interruption is terminal and cancels the call, and a failure surfaces as the
     * attempt's failure. An interrupted call stops waiting but leaves the provider's future alone: caching providers
     * may share it between calls and refreshes, so one caller must not cancel it.
     */
    private static Identity await(CallState<?, ?> call, CompletableFuture<? extends Identity> identity) {
        try {
            return identity.get();
        } catch (InterruptedException e) {
            throw new CallInterruptedException(e);
        } catch (ExecutionException e) {
            throw CallStages.identityFailure(call, e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public String toString() {
        return "AwsCallEngine(" + runtime + ")";
    }

    /**
     * Carries the {@link InterruptedException} of an interrupted wait past the attempt's classification to the
     * call's exception-mapping path, which restores the interrupt status and cancels the call.
     */
    private static final class CallInterruptedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private CallInterruptedException(InterruptedException cause) {
            super(null, cause, false, false);
        }
    }

    /**
     * Builder of an {@link AwsCallEngine}; the runtime is required, a sync runtime requires a sync transport, and an
     * async runtime requires an async transport and a runtime with a completion executor and retry scheduler.
     */
    public static final class Builder {
        private ClientRuntime runtime;
        private SdkSyncHttpClient syncHttpClient;
        private SdkAsyncHttpClient asyncHttpClient;
        private IdentityProviders identityProviders = IdentityProviders.empty();
        private AuthOptionsResolver authOptions = AuthOptionsResolver.modeled();
        private StageObserver stageObserver;
        private boolean signingOnly;

        private Builder() {
        }

        /**
         * Makes the engine signing-only: it has no transport, only {@link AwsCallEngine#presign presigns}, and its
         * execute methods fail. The engine of a presigner, which shares a client's configuration but never
         * transmits.
         */
        public Builder signingOnly() {
            this.signingOnly = true;
            return this;
        }

        /** A test-only observer of the stages every call runs; none by default, which costs nothing. */
        Builder stageObserver(StageObserver stageObserver) {
            this.stageObserver = stageObserver;
            return this;
        }

        /** The runtime of the client. */
        public Builder runtime(ClientRuntime runtime) {
            this.runtime = runtime;
            return this;
        }

        /** The sync transport; required when the runtime's transport mode is {@link HttpTransportMode#SYNC}. */
        public Builder syncHttpClient(SdkSyncHttpClient syncHttpClient) {
            this.syncHttpClient = syncHttpClient;
            return this;
        }

        /** The async transport; required when the runtime's transport mode is {@link HttpTransportMode#ASYNC}. */
        public Builder asyncHttpClient(SdkAsyncHttpClient asyncHttpClient) {
            this.asyncHttpClient = asyncHttpClient;
            return this;
        }

        /** The identity providers auth-scheme selection draws on; none by default. */
        public Builder identityProviders(IdentityProviders identityProviders) {
            this.identityProviders = Objects.requireNonNull(identityProviders, "identityProviders");
            return this;
        }

        /** The derivation of auth scheme candidates; {@link AuthOptionsResolver#modeled()} by default. */
        public Builder authOptions(AuthOptionsResolver authOptions) {
            this.authOptions = Objects.requireNonNull(authOptions, "authOptions");
            return this;
        }

        /**
         * Builds the engine.
         *
         * @throws NullPointerException if the runtime, the transport its mode requires, or (for an async runtime)
         *     its completion executor or retry scheduler is missing
         * @throws SdkClientConfigurationException if the transport cannot carry the runtime's protocol: only a
         *     transport of the other mode is given, or its capabilities fail the runtime's transport requirements;
         *     the message names the protocol and the transport, and the runtime has been closed
         */
        public AwsCallEngine build() {
            return new AwsCallEngine(this);
        }
    }
}
