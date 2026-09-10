package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;

/**
 * Multipart upload and download on the bridged pipeline, driven by v2's own
 * {@link MultipartS3AsyncClient} through {@link SyncBackedS3AsyncClient}.
 *
 * <p>The point is that the multipart client is <em>unmodified</em>. It is a decorator over an
 * {@code S3AsyncClient}: it splits a body, issues {@code CreateMultipartUpload}, N {@code UploadPart}s
 * and {@code CompleteMultipartUpload}, retries, aborts, and reassembles a download — all in v2 code that
 * knows nothing about smithy-java. If that code runs unchanged against a shim over the bridged sync
 * client, then multipart is not a separate porting problem; the async entry point is.
 *
 * <p>What is asserted is the thing that actually matters and that a request-by-request diff would not
 * catch: the parts, reassembled in part-number order, are byte-identical to what the caller handed in.
 * A splitter that dropped, duplicated, truncated or reordered a part passes every per-request assertion
 * and fails this one.
 *
 * <p>Sizes are small (20 MiB, 5 MiB parts) because this module's surefire runs with {@code -Xmx256m} and
 * the transport keeps every part in memory to reassemble it. The multipart <em>machinery</em> does not
 * care about the absolute size, only about the part count, which is 4 here.
 */
class S3MultipartTest {

    private static final long PART_SIZE = 5 * 1024 * 1024;
    private static final int OBJECT_BYTES = 20 * 1024 * 1024;
    private static final int EXPECTED_PARTS = 4;
    private static final String UPLOAD_ID = "wirediff-upload-id";

    /**
     * Guard: the client underneath really is the bridged one.
     *
     * <p>Everything else here would pass just as happily against stock v2, since the whole point is that
     * the multipart layer cannot tell the difference. If the module ever resolved a published SDK instead
     * of the locally-installed snapshot, the suite would go green while measuring nothing.
     */
    @Test
    void theClientUnderTestIsBridged() throws Exception {
        boolean bridged = java.util.Arrays.stream(
                                  Class.forName("software.amazon.awssdk.services.s3.DefaultS3Client")
                                       .getDeclaredFields())
                              .anyMatch(f -> f.getType().getName().endsWith(".SmithyBridgeClient"));
        assertTrue(bridged, "DefaultS3Client has no SmithyBridgeClient field, so these tests are running "
                            + "against a stock v2 pipeline");
    }

    @Test
    void multipartUploadSendsEveryByteExactlyOnce() throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        MultipartTransport transport = new MultipartTransport(payload);

        PutObjectResponse response;
        try (S3AsyncClient multipart = multipartClient(transport)) {
            response = multipart.putObject(r -> r.bucket("wirediff-bucket").key("big.bin"),
                                           AsyncRequestBody.fromBytes(payload))
                                .get(60, TimeUnit.SECONDS);
        }

        assertEquals(1, transport.createCount.get(), "exactly one CreateMultipartUpload");
        assertEquals(EXPECTED_PARTS, transport.uploadedParts.size(),
                     "a " + OBJECT_BYTES + " byte object at " + PART_SIZE + " byte parts is "
                     + EXPECTED_PARTS + " parts, got " + transport.uploadedParts.keySet());
        assertEquals(1, transport.completeCount.get(), "exactly one CompleteMultipartUpload");
        assertEquals(0, transport.abortCount.get(), "a successful upload must not abort");
        assertArrayEquals(payload, transport.reassembleUpload(),
                          "the parts do not reassemble into the object that was handed in");
        assertEquals("\"wirediff-final-etag\"", response.eTag(),
                     "the completed response must reach the caller through the shim");

