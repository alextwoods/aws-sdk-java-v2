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

package software.amazon.awssdk.awscodegen;

import software.amazon.smithy.codegen.core.Symbol;

/** Symbols of JDK and V3 runtime types that generated sources reference. */
final class RuntimeSymbols {

    static final Symbol STRING = type("java.lang", "String");
    static final Symbol LIST = type("java.util", "List");
    static final Symbol OPTIONAL = type("java.util", "Optional");

    /** Smithy shape identity, exposed to generated services through {@code smithy-runtime}. */
    static final Symbol SHAPE_ID = type("software.amazon.smithy.model.shapes", "ShapeId");

    /** The static operation descriptor contract in {@code aws-core}. */
    static final Symbol AWS_OPERATION_DESCRIPTOR = type("software.amazon.awssdk.awscore.operation",
                                                        "AwsOperationDescriptor");

    /** The links from an operation schema to its input, output, error and stream member schemas, in smithy-runtime. */
    static final Symbol OPERATION_SCHEMAS = type("software.amazon.awssdk.smithy.runtime.schema", "OperationSchemas");
    /** The reader of the model provenance published in each service artifact, in {@code aws-core}. */
    static final Symbol MODEL_PROVENANCE = type("software.amazon.awssdk.awscore.metadata", "ModelProvenance");

    /** Smithy Java's schema type, exposed through {@code smithy-runtime}. */
    static final Symbol SCHEMA = type("software.amazon.smithy.java.core.schema", "Schema");
    /** The exact-ID view of a service's schemas, in {@code smithy-runtime}. */
    static final Symbol SERVICE_SCHEMA_INDEX = type("software.amazon.awssdk.smithy.runtime.schema",
                                                    "ServiceSchemaIndex");
    /** The schema-aware type registry of generated services, in {@code smithy-runtime}. */
    static final Symbol SCHEMA_TYPE_REGISTRY = type("software.amazon.awssdk.smithy.runtime.registry",
                                                    "SchemaTypeRegistry");
    /** One entry of a {@code SchemaTypeRegistry}, in {@code smithy-runtime}. */
    static final Symbol REGISTERED_TYPE = type("software.amazon.awssdk.smithy.runtime.registry", "RegisteredType");

    /** The declared protocols of a service, in {@code protocol-core}. */
    static final Symbol SERVICE_PROTOCOLS = type("software.amazon.awssdk.protocols.core", "ServiceProtocols");
    /** One declared protocol trait with its options, in {@code protocol-core}. */
    static final Symbol MODELED_PROTOCOL = type("software.amazon.awssdk.protocols.core", "ModeledProtocol");

    private RuntimeSymbols() {
    }

    private static Symbol type(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
