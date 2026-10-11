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
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Port of smithy-model 1.74.0 {@code software.amazon.smithy.model.shapes.ShapeIdTest}, limited to the runtime subset
 * of the API that the SDK {@link ShapeId} keeps (task 1.18). Methods keep the source names; adapted methods say how.
 * Skipped methods are listed in {@code docs/design/schema-serde/porting-deviations.md}.
 */
class PortedShapeIdTest {

    @Test
    void computesHash() {
        assertThat(ShapeId.from("name.space#Name").hashCode()).isNotZero();
    }

    @Test
    void returnsWithNewMember() {
        ShapeId a = ShapeId.from("ns.foo#Bar$baz");
        ShapeId b = a.withMember("bam");

        assertThat(b.toString()).isEqualTo("ns.foo#Bar$bam");
    }

    @Test
    void validatesMemberNameSyntax() {
        assertThatThrownBy(() -> ShapeId.from("ns.foo#Bar").withMember("1invalid"))
            .isInstanceOf(ShapeIdSyntaxException.class);
    }

    @Test
    void fromAbsolute() {
        ShapeId id = ShapeId.from("foo.bar#Name$member");

        assertThat(id.getNamespace()).isEqualTo("foo.bar");
        assertThat(id.getName()).isEqualTo("Name");
        assertThat(id.getMember()).contains("member");
    }

