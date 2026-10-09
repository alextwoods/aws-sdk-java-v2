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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * The per-repetition validity rules (Req 20.7, 20.9, 20.26, 20.27), evaluated on one orchestrator run record. Every
 * rule is a pure function of the record and the plan's {@link Expectations}, so results can be re-validated offline
 * from {@code manifest.json} and {@code results.jsonl}. A repetition is valid iff it has no violation.
 *
 * <p>Operation rules: {@link #RUN_STATUS} (the run completed and wrote a result), {@link #FAILED_OPERATIONS} (no failed
 * operation in warmup or measurement), {@link #OPERATION_COUNT} (the mock served exactly
 * {@code operations x requests-per-operation} matching requests, plus the injected retries of a retry cell),
 * {@link #UNEXPECTED_OUTCOME} (the mock's outcome counters match the cell's outcome), {@link #UNMATCHED_REQUESTS} (no
 * request outside the fixture), {@link #WIRE_BYTES} (success cells moved exactly the fixture's bytes per operation),
 * {@link #TRANSPORT} (the built transport is the cell's).
 *
 * <p>Sample rules: {@link #INCOMPLETE_SAMPLE} (operations, latency and allocation present; the measured wall time and
 * full sampled intervals cover the configured window; at least 30 s in release mode), {@link #STEADY_STATE_GATE} (the
 * warmup was the steady-state gate under the plan's policy and its closing window re-checks stable; a gate that did not
 * pass within the timeout invalidates the cell), {@link #MEASURED_STABILITY} (every rolling window of the measured
 * series is stable).
 *
 * <p>Host-policy rules: {@link #PINNING} (both processes pinned, kernel affinity as requested), {@link #BACKGROUND_LOAD}
 * (host CPU not attributable to the client or mock service stays within {@link #maxBackgroundCpus}),
 * {@link #POWER} (not on battery, no low-power mode), {@link #HOST_DRIFT} (host, OS, CPU, memory, JDK, JVM flags and
 * power settings equal the preflight snapshot; both processes ran the plan's exact JVM flags and the client's JDK).
 */
public final class ValidityRules {

    public static final String RUN_STATUS = "run-status";
    public static final String FAILED_OPERATIONS = "failed-operations";
    public static final String OPERATION_COUNT = "operation-count";
    public static final String UNEXPECTED_OUTCOME = "unexpected-outcome";
    public static final String UNMATCHED_REQUESTS = "unmatched-requests";
    public static final String WIRE_BYTES = "wire-bytes";
    public static final String TRANSPORT = "transport-mismatch";
    public static final String INCOMPLETE_SAMPLE = "incomplete-sample";
    public static final String STEADY_STATE_GATE = "steady-state-gate";
    public static final String MEASURED_STABILITY = "measurement-stability";
    public static final String PINNING = "pinning";
    public static final String BACKGROUND_LOAD = "background-load";
    public static final String POWER = "power";
    public static final String HOST_DRIFT = "host-drift";

    /** Background CPU allowance: the larger of this many CPUs ... */
    public static final double BACKGROUND_MIN_CPUS = 0.25;
    /** ... and this fraction of the host's logical CPUs, averaged over the measured window. */
    public static final double BACKGROUND_HOST_FRACTION = 0.05;

    /** Host metadata that must not change between the preflight and any run (dynamic load keys excluded). */
    public static final List<String> DRIFT_KEYS = HostMetadata.REQUIRED_KEYS.stream()
        .filter(k -> !List.of("background.load_average", "background.process_count", "background.top_cpu",
                              "power.state").contains(k))
        .toList();

    private ValidityRules() {
    }

    /**
     * What the plan promised, against which each record is checked.
     *
     * @param host the preflight host metadata
     */
    public record Expectations(HarnessMode mode, Duration measure, List<String> jvmFlags, SteadyStatePolicy sampling,
                               Map<String, String> host) {
        public Expectations {
            jvmFlags = List.copyOf(jvmFlags);
            host = Map.copyOf(host);
        }

        /** From the plan node the orchestrator gives {@link ResultSink#begin}. */
        public static Expectations fromPlan(ObjectNode plan) {
            ObjectNode config = plan.expectObjectMember("config");
            Map<String, String> host = new java.util.LinkedHashMap<>();
            plan.expectObjectMember("host").getStringMap()
                .forEach((k, v) -> host.put(k, v.expectStringNode().getValue()));
            return new Expectations(
                HarnessMode.fromId(plan.expectStringMember("mode").getValue()),
                Duration.ofNanos(config.expectNumberMember("measure_ns").getValue().longValue()),
                config.expectArrayMember("jvm_flags").getElementsAs(n -> n.expectStringNode().getValue()),
                SteadyStatePolicy.fromNode(config.expectObjectMember("warmup").expectObjectMember("sampling")),
                host);
        }
    }

    /** The per-repetition metrics a null experiment compares (Req 20.24); 19.3 owns the full metric set. */
    public record Metrics(double cpuPerOpNanos, double allocatedBytesPerOp, double p50Nanos, double p99Nanos,
                          double throughput) {

        public static final List<String> NAMES = List.of("cpu_per_op_ns", "alloc_per_op_bytes", "p50_ns", "p99_ns",
                                                         "throughput_ops_per_s");

        public double get(int i) {
            return switch (i) {
                case 0 -> cpuPerOpNanos;
                case 1 -> allocatedBytesPerOp;
                case 2 -> p50Nanos;
                case 3 -> p99Nanos;
                case 4 -> throughput;
                default -> throw new IndexOutOfBoundsException(i);
            };
        }
    }

    /** The null-experiment metrics of a completed record with a measurement, if any. */
    public static Optional<Metrics> metrics(ObjectNode run) {
        Optional<ObjectNode> m = run.getObjectMember("result").flatMap(r -> r.getObjectMember("measurement"));
        if (m.isEmpty()) {
            return Optional.empty();
        }
        long ops = num(m.get(), "operations");
        long wall = num(m.get(), "wall_ns");
        if (ops <= 0 || wall <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Metrics(num(m.get(), "process_cpu_ns") / (double) ops,
                                       num(m.get(), "allocated_bytes") / (double) ops, num(m.get(), "p50_ns"),
                                       num(m.get(), "p99_ns"), ops / (wall / 1e9)));
    }

    /** The background-CPU allowance of a host with {@code logicalCpus} CPUs. */
    public static double maxBackgroundCpus(int logicalCpus) {
        return Math.max(BACKGROUND_MIN_CPUS, BACKGROUND_HOST_FRACTION * logicalCpus);
    }

    /** Every violation of one run record. */
    public static List<Violation> repetition(ObjectNode run, Expectations exp) {
        List<Violation> v = new ArrayList<>();
        String status = run.getStringMember("status").map(StringNode::getValue).orElse("missing");
        if (!"completed".equals(status)) {
            String why = run.getStringMember("reason").map(StringNode::getValue)
                            .or(() -> run.getObjectMember("result").flatMap(r -> r.getStringMember("error"))
                                         .map(StringNode::getValue))
                            .map(s -> ": " + s).orElse("");
            v.add(Violation.repetition(RUN_STATUS, "run status " + status + why));
            return v;
        }
        ObjectNode cell = run.expectObjectMember("cell");
        ObjectNode result = run.expectObjectMember("result");
        Optional<ObjectNode> measurement = result.getObjectMember("measurement");
        Optional<ObjectNode> warmup = result.getObjectMember("warmup");
        if (measurement.isEmpty() || warmup.isEmpty() || result.getObjectMember("service").isEmpty()) {
            v.add(Violation.repetition(INCOMPLETE_SAMPLE, "result has no warmup, measurement or service counters"));
            return v;
        }
        operationRules(cell, result, measurement.get(), warmup.get(), v);
        sampleRules(result, measurement.get(), warmup.get(), exp, v);
        hostRules(run, result, measurement.get(), exp, v);
        return v;
    }

    private static void operationRules(ObjectNode cell, ObjectNode result, ObjectNode m, ObjectNode warmup,
                                       List<Violation> v) {
        long warmFailed = num(warmup, "failed_operations");
        long failed = num(m, "failed_operations");
        if (warmFailed != 0 || failed != 0) {
            String first = result.getStringMember("first_failure")
                                 .or(() -> m.getStringMember("first_failure"))
                                 .or(() -> result.getStringMember("first_warmup_failure"))
                                 .map(s -> "; first: " + s.getValue()).orElse("");
            v.add(Violation.repetition(FAILED_OPERATIONS, warmFailed + " failed in warmup, " + failed
                                                          + " failed in measurement" + first));
        }
        ObjectNode service = result.expectObjectMember("service");
        long ops = num(m, "operations");
        long perOp = result.getNumberMember("expected_requests_per_op").map(n -> n.getValue().longValue()).orElse(1L);
        long requests = num(service, "requests");
        long successes = num(service, "successes");
        long errors = num(service, "modeled_errors");
        long retries = num(service, "injected_retries");
        long unmatched = num(service, "unmatched");
        Outcome outcome = Outcome.fromId(cell.expectStringMember("outcome").getValue());
        long logical = ops * perOp;
        long expectedRequests = outcome == Outcome.RETRY ? logical + retries : logical;
        if (requests != expectedRequests) {
            v.add(Violation.repetition(OPERATION_COUNT, "mock served " + requests + " matching requests; " + ops
                                                        + " operations x " + perOp + " request(s)"
                                                        + (outcome == Outcome.RETRY ? " + " + retries + " retries" : "")
                                                        + " = " + expectedRequests));
        }
        String outcomeProblem = switch (outcome) {
            case SUCCESS -> successes != requests || errors != 0 || retries != 0
                            ? "success cell: successes=" + successes + " modeled_errors=" + errors
                              + " injected_retries=" + retries + " of " + requests : null;
            case MODELED_ERROR -> errors != requests || successes != 0 || retries != 0
                                  ? "error cell: modeled_errors=" + errors + " successes=" + successes
                                    + " injected_retries=" + retries + " of " + requests : null;
            case RETRY -> successes != logical || errors != 0 || retries < 0
                          ? "retry cell: successes=" + successes + " (expected " + logical + ") modeled_errors="
                            + errors : null;
        };
        if (outcomeProblem != null) {
            v.add(Violation.repetition(UNEXPECTED_OUTCOME, outcomeProblem));
        }
        if (unmatched != 0) {
            v.add(Violation.repetition(UNMATCHED_REQUESTS, unmatched + " request(s) did not match the fixture"));
        }
        if (outcome == Outcome.SUCCESS && ops > 0) {
            long expectedBytes = num(result, "expected_encoded_bytes_per_op") * ops;
            long bytes = num(service, "bytes_in") + num(service, "bytes_out");
            if (bytes != expectedBytes) {
                v.add(Violation.repetition(WIRE_BYTES, "mock moved " + bytes + " body bytes; fixture says "
                                                       + expectedBytes + " for " + ops + " operations"));
            }
        }
        String expectedTransport = cell.expectStringMember("transport_class").getValue();
        String transport = result.getStringMember("transport_class").map(StringNode::getValue).orElse("missing");
        if (!expectedTransport.equals(transport)) {
            v.add(Violation.repetition(TRANSPORT, "built " + transport + ", cell requires " + expectedTransport));
        }
    }

    private static void sampleRules(ObjectNode result, ObjectNode m, ObjectNode warmup, Expectations exp,
                                    List<Violation> v) {
        SteadyStatePolicy policy = exp.sampling();
        List<String> incomplete = new ArrayList<>();
        long ops = num(m, "operations");
        if (ops <= 0) {
            incomplete.add("no measured operations");
        }
        if (num(m, "wall_ns") < exp.measure().toNanos()) {
            incomplete.add("measured " + num(m, "wall_ns") / 1e9 + " s of the configured "
                           + exp.measure().toNanos() / 1e9 + " s");
        }
        if (exp.mode() == HarnessMode.RELEASE && exp.measure().compareTo(SteadyStatePolicy.MIN_RELEASE_MEASURE) < 0) {
            incomplete.add("release repetitions measure at least " + SteadyStatePolicy.MIN_RELEASE_MEASURE.toSeconds()
                           + " s");
        }
        if (num(m, "allocated_bytes") < 0) {
            incomplete.add("allocated bytes not measured");
        }
        if (num(m, "control_failures") > 0) {
            incomplete.add(num(m, "control_failures") + " mock /stats read(s) failed: "
                           + m.getStringMember("first_control_failure").map(StringNode::getValue).orElse("?"));
        }
        long p50 = num(m, "p50_ns");
        long p99 = num(m, "p99_ns");
        if (p50 < 0 || p99 < p50) {
            incomplete.add("latency quantiles missing (p50=" + p50 + ", p99=" + p99 + ")");
        }
        List<SteadyStateDetector.Sample> measured = samples(m);
        long expectedIntervals = exp.measure().toNanos() / policy.interval().toNanos();
        long intervals = Math.max(0, measured.size() - 1);
        if (intervals < expectedIntervals) {
            incomplete.add(intervals + " sampled intervals of the expected " + expectedIntervals);
        }
        if (!incomplete.isEmpty()) {
            v.add(Violation.repetition(INCOMPLETE_SAMPLE, String.join("; ", incomplete)));
        }

        Optional<SteadyStatePolicy> recorded = result.getObjectMember("sampling").map(SteadyStatePolicy::fromNode);
        if (!recorded.equals(Optional.of(policy))) {
            v.add(Violation.repetition(STEADY_STATE_GATE, "client sampled with " + recorded.orElse(null)
                                                          + ", the plan's policy is " + policy));
        }
        String kind = warmup.getStringMember("policy").map(StringNode::getValue).orElse("missing");
        if (!WarmupPolicy.Kind.STEADY_STATE.id().equals(kind)) {
            v.add(Violation.repetition(STEADY_STATE_GATE, kind + " warmup: no JIT steady-state gate"));
        } else if (!warmup.getBooleanMember("passed").map(b -> b.getValue()).orElse(false)) {
            v.add(Violation.cell(STEADY_STATE_GATE, "JIT steady-state gate did not pass within "
                                                    + policy.warmupTimeout().toNanos() / 1e9 + " s: "
                                                    + warmup.getObjectMember("closing_window")
                                                            .flatMap(w -> w.getStringMember("reason"))
                                                            .map(StringNode::getValue).orElse("no reason recorded")));
        } else {
            SteadyStateDetector.WindowCheck closing = SteadyStateDetector.closingWindow(
                SteadyStateDetector.intervals(samples(warmup)), policy);
            if (!closing.stable()) {
                v.add(Violation.repetition(STEADY_STATE_GATE, "recorded gate window does not re-check stable: "
                                                              + closing.reason()));
            }
        }
        SteadyStateDetector.WindowCheck stability =
            SteadyStateDetector.everyWindow(SteadyStateDetector.intervals(measured), policy);
        if (!stability.stable()) {
            v.add(Violation.repetition(MEASURED_STABILITY, "measured intervals " + stability.firstInterval() + ".."
                                                           + stability.lastInterval() + ": " + stability.reason()));
        }
    }

    private static void hostRules(ObjectNode run, ObjectNode result, ObjectNode m, Expectations exp,
                                  List<Violation> v) {
        for (String role : List.of("client", "mock_service")) {
            Optional<ObjectNode> pinning = run.getObjectMember(role).flatMap(p -> p.getObjectMember("pinning"));
            if (pinning.isEmpty()) {
                v.add(Violation.repetition(PINNING, role + " pinning not recorded"));
                continue;
            }
            ObjectNode p = pinning.get();
            if (!p.getBooleanMember("applied").map(b -> b.getValue()).orElse(false)) {
                v.add(Violation.repetition(PINNING, role + " not pinned: "
                                                    + p.getStringMember("reason").map(StringNode::getValue)
                                                       .orElse("no reason recorded")));
            } else if (p.getStringMember("violation").isPresent()) {
                v.add(Violation.repetition(PINNING, role + ": " + p.expectStringMember("violation").getValue()
                                                    + " (effective "
                                                    + p.getStringMember("effective_cpus").map(StringNode::getValue)
                                                       .orElse("?") + ")"));
            }
        }

        Map<String, String> host = new java.util.LinkedHashMap<>();
        run.getObjectMember("host").ifPresent(h -> h.getStringMap().forEach(
            (k, n) -> host.put(k, n.asStringNode().map(StringNode::getValue).orElse(Node.printJson(n)))));
        backgroundRule(result, m, host, v);

        String source = host.getOrDefault("power.source", "");
        String settings = host.getOrDefault("power.settings", "");
        if (source.toLowerCase(Locale.ROOT).contains("battery")) {
            v.add(Violation.repetition(POWER, "host ran on " + source));
        }
        if (settings.contains("lowpowermode=1")) {
            v.add(Violation.repetition(POWER, "low-power mode is on: " + settings));
        }

        List<String> drift = new ArrayList<>();
        if (host.isEmpty()) {
            drift.add("no per-run host snapshot");
        } else {
            for (String key : DRIFT_KEYS) {
                if (!Objects.equals(exp.host().get(key), host.get(key))) {
                    drift.add(key + ": preflight '" + exp.host().get(key) + "', run '" + host.get(key) + "'");
                }
            }
        }
        Optional<ObjectNode> process = result.getObjectMember("process");
        String jdk = exp.host().get("jdk.version");
        String clientJdk = process.flatMap(p -> p.getStringMember("jdk_version")).map(StringNode::getValue)
                                  .orElse(null);
        if (!HostMetadata.isUnavailable(jdk) && !Objects.equals(jdk, clientJdk)) {
            drift.add("client JDK " + clientJdk + ", preflight JDK " + jdk);
        }
        String flags = String.join(" ", exp.jvmFlags());
        String clientFlags = process.flatMap(p -> p.getStringMember("jvm_input_arguments")).map(StringNode::getValue)
                                    .orElse(null);
        if (!flags.equals(clientFlags)) {
            drift.add("client JVM input arguments '" + clientFlags + "', plan '" + flags + "'");
        }
        for (String role : List.of("client", "mock_service")) {
            List<String> command = run.getObjectMember(role).flatMap(p -> p.getArrayMember("command"))
                                      .map(a -> a.getElementsAs(n -> n.expectStringNode().getValue()))
                                      .orElse(List.of());
            if (java.util.Collections.indexOfSubList(command, exp.jvmFlags()) < 0) {
                drift.add(role + " command does not carry the plan's JVM flags");
            }
        }
        if (!drift.isEmpty()) {
            v.add(Violation.repetition(HOST_DRIFT, String.join("; ", drift)));
        }
    }

    /**
     * Background CPU over the measured window: whole-host busy CPU (sampled system load x logical CPUs x interval
     * wall time) minus the client's and mock service's own CPU over the same sampled span.
     */
    private static void backgroundRule(ObjectNode result, ObjectNode m, Map<String, String> host,
                                       List<Violation> v) {
        int logical;
        try {
            logical = Integer.parseInt(host.getOrDefault("cpu.logical", ""));
        } catch (NumberFormatException e) {
            v.add(Violation.repetition(BACKGROUND_LOAD, "host logical CPU count unknown"));
            return;
        }
        List<SteadyStateDetector.Interval> intervals = SteadyStateDetector.intervals(samples(m));
        if (intervals.isEmpty() || intervals.stream().anyMatch(i -> i.systemCpuLoad() < 0)) {
            v.add(Violation.repetition(BACKGROUND_LOAD, "host CPU load not measurable over the measured window"));
            return;
        }
        double sampledSeconds = 0;
        double busy = 0;
        for (SteadyStateDetector.Interval i : intervals) {
            double s = i.wallNanos() / 1e9;
            sampledSeconds += s;
            busy += i.systemCpuLoad() * logical * s;
        }
        long wall = num(m, "wall_ns");
        double ownShare = wall <= 0 ? 0 : Math.min(1.0, sampledSeconds / (wall / 1e9));
        long serviceCpu = result.getObjectMember("service").map(s -> num(s, "cpu_ns")).orElse(0L);
        double own = (num(m, "process_cpu_ns") + Math.max(0, serviceCpu)) / 1e9 * ownShare;
        double background = (busy - own) / sampledSeconds;
        double limit = maxBackgroundCpus(logical);
        if (background > limit) {
            v.add(Violation.repetition(BACKGROUND_LOAD, String.format(
                Locale.ROOT, "background used %.2f CPUs (limit %.2f of %d logical)", background, limit, logical)));
        }
    }

    static List<SteadyStateDetector.Sample> samples(ObjectNode node) {
        return node.getArrayMember("samples").map(SteadyStateDetector::parse).orElse(List.of());
    }

    static long num(ObjectNode node, String member) {
        return node.getNumberMember(member).map(n -> n.getValue().longValue()).orElse(-1L);
    }
}
