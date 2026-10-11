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

import org.junit.jupiter.api.Test;

/**
 * Port of smithy-model 1.74.0 {@code software.amazon.smithy.model.shapes.ShapeTypeTest} (task 1.18). The source's
 * {@code createsBuilders} is skipped: it builds smithy-model {@code Shape}s, which the runtime has no equivalent of.
 */
class PortedShapeTypeTest {

    @Test
    void hasCategory() {
        assertThat(ShapeType.STRING.getCategory()).isEqualTo(ShapeType.Category.SIMPLE);
        assertThat(ShapeType.LIST.getCategory()).isEqualTo(ShapeType.Category.AGGREGATE);
        assertThat(ShapeType.SERVICE.getCategory()).isEqualTo(ShapeType.Category.SERVICE);
    }

    @Test
    void enumEquivalence() {
        assertThat(ShapeType.ENUM.isShapeType(ShapeType.STRING)).isTrue();
        assertThat(ShapeType.STRING.isShapeType(ShapeType.ENUM)).isFalse();
        assertThat(ShapeType.INT_ENUM.isShapeType(ShapeType.INTEGER)).isTrue();
        assertThat(ShapeType.INTEGER.isShapeType(ShapeType.INT_ENUM)).isFalse();
    }
}
