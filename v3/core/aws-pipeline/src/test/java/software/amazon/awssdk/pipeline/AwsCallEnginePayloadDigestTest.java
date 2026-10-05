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

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.PayloadSigningMode;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
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
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Streaming uploads whose signer or request checksum needs the payload's bytes before transmission (Requirements
 * 13.6, 10.6, 10.7, 5.11, 8.4, 12.5; design state invariant 8), through both drivers:
 * <ul>
 *   <li>a replayable body is read once, in one pass, for both the SHA-256 the signer signs and the header checksum,
 *       then sent, and sent again with identical bytes when a transport failure after transmission is retried;</li>
 *   <li>the pass reads one chunk at a time under exact demand, whatever the body's size;</li>
 *   <li>a one-shot body is never read before transmission: it is sent with an unsigned payload and a trailer
 *       checksum when the signer allows, never retried once sent, and rejected before anything is read or sent when
 *       its bytes would be needed first.</li>
 * </ul>
 */
class AwsCallEnginePayloadDigestTest {
    private static final Schema STRING = PipelineFixtures.STRING;
    private static final Schema UPLOAD_INPUT = Schema.structureBuilder(ShapeId.from("com.example#UploadInput"))
                                                     .putMember("Name", STRING)
                                                     .putMember("ChecksumAlgorithm", STRING)
                                                     .putMember("Body", Schema.createBlob(
                                                         ShapeId.from("com.example#Stream"), new StreamingTrait()))
                                                     .build();
    private static final Identity IDENTITY = new Identity() {
    };
    private static final byte[] PAYLOAD = "streamed upload payload".getBytes(StandardCharsets.UTF_8);

