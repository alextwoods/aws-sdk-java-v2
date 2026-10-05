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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.smithy.runtime.version.RuntimeModuleVersion;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionConflict;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionHandshake;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionMismatchException;
import software.amazon.awssdk.test.architecture.MavenResolutionModel.ImportedBom;
import software.amazon.awssdk.test.architecture.MavenResolutionModel.Key;
import software.amazon.awssdk.test.architecture.MavenResolutionModel.Project;

/**
 * Property 55: BOM and runtime versions are coherent (Requirements 15.1 and 15.11).
 *
 * <p>Each try generates a release version, a consumer project and the class path the consumer's client runs on:
 *
 * <ul>
 *   <li>the real {@code v3/bom/pom.xml} released at the generated version, imported by the consumer before, between or
 *       after generated stale BOMs (arbitrary artifact/version inventories, including retired V3 artifacts and
 *       third-party artifacts whose artifactIds collide with V3 artifactIds);</li>
 *   <li>participating V3 modules declared directly without a version, pinned to a version, or reached only
 *       transitively at arbitrary versions and depths, plus the consumer's own dependency management;</li>
 *   <li>per-module class-path defects: absent modules, missing, malformed, version-less or foreign version resources,
 *       whitespace-padded resources and duplicate copies before or after the module jar.</li>
 * </ul>
 *
 * <p>The BOM is checked against the dependency policy's release inventory; the Maven resolution of the consumer graph
 * ({@link MavenResolutionModel}, which reads the real BOM) against {@link Oracle}, which uses the policy inventory in
 * place of the BOM and mediates versions by a depth-first minimum instead of a breadth-first walk; and
 * {@link RuntimeVersionHandshake#findConflicts} against the oracle's conflict rule (the distinct declared
 * artifactId/version pairs of a participating module must be exactly {@code {(module, release)}}). About a third of
 * the tries also materialize the class path as directories behind an isolated class loader and run
 * {@link RuntimeVersionHandshake#verify} end to end, checking the thrown diagnostic lists every conflicting module and
 * version.
 */
class RuntimeVersionCoherencePropertyTest {

    private static final String THIRD_PARTY_GROUP = "org.example.thirdparty";

    /** Third-party artifactIds; two collide with V3 artifactIds, so management must be keyed by groupId. */
    private static final List<String> THIRD_PARTY_ARTIFACTS =
        List.of("annotations", "sdk-core", "commons-lang3", "reactive-streams", "jackson-core");

    /** V3 artifactIds that an older BOM could manage but that are not (or no longer) release artifacts. */
    private static final List<String> RETIRED_V3_ARTIFACTS = List.of("legacy-core", "legacy-protocols");

