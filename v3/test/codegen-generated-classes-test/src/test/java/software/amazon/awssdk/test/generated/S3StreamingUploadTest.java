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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.test.generated.support.FakeHttp;
import software.amazon.awssdk.test.generated.support.FakeHttp.Recorded;

/**
 * Streaming uploads through the clients generated from the canonical S3 model (Requirements 13.6, 10.6, 10.7, 5.11,
 * 8.4, 12.5), with S3's V2 signer defaults:
 * <ul>
 *   <li>over HTTPS the payload is not hashed into the signature ({@code UNSIGNED-PAYLOAD}), so any body, replayable or
 *       one-shot, is sent without being read first, its CRC32 in an unsigned {@code aws-chunked} trailer; a one-shot
 *       body is never retried once transmission began;</li>
 *   <li>over plain HTTP the payload is always signed (V2 behavior): a replayable body is read once for its SHA-256
 *       and header checksum, in bounded memory, then sent, and sent again with identical bytes on a retry.</li>
 * </ul>
 */
class S3StreamingUploadTest {
    private static final URI HTTPS = URI.create("https://s3.test");
    private static final URI HTTP = URI.create("http://s3.test");
    private static final String PAYLOAD = "streamed object data";

    @Test
    void putObject_bareAsyncStringBody_isSentWithAnUnsignedTrailerChecksum() {
        FakeHttp http = FakeHttp.async(S3StreamingUploadTest::ok);
        try (S3AsyncClient client = asyncClient(http, HTTPS)) {
            PutObjectResponse put = join(client.putObject(r -> r.bucket("my-bucket").key("k"),
                                                          AsyncRequestBody.fromString("x")));

            assertEquals("\"etag\"", put.eTag());
            Recorded sent = http.requests().get(0);
            assertEquals(Optional.of("STREAMING-UNSIGNED-PAYLOAD-TRAILER"),
                         sent.request().headers().firstValue("x-amz-content-sha256"));
            assertEquals(Optional.of("aws-chunked"), sent.request().headers().firstValue("Content-Encoding"));
            assertEquals("x", sent.decodedBody());
            assertEquals("x-amz-checksum-crc32:" + crc32("x"), sent.trailer());
        }
    }

    @Test
    void uploadPart_bareAsyncBytesBody_isSentWithAnUnsignedTrailerChecksum() {
        FakeHttp http = FakeHttp.async(S3StreamingUploadTest::ok);
        try (S3AsyncClient client = asyncClient(http, HTTPS)) {
            UploadPartResponse part = join(client.uploadPart(
                r -> r.bucket("my-bucket").key("k").uploadId("u1").partNumber(1),
                AsyncRequestBody.fromBytes(PAYLOAD.getBytes(StandardCharsets.UTF_8))));

            assertEquals("\"etag\"", part.eTag());
            Recorded sent = http.requests().get(0);
            assertEquals(Optional.of(String.valueOf(PAYLOAD.length())),
                         sent.request().headers().firstValue("x-amz-decoded-content-length"));
            assertEquals(PAYLOAD, sent.decodedBody());
        }
    }

    @Test
    void putObject_oneShotPublisherOverHttps_isSentUnreadAndNeverRetriedAfterTransmission() {
        AtomicInteger attempts = new AtomicInteger();
        FakeHttp http = FakeHttp.async(recorded -> {
            if (attempts.incrementAndGet() == 1) {
                throw connectionReset();
            }
            return ok(recorded);
        });
        AtomicInteger subscriptions = new AtomicInteger();
        AsyncRequestBody source = AsyncRequestBody.fromString(PAYLOAD);
        AsyncRequestBody oneShot = AsyncRequestBody.fromPublisher(subscriber -> {
            subscriptions.incrementAndGet();
            source.subscribe(subscriber);
        });

        try (S3AsyncClient client = asyncClient(http, HTTPS)) {
            CompletableFuture<PutObjectResponse> put = client.putObject(r -> r.bucket("my-bucket").key("k"), oneShot);

            HttpTransportException failure = assertThrows(HttpTransportException.class, () -> join(put));
            assertTrue(failure.getMessage().contains("connection reset"), failure.getMessage());
        }
        assertEquals(1, http.requests().size(), "a consumed one-shot body is not retransmitted");
        assertEquals(1, subscriptions.get(), "transmission is the only read");
        Recorded sent = http.requests().get(0);
        assertEquals(Optional.of("STREAMING-UNSIGNED-PAYLOAD-TRAILER"),
                     sent.request().headers().firstValue("x-amz-content-sha256"));
        assertEquals(PAYLOAD, sent.decodedBody());
    }

