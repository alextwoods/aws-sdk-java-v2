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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * SDK tests for {@link ByteArrays} and {@link NumberCodec#unsignedMultiplyHigh}, the Java 8 replacements for the
 * {@code VarHandle} array views and {@code Math.unsignedMultiplyHigh}. {@link ByteBuffer} views and
 * {@link BigInteger} are the oracles.
 */
class ByteArraysTest {

    private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(64);

    @Test
    void getters_atEveryOffset_matchByteBufferViews() {
        Random random = new Random(71);
        byte[] buf = new byte[32];
        for (int round = 0; round < 1_000; round++) {
            random.nextBytes(buf);
            for (int pos = 0; pos + 8 <= buf.length; pos++) {
                ByteBuffer little = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
                ByteBuffer big = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
                assertThat(ByteArrays.getLongLittleEndian(buf, pos)).isEqualTo(little.getLong(pos));
                assertThat(ByteArrays.getIntLittleEndian(buf, pos)).isEqualTo(little.getInt(pos));
                assertThat(ByteArrays.getLongBigEndian(buf, pos)).isEqualTo(big.getLong(pos));
                assertThat(ByteArrays.getIntBigEndian(buf, pos)).isEqualTo(big.getInt(pos));
            }
        }
    }

    @Test
    void putters_atRandomOffsets_matchByteBufferViewsAndTouchOnlyTheirBytes() {
        Random random = new Random(72);
        for (int round = 0; round < 1_000; round++) {
            long value = random.nextLong();
            int pos = random.nextInt(8);
            assertWrites(value, pos, ByteOrder.LITTLE_ENDIAN, 8);
            assertWrites(value, pos, ByteOrder.BIG_ENDIAN, 8);
            assertWrites(value, pos, ByteOrder.LITTLE_ENDIAN, 4);
            assertWrites(value, pos, ByteOrder.BIG_ENDIAN, 4);
        }
    }

    @Test
    void getAndPut_outsideTheArray_throwArrayIndexOutOfBounds() {
        byte[] buf = new byte[7];
        assertThatThrownBy(() -> ByteArrays.getLongLittleEndian(buf, 0))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> ByteArrays.putIntBigEndian(buf, 4, 1))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    void rangeEquals_comparesLengthsAndBytes() {
        byte[] a = {1, 2, 3, 4, 5};
        byte[] b = {9, 2, 3, 4, 9};
        assertThat(ByteArrays.rangeEquals(a, 1, 4, b, 1, 4)).isTrue();
        assertThat(ByteArrays.rangeEquals(a, 1, 4, b, 0, 3)).isFalse();
        assertThat(ByteArrays.rangeEquals(a, 1, 4, b, 1, 5)).isFalse();
        assertThat(ByteArrays.rangeEquals(a, 2, 2, b, 0, 0)).isTrue();
    }

    @Test
    void unsignedMultiplyHigh_edgeAndRandomValues_matchesBigInteger() {
        long[] edges = {0, 1, 2, -1, -2, Long.MAX_VALUE, Long.MIN_VALUE, 0xFFFFFFFFL, 0x100000000L, 0x8000000000000001L};
        for (long x : edges) {
            for (long y : edges) {
                assertThat(NumberCodec.unsignedMultiplyHigh(x, y)).as("%x * %x", x, y).isEqualTo(expectedHigh(x, y));
            }
        }
        Random random = new Random(73);
        for (int i = 0; i < 100_000; i++) {
            long x = random.nextLong();
            long y = random.nextLong();
            assertThat(NumberCodec.unsignedMultiplyHigh(x, y)).as("%x * %x", x, y).isEqualTo(expectedHigh(x, y));
        }
    }

    private static long expectedHigh(long x, long y) {
        return unsigned(x).multiply(unsigned(y)).shiftRight(64).longValue();
    }

    private static BigInteger unsigned(long value) {
        BigInteger result = BigInteger.valueOf(value);
        return value < 0 ? result.add(TWO_TO_64) : result;
    }

    private static void assertWrites(long value, int pos, ByteOrder order, int width) {
        byte[] actual = new byte[24];
        byte[] expected = new byte[24];
        ByteBuffer view = ByteBuffer.wrap(expected).order(order);
        if (width == 8) {
            view.putLong(pos, value);
            if (order == ByteOrder.LITTLE_ENDIAN) {
                ByteArrays.putLongLittleEndian(actual, pos, value);
            } else {
                ByteArrays.putLongBigEndian(actual, pos, value);
            }
        } else {
            view.putInt(pos, (int) value);
            if (order == ByteOrder.LITTLE_ENDIAN) {
                ByteArrays.putIntLittleEndian(actual, pos, (int) value);
            } else {
                ByteArrays.putIntBigEndian(actual, pos, (int) value);
            }
        }
        assertThat(actual).isEqualTo(expected);
    }
}
