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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Formats and parses timestamps in one Smithy timestamp format.
 *
 * <p>A timestamp format is a concrete encoding, so its formatting and parsing can be implemented once, independent of
 * the data format. A formatted timestamp is either a string or a number.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.TimestampFormatter}.
 */
@SdkPublicApi
public interface TimestampFormatter {
    /**
     * Returns the formatter for the {@code timestampFormat} trait of {@code schema}, or for {@code defaultFormat} if the
     * schema has no such trait.
     *
     * @param schema the schema to read the {@code timestampFormat} trait from
     * @param defaultFormat the format to use if the schema has no {@code timestampFormat} trait
     * @return the formatter
     * @throws SerializationException for an unknown format
     */
    static TimestampFormatter of(Schema schema, TimestampFormatTrait.Format defaultFormat) {
        return of(schema.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT), defaultFormat);
    }

    /**
     * Returns the formatter for {@code trait}, or for {@code defaultFormat} if {@code trait} is null.
     *
     * @param trait the trait to use, or null
     * @param defaultFormat the format to use if {@code trait} is null
     * @return the formatter
     * @throws SerializationException for an unknown format
     */
    static TimestampFormatter of(TimestampFormatTrait trait, TimestampFormatTrait.Format defaultFormat) {
        return of(trait == null ? defaultFormat : trait.getFormat());
    }

    /**
     * Returns the formatter for a non-null {@code trait}.
     *
     * @param trait the trait to use
     * @return the formatter
     * @throws SerializationException for an unknown format
     */
    static TimestampFormatter of(TimestampFormatTrait trait) {
        return of(trait.getFormat());
    }

    /**
     * Returns the formatter for {@code format}.
     *
     * @param format the format
     * @return the formatter
     * @throws SerializationException for an unknown or null format
     */
    static TimestampFormatter of(TimestampFormatTrait.Format format) {
        TimestampFormatter result = match(format);
        if (result == null) {
            throw new SerializationException("Unknown timestamp format: " + format);
        }
        return result;
    }

    /**
     * Returns the formatter for {@code format}, or null if the format is unknown or null.
     *
     * @param format the format
     * @return the formatter, or null
     */
    static TimestampFormatter match(TimestampFormatTrait.Format format) {
        if (format == null) {
            return null;
        }
        switch (format) {
            case DATE_TIME:
                return Prelude.DATE_TIME;
            case EPOCH_SECONDS:
                return Prelude.EPOCH_SECONDS;
            case HTTP_DATE:
                return Prelude.HTTP_DATE;
            default:
                return null;
        }
    }

    /**
     * Returns the timestamp format this formatter implements.
     *
     * @return the format
     */
    TimestampFormatTrait.Format format();

    /**
     * Formats {@code value} as a string. A format that is written as a number returns the number's text.
     *
     * @param value the timestamp
     * @return the formatted timestamp
     */
    String writeString(Instant value);

    /**
     * Writes {@code value} to {@code serializer} as the string or number this format uses.
     *
     * <p>Implementations must not call {@link ShapeSerializer#writeTimestamp}, which would recurse; they write a string
     * or a number instead.
     *
     * @param schema the timestamp's schema
     * @param value the timestamp
     * @param serializer where to write
     */
    void writeToSerializer(Schema schema, Instant value, ShapeSerializer serializer);

    /**
     * Parses a timestamp from a string. This must accept the output of {@link #writeString(Instant)}.
     *
     * @param value the text to parse
     * @param strict whether to reject a string for a format that the data format writes as a number
     * @return the timestamp
     * @throws TimestampSyntaxError if the value has the wrong type for this format
     * @throws java.time.format.DateTimeParseException if the text is not in this format
     */
    Instant readFromString(String value, boolean strict);

    /**
     * Converts a number to a timestamp, if this format is numeric.
     *
     * @param value the number
     * @return the timestamp
     * @throws TimestampSyntaxError if this format is not numeric or the number's type is not supported
     */
    Instant readFromNumber(Number value);

    /**
     * The timestamp formats defined in the Smithy prelude.
     */
    enum Prelude implements TimestampFormatter {
        /**
         * {@code epoch-seconds}: seconds since the Unix epoch as a number, with millisecond precision.
         */
        EPOCH_SECONDS {
            @Override
            public TimestampFormatTrait.Format format() {
                return TimestampFormatTrait.Format.EPOCH_SECONDS;
            }

            @Override
            public String writeString(Instant value) {
                double v = ((double) value.toEpochMilli()) / 1000;
                // only write fractional seconds if we wouldn't write ".000"
                if (v - (long) v >= 0.001d) {
                    return String.format(Locale.ROOT, "%.3f", v);
                }
                return Long.toString((long) v);
            }

            @Override
            public Instant readFromString(String value, boolean strict) {
                if (strict) {
                    throw new TimestampSyntaxError(format(), ExpectedType.NUMBER, value);
                }
                return Instant.ofEpochMilli((long) (Double.parseDouble(value) * 1000));
            }

            @Override
            public Instant readFromNumber(Number value) {
                // The most common types for serialized epoch-seconds, double/integer/long, are checked first.
                if (value instanceof Double) {
                    return Instant.ofEpochMilli((long) ((Double) value * 1000f));
                } else if (value instanceof Integer) {
                    return Instant.ofEpochMilli((Integer) value * 1000L);
                } else if (value instanceof Long) {
                    return Instant.ofEpochMilli((Long) value * 1000L);
                } else if (value instanceof Byte) {
                    return Instant.ofEpochMilli((Byte) value * 1000L);
                } else if (value instanceof Short) {
                    return Instant.ofEpochMilli((Short) value * 1000L);
                } else if (value instanceof Float) {
                    return Instant.ofEpochMilli((long) ((Float) value * 1000f));
                } else if (value instanceof BigInteger) {
                    return Instant.ofEpochMilli(((BigInteger) value).longValue() * 1000);
                } else if (value instanceof BigDecimal) {
                    return Instant.ofEpochMilli(((BigDecimal) value).longValue() * 1000);
                }
                throw new TimestampSyntaxError(format(), ExpectedType.NUMBER, value);
            }

            @Override
            public void writeToSerializer(Schema schema, Instant instant, ShapeSerializer serializer) {
                serializer.writeDouble(schema, instant.toEpochMilli() / 1000.0);
            }
        },

        /**
         * {@code date-time}: an RFC 3339 string in UTC, such as {@code 1985-04-12T23:20:50.52Z}.
         */
        DATE_TIME {
            @Override
            public TimestampFormatTrait.Format format() {
                return TimestampFormatTrait.Format.DATE_TIME;
            }

            @Override
            public String writeString(Instant value) {
                return value.toString();
            }

            @Override
            public Instant readFromString(String value, boolean strict) {
                return DateTimeFormatter.ISO_INSTANT.parse(value, Instant::from);
            }
        },

        /**
         * {@code http-date}: an RFC 7231 IMF-fixdate string, such as {@code Tue, 29 Apr 2014 18:30:38 GMT}.
         */
        HTTP_DATE {
            @Override
            public TimestampFormatTrait.Format format() {
                return TimestampFormatTrait.Format.HTTP_DATE;
            }

            @Override
            public String writeString(Instant value) {
                long epoch = value.getEpochSecond();
                if (HttpDateFormat.isYearRepresentable(epoch)) {
                    return HttpDateFormat.format(epoch);
                }
                // Years before 0001 or after 9999 use the JDK formatter, as the legacy path did.
                return HttpDateFormat.formatFallback(value);
            }

            @Override
            public Instant readFromString(String value, boolean strict) {
                return HttpDateFormat.parse(value);
            }
        };

        /**
         * Returns the format name, for example {@code epoch-seconds}.
         *
         * @return the format name
         */
        @Override
        public String toString() {
            return format().toString();
        }

        @Override
        public void writeToSerializer(Schema schema, Instant value, ShapeSerializer serializer) {
            serializer.writeString(schema, writeString(value));
        }

        @Override
        public Instant readFromNumber(Number value) {
            throw new TimestampSyntaxError(format(), ExpectedType.STRING, value);
        }
    }

    /**
     * The value type a timestamp format expects, reported by {@link TimestampSyntaxError}.
     */
    enum ExpectedType {
        /**
         * The format is written as a string.
         */
        STRING,

        /**
         * The format is written as a number.
         */
        NUMBER
    }

    /**
     * Thrown when a timestamp value has the wrong type for its format.
     */
    final class TimestampSyntaxError extends SerializationException {
        private static final long serialVersionUID = 1L;

        private final TimestampFormatTrait.Format format;
        private final ExpectedType expectedType;
        private final Object value;

        /**
         * Creates the exception.
         *
         * @param format the timestamp format
         * @param expectedType the value type the format expects
         * @param value the value that was found
         */
        public TimestampSyntaxError(TimestampFormatTrait.Format format, ExpectedType expectedType, Object value) {
            super("Expected a " + expectedType + " value for a " + format.name() + " timestamp, but found "
                  + (value == null ? "null" : value.getClass().getSimpleName()));
            this.format = format;
            this.expectedType = expectedType;
            this.value = value;
        }

        /**
         * Returns the timestamp format.
         *
         * @return the timestamp format
         */
        public TimestampFormatTrait.Format format() {
            return format;
        }

        /**
         * Returns the value type the format expects.
         *
         * @return the expected type
         */
        public ExpectedType expectedType() {
            return expectedType;
        }

        /**
         * Returns the value that could not be read.
         *
         * @return the value
         */
        public Object value() {
            return value;
        }
    }
}
