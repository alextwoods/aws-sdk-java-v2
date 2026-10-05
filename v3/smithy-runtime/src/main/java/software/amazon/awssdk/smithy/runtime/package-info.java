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

/**
 * Root package of the {@code software.amazon.awssdk:smithy-runtime} V3 artifact.
 *
 * <p>Owns: The curated re-export and compatibility boundary for Smithy Java Schema, SerializableStruct, Document,
 * codecs, and protocol interfaces, plus the runtime version handshake.
 *
 * <p>Allowed V3 module dependencies: none yet. Forbidden dependencies: Smithy model parser/assembler and
 * code-generation artifacts.
 *
 * <p>Generated code uses the pinned Smithy Java runtime types (shape identity, runtime traits, {@code Schema},
 * {@code SerializableStruct}, {@code ShapeBuilder}, {@code Document}, {@code TypeRegistry} and the JSON, CBOR and
 * XML codecs) directly. {@code software.amazon.awssdk.smithy.runtime.schema} adds the V3 schema metadata that Smithy
 * Java's {@code Schema} cannot carry (unknown traits by exact trait ID, enum member schemas, modeled member order and
 * operation schema links). The artifact and class rules that keep Smithy model parsing, assembly, and code generation
 * off the generated-client path are defined by
 * {@link software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary}.
 */
package software.amazon.awssdk.smithy.runtime;