    @Test
    void putObject_oneShotPublisherOverHttps_succeeds() {
        FakeHttp http = FakeHttp.async(S3StreamingUploadTest::ok);
        try (S3AsyncClient client = asyncClient(http, HTTPS)) {
            PutObjectResponse put = join(client.putObject(
                r -> r.bucket("my-bucket").key("k"),
                AsyncRequestBody.fromPublisher(AsyncRequestBody.fromString(PAYLOAD))));

            assertEquals("\"etag\"", put.eTag());
            assertEquals(PAYLOAD, http.requests().get(0).decodedBody());
            assertEquals(Optional.empty(),
                         http.requests().get(0).request().headers().firstValue("x-amz-decoded-content-length"),
                         "the publisher's length is unknown");
        }
    }

    @Test
    void putObject_replayableAsyncBodyOverHttp_isHashedSentAndRetriedWithIdenticalBytes() {
        AtomicInteger attempts = new AtomicInteger();
        FakeHttp http = FakeHttp.async(recorded -> {
            if (attempts.incrementAndGet() == 1) {
                throw connectionReset();
            }
            return ok(recorded);
        });
        try (S3AsyncClient client = asyncClient(http, HTTP)) {
            PutObjectResponse put = join(client.putObject(r -> r.bucket("my-bucket").key("k"),
                                                          AsyncRequestBody.fromString(PAYLOAD)));

            assertEquals("\"etag\"", put.eTag());
        }
        assertPayloadSignedAndRetried(http.requests());
    }

    @Test
    void putObject_replayableSyncBodyOverHttp_isHashedSentAndRetriedWithIdenticalBytes() {
        AtomicInteger attempts = new AtomicInteger();
        FakeHttp http = FakeHttp.sync(recorded -> {
            if (attempts.incrementAndGet() == 1) {
                throw connectionReset();
            }
            return ok(recorded);
        });
        try (S3Client client = S3Client.builder().region(Region.of("us-west-2"))
                                       .credentialsProvider(FakeHttp.CREDENTIALS)
                                       .endpointOverride(HTTP).httpClient(http).build()) {
            PutObjectResponse put = client.putObject(r -> r.bucket("my-bucket").key("k"),
                                                     RequestBody.fromString(PAYLOAD));

            assertEquals("\"etag\"", put.eTag());
        }
        assertPayloadSignedAndRetried(http.requests());
    }

    @Test
    void putObject_oneShotPublisherOverHttp_failsBeforeReadingOrSendingIt() {
        FakeHttp http = FakeHttp.async(S3StreamingUploadTest::ok);
        AtomicInteger subscriptions = new AtomicInteger();
        AsyncRequestBody source = AsyncRequestBody.fromString(PAYLOAD);
        AsyncRequestBody oneShot = AsyncRequestBody.fromPublisher(subscriber -> {
            subscriptions.incrementAndGet();
            source.subscribe(subscriber);
        });
        try (S3AsyncClient client = asyncClient(http, HTTP)) {
            CompletableFuture<PutObjectResponse> put = client.putObject(r -> r.bucket("my-bucket").key("k"), oneShot);

            RuntimeException failure = assertThrows(RuntimeException.class, () -> join(put));
            assertTrue(failure.getMessage().contains("not replayable"), failure.getMessage());
        }
        assertEquals(0, subscriptions.get());
        assertTrue(http.requests().isEmpty());
    }

    /**
     * A 32 MiB file over plain HTTP: the pipeline hashes it before signing with exactly one chunk of at most 64 KiB
     * requested at a time, then transmission reads it again the same way, so the SDK never holds more than one chunk
     * of it (the transport here hashes the bytes it receives instead of keeping them).
     */
    @Test
    void putObject_largeFileOverHttp_isHashedWithBoundedBufferingAndExactDemand(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("large.bin");
        long length = 32L * 1024 * 1024;
        String expectedSha256 = writePattern(file, length);
        TrackingBody body = new TrackingBody(AsyncRequestBody.fromFile(file));
        HashingHttp http = new HashingHttp();

        try (S3AsyncClient client = asyncClient(http, HTTP)) {
            join(client.putObject(r -> r.bucket("my-bucket").key("k"), body));
        }

        assertEquals(2, body.subscriptions.get(), "one digest pass, one transmission");
        assertEquals(1, body.maxOutstanding.get(), "chunks requested but not delivered");
        assertTrue(body.maxChunk.get() <= AsyncRequestBody.DEFAULT_CHUNK_SIZE, String.valueOf(body.maxChunk));
        assertEquals(length, http.receivedLength.get());
        assertEquals(List.of(expectedSha256), http.receivedSha256);
        SdkHttpRequest sent = http.requests.get(0);
        assertEquals(Optional.of(expectedSha256), sent.headers().firstValue("x-amz-content-sha256"));
        assertTrue(sent.headers().firstValue("x-amz-checksum-crc32").isPresent());
        assertEquals(Optional.empty(), sent.headers().firstValue("Content-Encoding"));
    }

