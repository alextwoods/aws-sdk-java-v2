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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.Status;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.MigrationDiagnostic;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The V2 Migration Corpus gate (task 15.5; Requirements 17.3, 17.7-17.12): every corpus project migrated with the real
 * Parity Manifest, its V2 baseline and its transformed project compiled and run, and the compatibility report
 * published. See {@link Corpus} and {@link CorpusRun}; work files and reports are under {@code -Dv3.corpus.output}.
 */
class MigrationCorpusTest {
    /** The migration concerns of Requirement 17.11 that the corpus must encounter. */
    private static final Set<MigrationCategory> REQUIRED_CATEGORIES = EnumSet.of(
        MigrationCategory.DEPENDENCY, MigrationCategory.BOM, MigrationCategory.BUILD, MigrationCategory.IMPORT,
        MigrationCategory.BUILDER, MigrationCategory.CLIENT, MigrationCategory.CONFIGURATION,
        MigrationCategory.INTERCEPTOR, MigrationCategory.HTTP, MigrationCategory.RETRY, MigrationCategory.SIGNER,
        MigrationCategory.STREAMING, MigrationCategory.HIGH_LEVEL_LIBRARY);

    private static Corpus corpus;

    @BeforeAll
    static void runCorpus() throws Exception {
        corpus = Corpus.get();
        System.out.println(summary());
    }

    /** Requirements 17.10, 17.11: one disposition per encountered item, consistent with its edits and diagnostic. */
    @Test
    void everyEncounteredItemHasExactlyOneDisposition() throws Exception {
        List<String> problems = new ArrayList<>();
        Set<MigrationCategory> encountered = EnumSet.noneOf(MigrationCategory.class);
        for (CorpusRun run : corpus.runs()) {
            String at = run.project().name() + ": ";
            Set<MigrationItem> seen = new HashSet<>();
            for (ItemResult r : run.migration().items()) {
                encountered.add(r.item().category());
                if (!seen.add(r.item())) {
                    problems.add(at + "two dispositions for " + r.item());
                }
                boolean edits = !r.edits().isEmpty();
                boolean diagnostic = r.diagnostic() != null;
                if (edits != (r.disposition() == Disposition.TRANSFORMED)
                    || diagnostic != (r.disposition() == Disposition.UNRESOLVED)) {
                    problems.add(at + r.disposition() + " with edits=" + edits + " diagnostic=" + diagnostic + ": "
                                 + r.item());
                }
                ManifestEntry entry = r.entry();
                if (entry == null && r.disposition() == Disposition.PRESERVED && r.recipe() == null) {
                    problems.add(at + "preserved without a manifest entry or a deciding recipe: " + r.item());
                }
                if (entry != null && r.item().id() != null && !entry.id().equals(r.item().id())) {
                    problems.add(at + "joined to " + entry.id() + " but identified as " + r.item().id());
                }
                if (entry != null && entry.classification() == Classification.EXCLUDED
                    && r.disposition() != Disposition.UNRESOLVED) {
                    problems.add(at + "an EXCLUDED construct must stay unresolved: " + r.item());
                }
            }
            // The published migration report records exactly these dispositions.
            ObjectNode report = Node.parse(Files.readString(corpus.output().resolve(CorpusRun.reportPath(run.project())),
                                                            StandardCharsets.UTF_8)).expectObjectNode();
            List<String> reported = report.expectArrayMember("items").getElements().stream()
                                          .map(n -> n.expectObjectNode().expectStringMember("disposition").getValue())
                                          .toList();
            List<String> actual = run.migration().items().stream().map(r -> r.disposition().name()).toList();
            if (!reported.equals(actual)) {
                problems.add(at + "the migration report's dispositions differ from the engine's");
            }
        }
        assertEquals(List.of(), problems);
        Set<MigrationCategory> missing = EnumSet.copyOf(REQUIRED_CATEGORIES);
        missing.removeAll(encountered);
        assertEquals(Set.of(), missing, "migration concerns no corpus project encounters");
    }

