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

package software.amazon.awssdk.core.schema.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;
import software.amazon.awssdk.utils.async.DelegatingSubscription;
import software.amazon.awssdk.utils.async.IterablePublisher;

/**
 * A {@link DataStream} over a synchronous {@link RequestBody}, which it keeps so that the request pipeline can send the
 * body itself. Creating it reads nothing.
 *
 * <p>Each subscription opens a new stream from the body's {@link ContentStreamProvider} and publishes it in chunks of
 * up to {@value #CHUNK_SIZE} bytes through the SDK's {@link IterablePublisher}. The reads block, on the thread that
 * requests the next chunk. The stream is closed at its end, on a read failure, and when the subscription is cancelled.
 */
@SdkInternalApi
public final class RequestBodyDataStream implements DataStream {
    static final int CHUNK_SIZE = 16 * 1024;
    private static final Logger log = Logger.loggerFor(RequestBodyDataStream.class);

    private final RequestBody body;

    public RequestBodyDataStream(RequestBody body) {
        this.body = Validate.paramNotNull(body, "body");
    }

    /**
     * Returns the request body this stream was created from.
     */
    public RequestBody body() {
        return body;
    }

    @Override
    public long contentLength() {
        return body.optionalContentLength().orElse(-1L);
    }

    @Override
    public String contentType() {
        return body.contentType();
    }

    /**
     * Returns true: a request body's provider opens the content again from the beginning for each stream, as the SDK's
     * retries and signers rely on.
     */
    @Override
    public boolean isReplayable() {
        return true;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Chunks chunks = new Chunks(body.contentStreamProvider());
        new IterablePublisher<ByteBuffer>(() -> chunks).subscribe(new ClosingSubscriber(subscriber, chunks));
    }

    @Override
    public String toString() {
        return "RequestBodyDataStream(contentLength=" + contentLength() + ", contentType=" + contentType() + ")";
    }

    /**
     * The chunks of one stream of the provider. The stream is opened by the first {@link #hasNext()}, and closed at its
     * end, when a read fails, or by {@link #close()}.
     */
    private static final class Chunks implements Iterator<ByteBuffer> {
        private final ContentStreamProvider provider;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile InputStream stream;
        private ByteBuffer next;

        private Chunks(ContentStreamProvider provider) {
            this.provider = provider;
        }

        @Override
        public boolean hasNext() {
            if (next == null && !closed.get()) {
                next = read();
            }
            return next != null;
        }

        @Override
        public ByteBuffer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ByteBuffer chunk = next;
            next = null;
            return chunk;
        }

        private ByteBuffer read() {
            try {
                if (stream == null) {
                    stream = provider.newStream();
                    if (closed.get()) {
                        // Cancelled while the stream was being opened.
                        IoUtils.closeQuietlyV2(stream, log);
                        return null;
                    }
                }
                byte[] buffer = new byte[CHUNK_SIZE];
                int read = 0;
                while (read < buffer.length) {
                    int n = stream.read(buffer, read, buffer.length - read);
                    if (n < 0) {
                        break;
                    }
                    read += n;
                }
                if (read == 0) {
                    close();
                    return null;
                }
                return ByteBuffer.wrap(buffer, 0, read);
            } catch (IOException e) {
                close();
                throw new UncheckedIOException(e);
            } catch (RuntimeException e) {
                close();
                throw e;
            }
        }

        void close() {
            InputStream opened = stream;
            if (closed.compareAndSet(false, true) && opened != null) {
                IoUtils.closeQuietlyV2(opened, log);
            }
        }
    }

    /**
     * Passes the publisher's signals through, and closes the stream when the subscription ends or is cancelled.
     */
    private static final class ClosingSubscriber extends DelegatingSubscriber<ByteBuffer, ByteBuffer> {
        private final Chunks chunks;

        private ClosingSubscriber(Subscriber<? super ByteBuffer> subscriber, Chunks chunks) {
            super(subscriber);
            this.chunks = chunks;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(new DelegatingSubscription(subscription) {
                @Override
                public void cancel() {
                    chunks.close();
                    super.cancel();
                }
            });
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            subscriber.onNext(chunk);
        }

        @Override
        public void onError(Throwable throwable) {
            chunks.close();
            super.onError(throwable);
        }

        @Override
        public void onComplete() {
            chunks.close();
            super.onComplete();
        }
    }
}
