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

import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DecimalToBinaryTest {

    @Test
    void reciprocalTableMatchesBigIntegerDerivation() {
        BigInteger numerator = BigInteger.ONE.shiftLeft(256);
        assertEquals(0, DecimalToBinary.RECIPROCAL_POW10[0]);
        for (int scale = 1; scale <= DecimalToBinary.MAX_DIGITS; scale++) {
            BigInteger quotient = numerator.divide(BigInteger.TEN.pow(scale));
            long expected = quotient.shiftRight(quotient.bitLength() - Long.SIZE).longValue();
            assertEquals(expected, DecimalToBinary.RECIPROCAL_POW10[scale], "scale=" + scale);
        }
    }

    @Test
    @Tag("stress")
    void randomValidatedSignificandsMatchJdk() {
        Random random = new Random(7);
        for (int i = 0; i < 200_000; i++) {
            long unscaled = (random.nextLong() & Long.MAX_VALUE) % 1_000_000_000_000_000_000L;
            int scale = random.nextInt(47) - 23;
            double fast = DecimalToBinary.toDouble(unscaled, scale);
            if (!Double.isNaN(fast)) {
                assertEquals(Double.doubleToRawLongBits(Double.parseDouble(unscaled + "e" + -scale)),
                        Double.doubleToRawLongBits(fast));
            }
        }
    }
}
