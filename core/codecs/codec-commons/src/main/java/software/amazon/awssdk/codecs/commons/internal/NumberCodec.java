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

package software.amazon.awssdk.codecs.commons.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Reads and writes numeric primitives directly from and to byte arrays, for the byte-level text codecs.
 *
 * <p>The write methods return the position after the last byte written; the caller must make room first. Finite
 * floats and doubles are written in the JDK's shortest decimal spelling ({@code Double.toString} on JDK 19+), except
 * that values holding an integer in {@code int} or {@code long} range are written as that integer.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.codecs.commons.NumberCodec}. The {@code VarHandle} array
 * views become {@link ByteArrays} reads and writes, and the general float parser is the JDK's instead of
 * FastDoubleParser.
 */
@SdkInternalApi
public final class NumberCodec {

    /** Maximum digit count for the decimal fast path. */
    public static final int MAX_FAST_DECIMAL_DIGITS = DecimalToBinary.MAX_DIGITS;

    /**
     * Required capacity from the starting position for float formatting, including packed stores beyond the returned
     * end. Also covers non-finite values, with or without quotes.
     */
    public static final int FLOAT_MAX_BYTES = 15;

    /**
     * Required capacity from the starting position for double formatting, including packed stores beyond the returned
     * end. Also covers non-finite values, with or without quotes.
     */
    public static final int DOUBLE_MAX_BYTES = 24;