    @Test
    void fromAbsoluteWithoutName() {
        assertThatThrownBy(() -> ShapeId.from("name.space#")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromAbsoluteWithoutNamespace() {
        assertThatThrownBy(() -> ShapeId.from("Name$member")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromAbsoluteWithDoubleNamespace() {
        assertThatThrownBy(() -> ShapeId.from("ns.foo#bar#Name")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Adapted: {@code ShapeId.fromRelative} is not ported. {@code fromOptionalNamespace} resolves a relative ID the
     * same way.
     */
    @Test
    void fromRelative() {
        ShapeId id = ShapeId.fromOptionalNamespace("name.space", "Name$member");

        assertThat(id.getNamespace()).isEqualTo("name.space");
        assertThat(id.getName()).isEqualTo("Name");
        assertThat(id.getMember()).contains("member");
    }

    @Test
    void fromOptionalNamespaceTestWithoutNamespace() {
        ShapeId id = ShapeId.fromOptionalNamespace("default.ns", "RelativeName");

        assertThat(id.getNamespace()).isEqualTo("default.ns");
        assertThat(id.getName()).isEqualTo("RelativeName");
        assertThat(id.getMember()).isNotPresent();
    }

    @Test
    void fromOptionalNamespaceTestWithNamespace() {
        ShapeId id = ShapeId.fromOptionalNamespace("default.ns", "other.ns#Name");

        assertThat(id.getNamespace()).isEqualTo("other.ns");
        assertThat(id.getName()).isEqualTo("Name");
        assertThat(id.getMember()).isNotPresent();
    }

    /**
     * Adapted: {@code ShapeId.isValidNamespace} is private in the SDK, so namespace validity is observed through
     * {@link ShapeId#fromParts(String, String)}, which validates the namespace on its own.
     */
    @Test
    void checksIfValidNamespaceName() {
        assertThat(isValidNamespace("foo.bar.")).isFalse();
        assertThat(isValidNamespace(".foo.bar")).isFalse();
        assertThat(isValidNamespace("foo.bar#baz")).isFalse();
        assertThat(isValidNamespace("1foo.bar")).isFalse();
        assertThat(isValidNamespace("foo.1bar")).isFalse();
        assertThat(isValidNamespace("foo.bar.1")).isFalse();
        assertThat(isValidNamespace("foo.bar.*")).isFalse();
        assertThat(isValidNamespace("foo.bar..")).isFalse();
        assertThat(isValidNamespace("")).isFalse();

        assertThat(isValidNamespace("foo")).isTrue();
        assertThat(isValidNamespace("Foo.bar")).isTrue();
        assertThat(isValidNamespace("foo._bar")).isTrue();
        assertThat(isValidNamespace("foo.bar")).isTrue();
        assertThat(isValidNamespace("foo.bar1")).isTrue();
        assertThat(isValidNamespace("_foo.bar")).isTrue();
        assertThat(isValidNamespace("f.b")).isTrue();
        assertThat(isValidNamespace("f.b1.c_d")).isTrue();
        assertThat(isValidNamespace("f.b1.c_d_.e")).isTrue();
        assertThat(isValidNamespace("f.b1.c_d_1234.e")).isTrue();
    }

    /**
     * Adapted the same way as {@link #checksIfValidNamespaceName()}.
     */
    @Test
    void checksIfValidNamespaceNameIdentifierOnly() {
        // Empty string
        assertThat(isValidNamespace("")).isFalse();

        // Underscore only
        assertThat(isValidNamespace("_")).isFalse();
        assertThat(isValidNamespace("__")).isFalse();

        // Starts with a digit
        assertThat(isValidNamespace("1")).isFalse();
        assertThat(isValidNamespace("1_")).isFalse();
        assertThat(isValidNamespace("1a")).isFalse();
        assertThat(isValidNamespace("12")).isFalse();

        // IdentifierStart: 1*"_" (ALPHA / DIGIT)
        assertThat(isValidNamespace("_a")).isTrue();
        assertThat(isValidNamespace("_1")).isTrue();
        assertThat(isValidNamespace("__a")).isTrue();
        assertThat(isValidNamespace("__1")).isTrue();
        assertThat(isValidNamespace("__#")).isFalse();

        // IdentifierStart: ALPHA
        assertThat(isValidNamespace("a")).isTrue();
        assertThat(isValidNamespace("#")).isFalse();

        // Identifier: 1*"_" (ALPHA / DIGIT) *IdentifierChars
        assertThat(isValidNamespace("_ab")).isTrue();
        assertThat(isValidNamespace("_a1")).isTrue();
        assertThat(isValidNamespace("_a_")).isTrue();
        assertThat(isValidNamespace("_a#")).isFalse();
        assertThat(isValidNamespace("_1a")).isTrue();
        assertThat(isValidNamespace("_12")).isTrue();
        assertThat(isValidNamespace("_1_")).isTrue();
        assertThat(isValidNamespace("_1#")).isFalse();
        assertThat(isValidNamespace("__ab")).isTrue();
        assertThat(isValidNamespace("__a1")).isTrue();
        assertThat(isValidNamespace("__a_")).isTrue();
        assertThat(isValidNamespace("__a#")).isFalse();
        assertThat(isValidNamespace("__1a")).isTrue();
        assertThat(isValidNamespace("__12")).isTrue();
        assertThat(isValidNamespace("__1_")).isTrue();
        assertThat(isValidNamespace("__1#")).isFalse();

        // Identifier: ALPHA *IdentifierChars
        assertThat(isValidNamespace("ab")).isTrue();
        assertThat(isValidNamespace("a1")).isTrue();
        assertThat(isValidNamespace("a_")).isTrue();
        assertThat(isValidNamespace("a#")).isFalse();
    }

    /**
     * Source name {@code ShapeIdValidationsTest}, renamed to a lower-case method name.
     */
    @ParameterizedTest
    @MethodSource("shapeIdData")
    void shapeIdValidations(String shapeId, boolean isInvalid) {
        try {
            ShapeId id = ShapeId.from(shapeId);
            if (isInvalid) {
                fail("Expected ShapeIdSyntaxException but nothing was raised: " + id);
            }
        } catch (ShapeIdSyntaxException ex) {
            if (!isInvalid) {
                fail("Received unexpected validation error: " + ex.getMessage());
            }
        }
    }

    static Collection<Object[]> shapeIdData() {
        return Arrays.asList(new Object[][] {
            // valid namespaces
            {"n.s#name", false},
            {"name.space#name", false},
            {"name1.space2#name", false},
            {"na.me.spa.ce#name", false},
            {"na.me.spa.ce_#name", false},
            {"na.me.spa.ce__#name", false},
            {"na.me.spa.ce__2#name", false},
            {"na.me.spa.ce__2_#name", false},
            {"namespace#name", false},
            {"mixed.Case#name", false},
            {"NAMESPACE#name", false},
            {"nameSpace#name", false},
            {"Mixed.case#name", false},
            {"_foo.baz#name", false},
            {"__foo.baz#name", false},

            // invalid namespaces
            {"#name", true},
            {"name.space.#name", true},
            {"name..space#name", true},
            {".name.space#name", true},
            {"name-space#name", true},
            {"1namespace.foo#name", true},
            {"a._.b#name", true},
            {"a.____.b#name", true},

            // valid shape names
            {"ns.foo#shapename", false},
            {"ns.foo#shapeName", false},
            {"ns.foo#ShapeName", false},
            {"ns.foo#SHAPENAME", false},
            {"ns.foo#name1", false},
            {"ns.foo#Shape_Name", false},
            {"ns.foo#shape_name_num1", false},
            {"ns.foo#shape_1name", false},
            {"ns.foo#_Shape_Name", false},
            {"ns.foo#__shape_name_num1", false},

            // invalid shape names
            {"ns.foo#", true},
            {"ns.foo#1name", true},
            {"ns.foo#1", true},
            {"ns.foo#shape.name", true},

            // valid segments
            {"ns.foo#name$abc", false},
            {"ns.foo#name$Abc", false},
            {"ns.foo#name$ABC", false},
            {"ns.foo#name$abcMno", false},
            {"ns.foo#name$AbcMno", false},
            {"ns.foo#name$AbcMno1", false},
            {"ns.foo#name$abc_mno", false},
            {"ns.foo#name$Abc_Mno", false},
            {"ns.foo#name$abc_", false},
            {"ns.foo#name$abc__mno", false},
            {"ns.foo#name$_abc", false},
            {"ns.foo#name$_abc_", false},

            // invalid segments
            {"ns.foo#name$", true},
            {"ns.foo#name$abc-mno", true},
            {"ns.foo#name$1abc", true},
            {"ns.foo#name$abc.mno", true},
            {"ns.foo#name$abc.", true},
        });
    }

    /**
     * Adapted: the SDK {@code fromParts(namespace, name, member)} rejects a null member, so the non-member row uses
     * {@code fromParts(namespace, name)}.
     */
    @ParameterizedTest
    @MethodSource("toStringData")
    void toStringTest(String namespace, String name, String member, String expectedResult) {
        ShapeId shapeId = member == null ? ShapeId.fromParts(namespace, name)
                                         : ShapeId.fromParts(namespace, name, member);

        assertThat(shapeId.toString()).isEqualTo(expectedResult);
    }

    static Collection<Object[]> toStringData() {
        return Arrays.asList(new Object[][] {
            {"name.space", "Name", null, "name.space#Name"},
            {"name.space", "Name", "member", "name.space#Name$member"},
        });
    }

    @Test
    void compareToTest() {
        List<ShapeId> given = Arrays.asList(
            ShapeId.fromParts("ns.foo", "foo"),
            ShapeId.fromParts("ns.foo", "Foo"),
            ShapeId.fromParts("ns.foo", "bar"),
            ShapeId.fromParts("ns.foo", "bar", "member"),
            ShapeId.fromParts("ns.foo", "bar", "Member"),
            ShapeId.fromParts("ns.foo", "bar", "AMember"),
            ShapeId.fromParts("ns.Foo", "foo"),
            ShapeId.fromParts("ns.baz", "foo"));
        given.sort(ShapeId::compareTo);

        List<ShapeId> expected = Arrays.asList(
            ShapeId.fromParts("ns.baz", "foo"),
            ShapeId.fromParts("ns.foo", "bar"),
            ShapeId.fromParts("ns.foo", "bar", "AMember"),
            ShapeId.fromParts("ns.foo", "bar", "Member"),
            ShapeId.fromParts("ns.foo", "bar", "member"),
            ShapeId.fromParts("ns.Foo", "foo"),
            ShapeId.fromParts("ns.foo", "Foo"),
            ShapeId.fromParts("ns.foo", "foo"));

        assertThat(given).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("equalsData")
    void equalsTest(ShapeId lhs, Object rhs, boolean expected) {
        assertThat(lhs.equals(rhs)).isEqualTo(expected);
    }

    static Collection<Object[]> equalsData() {
        ShapeId obj = ShapeId.fromParts("ns.foo", "name");
        return Arrays.asList(new Object[][] {
            {obj, obj, true},
            {ShapeId.fromParts("ns.foo", "name"), "other-object", false},
            {ShapeId.fromParts("ns.foo", "name"), ShapeId.fromParts("ns.foo", "name"), true},
            {ShapeId.fromParts("ns.foo", "name1"), ShapeId.fromParts("ns.foo", "name2"), false},
            {ShapeId.fromParts("ns.foo1", "name1"), ShapeId.fromParts("ns.foo2", "name2"), false},
            {ShapeId.fromParts("ns.foo", "n", "a"), ShapeId.fromParts("ns.foo", "n", "a"), true},
            {ShapeId.fromParts("ns.foo", "n", "a"), ShapeId.fromParts("ns.foo", "n", "c"), false},
        });
    }

    private static boolean isValidNamespace(String namespace) {
        try {
            ShapeId.fromParts(namespace, "Name");
            return true;
        } catch (ShapeIdSyntaxException e) {
            return false;
        }
    }
}
