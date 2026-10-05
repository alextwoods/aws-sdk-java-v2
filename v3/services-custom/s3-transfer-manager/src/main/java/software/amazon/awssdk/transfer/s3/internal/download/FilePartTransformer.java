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

package software.amazon.awssdk.transfer.s3.internal.download;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Writes the payload of one {@code GetObject} (a part, a range, or a whole object) into a shared
 * {@link DestinationFile} at the position given by the response's {@code Content-Range} (or {@code defaultStart}).
 *
 * <p>Demand is exact and bounded: one chunk is requested, written on the I/O executor, and only then is the next one
 * requested, so a part holds at most one chunk in memory and the HTTP thread that delivers a chunk never writes to
 * disk. Every attempt ({@link #prepare()}) starts over at the part's first byte and retracts the bytes the previous
 * attempt reported, so retries neither duplicate progress nor leave stale writes (a stale attempt stops writing).
 */
final class FilePartTransformer implements AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> {
    private final DestinationFile file;
    private final Executor executor;
    private final LongConsumer progress;
    private final long defaultStart;
    private final Consumer<GetObjectResponse> responseListener;
    private volatile Attempt attempt;

    /**
     * @param responseListener called with the response of every attempt before its payload is written (the part-1
     *     response tells the downloader how many parts to fetch); must not block
     */
    FilePartTransformer(DestinationFile file, Executor executor, LongConsumer progress, long defaultStart,
                        Consumer<GetObjectResponse> responseListener) {
        this.file = file;
        this.executor = executor;
        this.progress = progress;
        this.defaultStart = defaultStart;
        this.responseListener = responseListener;
    }

    /** The bytes the current attempt has written. */
    long writtenBytes() {
        Attempt a = attempt;
        if (a == null) {
            return 0;
        }
        synchronized (a) {
            return a.written;
        }
    }

    @Override
    public CompletableFuture<GetObjectResponse> prepare() {
        Attempt previous = attempt;
        if (previous != null) {
            long written = previous.retire();
            if (written != 0) {
                progress.accept(-written);
            }
        }
        Attempt next = new Attempt();
        attempt = next;
        return next.future;
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        Attempt a = attempt;
        a.response = response;
        try {
            a.position = startOf(response.contentRange(), defaultStart);
            responseListener.accept(response);
        } catch (RuntimeException e) {
            a.fail(e);
        }
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        publisher.subscribe(attempt.new WritingSubscriber());
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        Attempt a = attempt;
        if (a != null) {
            a.fail(error);
        }
    }

    @Override
    public String name() {
        return "TransferManagerFilePart";
    }

    /** The first byte position of a {@code Content-Range: bytes first-last/total} value, or {@code fallback}. */
    static long startOf(String contentRange, long fallback) {
        if (contentRange == null) {
            return fallback;
        }
        String value = contentRange.trim();
        if (!value.startsWith("bytes ")) {
            throw SdkClientException.create("Unexpected Content-Range in a GetObject response: " + contentRange);
        }
        int dash = value.indexOf('-');
        if (dash < 0) {
            throw SdkClientException.create("Unexpected Content-Range in a GetObject response: " + contentRange);
        }
        try {
            return Long.parseLong(value.substring("bytes ".length(), dash).trim());
        } catch (NumberFormatException e) {
            throw SdkClientException.create("Unexpected Content-Range in a GetObject response: " + contentRange, e);
        }
    }

    /** The total object size of a {@code Content-Range: bytes first-last/total} value, or {@code null}. */
    static Long totalOf(String contentRange) {
        if (contentRange == null) {
            return null;
        }
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0 || contentRange.endsWith("*")) {
            return null;
        }
        try {
            return Long.parseLong(contentRange.substring(slash + 1).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private final class Attempt {
        private final CompletableFuture<GetObjectResponse> future = new CompletableFuture<>();
        private volatile GetObjectResponse response;
        private long position;
        // Guarded by this.
        private long written;
        private boolean retired;
        private boolean writing;
        private boolean streamComplete;
        private Subscription subscription;

        synchronized long retire() {
            retired = true;
            return written;
        }

        void fail(Throwable t) {
            Subscription s;
            synchronized (this) {
                retired = true;
                s = subscription;
            }
            if (s != null) {
                s.cancel();
            }
            future.completeExceptionally(t);
        }

        private void finishIfDone() {
            boolean done;
            synchronized (this) {
                done = streamComplete && !writing && !retired;
            }
            if (done) {
                future.complete(response);
            }
        }

        private final class WritingSubscriber implements Subscriber<ByteBuffer> {
            @Override
            public void onSubscribe(Subscription s) {
                boolean cancel;
                synchronized (Attempt.this) {
                    cancel = subscription != null || retired;
                    if (!cancel) {
                        subscription = s;
                    }
                }
                if (cancel) {
                    s.cancel();
                    return;
                }
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                synchronized (Attempt.this) {
                    if (retired) {
                        return;
                    }
                    writing = true;
                }
                try {
                    executor.execute(() -> write(chunk));
                } catch (RejectedExecutionException e) {
                    fail(SdkClientException.create("The transfer manager executor rejected a file write", e));
                }
            }

            private void write(ByteBuffer chunk) {
                int size = chunk.remaining();
                long at;
                Subscription s;
                synchronized (Attempt.this) {
                    if (retired) {
                        writing = false;
                        return;
                    }
                    at = position;
                    position += size;
                    s = subscription;
                }
                try {
                    file.write(chunk, at);
                } catch (IOException | RuntimeException e) {
                    synchronized (Attempt.this) {
                        writing = false;
                    }
                    fail(e instanceof IOException
                         ? SdkClientException.create("Failed to write to " + file.path(), e) : e);
                    return;
                }
                boolean report;
                synchronized (Attempt.this) {
                    writing = false;
                    report = !retired;
                    if (report) {
                        written += size;
                    }
                }
                if (report) {
                    progress.accept(size);
                    s.request(1);
                    finishIfDone();
                }
            }

            @Override
            public void onError(Throwable t) {
                fail(t);
            }

            @Override
            public void onComplete() {
                synchronized (Attempt.this) {
                    streamComplete = true;
                }
                finishIfDone();
            }
        }
    }
}
