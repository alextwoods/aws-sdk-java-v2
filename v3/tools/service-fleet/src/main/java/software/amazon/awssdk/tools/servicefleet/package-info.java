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
 * The V3 service fleet (task 13.2; Requirements 2.1, 2.2, 2.10, 4.1, 7.1, 8.1, 16.4, 16.5).
 *
 * <ul>
 *   <li>{@link software.amazon.awssdk.tools.servicefleet.ServiceFleetPlanner} maps every service of the V2 Launch
 *       Parity Baseline to exactly one canonical Smithy model of a pinned aws-models revision, by SDK ID.</li>
 *   <li>{@link software.amazon.awssdk.tools.servicefleet.ServiceFleetLock} is the pinned plan,
 *       {@code v3/services/service-fleet.json}: the revision, and per service its module, package, service shape ID,
 *       protocol modules, required V2 client types and the digests of its vendored model files.</li>
 *   <li>{@link software.amazon.awssdk.tools.servicefleet.ServiceFleetWriter} writes the service modules, their vendored
 *       model files, the services aggregator's module list and pinned revision, and the BOM entries from the lock.</li>
 *   <li>{@link software.amazon.awssdk.tools.servicefleet.ServiceFleetGate} fails the fleet build for a baseline
 *       service without a generated service module, for any module, aggregator or BOM content that is not exactly
 *       what the lock renders, and for every provenance mismatch between the lock, the vendored model files and the
 *       canonical checkout.</li>
 * </ul>
 *
 * <p>Each generated module passes its pinned model digest and required types to {@code aws-codegen:generate}, which
 * fails the module's build on a provenance mismatch or a missing client surface; missing AWS semantics fail the same
 * goal through {@code aws-codegen}'s semantic validation.
 */
package software.amazon.awssdk.tools.servicefleet;
