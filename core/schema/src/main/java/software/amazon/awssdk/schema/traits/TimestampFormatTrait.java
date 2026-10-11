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

package software.amazon.awssdk.schema.traits;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;

/**
 * The {@code smithy.api#timestampFormat} trait: the serialization format of a timestamp.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.TimestampFormatTrait}. As in smithy-model, a
 * value other than the three defined formats is kept and reported as {@link Format#UNKNOWN}.
 */
@SdkPublicApi
public final class TimestampFormatTrait extends StringTrait {

    /**
     * The {@code epoch-seconds} format.
     */
    public static final String EPOCH_SECONDS = "epoch-seconds";

    /**
     * The {@code date-time} format (RFC 3339).
     */
    public static final String DATE_TIME = "date-time";

    /**
     * The {@code http-date} format (RFC 7231 IMF-fixdate).
     */
    public static final String HTTP_DATE = "http-date";

    /**
     * The trait's shape ID, {@code smithy.api#timestampFormat}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "timestampFormat");

    private final Format format;

    /**
     * Creates the trait.
     *
     * @param value the format name, for example {@link #EPOCH_SECONDS}
     * @throws NullPointerException if {@code value} is null
     */
    public TimestampFormatTrait(String value) {
        super(ID, value);
        this.format = Format.fromString(value);
    }

    /**
     * Returns the format.
     *
     * @return the format, or {@link Format#UNKNOWN} for an unrecognized value
     */
    public Format getFormat() {
        return format;
    }

    /**
     * The timestamp formats.
     */
    public enum Format {
        EPOCH_SECONDS(TimestampFormatTrait.EPOCH_SECONDS),
        DATE_TIME(TimestampFormatTrait.DATE_TIME),
        HTTP_DATE(TimestampFormatTrait.HTTP_DATE),
        UNKNOWN("unknown");

        private final String value;

        Format(String value) {
            this.value = value;
        }

        /**
         * Returns the format with the given name.
         *
         * @param value the format name
         * @return the format, or {@link #UNKNOWN} if the name is not recognized
         */
        public static Format fromString(String value) {
            for (Format format : values()) {
                if (format != UNKNOWN && format.value.equals(value)) {
                    return format;
                }
            }
            return UNKNOWN;
        }

        /**
         * Returns the format name, for example {@code epoch-seconds}.
         *
         * @return the format name
         */
        @Override
        public String toString() {
            return value;
        }
    }
}
