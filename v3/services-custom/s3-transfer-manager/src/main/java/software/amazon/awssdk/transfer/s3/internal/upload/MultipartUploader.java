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

package software.amazon.awssdk.transfer.s3.internal.upload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.TransferScope;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.internal.util.Log;
import software.amazon.awssdk.transfer.s3.internal.util.SerialActor;

/**
 * Uploads one object from a {@link PartSource}: a single {@code PutObject} when the source's first part is its last
 * and {@code singlePartAllowed}, else a multipart upload with at most {@code maxInFlightParts} {@code UploadPart}
 * requests in flight (the transfer's explicit multipart concurrency, independent of any executor).
 *
 * <p>All state is owned by a {@link SerialActor}; S3 calls are started from its tasks and their completions are posted
 * back to it. The multipart upload is created lazily, when the first part is ready.
 *
 * <p>Cleanup policy, applied when the transfer's {@link TransferScope} ends before completion: every request in flight
 * is cancelled and the part source stops; then a {@link TransferScope.Reason#FAILED failed} or
 * {@link TransferScope.Reason#CANCELLED cancelled} upload aborts its multipart upload (a {@code CreateMultipartUpload}
 * still in flight is aborted as soon as it returns, so no upload is orphaned) and the result fails with the cause once
 * the abort has finished; a {@link TransferScope.Reason#PAUSED paused} upload keeps the multipart upload for
 * {@code resumeUploadFile}. Abort failures are added to the cause as suppressed exceptions.
 */
public final class MultipartUploader {
    private static final Log log = Log.loggerFor(MultipartUploader.class);

    private final S3AsyncClient s3;
    private final TransferScope scope;
    private final PutObjectRequest request;
    private final PartSource source;
    private final int maxInFlightParts;
    private final boolean singlePartAllowed;
    private final Long objectSize;
    private final SerialActor actor = new SerialActor(this::fail);
    private final CompletableFuture<PutObjectResponse> result = new CompletableFuture<>();
    private final Map<Integer, CompletedPart> completedParts = new ConcurrentSkipListMap<>();

    // Owned by the actor.
    private final Deque<UploadPart> awaitingUploadId = new ArrayDeque<>();
    private boolean requestingPart;
    private boolean sourceEnded;
    private boolean creating;
    private int inFlight;
    private boolean completing;
    private boolean cleaningUp;
    private volatile String uploadId;
    private volatile String pausedUploadId;

    /**
     * @param existingUploadId the multipart upload to continue (resume), or {@code null}
     * @param existingParts the parts that upload already holds, skipped when the source offers them
     * @param objectSize the object size if known, sent as {@code MpuObjectSize}
     */
    public MultipartUploader(S3AsyncClient s3, TransferScope scope, PutObjectRequest request, PartSource source,
                             int maxInFlightParts, boolean singlePartAllowed, Long objectSize,
                             String existingUploadId, Map<Integer, CompletedPart> existingParts) {
        this.s3 = s3;
        this.scope = scope;
        this.request = request;
        this.source = source;
        this.maxInFlightParts = maxInFlightParts;
        this.singlePartAllowed = singlePartAllowed && existingUploadId == null;
        this.objectSize = objectSize;
        this.uploadId = existingUploadId;
        this.completedParts.putAll(existingParts);
    }

    /** Starts the upload; the returned future completes once the upload and any cleanup have finished. */
    public CompletableFuture<PutObjectResponse> start() {
        scope.onEnd(() -> actor.post(this::onScopeEnded));
        actor.post(this::pump);
        return result;
    }

    /** The multipart upload ID, once created. */
    public String uploadId() {
        return uploadId;
    }

    /** Parts that have finished uploading. */
    public int completedPartCount() {
        return completedParts.size();
    }

    private void pump() {
        if (scope.isEnded() || result.isDone() || requestingPart || sourceEnded || completing) {
            return;
        }
        if (inFlight + awaitingUploadId.size() >= maxInFlightParts) {
            return;
        }
        requestingPart = true;
        source.next(new PartSource.Receiver() {
            @Override
            public void onPart(UploadPart part) {
                actor.post(() -> handlePart(part));
            }

            @Override
            public void onEnd() {
                actor.post(() -> {
                    requestingPart = false;
                    sourceEnded = true;
                    maybeComplete();
                });
            }

            @Override
            public void onError(Throwable error) {
                actor.post(() -> {
                    requestingPart = false;
                    fail(error);
                });
            }
        });
    }

    private void handlePart(UploadPart part) {
        requestingPart = false;
        if (scope.isEnded() || result.isDone()) {
            part.release();
            maybeCleanUp();
            return;
        }
        if (completedParts.containsKey(part.partNumber())) {
            // Already uploaded before a pause: skip it.
            part.release();
            pump();
            return;
        }
        if (uploadId == null) {
            if (part.partNumber() == 1 && part.last() && singlePartAllowed) {
                putObject(part);
                return;
            }
            awaitingUploadId.add(part);
            if (!creating) {
                createUpload();
            }
        } else {
            uploadPart(part);
        }
        pump();
    }

