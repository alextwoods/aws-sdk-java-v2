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

package software.amazon.awssdk.services.cloudwatch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;

/**
 * Schema serde members of generated CloudWatch model classes (task 3.7, design A.6).
 */
class SchemaSerdeModelClassTest {
    private static final Rpcv2CborCodec CBOR = Rpcv2CborCodec.builder().build();
    private static final Instant TIME = Instant.ofEpochMilli(1_700_000_000_123L);

    private static MetricAlarm alarm() {
        return MetricAlarm.builder()
                          .alarmName("a")
                          .okActions(Collections.emptyList())
                          .alarmActions("arn:1", "arn:2")
                          .stateValue("FUTURE_STATE")
                          .stateUpdatedTimestamp(TIME)
                          .dimensions(Dimension.builder().name("n").value("v").build())
                          .period(60)
                          .threshold(1.5)
                          .actionsEnabled(true)
                          .build();
    }

    @Test
    void schema_isTheRootSchemaAndCarriesTheClassAndBuilder() {
        assertThat(MetricAlarm.SCHEMA.id().toString()).isEqualTo("com.amazonaws.cloudwatch#MetricAlarm");
        assertThat(alarm().sdkSchema()).isSameAs(MetricAlarm.SCHEMA);
        assertThat(MetricAlarm.SCHEMA.shapeClass()).isSameAs(MetricAlarm.class);
        assertThat(MetricAlarm.SCHEMA.shapeBuilder().build()).isInstanceOf(MetricAlarm.class);
        assertThat(MetricAlarm.builder()).isInstanceOf(ShapeBuilder.class);
        assertThat(StateValue.ALARM.sdkSchema()).isSameAs(StateValue.SCHEMA);
        assertThat(StateValue.ALARM.getValue()).isEqualTo("ALARM");
        assertThat(StateValue.UNKNOWN_TO_SDK_VERSION.getValue()).isNull();
    }

    @Test
    void serializeMembers_writesSetMembersOnceInMemberOrder() {
        List<String> written = new ArrayList<>();
        alarm().serializeMembers(new RecordingSerializer(written));

        // Unset members and the never-set lists are not written; the explicitly empty OKActions list is; the enum is
        // written as its wire string, including a value the enum does not define.
        assertThat(written).containsExactly("AlarmName=a", "ActionsEnabled=true", "OKActions=list(0)", "AlarmActions=list(2)",
                                            "StateValue=FUTURE_STATE", "StateUpdatedTimestamp=" + TIME,
                                            "Dimensions=list(1)", "Period=60", "Threshold=1.5");
        List<Integer> indexes = new ArrayList<>();
        for (String entry : written) {
            indexes.add(MetricAlarm.SCHEMA.member(entry.substring(0, entry.indexOf('='))).memberIndex());
        }
        assertThat(indexes).isSorted();
    }

