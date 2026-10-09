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
package software.amazon.awssdk.test.boundedmemory;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.SplittableRandom;
/**
 * A deterministic payload generated on the fly, never materialized: byte {@code p} is
 * {@code PATTERN[p % PERIOD] ^ (p / PERIOD)}. The period (a prime) is coprime with every chunk size the SDK and the
 * transports use, and the block index perturbs each repetition, so a dropped, duplicated or reordered chunk changes
 * the bytes at its offset and is caught by {@link Verifier}.
 */
final class Payload {
    static final long GIB = 1L << 30;
    static final long MIB = 1L << 20;
    private static final int PERIOD = 65_521;
    private static final byte[] PATTERN = new byte[PERIOD];
    static {
        new SplittableRandom(0x5EED_18_08L).nextBytes(PATTERN);
    }
    private Payload() {
    }
    /** Writes payload bytes {@code [position, position + length)} to {@code target[offset..]}. */
    static void fill(long position, byte[] target, int offset, int length) {
        int index = (int) (position % PERIOD);
        byte block = (byte) (position / PERIOD);
        for (int i = 0; i < length; i++) {
            target[offset + i] = (byte) (PATTERN[index] ^ block);
            if (++index == PERIOD) {
                index = 0;
                block++;
            }
        }
    }
    /** The SHA-256 (hex) of the first {@code length} payload bytes, computed by streaming the generator. */
    static String sha256(long length) {
        MessageDigest digest = newSha256();
        byte[] buffer = new byte[64 * 1024];
        for (long position = 0; position < length; ) {
            int n = (int) Math.min(buffer.length, length - position);
            fill(position, buffer, 0, n);
            digest.update(buffer, 0, n);
            position += n;
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    /** A stream of the first {@code length} payload bytes that counts every byte it produces into {@code gauge}. */
    static InputStream stream(long length, InFlightGauge gauge) {
        return new InputStream() {
            private long position;
            @Override
            public int read() {
                byte[] one = new byte[1];
                return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
            }
            @Override
            public int read(byte[] b, int off, int len) {
                if (len == 0) {
                    return 0;
                }
                if (position >= length) {
                    return -1;
                }
                int n = (int) Math.min(len, length - position);
                fill(position, b, off, n);
                position += n;
                gauge.produced(n);
                return n;
            }
            @Override
            public int available() {
                return (int) Math.min(Integer.MAX_VALUE, length - position);
            }
        };
    }
    /**
     * A rolling verifier of received bytes: every byte is compared, in order, with the generator (the first mismatch is
     * reported with its offset) and fed into a SHA-256 of the whole stream.
     */
    static final class Verifier {
        private final MessageDigest digest = newSha256();
        private final byte[] expected = new byte[64 * 1024];
        private final byte[] copy = new byte[64 * 1024];
        private long position;
        void update(byte[] bytes, int offset, int length) {
            int done = 0;
            while (done < length) {
                int n = Math.min(expected.length, length - done);
                fill(position, expected, 0, n);
                int mismatch = Arrays.mismatch(bytes, offset + done, offset + done + n, expected, 0, n);
                if (mismatch >= 0) {
                    throw new AssertionError("Payload byte " + (position + mismatch) + " differs: expected "
                                             + expected[mismatch] + " but was " + bytes[offset + done + mismatch]);
                }
                digest.update(bytes, offset + done, n);
                position += n;
                done += n;
            }
        }
        void update(ByteBuffer buffer) {
            ByteBuffer view = buffer.duplicate();
            while (view.hasRemaining()) {
                int n = Math.min(copy.length, view.remaining());
                view.get(copy, 0, n);
                update(copy, 0, n);
            }
        }
        long position() {
            return position;
        }
        String sha256() {
            return HexFormat.of().formatHex(digest.digest());
        }
    }
}
