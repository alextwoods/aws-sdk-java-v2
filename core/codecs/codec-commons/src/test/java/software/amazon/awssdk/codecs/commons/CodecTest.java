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

package software.amazon.awssdk.codecs.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;

/**
 * Tests the {@link Codec} default methods with a minimal codec whose format is a single UTF-8 string. smithy-java has no
 * tests for {@code Codec} itself; its codec tests are ported with each codec.
 */
class CodecTest {
    private static final Codec CODEC = new Utf8StringCodec();

    @Test
    void serialize_shape_matchesSerializerPathBytes() {
        TextShape shape = new TextShape("h\u00e9llo"); // non-ASCII, so the bytes are UTF-8 specific
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = CODEC.createSerializer(expected)) {
            shape.serialize(serializer);
        }

        ByteBuffer serialized = CODEC.serialize(shape);

        assertThat(bytes(serialized)).isEqualTo(expected.toByteArray());
        assertThat(serialized.hasArray()).isTrue();
    }

    @Test
    void serializeToString_shape_decodesSerializedBytesAsUtf8() {
        TextShape shape = new TextShape("h\u00e9llo"); // non-ASCII, so the decoding is UTF-8 specific

        assertThat(CODEC.serializeToString(shape)).isEqualTo("h\u00e9llo"); // same text back
    }

    @Test
    void serialize_serializerNotClosedByShape_closesSerializerToFlushOutput() {
        BufferingCodec codec = new BufferingCodec();

        assertThat(bytes(codec.serialize(new TextShape("abc")))).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
        assertThat(codec.serializeToString(new TextShape("xyz"))).isEqualTo("xyz");
    }

    @Test
    void deserializeShape_allSourceTypes_buildErrorCorrectedShape() {
        byte[] data = "value".getBytes(StandardCharsets.UTF_8);

        TextShape fromBytes = CODEC.deserializeShape(data, new TextShape.Builder());
        TextShape fromBuffer = CODEC.deserializeShape(ByteBuffer.wrap(data), new TextShape.Builder());
        TextShape fromString = CODEC.deserializeShape("value", new TextShape.Builder());

        for (TextShape shape : new TextShape[] {fromBytes, fromBuffer, fromString}) {
            assertThat(shape.value).isEqualTo("value");
            assertThat(shape.errorCorrected).isTrue();
        }
    }

    @Test
    void createDeserializer_byteArray_wrapsArrayWithoutCopy() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);

        Utf8StringDeserializer deserializer = (Utf8StringDeserializer) CODEC.createDeserializer(data);

        assertThat(deserializer.source.array()).isSameAs(data);
        assertThat(deserializer.source.remaining()).isEqualTo(3);
    }

    @Test
    void deserializeShape_slicedBuffer_readsOnlyRemainingBytes() {
        ByteBuffer source = ByteBuffer.wrap("[[inner]]".getBytes(StandardCharsets.UTF_8), 2, 5);

        assertThat(CODEC.deserializeShape(source, new TextShape.Builder()).value).isEqualTo("inner");
    }

    @Test
    void deserializeShape_deserializerThrows_propagatesWithoutBuilding() {
        TextShape.Builder builder = new TextShape.Builder();
        Codec failing = new Utf8StringCodec() {
            @Override
            public ShapeDeserializer createDeserializer(ByteBuffer source) {
                return new SpecificShapeDeserializer() {
                };
            }
        };

        assertThatThrownBy(() -> failing.deserializeShape("x", builder))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#String");
        assertThat(builder.built).isFalse();
    }

    @Test
    void deserializeShape_success_closesDeserializerAfterBuilding() {
        ScriptedDeserializer deserializer = new ScriptedDeserializer(null, null);
        TextShape.Builder builder = new TextShape.Builder();

        TextShape shape = codecFor(deserializer).deserializeShape(new byte[0], builder);

        assertThat(shape.value).isEqualTo("scripted");
        assertThat(deserializer.closed).isTrue();
    }

    @Test
    void deserializeShape_closeRejectsTrailingContent_throwsNamingShapeIdWithCause() {
        SerializationException trailing = new SerializationException("Unexpected content after the value");
        Codec codec = codecFor(new ScriptedDeserializer(null, trailing));
        TextShape.Builder builder = new TextShape.Builder();

        assertThatThrownBy(() -> codec.deserializeShape(ByteBuffer.allocate(0), builder))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unable to deserialize smithy.api#String: Unexpected content after the value")
            .hasCause(trailing);
    }

    @Test
    void deserializeShape_readFails_closesQuietlyAndBuildsNothing() {
        SerializationException readFailure = new SerializationException("Unexpected end of input");
        SerializationException closeFailure = new SerializationException("incomplete value");
        ScriptedDeserializer deserializer = new ScriptedDeserializer(readFailure, closeFailure);
        TextShape.Builder builder = new TextShape.Builder();

        assertThatThrownBy(() -> codecFor(deserializer).deserializeShape(new byte[0], builder))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unable to deserialize smithy.api#String: Unexpected end of input")
            .hasCause(readFailure);
        assertThat(deserializer.closed).isTrue();
        assertThat(readFailure.getSuppressed()).containsExactly(closeFailure);
        assertThat(builder.built).isFalse();
    }

    @Test
    void deserializeShape_createDeserializerFails_throwsNamingShapeId() {
        Codec failing = new Utf8StringCodec() {
            @Override
            public ShapeDeserializer createDeserializer(ByteBuffer source) {
                throw new SerializationException("illegal first byte");
            }
        };

        assertThatThrownBy(() -> failing.deserializeShape("x", new TextShape.Builder()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unable to deserialize smithy.api#String: illegal first byte");
    }

    @Test
    void deserializeShape_otherRuntimeException_propagatesUnchanged() {
        IllegalStateException failure = new IllegalStateException("builder bug");
        ScriptedDeserializer deserializer = new ScriptedDeserializer(failure, null);

        assertThatThrownBy(() -> codecFor(deserializer).deserializeShape(new byte[0], new TextShape.Builder()))
            .isSameAs(failure);
        assertThat(deserializer.closed).isTrue();
    }

    @Test
    void convenienceMethods_nullArguments_throwNamingParameter() {
        TextShape.Builder builder = new TextShape.Builder();

        assertThatThrownBy(() -> CODEC.serialize(null)).isInstanceOf(NullPointerException.class)
                                                       .hasMessage("shape must not be null");
        assertThatThrownBy(() -> CODEC.serializeToString(null)).isInstanceOf(NullPointerException.class)
                                                               .hasMessage("shape must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape((byte[]) null, builder))
            .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape((ByteBuffer) null, builder))
            .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape((String) null, builder))
            .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape(new byte[0], null))
            .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape(ByteBuffer.allocate(0), null))
            .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
        assertThatThrownBy(() -> CODEC.deserializeShape("", null))
            .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
    }

    @Test
    void close_default_doesNothing() {
        Codec codec = new Utf8StringCodec();
        codec.close();

        assertThat(codec.serializeToString(new TextShape("still usable"))).isEqualTo("still usable");
    }

    @Test
    void defaultMethods_sharedCodecAcrossThreads_eachThreadGetsItsOwnResult() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                String value = "thread-" + t;
                Callable<Boolean> task = () -> {
                    for (int i = 0; i < 500; i++) {
                        String text = value + "-" + i;
                        ByteBuffer serialized = CODEC.serialize(new TextShape(text));
                        if (!CODEC.deserializeShape(serialized, new TextShape.Builder()).value.equals(text)
                            || !CODEC.serializeToString(new TextShape(text)).equals(text)) {
                            return false;
                        }
                    }
                    return true;
                };
                futures.add(executor.submit(task));
            }
            for (Future<Boolean> future : futures) {
                assertThat(future.get(30, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }

    private static Codec codecFor(ScriptedDeserializer deserializer) {
        return new Utf8StringCodec() {
            @Override
            public ShapeDeserializer createDeserializer(ByteBuffer source) {
                return deserializer;
            }
        };
    }

    /**
     * A deserializer that reads {@code "scripted"} or throws {@code readFailure}, and whose {@code close} records the
     * call and then throws {@code closeFailure} if it is set.
     */
    private static final class ScriptedDeserializer extends SpecificShapeDeserializer {
        private final RuntimeException readFailure;
        private final RuntimeException closeFailure;
        private boolean closed;

        private ScriptedDeserializer(RuntimeException readFailure, RuntimeException closeFailure) {
            this.readFailure = readFailure;
            this.closeFailure = closeFailure;
        }

        @Override
        public String readString(Schema schema) {
            if (readFailure != null) {
                throw readFailure;
            }
            return "scripted";
        }

        @Override
        public void close() {
            closed = true;
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    /**
     * A codec whose format is one UTF-8 string.
     */
    private static class Utf8StringCodec implements Codec {
        @Override
        public ShapeSerializer createSerializer(OutputStream sink) {
            return new SpecificShapeSerializer() {
                @Override
                public void writeString(Schema schema, String value) {
                    write(sink, value.getBytes(StandardCharsets.UTF_8));
                }
            };
        }

        @Override
        public ShapeDeserializer createDeserializer(ByteBuffer source) {
            return new Utf8StringDeserializer(source);
        }
    }

    /**
     * A codec whose serializer only writes to the sink when it is flushed, like a buffering codec.
     */
    private static final class BufferingCodec extends Utf8StringCodec {
        @Override
        public ShapeSerializer createSerializer(OutputStream sink) {
            return new SpecificShapeSerializer() {
                private byte[] pending = new byte[0];

                @Override
                public void writeString(Schema schema, String value) {
                    pending = value.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public void flush() {
                    write(sink, pending);
                    pending = new byte[0];
                }
            };
        }
    }

    private static void write(OutputStream sink, byte[] bytes) {
        try {
            sink.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class Utf8StringDeserializer extends SpecificShapeDeserializer {
        private final ByteBuffer source;

        private Utf8StringDeserializer(ByteBuffer source) {
            this.source = source;
        }

        @Override
        public String readString(Schema schema) {
            return ByteBufferUtils.getUTF8String(source);
        }
    }

    private static final class TextShape implements SerializableShape {
        private final String value;
        private final boolean errorCorrected;

        private TextShape(String value) {
            this(value, false);
        }

        private TextShape(String value, boolean errorCorrected) {
            this.value = value;
            this.errorCorrected = errorCorrected;
        }

        @Override
        public void serialize(ShapeSerializer encoder) {
            encoder.writeString(PreludeSchemas.STRING, value);
        }

        private static final class Builder implements ShapeBuilder<TextShape> {
            private String value;
            private boolean errorCorrected;
            private boolean built;

            @Override
            public TextShape build() {
                built = true;
                return new TextShape(value, errorCorrected);
            }

            @Override
            public ShapeBuilder<TextShape> deserialize(ShapeDeserializer decoder) {
                value = decoder.readString(PreludeSchemas.STRING);
                return this;
            }

            @Override
            public ShapeBuilder<TextShape> errorCorrection() {
                errorCorrected = true;
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return PreludeSchemas.STRING;
            }
        }
    }
}