    @Test
    void getMemberValue_returnsTheGetterValue() {
        MetricAlarm alarm = alarm();
        assertThat((String) alarm.getMemberValue(MetricAlarm.SCHEMA.member("AlarmName"))).isEqualTo(alarm.alarmName());
        assertThat((String) alarm.getMemberValue(MetricAlarm.SCHEMA.member("StateValue"))).isEqualTo("FUTURE_STATE");
        assertThat((List<?>) alarm.getMemberValue(MetricAlarm.SCHEMA.member("Dimensions")))
            .isEqualTo(alarm.dimensions());
        assertThat((Object) alarm.getMemberValue(MetricAlarm.SCHEMA.member("Unit"))).isNull();

        assertThatThrownBy(() -> alarm.getMemberValue(Dimension.SCHEMA.member("Name")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> alarm.getMemberValue(PreludeSchemas.STRING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a member of com.amazonaws.cloudwatch#MetricAlarm");
        assertThat(alarm).isEqualTo(alarm());
    }

    @Test
    void setMemberValue_setsTheMemberThroughTheBuilderSetter() {
        ShapeBuilder<MetricAlarm> builder = MetricAlarm.newShapeBuilder();
        for (Schema member : MetricAlarm.SCHEMA.members()) {
            Object value = alarm().getMemberValue(member);
            if (value != null) {
                builder.setMemberValue(member, value);
            }
        }
        assertThat(builder.build()).isEqualTo(alarm());

        assertThatThrownBy(() -> builder.setMemberValue(Dimension.SCHEMA.member("Value"), "x"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cbor_roundTripsStructuresListsTimestampsAndBlobs() {
        PutMetricDataRequest request =
            PutMetricDataRequest.builder()
                                .namespace("ns")
                                .metricData(MetricDatum.builder()
                                                       .metricName("m")
                                                       .timestamp(TIME)
                                                       .values(1.0, Double.MAX_VALUE, -Double.MIN_VALUE)
                                                       .counts(Collections.emptyList())
                                                       .statisticValues(s -> s.sampleCount(2.0).sum(3.0).minimum(1.0)
                                                                               .maximum(2.0))
                                                       .unit(StandardUnit.BYTES)
                                                       .storageResolution(Integer.MAX_VALUE)
                                                       .build())
                                .strictEntityValidation(false)
                                .build();
        assertThat(roundTrip(request, PutMetricDataRequest.newShapeBuilder())).isEqualTo(request);
        assertThat(roundTrip(alarm(), MetricAlarm.newShapeBuilder())).isEqualTo(alarm());

        GetMetricWidgetImageResponse image = GetMetricWidgetImageResponse.builder()
                                                                         .metricWidgetImage(SdkBytes.fromUtf8String("png"))
                                                                         .build();
        assertThat(roundTrip(image, GetMetricWidgetImageResponse.newShapeBuilder())).isEqualTo(image);

        DashboardNotFoundErrorException error = DashboardNotFoundErrorException.builder().message("missing").build();
        DashboardNotFoundErrorException readError = roundTrip(error, DashboardNotFoundErrorException.newShapeBuilder());
        assertThat(readError.getMessage()).isEqualTo("missing");
    }

    @Test
    void deserialize_keepsPresetMembersAbsentFromTheData() {
        ByteBuffer data = CBOR.serialize(MetricAlarm.builder().stateReason("new").alarmDescription("d").build());
        @SuppressWarnings("unchecked")
        ShapeBuilder<MetricAlarm> builder = (ShapeBuilder<MetricAlarm>) MetricAlarm.builder()
                                                                                    .alarmName("preset")
                                                                                    .stateReason("old");

        assertThat(builder.deserialize(CBOR.createDeserializer(data))).isSameAs(builder);
        MetricAlarm alarm = builder.build();
        assertThat(alarm.alarmName()).isEqualTo("preset");
        assertThat(alarm.stateReason()).isEqualTo("new");
        assertThat(alarm.alarmDescription()).isEqualTo("d");
        assertThat(alarm.hasDimensions()).isFalse();
    }

    @Test
    void deserialize_skipsUnknownMembers() {
        Schema newerDimension = Schema.structureBuilder(Dimension.SCHEMA.id())
                                      .putMember("Name", PreludeSchemas.STRING)
                                      .putMember("AddedLater", PreludeSchemas.STRING)
                                      .putMember("Value", PreludeSchemas.STRING)
                                      .build();
        SerializableStruct newer = new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return newerDimension;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                serializer.writeString(newerDimension.member("Name"), "n");
                serializer.writeString(newerDimension.member("AddedLater"), "x");
                serializer.writeString(newerDimension.member("Value"), "v");
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                throw new UnsupportedOperationException();
            }
        };

        Dimension dimension = CBOR.deserializeShape(CBOR.serialize(newer), Dimension.newShapeBuilder());
        assertThat(dimension).isEqualTo(Dimension.builder().name("n").value("v").build());
    }

    @Test
    void toString_matchesThePreviousGeneratedFormat() {
        assertThat(alarm().toString())
            .isEqualTo("MetricAlarm(AlarmName=a, ActionsEnabled=true, OKActions=[], AlarmActions=[arn:1, arn:2], "
                       + "StateValue=FUTURE_STATE, StateUpdatedTimestamp=2023-11-14T22:13:20.123Z, "
                       + "Dimensions=[Dimension(Name=n, Value=v)], Period=60, Threshold=1.5)");
        assertThat(MetricAlarm.builder().build().toString()).isEqualTo("MetricAlarm()");
        assertThat(GetMetricWidgetImageResponse.builder().metricWidgetImage(SdkBytes.fromUtf8String("hi")).build()
                                               .toString())
            .isEqualTo("GetMetricWidgetImageResponse(MetricWidgetImage=SdkBytes(bytes=0x6869))");
    }

    @Test
    void sdkFields_matchThePreviousGeneratedFields() {
        List<SdkField<?>> fields = MetricDatum.builder().build().sdkFields();
        assertThat(fields.stream().map(SdkField::memberName).collect(Collectors.toList()))
            .containsExactly("MetricName", "Dimensions", "Timestamp", "Value", "StatisticValues", "Values", "Counts",
                             "Unit", "StorageResolution");
        assertThat(MetricDatum.builder().sdkFields()).isSameAs(fields);
        assertThat(MetricDatum.builder().sdkFieldNameToField().keySet())
            .containsExactlyInAnyOrder("MetricName", "Dimensions", "Timestamp", "Value", "StatisticValues", "Values",
                                       "Counts", "Unit", "StorageResolution");
        for (SdkField<?> field : fields) {
            LocationTrait location = field.getRequiredTrait(LocationTrait.class);
            assertThat(location.location()).isEqualTo(MarshallLocation.PAYLOAD);
            assertThat(location.locationName()).isEqualTo(field.memberName());
            assertThat(location.unmarshallLocationName()).isEqualTo(field.memberName());
        }
        assertThat(fields.get(2).marshallingType()).isEqualTo(MarshallingType.INSTANT);
        assertThat(fields.get(4).marshallingType()).isEqualTo(MarshallingType.SDK_POJO);
        assertThat(fields.get(4).constructor().get()).isInstanceOf(StatisticSet.Builder.class);
        assertThat(fields.get(5).getRequiredTrait(ListTrait.class).memberFieldInfo().marshallingType())
            .isEqualTo(MarshallingType.DOUBLE);

        // Getters and setters read and write the members as the previous fields did; enums as their wire string.
        MetricDatum.Builder builder = MetricDatum.builder();
        fields.get(7).set(builder, "Bytes");
        fields.get(0).set(builder, "m");
        MetricDatum datum = builder.build();
        assertThat(datum.unit()).isEqualTo(StandardUnit.BYTES);
        assertThat(fields.get(7).getValueOrDefault(datum)).isEqualTo("Bytes");
        assertThat(fields.get(0).getValueOrDefault(datum)).isEqualTo("m");
        assertThat(datum.equalsBySdkFields(MetricDatum.builder().metricName("m").unit("Bytes").build())).isTrue();
    }

    @Test
    void buildResponse_setsResponseMetadataAndHttpResponseOnTheBuilder() {
        SdkHttpResponse http = SdkHttpResponse.builder().statusCode(200).build();
        ShapeBuilder<DescribeAlarmsResponse> builder = DescribeAlarmsResponse.newShapeBuilder();
        ((CloudWatchResponse.Builder) builder).responseMetadata(DefaultAwsResponseMetadata.create(
            Collections.singletonMap("AWS_REQUEST_ID", "req-1")));

        DescribeAlarmsResponse response = SchemaSerdeSupport.buildResponse(builder, http);

        assertThat(response.sdkHttpResponse()).isSameAs(http);
        assertThat(response.responseMetadata().requestId()).isEqualTo("req-1");
    }

    private static <T extends SerializableStruct> T roundTrip(SerializableStruct value, ShapeBuilder<T> builder) {
        return CBOR.deserializeShape(CBOR.serialize(value), builder);
    }

    /**
     * Records the member name and value of each top-level member written.
     */
    private static final class RecordingSerializer extends SpecificShapeSerializer {
        private final List<String> written;

        private RecordingSerializer(List<String> written) {
            this.written = written;
        }

        private void add(Schema schema, Object value) {
            written.add(schema.memberName() + "=" + value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            add(schema, value);
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            add(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            add(schema, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            add(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            add(schema, value);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            add(schema, "list(" + size + ")");
        }
    }
}
