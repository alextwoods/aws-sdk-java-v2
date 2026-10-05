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

package software.amazon.awssdk.transfer.s3.internal.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.TransferScope;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.internal.util.SerialActor;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

/**
 * Downloads one object into a file, part by part: the first request fetches part 1 ({@code partNumber=1}); if its
 * response reports more parts, parts 2..N are fetched with at most {@code maxInFlightParts} requests in flight, each
 * conditional on the first part's ETag ({@code If-Match}) so a concurrently replaced object fails the download instead
 * of mixing versions. Each part is written at the offset of its {@code Content-Range}. A request with a caller
 * {@code range} or {@code partNumber} is fetched with that single request (V2 behavior).
 *
 * <p>The file is opened (created, and truncated unless resuming) on the I/O executor before the first request. On
 * failure or cancellation the requests in flight are cancelled, the file is closed and left as is (V2 behavior: no
 * delete, so the download can be resumed); on pause, {@link #pause()} additionally waits for any write in progress so
 * the returned token describes the file exactly.
 */
public final class FileDownloader {
    private final S3AsyncClient s3;
    private final TransferScope scope;
    private final Executor executor;
    private final TransferProgressUpdater progress;
    private final DownloadFileRequest request;
    private final int maxInFlightParts;
    private final Resume resume;
    private final DestinationFile file;
    private final SerialActor actor = new SerialActor(this::fail);
    private final CompletableFuture<GetObjectResponse> result = new CompletableFuture<>();
    private final Set<Integer> completedParts = new ConcurrentSkipListSet<>();
    private final AtomicLong completedPartBytes = new AtomicLong();

    // Owned by the actor (volatile where pause() reads them).
    private volatile String eTag;
    private volatile Instant lastModified;
    private volatile Long totalSize;
    private volatile boolean multipart;
    private volatile FilePartTransformer singleTransformer;
    private GetObjectResponse firstResponse;
    private boolean firstResponseSeen;
    private int totalParts;
    private int nextPart;
    private int inFlight;
    private boolean finishing;

    /** Where a resumed download continues; {@link #fresh()} for a new download. */
    public record Resume(boolean keepContent, Set<Integer> completedParts, long completedBytes, Integer partsCount,
                         Long totalSize, String eTag, Instant lastModified, long singleOffset,
                         GetObjectResponse baseResponse) {
        public static Resume fresh() {
            return new Resume(false, Set.of(), 0, null, null, null, null, 0, null);
        }
    }

    public FileDownloader(S3AsyncClient s3, TransferScope scope, Executor executor, TransferProgressUpdater progress,
                          DownloadFileRequest request, int maxInFlightParts, Resume resume) {
        this.s3 = s3;
        this.scope = scope;
        this.executor = executor;
        this.progress = progress;
        this.request = request;
        this.maxInFlightParts = maxInFlightParts;
        this.resume = resume;
        this.file = new DestinationFile(request.destination());
        this.completedParts.addAll(resume.completedParts());
        this.completedPartBytes.set(resume.completedBytes());
        this.eTag = resume.eTag();
        this.lastModified = resume.lastModified();
        this.totalSize = resume.totalSize();
    }

    /** Starts the download; the future completes after the file is closed. */
    public CompletableFuture<GetObjectResponse> start() {
        scope.onEnd(() -> actor.post(this::maybeFinishEnded));
        try {
            executor.execute(() -> {
                try {
                    file.open(resume.keepContent());
                    actor.post(this::startRequests);
                } catch (IOException | RuntimeException e) {
                    fail(SdkClientException.create("Failed to open " + request.destination(), e));
                }
            });
        } catch (RejectedExecutionException e) {
            fail(SdkClientException.create("The transfer manager executor rejected the download", e));
        }
        return result;
    }

    private void startRequests() {
        if (scope.isEnded()) {
            maybeFinishEnded();
            return;
        }
        GetObjectRequest base = request.getObjectRequest();
        if (resume.partsCount() != null && resume.partsCount() > 1) {
            // A resumed multipart object: fetch the parts the token does not list as complete.
            multipart = true;
            firstResponseSeen = true;
            firstResponse = resume.baseResponse();
            totalParts = resume.partsCount();
            nextPart = 1;
            pump();
            maybeComplete();
            return;
        }
        if (resume.keepContent()) {
            // A resumed single-part object: fetch the remaining range.
            long offset = resume.singleOffset();
            if (totalSize != null && offset >= totalSize) {
                firstResponseSeen = true;
                firstResponse = resume.baseResponse();
                maybeComplete();
                return;
            }
            GetObjectRequest.Builder ranged = base.toBuilder().range("bytes=" + offset + "-");
            if (base.ifMatch() == null && eTag != null) {
                ranged.ifMatch(eTag);
            }
            startSingle(ranged.build(), offset, false);
            return;
        }
        if (base.range() != null || base.partNumber() != null) {
            startSingle(base, 0, false);
            return;
        }
        startSingle(base.toBuilder().partNumber(1).build(), 0, true);
    }

