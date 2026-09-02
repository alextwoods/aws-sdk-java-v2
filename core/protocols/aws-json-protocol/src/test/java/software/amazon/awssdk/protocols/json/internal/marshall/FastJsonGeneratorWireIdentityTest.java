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

package software.amazon.awssdk.protocols.json.internal.marshall;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;

/**
 * Verifies that {@link FastJsonGenerator} produces byte-identical output to the Jackson-based
 * {@link SdkJsonGenerator} for every write operation the SDK marshallers use.
 */
public class FastJsonGeneratorWireIdentityTest {

    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";

    /**
     * Applies the same write sequence to both generators and asserts identical bytes.
     */
    private static void assertIdentical(Consumer<StructuredJsonGenerator> writes) {
        SdkJsonGenerator jackson = new SdkJsonGenerator(
            AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.getJsonFactory(), CONTENT_TYPE);
        FastJsonGenerator fast = new FastJsonGenerator(CONTENT_TYPE, 4);

        writes.accept(jackson);
        writes.accept(fast);

        byte[] expected = jackson.getBytes();
        byte[] actual = fast.getBytes();
        assertThat(new String(actual, StandardCharsets.UTF_8))
            .as("JSON text")
            .isEqualTo(new String(expected, StandardCharsets.UTF_8));
        assertThat(actual).as("exact bytes").isEqualTo(expected);
    }

    private static void assertIdenticalValue(Consumer<StructuredJsonGenerator> valueWrite) {
        assertIdentical(g -> {
            g.writeStartObject();
            g.writeFieldName("v");
            valueWrite.accept(g);
            g.writeEndObject();
        });
    }

    @Test
    public void emptyObject() {
        assertIdentical(g -> {
            g.writeStartObject();
            g.writeEndObject();
        });
    }

    @Test
    public void nestedStructure() {
        assertIdentical(g -> {
            g.writeStartObject();
            g.writeFieldName("TableName");
            g.writeValue("my-table");
            g.writeFieldName("Item");
            g.writeStartObject();
            g.writeFieldName("id");
            g.writeStartObject();
            g.writeFieldName("S");
            g.writeValue("123");
            g.writeEndObject();
            g.writeFieldName("tags");
            g.writeStartObject();
            g.writeFieldName("L");
            g.writeStartArray();
            g.writeStartObject();
            g.writeFieldName("S");
            g.writeValue("a");
            g.writeEndObject();
            g.writeStartObject();
            g.writeFieldName("N");
            g.writeValue("1.5");
            g.writeEndObject();
            g.writeEndArray();
            g.writeEndObject();
            g.writeEndObject();
            g.writeEndObject();
        });
    }

    @Test
    public void arrays() {
        assertIdentical(g -> {
            g.writeStartArray();
            g.writeValue("a");
            g.writeValue(1);
            g.writeValue(true);
            g.writeNull();
            g.writeStartArray();
            g.writeEndArray();
            g.writeStartObject();
            g.writeEndObject();
            g.writeEndArray();
        });
    }

    @Test
    public void strings() {
        String[] cases = {
            "", "a", "hello world", "with \"quotes\"", "back\\slash", "tab\there",
            "new\nline", "carriage\rreturn", "back\bspace", "form\ffeed",
            "\u0000\u0001\u001f", "caf\u00e9", "\u00ff", "\u0800", "\u4e2d\u6587",
            "emoji \ud83d\ude00 mixed", "\ud83d\ude00", "trailing unicode \u20ac",
            "\u007f", "a".concat(new String(new char[100]).replace('\0', 'x')),
        };
        for (String s : cases) {
            assertIdenticalValue(g -> g.writeValue(s));
            // Also as a field name.
            assertIdentical(g -> {
                g.writeStartObject();
                g.writeFieldName(s.isEmpty() ? "e" : s);
                g.writeValue("x");
                g.writeEndObject();
            });
        }
    }

    @Test
    public void allControlCharacters() {
        for (char c = 0; c < 0x20; c++) {
            String s = "a" + c + "b";
            assertIdenticalValue(g -> g.writeValue(s));
        }
    }

    /**
     * The ASCII fast path consumes four characters per iteration and only stores a group once the whole
     * group is known to be safe, so where a rejecting character falls inside a group, and how the string
     * length lines up with the group stride, both select different code. Walk one rejecting character
     * through every offset of every length that spans the boundary cases rather than relying on the fuzz
     * above to hit them.
     */
    @Test
    public void asciiFastPathRejectionAtEveryOffset() {
        char[] rejecting = {'"', '\\', '\n', '\u0000', '\u001f', '\u0080', '\u00e9', '\u4e2d'};

        for (char bad : rejecting) {
            for (int len = 1; len <= 13; len++) {
                for (int at = 0; at < len; at++) {
                    char[] chars = new char[len];
                    for (int i = 0; i < len; i++) {
                        chars[i] = (char) ('a' + (i % 26));
                    }
                    chars[at] = bad;
                    String s = new String(chars);
                    assertIdenticalValue(g -> g.writeValue(s));
                }
            }
        }
    }

