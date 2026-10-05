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
 * Root package of the {@code software.amazon.awssdk:parity-manifest} V3 artifact.
 *
 * <p>Owns: The Parity Manifest schema, model, and release gate: the entry model and stable ID scheme
 * ({@link software.amazon.awssdk.tools.paritymanifest.EntryKind}), the checked-in classification rules
 * ({@link software.amazon.awssdk.tools.paritymanifest.ClassificationRules}), validation
 * ({@link software.amazon.awssdk.tools.paritymanifest.ManifestValidator}), the deterministic JSON form
 * ({@link software.amazon.awssdk.tools.paritymanifest.ManifestJson}, schema {@code parity-manifest-1.schema.json}),
 * and generation from pluggable V2 baseline inventories
 * ({@link software.amazon.awssdk.tools.paritymanifest.InventorySource},
 * {@link software.amazon.awssdk.tools.paritymanifest.ManifestGenerator},
 * {@link software.amazon.awssdk.tools.paritymanifest.ParityManifestCli}), and the launch-surface and behavior
 * inventory gate (task 13.3): what a release candidate ships
 * ({@link software.amazon.awssdk.tools.paritymanifest.CandidateInventoryScanner},
 * {@link software.amazon.awssdk.tools.paritymanifest.CandidateTree}), its evidence hooks
 * ({@link software.amazon.awssdk.tools.paritymanifest.EvidenceHooks}) and candidate evidence
 * ({@link software.amazon.awssdk.tools.paritymanifest.CandidateEvidence}), compared per
 * {@link software.amazon.awssdk.tools.paritymanifest.LaunchCategory} with the manifest by
 * {@link software.amazon.awssdk.tools.paritymanifest.LaunchGate}
 * ({@link software.amazon.awssdk.tools.paritymanifest.LaunchGateCli}).
 *
 * <p>Allowed V3 module dependencies: none. Third-party: {@code software.amazon.smithy:smithy-model} (Smithy Node
 * JSON only; artifact class {@code smithy-model-toolchain}). Forbidden dependencies: production dependency trees.
 */
package software.amazon.awssdk.tools.paritymanifest;
