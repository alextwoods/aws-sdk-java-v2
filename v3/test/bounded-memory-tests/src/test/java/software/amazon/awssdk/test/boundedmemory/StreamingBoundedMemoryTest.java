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
package software.amazon.awssdk.test.boundedmemory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataAsyncClient;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataClient;
import software.amazon.awssdk.services.mediastoredata.model.GetObjectResponse;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectResponse;
/**
 * Bounded-memory streaming through the real pipeline (Requirements 5.11, 8.4, 8.5, 12.5, 19.11, 19.14, 20.20): a
 * {@value #PAYLOAD_GIB} GiB payload moves through the generated MediaStore Data clients (restJson1, unsigned streaming
 * {@code PutObject} input, streaming {@code GetObject} output), the real request pipeline and a real transport over
 * loopback HTTP/1.1 (url-connection-client for the sync client, netty-nio-client for the async client), in a JVM whose
 * heap is smaller than the payload (the {@code bounded-memory} surefire execution, {@code -Xmx256m}).
 *
 * <p>Every transfer is generated on the fly by {@link Payload} and verified by a rolling {@link Payload.Verifier} on the
 * receiving side (every byte at its offset, plus a SHA-256 of the whole stream against the generator's), so it proves
 * exact bytes and order without materializing the payload or a temporary file. An {@link InFlightGauge} counts the
 * bytes produced but not yet consumed (an upper bound on everything the SDK, the transport and the sockets buffer) and
 * the outstanding Reactive Streams demand; both must stay bounded. Over plain HTTP the signer hashes the payload (as
 * V2 does), so an upload reads its replayable body twice, each pass in bounded memory; the gauge measures the
 * transmission pass against the peer. The early-close and cancel cases abandon a transfer
 * mid-stream and prove the payload source stops, the connection is released (the peer sees it fail) and the client
 * keeps working. Any {@link OutOfMemoryError} fails the test that raised it.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 300, unit = TimeUnit.SECONDS)
class StreamingBoundedMemoryTest {
    static final int PAYLOAD_GIB = 1;
    static final long PAYLOAD = PAYLOAD_GIB * Payload.GIB;
    /** Where an abandoned transfer is closed or cancelled. */
    private static final long EARLY = 16 * Payload.MIB;
    /**
     * The in-flight bound: SDK, transport and loopback socket buffers together. Generous for slow CI kernels, and still
     * a sixteenth of the payload and a quarter of the heap.
     */
    private static final long IN_FLIGHT_BOUND = 64 * Payload.MIB;
    /** The demand bound of the Reactive Streams legs (chunks requested but not delivered). */
    private static final long DEMAND_BOUND = 64;
    private static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS = new IdentityProvider<>() {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }
        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("AKIDEXAMPLE", "SECRETEXAMPLE"));
        }
    };
    private static final List<String> REPORT = new ArrayList<>();
    private static LoopbackObjectServer server;
    private static String expectedSha256;
    private static SdkSyncHttpClient syncHttp;
    private static SdkAsyncHttpClient asyncHttp;
    private static MediaStoreDataClient sync;
    private static MediaStoreDataAsyncClient async;
    private long started;
    @BeforeAll
    static void setUp() throws IOException {
        long heap = Runtime.getRuntime().maxMemory();
        assertThat(heap).as("the payload must be larger than the heap (run by the bounded-memory execution)")
                        .isLessThan(PAYLOAD);
        long start = System.nanoTime();
        expectedSha256 = Payload.sha256(PAYLOAD);
        REPORT.add(String.format("heap=%d MiB payload=%d MiB expectedSha256=%s (%d ms)", heap / Payload.MIB,
                                 PAYLOAD / Payload.MIB, expectedSha256, (System.nanoTime() - start) / 1_000_000));
        server = new LoopbackObjectServer();
        syncHttp = UrlConnectionHttpClient.builder().build();
        asyncHttp = NettyNioAsyncHttpClient.builder().build();
        sync = MediaStoreDataClient.builder().region(Region.of("us-east-1")).credentialsProvider(CREDENTIALS)
                                   .endpointOverride(server.endpoint()).httpClient(syncHttp)
                                   .retryStrategy(StandardRetryStrategy.doNotRetry()).build();
        async = MediaStoreDataAsyncClient.builder().region(Region.of("us-east-1")).credentialsProvider(CREDENTIALS)
                                         .endpointOverride(server.endpoint()).httpClient(asyncHttp)
                                         .retryStrategy(StandardRetryStrategy.doNotRetry()).build();
    }
    @AfterAll
    static void tearDown() {
        closeQuietly(sync);
        closeQuietly(async);
        closeQuietly(syncHttp);
        closeQuietly(asyncHttp);
        closeQuietly(server);
        REPORT.forEach(line -> System.out.println("[bounded-memory] " + line));
    }
    @BeforeEach
    void start() {
        ManagementFactory.getMemoryPoolMXBeans().forEach(MemoryPoolMXBean::resetPeakUsage);
        started = System.nanoTime();
    }
    @AfterEach
    void report(TestInfo test) {
        long heapPeak = ManagementFactory.getMemoryPoolMXBeans().stream()
                                         .filter(pool -> pool.getType() == MemoryType.HEAP)
                                         .mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
        REPORT.add(String.format("%s: %d ms, heap pool peaks %d MiB", test.getDisplayName(),
                                 (System.nanoTime() - started) / 1_000_000, heapPeak / Payload.MIB));
    }
    @Test
    @Order(1)
    void syncUpload_payloadLargerThanTheHeap_arrivesExactlyThroughUrlConnection() throws Exception {
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> received = server.nextUpload(gauge);
        RequestBody body = RequestBody.fromContentProvider(() -> {
            gauge.newPass();
            return Payload.stream(PAYLOAD, gauge);
        }, PAYLOAD, "application/octet-stream");
        PutObjectResponse response = sync.putObject(r -> r.path("sync/upload"), body);
        LoopbackObjectServer.Outcome outcome = received.get(30, TimeUnit.SECONDS);
        REPORT.add("syncUpload " + gauge);
        assertThat(outcome.failure()).isNull();
        assertThat(outcome.bytes()).isEqualTo(PAYLOAD);
        assertThat(outcome.sha256()).isEqualTo(expectedSha256);
        assertThat(response.contentSHA256()).isEqualTo(expectedSha256);
        assertReadPasses(gauge);
        assertThat(gauge.peakInFlight()).isLessThanOrEqualTo(IN_FLIGHT_BOUND);
    }
    @Test
    @Order(2)
    void syncDownload_payloadLargerThanTheHeap_isStreamedExactlyThroughUrlConnection() throws Exception {
        server.objectLength(PAYLOAD);
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> sent = server.nextDownload(gauge);
        Payload.Verifier verifier = sync.getObject(r -> r.path("sync/download"),
                                                   (GetObjectResponse response, InputStream in) -> {
            assertThat(response.contentLength()).isEqualTo(PAYLOAD);
            return drain(in, gauge, Long.MAX_VALUE);
        });
        REPORT.add("syncDownload " + gauge);
        assertThat(verifier.position()).isEqualTo(PAYLOAD);
        assertThat(verifier.sha256()).isEqualTo(expectedSha256);
        assertThat(sent.get(30, TimeUnit.SECONDS).failure()).isNull();
        assertThat(gauge.peakInFlight()).isLessThanOrEqualTo(IN_FLIGHT_BOUND);
    }
    @Test
    @Order(3)
    void asyncUpload_payloadLargerThanTheHeap_arrivesExactlyThroughNettyUnderBoundedDemand() throws Exception {
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> received = server.nextUpload(gauge);
        GeneratedAsyncBody body = new GeneratedAsyncBody(PAYLOAD, gauge);
        PutObjectResponse response = async.putObject(r -> r.path("async/upload"), body).get(240, TimeUnit.SECONDS);
        LoopbackObjectServer.Outcome outcome = received.get(30, TimeUnit.SECONDS);
        REPORT.add("asyncUpload " + gauge);
        assertThat(outcome.failure()).isNull();
        assertThat(outcome.bytes()).isEqualTo(PAYLOAD);
        assertThat(outcome.sha256()).isEqualTo(expectedSha256);
        assertThat(response.contentSHA256()).isEqualTo(expectedSha256);
        assertReadPasses(gauge);
        assertThat(gauge.peakInFlight()).isLessThanOrEqualTo(IN_FLIGHT_BOUND);
        assertThat(gauge.peakDemand()).isLessThanOrEqualTo(DEMAND_BOUND);
    }
    @Test
    @Order(4)
    void asyncDownload_payloadLargerThanTheHeap_isStreamedExactlyThroughNettyUnderBoundedDemand() throws Exception {
        server.objectLength(PAYLOAD);
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> sent = server.nextDownload(gauge);
        Payload.Verifier verifier = async.getObject(r -> r.path("async/download"),
                                                    new VerifyingTransformer<GetObjectResponse>(gauge, Long.MAX_VALUE))
                                         .get(240, TimeUnit.SECONDS);
        REPORT.add("asyncDownload " + gauge);
        assertThat(verifier.position()).isEqualTo(PAYLOAD);
        assertThat(verifier.sha256()).isEqualTo(expectedSha256);
        assertThat(sent.get(30, TimeUnit.SECONDS).failure()).isNull();
        assertThat(gauge.peakInFlight()).isLessThanOrEqualTo(IN_FLIGHT_BOUND);
        assertThat(gauge.peakDemand()).as("the transformer requests one chunk at a time").isEqualTo(1);
    }
    @Test
    @Order(5)
    void syncDownload_closedEarly_abortsTheConnectionAndTheClientStaysUsable() throws Exception {
        server.objectLength(PAYLOAD);
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> sent = server.nextDownload(gauge);
        Payload.Verifier verifier = sync.getObject(r -> r.path("sync/early-close"),
                                                   (GetObjectResponse response, InputStream in) ->
                                                       drain(in, gauge, EARLY));
        LoopbackObjectServer.Outcome outcome = sent.get(60, TimeUnit.SECONDS);
        REPORT.add("syncDownload early close " + gauge + " serverSent=" + outcome.bytes());
        assertThat(verifier.position()).isEqualTo(EARLY);
        assertThat(outcome.failure()).as("closing the stream early releases the connection, not drains it")
                                     .isInstanceOf(IOException.class);
        assertThat(outcome.bytes()).isLessThan(EARLY + IN_FLIGHT_BOUND);
        assertSmallSyncDownloadSucceeds();
    }
    @Test
    @Order(6)
    void asyncDownload_cancelledMidStream_abortsTheConnectionAndTheClientStaysUsable() throws Exception {
        server.objectLength(PAYLOAD);
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> sent = server.nextDownload(gauge);
        Payload.Verifier verifier = async.getObject(r -> r.path("async/cancel"),
                                                    new VerifyingTransformer<GetObjectResponse>(gauge, EARLY))
                                         .get(60, TimeUnit.SECONDS);
        LoopbackObjectServer.Outcome outcome = sent.get(60, TimeUnit.SECONDS);
        REPORT.add("asyncDownload cancel " + gauge + " serverSent=" + outcome.bytes());
        assertThat(verifier.position()).isGreaterThanOrEqualTo(EARLY).isLessThan(EARLY + Payload.MIB);
        assertThat(outcome.failure()).as("cancelling the payload subscription closes the connection")
                                     .isInstanceOf(IOException.class);
        assertThat(outcome.bytes()).isLessThan(EARLY + IN_FLIGHT_BOUND);
        assertSmallAsyncDownloadSucceeds();
    }
    @Test
    @Order(7)
    void asyncUpload_callCancelledMidStream_cancelsTheBodyAndReleasesTheConnection() throws Exception {
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> received = server.nextUpload(gauge);
        GeneratedAsyncBody body = new GeneratedAsyncBody(PAYLOAD, gauge);
        CompletableFuture<PutObjectResponse> call = async.putObject(r -> r.path("async/upload-cancel"), body);
        awaitConsumed(gauge, EARLY);
        call.cancel(true);
        assertThat(body.cancelled().await(30, TimeUnit.SECONDS)).as("the body subscription is cancelled").isTrue();
        LoopbackObjectServer.Outcome outcome = received.get(60, TimeUnit.SECONDS);
        long producedAtCancel = gauge.produced();
        Thread.sleep(200);
        REPORT.add("asyncUpload cancel " + gauge + " serverReceived=" + outcome.bytes());
        assertThat(gauge.produced()).as("no chunk is generated after cancellation").isEqualTo(producedAtCancel);
        assertThat(outcome.failure()).as("the truncated request fails on the peer").isNotNull();
        assertThat(outcome.bytes()).isLessThan(PAYLOAD);
        assertSmallAsyncDownloadSucceeds();
    }
    @Test
    @Order(8)
    void syncUpload_bodyFailingMidStream_closesTheBodyAndReleasesTheConnection() throws Exception {
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> received = server.nextUpload(gauge);
        AtomicBoolean closed = new AtomicBoolean();
        RequestBody body = RequestBody.fromContentProvider(() -> new InputStream() {
            private final InputStream source = startPass(gauge);
            @Override
            public int read() throws IOException {
                throw new UnsupportedOperationException();
            }
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (gauge.consumed() >= EARLY) {
                    throw new IOException("source failed mid-stream");
                }
                return source.read(b, off, len);
            }
            @Override
            public void close() {
                closed.set(true);
            }
        }, PAYLOAD, "application/octet-stream");
        assertThatThrownBy(() -> sync.putObject(r -> r.path("sync/upload-failure"), body))
            .hasStackTraceContaining("source failed mid-stream");
        LoopbackObjectServer.Outcome outcome = received.get(60, TimeUnit.SECONDS);
        REPORT.add("syncUpload failure " + gauge + " serverReceived=" + outcome.bytes());
        assertThat(closed.get()).as("the SDK closes the body stream it opened").isTrue();
        assertThat(outcome.failure()).as("the truncated request fails on the peer").isNotNull();
        assertThat(outcome.bytes()).isLessThan(EARLY + IN_FLIGHT_BOUND);
        assertSmallSyncDownloadSucceeds();
    }
    /** Reads and verifies {@code in} until it ends or {@code limit} bytes were read. */
    private static Payload.Verifier drain(InputStream in, InFlightGauge gauge, long limit) throws IOException {
        Payload.Verifier verifier = new Payload.Verifier();
        byte[] buffer = new byte[64 * 1024];
        while (verifier.position() < limit) {
            int n = in.read(buffer, 0, (int) Math.min(buffer.length, limit - verifier.position()));
            if (n < 0) {
                break;
            }
            gauge.consumed(n);
            verifier.update(buffer, 0, n);
        }
        return verifier;
    }
    /**
     * Plain HTTP signs the payload (V2 parity: the V4 signer enables payload signing for an unencrypted request with a
     * payload, even for an unsigned-payload operation), so a replayable body is read twice, once for its SHA-256 and
     * once for transmission, each pass streaming in bounded memory; never more.
     */
    private static void assertReadPasses(InFlightGauge gauge) {
        assertThat(gauge.passes()).as("one hashing pass at most, then transmission").isBetween(1, 2);
    }

    private static InputStream startPass(InFlightGauge gauge) {
        gauge.newPass();
        return Payload.stream(PAYLOAD, gauge);
    }

    /** Waits until the peer has received {@code bytes}: the transmission pass is under way. */
    private static void awaitConsumed(InFlightGauge gauge, long bytes) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (gauge.consumed() < bytes) {
            assertThat(System.nanoTime()).as("the body was not transmitted within 60 s").isLessThan(deadline);
            Thread.sleep(5);
        }
    }
    private static void assertSmallSyncDownloadSucceeds() throws Exception {
        server.objectLength(Payload.MIB);
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> sent = server.nextDownload(gauge);
        Payload.Verifier verifier = sync.getObject(r -> r.path("sync/after"),
                                                   (GetObjectResponse response, InputStream in) ->
                                                       drain(in, gauge, Long.MAX_VALUE));
        assertThat(verifier.position()).isEqualTo(Payload.MIB);
        assertThat(verifier.sha256()).isEqualTo(Payload.sha256(Payload.MIB));
        assertThat(sent.get(30, TimeUnit.SECONDS).failure()).isNull();
    }
    private static void assertSmallAsyncDownloadSucceeds() throws Exception {
        server.objectLength(Payload.MIB);
        InFlightGauge gauge = new InFlightGauge();
        CompletableFuture<LoopbackObjectServer.Outcome> sent = server.nextDownload(gauge);
        Payload.Verifier verifier = async.getObject(r -> r.path("async/after"),
                                                    new VerifyingTransformer<GetObjectResponse>(gauge, Long.MAX_VALUE))
                                         .get(30, TimeUnit.SECONDS);
        assertThat(verifier.position()).isEqualTo(Payload.MIB);
        assertThat(verifier.sha256()).isEqualTo(Payload.sha256(Payload.MIB));
        assertThat(sent.get(30, TimeUnit.SECONDS).failure()).isNull();
    }
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            REPORT.add("close failed: " + e);
        }
    }
}
