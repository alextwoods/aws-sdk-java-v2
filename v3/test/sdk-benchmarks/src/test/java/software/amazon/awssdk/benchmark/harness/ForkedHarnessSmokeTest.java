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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * End to end: forks one mock-service JVM and one client JVM for a single small cell, measures for half a second, and
 * checks the run record, the client's result record and the manifest. A second, uncovered cell checks that it is
 * recorded rather than silently dropped.
 */
class ForkedHarnessSmokeTest {
    private static final BenchmarkCell CELL =
        BenchmarkCell.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1");
    /**
     * A well-formed cell no provider serves. Every matrix cell has a fixture ({@link CellMatrixTest}), so this is an
     * awsJson1.0 Event Stream cell, outside the matrix because no fleet model binds an event stream on awsJson1.0.
     */
    static final BenchmarkCell UNCOVERED =
        BenchmarkCell.parse("awsJson1_0/generated-async/netty-async/eventstream/-/success/c1");

    @Test
    void forksClientAndMockServiceAndProducesAResultRecord(@TempDir Path out) throws Exception {
        HarnessConfig config = new HarnessConfig(
            HarnessMode.EXPLORATORY, HarnessConfig.currentJava(),
            List.of("-Xms128m", "-Xmx128m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1"),
            System.getProperty("java.class.path"), Duration.ofMillis(200), Duration.ofMillis(500),
            Duration.ofSeconds(60), out, List.of(), List.of(), "unmanaged", Map.of());
        List<ObjectNode> records = new ArrayList<>();
        ObjectNode manifest = new HarnessOrchestrator(config, SystemProbe.live())
            .run(List.of(CELL, UNCOVERED), RepetitionSchedule.once(), records::add);

        assertEquals(2, records.size());
        ObjectNode run = records.get(0);
        assertEquals("completed", run.expectStringMember("status").getValue(), Node.prettyPrintJson(run));
        assertEquals(CELL.id(), run.expectObjectMember("cell").expectStringMember("id").getValue());
        assertFalse(run.expectBooleanMember("release_evidence").getValue());

        ObjectNode client = run.expectObjectMember("client");
        ObjectNode service = run.expectObjectMember("mock_service");
        long clientPid = client.expectNumberMember("pid").getValue().longValue();
        long servicePid = service.expectNumberMember("pid").getValue().longValue();
        assertNotEquals(clientPid, servicePid);
        assertNotEquals(ProcessHandle.current().pid(), clientPid);
        assertEquals(0, client.expectNumberMember("exit_code").getValue().intValue());
        assertEquals(Long.toString(servicePid), service.expectObjectMember("ready").expectStringMember("pid").getValue());
        for (ObjectNode process : List.of(client, service)) {
            ObjectNode pinning = process.expectObjectMember("pinning");
            if (!pinning.expectBooleanMember("applied").getValue()) {
                assertFalse(pinning.expectStringMember("reason").getValue().isBlank());
            }
        }

        ObjectNode result = run.expectObjectMember("result");
        assertEquals(ClientMain.SCHEMA, result.expectStringMember("schema").getValue());
        assertEquals(clientPid, result.expectObjectMember("process").expectNumberMember("pid").getValue().longValue());
        assertTrue(result.expectObjectMember("process").expectStringMember("jvm_input_arguments").getValue()
                         .contains("-Xmx128m"));
        assertEquals("software.amazon.awssdk.http.apache5.Apache5HttpClient",
                     result.expectStringMember("transport_class").getValue());
        ObjectNode m = result.expectObjectMember("measurement");
        long ops = m.expectNumberMember("operations").getValue().longValue();
        assertTrue(ops > 0, Node.prettyPrintJson(result));
        assertEquals(0, m.expectNumberMember("failed_operations").getValue().longValue());
        assertTrue(m.expectNumberMember("process_cpu_ns").getValue().longValue() > 0);
        assertTrue(m.expectNumberMember("p99_ns").getValue().longValue()
                   >= m.expectNumberMember("p50_ns").getValue().longValue());
        ObjectNode served = result.expectObjectMember("service");
        assertEquals(ops, served.expectNumberMember("requests").getValue().longValue());
        assertEquals(0, served.expectNumberMember("unmatched").getValue().longValue());
        long perOp = (served.expectNumberMember("bytes_in").getValue().longValue()
                      + served.expectNumberMember("bytes_out").getValue().longValue()) / ops;
        assertEquals(result.expectNumberMember("expected_encoded_bytes_per_op").getValue().longValue(), perOp);
        assertTrue(PayloadClass.SMALL.contains(perOp));
        assertTrue(Files.exists(Path.of(run.expectStringMember("result_file").getValue())));

        ObjectNode none = records.get(1);
        assertEquals("no-fixture", none.expectStringMember("status").getValue());
        assertFalse(none.getMember("client").isPresent());

        ObjectNode onDisk = Node.parse(Files.readString(out.resolve("manifest.json"), StandardCharsets.UTF_8))
                                .expectObjectNode();
        assertEquals(manifest, onDisk);
        ObjectNode host = manifest.expectObjectMember("host");
        for (String key : HostMetadata.REQUIRED_KEYS) {
            assertTrue(host.getMember(key).isPresent(), key);
        }
        assertEquals(List.of(UNCOVERED.id()), manifest.expectArrayMember("missing_fixtures")
                                                      .getElementsAs(n -> n.expectStringNode().getValue()));
    }
}
