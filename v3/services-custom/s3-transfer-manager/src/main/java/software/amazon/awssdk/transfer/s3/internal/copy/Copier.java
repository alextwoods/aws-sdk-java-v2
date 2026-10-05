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

package software.amazon.awssdk.transfer.s3.internal.copy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.TransferScope;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.internal.upload.S3Requests;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.internal.util.SerialActor;

/**
 * {@code copy}: reads the source size with {@code HeadObject}; a source smaller than the multipart threshold (or a
 * {@code CopySource} that is not a {@code bucket/key} path, such as an access point ARN) is copied with one
 * {@code CopyObject}, a larger one with {@code UploadPartCopy} ranges of {@code partSize} bytes, at most
 * {@code maxInFlightParts} at a time. A failed or cancelled multipart copy is aborted (same cleanup policy as uploads).
 */
public final class Copier {
    private final S3AsyncClient s3;
    private final TransferScope scope;
    private final TransferProgressUpdater progress;
    private final CopyObjectRequest request;
    private final long threshold;
    private final long minimumPartSize;
    private final int maxInFlightParts;
    private final SerialActor actor = new SerialActor(this::fail);
    private final CompletableFuture<CopyObjectResponse> result = new CompletableFuture<>();
    private final Map<Integer, CompletedPart> parts = new ConcurrentSkipListMap<>();

    // Owned by the actor.
    private String uploadId;
    private long size;
    private long partSize;
    private int totalParts;
    private int nextPart = 1;
    private int inFlight;
    private boolean waitingForCreate;
    private boolean completing;
    private boolean cleaningUp;

    public Copier(S3AsyncClient s3, TransferScope scope, TransferProgressUpdater progress, CopyObjectRequest request,
                  long threshold, long minimumPartSize, int maxInFlightParts) {
        this.s3 = s3;
        this.scope = scope;
        this.progress = progress;
        this.request = request;
        this.threshold = threshold;
        this.minimumPartSize = minimumPartSize;
        this.maxInFlightParts = maxInFlightParts;
    }

