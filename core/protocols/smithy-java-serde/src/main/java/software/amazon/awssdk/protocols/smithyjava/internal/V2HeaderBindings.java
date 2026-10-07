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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Reads a response's header-bound and status-code-bound members the way v2 reads them, straight from the
 * shape's {@code SdkField}s.
 *
 * <p>These members do not go through smithy-java's HTTP binding because its binding index cannot express what
 * v2 does with them, and the differences are silent:
 *
 * <ul>
 *   <li><b>Two members on one header.</b> {@code GetObjectResponse} binds both {@code Expires} (a timestamp)
 *       and {@code ExpiresString} to the {@code Expires} header. smithy-java indexes one member per header
 *       name, so one of the two is always left null.</li>
 *   <li><b>Header-map key case.</b> {@code x-amz-meta-Color} must come back as {@code Color}; smithy-java's
 *       {@code HttpHeaders} lower-cases header names, so it came back as {@code color}. User metadata keys are
 *       data, not protocol.</li>
 *   <li><b>Tolerated conversion failures.</b> A field with {@code ignoreDataTypeConversionFailures} (S3's
 *       {@code Expires}, which the service has been known to fill with unparseable values) must be left null
 *       rather than failing the call.</li>
 *   <li><b>Status codes.</b> v2 binds {@code MarshallLocation.STATUS_CODE} members from the response status;
 *       the request-side schema translation has no trait for them.</li>
 * </ul>
 */
@SdkInternalApi
public final class V2HeaderBindings {

    private static final Logger log = Logger.loggerFor(V2HeaderBindings.class);

    private V2HeaderBindings() {
    }

    /** Whether {@code field} is one this class owns, rather than smithy-java's binding layer. */
    public static boolean isBound(SdkField<?> field) {
        return field.location() == MarshallLocation.HEADER || field.location() == MarshallLocation.STATUS_CODE;
    }

    /**
     * Sets every header- and status-code-bound member of {@code builder} from {@code response}. Overwrites
     * whatever the binding layer put there, so that these members are v2's reading of the response and not a
     * mix of the two.
     */
    public static void apply(SdkPojo builder, List<SdkField<?>> fields, SdkHttpResponse response) {
        for (SdkField<?> field : fields) {
            if (!isBound(field)) {
                continue;
            }
            if (field.location() == MarshallLocation.STATUS_CODE) {
                field.set(builder, response.statusCode());
                continue;
            }
            LocationTrait location = field.getTrait(LocationTrait.class);
            String name = location != null && location.locationName() != null ? location.locationName()
                                                                              : field.memberName();
            if (field.marshallingType() == MarshallingType.MAP) {
                field.set(builder, prefixHeaders(response, name));
            } else if (field.marshallingType() == MarshallingType.LIST) {
                List<String> values = response.matchingHeaders(name);
                field.set(builder, values.isEmpty() ? null : new ArrayList<>(values));
            } else {
                String value = response.firstMatchingHeader(name).orElse(null);
                if (value != null) {
                    field.set(builder, convert(value, field, name));
                }
            }
        }
    }

    // v2's HeaderUnmarshaller.MAP: every header whose name starts with the prefix, case-insensitively, keyed by
    // the rest of the name with its original case, values comma-joined.
    private static Map<String, String> prefixHeaders(SdkHttpResponse response, String prefix) {
        Map<String, String> values = new LinkedHashMap<>();
        String lowerPrefix = prefix.toLowerCase(Locale.US);
        response.forEachHeader((name, value) -> {
            if (name.length() >= prefix.length() && name.toLowerCase(Locale.US).startsWith(lowerPrefix)) {
                values.put(name.substring(prefix.length()), String.join(",", value));
            }
        });
        return values;
    }

    private static Object convert(String value, SdkField<?> field, String name) {
        try {
            MarshallingType<?> type = field.marshallingType();
            if (type == MarshallingType.STRING) {
                return value;
            } else if (type == MarshallingType.INTEGER) {
                return Integer.parseInt(value);
            } else if (type == MarshallingType.LONG) {
                return Long.parseLong(value);
            } else if (type == MarshallingType.SHORT) {
                return Short.parseShort(value);
            } else if (type == MarshallingType.FLOAT) {
                return Float.parseFloat(value);
            } else if (type == MarshallingType.DOUBLE) {
                return Double.parseDouble(value);
            } else if (type == MarshallingType.BIG_DECIMAL) {
                return new BigDecimal(value);
            } else if (type == MarshallingType.BOOLEAN) {
                return Boolean.parseBoolean(value);
            } else if (type == MarshallingType.INSTANT) {
                return instant(value, field);
            }
            return value;
        } catch (RuntimeException e) {
            // Same choice v2's SimpleHeaderUnmarshaller makes: warn, and only swallow when the field says so.
            log.warn(() -> "Exception found while parsing response header " + name, e);
            if (field.ignoreDataTypeConversionFailures()) {
                return null;
            }
            throw e;
        }
    }

    private static Instant instant(String value, SdkField<?> field) {
        TimestampFormatTrait trait = field.getTrait(TimestampFormatTrait.class);
        TimestampFormatTrait.Format format = trait != null ? trait.format() : TimestampFormatTrait.Format.RFC_822;
        switch (format) {
            case ISO_8601:
                return DateUtils.parseIso8601Date(value);
            case UNIX_TIMESTAMP:
                return DateUtils.parseUnixTimestampInstant(value);
            case UNIX_TIMESTAMP_MILLIS:
                return DateUtils.parseUnixTimestampMillisInstant(value);
            case RFC_822:
            default:
                return DateUtils.parseRfc822Date(value);
        }
    }
}
