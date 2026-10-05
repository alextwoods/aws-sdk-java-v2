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
import java.util.Objects;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * The unsigned {@code aws-chunked} encoding with a checksum trailer ({@code STREAMING-UNSIGNED-PAYLOAD-TRAILER}):
 *
 * <pre>
 * hex(size) CRLF data CRLF        (one frame per {@code chunkSize} bytes; the last frame may be shorter)
 * 0 CRLF
 * x-amz-checksum-&lt;alg&gt;:base64(checksum) CRLF
 * CRLF
 * </pre>
 *
 * <p>Frames have exactly {@code chunkSize} data bytes except the last, so the encoded length is a function of the
 * decoded length ({@link #encodedLength}) and can be sent as {@code Content-Length}. The checksum covers exactly the
 * decoded payload bytes. At most one frame of data is buffered.
 */
public final class AwsChunkedTrailerTransform implements ChunkTransform {
    /** Data bytes per frame: the default bounded chunk size of streaming bodies. */
    public static final int DEFAULT_CHUNK_SIZE = 64 * 1024;

    private static final String CRLF = "\r\n";

    private final SdkChecksum checksum;
    private final String trailerName;
    private final byte[] frame;
    private int filled;

    public AwsChunkedTrailerTransform(ChecksumAlgorithm algorithm, int chunkSize) {
        Objects.requireNonNull(algorithm, "algorithm");
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("A chunk size is positive: " + chunkSize);
        }
        this.checksum = SdkChecksum.create(algorithm);
        this.trailerName = algorithm.headerName();
        this.frame = new byte[chunkSize];
    }

    /** Length of the encoding of {@code decodedLength} payload bytes with {@code algorithm}'s trailer. */
    public static long encodedLength(long decodedLength, ChecksumAlgorithm algorithm, int chunkSize) {
        if (decodedLength < 0) {
            throw new IllegalArgumentException("A decoded length is non-negative: " + decodedLength);
        }
        long fullFrames = decodedLength / chunkSize;
        long remainder = decodedLength % chunkSize;
        long length = fullFrames * frameLength(chunkSize);
        if (remainder > 0) {
            length += frameLength(remainder);
        }
        return length
               + 3                                        // "0" CRLF
               + algorithm.headerName().length() + 1      // name ":"
               + algorithm.base64Length() + 2             // value CRLF
               + 2;                                       // CRLF
    }

    private static long frameLength(long dataLength) {
        return Long.toHexString(dataLength).length() + 2 + dataLength + 2;
    }

    @Override
    public ByteBuffer onChunk(ByteBuffer chunk) {
        checksum.update(chunk.duplicate());
        ByteSink out = null;
        ByteBuffer data = chunk.duplicate();
        while (data.hasRemaining()) {
            int n = Math.min(frame.length - filled, data.remaining());
            data.get(frame, filled, n);
            filled += n;
            if (filled == frame.length) {
                if (out == null) {
                    out = new ByteSink(frame.length + 16);
                }
                writeFrame(out);
            }
        }
        return out == null ? ByteBuffer.allocate(0) : out.drain();
    }

    @Override
    public ByteBuffer onComplete() {
        ByteSink out = new ByteSink(filled + 128);
        if (filled > 0) {
            writeFrame(out);
        }
        out.writeAscii("0" + CRLF + trailerName + ":" + checksum.digestBase64() + CRLF + CRLF);
        return out.drain();
    }

    private void writeFrame(ByteSink out) {
        out.writeAscii(Integer.toHexString(filled) + CRLF).write(frame, 0, filled).writeAscii(CRLF);
        filled = 0;
    }
}
