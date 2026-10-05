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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;

/**
 * Reports the progress of a caller's {@link AsyncResponseTransformer}: the response's length becomes the total, and
 * each chunk the caller's subscriber receives is counted. The payload publisher is wrapped without changing demand
 * (the caller's subscriber drives it), and each retry ({@link #prepare()}) retracts the previous attempt's bytes.
 */
public final class ProgressReportingResponseTransformer<ResultT>
    implements AsyncResponseTransformer<GetObjectResponse, ResultT> {
    private final AsyncResponseTransformer<GetObjectResponse, ResultT> delegate;
    private final TransferProgressUpdater progress;
    private volatile AtomicLong attemptBytes;

    public ProgressReportingResponseTransformer(AsyncResponseTransformer<GetObjectResponse, ResultT> delegate,
                                                TransferProgressUpdater progress) {
        this.delegate = delegate;
        this.progress = progress;
    }

    @Override
    public CompletableFuture<ResultT> prepare() {
        AtomicLong previous = attemptBytes;
        if (previous != null) {
            long reported = previous.getAndSet(Long.MIN_VALUE);
            if (reported > 0) {
                progress.bytesTransferred(-reported);
            }
        }
        attemptBytes = new AtomicLong();
        return delegate.prepare();
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        if (response.contentLength() != null) {
            // The bytes this request delivers: the whole object, or the caller's range or part.
            progress.totalBytes(response.contentLength());
        }
        progress.response(response);
        delegate.onResponse(response);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        AtomicLong counter = attemptBytes;
        delegate.onStream(subscriber -> publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriber.onSubscribe(s);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                int bytes = chunk.remaining();
                if (counter.getAndUpdate(v -> v < 0 ? v : v + bytes) >= 0) {
                    progress.bytesTransferred(bytes);
                }
                subscriber.onNext(chunk);
            }

            @Override
            public void onError(Throwable t) {
                subscriber.onError(t);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        }));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        delegate.exceptionOccurred(error);
    }

    @Override
    public String name() {
        return delegate.name();
    }
}
