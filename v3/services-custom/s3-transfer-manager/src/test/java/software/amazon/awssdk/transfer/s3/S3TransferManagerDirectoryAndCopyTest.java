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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.PART;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.await;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.failureOf;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.join;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.randomBytes;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.transferManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;

/**
 * Directory upload/download (paginated listing, key mapping, filters, failure collection, concurrency bound) and copy
 * (single request and multipart, with cleanup) behavior (Requirements 16.7, 19.7, 19.8).
 */
class S3TransferManagerDirectoryAndCopyTest {
    private final FakeS3 s3 = new FakeS3();
    private S3TransferManager tm = transferManager(s3, 3);

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        tm.close();
        s3.shutdown();
    }

    private Path write(String relative, byte[] data) throws IOException {
        Path file = dir.resolve("src").resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, data);
        return file;
    }

    @Test
    void uploadDirectory_mapsRelativePathsToPrefixedKeysAndUploadsLargeFilesInParts() throws IOException {
        byte[] big = randomBytes(PART * 3, 1);
        write("sample.jpg", "a".getBytes(StandardCharsets.UTF_8));
        write("photos/2022/January/sample.jpg", "b".getBytes(StandardCharsets.UTF_8));
        write("photos/2022/February/big.bin", big);

        CompletedDirectoryUpload done = join(tm.uploadDirectory(r -> r.source(dir.resolve("src")).bucket("b")
                                                                      .s3Prefix("backup"))
                                               .completionFuture());

        assertThat(done.failedTransfers()).isEmpty();
        assertThat(s3.keys()).containsExactlyInAnyOrder("b/backup/sample.jpg", "b/backup/photos/2022/January/sample.jpg",
                                                         "b/backup/photos/2022/February/big.bin");
        assertThat(s3.object("b", "backup/photos/2022/February/big.bin")).isEqualTo(big);
        assertThat(s3.calls).contains("uploadPart");
    }

    @Test
    void uploadDirectory_customDelimiterMaxDepthAndTransformer_areApplied() throws IOException {
        write("top.txt", new byte[] {1});
        write("a/nested.txt", new byte[] {2});
        write("a/b/deep.txt", new byte[] {3});

        CompletedDirectoryUpload done = join(tm.uploadDirectory(r -> r.source(dir.resolve("src")).bucket("b")
                                                                      .s3Delimiter("|").maxDepth(2)
                                                                      .uploadFileRequestTransformer(u -> u.putObjectRequest(
                                                                          u.build().putObjectRequest().toBuilder()
                                                                           .contentType("x/y").build())))
                                               .completionFuture());

        assertThat(done.failedTransfers()).isEmpty();
        assertThat(s3.keys()).containsExactlyInAnyOrder("b/top.txt", "b/a|nested.txt");
        assertThat(s3.requests).filteredOn(PutObjectRequest.class::isInstance)
                               .allSatisfy(p -> assertThat(((PutObjectRequest) p).contentType()).isEqualTo("x/y"));
    }

    @Test
    void uploadDirectory_failingFile_isCollectedWhileTheRestSucceed() throws IOException {
        write("ok-1.txt", new byte[] {1});
        write("bad.txt", new byte[] {2});
        write("ok-2.txt", new byte[] {3});
        S3Exception denied = FakeS3.serviceError(403, "AccessDenied");
        s3.failWith(r -> r instanceof PutObjectRequest p && p.key().equals("bad.txt") ? denied : null);

        CompletedDirectoryUpload done = join(tm.uploadDirectory(r -> r.source(dir.resolve("src")).bucket("b"))
                                               .completionFuture());

        assertThat(done.failedTransfers()).singleElement().satisfies(f -> {
            assertThat(f.exception()).isSameAs(denied);
            assertThat(f.request().putObjectRequest().key()).isEqualTo("bad.txt");
        });
        assertThat(s3.keys()).containsExactlyInAnyOrder("b/ok-1.txt", "b/ok-2.txt");
    }

    @Test
    void uploadDirectory_missingSource_failsTheWholeTransfer() {
        Throwable failure = failureOf(tm.uploadDirectory(r -> r.source(dir.resolve("nope")).bucket("b"))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not exist");
    }

    @Test
    void uploadDirectory_concurrency_isBoundedByTransferDirectoryMaxConcurrency() throws IOException {
        tm.close();
        tm = S3TransferManager.builder().s3Client(s3.client()).transferDirectoryMaxConcurrency(2).build();
        for (int i = 0; i < 8; i++) {
            write("f" + i, new byte[] {(byte) i});
        }
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        s3.gate(r -> {
            max.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            CompletableFuture<Void> delay = new CompletableFuture<>();
            CompletableFuture.delayedExecutor(10, java.util.concurrent.TimeUnit.MILLISECONDS).execute(() -> {
                inFlight.decrementAndGet();
                delay.complete(null);
            });
            return delay;
        });

        CompletedDirectoryUpload done = join(tm.uploadDirectory(r -> r.source(dir.resolve("src")).bucket("b"))
                                               .completionFuture());

        assertThat(done.failedTransfers()).isEmpty();
        assertThat(s3.keys()).hasSize(8);
        assertThat(max.get()).isEqualTo(2);
    }

    @Test
    void uploadDirectory_cancelled_cancelsTheFileUploadsInFlight() throws IOException {
        for (int i = 0; i < 5; i++) {
            write("f" + i, new byte[] {(byte) i});
        }
        AtomicInteger held = new AtomicInteger();
        s3.gate(r -> {
            held.incrementAndGet();
            return new CompletableFuture<>();
        });
        DirectoryUpload upload = tm.uploadDirectory(r -> r.source(dir.resolve("src")).bucket("b"));
        await(() -> held.get() == 5, "every file upload in flight");

        upload.completionFuture().cancel(true);

        await(() -> s3.cancelledCalls.get() == 5, "the file uploads are cancelled");
    }

    @Test
    void downloadDirectory_paginatedListing_mapsKeysUnderThePrefixAndAppliesTheFilter() throws IOException {
        s3.listPageSize(2);
        s3.putObject("b", "photos/a.jpg", new byte[] {1});
        s3.putObject("b", "photos/2022/b.jpg", new byte[] {2});
        s3.putObject("b", "photos/2022/c.txt", new byte[] {3});
        s3.putObject("b", "photos/", new byte[0]);
        s3.putMultipartObject("b", "photos/big.bin", randomBytes(PART * 3, 2), PART);
        s3.putObject("b", "other/x.jpg", new byte[] {4});
        Path destination = dir.resolve("dst");

        CompletedDirectoryDownload done = join(tm.downloadDirectory(r -> r.bucket("b").destination(destination)
                                                                          .listObjectsV2RequestTransformer(
                                                                              l -> l.prefix("photos/"))
                                                                          .filter(o -> !o.key().endsWith(".txt")))
                                                 .completionFuture());

        assertThat(done.failedTransfers()).isEmpty();
        assertThat(Files.readAllBytes(destination.resolve("a.jpg"))).containsExactly(1);
        assertThat(Files.readAllBytes(destination.resolve("2022/b.jpg"))).containsExactly(2);
        assertThat(Files.readAllBytes(destination.resolve("big.bin"))).isEqualTo(s3.object("b", "photos/big.bin"));
        assertThat(destination.resolve("2022/c.txt")).doesNotExist();
        assertThat(destination.resolve("other")).doesNotExist();
        assertThat(s3.requests).filteredOn(ListObjectsV2Request.class::isInstance).hasSizeGreaterThan(1);
    }

    @Test
    void downloadDirectory_withDelimiter_listsCommonPrefixesRecursively() throws IOException {
        s3.putObject("b", "a/1", new byte[] {1});
        s3.putObject("b", "a/b/2", new byte[] {2});
        s3.putObject("b", "c/3", new byte[] {3});
        Path destination = dir.resolve("dst");

        CompletedDirectoryDownload done = join(tm.downloadDirectory(r -> r.bucket("b").destination(destination)
                                                                          .listObjectsV2RequestTransformer(
                                                                              l -> l.delimiter("/")))
                                                 .completionFuture());

        assertThat(done.failedTransfers()).isEmpty();
        assertThat(destination.resolve("a/1")).exists();
        assertThat(destination.resolve("a/b/2")).exists();
        assertThat(destination.resolve("c/3")).exists();
    }

    @Test
    void downloadDirectory_keyEscapingTheDestination_isAFailedTransferAndIsNotWritten() throws IOException {
        s3.putObject("b", "../escape.txt", new byte[] {1});
        s3.putObject("b", "safe.txt", new byte[] {2});
        Path destination = dir.resolve("dst");

        CompletedDirectoryDownload done = join(tm.downloadDirectory(r -> r.bucket("b").destination(destination))
                                                 .completionFuture());

        assertThat(done.failedTransfers()).singleElement().satisfies(
            f -> assertThat(f.exception()).hasMessageContaining("resolves outside the parent directory"));
        assertThat(dir.resolve("escape.txt")).doesNotExist();
        assertThat(destination.resolve("safe.txt")).exists();
    }

    @Test
    void downloadDirectory_listingFails_failsTheWholeTransfer() {
        S3Exception denied = FakeS3.serviceError(403, "AccessDenied");
        s3.failWith(r -> r instanceof ListObjectsV2Request ? denied : null);

        Throwable failure = failureOf(tm.downloadDirectory(r -> r.bucket("b").destination(dir.resolve("dst")))
                                        .completionFuture());

        assertThat(failure).isSameAs(denied);
    }

    @Test
    void copy_smallObject_usesOneCopyObject() {
        s3.putObject("src", "k", new byte[] {1, 2, 3});

        CompletedCopy done = join(tm.copy(c -> c.copyObjectRequest(r -> r.copySource("src/k").bucket("dst").key("k2")))
                                    .completionFuture());

        assertThat(done.response().copyObjectResult().eTag()).isNotNull();
        assertThat(s3.object("dst", "k2")).containsExactly(1, 2, 3);
        assertThat(s3.calls).containsExactly("headObject", "copyObject");
    }

    @Test
    void copy_largeObject_copiesRangesInParallelParts() {
        byte[] data = randomBytes(PART * 4 + 9, 3);
        s3.putObject("src", "dir/k", data);

        CompletedCopy done = join(tm.copy(c -> c.copyObjectRequest(r -> r.copySource("/src/dir/k").bucket("dst")
                                                                         .key("k2")))
                                    .completionFuture());

        assertThat(s3.object("dst", "k2")).isEqualTo(data);
        assertThat(s3.requests).filteredOn(UploadPartCopyRequest.class::isInstance)
                               .extracting(r -> ((UploadPartCopyRequest) r).copySourceRange())
                               .containsExactlyInAnyOrder("bytes=0-65535", "bytes=65536-131071", "bytes=131072-196607",
                                                          "bytes=196608-262143", "bytes=262144-262152");
        assertThat(done.response().copyObjectResult().eTag()).isNotNull();
    }

    @Test
    void copy_partFails_abortsTheMultipartCopy() {
        s3.putObject("src", "k", randomBytes(PART * 3, 4));
        SdkClientException failure = SdkClientException.create("Unable to execute HTTP request");
        s3.failWith(r -> r instanceof UploadPartCopyRequest p && p.partNumber() == 2 ? failure : null);

        assertThat(failureOf(tm.copy(c -> c.copyObjectRequest(r -> r.copySource("src/k").bucket("dst").key("k2")))
                               .completionFuture())).isSameAs(failure);
        assertThat(s3.abortedUploads).hasSize(1);
        assertThat(s3.openUploads()).isEmpty();
        assertThat(s3.keys()).containsExactly("src/k");
    }

    @Test
    void copy_accessPointArnSource_isCopiedWithOneRequestWithoutHead() {
        CopyObjectRequest request = CopyObjectRequest.builder()
                                                     .copySource("arn:aws:s3:us-west-2:123456789012:accesspoint/ap/"
                                                                 + "object/k")
                                                     .bucket("dst").key("k2").build();
        failureOf(tm.copy(c -> c.copyObjectRequest(request)).completionFuture());

        assertThat(s3.calls).containsExactly("copyObject");
        assertThat(List.copyOf(s3.requests)).containsExactly(request);
    }
}
