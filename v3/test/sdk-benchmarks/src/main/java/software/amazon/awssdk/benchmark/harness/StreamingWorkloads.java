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

package software.amazon.awssdk.benchmark.harness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The shared engine of the streaming blob workloads (Req 20.3): one logical operation is a streaming upload
 * ({@code PUT}, the operation's streaming blob input member) of {@link #STREAM_BYTES} bytes followed by a streaming
 * download ({@code GET}, the streaming blob output member) of the same bytes, on the cell's client path and transport.
 *
 * <p>Byte exactness is checked on both sides of the wire. The mock service accepts an upload only if its body equals
 * the fixture's content byte for byte (anything else is {@code unmatched}, which invalidates the run), and serves
 * the content as the download. The client consumes every download as a stream, never buffering it: each operation
 * counts the bytes it read and fails unless the count is exact; {@link CellWorkload#verify()} additionally compares
 * every byte, in order, with the content before timing starts.
 *
 * <p>Streaming cells carry no finite payload class ({@link WorkloadKind}): the stream length is the fixed
 * {@link #STREAM_BYTES}, larger than the large finite class, and the fixture's {@link ServiceFixture#encodedBytes()}
 * is the upload plus the upload response plus the download. The 1 GiB / 256 MiB-heap transfer of Req 20.20 is a
 * separate release-gate check, not a harness cell.
 */
final class StreamingWorkloads {
    /** The stream length of every streaming cell: 1 MiB. */
    static final int STREAM_BYTES = 1 << 20;
    static final String OCTET_STREAM = "application/octet-stream";
    private static final byte[] CONTENT = content(STREAM_BYTES);

    private StreamingWorkloads() {
    }

    /**
     * Deterministic content whose byte values are not periodic in any short period, so a dropped, duplicated or
     * reordered chunk changes the bytes at its offset (SplitMix64 words, little-endian).
     */
    static byte[] content(int length) {
        byte[] bytes = new byte[length];
        long state = 0x5EED_BEEF_CAFEL;
        for (int i = 0; i < length; i += 8) {
            long z = state += 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            z ^= z >>> 31;
            for (int b = 0; b < 8 && i + b < length; b++) {
                bytes[i + b] = (byte) (z >>> (8 * b));
            }
        }
        return bytes;
    }

    /** The shared content; callers must not modify it. */
    static byte[] sharedContent() {
        return CONTENT;
    }

    /**
     * The mock-service half: the upload is recognized by method and path and accepted only with the exact content;
     * the download by method and path with an empty body.
     */
    static final class StreamFixture implements ServiceFixture {
        private final String uploadPath;
        private final String downloadPath;
        private final MockResponse uploadResponse;
        private final MockResponse downloadResponse;
        private final MockResponse modeledError;
        private final MockResponse retryableError;

        /**
         * @param downloadHeaders the download response's headers besides {@code Content-Type} and the length
         */
        StreamFixture(String uploadPath, String downloadPath, MockResponse uploadResponse,
                      Map<String, String> downloadHeaders, MockResponse modeledError, MockResponse retryableError) {
            this.uploadPath = uploadPath;
            this.downloadPath = downloadPath;
            this.uploadResponse = uploadResponse;
            this.downloadResponse = new MockResponse(200, OCTET_STREAM, CONTENT, downloadHeaders);
            this.modeledError = modeledError;
            this.retryableError = retryableError;
        }

        @Override
        public boolean matches(String method, String path, String routingHeader) {
            return "PUT".equals(method) && uploadPath.equals(path) || "GET".equals(method) && downloadPath.equals(path);
        }

        @Override
        public boolean matchesBody(String method, byte[] body) {
            return "PUT".equals(method) ? Arrays.equals(CONTENT, body) : body.length == 0;
        }

        @Override
        public String routingHeaderName() {
            return null;
        }

        @Override
        public MockResponse success() {
            return downloadResponse;
        }

        @Override
        public MockResponse success(String method) {
            return "PUT".equals(method) ? uploadResponse : downloadResponse;
        }

        @Override
        public MockResponse modeledError() {
            return modeledError;
        }

        @Override
        public MockResponse retryableError() {
            return retryableError;
        }

        @Override
        public long encodedBytes() {
            return 2L * CONTENT.length + uploadResponse.body().length;
        }
    }

    /**
     * One client path's upload and download. {@code upload} returns the decoded upload output; {@code download}
     * consumes the streamed download with the given sync transformer or async transformer factory and returns the
     * number of bytes read.
     */
    interface Paths extends AutoCloseable {
        Object upload();

        long download(boolean exact);

        CompletableFuture<Object> uploadAsync();

        CompletableFuture<Long> downloadAsync(boolean exact);
    }

    /** A protocol's streaming provider: one service's streaming upload and download on its four client paths. */
    abstract static class Provider implements WorkloadProvider {
        private final String name;
        private final BenchProtocol protocol;
        private final String modelKey;
        private final ShapeId service;
        private final String uploadOperation;
        private final String downloadOperation;

        Provider(String name, BenchProtocol protocol, String modelKey, String service, String uploadOperation,
                 String downloadOperation) {
            this.name = name;
            this.protocol = protocol;
            this.modelKey = modelKey;
            this.service = ShapeId.from(service);
            this.uploadOperation = uploadOperation;
            this.downloadOperation = downloadOperation;
        }

        @Override
        public final String name() {
            return name;
        }

        @Override
        public boolean supports(BenchmarkCell cell) {
            return cell.protocol() == protocol && cell.kind() == WorkloadKind.STREAMING;
        }

        @Override
        public abstract StreamFixture fixture(BenchmarkCell cell);

        /** The generated client's upload and download on {@code http}. */
        abstract Paths generated(boolean async, URI endpoint, AutoCloseable http);

        /** The Dynamic Client input of both operations (the object's identifying members). */
        abstract Document dynamicInput();

        /** Checks the decoded generated or dynamic upload output; throws if it is not the fixture's. */
        abstract void checkUploadOutput(Object output);

        Path defaultModel() {
            return Path.of("../../services/" + modelKey + "/src/main/smithy/model.json");
        }

        @Override
        public CellWorkload create(BenchmarkCell cell, URI endpoint, Options options) {
            AutoCloseable transport = cell.transport().create(cell.concurrency());
            try {
                boolean async = cell.path().mode() == HttpTransportMode.ASYNC;
                Paths paths = cell.path().isDynamic() ? dynamic(async, endpoint, transport, options)
                                                      : generated(async, endpoint, transport);
                return new StreamWorkload(cell, transport, paths, async, this::checkUploadOutput);
            } catch (RuntimeException e) {
                try {
                    transport.close();
                } catch (Exception ignored) {
                    // best effort while failing construction
                }
                throw e;
            }
        }

        private Paths dynamic(boolean async, URI endpoint, AutoCloseable http, Options options) {
            Path model = options.models().getOrDefault(modelKey, defaultModel());
            Document input = dynamicInput();
            if (async) {
                DynamicAsyncClient client = DynamicAsyncClient.builder().model(model).serviceId(service)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkAsyncHttpClient) http).build();
                return asyncPaths(client,
                                  () -> client.callAsync(uploadOperation, input, asyncBody()).thenApply(o -> o),
                                  exact -> client.callAsync(downloadOperation, input, asyncConsumer(exact)));
            }
            DynamicClient client = DynamicClient.builder().model(model).serviceId(service).region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
            return syncPaths(client, () -> client.call(uploadOperation, input, syncBody()),
                             exact -> client.call(downloadOperation, input, syncConsumer(exact)));
        }
    }

    /** The replayable sync upload body over the shared content, without copying it per call. */
    static RequestBody syncBody() {
        return RequestBody.fromContentProvider(() -> new ByteArrayInputStream(CONTENT), CONTENT.length, OCTET_STREAM);
    }

    /** The async upload body over the shared content, without copying it per call (the SDK never writes to it). */
    static AsyncRequestBody asyncBody() {
        return AsyncRequestBody.fromBytesUnsafe(CONTENT);
    }

    /**
     * A sync download consumer: reads the live stream through an 8 KiB buffer, counting bytes, and with
     * {@code exact} compares every byte with the content at its offset.
     */
    static <R> ResponseTransformer<R, Long> syncConsumer(boolean exact) {
        return (response, in) -> consume(in, exact);
    }

    static long consume(InputStream in, boolean exact) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (exact && (total + n > CONTENT.length
                          || Arrays.mismatch(buffer, 0, n, CONTENT, (int) total, (int) total + n) != -1)) {
                throw new IllegalStateException("downloaded bytes differ from the content at or after offset " + total);
            }
            total += n;
        }
        return total;
    }

    /**
     * An async download consumer: demands one chunk at a time (so nothing beyond one chunk is buffered), counts bytes
     * and with {@code exact} compares every byte with the content at its offset.
     */
    static <R> AsyncResponseTransformer<R, Long> asyncConsumer(boolean exact) {
        return new AsyncResponseTransformer<>() {
            private CompletableFuture<Long> result;

            @Override
            public CompletableFuture<Long> prepare() {
                result = new CompletableFuture<>();
                return result;
            }

            @Override
            public void onResponse(R response) {
                // the output has no member the workload checks; the bytes are checked as they stream
            }

            @Override
            public void onStream(Publisher<ByteBuffer> publisher) {
                CompletableFuture<Long> target = result;
                publisher.subscribe(new Subscriber<ByteBuffer>() {
                    private Subscription subscription;
                    private long total;

                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription = s;
                        s.request(1);
                    }

                    @Override
                    public void onNext(ByteBuffer chunk) {
                        int n = chunk.remaining();
                        if (exact && (total + n > CONTENT.length
                                      || chunk.mismatch(ByteBuffer.wrap(CONTENT, (int) total, n)) != -1)) {
                            subscription.cancel();
                            target.completeExceptionally(new IllegalStateException(
                                "downloaded bytes differ from the content at or after offset " + total));
                            return;
                        }
                        total += n;
                        subscription.request(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        target.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        target.complete(total);
                    }
                });
            }

            @Override
            public void exceptionOccurred(Throwable error) {
                if (result != null) {
                    result.completeExceptionally(error);
                }
            }
        };
    }

    /** Sync {@link Paths} over a client and its two calls. */
    static Paths syncPaths(AutoCloseable client, java.util.function.Supplier<Object> upload,
                           Function<Boolean, Long> download) {
        return new Paths() {
            @Override
            public Object upload() {
                return upload.get();
            }

            @Override
            public long download(boolean exact) {
                return download.apply(exact);
            }

            @Override
            public CompletableFuture<Object> uploadAsync() {
                throw new UnsupportedOperationException("sync path");
            }

            @Override
            public CompletableFuture<Long> downloadAsync(boolean exact) {
                throw new UnsupportedOperationException("sync path");
            }

            @Override
            public void close() throws Exception {
                client.close();
            }
        };
    }

    /** Async {@link Paths} over a client and its two calls. */
    static Paths asyncPaths(AutoCloseable client, java.util.function.Supplier<CompletableFuture<Object>> upload,
                            Function<Boolean, CompletableFuture<Long>> download) {
        return new Paths() {
            @Override
            public Object upload() {
                throw new UnsupportedOperationException("async path");
            }

            @Override
            public long download(boolean exact) {
                throw new UnsupportedOperationException("async path");
            }

            @Override
            public CompletableFuture<Object> uploadAsync() {
                return upload.get();
            }

            @Override
            public CompletableFuture<Long> downloadAsync(boolean exact) {
                return download.apply(exact);
            }

            @Override
            public void close() throws Exception {
                client.close();
            }
        };
    }

    /** One streaming cell's client workload: upload, then download, with the checks of the class comment. */
    static final class StreamWorkload implements CellWorkload {
        private final BenchmarkCell cell;
        private final AutoCloseable transport;
        private final Paths paths;
        private final boolean async;
        private final java.util.function.Consumer<Object> uploadCheck;

        StreamWorkload(BenchmarkCell cell, AutoCloseable transport, Paths paths, boolean async,
                       java.util.function.Consumer<Object> uploadCheck) {
            this.cell = cell;
            this.transport = transport;
            this.paths = paths;
            this.async = async;
            this.uploadCheck = uploadCheck;
        }

        @Override
        public boolean isAsync() {
            return async;
        }

        @Override
        public String transportClass() {
            return transport.getClass().getName();
        }

        private void checkCount(long count) {
            if (count != CONTENT.length) {
                throw new UnexpectedOutcomeException(cell, new IllegalStateException(
                    "downloaded " + count + " bytes, expected " + CONTENT.length));
            }
        }

        private static RuntimeException unwrap(Throwable t) {
            Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
            return cause instanceof RuntimeException r ? r : new CompletionException(cause);
        }

        @Override
        public void call() {
            paths.upload();
            checkCount(paths.download(false));
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            if (!async) {
                throw new UnsupportedOperationException("sync path");
            }
            return paths.uploadAsync().thenCompose(o -> paths.downloadAsync(false)).thenAccept(this::checkCount);
        }

        @Override
        public void verify() {
            try {
                Object output = async ? paths.uploadAsync().join() : paths.upload();
                uploadCheck.accept(output);
                checkCount(async ? paths.downloadAsync(true).join() : paths.download(true));
            } catch (CompletionException e) {
                throw unwrap(e);
            }
        }

        @Override
        public void close() throws Exception {
            try {
                paths.close();
            } finally {
                transport.close();
            }
        }
    }
}