    private void putObject(UploadPart part) {
        inFlight++;
        sourceEnded = true;
        completing = true;
        CompletableFuture<PutObjectResponse> call = scope.track(s3.putObject(request, part.body()));
        call.whenComplete((response, failure) -> actor.post(() -> {
            inFlight--;
            part.release();
            if (failure != null) {
                fail(failure);
            } else if (!scope.isEnded()) {
                result.complete(response);
            } else {
                maybeCleanUp();
            }
        }));
    }

    private void createUpload() {
        creating = true;
        CompletableFuture<String> created;
        try {
            S3Requests.validateForMultipart(request);
            // Not tracked by the scope: the upload ID must be learned (and the upload aborted) even if the transfer
            // ends while the request is in flight.
            created = s3.createMultipartUpload(S3Requests.createMultipartUpload(request))
                        .thenApply(r -> r.uploadId());
        } catch (RuntimeException e) {
            created = Futures.failed(e);
        }
        created.whenComplete((id, failure) -> actor.post(() -> {
            creating = false;
            if (failure != null) {
                fail(failure);
                return;
            }
            uploadId = id;
            log.debug(() -> "Created multipart upload " + id + " for " + request.bucket() + "/" + request.key());
            if (scope.isEnded()) {
                maybeCleanUp();
                return;
            }
            while (!awaitingUploadId.isEmpty()) {
                uploadPart(awaitingUploadId.poll());
            }
            pump();
        }));
    }

    private void uploadPart(UploadPart part) {
        inFlight++;
        CompletableFuture<?> call;
        try {
            call = scope.track(s3.uploadPart(S3Requests.uploadPart(request, uploadId, part.partNumber(), part.length()),
                                             part.body()))
                        .thenApply(r -> {
                            completedParts.put(part.partNumber(), S3Requests.completedPart(r, part.partNumber()));
                            return r;
                        });
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((r, failure) -> actor.post(() -> {
            inFlight--;
            part.release();
            if (failure != null) {
                fail(failure);
                return;
            }
            pump();
            maybeComplete();
        }));
    }

    private void maybeComplete() {
        if (!sourceEnded || inFlight > 0 || !awaitingUploadId.isEmpty() || creating || completing || scope.isEnded()
            || result.isDone()) {
            maybeCleanUp();
            return;
        }
        if (uploadId == null) {
            fail(new IllegalStateException("The upload source produced no parts"));
            return;
        }
        completing = true;
        List<CompletedPart> parts = new ArrayList<>(completedParts.values());
        CompletableFuture<PutObjectResponse> call;
        try {
            call = scope.track(s3.completeMultipartUpload(S3Requests.complete(request, uploadId, parts, objectSize)))
                        .thenApply(S3Requests::putObjectResponse);
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((response, failure) -> actor.post(() -> {
            if (failure != null) {
                completing = false;
                fail(failure);
            } else {
                result.complete(response);
            }
        }));
    }

    private void fail(Throwable failure) {
        Throwable cause = Futures.unwrap(failure);
        if (!scope.end(TransferScope.Reason.FAILED, cause)) {
            // Already ended: this is a request cancelled by the end of the scope.
            actor.post(this::maybeCleanUp);
        }
    }

    private void onScopeEnded() {
        source.cancel();
        awaitingUploadId.forEach(UploadPart::release);
        awaitingUploadId.clear();
        maybeCleanUp();
    }

    /** Once nothing is in flight, applies the cleanup policy of the scope's end reason and fails the result. */
    private void maybeCleanUp() {
        if (!scope.isEnded() || result.isDone() || cleaningUp || inFlight > 0 || creating) {
            return;
        }
        cleaningUp = true;
        Throwable cause = scope.cause() != null ? scope.cause()
                                                : new CancellationException("The upload was cancelled");
        String id = uploadId;
        // A pause keeps the upload only if its token recorded the ID: an upload created after the pause took its
        // snapshot would be unreachable, so it is aborted like a cancelled one.
        boolean keepForResume = scope.reason() == TransferScope.Reason.PAUSED && id != null
                                && id.equals(pausedUploadId);
        if (id == null || keepForResume) {
            result.completeExceptionally(cause);
            return;
        }
        CompletableFuture<?> abort;
        try {
            abort = s3.abortMultipartUpload(S3Requests.abort(request, id));
        } catch (RuntimeException e) {
            abort = Futures.failed(e);
        }
        abort.whenComplete((r, abortFailure) -> {
            if (abortFailure != null) {
                Throwable unwrapped = Futures.unwrap(abortFailure);
                log.debug(() -> "Failed to abort multipart upload " + id + ": " + unwrapped);
                if (unwrapped != cause) {
                    cause.addSuppressed(unwrapped);
                }
            } else {
                log.debug(() -> "Aborted multipart upload " + id);
            }
            result.completeExceptionally(cause);
        });
    }

    /** Records the upload ID a pause token captured; called by the pausing thread before the scope ends. */
    public void recordPausedUploadId(String id) {
        this.pausedUploadId = id;
    }
}
