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

package software.amazon.awssdk.awscodegen.schema;

import software.amazon.smithy.codegen.core.Symbol;

/**
 * Symbols of the JDK, {@code smithy-runtime} and Smithy Java runtime types that generated schema sources reference.
 * They are named, not linked: {@code aws-codegen} does not depend on the runtime.
 */
final class SchemaRuntimeSymbols {

    static final Symbol SET = type("java.util", "Set");
    static final Symbol BIG_INTEGER = type("java.math", "BigInteger");
    static final Symbol BIG_DECIMAL = type("java.math", "BigDecimal");

    static final Symbol SHAPE_ID = type("software.amazon.smithy.model.shapes", "ShapeId");
    static final Symbol NODE = type("software.amazon.smithy.model.node", "Node");
    static final Symbol DYNAMIC_TRAIT = type("software.amazon.smithy.model.traits", "DynamicTrait");

    private static final String SMITHY_JAVA_SCHEMA = "software.amazon.smithy.java.core.schema";
    static final Symbol SCHEMA = type(SMITHY_JAVA_SCHEMA, "Schema");
    static final Symbol SCHEMA_BUILDER = type(SMITHY_JAVA_SCHEMA, "SchemaBuilder");
    static final Symbol PRELUDE_SCHEMAS = type(SMITHY_JAVA_SCHEMA, "PreludeSchemas");
    static final Symbol UNIT = type(SMITHY_JAVA_SCHEMA, "Unit");

    static final Symbol SERVICE_SCHEMA_INDEX = type("software.amazon.awssdk.smithy.runtime.schema",
                                                    "ServiceSchemaIndex");

    private SchemaRuntimeSymbols() {
    }

    /** The symbol of a top-level runtime class, such as a Smithy prelude trait class. */
    static Symbol type(Class<?> type) {
        if (type.getEnclosingClass() != null) {
            throw new IllegalArgumentException("Nested runtime classes are referenced through their outer class: "
                                               + type.getName());
        }
        return type(type.getPackageName(), type.getSimpleName());
    }

    static Symbol type(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
