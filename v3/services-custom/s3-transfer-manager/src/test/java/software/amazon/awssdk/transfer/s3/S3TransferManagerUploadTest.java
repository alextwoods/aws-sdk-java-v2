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
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.publisherOf;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.randomBytes;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.transferManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;

/**
 * Upload, multipart, progress, failure-cleanup, cancellation and pause/resume behavior of {@code upload} and
 * {@code uploadFile} (Requirements 16.7, 19.7, 19.8), against the in-memory {@link FakeS3}.
 */
class S3TransferManagerUploadTest {
    private final FakeS3 s3 = new FakeS3();
    private final S3TransferManager tm = transferManager(s3, 3);

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        tm.close();
        s3.shutdown();
    }

    private Path file(byte[] data) {
        Path file = dir.resolve("source-" + data.length + ".bin");
        try {
            Files.write(file, data);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return file;
    }

    @Test
    void uploadFile_belowThreshold_sendsOnePutObject() throws IOException {
        byte[] data = randomBytes(PART - 1, 1);
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();
        CompletedFileUpload done = join(tm.uploadFile(u -> u.source(file(data)).addTransferListener(listener)
                                                           .putObjectRequest(p -> p.bucket("b").key("k")))
                                          .completionFuture());

        assertThat(s3.object("b", "k")).isEqualTo(data);
        assertThat(s3.calls).containsExactly("putObject");
        assertThat(done.response().eTag()).isNotNull();
        listener.assertCompletedWith(data.length);
    }

    @Test
    void uploadFile_aboveThreshold_uploadsOrderedPartsWithBoundedConcurrency() throws IOException {
        byte[] data = randomBytes(PART * 7 + 123, 2);
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();
        FileUpload upload = tm.uploadFile(u -> u.source(file(data)).addTransferListener(listener)
                                               .putObjectRequest(p -> p.bucket("b").key("k")
                                                                       .serverSideEncryption(ServerSideEncryption.AES256)
                                                                       .contentType("text/plain")));
        join(upload.completionFuture());

        assertThat(s3.object("b", "k")).isEqualTo(data);
        assertThat(s3.calls.stream().filter("uploadPart"::equals)).hasSize(8);
        assertThat(s3.calls).startsWith("createMultipartUpload").endsWith("completeMultipartUpload");
        assertThat(s3.maxInFlightParts.get()).isBetween(1, 3);
        CreateMultipartUploadRequest create = (CreateMultipartUploadRequest) s3.requests.get(0);
        assertThat(create.serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
        assertThat(create.contentType()).isEqualTo("text/plain");
        assertThat(upload.progress().snapshot().transferredBytes()).isEqualTo(data.length);
        listener.assertCompletedWith(data.length);
        assertThat(listener.transferred).isNotEmpty().allSatisfy(b -> assertThat(b).isBetween(0L, (long) data.length));
    }

    @Test
    void upload_unknownLengthBody_fittingOnePart_sendsOnePutObject() {
        byte[] data = randomBytes(PART / 2, 3);
        CompletedUpload done = join(tm.upload(u -> u.requestBody(AsyncRequestBody.fromPublisher(publisherOf(data, 1000)))
                                                   .putObjectRequest(p -> p.bucket("b").key("k")))
                                      .completionFuture());

        assertThat(s3.object("b", "k")).isEqualTo(data);
        assertThat(s3.calls).containsExactly("putObject");
        assertThat(done.response()).isNotNull();
    }

    @Test
    void upload_unknownLengthBody_splitsIntoReplayableParts() {
        byte[] data = randomBytes(PART * 5 + 7, 4);
        join(tm.upload(u -> u.requestBody(AsyncRequestBody.fromPublisher(publisherOf(data, 10_000)))
                            .putObjectRequest(p -> p.bucket("b").key("k")))
               .completionFuture());

        assertThat(s3.object("b", "k")).isEqualTo(data);
        assertThat(s3.calls.stream().filter("uploadPart"::equals)).hasSize(6);
        assertThat(s3.maxInFlightParts.get()).isLessThanOrEqualTo(3);
    }

    @Test
    void upload_knownLengthBodyShorterThanDeclared_failsAndAborts() {
        byte[] data = randomBytes(PART * 3, 5);
        AsyncRequestBody lying = new AsyncRequestBody() {
            @Override
            public java.util.Optional<Long> contentLength() {
                return java.util.Optional.of((long) data.length + 10);
            }

            @Override
            public void subscribe(org.reactivestreams.Subscriber<? super java.nio.ByteBuffer> s) {
                publisherOf(data, 4096).subscribe(s);
            }
        };
        Throwable failure = failureOf(tm.upload(u -> u.requestBody(lying).putObjectRequest(p -> p.bucket("b").key("k")))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(SdkClientException.class).hasMessageContaining("declared a content length");
        await(() -> s3.openUploads().isEmpty(), "the multipart upload is aborted");
        assertThat(s3.abortedUploads).hasSize(1);
        assertThat(s3.keys()).isEmpty();
    }

    @Test
    void uploadFile_partFailsWithThrottling_failsWithTheServiceErrorAndAbortsTheUpload() throws IOException {
        byte[] data = randomBytes(PART * 6, 6);
        S3Exception throttled = FakeS3.serviceError(503, "SlowDown");
        s3.failWith(r -> r instanceof UploadPartRequest p && p.partNumber() == 4 ? throttled : null);
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();

        Throwable failure = failureOf(tm.uploadFile(u -> u.source(file(data)).addTransferListener(listener)
                                                         .putObjectRequest(p -> p.bucket("b").key("k")))
                                        .completionFuture());

        assertThat(failure).isSameAs(throttled);
        await(() -> listener.failed != null, "transferFailed");
        assertThat(listener.events).containsExactly("initiated", "failed");
        assertThat(listener.failed.exception()).isSameAs(throttled);
        // Cleanup has finished before the transfer fails: the upload is aborted and no object is created.
        assertThat(s3.abortedUploads).hasSize(1);
        assertThat(s3.openUploads()).isEmpty();
        assertThat(s3.keys()).isEmpty();
        assertThat(s3.calls).doesNotContain("completeMultipartUpload");
    }

    @Test
    void uploadFile_createMultipartUploadFails_failsWithoutAbort() throws IOException {
        byte[] data = randomBytes(PART * 2, 7);
        SdkClientException connectionFailure = SdkClientException.create("Unable to connect");
        s3.failWith(r -> r instanceof CreateMultipartUploadRequest ? connectionFailure : null);

        Throwable failure = failureOf(tm.uploadFile(u -> u.source(file(data))
                                                         .putObjectRequest(p -> p.bucket("b").key("k")))
                                        .completionFuture());

        assertThat(failure).isSameAs(connectionFailure);
        assertThat(s3.calls).containsExactly("createMultipartUpload");
        assertThat(s3.abortedUploads).isEmpty();
    }

    @Test
    void uploadFile_cancelled_cancelsPartsInFlightAndAbortsTheUpload() throws IOException {
        byte[] data = randomBytes(PART * 6, 8);
        CompletableFuture<Void> never = new CompletableFuture<>();
        AtomicInteger heldParts = new AtomicInteger();
        s3.gate(r -> {
            if (r instanceof UploadPartRequest) {
                heldParts.incrementAndGet();
                return never;
            }
            return CompletableFuture.completedFuture(null);
        });
        FileUpload upload = tm.uploadFile(u -> u.source(file(data)).putObjectRequest(p -> p.bucket("b").key("k")));
        await(() -> heldParts.get() == 3, "three parts in flight");

        assertThat(upload.completionFuture().cancel(true)).isTrue();

        await(() -> s3.abortedUploads.size() == 1, "the multipart upload is aborted");
        assertThat(s3.cancelledCalls.get()).isEqualTo(3);
        assertThat(heldParts.get()).isEqualTo(3);
        assertThat(s3.calls).doesNotContain("completeMultipartUpload");
    }

    @Test
    void close_withTransfersInFlight_cancelsAndCleansThemUp() throws IOException {
        byte[] data = randomBytes(PART * 4, 9);
        AtomicInteger heldParts = new AtomicInteger();
        s3.gate(r -> {
            if (r instanceof UploadPartRequest) {
                heldParts.incrementAndGet();
                return new CompletableFuture<>();
            }
            return CompletableFuture.completedFuture(null);
        });
        FileUpload upload = tm.uploadFile(u -> u.source(file(data)).putObjectRequest(p -> p.bucket("b").key("k")));
        await(() -> heldParts.get() == 3, "three parts in flight");

        tm.close();

        assertThat(failureOf(upload.completionFuture())).isInstanceOf(CancellationException.class)
                                                         .hasMessageContaining("closed");
        assertThat(s3.abortedUploads).hasSize(1);
    }

    @Test
    void pauseAndResume_uploadsOnlyTheMissingParts() throws IOException {
        byte[] data = randomBytes(PART * 6 + 11, 10);
        Path source = file(data);
        CompletableFuture<Void> release = new CompletableFuture<>();
        s3.gate(r -> r instanceof UploadPartRequest p && p.partNumber() > 2 ? release
                                                                             : CompletableFuture.completedFuture(null));
        FileUpload upload = tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("b").key("k")));
        await(() -> s3.storedParts() == 2 && s3.calls.stream().filter("uploadPart"::equals).count() == 5,
              "parts 1 and 2 uploaded and parts 3 to 5 held");

        ResumableFileUpload token = upload.pause();
        assertThat(failureOf(upload.completionFuture())).isInstanceOf(CancellationException.class);
        assertThat(token.multipartUploadId()).isPresent();
        assertThat(token.partSizeInBytes()).hasValue(PART);
        assertThat(token.totalParts()).hasValue(7);
        assertThat(s3.abortedUploads).isEmpty();
        assertThat(s3.openUploads()).containsExactly(token.multipartUploadId().get());

        ResumableFileUpload restored = ResumableFileUpload.fromString(token.serializeToString());
        assertThat(restored).isEqualTo(token);

        s3.gate(r -> CompletableFuture.completedFuture(null));
        int partsBefore = (int) s3.calls.stream().filter("uploadPart"::equals).count();
        FileUpload resumed = tm.resumeUploadFile(restored);
        join(resumed.completionFuture());

        assertThat(s3.object("b", "k")).isEqualTo(data);
        long resumedParts = s3.calls.stream().filter("uploadPart"::equals).count() - partsBefore;
        assertThat(resumedParts).isEqualTo(5);
        assertThat(s3.calls).contains("listParts");
        assertThat(resumed.progress().snapshot().transferredBytes()).isEqualTo(data.length);
    }

    @Test
    void resumeUploadFile_fileModifiedSincePause_uploadsAgainAndAbortsTheStaleUpload() throws IOException {
        byte[] data = randomBytes(PART * 3, 11);
        Path source = file(data);
        s3.gate(r -> r instanceof UploadPartRequest p && p.partNumber() > 1 ? new CompletableFuture<>()
                                                                            : CompletableFuture.completedFuture(null));
        FileUpload upload = tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("b").key("k")));
        await(() -> s3.storedParts() == 1, "part 1 uploaded");
        ResumableFileUpload token = upload.pause();
        String staleUpload = token.multipartUploadId().orElseThrow();

        byte[] changed = randomBytes(PART * 2, 12);
        Files.write(source, changed);
        Files.setLastModifiedTime(source, FileTime.from(Instant.now().plusSeconds(5)));
        s3.gate(r -> CompletableFuture.completedFuture(null));
        join(tm.resumeUploadFile(token).completionFuture());

        assertThat(s3.object("b", "k")).isEqualTo(changed);
        await(() -> s3.abortedUploads.contains(staleUpload), "the stale upload is aborted");
    }

    @Test
    void uploadFile_missingSource_failsTheFutureWithoutCallingS3() {
        Throwable failure = failureOf(tm.uploadFile(u -> u.source(dir.resolve("missing"))
                                                         .putObjectRequest(p -> p.bucket("b").key("k")))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(SdkClientException.class).hasMessageContaining("missing");
        assertThat(s3.calls).isEmpty();
    }

    @Test
    void upload_objectLambdaArn_isRejected() {
        Throwable failure = failureOf(tm.upload(u -> u.requestBody(AsyncRequestBody.fromString("x"))
                                                     .putObjectRequest(PutObjectRequest.builder()
                                                         .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:"
                                                                 + "accesspoint/my-ap")
                                                         .key("k").build()))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Object Lambda");
        assertThat(s3.calls).isEmpty();
    }
}
