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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/** Each per-repetition rule on synthetic run records: a valid record has no violation; each fault trips its rule. */
class ValidityRulesTest {
    static final List<String> FLAGS = List.of("-Xms2g", "-Xmx2g", "-XX:+AlwaysPreTouch");
    static final BenchmarkCell SUCCESS = BenchmarkCell.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1");
    static final long OPS_PER_S = 10_000;
    static final int SECONDS = 30;
    static final Map<String, String> HOST = host();

    static Map<String, String> host() {
        Map<String, String> h = new LinkedHashMap<>();
        for (String k : HostMetadata.REQUIRED_KEYS) {
            h.put(k, "value-of-" + k);
        }
        h.put("cpu.logical", "8");
        h.put("power.source", "AC Power");
        h.put("power.settings", "lowpowermode=0 sleep=0");
        h.put("jdk.version", "21.0.5+11-LTS");
        h.put("jvm.flags", String.join(" ", FLAGS));
        return h;
    }

    static ValidityRules.Expectations expectations(HarnessMode mode) {
        return new ValidityRules.Expectations(mode, Duration.ofSeconds(SECONDS), FLAGS, SteadyStatePolicy.RELEASE,
                                              HOST);
    }

    /** Cumulative samples of a perfectly steady client: 1 CPU, 4 KB/op, host load 0.19 of 8 CPUs. */
    static ArrayNode samples(int intervals, long opsPerInterval, double load) {
        List<SteadyStateDetector.Sample> s = new ArrayList<>();
        s.add(new SteadyStateDetector.Sample(0, 0, 0, 0, 0, 500, -1));
        for (int k = 1; k <= intervals; k++) {
            s.add(new SteadyStateDetector.Sample(k * 1_000_000_000L, k * opsPerInterval, 0, k * 1_000_000_000L,
                                                 k * opsPerInterval * 4000, 500, load));
        }
        return SteadyStateDetector.toNode(s);
    }

    static ObjectNode pinning() {
        return ObjectNode.builder().withMember("applied", true).withMember("mechanism", "taskset")
                         .withMember("cpus", "4-7").withMember("effective_cpus", "4-7").build();
    }

    static ArrayNode command() {
        List<String> c = new ArrayList<>(List.of("/usr/bin/taskset", "-c", "4-7", "/jdk/bin/java"));
        c.addAll(FLAGS);
        c.addAll(List.of("-cp", "/a.jar", "Main"));
        return ArrayNode.fromStrings(c);
    }