    // Feature: aws-sdk-java-v3, Property 55: BOM and runtime versions are coherent
    // Embedded edge cases are disabled: through the nested flatMaps they pick the first alternative of most choices
    // (half of the tries at disorder 0), starving conflicting scenarios. The generator weights boundary cases
    // explicitly instead, and the statistics coverage checks below fail the property if the mix degrades.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 55: BOM and runtime versions are coherent")
    void bomAndRuntimeVersionsAreCoherent(@ForAll("scenarios") Scenario scenario) throws IOException {
        String release = scenario.releaseVersion();
        Path trialDirectory = Files.createTempDirectory(Inventory.get().workDirectory(), "try-");
        try {
            // Requirement 15.1: the BOM maps every release artifact, and nothing else, to exactly the release version.
            Map<Key, String> bom = releasedBom(release, trialDirectory);
            assertEquals(List.of(), mismatches(Oracle.bom(release), bom),
                         "v3/bom released at " + release + " must manage exactly the policy's release artifacts at "
                         + release);

            // Importing the BOM resolves the consumer graph as the independent oracle predicts.
            Map<Key, String> resolved = MavenResolutionModel.resolve(scenario.project(bom));
            Map<Key, String> expectedResolved = Oracle.resolve(scenario);
            assertEquals(List.of(), mismatches(expectedResolved, resolved), "Resolved dependency versions");

            // Requirement 15.11: the handshake reports exactly the conflicting modules, with every observed copy.
            List<ClasspathEntry> classpath = classpath(scenario, resolved);
            List<RuntimeVersionConflict> expectedConflicts = Oracle.conflicts(scenario, expectedResolved);
            List<RuntimeVersionConflict> conflicts =
                RuntimeVersionHandshake.findConflicts(release, scenario.verifiedModules(), observed(classpath));
            assertEquals(expectedConflicts, conflicts, "Runtime version conflicts");
            if (scenario.bomGoverned()) {
                assertEquals(List.of(), conflicts, "A BOM-governed dependency tree must pass the handshake");
            }

            if (scenario.materialize()) {
                verifyThroughClassLoader(scenario, classpath, expectedResolved, expectedConflicts, trialDirectory);
            }

            collectStatistics(scenario, expectedResolved, expectedConflicts);
        } finally {
            deleteRecursively(trialDirectory);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Checks
    // ---------------------------------------------------------------------------------------------------------------

    /** Writes the real BOM with its release version set to {@code release} and reads its managed versions. */
    private static Map<Key, String> releasedBom(String release, Path trialDirectory) throws IOException {
        Document document = parseXml(Inventory.get().bomPom());
        Element project = document.getDocumentElement();
        Element parent = onlyChild(project, "parent").orElseThrow();
        onlyChild(parent, "version").orElseThrow().setTextContent(release);
        onlyChild(project, "version").ifPresent(v -> v.setTextContent(release));

        Path file = Files.createDirectories(trialDirectory.resolve("bom")).resolve("pom.xml");
        writeXml(document, file);
        PomFile bom = PomFile.read(file);
        assertEquals(release, bom.version(), "BOM version");
        return MavenResolutionModel.managedVersions(bom);
    }

    /** Every key whose version differs between {@code expected} and {@code actual}, sorted. */
    private static List<String> mismatches(Map<Key, String> expected, Map<Key, String> actual) {
        Set<Key> keys = new TreeSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        return keys.stream()
                   .filter(k -> !Objects.equals(expected.get(k), actual.get(k)))
                   .map(k -> k + ": expected " + Optional.ofNullable(expected.get(k)).orElse("<unmanaged>")
                             + " but was " + Optional.ofNullable(actual.get(k)).orElse("<unmanaged>"))
                   .toList();
    }

    private static void verifyThroughClassLoader(Scenario scenario,
                                                 List<ClasspathEntry> classpath,
                                                 Map<Key, String> expectedResolved,
                                                 List<RuntimeVersionConflict> expectedConflicts,
                                                 Path trialDirectory) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (ClasspathEntry entry : classpath) {
            Path root = trialDirectory.resolve("classpath").resolve(entry.label());
            Path resource = root.resolve(RuntimeVersionHandshake.resourceName(entry.resourceModule()));
            Files.createDirectories(resource.getParent());
            Files.writeString(resource, entry.content(), StandardCharsets.ISO_8859_1);
            urls.add(root.toUri().toURL());
        }
        String release = scenario.releaseVersion();
        // The platform class loader as parent keeps this module's own version resources off the class path.
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            RuntimeVersionHandshake handshake = RuntimeVersionHandshake.create(loader);

            Map<String, List<RuntimeModuleVersion>> discovered = handshake.discover(scenario.verifiedModules());
            assertEquals(List.copyOf(new TreeSet<>(scenario.verifiedModules())), List.copyOf(discovered.keySet()),
                         "Discovered modules");
            discovered.forEach((module, copies) -> assertSameCopies(
                Oracle.copies(scenario, expectedResolved, module), copies, "discovered copies of " + module));

            if (expectedConflicts.isEmpty()) {
                assertDoesNotThrow(() -> handshake.verify(release, scenario.verifiedModules()));
                return;
            }
            RuntimeVersionMismatchException e =
                assertThrows(RuntimeVersionMismatchException.class,
                             () -> handshake.verify(release, scenario.verifiedModules()));
            assertEquals(release, e.expectedVersion());
            assertEquals(expectedConflicts.stream().map(RuntimeVersionConflict::artifactId).toList(),
                         e.conflicts().stream().map(RuntimeVersionConflict::artifactId).toList(),
                         "Conflicting modules");
            for (int i = 0; i < expectedConflicts.size(); i++) {
                RuntimeVersionConflict expected = expectedConflicts.get(i);
                RuntimeVersionConflict actual = e.conflicts().get(i);
                assertEquals(release, actual.expectedVersion());
                assertSameCopies(expected.observed(), actual.observed(), "conflict copies of " + expected.artifactId());
            }
            assertMessageListsEveryConflict(e.getMessage(), release, expectedConflicts);
        }
    }

    /**
     * Compares copies by declared artifactId and version; {@code actual} sources are class-path URLs that must name the
     * class-path entry recorded as the {@code expected} source.
     */
    private static void assertSameCopies(List<RuntimeModuleVersion> expected, List<RuntimeModuleVersion> actual,
                                         String description) {
        assertEquals(expected.stream().map(c -> c.artifactId() + "@" + c.version()).toList(),
                     actual.stream().map(c -> c.artifactId() + "@" + c.version()).toList(), description);
        for (int i = 0; i < expected.size(); i++) {
            String entry = "/" + expected.get(i).source() + "/META-INF/";
            assertTrue(actual.get(i).source().contains(entry),
                       description + ": source " + actual.get(i).source() + " does not name " + entry);
        }
    }

