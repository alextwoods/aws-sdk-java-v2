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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * An in-memory S3 behind the public {@link S3AsyncClient} interface (a dynamic proxy: the transfer manager can only
 * use public client methods). It consumes request bodies and produces response bodies with exact one-chunk demand,
 * completes calls on its own threads (as the real client completes on its completion executor), and observes
 * cancellation of every call it returns.
 *
 * <p>Objects are either stored bytes, or "generated" objects whose byte {@code i} is {@link #generatedByte(long)},
 * which are never materialized, so bounded-memory tests can stream objects larger than the heap. With
 * {@link #digestOnly(boolean)}, uploaded bodies are hashed instead of stored.
 *
 * <p>Fault injection: {@link #failWith} decides, per call, whether the call fails before any data moves;
 * {@link #failStreamAfter} breaks a {@code GetObject} payload after a number of bytes on its first attempt and then
 * retries the attempt (calling {@code prepare()} again, as the client's retry does); {@link #gate} holds calls until
 * a future completes.
 */
final class FakeS3 implements InvocationHandler {
    static final int CHUNK = 8 * 1024;
    private static final Instant LAST_MODIFIED = Instant.parse("2024-01-01T00:00:00Z");

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();
    private final Map<String, Upload> uploads = new ConcurrentHashMap<>();
    private final AtomicInteger uploadIds = new AtomicInteger();
    private final ExecutorService responders = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "fake-s3-responder");
        t.setDaemon(true);
        return t;
    });

    final List<String> calls = new CopyOnWriteArrayList<>();
    final List<Object> requests = new CopyOnWriteArrayList<>();
    final Set<String> abortedUploads = ConcurrentHashMap.newKeySet();
    final AtomicInteger cancelledCalls = new AtomicInteger();
    final AtomicInteger inFlightParts = new AtomicInteger();
    final AtomicInteger maxInFlightParts = new AtomicInteger();
    final AtomicInteger getObjectAttempts = new AtomicInteger();

    private volatile Function<Object, Throwable> failWith = r -> null;
    private volatile Function<Object, CompletableFuture<?>> gate = r -> CompletableFuture.completedFuture(null);
    private volatile Function<GetObjectRequest, Long> failStreamAfter = r -> null;
    private volatile boolean digestOnly;
    private volatile int listPageSize = 1000;

    private final S3AsyncClient client = (S3AsyncClient) Proxy.newProxyInstance(
        S3AsyncClient.class.getClassLoader(), new Class<?>[] {S3AsyncClient.class}, this);

    S3AsyncClient client() {
        return client;
    }

    FakeS3 failWith(Function<Object, Throwable> failWith) {
        this.failWith = failWith;
        return this;
    }

    FakeS3 gate(Function<Object, CompletableFuture<?>> gate) {
        this.gate = gate;
        return this;
    }

    FakeS3 failStreamAfter(Function<GetObjectRequest, Long> failStreamAfter) {
        this.failStreamAfter = failStreamAfter;
        return this;
    }

    FakeS3 digestOnly(boolean digestOnly) {
        this.digestOnly = digestOnly;
        return this;
    }

    FakeS3 listPageSize(int listPageSize) {
        this.listPageSize = listPageSize;
        return this;
    }

    void shutdown() {
        responders.shutdownNow();
    }

    // ------------------------------------------------------------------------------------------------ the store

    /** Stores {@code data} as a single-part object. */
    void putObject(String bucket, String key, byte[] data) {
        objects.put(bucket + "/" + key, StoredObject.of(data, List.of((long) data.length), eTagOf(data)));
    }

    /** Stores {@code data} as a multipart object with parts of {@code partSize} bytes. */
    void putMultipartObject(String bucket, String key, byte[] data, int partSize) {
        List<Long> sizes = new ArrayList<>();
        for (long offset = 0; offset < data.length; offset += partSize) {
            sizes.add(Math.min(partSize, data.length - offset));
        }
        objects.put(bucket + "/" + key, StoredObject.of(data, sizes, eTagOf(data) + "-" + sizes.size()));
    }

    /** Stores a generated multipart object of {@code size} bytes (never materialized). */
    void putGeneratedObject(String bucket, String key, long size, long partSize) {
        List<Long> sizes = new ArrayList<>();
        for (long offset = 0; offset < size; offset += partSize) {
            sizes.add(Math.min(partSize, size - offset));
        }
        objects.put(bucket + "/" + key, new StoredObject(null, size, sizes, "generated-" + size, null, LAST_MODIFIED));
    }

    /** Replaces an object's ETag (a concurrent overwrite). */
    void changeETag(String bucket, String key, String eTag) {
        objects.computeIfPresent(bucket + "/" + key, (k, o) -> new StoredObject(o.data, o.size, o.partSizes, eTag,
                                                                                o.digest, o.lastModified));
    }

    byte[] object(String bucket, String key) {
        StoredObject o = objects.get(bucket + "/" + key);
        return o == null ? null : o.data;
    }

    /** SHA-256 of a digest-only object, as hex. */
    String objectDigest(String bucket, String key) {
        StoredObject o = objects.get(bucket + "/" + key);
        return o == null ? null : o.digest;
    }

    Set<String> keys() {
        return Collections.unmodifiableSet(objects.keySet());
    }

    /** Parts stored across the open multipart uploads. */
    int storedParts() {
        return uploads.values().stream().mapToInt(u -> u.parts.size()).sum();
    }

    Set<String> openUploads() {
        return Collections.unmodifiableSet(uploads.keySet());
    }

    /** Byte {@code i} of every generated object. */
    static byte generatedByte(long i) {
        long x = i * 0x9E3779B97F4A7C15L;
        return (byte) ((x >>> 56) ^ (i >>> 13) ^ i);
    }

    static String sha256Hex(byte[] data) {
        MessageDigest d = sha256();
        return HexFormat.of().formatHex(d.digest(data));
    }

    static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String eTagOf(byte[] data) {
        return "\"" + sha256Hex(data).substring(0, 32) + "\"";
    }

    // ---------------------------------------------------------------------------------------------- dispatching

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            return switch (name) {
                case "toString" -> "FakeS3";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(name);
            };
        }
        if (name.equals("getObject") && args.length == 2 && args[1] instanceof AsyncResponseTransformer) {
            return getObject((GetObjectRequest) args[0], (AsyncResponseTransformer<GetObjectResponse, ?>) args[1]);
        }
        if (name.equals("close")) {
            return null;
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        Object request = args[0];
        return switch (name) {
            case "putObject" -> call(name, request, () -> consume((AsyncRequestBody) args[1])
                .thenApply(body -> putObject((PutObjectRequest) request, body)));
            case "createMultipartUpload" -> call(name, request, () -> CompletableFuture.completedFuture(
                create((CreateMultipartUploadRequest) request)));
            case "uploadPart" -> part((UploadPartRequest) request, (AsyncRequestBody) args[1]);
            case "completeMultipartUpload" -> call(name, request, () -> CompletableFuture.completedFuture(
                complete((CompleteMultipartUploadRequest) request)));
            case "abortMultipartUpload" -> call(name, request, () -> CompletableFuture.completedFuture(
                abort((AbortMultipartUploadRequest) request)));
            case "listParts" -> call(name, request, () -> CompletableFuture.completedFuture(
                listParts((ListPartsRequest) request)));
            case "headObject" -> call(name, request, () -> CompletableFuture.completedFuture(
                head((HeadObjectRequest) request)));
            case "listObjectsV2" -> call(name, request, () -> CompletableFuture.completedFuture(
                list((ListObjectsV2Request) request)));
            case "copyObject" -> call(name, request, () -> CompletableFuture.completedFuture(
                copy((CopyObjectRequest) request)));
            case "uploadPartCopy" -> call(name, request, () -> CompletableFuture.completedFuture(
                partCopy((UploadPartCopyRequest) request)));
            default -> throw new UnsupportedOperationException("FakeS3 does not implement " + name);
        };
    }

    private interface Action<T> {
        CompletableFuture<T> run() throws Exception;
    }

    /** Records the call, applies the gate and injected failure, and completes on a responder thread. */
    private <T> CompletableFuture<T> call(String name, Object request, Action<T> action) {
        calls.add(name);
        requests.add(request);
        CompletableFuture<T> returned = new CompletableFuture<>();
        returned.whenComplete((r, t) -> {
            if (returned.isCancelled()) {
                cancelledCalls.incrementAndGet();
            }
        });
        gate.apply(request).whenCompleteAsync((ignored, gateFailure) -> {
            if (returned.isDone()) {
                return;
            }
            Throwable injected = failWith.apply(request);
            if (injected != null) {
                returned.completeExceptionally(injected);
                return;
            }
            try {
                action.run().whenComplete((r, t) -> {
                    if (t != null) {
                        returned.completeExceptionally(t);
                    } else {
                        returned.complete(r);
                    }
                });
            } catch (Exception | AssertionError e) {
                returned.completeExceptionally(e);
            }
        }, responders);
        return returned;
    }

    // ------------------------------------------------------------------------------------------------- uploads

    private PutObjectResponse putObject(PutObjectRequest request, Body body) {
        String key = request.bucket() + "/" + request.key();
        objects.put(key, body.toObject(List.of(body.size)));
        return PutObjectResponse.builder().eTag(body.eTag()).build();
    }

    private CreateMultipartUploadResponse create(CreateMultipartUploadRequest request) {
        String id = "upload-" + uploadIds.incrementAndGet();
        uploads.put(id, new Upload(request.bucket(), request.key()));
        return CreateMultipartUploadResponse.builder().bucket(request.bucket()).key(request.key()).uploadId(id).build();
    }

    private CompletableFuture<UploadPartResponse> part(UploadPartRequest request, AsyncRequestBody body) {
        return call("uploadPart", request, () -> {
            Upload upload = uploads.get(request.uploadId());
            if (upload == null) {
                throw noSuchUpload(request.uploadId());
            }
            int now = inFlightParts.incrementAndGet();
            maxInFlightParts.accumulateAndGet(now, Math::max);
            return consume(body).whenComplete((r, t) -> inFlightParts.decrementAndGet()).thenApply(b -> {
                if (request.contentLength() != null && request.contentLength() != b.size) {
                    throw new AssertionError("Part " + request.partNumber() + " declared " + request.contentLength()
                                             + " bytes but sent " + b.size);
                }
                Upload current = uploads.get(request.uploadId());
                if (current == null) {
                    throw noSuchUpload(request.uploadId());
                }
                current.parts.put(request.partNumber(), b);
                return UploadPartResponse.builder().eTag(b.eTag()).build();
            });
        });
    }

    private CompleteMultipartUploadResponse complete(CompleteMultipartUploadRequest request) {
        Upload upload = uploads.remove(request.uploadId());
        if (upload == null) {
            throw noSuchUpload(request.uploadId());
        }
        List<CompletedPart> parts = request.multipartUpload().parts();
        MessageDigest digest = sha256();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Long> sizes = new ArrayList<>();
        int expected = 1;
        for (CompletedPart part : parts) {
            if (part.partNumber() != expected++) {
                throw new AssertionError("Parts are not consecutive and ordered: " + parts);
            }
            Body body = upload.parts.get(part.partNumber());
            if (body == null || !body.eTag().equals(part.eTag())) {
                throw new AssertionError("Part " + part.partNumber() + " was not uploaded with ETag " + part.eTag());
            }
            sizes.add(body.size);
            if (body.data != null) {
                out.writeBytes(body.data);
                digest.update(body.data);
            }
        }
        if (upload.parts.size() != parts.size()) {
            throw new AssertionError("Uploaded parts " + upload.parts.keySet() + " but completed " + parts.size());
        }
        long size = sizes.stream().mapToLong(Long::longValue).sum();
        if (request.mpuObjectSize() != null && request.mpuObjectSize() != size) {
            throw new AssertionError("MpuObjectSize " + request.mpuObjectSize() + " but parts hold " + size);
        }
        String key = upload.bucket + "/" + upload.key;
        if (digestOnly) {
            objects.put(key, new StoredObject(null, size, sizes, "mpu-" + sizes.size(),
                                              combinedPartDigests(upload, parts), LAST_MODIFIED));
        } else {
            byte[] data = out.toByteArray();
            objects.put(key, StoredObject.of(data, sizes, eTagOf(data) + "-" + sizes.size()));
        }
        return CompleteMultipartUploadResponse.builder().bucket(upload.bucket).key(upload.key)
                                              .eTag(objects.get(key).eTag).build();
    }

    /** In digest-only mode each part keeps its own digest; the object digest is the list of part digests. */
    private static String combinedPartDigests(Upload upload, List<CompletedPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (CompletedPart part : parts) {
            sb.append(upload.parts.get(part.partNumber()).digest).append(',');
        }
        return sb.toString();
    }

    private AbortMultipartUploadResponse abort(AbortMultipartUploadRequest request) {
        if (uploads.remove(request.uploadId()) == null) {
            throw noSuchUpload(request.uploadId());
        }
        abortedUploads.add(request.uploadId());
        return AbortMultipartUploadResponse.builder().build();
    }

    private ListPartsResponse listParts(ListPartsRequest request) {
        Upload upload = uploads.get(request.uploadId());
        if (upload == null) {
            throw noSuchUpload(request.uploadId());
        }
        int marker = request.partNumberMarker() == null ? 0 : Integer.parseInt(request.partNumberMarker());
        List<Part> page = new ArrayList<>();
        boolean truncated = false;
        int last = marker;
        for (Map.Entry<Integer, Body> e : new TreeMap<>(upload.parts).entrySet()) {
            if (e.getKey() <= marker) {
                continue;
            }
            if (page.size() == 2) {
                truncated = true;
                break;
            }
            page.add(Part.builder().partNumber(e.getKey()).size(e.getValue().size).eTag(e.getValue().eTag()).build());
            last = e.getKey();
        }
        return ListPartsResponse.builder().bucket(upload.bucket).key(upload.key).uploadId(request.uploadId())
                                .parts(page).isTruncated(truncated)
                                .nextPartNumberMarker(truncated ? String.valueOf(last) : null).build();
    }

    private static S3Exception noSuchUpload(String id) {
        return (S3Exception) S3Exception.builder().statusCode(404).message("NoSuchUpload " + id)
                                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchUpload").build())
                                        .build();
    }

    static S3Exception serviceError(int status, String code) {
        return (S3Exception) S3Exception.builder().statusCode(status).message(code)
                                        .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build()).build();
    }

    // ----------------------------------------------------------------------------------------------- downloads

    private StoredObject require(String bucket, String key) {
        StoredObject o = objects.get(bucket + "/" + key);
        if (o == null) {
            throw serviceError(404, "NoSuchKey");
        }
        return o;
    }

    private HeadObjectResponse head(HeadObjectRequest request) {
        StoredObject o = require(request.bucket(), request.key());
        HeadObjectResponse.Builder builder = HeadObjectResponse.builder().eTag(o.eTag).lastModified(o.lastModified);
        if (request.partNumber() != null) {
            long[] range = o.partRange(request.partNumber());
            builder.contentLength(range[1] - range[0] + 1)
                   .contentRange("bytes " + range[0] + "-" + range[1] + "/" + o.size);
            if (o.partSizes.size() > 1) {
                builder.partsCount(o.partSizes.size());
            }
        } else {
            builder.contentLength(o.size);
        }
        return builder.build();
    }

    private <R> CompletableFuture<R> getObject(GetObjectRequest request,
                                               AsyncResponseTransformer<GetObjectResponse, R> transformer) {
        calls.add("getObject");
        requests.add(request);
        CompletableFuture<R> returned = new CompletableFuture<>();
        Subscription[] current = new Subscription[1];
        returned.whenComplete((r, t) -> {
            if (returned.isCancelled()) {
                cancelledCalls.incrementAndGet();
                synchronized (current) {
                    if (current[0] != null) {
                        current[0].cancel();
                    }
                }
            }
        });
        gate.apply(request).whenCompleteAsync((ignored, gateFailure) -> {
            if (returned.isDone()) {
                return;
            }
            CompletableFuture<R> attempt = transformer.prepare();
            Long breakAfter = failStreamAfter.apply(request);
            if (breakAfter == null) {
                // Only the final attempt's result is the call's result (a retried attempt's failure is not).
                attempt.whenComplete((r, t) -> {
                    if (t != null) {
                        returned.completeExceptionally(t);
                    } else {
                        returned.complete(r);
                    }
                });
            }
            Throwable injected = failWith.apply(request);
            if (injected != null) {
                transformer.exceptionOccurred(injected);
                returned.completeExceptionally(injected);
                return;
            }
            try {
                StoredObject o = require(request.bucket(), request.key());
                if (request.ifMatch() != null && !request.ifMatch().equals(o.eTag)) {
                    throw serviceError(412, "PreconditionFailed");
                }
                long[] range = rangeOf(request, o);
                GetObjectResponse.Builder response = GetObjectResponse.builder().eTag(o.eTag)
                                                                      .lastModified(o.lastModified)
                                                                      .contentLength(range[1] - range[0] + 1);
                if (request.partNumber() != null || request.range() != null) {
                    response.contentRange("bytes " + range[0] + "-" + range[1] + "/" + o.size);
                }
                if (request.partNumber() != null && o.partSizes.size() > 1) {
                    response.partsCount(o.partSizes.size());
                }
                if (breakAfter != null) {
                    // First attempt: the connection breaks after breakAfter bytes; the client retries.
                    getObjectAttempts.incrementAndGet();
                    CompletableFuture<Void> broken = new CompletableFuture<>();
                    IllegalStateException reset = new IllegalStateException("connection reset");
                    transformer.onResponse(response.build());
                    transformer.onStream(payload(o, range[0], Math.min(range[1], range[0] + breakAfter - 1),
                                                 reset, current, broken));
                    broken.whenComplete((r, t) -> {
                        transformer.exceptionOccurred(reset);
                        CompletableFuture<R> retry = transformer.prepare();
                        retry.whenComplete((r2, t2) -> {
                            if (t2 != null) {
                                returned.completeExceptionally(t2);
                            } else {
                                returned.complete(r2);
                            }
                        });
                        getObjectAttempts.incrementAndGet();
                        transformer.onResponse(response.build());
                        transformer.onStream(payload(o, range[0], range[1], null, current, null));
                    });
                    return;
                }
                getObjectAttempts.incrementAndGet();
                transformer.onResponse(response.build());
                transformer.onStream(payload(o, range[0], range[1], null, current, null));
            } catch (RuntimeException e) {
                transformer.exceptionOccurred(e);
                returned.completeExceptionally(e);
            }
        }, responders);
        return returned;
    }

    private static long[] rangeOf(GetObjectRequest request, StoredObject o) {
        if (request.partNumber() != null) {
            return o.partRange(request.partNumber());
        }
        if (request.range() != null) {
            String spec = request.range().substring("bytes=".length());
            int dash = spec.indexOf('-');
            long first = Long.parseLong(spec.substring(0, dash));
            long last = dash == spec.length() - 1 ? o.size - 1 : Long.parseLong(spec.substring(dash + 1));
            return new long[] {first, Math.min(last, o.size - 1)};
        }
        return new long[] {0, o.size - 1};
    }

    /**
     * The bytes {@code [first, last]} of {@code o} in chunks, one chunk per unit of demand, delivered on a responder
     * thread; ends with {@code error} (completing {@code broken}) instead of onComplete when given.
     */
    private Publisher<ByteBuffer> payload(StoredObject o, long first, long last, Throwable error,
                                          Subscription[] current, CompletableFuture<Void> broken) {
        return subscriber -> {
            AtomicLong position = new AtomicLong(first);
            AtomicLong demand = new AtomicLong();
            AtomicInteger wip = new AtomicInteger();
            boolean[] done = new boolean[1];
            Subscription subscription = new Subscription() {
                private volatile boolean cancelled;

                @Override
                public void request(long n) {
                    demand.addAndGet(n);
                    drain();
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }

                private void drain() {
                    if (wip.getAndIncrement() != 0) {
                        return;
                    }
                    responders.execute(() -> {
                        int missed = 1;
                        do {
                            while (!done[0] && !cancelled && demand.get() > 0 && position.get() <= last) {
                                long start = position.get();
                                int size = (int) Math.min(CHUNK, last - start + 1);
                                byte[] chunk = new byte[size];
                                for (int i = 0; i < size; i++) {
                                    chunk[i] = o.byteAt(start + i);
                                }
                                position.addAndGet(size);
                                demand.decrementAndGet();
                                subscriber.onNext(ByteBuffer.wrap(chunk).asReadOnlyBuffer());
                            }
                            if (!done[0] && !cancelled && position.get() > last) {
                                done[0] = true;
                                if (error != null) {
                                    subscriber.onError(error);
                                    broken.complete(null);
                                } else {
                                    subscriber.onComplete();
                                }
                            }
                            missed = wip.addAndGet(-missed);
                        } while (missed != 0);
                    });
                }
            };
            synchronized (current) {
                current[0] = subscription;
            }
            subscriber.onSubscribe(subscription);
        };
    }

    private ListObjectsV2Response list(ListObjectsV2Request request) {
        String prefix = request.prefix() == null ? "" : request.prefix();
        String delimiter = request.delimiter();
        List<String> keys = new ArrayList<>();
        for (String full : new TreeMap<>(objects).keySet()) {
            if (full.startsWith(request.bucket() + "/")) {
                String key = full.substring(request.bucket().length() + 1);
                if (key.startsWith(prefix)) {
                    keys.add(key);
                }
            }
        }
        int start = request.continuationToken() == null ? 0 : Integer.parseInt(request.continuationToken());
        List<S3Object> contents = new ArrayList<>();
        List<CommonPrefix> prefixes = new ArrayList<>();
        Set<String> seenPrefixes = new java.util.HashSet<>();
        int i = start;
        for (; i < keys.size() && contents.size() + prefixes.size() < listPageSize; i++) {
            String key = keys.get(i);
            int d = delimiter == null ? -1 : key.indexOf(delimiter, prefix.length());
            if (d >= 0) {
                String common = key.substring(0, d + delimiter.length());
                if (seenPrefixes.add(common)) {
                    prefixes.add(CommonPrefix.builder().prefix(common).build());
                }
                continue;
            }
            StoredObject o = objects.get(request.bucket() + "/" + key);
            contents.add(S3Object.builder().key(key).size(o.size).eTag(o.eTag).build());
        }
        boolean truncated = i < keys.size();
        return ListObjectsV2Response.builder().name(request.bucket()).prefix(request.prefix())
                                    .delimiter(delimiter).contents(contents).commonPrefixes(prefixes)
                                    .isTruncated(truncated)
                                    .nextContinuationToken(truncated ? String.valueOf(i) : null).build();
    }

    // ---------------------------------------------------------------------------------------------------- copy

    private static String[] source(String copySource) {
        String path = copySource.startsWith("/") ? copySource.substring(1) : copySource;
        int slash = path.indexOf('/');
        return new String[] {path.substring(0, slash), path.substring(slash + 1)};
    }

    private CopyObjectResponse copy(CopyObjectRequest request) {
        String[] src = source(request.copySource());
        StoredObject o = require(src[0], src[1]);
        objects.put(request.bucket() + "/" + request.key(), o);
        return CopyObjectResponse.builder().copyObjectResult(CopyObjectResult.builder().eTag(o.eTag).build()).build();
    }

    private UploadPartCopyResponse partCopy(UploadPartCopyRequest request) {
        String[] src = source(request.copySource());
        StoredObject o = require(src[0], src[1]);
        Upload upload = uploads.get(request.uploadId());
        if (upload == null) {
            throw noSuchUpload(request.uploadId());
        }
        String spec = request.copySourceRange().substring("bytes=".length());
        long first = Long.parseLong(spec.substring(0, spec.indexOf('-')));
        long last = Long.parseLong(spec.substring(spec.indexOf('-') + 1));
        byte[] data = new byte[Math.toIntExact(last - first + 1)];
        for (int i = 0; i < data.length; i++) {
            data[i] = o.byteAt(first + i);
        }
        Body body = Body.of(data);
        upload.parts.put(request.partNumber(), body);
        return UploadPartCopyResponse.builder().copyPartResult(CopyPartResult.builder().eTag(body.eTag()).build())
                                     .build();
    }

    // --------------------------------------------------------------------------------------------------- bodies

    /** Reads a request body with one-chunk demand, storing or hashing it. */
    private CompletableFuture<Body> consume(AsyncRequestBody body) {
        CompletableFuture<Body> result = new CompletableFuture<>();
        boolean hashOnly = digestOnly;
        body.subscribe(new Subscriber<ByteBuffer>() {
            private final ByteArrayOutputStream out = new ByteArrayOutputStream();
            private final MessageDigest digest = sha256();
            private long size;
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                ByteBuffer copy = chunk.duplicate();
                size += copy.remaining();
                if (hashOnly) {
                    digest.update(copy);
                } else {
                    byte[] bytes = new byte[copy.remaining()];
                    copy.get(bytes);
                    out.writeBytes(bytes);
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                result.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                if (hashOnly) {
                    result.complete(new Body(null, size, HexFormat.of().formatHex(digest.digest())));
                } else {
                    result.complete(Body.of(out.toByteArray()));
                }
            }
        });
        return result;
    }

    private static final class Body {
        private final byte[] data;
        private final long size;
        private final String digest;

        Body(byte[] data, long size, String digest) {
            this.data = data;
            this.size = size;
            this.digest = digest;
        }

        static Body of(byte[] data) {
            return new Body(data, data.length, sha256Hex(data));
        }

        String eTag() {
            return "\"" + digest.substring(0, 32) + "\"";
        }

        StoredObject toObject(List<Long> sizes) {
            return new StoredObject(data, size, sizes, eTag(), digest, LAST_MODIFIED);
        }
    }

    private static final class Upload {
        private final String bucket;
        private final String key;
        private final Map<Integer, Body> parts = new ConcurrentHashMap<>();

        Upload(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
        }
    }

    private static final class StoredObject {
        private final byte[] data;
        private final long size;
        private final List<Long> partSizes;
        private final String eTag;
        private final String digest;
        private final Instant lastModified;

        StoredObject(byte[] data, long size, List<Long> partSizes, String eTag, String digest, Instant lastModified) {
            this.data = data;
            this.size = size;
            this.partSizes = List.copyOf(partSizes);
            this.eTag = Objects.requireNonNull(eTag);
            this.digest = digest;
            this.lastModified = lastModified;
        }

        static StoredObject of(byte[] data, List<Long> partSizes, String eTag) {
            return new StoredObject(data, data.length, partSizes, eTag, sha256Hex(data), LAST_MODIFIED);
        }

        byte byteAt(long i) {
            return data != null ? data[Math.toIntExact(i)] : generatedByte(i);
        }

        long[] partRange(int partNumber) {
            if (partNumber < 1 || partNumber > partSizes.size()) {
                throw serviceError(416, "InvalidPartNumber");
            }
            long first = 0;
            for (int i = 1; i < partNumber; i++) {
                first += partSizes.get(i - 1);
            }
            return new long[] {first, first + partSizes.get(partNumber - 1) - 1};
        }
    }
}
