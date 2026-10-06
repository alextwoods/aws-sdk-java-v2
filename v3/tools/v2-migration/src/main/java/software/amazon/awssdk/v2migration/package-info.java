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
 * Root package of the {@code software.amazon.awssdk:v2-migration} V3 artifact: the V2-to-V3 Migration Tooling
 * (Requirement 17).
 *
 * <p>Owns: the migration engine ({@link software.amazon.awssdk.v2migration.MigrationEngine}), which inventories a
 * project's effective V2 dependency/BOM graph and Java sources into {@link software.amazon.awssdk.v2migration.MigrationItem}s,
 * joins each to the Parity Manifest ({@code manifest}), offers it to the recipes of the extension point
 * ({@code recipe}), and records exactly one {@link software.amazon.awssdk.v2migration.Disposition} per item with
 * deterministic edits and source-located diagnostics; the machine-readable report
 * ({@link software.amazon.awssdk.v2migration.MigrationReport}); and the Maven goals ({@code maven}).
 *
 * <p>Allowed V3 module dependencies: none. Third-party: {@code software.amazon.smithy:smithy-model} (Smithy Node JSON
 * only; artifact class {@code smithy-model-toolchain}) and the provided Maven plugin APIs ({@code maven-plugin-api}).
 * Java source is parsed and attributed with the JDK's {@code jdk.compiler} tree API. Forbidden dependencies: V2 and
 * V3 runtime artifacts; the tool reads a project's V2 code and class path but never links it.
 */
package software.amazon.awssdk.v2migration;
