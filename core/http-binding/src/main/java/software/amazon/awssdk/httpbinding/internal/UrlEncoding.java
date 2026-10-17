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

package software.amazon.awssdk.httpbinding.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Percent-encoding and decoding per RFC 3986.
 *
 * <p>Port of smithy-java {@code io.uri.URLEncoding}. Used for HTTP labels; query parameters are encoded by the
 * {@code SdkHttpFullRequest} (through {@code SdkHttpUtils.urlEncode}, which encodes the same unreserved set).
 */
@SdkInternalApi
final class UrlEncoding {
    // RFC 3986 unreserved character set encoded as a 128-bit table split into two longs.
    // Bit `c` of UNRESERVED_LOW (for c in 0..63) or bit `c - 64` of UNRESERVED_HIGH
    // (for c in 64..127) is set iff `c` is unreserved.
    private static final long UNRESERVED_LOW;
    private static final long UNRESERVED_HIGH;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    static {
        long low = 0L;
        long high = 0L;
        for (char c = 'A'; c <= 'Z'; c++) {
            high |= 1L << (c - 64);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            high |= 1L << (c - 64);
        }
        for (char c = '0'; c <= '9'; c++) {
            low |= 1L << c;
        }
        low |= 1L << '-';
        low |= 1L << '.';
        high |= 1L << ('_' - 64);
        high |= 1L << ('~' - 64);
        UNRESERVED_LOW = low;
        UNRESERVED_HIGH = high;
    }

    private UrlEncoding() {
    }

    /**
     * Returns true iff {@code c} is an RFC 3986 unreserved ASCII character (alphanumeric, '-', '.', '_', or '~').
     */
    static boolean isUnreserved(int c) {
        if (c >= 128 || c < 0) {
            return false;
        }
        return c < 64 ? ((UNRESERVED_LOW >>> c) & 1L) != 0L : ((UNRESERVED_HIGH >>> (c - 64)) & 1L) != 0L;
    }

    /**
     * Percent-encodes every UTF-8 byte of {@code source} outside the RFC 3986 unreserved set. If
     * {@code preserveSlashes} is true, '/' is also left unencoded. Existing percent-encoding is encoded again. A lone
     * surrogate is encoded as U+FFFD.
     */
    static void encodeUnreserved(String source, StringBuilder sink, boolean preserveSlashes) {
        int len = source.length();
        sink.ensureCapacity(sink.length() + len);

        // Fast path: skip encoding if the input is already URL-safe.
        int i = 0;
        while (i < len) {
            char c = source.charAt(i);
            if (isUnreserved(c) || (preserveSlashes && c == '/')) {
                i++;
            } else {
                break;
            }
        }

        if (i == len) {
            sink.append(source);
            return;
        }

        if (i > 0) {
            sink.append(source, 0, i);
        }

        // Slow path: encode the remainder character-by-character.
        for (; i < len; i++) {
            char c = source.charAt(i);
            if (isUnreserved(c)) {
                sink.append(c);
            } else if (preserveSlashes && c == '/') {
                sink.append('/');
            } else if (c < 0x80) {
                percentEncode(sink, (byte) c);
            } else {
                int codePoint;
                if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(source.charAt(i + 1))) {
                    codePoint = Character.toCodePoint(c, source.charAt(i + 1));
                    i++;
                } else if (Character.isSurrogate(c)) {
                    // Lone surrogate -> U+FFFD
                    codePoint = 0xFFFD;
                } else {
                    codePoint = c;
                }
                encodeUtf8(sink, codePoint);
            }
        }
    }

    /**
     * Percent-encodes {@code source}; see {@link #encodeUnreserved(String, StringBuilder, boolean)}.
     */
    static String encodeUnreserved(String source, boolean preserveSlashes) {
        StringBuilder result = new StringBuilder(source.length());
        encodeUnreserved(source, result, preserveSlashes);
        return result.toString();
    }

    // Encodes a code point of at least U+0080 as percent-encoded UTF-8 bytes.
    private static void encodeUtf8(StringBuilder sink, int cp) {
        if (cp < 0x800) {
            percentEncode(sink, (byte) (0xC0 | (cp >> 6)));
            percentEncode(sink, (byte) (0x80 | (cp & 0x3F)));
        } else if (cp < 0x10000) {
            percentEncode(sink, (byte) (0xE0 | (cp >> 12)));
            percentEncode(sink, (byte) (0x80 | ((cp >> 6) & 0x3F)));
            percentEncode(sink, (byte) (0x80 | (cp & 0x3F)));
        } else {
            percentEncode(sink, (byte) (0xF0 | (cp >> 18)));
            percentEncode(sink, (byte) (0x80 | ((cp >> 12) & 0x3F)));
            percentEncode(sink, (byte) (0x80 | ((cp >> 6) & 0x3F)));
            percentEncode(sink, (byte) (0x80 | (cp & 0x3F)));
        }
    }

    private static void percentEncode(StringBuilder sink, byte b) {
        sink.append('%');
        sink.append(HEX[(b >> 4) & 0x0F]);
        sink.append(HEX[b & 0x0F]);
    }
}
