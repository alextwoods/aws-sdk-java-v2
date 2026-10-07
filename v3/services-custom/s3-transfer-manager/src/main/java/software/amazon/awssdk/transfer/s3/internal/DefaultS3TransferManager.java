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

package software.amazon.awssdk.transfer.s3.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.body.ProgressReportingRequestBody;
import software.amazon.awssdk.transfer.s3.internal.copy.Copier;
import software.amazon.awssdk.transfer.s3.internal.directory.DirectoryDownloader;
import software.amazon.awssdk.transfer.s3.internal.directory.DirectoryUploader;
import software.amazon.awssdk.transfer.s3.internal.download.FileDownloader;
import software.amazon.awssdk.transfer.s3.internal.download.ProgressReportingResponseTransformer;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultCopy;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDirectoryDownload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDirectoryUpload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDownload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileUpload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultUpload;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.internal.upload.BufferedPartSource;
import software.amazon.awssdk.transfer.s3.internal.upload.FilePartSource;
import software.amazon.awssdk.transfer.s3.internal.upload.MultipartUploader;
import software.amazon.awssdk.transfer.s3.internal.upload.S3Requests;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.internal.util.Pages;
import software.amazon.awssdk.transfer.s3.internal.util.Shapes;
import software.amazon.awssdk.transfer.s3.internal.util.Validate;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * The V3 {@link S3TransferManager}: every transfer runs over the public {@link S3AsyncClient} API, in its own
 * {@link TransferScope} (a child of the manager's scope, which {@link #close()} ends), with its own
 * {@link TransferProgressUpdater}.
 *
 * <p>Each public method returns at once; failures, including invalid requests detected before any S3 call, are
 * reported through the returned completion future. Cancelling that future ends the transfer's scope with
 * {@link TransferScope.Reason#CANCELLED}, which cancels the transfer's requests and applies its cleanup policy.
 */
public final class DefaultS3TransferManager implements S3TransferManager {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private final TransferManagerConfiguration config;
    private final S3AsyncClient s3;
    private final Executor executor;
    private final TransferScope managerScope = new TransferScope();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final DirectoryUploader directoryUploader;
    private final DirectoryDownloader directoryDownloader;

    DefaultS3TransferManager(TransferManagerConfiguration config) {
        this.config = config;
        this.s3 = config.s3Client();
        this.executor = config.executor();
        this.directoryUploader = new DirectoryUploader(executor, (r, scope) -> uploadFile(r, scope).completionFuture());
        this.directoryDownloader =
            new DirectoryDownloader(s3, executor, (r, scope) -> downloadFile(r, scope).completionFuture());
    }

    // ---------------------------------------------------------------------------------------------------- uploads

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        Validate.paramNotNull(uploadRequest, "uploadRequest");
        AsyncRequestBody body = uploadRequest.requestBody();
        Optional<Long> length = body.contentLength();
        TransferProgressUpdater progress = new TransferProgressUpdater(uploadRequest, length.orElse(null));
        progress.transferInitiated();
        TransferScope scope = managerScope.child();
        CompletableFuture<CompletedUpload> returned = new CompletableFuture<>();
        progress.registerCompletion(returned);
        try {
            PutObjectRequest put = uploadRequest.putObjectRequest();
            assertNotUnsupportedArn(put.bucket(), "upload");
            Function<AsyncRequestBody, AsyncRequestBody> reporting =
                b -> new ProgressReportingRequestBody(b, progress::bytesTransferred);
            CompletableFuture<PutObjectResponse> call;
            if (length.isPresent() && length.get() < config.multipartThresholdInBytes()) {
                call = scope.track(s3.putObject(put, reporting.apply(body)));
            } else {
                Long size = length.orElse(null);
                BufferedPartSource parts = new BufferedPartSource(body, config.partSize(size),
                                                                  config.maxInFlightParts(), size, reporting);
                // A body of unknown length that fits in one part is sent with one PutObject.
                call = new MultipartUploader(s3, scope, put, parts, config.maxInFlightParts(), size == null, size,
                                             null, Map.of()).start();
            }
            link(call, returned, scope, r -> CompletedUpload.builder().response(r).build());
        } catch (RuntimeException e) {
            fail(returned, scope, e);
        }
        return new DefaultUpload(returned, progress.progress());
    }

    @Override
    public FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        Validate.paramNotNull(uploadFileRequest, "uploadFileRequest");
        return uploadFile(uploadFileRequest, managerScope.child());
    }

    private DefaultFileUpload uploadFile(UploadFileRequest request, TransferScope scope) {
        CompletableFuture<CompletedFileUpload> returned = new CompletableFuture<>();
        Path source = request.source();
        long length;
        FileTime modified;
        try {
            assertNotUnsupportedArn(request.putObjectRequest().bucket(), "upload");
            length = Files.size(source);
            modified = Files.getLastModifiedTime(source);
        } catch (IOException | RuntimeException e) {
            TransferProgressUpdater progress = new TransferProgressUpdater(request, null);
            progress.transferInitiated();
            progress.registerCompletion(returned);
            fail(returned, scope, e instanceof IOException ? SdkClientException.create("Failed to read " + source, e)
                                                           : e);
            return new DefaultFileUpload(returned, progress.progress(), () -> {
                throw new IllegalStateException("The upload failed to start and cannot be paused");
            });
        }
        TransferProgressUpdater progress = new TransferProgressUpdater(request, length);
        progress.transferInitiated();
        progress.registerCompletion(returned);
        boolean single = length < config.multipartThresholdInBytes();
        long partSize = single ? Math.max(length, 1) : config.partSize(length);
        MultipartUploader uploader = fileUploader(request, scope, progress, length, modified, partSize, single, null,
                                                  Map.of());
        link(uploader.start(), returned, scope, r -> CompletedFileUpload.builder().response(r).build());
        int totalParts = Math.toIntExact((length + partSize - 1) / partSize);
        return new DefaultFileUpload(returned, progress.progress(),
                                     () -> pauseUpload(request, scope, uploader, length, modified, partSize,
                                                       totalParts, returned));
    }

    private MultipartUploader fileUploader(UploadFileRequest request, TransferScope scope,
                                           TransferProgressUpdater progress, long length, FileTime modified,
                                           long partSize, boolean single, String uploadId,
                                           Map<Integer, CompletedPart> existingParts) {
        FilePartSource parts = new FilePartSource(request.source(), length, partSize, executor, modified,
                                                  b -> new ProgressReportingRequestBody(b, progress::bytesTransferred));
        return new MultipartUploader(s3, scope, request.putObjectRequest(), parts, config.maxInFlightParts(), single,
                                     length, uploadId, existingParts);
    }

    private static ResumableFileUpload pauseUpload(UploadFileRequest request, TransferScope scope,
                                                   MultipartUploader uploader, long length, FileTime modified,
                                                   long partSize, int totalParts,
                                                   CompletableFuture<CompletedFileUpload> returned) {
        String id = returned.isDone() && !returned.isCompletedExceptionally() ? null : uploader.uploadId();
        uploader.recordPausedUploadId(id);
        scope.end(TransferScope.Reason.PAUSED, new CancellationException("The upload was paused"));
        ResumableFileUpload.Builder token = ResumableFileUpload.builder()
                                                               .uploadFileRequest(request)
                                                               .fileLength(length)
                                                               .fileLastModified(modified.toInstant());
        if (id != null) {
            token.multipartUploadId(id)
                 .partSizeInBytes(partSize)
                 .totalParts((long) totalParts)
                 .transferredParts((long) uploader.completedPartCount());
        }
        return token.build();
    }

    @Override
    public FileUpload resumeUploadFile(ResumableFileUpload resumableFileUpload) {
        Validate.paramNotNull(resumableFileUpload, "resumableFileUpload");
        UploadFileRequest request = resumableFileUpload.uploadFileRequest();
        Optional<String> uploadId = resumableFileUpload.multipartUploadId();
        long length;
        FileTime modified;
        try {
            length = Files.size(request.source());
            modified = Files.getLastModifiedTime(request.source());
        } catch (IOException e) {
            return uploadFile(request);
        }
        boolean fileModified = length != resumableFileUpload.fileLength()
                               || !modified.toInstant().equals(resumableFileUpload.fileLastModified());
        if (uploadId.isEmpty() || fileModified) {
            if (uploadId.isPresent()) {
                // The parts uploaded before the pause no longer match the file: abort them so they are not kept.
                log.debug(() -> "The file " + request.source() + " changed since the upload was paused; uploading "
                                + "it again and aborting multipart upload " + uploadId.get());
                s3.abortMultipartUpload(S3Requests.abort(request.putObjectRequest(), uploadId.get()))
                  .whenComplete((r, t) -> { });
            }
            return uploadFile(request);
        }
        String id = uploadId.get();
        long partSize = resumableFileUpload.partSizeInBytes().orElse(config.partSize(length));
        int totalParts = Math.toIntExact((length + partSize - 1) / partSize);
        TransferScope scope = managerScope.child();
        TransferProgressUpdater progress = new TransferProgressUpdater(request, length);
        progress.transferInitiated();
        CompletableFuture<CompletedFileUpload> returned = new CompletableFuture<>();
        progress.registerCompletion(returned);
        AtomicReference<MultipartUploader> uploaderRef = new AtomicReference<>();
        CompletableFuture<List<ListPartsResponse>> listing;
        try {
            listing = scope.track(Pages.collect(s3.listPartsPaginator(S3Requests.listParts(request.putObjectRequest(),
                                                                                           id))));
        } catch (RuntimeException e) {
            listing = Futures.failed(e);
        }
        listing.whenComplete((pages, failure) -> {
            if (failure != null) {
                fail(returned, scope, Futures.unwrap(failure));
                return;
            }
            Map<Integer, CompletedPart> existing = new HashMap<>();
            long uploadedBytes = 0;
            for (ListPartsResponse page : pages) {
                for (Part part : page.parts()) {
                    existing.put(part.partNumber(), S3Requests.completedPart(part));
                    uploadedBytes += part.size() == null ? 0 : part.size();
                }
            }
            progress.bytesTransferred(uploadedBytes);
            MultipartUploader uploader = fileUploader(request, scope, progress, length, modified, partSize, false, id,
                                                      existing);
            uploaderRef.set(uploader);
            link(uploader.start(), returned, scope, r -> CompletedFileUpload.builder().response(r).build());
        });
        cancelScopeWhenCancelled(returned, scope);
        return new DefaultFileUpload(returned, progress.progress(), () -> {
            MultipartUploader uploader = uploaderRef.get();
            if (uploader == null) {
                scope.end(TransferScope.Reason.PAUSED, new CancellationException("The upload was paused"));
                return resumableFileUpload;
            }
            return pauseUpload(request, scope, uploader, length, modified, partSize, totalParts, returned);
        });
    }

    // -------------------------------------------------------------------------------------------------- downloads

    @Override
    public <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
        Validate.paramNotNull(downloadRequest, "downloadRequest");
        TransferProgressUpdater progress = new TransferProgressUpdater(downloadRequest, null);
        progress.transferInitiated();
        TransferScope scope = managerScope.child();
        CompletableFuture<CompletedDownload<ResultT>> returned = new CompletableFuture<>();
        progress.registerCompletion(returned);
        try {
            assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");
            CompletableFuture<ResultT> call =
                scope.track(s3.getObject(downloadRequest.getObjectRequest(),
                                         new ProgressReportingResponseTransformer<>(
                                             downloadRequest.responseTransformer(), progress)));
            link(call, returned, scope, r -> CompletedDownload.builder().result(r).build());
        } catch (RuntimeException e) {
            fail(returned, scope, e);
        }
        return new DefaultDownload<>(returned, progress.progress());
    }

    @Override
    public FileDownload downloadFile(DownloadFileRequest downloadRequest) {
        Validate.paramNotNull(downloadRequest, "downloadFileRequest");
        return downloadFile(downloadRequest, managerScope.child());
    }

    private DefaultFileDownload downloadFile(DownloadFileRequest request, TransferScope scope) {
        TransferProgressUpdater progress = new TransferProgressUpdater(request, null);
        progress.transferInitiated();
        CompletableFuture<CompletedFileDownload> returned = new CompletableFuture<>();
        progress.registerCompletion(returned);
        try {
            assertNotUnsupportedArn(request.getObjectRequest().bucket(), "download");
        } catch (RuntimeException e) {
            fail(returned, scope, e);
            return new DefaultFileDownload(returned, progress.progress(), () -> {
                throw new IllegalStateException("The download failed to start and cannot be paused");
            });
        }
        FileDownloader downloader = new FileDownloader(s3, scope, executor, progress, request,
                                                       config.maxInFlightParts(), FileDownloader.Resume.fresh());
        link(downloader.start(), returned, scope, r -> CompletedFileDownload.builder().response(r).build());
        return new DefaultFileDownload(returned, progress.progress(), downloader::pause);
    }

    @Override
    public FileDownload resumeDownloadFile(ResumableFileDownload resumableFileDownload) {
        Validate.paramNotNull(resumableFileDownload, "resumableFileDownload");
        DownloadFileRequest request = resumableFileDownload.downloadFileRequest();
        GetObjectRequest getObject = request.getObjectRequest();
        TransferScope scope = managerScope.child();
        TransferProgressUpdater progress = new TransferProgressUpdater(request, null);
        progress.transferInitiated();
        CompletableFuture<CompletedFileDownload> returned = new CompletableFuture<>();
        progress.registerCompletion(returned);
        cancelScopeWhenCancelled(returned, scope);
        AtomicReference<FileDownloader> downloaderRef = new AtomicReference<>();
        boolean partwise = getObject.range() == null && getObject.partNumber() == null;
        CompletableFuture<HeadObjectResponse> head;
        try {
            head = scope.track(s3.headObject(headRequest(getObject, partwise)));
        } catch (RuntimeException e) {
            head = Futures.failed(e);
        }
        head.thenAcceptAsync(response -> {
            FileDownloader.Resume resume = partwise ? resumePoint(resumableFileDownload, response)
                                                    : FileDownloader.Resume.fresh();
            if (resume.totalSize() != null) {
                progress.totalBytes(resume.totalSize());
            }
            progress.bytesTransferred(resume.keepContent()
                                      ? resume.completedBytes() + resume.singleOffset() : 0);
            FileDownloader downloader = new FileDownloader(s3, scope, executor, progress, request,
                                                           config.maxInFlightParts(), resume);
            downloaderRef.set(downloader);
            link(downloader.start(), returned, scope, r -> CompletedFileDownload.builder().response(r).build());
        }, executor).whenComplete((r, failure) -> {
            if (failure != null) {
                Throwable cause = Futures.unwrap(failure);
                fail(returned, scope, cause instanceof RuntimeException || cause instanceof Error
                                      ? cause : SdkClientException.create("Failed to resume the request", cause));
            }
        });
        return new DefaultFileDownload(returned, progress.progress(), () -> {
            FileDownloader downloader = downloaderRef.get();
            if (downloader == null) {
                scope.end(TransferScope.Reason.PAUSED, new CancellationException("The download was paused"));
                return resumableFileDownload;
            }
            return downloader.pause();
        });
    }

    private static HeadObjectRequest headRequest(GetObjectRequest getObject, boolean partwise) {
        HeadObjectRequest.Builder head = Shapes.copyMembers(getObject, HeadObjectRequest.builder(),
                                                            Set.of("IfMatch", "IfNoneMatch", "IfModifiedSince",
                                                                   "IfUnmodifiedSince", "Range", "PartNumber"));
        if (partwise) {
            head.partNumber(1);
        }
        return head.build();
    }

    /** Where to continue a paused download, or {@link FileDownloader.Resume#fresh()} if the object or file changed. */
    private static FileDownloader.Resume resumePoint(ResumableFileDownload token, HeadObjectResponse head) {
        boolean objectChanged = token.s3ObjectEtag().map(e -> !e.equals(head.eTag())).orElse(false)
                                || token.s3ObjectLastModified().map(m -> !m.equals(head.lastModified())).orElse(false);
        Path destination = token.downloadFileRequest().destination();
        boolean fileChanged;
        try {
            fileChanged = token.fileLastModified() == null || !Files.exists(destination)
                          || !Files.getLastModifiedTime(destination).toInstant().equals(token.fileLastModified());
        } catch (IOException e) {
            fileChanged = true;
        }
        if (objectChanged || fileChanged) {
            log.debug(() -> "The object or the file changed since the download of " + destination + " was paused; "
                            + "downloading it again");
            return FileDownloader.Resume.fresh();
        }
        Long total = token.totalSizeInBytes().isPresent() ? Long.valueOf(token.totalSizeInBytes().getAsLong())
                                                          : null;
        GetObjectResponse base = Shapes.copyMembers(head, GetObjectResponse.builder(), Set.of("ContentRange"))
                                       .contentLength(total).build();
        Integer parts = head.partsCount();
        if (parts != null && parts > 1) {
            Set<Integer> completed = new HashSet<>(token.completedParts());
            return new FileDownloader.Resume(true, completed, token.bytesTransferred(), parts, total, head.eTag(),
                                             head.lastModified(), 0, base);
        }
        return new FileDownloader.Resume(true, Set.of(), 0, null, total, head.eTag(), head.lastModified(),
                                         token.bytesTransferred(), base);
    }

    // ------------------------------------------------------------------------------------------------------ copy

    @Override
    public Copy copy(CopyRequest copyRequest) {
        Validate.paramNotNull(copyRequest, "copyRequest");
        TransferProgressUpdater progress = new TransferProgressUpdater(copyRequest, null);
        progress.transferInitiated();
        TransferScope scope = managerScope.child();
        CompletableFuture<CompletedCopy> returned = new CompletableFuture<>();
        progress.registerCompletion(returned);
        try {
            assertNotUnsupportedArn(copyRequest.copyObjectRequest().copySource(), "copy copySource");
            assertNotUnsupportedArn(copyRequest.copyObjectRequest().bucket(), "copy bucket");
            Copier copier = new Copier(s3, scope, progress, copyRequest.copyObjectRequest(),
                                       config.multipartThresholdInBytes(), config.minimumPartSizeInBytes(),
                                       config.maxInFlightParts());
            link(copier.start(), returned, scope, r -> CompletedCopy.builder().response(r).build());
        } catch (RuntimeException e) {
            fail(returned, scope, e);
        }
        return new DefaultCopy(returned, progress.progress());
    }

    // ------------------------------------------------------------------------------------------------ directories

    @Override
    public DirectoryUpload uploadDirectory(UploadDirectoryRequest request) {
        Validate.paramNotNull(request, "uploadDirectoryRequest");
        TransferScope scope = managerScope.child();
        CompletableFuture<CompletedDirectoryUpload> returned = new CompletableFuture<>();
        runOnExecutor(returned, scope, () -> {
            assertNotUnsupportedArn(request.bucket(), "uploadDirectory");
            boolean follow = request.followSymbolicLinks().orElse(config.uploadDirectoryFollowSymbolicLinks());
            int maxDepth = request.maxDepth().orElse(config.uploadDirectoryMaxDepth());
            DirectoryUploader.validateSource(request.source(), follow);
            return directoryUploader.upload(request, scope, follow, maxDepth, config.transferDirectoryMaxConcurrency());
        });
        return new DefaultDirectoryUpload(returned);
    }

    @Override
    public DirectoryDownload downloadDirectory(DownloadDirectoryRequest request) {
        Validate.paramNotNull(request, "downloadDirectoryRequest");
        TransferScope scope = managerScope.child();
        CompletableFuture<CompletedDirectoryDownload> returned = new CompletableFuture<>();
        runOnExecutor(returned, scope, () -> {
            assertNotUnsupportedArn(request.bucket(), "downloadDirectory");
            return directoryDownloader.download(request, scope, config.transferDirectoryMaxConcurrency());
        });
        return new DefaultDirectoryDownload(returned);
    }

    /** Starts a directory transfer on the I/O executor (validation and the first walk step touch the file system). */
    private <T> void runOnExecutor(CompletableFuture<T> returned, TransferScope scope,
                                   java.util.function.Supplier<CompletableFuture<T>> start) {
        cancelScopeWhenCancelled(returned, scope);
        try {
            executor.execute(() -> {
                try {
                    link(start.get(), returned, scope, Function.identity());
                } catch (RuntimeException e) {
                    fail(returned, scope, e);
                }
            });
        } catch (RejectedExecutionException e) {
            fail(returned, scope, SdkClientException.create("The transfer manager executor rejected the transfer", e));
        }
    }

    // ---------------------------------------------------------------------------------------------------- common

    /**
     * Completes {@code returned} with the outcome of {@code internal} (after the transfer's cleanup), releases the
     * transfer's scope, and ends the scope with {@link TransferScope.Reason#CANCELLED} if the caller cancels
     * {@code returned}.
     */
    private static <I, R> void link(CompletableFuture<I> internal, CompletableFuture<R> returned, TransferScope scope,
                                    Function<? super I, ? extends R> mapper) {
        cancelScopeWhenCancelled(returned, scope);
        internal.whenComplete((result, failure) -> {
            if (failure != null) {
                scope.end(TransferScope.Reason.FAILED, Futures.unwrap(failure));
                returned.completeExceptionally(Futures.unwrap(failure));
                return;
            }
            scope.detach();
            try {
                returned.complete(mapper.apply(result));
            } catch (RuntimeException e) {
                returned.completeExceptionally(e);
            }
        });
    }

    private static void cancelScopeWhenCancelled(CompletableFuture<?> returned, TransferScope scope) {
        returned.whenComplete((r, t) -> {
            if (returned.isCancelled()) {
                scope.end(TransferScope.Reason.CANCELLED, new CancellationException("The transfer was cancelled"));
            }
        });
    }

    private static void fail(CompletableFuture<?> returned, TransferScope scope, Throwable failure) {
        scope.end(TransferScope.Reason.FAILED, failure);
        returned.completeExceptionally(failure);
    }

    /** V2 behavior: S3 Object Lambda access points cannot be used for transfers. */
    static void assertNotUnsupportedArn(String bucket, String operation) {
        if (bucket != null && bucket.startsWith("arn:") && bucket.contains(":s3-object-lambda")) {
            throw new IllegalArgumentException(String.format("%s does not support S3 Object Lambda resources",
                                                             operation));
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        managerScope.end(TransferScope.Reason.CANCELLED,
                         new CancellationException("The transfer manager was closed"));
        if (config.ownedClient() != null) {
            closeQuietly(config.ownedClient());
        }
        if (config.ownedExecutor() != null) {
            config.ownedExecutor().shutdown();
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            log.debug(() -> "Failed to close " + closeable + ": " + e);
        }
    }

    @Override
    public String toString() {
        return "S3TransferManager[" + Objects.toString(s3) + "]";
    }
}
