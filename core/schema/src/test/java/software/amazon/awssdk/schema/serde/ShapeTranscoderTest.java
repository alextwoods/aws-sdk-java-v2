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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.internal.ShapeTranscoderSchemaExtensions;

/**
 * Ported from smithy-java {@code ShapeTranscoderTest}. The {@code DataStream} and {@code EventStream} members and
 * assertions are left out until those types are ported (design A.13). Records become final classes, and the tests
 * after {@code canBeReusedAfterFailedConversion} are SDK additions for unknown members and malformed input
 * (Requirements 7.7, 7.8).
 */
class ShapeTranscoderTest {

    private static final Schema SOURCE_CHILD = Schema.structureBuilder(ShapeId.from("example.source#Child"))
                                                     .putMember("value", PreludeSchemas.STRING)
                                                     .build();
    private static final Schema SOURCE_LIST = Schema.listBuilder(ShapeId.from("example.source#Values"))
                                                    .putMember("member", PreludeSchemas.STRING)
                                                    .build();
    private static final Schema SOURCE_MAP = Schema.mapBuilder(ShapeId.from("example.source#Tags"))
                                                   .putMember("key", PreludeSchemas.STRING)
                                                   .putMember("value", PreludeSchemas.STRING)
                                                   .build();
    private static final Schema SOURCE = Schema.structureBuilder(ShapeId.from("example.source#Envelope"))
                                               .putMember("ignored", PreludeSchemas.STRING)
                                               .putMember("name", PreludeSchemas.STRING)
                                               .putMember("number", PreludeSchemas.INTEGER)
                                               .putMember("child", SOURCE_CHILD)
                                               .putMember("values", SOURCE_LIST)
                                               .putMember("tags", SOURCE_MAP)
                                               .putMember("document", PreludeSchemas.DOCUMENT)
                                               .putMember("blob", PreludeSchemas.BLOB)
                                               .putMember("timestamp", PreludeSchemas.TIMESTAMP)
                                               .build();

    private static final Schema TARGET_CHILD = Schema.structureBuilder(ShapeId.from("example.target#Child"))
                                                     .putMember("value", PreludeSchemas.STRING)
                                                     .build();
    private static final Schema TARGET_LIST = Schema.listBuilder(ShapeId.from("example.target#Values"))
                                                    .putMember("member", PreludeSchemas.STRING)
                                                    .build();
    private static final Schema TARGET_MAP = Schema.mapBuilder(ShapeId.from("example.target#Tags"))
                                                   .putMember("key", PreludeSchemas.STRING)
                                                   .putMember("value", PreludeSchemas.STRING)
                                                   .build();
    private static final Schema TARGET = Schema.structureBuilder(ShapeId.from("example.target#Envelope"))
                                               .putMember("child", TARGET_CHILD)
                                               .putMember("name", PreludeSchemas.STRING)
                                               .putMember("number", PreludeSchemas.LONG)
                                               .putMember("tags", TARGET_MAP)
                                               .putMember("values", TARGET_LIST)
                                               .putMember("document", PreludeSchemas.DOCUMENT)
                                               .putMember("blob", PreludeSchemas.BLOB)
                                               .putMember("timestamp", PreludeSchemas.TIMESTAMP)
                                               .putMember("targetOnly", PreludeSchemas.STRING)
                                               .build();
    private static final Schema EMPTY_SOURCE = Schema.structureBuilder(ShapeId.from("example.source#Empty")).build();
    private static final Schema REQUIRED_TARGET = Schema.structureBuilder(ShapeId.from("example.target#Required"))
                                                        .putMember("required", PreludeSchemas.STRING)
                                                        .build();

