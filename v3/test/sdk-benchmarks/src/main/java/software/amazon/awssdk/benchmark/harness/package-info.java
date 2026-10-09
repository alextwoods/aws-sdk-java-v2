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
 * The forked-process Performance Harness (Req 20.1-20.10, 20.23-20.28).
 *
 * <ul>
 *     <li>Cell model: {@link software.amazon.awssdk.benchmark.harness.BenchmarkCell} over protocol, client path,
 *     explicit transport, workload kind, payload class, outcome and concurrency;
 *     {@link software.amazon.awssdk.benchmark.harness.CellMatrix} enumerates the required matrix.</li>
 *     <li>Processes: {@link software.amazon.awssdk.benchmark.harness.HarnessOrchestrator} launches one
 *     {@link software.amazon.awssdk.benchmark.harness.MockServiceMain} and one
 *     {@link software.amazon.awssdk.benchmark.harness.ClientMain} JVM per scheduled run, pinned by
 *     {@link software.amazon.awssdk.benchmark.harness.CpuPinning}, and records
 *     {@link software.amazon.awssdk.benchmark.harness.HostMetadata}.</li>
 *     <li>Workloads: {@link software.amazon.awssdk.benchmark.harness.WorkloadProvider} supplies the fixture and client
 *     of covered cells; uncovered cells are recorded, and refused in release mode.</li>
 *     <li>Schedule: {@link software.amazon.awssdk.benchmark.harness.RepetitionSchedule#interleaved} runs 5-10 rotated
 *     rounds of every {@link software.amazon.awssdk.benchmark.harness.BenchmarkArm}, including a same-artifact null
 *     arm per cell ({@link software.amazon.awssdk.benchmark.harness.BenchmarkArm#nullTwin()}).</li>
 *     <li>Steady state: {@link software.amazon.awssdk.benchmark.harness.SteadyStatePolicy} documents the JIT gate and
 *     rolling stability thresholds; {@link software.amazon.awssdk.benchmark.harness.SteadyStateDetector} evaluates
 *     them in the client and again in the validity rules.</li>
 *     <li>Validity: {@link software.amazon.awssdk.benchmark.harness.ValidityRules} (per repetition),
 *     {@link software.amazon.awssdk.benchmark.harness.CollectionValidity} (cells, null experiments, collection), wired
 *     in as {@link software.amazon.awssdk.benchmark.harness.ValiditySink}, the first
 *     {@link software.amazon.awssdk.benchmark.harness.ResultSink}; statistics, reports (19.3) and gates (19.4) consume
 *     its annotated records and verdict.</li>
 * </ul>
 *
 * <p>The older {@code software.amazon.awssdk.benchmark.dynamodb} runner (serde+transport arms, {@code paired.sh})
 * remains as the Racecar-comparable microbenchmark-scope tool; only this package produces release-gate evidence.
 */
package software.amazon.awssdk.benchmark.harness;
