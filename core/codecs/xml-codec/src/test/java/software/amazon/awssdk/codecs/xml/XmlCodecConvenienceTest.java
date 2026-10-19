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

package software.amazon.awssdk.codecs.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Tests the {@code Codec} convenience methods ({@code serialize} and {@code deserializeShape}) on {@link XmlCodec}
 * (Requirement 39), with both XML implementations.
 */
class XmlCodecConvenienceTest extends ProviderTestBase {
    private static final String SHAPE_ID = XmlTestModel.NS + "#NestedStruct";

    @PerProvider
    void serialize_struct_matchesSerializerPathBytes(boolean useNative) {
        try (XmlCodec codec = newCodec(useNative)) {
            ByteArrayOutputStream expected = new ByteArrayOutputStream();
            try (ShapeSerializer serializer = codec.createSerializer(expected)) {
                value().serialize(serializer);
            }

            assertThat(bytes(codec.serialize(value()))).isEqualTo(expected.toByteArray());
        }
    }

    @PerProvider
    void deserializeShape_serializedBytes_matchesDeserializerPathAndOriginal(boolean useNative) {
        try (XmlCodec codec = newCodec(useNative)) {
            byte[] bytes = bytes(codec.serialize(value()));
            SchemaStruct expected;
            try (ShapeDeserializer deserializer = codec.createDeserializer(bytes)) {
                expected = SchemaStruct.builder(XmlTestModel.NESTED_STRUCT)
                                       .deserialize(deserializer).errorCorrection().build();
            }

            SchemaStruct actual = codec.deserializeShape(bytes, SchemaStruct.builder(XmlTestModel.NESTED_STRUCT));

            assertThat(actual).isEqualTo(expected).isEqualTo(value());
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("buffers")
    void deserializeShape_byteBuffer_readsRemainingBytesWithoutMovingPosition(boolean useNative, String name,
                                                                              Function<byte[], ByteBuffer> wrap) {
        try (XmlCodec codec = newCodec(useNative)) {
            ByteBuffer source = wrap.apply(bytes(codec.serialize(value())));
            int position = source.position();

            SchemaStruct actual = codec.deserializeShape(source, SchemaStruct.builder(XmlTestModel.NESTED_STRUCT));

            assertThat(actual).isEqualTo(value());
            assertThat(source.position()).isEqualTo(position);
        }
    }

    @PerProvider
    void convenienceMethods_nullArguments_throwNamingParameter(boolean useNative) {
        try (XmlCodec codec = newCodec(useNative)) {
            assertThatThrownBy(() -> codec.serialize(null))
                .isInstanceOf(NullPointerException.class).hasMessage("shape must not be null");
            assertThatThrownBy(() -> codec.deserializeShape((byte[]) null,
                                                            SchemaStruct.builder(XmlTestModel.NESTED_STRUCT)))
                .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
            assertThatThrownBy(() -> codec.deserializeShape((ByteBuffer) null,
                                                            SchemaStruct.builder(XmlTestModel.NESTED_STRUCT)))
                .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
            assertThatThrownBy(() -> codec.deserializeShape(new byte[0], null))
                .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
            assertThatThrownBy(() -> codec.deserializeShape(ByteBuffer.allocate(0), null))
                .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("malformed")
    void deserializeShape_malformedTruncatedOrMistyped_throwsSerializationExceptionNamingShapeId(boolean useNative,
                                                                                                 String xml) {
        assertRejected(useNative, xml.getBytes(StandardCharsets.UTF_8));
    }

    @PerProvider
    void deserializeShape_trailingContent_throwsSerializationExceptionNamingShapeId(boolean useNative) {
        byte[] serialized;
        try (XmlCodec codec = newCodec(useNative)) {
            serialized = bytes(codec.serialize(value()));
        }

        assertRejected(useNative, concat(serialized, "<NestedStruct/>".getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertRejected(boolean useNative, byte[] bytes) {
        try (XmlCodec codec = newCodec(useNative)) {
            assertThatThrownBy(() -> codec.deserializeShape(bytes, SchemaStruct.builder(XmlTestModel.NESTED_STRUCT)))
                .isInstanceOf(SerializationException.class).hasMessageContaining(SHAPE_ID);
            assertThatThrownBy(() -> codec.deserializeShape(ByteBuffer.wrap(bytes),
                                                            SchemaStruct.builder(XmlTestModel.NESTED_STRUCT)))
                .isInstanceOf(SerializationException.class).hasMessageContaining(SHAPE_ID);
        }
    }

    private static List<Arguments> buffers() {
        Function<byte[], ByteBuffer> offset = b -> {
            ByteBuffer buffer = ByteBuffer.wrap(concat(concat("<<<".getBytes(StandardCharsets.UTF_8), b),
                                                       "<<<".getBytes(StandardCharsets.UTF_8)));
            buffer.position(3);
            buffer.limit(3 + b.length);
            return buffer;
        };
        Function<byte[], ByteBuffer> readOnly = b -> offset.apply(b).asReadOnlyBuffer();
        Function<byte[], ByteBuffer> direct = b -> {
            ByteBuffer buffer = ByteBuffer.allocateDirect(b.length + 2);
            buffer.put((byte) '<').put(b).flip();
            buffer.position(1);
            return buffer;
        };
        List<Arguments> result = new ArrayList<>();
        for (Arguments provider : providers()) {
            Object useNative = provider.get()[0];
            result.add(Arguments.of(useNative, "non-zero position and limit", offset));
            result.add(Arguments.of(useNative, "read-only", readOnly));
            result.add(Arguments.of(useNative, "direct", direct));
        }
        return result;
    }

    private static List<Arguments> malformed() {
        List<String> inputs = Arrays.asList("<NestedStruct>",
                                            "<NestedStruct><field1>a",
                                            "<NestedStruct><field1>a</field2></NestedStruct>",
                                            "<Other><field1>a</field1></Other>",
                                            "<NestedStruct><field2>x</field2></NestedStruct>",
                                            "nope");
        List<Arguments> result = new ArrayList<>();
        for (Arguments provider : providers()) {
            for (String input : inputs) {
                result.add(Arguments.of(provider.get()[0], Named.of(input, input)));
            }
        }
        return result;
    }

    private static XmlCodec newCodec(boolean useNative) {
        return XmlCodec.builder().settings(XmlSettings.REST_XML).useNative(useNative).build();
    }

    private static SchemaStruct value() {
        SchemaStruct inner = SchemaStruct.builder(XmlTestModel.INNER_STRUCT)
                                         .set("value", "inner \u00e9 <&>")
                                         .set("numbers", Arrays.asList(1, 2, 3))
                                         .build();
        return SchemaStruct.builder(XmlTestModel.NESTED_STRUCT)
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
