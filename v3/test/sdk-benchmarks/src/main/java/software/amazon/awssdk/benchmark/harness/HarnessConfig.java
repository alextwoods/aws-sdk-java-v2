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
 * @param warmup client warmup per repetition (fixed duration until 19.2's steady-state gate)
 * @param measure client measurement window per repetition
 * @param processTimeout the longest a client process may run, and a mock service may take to become ready
 * @param clientCpus explicit client CPUs, or empty for the default split
 * @param serviceCpus explicit mock-service CPUs, or empty for the default split
 * @param backgroundPolicy the declared background-service policy, e.g. {@code unmanaged} or a runbook id
 * @param models model files by service, forwarded to the client for Dynamic Client cells
 */
public record HarnessConfig(HarnessMode mode, String javaExecutable, List<String> jvmFlags, String classpath,
                            Duration warmup, Duration measure, Duration processTimeout, Path outputDirectory,
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
        if (measure.isZero() || measure.isNegative()) {
            throw new IllegalArgumentException("measure must be positive");
        }
    }

    /** The {@code java} of the running JVM. */
    public static String currentJava() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
