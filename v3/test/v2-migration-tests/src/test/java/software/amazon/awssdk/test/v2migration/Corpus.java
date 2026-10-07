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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import software.amazon.awssdk.tools.paritymanifest.CandidateInventory;
import software.amazon.awssdk.tools.paritymanifest.CandidateInventoryScanner;
import software.amazon.awssdk.tools.paritymanifest.CandidateTree;
import software.amazon.awssdk.tools.paritymanifest.InventorySource;
import software.amazon.awssdk.tools.paritymanifest.ParityManifestCli;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.MigrationDiagnostic;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.manifest.ManifestBaseline;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/**
 * The whole Migration Corpus, run once per test JVM: the real Parity Manifest of the V2 baseline, every corpus project
 * through {@link CorpusRun}, and the compatibility report published from the results
 * ({@code <output>/compatibility-report.json}, with each project's migration report under {@code reports/}).
 */
final class Corpus {
    static final Path V3_ROOT = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
    static final Path REPOSITORY = V3_ROOT.getParent();
    static final String CANDIDATE_VERSION = System.getProperty("v3.candidateVersion");
    static final String V2_VERSION = System.getProperty("v3.v2Baseline.version");
    static final String REPORT = "compatibility-report.json";

    private static Corpus instance;

    private final Path output;
    private final MigrationManifest manifest;
    private final List<MigrationRecipe> recipes;
    private final CandidateArtifacts candidate;
    private final SortedSet<String> bomArtifacts;
    private final List<CorpusRun> runs = new ArrayList<>();
    private CandidateInventory inventory;
    private CompatibilityReport report;

    private Corpus(Path output) throws Exception {
        this.output = output;
        this.manifest = MigrationManifest.read(generateManifest(output.resolve("parity-manifest.json")));
        this.recipes = Recipes.discover(MigrationEngine.class.getClassLoader());
        this.candidate = CandidateArtifacts.load(Path.of(System.getProperty("v3.corpus.candidate")));
        this.bomArtifacts = CandidateTree.bomArtifacts(V3_ROOT.resolve("bom/pom.xml"));
    }

    /** The corpus run of this JVM (run on first use). */
    static synchronized Corpus get() throws Exception {
        if (instance == null) {
            Path output = Path.of(System.getProperty("v3.corpus.output")).toAbsolutePath();
            CorpusRun.deleteRecursively(output);
            Files.createDirectories(output);
            Corpus corpus = new Corpus(output);
            for (CorpusProject project : CorpusProject.loadAll(Path.of(System.getProperty("v3.corpus")))) {
                corpus.runs.add(CorpusRun.run(project, corpus));
            }
            corpus.report = corpus.buildReport();
            Files.writeString(output.resolve(REPORT), corpus.report.toJson(), StandardCharsets.UTF_8);
            instance = corpus;
        }
        return instance;
    }

    Path output() {
        return output;
    }

    MigrationManifest manifest() {
        return manifest;
    }

    List<MigrationRecipe> recipes() {
        return recipes;
    }

    CandidateArtifacts candidate() {
        return candidate;
    }

    /** The {@code groupId:artifactId} of every release artifact the V3 BOM manages. */
    SortedSet<String> bomArtifacts() {
        return bomArtifacts;
    }

    List<CorpusRun> runs() {
        return runs;
    }

    CompatibilityReport report() {
        return report;
    }

    /**
     * True if a candidate artifact ships the manifest entry {@code id}: one of the artifacts this build resolved for
     * the corpus, or a built non-service release module of the V3 tree.
     */
    synchronized boolean ships(String id) throws IOException {
        if (inventory == null) {
            List<Path> roots = new ArrayList<>(candidate.files(bomArtifacts));
            SortedMap<String, Path> outputs = CandidateTree.moduleOutputs(V3_ROOT, bomArtifacts);
            outputs.forEach((coordinates, path) -> {
                // The artifacts this build resolved are scanned as resolved; generated services ship only their own
                // service packages, which no corpus construct outside the resolved services refers to.
                if (!candidate.has(coordinates) && !path.startsWith(V3_ROOT.resolve("services"))) {
                    roots.add(path);
                }
            });
            inventory = CandidateInventoryScanner.scan(roots, List.of());
        }
        return inventory.ships(id);
    }

