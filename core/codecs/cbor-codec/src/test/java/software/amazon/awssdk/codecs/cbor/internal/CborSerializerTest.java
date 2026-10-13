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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.cbor.CborSerializerTest}.
 */
class CborSerializerTest {
    private static final CborSettings SETTINGS = CborSettings.defaultSettings();
    private static final DefaultCborSerdeProvider CODEC = new DefaultCborSerdeProvider();

    @Test
    void birdSerialization() {
        String name = "kestrel";
        ByteBuffer source = ByteBuffer.wrap(" li'l guy".getBytes(StandardCharsets.UTF_8));
        source.position(1);
        ByteBuffer bytes = source.asReadOnlyBuffer();
        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        BigInteger flightRange = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        BigDecimal wingspan = BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.ONE);
        CborTestData.Bird bird = new CborTestData.BirdBuilder()
            .name(name)
            .bytes(bytes.duplicate())
            .lastSquawkAt(timestamp)
            .flightRange(flightRange)
            .wingspan(wingspan)
            .build();

        ByteBuffer ser = CODEC.serialize(bird, SETTINGS);
        CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS)).build();

        assertEquals(name, de.name);
        assertEquals(timestamp, de.lastSquawkAt);
        assertEquals(flightRange, de.flightRange);
        assertEquals(wingspan, de.wingspan);
        assertBuffersEqual(bytes, de.bytes);
        assertEquals("li'l guy", new String(ByteBufferUtils.getBytes(de.bytes), StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1",
        "-5",
        "9223372036854775807",
        "-9223372036854775807",
        "1844674407370955161618446744073709551616",
        "-922337203685477581318446744073709551616",
        "9223372036854775813",
        "1000000000000000000000000000000000000000000000000001",
        "-9223372036854775936",
        "18446744073709551616",
        "-18446744073709551616",
        "-9223372036854775808"
    })
    void testBigInteger(String input) {
        BigInteger flightRange = new BigInteger(input);
        CborTestData.Bird bird = new CborTestData.BirdBuilder().flightRange(flightRange).build();
        ByteBuffer ser = CODEC.serialize(bird, SETTINGS);
        CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS)).build();
        assertEquals(flightRange, de.flightRange);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1",
        "1.0",
        "-5.0",
        "18446744073709551616.18446744073709551616",
        "-9223372036854775813.18446744073709551616",
        "92233720.36854775813",
        "-18446744073709551616",
        "-0.18446744073709551616",
        "0.18446744073709551616",
        "18446744073709551616.18446744073709551616184467440737095516160",
        "-18446744073709551616184467440.73709551616184467440737095516160",
        "55415038757710541115685710749848141603954998263526436372.47"
    })
    void testBigDecimal(String input) {
        BigDecimal wingspan = new BigDecimal(input);
        CborTestData.Bird bird = new CborTestData.BirdBuilder().wingspan(wingspan).build();
        ByteBuffer ser = CODEC.serialize(bird, SETTINGS);
        CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS)).build();
        assertEquals(wingspan, de.wingspan);
    }

    @Test
    void testBigIntegerWithNegativeFirstByte() {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("bf6b666c6967687452616e6765c251fb3fffffffffffffff0000000000000001ff",
                   "85495944688885788945154423373158053576705");
        inputs.put("bf6b666c6967687452616e6765c351fb3fffffffffffffff0000000000000001ff",
                   "-85495944688885788945154423373158053576706");
        for (Map.Entry<String, String> input : inputs.entrySet()) {
            byte[] ser = CborTestData.hex(input.getKey());
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();
            assertEquals(new BigInteger(input.getValue()), de.flightRange);
        }
    }

    @Test
    void testBigDecimalWithNegativeFirstByte() {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("bf6877696e677370616ec48221c25818e1ffffffffffffff40000000000000017fffffffffffffffff",
                   "55415038757710541115685710749848141603954998263526436372.47");
        inputs.put("bf6877696e677370616ec48221c35818e1ffffffffffffff40000000000000017fffffffffffffffff",
                   "-55415038757710541115685710749848141603954998263526436372.48");
        for (Map.Entry<String, String> input : inputs.entrySet()) {
            byte[] ser = CborTestData.hex(input.getKey());
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();
            assertEquals(new BigDecimal(input.getValue()), de.wingspan);
        }
    }

    private static void assertBuffersEqual(ByteBuffer expected, ByteBuffer actual) {
        assertArrayEquals(ByteBufferUtils.getBytes(expected), ByteBufferUtils.getBytes(actual));
    }
}
