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

import com.squareup.javapoet.ClassName;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * JavaPoet names of the {@code core/schema} runtime types that generated Schema code references.
 *
 * <p>The names are spelled out instead of taken from {@code Foo.class}, so {@code codegen} does not need the runtime
 * schema module on its compile class path. {@code SchemaRuntimeTypeTest} checks that every name resolves to a class in
 * the runtime module.
 */
@SdkInternalApi
public final class SchemaRuntimeType {
    public static final String SCHEMA_PACKAGE = "software.amazon.awssdk.schema";
    public static final String TRAITS_PACKAGE = "software.amazon.awssdk.schema.traits";
    public static final String DOCUMENT_PACKAGE = "software.amazon.awssdk.schema.serde.document";

    public static final ClassName SHAPE_ID = ClassName.get(SCHEMA_PACKAGE, "ShapeId");
    public static final ClassName DOCUMENT = ClassName.get(DOCUMENT_PACKAGE, "Document");
    public static final ClassName TRAIT = ClassName.get(TRAITS_PACKAGE, "Trait");
    public static final ClassName GENERIC_TRAIT = ClassName.get(TRAITS_PACKAGE, "GenericTrait");
    public static final ClassName UNIT_TYPE_TRAIT = ClassName.get(TRAITS_PACKAGE, "UnitTypeTrait");
    public static final ClassName SCHEMA = ClassName.get(SCHEMA_PACKAGE, "Schema");
    public static final ClassName SCHEMA_BUILDER = ClassName.get(SCHEMA_PACKAGE, "SchemaBuilder");
    public static final ClassName PRELUDE_SCHEMAS = ClassName.get(SCHEMA_PACKAGE, "PreludeSchemas");
    public static final ClassName SCHEMA_INDEX = ClassName.get(SCHEMA_PACKAGE, "SchemaIndex");
    public static final ClassName SERIALIZABLE_STRUCT = ClassName.get(SCHEMA_PACKAGE, "SerializableStruct");
    public static final ClassName API_OPERATION = ClassName.get(SCHEMA_PACKAGE, "ApiOperation");
    public static final ClassName API_SERVICE = ClassName.get(SCHEMA_PACKAGE, "ApiService");
    public static final ClassName SIMPLE_API_OPERATION = ClassName.get(SCHEMA_PACKAGE, "SimpleApiOperation");
    public static final ClassName ERROR_REGISTRY = ClassName.get(SCHEMA_PACKAGE + ".serde", "ErrorRegistry");
    public static final ClassName SHAPE_BUILDER = ClassName.get(SCHEMA_PACKAGE, "ShapeBuilder");
    public static final ClassName SMITHY_ENUM = ClassName.get(SCHEMA_PACKAGE, "SmithyEnum");
    public static final ClassName SCHEMA_UTILS = ClassName.get(SCHEMA_PACKAGE, "SchemaUtils");
    public static final ClassName SHAPE_SERIALIZER = ClassName.get(SCHEMA_PACKAGE + ".serde", "ShapeSerializer");
    public static final ClassName SHAPE_DESERIALIZER = ClassName.get(SCHEMA_PACKAGE + ".serde", "ShapeDeserializer");
    public static final ClassName MAP_SERIALIZER = ClassName.get(SCHEMA_PACKAGE + ".serde", "MapSerializer");
    public static final ClassName TO_STRING_SERIALIZER = ClassName.get(SCHEMA_PACKAGE + ".serde", "ToStringSerializer");

    /**
     * {@code sdk-core} types that generated schema serde code uses (design A.6).
     */
    public static final ClassName SDK_FIELDS_FROM_SCHEMA = ClassName.get("software.amazon.awssdk.core.schema",
                                                                         "SdkFieldsFromSchema");
    public static final ClassName SCHEMA_SERDE_SUPPORT = ClassName.get("software.amazon.awssdk.core.schema",
                                                                       "SchemaSerdeSupport");

    private SchemaRuntimeType() {
    }

    /**
     * Returns the name of a runtime trait class in {@code software.amazon.awssdk.schema.traits}.
     *
     * @param simpleName the class name, for example {@code "JsonNameTrait"}
     * @return the class name
     */
    public static ClassName trait(String simpleName) {
        return ClassName.get(TRAITS_PACKAGE, simpleName);
    }
}