    @Test
    void convertsDistinctGeneratedShapeGraphsWithoutReadingMembers() {
        Document document = Document.of(Collections.singletonMap("key", Document.of("value")));
        Instant timestamp = Instant.parse("2025-03-10T12:30:00Z");
        SourceEnvelope source = new SourceEnvelope("name",
                                                   42,
                                                   new SourceChild("nested"),
                                                   Arrays.asList("one", "two"),
                                                   mapWithNullValue(),
                                                   document,
                                                   ByteBuffer.wrap(new byte[] {1, 2, 3}),
                                                   timestamp);

        TargetEnvelope result = ShapeTranscoder.convert(source, new TargetBuilder());

        assertThat(result.name).isEqualTo("name");
        assertThat(result.number).isEqualTo(42L);
        assertThat(result.child.value).isEqualTo("nested");
        assertThat(result.values).containsExactly("one", "two");
        assertThat(result.tags).isEqualTo(mapWithNullValue());
        assertThat(result.document).isSameAs(document);
        assertThat(result.blob).isEqualTo(ByteBuffer.wrap(new byte[] {1, 2, 3}));
        assertThat(result.timestamp).isEqualTo(timestamp);
        assertThat(result.targetOnly).isNull();
    }

    @Test
    void reusesATranscoderAcrossConversions() {
        ShapeTranscoder transcoder = new ShapeTranscoder();

        TargetEnvelope first = transcoder.transcode(source("first"), new TargetBuilder());
        TargetEnvelope second = transcoder.transcode(source("second"), new TargetBuilder());

        assertThat(first.name).isEqualTo("first");
        assertThat(second.name).isEqualTo("second");
    }

    @Test
    void strictlyConvertsMatchingShapeTypes() {
        SerializableShape source = serializer -> serializer.writeString(PreludeSchemas.STRING, "value");

        ScalarResult result = ShapeTranscoder.convertStrict(
            source, new ScalarBuilder(PreludeSchemas.STRING, d -> d.readString(PreludeSchemas.STRING)));

        assertThat(result.value).isEqualTo("value");
    }

    @Test
    void strictConversionAllowsLosslessNumericConversions() {
        long longValue = 9_007_199_254_740_993L;
        assertThat(transcodeScalarStrict(serializer -> serializer.writeLong(PreludeSchemas.LONG, longValue),
                                         PreludeSchemas.BIG_INTEGER,
                                         d -> d.readBigInteger(PreludeSchemas.BIG_INTEGER)))
            .isEqualTo(BigInteger.valueOf(longValue));
        assertThat(transcodeScalarStrict(
            serializer -> serializer.writeBigInteger(PreludeSchemas.BIG_INTEGER, BigInteger.valueOf(42)),
            PreludeSchemas.LONG,
            d -> d.readLong(PreludeSchemas.LONG)))
            .isEqualTo(42L);
        assertThat(transcodeScalarStrict(serializer -> serializer.writeFloat(PreludeSchemas.FLOAT, 1.5f),
                                         PreludeSchemas.DOUBLE,
                                         d -> d.readDouble(PreludeSchemas.DOUBLE)))
            .isEqualTo(1.5d);
    }

