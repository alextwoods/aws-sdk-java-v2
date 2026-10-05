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

package software.amazon.awssdk.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.checksums.ResolvedRequestChecksum;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.StreamingBody;

/**
 * The digests one attempt must compute over its replayable streaming payload before signing (Requirements 13.6, 5.11,
 * 8.4, 12.5): the header request checksum the request checksum stage left pending, and the SHA-256 payload hash a
 * {@link software.amazon.awssdk.auth.api.PayloadSigningMode#PAYLOAD_HASH} signer signs. Both are computed in one pass
 * over the body, which transmission (and any retry) then reads again from its first byte.
 *
 * <p>Memory is bounded and independent of the payload size: the sync driver reads the body's blocking view through
 * one fixed buffer, and the async driver subscribes to its publisher with a demand of exactly one chunk at a time,
 * folding each chunk into the digests before it requests the next; nothing is retained. The async pass never blocks:
 * it runs on the threads that deliver the body's chunks, and its result wakes the driver. One instance per attempt.
 */
final class PayloadDigests {
    private static final int READ_BUFFER = 64 * 1024;

    private final StreamingBody body;
    private final ResolvedRequestChecksum checksum;
    private final boolean sha256;

    /**
     * @param body the replayable body to read
     * @param checksum the pending header checksum, or {@code null}
     * @param sha256 whether the signer needs the payload's SHA-256
     */
    PayloadDigests(StreamingBody body, ResolvedRequestChecksum checksum, boolean sha256) {
        this.body = Objects.requireNonNull(body, "body");
        if (checksum == null && !sha256) {
            throw new IllegalArgumentException("No payload digest is needed");
        }
        this.checksum = checksum;
        this.sha256 = sha256;
    }

    /** The pending header checksum, or {@code null}. */
    ResolvedRequestChecksum checksum() {
        return checksum;
    }

    /** Reads the body once through its blocking view (sync driver only). */
    Result read() {
        Accumulator digests = new Accumulator();
        byte[] buffer = new byte[READ_BUFFER];
        try (InputStream in = body.openStream()) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                digests.update(buffer, n);
            }
        } catch (IOException | UncheckedIOException e) {
            throw SdkClientException.create("Failed to read the request payload to compute its digests", e);
        }
        return digests.result();
    }

    /**
     * Starts reading the body once through its publisher, one chunk at a time, without blocking (async driver). The
     * returned handle's future completes with the digests, or with the body's failure; cancelling the handle cancels
     * the subscription.
     */
    Pass subscribe() {
        Pass pass = new Pass(new Accumulator());
        body.subscribe(pass);
        return pass;
    }

    /** The computed digests: the checksum's bytes and the lower-case hex SHA-256, each {@code null} if not needed. */
    record Result(byte[] checksum, String sha256) {
    }

    /** The digests of the bytes seen so far. Confined to one reader at a time. */
    private final class Accumulator {
        private final SdkChecksum checksumState = checksum == null ? null : SdkChecksum.create(checksum.algorithm());
        private final SdkChecksum sha256State = sha256 ? SdkChecksum.create(ChecksumAlgorithm.SHA256) : null;

        void update(byte[] bytes, int length) {
            if (checksumState != null) {
                checksumState.update(bytes, 0, length);
            }
            if (sha256State != null) {
                sha256State.update(bytes, 0, length);
            }
        }

        void update(ByteBuffer chunk) {
            if (checksumState != null) {
                checksumState.update(chunk.duplicate());
            }
            if (sha256State != null) {
                sha256State.update(chunk.duplicate());
            }
        }

        Result result() {
            return new Result(checksumState == null ? null : checksumState.digest(),
                              sha256State == null ? null : HexFormat.of().formatHex(sha256State.digest()));
        }
    }

    /**
     * One non-blocking pass: a Reactive Streams subscriber with a demand of one chunk at a time. Signals are serial
     * (Reactive Streams rule 1.3), so the accumulator needs no locking; the future completes exactly once.
     */
    static final class Pass implements Subscriber<ByteBuffer> {
        private final Accumulator digests;
        private final CompletableFuture<Result> result = new CompletableFuture<>();
        private final AtomicBoolean subscribed = new AtomicBoolean();
        private volatile Subscription subscription;
        private volatile boolean cancelled;

        private Pass(Accumulator digests) {
            this.digests = digests;
        }

        /** The digests once the body completed, or the body's failure. */
        CompletableFuture<Result> result() {
            return result;
        }

        /** Stops reading: cancels the subscription (now, or as soon as it arrives) and fails the result. */
        void cancel() {
            cancelled = true;
            Subscription s = subscription;
            if (s != null) {
                s.cancel();
            }
            result.completeExceptionally(SdkClientException.create("Reading the request payload was cancelled"));
        }

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s, "subscription");
            if (!subscribed.compareAndSet(false, true)) {
                // Rule 2.5: a second subscription is cancelled.
                s.cancel();
                return;
            }
            subscription = s;
            if (cancelled) {
                s.cancel();
                return;
            }
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            Objects.requireNonNull(chunk, "chunk");
            if (result.isDone()) {
                return;
            }
            try {
                digests.update(chunk);
            } catch (RuntimeException e) {
                // Rule 2.13 forbids throwing: the pass fails and stops reading.
                subscription.cancel();
                result.completeExceptionally(e);
                return;
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable failure) {
            Objects.requireNonNull(failure, "failure");
            result.completeExceptionally(SdkClientException.create(
                "Failed to read the request payload to compute its digests", failure));
        }

        @Override
        public void onComplete() {
            if (!result.isDone()) {
                result.complete(digests.result());
            }
        }
    }
}
