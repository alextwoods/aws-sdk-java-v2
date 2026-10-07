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

package software.amazon.awssdk.protocols.smithyjava.internal;

import java.math.BigDecimal;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.TimestampFormatter;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Epoch-seconds timestamps written the way v2 writes them: millisecond precision, always three decimals,
 * never an exponent ({@code 1700000000.123}, {@code 1700000000.000}).
 *
 * <p>smithy-java's own epoch-seconds formatter writes a {@code double}, which comes out as
 * {@code 1700000000} for a whole second and {@code 1.700000000123E9} otherwise. Both are valid JSON numbers
 * for the same instant, but they are different bytes than v2 sends, so this is the codec's default
 * timestamp format for JSON bodies. Reading is unchanged: both spellings parse to the same instant.
 */
@SdkInternalApi
public final class V2EpochSecondsFormatter implements TimestampFormatter {

    public static final V2EpochSecondsFormatter INSTANCE = new V2EpochSecondsFormatter();

    private V2EpochSecondsFormatter() {
    }

    @Override
    public TimestampFormatTrait.Format format() {
        return TimestampFormatTrait.Format.EPOCH_SECONDS;
    }

    @Override
    public String writeString(Instant value) {
        return toBigDecimal(value).toPlainString();
    }

    @Override
    public void writeToSerializer(Schema schema, Instant value, ShapeSerializer serializer) {
        serializer.writeBigDecimal(schema, toBigDecimal(value));
    }

    @Override
    public Instant readFromString(String value, boolean strict) {
        return Prelude.EPOCH_SECONDS.readFromString(value, strict);
    }

    @Override
    public Instant readFromNumber(Number value) {
        return Prelude.EPOCH_SECONDS.readFromNumber(value);
    }

    // Same arithmetic as DateUtils.formatUnixTimestampInstant: truncate to milliseconds, scale by 10^-3.
    private static BigDecimal toBigDecimal(Instant value) {
        return BigDecimal.valueOf(value.toEpochMilli()).scaleByPowerOfTen(-3);
    }
}
