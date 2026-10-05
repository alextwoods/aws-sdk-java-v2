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

package software.amazon.awssdk.core.internal.async;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * One subscription's cursor over a body: {@link #next()} returns the next non-empty read-only chunk, or {@code null}
 * at the end. Opened by a {@link Factory} per subscription, so a replayable body restarts from its first byte.
 */
public interface ChunkSource extends Closeable {
    /** The next chunk, never empty, or {@code null} once the body is exhausted. */
    ByteBuffer next() throws IOException;

    @Override
    default void close() throws IOException {
    }

    /** Opens a fresh {@link ChunkSource} for each subscription. */
    @FunctionalInterface
    interface Factory {
        ChunkSource open() throws IOException;
    }

    /**
     * Zero-copy read-only slices of at most {@code chunkSize} bytes of the remaining bytes of {@code data}, which must
     * not be modified afterwards.
     */
    static Factory ofBuffer(ByteBuffer data, int chunkSize) {
        ByteBuffer frozen = data.asReadOnlyBuffer();
        requireChunkSize(chunkSize);
        return () -> {
            ByteBuffer view = frozen.duplicate();
            return () -> {
                if (!view.hasRemaining()) {
                    return null;
                }
                int size = Math.min(chunkSize, view.remaining());
                ByteBuffer chunk = view.slice(view.position(), size).asReadOnlyBuffer();
                view.position(view.position() + size);
                return chunk;
            };
        };
    }

    /**
     * Chunks of at most {@code chunkSize} bytes of exactly {@code length} bytes of {@code file}, reopened for each
     * subscription. A file that is shorter or longer than {@code length} when read fails with {@link IOException},
     * so a body whose file changed after its length was taken is never sent with a wrong length.
     */
    static Factory ofFile(Path file, long length, int chunkSize) {
        Objects.requireNonNull(file, "file");
        requireChunkSize(chunkSize);
        return () -> new FileChunkSource(FileChannel.open(file, StandardOpenOption.READ), file, length, chunkSize);
    }

    private static void requireChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("A chunk size is positive: " + chunkSize);
        }
    }

    /** Reads a file sequentially through its own channel. */
    final class FileChunkSource implements ChunkSource {
        private final FileChannel channel;
        private final Path file;
        private final long length;
        private final int chunkSize;
        private long read;

        private FileChunkSource(FileChannel channel, Path file, long length, int chunkSize) {
            this.channel = channel;
            this.file = file;
            this.length = length;
            this.chunkSize = chunkSize;
        }

        @Override
        public ByteBuffer next() throws IOException {
            if (read == length) {
                if (channel.size() != length) {
                    throw new IOException("File " + file + " changed size while being read: expected " + length
                                          + " bytes, now " + channel.size() + ".");
                }
                return null;
            }
            ByteBuffer chunk = ByteBuffer.allocate((int) Math.min(chunkSize, length - read));
            while (chunk.hasRemaining()) {
                if (channel.read(chunk) < 0) {
                    throw new IOException("File " + file + " ended after " + (read + chunk.position())
                                          + " bytes, but " + length + " were expected.");
                }
            }
            read += chunk.capacity();
            return chunk.flip().asReadOnlyBuffer();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}
