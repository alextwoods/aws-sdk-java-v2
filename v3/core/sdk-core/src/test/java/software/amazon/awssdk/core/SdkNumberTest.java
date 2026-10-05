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

package software.amazon.awssdk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** The V2 SdkNumber behaviors: exact string retention, numeric conversions and value equality. */
class SdkNumberTest {
    private static final String THIRTY_TWO_DIGITS = "100000000000000000000000000000000";

    @Test
    void integerAndLongNumbersConvertToEveryPrimitive() {
        SdkNumber number = SdkNumber.fromInteger(-100);
        assertEquals(BigDecimal.valueOf(-100), number.bigDecimalValue());
        assertEquals(-100L, number.longValue());
        assertEquals(-100.0, number.doubleValue());
        assertEquals((short) -100, number.shortValue());
        assertEquals((byte) -100, number.byteValue());
        assertEquals("-100", number.toString());
        assertEquals(SdkNumber.fromInteger(-100), number);
        assertEquals(SdkNumber.fromInteger(-100).hashCode(), number.hashCode());
        assertEquals(-123456789, SdkNumber.fromLong(-123456789L).intValue());
        assertEquals(-100.0f, number.floatValue());
        SdkNumber small = SdkNumber.fromShort((short) 2);
        assertEquals(2L, small.longValue());
        assertEquals("2", small.stringValue());
        assertEquals(-123456789.987654321f, SdkNumber.fromFloat(-123456789.987654321f).floatValue());
    }

    @Test
    void doubleNumberKeepsTheDoubleRepresentation() {
        SdkNumber number = SdkNumber.fromDouble(-123456789.987654321);
        assertEquals(-123456789L, number.longValue());
        assertEquals(BigDecimal.valueOf(-123456789.987654321), number.bigDecimalValue());
        assertEquals("-1.2345678998765433E8", number.toString());
    }

    @Test
    void stringNumbersKeepFullPrecision() {
        String digits = THIRTY_TWO_DIGITS + ".123456789000000";
        SdkNumber number = SdkNumber.fromString(digits);
        assertEquals(new BigDecimal(digits), number.bigDecimalValue());
        assertEquals(digits, number.stringValue());
        assertEquals(SdkNumber.fromBigDecimal(new BigDecimal(digits)), number);
        assertEquals(new BigInteger(THIRTY_TWO_DIGITS).toString(),
                     SdkNumber.fromBigInteger(new BigInteger(THIRTY_TWO_DIGITS)).toString());
    }

    @Test
    void nonFiniteNumbersHaveNoBigDecimalValue() {
        assertThrows(NumberFormatException.class, () -> SdkNumber.fromDouble(Double.NaN).bigDecimalValue());
        assertThrows(NumberFormatException.class,
                     () -> SdkNumber.fromFloat(Float.NEGATIVE_INFINITY).bigDecimalValue());
        assertEquals("Infinity", SdkNumber.fromDouble(Double.POSITIVE_INFINITY).toString());
    }

    @Test
    void isNotEqualToAPrimitiveWrapper() {
        assertNotEquals(Integer.valueOf(2), SdkNumber.fromInteger(2));
        assertThrows(NullPointerException.class, () -> SdkNumber.fromBigDecimal(null));
    }
}
