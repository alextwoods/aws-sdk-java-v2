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

package software.amazon.awssdk.transfer.s3.internal.download;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The destination file of a download, written at explicit positions by any number of parts concurrently. Writes and
 * {@link #close()} are blocking file I/O and run on the transfer manager's I/O executor (or the pausing caller's
 * thread). Once {@link #close()} returns, no write is in progress and none will start, so a pause can record the
 * file's final state. Thread safe.
 */
final class DestinationFile {
    private final Path path;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private FileChannel channel;
    private boolean closed;

    DestinationFile(Path path) {
        this.path = path;
    }

    Path path() {
        return path;
    }

    /** Opens the file, creating it, and truncating it unless {@code keepContent} (a resumed download). */
    void open(boolean keepContent) throws IOException {
        lock.writeLock().lock();
        try {
            if (closed) {
                throw new ClosedChannelException();
            }
            channel = keepContent
                      ? FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                      : FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                                         StandardOpenOption.TRUNCATE_EXISTING);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Writes all of {@code data} at {@code position}; fails with {@link ClosedChannelException} once closed. */
    void write(ByteBuffer data, long position) throws IOException {
        lock.readLock().lock();
        try {
            if (closed || channel == null) {
                throw new ClosedChannelException();
            }
            long at = position;
            while (data.hasRemaining()) {
                at += channel.write(data, at);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Closes the file after any write in progress; idempotent. */
    void close() throws IOException {
        lock.writeLock().lock();
        try {
            closed = true;
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Closes the file, ignoring failures (failure and cancellation paths). */
    void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
            // The transfer already failed; the descriptor is released by close even when it reports an error.
        }
    }
}
