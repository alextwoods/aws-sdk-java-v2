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

package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.codecs.json.TestCollections.listOf;
import static software.amazon.awssdk.codecs.json.TestCollections.mapOf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Port of smithy-java {@code GeneratedModelSerdeTest}: round trips of the json-codec test model through the codec with
 * {@code jsonName} and {@code timestampFormat} on. smithy-java uses classes generated from {@code bench.smithy}; the
 * port builds the same shapes as {@link BenchModel} schemas held in {@link SchemaStruct}s (enums as strings), so the
 * tests check the codec, not generated classes.
 */
public class GeneratedModelSerdeTest extends ProviderTestBase {

    private static <T extends SerializableShape> T roundtrip(JsonSerdeProvider provider, SerializableShape original,
                                                             ShapeBuilder<T> builder) {
        return roundtrip(provider, provider, original, builder);
    }

    private static <T extends SerializableShape> T roundtrip(JsonSerdeProvider ser, JsonSerdeProvider de,
                                                             SerializableShape original, ShapeBuilder<T> builder) {
        try (JsonCodec serCodec = JsonCodec.builder()
                                           .overrideSerdeProvider(ser)
                                           .useJsonName(true)
                                           .useTimestampFormat(true)
                                           .build();
             JsonCodec deCodec = JsonCodec.builder()
                                          .overrideSerdeProvider(de)
                                          .useJsonName(true)
                                          .useTimestampFormat(true)
                                          .build()) {
            ByteBuffer serialized = serCodec.serialize(original);
            byte[] bytes = new byte[serialized.remaining()];
            serialized.get(bytes);
            return deCodec.deserializeShape(bytes, builder);
        }
    }

