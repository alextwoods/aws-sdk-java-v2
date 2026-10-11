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
