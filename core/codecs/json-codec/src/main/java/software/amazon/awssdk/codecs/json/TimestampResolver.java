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

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.json.internal.JsonSchemaExtensions;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;

/**
 * Resolves the timestamp format of a shape. The two implementations are {@link StaticFormat} and
 * {@link UseTimestampFormatTrait}; {@link JsonSettings#timestampResolver()} returns the one the settings select.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.TimestampResolver}. The source is a {@code sealed}
 * interface with a record implementation; here both implementations are final classes with value equality.
 */
@SdkPublicApi
public interface TimestampResolver {

    /**
     * Returns the formatter of a shape.
     *
     * @param schema the shape to resolve
     * @return the resolved formatter
     */
    TimestampFormatter resolve(Schema schema);

    /**
     * Returns the default formatter.
     *
     * @return the default formatter
     */
    TimestampFormatter defaultFormat();

    /**
     * Parses a timestamp from a JSON number or string value with a specific formatter.
     *
     * @param any the JSON value: a {@link Number} or a {@link String}
     * @param format the formatter used to parse the timestamp
     * @return the parsed instant
     * @throws SerializationException if the value is neither a number nor a string, or is not a valid timestamp
     */
    static Instant readTimestamp(Object any, TimestampFormatter format) {
        if (any instanceof Number) {
            return format.readFromNumber(((Number) any).doubleValue());
        }
        if (any instanceof String) {
            return format.readFromString((String) any, true);
        }
        throw new SerializationException("Expected a timestamp, but found "
                                         + (any == null ? "null" : any.getClass().getSimpleName()));
    }

    /**
     * Always returns the default timestamp format and ignores the {@code timestampFormat} trait.
     */
    final class StaticFormat implements TimestampResolver {
        private final TimestampFormatter defaultFormat;

        StaticFormat(TimestampFormatter defaultFormat) {
            this.defaultFormat = Objects.requireNonNull(defaultFormat, "defaultFormat must not be null");
        }

        @Override
        public TimestampFormatter resolve(Schema schema) {
            return defaultFormat;
        }

        @Override
        public TimestampFormatter defaultFormat() {
            return defaultFormat;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof StaticFormat && defaultFormat.equals(((StaticFormat) o).defaultFormat);
        }

        @Override
        public int hashCode() {
            return defaultFormat.hashCode();
        }

        @Override
        public String toString() {
            return "TimestampResolver{useTimestampFormat=false; default=" + defaultFormat + '}';
        }
    }

    /**
     * Uses the {@code timestampFormat} trait if present, otherwise the default format.
     */
    final class UseTimestampFormatTrait implements TimestampResolver {
        private final TimestampFormatter defaultFormat;

        UseTimestampFormatTrait(TimestampFormatter defaultFormat) {
            this.defaultFormat = Objects.requireNonNull(defaultFormat, "defaultFormat must not be null");
        }

        @Override
        public TimestampFormatter defaultFormat() {
            return defaultFormat;
        }

        @Override
        public TimestampFormatter resolve(Schema schema) {
            JsonSchemaExtensions.JsonSchemaExtension ext = schema.getExtension(JsonSchemaExtensions.KEY);
            if (ext != null && ext.timestampFormatter() != null) {
                return ext.timestampFormatter();
            }
            return defaultFormat;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof UseTimestampFormatTrait
                   && defaultFormat.equals(((UseTimestampFormatTrait) o).defaultFormat);
        }

        @Override
        public int hashCode() {
            return 31 + defaultFormat.hashCode();
        }

        @Override
        public String toString() {
            return "TimestampResolver{useTimestampFormat=true; default=" + defaultFormat + '}';
        }
    }
}