    private static void assertMessageListsEveryConflict(String message, String release,
                                                        List<RuntimeVersionConflict> expected) {
        List<String> lines = message.lines().toList();
        String header = lines.get(0);
        assertTrue(header.contains("exactly " + release + ","), header);
        assertTrue(header.contains(expected.size() + (expected.size() == 1 ? " module conflicts:"
                                                                             : " modules conflict:")), header);

        List<String> listed = lines.stream().filter(l -> l.startsWith("  - ")).toList();
        assertEquals(expected.stream().map(RuntimeVersionConflict::artifactId).toList(),
                     listed.stream().map(l -> l.substring(4, l.indexOf(": "))).toList(),
                     "Modules listed in the diagnostic:\n" + message);
        for (int i = 0; i < expected.size(); i++) {
            RuntimeVersionConflict conflict = expected.get(i);
            String line = listed.get(i);
            if (conflict.observed().isEmpty()) {
                assertTrue(line.contains(": missing"), line);
            }
            for (RuntimeModuleVersion copy : conflict.observed()) {
                String version = copy.version().isEmpty() ? "<no version>" : copy.version();
                assertTrue(line.contains(version + " (") || line.contains(version + " declaring"),
                           "Diagnostic line must identify version " + version + ": " + line);
                if (!copy.artifactId().equals(conflict.artifactId())) {
                    String declared = copy.artifactId().isEmpty() ? "<none>" : copy.artifactId();
                    assertTrue(line.contains("declaring artifactId=" + declared), line);
                }
                assertTrue(line.contains("/" + copy.source() + "/"), "Diagnostic line must name " + copy.source()
                                                                      + ": " + line);
            }
        }
    }

    private static void collectStatistics(Scenario scenario, Map<Key, String> resolved,
                                          List<RuntimeVersionConflict> conflicts) {
        String release = scenario.releaseVersion();
        Statistics.label("handshake outcome").collect(conflicts.isEmpty() ? "aligned" : "conflicting");
        Statistics.label("handshake outcome").coverage(c -> {
            c.check("aligned").percentage(p -> p >= 5);
            c.check("conflicting").percentage(p -> p >= 30);
        });

        Statistics.label("disorder %").collect(scenario.disorder());
        Statistics.label("class-loader try").collect(scenario.materialize());
        Statistics.label("class-loader try").coverage(c -> c.check(true).percentage(p -> p >= 15));

        boolean bomOverrodeStaleVersion = scenario.modules().stream().anyMatch(
            m -> m.placement() == Placement.TRANSITIVE
                 && m.transitive().stream().anyMatch(t -> !t.version().equals(release))
                 && release.equals(resolved.get(Oracle.v3Key(m.artifactId()))));
        Statistics.label("BOM replaced a stale transitive version").collect(bomOverrodeStaleVersion);
        Statistics.label("BOM replaced a stale transitive version")
                  .coverage(c -> c.check(true).percentage(p -> p >= 10));

        // Every labelled collector needs at least one entry per property run, or jqwik's report fails.
        if (conflicts.isEmpty()) {
            Statistics.label("conflict kind").collect("(none in this try)");
        }
        for (RuntimeVersionConflict conflict : conflicts) {
            Statistics.label("conflict kind").collect(conflictKind(conflict));
        }
        Statistics.label("conflict kind").coverage(c -> {
            for (String kind : List.of("missing", "foreign or unreadable resource", "no version", "wrong version",
                                       "copies disagree")) {
                c.check(kind).count(n -> n >= 1);
            }
        });
    }

