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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.commons.TimestampFormatter.ExpectedType;
import software.amazon.awssdk.codecs.commons.TimestampFormatter.Prelude;
import software.amazon.awssdk.codecs.commons.TimestampFormatter.TimestampSyntaxError;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait.Format;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.core.serde.TimestampFormatterTest}, plus SDK tests.
 */
public class TimestampFormatterTest {
    @Test
    public void testEpochSecondsRounding() {
        Instant wholeTime = Instant.ofEpochSecond(7234);
        assertEquals("7234", TimestampFormatter.Prelude.EPOCH_SECONDS.writeString(wholeTime));

        Instant fractionalTime = Instant.ofEpochMilli(1718830549174L);
        assertEquals("1718830549.174", TimestampFormatter.Prelude.EPOCH_SECONDS.writeString(fractionalTime));

        fractionalTime = Instant.ofEpochMilli(1718830549002L);
        assertEquals("1718830549.002", TimestampFormatter.Prelude.EPOCH_SECONDS.writeString(fractionalTime));
    }

    @Test
    void of_knownFormats_returnsPreludeFormatters() {
        assertThat(TimestampFormatter.of(Format.EPOCH_SECONDS)).isSameAs(Prelude.EPOCH_SECONDS);
        assertThat(TimestampFormatter.of(Format.DATE_TIME)).isSameAs(Prelude.DATE_TIME);
        assertThat(TimestampFormatter.of(Format.HTTP_DATE)).isSameAs(Prelude.HTTP_DATE);
        for (Prelude prelude : Prelude.values()) {
            assertThat(TimestampFormatter.of(prelude.format())).isSameAs(prelude);
            assertThat(prelude.toString()).isEqualTo(prelude.format().toString());
        }
        assertThat(Prelude.HTTP_DATE.toString()).isEqualTo("http-date");
    }

    @Test
    void of_unknownOrNullFormat_throwsSerializationException() {
        assertThat(TimestampFormatter.match(Format.UNKNOWN)).isNull();
        assertThat(TimestampFormatter.match(null)).isNull();
        assertThatThrownBy(() -> TimestampFormatter.of(Format.UNKNOWN))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unknown timestamp format: unknown");
        assertThatThrownBy(() -> TimestampFormatter.of((Format) null))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Unknown timestamp format");
        assertThatThrownBy(() -> TimestampFormatter.of(new TimestampFormatTrait("iso-week")))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void of_traitAndDefault_prefersTrait() {
        TimestampFormatTrait httpDate = new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE);

        assertThat(TimestampFormatter.of(httpDate)).isSameAs(Prelude.HTTP_DATE);
        assertThat(TimestampFormatter.of(httpDate, Format.EPOCH_SECONDS)).isSameAs(Prelude.HTTP_DATE);
        assertThat(TimestampFormatter.of((TimestampFormatTrait) null, Format.DATE_TIME)).isSameAs(Prelude.DATE_TIME);
    }

    @Test
    void of_schema_usesTimestampFormatTraitOrDefault() {
        Schema withTrait = Schema.createTimestamp(ShapeId.from("smithy.example#HttpDate"),
                                                  new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));

