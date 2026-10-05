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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.transfer.s3.internal.util.Futures;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The progress of one object transfer and the listeners observing it. Byte counts may move backwards: when an attempt
 * of a part (or of a single request) is retried, the bytes the failed attempt reported are subtracted, so the snapshot
 * always counts bytes that the current attempts have transferred. Thread safe; listener calls run on the thread that
 * reports the progress and must not block.
 */
public final class TransferProgressUpdater {
    private final TransferObjectRequest request;
    private final DefaultTransferProgress progress;
    private final TransferListenerInvoker listeners;

    public TransferProgressUpdater(TransferObjectRequest request, Long totalBytes) {
        this.request = request;
        this.progress = new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder()
                                                                                   .transferredBytes(0L)
                                                                                   .totalBytes(totalBytes)
                                                                                   .build());
        this.listeners = new TransferListenerInvoker(request.transferListeners() == null
                                                     ? java.util.List.of() : request.transferListeners());
    }

    public TransferProgress progress() {
        return progress;
    }

    public void transferInitiated() {
        listeners.transferInitiated(context(progress.snapshot()));
    }

    /** Records the object size once it is known. */
    public void totalBytes(long totalBytes) {
        progress.updateAndGet(b -> b.totalBytes(totalBytes));
    }

    /** Records the response the snapshot reports. */
    public void response(SerializableStruct response) {
        progress.updateAndGet(b -> b.sdkResponse(response));
    }

    /** Adds {@code delta} (negative to retract a failed attempt's bytes) and notifies the listeners. */
    public void bytesTransferred(long delta) {
        if (delta == 0) {
            return;
        }
        TransferProgressSnapshot snapshot = progress.updateAndGet(b -> {
            long next = Math.max(0, b.getTransferredBytes() + delta);
            Long total = b.getTotalBytes();
            b.transferredBytes(total == null ? next : Math.min(next, total));
        });
        if (!listeners.isEmpty()) {
            listeners.bytesTransferred(context(snapshot));
        }
    }

    /** Notifies the listeners of the outcome of {@code completion}, once. */
    public <T extends CompletedObjectTransfer> void registerCompletion(CompletableFuture<T> completion) {
        completion.whenComplete((result, failure) -> {
            if (failure != null) {
                listeners.transferFailed(TransferListenerFailedContext.builder()
                                                                      .transferContext(context(progress.snapshot()))
                                                                      .exception(Futures.unwrap(failure))
                                                                      .build());
                return;
            }
            SerializableStruct response = responseOf(result);
            TransferProgressSnapshot snapshot = response == null ? progress.snapshot()
                                                                 : progress.updateAndGet(b -> b.sdkResponse(response));
            listeners.transferComplete(TransferListenerContext.builder()
                                                              .request(request)
                                                              .progressSnapshot(snapshot)
                                                              .completedTransfer(result)
                                                              .build());
        });
    }

    private static SerializableStruct responseOf(CompletedObjectTransfer result) {
        try {
            return result.response();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private TransferListenerContext context(TransferProgressSnapshot snapshot) {
        return TransferListenerContext.builder().request(request).progressSnapshot(snapshot).build();
    }
}
