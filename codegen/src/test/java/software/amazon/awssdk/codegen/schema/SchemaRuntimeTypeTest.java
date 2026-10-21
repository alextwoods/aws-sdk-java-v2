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
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.schema.SdkFieldsFromSchema;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.SchemaIndex;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.SmithyEnum;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.ToStringSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
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
        assertThat(SchemaRuntimeType.SERIALIZABLE_STRUCT.reflectionName()).isEqualTo(SerializableStruct.class.getName());
        assertThat(SchemaRuntimeType.API_OPERATION.reflectionName()).isEqualTo(ApiOperation.class.getName());
        assertThat(SchemaRuntimeType.API_SERVICE.reflectionName()).isEqualTo(ApiService.class.getName());
        assertThat(SchemaRuntimeType.SIMPLE_API_OPERATION.reflectionName())
            .isEqualTo(SimpleApiOperation.class.getName());
        assertThat(SchemaRuntimeType.ERROR_REGISTRY.reflectionName()).isEqualTo(ErrorRegistry.class.getName());
        assertThat(SchemaRuntimeType.TYPE_REGISTRY.reflectionName()).isEqualTo(TypeRegistry.class.getName());
        assertThat(SchemaRuntimeType.SHAPE_BUILDER.reflectionName()).isEqualTo(ShapeBuilder.class.getName());
        assertThat(SchemaRuntimeType.SMITHY_ENUM.reflectionName()).isEqualTo(SmithyEnum.class.getName());
        assertThat(SchemaRuntimeType.SCHEMA_UTILS.reflectionName()).isEqualTo(SchemaUtils.class.getName());
        assertThat(SchemaRuntimeType.SHAPE_SERIALIZER.reflectionName()).isEqualTo(ShapeSerializer.class.getName());
        assertThat(SchemaRuntimeType.SHAPE_DESERIALIZER.reflectionName()).isEqualTo(ShapeDeserializer.class.getName());
        assertThat(SchemaRuntimeType.MAP_SERIALIZER.reflectionName()).isEqualTo(MapSerializer.class.getName());
        assertThat(SchemaRuntimeType.TO_STRING_SERIALIZER.reflectionName())
            .isEqualTo(ToStringSerializer.class.getName());
        assertThat(SchemaRuntimeType.SDK_FIELDS_FROM_SCHEMA.reflectionName())
            .isEqualTo(SdkFieldsFromSchema.class.getName());
        assertThat(SchemaRuntimeType.SCHEMA_SERDE_SUPPORT.reflectionName())
            .isEqualTo(SchemaSerdeSupport.class.getName());
    }
}
