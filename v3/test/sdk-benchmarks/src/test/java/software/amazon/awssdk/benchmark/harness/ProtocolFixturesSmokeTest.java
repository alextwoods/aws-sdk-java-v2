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
 * End to end over the forked harness, one tiny cell per protocol other than awsJson1.0 (which
 * {@link ForkedHarnessSmokeTest} covers) on the generated sync path and on the Dynamic Client async path: each must
 * complete with zero failed operations, zero unmatched requests (so the client spoke the cell's protocol and
 * operation), and exactly the fixture's encoded bytes per operation on the wire (so the payload class is of the bytes
 * actually sent and received).
 */
class ProtocolFixturesSmokeTest {
    private static final List<String> PROTOCOLS = List.of("awsJson1_1", "restJson1", "restXml", "awsQuery", "ec2Query",
                                                          "rpcv2Cbor");

    @Test
    void oneSmallCellPerProtocolCompletesWithExactWireBytes(@TempDir Path out) throws Exception {
        List<BenchmarkCell> cells = new ArrayList<>();
        for (String protocol : PROTOCOLS) {
            cells.add(BenchmarkCell.parse(protocol + "/generated-sync/apache5-sync/finite/small/success/c1"));
            cells.add(BenchmarkCell.parse(protocol + "/dynamic-async/netty-async/finite/small/success/c1"));
        }
        HarnessConfig config = new HarnessConfig(
            HarnessMode.EXPLORATORY, HarnessConfig.currentJava(),
            List.of("-Xms128m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1"),
            System.getProperty("java.class.path"), Duration.ofMillis(100), Duration.ofMillis(300),
            Duration.ofSeconds(90), out, List.of(), List.of(), "unmanaged", Map.of());
        List<ObjectNode> records = new ArrayList<>();
        new HarnessOrchestrator(config, SystemProbe.live()).run(cells, RepetitionSchedule.once(), records::add);
        assertEquals(cells.size(), records.size());
        for (int i = 0; i < cells.size(); i++) {
            ObjectNode run = records.get(i);
            String json = Node.prettyPrintJson(run);
            assertEquals("completed", run.expectStringMember("status").getValue(), json);
            assertEquals(cells.get(i).id(), run.expectObjectMember("cell").expectStringMember("id").getValue());
            ObjectNode result = run.expectObjectMember("result");
            ObjectNode m = result.expectObjectMember("measurement");
            long ops = m.expectNumberMember("operations").getValue().longValue();
            assertTrue(ops > 0, json);
            assertEquals(0, m.expectNumberMember("failed_operations").getValue().longValue(), json);
            ObjectNode served = result.expectObjectMember("service");
            assertEquals(ops, served.expectNumberMember("requests").getValue().longValue(), json);
            assertEquals(0, served.expectNumberMember("unmatched").getValue().longValue(), json);
            long perOp = (served.expectNumberMember("bytes_in").getValue().longValue()
                          + served.expectNumberMember("bytes_out").getValue().longValue()) / ops;
            assertEquals(result.expectNumberMember("expected_encoded_bytes_per_op").getValue().longValue(), perOp,
                         cells.get(i).id());
            assertTrue(PayloadClass.SMALL.contains(perOp), json);
        }
    }

    /**
     * One streaming blob cell (S3 upload then download: two requests per operation) and one Event Stream cell per
     * Event Stream protocol (Lambda {@code InvokeWithResponseStream}, S3 {@code SelectObjectContent} on the Dynamic
     * Client's sync event stream reader, Kinesis {@code SubscribeToShard}: one request per operation) end to end. The
     * client's pre-timing verification compared every streamed byte and event; the mock accepted every upload and
     * Event Stream request only with the exact body (else {@code unmatched}); the wire bytes per operation equal the
     * fixture's.
     */
    @Test
    void streamAndEventStreamCellsCompleteWithExactWireBytes(@TempDir Path out) throws Exception {
        Map<BenchmarkCell, Integer> requestsPerOp = new java.util.LinkedHashMap<>();
        requestsPerOp.put(BenchmarkCell.parse("restXml/generated-sync/apache5-sync/stream/-/success/c1"), 2);
        requestsPerOp.put(BenchmarkCell.parse("restJson1/generated-async/netty-async/eventstream/-/success/c1"), 1);
        requestsPerOp.put(BenchmarkCell.parse("restXml/dynamic-sync/apache5-sync/eventstream/-/success/c1"), 1);
        requestsPerOp.put(BenchmarkCell.parse("awsJson1_1/generated-async/netty-async/eventstream/-/success/c1"), 1);
        List<BenchmarkCell> cells = List.copyOf(requestsPerOp.keySet());
        HarnessConfig config = new HarnessConfig(
            HarnessMode.EXPLORATORY, HarnessConfig.currentJava(),
            List.of("-Xms128m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1"),
            System.getProperty("java.class.path"), Duration.ofMillis(100), Duration.ofMillis(300),
            Duration.ofSeconds(90), out, List.of(), List.of(), "unmanaged", Map.of());
        List<ObjectNode> records = new ArrayList<>();
        new HarnessOrchestrator(config, SystemProbe.live()).run(cells, RepetitionSchedule.once(), records::add);
        assertEquals(cells.size(), records.size());
        for (int i = 0; i < cells.size(); i++) {
            ObjectNode run = records.get(i);
            String json = Node.prettyPrintJson(run);
            assertEquals("completed", run.expectStringMember("status").getValue(), json);
            ObjectNode result = run.expectObjectMember("result");
            ObjectNode m = result.expectObjectMember("measurement");
            long ops = m.expectNumberMember("operations").getValue().longValue();
            assertTrue(ops > 0, json);
            assertEquals(0, m.expectNumberMember("failed_operations").getValue().longValue(), json);
            ObjectNode served = result.expectObjectMember("service");
            assertEquals(ops * requestsPerOp.get(cells.get(i)),
                         served.expectNumberMember("requests").getValue().longValue(), json);
            assertEquals(0, served.expectNumberMember("unmatched").getValue().longValue(), json);
            long perOp = (served.expectNumberMember("bytes_in").getValue().longValue()
                          + served.expectNumberMember("bytes_out").getValue().longValue()) / ops;
            assertEquals(result.expectNumberMember("expected_encoded_bytes_per_op").getValue().longValue(), perOp,
                         cells.get(i).id());
        }
    }
}
