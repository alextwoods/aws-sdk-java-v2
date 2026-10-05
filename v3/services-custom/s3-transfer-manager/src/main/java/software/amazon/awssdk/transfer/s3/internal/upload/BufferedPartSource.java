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

package software.amazon.awssdk.transfer.s3.internal.upload;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Splits a caller's {@link AsyncRequestBody} into parts while it is read: the replayable split-body factory for bodies
 * that cannot be re-read. The body is subscribed to exactly once; each part is copied into one {@code partSize} buffer
 * and offered as an in-memory body that every subscription replays, so the S3 client can retry a part.
 *
 * <p>Memory is bounded by demand: at most {@code maxBufferedParts} parts are sealed or in flight (handed out and not
 * yet {@linkplain UploadPart#release() released}), plus the part being filled and one upstream chunk. Upstream demand
 * is one chunk at a time and is only signalled while there is capacity, so a slow S3 or a fast body never grows the
 * buffer: total memory is at most {@code (maxBufferedParts + 1) * partSize} plus one chunk, whatever the object size.
 *
 * <p>A part is sealed once it is full and more data (or the end of the body) has been seen, so every part knows
 * whether it is the last one; the uploader sends a body that fits in its first part with a single {@code PutObject}.
 * When the body declared a length, more or fewer bytes than declared fail the upload. Thread safe.
 */
public final class BufferedPartSource implements PartSource, Subscriber<ByteBuffer> {
    private final AsyncRequestBody body;
    private final int partSize;
    private final int maxBufferedParts;
    private final Long expectedLength;
    private final Function<AsyncRequestBody, AsyncRequestBody> bodyDecorator;

    // Guarded by this.
    private Subscription upstream;
    private boolean upstreamRequested;
    private boolean upstreamComplete;
    private ByteBuffer pending;
    private byte[] filling;
    private int filled;
    private int nextPartNumber = 1;
    private long consumed;
    private final Deque<UploadPart> sealed = new ArrayDeque<>();
    private int outstanding;
    private Receiver waiting;
    private Throwable error;
    private boolean ended;
    private boolean cancelled;
    private boolean started;

    public BufferedPartSource(AsyncRequestBody body, long partSize, int maxBufferedParts, Long expectedLength,
                              Function<AsyncRequestBody, AsyncRequestBody> bodyDecorator) {
        this.body = Objects.requireNonNull(body, "body");
        this.partSize = Math.toIntExact(partSize);
        this.maxBufferedParts = maxBufferedParts;
        this.expectedLength = expectedLength;
        this.bodyDecorator = bodyDecorator;
    }

    @Override
    public void next(Receiver receiver) {
        boolean subscribe;
        synchronized (this) {
            if (waiting != null) {
                throw new IllegalStateException("A part request is already outstanding");
            }
            waiting = receiver;
            subscribe = !started;
            started = true;
        }
        if (subscribe) {
            body.subscribe(this);
        }
        process();
    }

    @Override
    public void cancel() {
        Subscription s;
        synchronized (this) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            s = upstream;
            sealed.clear();
            filling = null;
            pending = null;
            waiting = null;
        }
        if (s != null) {
            s.cancel();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        boolean cancel;
        synchronized (this) {
            cancel = upstream != null || cancelled;
            if (!cancel) {
                upstream = subscription;
            }
        }
        if (cancel) {
            subscription.cancel();
            return;
        }
        process();
    }

    @Override
    public void onNext(ByteBuffer chunk) {
        synchronized (this) {
            if (cancelled) {
                return;
            }
            upstreamRequested = false;
            pending = chunk.duplicate();
        }
        process();
    }

    @Override
    public void onError(Throwable t) {
        synchronized (this) {
            upstreamComplete = true;
            if (error == null) {
                error = t;
            }
        }
        process();
    }

    @Override
    public void onComplete() {
        synchronized (this) {
            upstreamComplete = true;
        }
        process();
    }

    private void release() {
        synchronized (this) {
            outstanding--;
        }
        process();
    }

    /** Moves data into parts, hands out a sealed part or the end, and asks upstream for more when there is room. */
    private void process() {
        List<Runnable> actions = new ArrayList<>(2);
        synchronized (this) {
            if (cancelled) {
                return;
            }
            fill();
            if (waiting != null) {
                Receiver receiver = waiting;
                if (error != null) {
                    waiting = null;
                    Throwable failure = error;
                    actions.add(() -> receiver.onError(failure));
                } else if (!sealed.isEmpty()) {
                    waiting = null;
                    UploadPart part = sealed.poll();
                    outstanding++;
                    actions.add(() -> receiver.onPart(part));
                } else if (ended) {
                    waiting = null;
                    actions.add(receiver::onEnd);
                }
            }
            if (upstream != null && !upstreamRequested && !upstreamComplete && pending == null && error == null
                && sealed.size() + outstanding < maxBufferedParts) {
                upstreamRequested = true;
                Subscription s = upstream;
                actions.add(() -> s.request(1));
            }
        }
        actions.forEach(Runnable::run);
    }

    /** Copies pending data into the filling buffer and seals parts; caller holds the lock. */
    private void fill() {
        while (error == null && !ended) {
            if (filling != null && filled == partSize && (pending != null || upstreamComplete)) {
                seal(upstreamComplete && pending == null);
                continue;
            }
            if (pending != null && sealed.size() + outstanding < maxBufferedParts) {
                if (filling == null) {
                    filling = new byte[partSize];
                    filled = 0;
                }
                if (filled == partSize) {
                    // Full and more data is pending: sealed by the branch above on the next iteration.
                    seal(false);
                    continue;
                }
                int n = Math.min(pending.remaining(), partSize - filled);
                pending.get(filling, filled, n);
                filled += n;
                consumed += n;
                if (expectedLength != null && consumed > expectedLength) {
                    error = SdkClientException.create("The request body delivered more bytes than its declared "
                                                      + "content length " + expectedLength);
                    return;
                }
                if (!pending.hasRemaining()) {
                    pending = null;
                }
                continue;
            }
            if (pending == null && upstreamComplete) {
                if (expectedLength != null && consumed != expectedLength) {
                    error = SdkClientException.create("The request body delivered " + consumed
                                                      + " bytes but declared a content length of " + expectedLength);
                    return;
                }
                if (filling != null && filled > 0 || nextPartNumber == 1) {
                    seal(true);
                }
                ended = true;
            }
            return;
        }
    }

    private void seal(boolean last) {
        byte[] data = filling == null ? new byte[0] : filled == partSize ? filling : Arrays.copyOf(filling, filled);
        int number = nextPartNumber++;
        filling = null;
        filled = 0;
        sealed.add(new UploadPart(number, data.length, bodyDecorator.apply(AsyncRequestBody.fromBytesUnsafe(data)),
                                  last, this::release));
    }
}
