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

package software.amazon.awssdk.benchmark.harness;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Everything a harness run is configured with; recorded verbatim in the run manifest.
 *
 * @param javaExecutable the {@code java} both forked processes run
 * @param jvmFlags the one JVM flag list both forked processes get
 * @param classpath the classpath of both forked processes (this module's classes and runtime dependencies)
 * @param warmup client warmup per repetition: the steady-state gate (required with
 *     {@link SteadyStatePolicy#RELEASE} in release mode) or, exploratory only, a fixed duration
 * @param measure client measurement window per repetition (at least
 *     {@link SteadyStatePolicy#MIN_RELEASE_MEASURE} in release mode)
 * @param processTimeout the longest a client process may run, and a mock service may take to become ready
 * @param clientCpus explicit client CPUs, or empty for the default split
 * @param serviceCpus explicit mock-service CPUs, or empty for the default split
 * @param backgroundPolicy the declared background-service policy, e.g. {@code unmanaged} or a runbook id
 * @param models model files by service, forwarded to the client for Dynamic Client cells
 */
public record HarnessConfig(HarnessMode mode, String javaExecutable, List<String> jvmFlags, String classpath,
                            WarmupPolicy warmup, Duration measure, Duration processTimeout, Path outputDirectory,
                            List<Integer> clientCpus, List<Integer> serviceCpus, String backgroundPolicy,
                            Map<String, Path> models) {

    /** Shared by client and mock service; as {@code scripts/lib.sh}, plus native access for CRT. */
    public static final List<String> DEFAULT_JVM_FLAGS = List.of(
        "-Xms2g", "-Xmx2g", "-XX:+AlwaysPreTouch", "-XX:ParallelGCThreads=4", "-XX:CICompilerCount=4",
        "--enable-native-access=ALL-UNNAMED");

    public HarnessConfig {
        jvmFlags = List.copyOf(jvmFlags);
        clientCpus = List.copyOf(clientCpus);
        serviceCpus = List.copyOf(serviceCpus);
        models = Map.copyOf(models);
        java.util.Objects.requireNonNull(warmup, "warmup");
        if (measure.isZero() || measure.isNegative()) {
            throw new IllegalArgumentException("measure must be positive");
        }
    }

    /** A fixed-duration warmup sampled at the documented interval (exploratory runs). */
    public HarnessConfig(HarnessMode mode, String javaExecutable, List<String> jvmFlags, String classpath,
                         Duration fixedWarmup, Duration measure, Duration processTimeout, Path outputDirectory,
                         List<Integer> clientCpus, List<Integer> serviceCpus, String backgroundPolicy,
                         Map<String, Path> models) {
        this(mode, javaExecutable, jvmFlags, classpath, WarmupPolicy.fixed(fixedWarmup, SteadyStatePolicy.RELEASE),
             measure, processTimeout, outputDirectory, clientCpus, serviceCpus, backgroundPolicy, models);
    }

    /**
     * Why this configuration cannot produce a release-gate collection, or empty: release mode requires the documented
     * steady-state gate (Req 20.7, 20.9) and at least {@link SteadyStatePolicy#MIN_RELEASE_MEASURE} of measurement.
     */
    public List<String> releasePolicyProblems() {
        List<String> problems = new java.util.ArrayList<>();
        if (warmup.kind() != WarmupPolicy.Kind.STEADY_STATE) {
            problems.add("release mode requires the steady-state warmup gate, not " + warmup.kind().id());
        }
        if (!warmup.sampling().isDocumented()) {
            problems.add("release mode requires the documented steady-state policy " + SteadyStatePolicy.RELEASE
                         + ", not " + warmup.sampling());
        }
        if (measure.compareTo(SteadyStatePolicy.MIN_RELEASE_MEASURE) < 0) {
            problems.add("release mode measures at least " + SteadyStatePolicy.MIN_RELEASE_MEASURE.toSeconds()
                         + " s per repetition, not " + measure.toNanos() / 1e9 + " s");
        }
        return problems;
    }

    /** The {@code java} of the running JVM. */
    public static String currentJava() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
