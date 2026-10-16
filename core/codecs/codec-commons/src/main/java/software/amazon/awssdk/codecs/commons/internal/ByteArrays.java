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

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Reads and writes {@code int} and {@code long} values at any offset of a byte array, in little-endian or big-endian
 * byte order.
 *
 * <p>Java 8 replacement for the {@code MethodHandles.byteArrayViewVarHandle} views that smithy-java's codecs use. Each
 * method is a plain shift-and-mask expression over the array elements, so an index outside the array throws
 * {@link ArrayIndexOutOfBoundsException}, as the views do. Racecar E6/E7 loop optimizations are not applied here.
 */
@SdkInternalApi
public final class ByteArrays {

    private ByteArrays() {
    }

    /**
     * Reads eight bytes starting at {@code pos} as a little-endian {@code long} (the byte at {@code pos} is the lowest
     * byte).
     */
    public static long getLongLittleEndian(byte[] buf, int pos) {
        return (buf[pos] & 0xFFL)
               | (buf[pos + 1] & 0xFFL) << 8
               | (buf[pos + 2] & 0xFFL) << 16
               | (buf[pos + 3] & 0xFFL) << 24
               | (buf[pos + 4] & 0xFFL) << 32
               | (buf[pos + 5] & 0xFFL) << 40
               | (buf[pos + 6] & 0xFFL) << 48
               | (buf[pos + 7] & 0xFFL) << 56;
    }

    /**
     * Writes {@code value} as eight little-endian bytes starting at {@code pos}.
     */
    public static void putLongLittleEndian(byte[] buf, int pos, long value) {
        buf[pos] = (byte) value;
        buf[pos + 1] = (byte) (value >>> 8);
        buf[pos + 2] = (byte) (value >>> 16);
        buf[pos + 3] = (byte) (value >>> 24);
        buf[pos + 4] = (byte) (value >>> 32);
        buf[pos + 5] = (byte) (value >>> 40);
        buf[pos + 6] = (byte) (value >>> 48);
        buf[pos + 7] = (byte) (value >>> 56);
    }

    /**
     * Reads four bytes starting at {@code pos} as a little-endian {@code int}.
     */
    public static int getIntLittleEndian(byte[] buf, int pos) {
        return (buf[pos] & 0xFF)
               | (buf[pos + 1] & 0xFF) << 8
               | (buf[pos + 2] & 0xFF) << 16
               | (buf[pos + 3] & 0xFF) << 24;
    }

    /**
     * Writes {@code value} as four little-endian bytes starting at {@code pos}.
     */
    public static void putIntLittleEndian(byte[] buf, int pos, int value) {
        buf[pos] = (byte) value;
        buf[pos + 1] = (byte) (value >>> 8);
        buf[pos + 2] = (byte) (value >>> 16);
        buf[pos + 3] = (byte) (value >>> 24);
    }

    /**
     * Reads eight bytes starting at {@code pos} as a big-endian {@code long} (the byte at {@code pos} is the highest
     * byte).
     */
    public static long getLongBigEndian(byte[] buf, int pos) {
        return (buf[pos] & 0xFFL) << 56
               | (buf[pos + 1] & 0xFFL) << 48
               | (buf[pos + 2] & 0xFFL) << 40
               | (buf[pos + 3] & 0xFFL) << 32
               | (buf[pos + 4] & 0xFFL) << 24
               | (buf[pos + 5] & 0xFFL) << 16
               | (buf[pos + 6] & 0xFFL) << 8
               | (buf[pos + 7] & 0xFFL);
    }

    /**
     * Writes {@code value} as eight big-endian bytes starting at {@code pos}.
     */
    public static void putLongBigEndian(byte[] buf, int pos, long value) {
        buf[pos] = (byte) (value >>> 56);
        buf[pos + 1] = (byte) (value >>> 48);
        buf[pos + 2] = (byte) (value >>> 40);
        buf[pos + 3] = (byte) (value >>> 32);
        buf[pos + 4] = (byte) (value >>> 24);
        buf[pos + 5] = (byte) (value >>> 16);
        buf[pos + 6] = (byte) (value >>> 8);
        buf[pos + 7] = (byte) value;
    }

    /**
     * Reads four bytes starting at {@code pos} as a big-endian {@code int}.
     */
    public static int getIntBigEndian(byte[] buf, int pos) {
        return (buf[pos] & 0xFF) << 24
               | (buf[pos + 1] & 0xFF) << 16
               | (buf[pos + 2] & 0xFF) << 8
               | (buf[pos + 3] & 0xFF);
    }

    /**
     * Writes {@code value} as four big-endian bytes starting at {@code pos}.
     */
    public static void putIntBigEndian(byte[] buf, int pos, int value) {
        buf[pos] = (byte) (value >>> 24);
        buf[pos + 1] = (byte) (value >>> 16);
        buf[pos + 2] = (byte) (value >>> 8);
        buf[pos + 3] = (byte) value;
    }

    /**
     * Returns whether {@code a[aFrom, aTo)} and {@code b[bFrom, bTo)} hold the same bytes. Java 8 replacement for
     * {@code Arrays.equals(byte[], int, int, byte[], int, int)}; the ranges must be inside the arrays.
     */
    public static boolean rangeEquals(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        int length = aTo - aFrom;
        if (length != bTo - bFrom) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[aFrom + i] != b[bFrom + i]) {
                return false;
            }
        }
        return true;
    }
}
