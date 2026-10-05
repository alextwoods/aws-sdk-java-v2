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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.ResponseBytes;

class AsyncResponseTransformerTest {
    private static final String RESPONSE = "response";

    /** A publisher of fixed chunks that records every request, to check the subscriber's demand. */
    private static final class CountingPublisher implements Publisher<ByteBuffer> {
        private final List<ByteBuffer> chunks;
        private final List<Long> requests = new ArrayList<>();
        private final Throwable failure;
        private Subscriber<? super ByteBuffer> subscriber;
        private int next;
        private boolean terminated;

        CountingPublisher(Throwable failure, byte[]... chunks) {
            this.failure = failure;
            this.chunks = new ArrayList<>();
            for (byte[] chunk : chunks) {
                this.chunks.add(ByteBuffer.wrap(chunk));
            }
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    requests.add(n);
                    for (long i = 0; i < n && next < chunks.size(); i++) {
                        subscriber.onNext(chunks.get(next++));
                    }
                    if (next == chunks.size() && !terminated) {
                        terminated = true;
                        if (failure == null) {
                            subscriber.onComplete();
                        } else {
                            subscriber.onError(failure);
                        }
                    }
                }

                @Override
                public void cancel() {
                    terminated = true;
                }
            });
        }
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    @Test
    void toBytesCollectsWithOneChunkOfDemandAtATime() throws Exception {
        AsyncResponseTransformer<String, ResponseBytes<String>> transformer = AsyncResponseTransformer.toBytes();
        CompletableFuture<ResponseBytes<String>> future = transformer.prepare();
        transformer.onResponse(RESPONSE);
        CountingPublisher publisher = new CountingPublisher(null, new byte[] {1, 2}, new byte[] {3});
        transformer.onStream(publisher);

        ResponseBytes<String> result = await(future);
        assertSame(RESPONSE, result.response());
        assertArrayEquals(new byte[] {1, 2, 3}, result.asByteArray());
        assertTrue(publisher.requests.stream().allMatch(n -> n == 1), publisher.requests.toString());
    }

    @Test
    void toBytesPropagatesStreamAndAttemptFailuresAndResetsOnPrepare() throws Exception {
        AsyncResponseTransformer<String, ResponseBytes<String>> transformer = AsyncResponseTransformer.toBytes();
        CompletableFuture<ResponseBytes<String>> first = transformer.prepare();
        transformer.onResponse(RESPONSE);
        IOException failure = new IOException("boom");
        transformer.onStream(new CountingPublisher(failure, new byte[] {1}));
        ExecutionException e = assertThrows(ExecutionException.class, () -> await(first));
        assertSame(failure, e.getCause());

        CompletableFuture<ResponseBytes<String>> second = transformer.prepare();
        assertNotSame(first, second);
        transformer.exceptionOccurred(failure);
        assertSame(failure, assertThrows(ExecutionException.class, () -> await(second)).getCause());

        CompletableFuture<ResponseBytes<String>> third = transformer.prepare();
        transformer.onResponse(RESPONSE);
        transformer.onStream(AsyncRequestBody.fromString("ok"));
        assertEquals("ok", await(third).asUtf8String());
    }

    @Test
    void toFileWritesTheWholePayload(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("out.bin");
        byte[] payload = new byte[3 * AsyncRequestBody.DEFAULT_CHUNK_SIZE + 7];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 31);
        }
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(file);
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse(RESPONSE);
        transformer.onStream(AsyncRequestBody.fromBytes(payload));

        assertSame(RESPONSE, await(future));
        assertArrayEquals(payload, Files.readAllBytes(file));
    }

    @Test
    void toFileDeletesThePartialFileWhenTheStreamFails(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("out.bin");
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(file.toFile());
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse(RESPONSE);
        IOException failure = new IOException("reset");
        transformer.onStream(new CountingPublisher(failure, new byte[] {1, 2, 3}));

        assertSame(failure, assertThrows(ExecutionException.class, () -> await(future)).getCause());
        assertFalse(Files.exists(file));

        // A retry starts again from a fresh file.
        CompletableFuture<String> retry = transformer.prepare();
        transformer.onResponse(RESPONSE);
        transformer.onStream(AsyncRequestBody.fromString("again"));
        assertSame(RESPONSE, await(retry));
        assertEquals("again", Files.readString(file));
    }

    @Test
    void toFileRefusesAnExistingFileAndLeavesIt(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("existing.txt");
        Files.writeString(file, "keep");
        AsyncResponseTransformer<String, String> transformer = AsyncResponseTransformer.toFile(file);
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse(RESPONSE);
        CountingPublisher publisher = new CountingPublisher(null, new byte[] {1});
        transformer.onStream(publisher);

        assertInstanceOf(FileAlreadyExistsException.class,
                         assertThrows(ExecutionException.class, () -> await(future)).getCause());
        assertTrue(publisher.terminated, "the stream is cancelled");
        assertTrue(publisher.requests.isEmpty());
        assertEquals("keep", Files.readString(file));
    }
}
