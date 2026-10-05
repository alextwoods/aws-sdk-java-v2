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

package software.amazon.awssdk.transfer.s3.internal.body;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;

/**
 * Reports the bytes a request body delivers to a progress consumer, without changing demand: the wrapped body's
 * subscription is handed to the subscriber unchanged, so demand and cancellation pass straight through.
 *
 * <p>Each new subscription is a new read of the body (the S3 client re-subscribes to retry, and to transmit after
 * hashing a payload it signs, as over plain HTTP): the bytes the previous read reported are retracted (reported
 * negatively) and that read stops reporting, so progress counts only the current read's bytes.
 */
public final class ProgressReportingRequestBody implements AsyncRequestBody {
    private final AsyncRequestBody delegate;
    private final LongConsumer progress;
    private final AtomicReference<Attempt> current = new AtomicReference<>();

    public ProgressReportingRequestBody(AsyncRequestBody delegate, LongConsumer progress) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    @Override
    public Optional<Long> contentLength() {
        return delegate.contentLength();
    }

    @Override
    public String contentType() {
        return delegate.contentType();
    }

    /** As replayable as the wrapped body: reporting changes no bytes and each subscription is a fresh attempt. */
    @Override
    public boolean isReplayable() {
        return delegate.isReplayable();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        Attempt attempt = new Attempt();
        Attempt previous = current.getAndSet(attempt);
        if (previous != null) {
            long reported = previous.retire();
            if (reported != 0) {
                progress.accept(-reported);
            }
        }
        delegate.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriber.onSubscribe(s);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                int bytes = byteBuffer.remaining();
                if (attempt.add(bytes)) {
                    progress.accept(bytes);
                }
                subscriber.onNext(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
                subscriber.onError(t);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
    }

    @Override
    public String toString() {
        return "ProgressReporting(" + delegate + ")";
    }

    /** The bytes one subscription reported; retired once a newer subscription starts. */
    private static final class Attempt {
        private long reported;
        private boolean retired;

        synchronized boolean add(long bytes) {
            if (retired) {
                return false;
            }
            reported += bytes;
            return true;
        }

        synchronized long retire() {
            retired = true;
            return reported;
        }
    }
}
