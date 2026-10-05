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

package software.amazon.awssdk.transfer.s3.internal.model;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.ToString;

/** The {@link FileDownload} handle: its completion future and progress and pause action. */
public final class DefaultFileDownload implements FileDownload {
    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final TransferProgress progress;
    private final Supplier<ResumableFileDownload> pause;

    public DefaultFileDownload(CompletableFuture<CompletedFileDownload> completionFuture, TransferProgress progress, Supplier<ResumableFileDownload> pause) {
        this.completionFuture = completionFuture;
        this.progress = progress;
        this.pause = pause;
    }

    @Override
    public CompletableFuture<CompletedFileDownload> completionFuture() {
        return completionFuture;
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public ResumableFileDownload pause() {
        return pause.get();
    }

    @Override
    public String toString() {
        return ToString.builder("FileDownload").add("completionFuture", completionFuture)
                       .add("progress", progress).build();
    }
}
