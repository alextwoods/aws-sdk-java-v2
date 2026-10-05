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

package software.amazon.awssdk.checksums.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * The blocking view of a {@link ChunkTransform} over a source stream: reads at most one source chunk ahead and
 * buffers only the transform's output for that chunk. Closing closes the source and releases the transform.
 *
 * <p>Runtime exceptions of the transform (for example a checksum mismatch) propagate unchanged from {@code read}.
 */
public final class TransformingInputStream extends InputStream {
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    private final InputStream source;
    private final ChunkTransform transform;
    private final byte[] readBuffer;
    private ByteBuffer output = EMPTY;
    private boolean sourceDone;
    private boolean finished;
    private boolean closed;

    public TransformingInputStream(InputStream source, ChunkTransform transform, int readSize) {
        this.source = Objects.requireNonNull(source, "source");
        this.transform = Objects.requireNonNull(transform, "transform");
        if (readSize <= 0) {
            throw new IllegalArgumentException("A read size is positive: " + readSize);
        }
        this.readBuffer = new byte[readSize];
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        return n < 0 ? -1 : one[0] & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if (closed) {
            throw new IOException("Stream closed");
        }
        if (length == 0) {
            return 0;
        }
        while (!output.hasRemaining()) {
            if (finished) {
                return -1;
            }
            fill();
        }
        int n = Math.min(length, output.remaining());
        output.get(bytes, offset, n);
        return n;
    }

    @Override
    public int available() {
        return closed ? 0 : output.remaining();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            source.close();
        } finally {
            transform.release();
        }
    }

    private void fill() throws IOException {
        try {
            if (!sourceDone) {
                int n = source.read(readBuffer, 0, readBuffer.length);
                if (n < 0) {
                    sourceDone = true;
                } else if (n > 0) {
                    output = transform.onChunk(ByteBuffer.wrap(readBuffer, 0, n));
                }
            } else {
                finished = true;
                output = transform.onComplete();
            }
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
