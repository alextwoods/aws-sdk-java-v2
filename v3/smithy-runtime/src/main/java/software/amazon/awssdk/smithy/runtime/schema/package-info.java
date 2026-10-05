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
 * V3 schema metadata that complements Smithy Java's {@code Schema} for generated services.
 *
 * <p>Generated services build their schemas with Smithy Java's {@code Schema} and {@code SchemaBuilder} directly and
 * add, through these types, the modeled facts that {@code Schema} cannot carry:
 * <ul>
 *   <li>{@link software.amazon.awssdk.smithy.runtime.schema.UnknownTraits}: unknown traits keyed by exact trait ID,
 *       any number per shape or member (Smithy Java stores traits by Java class);</li>
 *   <li>{@link software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex}: exact-ID lookup of every schema of a
 *       service, enum and intEnum member schemas, modeled member order, and the unknown traits of every shape and
 *       member;</li>
 *   <li>{@link software.amazon.awssdk.smithy.runtime.schema.OperationSchemas}: the links from an operation schema to
 *       its input, output, error and streaming member schemas.</li>
 * </ul>
 * Everything is constructed directly by generated code: no type in this package parses or loads a Smithy model, uses
 * reflection or discovers services.
 */
package software.amazon.awssdk.smithy.runtime.schema;
