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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamRequestBody;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The async driver of one call (design "Plan and drivers"; Requirements 8.2, 8.8, 8.12, 10.4): one compact state
 * machine over the shared {@link CallStages}, not a graph of per-stage futures.
 *
 * <p>The call owns exactly one caller future, a {@link Step}, the current attempt, and at most one outstanding wait:
 * the attempt's identity, the pass over a streaming payload that computes its digests before signing, the active
 * {@link AsyncHttpExchange} with its response-body subscription, or a scheduled retry delay. A wait ends with exactly one {@linkplain #signal event}, which may arrive on any thread (a transport
 * event loop, an identity provider, the retry scheduler); the event only wakes the driver, and the driver always
 * continues on the runtime's completion executor, so codec, feature and caller-completion work never runs on an event
 * loop or the common pool, and nothing blocks.
 *
 * <p>The driver is a drain loop: {@link #wake()} counts wake-ups and starts a drain on the executor only when none is
 * running; a running drain loops once more per wake-up it absorbed. So at most one thread runs {@link #advance()} at
 * a time, and the driver fields are confined to it. The identity wait, the exchange and its body subscription are
 * registered on the attempt's cancellation scope; a retry delay, which runs between attempts, on the call's.
 *
 * <p>Cancelling the caller future cancels the call through {@link CallStages#cancel} (Requirements 8.9, 8.10, 12.8,
 * 14.13), only if that makes the call's one terminal transition: every registered handle (the identity wait, the
 * active exchange, its body subscription, or the delay) runs exactly once, and the future completes with the call's
 * cancellation. The driver observes the terminal call before each step, and before it calls an identity provider or
 * the transport within one, so a queued or late wake-up runs no further stage, hook, attempt or transmission, and a
 * late event is dropped. The future is completed only from the call's terminal state, so it reports exactly one
 * outcome, the call's (Requirements 10.15, 14.10).
 */
final class AsyncCall<I extends SerializableStruct, O extends SerializableStruct> {
    /** Where the driver is; the waiting steps resume when their one event arrives. */
    private enum Step {
        BEFORE_ATTEMPTS,
        ATTEMPT,
        /** Waiting for the attempt's identity. */
        IDENTITY,
        TRANSMIT,
        /** Waiting for the one pass over a replayable streaming payload that computes its digests before signing. */
        DIGESTS,
        /**
         * Waiting for the exchange's response or failure: the headers and the whole body, or with a response
         * transformer, the headers and the live body of a successful response.
         */
        RESPONSE,
        /** Waiting for the response transformer's result, while it consumes the payload under its own demand. */
        TRANSFORM,
        /** Waiting for the RPC {@code initial-response} event of an event-stream output, read without demand. */
        INITIAL,
        /** Waiting for the end of the event stream handed to the caller, which it reads under its own demand. */
        EVENTS,
        /** Waiting for a retry delay. */
        DELAY,
        FAIL,
        DONE
    }

    private static final Object DELAY_ELAPSED = new Object();

    private final CallStages stages;
    private final SdkAsyncHttpClient http;
    private final Executor executor;
    private final RetryScheduler scheduler;
    private final CallState<I, O> call;
    private final HttpBody body;
    /** The streaming-output call's response transformer, or {@code null} for a call whose result is its output. */
    private final AsyncResponseTransformer<O, ?> transformer;
    /** The event-stream-output call's handler, or {@code null}. */
    private final EventStreamResponseHandler<O, Object> handler;
    /** How the event-stream-output call reads its event stream, or {@code null}. */
    private final OutputEventStream eventStream;
    private final CallerFuture future = new CallerFuture();
    private final AtomicInteger wakeUps = new AtomicInteger();
    /** The one event of the current wait: its result, or a {@link Failed}. */
    private volatile Object event;

    // Driver state, confined to the running drain.
    private Step step = Step.BEFORE_ATTEMPTS;
    private RetryToken token;
    private AttemptState attempt;
    private boolean transmitted;
    private Step afterDelay;
    private CancellationScope.Registration delayRegistration;
    private Throwable failure;
    /**
     * Whether the transformer was handed a response, or the event stream handler its output; from then on no failure
     * is retried.
     */
    private boolean transforming;
    /** Whether the transformer's own future completed, so it needs no {@code exceptionOccurred}. */
    private boolean transformerDone;
    private O streamedOutput;
    private Object transformed;
    /** The event-stream-output call's live event stream, once its successful response arrived. */
    private EventStreamRelay relay;
    /** Whether the handler received its one terminal callback. */
    private boolean handlerDone;

    /**
     * The driver of {@code call}: with a {@code transformer}, a streaming-blob-output call's; with a {@code handler}
     * and its {@code eventStream}, an event-stream-output call's; with neither, a call whose result is its output.
     */
    AsyncCall(CallStages stages, SdkAsyncHttpClient http, Executor executor, RetryScheduler scheduler,
              CallState<I, O> call, HttpBody body, AsyncResponseTransformer<O, ?> transformer,
              EventStreamResponseHandler<O, Object> handler, OutputEventStream eventStream) {
        if (handler != null && (transformer != null || eventStream == null)) {
            throw new IllegalArgumentException("An event stream call has a handler and an event stream only");
        }
        this.stages = stages;
        this.http = http;
        this.executor = executor;
        this.scheduler = scheduler;
        this.call = call;
        this.body = body;
        this.transformer = transformer;
        this.handler = handler;
        this.eventStream = eventStream;
    }

    /**
     * Starts the call on the executor and returns its future, before any stage runs: the future of the output, or
     * with a transformer, of the transformer's result.
     */
    @SuppressWarnings("unchecked")
    <R> CompletableFuture<R> start() {
        wake();
        return (CompletableFuture<R>) (CompletableFuture<?>) future;
    }

    /**
     * Delivers the current wait's one event and wakes the driver. Any thread. The late event of a call that already
     * ended (for example an identity or response arriving after the caller cancelled) is dropped.
     */
    private void signal(Object outcome) {
        if (!call.terminal().isTerminal()) {
            event = outcome;
        }
        wake();
    }

    private void wake() {
        if (wakeUps.getAndIncrement() != 0) {
            return;
        }
        try {
            executor.execute(this::drain);
        } catch (RuntimeException rejected) {
            // No drain runs and none will start (the count stays positive), so this thread owns the driver.
            terminate(rejected);
            finished();
        }
    }

    private void drain() {
        do {
            advance();
        } while (wakeUps.decrementAndGet() != 0);
    }

    /** Runs steps until the call is terminal or the driver waits for an event. */
    private void advance() {
        while (!finished()) {
            boolean proceed;
            try {
                proceed = step();
            } catch (Throwable stepFailure) {
                proceed = recover(stepFailure);
            }
            if (!proceed) {
                return;
            }
        }
    }

    /** Runs the current step; false if it waits for an event that has not arrived. */
    private boolean step() {
        switch (step) {
            case BEFORE_ATTEMPTS -> {
                AcquireInitialTokenResponse initial = stages.beforeAttempts(call);
                token = initial.token();
                return delay(initial.delay(), Step.ATTEMPT);
            }
            case ATTEMPT -> {
                attempt = stages.startAttempt(call, token);
                transmitted = false;
                stages.resolveEndpoint(call, attempt);
                SelectedAuthScheme<?> scheme = stages.selectAuthScheme(call, attempt);
                if (scheme == null) {
                    step = Step.TRANSMIT;
                } else if (!call.terminal().isTerminal()) {
                    step = Step.IDENTITY;
                    awaitIdentity(scheme);
                }
                return true;
            }
            case IDENTITY -> {
                Object resolved = take();
                if (resolved == null) {
                    return false;
                }
                if (resolved instanceof Failed failed) {
                    return attemptFailed(CallStages.identityFailure(call, failed.cause()));
                }
                stages.identityResolved(attempt, (Identity) resolved);
                step = Step.TRANSMIT;
                return true;
            }
            case TRANSMIT -> {
                stages.serialize(call, attempt, body);
                stages.requestFeatures(call, attempt);
                PayloadDigests digests = stages.prepareSigning(call, attempt);
                if (digests != null) {
                    if (!call.terminal().isTerminal()) {
                        step = Step.DIGESTS;
                        awaitDigests(digests);
                    }
                    return true;
                }
                return send(null);
            }
            case DIGESTS -> {
                Object computed = take();
                if (computed == null) {
                    return false;
                }
                if (computed instanceof Failed failed) {
                    return attemptFailed(failed.cause());
                }
                return send((PayloadDigests.Result) computed);
            }
            case RESPONSE -> {
                Object outcome = take();
                if (outcome == null) {
                    return false;
                }
                if (outcome instanceof Failed failed) {
                    return attemptFailed(failed.cause());
                }
                SdkHttpResponse response = stages.responseReceived(call, attempt, (SdkHttpResponse) outcome);
                if (handler != null) {
                    startEventStream(response);
                } else if (transformer == null) {
                    stages.succeed(call, attempt, stages.deserialize(call, attempt, response));
                } else {
                    startTransformer(stages.deserializeStreamingOutput(call, attempt, response), response);
                }
                return true;
            }
            case INITIAL -> {
                take();
                OutputEventStream.InitialResponse initial = relay.initial();
                if (initial == null) {
                    if (!relay.upstreamDone()) {
                        return false;
                    }
                    // The decoder ended before the initial-response event (it fails then); the attempt's failure.
                    Throwable ended = relay.upstreamFailure();
                    return attemptFailed(ended != null ? ended : new IllegalStateException(
                        "The event stream ended before its initial-response event"));
                }
                O output;
                try {
                    output = stages.readInitialResponse(call, attempt, initial);
                } catch (Throwable initialFailure) {
                    // Not handed over yet: the attempt's failure, and the stream is abandoned.
                    relay.abandon();
                    throw initialFailure;
                }
                handOverEvents(output);
                return true;
            }
            case EVENTS -> {
                if (!relay.upstreamDone()) {
                    take();
                    if (!relay.upstreamDone()) {
                        return false;
                    }
                }
                Throwable streamFailure = relay.upstreamFailure();
                if (streamFailure != null) {
                    return terminate(streamFailure);
                }
                stages.succeed(call, attempt, streamedOutput);
                return true;
            }
            case TRANSFORM -> {
                Object outcome = take();
                if (outcome == null) {
                    return false;
                }
                transformerDone = true;
                if (outcome instanceof Failed failed) {
                    return terminate(failed.cause());
                }
                transformed = ((Transformed) outcome).result();
                stages.succeed(call, attempt, streamedOutput);
                return true;
            }
            case DELAY -> {
                Object elapsed = take();
                if (elapsed == null) {
                    return false;
                }
                delayRegistration.release();
                delayRegistration = null;
                if (elapsed instanceof Failed failed) {
                    return terminate(failed.cause());
                }
                step = afterDelay;
                return true;
            }
            case FAIL -> {
                return terminate(failure);
            }
            default -> {
                return false;
            }
        }
    }

    /** A step failed: inside an attempt it is the attempt's failure, otherwise the call's. */
    private boolean recover(Throwable stepFailure) {
        if (call.terminal().isTerminal()) {
            // The call ended under the step (for example, it was cancelled); finished() completes it.
            return true;
        }
        try {
            // Once the transformer has the payload, a failure is the call's: the payload cannot be replayed to it.
            return attempt != null && !transforming ? attemptFailed(stepFailure) : terminate(stepFailure);
        } catch (Throwable secondary) {
            return terminate(secondary);
        }
    }

    /**
     * Classifies the attempt's failure once; a granted token authorizes exactly one further attempt. The failure of an
     * attempt whose call already ended (for example, the exchange failing because the caller cancelled) is not
     * classified: the call's end already released the attempt's token.
     */
    private boolean attemptFailed(Throwable attemptFailure) {
        AttemptState failed = attempt;
        attempt = null;
        if (call.terminal().isTerminal()) {
            failed.complete();
            return true;
        }
        if (attemptFailure instanceof Error) {
            return terminate(attemptFailure);
        }
        RetryDecision decision;
        try {
            decision = stages.classify(call, failed, attemptFailure, transmitted);
        } catch (Throwable classification) {
            if (call.terminal().isTerminal()) {
                // Cancelled during classification, which then consumed no token or released the one it obtained.
                return true;
            }
            classification.addSuppressed(attemptFailure);
            return terminate(classification);
        }
        if (decision instanceof RetryDecision.Retry retry) {
            token = retry.token();
            return delay(retry.delay(), Step.ATTEMPT);
        }
        // The failure the attempt's hooks left, which the classifier received.
        failure = failed.failure().orElse(attemptFailure);
        return delay(decision.delay(), Step.FAIL);
    }

    /**
     * Waits for the identity of {@code scheme} without blocking. The wait is a {@linkplain CompletableFuture#copy()
     * copy} of the provider's future, registered on the attempt's scope: cancelling the call cancels only the copy,
     * so the driver stops waiting and drops a late identity, while a provider's future, which caching providers may
     * share between calls and refreshes, is never cancelled by one caller.
     */
    private void awaitIdentity(SelectedAuthScheme<?> scheme) {
        CompletableFuture<? extends Identity> wait = stages.resolveIdentity(call, attempt, scheme).copy();
        attempt.cancellation().register(reason -> wait.cancel(false));
        // An already resolved identity signals inline, and this drain picks it up without a hop.
        wait.whenComplete((identity, error) -> signal(error == null ? identity : new Failed(unwrap(error))));
    }

    /**
     * Signs the attempt's request with the payload {@code digests} (or none), then hands it to the transport and
     * waits for the response.
     */
    private boolean send(PayloadDigests.Result digests) {
        SdkHttpRequest request = stages.sign(call, attempt, digests);
        stages.transmit(call, attempt);
        if (call.terminal().isTerminal()) {
            // Cancelled during this step: nothing is handed to the transport.
            return true;
        }
        CancellationScope scope = attempt.cancellation();
        step = Step.RESPONSE;
        transmitted = true;
        AsyncHttpExchange exchange = http.execute(request, new Exchange(scope), HttpExecutionOptions.defaults());
        scope.register(exchange::cancel);
        registerEventStreamBody(request, scope);
        return true;
    }

    /**
     * Computes the payload digests the attempt needs before signing, in one pass over the replayable body without
     * blocking: the pass requests one chunk at a time and runs on the threads that deliver them, and its result is
     * the wait's one event. The pass is registered on the attempt's scope, so cancelling the call cancels the body
     * subscription. Nothing is transmitted until the pass completes, so a failure is an attempt failure before
     * transmission.
     */
    private void awaitDigests(PayloadDigests digests) {
        PayloadDigests.Pass pass = digests.subscribe();
        attempt.cancellation().register(reason -> pass.cancel());
        // A body that completes inline signals inline, and this drain picks the result up without a hop.
        pass.result().whenComplete((result, error) -> signal(error == null ? result : new Failed(unwrap(error))));
    }

    /** Continues with {@code next} after {@code delay}, scheduled without blocking and cancelled with the call. */
    private boolean delay(Duration delay, Step next) {
        if (delay.isZero()) {
            step = next;
            return true;
        }
        afterDelay = next;
        step = Step.DELAY;
        CompletableFuture<Void> timer = scheduler.delay(delay);
        delayRegistration = call.cancellation().register(reason -> timer.cancel(false));
        timer.whenComplete((ignored, error) -> signal(error == null ? DELAY_ELAPSED : new Failed(error)));
        return true;
    }

    /** The call's one exception-mapping path; {@link #finished()} then completes the future. */
    private boolean terminate(Throwable terminalFailure) {
        step = Step.DONE;
        if (transforming && attempt != null) {
            // The payload is abandoned mid-consumption: the exchange and the transformer's subscription are cancelled.
            attempt.cancellation().cancel(CancellationReason.RESPONSE_ABANDONED);
        }
        try {
            stages.fail(call, terminalFailure);
        } catch (Throwable unmapped) {
            // An Error (already the call's terminal failure), or a failure of the mapping path itself, which still
            // ends the call so the caller future always mirrors its terminal state.
            call.fail(unmapped);
        }
        return true;
    }

    /** If the call is terminal: ends the attempt, completes the caller future from the outcome, and returns true. */
    private boolean finished() {
        TerminalState<O> terminal = call.terminal();
        if (!terminal.isTerminal()) {
            return false;
        }
        step = Step.DONE;
        event = null;
        if (attempt != null) {
            attempt.complete();
            attempt = null;
        }
        // A call cancelled by its caller ends here, after its attempt: its terminal telemetry record, if not emitted.
        stages.telemetryEnded(call);
        boolean succeeded = terminal.status() == TerminalState.Status.SUCCEEDED;
        if (transforming && !transformerDone && !succeeded) {
            // The call failed or was cancelled while the transformer consumed the payload: it releases its output.
            transformerDone = true;
            try {
                transformer.exceptionOccurred(terminal.failure().orElseThrow());
            } catch (RuntimeException ignored) {
                // The call's outcome is already decided; a failing transformer callback cannot change it.
            }
        }
        if (handler != null && !handlerDone) {
            endEventStream(terminal, succeeded);
        }
        if (!future.isDone()) {
            if (handler != null) {
                if (succeeded) {
                    future.complete(null);
                } else {
                    future.completeExceptionally(terminal.failure().orElseThrow());
                }
            } else if (succeeded) {
                future.complete(transformer == null ? terminal.output().orElseThrow() : transformed);
            } else {
                future.completeExceptionally(terminal.failure().orElseThrow());
            }
        }
        return true;
    }

    /**
     * Hands a successful streaming-output response to the transformer: {@code prepare}, {@code onResponse} with the
     * output (whose streaming member is not read), then {@code onStream} with the live payload, which the
     * transformer consumes under its own demand. Its subscription is registered on the attempt's scope, so
     * cancelling the call cancels it. The driver then waits for the transformer's result.
     */
    private void startTransformer(O output, SdkHttpResponse response) {
        transforming = true;
        streamedOutput = output;
        step = Step.TRANSFORM;
        CompletableFuture<?> result = transformer.prepare();
        transformer.onResponse(output);
        if (call.terminal().isTerminal()) {
            return;
        }
        result.whenComplete((value, error) -> signal(error == null ? new Transformed(value)
                                                                    : new Failed(unwrap(error))));
        transformer.onStream(new ScopedPublisher(payload(response), attempt.cancellation()));
    }

    /**
     * Starts reading the event stream of a successful event-stream-output response; an error response is read in
     * full and thrown by the deserialization stage, as always. The decoder is subscribed at once over the live body,
     * registered on the attempt's scope so that ending the attempt cancels it, and reads nothing without demand: for
     * an output bound to the headers the output goes to the handler now; for an RPC output the decoder first reads
     * the {@code initial-response} event, and the driver waits for it.
     */
    private void startEventStream(SdkHttpResponse response) {
        O output = stages.deserializeEventStreamHeaders(call, attempt, response, eventStream);
        if (call.terminal().isTerminal()) {
            return;
        }
        EventStreamRelay events = new EventStreamRelay(new EventStreamRelay.Listener() {
            @Override
            public void wake() {
                AsyncCall.this.wake();
            }

            @Override
            public void cancelled() {
                // The caller's own cancellation of the stream is the call's (Requirements 8.9, 8.10).
                if (stages.cancel(call, CancellationReason.CALLER_CANCELLED)) {
                    AsyncCall.this.wake();
                }
            }
        });
        relay = events;
        attempt.cancellation().register(reason -> events.abandon());
        eventStream.publisher(payload(response), response, eventStream.initialResponse() ? events::initialMessage
                                                                                         : null)
                   .subscribe(events);
        if (eventStream.initialResponse()) {
            step = Step.INITIAL;
        } else {
            handOverEvents(output);
        }
    }

    /**
     * Hands the output and then the event stream to the handler; from here no failure is retried. The driver then
     * waits for the end of the stream, which the caller reads under its own demand.
     */
    private void handOverEvents(O output) {
        transforming = true;
        streamedOutput = output;
        step = Step.EVENTS;
        handler.responseReceived(output);
        if (call.terminal().isTerminal()) {
            return;
        }
        handler.onEventStream(relay);
    }

    /**
     * The handler's one terminal callback, and once handed the stream, the subscriber's one terminal signal: the
     * stream's completion for a successful call, else the call's mapped failure or cancellation.
     */
    private void endEventStream(TerminalState<O> terminal, boolean succeeded) {
        handlerDone = true;
        Throwable callFailure = succeeded ? null : terminal.failure().orElseThrow();
        if (relay != null) {
            if (succeeded) {
                relay.complete();
            } else {
                relay.abandon();
                relay.fail(callFailure);
            }
        }
        try {
            if (succeeded) {
                handler.complete();
            } else {
                handler.exceptionOccurred(callFailure);
            }
        } catch (RuntimeException ignored) {
            // The call's outcome is already decided; a failing handler callback cannot change it.
        }
    }

    /** The payload of a successful streaming-output response as a demand-driven body; never buffered here. */
    private static StreamingBody payload(SdkHttpResponse response) {
        HttpBody payload = response.body();
        return switch (payload.kind()) {
            case STREAMING -> payload.streamingBody();
            case BYTES -> StreamingBodies.fromBytes(payload.asByteArray(), null);
            case EMPTY -> StreamingBodies.fromBytes(new byte[0], null);
        };
    }

    private Object take() {
        Object outcome = event;
        if (outcome != null) {
            event = null;
        }
        return outcome;
    }

    /** The blocking view of an async transport's live body, which has none. */
    private static InputStream noBlockingView() {
        throw new UnsupportedOperationException("The response body of an async transport has no blocking view");
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    @Override
    public String toString() {
        return "AsyncCall(" + call + ", " + step + ")";
    }

    /** The failure event of a wait. */
    private record Failed(Throwable cause) {
        private Failed {
            Objects.requireNonNull(cause, "cause");
        }
    }

    /** The response transformer's result, which may be {@code null}. */
    private record Transformed(Object result) {
    }

    /**
     * The payload handed to a response transformer: each subscription is passed through unchanged, so demand is
     * exactly the transformer's, and is registered on the attempt's scope so the call's cancellation cancels it.
     */
    /**
     * Cancels an input event stream body with the attempt (Reactive Streams 3.5, Requirement 8.10): whether or not
     * the transport cancels its body subscription when its exchange is cancelled, the caller's event publisher is
     * cancelled at once, even while it is silent. A normal end of the attempt releases the handle without running it.
     */
    private static void registerEventStreamBody(SdkHttpRequest request, CancellationScope scope) {
        HttpBody body = request.body();
        if (body.kind() == HttpBody.Kind.STREAMING && body.streamingBody() instanceof EventStreamRequestBody events) {
            scope.register(reason -> events.cancel());
        }
    }

    private static final class ScopedPublisher implements Publisher<ByteBuffer> {
        private final StreamingBody payload;
        private final CancellationScope scope;

        private ScopedPublisher(StreamingBody payload, CancellationScope scope) {
            this.payload = payload;
            this.scope = scope;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            Objects.requireNonNull(subscriber, "subscriber");
            payload.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                    // After the hand-off, so a scope that already ended cancels a subscription the subscriber has.
                    scope.register(reason -> subscription.cancel());
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    subscriber.onNext(chunk);
                }

                @Override
                public void onError(Throwable error) {
                    subscriber.onError(error);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
        }
    }

    /**
     * The caller's one future, which always mirrors the call's {@link TerminalState}: cancelling it is the call's
     * cancellation, so it succeeds only if it makes the call's one terminal transition, before a racing success or
     * failure does. A cancellation that loses changes nothing and returns false; the call's outcome then completes
     * the future.
     */
    private final class CallerFuture extends CompletableFuture<Object> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!stages.cancel(call, CancellationReason.CALLER_CANCELLED)) {
                return false;
            }
            // The call's own CancellationException, carrying any suppressed failure of a cancellation handle.
            completeExceptionally(call.terminal().failure().orElseThrow());
            // A queued or running drain observes the terminal call and runs no further stage; an idle driver wakes
            // to end its attempt.
            wake();
            return true;
        }
    }

    /**
     * The response handler and body subscriber of one exchange: it buffers the body and signals the driver once,
     * with the complete response or the failure. For a streaming-output call's successful response it does not
     * subscribe: it signals the headers with the live body, which the response transformer subscribes to. Runs on the
     * transport's threads, so it only copies and signals.
     */
    private final class Exchange implements AsyncHttpResponseHandler, Subscriber<ByteBuffer> {
        private final CancellationScope scope;
        private SdkHttpResponse headers;
        private ByteArrayOutputStream content;
        private boolean done;

        private Exchange(CancellationScope scope) {
            this.scope = scope;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            headers = response;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            if ((transformer != null || handler != null) && headers != null && !done
                && !call.plan().protocol().isError(headers)) {
                // A successful streaming-output response: the live body goes to the driver unsubscribed, and its
                // transformer (or event stream decoder) subscribes with its own demand. An error response is still
                // read in full.
                done = true;
                StreamingBody live = StreamingBodies.of(-1, headers.headers().firstValue("Content-Type").orElse(null),
                                                        false, AsyncCall::noBlockingView, publisher);
                signal(headers.toBuilder().body(HttpBody.streaming(live)).build());
                return;
            }
            publisher.subscribe(this);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (done) {
                subscription.cancel();
                return;
            }
            scope.register(reason -> subscription.cancel());
            if (scope.isOpen()) {
                // A scope that already ended cancelled the subscription at registration: no demand is signalled.
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            if (done) {
                return;
            }
            int length = chunk.remaining();
            if (content == null) {
                content = new ByteArrayOutputStream(length);
            }
            byte[] bytes = new byte[length];
            chunk.duplicate().get(bytes);
            content.write(bytes, 0, length);
        }

        /** A failure of the exchange (handler) or of the response body (subscriber). */
        @Override
        public void onError(Throwable error) {
            if (done) {
                return;
            }
            done = true;
            signal(new Failed(error));
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            if (headers == null) {
                signal(new Failed(new IllegalStateException("The transport completed a response body before its "
                                                            + "headers")));
                return;
            }
            HttpBody received = content == null ? HttpBody.empty() : HttpBody.fromBytes(content.toByteArray());
            signal(headers.toBuilder().body(received).build());
        }
    }
}
