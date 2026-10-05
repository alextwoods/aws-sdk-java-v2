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

package software.amazon.awssdk.core.async;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;

/**
 * Consumes the streaming payload of an async streaming-output operation and produces the call's result, with the V2
 * callback sequence and the V2 factories {@code toBytes} and {@code toFile}.
 *
 * <p>For each attempt the SDK calls {@link #prepare()}, then {@link #onResponse} with the unmarshalled response and
 * {@link #onStream} with the payload publisher, or {@link #exceptionOccurred} if the attempt fails first. A retry calls
 * {@link #prepare()} again, which must reset any state of the failed attempt. The transformers created here subscribe
 * with exact demand: one chunk is requested at a time, and the next only once the previous one has been consumed.
 *
 * <p>ADAPTED from V2: {@link #onStream} receives a Reactive Streams {@link Publisher} (V2's {@code SdkPublisher}
 * operators are not part of V3 {@code sdk-core}); {@code split}, {@code toPublisher}, {@code toBlockingInputStream}
 * and {@code FileTransformerConfiguration} are not provided yet.
 *
 * @param <ResponseT> the unmarshalled response type
 * @param <ResultT> the result type
 */
public interface AsyncResponseTransformer<ResponseT, ResultT> {
    /** Starts an attempt: resets per-attempt state and returns the future of the call's result. */
    CompletableFuture<ResultT> prepare();

    /** Receives the unmarshalled response of the current attempt, before {@link #onStream}. */
    void onResponse(ResponseT response);

    /** Receives the payload of the current attempt; the transformer must subscribe to it. */
    void onStream(Publisher<ByteBuffer> publisher);

    /**
     * The current attempt failed before or while the payload was consumed; the future returned by {@link #prepare()}
     * must complete exceptionally, and any partial output must be released.
     */
    void exceptionOccurred(Throwable error);

    /** Diagnostic name of the transformer. */
    default String name() {
        return "UNKNOWN";
    }

    /**
     * Writes the payload to a new file at {@code path} and returns the response. The file must not exist, and its
     * parent directories must; it is written without blocking the delivering thread, and deleted if the attempt fails.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(Path path) {
        return new FileAsyncResponseTransformer<>(path);
    }

    /** {@link #toFile(Path)} of {@code file}. */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(File file) {
        return toFile(file.toPath());
    }

    /** Reads the payload into memory. */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> toBytes() {
        return new ByteArrayAsyncResponseTransformer<>();
    }
}
