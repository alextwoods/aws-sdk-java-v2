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

package software.amazon.awssdk.codegen.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.SchemaIndex;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;

class SchemaRuntimeTypeTest {
    @Test
    void namesResolveToRuntimeClasses() {
        assertThat(SchemaRuntimeType.SHAPE_ID.reflectionName()).isEqualTo(ShapeId.class.getName());
        assertThat(SchemaRuntimeType.DOCUMENT.reflectionName()).isEqualTo(Document.class.getName());
        assertThat(SchemaRuntimeType.TRAIT.reflectionName()).isEqualTo(Trait.class.getName());
        assertThat(SchemaRuntimeType.GENERIC_TRAIT.reflectionName()).isEqualTo(GenericTrait.class.getName());
        assertThat(SchemaRuntimeType.UNIT_TYPE_TRAIT.reflectionName()).isEqualTo(UnitTypeTrait.class.getName());
        assertThat(SchemaRuntimeType.SCHEMA.reflectionName()).isEqualTo(Schema.class.getName());
        assertThat(SchemaRuntimeType.SCHEMA_BUILDER.reflectionName()).isEqualTo(SchemaBuilder.class.getName());
        assertThat(SchemaRuntimeType.PRELUDE_SCHEMAS.reflectionName()).isEqualTo(PreludeSchemas.class.getName());
        assertThat(SchemaRuntimeType.SCHEMA_INDEX.reflectionName()).isEqualTo(SchemaIndex.class.getName());
    }
}
