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

package software.amazon.awssdk.tools.paritymanifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * The high-level-library parity matrix (task 14.6; Requirements 16.6-16.8, 16.12, 16.14, 16.15, 19.7-19.9, 19.12):
 * for every Parity Manifest entry a high-level library owns ({@code high-level-library-matrix.json}), the evidence
 * that verifies each compatibility dimension the entry claims, derived from the existing evidence mechanisms rather
 * than declared again:
 *
 * <ul>
 *   <li>an {@code api:} entry is verified per dimension by its generated compatibility fixtures
 *       ({@code compatibility-fixtures.json}): {@code source} by its compile and source signature/hierarchy fixtures,
 *       {@code binary} by its linkage and binary signature/hierarchy fixtures, {@code behavior} and
 *       {@code configuration} by its behavior fixtures and their evidence tests, and the differences of an
 *       {@code ADAPTED} entry by its classification behavior fixture; each fixture kind counts as the suite that
 *       executes it;</li>
 *   <li>any other entry ({@code artifact:}, {@code behavior:}) is verified by its evidence hooks
 *       ({@link EvidenceHooks}: the entry's evidence IDs and the hooks bound to it).</li>
 * </ul>
 *
 * <p>{@link #evaluate} reports a {@link Problem} (and the matrix gate fails) when a non-deprecated owned entry that is
 * not a known gap is {@code EXCLUDED}, is an {@code api:} entry outside the fixture scope, lacks evidence for a
 * claimed dimension or its {@code ADAPTED} differences, or names evidence that is not a registered hook or not a test
 * class of the V3 tree. For the libraries with asynchronous transfers it also requires the four {@link #LIFECYCLE}
 * aspect entries (cancellation, failure cleanup, fault handling, concurrency), each owned, claimed, hooked and listing
 * test methods that exist in a hook of the entry. A known gap must remain one: matched, non-deprecated, claimed
 * {@code PRESERVED} or {@code ADAPTED} and without evidence, so the launch gate keeps blocking it; a gap that gained
 * evidence, or was reclassified {@code EXCLUDED}, is a problem too. Evaluation is deterministic.
 */
public final class HighLevelLibraryMatrix {
    /** The classpath resource of the checked-in definition. */
    public static final String DEFAULT_RESOURCE = "high-level-library-matrix.json";
    /** The lifecycle aspects a library with a {@code lifecycle} section must cover, in report order. */
    public static final List<String> LIFECYCLE = List.of("cancellation", "failure-cleanup", "fault-handling",
                                                         "concurrency");
    /** The evidence key of an {@code ADAPTED} entry's listed differences. */
    public static final String DIFFERENCES = "differences";
    /** The evidence key of rejection fixtures (an unclaimed dimension of a non-preserved entry stays unusable). */
    public static final String REJECTED = "rejected";

    private static final Set<String> TOP = Set.of("schemaVersion", "comment", "libraries");
    private static final Set<String> LIBRARY = Set.of("name", "artifact", "entries", "lifecycle", "knownGaps",
                                                      "comment");
    private static final Set<String> ASPECT = Set.of("entry", "tests");
    private static final Set<String> GAP = Set.of("entries", "reason");

    private final List<Library> libraries;

    private HighLevelLibraryMatrix(List<Library> libraries) {
        this.libraries = List.copyOf(libraries);
    }

    /** A lifecycle aspect: the behavior entry that claims it and the {@code TestClass#method}s that verify it. */
    public record Aspect(String name, String entry, List<String> tests) {
        public Aspect {
            tests = List.copyOf(tests);
        }
    }

    /** A known launch gap: entry ID globs and why V3 does not ship or verify them. */
    public record Gap(List<String> entries, String reason) {
        public Gap {
            entries = List.copyOf(entries);
        }

        boolean matches(String id) {
            return entries.stream().anyMatch(g -> glob(g).matcher(id).matches());
        }
    }

    /** One high-level library of the matrix. */
    public record Library(String name, String artifact, List<String> entries, List<Aspect> lifecycle,
                          List<Gap> knownGaps) {
        public Library {
            entries = List.copyOf(entries);
            lifecycle = List.copyOf(lifecycle);
            knownGaps = List.copyOf(knownGaps);
        }

        /** True if {@code id} matches one of the library's entry globs. */
        public boolean owns(String id) {
            return entries.stream().anyMatch(g -> glob(g).matcher(id).matches());
        }

        /** The known gap matching {@code id}, if any. */
        public Optional<Gap> gapOf(String id) {
            return knownGaps.stream().filter(g -> g.matches(id)).findFirst();
        }
    }

    /** The part of one generated compatibility fixture the matrix reads. */
    public record Fixture(String entryId, String kind, String dimension, List<String> evidenceIds) {
        public Fixture {
            evidenceIds = List.copyOf(evidenceIds);
        }
    }

    /** A generated compatibility fixture document: its test scope and fixtures. */
    public record Fixtures(List<String> scope, List<Fixture> fixtures) {
        public Fixtures {
            scope = List.copyOf(scope);
            fixtures = List.copyOf(fixtures);
        }

        /** Reads the members the matrix needs from {@code compatibility-fixtures.json}. */
        public static Fixtures parse(String json) {
            ObjectNode top = Node.parse(json).expectObjectNode();
            List<Fixture> fixtures = new ArrayList<>();
            for (ObjectNode f : top.expectArrayMember("fixtures").getElementsAs(ObjectNode.class)) {
                Node dimension = f.expectMember("dimension");
                fixtures.add(new Fixture(f.expectStringMember("entryId").getValue(),
                                         f.expectStringMember("kind").getValue(),
                                         dimension.isNullNode() ? null : dimension.expectStringNode().getValue(),
                                         strings(f.expectArrayMember("evidenceIds"))));
            }
            return new Fixtures(strings(top.expectArrayMember("scope")), fixtures);
        }
    }

    /** The verifications of the V3 tree: test classes by simple name and their methods. */
    public interface TestIndex {
        /** True if the tree has a test class with simple name {@code simpleName}. */
        boolean hasClass(String simpleName);

        /** True if that test class declares a method named {@code method}. */
        boolean hasMethod(String simpleName, String method);

        /** An index of the given classes and their method names. */
        static TestIndex of(Map<String, ? extends Set<String>> methods) {
            Map<String, Set<String>> copy = new HashMap<>();
            methods.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
            return new TestIndex() {
                @Override
                public boolean hasClass(String simpleName) {
                    return copy.containsKey(simpleName);
                }

                @Override
                public boolean hasMethod(String simpleName, String method) {
                    return copy.getOrDefault(simpleName, Set.of()).contains(method);
                }
            };
        }
    }

    /** The matrix status of one entry. */
    public enum Status {
        /** Every claimed dimension has resolving evidence. */
        VERIFIED,
        /** A known launch gap, which the launch gate blocks. */
        KNOWN_GAP,
        /** Deprecated: listed, not gated (Requirement 16.15). */
        DEPRECATED,
        /** Some claimed dimension has no evidence, or its evidence does not resolve. */
        MISSING_EVIDENCE
    }

    /**
     * One matrix row.
     *
     * @param library        the library
     * @param entryId        the manifest entry
     * @param classification its classification
     * @param claimed        the dimensions it claims (JSON names), plus {@link #DIFFERENCES} for {@code ADAPTED}
     * @param evidence       evidence IDs per dimension key ({@link #REJECTED} for rejection fixtures, {@code hooks}
     *                       for the hooks of an {@code api:} entry, which back no dimension by themselves)
     * @param owned          false for a known gap outside the library's own entries
     * @param status         the row's status
     * @param note           the gap reason, or why evidence is missing
     */
    public record Row(String library, String entryId, Classification classification, List<String> claimed,
                      SortedMap<String, SortedSet<String>> evidence, boolean owned, Status status, String note) {
    }

    /** A matrix gate failure. */
    public record Problem(String library, String subject, String message) {
        @Override
        public String toString() {
            return "[" + library + "] " + subject + ": " + message;
        }
    }

    /** An evaluated matrix. */
    public record Result(List<Library> libraries, List<Row> rows, List<Problem> problems) {
        public Result {
            libraries = List.copyOf(libraries);
            rows = rows.stream().sorted(Comparator.comparing(Row::library).thenComparing(Row::entryId)).toList();
            problems = problems.stream().distinct().sorted(Comparator.comparing(Problem::toString)).toList();
        }

        /** The canonical JSON form, ending with a newline. */
        public String toJson() {
            ArrayNode.Builder rowNodes = ArrayNode.builder();
            for (Row r : rows) {
                ObjectNode.Builder evidence = Node.objectNodeBuilder();
                r.evidence().forEach((k, v) -> evidence.withMember(k, Node.fromStrings(List.copyOf(v))));
                rowNodes.withValue(Node.objectNodeBuilder()
                                       .withMember("library", r.library())
                                       .withMember("entryId", r.entryId())
                                       .withMember("classification", r.classification().name())
                                       .withMember("claimed", Node.fromStrings(r.claimed()))
                                       .withMember("evidence", evidence.build())
                                       .withMember("owned", r.owned())
                                       .withMember("status", r.status().name())
                                       .withMember("note", r.note() == null ? Node.nullNode() : Node.from(r.note()))
                                       .build());
            }
            ArrayNode.Builder problemNodes = ArrayNode.builder();
            problems.forEach(p -> problemNodes.withValue(Node.objectNodeBuilder().withMember("library", p.library())
                                                             .withMember("subject", p.subject())
                                                             .withMember("message", p.message()).build()));
            return Node.prettyPrintJson(Node.objectNodeBuilder().withMember("schemaVersion", 1)
                                            .withMember("passed", problems.isEmpty())
                                            .withMember("rows", rowNodes.build())
                                            .withMember("problems", problemNodes.build()).build()) + "\n";
        }

        /** A per-library summary: entry and status counts, evidence per dimension, lifecycle, gaps and problems. */
        public String toText() {
            StringBuilder out = new StringBuilder("High-level library parity matrix: ")
                .append(problems.isEmpty() ? "PASSED" : "FAILED (" + problems.size() + " problem(s))").append('\n');
            for (Library library : libraries) {
                List<Row> own = rows.stream().filter(r -> r.library().equals(library.name())).toList();
                Map<Status, Integer> statuses = new LinkedHashMap<>();
                for (Status s : Status.values()) {
                    statuses.put(s, (int) own.stream().filter(r -> r.owned() && r.status() == s).count());
                }
                out.append("  ").append(library.name()).append(": ").append(own.stream().filter(Row::owned).count())
                   .append(" entries ").append(statuses).append('\n');
                Map<String, int[]> dims = new TreeMap<>();
                for (Row r : own) {
                    if (r.owned() && r.status() != Status.DEPRECATED && r.status() != Status.KNOWN_GAP) {
                        for (String d : r.claimed()) {
                            int[] c = dims.computeIfAbsent(d, k -> new int[2]);
                            c[0]++;
                            if (!r.evidence().getOrDefault(d, Collections.emptySortedSet()).isEmpty()) {
                                c[1]++;
                            }
                        }
                    }
                }
                dims.forEach((d, c) -> out.append("    ").append(d).append(": ").append(c[1]).append('/').append(c[0])
                                          .append(" claimed entries have evidence\n"));
                for (Aspect a : library.lifecycle()) {
                    out.append("    lifecycle ").append(a.name()).append(": ").append(a.entry()).append(" (")
                       .append(a.tests().size()).append(" tests)\n");
                }
                for (Gap g : library.knownGaps()) {
                    long n = own.stream().filter(r -> r.status() == Status.KNOWN_GAP && g.matches(r.entryId())).count();
                    out.append("    known gap (").append(n).append(" entries, blocked by the launch gate): ")
                       .append(g.reason()).append('\n');
                }
            }
            problems.forEach(p -> out.append("  PROBLEM ").append(p).append('\n'));
            return out.toString();
        }
    }

    /** The checked-in definition. */
    public static HighLevelLibraryMatrix loadDefault() {
        try (InputStream in = HighLevelLibraryMatrix.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + DEFAULT_RESOURCE);
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Parses a definition document; malformed documents fail with every problem. */
    public static HighLevelLibraryMatrix parse(String json) {
        List<ManifestProblem> problems = new ArrayList<>();
        ObjectNode top = JsonFields.object(Node.parse(json), "matrix", "the matrix", problems);
        List<Library> libraries = new ArrayList<>();
        if (top != null) {
            new JsonFields(top, "matrix", TOP, problems);
            for (Node l : top.getArrayMember("libraries").map(ArrayNode::getElements).orElse(List.of())) {
                ObjectNode library = JsonFields.object(l, "matrix", "a library", problems);
                if (library != null) {
                    libraries.add(library(library, problems));
                }
            }
        }
        if (libraries.isEmpty()) {
            problems.add(new ManifestProblem(ManifestProblem.Code.MALFORMED, "matrix", "no libraries"));
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Malformed high-level library matrix: " + problems);
        }
        return new HighLevelLibraryMatrix(libraries);
    }

    private static Library library(ObjectNode node, List<ManifestProblem> problems) {
        JsonFields fields = new JsonFields(node, "matrix", LIBRARY, problems);
        String name = fields.requiredString("name");
        String subject = "matrix library " + name;
        List<Aspect> aspects = new ArrayList<>();
        node.getObjectMember("lifecycle").ifPresent(lifecycle -> lifecycle.getStringMap().forEach((aspect, value) -> {
            ObjectNode a = JsonFields.object(value, subject, "lifecycle aspect " + aspect, problems);
            if (a != null) {
                JsonFields af = new JsonFields(a, subject + " " + aspect, ASPECT, problems);
                aspects.add(new Aspect(aspect, af.requiredString("entry"), af.strings("tests")));
            }
        }));
        List<Gap> gaps = new ArrayList<>();
        for (Node g : node.getArrayMember("knownGaps").map(ArrayNode::getElements).orElse(List.of())) {
            ObjectNode gap = JsonFields.object(g, subject, "a known gap", problems);
            if (gap != null) {
                JsonFields gf = new JsonFields(gap, subject + " gap", GAP, problems);
                gaps.add(new Gap(gf.strings("entries"), gf.requiredString("reason")));
            }
        }
        return new Library(name, fields.requiredString("artifact"), fields.strings("entries"), aspects, gaps);
    }

    /** The libraries, in definition order. */
    public List<Library> libraries() {
        return libraries;
    }

    /**
     * Evaluates the matrix.
     *
     * @param manifest    the Parity Manifest (of at least the libraries' V2 jars and every known gap)
     * @param hooks       the registered evidence hooks: launch-evidence-hooks.txt merged with the fixtures' hooks
     * @param fixtures    the generated compatibility fixtures and their scope
     * @param suiteByKind the test suite that executes each fixture kind
     * @param tests       the test classes and methods of the V3 tree
     */
    public Result evaluate(ParityManifest manifest, EvidenceHooks hooks, Fixtures fixtures,
                           Map<String, String> suiteByKind, TestIndex tests) {
        Map<String, ManifestEntry> entries = new TreeMap<>();
        manifest.entries().forEach(e -> entries.putIfAbsent(e.id(), e));
        Map<String, List<Fixture>> fixturesByEntry = new HashMap<>();
        fixtures.fixtures().forEach(f -> fixturesByEntry.computeIfAbsent(f.entryId(), k -> new ArrayList<>()).add(f));
        List<Row> rows = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        for (Library library : libraries) {
            new Evaluation(library, entries, hooks, fixtures.scope(), fixturesByEntry, suiteByKind, tests, rows,
                           problems).run();
        }
        return new Result(libraries, rows, problems);
    }

    /**
     * The launch gate's problem counts per library (owned entries) and per library's known-gap dependencies (gap
     * entries it does not own), by problem code, from a launch gate report ({@code launch-gate-report.json}).
     */
    public SortedMap<String, SortedMap<String, Integer>> launchProblems(ObjectNode report) {
        SortedMap<String, SortedMap<String, Integer>> counts = new TreeMap<>();
        for (Library l : libraries) {
            counts.put(l.name(), new TreeMap<>());
        }
        for (ObjectNode p : report.expectArrayMember("problems").getElementsAs(ObjectNode.class)) {
            String subject = p.expectStringMember("subject").getValue();
            String code = p.expectStringMember("code").getValue();
            for (Library l : libraries) {
                String key = l.owns(subject) ? l.name() : l.gapOf(subject).isPresent()
                                                          ? l.name() + " (known-gap dependencies)" : null;
                if (key != null) {
                    counts.computeIfAbsent(key, k -> new TreeMap<>()).merge(code, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    /** The evaluation of one library. */
    private static final class Evaluation {
        private final Library library;
        private final Map<String, ManifestEntry> entries;
        private final EvidenceHooks hooks;
        private final List<String> scope;
        private final Map<String, List<Fixture>> fixtures;
        private final Map<String, String> suiteByKind;
        private final TestIndex tests;
        private final List<Row> rows;
        private final List<Problem> problems;
        private final Map<String, Row> owned = new HashMap<>();

        Evaluation(Library library, Map<String, ManifestEntry> entries, EvidenceHooks hooks, List<String> scope,
                   Map<String, List<Fixture>> fixtures, Map<String, String> suiteByKind, TestIndex tests,
                   List<Row> rows, List<Problem> problems) {
            this.library = library;
            this.entries = entries;
            this.hooks = hooks;
            this.scope = scope;
            this.fixtures = fixtures;
            this.suiteByKind = suiteByKind;
            this.tests = tests;
            this.rows = rows;
            this.problems = problems;
        }

        void run() {
            for (String glob : library.entries()) {
                if (entries.keySet().stream().noneMatch(id -> glob(glob).matcher(id).matches())) {
                    problem(glob, "the library's entry glob matches no manifest entry");
                }
            }
            for (ManifestEntry e : entries.values()) {
                if (library.owns(e.id()) && library.gapOf(e.id()).isEmpty()) {
                    entry(e);
                }
            }
            lifecycle();
            gaps();
        }

        private void entry(ManifestEntry e) {
            SortedMap<String, SortedSet<String>> evidence = evidence(e);
            List<String> claimed = claimed(e);
            if (e.deprecated()) {
                add(new Row(library.name(), e.id(), e.classification(), claimed, evidence, true, Status.DEPRECATED,
                            null));
                return;
            }
            List<String> missing = new ArrayList<>();
            if (e.classification() == Classification.EXCLUDED) {
                missing.add("non-deprecated but EXCLUDED: the library ships no capability for it");
            }
            if (api(e) && !EntryGlobs.inScope(e.id(), scope)) {
                missing.add("not in the compatibility fixture scope (compatibility-scope.txt) and not a known gap");
            }
            for (String d : claimed) {
                if (evidence.getOrDefault(d, Collections.emptySortedSet()).isEmpty()) {
                    missing.add("no evidence for its " + d + (d.equals(DIFFERENCES) ? "" : " claim"));
                }
            }
            for (SortedSet<String> ids : evidence.values()) {
                for (String id : ids) {
                    unresolved(id).ifPresent(missing::add);
                }
            }
            missing.stream().distinct().forEach(m -> problem(e.id(), m));
            add(new Row(library.name(), e.id(), e.classification(), claimed, evidence, true,
                        missing.isEmpty() ? Status.VERIFIED : Status.MISSING_EVIDENCE,
                        missing.isEmpty() ? null : String.join("; ", missing)));
        }

        /** The claimed dimension keys: the claimed dimensions, plus the differences of an ADAPTED entry. */
        private static List<String> claimed(ManifestEntry e) {
            List<String> claimed = new ArrayList<>(e.dimensions().stream().map(CompatibilityDimension::jsonName)
                                                    .toList());
            if (e.classification() == Classification.ADAPTED) {
                claimed.add(DIFFERENCES);
            }
            return claimed;
        }

        /** Evidence per dimension key: fixtures for api: entries, hooks for every other kind. */
        private SortedMap<String, SortedSet<String>> evidence(ManifestEntry e) {
            SortedMap<String, SortedSet<String>> evidence = new TreeMap<>();
            SortedSet<String> entryHooks = hooks.of(e);
            if (!api(e)) {
                for (String d : claimed(e)) {
                    evidence.put(d, new TreeSet<>(entryHooks));
                }
                return evidence;
            }
            for (Fixture f : fixtures.getOrDefault(e.id(), List.of())) {
                String key = f.kind().endsWith("_REJECTED") ? REJECTED
                                                            : f.dimension() == null ? DIFFERENCES : f.dimension();
                SortedSet<String> ids = evidence.computeIfAbsent(key, k -> new TreeSet<>());
                String suite = suiteByKind.get(f.kind());
                if (suite == null) {
                    problem(e.id(), "no suite executes " + f.kind() + " fixtures");
                } else {
                    ids.add(suite);
                }
                ids.addAll(f.evidenceIds());
            }
            if (!entryHooks.isEmpty()) {
                evidence.put("hooks", new TreeSet<>(entryHooks));
            }
            return evidence;
        }

        /** Why evidence ID {@code id} does not resolve, if it does not. */
        private Optional<String> unresolved(String id) {
            int hash = id.indexOf('#');
            String type = hash < 0 ? id : id.substring(0, hash);
            String simple = type.substring(type.lastIndexOf('.') + 1);
            if (hooks.registered() && !hooks.known(id) && !hooks.known(simple)) {
                return Optional.of("evidence " + id + " is not a registered evidence hook");
            }
            if (!tests.hasClass(simple)) {
                return Optional.of("evidence " + id + " is not a test class of the V3 tree");
            }
            if (hash >= 0 && !tests.hasMethod(simple, id.substring(hash + 1))) {
                return Optional.of("evidence " + id + " names no test method of " + simple);
            }
            return Optional.empty();
        }

        private void lifecycle() {
            if (library.lifecycle().isEmpty()) {
                return;
            }
            Set<String> names = new TreeSet<>();
            library.lifecycle().forEach(a -> names.add(a.name()));
            for (String aspect : LIFECYCLE) {
                if (!names.contains(aspect)) {
                    problem("lifecycle", "no " + aspect + " aspect; a lifecycle covers " + LIFECYCLE);
                }
            }
            for (Aspect a : library.lifecycle()) {
                String subject = "lifecycle " + a.name();
                if (!LIFECYCLE.contains(a.name())) {
                    problem(subject, "unknown lifecycle aspect; expected one of " + LIFECYCLE);
                }
                ManifestEntry e = entries.get(a.entry());
                Row row = owned.get(a.entry());
                if (e == null || !library.owns(a.entry())) {
                    problem(subject, a.entry() + " is not a manifest entry of this library");
                    continue;
                }
                if (row == null || row.status() != Status.VERIFIED) {
                    problem(subject, a.entry() + " is not a verified entry of this library ("
                                     + (row == null ? "a known gap" : row.status()) + ")");
                }
                if (!e.dimensions().contains(CompatibilityDimension.BEHAVIOR)) {
                    problem(subject, a.entry() + " claims no behavior dimension");
                }
                if (a.tests().isEmpty()) {
                    problem(subject, "lists no test methods");
                }
                SortedSet<String> entryHooks = hooks.of(e);
                for (String test : a.tests()) {
                    int hash = test.indexOf('#');
                    if (hash <= 0 || hash == test.length() - 1) {
                        problem(subject, "'" + test + "' is not TestClass#method");
                        continue;
                    }
                    unresolvedMethod(test).ifPresent(m -> problem(subject, m));
                    if (!entryHooks.contains(test.substring(0, hash))) {
                        problem(subject, test.substring(0, hash) + " is not an evidence hook of " + a.entry());
                    }
                }
            }
        }

        private Optional<String> unresolvedMethod(String test) {
            int hash = test.indexOf('#');
            String type = test.substring(0, hash);
            if (!tests.hasClass(type)) {
                return Optional.of(test + ": no test class " + type + " in the V3 tree");
            }
            return tests.hasMethod(type, test.substring(hash + 1)) ? Optional.empty()
                                                                   : Optional.of(test + ": no such test method");
        }

        private void gaps() {
            for (Gap gap : library.knownGaps()) {
                for (String glob : gap.entries()) {
                    List<ManifestEntry> matched = entries.values().stream()
                                                         .filter(e -> glob(glob).matcher(e.id()).matches()).toList();
                    if (matched.isEmpty()) {
                        problem(glob, "the known gap matches no manifest entry; remove it");
                    }
                    for (ManifestEntry e : matched) {
                        gapEntry(gap, e);
                    }
                }
            }
        }

        private void gapEntry(Gap gap, ManifestEntry e) {
            if (owned.containsKey(e.id()) && owned.get(e.id()).status() == Status.KNOWN_GAP) {
                return;
            }
            SortedMap<String, SortedSet<String>> evidence = evidence(e);
            if (e.deprecated()) {
                problem(e.id(), "a deprecated entry is not gated, so it is no launch gap; narrow the known gap");
            }
            if (e.classification() == Classification.EXCLUDED) {
                problem(e.id(), "a known gap must stay PRESERVED or ADAPTED so the launch gate blocks it, but it is "
                                + "EXCLUDED");
            }
            boolean hasEvidence = evidence.values().stream().anyMatch(s -> !s.isEmpty())
                                  || api(e) && EntryGlobs.inScope(e.id(), scope);
            if (hasEvidence) {
                problem(e.id(), "the known gap now has evidence " + evidence + "; verify it ships and remove it from "
                                + "knownGaps");
            }
            add(new Row(library.name(), e.id(), e.classification(), claimed(e), evidence, library.owns(e.id()),
                        Status.KNOWN_GAP, gap.reason()));
        }

        private void add(Row row) {
            rows.add(row);
            owned.put(row.entryId(), row);
        }

        private void problem(String subject, String message) {
            problems.add(new Problem(library.name(), subject, message));
        }

        private static boolean api(ManifestEntry e) {
            return e.kind() == EntryKind.PUBLIC_TYPE || e.kind() == EntryKind.PUBLIC_MEMBER;
        }
    }

    static Pattern glob(String glob) {
        return EntryGlobs.pattern(glob);
    }

    private static List<String> strings(ArrayNode array) {
        return array.getElementsAs(StringNode.class).stream().map(StringNode::getValue).toList();
    }
}
