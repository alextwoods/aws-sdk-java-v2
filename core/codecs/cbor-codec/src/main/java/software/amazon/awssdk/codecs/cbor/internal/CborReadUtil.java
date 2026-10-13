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

import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.EIGHT_BYTES;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_MASK;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_NEGINT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_POSINT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_SHIFT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_TAG;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MINOR_TYPE_MASK;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.ONE_BYTE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_NEG_BIG_INT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_POS_BIG_INT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.ZERO_BYTES;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Reads CBOR integers, big numbers, decimal fractions and (possibly chunked) strings from a byte array.
 *
 * <p>Port of smithy-java {@code CborReadUtil}. The {@code VarHandle} big-endian views are replaced by explicit
 * shift-and-mask reads ({@link #readShortBigEndian}, {@link #readIntBigEndian}, {@link #readLongBigEndian}), because
 * {@code VarHandle} is Java 9.
 */
@SdkInternalApi
final class CborReadUtil {

    private CborReadUtil() {
    }

    static int argLength(int minorType) {
        if (minorType <= ZERO_BYTES) {
            return 0;
        }
        if (minorType > EIGHT_BYTES) {
            throw new BadCborException("illegal arg length type: " + minorType);
        }
        int shift = minorType - ONE_BYTE;
        return 1 << shift;
    }

    static int readPosInt(byte[] buffer, int off, int len) {
        if (len == 0) {
            return buffer[off] & MINOR_TYPE_MASK;
        }
        long val = readLong0(buffer, off, len);
        if (val > Integer.MAX_VALUE) {
            throw new BadCborException("value cannot fit into an int");
        }
        if (val < 0) {
            throw new BadCborException("expected positive int");
        }
        return (int) val;
    }

    static int readStrLen(byte[] buffer, int off, int minor, int argLen) {
        if (argLen == 0) {
            return minor;
        }
        return readPosInt(buffer, off + 1, argLen);
    }

    /**
     * Flips all the bits of the given array with the bitwise negation operator ({@code ~}).
     */
    static void flipBytes(byte[] src) {
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) ~src[i];
        }
    }

    static long readLong(byte[] buffer, byte type, int off, int len) {
        long val = len == 0 ? buffer[off] & MINOR_TYPE_MASK : readLong0(buffer, off, len);
        if (type == MAJOR_TYPE_NEGINT) {
            return -val - 1;
        }
        return val;
    }

    static int readShortBigEndian(byte[] buffer, int off) {
        return ((buffer[off] & 0xFF) << 8) | (buffer[off + 1] & 0xFF);
    }

    static int readIntBigEndian(byte[] buffer, int off) {
        return ((buffer[off] & 0xFF) << 24)
               | ((buffer[off + 1] & 0xFF) << 16)
               | ((buffer[off + 2] & 0xFF) << 8)
               | (buffer[off + 3] & 0xFF);
    }

    static long readLongBigEndian(byte[] buffer, int off) {
        return ((long) readIntBigEndian(buffer, off) << 32) | (readIntBigEndian(buffer, off + 4) & 0xFFFF_FFFFL);
    }

    private static long readLong0(byte[] buffer, int off, int len) {
        switch (len) {
            case 1:
                return buffer[off] & 0xFFL;
            case 2:
                return readShortBigEndian(buffer, off);
            case 4:
                return readIntBigEndian(buffer, off) & 0xFFFF_FFFFL;
            case 8:
                return readLongBigEndian(buffer, off);
            default:
                throw new BadCborException("invalid length: " + len, true);
        }
    }

    static BigInteger readBigInteger(byte[] buffer, byte context, int off, int len) {
        boolean isPosInt = context == CborDeserializer.Token.POS_INT;
        if (isPosInt || context == CborDeserializer.Token.NEG_INT) {
            return readSmallBigInteger(buffer, context, off, len, isPosInt);
        }
        byte[] buff;
        if (len >= 0) {
            checkInvalidLength(buffer, off, len);
            int desOff;
            if (len == 0 || buffer[off] >= 0) {
                buff = new byte[len];
                desOff = 0;
            } else {
                buff = new byte[len + 1];
                desOff = 1;
            }
            readByteString(buffer, off, buff, desOff, len);
        } else {
            // Indefinite-length byte string.
            byte[] indefBuf = readByteString(buffer, off, len);
            if (indefBuf.length == 0 || indefBuf[0] < 0) {
                buff = new byte[indefBuf.length + 1];
                System.arraycopy(indefBuf, 0, buff, 1, indefBuf.length);
            } else {
                buff = indefBuf;
            }
        }
        if (buff.length == 0) {
            // An empty bignum is zero (RFC 8949 3.4.3); a negative empty bignum is -1 - 0.
            return context == CborDeserializer.Token.NEG_BIGINT ? BigInteger.ONE.negate() : BigInteger.ZERO;
        }
        if (context == CborDeserializer.Token.NEG_BIGINT) {
            flipBytes(buff);
        }
        return new BigInteger(buff);
    }

    private static BigInteger readSmallBigInteger(byte[] buffer, byte context, int off, int len, boolean isPosInt) {
        // A 64-bit argument with its top bit set does not fit in a long.
        if (len == 8 && buffer[off] < 0) {
            byte[] val = new byte[9];
            readByteString(buffer, off, val, 1, len);
            if (!isPosInt) {
                flipBytes(val);
            }
            return new BigInteger(val);
        }
        return BigInteger.valueOf(readLong(buffer, context, off, len));
    }

    static BigDecimal readBigDecimal(byte[] buffer, int originalOff) {
        int cur = originalOff;
        int exponentLength = argLength(getMinor(buffer[cur]));
        long exponent = readLong(buffer, getMajor(buffer[cur]), exponentLength > 0 ? cur + 1 : cur, exponentLength);
        int intExponent = (int) exponent;
        if (intExponent != exponent || intExponent == Integer.MIN_VALUE) {
            throw new BadCborException("exponent cannot fit into an int");
        }
        cur += exponentLength + 1;
        byte major = getMajor(buffer[cur]);
        byte minor = getMinor(buffer[cur]);
        byte context;
        int mantissaLength;
        switch (major) {
            case MAJOR_TYPE_TAG:
                if (minor == TAG_POS_BIG_INT) {
                    context = CborDeserializer.Token.POS_BIGINT;
                } else if (minor == TAG_NEG_BIG_INT) {
                    context = CborDeserializer.Token.NEG_BIGINT;
                } else {
                    throw new BadCborException("Unexpected minor " + minor, true);
                }
                cur++;
                byte mantissaLengthMinor = getMinor(buffer[cur]);
                int argLen = argLength(mantissaLengthMinor);
                if (argLen == 0) {
                    mantissaLength = mantissaLengthMinor;
                    cur++;
                } else {
                    cur++;
                    mantissaLength = readPosInt(buffer, cur, argLen);
                    cur += argLen;
                }
                break;
            case MAJOR_TYPE_POSINT:
            case MAJOR_TYPE_NEGINT:
                mantissaLength = argLength(minor);
                if (mantissaLength > 0) {
                    cur++;
                }
                context = major;
                break;
            default:
                throw new BadCborException("Unexpected major " + major, true);
        }
        BigInteger unscaled = readBigInteger(buffer, context, cur, mantissaLength);
        return new BigDecimal(unscaled, -intExponent);
    }

    static String readTextString(byte[] buffer, int off, int len) {
        if (CborDeserializer.isIndefinite(len)) {
            return new String(readBytesIndefinite(buffer, off, CborDeserializer.itemLength(len)),
                              StandardCharsets.UTF_8);
        }
        return new String(buffer, off, len, StandardCharsets.UTF_8);
    }

    static byte[] readByteString(byte[] buffer, int off, int len) {
        if (CborDeserializer.isIndefinite(len)) {
            return readBytesIndefinite(buffer, off, CborDeserializer.itemLength(len));
        }
        return readBytesFinite(buffer, off, len);
    }

    static void readByteString(byte[] buffer, int off, byte[] dest, int destOff, int len) {
        if (CborDeserializer.isIndefinite(len)) {
            readBytesIndefinite(buffer, off, dest, destOff, CborDeserializer.itemLength(len));
        } else {
            readBytesFinite(buffer, off, dest, destOff, len);
        }
    }

    /**
     * Returns whether {@code a[aFrom, aTo)} equals {@code b[bFrom, bFrom + (aTo - aFrom))}. Replaces the Java 9
     * range overload of {@code Arrays.equals}.
     */
    static boolean regionEquals(byte[] a, int aFrom, int aTo, byte[] b, int bFrom) {
        int len = aTo - aFrom;
        if (len < 0 || aTo > a.length || bFrom + len > b.length) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (a[aFrom + i] != b[bFrom + i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] readBytesFinite(byte[] b, int off, int len) {
        byte[] dest = new byte[len];
        readBytesFinite(b, off, dest, 0, len);
        return dest;
    }

    private static void readBytesFinite(byte[] b, int off, byte[] dest, int destOff, int len) {
        if (off + len > b.length) {
            throw new BadCborException("out-of-bounds finite string read operands", true);
        }
        System.arraycopy(b, off, dest, destOff, len);
    }

    private static byte[] readBytesIndefinite(byte[] buffer, int off, int len) {
        byte[] dest = new byte[len];
        readBytesIndefinite(buffer, off, dest, 0, len);
        return dest;
    }

    private static void readBytesIndefinite(byte[] buffer, int off, byte[] dest, int destOff, int len) {
        int strPos = 0;
        int cursor = off;
        while (strPos < len) {
            byte b = buffer[cursor];
            int minor = b & MINOR_TYPE_MASK;
            int argLen = argLength(minor);
            int strLen = readStrLen(buffer, cursor, minor, argLen);
            cursor += argLen + 1;
            System.arraycopy(buffer, cursor, dest, destOff + strPos, strLen);
            cursor += strLen;
            strPos += strLen;
        }
        if (strPos != len) {
            throw new BadCborException("cannot read unclosed indefinite length string");
        }
    }

    private static byte getMajor(byte b) {
        return (byte) ((b & MAJOR_TYPE_MASK) >> MAJOR_TYPE_SHIFT);
    }

    private static byte getMinor(byte b) {
        return (byte) (b & MINOR_TYPE_MASK);
    }

    private static void checkInvalidLength(byte[] buf, int off, int len) {
        if (len > buf.length - off) {
            throw new BadCborException("Invalid length");
        }
    }
}