        assertThat(TimestampFormatter.of(withTrait, Format.EPOCH_SECONDS)).isSameAs(Prelude.HTTP_DATE);
        assertThat(TimestampFormatter.of(PreludeSchemas.TIMESTAMP, Format.DATE_TIME)).isSameAs(Prelude.DATE_TIME);
    }

    static Stream<Arguments> epochNumbers() {
        return Stream.of(
            Arguments.of(1.5d, Instant.ofEpochMilli(1500)),
            Arguments.of(2, Instant.ofEpochSecond(2)),
            Arguments.of(3L, Instant.ofEpochSecond(3)),
            Arguments.of((byte) 4, Instant.ofEpochSecond(4)),
            Arguments.of((short) 5, Instant.ofEpochSecond(5)),
            Arguments.of(6.25f, Instant.ofEpochMilli(6250)),
            Arguments.of(BigInteger.valueOf(7), Instant.ofEpochSecond(7)),
            Arguments.of(new BigDecimal("8.9"), Instant.ofEpochSecond(8)));
    }

    @ParameterizedTest
    @MethodSource("epochNumbers")
    void readFromNumber_epochSecondsNumberTypes_convertsToInstant(Number value, Instant expected) {
        assertThat(Prelude.EPOCH_SECONDS.readFromNumber(value)).isEqualTo(expected);
    }

    @Test
    void readFromNumber_epochSecondsUnsupportedOrNull_throwsTimestampSyntaxError() {
        assertThatThrownBy(() -> Prelude.EPOCH_SECONDS.readFromNumber(new java.util.concurrent.atomic.AtomicLong(1)))
            .isInstanceOf(TimestampSyntaxError.class)
            .hasMessage("Expected a NUMBER value for a EPOCH_SECONDS timestamp, but found AtomicLong");
        assertThatThrownBy(() -> Prelude.EPOCH_SECONDS.readFromNumber(null))
            .isInstanceOf(TimestampSyntaxError.class)
            .hasMessageEndingWith("but found null");
    }

    @Test
    void readFromNumber_stringFormats_throwsTimestampSyntaxError() {
        for (Prelude prelude : new Prelude[] {Prelude.DATE_TIME, Prelude.HTTP_DATE}) {
            assertThatThrownBy(() -> prelude.readFromNumber(1))
                .isInstanceOfSatisfying(TimestampSyntaxError.class, e -> {
                    assertThat(e.format()).isEqualTo(prelude.format());
                    assertThat(e.expectedType()).isEqualTo(ExpectedType.STRING);
                    assertThat(e.value()).isEqualTo(1);
                })
                .hasMessage("Expected a STRING value for a " + prelude.format().name() + " timestamp, but found Integer");
        }
    }

    @Test
    void readFromString_epochSeconds_parsesUnlessStrict() {
        assertThat(Prelude.EPOCH_SECONDS.readFromString("1718830549.174", false))
            .isEqualTo(Instant.ofEpochMilli(1718830549174L));
        assertThatThrownBy(() -> Prelude.EPOCH_SECONDS.readFromString("1", true))
            .isInstanceOfSatisfying(TimestampSyntaxError.class,
                                    e -> assertThat(e.expectedType()).isEqualTo(ExpectedType.NUMBER))
            .hasMessageContaining("but found String");
    }

    @Test
    void writeString_dateTimeAndHttpDate_roundTripThroughReadFromString() {
        Instant value = Instant.parse("1985-04-12T23:20:50.520Z");

        assertThat(Prelude.DATE_TIME.writeString(value)).isEqualTo("1985-04-12T23:20:50.520Z");
        assertThat(Prelude.DATE_TIME.readFromString("1985-04-12T23:20:50.52Z", true)).isEqualTo(value);
        assertThat(Prelude.HTTP_DATE.writeString(value)).isEqualTo("Fri, 12 Apr 1985 23:20:50 GMT");
        assertThat(Prelude.HTTP_DATE.readFromString("Fri, 12 Apr 1985 23:20:50 GMT", true))
            .isEqualTo(Instant.parse("1985-04-12T23:20:50Z"));
        assertThatThrownBy(() -> Prelude.DATE_TIME.readFromString("not a date", false))
            .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void writeString_httpDateOutsideFourDigitYears_usesFallbackFormatter() {
        Instant year10000 = Instant.parse("+10000-01-01T00:00:00Z");

        assertThat(Prelude.HTTP_DATE.writeString(year10000)).isEqualTo(HttpDateFormat.formatFallback(year10000));
    }

    @Test
    void writeToSerializer_eachFormat_writesNumberOrString() {
        RecordingSerializer serializer = new RecordingSerializer();
        Instant value = Instant.ofEpochMilli(1500);

        Prelude.EPOCH_SECONDS.writeToSerializer(PreludeSchemas.TIMESTAMP, value, serializer);
        Prelude.DATE_TIME.writeToSerializer(PreludeSchemas.TIMESTAMP, value, serializer);
        Prelude.HTTP_DATE.writeToSerializer(PreludeSchemas.TIMESTAMP, value, serializer);

        assertThat(serializer.values).containsExactly(1.5d, "1970-01-01T00:00:01.500Z", "Thu, 01 Jan 1970 00:00:01 GMT");
    }

    private static final class RecordingSerializer extends SpecificShapeSerializer {
        private final List<Object> values = new ArrayList<>();

        @Override
        public void writeDouble(Schema schema, double value) {
            values.add(value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            values.add(value);
        }
    }
}
