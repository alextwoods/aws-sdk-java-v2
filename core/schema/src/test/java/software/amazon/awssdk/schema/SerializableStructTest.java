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

package software.amazon.awssdk.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.RecordingShapeSerializer;

/**
 * Tests for the {@link SerializableStruct} default method (Requirements 6.1, 6.2).
 */
class SerializableStructTest {

    @Test
    void serialize_callsWriteStructOnceWithSchemaAndThis() {
        TestStruct struct = TestStruct.builder().name("a").build();
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        struct.serialize(serializer);

        List<RecordingShapeSerializer.Call> calls = serializer.calls();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).method()).isEqualTo("writeStruct");
        assertThat(calls.get(0).schema()).isSameAs(TestStruct.SCHEMA);
        assertThat(calls.get(0).value()).isSameAs(struct);
    }

    @Test
    void serialize_doesNotWriteMembersItself() {
        // The serializer, not serialize(), decides whether to call serializeMembers.
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        TestStruct.builder().name("a").build().serialize(serializer);

        assertThat(serializer.calls()).extracting(RecordingShapeSerializer.Call::method).containsExactly("writeStruct");
    }

    @Test
    void schemaShapeBuilder_returnsNewShapeBuilderEachCall() {
        ShapeBuilder<? extends SerializableShape> first = TestStruct.SCHEMA.shapeBuilder();
        ShapeBuilder<? extends SerializableShape> second = TestStruct.SCHEMA.shapeBuilder();

        assertThat(first).isInstanceOf(TestStruct.Builder.class).isNotSameAs(second);
        assertThat(first.sdkSchema()).isSameAs(TestStruct.SCHEMA);
    }
}
