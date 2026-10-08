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

package software.amazon.awssdk.test.protocoltests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.test.protocoltests.support.ApprovedOmissions;
import software.amazon.awssdk.test.protocoltests.support.Conformance;
import software.amazon.awssdk.test.protocoltests.support.Conformance.Outcome;
import software.amazon.awssdk.test.protocoltests.support.GeneratedClients;
import software.amazon.awssdk.test.protocoltests.support.ProtocolCase;
import software.amazon.awssdk.test.protocoltests.support.ProtocolTestModels;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Requirements 19.1, 19.14 and 19.15: every applicable case of the pinned Smithy protocol test models, for every one
 * of the seven launch protocol runtimes, runs through a client generated from its test service by the single
 * generator and through the real V3 pipeline ({@link ProtocolCaseRunner}).
 *
 * <p>Every discovered case is accounted for exactly once:
 * <ul>
 *   <li>a case that does not apply to a V3 client (server cases, malformed-request cases, protocols without a V3
 *       runtime) is counted with its reason ({@link ProtocolTestModels});</li>
 *   <li>every applicable case is one dynamic test, bounded by {@link Conformance#CASE_TIMEOUT}; a failure or timeout
 *       fails the build;</li>
 *   <li>an applicable case listed in {@code approved-protocol-omissions.txt} (reason and manifest/requirement
 *       reference per case) still runs; it passes the build only while it fails, so an approved case that starts
 *       passing fails until its entry is removed. A timeout is never approved.</li>
 * </ul>
 * Nothing is skipped: the suite uses no assumptions. The machine-readable report
 * ({@code target/protocol-conformance/protocol-conformance-report.json}) gives the case counts per protocol and per
 * service, every approved omission, every not-applicable reason and every failure, for the release evidence.
 */
class ProtocolConformanceTest {
    static final String OMISSIONS = "approved-protocol-omissions.txt";

    private static ProtocolTestModels models;
    private static GeneratedClients clients;
    private static ApprovedOmissions omissions;
    private static ProtocolCaseRunner runner;
    private static final Map<String, String> RESULTS = new ConcurrentHashMap<>();
    private static final Map<String, String> FAILURES = new ConcurrentHashMap<>();

    @BeforeAll
    static void generateClients() {
        models = ProtocolTestModels.load();
        omissions = ApprovedOmissions.load(OMISSIONS);
        Path work = Path.of(Conformance.requiredProperty("v3.protocolTests.work"));
        clients = GeneratedClients.generate(
            models, work, ProtocolTestModels.loadCanonical(Path.of(Conformance.requiredProperty(
                "v3.protocolTests.s3Model"))));
        runner = new ProtocolCaseRunner(models.model(), clients);
    }

    @AfterAll
    static void writeReport() throws IOException {
        if (models == null) {
            return;
        }
        try {
            Path work = Path.of(Conformance.requiredProperty("v3.protocolTests.work"));
            Files.createDirectories(work);
            Files.writeString(work.resolve("protocol-conformance-report.json"), Node.prettyPrintJson(report()),
                              StandardCharsets.UTF_8);
        } finally {
            if (clients != null) {
                clients.close();
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> everyApplicablePinnedProtocolCase() {
        return models.cases().stream().filter(ProtocolCase::applicable).map(c -> DynamicTest.dynamicTest(
            c.protocolName() + " " + c.key(), () -> runCase(c)));
    }

    private static void runCase(ProtocolCase c) {
        Outcome outcome = Conformance.run(() -> runner.run(c));
        var approved = omissions.entry(c.key());
        if (outcome instanceof Outcome.TimedOut) {
            record(c, "timed-out", "did not finish within " + Conformance.CASE_TIMEOUT);
            throw Conformance.failure(c.key(), outcome);
        }
        if (approved.isPresent()) {
            if (outcome instanceof Outcome.Passed) {
                record(c, "failed", "stale approved omission: the case passes");
                throw new AssertionFailedError(c.key() + ": the case passes, so its approved omission ("
                                               + omissions.resource() + " line " + approved.get().line()
                                               + ") is stale; remove the entry");
            }
            RESULTS.put(c.key(), "approved-omission");
            return;
        }
        if (outcome instanceof Outcome.Failed) {
            record(c, "failed", Conformance.describe(((Outcome.Failed) outcome).failure()));
            throw Conformance.failure(c.key(), outcome);
        }
        RESULTS.put(c.key(), "passed");
    }

    private static void record(ProtocolCase c, String result, String message) {
        RESULTS.put(c.key(), result);
        FAILURES.put(c.key(), message);
    }

    /** Discovery cannot silently come up empty: every launch protocol has applicable cases of the pinned models. */
    @Test
    void everyLaunchProtocolHasApplicablePinnedCases() {
        Set<ShapeId> covered = models.cases().stream().filter(ProtocolCase::applicable).map(ProtocolCase::protocol)
                                     .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(new TreeSet<>(ProtocolTestModels.LAUNCH_PROTOCOLS), covered);
        for (ProtocolCase.Kind kind : List.of(ProtocolCase.Kind.REQUEST, ProtocolCase.Kind.RESPONSE,
                                              ProtocolCase.Kind.ERROR)) {
            for (ShapeId protocol : ProtocolTestModels.LAUNCH_PROTOCOLS) {
                assertTrue(models.cases().stream().anyMatch(c -> c.applicable() && c.kind() == kind
                                                                 && c.protocol().equals(protocol)),
                           () -> "no applicable " + kind.label() + " case for " + protocol);
            }
        }
    }

    /** Every service with applicable cases was generated and compiled; a generation failure is a defect. */
    @Test
    void everyServiceUnderTestIsGenerated() {
        Map<String, String> failures = new TreeMap<>();
        clients.failures().forEach((service, failure) -> failures.put(service.toString(),
                                                                       Conformance.describe(failure)));
        assertEquals(Map.of(), failures);
    }

    /** Every approved omission is well formed and names an applicable case of the pinned models. */
    @Test
    void everyApprovedOmissionNamesAnApplicablePinnedCase() {
        assertEquals(List.of(), omissions.problems());
        Set<String> applicable = models.cases().stream().filter(ProtocolCase::applicable).map(ProtocolCase::key)
                                       .collect(Collectors.toSet());
        Set<String> notApplicable = models.cases().stream().filter(c -> !c.applicable()).map(ProtocolCase::key)
                                          .collect(Collectors.toSet());
        List<String> problems = new ArrayList<>();
        omissions.entries().values().forEach(entry -> {
            if (notApplicable.contains(entry.key())) {
                problems.add(entry.key() + " is not applicable to a client, so it needs no approval");
            } else if (!applicable.contains(entry.key())) {
                problems.add(entry.key() + " names no case of the pinned protocol test models");
            }
        });
        assertEquals(List.of(), problems);
    }

    private static ObjectNode report() {
        List<ProtocolCase> cases = models.cases();
        ObjectNode.Builder out = Node.objectNodeBuilder();
        out.withMember("requirements", Node.fromStrings("19.1", "19.14", "19.15"));
        out.withMember("models", Node.fromStrings(models.jars().stream().map(p -> p.getFileName().toString())
                                                        .toList()));
        out.withMember("caseTimeoutSeconds", Conformance.CASE_TIMEOUT.toSeconds());
        out.withMember("totals", counts(cases));
        out.withMember("byProtocol", grouped(cases, ProtocolCase::protocolName));
        out.withMember("byService", grouped(cases, c -> c.service().toString()));
        ObjectNode.Builder notApplicable = Node.objectNodeBuilder();
        cases.stream().filter(c -> !c.applicable())
             .collect(Collectors.groupingBy(ProtocolCase::notApplicable, TreeMap::new, Collectors.counting()))
             .forEach((reason, count) -> notApplicable.withMember(reason, count));
        out.withMember("notApplicableReasons", notApplicable.build());
        List<Node> approved = new ArrayList<>();
        omissions.entries().values().forEach(e -> approved.add(Node.objectNodeBuilder()
            .withMember("case", e.key()).withMember("reason", e.reason()).withMember("reference", e.reference())
            .withMember("result", RESULTS.getOrDefault(e.key(), "not-run")).build()));
        out.withMember("approvedOmissions", Node.fromNodes(approved));
        ObjectNode.Builder failures = Node.objectNodeBuilder();
        new TreeMap<>(FAILURES).forEach(failures::withMember);
        out.withMember("failures", failures.build());
        ObjectNode.Builder generation = Node.objectNodeBuilder();
        clients.failures().forEach((service, failure) -> generation.withMember(service.toString(),
                                                                                Conformance.describe(failure)));
        out.withMember("generationFailures", generation.build());
        return out.build();
    }

    private static ObjectNode grouped(List<ProtocolCase> cases, Function<ProtocolCase, String> key) {
        ObjectNode.Builder out = Node.objectNodeBuilder();
        cases.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()))
             .forEach((name, group) -> out.withMember(name, counts(group)));
        return out.build();
    }

    private static ObjectNode counts(List<ProtocolCase> cases) {
        Map<String, Long> results = new TreeMap<>();
        for (ProtocolCase c : cases) {
            String result = c.applicable() ? RESULTS.getOrDefault(c.key(), "not-run") : "not-applicable";
            results.merge(result, 1L, Long::sum);
        }
        ObjectNode.Builder byKind = Node.objectNodeBuilder();
        cases.stream().collect(Collectors.groupingBy(c -> c.kind().label(), TreeMap::new, Collectors.counting()))
             .forEach(byKind::withMember);
        ObjectNode.Builder out = Node.objectNodeBuilder().withMember("discovered", cases.size())
                                     .withMember("applicable", cases.stream().filter(ProtocolCase::applicable).count())
                                     .withMember("skipped", 0);
        results.forEach(out::withMember);
        return out.withMember("byKind", byKind.build()).build();
    }
}
