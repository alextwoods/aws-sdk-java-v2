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

package software.amazon.awssdk.codecs.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codecs.xml.TestCollections.listOf;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.ToStringSerializer;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.xml.XmlCodecTest}.
 */
public class XmlCodecTest extends ProviderTestBase {

    // The shape's root element is <Foo>; this document wraps the same members in a different root name.
    private static final String WRONG_ROOT = "<CopyObjectResult>\n"
                                             + "    <name>Hello</name>\n"
                                             + "    <numbers>\n"
                                             + "        <member>1</member>\n"
                                             + "        <member>2</member>\n"
                                             + "    </numbers>\n"
                                             + "</CopyObjectResult>\n";

    @Test
    public void defaultsToSmithyProvider() {
        try (XmlCodec codec = XmlCodec.builder().build();
             ShapeSerializer serializer = codec.createSerializer(new ByteArrayOutputStream())) {
            assertThat(serializer.getClass().getSimpleName()).isEqualTo("LazyXmlSerializer");
        }
    }

    @PerProvider
    public void deserializesXml(boolean useNative) {
        String xml = "<Foo>\n"
                     + "    <date>2006-03-01T00:00:00Z</date>\n"
                     + "    <name>Hello</name>\n"
                     + "    <numbers>\n"
                     + "        <member>1</member>\n"
                     + "        <member>2</member>\n"
                     + "        <member>3</member>\n"
                     + "    </numbers>\n"
                     + "</Foo>\n";

        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).build()) {
            TestPojo pojo = codec.deserializeShape(xml, new TestPojo.Builder());

            assertThat(pojo.name).isEqualTo("Hello");
            assertThat(pojo.date).isEqualTo(Instant.parse("2006-03-01T00:00:00Z"));
            assertThat(pojo.numbers).containsExactly(1, 2, 3);
        }
    }

    @PerProvider
    public void strictRootElementRejectsMismatchedRoot(boolean useNative) {
        // Default (strict) — a root name that doesn't match the shape is rejected. Server behavior.
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).build()) {
            assertThatThrownBy(() -> codec.deserializeShape(WRONG_ROOT, new TestPojo.Builder()))
                .isInstanceOf(SerializationException.class);
        }
    }

    @PerProvider
    public void lenientRootElementToleratesMismatchedRoot(boolean useNative) {
        // strictRootElement(false) — the mismatched root is ignored and the children are read. Client
        // behavior, e.g. S3's <CopyObjectResult> for a CopyObjectOutput shape.
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).strictRootElement(false).build()) {
            TestPojo pojo = codec.deserializeShape(WRONG_ROOT, new TestPojo.Builder());
            assertThat(pojo.name).isEqualTo("Hello");
            assertThat(pojo.numbers).containsExactly(1, 2);
        }
    }

    @PerProvider
    public void matchingRootStillWorksInBothModes(boolean useNative) {
        // A correct root name (<Foo>) deserializes regardless of the strict setting.
        String xml = "<Foo><name>Hi</name></Foo>";
        try (XmlCodec strict = XmlCodec.builder().useNative(useNative).build();
             XmlCodec lenient = XmlCodec.builder().useNative(useNative).strictRootElement(false).build()) {
            assertThat(strict.deserializeShape(xml, new TestPojo.Builder()).name).isEqualTo("Hi");
            assertThat(lenient.deserializeShape(xml, new TestPojo.Builder()).name).isEqualTo("Hi");
        }
    }

    @PerProvider
    public void deserializesEmptyBody(boolean useNative) {
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).build()) {
            TestPojo pojo = codec.deserializeShape(ByteBuffer.allocate(0), new TestPojo.Builder());
            assertThat(pojo.name).isNull();
            assertThat(pojo.date).isNull();
            assertThat(pojo.numbers).isEmpty();
        }
    }

    @PerProvider
    public void serializesXml(boolean useNative) {
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).build()) {
            TestPojo.Builder builder = new TestPojo.Builder();
            builder.name = "Hello";
            builder.date = Instant.parse("2006-03-01T00:00:00Z");
            builder.numbers.addAll(listOf(1, 2, 3));
            TestPojo pojo = builder.build();
            String xml = codec.serializeToString(pojo);

            assertThat(xml).isEqualTo("<Foo><name>Hello</name><date>2006-03-01T00:00:00Z</date><numbers><member>1</member>"
                                      + "<member>2</member><member>3</member></numbers></Foo>");
        }
    }

    @PerProvider
    public void deserializesMapWithEmptyBlobValue(boolean useNative) {
        String xml = "<MapPojo><value><entry><key>k1</key><value></value></entry>"
                     + "<entry><key>k2</key><value>AQID</value></entry></value></MapPojo>";
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).build()) {
            MapPojo pojo = codec.deserializeShape(xml, new MapPojo.Builder());
            assertThat(pojo.value).hasSize(2);
            assertThat(pojo.value.get("k1")).isEqualTo(ByteBuffer.wrap(new byte[0]));
            assertThat(pojo.value.get("k2")).isEqualTo(ByteBuffer.wrap(new byte[] {1, 2, 3}));
        }
    }

    @Test
    public void nativeAndStaxProduceSameResultForBlobMap() {
        String xml = "<MapPojo><value>"
                     + "<entry><key>k1</key><value></value></entry>"
                     + "<entry><key>k2</key><value>AQID</value></entry>"
                     + "<entry><key>k3</key><value>BAUG</value></entry>"
                     + "<entry><key>k4</key><value>BwgJ</value></entry>"
                     + "</value></MapPojo>";
        try (XmlCodec stax = XmlCodec.builder().useNative(false).build();
             XmlCodec nativeCodec = XmlCodec.builder().useNative(true).build()) {
            MapPojo staxResult = stax.deserializeShape(xml, new MapPojo.Builder());
            MapPojo nativeResult = nativeCodec.deserializeShape(xml, new MapPojo.Builder());
            assertThat(nativeResult.value).hasSameSizeAs(staxResult.value);
            for (Map.Entry<String, ByteBuffer> entry : staxResult.value.entrySet()) {
                assertThat(nativeResult.value.get(entry.getKey())).as("key=" + entry.getKey()).isEqualTo(entry.getValue());
            }
        }
    }

    private static final class MapPojo implements SerializableStruct {
        private static final ShapeId ID = ShapeId.from("smithy.example#MapPojo");
        private static final Schema BLOB_MAP = Schema.mapBuilder(ShapeId.from("smithy.example#BlobMap"))
                                                     .putMember("key", PreludeSchemas.STRING)
                                                     .putMember("value", PreludeSchemas.BLOB)
                                                     .build();
        private static final Schema SCHEMA = Schema.structureBuilder(ID)
                                                   .putMember("value", BLOB_MAP)
                                                   .build();
        private static final Schema VALUE = SCHEMA.member("value");

        private final Map<String, ByteBuffer> value;

        MapPojo(Builder builder) {
            this.value = builder.value;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (value != null) {
                serializer.writeMap(VALUE, value, value.size(), (map, ms) -> {
                    for (Map.Entry<String, ByteBuffer> e : map.entrySet()) {
                        ms.writeEntry(BLOB_MAP.mapKeyMember(), e.getKey(), e.getValue(),
                                      (v, s) -> s.writeBlob(BLOB_MAP.mapValueMember(), v));
                    }
                });
            }
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new UnsupportedOperationException();
        }

        private static final class Builder implements ShapeBuilder<MapPojo> {
            private final Map<String, ByteBuffer> value = new LinkedHashMap<>();

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (pojo, member, deser) -> {
                    if ("value".equals(member.memberName())) {
                        deser.readStringMap(VALUE, pojo.value, (map, key, de) -> {
                            map.put(key, de.readBlob(BLOB_MAP.mapValueMember()));
                        });
                    }
                });
                return this;
            }

            @Override
            public MapPojo build() {
                return new MapPojo(this);
            }
        }
    }

    private static final class TestPojo implements SerializableStruct {
        private static final ShapeId ID = ShapeId.from("smithy.example#Foo");
        private static final Schema NUMBERS_LIST = Schema.listBuilder(ShapeId.from("smithy.example#Numbers"))
                                                         .putMember("member", PreludeSchemas.INTEGER)
                                                         .build();
        private static final Schema SCHEMA = Schema.structureBuilder(ID)
                                                   .putMember("name", PreludeSchemas.STRING)
                                                   .putMember("binary", PreludeSchemas.BLOB, new JsonNameTrait("BINARY"))
                                                   .putMember("date", PreludeSchemas.TIMESTAMP,
                                                              new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
                                                   .putMember("numbers", NUMBERS_LIST)
                                                   .build();
        private static final Schema NAME = SCHEMA.member("name");
        private static final Schema BINARY = SCHEMA.member("binary");
        private static final Schema DATE = SCHEMA.member("date");
        private static final Schema NUMBERS = SCHEMA.member("numbers");

        private final String name;
        private final ByteBuffer binary;
        private final Instant date;
        private final List<Integer> numbers;

        TestPojo(Builder builder) {
            this.name = builder.name;
            this.binary = builder.binary;
            this.date = builder.date;
            this.numbers = builder.numbers;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (name != null) {
                serializer.writeString(NAME, name);
            }
            if (binary != null) {
                serializer.writeBlob(BINARY, binary);
            }
            if (date != null) {
                serializer.writeTimestamp(DATE, date);
            }
            if (numbers != null) {
                serializer.writeList(NUMBERS, numbers, numbers.size(), (elements, ser) -> {
                    for (Integer e : elements) {
                        ser.writeInteger(PreludeSchemas.INTEGER, e);
                    }
                });
            }
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

        private static final class Builder implements ShapeBuilder<TestPojo> {
            private final List<Integer> numbers = new ArrayList<>();
            private String name;
            private ByteBuffer binary;
            private Instant date;

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (pojo, member, deser) -> {
                    switch (member.memberName()) {
                        case "name":
                            pojo.name = deser.readString(NAME);
                            break;
                        case "binary":
                            pojo.binary = deser.readBlob(BINARY);
                            break;
                        case "date":
                            pojo.date = deser.readTimestamp(DATE);
                            break;
                        case "numbers":
                            deser.readList(NUMBERS, pojo.numbers, (values, de) -> {
                                values.add(de.readInteger(NUMBERS_LIST.member("member")));
                            });
                            break;
                        default:
                            throw new UnsupportedOperationException(member.toString());
                    }
                });
                return this;
            }

            @Override
            public TestPojo build() {
                return new TestPojo(this);
            }
        }
    }
}
