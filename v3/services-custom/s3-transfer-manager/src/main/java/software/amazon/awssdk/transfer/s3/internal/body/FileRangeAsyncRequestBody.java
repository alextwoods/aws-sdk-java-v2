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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * A replayable body of the byte range {@code [offset, offset + length)} of a file: the body of one multipart-upload
 * part, or of a whole single-request upload. Every subscriber reads the range again from its first byte, so the S3
 * client can retry a part without the transfer manager buffering it.
 *
 * <p>Reactive Streams behavior: chunks of at most {@code chunkSize} bytes are read only under outstanding demand and
 * never more chunks than requested are delivered; a non-positive request fails the subscription (rule 3.9);
 * cancellation closes the file. Reads are blocking file I/O, so they run on {@code executor} (the transfer manager's
 * I/O executor), never on the thread that calls {@code request} (which may be an HTTP event loop). Signals to one
 * subscriber are serialized. The subscription fails if the file is shorter than the range or its last-modified time
 * changed since the body was created, so a file modified during an upload is never sent with mixed content.
 */
public final class FileRangeAsyncRequestBody implements AsyncRequestBody {
    private final Path path;
    private final long offset;
    private final long length;
    private final int chunkSize;
    private final Executor executor;
    private final FileTime expectedModified;

    public FileRangeAsyncRequestBody(Path path, long offset, long length, int chunkSize, Executor executor,
                                     FileTime expectedModified) {
        if (offset < 0 || length < 0 || chunkSize <= 0) {
            throw new IllegalArgumentException("Invalid file range: offset " + offset + ", length " + length
                                               + ", chunk size " + chunkSize);
        }
        this.path = Objects.requireNonNull(path, "path");
        this.offset = offset;
        this.length = length;
        this.chunkSize = chunkSize;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.expectedModified = expectedModified;
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of(length);
    }

    /** Every subscription reads the range again from the file, so the S3 client may hash and retry the part. */
    @Override
    public boolean isReplayable() {
        return true;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        RangeSubscription subscription = new RangeSubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

    @Override
    public String toString() {
        return "FileRangeAsyncRequestBody[" + path + ", offset " + offset + ", length " + length + "]";
    }

    private final class RangeSubscription implements Subscription, Runnable {
        private final Subscriber<? super ByteBuffer> subscriber;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private volatile boolean cancelled;
        private volatile Throwable invalidRequest;
        // Only touched by the (serialized) drain loop.
        private boolean done;
        private long position = offset;
        private long remaining = length;
        private FileChannel channel;

        RangeSubscription(Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                invalidRequest = new IllegalArgumentException(
                    "A Reactive Streams subscriber must request a positive number of elements (rule 3.9), got " + n);
            } else {
                demand.getAndUpdate(current -> current + n < 0 ? Long.MAX_VALUE : current + n);
            }
            schedule();
        }

        @Override
        public void cancel() {
            cancelled = true;
            schedule();
        }

        private void schedule() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            try {
                executor.execute(this);
            } catch (RejectedExecutionException e) {
                // This thread owns the drain loop now; fail the subscription without reading.
                invalidRequest = SdkClientException.create("The transfer manager executor rejected a file read", e);
                run();
            }
        }

        @Override
        public void run() {
            int missed = 1;
            do {
                drain();
                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        private void drain() {
            if (done) {
                return;
            }
            if (cancelled) {
                terminate();
                return;
            }
            Throwable invalid = invalidRequest;
            if (invalid != null) {
                terminate();
                subscriber.onError(invalid);
                return;
            }
            try {
                while (remaining > 0 && demand.get() > 0 && !cancelled && invalidRequest == null) {
                    ByteBuffer chunk = read((int) Math.min(chunkSize, remaining));
                    demand.decrementAndGet();
                    subscriber.onNext(chunk);
                }
                if (remaining == 0 && !cancelled && invalidRequest == null) {
                    verifyUnmodified();
                    terminate();
                    subscriber.onComplete();
                }
            } catch (IOException | RuntimeException e) {
                terminate();
                subscriber.onError(e instanceof IOException
                                   ? SdkClientException.create("Failed to read " + path + ": " + e.getMessage(), e)
                                   : e);
            }
        }

        private ByteBuffer read(int size) throws IOException {
            if (channel == null) {
                channel = FileChannel.open(path, StandardOpenOption.READ);
            }
            ByteBuffer buffer = ByteBuffer.allocate(size);
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer, position + buffer.position());
                if (read < 0) {
                    throw new IOException("The file " + path + " ended at byte " + (position + buffer.position())
                                          + ", before the end of the range being uploaded (" + (offset + length)
                                          + "); it was modified during the transfer");
                }
            }
            buffer.flip();
            position += size;
            remaining -= size;
            return buffer.asReadOnlyBuffer();
        }

        private void verifyUnmodified() throws IOException {
            if (expectedModified != null && !expectedModified.equals(Files.getLastModifiedTime(path))) {
                throw new IOException("The file " + path + " was modified during the transfer");
            }
        }

        private void terminate() {
            done = true;
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                    // Closing a read-only channel releases the descriptor even if close reports an error.
                }
                channel = null;
            }
        }
    }
}
