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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

class SchemaSerdeSupportTest {
    private static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts("test", "Output")).build();

    @Test
    void toSdkBytes_null_returnsNull() {
        assertThat(SchemaSerdeSupport.toSdkBytes(null)).isNull();
    }

    @Test
    void toSdkBytes_wholeArray_wrapsWithoutCopy() {
        byte[] array = {1, 2, 3};
        SdkBytes bytes = SchemaSerdeSupport.toSdkBytes(ByteBuffer.wrap(array));
        assertThat(bytes.asByteArrayUnsafe()).isSameAs(array);
    }

    @Test
    void toSdkBytes_view_copiesRemainingBytesAndLeavesBufferUnchanged() {
        byte[] array = {1, 2, 3, 4};
        ByteBuffer view = ByteBuffer.wrap(array, 1, 2).slice();
        SdkBytes bytes = SchemaSerdeSupport.toSdkBytes(view);
        assertThat(bytes.asByteArray()).containsExactly(2, 3);
        assertThat(bytes.asByteArrayUnsafe()).isNotSameAs(array);
        assertThat(view.remaining()).isEqualTo(2);
        array[1] = 9;
        assertThat(bytes.asByteArray()).containsExactly(2, 3);
    }

    @Test
    void toSdkBytes_readOnlyAndDirectBuffers_areCopied() {
        assertThat(SchemaSerdeSupport.toSdkBytes(ByteBuffer.wrap(new byte[] {5}).asReadOnlyBuffer()).asByteArray())
            .containsExactly(5);
        ByteBuffer direct = ByteBuffer.allocateDirect(2);
        direct.put((byte) 6).put((byte) 7).flip();
        assertThat(SchemaSerdeSupport.toSdkBytes(direct).asByteArray()).containsExactly(6, 7);
    }

    @Test
    void newListAndNewMap_capPreAllocationAndReturnEmptyMutableContainers() {
        ShapeDeserializer huge = Mockito.mock(ShapeDeserializer.class);
        Mockito.when(huge.containerSize()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(huge.containerPreAllocationLimit()).thenReturn(4);
        ShapeDeserializer unknown = Mockito.mock(ShapeDeserializer.class);
        Mockito.when(unknown.containerSize()).thenReturn(-1);

        List<String> list = SchemaSerdeSupport.newList(huge);
        list.add("a");
        assertThat(list).containsExactly("a");
        assertThat(SchemaSerdeSupport.<String>newList(unknown)).isEmpty();

        Map<String, Integer> map = SchemaSerdeSupport.newMap(huge);
        map.put("b", 2);
        map.put("a", 1);
        assertThat(map.keySet()).containsExactly("b", "a");
        assertThat(SchemaSerdeSupport.<Integer>newMap(unknown)).isEmpty();
    }

    @Test
    void buildResponse_setsTheHttpResponseOnTheBuilderBeforeBuilding() {
        SdkHttpResponse http = SdkHttpResponse.builder().statusCode(200).build();
        TestResponse response = SchemaSerdeSupport.buildResponse(new TestResponse.BuilderImpl(), http);
        assertThat(response.sdkHttpResponse()).isSameAs(http);
    }

    @Test
    void buildResponse_rejectsBuildersThatAreNotResponseBuilders() {
        ShapeBuilder<TestResponse> notAResponseBuilder = new ShapeBuilder<TestResponse>() {
            @Override
            public TestResponse build() {
                return null;
            }

            @Override
            public ShapeBuilder<TestResponse> deserialize(ShapeDeserializer decoder) {
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        };
        assertThatThrownBy(() -> SchemaSerdeSupport.buildResponse(notAResponseBuilder, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("test#Output");
    }

    private static final class TestResponse extends SdkResponse implements SerializableStruct {
        private TestResponse(BuilderImpl builder) {
            super(builder);
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        private static final class BuilderImpl extends SdkResponse.BuilderImpl implements ShapeBuilder<TestResponse> {
            @Override
            public TestResponse build() {
                return new TestResponse(this);
            }

            @Override
            public ShapeBuilder<TestResponse> deserialize(ShapeDeserializer decoder) {
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }
}