    @ParameterizedTest
    @EnumSource(Driver.class)
    void execute_replayableBodyWithPayloadHashSigner_hashesSendsAndRetriesIdenticalBytes(Driver driver) {
        BodyReadingHttp http = new BodyReadingHttp()
            .fail(HttpTransportException.create(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.IO,
                                                "connection reset", null))
            .respond(200);
        PayloadScheme scheme = new PayloadScheme(PayloadSigningMode.PAYLOAD_HASH);

        Struct output = call(driver, http, scheme, PAYLOAD, true);

        assertThat(output.<String>getMemberValue(output.schema().member("Value"))).isEqualTo("ok");
        assertThat(http.bodies).hasSize(2);
        assertThat(http.bodies).allSatisfy(body -> assertThat(body).isEqualTo(PAYLOAD));
        assertThat(scheme.suppliedHashes).containsExactly(sha256Hex(PAYLOAD), sha256Hex(PAYLOAD));
        for (SdkHttpRequest sent : http.requests) {
            assertThat(sent.headers().firstValue("x-amz-checksum-crc32")).contains(crc32(PAYLOAD));
            assertThat(sent.headers().firstValue("x-amz-trailer")).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void execute_largeReplayableBody_hashesOneChunkAtATimeUnderExactDemand(Driver driver) {
        BodyReadingHttp http = new BodyReadingHttp().respond(200);
        PayloadScheme scheme = new PayloadScheme(PayloadSigningMode.PAYLOAD_HASH);
        long length = 64L * 1024 * 1024;
        DemandTrackingBody body = new DemandTrackingBody(length);
        http.digestOnly = true;

        call(driver, http, scheme, body);

        // Two subscriptions: the digest pass, then transmission. Neither ever had more than one chunk requested
        // and undelivered, so at most one 64 KiB chunk was in flight while 64 MiB were hashed.
        assertThat(body.subscriptions).hasValue(2);
        assertThat(body.maxOutstanding).hasValue(1);
        assertThat(body.maxChunk).hasValueLessThanOrEqualTo(DemandTrackingBody.CHUNK);
        assertThat(scheme.suppliedHashes).containsExactly(body.sha256Hex());
        assertThat(http.digests).containsExactly(body.sha256Hex());
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void execute_oneShotBodyWithUnsignedPayload_sendsTrailerChecksumAndNeverRetriesAfterTransmission(Driver driver) {
        BodyReadingHttp http = new BodyReadingHttp()
            .fail(HttpTransportException.create(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.IO,
                                                "connection reset", null))
            .respond(200);
        PayloadScheme scheme = new PayloadScheme(PayloadSigningMode.UNSIGNED);

        Throwable failure = failureOf(() -> call(driver, http, scheme, PAYLOAD, false));

        assertThat(failure).isInstanceOf(HttpTransportException.class).hasMessageContaining("connection reset");
        assertThat(http.requests).as("a consumed one-shot body is never retransmitted").hasSize(1);
        SdkHttpRequest sent = http.requests.get(0);
        assertThat(sent.headers().firstValue("x-amz-trailer")).contains("x-amz-checksum-crc32");
        assertThat(sent.headers().firstValue("x-amz-content-sha256")).contains("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(new String(http.bodies.get(0), StandardCharsets.US_ASCII))
            .startsWith(Integer.toHexString(PAYLOAD.length) + "\r\n" + new String(PAYLOAD, StandardCharsets.UTF_8))
            .contains("x-amz-checksum-crc32:" + crc32(PAYLOAD));
        assertThat(scheme.suppliedHashes).containsExactly("-");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void execute_oneShotBodyWithPayloadHashSigner_failsBeforeReadingOrSendingIt(Driver driver) {
        BodyReadingHttp http = new BodyReadingHttp().respond(200);
        PayloadScheme scheme = new PayloadScheme(PayloadSigningMode.PAYLOAD_HASH);
        AtomicInteger subscriptions = new AtomicInteger();

        Throwable failure = failureOf(() -> call(driver, http, scheme, PAYLOAD, false, subscriptions));

        assertThat(failure).isInstanceOf(SdkClientException.class)
                           .hasMessageContaining("not replayable")
                           .hasMessageContaining("x-amz-checksum-crc32");
        assertThat(subscriptions).as("the one-shot body is never read").hasValue(0);
        assertThat(http.requests).isEmpty();
        assertThat(scheme.suppliedHashes).isEmpty();
    }

    private static Struct call(Driver driver, BodyReadingHttp http, PayloadScheme scheme, byte[] payload,
                               boolean replayable) {
        return call(driver, http, scheme, payload, replayable, new AtomicInteger());
    }

    /** One call with {@code payload}, as a replayable or one-shot body whose reads count in {@code reads}. */
    private static Struct call(Driver driver, BodyReadingHttp http, PayloadScheme scheme, byte[] payload,
                               boolean replayable, AtomicInteger reads) {
        Client client = Client.of(driver, http, scheme);
        if (driver == Driver.SYNC) {
            return client.sync(replayable ? RequestBody.fromBytes(payload)
                                          : RequestBody.fromInputStream(new CountingStream(payload, reads),
                                                                        payload.length));
        }
        AsyncRequestBody source = AsyncRequestBody.fromBytes(payload);
        Publisher<ByteBuffer> counted = subscriber -> {
            reads.incrementAndGet();
            source.subscribe(subscriber);
        };
        return client.async(replayable ? source : AsyncRequestBody.fromPublisher(counted));
    }

    private static Struct call(Driver driver, BodyReadingHttp http, PayloadScheme scheme, DemandTrackingBody body) {
        Client client = Client.of(driver, http, scheme);
        if (driver == Driver.SYNC) {
            return client.sync(RequestBody.fromContentProvider(body::newStream, body.length,
                                                               "application/octet-stream"));
        }
        return client.async(body);
    }

    /** An engine over {@code http} whose one auth scheme is {@code scheme}, and its plan of the upload operation. */
    private record Client(AwsCallEngine engine, PipelinePlan<Struct, Struct> plan) {
        static Client of(Driver driver, BodyReadingHttp http, PayloadScheme scheme) {
            ClientRuntime.Builder runtime =
                PipelineFixtures.engineRuntime(PipelineFixtures.retries(3), driver.mode, new HeaderProtocol())
                                .authSchemeSelector(AuthSchemeSelector.create(List.of(scheme)));
            if (driver == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http)
                                                          : builder.asyncHttpClient(http)).build();
            ShapeId id = ShapeId.from("com.example#Upload");
            AwsOperation<Struct, Struct> upload =
                PipelineFixtures.operationBuilder(Schema.createOperation(id), UPLOAD_INPUT,
                                                  PipelineFixtures.GET_OUTPUT)
                                .httpChecksum(HttpChecksumMetadata.builder()
                                                                  .requestAlgorithmMember("ChecksumAlgorithm")
                                                                  .build())
                                .auth(ModeledAuth.builder().operationId(id.toString())
                                                 .addServiceScheme(PayloadScheme.ID).build())
                                .build();
            return new Client(engine, engine.runtime().plan(upload));
        }

        Struct sync(RequestBody body) {
            return engine.executeSync(plan, input(), null, body);
        }

        Struct async(AsyncRequestBody body) {
            return join(engine.executeAsync(plan, input(), null, body));
        }
    }

    private static Struct input() {
        return new Struct(UPLOAD_INPUT, Map.of("Name", "thing"));
    }

    private static Struct join(CompletableFuture<Struct> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Throwable failureOf(Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            return e;
        }
        throw new AssertionError("The call succeeded");
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    /**
     * A scheme whose signer reports {@code mode} and records the payload hash the pipeline supplied ({@code -} for
     * none) in {@link #suppliedHashes}.
     */
    private static final class PayloadScheme implements AuthScheme<Identity> {
        static final String ID = "test#payload";
        final List<String> suppliedHashes = new CopyOnWriteArrayList<>();
        private final PayloadSigningMode mode;

        PayloadScheme(PayloadSigningMode mode) {
            this.mode = mode;
        }

        @Override
        public String schemeId() {
            return ID;
        }

        @Override
        public Class<Identity> identityType() {
            return Identity.class;
        }

        @Override
        public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
            return Optional.of(new IdentityProvider<>() {
                @Override
                public Class<Identity> identityType() {
                    return Identity.class;
                }

                @Override
                public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
                    return CompletableFuture.completedFuture(IDENTITY);
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return new HttpSigner<>() {
                @Override
                public SignedRequest sign(SignRequest<? extends Identity> request) {
                    String hash = request.property(HttpSigner.PAYLOAD_SHA256);
                    suppliedHashes.add(hash == null ? "-" : hash);
                    SdkHttpRequest.Builder signed = request.request().toBuilder();
                    if (mode == PayloadSigningMode.UNSIGNED
                        && !request.request().headers().contains("x-amz-content-sha256")) {
                        signed.putHeader("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
                    }
                    return new SignedRequest(signed.putHeader("Authorization", "test").build(), "test");
                }

                @Override
                public Optional<PayloadSigningMode> payloadSigningMode(SignRequest<? extends Identity> request) {
                    return request.request().headers().contains("x-amz-content-sha256") ? Optional.empty()
                                                                                          : Optional.of(mode);
                }
            };
        }
    }

    /**
     * A transport for both drivers that reads each request body completely (as a real transport transmits it), then
     * fails or responds as scripted; records each request and its body bytes, or with {@link #digestOnly} only the
     * body's SHA-256 (for payloads too large to keep).
     */
    private static final class BodyReadingHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        final List<byte[]> bodies = new CopyOnWriteArrayList<>();
        final List<String> digests = new CopyOnWriteArrayList<>();
        private final Deque<Object> outcomes = new ArrayDeque<>();
        boolean digestOnly;

        BodyReadingHttp respond(int status) {
            outcomes.add(SdkHttpResponse.builder().statusCode(status).body(HttpBody.fromUtf8("ok")).build());
            return this;
        }

        BodyReadingHttp fail(RuntimeException failure) {
            outcomes.add(failure);
            return this;
        }

        private void record(SdkHttpRequest request, InputStream body) throws IOException {
            requests.add(request);
            MessageDigest sha256;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            ByteArrayOutputStream copy = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = body.read(buffer)) >= 0) {
                sha256.update(buffer, 0, n);
                if (!digestOnly) {
                    copy.write(buffer, 0, n);
                }
            }
            digests.add(HexFormat.of().formatHex(sha256.digest()));
            if (!digestOnly) {
                bodies.add(copy.toByteArray());
            }
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    try (InputStream body = request.body().kind() == HttpBody.Kind.STREAMING
                                            ? request.body().streamingBody().openStream()
                                            : InputStream.nullInputStream()) {
                        record(request, body);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    Object outcome = outcomes.remove();
                    if (outcome instanceof RuntimeException failure) {
                        throw failure;
                    }
                    return (SdkHttpResponse) outcome;
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            // The async view, as an async transport transmits it.
            try (InputStream body = new PublisherBridge(request.body().streamingBody())) {
                record(request, body);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Object outcome = outcomes.remove();
            if (outcome instanceof RuntimeException failure) {
                exchange.fail(failure);
                handler.onError(failure);
                return exchange;
            }
            SdkHttpResponse response = (SdkHttpResponse) outcome;
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            byte[] ok = response.body().asByteArray();
            handler.onStream(subscriber -> subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    if (!done) {
                        done = true;
                        subscriber.onNext(ByteBuffer.wrap(ok).asReadOnlyBuffer());
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    done = true;
                }
            }));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "BodyReadingHttp";
        }

        @Override
        public void close() {
        }
    }

    /** Drains a publisher synchronously, one chunk at a time (the test publishers deliver inline). */
    private static final class PublisherBridge extends InputStream {
        private final Deque<ByteBuffer> chunks = new ArrayDeque<>();
        private Subscription subscription;
        private boolean done;
        private Throwable error;

        PublisherBridge(Publisher<ByteBuffer> publisher) {
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    chunks.add(chunk);
                }

                @Override
                public void onError(Throwable t) {
                    error = t;
                    done = true;
                }

                @Override
                public void onComplete() {
                    done = true;
                }
            });
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            while (chunks.isEmpty() || !chunks.peek().hasRemaining()) {
                chunks.poll();
                if (chunks.isEmpty()) {
                    if (done) {
                        if (error != null) {
                            throw new IOException(error);
                        }
                        return -1;
                    }
                    subscription.request(1);
                    if (chunks.isEmpty() && !done) {
                        throw new IllegalStateException("The test body did not deliver inline");
                    }
                }
            }
            ByteBuffer chunk = chunks.peek();
            int n = Math.min(len, chunk.remaining());
            chunk.get(b, off, n);
            return n;
        }
    }

    /** A one-shot stream that counts its first read as a subscription. */
    private static final class CountingStream extends InputStream {
        private final InputStream delegate;
        private final AtomicInteger opened;
        private boolean counted;

        CountingStream(byte[] bytes, AtomicInteger opened) {
            this.delegate = new ByteArrayInputStream(bytes);
            this.opened = opened;
        }

        @Override
        public int read() throws IOException {
            count();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            count();
            return delegate.read(b, off, len);
        }

        private void count() {
            if (!counted) {
                counted = true;
                opened.incrementAndGet();
            }
        }
    }

    /**
     * A replayable body of {@code length} generated bytes, never materialized: every subscription generates them
     * again, chunk by chunk under its demand, and the body records the largest demand outstanding (requested, not
     * yet delivered) and the largest chunk. Its blocking stream (the sync view) generates the same bytes.
     */
    private static final class DemandTrackingBody implements AsyncRequestBody {
        static final int CHUNK = 64 * 1024;
        final long length;
        final AtomicInteger subscriptions = new AtomicInteger();
        final AtomicLong maxOutstanding = new AtomicLong();
        final AtomicLong maxChunk = new AtomicLong();

        DemandTrackingBody(long length) {
            this.length = length;
        }

        private static byte at(long position) {
            return (byte) (position * 31 + 7);
        }

        String sha256Hex() {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] chunk = new byte[CHUNK];
                for (long position = 0; position < length; position += CHUNK) {
                    int size = (int) Math.min(CHUNK, length - position);
                    fill(chunk, position, size);
                    digest.update(chunk, 0, size);
                }
                return HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private static void fill(byte[] chunk, long position, int size) {
            for (int i = 0; i < size; i++) {
                chunk[i] = at(position + i);
            }
        }

        InputStream newStream() {
            subscriptions.incrementAndGet();
            return new InputStream() {
                private long position;

                @Override
                public int read() {
                    return position < length ? at(position++) & 0xFF : -1;
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    if (position >= length) {
                        return -1;
                    }
                    int n = (int) Math.min(Math.min(len, CHUNK), length - position);
                    maxChunk.accumulateAndGet(n, Math::max);
                    for (int i = 0; i < n; i++) {
                        b[off + i] = at(position++);
                    }
                    maxOutstanding.accumulateAndGet(1, Math::max);
                    return n;
                }
            };
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of(length);
        }

        @Override
        public boolean isReplayable() {
            return true;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriptions.incrementAndGet();
            subscriber.onSubscribe(new Subscription() {
                private long position;
                private long demand;
                private boolean emitting;
                private boolean done;

                @Override
                public void request(long n) {
                    if (n <= 0) {
                        done = true;
                        subscriber.onError(new IllegalArgumentException("non-positive request"));
                        return;
                    }
                    demand += n;
                    maxOutstanding.accumulateAndGet(demand, Math::max);
                    if (emitting) {
                        return;
                    }
                    emitting = true;
                    while (demand > 0 && !done) {
                        if (position >= length) {
                            done = true;
                            subscriber.onComplete();
                            break;
                        }
                        int size = (int) Math.min(CHUNK, length - position);
                        byte[] chunk = new byte[size];
                        fill(chunk, position, size);
                        position += size;
                        demand--;
                        maxChunk.accumulateAndGet(size, Math::max);
                        subscriber.onNext(ByteBuffer.wrap(chunk).asReadOnlyBuffer());
                    }
                    if (!done && position >= length) {
                        done = true;
                        subscriber.onComplete();
                    }
                    emitting = false;
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        }
    }
}