    /** A record every rule accepts, for {@code cell} with {@code requestsPerOp} matching requests per operation. */
    static ObjectNode valid(BenchmarkCell cell, int requestsPerOp, long retries) {
        long ops = OPS_PER_S * SECONDS;
        long logical = ops * requestsPerOp;
        long successes = cell.outcome() == Outcome.MODELED_ERROR ? 0 : logical;
        long errors = cell.outcome() == Outcome.MODELED_ERROR ? logical : 0;
        Map<String, Node> hostNodes = new LinkedHashMap<>();
        HOST.forEach((k, v) -> hostNodes.put(k, Node.from(v)));
        ObjectNode measurement = ObjectNode.builder()
            .withMember("wall_ns", SECONDS * 1_000_000_000L + 100_000)
            .withMember("operations", ops)
            .withMember("failed_operations", 0)
            .withMember("process_cpu_ns", SECONDS * 1_000_000_000L)
            .withMember("allocated_bytes", ops * 4000)
            .withMember("gc_ms", 10)
            .withMember("p50_ns", 90_000)
            .withMember("p99_ns", 250_000)
            .withMember("loaded_classes", 4000)
            .withMember("configured_s", SECONDS)
            .withMember("samples", samples(SECONDS, OPS_PER_S, 0.19))
            .build();
        ObjectNode result = ObjectNode.builder()
            .withMember("schema", ClientMain.SCHEMA)
            .withMember("process", ObjectNode.builder().withMember("jdk_version", "21.0.5+11-LTS")
                                             .withMember("jvm_input_arguments", String.join(" ", FLAGS)).build())
            .withMember("transport_class", cell.transport().implementation().getName())
            .withMember("expected_encoded_bytes_per_op", 300)
            .withMember("expected_requests_per_op", requestsPerOp)
            .withMember("sampling", SteadyStatePolicy.RELEASE.toNode())
            .withMember("warmup", ObjectNode.builder().withMember("policy", "steady-state")
                                            .withMember("passed", true).withMember("reason", "stable")
                                            .withMember("operations", 70_000).withMember("failed_operations", 0)
                                            .withMember("samples", samples(7, OPS_PER_S, 0.19)).build())
            .withMember("measurement", measurement)
            .withMember("service", ObjectNode.builder()
                .withMember("requests", logical + retries).withMember("successes", successes)
                .withMember("modeled_errors", errors).withMember("injected_retries", retries)
                .withMember("unmatched", 0).withMember("bytes_in", ops * 100).withMember("bytes_out", ops * 200)
                .withMember("cpu_ns", SECONDS * 500_000_000L).build())
            .build();
        return ObjectNode.builder()
            .withMember("schema", HarnessOrchestrator.RECORD_SCHEMA)
            .withMember("index", 1)
            .withMember("repetition", 1)
            .withMember("status", "completed")
            .withMember("cell", HarnessOrchestrator.cellNode(cell))
            .withMember("arm", BenchmarkArm.V3.toNode().toBuilder().withMember("artifact_digest", "sha256:aa")
                                              .build())
            .withMember("host", Node.objectNode(toStringNodes(hostNodes)))
            .withMember("client", ObjectNode.builder().withMember("pinning", pinning())
                                            .withMember("command", command()).build())
            .withMember("mock_service", ObjectNode.builder().withMember("pinning", pinning())
                                                  .withMember("command", command()).build())
            .withMember("result", result)
            .build();
    }

    static ObjectNode valid() {
        return valid(SUCCESS, 1, 0);
    }

    /** {@code node} with the member at {@code /}-separated {@code path} replaced (or added). */
    static ObjectNode set(ObjectNode node, String path, Node value) {
        int dot = path.indexOf('/');
        if (dot < 0) {
            return node.withMember(path, value);
        }
        String head = path.substring(0, dot);
        return node.withMember(head, set(node.expectObjectMember(head), path.substring(dot + 1), value));
    }

    static ObjectNode set(ObjectNode node, String path, long value) {
        return set(node, path, Node.from(value));
    }

    static List<Violation> check(ObjectNode run) {
        return ValidityRules.repetition(run, expectations(HarnessMode.RELEASE));
    }

    static List<String> rules(List<Violation> v) {
        return v.stream().map(Violation::rule).distinct().toList();
    }

    @Test
    void validRecordsHaveNoViolation() {
        assertEquals(List.of(), check(valid()));
        BenchmarkCell retry = BenchmarkCell.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/retry/c8");
        BenchmarkCell error = BenchmarkCell.parse("awsJson1_0/generated-async/netty-async/finite/large/error/c32");
        BenchmarkCell stream = BenchmarkCell.parse("restXml/generated-sync/apache5-sync/stream/-/success/c1");
        assertEquals(List.of(), check(valid(retry, 1, 290_000)));
        assertEquals(List.of(), check(valid(error, 1, 0)));
        assertEquals(List.of(), check(valid(stream, 2, 0)));
        assertTrue(ValidityRules.metrics(valid()).isPresent());
        assertEquals(OPS_PER_S, ValidityRules.metrics(valid()).get().throughput(), 1.0);
    }

