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
 * Compatibility test generation from the Parity Manifest (Requirements 17.1, 17.2, 17.3, 19.12).
 *
 * <p>Owns: generation of compile fixtures, binary linkage callers, signature and hierarchy comparisons, and behavior
 * fixtures per compatibility dimension for the {@code PRESERVED}, {@code ADAPTED} and {@code EXCLUDED} entries of a
 * test scope ({@link software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixtureGenerator}), from the
 * manifest and the V2 baseline class files
 * ({@link software.amazon.awssdk.tools.paritymanifest.compat.BaselineClasses}), and their deterministic JSON form
 * ({@link software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixtures},
 * {@link software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixturesCli}). The fixtures are executed
 * against a release candidate by the module that has the candidate on its class path
 * ({@code test/codegen-generated-classes-test}).
 *
 * <p>Allowed dependencies: the root {@code paritymanifest} package and {@code smithy-model} (Smithy Node JSON).
 */
package software.amazon.awssdk.tools.paritymanifest.compat;
