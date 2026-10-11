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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShapeIdTest {

    @Test
    void from_withNonMemberId_parsesParts() {
        ShapeId id = ShapeId.from("com.amazonaws.sqs#GetQueueUrlRequest");

        assertThat(id.getNamespace()).isEqualTo("com.amazonaws.sqs");
        assertThat(id.getName()).isEqualTo("GetQueueUrlRequest");
        assertThat(id.getMember()).isEmpty();
        assertThat(id.hasMember()).isFalse();
    }

    @Test
    void from_withMemberId_parsesParts() {
        ShapeId id = ShapeId.from("com.amazonaws.sqs#GetQueueUrlRequest$QueueName");

        assertThat(id.getNamespace()).isEqualTo("com.amazonaws.sqs");
        assertThat(id.getName()).isEqualTo("GetQueueUrlRequest");
        assertThat(id.getMember()).hasValue("QueueName");
        assertThat(id.hasMember()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "smithy.api#String",
        "a#B",
        "com.amazonaws.sqs#GetQueueUrlRequest$QueueName",
        "ns_1.sub_2#_Shape$_1member",
        "__ns#__9",
        "a.b.c.d#Name_With_Underscores$m"
    })
    void toString_withValidId_returnsOriginalString(String input) {
        assertThat(ShapeId.from(input).toString()).isEqualTo(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "Name",
        "#Name",
        "ns#",
        "ns#$member",
        "ns#Name$",
        "ns#Name$a$b",
        "ns#Na#me",
        "ns#Na-me",
        "ns#1Name",
        "ns#_",
        "ns#__",
        "ns#Name$1member",
        "ns..sub#Name",
        ".ns#Name",
        "ns.#Name",
        "1ns#Name",
        "n s#Name",
        "ns#Näme",
        "ns#Name "
    })
    void from_withInvalidId_throwsSyntaxExceptionNamingInput(String input) {
        assertThatThrownBy(() -> ShapeId.from(input))
            .isInstanceOf(ShapeIdSyntaxException.class)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'" + input + "'");
    }

    @Test
    void from_withNull_throwsNullPointerException() {
        assertThatThrownBy(() -> ShapeId.from(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("absoluteShapeId");
    }

    @Test
    void fromParts_withNamespaceAndName_equalsParsedId() {
        assertThat(ShapeId.fromParts("com.amazonaws.sqs", "QueueName"))
            .isEqualTo(ShapeId.from("com.amazonaws.sqs#QueueName"));
    }

    @Test
    void fromParts_withMember_equalsParsedId() {
        assertThat(ShapeId.fromParts("com.amazonaws.sqs", "Shape", "member"))
            .isEqualTo(ShapeId.from("com.amazonaws.sqs#Shape$member"));
    }

    @Test
    void fromParts_withInvalidPart_throwsSyntaxExceptionNamingInput() {
        assertThatThrownBy(() -> ShapeId.fromParts("ns", "Bad$Name"))
            .isInstanceOf(ShapeIdSyntaxException.class)
            .hasMessageContaining("'ns#Bad$Name'");
        assertThatThrownBy(() -> ShapeId.fromParts("", "Name"))
            .isInstanceOf(ShapeIdSyntaxException.class)
            .hasMessageContaining("namespace is empty");
        assertThatThrownBy(() -> ShapeId.fromParts("ns", "Name", ""))
            .isInstanceOf(ShapeIdSyntaxException.class)
            .hasMessageContaining("member name is empty");
    }

    @Test
    void fromParts_withNullPart_throwsNullPointerException() {
        assertThatThrownBy(() -> ShapeId.fromParts(null, "Name")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ShapeId.fromParts("ns", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ShapeId.fromParts("ns", "Name", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromOptionalNamespace_withRelativeId_usesDefaultNamespace() {
        assertThat(ShapeId.fromOptionalNamespace("com.example", "Shape"))
            .isEqualTo(ShapeId.from("com.example#Shape"));
        assertThat(ShapeId.fromOptionalNamespace("com.example", "Shape$member"))
            .isEqualTo(ShapeId.from("com.example#Shape$member"));
    }

    @Test
    void fromOptionalNamespace_withAbsoluteId_ignoresDefaultNamespace() {
        assertThat(ShapeId.fromOptionalNamespace("com.example", "other.ns#Shape"))
            .isEqualTo(ShapeId.from("other.ns#Shape"));
    }

    @Test
    void fromOptionalNamespace_withRelativeIdAndNoDefault_throwsSyntaxException() {
        assertThatThrownBy(() -> ShapeId.fromOptionalNamespace(null, "Shape"))
            .isInstanceOf(ShapeIdSyntaxException.class)
            .hasMessageContaining("'Shape'");
    }

    @Test
    void withMember_returnsMemberIdOfSameShape() {
        ShapeId shape = ShapeId.from("ns#Shape");
        ShapeId member = shape.withMember("foo");

        assertThat(member).isEqualTo(ShapeId.from("ns#Shape$foo"));
        assertThat(shape.withMember("bar").withMember("foo")).isEqualTo(member);
    }

    @Test
    void withMember_withInvalidMember_throwsSyntaxException() {
        assertThatThrownBy(() -> ShapeId.from("ns#Shape").withMember("1bad"))
            .isInstanceOf(ShapeIdSyntaxException.class)
            .hasMessageContaining("'ns#Shape$1bad'");
    }

    @Test
    void withoutMember_removesMember() {
        ShapeId shape = ShapeId.from("ns#Shape");

        assertThat(ShapeId.from("ns#Shape$foo").withoutMember()).isEqualTo(shape);
        assertThat(shape.withoutMember()).isSameAs(shape);
    }

    @Test
    void equalsAndHashCode_withEqualStrings_areEqual() {
        ShapeId a = ShapeId.from("ns#Shape$member");
        ShapeId b = ShapeId.fromParts("ns", "Shape", "member");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a.hashCode()).isEqualTo("ns#Shape$member".hashCode());
    }

    @Test
    void equals_withDifferentIds_isFalse() {
        ShapeId id = ShapeId.from("ns#Shape");

        assertThat(id).isNotEqualTo(ShapeId.from("ns#shape"));
        assertThat(id).isNotEqualTo(ShapeId.from("ns#Shape$m"));
        assertThat(id).isNotEqualTo(ShapeId.from("ns2#Shape"));
        assertThat(id).isNotEqualTo("ns#Shape");
        assertThat(id).isNotEqualTo(null);
    }

    @Test
    void compareTo_sortsCaseInsensitivelyThenCaseSensitively() {
        List<ShapeId> ids = new ArrayList<>(Arrays.asList(
            ShapeId.from("b#A"),
            ShapeId.from("a#b"),
            ShapeId.from("a#B"),
            ShapeId.from("a#A$z"),
            ShapeId.from("a#A")));

        Collections.sort(ids);

        assertThat(ids).extracting(ShapeId::toString)
                       .containsExactly("a#A", "a#A$z", "a#B", "a#b", "b#A");
    }

    @Test
    void compareTo_isConsistentWithEquals() {
        ShapeId a = ShapeId.from("ns#Shape");

        assertThat(a.compareTo(ShapeId.fromParts("ns", "Shape"))).isZero();
        assertThat(a.compareTo(ShapeId.from("ns#shape"))).isNotZero();
        assertThat(Integer.signum(a.compareTo(ShapeId.from("ns#shape"))))
            .isEqualTo(-Integer.signum(ShapeId.from("ns#shape").compareTo(a)));
    }

    @Test
    void serialization_roundTripsToEqualId() throws Exception {
        ShapeId id = ShapeId.from("com.example#Shape$member");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(id);
        }
        Object read;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            read = in.readObject();
        }

        assertThat(read).isEqualTo(id).hasSameHashCodeAs(id);
        assertThat(read.toString()).isEqualTo("com.example#Shape$member");
    }
}