    /**
     * Requirement 17.9: an unresolved construct is left byte-for-byte unchanged and its diagnostic names the file,
     * line, construct, classification rationale and the V3 replacement when the manifest records one.
     */
    @Test
    void unresolvedConstructsAreUnchangedAndFullyDiagnosed() throws Exception {
        List<String> problems = new ArrayList<>();
        for (CorpusRun run : corpus.runs()) {
            String at = run.project().name() + ": ";
            List<TextEdit> edits = run.migration().items().stream().flatMap(r -> r.edits().stream()).toList();
            for (ItemResult r : run.migration().items()) {
                if (r.disposition() != Disposition.UNRESOLVED) {
                    continue;
                }
                MigrationDiagnostic d = r.diagnostic();
                var location = r.item().location();
                for (TextEdit e : edits) {
                    if (location.overlaps(e.path(), e.startOffset(), e.endOffset())) {
                        problems.add(at + "an edit changes the unresolved construct " + r.item());
                    }
                }
                String before = run.migration().originals().get(location.path());
                String after = Files.readString(run.migrated().resolve(location.path()), StandardCharsets.UTF_8);
                if (before != null && !location.isPosition()) {
                    String original = before.substring(location.startOffset(), location.endOffset());
                    int shift = shift(edits, location.path(), location.startOffset());
                    int start = location.startOffset() + shift;
                    if (start < 0 || start + original.length() > after.length()
                        || !after.substring(start, start + original.length()).equals(original)) {
                        problems.add(at + "the unresolved construct changed in the written file: " + r.item());
                    }
                }
                if (d.location().line() < 1 || d.construct().isBlank() || d.reason().isBlank()) {
                    problems.add(at + "incomplete diagnostic " + d);
                }
                ManifestEntry entry = r.entry();
                if (entry != null) {
                    if (d.classification() != entry.classification() || d.rationale() == null) {
                        problems.add(at + "the diagnostic lacks the classification rationale of " + entry.id());
                    }
                    if (entry.replacement() != null && !entry.replacement().equals(d.replacement())) {
                        problems.add(at + "the diagnostic of " + entry.id() + " does not name its replacement "
                                     + entry.replacement());
                    }
                    if (entry.classification() != Classification.PRESERVED && d.documentationId() == null) {
                        problems.add(at + "the diagnostic of " + entry.id() + " has no documentation ID");
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    /** The declared expectations of every project (corpus-project.json), including the Java level (17.7). */
    @Test
    void everyProjectMeetsItsDeclaredExpectations() throws Exception {
        List<String> problems = new ArrayList<>();
        for (CorpusRun run : corpus.runs()) {
            String at = run.project().name() + ": ";
            CorpusProject.Expectations e = run.project().expect();
            if (e.cleanMigration() && run.migration().hasUnresolved()) {
                problems.add(at + "expected a clean migration but " + run.migration().count(Disposition.UNRESOLVED)
                             + " items are unresolved: " + CorpusRun.strings(run.migration().diagnostics().stream()
                                                                               .map(MigrationDiagnostic::message)
                                                                               .toList()));
            }
            if (e.javaSourcesUnchanged() && !run.changedJavaFiles().isEmpty()) {
                problems.add(at + "expected unchanged Java sources but the migration changed "
                             + run.changedJavaFiles());
            }
            if (run.migratedPom().javaRelease() != e.javaRelease()) {
                problems.add(at + "the migrated POM compiles for Java " + run.migratedPom().javaRelease() + ", not "
                             + e.javaRelease());
            }
            for (String id : e.transformed()) {
                boolean transformed = run.migration().items().stream()
                                                   .anyMatch(r -> id.equals(r.item().id())
                                                                  && r.disposition() == Disposition.TRANSFORMED);
                if (!transformed) {
                    problems.add(at + "no transformed item for " + id);
                }
            }
            for (CorpusProject.Unresolved u : e.unresolved()) {
                boolean found = run.migration().items().stream().anyMatch(
                    r -> r.disposition() == Disposition.UNRESOLVED && r.item().id() != null
                         && r.item().id().startsWith(u.entryIdPrefix())
                         && u.replacement().equals(r.diagnostic().replacement()));
                if (!found) {
                    problems.add(at + "no unresolved item " + u.entryIdPrefix() + "* naming " + u.replacement());
                }
            }
            if (e.compiles() && (run.compile().status() != Status.PASSED || run.behavior().status() != Status.PASSED)) {
                problems.add(at + "must compile and pass its behavioral suite on the candidate, but compile is "
                             + run.compile().status() + " (" + run.compile().detail() + ") and behavior is "
                             + run.behavior().status() + " (" + run.behavior().detail() + "): " + run.compile().failures()
                             + run.behavior().failures());
            }
        }
        assertEquals(List.of(), problems);
    }

    /** Every unmigrated project compiles and prints its expected transcript on the V2 baseline. */
    @Test
    void everyProjectCompilesAndBehavesOnTheV2Baseline() {
        List<String> problems = new ArrayList<>();
        for (CorpusRun run : corpus.runs()) {
            if (run.baseline().status() != Status.PASSED) {
                problems.add(run.project().name() + ": " + run.baseline().detail() + " " + run.baseline().failures());
            }
        }
        assertEquals(List.of(), problems);
    }

    /**
     * Requirements 17.3, 17.7, 17.8: a transformed project without unresolved diagnostics compiles as Java 21 against
     * the candidate and reproduces the V2 behavior. A failure caused only by entries the candidate does not ship blocks
     * the launch gate stages ({@code -Dv3.launchGate}); every other failure blocks every build.
     */
    @Test
    void transformedProjectsCompileAsJava21AndBehaveLikeTheV2Baseline() {
        boolean launchGate = !System.getProperty("v3.launchGate", "").isBlank();
        assertEquals(List.of(), corpus.report().blockingResults(launchGate));
    }

    /** Requirement 17.12: the machine-readable compatibility report covers the whole corpus run. */
    @Test
    void compatibilityReportIsPublished() throws Exception {
        CompatibilityReport report = corpus.report();
        assertEquals(List.of(), report.problems());
        Path file = corpus.output().resolve(Corpus.REPORT);
        assertEquals(report.toJson(), Files.readString(file, StandardCharsets.UTF_8));
        ObjectNode json = Node.parse(Files.readString(file, StandardCharsets.UTF_8)).expectObjectNode();
        assertEquals(List.of("schemaVersion", "candidate", "baseline", "manifest", "summary", "recipes", "corpus"),
                     json.getMembers().keySet().stream().map(Object::toString).toList());
        assertEquals(corpus.manifest().size(), json.expectObjectMember("manifest").expectNumberMember("entries")
                                                   .getValue().intValue());
        assertEquals(corpus.recipes().stream().map(r -> r.id()).toList(),
                     report.recipes().stream().map(CompatibilityReport.Recipe::id).toList());
        assertEquals(corpus.runs().size(), report.corpus().size());
        for (CompatibilityReport.CorpusProject p : report.corpus()) {
            assertTrue(Files.isRegularFile(file.resolveSibling(p.migrationReport())), p.migrationReport());
        }
        int unresolved = corpus.runs().stream().mapToInt(r -> (int) r.migration().count(Disposition.UNRESOLVED)).sum();
        assertEquals(unresolved, report.corpus().stream().mapToInt(p -> p.migration().unresolved().size()).sum());
        assertTrue(CompatibilityReport.class.getResource(CompatibilityReport.SCHEMA_RESOURCE) != null);
    }

    /** Requirement 17.3: a preserved-only project's migration changes nothing but its build. */
    @Test
    void preservedOnlyProjectsChangeOnlyTheirBuild() {
        List<String> problems = new ArrayList<>();
        for (CorpusRun run : corpus.runs()) {
            if (!run.project().scenarios().equals(List.of("PRESERVED_ONLY"))) {
                continue;
            }
            for (ItemResult r : run.migration().items()) {
                MigrationCategory c = r.item().category();
                boolean build = c == MigrationCategory.DEPENDENCY || c == MigrationCategory.BOM
                                || c == MigrationCategory.BUILD;
                if (r.disposition() == Disposition.TRANSFORMED && !build) {
                    problems.add(run.project().name() + ": a preserved-only project transforms " + r.item());
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    private static int shift(List<TextEdit> edits, String path, int offset) {
        int shift = 0;
        for (TextEdit e : edits) {
            if (e.path().equals(path) && e.endOffset() <= offset) {
                shift += e.replacement().length() - (e.endOffset() - e.startOffset());
            }
        }
        return shift;
    }

    private static String summary() {
        StringBuilder out = new StringBuilder("V2 Migration Corpus (" + corpus.output().resolve(Corpus.REPORT) + "):\n");
        Map<String, Integer> totals = new TreeMap<>();
        for (CorpusRun run : corpus.runs()) {
            Map<String, Long> d = run.migration().items().stream()
                                               .collect(Collectors.groupingBy(r -> r.disposition().name(),
                                                                              TreeMap::new, Collectors.counting()));
            d.forEach((k, v) -> totals.merge(k, v.intValue(), Integer::sum));
            out.append(String.format("  %-22s items=%-4d %s baseline=%s compile=%s behavior=%s%n", run.project().name(),
                                     run.migration().items().size(), d, run.baseline().status(), run.compile().status(),
                                     run.behavior().status()));
        }
        out.append("  totals ").append(totals).append('\n');
        return out.toString();
    }
}
