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

package software.amazon.awssdk.transfer.s3.internal.directory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.internal.TransferScope;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload;
import software.amazon.awssdk.utils.Logger;

/**
 * {@code downloadDirectory}: lists the bucket one page at a time and downloads each object to the path its key maps
 * to under the destination (V2 mapping: the listed prefix is stripped and the delimiter, {@code "/"} by default,
 * becomes the file-system separator), with at most {@code maxConcurrency} downloads in flight. A key that resolves
 * outside the destination is recorded as a failed transfer and never written. Per-object failures are collected in
 * {@link CompletedDirectoryDownload#failedTransfers()}; only an invalid request or a failing listing fails the whole
 * transfer.
 */
public final class DirectoryDownloader {
    private static final Logger log = Logger.loggerFor(DirectoryDownloader.class);
    private static final String DEFAULT_DELIMITER = "/";

    private final S3AsyncClient s3;
    private final Executor executor;
    private final BiFunction<DownloadFileRequest, TransferScope, CompletableFuture<CompletedFileDownload>> downloadFile;

    public DirectoryDownloader(S3AsyncClient s3, Executor executor,
                               BiFunction<DownloadFileRequest, TransferScope, CompletableFuture<CompletedFileDownload>>
                                   downloadFile) {
        this.s3 = s3;
        this.executor = executor;
        this.downloadFile = downloadFile;
    }

    public CompletableFuture<CompletedDirectoryDownload> download(DownloadDirectoryRequest request, TransferScope scope,
                                                                  int maxConcurrency) {
        Path destination = request.destination();
        if (Files.exists(destination) && !Files.isDirectory(destination)) {
            throw new IllegalArgumentException(String.format("The destination directory provided (%s) is not a "
                                                             + "directory", destination));
        }
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                                                               .bucket(request.bucket())
                                                               .prefix("")
                                                               .applyMutation(request.listObjectsRequestTransformer())
                                                               .build();
        Collection<FailedFileDownload> failed = new ConcurrentLinkedQueue<>();
        ObjectListingSource objects = new ObjectListingSource(s3, listRequest, request.filter());
        BoundedDispatcher<S3Object> dispatcher = new BoundedDispatcher<>(objects, object -> {
            DownloadFileRequest fileRequest = null;
            try {
                Path target = destinationPath(request, listRequest, object);
                fileRequest = DownloadFileRequest.builder()
                                                 .destination(target)
                                                 .getObjectRequest(GetObjectRequest.builder()
                                                                                   .bucket(request.bucket())
                                                                                   .key(object.key())
                                                                                   .build())
                                                 .applyMutation(request.downloadFileRequestTransformer())
                                                 .build();
                createParentDirectories(fileRequest.destination());
            } catch (RuntimeException e) {
                DownloadFileRequest failedRequest = fileRequest != null ? fileRequest : unmappedRequest(request, object);
                failed.add(FailedFileDownload.builder().request(failedRequest).exception(e).build());
                return CompletableFuture.completedFuture(null);
            }
            DownloadFileRequest started = fileRequest;
            log.debug(() -> "Downloading key " + object.key() + " to " + started.destination());
            return downloadFile.apply(started, scope.child()).whenComplete((r, t) -> {
                if (t != null) {
                    failed.add(FailedFileDownload.builder().request(started).exception(Futures.unwrap(t)).build());
                }
            });
        }, maxConcurrency, executor, scope);
        return dispatcher.run().handle((r, t) -> {
            if (t != null) {
                Throwable cause = Futures.unwrap(t);
                throw cause instanceof RuntimeException e ? e : SdkClientException.create("Failed to send request",
                                                                                           cause);
            }
            return CompletedDirectoryDownload.builder().failedTransfers(failed).build();
        });
    }

    private static DownloadFileRequest unmappedRequest(DownloadDirectoryRequest request, S3Object object) {
        return DownloadFileRequest.builder()
                                  .destination(request.destination())
                                  .getObjectRequest(GetObjectRequest.builder().bucket(request.bucket())
                                                                    .key(object.key()).build())
                                  .build();
    }

    static Path destinationPath(DownloadDirectoryRequest request, ListObjectsV2Request listRequest, S3Object object) {
        FileSystem fileSystem = request.destination().getFileSystem();
        String delimiter = listRequest.delimiter() == null ? DEFAULT_DELIMITER : listRequest.delimiter();
        String key = normalizeKey(listRequest.prefix(), object.key(), delimiter);
        String relative = fileSystem.getSeparator().equals(delimiter) ? key
                                                                      : key.replace(delimiter, fileSystem.getSeparator());
        Path target = request.destination().resolve(relative);
        if (!target.toAbsolutePath().normalize().startsWith(request.destination().toAbsolutePath().normalize())) {
            throw SdkClientException.create("Cannot download key " + object.key()
                                            + ", its relative path resolves outside the parent directory.");
        }
        return target;
    }

    /** The key with the listed prefix stripped (V2 {@code DirectoryHelperUtils.normalizeKey}). */
    static String normalizeKey(String prefix, String key, String delimiter) {
        if (key == null || key.isEmpty() || prefix == null || prefix.isEmpty()) {
            return key;
        }
        if (!key.startsWith(prefix) || !key.contains(delimiter)) {
            return key;
        }
        String stripped = key.substring(prefix.length());
        if (prefix.endsWith(delimiter) || !stripped.startsWith(delimiter)) {
            return stripped;
        }
        return stripped.substring(1);
    }

    private static void createParentDirectories(Path destination) {
        Path parent = destination.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw SdkClientException.create("Failed to create parent directories for " + destination, e);
        }
    }
}
