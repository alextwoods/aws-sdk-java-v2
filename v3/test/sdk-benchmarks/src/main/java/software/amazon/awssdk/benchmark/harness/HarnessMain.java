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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line of the forked-process harness ({@code scripts/harness.sh}).
 *
 * <pre>
 * HarnessMain [--mode exploratory|release] [--list] [--dry-run]
 *             [--protocols ids] [--paths ids] [--transports ids] [--kinds ids] [--payloads ids]
 *             [--outcomes ids] [--concurrency 1,8,32]
 *             [--repetitions N] [--null-experiment true|false]
 *             [--warmup-policy fixed|steady-state] [--warmup-seconds S]
 *             [--sample-interval-seconds S] [--steady-window N] [--warmup-timeout-seconds S]
 *             [--measure-seconds S] [--timeout-seconds S]
 *             [--java PATH] [--jvm-flags "..."] [--client-cpus 4-7] [--service-cpus 1-3]
 *             [--background-policy NAME] [--model dynamodb=PATH] [--out DIR]
 * </pre>
 *
 * <p>Defaults by mode: {@code release} runs 5 interleaved repetitions with a same-artifact null arm, the documented
 * steady-state gate ({@link SteadyStatePolicy#RELEASE}) and 30 s of measurement, and refuses anything weaker in
 * preflight; {@code exploratory} runs each cell once, without a null arm, after a fixed 10 s warmup, all marked as
 * non-evidence. The arms are {@link BenchmarkArm#V3} (and its null twin) plus every {@link PerformanceReferences}
 * reference that is ready, on the cells it applies to; a release collection fails closed unless all three references
 * are ready.
 *
 * <p>{@code --list} prints the selected cells (and whether each has a fixture); {@code --dry-run} additionally prints
 * the preflight (pinning, coverage, schedule problems) and the exact argv of each process without launching anything.
 * A run writes {@code manifest.json}, {@code results.jsonl} (each record with its {@code validity}),
 * {@code validity.json} ({@link ValiditySink}) and {@code report.json} ({@link PerformanceReport}).
 */
public final class HarnessMain {

    private HarnessMain() {
    }

    public static void main(String[] argv) throws Exception {
        Map<String, String> a = new LinkedHashMap<>();
        List<String> flags = new ArrayList<>();
        Map<String, Path> models = new LinkedHashMap<>();
        for (int i = 0; i < argv.length; i++) {
            String k = argv[i];
            if (k.equals("--list") || k.equals("--dry-run")) {
                flags.add(k);
            } else if (k.equals("--model")) {
                String v = argv[++i];
                models.put(v.substring(0, v.indexOf('=')), Path.of(v.substring(v.indexOf('=') + 1)));
            } else if (k.startsWith("--") && i + 1 < argv.length) {
                a.put(k.substring(2), argv[++i]);
            } else {
                System.err.println("bad argument: " + k);
                System.exit(2);
            }
        }
        List<BenchmarkCell> cells = CellMatrix.enumerate(CellMatrix.Selection.parse(
            a.get("protocols"), a.get("paths"), a.get("transports"), a.get("kinds"), a.get("payloads"),
            a.get("outcomes"), a.get("concurrency")));
        if (flags.contains("--list")) {
            for (BenchmarkCell c : cells) {
                System.out.println(c.id() + (WorkloadProvider.forCell(c).isPresent() ? "" : "  (no fixture)"));
            }
            System.out.println(cells.size() + " cells");
            return;
        }
        HarnessMode mode = HarnessMode.fromId(a.getOrDefault("mode", "exploratory"));
        Path out = Path.of(a.getOrDefault("out", "target/harness/"
                                                 + LocalDateTime.now().format(DateTimeFormatter.ofPattern(
                                                     "yyyyMMdd-HHmmss"))));
        boolean release = mode == HarnessMode.RELEASE;
        SteadyStatePolicy sampling = new SteadyStatePolicy(
            ClientDriver.Args.seconds(a.getOrDefault("sample-interval-seconds",
                                                   HarnessOrchestrator.seconds(
                                                       SteadyStatePolicy.RELEASE.interval().toNanos()))),
            Integer.parseInt(a.getOrDefault("steady-window", Integer.toString(SteadyStatePolicy.RELEASE.window()))),
            ClientDriver.Args.seconds(a.getOrDefault("warmup-timeout-seconds", HarnessOrchestrator.seconds(
                SteadyStatePolicy.RELEASE.warmupTimeout().toNanos()))));
        WarmupPolicy.Kind warmupKind = WarmupPolicy.Kind.fromId(a.getOrDefault("warmup-policy",
                                                                               release ? "steady-state" : "fixed"));
        WarmupPolicy warmup = warmupKind == WarmupPolicy.Kind.FIXED
                              ? WarmupPolicy.fixed(ClientDriver.Args.seconds(a.getOrDefault("warmup-seconds", "10")),
                                                   sampling)
                              : WarmupPolicy.steadyState(sampling);
        int repetitions = Integer.parseInt(a.getOrDefault(
            "repetitions", Integer.toString(release ? RepetitionSchedule.MIN_RELEASE_REPETITIONS : 1)));
        boolean nullExperiment = Boolean.parseBoolean(a.getOrDefault("null-experiment", Boolean.toString(release)));
        // Performance References: a release collection fails closed unless all three are runnable; the ready ones
        // run as arms of every cell they apply to.
        List<PerformanceReferences.Reference> references = PerformanceReferences.resolveLocal();
        for (PerformanceReferences.Reference r : references) {
            System.out.println("reference " + r.id() + " (" + r.pin() + "): " + r.status().id()
                               + (r.reason() == null ? "" : " - " + r.reason()));
        }
        PerformanceReferences.requireReady(mode, references, cells);
        List<BenchmarkArm> arms = new ArrayList<>(List.of(BenchmarkArm.V3));
        arms.addAll(PerformanceReferences.readyArms(references));
        java.util.function.BiPredicate<BenchmarkArm, BenchmarkCell> applies = (arm, cell) ->
            references.stream().filter(r -> r.arm().filter(arm::equals).isPresent())
                      .allMatch(r -> r.appliesTo(cell));
        if (nullExperiment) {
            arms.add(BenchmarkArm.V3.nullTwin());
        }
        RepetitionSchedule schedule = RepetitionSchedule.interleaved(arms, repetitions, applies);
        HarnessConfig config = new HarnessConfig(
            mode, a.getOrDefault("java", HarnessConfig.currentJava()),
            a.containsKey("jvm-flags") ? Arrays.asList(a.get("jvm-flags").trim().split("\\s+"))
                                       : HarnessConfig.DEFAULT_JVM_FLAGS,
            System.getProperty("java.class.path"),
            warmup,
            ClientDriver.Args.seconds(a.getOrDefault("measure-seconds", "30")),
            ClientDriver.Args.seconds(a.getOrDefault("timeout-seconds", "600")), out,
            CpuPinning.parseCpuList(a.get("client-cpus")), CpuPinning.parseCpuList(a.get("service-cpus")),
            a.getOrDefault("background-policy", "unmanaged"), models);
        HarnessOrchestrator orchestrator = new HarnessOrchestrator(config, SystemProbe.live());
        if (flags.contains("--dry-run")) {
            List<RepetitionSchedule.ScheduledRun> runs = schedule.schedule(cells);
            HarnessOrchestrator.Preflight pre = orchestrator.preflight(cells, runs);
            System.out.println("pinning client: " + pre.pinning().client());
            System.out.println("pinning mock-service: " + pre.pinning().mockService());
            pre.host().entries().forEach((k, v) -> System.out.println("host " + k + ": " + v));
            System.out.println("cells without fixture: " + pre.missingFixtures().size() + " of " + cells.size());
            System.out.println("scheduled runs: " + runs.size() + ", schedule problems: " + pre.scheduleProblems());
            for (RepetitionSchedule.ScheduledRun run : runs) {
                if (WorkloadProvider.forCell(run.cell()).isPresent()) {
                    System.out.println(String.join(" ", orchestrator.mockServiceSpec(run.cell(), pre.pinning())
                                                                    .command()));
                    System.out.println(String.join(" ", orchestrator.clientSpec(
                        run, java.net.URI.create("http://127.0.0.1:PORT"), out.resolve("result.json"),
                        pre.pinning()).command()));
                }
            }
            return;
        }
        PerformanceReportSink.Chain sinks = PerformanceReportSink.chain(
            out.resolve("validity.json"), out.resolve("report.json"), references,
            ResultSink.jsonLines(out.resolve("results.jsonl")));
        java.nio.file.Files.createDirectories(out);
        orchestrator.run(cells, schedule, sinks.validity());
        CollectionValidity.CollectionVerdict verdict = sinks.validity().verdict();
        System.out.println("manifest: " + out.resolve("manifest.json"));
        System.out.println("results: " + out.resolve("results.jsonl"));
        System.out.println("report: " + out.resolve("report.json") + " (references ready "
                           + sinks.report().report().expectBooleanMember("references_ready").getValue() + ")");
        System.out.println("validity: " + out.resolve("validity.json") + " (collection "
                           + (verdict.valid() ? "valid" : "invalid") + ", " + verdict.invalidCells() + " of "
                           + verdict.cells().size() + " cells invalid, release evidence "
                           + verdict.releaseEvidence() + ")");
    }
}
