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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class CellMatrixTest {
    private static final List<BenchmarkCell> FULL = CellMatrix.full();

    @Test
    void fullMatrixHasTheIndependentlyCountedSize() {
        // Per protocol: 2 sync paths x 4 sync transports + 2 async paths x 3 async transports = 14 path/transport pairs.
        int pairs = 2 * 4 + 2 * 3;
        // Finite: 3 payload classes x (success, retry at 1/8/32 + error at 1); ec2Query has no error cells.
        int finite = 3 * (2 * 3 + 1);
        int ec2Finite = 3 * (2 * 3);
        int streaming = 2;   // restJson1, restXml
        // Event Stream: awsJson1_1, restJson1, restXml, on every pair but generated-sync's 4.
        int eventStream = 3;
        assertEquals(6 * pairs * finite + pairs * ec2Finite + streaming * pairs + eventStream * (pairs - 4),
                     FULL.size());
        assertEquals(2074, FULL.size());
    }

    @Test
    void idsAreUniqueAndRoundTrip() {
        Set<String> ids = new HashSet<>();
        for (BenchmarkCell cell : FULL) {
            assertTrue(ids.add(cell.id()), cell.id());
            assertEquals(cell, BenchmarkCell.parse(cell.id()));
        }
    }

    @Test
    void everyProtocolPathTransportAndOutcomeIsCovered() {
        assertEquals(EnumSet.allOf(BenchProtocol.class), FULL.stream().map(BenchmarkCell::protocol)
                                                             .collect(Collectors.toCollection(
                                                                 () -> EnumSet.noneOf(BenchProtocol.class))));
        for (BenchProtocol protocol : BenchProtocol.values()) {
            for (ClientPath path : ClientPath.values()) {
                Set<Transport> transports = FULL.stream().filter(c -> c.protocol() == protocol && c.path() == path)
                                                .map(BenchmarkCell::transport).collect(Collectors.toSet());
                Set<Transport> expected = EnumSet.allOf(Transport.class);
                expected.removeIf(t -> t.mode() != path.mode());
                assertEquals(expected, transports, protocol + " " + path);
            }
        }
        assertTrue(FULL.stream().allMatch(c -> c.transport().mode() == c.path().mode()));
        assertTrue(FULL.stream().anyMatch(c -> c.path().mode() == HttpTransportMode.ASYNC));
    }

    @Test
    void finiteSuccessAndRetryRunAtOneEightAndThirtyTwo() {
        for (Outcome outcome : List.of(Outcome.SUCCESS, Outcome.RETRY)) {
            Set<Integer> levels = FULL.stream().filter(c -> c.kind() == WorkloadKind.FINITE && c.outcome() == outcome)
                                      .map(BenchmarkCell::concurrency).collect(Collectors.toCollection(TreeSet::new));
            assertEquals(Set.of(1, 8, 32), levels, outcome.id());
        }
        assertTrue(FULL.stream().filter(c -> c.outcome() == Outcome.MODELED_ERROR || c.kind().isStreaming())
                       .allMatch(c -> c.concurrency() == 1));
        assertTrue(FULL.stream().filter(c -> c.kind() == WorkloadKind.FINITE)
                       .map(BenchmarkCell::payload).collect(Collectors.toSet())
                       .containsAll(EnumSet.allOf(PayloadClass.class)));
    }

    @Test
    void streamingCellsOnlyWhereTheProtocolSupportsThem() {
        Set<BenchProtocol> streaming = FULL.stream().filter(c -> c.kind() == WorkloadKind.STREAMING)
                                           .map(BenchmarkCell::protocol).collect(Collectors.toSet());
        Set<BenchProtocol> eventStream = FULL.stream().filter(c -> c.kind() == WorkloadKind.EVENT_STREAM)
                                             .map(BenchmarkCell::protocol).collect(Collectors.toSet());
        assertEquals(Set.of(BenchProtocol.REST_JSON_1, BenchProtocol.REST_XML), streaming);
        assertEquals(Set.of(BenchProtocol.AWS_JSON_1_1, BenchProtocol.REST_JSON_1, BenchProtocol.REST_XML),
                     eventStream);
        assertTrue(FULL.stream().noneMatch(c -> c.kind() == WorkloadKind.EVENT_STREAM
                                                && c.path() == ClientPath.GENERATED_SYNC));
        assertTrue(FULL.stream().filter(c -> c.kind().isStreaming())
                       .allMatch(c -> c.payload() == null && c.outcome() == Outcome.SUCCESS));
    }

    @Test
    void cellInvariantsRejectMalformedCells() {
        assertThrows(IllegalArgumentException.class, () -> BenchmarkCell.finite(
            BenchProtocol.AWS_JSON_1_0, ClientPath.GENERATED_SYNC, Transport.NETTY_ASYNC, PayloadClass.SMALL,
            Outcome.SUCCESS, 1));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkCell(
            BenchProtocol.AWS_QUERY, ClientPath.GENERATED_SYNC, Transport.APACHE5_SYNC, WorkloadKind.EVENT_STREAM,
            null, Outcome.SUCCESS, 1));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkCell(
            BenchProtocol.REST_XML, ClientPath.GENERATED_SYNC, Transport.APACHE5_SYNC, WorkloadKind.STREAMING,
            PayloadClass.SMALL, Outcome.SUCCESS, 1));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkCell(
            BenchProtocol.AWS_JSON_1_0, ClientPath.GENERATED_SYNC, Transport.APACHE5_SYNC, WorkloadKind.FINITE,
            null, Outcome.SUCCESS, 1));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkCell.finite(
            BenchProtocol.AWS_JSON_1_0, ClientPath.GENERATED_SYNC, Transport.APACHE5_SYNC, PayloadClass.SMALL,
            Outcome.SUCCESS, 0));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkCell.parse("awsJson1_0/generated-sync"));
    }

    @Test
    void selectionNarrowsWithoutAddingCells() {
        CellMatrix.Selection selection = CellMatrix.Selection.parse("awsJson1_0", "generated-async", "netty-async",
                                                                    null, "small,large", "success,error", "1");
        List<BenchmarkCell> cells = CellMatrix.enumerate(selection);
        assertEquals(List.of("awsJson1_0/generated-async/netty-async/finite/small/success/c1",
                             "awsJson1_0/generated-async/netty-async/finite/small/error/c1",
                             "awsJson1_0/generated-async/netty-async/finite/large/success/c1",
                             "awsJson1_0/generated-async/netty-async/finite/large/error/c1"),
                     cells.stream().map(BenchmarkCell::id).toList());
        assertTrue(FULL.containsAll(cells));
        // A payload filter excludes streaming cells, which have no payload class.
        assertTrue(CellMatrix.enumerate(CellMatrix.Selection.parse("restJson1", null, null, null, "small", null, null))
                             .stream().noneMatch(c -> c.kind().isStreaming()));
        assertEquals(FULL, CellMatrix.enumerate(CellMatrix.Selection.all()));
    }

    @Test
    void payloadClassesAreContiguousAndNonOverlapping() {
        PayloadClass[] classes = PayloadClass.values();
        assertEquals(0, classes[0].minInclusive());
        for (int i = 1; i < classes.length; i++) {
            assertEquals(classes[i - 1].maxExclusive(), classes[i].minInclusive());
        }
        for (PayloadClass c : classes) {
            assertEquals(Optional.of(c), PayloadClass.classify(c.minInclusive()));
            assertEquals(Optional.of(c), PayloadClass.classify(c.maxExclusive() - 1));
            assertEquals(Optional.of(c), PayloadClass.classify(c.targetBytes()));
        }
        assertEquals(Optional.empty(), PayloadClass.classify(-1));
        assertEquals(Optional.empty(), PayloadClass.classify(PayloadClass.LARGE.maxExclusive()));
    }

    @Test
    void awsJsonFixturesLandInTheirPayloadClass() {
        for (PayloadClass c : PayloadClass.values()) {
            DynamoDbWorkloads.Fixture fixture = new DynamoDbWorkloads.Fixture(c);
            assertEquals(Optional.of(c), PayloadClass.classify(fixture.encodedBytes()));
            assertEquals(Optional.of(c), PayloadClass.classify(
                fixture.requestJson().length + (long) fixture.modeledError().body().length));
        }
    }

    @Test
    void everyFiniteFixtureLandsInItsPayloadClass() {
        for (BenchmarkCell cell : FULL) {
            if (cell.kind() != WorkloadKind.FINITE) {
                continue;
            }
            WorkloadProvider.forCell(cell).ifPresent(p -> {
                ServiceFixture fixture = p.fixture(cell);
                assertEquals(Optional.of(cell.payload()), PayloadClass.classify(fixture.encodedBytes()), cell.id());
                if (fixture instanceof FiniteWorkloads.PaddedFixture padded && cell.outcome() == Outcome.MODELED_ERROR) {
                    assertEquals(Optional.of(cell.payload()), PayloadClass.classify(
                        padded.request().length + (long) padded.modeledError().body().length), cell.id());
                }
            });
        }
    }

    /**
     * The exact set of matrix cells without a fixture, which release mode refuses: none. Losing a fixture for any
     * cell fails here.
     */
    @Test
    void everyMatrixCellHasAFixture() {
        List<BenchmarkCell> missing = FULL.stream().filter(c -> WorkloadProvider.forCell(c).isEmpty()).toList();
        assertEquals(List.of(), missing);
    }

    /**
     * Every Event Stream cell is served by its protocol's Event Stream fixture, on every path but generated-sync
     * (inapplicable) and every transport of the path's mode: 10 cells per protocol.
     */
    @Test
    void eventStreamCellsHaveTheirProtocolsFixture() {
        Map<BenchProtocol, String> expected = Map.of(BenchProtocol.AWS_JSON_1_1, "kinesis-subscribetoshard",
                                                     BenchProtocol.REST_JSON_1, "lambda-invokewithresponsestream",
                                                     BenchProtocol.REST_XML, "s3-selectobjectcontent");
        Map<BenchProtocol, Long> counts = new java.util.EnumMap<>(BenchProtocol.class);
        for (BenchmarkCell cell : FULL) {
            if (cell.kind() != WorkloadKind.EVENT_STREAM) {
                continue;
            }
            assertEquals(expected.get(cell.protocol()), WorkloadProvider.forCell(cell).orElseThrow().name(),
                         cell.id());
            counts.merge(cell.protocol(), 1L, Long::sum);
        }
        assertEquals(Map.of(BenchProtocol.AWS_JSON_1_1, 10L, BenchProtocol.REST_JSON_1, 10L,
                            BenchProtocol.REST_XML, 10L), counts);
    }

    /** Exactly the three documented groups of well-formed cells are inapplicable, each with its reason. */
    @Test
    void inapplicableCellsAreExactlyTheDocumentedGroups() {
        List<BenchmarkCell> wellFormed = new ArrayList<>();
        for (BenchProtocol protocol : BenchProtocol.values()) {
            for (ClientPath path : ClientPath.values()) {
                for (Transport transport : Transport.values()) {
                    if (transport.mode() != path.mode()) {
                        continue;
                    }
                    for (PayloadClass payload : PayloadClass.values()) {
                        for (Outcome outcome : Outcome.values()) {
                            wellFormed.add(BenchmarkCell.finite(protocol, path, transport, payload, outcome, 1));
                        }
                    }
                    if (protocol.supportsEventStreams()) {
                        wellFormed.add(new BenchmarkCell(protocol, path, transport, WorkloadKind.EVENT_STREAM, null,
                                                         Outcome.SUCCESS, 1));
                    }
                    if (protocol.supportsStreamingBlobs()) {
                        wellFormed.add(new BenchmarkCell(protocol, path, transport, WorkloadKind.STREAMING, null,
                                                         Outcome.SUCCESS, 1));
                    }
                }
            }
        }
        Map<String, Long> byReason = wellFormed.stream().map(CellMatrix::inapplicability).filter(Optional::isPresent)
                                               .collect(Collectors.groupingBy(Optional::get, Collectors.counting()));
        assertEquals(Map.of(CellMatrix.GENERATED_EVENT_STREAMS_ASYNC_ONLY, 5L * 4,
                            CellMatrix.NO_FLEET_EVENT_STREAM_REASON, 2L * 10,
                            CellMatrix.EC2_NO_MODELED_ERRORS, 3L * 14), byReason);
        assertTrue(FULL.stream().allMatch(c -> CellMatrix.inapplicability(c).isEmpty()));
        assertTrue(CellMatrix.enumerate(CellMatrix.Selection.parse("ec2Query", null, null, null, null, "error", null))
                             .isEmpty());
    }

    /** The product fact behind {@link CellMatrix#GENERATED_EVENT_STREAMS_ASYNC_ONLY}, for every fleet event stream. */
    @Test
    void generatedEventStreamOperationsAreAsyncOnly() {
        Map<Class<?>, Class<?>> syncToAsync = Map.of(
            software.amazon.awssdk.services.kinesis.KinesisClient.class,
            software.amazon.awssdk.services.kinesis.KinesisAsyncClient.class,
            software.amazon.awssdk.services.lambda.LambdaClient.class,
            software.amazon.awssdk.services.lambda.LambdaAsyncClient.class,
            software.amazon.awssdk.services.s3.S3Client.class, software.amazon.awssdk.services.s3.S3AsyncClient.class);
        Map<Class<?>, String> operation = Map.of(
            software.amazon.awssdk.services.kinesis.KinesisClient.class, "subscribeToShard",
            software.amazon.awssdk.services.lambda.LambdaClient.class, "invokeWithResponseStream",
            software.amazon.awssdk.services.s3.S3Client.class, "selectObjectContent");
        syncToAsync.forEach((sync, async) -> {
            String name = operation.get(sync);
            assertTrue(Arrays.stream(async.getMethods()).anyMatch(m -> m.getName().equals(name)), async + name);
            assertFalse(Arrays.stream(sync.getMethods()).anyMatch(m -> m.getName().equals(name)), sync + name);
        });
    }

    /**
     * The model facts behind {@link CellMatrix#NO_FLEET_EVENT_STREAM} and {@link CellMatrix#EC2_NO_MODELED_ERRORS},
     * read from the vendored fleet models: no service on awsJson1.0 or rpcv2Cbor has an operation whose input or
     * output binds a {@code @streaming} union, while awsJson1.1, restJson1 and restXml services do; and the EC2 model,
     * the only ec2Query service, has no {@code @error} shape.
     */
    @Test
    void fleetModelFactsBehindTheExclusions() throws Exception {
        Path services = Path.of("../../services");
        Map<String, Set<String>> eventStreamOps = new java.util.TreeMap<>();
        List<String> ec2QueryServices = new ArrayList<>();
        try (java.util.stream.Stream<Path> modules = Files.list(services)) {
            for (Path module : modules.sorted().toList()) {
                Path model = module.resolve("src/main/smithy/model.json");
                if (!Files.isRegularFile(model)) {
                    continue;
                }
                String text = Files.readString(model);
                boolean candidate = text.contains("\"smithy.api#streaming\"") || text.contains("aws.protocols#ec2Query");
                if (!candidate) {
                    continue;
                }
                ObjectNode shapes = Node.parse(text).expectObjectNode().expectObjectMember("shapes");
                for (Map.Entry<String, Node> e : shapes.getStringMap().entrySet()) {
                    ObjectNode shape = e.getValue().expectObjectNode();
                    if (!"service".equals(shape.expectStringMember("type").getValue())) {
                        continue;
                    }
                    ObjectNode traits = shape.getObjectMember("traits").orElse(Node.objectNode());
                    for (BenchProtocol protocol : BenchProtocol.values()) {
                        if (traits.containsMember(protocol.shapeId()) && hasEventStream(shapes)) {
                            eventStreamOps.computeIfAbsent(protocol.id(), k -> new TreeSet<>()).add(e.getKey());
                        }
                    }
                    if (traits.containsMember(BenchProtocol.EC2_QUERY.shapeId())) {
                        ec2QueryServices.add(e.getKey());
                        assertTrue(shapes.getMembers().values().stream().noneMatch(
                            n -> n.expectObjectNode().getObjectMember("traits")
                                  .map(t -> t.containsMember("smithy.api#error")).orElse(false)), e.getKey());
                    }
                }
            }
        }
        for (BenchProtocol protocol : CellMatrix.NO_FLEET_EVENT_STREAM) {
            assertFalse(eventStreamOps.containsKey(protocol.id()), protocol.id() + ": " + eventStreamOps);
        }
        for (String covered : List.of("awsJson1_1", "restJson1", "restXml")) {
            assertTrue(eventStreamOps.containsKey(covered), covered + ": " + eventStreamOps);
        }
        assertEquals(List.of("com.amazonaws.ec2#AmazonEC2"), ec2QueryServices);
    }

    private static boolean hasEventStream(ObjectNode shapes) {
        for (Node node : shapes.getMembers().values()) {
            ObjectNode shape = node.expectObjectNode();
            if (!"operation".equals(shape.expectStringMember("type").getValue())) {
                continue;
            }
            for (String direction : List.of("input", "output")) {
                Optional<String> target = shape.getObjectMember(direction)
                                               .flatMap(o -> o.getStringMember("target")).map(t -> t.getValue());
                Optional<ObjectNode> struct = target.flatMap(t -> shapes.getObjectMember(t));
                if (struct.isEmpty()) {
                    continue;
                }
                for (Node member : struct.get().getObjectMember("members").orElse(Node.objectNode())
                                         .getMembers().values()) {
                    String memberTarget = member.expectObjectNode().expectStringMember("target").getValue();
                    Optional<ObjectNode> union = shapes.getObjectMember(memberTarget);
                    if (union.isPresent() && "union".equals(union.get().expectStringMember("type").getValue())
                        && union.get().getObjectMember("traits").map(t -> t.containsMember("smithy.api#streaming"))
                                .orElse(false)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
