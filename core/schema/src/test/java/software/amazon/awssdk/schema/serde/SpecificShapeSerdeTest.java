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

package software.amazon.awssdk.schema.serde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Tests for {@link SpecificShapeSerializer}, {@link SpecificShapeDeserializer} and {@link InterceptingSerializer}.
 * smithy-java has no tests of its own for these types.
 */
class SpecificShapeSerdeTest {

    private static final Schema STRUCT = PreludeSchemas.UNIT;

    @Test
    void specificSerializerRejectsEveryUnimplementedWrite() {
        SpecificShapeSerializer serializer = new SpecificShapeSerializer() {
        };
        List<Consumer<ShapeSerializer>> writes = Arrays.asList(
            s -> s.writeStruct(STRUCT, TestHelper.create(STRUCT, (schema, ser) -> { })),
            s -> s.writeList(PreludeSchemas.STRING, null, 0, (state, ls) -> { }),
            s -> s.writeMap(PreludeSchemas.STRING, null, 0, (state, ms) -> { }),
            s -> s.writeBoolean(PreludeSchemas.STRING, true),
            s -> s.writeByte(PreludeSchemas.STRING, (byte) 1),
            s -> s.writeShort(PreludeSchemas.STRING, (short) 1),
            s -> s.writeInteger(PreludeSchemas.STRING, 1),
            s -> s.writeLong(PreludeSchemas.STRING, 1L),
            s -> s.writeFloat(PreludeSchemas.STRING, 1f),
            s -> s.writeDouble(PreludeSchemas.STRING, 1d),
            s -> s.writeBigInteger(PreludeSchemas.STRING, BigInteger.ONE),
            s -> s.writeBigDecimal(PreludeSchemas.STRING, BigDecimal.ONE),
            s -> s.writeString(PreludeSchemas.STRING, "x"),
            s -> s.writeBlob(PreludeSchemas.STRING, ByteBuffer.allocate(0)),
            s -> s.writeTimestamp(PreludeSchemas.STRING, Instant.EPOCH),
            s -> s.writeDocument(PreludeSchemas.STRING, Document.of("x")));

        for (Consumer<ShapeSerializer> write : writes) {
            assertThatThrownBy(() -> write.accept(serializer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unexpected schema type: ");
        }
        assertThatThrownBy(() -> serializer.writeNull(PreludeSchemas.STRING))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unexpected null value written for " + PreludeSchemas.STRING);
    }

    @Test
    void specificSerializerUsesTheOverriddenException() {
        SpecificShapeSerializer serializer = new SpecificShapeSerializer() {
            @Override
            protected RuntimeException throwForInvalidState(String message, Schema schema) {
                return new SerializationException("custom: " + schema.id());
            }
        };

        assertThatThrownBy(() -> serializer.writeInteger(PreludeSchemas.INTEGER, 1))
            .isInstanceOf(SerializationException.class)
            .hasMessage("custom: smithy.api#Integer");
        assertThatThrownBy(() -> serializer.writeNull(PreludeSchemas.INTEGER))
            .isInstanceOf(SerializationException.class)
            .hasMessage("custom: smithy.api#Integer");
    }

    @Test
    void specificDeserializerRejectsEveryUnimplementedReadWithTheShapeId() {
        SpecificShapeDeserializer deserializer = new SpecificShapeDeserializer() {
        };
        Schema schema = PreludeSchemas.TIMESTAMP;
        List<Consumer<ShapeDeserializer>> reads = Arrays.asList(
            d -> d.readBoolean(schema),
            d -> d.readBlob(schema),
            d -> d.readByte(schema),
            d -> d.readShort(schema),
            d -> d.readInteger(schema),
            d -> d.readLong(schema),
            d -> d.readFloat(schema),
            d -> d.readDouble(schema),
            d -> d.readBigInteger(schema),
            d -> d.readBigDecimal(schema),
            d -> d.readString(schema),
            d -> d.readTimestamp(schema),
            d -> d.readStruct(schema, null, (state, member, md) -> { }),
            d -> d.readList(schema, null, (state, md) -> { }),
            d -> d.readStringMap(schema, null, (state, key, md) -> { }));

        for (Consumer<ShapeDeserializer> read : reads) {
            assertThatThrownBy(() -> read.accept(deserializer))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("smithy.api#Timestamp");
        }
        assertThatThrownBy(deserializer::readDocument)
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#Document");
        assertThatThrownBy(deserializer::isNull).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void specificDeserializerReadsOverriddenTypes() {
        SpecificShapeDeserializer deserializer = new SpecificShapeDeserializer() {
            @Override
            public String readString(Schema schema) {
                return "value";
            }
        };

        assertThat(deserializer.readString(PreludeSchemas.STRING)).isEqualTo("value");
        assertThatThrownBy(() -> deserializer.readInteger(PreludeSchemas.INTEGER))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void interceptingSerializerRoutesEachWriteAndCallsAfter() {
        List<String> events = new ArrayList<>();
        ShapeSerializer target = new SpecificShapeSerializer() {
            @Override
            public void writeString(Schema schema, String value) {
                events.add("write " + value);
            }

            @Override
            public void writeNull(Schema schema) {
                events.add("null");
            }
        };
        InterceptingSerializer serializer = new InterceptingSerializer() {
            @Override
            protected ShapeSerializer before(Schema schema) {
                events.add("before " + schema.id().getName());
                return target;
            }

            @Override
            protected void after(Schema schema) {
                events.add("after " + schema.id().getName());
            }
        };

        serializer.writeString(PreludeSchemas.STRING, "a");
        serializer.writeNull(PreludeSchemas.STRING);

        assertThat(events).containsExactly("before String", "write a", "after String",
                                           "before String", "null", "after String");
    }

    @Test
    void interceptingSerializerSkipsAfterWhenTheWriteFails() {
        List<String> events = new ArrayList<>();
        InterceptingSerializer serializer = new InterceptingSerializer() {
            @Override
            protected ShapeSerializer before(Schema schema) {
                return new SpecificShapeSerializer() {
                };
            }

            @Override
            protected void after(Schema schema) {
                events.add("after");
            }
        };

        assertThatThrownBy(() -> serializer.writeInteger(PreludeSchemas.INTEGER, 1))
            .isInstanceOf(IllegalStateException.class);
        assertThat(events).isEmpty();
    }
}
