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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessLaunchTest {
    private static final List<String> FLAGS = List.of("-Xms64m", "-Xmx64m", "-XX:+UseSerialGC");
    private static final BenchmarkCell CELL = BenchmarkCell.parse(
        "awsJson1_0/dynamic-async/netty-async/finite/medium/retry/c8");

    private static HarnessConfig config(HarnessMode mode) {
        return new HarnessConfig(mode, "/jdk/bin/java", FLAGS, "/a.jar:/b.jar", Duration.ofMillis(1500),
                                 Duration.ofSeconds(30), Duration.ofMinutes(5), Path.of("/tmp/out"), List.of(),
                                 List.of(), "unmanaged", Map.of("dynamodb", Path.of("/models/ddb.json")));
    }

    private static RecordedProbe linux() {
        return new RecordedProbe("linux").executable("taskset", "/usr/bin/taskset");
    }

    @Test
    void bothProcessesGetTheSameJvmFlagsJdkAndClasspathAndTheirOwnPinning() {
        HarnessOrchestrator o = new HarnessOrchestrator(config(HarnessMode.RELEASE), linux());
        CpuPinning.Plan plan = CpuPinning.plan(linux(), HarnessMode.RELEASE, List.of(4, 5, 6, 7), List.of(1, 2), 8);
        JvmLaunchSpec service = o.mockServiceSpec(CELL, plan);
        RepetitionSchedule.ScheduledRun run = new RepetitionSchedule.ScheduledRun(CELL, 3, 2, "arm#null");
        JvmLaunchSpec client = o.clientSpec(run, URI.create("http://127.0.0.1:4567"), Path.of("/tmp/out/r.json"), plan);

        assertEquals(List.of("/usr/bin/taskset", "-c", "1-2", "/jdk/bin/java", "-Xms64m", "-Xmx64m",
                             "-XX:+UseSerialGC", "-cp", "/a.jar:/b.jar",
                             "software.amazon.awssdk.benchmark.harness.MockServiceMain",
                             "--cell", CELL.id(), "--port", "0", "--threads", "16"),
                     service.command());
        assertEquals(List.of("/usr/bin/taskset", "-c", "4-7", "/jdk/bin/java", "-Xms64m", "-Xmx64m",
                             "-XX:+UseSerialGC", "-cp", "/a.jar:/b.jar",
                             "software.amazon.awssdk.benchmark.harness.ClientMain",
                             "--cell", CELL.id(), "--endpoint", "http://127.0.0.1:4567",
                             "--warmup-seconds", "1.500", "--measure-seconds", "30.000",
                             "--result-file", "/tmp/out/r.json", "--repetition", "3", "--label", "arm#null",
                             "--model", "dynamodb=/models/ddb.json"),
                     client.command());
        assertEquals(service.jvmFlags(), client.jvmFlags());
        assertEquals(service.javaExecutable(), client.javaExecutable());
        assertEquals(service.classpath(), client.classpath());
    }

    @Test
    void clientArgumentsParseBackToTheScheduledRun() {
        HarnessOrchestrator o = new HarnessOrchestrator(config(HarnessMode.EXPLORATORY), new RecordedProbe("mac os x"));
        CpuPinning.Plan plan = CpuPinning.plan(new RecordedProbe("mac os x"), HarnessMode.EXPLORATORY, List.of(),
                                               List.of(), 8);
        RepetitionSchedule.ScheduledRun run = new RepetitionSchedule.ScheduledRun(CELL, 2, 1, "label-x");
        List<String> argv = o.clientSpec(run, URI.create("http://127.0.0.1:1"), Path.of("r.json"), plan).programArgs();
        ClientMain.Args args = ClientMain.Args.parse(argv.toArray(new String[0]));
        assertEquals(CELL, args.cell());
        assertEquals(URI.create("http://127.0.0.1:1"), args.endpoint());
        assertEquals(Duration.ofMillis(1500), args.warmup());
        assertEquals(Duration.ofSeconds(30), args.measure());
        assertEquals(2, args.repetition());
        assertEquals("label-x", args.label());
        assertEquals(Map.of("dynamodb", Path.of("/models/ddb.json")), args.models());
        // Unpinned on macOS: the command starts with java itself.
        assertEquals("/jdk/bin/java", o.mockServiceSpec(CELL, plan).command().get(0));
    }

    @Test
    void releaseModeRefusesCellsWithoutFixtures() {
        HarnessOrchestrator o = new HarnessOrchestrator(config(HarnessMode.RELEASE), linux());
        // Every matrix cell has a fixture (CellMatrixTest), so the gate is exercised with a well-formed cell that no
        // provider serves: an awsJson1.0 Event Stream cell, outside the matrix (no fleet event stream on awsJson1.0).
        BenchmarkCell uncovered = ForkedHarnessSmokeTest.UNCOVERED;
        assertTrue(WorkloadProvider.forCell(uncovered).isEmpty());
        assertTrue(CellMatrix.inapplicability(uncovered).isPresent());
        HarnessPolicyException e = assertThrows(HarnessPolicyException.class, () -> o.preflight(List.of(CELL, uncovered)));
        assertTrue(e.getMessage().contains(uncovered.id()), e.getMessage());
    }

    @Test
    void launchSpecsRejectClasspathFlagsAndClientArgsRejectBadInput() {
        assertThrows(IllegalArgumentException.class, () -> new JvmLaunchSpec(
            "client", List.of(), "java", List.of("-cp", "x"), "cp", "Main", List.of()));
        assertThrows(IllegalArgumentException.class, () -> ClientMain.Args.parse(new String[] {"--cell", CELL.id()}));
        assertThrows(IllegalArgumentException.class, () -> ClientMain.Args.parse(new String[] {
            "--cell", CELL.id(), "--endpoint", "http://x", "--result-file", "r", "--measure-seconds", "0"}));
        assertEquals(Map.of("port", "4321", "pid", "77", "cell", CELL.id()),
                     MockServiceProcess.parseReady("READY port=4321 pid=77 cell=" + CELL.id()));
    }

    @Test
    void retryInjectionIsFairAndUncorrelatedWithLockstepCallers() {
        // A caller in lockstep with k-1 others sees every k-th request number; plain alternation fails such a caller
        // on every attempt when k is even. The per-request coin must stay fair along every such stride.
        for (int stride = 1; stride <= 32; stride++) {
            for (int offset = 1; offset <= Math.min(stride, 4); offset++) {
                int failed = 0;
                int longestRun = 0;
                int run = 0;
                int n = 20_000;
                for (int i = 0; i < n; i++) {
                    if (MockServiceMain.injectRetry(offset + (long) i * stride)) {
                        failed++;
                        longestRun = Math.max(longestRun, ++run);
                    } else {
                        run = 0;
                    }
                }
                double rate = failed / (double) n;
                assertTrue(rate > 0.48 && rate < 0.52, "stride " + stride + " offset " + offset + " rate " + rate);
                assertTrue(longestRun < DynamoDbWorkloads.MAX_ATTEMPTS, "stride " + stride + " run " + longestRun);
            }
        }
    }

    @Test
    void latencyHistogramReportsWithinOneSixtyFourth() {
        LatencyHistogram h = new LatencyHistogram();
        for (long v = 1; v <= 1000; v++) {
            h.record(v * 1000);
        }
        assertEquals(1000, h.count());
        long p50 = h.quantile(0.5);
        long p99 = h.quantile(0.99);
        assertTrue(p50 >= 500_000 && p50 <= 500_000 + 500_000 / 64, Long.toString(p50));
        assertTrue(p99 >= 990_000 && p99 <= 990_000 + 990_000 / 64, Long.toString(p99));
        assertEquals(-1, new LatencyHistogram().quantile(0.5));
        for (int i = 0; i < 64 * 40; i++) {
            assertEquals(i, LatencyHistogram.index(LatencyHistogram.upperBound(i)));
        }
    }
}
