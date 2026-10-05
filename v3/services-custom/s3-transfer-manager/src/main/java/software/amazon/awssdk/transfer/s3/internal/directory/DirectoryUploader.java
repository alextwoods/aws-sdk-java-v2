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
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.internal.TransferScope;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.internal.util.Log;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FailedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * {@code uploadDirectory}: walks the source tree on the I/O executor (lazily, one entry per free slot) and uploads
 * each regular file with at most {@code maxConcurrency} uploads in flight, mapping paths to keys exactly as V2 does
 * (prefix, then the relative path with the file-system separator replaced by the delimiter). Per-file failures are
 * collected in {@link CompletedDirectoryUpload#failedTransfers()}; only an invalid source or a failing walk fails the
 * whole transfer.
 */
public final class DirectoryUploader {
    private static final Log log = Log.loggerFor(DirectoryUploader.class);
    private static final String DEFAULT_DELIMITER = "/";

    private final Executor executor;
    private final BiFunction<UploadFileRequest, TransferScope, CompletableFuture<CompletedFileUpload>> uploadFile;

    public DirectoryUploader(Executor executor,
                             BiFunction<UploadFileRequest, TransferScope, CompletableFuture<CompletedFileUpload>> upload) {
        this.executor = executor;
        this.uploadFile = upload;
    }

    public CompletableFuture<CompletedDirectoryUpload> upload(UploadDirectoryRequest request, TransferScope scope,
                                                              boolean followSymbolicLinks, int maxDepth,
                                                              int maxConcurrency) {
        Path source = request.source();
        Collection<FailedFileUpload> failed = new ConcurrentLinkedQueue<>();
        FileTreeSource files = new FileTreeSource(source, followSymbolicLinks, maxDepth);
        BoundedDispatcher<Path> dispatcher = new BoundedDispatcher<>(files, path -> {
            UploadFileRequest fileRequest = uploadFileRequest(request, path);
            log.debug(() -> "Uploading " + path + " to key " + fileRequest.putObjectRequest().key());
            return uploadFile.apply(fileRequest, scope.child()).whenComplete((r, t) -> {
                if (t != null) {
                    failed.add(FailedFileUpload.builder().request(fileRequest).exception(Futures.unwrap(t)).build());
                }
            });
        }, maxConcurrency, executor, scope);
        return dispatcher.run().handle((r, t) -> {
            if (t != null) {
                Throwable cause = Futures.unwrap(t);
                throw cause instanceof RuntimeException e ? e : SdkClientException.create("Failed to upload " + source,
                                                                                           cause);
            }
            return CompletedDirectoryUpload.builder().failedTransfers(failed).build();
        });
    }

    /** Throws {@link IllegalArgumentException} if the source is missing or not a directory (V2 messages). */
    public static void validateSource(Path directory, boolean followSymbolicLinks) {
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException(String.format("The source directory provided (%s) does not exist",
                                                             directory));
        }
        boolean isDirectory = followSymbolicLinks ? Files.isDirectory(directory)
                                                  : Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS);
        if (!isDirectory) {
            throw new IllegalArgumentException(String.format("The source directory provided (%s) is not a directory",
                                                             directory));
        }
    }

    static UploadFileRequest uploadFileRequest(UploadDirectoryRequest request, Path path) {
        String delimiter = request.s3Delimiter().filter(s -> !s.isEmpty()).orElse(DEFAULT_DELIMITER);
        String prefix = request.s3Prefix().map(p -> normalizePrefix(p, delimiter)).orElse("");
        String relative = request.source().relativize(path).toString();
        String separator = request.source().getFileSystem().getSeparator();
        if (!delimiter.equals(separator)) {
            relative = relative.replace(separator, delimiter);
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(request.bucket()).key(prefix + relative)
                                                            .build();
        UploadFileRequest.Builder builder = UploadFileRequest.builder().source(path).putObjectRequest(putObjectRequest);
        request.uploadFileRequestTransformer().accept(builder);
        return builder.build();
    }

    private static String normalizePrefix(String prefix, String delimiter) {
        if (prefix.isEmpty()) {
            return "";
        }
        return prefix.endsWith(delimiter) ? prefix : prefix + delimiter;
    }

    /** The regular files of a tree, walked lazily. */
    private static final class FileTreeSource implements ItemSource<Path> {
        private final Path root;
        private final boolean followSymbolicLinks;
        private final int maxDepth;
        private Stream<Path> stream;
        private Iterator<Path> iterator;
        private boolean exhausted;

        FileTreeSource(Path root, boolean followSymbolicLinks, int maxDepth) {
            this.root = root;
            this.followSymbolicLinks = followSymbolicLinks;
            this.maxDepth = maxDepth;
        }

        @Override
        public Path poll() throws IOException {
            if (exhausted) {
                return null;
            }
            if (iterator == null) {
                stream = followSymbolicLinks ? Files.walk(root, maxDepth, FileVisitOption.FOLLOW_LINKS)
                                             : Files.walk(root, maxDepth);
                iterator = stream.filter(p -> followSymbolicLinks ? Files.isRegularFile(p)
                                                                  : Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                                 .iterator();
            }
            try {
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            } catch (UncheckedIOException e) {
                throw SdkClientException.create("Failed to list files within the provided directory: " + root,
                                                e.getCause());
            }
            exhausted = true;
            close();
            return null;
        }

        @Override
        public boolean exhausted() {
            return exhausted;
        }

        @Override
        public void close() {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
