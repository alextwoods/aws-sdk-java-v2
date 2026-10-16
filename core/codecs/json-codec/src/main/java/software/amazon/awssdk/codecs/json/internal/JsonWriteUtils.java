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

package software.amazon.awssdk.codecs.json.internal;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.commons.internal.NumberCodec;
import software.amazon.awssdk.codecs.commons.internal.TimestampCodec;

/**
 * Low-level utilities for writing JSON primitives directly to byte arrays. All methods write UTF-8 encoded JSON bytes
 * and return the new write position.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.JsonWriteUtils}. The {@code
 * CompactStringAccess} Latin-1 fast path (and the word-at-a-time copy it feeds) is not ported: ASCII strings are
 * written with the {@code charAt} loop that smithy-java uses when compact string access is unavailable.
 */
@SdkInternalApi
final class JsonWriteUtils {

    static final byte[] NULL_BYTES = {'n', 'u', 'l', 'l'};

    private static final byte[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    // Two-character escapes for control characters and special characters; 0 means "use \\uXXXX".
    private static final byte[] ESCAPE_TABLE = new byte[128];
    private static final boolean[] NEEDS_ESCAPE = new boolean[128];

    static {
        for (int i = 0; i < 0x20; i++) {
            NEEDS_ESCAPE[i] = true;
        }
        NEEDS_ESCAPE['"'] = true;
        NEEDS_ESCAPE['\\'] = true;

        ESCAPE_TABLE['"'] = '"';
        ESCAPE_TABLE['\\'] = '\\';
        ESCAPE_TABLE['\b'] = 'b';
        ESCAPE_TABLE['\f'] = 'f';
        ESCAPE_TABLE['\n'] = 'n';
        ESCAPE_TABLE['\r'] = 'r';
        ESCAPE_TABLE['\t'] = 't';
    }

    private JsonWriteUtils() {
    }

    static int writeInt(byte[] buf, int pos, int value) {
        return NumberCodec.writeInt(buf, pos, value);
    }

    static int writeLong(byte[] buf, int pos, long value) {
        return NumberCodec.writeLong(buf, pos, value);
    }

    /**
     * Writes a quoted JSON string. Requires {@link #maxQuotedStringBytes} of capacity.
     */
    static int writeQuotedString(byte[] buf, int pos, String value) {
        int next = tryWriteQuotedAscii(buf, pos, value);
        return next >= 0 ? next : writeQuotedStringGeneral(buf, pos, value);
    }

    /**
     * Writes a quoted string if every char is unescaped ASCII. Returns {@code -1} otherwise. Requires
     * {@code value.length() + 2} bytes; on rejection, retry from the original position.
     */
    static int tryWriteQuotedAscii(byte[] buf, int pos, String value) {
        int len = value.length();
        int p = pos;
        buf[p++] = '"';
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c >= 0x80 || c < 0x20 || c == '"' || c == '\\') {
                return -1;
            }
            buf[p++] = (byte) c;
        }
        buf[p++] = '"';
        return p;
    }

    /**
     * Writes a JSON quoted string that may need escaping or multi-byte encoding, from scratch. Requires
     * {@link #maxQuotedStringBytes} of capacity.
     */
    static int writeQuotedStringGeneral(byte[] buf, int pos, String value) {
        int len = value.length();
        buf[pos++] = '"';
        if (len == 0) {
            buf[pos++] = '"';
            return pos;
        }

        // Copy the safe ASCII prefix before using the per-char encoder.
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c >= 0x80 || c < 0x20 || c == '"' || c == '\\') {
                pos = writeStringSlowPath(buf, pos, value, i, len);
                buf[pos++] = '"';
                return pos;
            }
            buf[pos++] = (byte) c;
        }
        buf[pos++] = '"';
        return pos;
    }

    static int writeEpochSeconds(byte[] buf, int pos, long epochSecond, int nano) {
        return TimestampCodec.writeEpochSeconds(buf, pos, epochSecond, nano);
    }

    static int writeIso8601Timestamp(byte[] buf, int pos, Instant value) {
        buf[pos++] = '"';
        pos = TimestampCodec.writeIso8601(buf, pos, value);
        buf[pos++] = '"';
        return pos;
    }

    static int writeHttpDate(byte[] buf, int pos, Instant value) {
        buf[pos++] = '"';
        pos = TimestampCodec.writeHttpDate(buf, pos, value);
        buf[pos++] = '"';
        return pos;
    }

    /**
     * Base64-encodes the given data and writes it as a JSON quoted string. Returns the new write position.
     */
    static int writeBase64String(byte[] buf, int pos, byte[] data, int off, int len) {
        return writeBase64String(buf, pos, ByteBuffer.wrap(data, off, len));
    }

    static int writeBase64String(byte[] buf, int pos, ByteBuffer data) {
        buf[pos++] = '"';
        byte[] encoded = ByteBufferUtils.base64EncodeToBytes(data);
        System.arraycopy(encoded, 0, buf, pos, encoded.length);
        pos += encoded.length;
        buf[pos++] = '"';
        return pos;
    }

    /**
     * Returns the maximum number of bytes needed to write a JSON-quoted string, for buffer capacity estimation.
     */
    static int maxQuotedStringBytes(String value) {
        return value.length() * 6 + 2;
    }

    /**
     * Computes the UTF-8 bytes of a JSON field name prefix: the opening quote, the escaped field name, the closing
     * quote and the colon. For field name {@code foo}, returns the bytes of {@code "foo":}.
     *
     * <p>The name is escaped like any other JSON string: a {@code jsonName} trait can carry any character, including
     * {@code "} and {@code \}, which would otherwise emit invalid JSON.
     */
    static byte[] encodeFieldNameToken(String fieldName) {
        byte[] scratch = new byte[maxQuotedStringBytes(fieldName) + 1];
        int pos = writeQuotedString(scratch, 0, fieldName);
        scratch[pos++] = ':';
        return Arrays.copyOf(scratch, pos);
    }

    private static int writeStringSlowPath(byte[] buf, int pos, String value, int startIdx, int len) {
        for (int i = startIdx; i < len; i++) {
            char c = value.charAt(i);
            if (c < 0x80) {
                if (c >= 0x20 && !NEEDS_ESCAPE[c]) {
                    buf[pos++] = (byte) c;
                } else if (ESCAPE_TABLE[c] != 0) {
                    buf[pos++] = '\\';
                    buf[pos++] = ESCAPE_TABLE[c];
                } else {
                    pos = writeUnicodeEscape(buf, pos, c);
                }
            } else if (c < 0x800) {
                buf[pos++] = (byte) (0xC0 | (c >> 6));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            } else if (!Character.isSurrogate(c)) {
                buf[pos++] = (byte) (0xE0 | (c >> 12));
                buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            } else if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                int cp = Character.toCodePoint(c, value.charAt(++i));
                buf[pos++] = (byte) (0xF0 | (cp >> 18));
                buf[pos++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                buf[pos++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (cp & 0x3F));
            } else {
                // Lone surrogate: escape it as unicode; the next char (if any) is processed on its own.
                pos = writeUnicodeEscape(buf, pos, c);
            }
        }
        return pos;
    }

    private static int writeUnicodeEscape(byte[] buf, int pos, int c) {
        buf[pos++] = '\\';
        buf[pos++] = 'u';
        buf[pos++] = HEX[(c >> 12) & 0xF];
        buf[pos++] = HEX[(c >> 8) & 0xF];
        buf[pos++] = HEX[(c >> 4) & 0xF];
        buf[pos++] = HEX[c & 0xF];
        return pos;
    }
}
