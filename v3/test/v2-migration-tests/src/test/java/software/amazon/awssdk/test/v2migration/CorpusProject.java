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

package software.amazon.awssdk.test.v2migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * One Migration Corpus project: a standalone V2 Maven project directory under {@code src/test/corpus} with
 * {@value #DESCRIPTOR} (what it is and what its migration must achieve) and {@value #EXPECTED_BEHAVIOR} (the transcript
 * its behavioral entry point prints on the V2 baseline, which the transformed project must reproduce).
 *
 * @param name             the directory name
 * @param directory        the project directory
 * @param description      what the project is
 * @param kind             {@code REPRESENTATIVE} (shaped like a real application) or {@code SYNTHETIC} (shaped to
 *                         reach specific migration paths)
 * @param scenarios        {@code PRESERVED_ONLY}, {@code ADAPTED} and/or {@code EXCLUDED}
 * @param entryPoint       the class whose {@code public static String run(String endpoint)} is the behavioral suite
 * @param service          the fake service the suite calls ({@code dynamodb}) or {@code none}
 * @param expect           the declared expectations
 * @param expectedBehavior the expected transcript
 */
record CorpusProject(String name, Path directory, String description, String kind, List<String> scenarios,
                     String entryPoint, String service, Expectations expect, String expectedBehavior) {

    static final String DESCRIPTOR = "corpus-project.json";
    static final String EXPECTED_BEHAVIOR = "expected-behavior.txt";
    private static final Set<String> KINDS = Set.of("REPRESENTATIVE", "SYNTHETIC");
    private static final Set<String> SCENARIOS = Set.of("PRESERVED_ONLY", "ADAPTED", "EXCLUDED");
    private static final Set<String> SERVICES = Set.of("dynamodb", "none");

    /**
     * What the migration of a project must achieve.
     *
     * @param cleanMigration       no item may be unresolved
     * @param javaSourcesUnchanged no Java file may change (only the POM)
     * @param compiles             the transformed project must compile against the candidate and pass its behavioral
     *                             suite in every build, not only at a launch gate stage
     * @param javaRelease          the Java release the migrated POM must compile for
     * @param transformed          manifest entry IDs that must have a {@code TRANSFORMED} item
     * @param unresolved           unresolved items that must exist, each with the replacement its diagnostic names
     */
    record Expectations(boolean cleanMigration, boolean javaSourcesUnchanged, boolean compiles, int javaRelease,
                        List<String> transformed, List<Unresolved> unresolved) {
    }

    /** An unresolved item whose entry ID starts with {@code entryIdPrefix} and whose diagnostic names {@code replacement}. */
    record Unresolved(String entryIdPrefix, String replacement) {
    }

    /** Every project under {@code root}, by name. */
    static List<CorpusProject> loadAll(Path root) throws IOException {
        try (Stream<Path> dirs = Files.list(root)) {
            List<Path> projects = dirs.filter(d -> Files.isRegularFile(d.resolve(DESCRIPTOR))).sorted().toList();
            if (projects.isEmpty()) {
                throw new AssertionError("No corpus projects in " + root);
            }
            List<CorpusProject> out = new ArrayList<>();
            for (Path p : projects) {
                out.add(load(p));
            }
            return out;
        }
    }

    static CorpusProject load(Path directory) throws IOException {
        String name = directory.getFileName().toString();
        ObjectNode json = Node.parse(Files.readString(directory.resolve(DESCRIPTOR), StandardCharsets.UTF_8))
                              .expectObjectNode();
        json.expectNoAdditionalProperties(List.of("description", "kind", "scenarios", "behavior", "expect"));
        String kind = json.expectStringMember("kind").getValue();
        List<String> scenarios = json.expectArrayMember("scenarios").getElementsAs(StringNode::getValue);
        ObjectNode behavior = json.expectObjectMember("behavior");
        String service = behavior.expectStringMember("service").getValue();
        if (!KINDS.contains(kind) || scenarios.isEmpty() || !SCENARIOS.containsAll(scenarios)
            || !SERVICES.contains(service)) {
            throw new IllegalArgumentException(name + ": invalid kind, scenarios or service in " + DESCRIPTOR);
        }
        ObjectNode e = json.expectObjectMember("expect");
        List<Unresolved> unresolved = e.getArrayMember("unresolved")
                                       .map(a -> a.getElementsAs(n -> {
                                           ObjectNode u = n.expectObjectNode();
                                           return new Unresolved(u.expectStringMember("entryIdPrefix").getValue(),
                                                                 u.expectStringMember("replacement").getValue());
                                       }))
                                       .orElse(List.of());
        Expectations expect = new Expectations(e.getBooleanMemberOrDefault("cleanMigration", false),
                                               e.getBooleanMemberOrDefault("javaSourcesUnchanged", false),
                                               e.getBooleanMemberOrDefault("compiles", false),
                                               e.expectNumberMember("javaRelease").getValue().intValue(),
                                               e.getArrayMember("transformed")
                                                .map(a -> a.getElementsAs(StringNode::getValue)).orElse(List.of()),
                                               unresolved);
        if (expect.compiles() && !expect.cleanMigration()) {
            throw new IllegalArgumentException(name + ": a project that must compile must also migrate cleanly");
        }
        Path expected = directory.resolve(EXPECTED_BEHAVIOR);
        if (!Files.isRegularFile(expected)) {
            throw new IllegalArgumentException(name + ": no " + EXPECTED_BEHAVIOR);
        }
        return new CorpusProject(name, directory, json.expectStringMember("description").getValue(), kind,
                                 scenarios, behavior.expectStringMember("entryPoint").getValue(), service, expect,
                                 Files.readString(expected, StandardCharsets.UTF_8).strip());
    }
}
