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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

/**
 * Download, multipart, progress, retry, failure, cancellation and pause/resume behavior of {@code download} and
 * {@code downloadFile} (Requirements 16.7, 19.7, 19.8), against the in-memory {@link FakeS3}.
 */
class S3TransferManagerDownloadTest {
    private final FakeS3 s3 = new FakeS3();
    private final S3TransferManager tm = transferManager(s3, 3);

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        tm.close();
        s3.shutdown();
    }

    private List<GetObjectRequest> getObjectRequests() {
        return s3.requests.stream().filter(GetObjectRequest.class::isInstance).map(GetObjectRequest.class::cast)
                          .toList();
    }

    @Test
    void downloadFile_singlePartObject_writesTheWholeObjectWithOneRequest() throws IOException {
        byte[] data = randomBytes(PART * 3 + 5, 1);
        s3.putObject("b", "k", data);
        Path target = dir.resolve("out.bin");
        Files.write(target, randomBytes(PART * 10, 99)); // replaced, not merged
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();

        CompletedFileDownload done = join(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k"))
                                                               .destination(target).addTransferListener(listener))
                                            .completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(data);
        assertThat(getObjectRequests()).singleElement().satisfies(r -> assertThat(r.partNumber()).isEqualTo(1));
        assertThat(done.response().contentLength()).isEqualTo(data.length);
        listener.assertCompletedWith(data.length);
    }

    @Test
    void downloadFile_multipartObject_fetchesEveryPartOnceWithIfMatchAndBoundedConcurrency() throws IOException {
        byte[] data = randomBytes(PART * 9 + 17, 2);
        s3.putMultipartObject("b", "k", data, PART);
        Path target = dir.resolve("out.bin");

        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k")).destination(target));
        CompletedFileDownload done = join(download.completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(data);
        List<GetObjectRequest> requests = getObjectRequests();
        assertThat(requests).extracting(GetObjectRequest::partNumber)
                            .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(requests.subList(1, requests.size())).allSatisfy(r -> assertThat(r.ifMatch()).isNotNull());
        assertThat(done.response().contentLength()).isEqualTo(data.length);
        assertThat(download.progress().snapshot().transferredBytes()).isEqualTo(data.length);
        assertThat(download.progress().snapshot().totalBytes()).hasValue(data.length);
    }

    @Test
    void downloadFile_payloadBreaksMidStream_retriedAttemptRewritesThePartWithoutDoubleCountingProgress()
        throws IOException {
        byte[] data = randomBytes(PART * 4, 3);
        s3.putMultipartObject("b", "k", data, PART);
        s3.failStreamAfter(r -> r.partNumber() != null && r.partNumber() == 3 ? 10_000L : null);
        Path target = dir.resolve("out.bin");
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();

        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k")).destination(target)
                                                      .addTransferListener(listener));
        join(download.completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(data);
        assertThat(s3.getObjectAttempts.get()).isEqualTo(5);
        listener.assertCompletedWith(data.length);
        assertThat(listener.transferred).allSatisfy(b -> assertThat(b).isLessThanOrEqualTo(data.length));
    }

    @Test
    void downloadFile_objectReplacedDuringDownload_failsWithPreconditionFailed() throws IOException {
        byte[] data = randomBytes(PART * 4, 4);
        s3.putMultipartObject("b", "k", data, PART);
        CompletableFuture<Void> release = new CompletableFuture<>();
        s3.gate(r -> r instanceof GetObjectRequest g && g.partNumber() != 1 ? release
                                                                            : CompletableFuture.completedFuture(null));
        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k"))
                                                      .destination(dir.resolve("out.bin")));
        await(() -> getObjectRequests().size() == 4, "parts 2 to 4 requested");
        s3.changeETag("b", "k", "\"replaced\"");
        release.complete(null);

        Throwable failure = failureOf(download.completionFuture());
        assertThat(failure).isInstanceOf(S3Exception.class);
        assertThat(((S3Exception) failure).statusCode()).isEqualTo(412);
    }

    @Test
    void downloadFile_missingKey_failsWithTheModeledErrorAndListenerFailure() {
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();
        Throwable failure = failureOf(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("missing"))
                                                            .destination(dir.resolve("out.bin"))
                                                            .addTransferListener(listener))
                                        .completionFuture());

        assertThat(failure).isInstanceOf(S3Exception.class);
        assertThat(((S3Exception) failure).awsErrorDetails().errorCode()).isEqualTo("NoSuchKey");
        await(() -> listener.failed != null, "transferFailed");
        assertThat(listener.events).containsExactly("initiated", "failed");
    }

    @Test
    void downloadFile_cancelled_cancelsPartRequestsInFlight() {
        s3.putMultipartObject("b", "k", randomBytes(PART * 6, 5), PART);
        AtomicInteger held = new AtomicInteger();
        s3.gate(r -> {
            if (r instanceof GetObjectRequest g && g.partNumber() != 1) {
                held.incrementAndGet();
                return new CompletableFuture<>();
            }
            return CompletableFuture.completedFuture(null);
        });
        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k"))
                                                      .destination(dir.resolve("out.bin")));
        await(() -> held.get() == 3, "three part requests in flight (the concurrency bound)");

        download.completionFuture().cancel(true);

        await(() -> s3.cancelledCalls.get() == 3, "the part requests are cancelled");
        assertThat(held.get()).isEqualTo(3);
    }

    @Test
    void pauseAndResume_multipartObject_fetchesOnlyTheMissingParts() throws IOException {
        byte[] data = randomBytes(PART * 6 + 3, 6);
        s3.putMultipartObject("b", "k", data, PART);
        Path target = dir.resolve("out.bin");
        s3.gate(r -> r instanceof GetObjectRequest g && g.partNumber() != null && g.partNumber() > 3
                     ? new CompletableFuture<>() : CompletableFuture.completedFuture(null));
        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k")).destination(target));
        await(() -> download.progress().snapshot().transferredBytes() == 3L * PART
                    && getObjectRequests().size() == 6, "parts 1 to 3 written, 4 to 6 held");

        ResumableFileDownload token = download.pause();
        assertThat(failureOf(download.completionFuture())).isInstanceOf(CancellationException.class);
        assertThat(token.completedParts()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(token.bytesTransferred()).isEqualTo(3L * PART);
        assertThat(token.totalSizeInBytes()).hasValue(data.length);
        assertThat(token.s3ObjectEtag()).isPresent();
        ResumableFileDownload restored = ResumableFileDownload.fromBytes(token.serializeToBytes());
        assertThat(restored).isEqualTo(token);

        s3.gate(r -> CompletableFuture.completedFuture(null));
        int before = getObjectRequests().size();
        FileDownload resumed = tm.resumeDownloadFile(restored);
        join(resumed.completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(data);
        assertThat(getObjectRequests().subList(before, getObjectRequests().size()))
            .extracting(GetObjectRequest::partNumber).containsExactlyInAnyOrder(4, 5, 6, 7);
        assertThat(resumed.progress().snapshot().transferredBytes()).isEqualTo(data.length);
    }

    @Test
    void pauseAndResume_singlePartObject_continuesWithARangeRequest() throws IOException {
        byte[] data = randomBytes(PART * 4, 7);
        s3.putObject("b", "k", data);
        Path target = dir.resolve("out.bin");
        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k")).destination(target));
        join(download.completionFuture());
        // A paused completed download resumes to a no-op.
        ResumableFileDownload completedToken = download.pause();
        assertThat(completedToken.bytesTransferred()).isEqualTo(data.length);

        // Simulate a pause after 3 chunks: truncate the file and build the token a pause would have produced.
        byte[] partial = java.util.Arrays.copyOf(data, FakeS3.CHUNK * 3);
        Files.write(target, partial);
        ResumableFileDownload token = completedToken.toBuilder()
                                                    .bytesTransferred((long) partial.length)
                                                    .fileLastModified(Files.getLastModifiedTime(target).toInstant())
                                                    .build();
        int before = getObjectRequests().size();
        join(tm.resumeDownloadFile(token).completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(data);
        GetObjectRequest ranged = getObjectRequests().get(before);
        assertThat(ranged.range()).isEqualTo("bytes=" + partial.length + "-");
        assertThat(ranged.ifMatch()).isEqualTo(token.s3ObjectEtag().orElseThrow());
    }

    @Test
    void resumeDownloadFile_objectChangedSincePause_downloadsAgainFromTheStart() throws IOException {
        byte[] original = randomBytes(PART * 3, 8);
        s3.putMultipartObject("b", "k", original, PART);
        Path target = dir.resolve("out.bin");
        s3.gate(r -> r instanceof GetObjectRequest g && g.partNumber() != null && g.partNumber() > 1
                     ? new CompletableFuture<>() : CompletableFuture.completedFuture(null));
        FileDownload download = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("k")).destination(target));
        await(() -> download.progress().snapshot().transferredBytes() == PART, "part 1 written");
        ResumableFileDownload token = download.pause();

        byte[] replacement = randomBytes(PART * 2, 9);
        s3.putMultipartObject("b", "k", replacement, PART);
        s3.gate(r -> CompletableFuture.completedFuture(null));
        join(tm.resumeDownloadFile(token).completionFuture());

        assertThat(Files.readAllBytes(target)).isEqualTo(replacement);
    }

    @Test
    void download_toBytes_streamsThroughTheCallerTransformerAndReportsProgress() {
        byte[] data = "hello transfer manager".getBytes(StandardCharsets.UTF_8);
        s3.putObject("b", "k", data);
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();

        CompletedDownload<ResponseBytes<GetObjectResponse>> done =
            join(tm.download(DownloadRequest.builder()
                                            .getObjectRequest(g -> g.bucket("b").key("k"))
                                            .responseTransformer(AsyncResponseTransformer.<GetObjectResponse>toBytes())
                                            .addTransferListener(listener)
                                            .build())
                   .completionFuture());

        assertThat(done.result().asByteArray()).isEqualTo(data);
        listener.assertCompletedWith(data.length);
        assertThat(listener.complete.progressSnapshot().sdkResponse()).isPresent();
    }

    @Test
    void download_payloadBreaksMidStream_progressCountsOnlyTheRetriedAttempt() {
        byte[] data = randomBytes(FakeS3.CHUNK * 5, 10);
        s3.putObject("b", "k", data);
        s3.failStreamAfter(r -> (long) FakeS3.CHUNK * 2);
        TransferTestSupport.RecordingListener listener = new TransferTestSupport.RecordingListener();

        CompletedDownload<ResponseBytes<GetObjectResponse>> done =
            join(tm.download(DownloadRequest.builder()
                                            .getObjectRequest(g -> g.bucket("b").key("k"))
                                            .responseTransformer(AsyncResponseTransformer.<GetObjectResponse>toBytes())
                                            .addTransferListener(listener)
                                            .build())
                   .completionFuture());

        assertThat(done.result().asByteArray()).isEqualTo(data);
        listener.assertCompletedWith(data.length);
    }
}