    public CompletableFuture<CopyObjectResponse> start() {
        scope.onEnd(() -> actor.post(this::maybeCleanUp));
        HeadObjectRequest head = headRequest(request);
        if (head == null) {
            actor.post(this::copyInOneRequest);
            return result;
        }
        inFlight++;
        CompletableFuture<HeadObjectResponse> call;
        try {
            call = scope.track(s3.headObject(head));
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((r, t) -> actor.post(() -> {
            inFlight--;
            if (t != null) {
                fail(t);
                return;
            }
            size = r.contentLength() == null ? 0 : r.contentLength();
            progress.totalBytes(size);
            if (size < threshold) {
                copyInOneRequest();
            } else {
                startMultipartCopy();
            }
        }));
        return result;
    }

    private void copyInOneRequest() {
        if (scope.isEnded()) {
            maybeCleanUp();
            return;
        }
        inFlight++;
        CompletableFuture<CopyObjectResponse> call;
        try {
            call = scope.track(s3.copyObject(request));
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((r, t) -> actor.post(() -> {
            inFlight--;
            if (t != null) {
                fail(t);
                return;
            }
            progress.bytesTransferred(size);
            result.complete(r);
        }));
    }

    private void startMultipartCopy() {
        if (scope.isEnded()) {
            maybeCleanUp();
            return;
        }
        partSize = Math.max(minimumPartSize, (size + 9_999) / 10_000);
        totalParts = Math.toIntExact((size + partSize - 1) / partSize);
        waitingForCreate = true;
        CompletableFuture<String> created;
        try {
            S3Requests.validateForMultipart(request);
            // Not tracked: the upload ID must be learned so a cancelled copy can be aborted.
            created = s3.createMultipartUpload(S3Requests.createMultipartUpload(request)).thenApply(r -> r.uploadId());
        } catch (RuntimeException e) {
            created = Futures.failed(e);
        }
        created.whenComplete((id, t) -> actor.post(() -> {
            waitingForCreate = false;
            if (t != null) {
                fail(t);
                return;
            }
            uploadId = id;
            pump();
        }));
    }

    private void pump() {
        if (scope.isEnded()) {
            maybeCleanUp();
            return;
        }
        while (inFlight < maxInFlightParts && nextPart <= totalParts) {
            int part = nextPart++;
            long first = (part - 1) * partSize;
            long last = Math.min(size, first + partSize) - 1;
            inFlight++;
            CompletableFuture<?> call;
            try {
                call = scope.track(s3.uploadPartCopy(S3Requests.uploadPartCopy(request, uploadId, part, first, last)))
                            .thenApply(r -> parts.put(part, S3Requests.completedPart(r.copyPartResult(), part)));
            } catch (RuntimeException e) {
                call = Futures.failed(e);
            }
            call.whenComplete((r, t) -> actor.post(() -> {
                inFlight--;
                if (t != null) {
                    fail(t);
                    return;
                }
                progress.bytesTransferred(last - first + 1);
                pump();
                maybeComplete();
            }));
        }
    }

    private void maybeComplete() {
        if (completing || inFlight > 0 || parts.size() < totalParts || scope.isEnded()) {
            return;
        }
        completing = true;
        List<CompletedPart> completed = new ArrayList<>(parts.values());
        CompletableFuture<CopyObjectResponse> call;
        try {
            call = scope.track(s3.completeMultipartUpload(S3Requests.complete(request, uploadId, completed, null)))
                        .thenApply(S3Requests::copyObjectResponse);
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((r, t) -> actor.post(() -> {
            if (t != null) {
                completing = false;
                fail(t);
            } else {
                result.complete(r);
            }
        }));
    }

    private void fail(Throwable t) {
        if (!scope.end(TransferScope.Reason.FAILED, Futures.unwrap(t))) {
            actor.post(this::maybeCleanUp);
        }
    }

    private void maybeCleanUp() {
        if (!scope.isEnded() || result.isDone() || cleaningUp || inFlight > 0 || waitingForCreate) {
            return;
        }
        cleaningUp = true;
        Throwable cause = scope.cause() != null ? scope.cause() : new CancellationException("The copy was cancelled");
        if (uploadId == null) {
            result.completeExceptionally(cause);
            return;
        }
        CompletableFuture<?> abort;
        try {
            abort = s3.abortMultipartUpload(S3Requests.abort(request, uploadId));
        } catch (RuntimeException e) {
            abort = Futures.failed(e);
        }
        abort.whenComplete((r, t) -> {
            if (t != null && Futures.unwrap(t) != cause) {
                cause.addSuppressed(Futures.unwrap(t));
            }
            result.completeExceptionally(cause);
        });
    }

    /**
     * The {@code HeadObject} of the copy source, or {@code null} when {@code CopySource} is not a
     * {@code [/]bucket/key[?versionId=id]} path (an ARN, for example), in which case the copy is one request.
     */
    static HeadObjectRequest headRequest(CopyObjectRequest request) {
        String source = request.copySource();
        if (source == null || source.startsWith("arn:")) {
            return null;
        }
        String path = source.startsWith("/") ? source.substring(1) : source;
        String versionId = null;
        int query = path.indexOf("?versionId=");
        if (query >= 0) {
            versionId = URLDecoder.decode(path.substring(query + "?versionId=".length()), StandardCharsets.UTF_8);
            path = path.substring(0, query);
        }
        int slash = path.indexOf('/');
        if (slash <= 0 || slash == path.length() - 1) {
            return null;
        }
        String bucket = path.substring(0, slash);
        String key = URLDecoder.decode(path.substring(slash + 1).replace("+", "%2B"), StandardCharsets.UTF_8);
        return HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .versionId(versionId)
                                .ifMatch(request.copySourceIfMatch())
                                .ifModifiedSince(request.copySourceIfModifiedSince())
                                .ifNoneMatch(request.copySourceIfNoneMatch())
                                .ifUnmodifiedSince(request.copySourceIfUnmodifiedSince())
                                .expectedBucketOwner(request.expectedSourceBucketOwner())
                                .sseCustomerAlgorithm(request.copySourceSSECustomerAlgorithm())
                                .sseCustomerKey(request.copySourceSSECustomerKey())
                                .sseCustomerKeyMD5(request.copySourceSSECustomerKeyMD5())
                                .requestPayer(request.requestPayerAsString())
                                .build();
    }
}
