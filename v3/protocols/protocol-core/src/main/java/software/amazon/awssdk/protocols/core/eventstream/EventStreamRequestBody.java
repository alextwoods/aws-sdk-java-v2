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

package software.amazon.awssdk.protocols.core.eventstream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.eventstream.FrameTransformer;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;

/**
 * The request body of an input event stream: the encoded frames of the events written to the member's
 * {@link EventStream} writer, preceded by the {@code initial-request} event for RPC protocols.
 *
 * <p>The writer is bound to its encoder on the first consumption, not at serialization, so the pipeline can attach
 * the per-frame transform after it signs the initial HTTP request, for SigV4
 * {@code body.withFrameTransformer(EventStreamSigner.sigV4(requestSignature, region, service, keys, clock))}; the
 * signer must be the last transform. Until then the writer's {@code write} blocks (Smithy Java writers wait for
 * activation). Frames are transformed (signed) in write order on the producer thread, at most the writer's bounded
 * queue ahead of transmission; the {@code initial-request} event is transformed on the consuming thread when the
 * body is first consumed.
 *
 * <p>Streaming contract: unknown length, content type {@code application/vnd.amazon.eventstream}, one-shot (never
 * {@linkplain #isReplayable() replayable}: a retry cannot resend written events). The publisher reads one frame per
 * demanded chunk from the writer's bounded pipe on the thread that calls {@code request} (task 7.1 adds the executor
 * handoff for this blocking source).
 *
 * <p>Terminal rules, identical for the sync ({@link #openStream()}) and async ({@link #subscribe}) views:
 * cancelling the subscription or closing the stream before its end cancels the event stream (no later frame is
 * transformed or signed, no closing frame is sent, later writes fail, a producer blocked on the full queue is
 * released, and the cancelling thread never blocks); a transform (signing) failure fails the failing write and then
 * the transport after the frames queued before it. {@link #cancel()} applies the same rules from outside the
 * transport (the engine cancels the body with the call's attempt). The events of a {@link PublisherEventStream} are
 * subscribed only when the body is first consumed, and cancelling the body cancels that subscription at once.
 */
public final class EventStreamRequestBody implements StreamingBody {
    private final EventStream<? extends SerializableStruct> events;
    private final EventStreamCodec codec;
    private final SerializableStruct initialRequest;
    private final FrameTransformer transformer;
    /** Shared by this body and every {@link #withFrameTransformer} view of it: the body is consumed once. */
    private final AtomicBoolean consumed;
    private final Consumption consumption;

    EventStreamRequestBody(EventStream<? extends SerializableStruct> events, EventStreamCodec codec,
                           SerializableStruct initialRequest, FrameTransformer transformer, AtomicBoolean consumed) {
        this.events = Objects.requireNonNull(events, "events");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.initialRequest = initialRequest;
        this.transformer = Objects.requireNonNull(transformer, "transformer");
        this.consumed = consumed;
        this.consumption = new Consumption();
    }

    private EventStreamRequestBody(EventStreamRequestBody base, FrameTransformer transformer) {
        this.events = base.events;
        this.codec = base.codec;
        this.initialRequest = base.initialRequest;
        this.transformer = transformer;
        this.consumed = base.consumed;
        this.consumption = base.consumption;
    }

    /**
     * This body with {@code next} applied to every frame after the current transform (and to the closing frame).
     *
     * @throws IllegalStateException if the body was already consumed
     */
    public EventStreamRequestBody withFrameTransformer(FrameTransformer next) {
        if (consumed.get()) {
            throw new IllegalStateException("The event stream request body was already consumed");
        }
        return new EventStreamRequestBody(this, transformer.andThen(next));
    }

    /** The per-frame transform applied to every outgoing frame. */
    public FrameTransformer frameTransformer() {
        return transformer;
    }

    @Override
    public OptionalLong contentLength() {
        return OptionalLong.empty();
    }

    @Override
    public Optional<String> contentType() {
        return Optional.of(EventStreams.CONTENT_TYPE);
    }

    @Override
    public boolean isReplayable() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return !consumed.get();
    }

    @Override
    public int maxChunkSize() {
        return StreamingBodies.DEFAULT_MAX_CHUNK_SIZE;
    }

    /**
     * The sync view: activates the writer and returns the frame stream. Closing the stream before its end cancels
     * the event stream.
     *
     * @throws IllegalStateException if the body was already consumed
     * @throws RuntimeException if the initial event cannot be encoded or transformed (for example signed)
     */
    @Override
    public InputStream openStream() {
        claim();
        return activate();
    }

    /**
     * The async view: activates the writer and publishes the frames against demand. Cancelling the subscription,
     * including before the first request, cancels the event stream.
     */
    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (!consumed.compareAndSet(false, true)) {
            reject(subscriber, new IllegalStateException("The event stream request body is one-shot and was "
                                                         + "already consumed"));
            return;
        }
        OutgoingEventStream frames;
        try {
            frames = activate();
        } catch (RuntimeException e) {
            reject(subscriber, e);
            return;
        }
        StreamingBodies.fromInputStream(frames, -1, EventStreams.CONTENT_TYPE)
                       .subscribe(new CancellingSubscriber(subscriber, frames));
    }

    private static void reject(Subscriber<? super ByteBuffer> subscriber, Throwable failure) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });
        subscriber.onError(failure);
    }

    private void claim() {
        if (!consumed.compareAndSet(false, true)) {
            throw new IllegalStateException("The event stream request body is one-shot and was already consumed");
        }
    }

    private OutgoingEventStream activate() {
        OutgoingEventStream frames = OutgoingEventStream.open(events, codec, initialRequest, transformer);
        consumption.frames = frames;
        if (consumption.cancelled) {
            // cancel() ran while the body was being opened, before it could see the frames.
            frames.cancel();
        }
        return frames;
    }

    /**
     * Cancels the event stream of this body from outside the transport, for example when the call's attempt is
     * cancelled or fails: an opened body is cancelled as if its consumer cancelled it (a caller's publisher's
     * subscription is cancelled at once); a body not yet consumed can no longer be consumed, its caller's publisher
     * is never subscribed and its writer is closed with a {@link CancellationException}. Idempotent, thread safe and
     * never blocks; a no-op after the end of the stream.
     */
    public void cancel() {
        consumption.cancelled = true;
        if (consumed.compareAndSet(false, true)) {
            PublisherEventStream.discard(events);
            events.closeWithError(new CancellationException("The event stream request body was cancelled before "
                                                            + "it was sent"));
            return;
        }
        OutgoingEventStream frames = consumption.frames;
        if (frames != null) {
            frames.cancel();
        }
    }

    /** The state of the one consumption of the body, shared by its views. */
    private static final class Consumption {
        private volatile boolean cancelled;
        private volatile OutgoingEventStream frames;
    }

    /**
     * Forwards every signal and also cancels the event stream when the subscription is cancelled, which covers a
     * cancellation before the publisher first opened the frame stream.
     */
    private static final class CancellingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> downstream;
        private final OutgoingEventStream frames;

        CancellingSubscriber(Subscriber<? super ByteBuffer> downstream, OutgoingEventStream frames) {
            this.downstream = downstream;
            this.frames = frames;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    subscription.cancel();
                    frames.cancel();
                }
            });
        }

        @Override
        public void onNext(ByteBuffer item) {
            downstream.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            downstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }

    @Override
    public String toString() {
        return "EventStreamRequestBody[" + codec.union().id() + "]";
    }
}
