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

package software.amazon.awssdk.protocols.core.serde;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import software.amazon.awssdk.protocols.core.binding.HttpBindingLocation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.TimestampFormatter;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Timestamp formats of the protocol runtimes: which format applies to a member in a location, and exact reading and
 * writing of each format.
 *
 * <p>Epoch-seconds values are read exactly from their decimal text ({@link #fromEpochSeconds(BigDecimal)}): Smithy
 * Java's {@code TimestampFormatter.Prelude.EPOCH_SECONDS.readFromNumber} converts through binary floating point and,
 * for example, reads {@code 512.035} as {@code 512.034} seconds. {@link #EXACT_EPOCH_SECONDS} is a drop-in formatter
 * with exact reading, for codecs that accept a formatter. It also writes epoch-seconds text exactly, where Smithy
 * Java 1.6.1's {@code writeString} drops the fraction of instants before the epoch.
 */
public final class TimestampFormats {
    /** An epoch-seconds formatter that reads numbers and numeric strings exactly. */
    public static final TimestampFormatter EXACT_EPOCH_SECONDS = new ExactEpochSeconds();

    private static final BigDecimal NANOS_PER_SECOND = BigDecimal.valueOf(1_000_000_000L);

    private TimestampFormats() {
    }

    /**
     * The format of {@code member}: its (or its target's) {@code smithy.api#timestampFormat} trait, else
     * {@code defaultFormat}.
     */
    public static TimestampFormatTrait.Format formatOf(Schema member, TimestampFormatTrait.Format defaultFormat) {
        TimestampFormatTrait trait = member.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
        return trait == null ? defaultFormat : trait.getFormat();
    }

    /**
     * The default format of a timestamp bound to an HTTP location: {@code http-date} in headers, {@code date-time}
     * in query parameters and URI labels.
     *
     * @throws IllegalArgumentException for a body, payload or status-code location, whose format the protocol's
     *         codec defines
     */
    public static TimestampFormatTrait.Format defaultFormat(HttpBindingLocation location) {
        return switch (location) {
            case HEADER, PREFIX_HEADERS -> TimestampFormatTrait.Format.HTTP_DATE;
            case QUERY, QUERY_PARAMS, LABEL -> TimestampFormatTrait.Format.DATE_TIME;
            default -> throw new IllegalArgumentException("The timestamp format of " + location
                                                          + " members is defined by the protocol's codec");
        };
    }

    /** The formatter of {@code format}; epoch-seconds reads exactly. */
    public static TimestampFormatter formatter(TimestampFormatTrait.Format format) {
        return format == TimestampFormatTrait.Format.EPOCH_SECONDS ? EXACT_EPOCH_SECONDS
                                                                   : TimestampFormatter.of(format);
    }

    /** {@code value} rendered as text in {@code format}, as it appears in a header, query parameter or label. */
    public static String write(java.time.Instant value, TimestampFormatTrait.Format format) {
        return formatter(format).writeString(value);
    }

    /**
     * Parses text in {@code format}.
     *
     * @throws SerializationException if the text is not a timestamp in that format
     */
    public static Instant read(String text, TimestampFormatTrait.Format format) {
        return formatter(format).readFromString(text, true);
    }

    /**
     * The instant {@code seconds} after the epoch, exactly, truncated toward negative infinity to nanosecond
     * precision.
     *
     * @throws SerializationException if it is outside the range of {@link Instant}
     */
    public static Instant fromEpochSeconds(BigDecimal seconds) {
        try {
            BigDecimal whole = seconds.setScale(0, RoundingMode.FLOOR);
            long nanos = seconds.subtract(whole).multiply(NANOS_PER_SECOND).setScale(0, RoundingMode.FLOOR)
                                .longValueExact();
            return Instant.ofEpochSecond(whole.longValueExact(), nanos);
        } catch (ArithmeticException | DateTimeException e) {
            throw new SerializationException("Epoch-seconds timestamp out of range", e);
        }
    }

    /**
     * The instant {@code seconds} after the epoch. A {@code double}/{@code float} is taken at its shortest decimal
     * representation (the literal a JSON or CBOR writer produced), not at its binary value.
     *
     * @throws SerializationException if it is not finite or out of range
     */
    public static Instant fromEpochSeconds(Number seconds) {
        if (seconds instanceof BigDecimal decimal) {
            return fromEpochSeconds(decimal);
        }
        if (seconds instanceof BigInteger integer) {
            return fromEpochSeconds(new BigDecimal(integer));
        }
        if (seconds instanceof Double || seconds instanceof Float) {
            double value = seconds.doubleValue();
            if (!Double.isFinite(value)) {
                throw new SerializationException("Epoch-seconds timestamp is not finite");
            }
            return fromEpochSeconds(new BigDecimal(seconds.toString()));
        }
        try {
            return Instant.ofEpochSecond(seconds.longValue());
        } catch (DateTimeException e) {
            throw new SerializationException("Epoch-seconds timestamp out of range", e);
        }
    }

    /** Epoch-seconds with exact reading and exact text writing; number writing is Smithy Java's. */
    private static final class ExactEpochSeconds implements TimestampFormatter {
        private static final TimestampFormatter PRELUDE = TimestampFormatter.Prelude.EPOCH_SECONDS;

        @Override
        public TimestampFormatTrait.Format format() {
            return TimestampFormatTrait.Format.EPOCH_SECONDS;
        }

        @Override
        public String writeString(Instant value) {
            // Millisecond precision, as Smithy Java writes; but exactly: its writeString truncates a negative
            // fractional value toward zero (1935-12-31T23:59:59.5Z, -1073001600.5 s, is written -1073001600).
            long millis = value.toEpochMilli();
            if (Math.floorMod(millis, 1000L) == 0) {
                return Long.toString(Math.floorDiv(millis, 1000L));
            }
            return BigDecimal.valueOf(millis, 3).toPlainString();
        }

        @Override
        public void writeToSerializer(Schema schema, Instant value, ShapeSerializer serializer) {
            PRELUDE.writeToSerializer(schema, value, serializer);
        }

        @Override
        public Instant readFromString(String value, boolean strict) {
            try {
                return fromEpochSeconds(new BigDecimal(value.trim()));
            } catch (NumberFormatException e) {
                throw new SerializationException("Not an epoch-seconds timestamp", e);
            }
        }

        @Override
        public Instant readFromNumber(Number value) {
            return fromEpochSeconds(value);
        }

        @Override
        public String toString() {
            return "EXACT_EPOCH_SECONDS";
        }
    }
}
