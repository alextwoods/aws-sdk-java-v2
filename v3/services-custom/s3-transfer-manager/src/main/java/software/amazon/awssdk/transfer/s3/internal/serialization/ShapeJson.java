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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.transfer.s3.internal.util.Shapes;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;

/**
 * Maps the scalar members of an S3 request shape (the {@code PutObjectRequest} or {@code GetObjectRequest} of a
 * resumable transfer) to a JSON object keyed by modeled member name, and back, through the shape's schema. Strings and
 * enums are JSON strings, numbers JSON numbers, booleans JSON booleans, timestamps ISO-8601 strings and string maps
 * JSON objects. Unknown keys are ignored when reading so tokens written by a newer model stay readable. Not a public
 * API.
 */
final class ShapeJson {
    private ShapeJson() {
    }

    static Map<String, Object> toJson(SerializableStruct shape) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Schema member : shape.schema().members()) {
            Object value = shape.getMemberValue(member);
            if (value == null) {
                continue;
            }
            switch (member.type()) {
                case STRING, ENUM, BOOLEAN, INTEGER, INT_ENUM, LONG -> result.put(member.memberName(), value);
                case SHORT, BYTE -> result.put(member.memberName(), ((Number) value).intValue());
                case TIMESTAMP -> result.put(member.memberName(), value.toString());
                case MAP -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    ((Map<?, ?>) value).forEach((k, v) -> map.put(String.valueOf(k), String.valueOf(v)));
                    result.put(member.memberName(), map);
                }
                default -> throw SdkClientException.create(
                    "Cannot serialize member " + member.memberName() + " of type " + member.type()
                    + " in a resumable transfer token");
            }
        }
        return result;
    }

    static <B extends ShapeBuilder<?>> B fromJson(Object json, B builder) {
        if (!(json instanceof Map<?, ?> fields)) {
            throw SdkClientException.create("Invalid resumable transfer token: expected an object for "
                                            + builder.schema().id().getName());
        }
        for (Map.Entry<?, ?> e : fields.entrySet()) {
            Schema member = Shapes.memberOrNull(builder.schema(), String.valueOf(e.getKey()));
            if (member == null || e.getValue() == null) {
                continue;
            }
            builder.setMemberValue(member, convert(member, e.getValue()));
        }
        return builder;
    }

    private static Object convert(Schema member, Object value) {
        try {
            return switch (member.type()) {
                case STRING, ENUM -> (String) value;
                case BOOLEAN -> (Boolean) value;
                case INTEGER, INT_ENUM -> ((BigDecimal) value).intValueExact();
                case LONG -> ((BigDecimal) value).longValueExact();
                case SHORT -> ((BigDecimal) value).shortValueExact();
                case BYTE -> ((BigDecimal) value).byteValueExact();
                case TIMESTAMP -> Instant.parse((String) value);
                case MAP -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    ((Map<?, ?>) value).forEach((k, v) -> map.put(String.valueOf(k), (String) v));
                    yield map;
                }
                default -> throw SdkClientException.create("Unsupported member " + member.memberName());
            };
        } catch (ClassCastException | ArithmeticException | DateTimeParseException e) {
            throw SdkClientException.create("Invalid resumable transfer token: bad value for member "
                                            + member.memberName(), e);
        }
    }
}