    private static String conflictKind(RuntimeVersionConflict conflict) {
        List<RuntimeModuleVersion> copies = conflict.observed();
        if (copies.isEmpty()) {
            return "missing";
        }
        if (copies.stream().anyMatch(c -> !c.artifactId().equals(conflict.artifactId()))) {
            return "foreign or unreadable resource";
        }
        if (copies.stream().anyMatch(c -> c.version().isEmpty())) {
            return "no version";
        }
        if (copies.stream().map(RuntimeModuleVersion::version).distinct().count() > 1) {
            return "copies disagree";
        }
        return "wrong version";
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Independent oracle
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Expected results computed without {@link MavenResolutionModel}, the real BOM file or the handshake: the BOM is the
     * dependency policy's release inventory at the release version, version mediation is a minimum over every
     * depth-first occurrence ordered by (depth, declaration path), and a participating module conflicts unless the
     * distinct artifactId/version pairs it declares on the class path are exactly {@code {(module, release)}}.
     */
    static final class Oracle {

        private Oracle() {
        }

        static Key v3Key(String artifactId) {
            return new Key(Inventory.get().groupId(), artifactId);
        }

        static Map<Key, String> bom(String release) {
            Map<Key, String> expected = new TreeMap<>();
            Inventory.get().releaseArtifacts().forEach(a -> expected.put(v3Key(a), release));
            return expected;
        }

        static Map<Key, String> resolve(Scenario scenario) {
            List<Occurrence> occurrences = new ArrayList<>();
            List<MavenResolutionModel.Node> dependencies = scenario.dependencies();
            for (int i = 0; i < dependencies.size(); i++) {
                collect(dependencies.get(i), 1, List.of(i), occurrences);
            }
            // Mediation by depth-first minimum equals Maven's breadth-first walk only if no losing occurrence has
            // children; the generator gives children only to unique third-party libraries.
            Map<Key, Long> parents = occurrences.stream().filter(o -> o.node().children().size() > 0)
                                                .collect(Collectors.groupingBy(o -> o.node().key(),
                                                                               Collectors.counting()));
            parents.forEach((key, count) -> assertEquals(1L, count, "Generator invariant: " + key));

            Map<Key, List<Occurrence>> byKey = occurrences.stream()
                                                          .collect(Collectors.groupingBy(o -> o.node().key()));
            Map<Key, String> expected = new TreeMap<>();
            byKey.forEach((key, all) -> {
                Optional<String> managed = managedVersion(scenario, key);
                Optional<Occurrence> direct = all.stream().filter(o -> o.depth() == 1).findFirst();
                String version;
                if (direct.isPresent()) {
                    version = direct.get().node().version() != null ? direct.get().node().version()
                                                                   : managed.orElseThrow();
                } else if (managed.isPresent()) {
                    version = managed.get();
                } else {
                    version = all.stream().min(Occurrence.NEAREST_FIRST).orElseThrow().node().version();
                }
                expected.put(key, version);
            });
            return expected;
        }

        private static Optional<String> managedVersion(Scenario scenario, Key key) {
            if (scenario.localManagement().containsKey(key)) {
                return Optional.of(scenario.localManagement().get(key));
            }
            for (BomImport bom : scenario.imports()) {
                Map<Key, String> managed = bom instanceof StaleBom stale ? stale.managedVersions()
                                                                         : bom(scenario.releaseVersion());
                if (managed.containsKey(key)) {
                    return Optional.of(managed.get(key));
                }
            }
            return Optional.empty();
        }

        private static void collect(MavenResolutionModel.Node node, int depth, List<Integer> path,
                                    List<Occurrence> into) {
            into.add(new Occurrence(node, depth, path));
            for (int i = 0; i < node.children().size(); i++) {
                List<Integer> childPath = new ArrayList<>(path);
                childPath.add(i);
                collect(node.children().get(i), depth + 1, childPath, into);
            }
        }

        /** Every version resource of {@code module} the class path should contain, in class-path order. */
        static List<RuntimeModuleVersion> copies(Scenario scenario, Map<Key, String> resolved, String module) {
            ModulePlan plan = scenario.plan(module);
            List<RuntimeModuleVersion> copies = new ArrayList<>();
            List<ExtraCopy> extras = plan.extraCopies();
            for (int k = 0; k < extras.size(); k++) {
                if (extras.get(k).prepend()) {
                    copies.add(new RuntimeModuleVersion(module, extras.get(k).version(), extraLabel(module, k)));
                }
            }
            String version = resolved.get(v3Key(module));
            if (version != null) {
                mainCopy(plan, version).ifPresent(e -> copies.add(e.declared()));
            }
            for (int k = 0; k < extras.size(); k++) {
                if (!extras.get(k).prepend()) {
                    copies.add(new RuntimeModuleVersion(module, extras.get(k).version(), extraLabel(module, k)));
                }
            }
            return copies;
        }

        static List<RuntimeVersionConflict> conflicts(Scenario scenario, Map<Key, String> resolved) {
            String release = scenario.releaseVersion();
            List<RuntimeVersionConflict> conflicts = new ArrayList<>();
            for (String module : new TreeSet<>(scenario.verifiedModules())) {
                List<RuntimeModuleVersion> copies = copies(scenario, resolved, module);
                Set<List<String>> declared = copies.stream()
                                                   .map(c -> List.of(c.artifactId(), c.version()))
                                                   .collect(Collectors.toSet());
                if (!declared.equals(Set.of(List.of(module, release)))) {
                    conflicts.add(new RuntimeVersionConflict(module, release, copies));
                }
            }
            return conflicts;
        }

        private record Occurrence(MavenResolutionModel.Node node, int depth, List<Integer> path) {
            static final Comparator<Occurrence> NEAREST_FIRST =
                Comparator.comparingInt(Occurrence::depth).thenComparing(Occurrence::path, Oracle::lexicographic);
        }

        private static int lexicographic(List<Integer> a, List<Integer> b) {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int c = Integer.compare(a.get(i), b.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(a.size(), b.size());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Class path
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * One class-path entry that contains the version resource of {@code resourceModule}. {@code declared} is what the
     * handshake is expected to read from {@code content}; its source is the entry label.
     */
    record ClasspathEntry(String label, String resourceModule, RuntimeModuleVersion declared, String content) {
    }

    /** The client's class path for the resolved versions: prepended duplicates, module jars, appended duplicates. */
    private static List<ClasspathEntry> classpath(Scenario scenario, Map<Key, String> resolved) {
        List<ClasspathEntry> prepended = new ArrayList<>();
        List<ClasspathEntry> modules = new ArrayList<>();
        List<ClasspathEntry> appended = new ArrayList<>();
        for (ModulePlan plan : scenario.modules()) {
            for (int k = 0; k < plan.extraCopies().size(); k++) {
                ExtraCopy extra = plan.extraCopies().get(k);
                String label = extraLabel(plan.artifactId(), k);
                ClasspathEntry entry = new ClasspathEntry(
                    label, plan.artifactId(), new RuntimeModuleVersion(plan.artifactId(), extra.version(), label),
                    standardResource(plan.artifactId(), extra.version()));
                (extra.prepend() ? prepended : appended).add(entry);
            }
        }
        resolved.forEach((key, version) -> {
            if (key.groupId().equals(Inventory.get().groupId())) {
                mainCopy(scenario.plan(key.artifactId()), version).ifPresent(modules::add);
            }
        });
        return Stream.of(prepended, modules, appended).flatMap(List::stream).toList();
    }

    private static Map<String, List<RuntimeModuleVersion>> observed(List<ClasspathEntry> classpath) {
        Map<String, List<RuntimeModuleVersion>> observed = new LinkedHashMap<>();
        for (ClasspathEntry entry : classpath) {
            observed.computeIfAbsent(entry.resourceModule(), m -> new ArrayList<>()).add(entry.declared());
        }
        return observed;
    }

    /** The version resource in the module's own jar resolved at {@code version}, shaped by the plan's defect. */
    private static Optional<ClasspathEntry> mainCopy(ModulePlan plan, String version) {
        String module = plan.artifactId();
        String label = "main-" + module;
        return switch (plan.resource()) {
            case STANDARD -> Optional.of(new ClasspathEntry(label, module, new RuntimeModuleVersion(module, version, label),
                                                            standardResource(module, version)));
            case PADDED -> Optional.of(new ClasspathEntry(
                label, module, new RuntimeModuleVersion(module, version, label),
                "# Re-packaged\n  groupId = " + Inventory.get().groupId() + "\n\tartifactId =  " + module
                + " \t\nversion:" + version + "  \n"));
            case MISSING -> Optional.empty();
            case MALFORMED -> Optional.of(new ClasspathEntry(label, module, new RuntimeModuleVersion("", "", label),
                                                             "artifactId=" + module + "\nversion=\\u00G1\n"));
            case NO_VERSION_KEY -> Optional.of(new ClasspathEntry(
                label, module, new RuntimeModuleVersion(module, "", label),
                "groupId=" + Inventory.get().groupId() + "\nartifactId=" + module + "\n"));
            case FOREIGN_ARTIFACT_ID -> Optional.of(new ClasspathEntry(
                label, module, new RuntimeModuleVersion(module + "-repackaged", version, label),
                standardResource(module + "-repackaged", version)));
        };
    }

    private static String standardResource(String module, String version) {
        return "# Generated by the AWS SDK for Java V3 build. Do not edit.\ngroupId=" + Inventory.get().groupId()
               + "\nartifactId=" + module + "\nversion=" + version + "\n";
    }

    private static String extraLabel(String module, int k) {
        return "extra" + k + "-" + module;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenario model
    // ---------------------------------------------------------------------------------------------------------------

    enum Placement {
        /** Declared directly without a version (taken from dependency management). */
        DIRECT_MANAGED,
        /** Declared directly with {@link ModulePlan#pinnedVersion()}. */
        DIRECT_PINNED,
        /** Only reached transitively. */
        TRANSITIVE,
        /** Participating but not on the class path. */
        ABSENT
    }

    enum ResourceState { STANDARD, PADDED, MISSING, MALFORMED, NO_VERSION_KEY, FOREIGN_ARTIFACT_ID }

    record TransitiveUse(int depth, String version) {
    }

    record ExtraCopy(String version, boolean prepend) {
    }

    record ThirdPartyUse(String artifactId, int depth, String version) {
    }

    /** How one V3 module reaches the consumer's dependency graph and class path. */
    record ModulePlan(String artifactId, Placement placement, String pinnedVersion, List<TransitiveUse> transitive,
                      ResourceState resource, List<ExtraCopy> extraCopies) {
    }

    sealed interface BomImport permits V3Bom, StaleBom {
    }

    /** The real {@code software.amazon.awssdk:bom} at the release version. */
    record V3Bom() implements BomImport {
    }

    /** A generated artifact/version inventory imported as a BOM, for example an older V3 or third-party BOM. */
    record StaleBom(Map<Key, String> managedVersions) implements BomImport {
    }

    record Scenario(String releaseVersion,
                    int disorder,
                    List<ModulePlan> modules,
                    List<MavenResolutionModel.Node> dependencies,
                    List<BomImport> imports,
                    Map<Key, String> localManagement,
                    List<String> verifiedModules,
                    boolean materialize) {

        ModulePlan plan(String artifactId) {
            return modules.stream().filter(m -> m.artifactId().equals(artifactId)).findFirst().orElseThrow();
        }

        Project project(Map<Key, String> v3Bom) {
            List<ImportedBom> boms = new ArrayList<>();
            for (BomImport bom : imports) {
                boms.add(bom instanceof StaleBom stale ? new ImportedBom("stale", stale.managedVersions())
                                                       : new ImportedBom("software.amazon.awssdk:bom", v3Bom));
            }
            return new Project(dependencies, localManagement, boms);
        }

        /**
         * True when, by construction, the tree is governed by the V3 BOM: it is the first import, the consumer manages
         * or pins V3 modules only at the release version, and every module is present with intact resources at the
         * release version. Such a tree must pass the handshake.
         */
        boolean bomGoverned() {
            String groupId = Inventory.get().groupId();
            return imports.get(0) instanceof V3Bom
                   && localManagement.entrySet().stream()
                                     .filter(e -> e.getKey().groupId().equals(groupId))
                                     .allMatch(e -> e.getValue().equals(releaseVersion))
                   && modules.stream().allMatch(
                       m -> m.placement() != Placement.ABSENT
                            && (m.placement() != Placement.DIRECT_PINNED || m.pinnedVersion().equals(releaseVersion))
                            && (m.resource() == ResourceState.STANDARD || m.resource() == ResourceState.PADDED)
                            && m.extraCopies().stream().allMatch(c -> c.version().equals(releaseVersion)));
        }
    }

    /** Generated choices before the declaration order of the direct dependencies is shuffled. */
    private record Draft(String releaseVersion,
                         int disorder,
                         List<ModulePlan> modules,
                         List<ThirdPartyUse> thirdParty,
                         List<StaleBom> staleBoms,
                         int v3BomPosition,
                         Map<Key, String> localManagement,
                         List<String> verifiedModules,
                         boolean materialize) {

        /** Direct dependencies in generation order; transitive uses hang below chains of unique libraries. */
        List<MavenResolutionModel.Node> directDependencies() {
            List<MavenResolutionModel.Node> direct = new ArrayList<>();
            int[] libraries = {0};
            for (ModulePlan plan : modules) {
                Key key = Oracle.v3Key(plan.artifactId());
                switch (plan.placement()) {
                    case DIRECT_MANAGED -> direct.add(new MavenResolutionModel.Node(key, null, List.of()));
                    case DIRECT_PINNED -> direct.add(new MavenResolutionModel.Node(key, plan.pinnedVersion(),
                                                                                   List.of()));
                    default -> {
                    }
                }
                if (plan.placement() != Placement.ABSENT) {
                    for (TransitiveUse use : plan.transitive()) {
                        direct.add(chain(use.depth(), new MavenResolutionModel.Node(key, use.version(), List.of()),
                                         libraries));
                    }
                }
            }
            for (ThirdPartyUse use : thirdParty) {
                direct.add(chain(use.depth(), new MavenResolutionModel.Node(
                    new Key(THIRD_PARTY_GROUP, use.artifactId()), use.version(), List.of()), libraries));
            }
            return direct;
        }

        private static MavenResolutionModel.Node chain(int depth, MavenResolutionModel.Node leaf, int[] libraries) {
            MavenResolutionModel.Node node = leaf;
            for (int level = 1; level < depth; level++) {
                node = new MavenResolutionModel.Node(new Key(THIRD_PARTY_GROUP, "lib-" + libraries[0]++), "1.0.0",
                                                     List.of(node));
            }
            return node;
        }

        Scenario toScenario(List<MavenResolutionModel.Node> declarationOrder) {
            List<BomImport> imports = new ArrayList<>(staleBoms);
            imports.add(Math.min(v3BomPosition, staleBoms.size()), new V3Bom());
            return new Scenario(releaseVersion, disorder, modules, declarationOrder, imports, localManagement, verifiedModules,
                                materialize);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Scenarios with a disorder level (percentage of choices that break alignment): 0 generates BOM-governed trees whose
     * transitive versions are still arbitrary, higher levels add pins, stale BOM precedence, absent modules and
     * defective or duplicate resources.
     */
    @Provide
    Arbitrary<Scenario> scenarios() {
        return Combinators.combine(releaseVersions(), Arbitraries.of(0, 10, 35, 70)).flatAs(this::scenario);
    }

    private Arbitrary<Scenario> scenario(String release, int disorder) {
        List<String> inventory = Inventory.get().releaseArtifacts();
        return Arbitraries.subsetOf(inventory).ofMinSize(1).ofMaxSize(8).flatMap(participating -> {
            List<String> others = inventory.stream().filter(a -> !participating.contains(a)).toList();
            return Arbitraries.subsetOf(others).ofMaxSize(3).flatMap(bystanders -> {
                List<Arbitrary<ModulePlan>> plans = new ArrayList<>();
                new TreeSet<>(participating).forEach(a -> plans.add(modulePlan(a, true, release, disorder)));
                new TreeSet<>(bystanders).forEach(a -> plans.add(modulePlan(a, false, release, disorder)));
                List<String> onGraph = Stream.concat(participating.stream(), bystanders.stream()).sorted().toList();
                return Combinators.combine(Combinators.combine(plans).as(List::copyOf),
                                           thirdPartyUses(),
                                           staleBom(release).list().ofMaxSize(2),
                                           ordered(disorder, Arbitraries.just(0), Arbitraries.integers().between(0, 2)),
                                           localManagement(onGraph, release, disorder),
                                           verifiedModules(participating),
                                           Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(true)),
                                                                   Tuple.of(2, Arbitraries.just(false))))
                                  .as((modules, thirdParty, stale, position, management, verified, materialize) ->
                                          new Draft(release, disorder, modules, thirdParty, stale, position, management, verified,
                                                    materialize));
            });
        }).flatMap(draft -> Arbitraries.shuffle(draft.directDependencies()).map(draft::toScenario));
    }

    private static Arbitrary<ModulePlan> modulePlan(String artifactId, boolean participating, String release,
                                                    int disorder) {
        Arbitrary<Placement> placement = Arbitraries.of(Placement.DIRECT_MANAGED, Placement.DIRECT_PINNED,
                                                        Placement.TRANSITIVE);
        if (participating) {
            placement = ordered(disorder, placement, Arbitraries.just(Placement.ABSENT));
        }
        Arbitrary<List<TransitiveUse>> transitive =
            Combinators.combine(Arbitraries.integers().between(2, 4), anyVersion(release))
                       .as(TransitiveUse::new).list().ofMinSize(1).ofMaxSize(3);
        Arbitrary<ResourceState> resource =
            ordered(disorder, Arbitraries.of(ResourceState.STANDARD, ResourceState.PADDED),
                    Arbitraries.of(ResourceState.MISSING, ResourceState.MALFORMED, ResourceState.NO_VERSION_KEY,
                                   ResourceState.FOREIGN_ARTIFACT_ID));
        Arbitrary<List<ExtraCopy>> extraCopies = Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(List.of())),
            Tuple.of(1, Combinators.combine(ordered(disorder, Arbitraries.just(release), otherVersions(release)),
                                            Arbitraries.of(true, false))
                                   .as(ExtraCopy::new).list().ofMinSize(1).ofMaxSize(2)));
        return Combinators.combine(placement,
                                   ordered(disorder, Arbitraries.just(release), otherVersions(release)),
                                   transitive, resource, extraCopies)
                          .as((p, pinned, uses, state, extras) -> new ModulePlan(artifactId, p, pinned, uses, state,
                                                                                 extras));
    }

    /** Third-party uses, possibly repeated at different depths and versions (nearest-wins, first-declared ties). */
    private static Arbitrary<List<ThirdPartyUse>> thirdPartyUses() {
        return Combinators.combine(Arbitraries.of(THIRD_PARTY_ARTIFACTS), Arbitraries.integers().between(2, 3),
                                   Arbitraries.of("1.0.0", "1.2.3", "2.0.0"))
                          .as(ThirdPartyUse::new).list().ofMaxSize(4);
    }

    private static Arbitrary<StaleBom> staleBom(String release) {
        List<String> v3Candidates = Stream.concat(Inventory.get().releaseArtifacts().stream(),
                                                  RETIRED_V3_ARTIFACTS.stream()).toList();
        return Combinators.combine(otherVersions(release),
                                   Arbitraries.subsetOf(v3Candidates).ofMaxSize(12),
                                   Arbitraries.subsetOf(THIRD_PARTY_ARTIFACTS).ofMaxSize(2))
                          .as((version, v3, thirdParty) -> {
                              Map<Key, String> managed = new TreeMap<>();
                              v3.forEach(a -> managed.put(Oracle.v3Key(a), version));
                              thirdParty.forEach(a -> managed.put(new Key(THIRD_PARTY_GROUP, a), "0.9.0"));
                              return new StaleBom(managed);
                          });
    }

    private static Arbitrary<Map<Key, String>> localManagement(List<String> onGraph, String release, int disorder) {
        return Combinators.combine(Arbitraries.of(onGraph),
                                   ordered(disorder, Arbitraries.just(release), otherVersions(release)))
                          .as((artifactId, version) -> Map.entry(Oracle.v3Key(artifactId), version))
                          .list().ofMaxSize(2).uniqueElements(Map.Entry::getKey)
                          .map(entries -> {
                              Map<Key, String> management = new TreeMap<>();
                              entries.forEach(e -> management.put(e.getKey(), e.getValue()));
                              return management;
                          });
    }

    /** The participating modules as a client passes them: arbitrary order, possibly with duplicates. */
    private static Arbitrary<List<String>> verifiedModules(Set<String> participating) {
        return Arbitraries.subsetOf(participating).flatMap(duplicates -> {
            List<String> modules = new ArrayList<>(new TreeSet<>(participating));
            modules.addAll(new TreeSet<>(duplicates));
            return Arbitraries.shuffle(modules);
        });
    }

    private static Arbitrary<String> releaseVersions() {
        return Combinators.combine(Arbitraries.integers().between(0, 40), Arbitraries.integers().between(0, 400),
                                   Arbitraries.of(false, true))
                          .as((minor, patch, snapshot) -> "3." + minor + "." + patch + (snapshot ? "-SNAPSHOT" : ""));
    }

    /** The release version or, more often, another version. */
    private static Arbitrary<String> anyVersion(String release) {
        return Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(release)), Tuple.of(2, otherVersions(release)));
    }

    /** Versions that differ from {@code release}, including near-identical strings that exact matching must reject. */
    private static Arbitrary<String> otherVersions(String release) {
        boolean snapshot = release.endsWith("-SNAPSHOT");
        String base = snapshot ? release.substring(0, release.length() - "-SNAPSHOT".length()) : release;
        String[] parts = base.split("\\.");
        String qualifier = snapshot ? "-SNAPSHOT" : "";
        Arbitrary<String> lookalikes = Arbitraries.of(
            snapshot ? base : release + "-SNAPSHOT",
            parts[0] + "." + parts[1] + ".0" + parts[2] + qualifier,
            snapshot ? base + "-snapshot" : release + ".0");
        Arbitrary<String> other3x = Combinators.combine(Arbitraries.integers().between(0, 40),
                                                        Arbitraries.integers().between(0, 400))
                                               .as((minor, patch) -> "3." + minor + "." + patch)
                                               .filter(v -> !v.equals(release));
        Arbitrary<String> v2 = Combinators.combine(Arbitraries.integers().between(17, 31),
                                                   Arbitraries.integers().between(0, 800))
                                          .as((minor, patch) -> "2." + minor + "." + patch);
        return Arbitraries.oneOf(lookalikes, other3x, v2);
    }

    /** {@code clean} normally, {@code dirty} for {@code disorder} percent of the choices. */
    private static <T> Arbitrary<T> ordered(int disorder, Arbitrary<T> clean, Arbitrary<T> dirty) {
        if (disorder == 0) {
            return clean;
        }
        return Arbitraries.frequencyOf(List.of(Tuple.of(100 - disorder, clean), Tuple.of(disorder, dirty)));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Inventory and file helpers
    // ---------------------------------------------------------------------------------------------------------------

    /** The dependency policy's release inventory and the raw public BOM, loaded once. */
    private static final class Inventory {
        private static final Inventory INSTANCE = load();

        private final String groupId;
        private final List<String> releaseArtifacts;
        private final byte[] bomPom;
        private final Path workDirectory;

        private Inventory(String groupId, List<String> releaseArtifacts, byte[] bomPom, Path workDirectory) {
            this.groupId = groupId;
            this.releaseArtifacts = releaseArtifacts;
            this.bomPom = bomPom;
            this.workDirectory = workDirectory;
        }

        static Inventory get() {
            return INSTANCE;
        }

        String groupId() {
            return groupId;
        }

        /** Sorted release artifactIds: policy modules with release="true" plus the reactor's generated services. */
        List<String> releaseArtifacts() {
            return releaseArtifacts;
        }

        byte[] bomPom() {
            return bomPom.clone();
        }

        Path workDirectory() {
            return workDirectory;
        }

        private static Inventory load() {
            try {
                V3Reactor reactor = V3Reactor.load();
                DependencyPolicy policy = reactor.policy();
                byte[] bom = Files.readAllBytes(reactor.root().resolve("bom/pom.xml"));
                Path work = Files.createDirectories(
                    Path.of(System.getProperty("v3.architectureTests.workDirectory",
                                               System.getProperty("java.io.tmpdir")),
                            "runtime-version-coherence"));
                List<String> release = List.copyOf(new TreeSet<>(policy.releaseArtifacts()));
                if (release.isEmpty()) {
                    throw new IllegalStateException("The dependency policy lists no release artifacts");
                }
                return new Inventory(policy.groupId(), release, bom, work);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static Document parseXml(byte[] xml) {
        try (InputStream in = new ByteArrayInputStream(xml)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(in);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse v3/bom/pom.xml: " + e.getMessage(), e);
        }
    }

    private static void writeXml(Document document, Path file) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(file.toFile()));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot write " + file + ": " + e.getMessage(), e);
        }
    }

    private static Optional<Element> onlyChild(Element parent, String name) {
        List<Element> matches = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && element.getTagName().equals(name)) {
                matches.add(element);
            }
        }
        assertTrue(matches.size() <= 1, "More than one <" + name + "> in <" + parent.getTagName() + ">");
        return matches.stream().findFirst();
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