    /** Pure-ASCII strings at every length across several group strides. */
    @Test
    public void asciiFastPathAtEveryLength() {
        for (int len = 0; len <= 40; len++) {
            char[] chars = new char[len];
            for (int i = 0; i < len; i++) {
                chars[i] = (char) ('a' + (i % 26));
            }
            String s = new String(chars);
            assertIdenticalValue(g -> g.writeValue(s));
        }
    }

    @Test
    public void randomStringFuzz() {
        Random r = new Random(42);
        for (int iter = 0; iter < 5000; iter++) {
            int len = r.nextInt(64);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                int kind = r.nextInt(10);
                char c;
                if (kind < 6) {
                    c = (char) (0x20 + r.nextInt(0x5f)); // printable ascii
                } else if (kind < 7) {
                    c = (char) r.nextInt(0x20); // control
                } else if (kind < 8) {
                    c = (char) (0x80 + r.nextInt(0x780)); // 2-byte utf-8
                } else if (kind < 9) {
                    c = (char) (0x800 + r.nextInt(0xD800 - 0x800)); // 3-byte utf-8 below surrogates
                } else {
                    // Valid surrogate pair (4-byte utf-8)
                    sb.append((char) (0xD800 + r.nextInt(0x400)));
                    c = (char) (0xDC00 + r.nextInt(0x400));
                }
                sb.append(c);
            }
            String s = sb.toString();
            assertIdenticalValue(g -> g.writeValue(s));
        }
    }

    @Test
    public void integers() {
        int[] cases = {0, 1, -1, 9, 10, 99, 100, 101, 999, 1000, 12345, -12345,
                       999999999, 1000000000, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int v : cases) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
    }

    @Test
    public void longs() {
        long[] cases = {0L, 1L, -1L, 999999999L, 1000000000L, -1000000000L,
                        Integer.MAX_VALUE, Integer.MAX_VALUE + 1L,
                        999999999999999999L, 1000000000000000000L,
                        Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1};
        for (long v : cases) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
        Random r = new Random(7);
        for (int i = 0; i < 2000; i++) {
            long v = r.nextLong();
            assertIdenticalValue(g -> g.writeValue(v));
        }
    }

    @Test
    public void shortsAndBytes() {
        for (short v : new short[] {0, 1, -1, 127, -128, Short.MAX_VALUE, Short.MIN_VALUE}) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
        for (byte v : new byte[] {0, 1, -1, Byte.MAX_VALUE, Byte.MIN_VALUE}) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
    }

    @Test
    public void doubles() {
        double[] cases = {0.0, -0.0, 1.0, -1.0, 1.5, 3.141592653589793, 1e300, -1e300,
                          1e-300, 4.9e-324, Double.MAX_VALUE, Double.MIN_VALUE,
                          Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                          123456.789, 0.1, 0.2, 1.0 / 3.0};
        for (double v : cases) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
        Random r = new Random(11);
        for (int i = 0; i < 2000; i++) {
            double v = Double.longBitsToDouble(r.nextLong());
            if (Double.isNaN(v)) {
                continue; // covered above; random NaN payloads normalize differently
            }
            assertIdenticalValue(g -> g.writeValue(v));
        }
    }

    @Test
    public void floats() {
        float[] cases = {0.0f, -0.0f, 1.0f, -1.0f, 1.5f, 3.1415927f, 1e30f, -1e30f,
                         Float.MAX_VALUE, Float.MIN_VALUE,
                         Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (float v : cases) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
        Random r = new Random(13);
        for (int i = 0; i < 2000; i++) {
            float v = Float.intBitsToFloat(r.nextInt());
            if (Float.isNaN(v)) {
                continue;
            }
            assertIdenticalValue(g -> g.writeValue(v));
        }
    }

    @Test
    public void bigNumbers() {
        assertIdenticalValue(g -> g.writeValue(new BigDecimal("123.456")));
        assertIdenticalValue(g -> g.writeValue(new BigDecimal("-0.000001")));
        assertIdenticalValue(g -> g.writeValue(new BigDecimal("1E+50")));
        assertIdenticalValue(g -> g.writeValue(BigInteger.ZERO));
        assertIdenticalValue(g -> g.writeValue(new BigInteger("-98765432109876543210987654321")));
        assertIdenticalValue(g -> g.writeValue(new BigInteger("12345678901234567890123456789012345678901234567890")));
    }

    @Test
    public void rawNumberStrings() {
        for (String n : new String[] {"0", "-1", "1.5", "1E10", "123456789.123456789", "-0.0"}) {
            assertIdenticalValue(g -> g.writeNumber(n));
        }
    }

    @Test
    public void booleansAndNull() {
        assertIdenticalValue(g -> g.writeValue(true));
        assertIdenticalValue(g -> g.writeValue(false));
        assertIdenticalValue(StructuredJsonGenerator::writeNull);
    }

    @Test
    public void instants() {
        Instant[] cases = {
            Instant.EPOCH,
            Instant.ofEpochMilli(1548118964201L),
            Instant.ofEpochMilli(1L),
            Instant.ofEpochMilli(999L),
            Instant.ofEpochMilli(-1L),
            Instant.ofEpochSecond(1548118964L),
            Instant.ofEpochSecond(-62135596800L), // year 1
        };
        for (Instant v : cases) {
            assertIdenticalValue(g -> g.writeValue(v));
        }
    }

    @Test
    public void binary() {
        Random r = new Random(17);
        for (int len : new int[] {0, 1, 2, 3, 4, 5, 6, 16, 57, 100, 1000}) {
            byte[] data = new byte[len];
            r.nextBytes(data);
            assertIdenticalValue(g -> g.writeBinaryValue(data));
            assertIdenticalValue(g -> g.writeValue(ByteBuffer.wrap(data)));

            // ByteBuffer with a non-zero offset and non-consumed position
            byte[] padded = new byte[len + 7];
            r.nextBytes(padded);
            System.arraycopy(data, 0, padded, 3, len);
            ByteBuffer sliced = ByteBuffer.wrap(padded, 3, len);
            assertIdenticalValue(g -> g.writeValue(sliced.duplicate()));

            // Direct (non-array-backed) buffer
            ByteBuffer direct = ByteBuffer.allocateDirect(len);
            direct.put(data);
            direct.flip();
            assertIdenticalValue(g -> g.writeValue(direct.duplicate()));

            // Read-only buffer
            assertIdenticalValue(g -> g.writeValue(ByteBuffer.wrap(data).asReadOnlyBuffer()));
        }
    }

    @Test
    public void bufferGrowthFromTinyInitialCapacity() {
        // FastJsonGenerator constructed with capacity 4 in the harness; also verify a large body.
        assertIdentical(g -> {
            g.writeStartObject();
            for (int i = 0; i < 500; i++) {
                g.writeFieldName("field" + i);
                g.writeValue("value-" + i + "-\u00e9\u4e2d");
            }
            g.writeEndObject();
        });
    }

    @Test
    public void preEncodedFieldNameTokenMatchesStringPath() {
        String[] names = {"a", "TableName", "S", "with \"quotes\"", "caf\u00e9", "\u4e2d\u6587",
                          "emoji \ud83d\ude00", "tab\there", "field-with-long-name-0123456789"};
        for (String name : names) {
            byte[] token = FastJsonGenerator.encodeFieldNameToken(name);

            FastJsonGenerator viaString = new FastJsonGenerator(CONTENT_TYPE, 4);
            viaString.writeStartObject();
            viaString.writeFieldName("first");
            viaString.writeValue(1);
            viaString.writeFieldName(name);
            viaString.writeValue("v");
            viaString.writeEndObject();

            FastJsonGenerator viaToken = new FastJsonGenerator(CONTENT_TYPE, 4);
            viaToken.writeStartObject();
            viaToken.writeFieldName("first", FastJsonGenerator.encodeFieldNameToken("first"));
            viaToken.writeValue(1);
            viaToken.writeFieldName(name, token);
            viaToken.writeValue("v");
            viaToken.writeEndObject();

            assertThat(viaToken.getBytes()).as("field name: %s", name).isEqualTo(viaString.getBytes());
        }
    }

    @Test
    public void contentSizeAndProviderMatchGetBytes() {
        FastJsonGenerator fast = new FastJsonGenerator(CONTENT_TYPE, 4);
        fast.writeStartObject();
        fast.writeFieldName("a");
        fast.writeValue("b");
        fast.writeEndObject();
        byte[] bytes = fast.getBytes();
        assertThat(fast.contentSize()).isEqualTo(bytes.length);
        ByteBuffer buffered = fast.contentStreamProvider().contentAsByteBufferOrNull();
        byte[] fromBuffer = new byte[buffered.remaining()];
        buffered.get(fromBuffer);
        assertThat(fromBuffer).isEqualTo(bytes);
    }
}
