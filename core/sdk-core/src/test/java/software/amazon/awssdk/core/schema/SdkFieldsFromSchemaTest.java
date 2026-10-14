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

package software.amazon.awssdk.core.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.schema.SdkFieldsFromSchema.HttpBindingMode;
import software.amazon.awssdk.core.schema.SdkFieldsFromSchema.WireNaming;
import software.amazon.awssdk.core.traits.DefaultValueTrait;
import software.amazon.awssdk.core.traits.JsonValueTrait;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.core.traits.XmlAttributesTrait;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.Ec2QueryNameTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;
import software.amazon.awssdk.schema.traits.HttpPayloadTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpResponseCodeTrait;
import software.amazon.awssdk.schema.traits.IdempotencyTokenTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;
import software.amazon.awssdk.schema.traits.XmlFlattenedTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

class SdkFieldsFromSchemaTest {
    private static final String NS = "test";

    private static final Schema JSON_STRING = Schema.createString(ShapeId.fromParts(NS, "JsonString"),
                                                                  new MediaTypeTrait("application/json"));
    private static final Schema EPOCH = Schema.createTimestamp(ShapeId.fromParts(NS, "Epoch"),
                                                               new software.amazon.awssdk.schema.traits.TimestampFormatTrait(
                                                                   "epoch-seconds"));
    private static final Schema NAMES = Schema.listBuilder(ShapeId.fromParts(NS, "Names"))
                                              .putMember("member", PreludeSchemas.STRING, new XmlNameTrait("Name"))
                                              .build();
    private static final Schema TAGS = Schema.mapBuilder(ShapeId.fromParts(NS, "Tags"))
                                             .putMember("key", PreludeSchemas.STRING, new XmlNameTrait("K"))
                                             .putMember("value", PreludeSchemas.INTEGER)
                                             .build();
    private static final Schema NESTED = Schema.structureBuilder(ShapeId.fromParts(NS, "Nested"),
                                                                 new XmlNamespaceTrait("https://example.com", "ex"))
                                               .putMember("attr", PreludeSchemas.STRING,
                                                          software.amazon.awssdk.schema.traits.XmlAttributeTrait.INSTANCE)
                                               .putMember("value", PreludeSchemas.STRING)
                                               .builderSupplier(TestStruct.Builder::new)
                                               .build();
    private static final Schema INPUT = Schema.structureBuilder(ShapeId.fromParts(NS, "Input"))
                                              .putMember("Plain", PreludeSchemas.STRING, new JsonNameTrait("plainJson"),
                                                         new XmlNameTrait("plainXml"),
                                                         software.amazon.awssdk.schema.traits.RequiredTrait.INSTANCE)
                                              .putMember("Header", JSON_STRING, new HttpHeaderTrait("x-header"))
                                              .putMember("Query", PreludeSchemas.INTEGER, new HttpQueryTrait("q"))
                                              .putMember("Label", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
                                              .putMember("Body", NESTED, HttpPayloadTrait.INSTANCE)
                                              .putMember("Names", NAMES, XmlFlattenedTrait.INSTANCE)
                                              .putMember("Tags", TAGS)
                                              .putMember("When", EPOCH)
                                              .putMember("Token", PreludeSchemas.STRING, IdempotencyTokenTrait.INSTANCE)
                                              .putMember("Status", PreludeSchemas.INTEGER, HttpResponseCodeTrait.INSTANCE)
                                              .putMember("camelName", PreludeSchemas.STRING,
                                                         new Ec2QueryNameTrait("EcName"))
                                              .putMember("Stream", Schema.createBlob(ShapeId.fromParts(NS, "Stream"),
                                                                                     StreamingTrait.INSTANCE))
                                              .builderSupplier(TestStruct.Builder::new)
                                              .build();

    private static final SdkFieldsFromSchema JSON_MODEL = SdkFieldsFromSchema.builder().wireNames(WireNaming.JSON).build();
    private static final SdkFieldsFromSchema XML_REQUEST = SdkFieldsFromSchema.builder()
                                                                              .wireNames(WireNaming.XML)
                                                                              .httpBindings(HttpBindingMode.REQUEST)
                                                                              .requiredTraits(true)
                                                                              .build();

    @Test
    void fields_followMemberOrderAndSkipStreamingMembers() {
        assertThat(JSON_MODEL.fields(INPUT).stream().map(SdkField::memberName).collect(Collectors.toList()))
            .containsExactly("Plain", "Header", "Query", "Label", "Body", "Names", "Tags", "When", "Token", "Status",
                             "camelName");
    }

    @Test
    void fields_areDerivedOncePerSchemaForTheFirstSettings() {
        Schema schema = Schema.structureBuilder(ShapeId.fromParts(NS, "Cached"))
                              .putMember("a", PreludeSchemas.STRING, new JsonNameTrait("j"))
                              .build();
        List<SdkField<?>> first = JSON_MODEL.fields(schema);
        assertThat(JSON_MODEL.fields(schema)).isSameAs(first);
        assertThat(JSON_MODEL.fieldsByName(schema)).isSameAs(JSON_MODEL.fieldsByName(schema));
        SdkFieldsFromSchema equalSettings = SdkFieldsFromSchema.builder().wireNames(WireNaming.JSON).build();
        assertThat(equalSettings.fields(schema)).isSameAs(first);

        // Other settings derive their own fields each time, without replacing the cached ones.
        List<SdkField<?>> xml = XML_REQUEST.fields(schema);
        assertThat(xml).isNotSameAs(first);
        assertThat(location(xml, "a").locationName()).isEqualTo("a");
        assertThat(location(first, "a").locationName()).isEqualTo("j");
        assertThat(JSON_MODEL.fields(schema)).isSameAs(first);
    }

    @Test
    void modelSettings_ignoreHttpBindings() {
        List<SdkField<?>> fields = JSON_MODEL.fields(INPUT);
        assertThat(location(fields, "Header").location()).isEqualTo(MarshallLocation.PAYLOAD);
        assertThat(location(fields, "Header").locationName()).isEqualTo("Header");
        assertThat(field(fields, "Header").containsTrait(JsonValueTrait.class)).isFalse();
        assertThat(field(fields, "Body").containsTrait(PayloadTrait.class)).isFalse();
        assertThat(location(fields, "Status").location()).isEqualTo(MarshallLocation.PAYLOAD);
        assertThat(field(fields, "Plain").containsTrait(RequiredTrait.class)).isFalse();
    }

    @Test
    void requestSettings_applyRequestBindings() {
        List<SdkField<?>> fields = XML_REQUEST.fields(INPUT);
        assertThat(location(fields, "Plain").locationName()).isEqualTo("plainXml");
        assertThat(location(fields, "Header").location()).isEqualTo(MarshallLocation.HEADER);
        assertThat(location(fields, "Header").locationName()).isEqualTo("x-header");
        assertThat(field(fields, "Header").containsTrait(JsonValueTrait.class)).isTrue();
        assertThat(location(fields, "Query").location()).isEqualTo(MarshallLocation.QUERY_PARAM);
        assertThat(location(fields, "Query").locationName()).isEqualTo("q");
        assertThat(location(fields, "Label").location()).isEqualTo(MarshallLocation.PATH);
        assertThat(location(fields, "Label").locationName()).isEqualTo("Label");
        assertThat(field(fields, "Body").containsTrait(PayloadTrait.class)).isTrue();
        assertThat(location(fields, "Status").location()).isEqualTo(MarshallLocation.PAYLOAD);
        assertThat(field(fields, "Plain").containsTrait(RequiredTrait.class)).isTrue();
        assertThat(field(fields, "Names").containsTrait(RequiredTrait.class)).isFalse();
    }

    @Test
    void responseSettings_applyResponseBindings() {
        SdkFieldsFromSchema response = SdkFieldsFromSchema.builder().httpBindings(HttpBindingMode.RESPONSE).build();
        List<SdkField<?>> fields = response.fields(INPUT);
        assertThat(location(fields, "Header").location()).isEqualTo(MarshallLocation.HEADER);
        assertThat(location(fields, "Status").location()).isEqualTo(MarshallLocation.STATUS_CODE);
        assertThat(location(fields, "Query").location()).isEqualTo(MarshallLocation.PAYLOAD);
        assertThat(location(fields, "Label").location()).isEqualTo(MarshallLocation.PAYLOAD);
    }

    @Test
    void containers_haveListAndMapTraits() {
        List<SdkField<?>> fields = XML_REQUEST.fields(INPUT);
        SdkField<?> names = field(fields, "Names");
        assertThat(names.marshallingType()).isSameAs(MarshallingType.LIST);
        ListTrait list = names.getRequiredTrait(ListTrait.class);
        assertThat(list.memberLocationName()).isEqualTo("Name");
        assertThat(list.isFlattened()).isTrue();
        assertThat(list.memberFieldInfo().marshallingType()).isSameAs(MarshallingType.STRING);
        assertThat(list.memberFieldInfo().locationName()).isEqualTo("Name");

        MapTrait map = field(fields, "Tags").getRequiredTrait(MapTrait.class);
        assertThat(map.keyLocationName()).isEqualTo("K");
        assertThat(map.valueLocationName()).isEqualTo("value");
        assertThat(map.isFlattened()).isFalse();
        assertThat(map.valueFieldInfo().marshallingType()).isSameAs(MarshallingType.INTEGER);
        assertThat(map.valueFieldInfo().locationName()).isEqualTo("value");
    }

    @Test
    void scalarTraits_matchTheMemberTraits() {
        List<SdkField<?>> fields = JSON_MODEL.fields(INPUT);
        assertThat(field(fields, "When").getRequiredTrait(TimestampFormatTrait.class).format())
            .isEqualTo(TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        assertThat(field(fields, "Token").getRequiredTrait(DefaultValueTrait.class).resolveValue(null)).isNotNull();
        assertThat(field(fields, "Body").marshallingType()).isSameAs(MarshallingType.SDK_POJO);
        assertThat(field(fields, "Body").constructor().get()).isInstanceOf(TestStruct.Builder.class);
        XmlAttributesTrait attributes = field(fields, "Body").getRequiredTrait(XmlAttributesTrait.class);
        assertThat(attributes.attributes().keySet()).containsExactly("xmlns:ex", "attr");
        assertThat(attributes.attributes().get("xmlns:ex").attributeGetter().apply(null)).isEqualTo("https://example.com");
        assertThat(field(JSON_MODEL.fields(NESTED), "attr").containsTrait(XmlAttributeTrait.class)).isTrue();
    }

    @Test
    void ec2_upperCasesMarshallNames() {
        SdkFieldsFromSchema ec2 = SdkFieldsFromSchema.builder().wireNames(WireNaming.EC2).build();
        List<SdkField<?>> fields = ec2.fields(INPUT);
        assertThat(location(fields, "camelName").locationName()).isEqualTo("EcName");
        assertThat(location(fields, "camelName").unmarshallLocationName()).isEqualTo("camelName");
        assertThat(location(fields, "Plain").locationName()).isEqualTo("PlainXml");
        assertThat(location(fields, "Plain").unmarshallLocationName()).isEqualTo("plainXml");
        assertThat(field(fields, "Names").getRequiredTrait(ListTrait.class).memberFieldInfo().locationName())
            .isEqualTo("Name");
    }

    @Test
    void fieldsByName_useTheMarshallLocationName() {
        Map<String, SdkField<?>> byName = JSON_MODEL.fieldsByName(INPUT);
        assertThat(byName).containsKeys("plainJson", "Header", "camelName");
        assertThat(byName.get("plainJson").memberName()).isEqualTo("Plain");
    }

    @Test
    void errorMessageMember_isLeftOut() {
        Schema error = Schema.structureBuilder(ShapeId.fromParts(NS, "Oops"), new ErrorTrait("client"))
                             .putMember("Message", PreludeSchemas.STRING)
                             .putMember("Reason", PreludeSchemas.STRING)
                             .build();
        assertThat(JSON_MODEL.fields(error).stream().map(SdkField::memberName)).containsExactly("Reason");
    }

    @Test
    void getterAndSetter_delegateToMemberValueMethods() {
        Schema schema = Schema.structureBuilder(ShapeId.fromParts(NS, "Pair"))
                              .putMember("a", PreludeSchemas.STRING)
                              .putMember("b", PreludeSchemas.STRING)
                              .build();
        TestStruct.Builder builder = new TestStruct.Builder(schema);
        List<SdkField<?>> fields = JSON_MODEL.fields(schema);
        fields.get(1).set(builder, "B");
        fields.get(0).set(builder, "A");
        TestStruct struct = builder.build();
        assertThat(struct.values).containsExactly("A", "B");
        assertThat(fields.get(1).getValueOrDefault(struct)).isEqualTo("B");
    }

    private static SdkField<?> field(List<SdkField<?>> fields, String memberName) {
        return fields.stream().filter(f -> memberName.equals(f.memberName())).findFirst()
                     .orElseThrow(() -> new AssertionError(memberName));
    }

    private static LocationTrait location(List<SdkField<?>> fields, String memberName) {
        return field(fields, memberName).getRequiredTrait(LocationTrait.class);
    }

    /**
     * A minimal generated-style class: values by member index.
     */
    private static final class TestStruct implements SerializableStruct, SdkPojo {
        private final Schema schema;
        private final List<Object> values;

        private TestStruct(Schema schema, List<Object> values) {
            this.schema = schema;
            this.values = values;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberIndex());
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        static final class Builder implements ShapeBuilder<TestStruct>, SdkPojo {
            private final Schema schema;
            private final Object[] values;

            Builder() {
                this(NESTED);
            }

            Builder(Schema schema) {
                this.schema = schema;
                this.values = new Object[schema.members().size()];
            }

            @Override
            public TestStruct build() {
                return new TestStruct(schema, Arrays.asList(values));
            }

            @Override
            public ShapeBuilder<TestStruct> deserialize(ShapeDeserializer decoder) {
                return this;
            }

            @Override
            public void setMemberValue(Schema member, Object value) {
                values[member.memberIndex()] = value;
            }

            @Override
            public Schema sdkSchema() {
                return schema;
            }

            @Override
            public List<SdkField<?>> sdkFields() {
                return Collections.emptyList();
            }
        }
    }
}
