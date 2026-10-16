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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.internal.ByteArrays;
import software.amazon.awssdk.codecs.commons.internal.NumberCodec;
import software.amazon.awssdk.codecs.commons.internal.TimestampCodec;
import software.amazon.awssdk.schema.serde.SerializationException;

/**
 * Low-level utilities for parsing JSON primitives directly from byte arrays.
 *
 * <p>All methods implement strict RFC 8259 compliance: no leading zeros on numbers, no unescaped control characters
 * in strings, and UTF-8 validation of strings that contain escapes.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.JsonReadUtils}. The {@code VarHandle}
 * little-endian array views become {@link ByteArrays} reads.
 */
@SdkInternalApi
final class JsonReadUtils {

    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    private static final long ASCII_ZEROES = 0x3030303030303030L;
    private static final long ASCII_NINES = 0x3939393939393939L;
    private static final long ASCII_HIGH_BITS = 0x8080808080808080L;

    // Hex digit lookup table: -1 means invalid hex digit
    private static final int[] HEX_VALUES = new int[128];

    static {
        Arrays.fill(HEX_VALUES, -1);
        for (int i = '0'; i <= '9'; i++) {
            HEX_VALUES[i] = i - '0';
        }
        for (int i = 'a'; i <= 'f'; i++) {
            HEX_VALUES[i] = 10 + (i - 'a');
        }
        for (int i = 'A'; i <= 'F'; i++) {
            HEX_VALUES[i] = 10 + (i - 'A');
        }
    }

    private JsonReadUtils() {
    }

    /**
     * Reads eight bytes at {@code pos} as a little-endian word (the first byte in the low lane).
     */
    static long readWord(byte[] buf, int pos) {
        return ByteArrays.getLongLittleEndian(buf, pos);
    }

    /**
     * Returns the value of a positive ten-digit integer at {@code pos} that is not followed by a digit, a fraction or
     * an exponent, or -1 when the input is anything else.
     */
    static long tryParseTenDigitEpochSecond(byte[] buf, int pos, int end) {
        int stop = pos + 10;
        if (stop > end || (buf[pos] < '1' || buf[pos] > '9')) {
            return -1;
        }
        byte tenthByte = buf[pos + 9];
        if (tenthByte < '0' || tenthByte > '9') {
            return -1;
        }
        if (stop < end) {
            byte next = buf[stop];
            if ((next >= '0' && next <= '9') || next == '.' || next == 'e' || next == 'E') {
                return -1;
            }
        }

        long chunk = readWord(buf, pos);
        long digits = chunk - ASCII_ZEROES;
        // Subtraction sets a lane's high bit for bytes outside '0'..'9'.
        if (((digits | (ASCII_NINES - chunk)) & ASCII_HIGH_BITS) != 0) {
            return -1;
        }
        int ninth = buf[pos + 8] - '0';
        if ((ninth | (9 - ninth)) < 0) {
            return -1;
        }

        int tenth = tenthByte - '0';
        // The little-endian view places the first digit in the low byte.
        long pairs = (digits * 10 + (digits >>> 8)) & 0x00FF00FF00FF00FFL;
        long quads = (pairs * 100 + (pairs >>> 16)) & 0x0000FFFF0000FFFFL;
        long firstEight = (quads & 0xFFFF) * 10_000 + (quads >>> 32);
        return firstEight * 100 + ninth * 10L + tenth;
    }