        // Every part but the last is exactly the configured size: a splitter that drifted would still
        // reassemble correctly if it compensated, and this is what says it did not have to.
        for (int part = 1; part < EXPECTED_PARTS; part++) {
            assertEquals(PART_SIZE, transport.uploadedParts.get(part).length, "part " + part + " size");
        }
    }

    /**
     * The parts are uploaded with a bounded pool, and the pool is the ceiling.
     *
     * <p>Not a performance assertion — a correctness one about the caveat. The shim holds a pool thread
     * for the whole of each {@code UploadPart}, so with a single-threaded pool the four parts must have
     * run one at a time. The multipart client is configured to want them concurrent, so if this observed
     * overlap it would mean the shim was not doing what its documentation says, and the deadlock warning
     * in {@link SyncBackedS3AsyncClient} would be wrong about why.
     */
    @Test
    void oneThreadSerializesTheParts() throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        MultipartTransport transport = new MultipartTransport(payload);

        try (S3Client sync = S3Cases.client(transport);
             SyncBackedS3AsyncClient shim = SyncBackedS3AsyncClient.create(sync, 1);
             S3AsyncClient multipart = MultipartS3AsyncClient.create(shim, multipartConfiguration(), false)) {
            multipart.putObject(r -> r.bucket("wirediff-bucket").key("big.bin"),
                                AsyncRequestBody.fromBytes(payload))
                     .get(60, TimeUnit.SECONDS);
        }

        assertEquals(EXPECTED_PARTS, transport.uploadedParts.size());
        assertArrayEquals(payload, transport.reassembleUpload());
        assertEquals(1, transport.maxConcurrentUploads.get(),
                     "one pool thread cannot upload two parts at once");
    }

    /**
     * A multipart <em>download</em>: v2 discovers the part count from the first response and fetches the
     * rest, feeding one split transformer per part.
     *
     * <p>This is the harder direction for the shim. Each part response drives an
     * {@link AsyncResponseTransformer} from a blocking stream, on the pool thread, with the transformer
     * pulling buffers from inside its own {@code onNext} — the reentrancy the publisher's
     * work-in-progress counter exists for. A stack overflow here, or a lost final buffer, is the failure
     * mode being checked.
     */
    @Test
    void multipartDownloadReassemblesEveryPart() throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        MultipartTransport transport = new MultipartTransport(payload);

        byte[] received;
        try (S3AsyncClient multipart = multipartClient(transport)) {
            received = multipart.getObject(r -> r.bucket("wirediff-bucket").key("big.bin"),
                                           AsyncResponseTransformer.toBytes())
                                .get(60, TimeUnit.SECONDS)
                                .asByteArray();
        }

        assertEquals(EXPECTED_PARTS, transport.downloadedParts.size(),
                     "every part must be fetched, got " + transport.downloadedParts);
        assertArrayEquals(payload, received, "the downloaded object differs from the stored one");
    }

    /**
     * A retryable server error on one part fails the whole upload instead of retrying — caveat 3 of
     * {@link SyncBackedS3AsyncClient}, confirmed rather than asserted in prose.
     *
     * <p>The diagnostic is v2's own, which is what makes this worth recording: the shim turns each
     * {@code AsyncRequestBody} into a {@code RequestBody} whose content provider subscribes on every
     * attempt, and the split bodies the multipart client produces permit exactly one subscriber. So the
     * second attempt is rejected by v2's splitter, as a {@code NonRetryableException}, before the bridged
     * pipeline is involved at all. Retries of non-multipart bodies (bytes, files) are unaffected because
     * those publishers resubscribe.
     *
     * <p>Cleanup is intact: the upload is aborted, so a failure here does not leak an incomplete
     * multipart upload. That is checked while the client is still open — the abort is fire-and-forget
     * after the caller's future has already failed, so closing first would cancel it and make this look
     * like a leak that it is not.
     */
    @Test
    void aRetryableFailureOnOnePartIsNotRetried() throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        MultipartTransport transport = new MultipartTransport(payload).failPart(2, 1);

        S3AsyncClient multipart = multipartClient(transport);
        try {
            Throwable failure = null;
            try {
                multipart.putObject(r -> r.bucket("wirediff-bucket").key("big.bin"),
                                    AsyncRequestBody.fromBytes(payload))
                         .get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                failure = e.getCause() != null ? e.getCause() : e;
            }

            assertNotNull(failure, "a 500 on one part must not be reported as a successful upload");
            assertEquals("NonRetryableException", failure.getClass().getSimpleName(),
                         "expected v2's splitter to refuse the second subscribe, got: " + failure);
            assertTrue(failure.getMessage().contains("Multiple subscribers detected"),
                       "expected the multiple-subscribers diagnostic, got: " + failure.getMessage());
            assertEquals(0, transport.completeCount.get(), "a failed upload must not complete");
            assertTrue(waitFor(() -> transport.abortCount.get() == 1),
                       "the upload should have been aborted, saw " + transport.abortCount.get() + " aborts");
        } finally {
            multipart.close();
        }
    }

    /** Polls for up to 10s, because the abort is issued after the caller's future has already failed. */
    private static boolean waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(20);
        }
        return condition.getAsBoolean();
    }

    /**
     * {@code toBlockingInputStream} works, and the pool thread is held until the caller lets go.
     *
     * <p>This was expected to deadlock and does not, which is worth a test of its own. The reason it
     * works is the same design that keeps the façade to one thread per call: the publisher reads on
     * whichever thread calls {@code request(n)}, and for this transformer that is the <em>caller's</em>
     * thread pulling bytes out of the stream. The pool thread parks inside the blocking
     * {@code ResponseTransformer} — which it must, since returning would close the body — and is released
     * when the stream ends.
     *
     * <p>So the cost is not a deadlock but a held thread, for as long as the caller holds the stream. A
     * caller that reads slowly, or forgets to close, occupies a pool slot indefinitely; on a
     * single-threaded pool that is the whole client. Early close is checked too, because a stream
     * abandoned halfway is the common case and it has to release the thread rather than strand it.
     */
    @Test
    void aBlockingInputStreamWorksButPinsAPoolThread() throws Exception {
        byte[] payload = pattern(256 * 1024);
        MultipartTransport transport = new MultipartTransport(payload);

        try (S3Client sync = S3Cases.client(transport);
             SyncBackedS3AsyncClient shim = SyncBackedS3AsyncClient.create(sync, 1)) {

            byte[] received;
            try (ResponseInputStream<GetObjectResponse> stream =
                     shim.getObject(r -> r.bucket("wirediff-bucket").key("small.bin").partNumber(1),
                                    AsyncResponseTransformer.toBlockingInputStream())
                         .get(10, TimeUnit.SECONDS)) {
                received = stream.readAllBytes();
            }
            assertArrayEquals(payload, received, "the blocking stream must deliver the whole body");

            // The single pool thread has to be back: this second call cannot start otherwise, and a
            // strand would show up as a timeout rather than a wrong answer.
            try (ResponseInputStream<GetObjectResponse> abandoned =
                     shim.getObject(r -> r.bucket("wirediff-bucket").key("small.bin").partNumber(1),
                                    AsyncResponseTransformer.toBlockingInputStream())
                         .get(10, TimeUnit.SECONDS)) {
                assertEquals(1024, abandoned.readNBytes(1024).length, "partial read");
            }

            // ...and again after an early close, which is the case that would strand the thread if
            // cancellation did not release it.
            byte[] afterAbandon = shim.getObject(r -> r.bucket("wirediff-bucket").key("small.bin")
                                                       .partNumber(1),
                                                 AsyncResponseTransformer.toBytes())
                                      .get(10, TimeUnit.SECONDS)
                                      .asByteArray();
            assertArrayEquals(payload, afterAbandon,
                              "the pool thread was not released by closing a partially-read stream");
        }
    }

    /** A plain (non-multipart) streaming round trip through the shim, as the control case. */
    @Test
    void shimStreamsWithoutMultipart() throws Exception {
        byte[] payload = pattern(1024 * 1024);
        MultipartTransport transport = new MultipartTransport(payload);

        byte[] received;
        PutObjectResponse put;
        try (S3Client sync = S3Cases.client(transport);
             SyncBackedS3AsyncClient shim = SyncBackedS3AsyncClient.create(sync, 2)) {
            put = shim.putObject(r -> r.bucket("wirediff-bucket").key("small.bin"),
                                 AsyncRequestBody.fromBytes(payload))
                      .get(30, TimeUnit.SECONDS);
            received = shim.getObject(r -> r.bucket("wirediff-bucket").key("small.bin").partNumber(1),
                                      AsyncResponseTransformer.toBytes())
                           .get(30, TimeUnit.SECONDS)
                           .asByteArray();
        }

        assertArrayEquals(payload, transport.singlePut, "the whole body must reach the transport");
        assertArrayEquals(payload, received, "the whole body must reach the caller");
        assertTrue(put.eTag() != null && !put.eTag().isEmpty(), "the response must carry the ETag");
    }

    private static S3AsyncClient multipartClient(SdkHttpClient transport) {
        // MultipartS3AsyncClient.create rather than S3AsyncClient.builder().multipartEnabled(true): the
        // builder would build a stock v2 async client, which is precisely the pipeline this test is not
        // interested in. Reaching for the internal factory is how the multipart layer gets pointed at a
        // delegate of our choosing.
        SyncBackedS3AsyncClient shim = SyncBackedS3AsyncClient.create(S3Cases.client(transport), 8);
        return MultipartS3AsyncClient.create(shim, multipartConfiguration(), false);
    }

    private static MultipartConfiguration multipartConfiguration() {
        return MultipartConfiguration.builder()
                                     .minimumPartSizeInBytes(PART_SIZE)
                                     .thresholdInBytes(PART_SIZE)
                                     .apiCallBufferSizeInBytes(PART_SIZE * 4)
                                     .build();
    }

    /** Position-dependent, so a reordered or duplicated part cannot compare equal. */
    private static byte[] pattern(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((i * 31 + (i >> 13)) & 0xFF);
        }
        return bytes;
    }

    /**
     * An {@link SdkHttpClient} that behaves enough like S3's multipart API to drive v2's multipart code.
     *
     * <p>Routing is by method plus query parameter, the same way S3 distinguishes these operations:
     * {@code POST ?uploads} creates, {@code PUT ?partNumber&uploadId} uploads a part,
     * {@code POST ?uploadId} completes, {@code DELETE ?uploadId} aborts, and {@code GET ?partNumber}
     * returns a part with the total count so the downloader knows how many to ask for.
     */
    private static final class MultipartTransport implements SdkHttpClient {

        private final byte[] storedObject;
        private final Map<Integer, byte[]> uploadedParts = new ConcurrentSkipListMap<>();
        private final List<Integer> downloadedParts = new ArrayList<>();
        private final AtomicInteger createCount = new AtomicInteger();
        private final AtomicInteger completeCount = new AtomicInteger();
        private final AtomicInteger abortCount = new AtomicInteger();
        private final AtomicInteger concurrentUploads = new AtomicInteger();
        private final AtomicInteger maxConcurrentUploads = new AtomicInteger();
        private final Map<Integer, Boolean> seenParts = new ConcurrentHashMap<>();
        private final Map<Integer, AtomicInteger> partFailures = new ConcurrentHashMap<>();
        private volatile byte[] singlePut;

        private MultipartTransport(byte[] storedObject) {
            this.storedObject = storedObject;
        }

        /** Makes {@code UploadPart} for {@code part} answer 500 the first {@code times} attempts. */
        private MultipartTransport failPart(int part, int times) {
            partFailures.put(part, new AtomicInteger(times));
            return this;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            SdkHttpFullRequest http = (SdkHttpFullRequest) request.httpRequest();
            SdkHttpMethod method = http.method();
            Optional<String> partNumber = firstQueryValue(http, "partNumber");
            boolean hasUploadId = firstQueryValue(http, "uploadId").isPresent();

            if (method == SdkHttpMethod.POST && http.rawQueryParameters().containsKey("uploads")) {
                createCount.incrementAndGet();
                return xml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                           + "<InitiateMultipartUploadResult><Bucket>wirediff-bucket</Bucket>"
                           + "<Key>big.bin</Key><UploadId>" + UPLOAD_ID + "</UploadId>"
                           + "</InitiateMultipartUploadResult>");
            }
            if (method == SdkHttpMethod.POST && hasUploadId) {
                completeCount.incrementAndGet();
                return xml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                           + "<CompleteMultipartUploadResult><Location>https://example/big.bin</Location>"
                           + "<Bucket>wirediff-bucket</Bucket><Key>big.bin</Key>"
                           + "<ETag>&quot;wirediff-final-etag&quot;</ETag>"
                           + "</CompleteMultipartUploadResult>");
            }
            if (method == SdkHttpMethod.DELETE && hasUploadId) {
                abortCount.incrementAndGet();
                return xml("");
            }
            if (method == SdkHttpMethod.PUT) {
                return uploadPart(request, partNumber);
            }
            if (method == SdkHttpMethod.GET) {
                return getPart(partNumber);
            }
            throw new IllegalStateException("unexpected request: " + method + " " + http.getUri());
        }

        private ExecutableHttpRequest uploadPart(HttpExecuteRequest request, Optional<String> partNumber) {
            // Read the body here, while the SDK still holds the stream open, and track how many uploads
            // overlap: the concurrency assertion is about the shim's pool, so it has to be observed at
            // the point where a pool thread is actually busy.
            int inFlight = concurrentUploads.incrementAndGet();
            maxConcurrentUploads.accumulateAndGet(inFlight, Math::max);
            try {
                byte[] body = readBody(request);
                if (partNumber.isPresent()) {
                    int part = Integer.parseInt(partNumber.get());
                    AtomicInteger remaining = partFailures.get(part);
                    if (remaining != null && remaining.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                        // Read the body first, above, so the failure looks like a server-side one after a
                        // complete upload rather than a transport that never consumed the request.
                        return response(500, ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error>"
                                              + "<Code>InternalError</Code><Message>injected</Message>"
                                              + "<RequestId>WIREDIFF000000000</RequestId></Error>")
                                                 .getBytes(StandardCharsets.UTF_8),
                                        "application/xml", Map.of());
                    }
                    if (seenParts.putIfAbsent(part, Boolean.TRUE) != null) {
                        throw new IllegalStateException("part " + part + " was uploaded twice");
                    }
                    uploadedParts.put(part, body);
                    return response(200, new byte[0], "application/xml",
                                    Map.of("ETag", "\"wirediff-part-" + part + "\""));
                }
                singlePut = body;
                return response(200, new byte[0], "application/xml",
                                Map.of("ETag", "\"wirediff-single\""));
            } finally {
                concurrentUploads.decrementAndGet();
            }
        }

        private ExecutableHttpRequest getPart(Optional<String> partNumber) {
            int part = partNumber.map(Integer::parseInt).orElse(1);
            synchronized (downloadedParts) {
                downloadedParts.add(part);
            }
            int offset = (int) ((part - 1) * PART_SIZE);
            int length = (int) Math.min(PART_SIZE, storedObject.length - offset);
            byte[] body = new byte[length];
            System.arraycopy(storedObject, offset, body, 0, length);
            int totalParts = (int) Math.ceil(storedObject.length / (double) PART_SIZE);
            // x-amz-mp-parts-count is how v2's downloader learns there is more than one part; without it
            // the download stays single-GET and the multipart download path is never exercised.
            return response(200, body, "application/octet-stream",
                            Map.of("x-amz-mp-parts-count", String.valueOf(totalParts),
                                   "ETag", "\"wirediff-part-" + part + "\""));
        }

        private byte[] reassembleUpload() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream(storedObject.length);
            for (Map.Entry<Integer, byte[]> part : uploadedParts.entrySet()) {
                out.write(part.getValue());
            }
            return out.toByteArray();
        }

        private static Optional<String> firstQueryValue(SdkHttpFullRequest request, String name) {
            List<String> values = request.rawQueryParameters().get(name);
            return values == null || values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
        }

        private static byte[] readBody(HttpExecuteRequest request) {
            Optional<ContentStreamProvider> provider = request.contentStreamProvider();
            if (provider.isEmpty()) {
                return new byte[0];
            }
            try (InputStream in = provider.get().newStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new IllegalStateException("failed reading a request body", e);
            }
        }

        private static ExecutableHttpRequest xml(String body) {
            return response(200, body.getBytes(StandardCharsets.UTF_8), "application/xml", Map.of());
        }

        private static ExecutableHttpRequest response(int status, byte[] body, String contentType,
                                                      Map<String, String> headers) {
            SdkHttpResponse.Builder builder = SdkHttpResponse.builder()
                                                             .statusCode(status)
                                                             .putHeader("Content-Type", contentType)
                                                             .putHeader("Content-Length",
                                                                        String.valueOf(body.length))
                                                             .putHeader("x-amz-request-id", "WIREDIFF000000000");
            headers.forEach(builder::putHeader);
            SdkHttpResponse built = builder.build();
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                                              .response(built)
                                              .responseBody(AbortableInputStream.create(
                                                  new ByteArrayInputStream(body)))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public String clientName() {
            return "WireDiffMultipart";
        }

        @Override
        public void close() {
        }
    }
}
