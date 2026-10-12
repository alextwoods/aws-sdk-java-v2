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

package software.amazon.awssdk.schema.serde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;

/**
 * Ported from smithy-java {@code ListSerializerTest}, plus checks of the constructor.
 */
class ListSerializerTest {

    @Test
    void incrementsPosition() {
        List<Integer> positions = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        SpecificShapeSerializer delegate = new SpecificShapeSerializer() {
            @Override
            public void writeString(Schema schema, String value) {
                strings.add(value);
            }
        };

        ListSerializer listSerializer = new ListSerializer(delegate, positions::add);
        listSerializer.writeString(PreludeSchemas.STRING, "1");
        listSerializer.writeString(PreludeSchemas.STRING, "2");

        assertThat(positions).containsExactly(0, 1);
        assertThat(strings).containsExactly("1", "2");
        assertThat(listSerializer.position()).isEqualTo(2);
    }

    @Test
    void callbackRunsBeforeTheDelegateWrites() {
        List<String> events = new ArrayList<>();
        SpecificShapeSerializer delegate = new SpecificShapeSerializer() {
            @Override
            public void writeInteger(Schema schema, int value) {
                events.add("write " + value);
            }

            @Override
            public void writeNull(Schema schema) {
                events.add("null");
            }
        };

        ListSerializer listSerializer = new ListSerializer(delegate, p -> events.add("before " + p));
        listSerializer.writeInteger(PreludeSchemas.INTEGER, 7);
        listSerializer.writeNull(PreludeSchemas.INTEGER);

        assertThat(events).containsExactly("before 0", "write 7", "before 1", "null");
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> new ListSerializer(null, p -> { }))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("delegate is null");
        assertThatThrownBy(() -> new ListSerializer(ShapeSerializer.nullSerializer(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("beforeEachValue is null");
    }
}