    @Test
    void operationRules() {
        assertOnly(set(valid(), "status", Node.from("timeout")), ValidityRules.RUN_STATUS);
        assertOnly(set(valid(), "result/measurement/failed_operations", 3), ValidityRules.FAILED_OPERATIONS);
        assertOnly(set(valid(), "result/warmup/failed_operations", 1), ValidityRules.FAILED_OPERATIONS);
        ObjectNode extra = set(set(valid(), "result/service/requests", OPS_PER_S * SECONDS + 1),
                               "result/service/successes", OPS_PER_S * SECONDS + 1);
        assertOnly(extra, ValidityRules.OPERATION_COUNT);
        assertOnly(set(valid(), "result/service/modeled_errors", 1), ValidityRules.UNEXPECTED_OUTCOME);
        assertOnly(set(valid(), "result/service/unmatched", 2), ValidityRules.UNMATCHED_REQUESTS);
        assertOnly(set(valid(), "result/service/bytes_out", OPS_PER_S * SECONDS * 200 + 1), ValidityRules.WIRE_BYTES);
        assertOnly(set(valid(), "result/transport_class", Node.from("java.net.HttpURLConnection")),
                   ValidityRules.TRANSPORT);
        // A stream cell whose mock saw one request per operation lost half of each operation.
        BenchmarkCell stream = BenchmarkCell.parse("restXml/generated-sync/apache5-sync/stream/-/success/c1");
        assertOnly(set(valid(stream, 1, 0), "result/expected_requests_per_op", 2), ValidityRules.OPERATION_COUNT);
        // A retry cell whose mock injected no retry is still exact: requests == operations.
        BenchmarkCell retry = BenchmarkCell.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/retry/c8");
        assertOnly(set(valid(retry, 1, 10), "result/service/requests", OPS_PER_S * SECONDS),
                   ValidityRules.OPERATION_COUNT);
    }

    @Test
    void sampleRules() {
        assertOnly(set(valid(), "result/measurement/allocated_bytes", -1), ValidityRules.INCOMPLETE_SAMPLE);
        assertOnly(set(valid(), "result/measurement/wall_ns", 29_000_000_000L), ValidityRules.INCOMPLETE_SAMPLE);
        assertOnly(set(valid(), "result/measurement/p50_ns", -1), ValidityRules.INCOMPLETE_SAMPLE);
        assertOnly(set(valid(), "result/measurement/samples", samples(20, OPS_PER_S, 0.19)),
                   ValidityRules.INCOMPLETE_SAMPLE);
        List<Violation> shortRelease = ValidityRules.repetition(valid(), new ValidityRules.Expectations(
            HarnessMode.RELEASE, Duration.ofSeconds(10), FLAGS, SteadyStatePolicy.RELEASE, HOST));
        assertEquals(List.of(ValidityRules.INCOMPLETE_SAMPLE), rules(shortRelease));
        assertEquals(List.of(), ValidityRules.repetition(valid(), new ValidityRules.Expectations(
            HarnessMode.EXPLORATORY, Duration.ofSeconds(10), FLAGS, SteadyStatePolicy.RELEASE, HOST)));

        List<Violation> fixed = check(set(valid(), "result/warmup/policy", Node.from("fixed-duration")));
        assertEquals(List.of(ValidityRules.STEADY_STATE_GATE), rules(fixed));
        assertEquals(Violation.Scope.REPETITION, fixed.get(0).scope());
        List<Violation> timedOut = check(set(valid(), "result/warmup/passed", Node.from(false)));
        assertEquals(List.of(ValidityRules.STEADY_STATE_GATE), rules(timedOut));
        assertEquals(Violation.Scope.CELL, timedOut.get(0).scope(), "Req 20.26: a gate timeout invalidates the cell");
        // The client claims the gate passed but its recorded window was not stable.
        assertOnly(set(valid(), "result/warmup/samples", samples(3, OPS_PER_S, 0.19)), ValidityRules.STEADY_STATE_GATE);
        assertOnly(set(valid(), "result/sampling", new SteadyStatePolicy(Duration.ofMillis(500), 5,
                                                                         Duration.ofSeconds(300)).toNode()),
                   ValidityRules.STEADY_STATE_GATE);

        // JIT compiled 100 ms (2% of the window's 5 CPU-seconds) in the middle of the measured window.
        List<SteadyStateDetector.Sample> s = new ArrayList<>(SteadyStateDetector.parse(
            valid().expectObjectMember("result").expectObjectMember("measurement").expectArrayMember("samples")));
        for (int k = 17; k < s.size(); k++) {
            SteadyStateDetector.Sample x = s.get(k);
            s.set(k, new SteadyStateDetector.Sample(x.tNanos(), x.operations(), x.failures(), x.cpuNanos(),
                                                    x.allocatedBytes(), x.compileMillis() + 100, x.systemCpuLoad()));
        }
        assertOnly(set(valid(), "result/measurement/samples", SteadyStateDetector.toNode(s)),
                   ValidityRules.MEASURED_STABILITY);
    }

