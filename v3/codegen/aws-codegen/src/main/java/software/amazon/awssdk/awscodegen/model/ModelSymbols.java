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

package software.amazon.awssdk.awscodegen.model;

import software.amazon.smithy.codegen.core.Symbol;

/**
 * Symbols of the JDK, V3 runtime and Smithy Java runtime types that generated model types reference. They are named,
 * not linked: {@code aws-codegen} does not depend on the runtime. Every type, including {@code java.lang} types, is
 * written through its symbol so that a generated type with the same simple name is handled.
 */
final class ModelSymbols {

    static final Symbol OBJECT = type("java.lang", "Object");
    static final Symbol STRING = type("java.lang", "String");
    static final Symbol INTEGER = type("java.lang", "Integer");
    static final Symbol OVERRIDE = type("java.lang", "Override");
    static final Symbol SUPPRESS_WARNINGS = type("java.lang", "SuppressWarnings");
    static final Symbol SAFE_VARARGS = type("java.lang", "SafeVarargs");
    static final Symbol ILLEGAL_ARGUMENT = type("java.lang", "IllegalArgumentException");
    static final Symbol UNSUPPORTED_OPERATION = type("java.lang", "UnsupportedOperationException");

    static final Symbol LIST = type("java.util", "List");
    static final Symbol MAP = type("java.util", "Map");
    static final Symbol COLLECTION = type("java.util", "Collection");
    static final Symbol ARRAY_LIST = type("java.util", "ArrayList");
    static final Symbol LINKED_HASH_MAP = type("java.util", "LinkedHashMap");
    static final Symbol ARRAYS = type("java.util", "Arrays");
    static final Symbol OBJECTS = type("java.util", "Objects");
    static final Symbol ENUM_SET = type("java.util", "EnumSet");
    static final Symbol SET = type("java.util", "Set");
    static final Symbol CONSUMER = type("java.util.function", "Consumer");
    static final Symbol BIG_INTEGER = type("java.math", "BigInteger");
    static final Symbol BIG_DECIMAL = type("java.math", "BigDecimal");
    static final Symbol INSTANT = type("java.time", "Instant");

    static final Symbol SHAPE_ID = type("software.amazon.smithy.model.shapes", "ShapeId");

    private static final String SMITHY_JAVA_SCHEMA = "software.amazon.smithy.java.core.schema";
    static final Symbol SCHEMA = type(SMITHY_JAVA_SCHEMA, "Schema");
    static final Symbol SCHEMA_UTILS = type(SMITHY_JAVA_SCHEMA, "SchemaUtils");
    static final Symbol SERIALIZABLE_STRUCT = type(SMITHY_JAVA_SCHEMA, "SerializableStruct");
    static final Symbol SHAPE_BUILDER = type(SMITHY_JAVA_SCHEMA, "ShapeBuilder");
    static final Symbol UNIT = type(SMITHY_JAVA_SCHEMA, "Unit");
    static final Symbol EVENT_STREAM = type("software.amazon.smithy.java.core.serde.event", "EventStream");
    static final Symbol SHAPE_SERIALIZER = type("software.amazon.smithy.java.core.serde", "ShapeSerializer");
    static final Symbol SHAPE_DESERIALIZER = type("software.amazon.smithy.java.core.serde", "ShapeDeserializer");
    static final Symbol DOCUMENT = type("software.amazon.smithy.java.core.serde.document", "Document");

    static final Symbol SDK_BYTES = type("software.amazon.awssdk.core", "SdkBytes");
    static final Symbol MODEL_COLLECTIONS = type("software.amazon.awssdk.core.internal.model", "ModelCollections");
    static final Symbol MISSING_REQUIRED_MEMBERS = type("software.amazon.awssdk.core.exception",
                                                        "MissingRequiredMembersException");
    static final Symbol AWS_ERROR_DETAILS = type("software.amazon.awssdk.awscore.exception", "AwsErrorDetails");
    static final Symbol AWS_SERVICE_EXCEPTION = type("software.amazon.awssdk.awscore.exception",
                                                     "AwsServiceException");
    static final Symbol AWS_RESPONSE = type("software.amazon.awssdk.awscore", "AwsResponse");
    static final Symbol AWS_RESPONSE_METADATA = type("software.amazon.awssdk.awscore", "AwsResponseMetadata");
    static final Symbol TO_STRING = type("software.amazon.awssdk.utils", "ToString");
    static final Symbol COPYABLE_BUILDER = type("software.amazon.awssdk.utils.builder", "CopyableBuilder");
    static final Symbol TO_COPYABLE_BUILDER = type("software.amazon.awssdk.utils.builder", "ToCopyableBuilder");

    private ModelSymbols() {
    }

    static Symbol type(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