    /**
     * Parses a JSON integer value starting at pos. Strict RFC 8259: no leading zeros, no + prefix. Stores the result
     * in {@code deser.parsedLong} and {@code deser.parsedEndPos}. Accumulates as negative to handle
     * {@code Long.MIN_VALUE}.
     */
    static void parseLong(byte[] buf, int pos, int end, SmithyJsonDeserializer deser) {
        if (pos >= end) {
            throw new SerializationException("Unexpected end of input while parsing number");
        }

        boolean negative = false;
        if (buf[pos] == '-') {
            negative = true;
            pos++;
            if (pos >= end) {
                throw new SerializationException("Unexpected end of input after '-'");
            }
        }

        byte first = buf[pos];
        if (first < '0' || first > '9') {
            throw new SerializationException("Expected digit, found: " + describeChar(first));
        }

        // RFC 8259: no leading zeros (except 0 itself)
        if (first == '0' && pos + 1 < end && buf[pos + 1] >= '0' && buf[pos + 1] <= '9') {
            throw new SerializationException("Leading zeros not allowed in JSON numbers");
        }

        // Accumulate as negative to handle Long.MIN_VALUE correctly (it has no positive counterpart)
        long value = -(first - '0');
        pos++;

        while (pos < end) {
            byte b = buf[pos];
            if (b < '0' || b > '9') {
                break;
            }
            long prev = value;
            value = value * 10 - (b - '0');
            if (value > prev) {
                // Overflowed past Long.MIN_VALUE
                throw new SerializationException("Number overflow");
            }
            pos++;
        }

        deser.parsedLong = negative ? value : -value;
        deser.parsedEndPos = pos;

        // Check for positive overflow (e.g., 9223372036854775808 without minus)
        if (!negative && deser.parsedLong < 0) {
            throw new SerializationException("Number overflow");
        }
    }

    /**
     * Parses a JSON number (integer or floating point) with strict RFC 8259 validation. Stores the result in
     * {@code deser.parsedDouble} and {@code deser.parsedEndPos}.
     */
    static void parseDouble(byte[] buf, int pos, int end, SmithyJsonDeserializer deser) {
        int start = pos;
        boolean negative = pos < end && buf[pos] == '-';
        if (negative) {
            pos++;
        }
        if (pos >= end) {
            throw new SerializationException("Unexpected end of input while parsing number");
        }

        byte first = buf[pos];
        if (first < '0' || first > '9') {
            throw new SerializationException("Expected digit, found: " + describeChar(first));
        }

        long unscaled = 0;
        int digits = 0;
        int scale = 0;
        boolean fast = true;
        if (first == '0') {
            pos++;
            if (pos < end && buf[pos] >= '0' && buf[pos] <= '9') {
                throw new SerializationException("Leading zeros not allowed in JSON numbers");
            }
        } else {
            while (pos < end) {
                if (pos + Long.BYTES <= end) {
                    long lanes = NumberCodec.digitLanes(buf, pos);
                    int count = NumberCodec.leadingDigitLaneCount(lanes);
                    if (count == 0) {
                        break;
                    }
                    // Skip two accumulation rounds when 19+ integer digits guarantee the general parser.
                    if (digits == 0 && count == Long.BYTES && hasNineteenDigitInteger(buf, pos, end)) {
                        fast = false;
                        digits = 19;
                        pos += 19;
                        while (pos < end && buf[pos] >= '0' && buf[pos] <= '9') {
                            pos++;
                        }
                        break;
                    }
                    if (digits + count <= NumberCodec.MAX_FAST_DECIMAL_DIGITS) {
                        unscaled = unscaled * NumberCodec.powerOfTen(count)
                                   + NumberCodec.combineDigitLanes(lanes >>> ((8 - count) << 3));
                    } else {
                        fast = false;
                    }
                    digits += count;
                    pos += count;
                    if (count < Long.BYTES) {
                        break;
                    }
                } else {
                    int d = buf[pos] - '0';
                    if (d < 0 || d > 9) {
                        break;
                    }
                    if (digits < NumberCodec.MAX_FAST_DECIMAL_DIGITS) {
                        unscaled = unscaled * 10 + d;
                    } else {
                        fast = false;
                    }
                    digits++;
                    pos++;
                }
            }
        }

        if (pos < end && buf[pos] == '.') {
            pos++;
            if (pos >= end || buf[pos] < '0' || buf[pos] > '9') {
                throw new SerializationException("Expected digit after decimal point");
            }
            while (pos < end) {
                if (pos + Long.BYTES <= end) {
                    long lanes = NumberCodec.digitLanes(buf, pos);
                    int count = NumberCodec.leadingDigitLaneCount(lanes);
                    if (count == 0) {
                        break;
                    }
                    if (digits + count <= NumberCodec.MAX_FAST_DECIMAL_DIGITS) {
                        unscaled = unscaled * NumberCodec.powerOfTen(count)
                                   + NumberCodec.combineDigitLanes(lanes >>> ((8 - count) << 3));
                    } else {
                        fast = false;
                    }
                    digits += count;
                    scale += count;
                    pos += count;
                    if (count < Long.BYTES) {
                        break;
                    }
                } else {
                    int d = buf[pos] - '0';
                    if (d < 0 || d > 9) {
                        break;
                    }
                    if (digits < NumberCodec.MAX_FAST_DECIMAL_DIGITS) {
                        unscaled = unscaled * 10 + d;
                    } else {
                        fast = false;
                    }
                    digits++;
                    scale++;
                    pos++;
                }
            }
        }

        if (pos < end && (buf[pos] == 'e' || buf[pos] == 'E')) {
            pos++;
            boolean expNegative = false;
            if (pos < end && (buf[pos] == '+' || buf[pos] == '-')) {
                expNegative = buf[pos] == '-';
                pos++;
            }
            if (pos >= end || buf[pos] < '0' || buf[pos] > '9') {
                throw new SerializationException("Expected digit in exponent");
            }
            int exponent = 0;
            while (pos < end && buf[pos] >= '0' && buf[pos] <= '9') {
                if (exponent < 10_000) {
                    exponent = exponent * 10 + (buf[pos] - '0');
                }
                pos++;
            }
            scale = expNegative ? scale + exponent : scale - exponent;
        }

        double value = fast ? NumberCodec.decimalToDouble(unscaled, scale) : Double.NaN;
        if (Double.isNaN(value)) {
            value = NumberCodec.parseDouble(buf, start, pos - start);
        } else if (negative) {
            value = -value;
        }
        deser.parsedDouble = value;
        deser.parsedEndPos = pos;
    }