    private static final byte[] INT_MAX_DIGITS = "2147483647".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INT_MIN_DIGITS = "2147483648".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LONG_MIN_DIGITS = "9223372036854775808".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] TRUE = {'t', 'r', 'u', 'e'};
    private static final byte[] FALSE = {'f', 'a', 'l', 's', 'e'};

    private static final byte[] NAN = {'N', 'a', 'N'};
    private static final byte[] INF = {'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
    private static final byte[] NEG_INF = {'-', 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
    private static final byte[] QUOTED_NAN = {'"', 'N', 'a', 'N', '"'};
    private static final byte[] QUOTED_INF = {'"', 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y', '"'};
    private static final byte[] QUOTED_NEG_INF = {'"', '-', 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y', '"'};

    private static final long[] POWERS_OF_10 = {
        1L,
        10L,
        100L,
        1000L,
        10000L,
        100000L,
        1000000L,
        10000000L,
        100000000L,
        1000000000L,
        10000000000L,
        100000000000L,
        1000000000000L,
        10000000000000L,
        100000000000000L,
        1000000000000000L,
        10000000000000000L,
        100000000000000000L,
        1000000000000000000L
    };

    private static final byte[] DIGIT_PAIRS = new byte[200];

    private static final BigInteger TEN_TO_18 = BigInteger.valueOf(1_000_000_000_000_000_000L);

    private static final int DIGITS_10_8 = 100_000_000;

    static {
        for (int i = 0; i < 100; i++) {
            DIGIT_PAIRS[i * 2] = (byte) ('0' + i / 10);
            DIGIT_PAIRS[i * 2 + 1] = (byte) ('0' + i % 10);
        }
    }

    private NumberCodec() {
    }

    /**
     * Parses an int from a byte array span. Handles an optional leading sign and leading zeros.
     *
     * @throws NumberFormatException if the span is not an integer in {@code int} range
     */
    public static int parseInt(byte[] buf, int start, int len) {
        if (len == 0) {
            throw new NumberFormatException("Empty input");
        }

        boolean negative = false;
        int i = start;
        int end = start + len;

        if (buf[i] == '-') {
            negative = true;
            i++;
            if (i == end) {
                throw new NumberFormatException("Just a minus sign");
            }
        } else if (buf[i] == '+') {
            i++;
            if (i == end) {
                throw new NumberFormatException("Just a plus sign");
            }
        }

        while (i < end - 1 && buf[i] == '0') {
            i++;
        }

        int digitLen = end - i;
        if (digitLen > 10) {
            throw new NumberFormatException("Integer overflow");
        }

        if (digitLen == 10) {
            byte[] limit = negative ? INT_MIN_DIGITS : INT_MAX_DIGITS;
            for (int j = 0; j < 10; j++) {
                int cmp = (buf[i + j] & 0xFF) - (limit[j] & 0xFF);
                if (cmp < 0) {
                    break;
                }
                if (cmp > 0) {
                    throw new NumberFormatException("Integer overflow");
                }
            }
        }

        int result = 0;
        while (i + 4 <= end) {
            int v = parse4Digits(buf, i);
            if (v < 0) {
                break;
            }
            result = result * 10000 - v;
            i += 4;
        }

        while (i < end) {
            int d = buf[i++] - '0';
            if (d < 0 || d > 9) {
                throw new NumberFormatException("Invalid digit at position " + (i - 1 - start));
            }
            result = result * 10 - d;
        }

        return negative ? result : -result;
    }

    /**
     * Parses a long from a byte array span. Handles an optional leading sign and leading zeros.
     *
     * @throws NumberFormatException if the span is not an integer in {@code long} range
     */
    public static long parseLong(byte[] buf, int start, int len) {
        if (len == 0) {
            throw new NumberFormatException("Empty input");
        }

        boolean negative = false;
        int i = start;
        int end = start + len;

        if (buf[i] == '-') {
            negative = true;
            i++;
            if (i == end) {
                throw new NumberFormatException("Just a minus sign");
            }
        } else if (buf[i] == '+') {
            i++;
            if (i == end) {
                throw new NumberFormatException("Just a plus sign");
            }
        }

        while (i < end - 1 && buf[i] == '0') {
            i++;
        }

        int digitLen = end - i;
        if (digitLen > 19) {
            throw new NumberFormatException("Long overflow");
        }

        long result = 0;
        while (i + 8 <= end) {
            long v = parse8Digits(buf, i);
            if (v < 0) {
                break;
            }
            result = result * 100000000L - v;
            i += 8;
        }

        while (i + 4 <= end) {
            int v = parse4Digits(buf, i);
            if (v < 0) {
                break;
            }
            result = result * 10000 - v;
            i += 4;
        }

        while (i < end) {
            int d = buf[i++] - '0';
            if (d < 0 || d > 9) {
                throw new NumberFormatException("Invalid digit at position " + (i - 1 - start));
            }
            long next = result * 10 - d;
            if (next > result) {
                throw new NumberFormatException("Long overflow");
            }
            result = next;
        }

        if (!negative) {
            result = -result;
            if (result < 0) {
                throw new NumberFormatException("Long overflow");
            }
        }
        return result;
    }

    /**
     * Returns eight big-endian byte lanes XORed with '0', without inter-lane borrowing.
     */
    public static long digitLanes(byte[] buf, int i) {
        return ByteArrays.getLongBigEndian(buf, i) ^ 0x3030303030303030L;
    }

    /**
     * Counts the leading digit lanes (0..8).
     */
    public static int leadingDigitLaneCount(long lanes) {
        long bad = (lanes & 0xF0F0F0F0F0F0F0F0L)
                   | (((lanes & 0x0F0F0F0F0F0F0F0FL) + 0x0606060606060606L) & 0x1010101010101010L);
        return bad == 0 ? 8 : Long.numberOfLeadingZeros(bad) >>> 3;
    }

    /**
     * Combines up to eight big-endian lanes containing digits 0..9; unused leading lanes must be zero.
     */
    public static long combineDigitLanes(long sub) {
        long lo = sub & 0x000F000F000F000FL;
        long hi = (sub >>> 8) & 0x000F000F000F000FL;
        long pairs = hi * 10 + lo;
        long lo2 = pairs & 0x0000007F0000007FL;
        long hi2 = (pairs >>> 16) & 0x0000007F0000007FL;
        long quads = hi2 * 100 + lo2;
        int upper = (int) (quads >>> 32);
        int lower = (int) quads;
        return (long) upper * 10000 + lower;
    }

    /**
     * Parses a double from a byte span in Java floating-point syntax, with {@link Double#parseDouble(String)}.
     *
     * @throws NumberFormatException if the span is not a number
     */
    public static double parseDouble(byte[] buf, int offset, int length) {
        return Double.parseDouble(new String(buf, offset, length, StandardCharsets.ISO_8859_1));
    }

    /**
     * Parses a float from a byte span in Java floating-point syntax, with {@link Float#parseFloat(String)}.
     *
     * @throws NumberFormatException if the span is not a number
     */
    public static float parseFloat(byte[] buf, int offset, int length) {
        return Float.parseFloat(new String(buf, offset, length, StandardCharsets.ISO_8859_1));
    }

    /**
     * Converts a validated nonnegative significand and scale; NaN requests a general parse.
     */
    public static double decimalToDouble(long unscaled, int scale) {
        return DecimalToBinary.toDouble(unscaled, scale);
    }

    /**
     * Returns 10^exponent for an exponent from 0 through 18.
     */
    public static long powerOfTen(int exponent) {
        return POWERS_OF_10[exponent];
    }

    /**
     * Returns the high 64 bits of the unsigned 128-bit product of {@code x} and {@code y}. Java 8 replacement for
     * {@code Math.unsignedMultiplyHigh} (Java 18).
     */
    public static long unsignedMultiplyHigh(long x, long y) {
        long x0 = x & 0xFFFFFFFFL;
        long x1 = x >>> 32;
        long y0 = y & 0xFFFFFFFFL;
        long y1 = y >>> 32;
        long p01 = x0 * y1;
        // (x1 * y0) plus the carry of (x0 * y0) plus the low half of (x0 * y1) fits in 64 unsigned bits.
        long middle = x1 * y0 + ((x0 * y0) >>> 32) + (p01 & 0xFFFFFFFFL);
        return x1 * y1 + (middle >>> 32) + (p01 >>> 32);
    }

    /**
     * Writes {@code true} or {@code false}.
     */
    public static int writeBoolean(byte[] buf, int pos, boolean value) {
        byte[] bytes = value ? TRUE : FALSE;
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        return pos + bytes.length;
    }

    /**
     * Writes the decimal digits of an int (at most 11 bytes).
     */
    public static int writeInt(byte[] buf, int pos, int value) {
        if (value == 0) {
            buf[pos] = '0';
            return pos + 1;
        }
        if (value < 0) {
            buf[pos++] = '-';
            if (value == Integer.MIN_VALUE) {
                return writePositiveLong(buf, pos, 2147483648L);
            }
            value = -value;
        }
        return writePositiveInt(buf, pos, value);
    }

    /**
     * Writes the decimal digits of a long (at most 20 bytes).
     */
    public static int writeLong(byte[] buf, int pos, long value) {
        if (value == 0) {
            buf[pos] = '0';
            return pos + 1;
        }
        if (value < 0) {
            buf[pos++] = '-';
            if (value == Long.MIN_VALUE) {
                System.arraycopy(LONG_MIN_DIGITS, 0, buf, pos, LONG_MIN_DIGITS.length);
                return pos + LONG_MIN_DIGITS.length;
            }
            value = -value;
        }
        return writePositiveLong(buf, pos, value);
    }

    /**
     * Writes the decimal digits of a big integer (at most {@code bitLength() / 3 + 2} bytes).
     */
    public static int writeBigInteger(byte[] buf, int pos, BigInteger value) {
        if (value.signum() < 0) {
            buf[pos++] = '-';
            value = value.negate();
        }
        if (value.compareTo(TEN_TO_18) < 0) {
            return writePositiveLong(buf, pos, value.longValue());
        }
        BigInteger[] qr = value.divideAndRemainder(TEN_TO_18);
        BigInteger high = qr[0];
        long low = qr[1].longValue();
        if (high.compareTo(TEN_TO_18) < 0) {
            pos = writePositiveLong(buf, pos, high.longValue());
            return writePaddedLong18(buf, pos, low);
        }
        BigInteger[] qr2 = high.divideAndRemainder(TEN_TO_18);
        long mid = qr2[1].longValue();
        if (qr2[0].compareTo(TEN_TO_18) < 0) {
            pos = writePositiveLong(buf, pos, qr2[0].longValue());
            pos = writePaddedLong18(buf, pos, mid);
            return writePaddedLong18(buf, pos, low);
        }
        String s = value.toString();
        return writeAsciiString(buf, pos, s);
    }

    /**
     * Writes a finite double and returns the end position. Reserve {@link #DOUBLE_MAX_BYTES} bytes from {@code pos};
     * packed stores may modify bytes beyond the returned end within that capacity.
     */
    public static int writeDouble(byte[] buf, int pos, double value) {
        long longValue = (long) value;
        // Same test as smithy-java's value == (double) longValue (true for -0.0 too), on the raw bits for SpotBugs.
        long bits = Double.doubleToRawLongBits(value);
        if ((bits << 1) == 0 || bits == Double.doubleToRawLongBits((double) longValue)) {
            return writeLong(buf, pos, longValue);
        }
        return Zmij.writeDouble(buf, pos, value);
    }

    /**
     * Writes a finite float and returns the end position. Reserve {@link #FLOAT_MAX_BYTES} bytes from {@code pos};
     * packed stores may modify bytes beyond the returned end within that capacity.
     */
    public static int writeFloat(byte[] buf, int pos, float value) {
        int intValue = (int) value;
        // Same test as smithy-java's value == (float) intValue (true for -0.0f too), on the raw bits for SpotBugs.
        int bits = Float.floatToRawIntBits(value);
        if ((bits << 1) == 0 || bits == Float.floatToRawIntBits((float) intValue)) {
            return writeInt(buf, pos, intValue);
        }
        return Zmij.writeFloat(buf, pos, value);
    }

    /**
     * Writes {@code NaN}, {@code Infinity} or {@code -Infinity}.
     */
    public static int writeNonFiniteFloat(byte[] buf, int pos, float value) {
        byte[] bytes;
        if (Float.isNaN(value)) {
            bytes = NAN;
        } else {
            bytes = value > 0 ? INF : NEG_INF;
        }
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        return pos + bytes.length;
    }

    /**
     * Writes {@code NaN}, {@code Infinity} or {@code -Infinity}.
     */
    public static int writeNonFiniteDouble(byte[] buf, int pos, double value) {
        byte[] bytes;
        if (Double.isNaN(value)) {
            bytes = NAN;
        } else {
            bytes = value > 0 ? INF : NEG_INF;
        }
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        return pos + bytes.length;
    }

    /**
     * Writes {@code "NaN"}, {@code "Infinity"} or {@code "-Infinity"}, with quotes.
     */
    public static int writeNonFiniteFloatQuoted(byte[] buf, int pos, float value) {
        byte[] bytes;
        if (Float.isNaN(value)) {
            bytes = QUOTED_NAN;
        } else {
            bytes = value > 0 ? QUOTED_INF : QUOTED_NEG_INF;
        }
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        return pos + bytes.length;
    }

    /**
     * Writes {@code "NaN"}, {@code "Infinity"} or {@code "-Infinity"}, with quotes.
     */
    public static int writeNonFiniteDoubleQuoted(byte[] buf, int pos, double value) {
        byte[] bytes;
        if (Double.isNaN(value)) {
            bytes = QUOTED_NAN;
        } else {
            bytes = value > 0 ? QUOTED_INF : QUOTED_NEG_INF;
        }
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        return pos + bytes.length;
    }

    /**
     * Writes a float, including non-finite values, and returns the end position. Reserve {@link #FLOAT_MAX_BYTES}
     * bytes from {@code pos}; packed stores may modify bytes beyond the returned end within that capacity.
     */
    public static int writeFloatFull(byte[] buf, int pos, float value) {
        if (Float.isFinite(value)) {
            return writeFloat(buf, pos, value);
        }
        return writeNonFiniteFloat(buf, pos, value);
    }

    /**
     * Writes a double, including non-finite values, and returns the end position. Reserve {@link #DOUBLE_MAX_BYTES}
     * bytes from {@code pos}; packed stores may modify bytes beyond the returned end within that capacity.
     */
    public static int writeDoubleFull(byte[] buf, int pos, double value) {
        if (Double.isFinite(value)) {
            return writeDouble(buf, pos, value);
        }
        return writeNonFiniteDouble(buf, pos, value);
    }

    /**
     * Writes a float, quoting non-finite values, and returns the end position. Reserve {@link #FLOAT_MAX_BYTES} bytes
     * from {@code pos}; packed stores may modify bytes beyond the returned end within that capacity.
     */
    public static int writeFloatFullQuoted(byte[] buf, int pos, float value) {
        if (Float.isFinite(value)) {
            return writeFloat(buf, pos, value);
        }
        return writeNonFiniteFloatQuoted(buf, pos, value);
    }

    /**
     * Writes a double, quoting non-finite values, and returns the end position. Reserve {@link #DOUBLE_MAX_BYTES}
     * bytes from {@code pos}; packed stores may modify bytes beyond the returned end within that capacity.
     */
    public static int writeDoubleFullQuoted(byte[] buf, int pos, double value) {
        if (Double.isFinite(value)) {
            return writeDouble(buf, pos, value);
        }
        return writeNonFiniteDoubleQuoted(buf, pos, value);
    }

    /**
     * Writes a big decimal in plain notation (no exponent). Reserve {@link #maxBigDecimalLength(BigDecimal)} bytes.
     */
    public static int writeBigDecimal(byte[] buf, int pos, BigDecimal value) {
        // TODO: Bound plain-notation expansion before calling BigDecimal.toPlainString (as in smithy-java).
        int scale = value.scale();
        if (value.unscaledValue().bitLength() < 64) {
            if (scale == 0) {
                return writeLong(buf, pos, value.longValueExact());
            }
            if (scale > 0 && scale < POWERS_OF_10.length) {
                long unscaled = value.unscaledValue().longValue();
                if (unscaled < 0) {
                    buf[pos++] = '-';
                    unscaled = -unscaled;
                }
                long divisor = POWERS_OF_10[scale];
                long intPart = unscaled / divisor;
                long fracPart = unscaled - intPart * divisor;
                pos = writeLong(buf, pos, intPart);
                buf[pos++] = '.';
                for (int i = scale - 1; i >= 0; i--) {
                    long p10 = POWERS_OF_10[i];
                    int d = (int) (fracPart / p10);
                    buf[pos++] = (byte) ('0' + d);
                    fracPart -= d * p10;
                }
                return pos;
            }
        }
        return writeAsciiString(buf, pos, value.toPlainString());
    }

    /**
     * Returns an upper bound of the bytes {@link #writeBigDecimal} writes for {@code value}.
     */
    public static int maxBigDecimalLength(BigDecimal value) {
        int scale = value.scale();
        int bitLength = value.unscaledValue().bitLength();
        if (bitLength < 64 && scale >= 0 && scale < POWERS_OF_10.length) {
            return 1 + 20 + 1 + scale;
        }
        int unscaledDigits = (int) (bitLength * 0.302) + 2;
        if (scale <= 0) {
            return 1 + unscaledDigits + (-scale);
        }
        return 1 + unscaledDigits + 1 + scale;
    }

    /**
     * Returns the number of decimal digits of a nonnegative int.
     */
    public static int digitCount(int value) {
        if (value < 10) {
            return 1;
        }
        if (value < 100) {
            return 2;
        }
        if (value < 1000) {
            return 3;
        }
        if (value < 10000) {
            return 4;
        }
        if (value < 100000) {
            return 5;
        }
        if (value < 1000000) {
            return 6;
        }
        if (value < 10000000) {
            return 7;
        }
        if (value < 100000000) {
            return 8;
        }
        if (value < 1000000000) {
            return 9;
        }
        return 10;
    }

    /**
     * Parses 4 ASCII digit bytes at buf[i..i+4) into a value 0..9999. Returns -1 if any byte is not a digit.
     */
    private static int parse4Digits(byte[] buf, int i) {
        int word = ByteArrays.getIntBigEndian(buf, i);
        int sub = word - 0x30303030;
        // (sub + 0x76767676) & 0x80808080 catches bytes > 9; sub & 0x80808080 catches bytes < 0 (byte < '0').
        if ((((sub + 0x76767676) | sub) & 0x80808080) != 0) {
            return -1;
        }
        int d0 = (sub >>> 24) & 0xFF;
        int d1 = (sub >>> 16) & 0xFF;
        int d2 = (sub >>> 8) & 0xFF;
        int d3 = sub & 0xFF;
        return d0 * 1000 + d1 * 100 + d2 * 10 + d3;
    }

    /**
     * Parses 8 ASCII digit bytes at buf[i..i+8) into a value 0..99999999. Returns -1 if any byte is not a digit.
     */
    private static long parse8Digits(byte[] buf, int i) {
        long word = ByteArrays.getLongBigEndian(buf, i);
        long sub = word - 0x3030303030303030L;
        if ((((sub + 0x7676767676767676L) | sub) & 0x8080808080808080L) != 0) {
            return -1;
        }
        return combineDigitLanes(sub);
    }

    @SuppressWarnings("deprecation")
    private static int writeAsciiString(byte[] buf, int pos, String s) {
        int len = s.length();
        s.getBytes(0, len, buf, pos);
        return pos + len;
    }

    private static int writePositiveInt(byte[] buf, int pos, int value) {
        int digits = digitCount(value);
        int end = pos + digits;
        int p = end;

        while (value >= 10000 && p - 4 >= pos) {
            int q = value / 10000;
            int r = value - q * 10000;
            value = q;
            p -= 4;
            write4Digits(buf, p, r);
        }

        while (value >= 100) {
            int q = value / 100;
            int r = (value - q * 100) * 2;
            value = q;
            buf[--p] = DIGIT_PAIRS[r + 1];
            buf[--p] = DIGIT_PAIRS[r];
        }

        if (value >= 10) {
            int r = value * 2;
            buf[--p] = DIGIT_PAIRS[r + 1];
            buf[--p] = DIGIT_PAIRS[r];
        } else {
            buf[--p] = (byte) ('0' + value);
        }

        return end;
    }

    private static int writePositiveLong(byte[] buf, int pos, long value) {
        if (value <= Integer.MAX_VALUE) {
            return writePositiveInt(buf, pos, (int) value);
        }

        // Split into 32-bit chunks to avoid repeated 64-bit division
        if (value < (long) DIGITS_10_8 * DIGITS_10_8) {
            int lo = (int) (value % DIGITS_10_8);
            int hi = (int) (value / DIGITS_10_8);
            pos = writePositiveInt(buf, pos, hi);
            return writePaddedInt8(buf, pos, lo);
        }
        long tmp = value / DIGITS_10_8;
        int lo = (int) (value - tmp * DIGITS_10_8);
        int mid = (int) (tmp % DIGITS_10_8);
        int hi = (int) (tmp / DIGITS_10_8);
        pos = writePositiveInt(buf, pos, hi);
        pos = writePaddedInt8(buf, pos, mid);
        return writePaddedInt8(buf, pos, lo);
    }

    /**
     * Writes exactly 8 digits (zero-padded) using 32-bit arithmetic only.
     */
    private static int writePaddedInt8(byte[] buf, int pos, int value) {
        int hi4 = value / 10000;
        int lo4 = value - hi4 * 10000;
        write4Digits(buf, pos, hi4);
        write4Digits(buf, pos + 4, lo4);
        return pos + 8;
    }

    /**
     * Writes 4 decimal digits (0000-9999) as a single big-endian int store.
     */
    private static void write4Digits(byte[] buf, int pos, int value) {
        int q = (value * 5243) >>> 19; // value / 100
        int r = value - q * 100;
        int d01 = DIGIT_PAIRS[q * 2] << 24 | (DIGIT_PAIRS[q * 2 + 1] & 0xFF) << 16;
        int d23 = (DIGIT_PAIRS[r * 2] & 0xFF) << 8 | (DIGIT_PAIRS[r * 2 + 1] & 0xFF);
        ByteArrays.putIntBigEndian(buf, pos, d01 | d23);
    }

    private static int writePaddedLong18(byte[] buf, int pos, long value) {
        int end = pos + 18;
        int p = end;
        for (int i = 0; i < 9; i++) {
            int r = (int) (value % 100) * 2;
            value /= 100;
            buf[--p] = DIGIT_PAIRS[r + 1];
            buf[--p] = DIGIT_PAIRS[r];
        }
        return end;
    }
}
