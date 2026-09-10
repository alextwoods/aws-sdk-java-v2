package software.amazon.awssdk.wirediff;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;

/**
 * An {@link S3AsyncClient} whose calls run on a thread pool against a blocking {@link S3Client}.
 *
 * <h2>Why this exists</h2>
 *
 * <p>The bridge generates a smithy-java-backed <em>sync</em> client only; the generated async client is
 * still stock v2 and does not touch smithy-java at all. Multipart is reached exclusively through
 * {@code MultipartS3AsyncClient}, which decorates an {@code S3AsyncClient}. So without something like
 * this, multipart on the bridged pipeline is not testable — not because multipart is hard, but because
 * the entry point is async and the pipeline underneath is not.
 *
 * <p>This shim makes the entry point fit: every operation is submitted to an executor and the blocking
 * client is called there, so {@code MultipartS3AsyncClient} runs completely unmodified on top of the
 * bridged pipeline. It is deliberately a shim and not a design proposal — see the caveats.
 *
 * <h2>Caveats, which are the point of the exercise</h2>
 *
 * <ol>
 *   <li><b>One pool thread is held for the entire duration of every call</b>, including the whole
 *       transfer of a body. A real async client holds no thread while bytes are in flight. For multipart
 *       that means the pool size, not the configured concurrency, is the real ceiling on parts in
 *       flight: with {@code MultipartConfiguration} asking for 16 concurrent parts and a pool of 8, 8
 *       parts run. Undersize the pool and the upload does not fail, it silently serializes.</li>
 *   <li><b>A body-producing publisher must not run on this executor.</b> The pool thread blocks reading
 *       the {@link AsyncRequestBody}; if the code feeding that publisher is itself queued behind the
 *       same pool, the two deadlock. Nothing detects this — it presents as a hang.</li>
 *   <li><b>Request bodies are not replayable unless the publisher is.</b> A retry needs a second
 *       {@code InputStream} from the {@link RequestBody}'s provider, which means subscribing to the
 *       {@code AsyncRequestBody} again. Bodies that support that (byte arrays, files) retry fine;
 *       one-shot publishers — which is what a split multipart body is — fail on the second subscribe
 *       instead of retrying.</li>
 *   <li><b>A transformer that defers consumption pins a pool thread for as long as the caller holds the
 *       stream.</b> {@code toBlockingInputStream} does work — the publisher below reads on whichever
 *       thread calls {@code request(n)}, which for that transformer is the caller draining the stream —
 *       but the pool thread has to stay parked inside the blocking {@code getObject} until the stream
 *       ends, because returning would close the body. A caller that reads slowly, or never closes,
 *       occupies a pool slot indefinitely; on a small pool that is the whole client. {@code toBytes},
 *       {@code toFile} and the split transformers multipart uses consume eagerly and release
 *       immediately.</li>
 *   <li><b>No cancellation.</b> Cancelling the returned future does not abort the HTTP request; the pool
 *       thread runs the call to completion.</li>
 * </ol>
 *
 * <p>Caveats 1 and 2 are properties of any sync-backed async façade and would apply to a generated one
 * as well. They are the reason this is a prototype for measuring and not a shipping strategy.
 */
public final class SyncBackedS3AsyncClient implements S3AsyncClient {

    private static final int CHUNK_BYTES = 64 * 1024;

    private final S3Client sync;
    private final ExecutorService executor;
    private final boolean ownsExecutor;

    private SyncBackedS3AsyncClient(S3Client sync, ExecutorService executor, boolean ownsExecutor) {
        this.sync = sync;
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
    }

    /**
     * Wraps {@code sync} using a pool of {@code threads} daemon threads, closed with this client.
     *
     * <p>Size the pool at or above the multipart concurrency; see caveat 1.
     */
    public static SyncBackedS3AsyncClient create(S3Client sync, int threads) {
        AtomicInteger counter = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "sync-backed-s3-async-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        return new SyncBackedS3AsyncClient(sync, pool, true);
    }

    /** Wraps {@code sync} on a caller-owned executor, which this client will not shut down. */
    public static SyncBackedS3AsyncClient create(S3Client sync, ExecutorService executor) {
        return new SyncBackedS3AsyncClient(sync, executor, false);
    }

    @Override
    public String serviceName() {
        return sync.serviceName();
    }

    @Override
    public S3ServiceClientConfiguration serviceClientConfiguration() {
        return sync.serviceClientConfiguration();
    }

    @Override
    public void close() {
        if (ownsExecutor) {
            executor.shutdownNow();
        }
        sync.close();
    }

    // ---- Non-streaming operations: submit and delegate ----

    @Override
    public CompletableFuture<CreateMultipartUploadResponse> createMultipartUpload(CreateMultipartUploadRequest request) {
        return submit(() -> sync.createMultipartUpload(request));
    }

