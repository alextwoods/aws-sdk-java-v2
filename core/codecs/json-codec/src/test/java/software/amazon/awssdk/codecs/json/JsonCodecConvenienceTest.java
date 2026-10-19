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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Tests the {@code Codec} convenience methods ({@code serialize} and {@code deserializeShape}) on {@link JsonCodec}
 * (Requirement 39).
 */
class JsonCodecConvenienceTest {
    private static final JsonCodec CODEC = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();
    private static final String SHAPE_ID = BenchModel.NS + "#NestedStruct";

    @Test
    void serialize_struct_matchesSerializerPathBytes() {
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = CODEC.createSerializer(expected)) {
            value().serialize(serializer);
        }

        assertThat(bytes(CODEC.serialize(value()))).isEqualTo(expected.toByteArray());
    }

    @Test
    void deserializeShape_serializedBytes_matchesDeserializerPathAndOriginal() {
        byte[] bytes = bytes(CODEC.serialize(value()));
        SchemaStruct expected;
        try (ShapeDeserializer deserializer = CODEC.createDeserializer(bytes)) {
            expected = SchemaStruct.builder(BenchModel.NESTED_STRUCT).deserialize(deserializer).errorCorrection().build();
        }

        SchemaStruct actual = CODEC.deserializeShape(bytes, SchemaStruct.builder(BenchModel.NESTED_STRUCT));

        assertThat(actual).isEqualTo(expected).isEqualTo(value());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    void deserializeShape_byteBuffer_readsRemainingBytesWithoutMovingPosition(String name,
                                                                              Function<byte[], ByteBuffer> wrap) {
        ByteBuffer source = wrap.apply(bytes(CODEC.serialize(value())));
        int position = source.position();

        SchemaStruct actual = CODEC.deserializeShape(source, SchemaStruct.builder(BenchModel.NESTED_STRUCT));

        assertThat(actual).isEqualTo(value());
        assertThat(source.position()).isEqualTo(position);
    }

    @Test
    void convenienceMethods_nullArguments_throwNamingParameter() {
        assertThatThrownBy(() -> CODEC.serialize(null))
            .isInstanceOf(NullPointerException.class).hasMessage("shape must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape((byte[]) null, SchemaStruct.builder(BenchModel.NESTED_STRUCT)))
            .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape((ByteBuffer) null,
                                                        SchemaStruct.builder(BenchModel.NESTED_STRUCT)))
            .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape(new byte[0], null))
            .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape(ByteBuffer.allocate(0), null))
            .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"field1\": \"a", "{\"field1\": \"a\",", "{\"field2\": \"x\"}", "[]", "nope"})
    void deserializeShape_malformedTruncatedOrMistyped_throwsSerializationExceptionNamingShapeId(String json) {
        assertRejected(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void deserializeShape_trailingContent_throwsSerializationExceptionNamingShapeId() {
        assertRejected(concat(bytes(CODEC.serialize(value())), " {}".getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertRejected(byte[] bytes) {
        assertThatThrownBy(() -> CODEC.deserializeShape(bytes, SchemaStruct.builder(BenchModel.NESTED_STRUCT)))
            .isInstanceOf(SerializationException.class).hasMessageContaining(SHAPE_ID);
        assertThatThrownBy(() -> CODEC.deserializeShape(ByteBuffer.wrap(bytes),
                                                        SchemaStruct.builder(BenchModel.NESTED_STRUCT)))
            .isInstanceOf(SerializationException.class).hasMessageContaining(SHAPE_ID);
    }

    private static Stream<Arguments> buffers() {
        Function<byte[], ByteBuffer> offset = b -> {
            ByteBuffer buffer = ByteBuffer.wrap(concat(concat("[[[".getBytes(StandardCharsets.UTF_8), b),
                                                       "]]]".getBytes(StandardCharsets.UTF_8)));
            buffer.position(3);
            buffer.limit(3 + b.length);
            return buffer;
        };
        Function<byte[], ByteBuffer> readOnly = b -> offset.apply(b).asReadOnlyBuffer();
        Function<byte[], ByteBuffer> direct = b -> {
            ByteBuffer buffer = ByteBuffer.allocateDirect(b.length + 2);
            buffer.put((byte) '[').put(b).flip();
            buffer.position(1);
            return buffer;
        };
        return Stream.of(Arguments.of("non-zero position and limit", offset),
                         Arguments.of("read-only", readOnly),
                         Arguments.of("direct", direct));
    }

    private static SchemaStruct value() {
        SchemaStruct inner = SchemaStruct.builder(BenchModel.INNER_STRUCT)
                                         .set("value", "inner \u00e9")
                                         .set("numbers", Arrays.asList(1, 2, 3))
                                         .build();
        return SchemaStruct.builder(BenchModel.NESTED_STRUCT)
                           .set("field1", "outer")
                           .set("field2", 42)
                           .set("inner", inner)
                           .build();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
