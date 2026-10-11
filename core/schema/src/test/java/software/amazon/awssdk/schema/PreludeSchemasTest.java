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
import software.amazon.awssdk.schema.serde.document.Document;

class PreludeSchemasTest {
    @Test
    void preludeSchemas_havePreludeIdsAndTypes() {
        assertThat(PreludeSchemas.STRING.id()).isEqualTo(ShapeId.from("smithy.api#String"));
        assertThat(PreludeSchemas.BIG_DECIMAL.id()).isEqualTo(ShapeId.from("smithy.api#BigDecimal"));
        assertThat(PreludeSchemas.DOCUMENT.type()).isEqualTo(ShapeType.DOCUMENT);
        assertThat(PreludeSchemas.TIMESTAMP.type()).isEqualTo(ShapeType.TIMESTAMP);
    }

    @Test
    void primitiveSchemas_haveZeroDefaults() {
        assertThat(PreludeSchemas.PRIMITIVE_BOOLEAN.expectTrait(TraitKey.DEFAULT_TRAIT).getValue())
            .isEqualTo(Document.of(false));
        assertThat(PreludeSchemas.PRIMITIVE_INTEGER.expectTrait(TraitKey.DEFAULT_TRAIT).getValue())
            .isEqualTo(Document.of(0));
        assertThat(PreludeSchemas.PRIMITIVE_DOUBLE.expectTrait(TraitKey.DEFAULT_TRAIT).getValue())
            .isEqualTo(Document.of(0.0));
        assertThat(PreludeSchemas.INTEGER.hasTrait(TraitKey.DEFAULT_TRAIT)).isFalse();
    }

    @Test
    void unit_isEmptyStructureWithUnitTypeTrait() {
        assertThat(PreludeSchemas.UNIT.id()).isEqualTo(ShapeId.from("smithy.api#Unit"));
        assertThat(PreludeSchemas.UNIT.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(PreludeSchemas.UNIT.members()).isEmpty();
        assertThat(PreludeSchemas.UNIT.hasTrait(TraitKey.UNIT_TYPE_TRAIT)).isTrue();
    }

    @Test
    void getSchemaForType_mapsTypes() {
        assertThat(PreludeSchemas.getSchemaForType(ShapeType.INTEGER)).isSameAs(PreludeSchemas.INTEGER);
        assertThat(PreludeSchemas.getSchemaForType(ShapeType.INT_ENUM)).isSameAs(PreludeSchemas.INTEGER);
        assertThat(PreludeSchemas.getSchemaForType(ShapeType.ENUM)).isSameAs(PreludeSchemas.STRING);
        assertThat(PreludeSchemas.getSchemaForType(ShapeType.BLOB)).isSameAs(PreludeSchemas.BLOB);
        assertThat(PreludeSchemas.getSchemaForType(ShapeType.STRUCTURE)).isSameAs(PreludeSchemas.DOCUMENT);
        assertThat(PreludeSchemas.getSchemaForType(ShapeType.LIST)).isSameAs(PreludeSchemas.DOCUMENT);
    }
}
