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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/**
 * Schema-driven serialization of the sparse collections of the event notification shapes: lists of structures and
 * the string map of bucket tags. They are sparse, as in V2, so {@code null} elements and values are kept in both
 * directions.
 */
public final class SerdeSupport {
    private SerdeSupport() {
    }

    /** Writes the list member {@code member} of structures, writing {@code null} elements as nulls. */
    public static void writeStructList(ShapeSerializer serializer, Schema member,
                                       List<? extends SerializableStruct> values) {
        Schema element = member.listMember();
        serializer.writeList(member, values, values.size(), (list, s) -> {
            for (SerializableStruct value : list) {
                if (value == null) {
                    s.writeNull(element);
                } else {
                    s.writeStruct(element, value);
                }
            }
        });
    }

    /** Reads the list member {@code member} of structures, each through a new builder, keeping null elements. */
    public static <T extends SerializableStruct> List<T> readStructList(ShapeDeserializer deserializer, Schema member,
                                                                       Supplier<? extends ShapeBuilder<T>> builders) {
        Schema element = member.listMember();
        List<T> result = new ArrayList<>();
        deserializer.readList(member, result, (list, d) -> {
            if (d.isNull()) {
                d.readNull();
                list.add(null);
            } else {
                list.add(builders.get().deserializeMember(d, element).build());
            }
        });
        return result;
    }

    /** Writes the map member {@code member} of strings, writing {@code null} values as nulls. */
    public static void writeStringMap(ShapeSerializer serializer, Schema member, Map<String, String> values) {
        Schema key = member.mapKeyMember();
        Schema value = member.mapValueMember();
        serializer.writeMap(member, values, values.size(), (map, s) -> {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                s.writeEntry(key, entry.getKey(), entry.getValue(), (v, w) -> {
                    if (v == null) {
                        w.writeNull(value);
                    } else {
                        w.writeString(value, v);
                    }
                });
            }
        });
    }

    /** Reads the map member {@code member} of strings in document order, keeping null values. */
    public static Map<String, String> readStringMap(ShapeDeserializer deserializer, Schema member) {
        Schema value = member.mapValueMember();
        Map<String, String> result = new LinkedHashMap<>();
        deserializer.readStringMap(member, result, (map, key, d) -> {
            if (d.isNull()) {
                d.readNull();
                map.put(key, null);
            } else {
                map.put(key, d.readString(value));
            }
        });
        return result;
    }

    /** The exception for a member that does not belong to {@code schema}. */
    public static IllegalArgumentException unknownMember(Schema schema, Schema member) {
        return new IllegalArgumentException("Attempted to access a non-existent member of " + schema.id() + ": "
                                            + member.id());
    }
}
