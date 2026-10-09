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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Runs a plan of benchmark cells as separate forked processes (Req 20.5): per scheduled run, one fresh mock-service
 * JVM for the cell, then one fresh client JVM against it, each under its own CPU pinning when the host supports it,
 * both with the identical JVM flag list, JDK and classpath.
 *
 * <p>Before anything is launched the orchestrator captures the host metadata, plans the pinning and checks coverage;
 * in {@link HarnessMode#RELEASE} an unpinnable host or a selected cell without a workload fixture fails the whole
 * run closed ({@link HarnessPolicyException}). In exploratory mode those are recorded with their reason, cells without
 * a fixture get a {@code status=no-fixture} record, and every record says {@code release_evidence=false}.
 *
 * <p>Output directory: {@code manifest.json} (host, config, pinning, plan, coverage), {@code results.jsonl} (one
 * record per scheduled run), and per-run client result files and process logs. 19.2-19.4 plug in through
 * {@link RepetitionSchedule} and {@link ResultSink}.
 */
public final class HarnessOrchestrator {

    public static final String MANIFEST_SCHEMA = "v3-benchmark-manifest/1";
    public static final String RECORD_SCHEMA = "v3-benchmark-run/1";

    private final HarnessConfig config;
    private final SystemProbe probe;

    public HarnessOrchestrator(HarnessConfig config, SystemProbe probe) {
        this.config = config;
        this.probe = probe;
    }

    /** The plan's preflight: metadata, pinning, coverage. Throws {@link HarnessPolicyException} in release mode. */
    public Preflight preflight(List<BenchmarkCell> cells) {
        CpuPinning.Plan pinning = CpuPinning.plan(probe, config.mode(), config.clientCpus(), config.serviceCpus(),
                                                  Runtime.getRuntime().availableProcessors());
        List<BenchmarkCell> missing = cells.stream().filter(c -> WorkloadProvider.forCell(c).isEmpty()).toList();
        if (config.mode() == HarnessMode.RELEASE && !missing.isEmpty()) {
            throw new HarnessPolicyException("release-gate collection selects " + missing.size()
                                             + " cell(s) without a workload fixture, e.g. " + missing.get(0));
        }
        HostMetadata host = HostMetadata.capture(probe, config.javaExecutable(), config.jvmFlags(),
                                                 config.backgroundPolicy());
        return new Preflight(host, pinning, missing);
    }

    /** Metadata, pinning plan and uncovered cells of a plan. */
    public record Preflight(HostMetadata host, CpuPinning.Plan pinning, List<BenchmarkCell> missingFixtures) {
    }

    /** The mock-service launch of {@code cell}. */
    public JvmLaunchSpec mockServiceSpec(BenchmarkCell cell, CpuPinning.Plan pinning) {
        int threads = Math.max(16, cell.concurrency() * 2);
        return new JvmLaunchSpec("mock-service", pinning.mockServicePrefix(), config.javaExecutable(),
                                 config.jvmFlags(), config.classpath(), MockServiceMain.class.getName(),
                                 List.of("--cell", cell.id(), "--port", "0", "--threads", Integer.toString(threads)));
    }

    /** The client launch of one scheduled run against {@code endpoint}. */
    public JvmLaunchSpec clientSpec(RepetitionSchedule.ScheduledRun run, URI endpoint, Path resultFile,
                                    CpuPinning.Plan pinning) {
        List<String> args = new ArrayList<>(List.of(
            "--cell", run.cell().id(), "--endpoint", endpoint.toString(),
            "--warmup-seconds", seconds(config.warmup().toNanos()),
            "--measure-seconds", seconds(config.measure().toNanos()),
            "--result-file", resultFile.toString(),
            "--repetition", Integer.toString(run.repetition()),
            "--label", run.label()));
        for (Map.Entry<String, Path> model : config.models().entrySet()) {
            args.add("--model");
            args.add(model.getKey() + "=" + model.getValue());
        }
        return new JvmLaunchSpec("client", pinning.clientPrefix(), config.javaExecutable(), config.jvmFlags(),
                                 config.classpath(), ClientMain.class.getName(), args);
    }

    private static String seconds(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1e9);
    }

    /** Runs {@code cells} under {@code schedule}; returns the manifest. */
    public ObjectNode run(List<BenchmarkCell> cells, RepetitionSchedule schedule, ResultSink sink)
            throws IOException, InterruptedException {
        Preflight pre = preflight(cells);
        Path out = config.outputDirectory();
        Files.createDirectories(out);
        String runId = Instant.now().toString();
        List<RepetitionSchedule.ScheduledRun> runs = schedule.schedule(cells);
        ArrayNode.Builder outcomes = ArrayNode.builder();
        int index = 0;
        for (RepetitionSchedule.ScheduledRun run : runs) {
            index++;
            ObjectNode record = runOne(runId, index, run, pre);
            sink.accept(record);
            outcomes.withValue(ObjectNode.builder().withMember("label", run.label())
                                         .withMember("status", record.expectStringMember("status").getValue())
                                         .build());
        }
        ObjectNode manifest = manifest(runId, cells, runs, pre, outcomes.build());
        Files.writeString(out.resolve("manifest.json"), Node.prettyPrintJson(manifest) + "\n",
                          StandardCharsets.UTF_8);
        sink.finish(manifest);
        return manifest;
    }

    private ObjectNode runOne(String runId, int index, RepetitionSchedule.ScheduledRun run, Preflight pre)
            throws IOException, InterruptedException {
        BenchmarkCell cell = run.cell();
        ObjectNode.Builder record = ObjectNode.builder()
            .withMember("schema", RECORD_SCHEMA)
            .withMember("run_id", runId)
            .withMember("index", index)
            .withMember("label", run.label())
            .withMember("repetition", run.repetition())
            .withMember("position", run.position())
            .withMember("mode", config.mode().id())
            .withMember("release_evidence", releaseEvidence(pre))
            .withMember("cell", cellNode(cell));
        Optional<WorkloadProvider> provider = WorkloadProvider.forCell(cell);
        if (provider.isEmpty()) {
            return record.withMember("status", "no-fixture")
                         .withMember("reason", "no WorkloadProvider covers " + cell.protocol().id() + " "
                                               + cell.kind().id() + " cells yet").build();
        }
        String stem = String.format(Locale.ROOT, "%04d", index);
        Path resultFile = config.outputDirectory().resolve("runs").resolve(stem + "-client.json");
        Path clientLog = config.outputDirectory().resolve("runs").resolve(stem + "-client.log");
        Path serviceLog = config.outputDirectory().resolve("runs").resolve(stem + "-mock-service.log");
        JvmLaunchSpec serviceSpec = mockServiceSpec(cell, pre.pinning());
        try (MockServiceProcess service = MockServiceProcess.start(serviceSpec, serviceLog,
                                                                   config.processTimeout())) {
            record.withMember("mock_service", processNode(serviceSpec, service.pid(), pre.pinning().mockService())
                .toBuilder().withMember("ready", Node.objectNode(toNodes(service.ready())))
                .withMember("log", serviceLog.toString()).build());
            JvmLaunchSpec clientSpec = clientSpec(run, service.endpoint(), resultFile, pre.pinning());
            Files.createDirectories(clientLog.getParent());
            Process client = new ProcessBuilder(clientSpec.command()).redirectErrorStream(true)
                                                                     .redirectOutput(clientLog.toFile()).start();
            client.getOutputStream().close();
            ObjectNode clientNode = processNode(clientSpec, client.pid(), pre.pinning().client());
            boolean finished = client.waitFor(config.processTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                client.destroyForcibly();
                client.waitFor(10, TimeUnit.SECONDS);
            }
            record.withMember("client", clientNode.toBuilder().withMember("log", clientLog.toString())
                                                  .withMember("exit_code", finished ? client.exitValue() : -1)
                                                  .build());
            if (!finished) {
                return record.withMember("status", "timeout").build();
            }
            if (!Files.exists(resultFile)) {
                return record.withMember("status", "no-result").build();
            }
            ObjectNode result = Node.parse(Files.readString(resultFile, StandardCharsets.UTF_8)).expectObjectNode();
            record.withMember("result_file", resultFile.toString()).withMember("result", result);
            String status = result.getMember("error").isPresent() ? "client-error"
                            : !service.isAlive() ? "mock-service-died" : "completed";
            return record.withMember("status", status).build();
        } catch (java.util.concurrent.TimeoutException e) {
            return record.withMember("status", "mock-service-not-ready").withMember("reason", e.getMessage()).build();
        }
    }

    private boolean releaseEvidence(Preflight pre) {
        return config.mode() == HarnessMode.RELEASE && pre.pinning().client().applied()
               && pre.missingFixtures().isEmpty();
    }

    /** One forked process's launch: pid, argv, requested and kernel-effective pinning. */
    private ObjectNode processNode(JvmLaunchSpec spec, long pid, CpuPinning.Assignment pinning) {
        ObjectNode.Builder pin = ObjectNode.builder()
            .withMember("applied", pinning.applied())
            .withMember("mechanism", pinning.mechanism().name().toLowerCase(Locale.ROOT))
            .withMember("cpus", CpuPinning.formatCpuList(pinning.cpus()));
        if (!pinning.applied()) {
            pin.withMember("reason", pinning.reason());
        }
        Optional<String> effective = CpuPinning.effectiveCpus(probe, pid);
        pin.withMember("effective_cpus", effective.orElse(HostMetadata.UNAVAILABLE + "OS does not expose affinity"));
        if (pinning.applied() && effective.isPresent()
            && !CpuPinning.parseCpuList(effective.get()).equals(pinning.cpus())) {
            if (config.mode() == HarnessMode.RELEASE) {
                throw new HarnessPolicyException(spec.role() + " pid " + pid + " runs on CPUs " + effective.get()
                                                 + ", not the pinned " + CpuPinning.formatCpuList(pinning.cpus()));
            }
            pin.withMember("violation", "effective CPUs differ from the requested pinning");
        }
        return ObjectNode.builder()
                         .withMember("role", spec.role())
                         .withMember("pid", pid)
                         .withMember("command", ArrayNode.fromStrings(spec.command()))
                         .withMember("pinning", pin.build())
                         .build();
    }

    static ObjectNode cellNode(BenchmarkCell cell) {
        return ObjectNode.builder()
                         .withMember("id", cell.id())
                         .withMember("protocol", cell.protocol().id())
                         .withMember("path", cell.path().id())
                         .withMember("transport", cell.transport().id())
                         .withMember("transport_class", cell.transport().implementation().getName())
                         .withMember("kind", cell.kind().id())
                         .withMember("payload", cell.payload() == null ? "-" : cell.payload().id())
                         .withMember("outcome", cell.outcome().id())
                         .withMember("concurrency", cell.concurrency())
                         .build();
    }

    private ObjectNode manifest(String runId, List<BenchmarkCell> cells, List<RepetitionSchedule.ScheduledRun> runs,
                                Preflight pre, ArrayNode outcomes) {
        ObjectNode.Builder payloadClasses = ObjectNode.builder();
        for (PayloadClass c : PayloadClass.values()) {
            payloadClasses.withMember(c.id(), ObjectNode.builder().withMember("min_inclusive", c.minInclusive())
                                                        .withMember("max_exclusive", c.maxExclusive())
                                                        .withMember("target", c.targetBytes()).build());
        }
        return ObjectNode.builder()
            .withMember("schema", MANIFEST_SCHEMA)
            .withMember("run_id", runId)
            .withMember("mode", config.mode().id())
            .withMember("release_evidence", releaseEvidence(pre))
            .withMember("host", Node.objectNode(toNodes(pre.host().entries())))
            .withMember("config", ObjectNode.builder()
                .withMember("java", config.javaExecutable())
                .withMember("jvm_flags", ArrayNode.fromStrings(config.jvmFlags()))
                .withMember("classpath_entries", config.classpath().split(java.io.File.pathSeparator).length)
                .withMember("warmup_s", config.warmup().toNanos() / 1e9)
                .withMember("measure_s", config.measure().toNanos() / 1e9)
                .withMember("background_policy", config.backgroundPolicy())
                .withMember("payload_classes", payloadClasses.build())
                .build())
            .withMember("pinning", ObjectNode.builder()
                .withMember("client", assignmentNode(pre.pinning().client()))
                .withMember("mock_service", assignmentNode(pre.pinning().mockService()))
                .build())
            .withMember("cells", ArrayNode.fromStrings(cells.stream().map(BenchmarkCell::id).toList()))
            .withMember("missing_fixtures", ArrayNode.fromStrings(
                pre.missingFixtures().stream().map(BenchmarkCell::id).toList()))
            .withMember("scheduled_runs", runs.size())
            .withMember("runs", outcomes)
            .build();
    }

    private static ObjectNode assignmentNode(CpuPinning.Assignment a) {
        ObjectNode.Builder b = ObjectNode.builder()
                                         .withMember("applied", a.applied())
                                         .withMember("mechanism", a.mechanism().name().toLowerCase(Locale.ROOT))
                                         .withMember("cpus", CpuPinning.formatCpuList(a.cpus()));
        return a.applied() ? b.build() : b.withMember("reason", a.reason()).build();
    }

    private static Map<software.amazon.smithy.model.node.StringNode, Node> toNodes(Map<String, String> m) {
        Map<software.amazon.smithy.model.node.StringNode, Node> nodes = new java.util.LinkedHashMap<>();
        m.forEach((k, v) -> nodes.put(Node.from(k), Node.from(v)));
        return nodes;
    }
}
