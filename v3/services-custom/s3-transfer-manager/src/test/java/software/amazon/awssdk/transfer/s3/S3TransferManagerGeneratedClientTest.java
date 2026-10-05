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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.failureOf;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.join;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.publisherOf;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.randomBytes;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;

/**
 * End to end over the generated V3 {@link S3AsyncClient}: the transfer manager drives the real client pipeline
 * (serialization, signing, response handling, error unmarshalling) against an in-JVM S3 at the HTTP layer, so the
 * public-API integration itself (response transformers, multipart and copy request shapes, paginators and REST-XML
 * results) is verified, not only the transfer logic.
 *
 * <p>Uploads go through the same client: over HTTPS the S3 client signs {@code UNSIGNED-PAYLOAD}, so each
 * {@code PutObject} and {@code UploadPart} body is sent without being read first, {@code aws-chunked} with its CRC32 in
 * an unsigned trailer (which this fake S3 decodes and verifies), and a part whose transmission failed is retried by the
 * client from its replayable part body.
 */
class S3TransferManagerGeneratedClientTest {
    private static final int PART = 64 * 1024;

    private final HttpS3 http = new HttpS3();
    private final S3AsyncClient client = S3AsyncClient.builder()
                                                      .region(Region.of("us-west-2"))
                                                      .credentialsProvider(HttpS3.CREDENTIALS)
                                                      .endpointOverride(URI.create("https://localhost:8443"))
                                                      .httpClient(http)
                                                      .build();
    private final S3TransferManager tm = S3TransferManager.builder()
                                                          .s3Client(client)
                                                          .multipartThresholdInBytes((long) PART)
                                                          .minimumPartSizeInBytes((long) PART)
                                                          .maxInFlightParts(2)
                                                          .build();

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        tm.close();
        client.close();
        http.close();
    }

    @Test
    void downloadFile_multipartObject_fetchesEveryPartThroughTheGeneratedClient() throws Exception {
        byte[] data = randomBytes(PART * 3 + 1000, 1);
        http.store("bucket/dir/obj", data, PART);
        Path target = dir.resolve("out.bin");

        CompletedFileDownload done =
            join(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("bucket").key("dir/obj")).destination(target))
                   .completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(data);
        assertThat(done.response().contentLength()).isEqualTo(data.length);
        assertThat(http.log).filteredOn(l -> l.startsWith("GET bucket/dir/obj") && l.contains("partNumber=true"))
                            .hasSize(4);
    }

    @Test
    void download_toBytes_streamsThroughTheGeneratedClient() {
        http.store("bucket/small", "hello transfer manager".getBytes(StandardCharsets.UTF_8), Integer.MAX_VALUE);

        CompletedDownload<ResponseBytes<GetObjectResponse>> done =
            join(tm.download(DownloadRequest.builder()
                                            .getObjectRequest(g -> g.bucket("bucket").key("small"))
                                            .responseTransformer(AsyncResponseTransformer.<GetObjectResponse>toBytes())
                                            .build())
                   .completionFuture());

        assertThat(done.result().asUtf8String()).isEqualTo("hello transfer manager");
    }

    @Test
    void downloadFile_missingKey_surfacesTheModeledServiceError() {
        Throwable failure = failureOf(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("bucket").key("missing"))
                                                            .destination(dir.resolve("out.bin")))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(S3Exception.class);
        assertThat(((S3Exception) failure).statusCode()).isEqualTo(404);
    }

    @Test
    void copy_largeObject_usesMultipartRequestsThroughTheGeneratedClient() {
        byte[] data = randomBytes(PART * 2 + 10, 2);
        http.store("src/k", data, Integer.MAX_VALUE);

        join(tm.copy(c -> c.copyObjectRequest(r -> r.copySource("src/k").bucket("bucket").key("copy")))
               .completionFuture());

        assertThat(http.objects.get("bucket/copy")).isEqualTo(data);
        assertThat(http.log).anyMatch(l -> l.startsWith("HEAD src/k"))
                            .anyMatch(l -> l.startsWith("POST bucket/copy") && l.contains("uploads"))
                            .anyMatch(l -> l.startsWith("POST bucket/copy") && l.contains("uploadId="));
        assertThat(http.log).filteredOn(l -> l.startsWith("PUT bucket/copy") && l.contains("partNumber=true"))
                            .hasSize(3);
    }

    @Test
    void copy_partRejected_abortsTheMultipartCopyThroughTheGeneratedClient() {
        http.store("src/k", randomBytes(PART * 3, 3), Integer.MAX_VALUE);
        http.rejectPart = 2;

        Throwable failure = failureOf(tm.copy(c -> c.copyObjectRequest(r -> r.copySource("src/k").bucket("bucket")
                                                                             .key("copy")))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(S3Exception.class);
        assertThat(((S3Exception) failure).statusCode()).isEqualTo(400);
        assertThat(http.log).anyMatch(l -> l.startsWith("DELETE bucket/copy") && l.contains("uploadId="));
        assertThat(http.uploads).isEmpty();
    }

    @Test
    void downloadDirectory_listsWithTheGeneratedPaginator() throws Exception {
        http.store("bucket/a/1.txt", new byte[] {1}, Integer.MAX_VALUE);
        http.store("bucket/a/b/2.txt", new byte[] {2}, Integer.MAX_VALUE);
        Path destination = dir.resolve("dst");

        CompletedDirectoryDownload done =
            join(tm.downloadDirectory(r -> r.bucket("bucket").destination(destination)).completionFuture());

        assertThat(done.failedTransfers()).isEmpty();
        assertThat(Files.readAllBytes(destination.resolve("a/1.txt"))).containsExactly(1);
        assertThat(Files.readAllBytes(destination.resolve("a/b/2.txt"))).containsExactly(2);
        assertThat(http.log).anyMatch(l -> l.startsWith("GET bucket/") && l.contains("list-type"));
    }

    @Test
    void uploadFile_multipartObject_uploadsEveryPartThroughTheGeneratedClient() throws Exception {
        byte[] data = randomBytes(PART * 3 + 1000, 4);
        Path source = dir.resolve("in.bin");
        Files.write(source, data);

        CompletedFileUpload done =
            join(tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("bucket").key("dir/up")))
                   .completionFuture());

        assertThat(http.objects.get("bucket/dir/up")).isEqualTo(data);
        assertThat(http.partSizes.get("bucket/dir/up")).containsExactly(PART, PART, PART, 1000);
        assertThat(done.response().eTag()).isEqualTo("\"mpu\"");
        assertThat(http.log).anyMatch(l -> l.startsWith("POST bucket/dir/up") && l.contains("uploads"))
                            .anyMatch(l -> l.startsWith("POST bucket/dir/up") && l.contains("uploadId="));
        assertThat(http.log).filteredOn(l -> l.startsWith("PUT bucket/dir/up") && l.contains("partNumber=true"))
                            .hasSize(4);
        assertThat(http.trailerChecksums).as("every part carries its CRC32 trailer").hasSize(4);
    }

    @Test
    void upload_unknownLengthBody_splitsIntoPartsThroughTheGeneratedClient() {
        byte[] data = randomBytes(PART * 2 + 17, 5);

        CompletedUpload done =
            join(tm.upload(u -> u.requestBody(AsyncRequestBody.fromPublisher(publisherOf(data, 10_000)))
                                 .putObjectRequest(p -> p.bucket("bucket").key("streamed")))
                   .completionFuture());

        assertThat(done.response()).isNotNull();
        assertThat(http.objects.get("bucket/streamed")).isEqualTo(data);
        assertThat(http.log).filteredOn(l -> l.startsWith("PUT bucket/streamed") && l.contains("partNumber=true"))
                            .hasSize(3);
    }

    @Test
    void uploadFile_belowThreshold_sendsOnePutObjectThroughTheGeneratedClient() throws Exception {
        byte[] data = randomBytes(PART / 2, 6);
        Path source = dir.resolve("small.bin");
        Files.write(source, data);

        join(tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("bucket").key("small")))
               .completionFuture());

        assertThat(http.objects.get("bucket/small")).isEqualTo(data);
        assertThat(http.log).filteredOn(l -> l.startsWith("PUT bucket/small")).hasSize(1)
                            .allMatch(l -> l.contains("partNumber=false"));
    }

    @Test
    void uploadFile_partTransmissionFails_clientRetriesThePartWithIdenticalBytes() throws Exception {
        byte[] data = randomBytes(PART * 3, 7);
        Path source = dir.resolve("retried.bin");
        Files.write(source, data);
        http.failOncePart = 2;

        join(tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("bucket").key("retried")))
               .completionFuture());

        assertThat(http.objects.get("bucket/retried")).isEqualTo(data);
        assertThat(http.failedPartBodies).hasSize(1);
        byte[] secondPart = Arrays.copyOfRange(data, PART, 2 * PART);
        assertThat(http.failedPartBodies.get(0)).as("the failed transmission's bytes").isEqualTo(secondPart);
        assertThat(http.log).filteredOn(l -> l.startsWith("PUT bucket/retried") && l.contains("partNumber=true"))
                            .as("the retried part is logged once, when it succeeds").hasSize(3);
    }

    /** S3 at the HTTP layer: REST-XML requests and responses, aws-chunked request bodies. */
    static final class HttpS3 implements SdkAsyncHttpClient {
        static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS = new IdentityProvider<>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("AKID", "SECRET"));
            }
        };

        final Map<String, byte[]> objects = new ConcurrentHashMap<>();
        final Map<String, List<Integer>> partSizes = new ConcurrentHashMap<>();
        final Map<String, TreeMap<Integer, byte[]>> uploads = new ConcurrentHashMap<>();
        final List<String> log = new CopyOnWriteArrayList<>();
        final AtomicInteger ids = new AtomicInteger();
        volatile int rejectPart = -1;
        /** A part number whose first transmission fails after its body was read, as a connection reset. */
        volatile int failOncePart = -1;
        final List<byte[]> failedPartBodies = new CopyOnWriteArrayList<>();
        final List<String> trailerChecksums = new CopyOnWriteArrayList<>();

        /** Stores {@code data} as an object with parts of {@code partSize} bytes. */
        void store(String fullKey, byte[] data, int partSize) {
            List<Integer> sizes = new ArrayList<>();
            for (int offset = 0; offset < data.length; offset += partSize) {
                sizes.add(Math.min(partSize, data.length - offset));
            }
            objects.put(fullKey, data);
            partSizes.put(fullKey, sizes);
        }
        private final ExecutorService io = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "http-s3");
            t.setDaemon(true);
            return t;
        });

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            readBody(request).whenCompleteAsync((body, failure) -> {
                if (exchange.isDone()) {
                    return;
                }
                if (failure != null) {
                    if (exchange.fail(failure)) {
                        handler.onError(exchange.asTransportException(failure));
                    }
                    return;
                }
                SdkHttpResponse response;
                try {
                    byte[] decoded = decode(request, body);
                    if (failOnce(request, decoded)) {
                        HttpTransportException reset = HttpTransportException.create(
                            HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.IO, "connection reset", null);
                        if (exchange.fail(reset)) {
                            handler.onError(reset);
                        }
                        return;
                    }
                    response = respond(request, decoded);
                } catch (RuntimeException | AssertionError e) {
                    response = error(500, "InternalError", String.valueOf(e));
                }
                byte[] payload = response.body().asByteArray();
                handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
                handler.onStream(chunks(payload, exchange));
            }, io);
            return exchange;
        }

        private CompletableFuture<byte[]> readBody(SdkHttpRequest request) {
            HttpBody body = request.body();
            if (body.kind() != HttpBody.Kind.STREAMING) {
                return CompletableFuture.completedFuture(body.asByteArray());
            }
            CompletableFuture<byte[]> result = new CompletableFuture<>();
            body.streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                private final ByteArrayOutputStream out = new ByteArrayOutputStream();
                private Subscription s;

                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer b) {
                    byte[] bytes = new byte[b.remaining()];
                    b.get(bytes);
                    out.writeBytes(bytes);
                    s.request(1);
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    result.complete(out.toByteArray());
                }
            });
            return result;
        }

        /** Whether this is the first transmission of part {@link #failOncePart}, which then fails. */
        private boolean failOnce(SdkHttpRequest request, byte[] decoded) {
            int part = failOncePart;
            if (part < 0) {
                return false;
            }
            for (HttpQueryParameter p : request.queryParameters()) {
                if (p.rawName().equals("partNumber") && String.valueOf(part).equals(p.rawValue())) {
                    failOncePart = -1;
                    failedPartBodies.add(decoded);
                    return true;
                }
            }
            return false;
        }

        /**
         * Removes aws-chunked framing from a request payload, verifying an {@code x-amz-checksum-crc32} trailer
         * against the decoded data.
         */
        private byte[] decode(SdkHttpRequest request, byte[] body) {
            boolean chunked = request.headers().firstValue("Content-Encoding").map(v -> v.contains("aws-chunked"))
                                     .orElse(false)
                              || request.headers().firstValue("x-amz-content-sha256")
                                        .map(v -> v.startsWith("STREAMING-")).orElse(false);
            if (!chunked) {
                return body;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int pos = 0;
            while (true) {
                int lineEnd = indexOfCrlf(body, pos);
                String header = new String(body, pos, lineEnd - pos, StandardCharsets.US_ASCII);
                int size = Integer.parseInt(header.split(";")[0].trim(), 16);
                pos = lineEnd + 2;
                if (size == 0) {
                    byte[] decoded = out.toByteArray();
                    String trailer = new String(body, pos, body.length - pos, StandardCharsets.US_ASCII).strip();
                    if (trailer.startsWith("x-amz-checksum-crc32:")) {
                        CRC32 crc = new CRC32();
                        crc.update(decoded);
                        String expected = Base64.getEncoder().encodeToString(
                            ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
                        if (!trailer.equals("x-amz-checksum-crc32:" + expected)) {
                            throw new AssertionError("Trailer " + trailer + " does not match the payload");
                        }
                        trailerChecksums.add(expected);
                    }
                    return decoded;
                }
                out.write(body, pos, size);
                pos += size + 2;
            }
        }

        private static int indexOfCrlf(byte[] data, int from) {
            for (int i = from; i < data.length - 1; i++) {
                if (data[i] == '\r' && data[i + 1] == '\n') {
                    return i;
                }
            }
            throw new AssertionError("Malformed aws-chunked body");
        }

        private SdkHttpResponse respond(SdkHttpRequest request, byte[] body) {
            String bucket = request.host().substring(0, request.host().indexOf('.'));
            String path = URLDecoder.decode(request.encodedPath(), StandardCharsets.UTF_8);
            // Known candidate defect (S3GeneratedApiTest): the bucket label also stays in the path of a
            // virtual-hosted request. Accept both forms.
            if (path.equals("/" + bucket) || path.startsWith("/" + bucket + "/")) {
                path = path.substring(bucket.length() + 1);
            }
            String key = path.isEmpty() ? "" : path.substring(1);
            Map<String, String> query = new TreeMap<>();
            for (HttpQueryParameter p : request.queryParameters()) {
                query.put(URLDecoder.decode(p.rawName(), StandardCharsets.UTF_8),
                          p.rawValue() == null ? "" : URLDecoder.decode(p.rawValue(), StandardCharsets.UTF_8));
            }
            log.add(request.method() + " " + bucket + "/" + key + " " + query.keySet() + " partNumber="
                    + query.containsKey("partNumber") + (query.containsKey("uploadId") ? " uploadId=" : "")
                    + (query.containsKey("uploads") ? " uploads" : ""));
            String full = bucket + "/" + key;
            switch (request.method()) {
                case "PUT" -> {
                    String copySource = request.headers().firstValue("x-amz-copy-source").orElse(null);
                    if (query.containsKey("partNumber")) {
                        int part = Integer.parseInt(query.get("partNumber"));
                        if (part == rejectPart) {
                            return error(400, "InvalidRequest", "rejected part");
                        }
                        TreeMap<Integer, byte[]> upload = uploads.get(query.get("uploadId"));
                        if (upload == null) {
                            return error(404, "NoSuchUpload", "no upload");
                        }
                        if (copySource != null) {
                            byte[] source = objects.get(URLDecoder.decode(copySource, StandardCharsets.UTF_8)
                                                                  .replaceFirst("^/", ""));
                            String range = request.headers().firstValue("x-amz-copy-source-range").orElseThrow();
                            String[] bounds = range.substring("bytes=".length()).split("-");
                            upload.put(part, java.util.Arrays.copyOfRange(source, Integer.parseInt(bounds[0]),
                                                                          Integer.parseInt(bounds[1]) + 1));
                            return xml("<CopyPartResult><ETag>\"c" + part + "\"</ETag></CopyPartResult>");
                        }
                        upload.put(part, body);
                        return SdkHttpResponse.builder().statusCode(200).addHeader("ETag", "\"p" + part + "\"")
                                              .body(HttpBody.empty()).build();
                    }
                    if (copySource != null) {
                        byte[] source = objects.get(URLDecoder.decode(copySource, StandardCharsets.UTF_8)
                                                              .replaceFirst("^/", ""));
                        store(full, source, Integer.MAX_VALUE);
                        return xml("<CopyObjectResult><ETag>\"copied\"</ETag></CopyObjectResult>");
                    }
                    store(full, body, Integer.MAX_VALUE);
                    return SdkHttpResponse.builder().statusCode(200).addHeader("ETag", "\"e\"").body(HttpBody.empty())
                                          .build();
                }
                case "POST" -> {
                    if (query.containsKey("uploads")) {
                        String id = "u" + ids.incrementAndGet();
                        uploads.put(id, new TreeMap<>());
                        return xml("<InitiateMultipartUploadResult><Bucket>" + bucket + "</Bucket><Key>" + key
                                   + "</Key><UploadId>" + id + "</UploadId></InitiateMultipartUploadResult>");
                    }
                    TreeMap<Integer, byte[]> upload = uploads.remove(query.get("uploadId"));
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    List<Integer> sizes = new ArrayList<>();
                    upload.values().forEach(p -> {
                        out.writeBytes(p);
                        sizes.add(p.length);
                    });
                    String completion = new String(body, StandardCharsets.UTF_8);
                    for (int part : upload.keySet()) {
                        if (!completion.contains("<PartNumber>" + part + "</PartNumber>")) {
                            throw new AssertionError("Part " + part + " missing from " + completion);
                        }
                    }
                    objects.put(full, out.toByteArray());
                    partSizes.put(full, sizes);
                    return xml("<CompleteMultipartUploadResult><Bucket>" + bucket + "</Bucket><Key>" + key
                               + "</Key><ETag>\"mpu\"</ETag></CompleteMultipartUploadResult>");
                }
                case "DELETE" -> {
                    uploads.remove(query.get("uploadId"));
                    return SdkHttpResponse.builder().statusCode(204).body(HttpBody.empty()).build();
                }
                case "HEAD" -> {
                    byte[] data = objects.get(full);
                    if (data == null) {
                        return SdkHttpResponse.builder().statusCode(404).body(HttpBody.empty()).build();
                    }
                    return SdkHttpResponse.builder().statusCode(200).addHeader("ETag", "\"mpu\"")
                                          .addHeader("Content-Length", String.valueOf(data.length))
                                          .body(HttpBody.empty()).build();
                }
                case "GET" -> {
                    if (query.containsKey("list-type")) {
                        StringBuilder listing = new StringBuilder("<ListBucketResult><Name>" + bucket + "</Name>"
                                                                  + "<IsTruncated>false</IsTruncated>");
                        new TreeMap<>(objects).forEach((k, v) -> {
                            if (k.startsWith(bucket + "/")) {
                                listing.append("<Contents><Key>").append(k.substring(bucket.length() + 1))
                                       .append("</Key><Size>").append(v.length).append("</Size></Contents>");
                            }
                        });
                        return xml(listing.append("</ListBucketResult>").toString());
                    }
                    byte[] data = objects.get(full);
                    if (data == null) {
                        return error(404, "NoSuchKey", "no key");
                    }
                    List<Integer> sizes = partSizes.get(full);
                    int part = Integer.parseInt(query.getOrDefault("partNumber", "0"));
                    if (part == 0) {
                        return SdkHttpResponse.builder().statusCode(200).addHeader("ETag", "\"mpu\"")
                                              .addHeader("Content-Length", String.valueOf(data.length))
                                              .body(HttpBody.fromBytes(data)).build();
                    }
                    if (sizes.size() == 1 && part == 1) {
                        return SdkHttpResponse.builder().statusCode(206).addHeader("ETag", "\"mpu\"")
                                              .addHeader("Content-Length", String.valueOf(data.length))
                                              .addHeader("Content-Range", "bytes 0-" + (data.length - 1) + "/"
                                                                          + data.length)
                                              .body(HttpBody.fromBytes(data)).build();
                    }
                    int first = 0;
                    for (int i = 1; i < part; i++) {
                        first += sizes.get(i - 1);
                    }
                    int length = sizes.get(part - 1);
                    byte[] slice = java.util.Arrays.copyOfRange(data, first, first + length);
                    return SdkHttpResponse.builder().statusCode(206).addHeader("ETag", "\"mpu\"")
                                          .addHeader("Content-Length", String.valueOf(length))
                                          .addHeader("Content-Range", "bytes " + first + "-" + (first + length - 1)
                                                                      + "/" + data.length)
                                          .addHeader("x-amz-mp-parts-count", String.valueOf(sizes.size()))
                                          .body(HttpBody.fromBytes(slice)).build();
                }
                default -> throw new AssertionError("Unexpected " + request.method());
            }
        }

        private static SdkHttpResponse xml(String body) {
            return SdkHttpResponse.builder().statusCode(200).addHeader("Content-Type", "application/xml")
                                  .body(HttpBody.fromUtf8("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + body)).build();
        }

        private static SdkHttpResponse error(int status, String code, String message) {
            return SdkHttpResponse.builder().statusCode(status).addHeader("Content-Type", "application/xml")
                                  .body(HttpBody.fromUtf8("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>"
                                                          + code + "</Code><Message>" + message
                                                          + "</Message></Error>")).build();
        }

        /** The payload in 16 KiB chunks with exact demand; completes the exchange after onComplete. */
        private Publisher<ByteBuffer> chunks(byte[] payload, AsyncExchangeCompletion exchange) {
            return subscriber -> subscriber.onSubscribe(new Subscription() {
                private int position;
                private boolean done;

                @Override
                public synchronized void request(long n) {
                    for (long i = 0; i < n && !done; i++) {
                        if (position >= payload.length) {
                            done = true;
                            subscriber.onComplete();
                            exchange.complete();
                            return;
                        }
                        int size = Math.min(16 * 1024, payload.length - position);
                        ByteBuffer chunk = ByteBuffer.wrap(payload, position, size).asReadOnlyBuffer();
                        position += size;
                        subscriber.onNext(chunk);
                    }
                    if (!done && position >= payload.length) {
                        done = true;
                        subscriber.onComplete();
                        exchange.complete();
                    }
                }

                @Override
                public synchronized void cancel() {
                    done = true;
                }
            });
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "HttpS3";
        }

        @Override
        public void close() {
            io.shutdownNow();
        }
    }
}
