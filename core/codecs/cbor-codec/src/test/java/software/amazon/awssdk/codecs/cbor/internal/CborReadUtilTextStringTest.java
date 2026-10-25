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

package software.amazon.awssdk.codecs.cbor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * SDK tests for the ASCII fast path of {@link CborReadUtil#readTextString} (smithy-java {@code 8c5cf2907}, round 2
 * P4): for every definite length up to past {@link CborReadUtil#ASCII_SCAN_LIMIT}, at several offsets, the result
 * equals the UTF-8 constructor's, for ASCII input, well-formed multibyte UTF-8 and malformed UTF-8 at every position.
 */
class CborReadUtilTextStringTest {

    private static final int MAX_OFFSET = 2;
    private static final int MAX_LENGTH = 40;

    private static final byte[][] MULTIBYTE = {
        {(byte) 0xC3, (byte) 0xA9},                         // U+00E9, 2 bytes
        {(byte) 0xE2, (byte) 0x82, (byte) 0xAC},            // U+20AC, 3 bytes
        {(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80} // U+1F600, 4 bytes (a surrogate pair)
    };

    private static final byte[][] MALFORMED = {
        {(byte) 0x80},                                      // lone continuation byte
        {(byte) 0xE2, (byte) 0x82},                         // truncated 3-byte sequence
        {(byte) 0xC0, (byte) 0x80}                          // overlong encoding of U+0000
    };

    @Test
    void sweepCoversTheScanLimit() {
        assertThat(CborReadUtil.ASCII_SCAN_LIMIT).isEqualTo(32);
        assertThat(MAX_LENGTH).isGreaterThan(CborReadUtil.ASCII_SCAN_LIMIT + 1);
    }

    @Test
    void readTextString_ascii_matchesUtf8Constructor() {
        for (int first = 0; first < 0x80; first++) {
            for (int off = 0; off <= MAX_OFFSET; off++) {
                for (int len = 0; len <= MAX_LENGTH; len++) {
                    byte[] buffer = new byte[off + len];
                    for (int i = 0; i < len; i++) {
                        buffer[off + i] = (byte) ((first + i) & 0x7F);
                    }
                    assertDecodesLikeUtf8(buffer, off, len);
                }
            }
        }
    }

    @Test
    void readTextString_everyNonAsciiByteAtEveryPosition_matchesUtf8Constructor() {
        for (int high = 0x80; high <= 0xFF; high++) {
            for (int off = 0; off <= MAX_OFFSET; off++) {
                for (int len = 1; len <= MAX_LENGTH; len++) {
                    for (int pos = 0; pos < len; pos++) {
                        byte[] buffer = asciiBuffer(off, len);
                        buffer[off + pos] = (byte) high;
                        assertDecodesLikeUtf8(buffer, off, len);
                    }
                }
            }
        }
    }

    @Test
    void readTextString_multibyteSequenceAtEveryPosition_matchesUtf8Constructor() {
        assertSequenceAtEveryPosition(MULTIBYTE);
    }

    @Test
    void readTextString_malformedSequenceAtEveryPosition_matchesUtf8Constructor() {
        assertSequenceAtEveryPosition(MALFORMED);
    }

    @Test
    void readTextString_aroundTheScanLimit_matchesUtf8Constructor() {
        int limit = CborReadUtil.ASCII_SCAN_LIMIT;
        for (int len = limit - 1; len <= limit + 1; len++) {
            byte[] ascii = asciiBuffer(1, len);
            assertThat(CborReadUtil.readTextString(ascii, 1, len))
                .isEqualTo(new String(ascii, 1, len, StandardCharsets.US_ASCII));

            byte[] lastNonAscii = asciiBuffer(1, len);
            lastNonAscii[len] = (byte) 0xE9;
            assertDecodesLikeUtf8(lastNonAscii, 1, len);
        }
    }

    @Test
    void readTextString_ignoresBytesOutsideTheRange() {
        byte[] buffer = {(byte) 0xC3, 'a', 'b', 'c', (byte) 0xA9};
        assertThat(CborReadUtil.readTextString(buffer, 1, 3)).isEqualTo("abc");
        assertThat(CborReadUtil.readTextString(buffer, 0, 0)).isEmpty();
        assertThat(CborReadUtil.readTextString(buffer, 5, 0)).isEmpty();
    }

    private static void assertSequenceAtEveryPosition(byte[][] sequences) {
        for (byte[] sequence : sequences) {
            for (int off = 0; off <= MAX_OFFSET; off++) {
                for (int len = sequence.length; len <= MAX_LENGTH; len++) {
                    for (int pos = 0; pos + sequence.length <= len; pos++) {
                        byte[] buffer = asciiBuffer(off, len);
                        System.arraycopy(sequence, 0, buffer, off + pos, sequence.length);
                        assertDecodesLikeUtf8(buffer, off, len);
                    }
                }
            }
        }
    }

    /** A buffer of exactly {@code off + len} bytes with ASCII letters in the range and a non-ASCII byte before it. */
    private static byte[] asciiBuffer(int off, int len) {
        byte[] buffer = new byte[off + len];
        for (int i = 0; i < off; i++) {
            buffer[i] = (byte) 0xC3;
        }
        for (int i = 0; i < len; i++) {
            buffer[off + i] = (byte) ('a' + i % 26);
        }
        return buffer;
    }

    private static void assertDecodesLikeUtf8(byte[] buffer, int off, int len) {
        assertThat(CborReadUtil.readTextString(buffer, off, len))
            .isEqualTo(new String(buffer, off, len, StandardCharsets.UTF_8));
    }
}
