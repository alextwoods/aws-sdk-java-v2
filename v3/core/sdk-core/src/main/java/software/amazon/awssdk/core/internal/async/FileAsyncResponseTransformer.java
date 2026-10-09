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

package software.amazon.awssdk.core.internal.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

/**
 * {@link AsyncResponseTransformer#toFile(java.nio.file.Path)}: writes the payload to a new file through an
 * {@link AsynchronousFileChannel}, requesting the next chunk only once the previous one is fully written, and deletes
 * the file it created when the attempt fails.
 *
 * @param <ResponseT> the unmarshalled response type
 */
public final class FileAsyncResponseTransformer<ResponseT> implements AsyncResponseTransformer<ResponseT, ResponseT> {
    private final Path path;
    private volatile CompletableFuture<ResponseT> result;
    private volatile ResponseT response;
    private volatile FileWritingSubscriber subscriber;

    public FileAsyncResponseTransformer(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    @Override
    public CompletableFuture<ResponseT> prepare() {
        response = null;
        subscriber = null;
        CompletableFuture<ResponseT> future = new CompletableFuture<>();
        result = future;
        return future;
    }

    @Override
    public void onResponse(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        CompletableFuture<ResponseT> future = Objects.requireNonNull(result, "prepare() was not called");
        AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        } catch (IOException | RuntimeException e) {
            publisher.subscribe(new CancellingSubscriber());
            future.completeExceptionally(e);
            return;
        }
        FileWritingSubscriber writer = new FileWritingSubscriber(channel, future, response);
        subscriber = writer;
        publisher.subscribe(writer);
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        FileWritingSubscriber writer = subscriber;
        if (writer != null) {
            writer.fail(error, true);
        } else {
            CompletableFuture<ResponseT> future = result;
            if (future != null) {
                future.completeExceptionally(error);
            }
        }
    }

    @Override
    public String name() {
        return "FILE";
    }

    /** Rejects a stream this transformer cannot write. */
    private static final class CancellingSubscriber implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription s) {
            s.cancel();
        }

        @Override
        public void onNext(ByteBuffer chunk) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
    }

    private final class FileWritingSubscriber implements Subscriber<ByteBuffer>, CompletionHandler<Integer, ByteBuffer> {
        private final AsynchronousFileChannel channel;
        private final CompletableFuture<ResponseT> future;
        private final ResponseT attemptResponse;
        private Subscription subscription;
        private volatile long position;
        private boolean writing;
        private boolean upstreamComplete;
        private boolean finished;

        private FileWritingSubscriber(AsynchronousFileChannel channel, CompletableFuture<ResponseT> future,
                                      ResponseT attemptResponse) {
            this.channel = channel;
            this.future = future;
            this.attemptResponse = attemptResponse;
        }

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s, "subscription");
            synchronized (this) {
                if (subscription != null || finished) {
                    s.cancel();
                    return;
                }
                subscription = s;
            }
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            Objects.requireNonNull(chunk, "chunk");
            synchronized (this) {
                if (finished) {
                    return;
                }
                writing = true;
            }
            write(chunk.duplicate());
        }

        private void write(ByteBuffer chunk) {
            try {
                channel.write(chunk, position, chunk, this);
            } catch (RuntimeException e) {
                fail(e, true);
            }
        }

        @Override
        public void completed(Integer written, ByteBuffer chunk) {
            position += written;
            if (chunk.hasRemaining()) {
                write(chunk);
                return;
            }
            boolean complete;
            synchronized (this) {
                if (finished) {
                    // The attempt failed while this chunk was written; upstream must not be asked for more.
                    return;
                }
                writing = false;
                complete = upstreamComplete;
            }
            if (complete) {
                succeed();
            } else {
                subscription.request(1);
            }
        }

        @Override
        public void failed(Throwable error, ByteBuffer chunk) {
            fail(error, true);
        }

        @Override
        public void onError(Throwable t) {
            // Rule 2.3: upstream has terminated, so its subscription is not cancelled from onError.
            fail(Objects.requireNonNull(t, "throwable"), false);
        }

        @Override
        public void onComplete() {
            synchronized (this) {
                if (writing) {
                    upstreamComplete = true;
                    return;
                }
            }
            succeed();
        }

        private void succeed() {
            synchronized (this) {
                if (finished) {
                    return;
                }
                finished = true;
            }
            try {
                channel.close();
            } catch (IOException e) {
                deleteQuietly(e);
                future.completeExceptionally(e);
                return;
            }
            if (attemptResponse == null) {
                IllegalStateException e = new IllegalStateException("onStream was called before onResponse");
                deleteQuietly(e);
                future.completeExceptionally(e);
            } else {
                future.complete(attemptResponse);
            }
        }

        /**
         * Stops the attempt: cancels upstream (unless upstream itself terminated), closes and deletes the file, and fails
         * the future. Idempotent.
         */
        private void fail(Throwable error, boolean cancelUpstream) {
            Subscription s;
            synchronized (this) {
                if (finished) {
                    return;
                }
                finished = true;
                s = subscription;
            }
            if (s != null && cancelUpstream) {
                s.cancel();
            }
            try {
                channel.close();
            } catch (IOException e) {
                error.addSuppressed(e);
            }
            deleteQuietly(error);
            future.completeExceptionally(error);
        }

        private void deleteQuietly(Throwable error) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                error.addSuppressed(e);
            }
        }
    }
}
