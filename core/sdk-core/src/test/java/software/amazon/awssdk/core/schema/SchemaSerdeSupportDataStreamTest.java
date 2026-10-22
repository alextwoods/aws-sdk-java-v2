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

package software.amazon.awssdk.core.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.schema.io.DataStream;

/**
 * The {@link DataStream} adapters of request bodies in {@link SchemaSerdeSupport}.
 */
class SchemaSerdeSupportDataStreamTest {
    private static final byte[] CONTENT = "hello data stream".getBytes(StandardCharsets.UTF_8);

    @Test
    void dataStream_requestBody_keepsTheBodyLengthAndTypeWithoutReadingIt() {
        AtomicInteger opened = new AtomicInteger();
        RequestBody body = RequestBody.fromContentProvider(() -> {
            opened.incrementAndGet();
            return new ByteArrayInputStream(CONTENT);
        }, CONTENT.length, "text/plain");

        DataStream stream = SchemaSerdeSupport.dataStream(body);

        assertThat(stream.contentLength()).isEqualTo(CONTENT.length);
        assertThat(stream.hasKnownLength()).isTrue();
        assertThat(stream.contentType()).isEqualTo("text/plain");
        assertThat(stream.isReplayable()).isTrue();
        assertThat(SchemaSerdeSupport.requestBody(stream)).containsSame(body);
        assertThat(SchemaSerdeSupport.asyncRequestBody(stream)).isEmpty();
        assertThat(opened).hasValue(0);
    }

    @Test
    void dataStream_requestBodyWithoutLength_hasANegativeLength() {
        DataStream stream = SchemaSerdeSupport.dataStream(
            RequestBody.fromContentProvider(() -> new ByteArrayInputStream(CONTENT), "text/plain"));

        assertThat(stream.contentLength()).isNegative();
        assertThat(stream.hasKnownLength()).isFalse();
    }

    @Test
    void dataStream_asyncRequestBody_keepsTheBodyLengthAndType() {
        AsyncRequestBody body = AsyncRequestBody.fromString("abc");

        DataStream stream = SchemaSerdeSupport.dataStream(body);

        assertThat(stream.contentLength()).isEqualTo(3);
        assertThat(stream.contentType()).isEqualTo(body.contentType());
        assertThat(SchemaSerdeSupport.asyncRequestBody(stream)).containsSame(body);
        assertThat(SchemaSerdeSupport.requestBody(stream)).isEmpty();
        assertThat(SchemaSerdeSupport.dataStream(AsyncRequestBody.fromPublisher(body)).contentLength()).isNegative();
    }

    @Test
    void subscribe_requestBody_publishesTheContentOfANewStreamEachTime() throws Exception {
        byte[] large = new byte[40 * 1024 + 7];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        DataStream stream = SchemaSerdeSupport.dataStream(RequestBody.fromBytes(large));

        assertThat(read(stream)).isEqualTo(large);
        assertThat(read(stream)).isEqualTo(large);
    }

    @Test
    void subscribe_asyncRequestBody_publishesTheBody() throws Exception {
        DataStream stream = SchemaSerdeSupport.dataStream(AsyncRequestBody.fromBytes(CONTENT));

        assertThat(read(stream)).isEqualTo(CONTENT);
    }

    @Test
    void subscribe_cancelled_closesTheStream() {
        ClosingInputStream input = new ClosingInputStream(new byte[100 * 1024]);
        DataStream stream = SchemaSerdeSupport.dataStream(RequestBody.fromContentProvider(() -> input, "a/b"));
        List<ByteBuffer> received = new ArrayList<>();

        stream.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                received.add(buffer);
                subscription.cancel();
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertThat(received).hasSize(1);
        assertThat(input.closed).isTrue();
    }

    @Test
    void subscribe_readFailure_signalsTheErrorAndClosesTheStream() throws Exception {
        ClosingInputStream input = new ClosingInputStream(new byte[0]) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("read failed");
            }
        };
        DataStream stream = SchemaSerdeSupport.dataStream(RequestBody.fromContentProvider(() -> input, "a/b"));

        CompletableFuture<byte[]> result = new CompletableFuture<>();
        stream.subscribe(new CollectingSubscriber(result));

        assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).hasRootCauseMessage("read failed");
        assertThat(input.closed).isTrue();
    }

    @Test
    void adapters_nullArguments_throw() {
        assertThatThrownBy(() -> SchemaSerdeSupport.dataStream((RequestBody) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SchemaSerdeSupport.dataStream((AsyncRequestBody) null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SchemaSerdeSupport.requestBody(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SchemaSerdeSupport.asyncRequestBody(null)).isInstanceOf(NullPointerException.class);
    }

    private static byte[] read(DataStream stream) throws Exception {
        CompletableFuture<byte[]> result = new CompletableFuture<>();
        stream.subscribe(new CollectingSubscriber(result));
        return result.get(5, TimeUnit.SECONDS);
    }

    private static final class CollectingSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<byte[]> result;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private CollectingSubscriber(CompletableFuture<byte[]> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            byte[] chunk = new byte[buffer.remaining()];
            buffer.get(chunk);
            bytes.write(chunk, 0, chunk.length);
        }

        @Override
        public void onError(Throwable t) {
            result.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            result.complete(bytes.toByteArray());
        }
    }

    private static class ClosingInputStream extends InputStream {
        private final ByteArrayInputStream delegate;
        private volatile boolean closed;

        ClosingInputStream(byte[] content) {
            this.delegate = new ByteArrayInputStream(content);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