    /**
     * Finds the end position of a JSON number starting at pos. Returns the position of the first non-number
     * character.
     */
    static int findNumberEnd(byte[] buf, int pos, int end) {
        while (pos < end) {
            byte b = buf[pos];
            if ((b >= '0' && b <= '9') || b == '-' || b == '+' || b == '.' || b == 'e' || b == 'E') {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    /**
     * Parses a JSON string starting at the opening quote. Stores the result in {@code deser.parsedString} and
     * {@code deser.parsedEndPos}. Strict: rejects unescaped control characters and invalid escape sequences.
     */
    static void parseString(byte[] buf, int pos, int end, SmithyJsonDeserializer deser) {
        if (pos >= end || buf[pos] != '"') {
            throw new SerializationException("Expected '\"', found: " + describePos(buf, pos, end));
        }
        pos++; // skip opening quote
        int start = pos;

        // Stop on the exact lane containing a quote, backslash, control byte, or non-ASCII byte.
        while (pos + Long.BYTES <= end) {
            long stops = stringStopMask(readWord(buf, pos));
            if (stops == 0) {
                pos += Long.BYTES;
                continue;
            }
            int stop = pos + stopOffset(stops);
            byte b = buf[stop];
            if (b == '"') {
                deser.parsedString = deser.decodeAsciiCached(buf, start, stop - start);
                deser.parsedEndPos = stop + 1;
                return;
            }
            if (b == '\\') {
                parseStringWithEscapes(buf, start, stop, end, deser);
                return;
            }
            if (b >= 0) {
                throw unescapedControlCharacter(b);
            }
            parseStringNonAscii(buf, start, stop + 1, end, deser);
            return;
        }

        parseStringTail(buf, start, pos, end, true, deser);
    }

    /**
     * Returns the high bit of each lane of a little-endian word that holds a quote, a backslash, a control byte or a
     * non-ASCII byte. Cross-lane borrows can only affect lanes after the earliest stop.
     */
    static long stringStopMask(long word) {
        long quoteOrControl = (word ^ 0x0202020202020202L) - 0x2121212121212121L;
        long backslash = (word ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L;
        return (quoteOrControl | backslash | word) & ASCII_HIGH_BITS;
    }

    /**
     * Skips JSON whitespace (space, tab, LF, CR) and returns the new position. The check at the top covers the common
     * case (the next byte is not whitespace).
     */
    static int skipWhitespace(byte[] buf, int pos, int end) {
        if (pos < end && buf[pos] > ' ') {
            return pos;
        }
        while (pos < end) {
            byte b = buf[pos];
            if (b != ' ' && b != '\n' && b != '\r' && b != '\t') {
                return pos;
            }
            pos++;
        }
        return pos;
    }

    /**
     * Parses an ISO-8601 timestamp directly from a JSON quoted string. Expects pos at the opening quote; returns null
     * when the content is not in the fast-path form.
     */
    static Instant parseIso8601(byte[] buf, int pos, int end, SmithyJsonDeserializer deser) {
        if (pos >= end || buf[pos] != '"' || pos + 22 > end) {
            return null;
        }
        int contentStart = pos + 1;
        int closeQuote = contentStart;
        while (closeQuote < end && buf[closeQuote] != '"') {
            closeQuote++;
        }
        if (closeQuote >= end) {
            return null;
        }
        Instant result = TimestampCodec.parseIso8601(buf, contentStart, closeQuote);
        if (result == null) {
            return null;
        }
        deser.parsedEndPos = closeQuote + 1;
        return result;
    }

    /**
     * Parses an HTTP-date directly from a JSON quoted string. Expects pos at the opening quote; returns null when the
     * content is not in the fast-path form.
     */
    static Instant parseHttpDate(byte[] buf, int pos, int end, SmithyJsonDeserializer deser) {
        if (pos >= end || buf[pos] != '"' || pos + 31 > end) {
            return null;
        }
        int contentStart = pos + 1;
        int closeQuote = contentStart;
        while (closeQuote < end && buf[closeQuote] != '"') {
            closeQuote++;
        }
        if (closeQuote >= end) {
            return null;
        }
        Instant result = TimestampCodec.parseHttpDate(buf, contentStart, closeQuote);
        if (result == null) {
            return null;
        }
        deser.parsedEndPos = closeQuote + 1;
        return result;
    }

    /**
     * Decodes a base64-encoded JSON string without building a String. Expects {@code pos} at the opening quote and
     * stores the position after the closing quote in {@code deser.parsedEndPos}.
     *
     * @return the decoded bytes
     * @throws SerializationException on an unterminated string or invalid base64
     */
    static ByteBuffer decodeBase64String(byte[] buf, int pos, int end, SmithyJsonDeserializer deser) {
        if (pos >= end || buf[pos] != '"') {
            throw new SerializationException("Expected '\"', found: " + describePos(buf, pos, end));
        }
        pos++; // skip opening quote

        // Find the closing quote 8 bytes at a time; base64 never includes '"'.
        int contentStart = pos;
        while (pos + 8 <= end) {
            long word = readWord(buf, pos);
            long xorQuote = word ^ 0x2222222222222222L;
            if (((xorQuote - 0x0101010101010101L) & ~xorQuote & 0x8080808080808080L) != 0) {
                break;
            }
            pos += 8;
        }
        while (pos < end && buf[pos] != '"') {
            pos++;
        }
        if (pos >= end) {
            throw new SerializationException("Unterminated base64 string");
        }
        int contentEnd = pos;
        deser.parsedEndPos = pos + 1; // after closing quote

        if (contentStart == contentEnd) {
            return ByteBuffer.allocate(0);
        }
        try {
            return BASE64_DECODER.decode(ByteBuffer.wrap(buf, contentStart, contentEnd - contentStart));
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid base64 in blob value", e);
        }
    }

    static String describeChar(byte b) {
        int unsigned = b & 0xFF;
        if (unsigned >= 0x20 && unsigned < 0x7F) {
            return "'" + (char) b + "'";
        }
        return "0x" + Integer.toHexString(b & 0xFF);
    }

    static String describePos(byte[] buf, int pos, int end) {
        if (pos >= end) {
            return "end of input";
        }
        return describeChar(buf[pos]);
    }

    private static boolean hasNineteenDigitInteger(byte[] buf, int pos, int end) {
        return pos + 19 <= end
               && buf[pos + 18] >= '0'
               && buf[pos + 18] <= '9'
               && buf[pos + 17] >= '0'
               && buf[pos + 17] <= '9'
               && buf[pos + 16] >= '0'
               && buf[pos + 16] <= '9'
               && NumberCodec.leadingDigitLaneCount(NumberCodec.digitLanes(buf, pos + 8)) == Long.BYTES;
    }

    private static void parseStringNonAscii(byte[] buf, int start, int pos, int end, SmithyJsonDeserializer deser) {
        while (pos + Long.BYTES <= end) {
            long stops = nonAsciiStringStopMask(readWord(buf, pos));
            if (stops == 0) {
                pos += Long.BYTES;
                continue;
            }
            int stop = pos + stopOffset(stops);
            byte b = buf[stop];
            if (b == '"') {
                deser.parsedString = deser.decodeUtf8Cached(buf, start, stop - start);
                deser.parsedEndPos = stop + 1;
                return;
            }
            if (b == '\\') {
                parseStringWithEscapes(buf, start, stop, end, deser);
                return;
            }
            throw unescapedControlCharacter(b);
        }
        parseStringTail(buf, start, pos, end, false, deser);
    }

    private static void parseStringTail(byte[] buf, int start, int pos, int end, boolean ascii,
                                        SmithyJsonDeserializer deser) {
        while (pos < end) {
            byte b = buf[pos];
            if (b == '"') {
                deser.parsedString = ascii
                                     ? deser.decodeAsciiCached(buf, start, pos - start)
                                     : deser.decodeUtf8Cached(buf, start, pos - start);
                deser.parsedEndPos = pos + 1;
                return;
            }
            if (b == '\\') {
                parseStringWithEscapes(buf, start, pos, end, deser);
                return;
            }
            if ((b & 0xFF) < 0x20) {
                throw unescapedControlCharacter(b);
            }
            ascii &= b >= 0;
            pos++;
        }
        throw new SerializationException("Unterminated string");
    }

    private static long nonAsciiStringStopMask(long word) {
        long ascii = ~word;
        long quoteOrControl = ((word ^ 0x0202020202020202L) - 0x2121212121212121L) & ascii;
        long backslash = ((word ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L) & ascii;
        return (quoteOrControl | backslash) & ASCII_HIGH_BITS;
    }

    private static int stopOffset(long stopMask) {
        return Long.numberOfTrailingZeros(stopMask) >>> 3;
    }

    private static SerializationException unescapedControlCharacter(byte b) {
        return new SerializationException("Unescaped control character 0x" + Integer.toHexString(b & 0xFF)
                                          + " in string");
    }

    private static void parseStringWithEscapes(byte[] buf, int start, int escapePos, int end,
                                               SmithyJsonDeserializer deser) {
        StringBuilder sb = new StringBuilder(escapePos - start + 16);
        sb.append(new String(buf, start, escapePos - start, StandardCharsets.UTF_8));
        int pos = escapePos;
        while (pos < end) {
            byte b = buf[pos];
            if (b == '"') {
                deser.parsedString = sb.toString();
                deser.parsedEndPos = pos + 1;
                return;
            }
            if ((b & 0xFF) < 0x20) {
                throw unescapedControlCharacter(b);
            }
            if (b == '\\') {
                pos++;
                if (pos >= end) {
                    throw new SerializationException("Unterminated escape sequence");
                }
                byte escaped = buf[pos++];
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        pos = appendUnicodeEscape(buf, pos, end, sb);
                        break;
                    default:
                        throw new SerializationException("Invalid escape character: \\" + (char) escaped);
                }
            } else if ((b & 0x80) == 0) {
                sb.append((char) b);
                pos++;
            } else {
                pos = appendUtf8Char(buf, pos, end, sb);
            }
        }
        throw new SerializationException("Unterminated string");
    }

    /**
     * Appends the character of a {@code \}{@code uXXXX} escape (and its low surrogate escape, for a high surrogate)
     * whose hex digits start at {@code pos}, and returns the position after it.
     */
    private static int appendUnicodeEscape(byte[] buf, int pos, int end, StringBuilder sb) {
        if (pos + 4 > end) {
            throw new SerializationException("Incomplete \\uXXXX escape");
        }
        char c = parseHex4(buf, pos);
        pos += 4;
        if (Character.isHighSurrogate(c)) {
            if (pos + 6 > end || buf[pos] != '\\' || buf[pos + 1] != 'u') {
                throw new SerializationException("Expected low surrogate after high surrogate");
            }
            pos += 2; // skip backslash-u
            char low = parseHex4(buf, pos);
            pos += 4;
            if (!Character.isLowSurrogate(low)) {
                throw new SerializationException("Expected low surrogate, got \\u" + Integer.toHexString(low));
            }
            sb.append(c);
            sb.append(low);
        } else if (Character.isLowSurrogate(c)) {
            throw new SerializationException("Unexpected low surrogate without preceding high surrogate");
        } else {
            sb.append(c);
        }
        return pos;
    }

    private static char parseHex4(byte[] buf, int pos) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            byte b = buf[pos + i];
            if (b < 0 || HEX_VALUES[b] == -1) {
                throw new SerializationException("Invalid hex digit in \\u escape: " + (char) (b & 0xFF));
            }
            value = (value << 4) | HEX_VALUES[b];
        }
        return (char) value;
    }

    /**
     * Decodes one UTF-8 character starting at pos, appends it, and returns the position after it. Validates
     * continuation bytes (10xxxxxx per RFC 3629) and rejects overlong forms and surrogate code points.
     *
     * <p>smithy-java returns {@code int[] {codePoint, newPos}}; the port appends directly and returns the position,
     * which avoids the array allocation.
     */
    private static int appendUtf8Char(byte[] buf, int pos, int end, StringBuilder sb) {
        byte b = buf[pos];
        int cp;
        int length;
        if ((b & 0xE0) == 0xC0) {
            if (pos + 2 > end) {
                throw new SerializationException("Truncated UTF-8 sequence");
            }
            validateContinuationByte(buf[pos + 1]);
            cp = ((b & 0x1F) << 6) | (buf[pos + 1] & 0x3F);
            if (cp < 0x80) {
                throw new SerializationException("Overlong UTF-8 sequence");
            }
            length = 2;
        } else if ((b & 0xF0) == 0xE0) {
            if (pos + 3 > end) {
                throw new SerializationException("Truncated UTF-8 sequence");
            }
            validateContinuationByte(buf[pos + 1]);
            validateContinuationByte(buf[pos + 2]);
            cp = ((b & 0x0F) << 12) | ((buf[pos + 1] & 0x3F) << 6) | (buf[pos + 2] & 0x3F);
            if (cp < 0x800) {
                throw new SerializationException("Overlong UTF-8 sequence");
            }
            if (cp >= 0xD800 && cp <= 0xDFFF) {
                throw new SerializationException("UTF-8 encoded surrogate code point: U+" + Integer.toHexString(cp));
            }
            length = 3;
        } else if ((b & 0xF8) == 0xF0) {
            if (pos + 4 > end) {
                throw new SerializationException("Truncated UTF-8 sequence");
            }
            validateContinuationByte(buf[pos + 1]);
            validateContinuationByte(buf[pos + 2]);
            validateContinuationByte(buf[pos + 3]);
            cp = ((b & 0x07) << 18)
                 | ((buf[pos + 1] & 0x3F) << 12)
                 | ((buf[pos + 2] & 0x3F) << 6)
                 | (buf[pos + 3] & 0x3F);
            if (cp < 0x10000 || cp > 0x10FFFF) {
                throw new SerializationException("Invalid UTF-8 code point: " + cp);
            }
            length = 4;
        } else {
            throw new SerializationException("Invalid UTF-8 start byte: 0x" + Integer.toHexString(b & 0xFF));
        }
        sb.appendCodePoint(cp);
        return pos + length;
    }

    private static void validateContinuationByte(byte b) {
        if ((b & 0xC0) != 0x80) {
            throw new SerializationException("Invalid UTF-8 continuation byte: 0x" + Integer.toHexString(b & 0xFF));
        }
    }
}