    @Test
    void strictConversionRejectsLossyNumericConversions() {
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            serializer -> serializer.writeLong(PreludeSchemas.LONG, 9_007_199_254_740_993L),
            new ScalarBuilder(PreludeSchemas.DOUBLE, d -> d.readDouble(PreludeSchemas.DOUBLE))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Strict conversion cannot losslessly convert long to double for smithy.api#Double");
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            serializer -> serializer.writeBigInteger(PreludeSchemas.BIG_INTEGER,
                                                     BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)),
            new ScalarBuilder(PreludeSchemas.LONG, d -> d.readLong(PreludeSchemas.LONG))))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            serializer -> serializer.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, new BigDecimal("1.5")),
            new ScalarBuilder(PreludeSchemas.BIG_INTEGER, d -> d.readBigInteger(PreludeSchemas.BIG_INTEGER))))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            serializer -> serializer.writeDouble(PreludeSchemas.DOUBLE, 0.1d),
            new ScalarBuilder(PreludeSchemas.FLOAT, d -> d.readFloat(PreludeSchemas.FLOAT))))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void strictConversionRejectsUnrelatedShapeTypes() {
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            serializer -> serializer.writeString(PreludeSchemas.STRING, "42"),
            new ScalarBuilder(PreludeSchemas.INTEGER, d -> d.readInteger(PreludeSchemas.INTEGER))))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void strictConversionChecksTypesForSchemaLessReaders() {
        SerializableShape documentSource = serializer -> serializer.writeDocument(PreludeSchemas.DOCUMENT,
                                                                                  Document.of("v"));
        SerializableShape nullSource = serializer -> serializer.writeNull(PreludeSchemas.STRING);

        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            documentSource, new ScalarBuilder(PreludeSchemas.STRING, ShapeDeserializer::readDocument)))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(
            nullSource, new ScalarBuilder(PreludeSchemas.LONG, ShapeDeserializer::readNull)))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void strictConversionDropsUnknownSourceMembers() {
        TargetEnvelope result = ShapeTranscoder.convertStrict(source("value"), new TargetBuilder());

        assertThat(result.name).isEqualTo("value");
        assertThat(result.targetOnly).isNull();
    }

    @Test
    void strictConversionDoesNotCorrectMissingRequiredTargetMembers() {
        assertThat(ShapeTranscoder.convert(new EmptySource(), new RequiredBuilder()).value).isEqualTo("corrected");
        assertThatThrownBy(() -> ShapeTranscoder.convertStrict(new EmptySource(), new RequiredBuilder()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void errorCorrectsByDefaultButStrictConversionBuildsDirectly() {
        SerializableShape source = serializer -> serializer.writeString(PreludeSchemas.STRING, "value");

        assertThat(ShapeTranscoder.convert(source, new CorrectingBuilder()).value).isEqualTo("corrected");
        assertThat(ShapeTranscoder.convertStrict(source, new CorrectingBuilder()).value).isEqualTo("value");
    }

    @Test
    void switchesModesWhenReusedAfterStrictFailure() {
        ShapeTranscoder transcoder = new ShapeTranscoder();
        SerializableShape longSource = serializer -> serializer.writeLong(PreludeSchemas.LONG, 256);

        assertThatThrownBy(() -> transcoder.transcodeStrict(
            longSource, new ScalarBuilder(PreludeSchemas.BYTE, d -> d.readByte(PreludeSchemas.BYTE))))
            .isInstanceOf(SerializationException.class);

        ScalarResult coerced = transcoder.transcode(
            longSource, new ScalarBuilder(PreludeSchemas.BYTE, d -> d.readByte(PreludeSchemas.BYTE)));
        assertThat(coerced.value).isEqualTo((byte) 0);

        ScalarResult strict = transcoder.transcodeStrict(
            longSource, new ScalarBuilder(PreludeSchemas.BIG_INTEGER, d -> d.readBigInteger(PreludeSchemas.BIG_INTEGER)));
        assertThat(strict.value).isEqualTo(BigInteger.valueOf(256));
    }

    @Test
    void sharesMemberMappingsThroughTheSourceSchema() {
        ShapeTranscoderSchemaExtensions.MemberMapping first = ShapeTranscoderSchemaExtensions.mapping(SOURCE, TARGET);
        ShapeTranscoderSchemaExtensions.MemberMapping second = ShapeTranscoderSchemaExtensions.mapping(SOURCE, TARGET);

        assertThat(second).isSameAs(first);
        assertThat(first.targetMember(SOURCE.member("name"))).isSameAs(TARGET.member("name"));
        assertThat(SOURCE.getExtension(ShapeTranscoderSchemaExtensions.KEY)).isNotNull();
        assertThat(PreludeSchemas.STRING.getExtension(ShapeTranscoderSchemaExtensions.KEY)).isNull();
    }

    @Test
    void safelyCreatesAndUsesMappingsConcurrently() throws Exception {
        Schema source = Schema.structureBuilder(ShapeId.from("concurrent.source#Struct"))
                              .putMember("value", PreludeSchemas.STRING)
                              .build();
        Schema target = Schema.structureBuilder(ShapeId.from("concurrent.target#Struct"))
                              .putMember("value", PreludeSchemas.STRING)
                              .build();
        Schema sourceMember = source.member("value");
        Schema targetMember = target.member("value");
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Schema>> results = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return ShapeTranscoderSchemaExtensions.mapping(source, target).targetMember(sourceMember);
                }));
            }
            start.countDown();
            for (Future<Schema> result : results) {
                assertThat(result.get()).isSameAs(targetMember);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(ShapeTranscoderSchemaExtensions.mapping(source, target))
            .isSameAs(ShapeTranscoderSchemaExtensions.mapping(source, target));
    }

    @Test
    void remainsCorrectWhenTheBoundedMappingCacheEvictsEntries() {
        Schema source = Schema.structureBuilder(ShapeId.from("eviction.source#Struct"))
                              .putMember("value", PreludeSchemas.STRING)
                              .build();
        Schema sourceMember = source.member("value");
        List<Schema> targets = new ArrayList<>();

        for (int i = 0; i < 64; i++) {
            Schema target = Schema.structureBuilder(ShapeId.from("eviction.target#Struct" + i))
                                  .putMember("value", PreludeSchemas.STRING)
                                  .build();
            targets.add(target);
            assertThat(ShapeTranscoderSchemaExtensions.mapping(source, target).targetMember(sourceMember))
                .isSameAs(target.member("value"));
        }

        for (Schema target : targets) {
            assertThat(ShapeTranscoderSchemaExtensions.mapping(source, target).targetMember(sourceMember))
                .isSameAs(target.member("value"));
        }
    }

    @Test
    void forwardsScalarValues() {
        assertThat(transcodeScalar(s -> s.writeBoolean(PreludeSchemas.BOOLEAN, true),
                                   PreludeSchemas.BOOLEAN, d -> d.readBoolean(PreludeSchemas.BOOLEAN)))
            .isEqualTo(true);
        assertThat(transcodeScalar(s -> s.writeByte(PreludeSchemas.BYTE, (byte) 1),
                                   PreludeSchemas.BYTE, d -> d.readByte(PreludeSchemas.BYTE)))
            .isEqualTo((byte) 1);
        assertThat(transcodeScalar(s -> s.writeShort(PreludeSchemas.SHORT, (short) 2),
                                   PreludeSchemas.SHORT, d -> d.readShort(PreludeSchemas.SHORT)))
            .isEqualTo((short) 2);
        assertThat(transcodeScalar(s -> s.writeLong(PreludeSchemas.LONG, 3),
                                   PreludeSchemas.LONG, d -> d.readLong(PreludeSchemas.LONG)))
            .isEqualTo(3L);
        assertThat(transcodeScalar(s -> s.writeFloat(PreludeSchemas.FLOAT, 4.5f),
                                   PreludeSchemas.FLOAT, d -> d.readFloat(PreludeSchemas.FLOAT)))
            .isEqualTo(4.5f);
        assertThat(transcodeScalar(s -> s.writeDouble(PreludeSchemas.DOUBLE, 5.5d),
                                   PreludeSchemas.DOUBLE, d -> d.readDouble(PreludeSchemas.DOUBLE)))
            .isEqualTo(5.5d);

        BigInteger bigInteger = new BigInteger("12345678901234567890");
        assertThat(transcodeScalar(s -> s.writeBigInteger(PreludeSchemas.BIG_INTEGER, bigInteger),
                                   PreludeSchemas.BIG_INTEGER, d -> d.readBigInteger(PreludeSchemas.BIG_INTEGER)))
            .isSameAs(bigInteger);

        BigDecimal bigDecimal = new BigDecimal("1234567890.123456789");
        assertThat(transcodeScalar(s -> s.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, bigDecimal),
                                   PreludeSchemas.BIG_DECIMAL, d -> d.readBigDecimal(PreludeSchemas.BIG_DECIMAL)))
            .isSameAs(bigDecimal);

        assertThat(transcodeScalar(s -> s.writeNull(PreludeSchemas.STRING),
                                   PreludeSchemas.STRING, ShapeDeserializer::readNull))
            .isNull();
    }

    @Test
    void preservesPrecisionWhenCoercingArbitraryPrecisionNumbers() {
        long longValue = 9_007_199_254_740_993L;
        assertThat(transcodeScalar(s -> s.writeLong(PreludeSchemas.LONG, longValue),
                                   PreludeSchemas.BIG_DECIMAL, d -> d.readBigDecimal(PreludeSchemas.BIG_DECIMAL)))
            .isEqualTo(BigDecimal.valueOf(longValue));

        BigInteger bigInteger = new BigInteger("123456789012345678901234567890");
        assertThat(transcodeScalar(s -> s.writeBigInteger(PreludeSchemas.BIG_INTEGER, bigInteger),
                                   PreludeSchemas.BIG_DECIMAL, d -> d.readBigDecimal(PreludeSchemas.BIG_DECIMAL)))
            .isEqualTo(new BigDecimal(bigInteger));

        BigDecimal bigDecimal = new BigDecimal("123456789012345678901234567890.987654321");
        assertThat(transcodeScalar(s -> s.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, bigDecimal),
                                   PreludeSchemas.BIG_INTEGER, d -> d.readBigInteger(PreludeSchemas.BIG_INTEGER)))
            .isEqualTo(bigDecimal.toBigInteger());
    }

    @Test
    void canBeReusedAfterFailedConversion() {
        ShapeTranscoder transcoder = new ShapeTranscoder();
        SerializableShape stringSource = serializer -> serializer.writeString(PreludeSchemas.STRING, "value");

        assertThatThrownBy(() -> transcoder.transcode(
            stringSource, new ScalarBuilder(PreludeSchemas.BOOLEAN, d -> d.readBoolean(PreludeSchemas.BOOLEAN))))
            .isInstanceOf(SerializationException.class);

        ScalarResult result = transcoder.transcode(
            stringSource, new ScalarBuilder(PreludeSchemas.STRING, d -> d.readString(PreludeSchemas.STRING)));
        assertThat(result.value).isEqualTo("value");
    }

    // SDK additions.

    @Test
    void reportsUnknownMembersToTheConsumerAndKeepsReading() {
        TargetBuilder builder = new TargetBuilder();

        TargetEnvelope result = ShapeTranscoder.convert(source("value"), builder);

        assertThat(builder.unknownMembers).containsExactly("ignored");
        assertThat(result.name).isEqualTo("value");
        assertThat(result.child.value).isEqualTo("child");
        assertThat(result.timestamp).isEqualTo(Instant.EPOCH);
    }

    @Test
    void strictConversionDoesNotReportUnknownMembers() {
        TargetBuilder builder = new TargetBuilder();

        ShapeTranscoder.convertStrict(source("value"), builder);

        assertThat(builder.unknownMembers).isEmpty();
    }

    @Test
    void mismatchedMemberTypeNamesTheMemberAndBuildsNothing() {
        Schema source = Schema.structureBuilder(ShapeId.from("example.source#Mismatch"))
                              .putMember("name", PreludeSchemas.INTEGER)
                              .build();
        SerializableStruct struct = TestHelper.create(source, (s, ser) -> ser.writeInteger(s.member("name"), 1));
        TargetBuilder builder = new TargetBuilder();

        assertThatThrownBy(() -> ShapeTranscoder.convert(struct, builder))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected STRING for example.target#Envelope$name, but found INTEGER");
        assertThat(builder.builds).isZero();
    }

    @Test
    void mismatchedNestedValueNamesTheTargetMember() {
        SerializableStruct struct = TestHelper.create(SOURCE, (s, ser) -> {
            ser.writeList(s.member("values"), null, 1, (state, ls) -> ls.writeBoolean(SOURCE_LIST.listMember(), true));
        });

        assertThatThrownBy(() -> ShapeTranscoder.convert(struct, new TargetBuilder()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected STRING for example.target#Values$member, but found BOOLEAN");
    }

    @Test
    void schemaLessReadsNameTheExpectedShape() {
        SerializableStruct struct = TestHelper.create(SOURCE, (s, ser) -> ser.writeString(s.member("document"), "x"));

        assertThatThrownBy(() -> ShapeTranscoder.convert(struct, new TargetBuilder()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected DOCUMENT for example.target#Envelope$document, but found STRING");
        assertThatThrownBy(() -> ShapeTranscoder.convert(s -> s.writeString(PreludeSchemas.STRING, "x"),
                                                         new ScalarBuilder(PreludeSchemas.STRING,
                                                                           ShapeDeserializer::readNull)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected NULL for smithy.api#String, but found STRING");
    }

    @Test
    void readingANumberFromANonNumberNamesTheShape() {
        assertThatThrownBy(() -> ShapeTranscoder.convert(s -> s.writeString(PreludeSchemas.STRING, "1"),
                                                         new ScalarBuilder(PreludeSchemas.LONG,
                                                                           d -> d.readLong(PreludeSchemas.LONG))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a number for smithy.api#Long, but found STRING");
    }

    @Test
    void rejectsSourcesThatWriteNoValueOrMoreThanOne() {
        ScalarBuilder builder = new ScalarBuilder(PreludeSchemas.STRING, d -> d.readString(PreludeSchemas.STRING));

        assertThatThrownBy(() -> ShapeTranscoder.convert(s -> { }, builder))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Shape did not serialize a value for smithy.api#String");
        assertThatThrownBy(() -> ShapeTranscoder.convert(s -> {
            s.writeString(PreludeSchemas.STRING, "a");
            s.writeString(PreludeSchemas.STRING, "b");
        }, builder))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("more than one value");
        assertThatThrownBy(() -> ShapeTranscoder.convert(TestHelper.create(SOURCE, (s, ser) -> {
            ser.writeMap(s.member("tags"), null, 1, (state, ms) -> {
                ms.writeEntry(SOURCE_MAP.mapKeyMember(), "k", null, (st, vs) -> { });
            });
        }), new TargetBuilder()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Shape did not serialize a value for example.target#Tags$value");
    }

    @Test
    void rejectsNestedConversionsOnOneInstance() {
        ShapeTranscoder transcoder = new ShapeTranscoder();
        SerializableShape nested = s -> {
            transcoder.transcode(t -> t.writeString(PreludeSchemas.STRING, "x"),
                                 new ScalarBuilder(PreludeSchemas.STRING, d -> d.readString(PreludeSchemas.STRING)));
        };

        assertThatThrownBy(() -> transcoder.transcode(nested, new ScalarBuilder(PreludeSchemas.STRING, d -> null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("ShapeTranscoder is already performing a conversion");
    }

    private static Object transcodeScalar(SerializableShape source, Schema targetSchema,
                                          Function<ShapeDeserializer, Object> reader) {
        return ShapeTranscoder.convert(source, new ScalarBuilder(targetSchema, reader)).value;
    }

    private static Object transcodeScalarStrict(SerializableShape source, Schema targetSchema,
                                                Function<ShapeDeserializer, Object> reader) {
        return ShapeTranscoder.convertStrict(source, new ScalarBuilder(targetSchema, reader)).value;
    }

    private static SourceEnvelope source(String name) {
        return new SourceEnvelope(name,
                                  1,
                                  new SourceChild("child"),
                                  Collections.emptyList(),
                                  Collections.emptyMap(),
                                  Document.of("document"),
                                  ByteBuffer.allocate(0),
                                  Instant.EPOCH);
    }

    private static Map<String, String> mapWithNullValue() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("present", "value");
        result.put("missing", null);
        return result;
    }

    private static final class SourceChild implements SerializableStruct {
        private final String value;

        private SourceChild(String value) {
            this.value = value;
        }

        @Override
        public Schema sdkSchema() {
            return SOURCE_CHILD;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString(SOURCE_CHILD.member("value"), value);
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new AssertionError("ShapeTranscoder must not call getMemberValue");
        }
    }

    private static final class SourceEnvelope implements SerializableStruct {
        private final String name;
        private final int number;
        private final SourceChild child;
        private final List<String> values;
        private final Map<String, String> tags;
        private final Document document;
        private final ByteBuffer blob;
        private final Instant timestamp;

        private SourceEnvelope(String name, int number, SourceChild child, List<String> values, Map<String, String> tags,
                               Document document, ByteBuffer blob, Instant timestamp) {
            this.name = name;
            this.number = number;
            this.child = child;
            this.values = values;
            this.tags = tags;
            this.document = document;
            this.blob = blob;
            this.timestamp = timestamp;
        }

        @Override
        public Schema sdkSchema() {
            return SOURCE;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString(SOURCE.member("ignored"), "ignored");
            serializer.writeString(SOURCE.member("name"), name);
            serializer.writeInteger(SOURCE.member("number"), number);
            serializer.writeStruct(SOURCE.member("child"), child);
            serializer.writeList(SOURCE.member("values"), values, values.size(), SourceEnvelope::writeValues);
            serializer.writeMap(SOURCE.member("tags"), tags, tags.size(), SourceEnvelope::writeTags);
            serializer.writeDocument(SOURCE.member("document"), document);
            serializer.writeBlob(SOURCE.member("blob"), blob);
            serializer.writeTimestamp(SOURCE.member("timestamp"), timestamp);
        }

        private static void writeValues(List<String> values, ShapeSerializer serializer) {
            for (String value : values) {
                if (value == null) {
                    serializer.writeNull(SOURCE_LIST.listMember());
                } else {
                    serializer.writeString(SOURCE_LIST.listMember(), value);
                }
            }
        }

        private static void writeTags(Map<String, String> tags, MapSerializer serializer) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                serializer.writeEntry(SOURCE_MAP.mapKeyMember(), entry.getKey(), entry.getValue(),
                                      SourceEnvelope::writeTag);
            }
        }

        private static void writeTag(String value, ShapeSerializer serializer) {
            if (value == null) {
                serializer.writeNull(SOURCE_MAP.mapValueMember());
            } else {
                serializer.writeString(SOURCE_MAP.mapValueMember(), value);
            }
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new AssertionError("ShapeTranscoder must not call getMemberValue");
        }
    }

    private static final class TargetChild {
        private final String value;

        private TargetChild(String value) {
            this.value = value;
        }
    }

    private static final class ScalarResult implements SerializableShape {
        private final Object value;

        private ScalarResult(Object value) {
            this.value = value;
        }

        @Override
        public void serialize(ShapeSerializer encoder) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class EmptySource implements SerializableStruct {
        @Override
        public Schema sdkSchema() {
            return EMPTY_SOURCE;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new IllegalArgumentException("Empty source has no members");
        }
    }

    private static final class RequiredResult implements SerializableShape {
        private final String value;

        private RequiredResult(String value) {
            this.value = value;
        }

        @Override
        public void serialize(ShapeSerializer encoder) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RequiredBuilder implements ShapeBuilder<RequiredResult> {
        private String value;

        @Override
        public RequiredResult build() {
            if (value == null) {
                throw new IllegalStateException("required member is missing");
            }
            return new RequiredResult(value);
        }

        @Override
        public ShapeBuilder<RequiredResult> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(REQUIRED_TARGET, this, (builder, member, d) -> builder.value = d.readString(member));
            return this;
        }

        @Override
        public ShapeBuilder<RequiredResult> errorCorrection() {
            value = "corrected";
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return REQUIRED_TARGET;
        }
    }

    private static final class ScalarBuilder implements ShapeBuilder<ScalarResult> {
        private final Schema schema;
        private final Function<ShapeDeserializer, Object> reader;
        private Object value;

        private ScalarBuilder(Schema schema, Function<ShapeDeserializer, Object> reader) {
            this.schema = schema;
            this.reader = reader;
        }

        @Override
        public ScalarResult build() {
            return new ScalarResult(value);
        }

        @Override
        public ShapeBuilder<ScalarResult> deserialize(ShapeDeserializer decoder) {
            value = reader.apply(decoder);
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }
    }

    private static final class CorrectingBuilder implements ShapeBuilder<ScalarResult> {
        private String value;

        @Override
        public ScalarResult build() {
            return new ScalarResult(value);
        }

        @Override
        public ShapeBuilder<ScalarResult> deserialize(ShapeDeserializer decoder) {
            value = decoder.readString(PreludeSchemas.STRING);
            return this;
        }

        @Override
        public ShapeBuilder<ScalarResult> errorCorrection() {
            value = "corrected";
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return PreludeSchemas.STRING;
        }
    }

    private static final class TargetEnvelope implements SerializableShape {
        private final String name;
        private final long number;
        private final TargetChild child;
        private final List<String> values;
        private final Map<String, String> tags;
        private final Document document;
        private final ByteBuffer blob;
        private final Instant timestamp;
        private final String targetOnly;

        private TargetEnvelope(TargetBuilder builder) {
            this.name = builder.name;
            this.number = builder.number;
            this.child = builder.child;
            this.values = builder.values;
            this.tags = builder.tags;
            this.document = builder.document;
            this.blob = builder.blob;
            this.timestamp = builder.timestamp;
            this.targetOnly = builder.targetOnly;
        }

        @Override
        public void serialize(ShapeSerializer encoder) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TargetBuilder implements ShapeBuilder<TargetEnvelope> {
        private final List<String> unknownMembers = new ArrayList<>();
        private int builds;
        private String name;
        private long number;
        private TargetChild child;
        private List<String> values;
        private Map<String, String> tags;
        private Document document;
        private ByteBuffer blob;
        private Instant timestamp;
        private String targetOnly;

        @Override
        public TargetEnvelope build() {
            builds++;
            return new TargetEnvelope(this);
        }

        @Override
        public ShapeBuilder<TargetEnvelope> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(TARGET, this, new ShapeDeserializer.StructMemberConsumer<TargetBuilder>() {
                @Override
                public void accept(TargetBuilder builder, Schema member, ShapeDeserializer value) {
                    switch (member.memberName()) {
                        case "name":
                            builder.name = value.readString(member);
                            break;
                        case "number":
                            builder.number = value.readLong(member);
                            break;
                        case "child":
                            builder.child = readChild(value);
                            break;
                        case "values":
                            builder.values = readValues(value);
                            break;
                        case "tags":
                            builder.tags = readTags(value);
                            break;
                        case "document":
                            builder.document = value.readDocument();
                            break;
                        case "blob":
                            builder.blob = value.readBlob(member);
                            break;
                        case "timestamp":
                            builder.timestamp = value.readTimestamp(member);
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected member " + member);
                    }
                }

                @Override
                public void unknownMember(TargetBuilder builder, String memberName) {
                    builder.unknownMembers.add(memberName);
                }
            });
            return this;
        }

        private static TargetChild readChild(ShapeDeserializer deserializer) {
            String[] value = new String[1];
            deserializer.readStruct(TARGET_CHILD, value, (state, member, child) -> state[0] = child.readString(member));
            return new TargetChild(value[0]);
        }

        private static List<String> readValues(ShapeDeserializer deserializer) {
            List<String> result = new ArrayList<>();
            deserializer.readList(TARGET_LIST, result, (values, value) -> {
                values.add(value.isNull() ? value.readNull() : value.readString(TARGET_LIST.listMember()));
            });
            return result;
        }

        private static Map<String, String> readTags(ShapeDeserializer deserializer) {
            Map<String, String> result = new LinkedHashMap<>();
            deserializer.readStringMap(TARGET_MAP, result, (tags, key, value) -> {
                tags.put(key, value.isNull() ? value.readNull() : value.readString(TARGET_MAP.mapValueMember()));
            });
            return result;
        }

        @Override
        public Schema sdkSchema() {
            return TARGET;
        }
    }
}
