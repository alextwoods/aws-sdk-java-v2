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

package software.amazon.awssdk.benchmark.dynamodb;

import java.lang.management.ManagementFactory;
import java.util.Locale;

/**
 * Build provenance and host/JVM metadata for the run header and results rows (racecar's {@code BuildProvenance},
 * plus the Req 20 host/JDK/JVM-flag metadata).
 *
 * <p>Deviation: racecar filters a {@code benchmark-provenance.properties} resource at build time. The V3 module does no
 * resource filtering, so {@code scripts/benchmark.sh} computes provenance when it launches the JVM and passes it as
 * system properties: {@code bench.phase}, {@code bench.commit}, {@code bench.branch}, {@code bench.dirty} (count of
 * modified tracked files), {@code bench.harness} (SHA-256 over {@code target/classes}) and {@code bench.sdk}
 * (SHA-256 over the V3 jars on the classpath, i.e. the SDK under test as installed by the reactor build).
 */
final class BuildProvenance {

    private BuildProvenance() {
    }

    static String phase() {
        return System.getProperty("bench.phase", "unspecified");
    }

    static String commit() {
        return abbreviate(System.getProperty("bench.commit", "unknown"));
    }

    static String dirty() {
        return System.getProperty("bench.dirty", "unknown");
    }

    static String harness() {
        return abbreviate(System.getProperty("bench.harness", "unrecorded"));
    }

    static String sdk() {
        return abbreviate(System.getProperty("bench.sdk", "unrecorded"));
    }

    static String summary() {
        return String.format(Locale.US, "phase=%s commit=%s branch=%s dirty_files=%s harness=%s sdk=%s",
                             phase(), commit(), System.getProperty("bench.branch", "unknown"), dirty(), harness(),
                             sdk());
    }

    /** Host and JVM metadata visible from Java; the scripts add the macOS/Linux host policy to the manifest. */
    static String host() {
        Runtime rt = Runtime.getRuntime();
        return String.format(Locale.US,
                             "os=%s/%s/%s cpus=%d jdk=%s(%s %s) max_heap_mb=%d jvm_flags=%s",
                             System.getProperty("os.name"), System.getProperty("os.version"),
                             System.getProperty("os.arch"), rt.availableProcessors(),
                             System.getProperty("java.version"), System.getProperty("java.vm.vendor"),
                             System.getProperty("java.vm.version"), rt.maxMemory() / (1024 * 1024),
                             String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()
                                                               .stream().filter(a -> !a.startsWith("-Dbench."))
                                                               .toList()));
    }

    private static String abbreviate(String sha) {
        return sha.length() > 12 ? sha.substring(0, 12) : sha;
    }
}
