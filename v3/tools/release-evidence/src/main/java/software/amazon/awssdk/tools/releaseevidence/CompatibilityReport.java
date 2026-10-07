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

package software.amazon.awssdk.tools.releaseevidence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The machine-readable V2 compatibility report of one release candidate (Requirement 17.12; {@link #SCHEMA_RESOURCE}):
 * the Compatibility Classification counts of the Parity Manifest the candidate was measured against, every migration
 * recipe with its dispositions across the Migration Corpus, and per corpus project its dispositions, its unresolved
 * diagnostics, and the results of compiling and running the transformed project.
 *
 * <p>The JSON is byte-reproducible for identical input: fixed member order, projects in the given order, maps with every
 * key present, UTF-8, four-space indentation and a trailing newline, and no timestamps. {@link #problems()} lists every
 * internal inconsistency; a report with problems must not be published.
 */
public record CompatibilityReport(Candidate candidate,
                                  Baseline baseline,
                                  Manifest manifest,
                                  List<Recipe> recipes,
                                  List<CorpusProject> corpus) {

    /** The classpath location of the report JSON schema. */
    public static final String SCHEMA_RESOURCE =
        "/software/amazon/awssdk/tools/releaseevidence/compatibility-report-1.schema.json";
    /** The report schema version. */
    public static final int SCHEMA_VERSION = 1;
    /** The Compatibility Classifications, in report order. */
    public static final List<String> CLASSIFICATIONS = List.of("PRESERVED", "ADAPTED", "EXCLUDED");
    /** The migration dispositions, in report order. */
    public static final List<String> DISPOSITIONS = List.of("PRESERVED", "TRANSFORMED", "UNRESOLVED");
    /** The {@code itemClassifications} key of items without a manifest entry. */
    public static final String UNCLASSIFIED = "UNCLASSIFIED";

    public CompatibilityReport {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(manifest, "manifest");
        recipes = List.copyOf(recipes);
        corpus = List.copyOf(corpus);
    }

    /** The release candidate the corpus ran against. */
    public record Candidate(String version, String gitRevision) {
        public Candidate {
            Objects.requireNonNull(version, "version");
        }
    }

    /** The Launch Parity Baseline: the V2 release whose APIs the manifest inventories. */
    public record Baseline(String v2Version, String apiVersion, String gitRevision, String scannerVersion) {
        public Baseline {
            Objects.requireNonNull(v2Version, "v2Version");
            Objects.requireNonNull(apiVersion, "apiVersion");
        }
    }

    /** The Parity Manifest's size and entries per Compatibility Classification. */
    public record Manifest(int entries, Map<String, Integer> classifications) {
        public Manifest {
            classifications = ordered(classifications, CLASSIFICATIONS);
        }
    }

    /** One migration recipe and the dispositions it decided across the corpus. */
    public record Recipe(String id, String description, int preserved, int transformed, int unresolved) {
        public Recipe {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(description, "description");
        }
    }

    /** The outcome of compiling or running a transformed corpus project. */
    public enum Status {
        /** The step ran and succeeded. */
        PASSED,
        /** The step ran and failed for a reason the tooling or the corpus owns. */
        FAILED,
        /**
         * The step ran and failed only on manifest entries that the candidate does not ship (a launch-parity gap of the
         * candidate, which the launch gate blocks), each listed in {@link StepResult#blockedBy()}.
         */
        BLOCKED_BY_CANDIDATE,
        /** The step did not run: the migration left unresolved diagnostics, or the step it depends on did not pass. */
        NOT_RUN
    }

    /**
     * The result of one step.
     *
     * @param status    the outcome
     * @param detail    a one-line explanation
     * @param blockedBy the manifest entry IDs the candidate does not ship ({@link Status#BLOCKED_BY_CANDIDATE} only)
     * @param failures  the individual failures ({@link Status#FAILED} and {@link Status#BLOCKED_BY_CANDIDATE})
     */
    public record StepResult(Status status, String detail, List<String> blockedBy, List<String> failures) {
        public StepResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(detail, "detail");
            blockedBy = List.copyOf(blockedBy);
            failures = List.copyOf(failures);
        }

        public static StepResult passed(String detail) {
            return new StepResult(Status.PASSED, detail, List.of(), List.of());
        }

        public static StepResult notRun(String detail) {
            return new StepResult(Status.NOT_RUN, detail, List.of(), List.of());
        }

        public static StepResult failed(String detail, List<String> failures) {
            return new StepResult(Status.FAILED, detail, List.of(), failures);
        }

        public static StepResult blocked(String detail, List<String> blockedBy, List<String> failures) {
            return new StepResult(Status.BLOCKED_BY_CANDIDATE, detail, blockedBy, failures);
        }
    }

    /**
     * One unresolved diagnostic of a corpus project (Requirement 17.9).
     *
     * @param path            the project-relative file
     * @param line            the 1-based line of the unchanged construct
     * @param column          the 1-based column
     * @param construct       the affected construct
     * @param category        the migration category
     * @param entryId         the Parity Manifest entry ID, or {@code null}
     * @param classification  the entry's Compatibility Classification, or {@code null} without an entry
     * @param replacement     the V3 replacement entry ID, or {@code null} when none exists
     * @param documentationId the documentation identifier, or {@code null}
     * @param recipe          the recipe that declined the construct, or {@code null} when the manifest decided
     * @param reason          why the construct was left unchanged
     */
    public record Diagnostic(String path, int line, int column, String construct, String category, String entryId,
                             String classification, String replacement, String documentationId, String recipe,
                             String reason) {
        public Diagnostic {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(construct, "construct");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * The migration of one corpus project.
     *
     * @param items               the number of encountered items
     * @param dispositions        items per disposition
     * @param categories          items per migration category and disposition
     * @param itemClassifications items per Compatibility Classification of their manifest entry (and
     *                            {@link #UNCLASSIFIED})
     * @param changedFiles        the project-relative files the migration changed
     * @param unresolved          the diagnostic of every unresolved item, in item order
     */
    public record Migration(int items, Map<String, Integer> dispositions, Map<String, Map<String, Integer>> categories,
                            Map<String, Integer> itemClassifications, List<String> changedFiles,
                            List<Diagnostic> unresolved) {
        public Migration {
            dispositions = ordered(dispositions, DISPOSITIONS);
            Map<String, Map<String, Integer>> byCategory = new TreeMap<>();
            categories.forEach((c, counts) -> byCategory.put(c, ordered(counts, DISPOSITIONS)));
            categories = Collections.unmodifiableMap(byCategory);
            List<String> keys = new ArrayList<>(CLASSIFICATIONS);
            keys.add(UNCLASSIFIED);
            itemClassifications = ordered(itemClassifications, keys);
            changedFiles = List.copyOf(changedFiles);
            unresolved = List.copyOf(unresolved);
        }
    }

    /**
     * One Migration Corpus project.
     *
     * @param name            the project directory name
     * @param kind            {@code REPRESENTATIVE} or {@code SYNTHETIC}
     * @param scenarios       what the project exercises ({@code PRESERVED_ONLY}, {@code ADAPTED}, {@code EXCLUDED})
     * @param description     what the project is
     * @param migration       its migration
     * @param baseline        compiling and running the unmigrated project against the V2 baseline
     * @param compile         compiling the transformed project against the candidate (Requirement 17.7)
     * @param behavior        running the transformed project's behavioral suite (Requirement 17.8)
     * @param migrationReport the path of the project's migration report, relative to this report
     */
    public record CorpusProject(String name, String kind, List<String> scenarios, String description,
                                Migration migration, StepResult baseline, StepResult compile, StepResult behavior,
                                String migrationReport) {
        public CorpusProject {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
            scenarios = List.copyOf(scenarios);
            Objects.requireNonNull(migration, "migration");
            Objects.requireNonNull(baseline, "baseline");
            Objects.requireNonNull(compile, "compile");
            Objects.requireNonNull(behavior, "behavior");
        }

        /** The number of unresolved items. */
        public int unresolved() {
            return migration.dispositions().get("UNRESOLVED");
        }
    }

    /**
     * Every inconsistency of this report: counts that do not add up, a step result that contradicts the migration (a
     * transformed project compiles exactly when the migration left no unresolved diagnostic, Requirement 17.7, and its
     * behavioral suite runs exactly when it compiled, 17.8), a failed or blocked step without its causes, and an
     * incomplete diagnostic (17.9).
     */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (!manifest.classifications().keySet().equals(new HashSet<>(CLASSIFICATIONS))) {
            problems.add("manifest: classifications must be exactly " + CLASSIFICATIONS);
        }
        int classified = manifest.classifications().values().stream().mapToInt(Integer::intValue).sum();
        if (classified != manifest.entries()) {
            problems.add("manifest: " + manifest.entries() + " entries but " + classified + " classified");
        }
        Set<String> recipeIds = new HashSet<>();
        for (Recipe r : recipes) {
            if (!recipeIds.add(r.id())) {
                problems.add("recipe " + r.id() + ": listed twice");
            }
        }
        Set<String> names = new HashSet<>();
        if (corpus.isEmpty()) {
            problems.add("corpus: no project");
        }
        for (CorpusProject p : corpus) {
            String at = "project " + p.name() + ": ";
            if (!names.add(p.name())) {
                problems.add(at + "listed twice");
            }
            Migration m = p.migration();
            check(problems, at + "dispositions", m.dispositions(), m.items());
            check(problems, at + "itemClassifications", m.itemClassifications(), m.items());
            int byCategory = m.categories().values().stream()
                              .mapToInt(c -> c.values().stream().mapToInt(Integer::intValue).sum()).sum();
            if (byCategory != m.items()) {
                problems.add(at + "categories count " + byCategory + " items, not " + m.items());
            }
            if (m.unresolved().size() != p.unresolved()) {
                problems.add(at + m.unresolved().size() + " diagnostics for " + p.unresolved() + " unresolved items");
            }
            for (Diagnostic d : m.unresolved()) {
                if (d.path().isBlank() || d.line() < 1 || d.column() < 1 || d.construct().isBlank()
                    || d.reason().isBlank()) {
                    problems.add(at + "incomplete diagnostic " + d);
                }
            }
            checkStep(problems, at + "baseline", p.baseline());
            checkStep(problems, at + "compile", p.compile());
            checkStep(problems, at + "behavior", p.behavior());
            if (p.unresolved() > 0 && p.compile().status() != Status.NOT_RUN) {
                problems.add(at + "compile must be NOT_RUN with " + p.unresolved() + " unresolved items");
            }
            if (p.unresolved() == 0 && p.compile().status() == Status.NOT_RUN) {
                problems.add(at + "compile must run: the migration left no unresolved item");
            }
            if ((p.compile().status() == Status.PASSED) != (p.behavior().status() != Status.NOT_RUN)) {
                problems.add(at + "behavior must run exactly when compile passed");
            }
            if (p.behavior().status() == Status.BLOCKED_BY_CANDIDATE) {
                problems.add(at + "behavior cannot be BLOCKED_BY_CANDIDATE");
            }
        }
        return problems;
    }

    private static void check(List<String> problems, String what, Map<String, Integer> counts, int items) {
        int sum = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != items) {
            problems.add(what + " count " + sum + " items, not " + items);
        }
    }

    private static void checkStep(List<String> problems, String what, StepResult step) {
        if (step.status() == Status.BLOCKED_BY_CANDIDATE && step.blockedBy().isEmpty()) {
            problems.add(what + ": BLOCKED_BY_CANDIDATE without the entries the candidate does not ship");
        }
        if ((step.status() == Status.FAILED || step.status() == Status.BLOCKED_BY_CANDIDATE)
            && step.failures().isEmpty()) {
            problems.add(what + ": " + step.status() + " without failures");
        }
        if ((step.status() == Status.PASSED || step.status() == Status.NOT_RUN)
            && (!step.failures().isEmpty() || !step.blockedBy().isEmpty())) {
            problems.add(what + ": " + step.status() + " with failures");
        }
    }

    /**
     * The results that block a release candidate: every {@code FAILED} step, and with {@code requireCandidateCoverage}
     * (the packaging and general-availability launch gate stages) every {@code BLOCKED_BY_CANDIDATE} step.
     */
    public List<String> blockingResults(boolean requireCandidateCoverage) {
        List<String> blocking = new ArrayList<>();
        for (CorpusProject p : corpus) {
            Map<String, StepResult> steps = new LinkedHashMap<>();
            steps.put("baseline", p.baseline());
            steps.put("compile", p.compile());
            steps.put("behavior", p.behavior());
            steps.forEach((name, step) -> {
                if (step.status() == Status.FAILED
                    || (requireCandidateCoverage && step.status() == Status.BLOCKED_BY_CANDIDATE)) {
                    blocking.add(p.name() + " " + name + " " + step.status() + ": " + step.detail()
                                 + (step.blockedBy().isEmpty() ? "" : " " + step.blockedBy()));
                }
            });
        }
        return blocking;
    }

    /** The report as JSON. */
    public String toJson() {
        ObjectNode.Builder top = Node.objectNodeBuilder()
                                     .withMember("schemaVersion", SCHEMA_VERSION)
                                     .withMember("candidate", Node.objectNodeBuilder()
                                                                  .withMember("version", candidate.version())
                                                                  .withMember("gitRevision",
                                                                              nullable(candidate.gitRevision()))
                                                                  .build())
                                     .withMember("baseline", Node.objectNodeBuilder()
                                                                 .withMember("v2Version", baseline.v2Version())
                                                                 .withMember("apiVersion", baseline.apiVersion())
                                                                 .withMember("gitRevision",
                                                                             nullable(baseline.gitRevision()))
                                                                 .withMember("scannerVersion",
                                                                             nullable(baseline.scannerVersion()))
                                                                 .build())
                                     .withMember("manifest", Node.objectNodeBuilder()
                                                                 .withMember("entries", manifest.entries())
                                                                 .withMember("classifications",
                                                                             counts(manifest.classifications()))
                                                                 .build())
                                     .withMember("summary", summary())
                                     .withMember("recipes", recipes.stream().map(CompatibilityReport::recipe)
                                                                   .collect(ArrayNode.collect()))
                                     .withMember("corpus", corpus.stream().map(CompatibilityReport::project)
                                                                 .collect(ArrayNode.collect()));
        return Node.prettyPrintJson(top.build()) + "\n";
    }

    private ObjectNode summary() {
        Map<String, Integer> dispositions = zero(DISPOSITIONS);
        Map<String, Integer> compile = zero(statusNames());
        Map<String, Integer> behavior = zero(statusNames());
        Map<String, Integer> unresolvedByClassification = new TreeMap<>();
        Map<String, Integer> unresolvedByRecipe = new TreeMap<>();
        Map<String, Integer> unresolvedByEntry = new TreeMap<>();
        for (CorpusProject p : corpus) {
            p.migration().dispositions().forEach((d, n) -> dispositions.merge(d, n, Integer::sum));
            compile.merge(p.compile().status().name(), 1, Integer::sum);
            behavior.merge(p.behavior().status().name(), 1, Integer::sum);
            for (Diagnostic d : p.migration().unresolved()) {
                unresolvedByClassification.merge(d.classification() == null ? UNCLASSIFIED : d.classification(), 1,
                                                 Integer::sum);
                unresolvedByRecipe.merge(d.recipe() == null ? "manifest" : d.recipe(), 1, Integer::sum);
                unresolvedByEntry.merge(d.entryId() == null ? "(unidentified)" : d.entryId(), 1, Integer::sum);
            }
        }
        return Node.objectNodeBuilder()
                   .withMember("projects", corpus.size())
                   .withMember("dispositions", counts(dispositions))
                   .withMember("compile", counts(compile))
                   .withMember("behavior", counts(behavior))
                   .withMember("unresolvedByClassification", counts(unresolvedByClassification))
                   .withMember("unresolvedByRecipe", counts(unresolvedByRecipe))
                   .withMember("unresolvedByEntry", counts(unresolvedByEntry))
                   .build();
    }

    private static ObjectNode recipe(Recipe r) {
        return Node.objectNodeBuilder()
                   .withMember("id", r.id())
                   .withMember("description", r.description())
                   .withMember("preserved", r.preserved())
                   .withMember("transformed", r.transformed())
                   .withMember("unresolved", r.unresolved())
                   .build();
    }

    private static ObjectNode project(CorpusProject p) {
        Migration m = p.migration();
        ObjectNode.Builder categories = Node.objectNodeBuilder();
        m.categories().forEach((c, counts) -> categories.withMember(c, counts(counts)));
        return Node.objectNodeBuilder()
                   .withMember("name", p.name())
                   .withMember("kind", p.kind())
                   .withMember("scenarios", Node.fromStrings(p.scenarios()))
                   .withMember("description", nullable(p.description()))
                   .withMember("migration", Node.objectNodeBuilder()
                                                .withMember("items", m.items())
                                                .withMember("dispositions", counts(m.dispositions()))
                                                .withMember("categories", categories.build())
                                                .withMember("itemClassifications", counts(m.itemClassifications()))
                                                .withMember("changedFiles", Node.fromStrings(m.changedFiles()))
                                                .withMember("unresolved", m.unresolved().stream()
                                                                           .map(CompatibilityReport::diagnostic)
                                                                           .collect(ArrayNode.collect()))
                                                .build())
                   .withMember("baseline", step(p.baseline()))
                   .withMember("compile", step(p.compile()))
                   .withMember("behavior", step(p.behavior()))
                   .withMember("migrationReport", nullable(p.migrationReport()))
                   .build();
    }

    private static ObjectNode diagnostic(Diagnostic d) {
        return Node.objectNodeBuilder()
                   .withMember("path", d.path())
                   .withMember("line", d.line())
                   .withMember("column", d.column())
                   .withMember("construct", d.construct())
                   .withMember("category", d.category())
                   .withMember("entryId", nullable(d.entryId()))
                   .withMember("classification", nullable(d.classification()))
                   .withMember("replacement", nullable(d.replacement()))
                   .withMember("documentationId", nullable(d.documentationId()))
                   .withMember("recipe", nullable(d.recipe()))
                   .withMember("reason", d.reason())
                   .build();
    }

    private static ObjectNode step(StepResult s) {
        return Node.objectNodeBuilder()
                   .withMember("status", s.status().name())
                   .withMember("detail", s.detail())
                   .withMember("blockedBy", Node.fromStrings(s.blockedBy()))
                   .withMember("failures", Node.fromStrings(s.failures()))
                   .build();
    }

    private static ObjectNode counts(Map<String, Integer> counts) {
        ObjectNode.Builder out = Node.objectNodeBuilder();
        counts.forEach(out::withMember);
        return out.build();
    }

    private static Node nullable(String value) {
        return value == null ? Node.nullNode() : Node.from(value);
    }

    private static List<String> statusNames() {
        List<String> names = new ArrayList<>();
        for (Status s : Status.values()) {
            names.add(s.name());
        }
        return names;
    }

    private static Map<String, Integer> zero(List<String> keys) {
        Map<String, Integer> out = new LinkedHashMap<>();
        keys.forEach(k -> out.put(k, 0));
        return out;
    }

    /** {@code counts} with exactly {@code keys}, in that order; a missing key counts 0, an unknown key is rejected. */
    private static Map<String, Integer> ordered(Map<String, Integer> counts, List<String> keys) {
        for (String key : counts.keySet()) {
            if (!keys.contains(key)) {
                throw new IllegalArgumentException("Unknown key " + key + "; expected one of " + keys);
            }
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        keys.forEach(k -> out.put(k, counts.getOrDefault(k, 0)));
        return Collections.unmodifiableMap(out);
    }
}