    /** The first (or only) request: its response tells whether the object has more parts. */
    private void startSingle(GetObjectRequest getObject, long offset, boolean partwise) {
        FilePartTransformer transformer =
            new FilePartTransformer(file, executor, progress::bytesTransferred, offset,
                                    r -> actor.post(() -> onFirstResponse(r, partwise)));
        singleTransformer = transformer;
        inFlight++;
        CompletableFuture<GetObjectResponse> call;
        try {
            call = scope.track(s3.getObject(getObject, transformer));
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((response, failure) -> actor.post(() -> {
            inFlight--;
            if (failure != null) {
                fail(failure);
                return;
            }
            if (multipart) {
                completedParts.add(1);
                completedPartBytes.addAndGet(lengthOf(response));
            }
            pump();
            maybeComplete();
        }));
    }

    private void onFirstResponse(GetObjectResponse response, boolean partRequest) {
        if (firstResponseSeen) {
            return;
        }
        firstResponseSeen = true;
        firstResponse = response;
        eTag = response.eTag();
        lastModified = response.lastModified();
        Long total = FilePartTransformer.totalOf(response.contentRange());
        if (total == null && response.contentLength() != null) {
            total = resume.singleOffset() + response.contentLength();
        }
        if (total != null) {
            totalSize = total;
            progress.totalBytes(total);
        }
        Integer parts = response.partsCount();
        if (partRequest && parts != null && parts > 1) {
            multipart = true;
            totalParts = parts;
            nextPart = 2;
            pump();
        }
    }

    private void pump() {
        while (multipart && !scope.isEnded() && inFlight < maxInFlightParts && nextPart <= totalParts) {
            int part = nextPart++;
            if (completedParts.contains(part)) {
                continue;
            }
            startPart(part);
        }
    }

    private void startPart(int part) {
        GetObjectRequest.Builder builder = request.getObjectRequest().toBuilder().partNumber(part);
        if (request.getObjectRequest().ifMatch() == null && eTag != null) {
            builder.ifMatch(eTag);
        }
        FilePartTransformer transformer = new FilePartTransformer(file, executor, progress::bytesTransferred, -1,
                                                                  r -> { });
        inFlight++;
        CompletableFuture<GetObjectResponse> call;
        try {
            call = scope.track(s3.getObject(builder.build(), transformer));
        } catch (RuntimeException e) {
            call = Futures.failed(e);
        }
        call.whenComplete((response, failure) -> actor.post(() -> {
            inFlight--;
            if (failure != null) {
                fail(failure);
                return;
            }
            completedParts.add(part);
            completedPartBytes.addAndGet(lengthOf(response));
            pump();
            maybeComplete();
        }));
    }

    private static long lengthOf(GetObjectResponse response) {
        return response.contentLength() == null ? 0 : response.contentLength();
    }

    private void maybeComplete() {
        if (finishing || scope.isEnded() || inFlight > 0 || !firstResponseSeen) {
            return;
        }
        if (multipart && completedParts.size() < totalParts) {
            return;
        }
        finishing = true;
        GetObjectResponse response = completedResponse();
        closeOnExecutor(() -> result.complete(response));
    }

    /** The response of the whole object: the first part's metadata with the object's total length. */
    private GetObjectResponse completedResponse() {
        GetObjectResponse response = firstResponse;
        if (response == null) {
            return GetObjectResponse.builder().contentLength(totalSize).eTag(eTag).lastModified(lastModified).build();
        }
        if (multipart || resume.keepContent()) {
            return response.toBuilder().contentLength(totalSize).contentRange(null).build();
        }
        return response;
    }

    private void fail(Throwable failure) {
        if (!scope.end(TransferScope.Reason.FAILED, Futures.unwrap(failure))) {
            actor.post(this::maybeFinishEnded);
        }
    }

    private void maybeFinishEnded() {
        if (!scope.isEnded() || finishing || inFlight > 0) {
            return;
        }
        finishing = true;
        Throwable cause = scope.cause() != null ? scope.cause() : new CancellationException("The download was cancelled");
        closeOnExecutor(() -> result.completeExceptionally(cause));
    }

    private void closeOnExecutor(Runnable then) {
        try {
            executor.execute(() -> {
                try {
                    file.close();
                } catch (IOException e) {
                    if (!scope.isEnded()) {
                        result.completeExceptionally(SdkClientException.create("Failed to close " + file.path(), e));
                        return;
                    }
                }
                then.run();
            });
        } catch (RejectedExecutionException e) {
            file.closeQuietly();
            then.run();
        }
    }

    /**
     * Pauses the download: ends the scope (cancelling the requests in flight), waits for any write in progress, closes
     * the file and describes what it holds. Runs on the caller's thread.
     */
    public ResumableFileDownload pause() {
        scope.end(TransferScope.Reason.PAUSED, new CancellationException("The download was paused"));
        file.closeQuietly();
        Instant fileModified = lastModifiedOf(request.destination());
        ResumableFileDownload.Builder token = ResumableFileDownload.builder()
                                                                   .downloadFileRequest(request)
                                                                   .fileLastModified(fileModified)
                                                                   .totalSizeInBytes(totalSize)
                                                                   .s3ObjectEtag(eTag)
                                                                   .s3ObjectLastModified(lastModified);
        if (multipart) {
            List<Integer> parts = new ArrayList<>(completedParts);
            token.completedParts(parts).bytesTransferred(completedPartBytes.get());
        } else {
            FilePartTransformer single = singleTransformer;
            long written = single == null ? 0 : single.writtenBytes();
            long offset = resume.keepContent() ? resume.singleOffset() : 0;
            if (result.isDone() && !result.isCompletedExceptionally() && totalSize != null) {
                token.bytesTransferred(totalSize);
            } else {
                token.bytesTransferred(offset + written);
            }
        }
        return token.build();
    }

    private static Instant lastModifiedOf(Path path) {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toInstant() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
