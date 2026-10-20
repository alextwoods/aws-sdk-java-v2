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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Tests the native XML serializer's buffer when it writes to a sink: a full buffer is written to the sink rather than
 * grown, and the output does not depend on where the buffer fills.
 *
 * <p>The XML codec has no sink-less path ({@link XmlCodec#serialize} also writes through a sink), so the expected
 * output is assembled from the serializations of one member at a time, each of which fits in the buffer, and from the
 * literal markup of the large string members.
 */
class XmlSerializerSinkBufferTest {

    // The initial buffer of the native XML serializer (SmithyXmlSerializer.DEFAULT_BUF_SIZE).
    private static final int BUFFER_SIZE = 4096;

    private static final Schema ROOT =
        Schema.structureBuilder(XmlTestModel.id("SinkBoundary"))
              .putMember("pad", PreludeSchemas.STRING)
              .putMember("attrs", XmlTestModel.XML_ATTRIBUTE_STRUCT)
              .putMember("numeric", XmlTestModel.NUMERIC_STRUCT)
              .putMember("value", PreludeSchemas.STRING)
              .putMember("blob", PreludeSchemas.BLOB)
              .putMember("names", XmlTestModel.STRING_LIST)
              .putMember("entries", XmlTestModel.STRING_MAP)
              .putMember("namespaced", XmlTestModel.NAMESPACED_STRUCT)
              .putMember("tail", PreludeSchemas.STRING)
              .build();

    private static final XmlCodec CODEC = XmlCodec.builder().useNative(true).build();

    @Test
    void createSerializer_withBodyLargerThanBuffer_writesFullBuffersToSinkWithoutGrowing() {
        List<Object> names = new ArrayList<>();
        StringBuilder expected = new StringBuilder("<SinkBoundary><names>");
        for (int i = 0; i < 4000; i++) {
            String name = "name-" + i + "-\u00e9\u4e2d";
            names.add(name);
            expected.append("<member>").append(name).append("</member>");
        }
        expected.append("</names></SinkBoundary>");
        SchemaStruct value = SchemaStruct.builder(ROOT).set("names", names).build();
        WriteRecordingStream output = new WriteRecordingStream();

        write(value, output);

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(expected.toString());
        assertThat(output.size()).isGreaterThan(100_000);
        assertThat(output.writes).isGreaterThan(1);
        // The serializer's buffer is drained when full rather than grown to the document size.
        assertThat(output.largestWrite).isLessThanOrEqualTo(BUFFER_SIZE);
        // The markup assumed above is what the codec writes for a short list.
        SchemaStruct shortList = SchemaStruct.builder(ROOT).set("names", Arrays.asList("a", "b")).build();
        assertThat(CODEC.serializeToString(shortList))
            .isEqualTo("<SinkBoundary><names><member>a</member><member>b</member></names></SinkBoundary>");
    }

    @Test
    void createSerializer_withValueLargerThanBuffer_drainsThenGrowsForThatValue() {
        String large = repeat("y", BUFFER_SIZE * 5);
        String pad = repeat("z", BUFFER_SIZE - 20);
        SchemaStruct value = SchemaStruct.builder(ROOT).set("pad", pad).set("value", large).set("tail", "t").build();
        ByteBufferOutputStream output = new ByteBufferOutputStream(16);

        try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
            value.serialize(serializer);
        }

        ByteBuffer bytes = output.toByteBuffer();
        assertThat(StandardCharsets.UTF_8.decode(bytes).toString())
            .isEqualTo("<SinkBoundary><pad>" + pad + "</pad><value>" + large + "</value><tail>t</tail></SinkBoundary>");
    }

    @Test
    void createSerializer_withSmallDocument_writesToSinkOnceOnClose() {
        SchemaStruct value = boundaryStruct(10, "value");
        WriteRecordingStream output = new WriteRecordingStream();

        try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
            value.serialize(serializer);
            assertThat(output.writes).isZero();
        }

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(expected(value));
        assertThat(output.writes).isEqualTo(1);
    }

    /**
     * Moves attributes, a structure of every number type, a string of each length below, a blob, a list, a map and a
     * namespaced structure across the end of the buffer one byte at a time, so that every write starts at, ends at and
     * straddles the point where the buffer is drained. The string lengths cover values whose reservation (up to six
     * bytes per character, for escapes) just fits the buffer, just does not, and needs it to grow.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("stringKinds")
    void createSerializer_atEveryBufferBoundary_matchesMemberByMemberOutput(String kind, String unit) {
        int sixth = BUFFER_SIZE / 6;
        int[] valueLengths = {0, 1, sixth - 1, sixth, sixth + 1, BUFFER_SIZE - 1, BUFFER_SIZE, BUFFER_SIZE + 1,
                              BUFFER_SIZE * 2 + 1};
        for (int valueLength : valueLengths) {
            String value = repeat(unit, valueLength);
            // The members after the pad are under 600 bytes, so the drain point passes through every write.
            for (int pad = BUFFER_SIZE - 600; pad <= BUFFER_SIZE + 2; pad++) {
                SchemaStruct struct = boundaryStruct(pad, value);
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                write(struct, output);

                assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                    .as("%s value of %d chars after %d pad chars", kind, valueLength, pad)
                    .isEqualTo(expected(struct));
            }
        }
    }

    static List<Arguments> stringKinds() {
        return Arrays.asList(Arguments.of("ascii", "a"),
                             Arguments.of("escaped", "&"),
                             Arguments.of("two-byte", "\u00e9"),
                             Arguments.of("three-byte", "\u4e2d"),
                             Arguments.of("surrogate pair", "\ud83d\ude00"));
    }

    private static SchemaStruct boundaryStruct(int padLength, String value) {
        Map<String, Object> entries = new LinkedHashMap<>();
        entries.put("k1", "v1");
        entries.put("k&2", "v<2>");
        return SchemaStruct.builder(ROOT)
                           .set("pad", repeat("p", padLength))
                           .set("attrs", SchemaStruct.builder(XmlTestModel.XML_ATTRIBUTE_STRUCT)
                                                     .set("version", "1.0")
                                                     .set("identifier", "id-\"1\"")
                                                     .set("content", "c")
                                                     .build())
                           .set("numeric", SchemaStruct.builder(XmlTestModel.NUMERIC_STRUCT)
                                                       .set("byteVal", (byte) -7)
                                                       .set("shortVal", (short) 300)
                                                       .set("intVal", Integer.MIN_VALUE)
                                                       .set("longVal", Long.MIN_VALUE)
                                                       .set("floatVal", 1.25f)
                                                       .set("doubleVal", -0.1)
                                                       .set("bigIntVal", BigInteger.TEN.pow(30))
                                                       .set("bigDecVal", new BigDecimal("1.5E-20"))
                                                       .build())
                           .set("value", value)
                           .set("blob", ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5}))
                           .set("names", Arrays.asList("a", "b"))
                           .set("entries", entries)
                           .set("namespaced", SchemaStruct.builder(XmlTestModel.NAMESPACED_STRUCT)
                                                          .set("name", "n")
                                                          .set("value", 3)
                                                          .build())
                           .set("tail", "tail")
                           .build();
    }

    /**
     * The expected document: the pad and value members as literal markup, and each other member as the codec writes it
     * in a document of its own, which fits in the buffer.
     */
    private static String expected(SchemaStruct struct) {
        StringBuilder xml = new StringBuilder("<SinkBoundary>");
        for (Schema member : ROOT.members()) {
            String name = member.memberName();
            Object value = struct.get(name);
            if (value == null) {
                continue;
            }
            if (name.equals("pad") || name.equals("value")) {
                xml.append('<').append(name).append('>').append(((String) value).replace("&", "&amp;"))
                   .append("</").append(name).append('>');
            } else {
                String alone = CODEC.serializeToString(SchemaStruct.builder(ROOT).set(name, value).build());
                assertThat(alone).startsWith("<SinkBoundary>").endsWith("</SinkBoundary>").hasSizeLessThan(1000);
                xml.append(alone, "<SinkBoundary>".length(), alone.length() - "</SinkBoundary>".length());
            }
        }
        return xml.append("</SinkBoundary>").toString();
    }

    private static void write(SchemaStruct value, ByteArrayOutputStream output) {
        try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
            value.serialize(serializer);
        }
    }

    private static String repeat(String value, int count) {
        StringBuilder sb = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(value);
        }
        return sb.toString();
    }

    /** Records the number and largest length of the writes it receives. */
    private static final class WriteRecordingStream extends ByteArrayOutputStream {
        private int writes;
        private int largestWrite;

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            writes++;
            largestWrite = Math.max(largestWrite, len);
            super.write(b, off, len);
        }
    }
}
