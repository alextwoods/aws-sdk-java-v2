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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ZmijBoundaryTest {
    @Test
    void signedZeroPowersOfTwoAndTheirNeighbors() {
        check(0.0);
        check(-0.0);
        check(0.0f);
        check(-0.0f);
        for (int exponent = 0; exponent < 2047; exponent++) {
            double value = Double.longBitsToDouble((long) exponent << 52);
            check(value);
            check(-value);
            check(Math.nextUp(value));
            check(Math.nextDown(value));
        }
        for (int exponent = 0; exponent < 255; exponent++) {
            float value = Float.intBitsToFloat(exponent << 23);
            check(value);
            check(-value);
            check(Math.nextUp(value));
            check(Math.nextDown(value));
        }
    }

    @Test
    void decimalPlacementAndTinySubnormals() {
        for (int bits = 1; bits < 10; bits++) {
            check(Double.longBitsToDouble(bits));
            check(-Double.longBitsToDouble(bits));
            check(Float.intBitsToFloat(bits));
            check(-Float.intBitsToFloat(bits));
        }
        for (double value : new double[] {
                0.0009999999999999998,
                0.001,
                0.01,
                0.1,
                1,
                1.25,
                12.5,
                1000.25,
                9999999,
                10000000,
                12345678.5,
                100000000.125,
                1e-99,
                1e-100,
                1e99,
                1e100,
                Double.MAX_VALUE,
                -2.0528210082510967E-190,
                -1.35191895E-20f,
                999999.9999999999,
                999999999999999.9
        }) {
            check(value);
            check(-value);
            check((float) value);
            check((float) -value);
        }
    }

    @Test
    void deterministicRoundingCases() {
        Random random = new Random(20260918);
        for (int i = 0; i < 10_000; i++) {
            check(Double.longBitsToDouble(random.nextLong()));
            check(Float.intBitsToFloat(random.nextInt()));
        }
    }

    private static void check(double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        byte[] bytes = new byte[3 + NumberCodec.DOUBLE_MAX_BYTES + 1];
        Arrays.fill(bytes, (byte) 0x7f);
        int end = Zmij.writeDouble(bytes, 3, value);
        String actual = new String(bytes, 3, end - 3, StandardCharsets.US_ASCII);
        assertEquals(Schubfach.toString(value), actual);
        if (Schubfach.JDK_TO_STRING_IS_SHORTEST) {
            assertEquals(Double.toString(value), actual);
        }
        assertEquals(0x7f, bytes[0]);
        assertEquals(0x7f, bytes[1]);
        assertEquals(0x7f, bytes[2]);
        assertEquals(0x7f, bytes[bytes.length - 1]);
    }

    private static void check(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        byte[] bytes = new byte[3 + NumberCodec.FLOAT_MAX_BYTES + 1];
        Arrays.fill(bytes, (byte) 0x7f);
        int end = Zmij.writeFloat(bytes, 3, value);
        String actual = new String(bytes, 3, end - 3, StandardCharsets.US_ASCII);
        assertEquals(Schubfach.toString(value), actual);
        if (Schubfach.JDK_TO_STRING_IS_SHORTEST) {
            assertEquals(Float.toString(value), actual);
        }
        assertEquals(0x7f, bytes[0]);
        assertEquals(0x7f, bytes[1]);
        assertEquals(0x7f, bytes[2]);
        assertEquals(0x7f, bytes[bytes.length - 1]);
    }
}