    /** Generates the Parity Manifest of the V2 baseline as the release pipeline does, with every inventory source. */
    private static Path generateManifest(Path output) throws IOException {
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(log, true, StandardCharsets.UTF_8);
        int status = ParityManifestCli.run(new String[] {
            "--repository", REPOSITORY.toString(), "--output", output.toString(), "--api-version", V2_VERSION,
            "--maven-repository", System.getProperty("v3.localRepository"), "--candidate-version", CANDIDATE_VERSION},
                                           ServiceLoader.load(InventorySource.class).stream()
                                                        .map(ServiceLoader.Provider::get).toList(), out, out);
        Files.writeString(output.resolveSibling("parity-manifest.log"), log.toString(StandardCharsets.UTF_8));
        if (status != 0) {
            throw new AssertionError("Parity Manifest generation failed (" + status + "):\n"
                                     + log.toString(StandardCharsets.UTF_8));
        }
        return output;
    }

    private CompatibilityReport buildReport() {
        ManifestBaseline b = manifest.baseline();
        Map<String, Integer> classifications = new LinkedHashMap<>();
        manifest.summary().forEach((c, n) -> classifications.put(c.name(), n));
        Map<String, int[]> byRecipe = new LinkedHashMap<>();
        recipes.forEach(r -> byRecipe.put(r.id(), new int[3]));
        List<CompatibilityReport.CorpusProject> projects = new ArrayList<>();
        for (CorpusRun run : runs) {
            for (ItemResult r : run.migration().items()) {
                if (r.recipe() != null) {
                    byRecipe.computeIfAbsent(r.recipe(), k -> new int[3])[r.disposition().ordinal()]++;
                }
            }
            projects.add(new CompatibilityReport.CorpusProject(
                run.project().name(), run.project().kind(), run.project().scenarios(), run.project().description(),
                migration(run), run.baseline(), run.compile(), run.behavior(), CorpusRun.reportPath(run.project())));
        }
        List<CompatibilityReport.Recipe> recipeRows = new ArrayList<>();
        for (MigrationRecipe r : recipes) {
            int[] n = byRecipe.get(r.id());
            recipeRows.add(new CompatibilityReport.Recipe(r.id(), r.description(), n[0], n[1], n[2]));
        }
        return new CompatibilityReport(
            new CompatibilityReport.Candidate(CANDIDATE_VERSION, b.candidateRevision() != null ? b.candidateRevision()
                                                                                                : b.gitRevision()),
            new CompatibilityReport.Baseline(b.v2Version(), V2_VERSION, b.gitRevision(), b.scannerVersion()),
            new CompatibilityReport.Manifest(manifest.size(), classifications), recipeRows, projects);
    }

    private static CompatibilityReport.Migration migration(CorpusRun run) {
        List<ItemResult> items = run.migration().items();
        Map<String, Integer> dispositions = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> categories = new TreeMap<>();
        Map<String, Integer> classifications = new LinkedHashMap<>();
        for (MigrationCategory c : MigrationCategory.values()) {
            categories.put(c.name(), new LinkedHashMap<>());
        }
        List<CompatibilityReport.Diagnostic> unresolved = new ArrayList<>();
        for (ItemResult r : items) {
            dispositions.merge(r.disposition().name(), 1, Integer::sum);
            categories.get(r.item().category().name()).merge(r.disposition().name(), 1, Integer::sum);
            classifications.merge(r.entry() == null ? CompatibilityReport.UNCLASSIFIED
                                                    : r.entry().classification().name(), 1, Integer::sum);
            if (r.disposition() == Disposition.UNRESOLVED) {
                MigrationDiagnostic d = r.diagnostic();
                unresolved.add(new CompatibilityReport.Diagnostic(
                    d.location().path(), d.location().line(), d.location().column(), d.construct(),
                    r.item().category().name(), d.entryId(), d.classification() == null ? null
                                                                                        : d.classification().name(),
                    d.replacement(), d.documentationId(), r.recipe(), d.reason()));
            }
        }
        return new CompatibilityReport.Migration(items.size(), dispositions, categories, classifications,
                                                 List.copyOf(run.migration().changed().keySet()), unresolved);
    }
}
