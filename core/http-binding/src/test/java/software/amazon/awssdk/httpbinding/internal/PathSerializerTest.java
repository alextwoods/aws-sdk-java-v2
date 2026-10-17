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

package software.amazon.awssdk.httpbinding.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SmithyEnum;
import software.amazon.awssdk.schema.SmithyIntEnum;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;

/**
 * Ported from smithy-java {@code PathSerializerTest}, plus the empty-label and pattern-shape cases.
 */
class PathSerializerTest {
    private static final ShapeId INPUT_ID = ShapeId.from("smithy.example#Input");
    private static final ShapeId COLOR_ID = ShapeId.from("smithy.example#Color");
    private static final ShapeId COUNT_ID = ShapeId.from("smithy.example#Count");

    // Each case binds a single member `v` as a label and asserts the serialized path. Cases cover every label type
    // in each value shape a struct can hand back: a raw Java value, a SmithyEnum/SmithyIntEnum, and a Document.
    static List<Arguments> labelCases() {
        Instant ts = Instant.parse("2020-01-02T03:04:05Z");
        return Arrays.asList(
            Arguments.of(PreludeSchemas.STRING, "abc", "/foo/abc"),
            Arguments.of(PreludeSchemas.STRING, Document.of("abc"), "/foo/abc"),
            Arguments.of(enumSchema(), smithyEnum("RED"), "/foo/RED"),
            Arguments.of(enumSchema(), Document.of("RED"), "/foo/RED"),
            Arguments.of(PreludeSchemas.BOOLEAN, true, "/foo/true"),
            Arguments.of(PreludeSchemas.BOOLEAN, Document.of(true), "/foo/true"),
            Arguments.of(PreludeSchemas.BYTE, (byte) 7, "/foo/7"),
            Arguments.of(PreludeSchemas.BYTE, Document.of((byte) 7), "/foo/7"),
            Arguments.of(PreludeSchemas.SHORT, (short) 8, "/foo/8"),
            Arguments.of(PreludeSchemas.SHORT, Document.of((short) 8), "/foo/8"),
            Arguments.of(PreludeSchemas.INTEGER, 9, "/foo/9"),
            Arguments.of(PreludeSchemas.INTEGER, Document.of(9), "/foo/9"),
            Arguments.of(intEnumSchema(), smithyIntEnum(2), "/foo/2"),
            Arguments.of(intEnumSchema(), Document.of(2), "/foo/2"),
            Arguments.of(PreludeSchemas.LONG, 10L, "/foo/10"),
            Arguments.of(PreludeSchemas.LONG, Document.of(10L), "/foo/10"),
            Arguments.of(PreludeSchemas.FLOAT, 1.5f, "/foo/1.5"),
            Arguments.of(PreludeSchemas.FLOAT, Document.of(1.5f), "/foo/1.5"),
            Arguments.of(PreludeSchemas.DOUBLE, 2.5d, "/foo/2.5"),
            Arguments.of(PreludeSchemas.DOUBLE, Document.of(2.5d), "/foo/2.5"),
            Arguments.of(PreludeSchemas.BIG_INTEGER, BigInteger.valueOf(11), "/foo/11"),
            Arguments.of(PreludeSchemas.BIG_INTEGER, Document.of(BigInteger.valueOf(11)), "/foo/11"),
            Arguments.of(PreludeSchemas.BIG_DECIMAL, new BigDecimal("12.5"), "/foo/12.5"),
            Arguments.of(PreludeSchemas.BIG_DECIMAL, Document.of(new BigDecimal("12.5")), "/foo/12.5"),
            // timestamp: date-time by default for a label; ':' is percent-encoded
            Arguments.of(PreludeSchemas.TIMESTAMP, ts, "/foo/2020-01-02T03%3A04%3A05Z"),
            Arguments.of(PreludeSchemas.TIMESTAMP, Document.of(ts), "/foo/2020-01-02T03%3A04%3A05Z"));
    }

    @ParameterizedTest
    @MethodSource("labelCases")
    void serializesLabel(Schema memberTarget, Object value, String expected) {
        Schema schema = Schema.structureBuilder(INPUT_ID)
                              .putMember("v", memberTarget, HttpLabelTrait.INSTANCE)
                              .build();
        PathSerializer serializer = new PathSerializer(UriPattern.parse("/foo/{v}"), schema);
        assertThat(serializer.serialize(struct(schema, Collections.singletonMap("v", value)))).isEqualTo(expected);
    }

    @Test
    void staticPathUsesTheLiteral() {
        Schema schema = Schema.structureBuilder(INPUT_ID).build();
        assertThat(new PathSerializer(UriPattern.parse("/"), schema).serialize(null)).isEqualTo("/");
        assertThat(new PathSerializer(UriPattern.parse("/a/b/"), schema).serialize(null)).isEqualTo("/a/b/");
    }

    @Test
    void literalsAroundLabelsAndGreedyLabels() {
        Schema schema = Schema.structureBuilder(INPUT_ID)
                              .putMember("a", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
                              .putMember("b", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
                              .build();
        PathSerializer serializer = new PathSerializer(UriPattern.parse("/x/{a}/y/{b+}?q=1"), schema);
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("a", "1/2");
        values.put("b", "3/4 5");
        assertThat(serializer.serialize(struct(schema, values))).isEqualTo("/x/1%2F2/y/3/4%205");
    }

    @Test
    void nullOrEmptyLabelThrowsNamingTheMember() {
        Schema schema = Schema.structureBuilder(INPUT_ID)
                              .putMember("v", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
                              .build();
        PathSerializer serializer = new PathSerializer(UriPattern.parse("/foo/{v}"), schema);
        assertThatThrownBy(() -> serializer.serialize(struct(schema, Collections.emptyMap())))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Required httpLabel member `smithy.example#Input$v` must not be null");
        assertThatThrownBy(() -> serializer.serialize(struct(schema, Collections.singletonMap("v", ""))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Required httpLabel member `smithy.example#Input$v` must not be empty");
    }

    @Test
    void labelWithoutMemberIsRejected() {
        Schema schema = Schema.structureBuilder(INPUT_ID).build();
        assertThatThrownBy(() -> new PathSerializer(UriPattern.parse("/foo/{missing}"), schema))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing");
    }

    private static Schema enumSchema() {
        return Schema.createEnum(COLOR_ID, new HashSet<>(Arrays.asList("RED", "GREEN")));
    }

    private static Schema intEnumSchema() {
        return Schema.createIntEnum(COUNT_ID, new HashSet<>(Arrays.asList(1, 2)));
    }

    private static SmithyEnum smithyEnum(String value) {
        return new SmithyEnum() {
            @Override
            public String getValue() {
                return value;
            }

            @Override
            public Schema sdkSchema() {
                return enumSchema();
            }
        };
    }

    private static SmithyIntEnum smithyIntEnum(int value) {
        return new SmithyIntEnum() {
            @Override
            public int getValue() {
                return value;
            }

            @Override
            public Schema sdkSchema() {
                return intEnumSchema();
            }
        };
    }

    private static SerializableStruct struct(Schema schema, Map<String, Object> values) {
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return schema;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                // Not exercised by PathSerializer.
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getMemberValue(Schema member) {
                return (T) values.get(member.memberName());
            }
        };
    }
}