    /**
     * Builds a {@link SchemaStruct} of {@code schema} from member name and value pairs.
     */
    private static SchemaStruct struct(Schema schema, Object... namesAndValues) {
        SchemaStruct.Builder builder = SchemaStruct.builder(schema);
        for (int i = 0; i < namesAndValues.length; i += 2) {
            builder.set((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return builder.build();
    }

    private static SchemaStruct.Builder builder(Schema schema) {
        return SchemaStruct.builder(schema);
    }

    private static SchemaStruct nested(String field1, int field2) {
        return struct(BenchModel.NESTED_STRUCT, "field1", field1, "field2", field2);
    }

    /**
     * A ComplexStruct with its required members set, and the given extra members.
     */
    private static SchemaStruct complex(String id, Object... namesAndValues) {
        SchemaStruct.Builder builder = builder(BenchModel.COMPLEX_STRUCT)
            .set("id", id)
            .set("count", 0)
            .set("nested", nested("f", 0));
        for (int i = 0; i < namesAndValues.length; i += 2) {
            builder.set((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return builder.build();
    }

    // --- Complex Struct ---

    private static SchemaStruct buildComplexStruct() {
        SchemaStruct inner = struct(BenchModel.INNER_STRUCT,
                                    "value", "inner-value",
                                    "numbers", listOf(1, 2, 3, 4, 5));
        SchemaStruct nested = struct(BenchModel.NESTED_STRUCT,
                                     "field1", "nested-field",
                                     "field2", 100,
                                     "inner", inner);
        Map<String, String> sparseMap = new HashMap<>();
        sparseMap.put("x", "1");
        sparseMap.put("y", "2");
        sparseMap.put("z", null);
        return builder(BenchModel.COMPLEX_STRUCT)
            .set("id", "bench-001")
            .set("count", 999)
            .set("enabled", true)
            .set("ratio", 1.618)
            .set("score", 2.718f)
            .set("bigCount", 1_000_000L)
            .set("optionalString", "optional-value")
            .set("optionalInt", 42)
            .set("createdAt", Instant.parse("2025-01-15T10:30:00Z"))
            .set("updatedAt", Instant.parse("2025-06-01T12:00:00Z"))
            .set("expiresAt", Instant.parse("2026-01-01T00:00:00Z"))
            .set("payload", ByteBuffer.wrap("binary-payload-data".getBytes(StandardCharsets.UTF_8)))
            .set("tags", listOf("alpha", "beta", "gamma", "delta"))
            .set("intList", listOf(10, 20, 30, 40, 50))
            .set("metadata", mapOf("key1", "value1", "key2", "value2", "key3", "value3"))
            .set("intMap", mapOf("a", 1, "b", 2, "c", 3))
            .set("nested", nested)
            .set("optionalNested", nested("opt-nested", 200))
            .set("structList", listOf(nested, nested))
            .set("structMap", mapOf("first", nested, "second", nested))
            .set("choice", struct(BenchModel.BENCH_UNION, "stringValue", "union-string"))
            .set("color", "GREEN")
            .set("colorList", listOf("RED", "BLUE", "YELLOW"))
            .set("sparseStrings", Arrays.asList("a", null, "c"))
            .set("sparseMap", sparseMap)
            .set("bigIntValue", new BigInteger("123456789012345678901234567890"))
            .set("bigDecValue", new BigDecimal("99999.99999"))
            .set("freeformData", Document.of(mapOf("key", Document.of("value"), "num", Document.of(42))))
            .build();
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void complexStructRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct original = buildComplexStruct();
        assertThat(roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT))).isEqualTo(original);
    }

    @PerProvider
    void simpleStructRoundtrip(JsonSerdeProvider provider) {
        SchemaStruct original = struct(BenchModel.SIMPLE_STRUCT,
                                       "name", "test",
                                       "age", 42,
                                       "active", true,
                                       "score", 98.6,
                                       "createdAt", Instant.parse("2025-01-15T10:30:00Z"));
        assertThat(roundtrip(provider, original, builder(BenchModel.SIMPLE_STRUCT))).isEqualTo(original);
    }

    // --- Numeric Boundary Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void numericMinValuesRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct original = struct(BenchModel.NUMERIC_STRUCT,
                                       "byteVal", Byte.MIN_VALUE,
                                       "shortVal", Short.MIN_VALUE,
                                       "intVal", Integer.MIN_VALUE,
                                       "longVal", Long.MIN_VALUE,
                                       "floatVal", Float.MIN_VALUE,
                                       "doubleVal", Double.MIN_VALUE,
                                       "bigIntVal", new BigInteger("-99999999999999999999999999999"),
                                       "bigDecVal", new BigDecimal("-99999.99999"));
        assertThat(roundtrip(ser, de, original, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void numericMaxValuesRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct original = struct(BenchModel.NUMERIC_STRUCT,
                                       "byteVal", Byte.MAX_VALUE,
                                       "shortVal", Short.MAX_VALUE,
                                       "intVal", Integer.MAX_VALUE,
                                       "longVal", Long.MAX_VALUE,
                                       "floatVal", Float.MAX_VALUE,
                                       "doubleVal", Double.MAX_VALUE,
                                       "bigIntVal", new BigInteger("99999999999999999999999999999"),
                                       "bigDecVal", new BigDecimal("123456789.123456789"));
        assertThat(roundtrip(ser, de, original, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(original);
    }

    @PerProvider
    void numericSpecialFloatingPoint(JsonSerdeProvider provider) {
        // NaN requires special comparison since NaN != NaN
        SchemaStruct nan = struct(BenchModel.NUMERIC_STRUCT, "floatVal", Float.NaN, "doubleVal", Double.NaN);
        SchemaStruct nanResult = roundtrip(provider, nan, builder(BenchModel.NUMERIC_STRUCT));
        assertThat((Float) nanResult.get("floatVal")).isNaN();
        assertThat((Double) nanResult.get("doubleVal")).isNaN();

        // Infinity values can use equals
        SchemaStruct posInf = struct(BenchModel.NUMERIC_STRUCT,
                                     "floatVal", Float.POSITIVE_INFINITY,
                                     "doubleVal", Double.POSITIVE_INFINITY);
        assertThat(roundtrip(provider, posInf, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(posInf);
        SchemaStruct negInf = struct(BenchModel.NUMERIC_STRUCT,
                                     "floatVal", Float.NEGATIVE_INFINITY,
                                     "doubleVal", Double.NEGATIVE_INFINITY);
        assertThat(roundtrip(provider, negInf, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(negInf);
    }

    @PerProvider
    void numericZeroValues(JsonSerdeProvider provider) {
        SchemaStruct original = struct(BenchModel.NUMERIC_STRUCT,
                                       "byteVal", (byte) 0,
                                       "shortVal", (short) 0,
                                       "intVal", 0,
                                       "longVal", 0L,
                                       "floatVal", 0.0f,
                                       "doubleVal", 0.0,
                                       "bigIntVal", BigInteger.ZERO,
                                       "bigDecVal", BigDecimal.ZERO);
        assertThat(roundtrip(provider, original, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(original);
    }

    @PerProvider
    void bigDecimalVariousScales(JsonSerdeProvider provider) {
        for (BigDecimal bd : listOf(
            new BigDecimal("42"), // scale=0
            new BigDecimal("1.005"), // leading zeros in fraction
            new BigDecimal("1E+10"), // negative scale
            new BigDecimal("12345678901234567890.12345") // >64 bit unscaled
        )) {
            SchemaStruct original = struct(BenchModel.NUMERIC_STRUCT, "bigDecVal", bd);
            SchemaStruct result = roundtrip(provider, original, builder(BenchModel.NUMERIC_STRUCT));
            assertThat((BigDecimal) result.get("bigDecVal")).isEqualByComparingTo(bd);
        }
    }

    @PerProvider
    void bigIntegerSmallAndLarge(JsonSerdeProvider provider) {
        for (BigInteger bi : listOf(BigInteger.valueOf(42),
                                    BigInteger.valueOf(-42),
                                    new BigInteger("123456789012345678901234567890123456789"))) {
            SchemaStruct original = struct(BenchModel.NUMERIC_STRUCT, "bigIntVal", bi);
            assertThat(roundtrip(provider, original, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(original);
        }
    }

    // --- String Edge Cases ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void stringEdgeCasesRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        for (String s : listOf(
            "", // empty
            "abcdefghijklmnopqrstuvwxyz0123456789", // long ASCII (>8 bytes, word-at-a-time scan)
            "quote:\" backslash:\\ tab:\t newline:\n cr:\r", // JSON escapes
            "\u00e9\u4e2d\u00fc", // BMP unicode
            "\uD83D\uDE00\uD83C\uDF89", // SMP emoji
            "hello\tworld\n\u00e9\uD83D\uDE00\"quoted\"" // mixed
        )) {
            SchemaStruct original = struct(BenchModel.STRING_STRUCT, "value", s);
            assertThat(roundtrip(ser, de, original, builder(BenchModel.STRING_STRUCT))).isEqualTo(original);
        }
    }

    // --- Timestamp Format Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void timestampAllFormatsRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        Instant epoch = Instant.EPOCH;
        SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT,
                                       "epochSeconds", epoch,
                                       "dateTime", epoch,
                                       "httpDate", epoch);
        assertThat(roundtrip(ser, de, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void timestampWithNanoseconds(JsonSerdeProvider ser, JsonSerdeProvider de) {
        Instant withNanos = Instant.parse("2025-01-15T10:30:00.123456789Z");
        SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT, "dateTime", withNanos);
        assertThat(roundtrip(ser, de, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void timestampEpochSecondsWithMillis(JsonSerdeProvider ser, JsonSerdeProvider de) {
        // Exercises all three branches in writeEpochSeconds
        for (Instant millis : listOf(
            Instant.ofEpochSecond(1000, 100_000_000), // millis%100==0
            Instant.ofEpochSecond(1000, 120_000_000), // millis%10==0
            Instant.ofEpochSecond(1000, 123_000_000) // 3-digit
        )) {
            SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT, "epochSeconds", millis);
            assertThat(roundtrip(ser, de, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
        }
    }

    @PerProvider
    void timestampPreEpoch(JsonSerdeProvider provider) {
        Instant preEpoch = Instant.parse("1969-12-31T23:59:59Z");
        SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT,
                                       "epochSeconds", preEpoch,
                                       "dateTime", preEpoch,
                                       "httpDate", preEpoch);
        assertThat(roundtrip(provider, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
    }

    @PerProvider
    void timestampLeapYear(JsonSerdeProvider provider) {
        Instant leapDay = LocalDate.of(2024, 2, 29).atStartOfDay(ZoneOffset.UTC).toInstant();
        SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT, "dateTime", leapDay);
        assertThat(roundtrip(provider, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
    }

    @PerProvider
    void timestampAllMonthsHttpDate(JsonSerdeProvider provider) {
        for (int month = 1; month <= 12; month++) {
            Instant instant = LocalDate.of(2025, month, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
            SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT, "httpDate", instant);
            assertThat(roundtrip(provider, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
        }
    }

    @PerProvider
    void timestampAllDaysOfWeekHttpDate(JsonSerdeProvider provider) {
        // 2025-01-06 = Monday, 06-12 covers Mon-Sun
        for (int day = 6; day <= 12; day++) {
            Instant instant = LocalDate.of(2025, 1, day).atStartOfDay(ZoneOffset.UTC).toInstant();
            SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT, "httpDate", instant);
            assertThat(roundtrip(provider, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
        }
    }

    // --- Collection Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void emptyCollections(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct original = complex("empty",
                                        "tags", listOf(),
                                        "intList", listOf(),
                                        "metadata", mapOf(),
                                        "intMap", mapOf());
        assertThat(roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT))).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void sparseCollectionsRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        Map<String, String> sparseMap = new HashMap<>();
        sparseMap.put("present", "value");
        sparseMap.put("absent", null);
        SchemaStruct original = complex("sparse",
                                        "sparseStrings", Arrays.asList("a", null, "c", null, "e"),
                                        "sparseMap", sparseMap);
        assertThat(roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT))).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void nestedStructCollections(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct nested1 = nested("first", 1);
        SchemaStruct nested2 = nested("second", 2);
        SchemaStruct original = builder(BenchModel.COMPLEX_STRUCT)
            .set("id", "nested-collections")
            .set("count", 0)
            .set("nested", nested1)
            .set("structList", listOf(nested1, nested2))
            .set("structMap", mapOf("a", nested1, "b", nested2))
            .build();
        assertThat(roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT))).isEqualTo(original);
    }

    // --- Union Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void unionVariantsRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        for (SchemaStruct choice : listOf(
            struct(BenchModel.BENCH_UNION, "stringValue", "hello"),
            struct(BenchModel.BENCH_UNION, "intValue", 42),
            struct(BenchModel.BENCH_UNION, "structValue", nested("in-union", 99)))) {
            SchemaStruct original = complex("u", "choice", choice);
            assertThat(roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT))).isEqualTo(original);
        }
    }

    // --- Enum Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void enumAllVariantsRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        for (String color : listOf("RED", "GREEN", "BLUE", "YELLOW")) {
            SchemaStruct original = complex("enum", "color", color);
            assertThat(roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT))).isEqualTo(original);
        }
    }

    // --- JsonName Trait ---

    @PerProvider
    void jsonNameTraitSerialization(JsonSerdeProvider provider) {
        SchemaStruct original = struct(BenchModel.JSON_NAME_STRUCT,
                                       "id", "test-id",
                                       "displayName", "Test Name",
                                       "normalField", "normal");

        // With useJsonName=true: verify field names use trait values
        try (JsonCodec codec = JsonCodec.builder()
                                        .overrideSerdeProvider(provider)
                                        .useJsonName(true)
                                        .useTimestampFormat(true)
                                        .build()) {
            ByteBuffer serialized = codec.serialize(original);
            String json = StandardCharsets.UTF_8.decode(serialized.duplicate()).toString();
            assertThat(json).contains("\"ID\"").contains("\"DisplayName\"").contains("\"normalField\"");
            byte[] bytes = new byte[serialized.remaining()];
            serialized.get(bytes);
            assertThat(codec.deserializeShape(bytes, builder(BenchModel.JSON_NAME_STRUCT))).isEqualTo(original);
        }

        // With useJsonName=false: verify field names use member names
        try (JsonCodec codec = JsonCodec.builder()
                                        .overrideSerdeProvider(provider)
                                        .useJsonName(false)
                                        .useTimestampFormat(true)
                                        .build()) {
            ByteBuffer serialized = codec.serialize(original);
            String json = StandardCharsets.UTF_8.decode(serialized.duplicate()).toString();
            assertThat(json).contains("\"id\"").contains("\"displayName\"");
            byte[] bytes = new byte[serialized.remaining()];
            serialized.get(bytes);
            assertThat(codec.deserializeShape(bytes, builder(BenchModel.JSON_NAME_STRUCT))).isEqualTo(original);
        }
    }

    // --- Recursive / Nested Depth ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void recursiveStructRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct current = struct(BenchModel.RECURSIVE_STRUCT, "value", "leaf");
        for (int i = 9; i >= 1; i--) {
            current = struct(BenchModel.RECURSIVE_STRUCT, "value", "level-" + i, "child", current);
        }
        assertThat(roundtrip(ser, de, current, builder(BenchModel.RECURSIVE_STRUCT))).isEqualTo(current);
    }

    // --- Blob Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void blobRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        // Empty blob
        SchemaStruct empty = struct(BenchModel.BLOB_STRUCT, "data", ByteBuffer.wrap(new byte[0]));
        assertThat(roundtrip(ser, de, empty, builder(BenchModel.BLOB_STRUCT))).isEqualTo(empty);

        // Small blob
        SchemaStruct small = struct(BenchModel.BLOB_STRUCT,
                                    "data", ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        assertThat(roundtrip(ser, de, small, builder(BenchModel.BLOB_STRUCT))).isEqualTo(small);

        // Large blob (1000 bytes)
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        SchemaStruct large = struct(BenchModel.BLOB_STRUCT, "data", ByteBuffer.wrap(largeData));
        assertThat(roundtrip(ser, de, large, builder(BenchModel.BLOB_STRUCT))).isEqualTo(large);
    }

    // --- Null optional members ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void nullOptionalMembersRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct original = struct(BenchModel.NUMERIC_STRUCT);
        assertThat(roundtrip(ser, de, original, builder(BenchModel.NUMERIC_STRUCT))).isEqualTo(original);
    }

    @PerProvider
    void emptyBodyDeserializesToDefaultStruct(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        SchemaStruct result = codec.deserializeShape(new byte[0], builder(BenchModel.NUMERIC_STRUCT));
        assertThat(result).isEqualTo(struct(BenchModel.NUMERIC_STRUCT));
    }

    // --- Fields in non-schema order (exercises hash lookup slow path) ---

    @PerProvider
    void fieldsInReverseOrder(JsonSerdeProvider provider) {
        String json = "{\"normalField\":\"c\",\"DisplayName\":\"b\",\"ID\":\"a\"}";
        try (JsonCodec codec = JsonCodec.builder()
                                        .overrideSerdeProvider(provider)
                                        .useJsonName(true)
                                        .useTimestampFormat(true)
                                        .build()) {
            SchemaStruct result = codec.deserializeShape(json.getBytes(StandardCharsets.UTF_8),
                                                         builder(BenchModel.JSON_NAME_STRUCT));
            assertThat(result).isEqualTo(struct(BenchModel.JSON_NAME_STRUCT,
                                                "id", "a",
                                                "displayName", "b",
                                                "normalField", "c"));
        }
    }

    // --- Unknown fields skipped ---

    @PerProvider
    void unknownFieldsSkipped(JsonSerdeProvider provider) {
        String json = "{\"value\":\"hello\",\"unknownField\":42,\"anotherUnknown\":[1,2,3]}";
        try (JsonCodec codec = JsonCodec.builder()
                                        .overrideSerdeProvider(provider)
                                        .useJsonName(true)
                                        .useTimestampFormat(true)
                                        .build()) {
            assertThat(codec.deserializeShape(json.getBytes(StandardCharsets.UTF_8), builder(BenchModel.STRING_STRUCT)))
                .isEqualTo(struct(BenchModel.STRING_STRUCT, "value", "hello"));
        }
    }

    // --- All-types list roundtrip (exercises ListElementSerializer for each type) ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void allListTypesRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de) {
        SchemaStruct original = struct(
            BenchModel.ALL_LISTS_STRUCT,
            "booleans", listOf(true, false, true),
            "bytes", listOf((byte) 1, (byte) -128, (byte) 127),
            "shorts", listOf((short) 1, (short) -32768, (short) 32767),
            "ints", listOf(1, Integer.MIN_VALUE, Integer.MAX_VALUE),
            "longs", listOf(1L, Long.MIN_VALUE, Long.MAX_VALUE),
            "floats", listOf(1.5f, Float.MIN_VALUE, Float.MAX_VALUE),
            "doubles", listOf(1.5, Double.MIN_VALUE, Double.MAX_VALUE),
            "bigInts", listOf(BigInteger.ZERO, new BigInteger("99999999999999999999")),
            "bigDecs", listOf(BigDecimal.ZERO, new BigDecimal("99999.99999")),
            "strings", listOf("hello", "world"),
            "blobs", listOf(ByteBuffer.wrap("a".getBytes(StandardCharsets.UTF_8)),
                            ByteBuffer.wrap("b".getBytes(StandardCharsets.UTF_8))),
            "timestamps", listOf(Instant.EPOCH, Instant.parse("2025-01-15T10:30:00Z")));
        assertThat(roundtrip(ser, de, original, builder(BenchModel.ALL_LISTS_STRUCT))).isEqualTo(original);
    }

    private static String serializeToJson(SerializableShape shape) {
        try (JsonCodec codec = JsonCodec.builder()
                                        .overrideSerdeProvider(SMITHY)
                                        .useJsonName(true)
                                        .useTimestampFormat(true)
                                        .build()) {
            ByteBuffer serialized = codec.serialize(shape);
            byte[] bytes = new byte[serialized.remaining()];
            serialized.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    @Test
    void epochSecondsNegativeWithNanos() {
        // Instant.ofEpochSecond(-1, 500_000_000) = -0.5 seconds from epoch (1969-12-31T23:59:59.5Z)
        // The wire format must be -0.5, NOT -1.5
        SchemaStruct original = struct(BenchModel.TIMESTAMP_STRUCT,
                                       "epochSeconds", Instant.ofEpochSecond(-1, 500_000_000));
        String json = serializeToJson(original);
        assertThat(json).contains("-0.5");
        assertThat(json).doesNotContain("-1.5");

        // Roundtrip must recover the original Instant
        assertThat(roundtrip(SMITHY, original, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(original);
    }

    @Test
    void epochSecondsNegativeWithVariousNanos() {
        // -1 epoch + 100_000_000 nanos = -0.9 seconds
        SchemaStruct neg09 = struct(BenchModel.TIMESTAMP_STRUCT, "epochSeconds", Instant.ofEpochSecond(-1, 100_000_000));
        assertThat(serializeToJson(neg09)).contains("-0.9");
        assertThat(roundtrip(SMITHY, neg09, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(neg09);

        // -1 epoch + 999_000_000 nanos = -0.001 seconds
        SchemaStruct neg0001 = struct(BenchModel.TIMESTAMP_STRUCT,
                                      "epochSeconds", Instant.ofEpochSecond(-1, 999_000_000));
        assertThat(serializeToJson(neg0001)).contains("-0.001");
        assertThat(roundtrip(SMITHY, neg0001, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(neg0001);

        // -2 epoch + 500_000_000 nanos = -1.5 seconds
        SchemaStruct neg15 = struct(BenchModel.TIMESTAMP_STRUCT, "epochSeconds", Instant.ofEpochSecond(-2, 500_000_000));
        assertThat(serializeToJson(neg15)).contains("-1.5");
        assertThat(roundtrip(SMITHY, neg15, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(neg15);
    }

    @Test
    void epochSecondsPreservesSubMillisecondPrecision() {
        // Full nanosecond precision: 123_456_789 nanos should NOT be truncated to 123 millis
        SchemaStruct fullNano = struct(BenchModel.TIMESTAMP_STRUCT,
                                       "epochSeconds", Instant.ofEpochSecond(1000, 123_456_789));
        String json = serializeToJson(fullNano);
        assertThat(json).contains("1000.123456789");
        assertThat(roundtrip(SMITHY, fullNano, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(fullNano);

        // Microsecond precision: trailing zeros stripped
        SchemaStruct microSec = struct(BenchModel.TIMESTAMP_STRUCT,
                                       "epochSeconds", Instant.ofEpochSecond(1000, 123_456_000));
        assertThat(serializeToJson(microSec)).contains("1000.123456");
        assertThat(roundtrip(SMITHY, microSec, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(microSec);

        // Single nanosecond
        SchemaStruct oneNano = struct(BenchModel.TIMESTAMP_STRUCT, "epochSeconds", Instant.ofEpochSecond(1000, 1));
        assertThat(serializeToJson(oneNano)).contains("1000.000000001");
        assertThat(roundtrip(SMITHY, oneNano, builder(BenchModel.TIMESTAMP_STRUCT))).isEqualTo(oneNano);
    }

    @ParameterizedTest
    @MethodSource
    void nonStructDocumentRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de, Document original) {
        try (JsonCodec serCodec = JsonCodec.builder()
                                           .overrideSerdeProvider(ser)
                                           .useJsonName(true)
                                           .useTimestampFormat(true)
                                           .build();
             JsonCodec deCodec = JsonCodec.builder()
                                          .overrideSerdeProvider(de)
                                          .useJsonName(true)
                                          .useTimestampFormat(true)
                                          .build()) {
            ByteBuffer serialized = serCodec.serialize(original);
            Document result = deCodec.createDeserializer(serialized).readDocument();
            assertThat(Document.equals(result, original)).isTrue();
        }
    }

    static Stream<Arguments> nonStructDocumentRoundtrip() {
        List<Document> documents = listOf(
            Document.of("hello world"),
            Document.of(true),
            Document.of(false),
            Document.of(42),
            Document.of(Long.MAX_VALUE),
            Document.of(3.14),
            Document.of(TestCollections.<Document>listOf()),
            Document.of(listOf(Document.of(1), Document.of("two"), Document.of(true))),
            Document.of(TestCollections.<String, Document>mapOf()),
            Document.of(mapOf("key1", Document.of("value1"), "key2", Document.of(42))),
            Document.of(mapOf(
                "list",
                Document.of(listOf(Document.of(1), Document.of(2))),
                "nested",
                Document.of(mapOf("inner", Document.of("value"))),
                "scalar",
                Document.of("hello"))));
        return crossProviders().stream()
                               .flatMap(cp -> documents.stream()
                                                       .map(doc -> Arguments.of(cp.get()[0], cp.get()[1], doc)));
    }

    @ParameterizedTest
    @MethodSource
    void nonStructDocumentInStructRoundtrip(JsonSerdeProvider ser, JsonSerdeProvider de, Document freeform) {
        SchemaStruct original = builder(BenchModel.COMPLEX_STRUCT)
            .set("id", "doc-test")
            .set("count", 1)
            .set("nested", nested("f", 0))
            .set("freeformData", freeform)
            .build();
        SchemaStruct result = roundtrip(ser, de, original, builder(BenchModel.COMPLEX_STRUCT));
        assertThat(Document.equals(result.get("freeformData"), freeform)).isTrue();
    }

    static Stream<Arguments> nonStructDocumentInStructRoundtrip() {
        List<Document> documents = listOf(
            Document.of("a string"),
            Document.of(true),
            Document.of(42),
            Document.of(Long.MAX_VALUE),
            Document.of(3.14),
            Document.of(listOf(Document.of(1), Document.of("two"))),
            Document.of(mapOf("key", Document.of("value"))),
            Document.of(mapOf(
                "list",
                Document.of(listOf(Document.of(1), Document.of(2))),
                "nested",
                Document.of(mapOf("inner", Document.of("value"))))));
        return crossProviders().stream()
                               .flatMap(cp -> documents.stream()
                                                       .map(doc -> Arguments.of(cp.get()[0], cp.get()[1], doc)));
    }

    @ParameterizedTest
    @MethodSource
    void invalidInputs(JsonSerdeProvider provider, String input, boolean isB64) {
        byte[] bytes;
        if (isB64) {
            bytes = Base64.getDecoder().decode(input);
        } else {
            bytes = input.getBytes(StandardCharsets.UTF_8);
        }
        JsonCodec codec = codec(provider);
        assertThrows(SerializationException.class,
                     () -> codec.deserializeShape(bytes, builder(BenchModel.NESTED_STRUCT)));
    }

    static Stream<Arguments> invalidInputs() {
        List<Arguments> arguments = new ArrayList<>();
        for (JsonSerdeProvider provider : providerInstances()) {
            arguments.add(Arguments.of(provider,
                                       "eyJ2YWx1ZSI6e2JpdWUiOntiaW50VmE6e2JpbnRWYXJpnm50VmE6e2JpbnRWYXJpnpF0IjotMjA3fQ==",
                                       true));
            arguments.add(Arguments.of(provider, "{\"value\":[true", false));
        }
        return arguments.stream();
    }
}