    @Override
    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        CompleteMultipartUploadRequest request) {
        return submit(() -> sync.completeMultipartUpload(request));
    }

    @Override
    public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(AbortMultipartUploadRequest request) {
        return submit(() -> sync.abortMultipartUpload(request));
    }

    @Override
    public CompletableFuture<UploadPartCopyResponse> uploadPartCopy(UploadPartCopyRequest request) {
        return submit(() -> sync.uploadPartCopy(request));
    }

    @Override
    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest request) {
        return submit(() -> sync.copyObject(request));
    }

    @Override
    public CompletableFuture<HeadObjectResponse> headObject(HeadObjectRequest request) {
        return submit(() -> sync.headObject(request));
    }

    /** Also serves {@code listPartsPaginator}, whose interface default paginates over this method. */
    @Override
    public CompletableFuture<ListPartsResponse> listParts(ListPartsRequest request) {
        return submit(() -> sync.listParts(request));
    }

    // ---- Streaming operations ----

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
        return submit(() -> sync.putObject(request, toRequestBody(body)));
    }

    @Override
    public CompletableFuture<UploadPartResponse> uploadPart(UploadPartRequest request, AsyncRequestBody body) {
        return submit(() -> sync.uploadPart(request, toRequestBody(body)));
    }

    /**
     * Drives an {@link AsyncResponseTransformer} from the blocking response stream.
     *
     * <p>The transformer is prepared on the calling thread, so the future exists before the pool thread
     * starts — a caller that composes onto it cannot miss a fast completion. Everything after that
     * happens on the pool thread, including the transformer's {@code request(n)} pulls, so no additional
     * thread is involved in moving the body.
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest request, AsyncResponseTransformer<GetObjectResponse, ReturnT> transformer) {

        CompletableFuture<ReturnT> result = transformer.prepare();
        executor.execute(() -> {
            try {
                sync.getObject(request, (response, body) -> {
                    transformer.onResponse(response);
                    BlockingBodyPublisher publisher = new BlockingBodyPublisher(body);
                    transformer.onStream(publisher);
                    // Must not return until the body is consumed: returning from a ResponseTransformer
                    // closes the stream, and anything the transformer had not yet pulled would be lost.
                    publisher.awaitDrained();
                    return null;
                });
            } catch (Throwable t) {
                transformer.exceptionOccurred(t);
                // Belt and braces: most transformers complete the future from exceptionOccurred, but
                // that is not contractual, and a future nobody completes is a hang.
                result.completeExceptionally(t);
            }
        });
        return result;
    }

    // ---- Plumbing ----

    /**
     * Runs {@code call} on the pool and completes a future with its result.
     *
     * <p>Not {@code CompletableFuture.supplyAsync}: that wraps the failure in a
     * {@code CompletionException}, while v2's async clients complete exceptionally with the
     * {@code SdkException} itself. Multipart's error handling inspects the exception it gets, so the
     * difference is behavioral rather than cosmetic.
     */
    private <T> CompletableFuture<T> submit(Supplier<T> call) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    future.complete(call.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (RuntimeException e) {
            // A rejected submission (pool shut down) has to surface as a failed future, not as a
            // synchronous throw from a method whose signature promises a future.
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Presents an {@link AsyncRequestBody} as a blocking {@link RequestBody}.
     *
     * <p>Subscribes inside the content provider rather than once up front, so a replayable publisher
     * really does replay on retry. A one-shot publisher fails its second subscribe instead (caveat 3),
     * which is at least a loud failure rather than a truncated upload.
     */
    private static RequestBody toRequestBody(AsyncRequestBody body) {
        long length = body.contentLength().orElseThrow(
            () -> SdkClientException.create("the sync-backed async shim needs a known content length; an "
                                            + "AsyncRequestBody of unknown length would have to be buffered "
                                            + "to measure it"));
        String contentType = body.contentType();
        return RequestBody.fromContentProvider(() -> {
            InputStreamSubscriber subscriber = new InputStreamSubscriber();
            body.subscribe(subscriber);
            return subscriber;
        }, length, contentType);
    }

    /**
     * Publishes a blocking {@link InputStream} to one subscriber, reading on whichever thread demands.
     *
     * <p>Reads happen inside {@code request(n)} rather than on a thread of their own, which is what keeps
     * this shim to one thread per call. That makes reentrancy the whole problem: transformers request the
     * next buffer from inside {@code onNext}, so a naive implementation recurses once per chunk and
     * overflows the stack on a large object. The usual work-in-progress counter is used instead — the
     * thread already draining absorbs the new demand and the reentrant call returns immediately.
     */
    private static final class BlockingBodyPublisher implements SdkPublisher<ByteBuffer> {

        private final InputStream stream;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final AtomicBoolean subscribed = new AtomicBoolean();
        private final CountDownLatch drained = new CountDownLatch(1);
        private volatile boolean cancelled;
        private Subscriber<? super ByteBuffer> subscriber;

        private BlockingBodyPublisher(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> target) {
            if (!subscribed.compareAndSet(false, true)) {
                target.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                    }
                });
                target.onError(new IllegalStateException("this publisher supports one subscriber"));
                return;
            }
            subscriber = target;
            target.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        fail(new IllegalArgumentException("request(" + n + ") violates rule 3.9"));
                        return;
                    }
                    demand.addAndGet(n);
                    drain();
                }

                @Override
                public void cancel() {
                    cancelled = true;
                    // Cancellation still releases the pool thread; the remaining bytes are abandoned,
                    // which is what closing the stream in the caller will do anyway.
                    finish();
                }
            });
        }

        /** Blocks until the body is fully read, the subscriber cancelled, or an error terminated it. */
        void awaitDrained() {
            try {
                drained.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw SdkClientException.create("interrupted while streaming a response body", e);
            }
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            while (true) {
                while (!cancelled && !terminated.get() && demand.get() > 0) {
                    byte[] buffer = new byte[CHUNK_BYTES];
                    int read;
                    try {
                        read = stream.read(buffer);
                    } catch (IOException e) {
                        fail(e);
                        return;
                    }
                    if (read < 0) {
                        complete();
                        return;
                    }
                    demand.decrementAndGet();
                    subscriber.onNext(ByteBuffer.wrap(buffer, 0, read));
                }
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }

        private void complete() {
            if (terminated.compareAndSet(false, true)) {
                subscriber.onComplete();
                finish();
            }
        }

        private void fail(Throwable error) {
            if (terminated.compareAndSet(false, true)) {
                subscriber.onError(error);
                finish();
            }
        }

        private void finish() {
            drained.countDown();
        }
    }
}