    @Test
    void hostPolicyRules() {
        assertOnly(set(valid(), "client/pinning/applied", Node.from(false)), ValidityRules.PINNING);
        assertOnly(set(valid(), "mock_service/pinning/violation", Node.from("effective CPUs differ")),
                   ValidityRules.PINNING);
        // 0.9 x 8 CPUs busy while the client and mock used 1.5: about 5.7 CPUs of background.
        assertOnly(set(valid(), "result/measurement/samples", samples(SECONDS, OPS_PER_S, 0.9)),
                   ValidityRules.BACKGROUND_LOAD);
        assertOnly(set(valid(), "result/measurement/samples", samples(SECONDS, OPS_PER_S, -1)),
                   ValidityRules.BACKGROUND_LOAD);
        assertEquals(0.4, ValidityRules.maxBackgroundCpus(8), 1e-9);
        assertEquals(0.25, ValidityRules.maxBackgroundCpus(2), 1e-9);

        UnaryOperator<ObjectNode> battery = r -> set(r, "host/power.source", Node.from("Battery Power"));
        Map<String, String> batteryHost = new LinkedHashMap<>(HOST);
        batteryHost.put("power.source", "Battery Power");
        assertEquals(List.of(ValidityRules.POWER), rules(ValidityRules.repetition(battery.apply(valid()),
            new ValidityRules.Expectations(HarnessMode.RELEASE, Duration.ofSeconds(SECONDS), FLAGS,
                                           SteadyStatePolicy.RELEASE, batteryHost))));
        // The same battery switch mid-collection is also drift from the preflight.
        assertOnly(battery.apply(valid()), ValidityRules.POWER, ValidityRules.HOST_DRIFT);

        assertOnly(set(valid(), "host/os.version", Node.from("changed")), ValidityRules.HOST_DRIFT);
        assertOnly(set(valid(), "host/jdk.version", Node.from("22")), ValidityRules.HOST_DRIFT);
        assertOnly(set(valid(), "result/process/jdk_version", Node.from("22")), ValidityRules.HOST_DRIFT);
        assertOnly(set(valid(), "result/process/jvm_input_arguments", Node.from("-Xmx1g")), ValidityRules.HOST_DRIFT);
        assertOnly(set(valid(), "mock_service/command", ArrayNode.fromStrings(List.of("java", "-cp", "x", "Main"))),
                   ValidityRules.HOST_DRIFT);
        // Load keys move between runs and are not drift.
        assertEquals(List.of(), check(set(valid(), "host/background.load_average", Node.from("3.10"))));
        assertOnly(valid().withoutMember("host"), ValidityRules.BACKGROUND_LOAD, ValidityRules.HOST_DRIFT);
    }

    private static void assertOnly(ObjectNode run, String... expected) {
        List<Violation> v = check(run);
        assertEquals(List.of(expected), rules(v), v.toString());
    }

    private static Map<software.amazon.smithy.model.node.StringNode, Node> toStringNodes(Map<String, Node> m) {
        Map<software.amazon.smithy.model.node.StringNode, Node> out = new LinkedHashMap<>();
        m.forEach((k, v) -> out.put(Node.from(k), v));
        return out;
    }
}
