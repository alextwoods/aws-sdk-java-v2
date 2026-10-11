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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ShapeTypeTest {

    @Test
    void values_matchSmithyIdl2TypesWithoutSet() {
        assertThat(Arrays.stream(ShapeType.values()).map(ShapeType::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "blob", "boolean", "string", "enum", "timestamp", "byte", "short", "integer", "intEnum", "long",
                "float", "double", "bigInteger", "bigDecimal", "document",
                "list", "map", "structure", "union",
                "member", "operation", "resource", "service");
    }

    @Test
    void getCategory_returnsSmithyCategory() {
        EnumSet<ShapeType> aggregates = EnumSet.of(ShapeType.LIST, ShapeType.MAP, ShapeType.STRUCTURE, ShapeType.UNION);
        EnumSet<ShapeType> services = EnumSet.of(ShapeType.SERVICE, ShapeType.RESOURCE, ShapeType.OPERATION);

        for (ShapeType type : ShapeType.values()) {
            ShapeType.Category expected = aggregates.contains(type) ? ShapeType.Category.AGGREGATE
                                          : services.contains(type) ? ShapeType.Category.SERVICE
                                          : type == ShapeType.MEMBER ? ShapeType.Category.MEMBER
                                          : ShapeType.Category.SIMPLE;
            assertThat(type.getCategory()).as(type.name()).isEqualTo(expected);
        }
    }

    @ParameterizedTest
    @EnumSource(ShapeType.class)
    void isShapeType_withSameType_isTrue(ShapeType type) {
        assertThat(type.isShapeType(type)).isTrue();
    }

    @Test
    void isShapeType_withEnumSpecializations_matchesBaseType() {
        assertThat(ShapeType.ENUM.isShapeType(ShapeType.STRING)).isTrue();
        assertThat(ShapeType.INT_ENUM.isShapeType(ShapeType.INTEGER)).isTrue();

        assertThat(ShapeType.STRING.isShapeType(ShapeType.ENUM)).isFalse();
        assertThat(ShapeType.INTEGER.isShapeType(ShapeType.INT_ENUM)).isFalse();
        assertThat(ShapeType.ENUM.isShapeType(ShapeType.INTEGER)).isFalse();
        assertThat(ShapeType.LIST.isShapeType(ShapeType.MAP)).isFalse();
    }
}
