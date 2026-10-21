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

package software.amazon.awssdk.dynamicclient.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;

/**
 * Schemas use the ShapeId a model transform recorded with {@code OriginalShapeIdTrait} (Requirement 25.6).
 */
class SchemaConverterOriginalShapeIdTest {
    private static final ShapeId RENAMED = ShapeId.from("test.orig#GetThingInput");
    private static final ShapeId ORIGINAL = ShapeId.from("test.orig#GetThingRequest");
    private static final ShapeId SYNTHETIC_UNIT = ShapeId.from("test.orig#PingInput");
    private static final ShapeId PLAIN = ShapeId.from("test.orig#Plain");

    private static final Model MODEL = Model.assembler()
                                            .addShape(StructureShape.builder()
                                                                    .id(RENAMED)
                                                                    .addMember("name", ShapeId.from("smithy.api#String"))
                                                                    .addTrait(new OriginalShapeIdTrait(ORIGINAL))
                                                                    .build())
                                            .addShape(StructureShape.builder()
                                                                    .id(SYNTHETIC_UNIT)
                                                                    .addTrait(new OriginalShapeIdTrait(
                                                                        ShapeId.from("smithy.api#Unit")))
                                                                    .build())
                                            .addShape(StructureShape.builder().id(PLAIN).build())
                                            .assemble()
                                            .unwrap();

    private final SchemaConverter converter = new SchemaConverter(MODEL);

    @Test
    void getSchema_renamedShape_usesTheOriginalShapeId() {
        Schema schema = converter.getSchema(RENAMED);

        assertThat(schema.id()).hasToString("test.orig#GetThingRequest");
        assertThat(schema.member("name").id()).hasToString("test.orig#GetThingRequest$name");
        // The cache is keyed by the model's ShapeId.
        assertThat(converter.getSchema(RENAMED)).isSameAs(schema);
        assertThat(converter.getSchemaIndex().getSchema(TestModels.sdkId(RENAMED))).isSameAs(schema);
    }

    @Test
    void getSchema_shapeSynthesizedFromUnit_keepsItsOwnIdAndGetsTheUnitTrait() {
        Schema schema = converter.getSchema(SYNTHETIC_UNIT);

        assertThat(schema.id()).hasToString("test.orig#PingInput");
        assertThat(schema.hasTrait(TraitKey.UNIT_TYPE_TRAIT)).isTrue();
    }

    @Test
    void getSchema_shapeWithoutTheTrait_usesItsOwnId() {
        Schema schema = converter.getSchema(PLAIN);

        assertThat(schema.id()).hasToString("test.orig#Plain");
        assertThat(schema.hasTrait(TraitKey.UNIT_TYPE_TRAIT)).isFalse();
    }
}