    private static void assertPayloadSignedAndRetried(List<Recorded> requests) {
        assertEquals(2, requests.size());
        for (Recorded sent : requests) {
            assertEquals(PAYLOAD, sent.body(), "the identical bytes, unframed");
            assertEquals(Optional.of(sha256(PAYLOAD)), sent.request().headers().firstValue("x-amz-content-sha256"));
            assertEquals(Optional.of(crc32(PAYLOAD)), sent.request().headers().firstValue("x-amz-checksum-crc32"));
            assertEquals(Optional.empty(), sent.request().headers().firstValue("x-amz-trailer"));
            String authorization = sent.request().headers().firstValue("Authorization").orElseThrow();
            assertTrue(authorization.contains("x-amz-content-sha256"), authorization);
        }
    }

    private static S3AsyncClient asyncClient(SdkAsyncHttpClient http, URI endpoint) {
        return S3AsyncClient.builder().region(Region.of("us-west-2")).credentialsProvider(FakeHttp.CREDENTIALS)
                            .endpointOverride(endpoint).httpClient(http).build();
    }

    private static SdkHttpResponse ok(Recorded recorded) {
        return SdkHttpResponse.builder().statusCode(200).addHeader("ETag", "\"etag\"").body(HttpBody.empty()).build();
    }

    private static HttpTransportException connectionReset() {
        return HttpTransportException.create(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.IO,
                                             "connection reset", null);
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException runtime ? runtime : new CompletionException(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String text) {
        return HexFormat.of().formatHex(sha256().digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String crc32(String text) {
        CRC32 crc = new CRC32();
        crc.update(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    /** Writes {@code length} pattern bytes to {@code file} one block at a time; returns their SHA-256. */
    private static String writePattern(Path file, long length) throws IOException {
        MessageDigest digest = sha256();
        byte[] block = new byte[1024 * 1024];
        try (OutputStream out = Files.newOutputStream(file)) {
            for (long written = 0; written < length; written += block.length) {
                for (int i = 0; i < block.length; i++) {
                    block[i] = (byte) ((written + i) * 31 + 7);
                }
                int size = (int) Math.min(block.length, length - written);
                out.write(block, 0, size);
                digest.update(block, 0, size);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * A replayable body that delegates to another and records, across its subscriptions, how many it had, the most
     * chunks ever requested and not yet delivered on one subscription, and the largest chunk.
     */
    private static final class TrackingBody implements AsyncRequestBody {
        final AtomicInteger subscriptions = new AtomicInteger();
        final AtomicLong maxOutstanding = new AtomicLong();
        final AtomicLong maxChunk = new AtomicLong();
        private final AsyncRequestBody delegate;

        TrackingBody(AsyncRequestBody delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Long> contentLength() {
            return delegate.contentLength();
        }

        @Override
        public boolean isReplayable() {
            return delegate.isReplayable();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriptions.incrementAndGet();
            AtomicLong outstanding = new AtomicLong();
            delegate.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                            maxOutstanding.accumulateAndGet(outstanding.addAndGet(n), Math::max);
                            subscription.request(n);
                        }

                        @Override
                        public void cancel() {
                            subscription.cancel();
                        }
                    });
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    outstanding.decrementAndGet();
                    maxChunk.accumulateAndGet(chunk.remaining(), Math::max);
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
            });
        }
    }

    /**
     * An async transport that reads each request body one chunk at a time and keeps only its length and SHA-256,
     * then answers 200.
     */
    private static final class HashingHttp implements SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        final AtomicLong receivedLength = new AtomicLong();
        final List<String> receivedSha256 = new CopyOnWriteArrayList<>();

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            requests.add(request);
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            MessageDigest digest = sha256();
            request.body().streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    receivedLength.addAndGet(chunk.remaining());
                    digest.update(chunk);
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable t) {
                    exchange.fail(t);
                    handler.onError(t);
                }

                @Override
                public void onComplete() {
                    receivedSha256.add(HexFormat.of().formatHex(digest.digest()));
                    handler.onHeaders(ok(null));
                    handler.onStream(subscriber -> subscriber.onSubscribe(new Subscription() {
                        private boolean done;

                        @Override
                        public void request(long n) {
                            if (!done) {
                                done = true;
                                subscriber.onComplete();
                                exchange.complete();
                            }
                        }

                        @Override
                        public void cancel() {
                            done = true;
                        }
                    }));
                }
            });
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "HashingHttp";
        }

        @Override
        public void close() {
        }
    }
}
