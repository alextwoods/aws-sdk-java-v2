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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.transfer.s3.internal.TransferManagerFactory;
import software.amazon.awssdk.transfer.s3.internal.util.Validate;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
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
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * The S3 Transfer Manager offers a simple API that allows you to transfer a single object or a set of objects to and
 * from Amazon S3 with enhanced throughput and reliability. It leverages Amazon S3 multipart upload and part-wise
 * fetches to perform transfers in parallel. In addition, the S3 Transfer Manager also enables you to monitor a
 * transfer's progress in real-time, as well as pause the transfer for execution at a later time.
 *
 * <h2>V3 behavior</h2>
 * The V3 Transfer Manager runs entirely over the public {@link S3AsyncClient} API: it has no privileged transport or
 * pipeline path, and it works with every {@code S3AsyncClient} (ADAPTED from V2, which delegated multipart work to the
 * CRT-based or multipart-enabled S3 client). The transfer manager owns the multipart behavior instead:
 * <ul>
 *     <li>Objects of at least {@link Builder#multipartThresholdInBytes(Long)} bytes are uploaded (and copied) with a
 *     multipart upload of {@link Builder#minimumPartSizeInBytes(Long)}-byte parts (more if needed to stay within
 *     10,000 parts). File downloads fetch part 1 first ({@code partNumber=1}) and, if the object was uploaded in
 *     parts, its remaining parts in parallel (V2 Java multipart client behavior).</li>
 *     <li>At most {@link Builder#maxInFlightParts(Integer)} part requests run at once per transfer. This bound is
 *     explicit and independent of the size of any executor, and it also bounds the memory a transfer buffers.</li>
 *     <li>Part bodies are replayable: a part read from a file is re-read from the file on every attempt, and a part
 *     split from a caller's {@link AsyncRequestBody} is buffered once, so retries never re-subscribe to the caller's
 *     body.</li>
 *     <li>Every transfer owns a cancellation scope. Cancelling a transfer's completion future, or a failing part,
 *     cancels all of the transfer's requests in flight and then cleans up: an unfinished multipart upload is aborted.
 *     {@code pause()} cancels the requests in flight without that cleanup, so the transfer can be resumed.</li>
 *     <li>Blocking file-system work (reading part bodies, writing downloaded parts, walking directories) runs on the
 *     {@link Builder#executor(Executor)}, never on an HTTP event loop or the common pool.</li>
 * </ul>
 *
 * <h2>Instantiate the S3 Transfer Manager</h2>
 * {@snippet :
 *      S3TransferManager transferManager = S3TransferManager.create();
 *
 *      S3TransferManager custom =
 *          S3TransferManager.builder()
 *                           .s3Client(S3AsyncClient.builder().region(Region.US_WEST_2).build())
 *                           .minimumPartSizeInBytes(16 * SizeConstant.MB)
 *                           .maxInFlightParts(16)
 *                           .build();
 * }
 *
 * <h2>Common Usage Patterns</h2>
 * {@snippet :
 *         FileUpload upload =
 *             transferManager.uploadFile(u -> u.putObjectRequest(req -> req.bucket("bucket").key("key"))
 *                                              .addTransferListener(LoggingTransferListener.create())
 *                                              .source(Paths.get("myFile.txt")));
 *         upload.completionFuture().join();
 *
 *         FileDownload download =
 *             transferManager.downloadFile(d -> d.getObjectRequest(req -> req.bucket("bucket").key("key"))
 *                                                .destination(Paths.get("myFile.txt")));
 *         download.completionFuture().join();
 *
 *         CompletedDirectoryUpload uploaded =
 *             transferManager.uploadDirectory(r -> r.source(Paths.get("source/directory")).bucket("bucket"))
 *                            .completionFuture().join();
 *         uploaded.failedTransfers().forEach(System.out::println);
 * }
 */
public interface S3TransferManager extends AutoCloseable {

    /**
     * Downloads an object identified by the bucket and key from S3 to a local file. For non-file-based downloads, you may use
     * {@link #download(DownloadRequest)} instead.
     * <p>
     * The SDK will create a new file if the provided one doesn't exist. If the file already exists, the SDK will replace
     * it. In the event of an error, the SDK will <b>NOT</b> attempt to delete the file, leaving it as-is (V2 behavior).
     * The object is fetched part by part ({@code partNumber} requests, the first one alone, the others conditional on its
     * ETag) with at most {@link Builder#maxInFlightParts(Integer)} parts in flight, each written at its offset in the
     * file; an object uploaded in one part, or a request with a {@code range} or {@code partNumber}, is fetched with
     * one request.
     * <p>
     * Users can monitor the progress of the transfer by attaching a {@link TransferListener}. The provided
     * {@link LoggingTransferListener} logs a basic progress bar; users can also implement their own listeners.
     *
     * @param downloadRequest the download request, containing a {@link GetObjectRequest} and the destination path
     * @return A {@link FileDownload} that can be used to track the ongoing transfer
     * @see #downloadFile(Consumer)
     * @see #download(DownloadRequest)
     */
    default FileDownload downloadFile(DownloadFileRequest downloadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link DownloadFileRequest} builder, avoiding the need to
     * create one manually via {@link DownloadFileRequest#builder()}.
     *
     * @see #downloadFile(DownloadFileRequest)
     */
    default FileDownload downloadFile(Consumer<DownloadFileRequest.Builder> request) {
        return downloadFile(DownloadFileRequest.builder().applyMutation(request).build());
    }

    /**
     * Resumes a downloadFile operation. This download operation uses the same configuration as the original download. Any
     * content that has already been fetched since the last pause will be skipped and only the remaining data will be
     * downloaded from Amazon S3.
     * <p>
     * If it is determined that the source S3 object or the destination file has be modified since the last pause, the SDK
     * will download the object from the beginning as if it is a new {@link DownloadFileRequest}.
     *
     * @param resumableFileDownload the download to resume.
     * @return A new {@code FileDownload} object to use to check the state of the download.
     * @see #downloadFile(DownloadFileRequest)
     * @see FileDownload#pause()
     */
    default FileDownload resumeDownloadFile(ResumableFileDownload resumableFileDownload) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link ResumableFileDownload} builder, avoiding the need to
     * create one manually via {@link ResumableFileDownload#builder()}.
     *
     * @see #resumeDownloadFile(ResumableFileDownload)
     */
    default FileDownload resumeDownloadFile(Consumer<ResumableFileDownload.Builder> resumableFileDownload) {
        return resumeDownloadFile(ResumableFileDownload.builder().applyMutation(resumableFileDownload).build());
    }

    /**
     * Downloads an object identified by the bucket and key from S3 through the given {@link AsyncResponseTransformer}. For
     * downloading to a file, you may use {@link #downloadFile(DownloadFileRequest)} instead.
     * <p>
     * The object is fetched with one {@code GetObject} request whose payload is streamed to the transformer with the
     * transformer's own demand. Users can monitor the progress of the transfer by attaching a {@link TransferListener}.
     *
     * @param downloadRequest the download request, containing a {@link GetObjectRequest} and {@link AsyncResponseTransformer}
     * @param <ResultT>       The type of data the {@link AsyncResponseTransformer} produces
     * @return A {@link Download} that can be used to track the ongoing transfer
     * @see #downloadFile(DownloadFileRequest)
     */
    default <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Uploads a local file to an object in S3. For non-file-based uploads, you may use {@link #upload(UploadRequest)} instead.
     * <p>
     * Files of at least the multipart threshold are uploaded with a multipart upload whose part bodies are read from the
     * file (and re-read on retry). If the upload fails or is cancelled, the multipart upload is aborted.
     * <p>
     * Users can monitor the progress of the transfer by attaching a {@link TransferListener}. The provided
     * {@link LoggingTransferListener} logs a basic progress bar; users can also implement their own listeners.
     *
     * @param uploadFileRequest the upload request, containing a {@link PutObjectRequest} and the source path
     * @return A {@link FileUpload} that can be used to track the ongoing transfer
     * @see #uploadFile(Consumer)
     * @see #upload(UploadRequest)
     */
    default FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link UploadFileRequest} builder, avoiding the need to create
     * one manually via {@link UploadFileRequest#builder()}.
     *
     * @see #uploadFile(UploadFileRequest)
     */
    default FileUpload uploadFile(Consumer<UploadFileRequest.Builder> request) {
        return uploadFile(UploadFileRequest.builder().applyMutation(request).build());
    }

    /**
     * Resumes uploadFile operation. This upload operation will use the same configuration provided in
     * {@link ResumableFileUpload}. The SDK will skip the data that has already been upload since the last pause
     * and only upload the remaining data from the source file.
     * <p>
     * If it is determined that the source file has be modified since the last pause, the SDK will upload the object from the
     * beginning as if it is a new {@link UploadFileRequest}.
     *
     * @param resumableFileUpload the upload to resume.
     * @return A new {@code FileUpload} object to use to check the state of the download.
     * @see #uploadFile(UploadFileRequest)
     * @see FileUpload#pause()
     */
    default FileUpload resumeUploadFile(ResumableFileUpload resumableFileUpload) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link ResumableFileUpload} builder, avoiding the need to
     * create one manually via {@link ResumableFileUpload#builder()}.
     *
     * @see #resumeUploadFile(ResumableFileUpload)
     */
    default FileUpload resumeUploadFile(Consumer<ResumableFileUpload.Builder> resumableFileUpload) {
        return resumeUploadFile(ResumableFileUpload.builder().applyMutation(resumableFileUpload).build());
    }

    /**
     * Uploads the given {@link AsyncRequestBody} to an object in S3. For file-based uploads, you may use
     * {@link #uploadFile(UploadFileRequest)} instead.
     * <p>
     * A body whose length is unknown or at least the multipart threshold is split into parts while it is read; each part
     * is buffered once so it can be retried, and at most {@link Builder#maxInFlightParts(Integer)} parts are buffered at
     * a time, so the memory an upload needs is bounded by that count times the part size whatever the object size. The
     * body is subscribed to exactly once.
     *
     * @param uploadRequest the upload request, containing a {@link PutObjectRequest} and {@link AsyncRequestBody}
     * @return An {@link Upload} that can be used to track the ongoing transfer
     * @see #upload(Consumer)
     * @see #uploadFile(UploadFileRequest)
     */
    default Upload upload(UploadRequest uploadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link UploadRequest} builder, avoiding the need to create one
     * manually via {@link UploadRequest#builder()}.
     *
     * @see #upload(UploadRequest)
     */
    default Upload upload(Consumer<UploadRequest.Builder> request) {
        return upload(UploadRequest.builder().applyMutation(request).build());
    }

    /**
     * Uploads all files under the given directory to the provided S3 bucket. The key name transformation depends on the
     * optional prefix and delimiter provided in the {@link UploadDirectoryRequest}. By default, all subdirectories will be
     * uploaded recursively, and symbolic links are not followed automatically. This behavior can be configured in at request
     * level via {@link UploadDirectoryRequest.Builder#followSymbolicLinks(Boolean)} or client level via
     * {@link S3TransferManager.Builder#uploadDirectoryFollowSymbolicLinks(Boolean)}. Note that request-level configuration
     * takes precedence over client-level configuration.
     * <p>
     * By default, the prefix is an empty string and the delimiter is {@code "/"}: a file {@code photos/2022/a.jpg} under the
     * source directory is uploaded to the key {@code photos/2022/a.jpg}.
     * <p>
     * The returned {@link CompletableFuture} only completes exceptionally if the request cannot be attempted as a whole (the
     * source directory provided does not exist for example). The future completes successfully for partial successful
     * requests, i.e., there might be failed uploads in the successfully completed response. As a result,
     * you should check for errors in the response via {@link CompletedDirectoryUpload#failedTransfers()}
     * even when the future completes successfully. At most
     * {@link Builder#transferDirectoryMaxConcurrency(Integer)} files are uploaded at once.
     *
     * @param uploadDirectoryRequest the upload directory request
     * @see #uploadDirectory(Consumer)
     */
    default DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link UploadDirectoryRequest} builder, avoiding the need to
     * create one manually via {@link UploadDirectoryRequest#builder()}.
     *
     * @see #uploadDirectory(UploadDirectoryRequest)
     */
    default DirectoryUpload uploadDirectory(Consumer<UploadDirectoryRequest.Builder> requestBuilder) {
        Validate.paramNotNull(requestBuilder, "requestBuilder");
        return uploadDirectory(UploadDirectoryRequest.builder().applyMutation(requestBuilder).build());
    }

    /**
     * Downloads all objects under a bucket to the provided directory. By default, all objects in the entire
     * bucket will be downloaded. You can modify this behavior by providing a
     * {@link DownloadDirectoryRequest#listObjectsRequestTransformer()} and/or
     * a {@link DownloadDirectoryRequest#filter()} in {@link DownloadDirectoryRequest} to
     * limit the S3 objects to download. Objects are listed with the {@code ListObjectsV2} paginator, one page at a time.
     * <p>
     * The downloaded directory structure will match with the provided S3 virtual bucket: the key
     * {@code photos/2022/a.jpg} is downloaded to {@code photos/2022/a.jpg} under the destination. A key that would resolve
     * outside the destination directory is reported as a failed transfer and not downloaded.
     * <p>
     * The returned {@link CompletableFuture} only completes exceptionally if the request cannot be attempted as a whole (the
     * downloadDirectoryRequest is invalid for example). The future completes successfully for partial successful
     * requests, i.e., there might be failed downloads in a successfully completed response. As a result, you should check for
     * errors in the response via {@link CompletedDirectoryDownload#failedTransfers()} even when the future completes
     * successfully.
     * <p>
     * The SDK will create the destination directory if it does not already exist. If a specific file
     * already exists, the existing content will be replaced with the corresponding S3 object content.
     *
     * @param downloadDirectoryRequest the download directory request
     * @see #downloadDirectory(Consumer)
     */
    default DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link DownloadDirectoryRequest} builder, avoiding the need to
     * create one manually via {@link DownloadDirectoryRequest#builder()}.
     *
     * @see #downloadDirectory(DownloadDirectoryRequest)
     */
    default DirectoryDownload downloadDirectory(Consumer<DownloadDirectoryRequest.Builder> requestBuilder) {
        Validate.paramNotNull(requestBuilder, "requestBuilder");
        return downloadDirectory(DownloadDirectoryRequest.builder().applyMutation(requestBuilder).build());
    }

    /**
     * Creates a copy of an object that is already stored in S3.
     * <p>
     * {@link S3TransferManager} uses a plain {@link CopyObjectRequest} for objects smaller than the multipart threshold,
     * and parallel {@link UploadPartCopyRequest}s for larger objects (the source size is read with {@code HeadObject}).
     * <p>
     * For multipart copy requests, source object metadata is not copied by S3; to provide your own metadata, set it using
     * {@link CopyObjectRequest.Builder#metadata(Map)} (with {@link MetadataDirective#REPLACE} for single-request copies).
     * <p>
     * While this API supports {@link TransferListener}s, they will not receive {@code bytesTransferred} callback-updates due to
     * the way the {@link CopyObjectRequest} API behaves. When copying an object, S3 performs the byte copying on your behalf
     * while keeping the connection alive. The progress of the copy is not known until it fully completes and S3 sends a
     * response describing the outcome.
     *
     * @param copyRequest the copy request, containing a {@link CopyObjectRequest}
     * @return A {@link Copy} that can be used to track the ongoing transfer
     * @see #copy(Consumer)
     * @see S3AsyncClient#copyObject(CopyObjectRequest)
     */
    default Copy copy(CopyRequest copyRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link CopyRequest} builder, avoiding the need to create one
     * manually via {@link CopyRequest#builder()}.
     *
     * @see #copy(CopyRequest)
     */
    default Copy copy(Consumer<CopyRequest.Builder> copyRequestBuilder) {
        return copy(CopyRequest.builder().applyMutation(copyRequestBuilder).build());
    }

    /**
     * Closes the transfer manager: transfers in flight are cancelled (and cleaned up), and the S3 client and executor are
     * closed only if this transfer manager created them. Idempotent.
     */
    @Override
    void close();

    /**
     * Creates a default instance of {@link S3TransferManager}, with a default {@link S3AsyncClient} that it owns.
     */
    static S3TransferManager create() {
        return builder().build();
    }

    /**
     * Creates a default builder for {@link S3TransferManager}.
     */
    static S3TransferManager.Builder builder() {
        return new TransferManagerFactory.DefaultBuilder();
    }

    /**
     * The builder definition for a {@link S3TransferManager}.
     */
    interface Builder {

        /**
         * Specifies the low level {@link S3AsyncClient} that will be used to send requests to S3. The SDK will create a default
         * {@link S3AsyncClient} if not provided.
         *
         * <p>
         * <b>Note:</b> the provided {@link S3AsyncClient} will not be closed when the transfer manager is closed; it must be
         * closed by the caller when it is ready to be disposed.
         *
         * @param s3AsyncClient the S3 async client to use
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder s3Client(S3AsyncClient s3AsyncClient);

        /**
         * Specifies the executor that {@link S3TransferManager} will use for its blocking file-system work: reading part
         * bodies, writing downloaded parts and walking directories. If not provided, the transfer manager creates (and
         * closes) a pool of daemon threads.
         *
         * <p>
         * <b>Note:</b> The provided executor will not be shut down when the transfer manager is closed. Its pool size
         * does not limit transfer concurrency, which is set by {@link #maxInFlightParts(Integer)} and
         * {@link #transferDirectoryMaxConcurrency(Integer)}.
         *
         * @param executor the executor to use
         * @return this builder for method chaining.
         */
        Builder executor(Executor executor);

        /**
         * Specifies whether to follow symbolic links when traversing the file tree in
         * {@link S3TransferManager#uploadDirectory} operation. Default {@code false}.
         *
         * @param uploadDirectoryFollowSymbolicLinks whether to follow symbolic links
         * @return This builder for method chaining.
         */
        Builder uploadDirectoryFollowSymbolicLinks(Boolean uploadDirectoryFollowSymbolicLinks);

        /**
         * Specifies the maximum number of levels of directories to visit in {@link S3TransferManager#uploadDirectory}
         * operation. Must be positive. 1 means only the files directly within the provided source directory are visited.
         * Default {@link Integer#MAX_VALUE}.
         *
         * @param uploadDirectoryMaxDepth the maximum number of directory levels to visit
         * @return This builder for method chaining.
         */
        Builder uploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth);

        /**
         * Specifies the maximum number of concurrent file transfers in {@link S3TransferManager#uploadDirectory} and
         * {@link S3TransferManager#downloadDirectory}. Must be positive. Default 100.
         *
         * @param transferDirectoryMaxConcurrency the maximum number of concurrent file transfers
         * @return This builder for method chaining.
         */
        Builder transferDirectoryMaxConcurrency(Integer transferDirectoryMaxConcurrency);

        /**
         * V3 addition: the object size, in bytes, from which uploads and copies use multipart requests (a body of
         * unknown length uses them when it does not fit in one part). Must be positive. Default 8 MiB (the V2
         * multipart client default).
         *
         * @param multipartThresholdInBytes the multipart threshold
         * @return This builder for method chaining.
         */
        Builder multipartThresholdInBytes(Long multipartThresholdInBytes);

        /**
         * V3 addition: the minimum size, in bytes, of each part of a multipart upload or copy. Must be positive; S3
         * rejects parts other than the last that are smaller than 5 MiB. The part size grows when needed to keep an
         * object within 10,000 parts. Default 8 MiB.
         *
         * @param minimumPartSizeInBytes the minimum part size
         * @return This builder for method chaining.
         */
        Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes);

        /**
         * V3 addition: the maximum number of part requests one transfer runs at once. Must be positive. Default 8. This is
         * the transfer's multipart concurrency, independent of executor and connection-pool sizes; for uploads of a
         * caller's {@link AsyncRequestBody} it also bounds the parts buffered in memory.
         *
         * @param maxInFlightParts the maximum number of concurrent part requests per transfer
         * @return This builder for method chaining.
         */
        Builder maxInFlightParts(Integer maxInFlightParts);

        /**
         * Builds an instance of {@link S3TransferManager} based on the settings supplied to this builder
         *
         * @return an instance of {@link S3TransferManager}
         */
        S3TransferManager build();
    }
}
