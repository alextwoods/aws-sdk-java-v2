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

package software.amazon.awssdk.test.fault;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;

/**
 * Wraps response transformers to count the pipeline's callbacks, recording a violation for any callback a transformer
 * must receive at most once per call: a second {@code transform} (sync), or a second {@code prepare},
 * {@code onResponse}, {@code onStream} or {@code exceptionOccurred} (async). Once a transformer has the response the
 * call is never retried, so a second {@code onResponse} is a retry after the payload was handed over. Use one probe per
 * call. Thread-safe.
 */
public final class TransformerProbe {
    private final Violations violations;
    private final AtomicInteger transforms = new AtomicInteger();
    private final AtomicInteger prepares = new AtomicInteger();
    private final AtomicInteger responses = new AtomicInteger();
    private final AtomicInteger streams = new AtomicInteger();
    private final AtomicInteger exceptions = new AtomicInteger();

    public TransformerProbe(Violations violations) {
        this.violations = Objects.requireNonNull(violations, "violations");
    }

    /** {@code delegate}, counted. */
    public <O, R> ResponseTransformer<O, R> sync(ResponseTransformer<O, R> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return new ResponseTransformer<>() {
            @Override
            public R transform(O response, InputStream inputStream) throws Exception {
                once(transforms, "transform");
                return delegate.transform(response, inputStream);
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return delegate.needsConnectionLeftOpen();
            }

            @Override
            public String name() {
                return delegate.name();
            }
        };
    }

    /** {@code delegate}, counted. */
    public <O, R> AsyncResponseTransformer<O, R> async(AsyncResponseTransformer<O, R> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return new AsyncResponseTransformer<>() {
            @Override
            public CompletableFuture<R> prepare() {
                once(prepares, "prepare");
                return delegate.prepare();
            }

            @Override
            public void onResponse(O response) {
                once(responses, "onResponse");
                delegate.onResponse(response);
            }

            @Override
            public void onStream(Publisher<ByteBuffer> publisher) {
                once(streams, "onStream");
                delegate.onStream(publisher);
            }

            @Override
            public void exceptionOccurred(Throwable error) {
                once(exceptions, "exceptionOccurred");
                delegate.exceptionOccurred(error);
            }

            @Override
            public String name() {
                return delegate.name();
            }
        };
    }

    /** Sync {@code transform} calls. */
    public int transforms() {
        return transforms.get();
    }

    /** Async {@code onResponse} calls. */
    public int responses() {
        return responses.get();
    }

    /** Async {@code onStream} calls. */
    public int streams() {
        return streams.get();
    }

    /** Async {@code exceptionOccurred} calls. */
    public int exceptions() {
        return exceptions.get();
    }

    /**
     * How many times the pipeline handed a response to a transformer: sync {@code transform} plus async
     * {@code onResponse} calls. One for a streaming-output call that reached its payload, zero for any other call.
     */
    public int handoffs() {
        return transforms.get() + responses.get();
    }

    private void once(AtomicInteger counter, String callback) {
        if (counter.incrementAndGet() > 1) {
            violations.record("transformer: " + callback + " called " + counter.get() + " times");
        }
    }
}
