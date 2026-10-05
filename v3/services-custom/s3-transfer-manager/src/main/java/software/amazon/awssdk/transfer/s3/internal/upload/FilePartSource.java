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

package software.amazon.awssdk.transfer.s3.internal.upload;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executor;
import java.util.function.Function;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.transfer.s3.internal.body.FileRangeAsyncRequestBody;

/**
 * The parts of a file: part {@code n} is the byte range {@code [(n - 1) * partSize, min(n * partSize, fileLength))},
 * read lazily (and re-read on retry) by a {@link FileRangeAsyncRequestBody}. Nothing is buffered, so a file upload
 * needs one read chunk per part in flight whatever the file size.
 */
public final class FilePartSource implements PartSource {
    private final Path path;
    private final long fileLength;
    private final long partSize;
    private final int totalParts;
    private final Executor executor;
    private final FileTime modified;
    private final Function<AsyncRequestBody, AsyncRequestBody> bodyDecorator;
    private int nextPart = 1;
    private volatile boolean cancelled;

    public FilePartSource(Path path, long fileLength, long partSize, Executor executor, FileTime modified,
                          Function<AsyncRequestBody, AsyncRequestBody> bodyDecorator) {
        this.path = path;
        this.fileLength = fileLength;
        this.partSize = partSize;
        this.totalParts = Math.toIntExact(Math.max(1, (fileLength + partSize - 1) / partSize));
        this.executor = executor;
        this.modified = modified;
        this.bodyDecorator = bodyDecorator;
    }

    public int totalParts() {
        return totalParts;
    }

    @Override
    public synchronized void next(Receiver receiver) {
        if (cancelled || nextPart > totalParts) {
            receiver.onEnd();
            return;
        }
        int number = nextPart++;
        long offset = (number - 1) * partSize;
        long length = Math.min(partSize, fileLength - offset);
        AsyncRequestBody body = new FileRangeAsyncRequestBody(path, offset, length,
                                                              AsyncRequestBody.DEFAULT_CHUNK_SIZE, executor, modified);
        receiver.onPart(new UploadPart(number, length, bodyDecorator.apply(body), number == totalParts, () -> { }));
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
