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

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Incremental gzip (RFC 1952) compression: a fixed 10-byte header, raw deflate data of every chunk, then the CRC-32
 * and length (mod 2^32) trailer. The deflater's native memory is released by {@link #release()}, which the stream and
 * publisher views call exactly once.
 */
public final class GzipTransform implements ChunkTransform {
    /** The gzip header: magic, deflate, no flags, no mtime, no extra flags, unknown OS. */
    private static final byte[] HEADER = {0x1f, (byte) 0x8b, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, (byte) 0xff};

    private final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    private final CRC32 crc = new CRC32();
    private final byte[] buffer = new byte[16 * 1024];
    private boolean headerWritten;
    private long inputLength;

    /** The gzip encoding of {@code bytes}. */
    public static byte[] compress(byte[] bytes) {
        GzipTransform transform = new GzipTransform();
        try {
            ByteSink out = new ByteSink(bytes.length / 2 + 64);
            out.write(transform.onChunk(ByteBuffer.wrap(bytes)));
            out.write(transform.onComplete());
            ByteBuffer all = out.drain();
            byte[] result = new byte[all.remaining()];
            all.get(result);
            return result;
        } finally {
            transform.release();
        }
    }

    @Override
    public ByteBuffer onChunk(ByteBuffer chunk) {
        ByteSink out = new ByteSink(chunk.remaining() / 2 + HEADER.length);
        writeHeader(out);
        crc.update(chunk.duplicate());
        inputLength += chunk.remaining();
        deflater.setInput(chunk.duplicate());
        while (!deflater.needsInput()) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        return out.drain();
    }

    @Override
    public ByteBuffer onComplete() {
        ByteSink out = new ByteSink(buffer.length);
        writeHeader(out);
        deflater.finish();
        while (!deflater.finished()) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        writeIntLe(out, crc.getValue());
        writeIntLe(out, inputLength);
        return out.drain();
    }

    @Override
    public void release() {
        deflater.end();
    }

    private void writeHeader(ByteSink out) {
        if (!headerWritten) {
            headerWritten = true;
            out.write(HEADER, 0, HEADER.length);
        }
    }

    private static void writeIntLe(ByteSink out, long value) {
        out.writeByte((int) value).writeByte((int) (value >>> 8)).writeByte((int) (value >>> 16))
           .writeByte((int) (value >>> 24));
    }
}
