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
 * The deterministic pipeline fault-injection kit (task 18.5; Requirements 19.7, 19.8). Test and tooling code only.
 *
 * <h2>Describing a fault</h2>
 * A {@link software.amazon.awssdk.test.fault.FaultTrace} is pure immutable data: what the transport does with each
 * transmission ({@link software.amazon.awssdk.test.fault.TransportFault}), what the identity provider and signer do
 * with each use ({@link software.amazon.awssdk.test.fault.IdentityStep},
 * {@link software.amazon.awssdk.test.fault.SignerStep}), whether endpoint resolution fails
 * ({@link software.amazon.awssdk.test.fault.EndpointMode}), the request body
 * ({@link software.amazon.awssdk.test.fault.BodyKind}), where the caller cancels
 * ({@link software.amazon.awssdk.test.fault.CancelPoint}) and the deterministic retry strategy
 * ({@link software.amazon.awssdk.test.fault.RetrySettings}). Property tests generate traces; an oracle predicts their
 * outcome from the trace alone.
 *
 * <h2>Running a fault</h2>
 * {@link software.amazon.awssdk.test.fault.FaultHarness} builds a generated service's real client runtime and engine
 * with every dependency replaced by a fake of a {@link software.amazon.awssdk.test.fault.FaultRig}, runs one call on
 * either driver, applies the cancellation, and returns a {@link software.amazon.awssdk.test.fault.FaultOutcome}:
 * terminal outcome, attempts, transmissions, retry delays (exact on the async driver's virtual clock), the token
 * ledger, every exchange, hook and telemetry counts, leaks and contract violations.
 *
 * <h2>Using the fakes directly</h2>
 * Every fake works on its own, for suites that build clients themselves (generated client builders, concurrency
 * suites): {@link software.amazon.awssdk.test.fault.FaultyHttpClient} (sync and async views of one script),
 * {@link software.amazon.awssdk.test.fault.ScriptedIdentityProvider},
 * {@link software.amazon.awssdk.test.fault.FaultAuthScheme} with
 * {@link software.amazon.awssdk.test.fault.ScriptedSigner}, {@link software.amazon.awssdk.test.fault.FaultEndpoints},
 * {@link software.amazon.awssdk.test.fault.VirtualRetryScheduler},
 * {@link software.amazon.awssdk.test.fault.RecordingRetryStrategy},
 * {@link software.amazon.awssdk.test.fault.LifecycleProbe}, {@link software.amazon.awssdk.test.fault.TelemetryProbe}
 * and {@link software.amazon.awssdk.test.fault.FaultBodies}. They are thread-safe; each records contract violations
 * into a shared {@link software.amazon.awssdk.test.fault.Violations} instead of throwing, so the pipeline's own
 * behaviour is what a test observes.
 */
package software.amazon.awssdk.test.fault;
