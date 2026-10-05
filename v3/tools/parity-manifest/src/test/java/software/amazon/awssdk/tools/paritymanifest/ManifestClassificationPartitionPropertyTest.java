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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static software.amazon.awssdk.tools.paritymanifest.ManifestClassificationPartitionPropertyTest.Presence.FORBIDDEN;
import static software.amazon.awssdk.tools.paritymanifest.ManifestClassificationPartitionPropertyTest.Presence.REQUIRED;
import static software.amazon.awssdk.tools.paritymanifest.ManifestClassificationPartitionPropertyTest.Presence.REQUIRED_UNLESS_DEPRECATED;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code;

/**
 * Property 56 (Requirements 16.2, 16.3, 16.11, 16.13): for any inventoried baseline entry, exactly one of
 * {@code PRESERVED}, {@code ADAPTED} or {@code EXCLUDED} is assigned, and every adapted or excluded entry carries its
 * required differences, rationale, replacement or action, diagnostics and verification evidence.
 *
 * <p>Each try runs {@link ManifestGenerator} over generated inventories (one to three
 * {@link StaticInventorySource}s) and generated classification rules ({@link ClassificationRules#of}). Inventory IDs
 * come from hand-written per-kind pools, so whether an item follows its kind's ID scheme is known by construction
 * (an item is well formed exactly when its declared kind is the pool it was drawn from); the IDs repeat within and
 * across sources. Rules match exact IDs, {@code *} globs, kinds and the deprecation flag, in both tiers. Assignments
 * start from a schema-valid template per classification; noisy tries then apply random field mutations (dropping,
 * blanking or adding rationale, dimensions, differences, replacements, migration kinds and texts, support evidence
 * and evidence IDs). Clean tries use unique well-formed items, non-conflicting exact-ID and kind rules and a
 * catch-all default, so they always yield a manifest.
 *
 * <p>{@link Oracle} predicts the outcome from the generated descriptions alone. It replays the documented duplicate
 * and precedence rules with its own wildcard matcher and lexicographic specificity, and checks every classified entry
 * against {@link #SCHEMA}: the classification-dependent field rules of {@code parity-manifest-1.schema.json} and the
 * requirements, written as a table rather than as validation code. The property then checks that:
 * <ul>
 *   <li>generation fails exactly when a problem is predicted, with exactly the predicted
 *       {@code (code, entry ID, field or detail)} multiset;</li>
 *   <li>a generated manifest has exactly one entry per inventoried ID (completeness and uniqueness), each with the
 *       single classification and every field of its winning rule, and a summary that partitions the entries;</li>
 *   <li>the generated manifest validates, and the same manifest with one entry repeated reports exactly that ID as
 *       a duplicate.</li>
 * </ul>
 */
class ManifestClassificationPartitionPropertyTest {

    private static final BaselineIdentity BASELINE =
        new BaselineIdentity("2.54.0", "0123456789abcdef0123456789abcdef01234567", LocalDate.of(2026, 9, 1),
                             "Generally available, supported", "parity-manifest/property56");

    /**
     * Well-formed IDs of each kind, written out by hand rather than with {@link EntryKind}'s builders. Every ID follows
     * exactly the scheme of the kind it is listed under: prefixes differ between kinds, and the two {@code api:}
     * kinds differ by the {@code #} member separator.
     */
    private static final Map<EntryKind, List<String>> IDS = idPools();
    private static final List<String> VALID_IDS = IDS.values().stream().flatMap(List::stream).toList();
    /** IDs that follow no kind's scheme (an artifact without group, and a behavior with upper case and a space). */
    private static final List<String> MALFORMED_IDS = List.of("artifact:no-group", "behavior:Upper Case");
    /** Replacement values that are not entry IDs. */
    private static final List<String> MALFORMED_REPLACEMENTS = List.of("not-an-id", "api:has space");
    /** Rule ID globs; several have equal literal lengths, so equally specific matches occur. */
    private static final List<String> GLOBS = List.of(
        "*", "api:*", "api:software.amazon.awssdk.core.*", "*S3Client*", "service:*", "*:s3", "config:*",
        "http-mode:*:sync", "behavior:*", "artifact:software.amazon.awssdk:*", "*s3*");
    private static final List<String> SOURCE_NAMES = List.of("source-a", "source-b", "source-c");
    private static final String SURFACE = "V2 surface";
    private static final String ARTIFACT = "software.amazon.awssdk:sdk-core";

    /** The fields problems name, as {@link ManifestValidator} and the schema name them. */
    private static final List<String> FIELDS = List.of(
        "v2Surface", "source.artifact", "rationale", "dimensions", "differences", "replacement", "migrationKind",
        "migration", "supportEvidence", "evidenceIds");
    private static final Map<String, Pattern> FIELD_PATTERNS = fieldPatterns();

    /** How the schema constrains a field for one classification. */
    enum Presence {
        /** Must be a non-blank string or a non-empty array ({@code minLength 1} / {@code minItems 1}). */
        REQUIRED,
        /** Must be null or an empty array ({@code type null} / {@code maxItems 0}). */
        FORBIDDEN,
        /** Required unless the V2 baseline deprecates the entry (the {@code EXCLUDED anyOf}). */
        REQUIRED_UNLESS_DEPRECATED
    }

    /** Fields every entry requires, whatever its classification (glossary: every entry has a rationale). */
    private static final List<String> ALWAYS_REQUIRED = List.of("v2Surface", "source.artifact", "rationale");

    /**
     * The classification-dependent field rules of {@code parity-manifest-1.schema.json} ({@code allOf}) and
     * Requirements 16.3, 16.11 and 16.13; fields not listed are optional.
     */
    private static final Map<Classification, Map<String, Presence>> SCHEMA = new EnumMap<>(Map.of(
        Classification.PRESERVED, Map.of("dimensions", REQUIRED, "differences", FORBIDDEN, "replacement", FORBIDDEN,
                                         "migrationKind", FORBIDDEN, "migration", FORBIDDEN),
        Classification.ADAPTED, Map.of("differences", REQUIRED, "replacement", REQUIRED, "migrationKind", REQUIRED,
                                       "migration", REQUIRED, "evidenceIds", REQUIRED),
        Classification.EXCLUDED, Map.of("dimensions", FORBIDDEN, "migrationKind", REQUIRED, "migration", REQUIRED,
                                        "supportEvidence", REQUIRED_UNLESS_DEPRECATED)));

    /** The migration kinds the schema allows where it requires one. */
    private static final Map<Classification, Set<MigrationKind>> MIGRATION_KINDS = new EnumMap<>(Map.of(
        Classification.ADAPTED, Set.of(MigrationKind.RECIPE, MigrationKind.DIAGNOSTIC),
        Classification.EXCLUDED, Set.of(MigrationKind.COMPILE_TIME, MigrationKind.DIAGNOSTIC)));

    /** Problem codes the scenarios are expected to produce; statistics check that each occurs. */
    private static final List<Code> PREDICTED_CODES = List.of(
        Code.INVENTORY, Code.INVALID_ID, Code.DUPLICATE_ID, Code.UNMATCHED, Code.CONFLICTING_RULES,
        Code.MISSING_FIELD, Code.FORBIDDEN_FIELD, Code.INVALID_VALUE);

    // Feature: aws-sdk-java-v3, Property 56: Manifest classifications form a complete partition
    // Embedded edge cases are disabled: they collapse the frequency choices to their first alternative, which starves
    // the noisy mutations. The statistics coverage checks fail the property if the generated mix degrades.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 56: Manifest classifications form a complete partition")
    void manifestClassificationsFormACompletePartition(@ForAll("scenarios") Scenario scenario) {
        Prediction expected = Oracle.predict(scenario);
        ManifestGenerator generator = new ManifestGenerator(scenario.sources(), scenario.classificationRules());
        ParityManifest manifest;
        try {
            manifest = generator.generate(null, BASELINE, null, ManifestValidator.Options.DEFAULT);
        } catch (ManifestValidationException e) {
            assertFalse(expected.problems().isEmpty(), () -> "No problem is predicted, but generation failed: "
                                                             + e.getMessage());
            assertEquals(expected.problems(), observed(e.problems()), e::getMessage);
            collectStatistics(expected);
            return;
        }
        assertEquals(List.of(), expected.problems(), "Generation succeeded although problems are predicted");

        // Completeness and uniqueness: exactly one entry per inventoried ID.
        List<String> inventoried = scenario.items().stream().map(ItemSpec::id).sorted().toList();
        assertEquals(inventoried, manifest.entries().stream().map(ManifestEntry::id).toList(), "Entry IDs");
        // Exactly one classification per entry, with every field the winning rule assigns.
        for (ManifestEntry entry : manifest.entries()) {
            Winner winner = expected.entries().get(entry.id());
            assertEquals(winner.toEntry(), entry, () -> "Entry " + entry.id() + " classified by " + winner.rule());
        }
        Map<Classification, Integer> counts = new EnumMap<>(Classification.class);
        for (Classification c : Classification.values()) {
            counts.put(c, (int) expected.entries().values().stream()
                                        .filter(w -> w.assignment().classification() == c).count());
        }
        assertEquals(counts, manifest.summary(), "Summary");
        assertEquals(inventoried.size(), manifest.summary().values().stream().mapToInt(Integer::intValue).sum(),
                     "The summary partitions the entries");

        assertEquals(List.of(), ManifestValidator.validate(manifest, ManifestValidator.Options.DEFAULT),
                     "A generated manifest validates");
        List<ManifestEntry> withDuplicate = new ArrayList<>(manifest.entries());
        ManifestEntry duplicated = withDuplicate.get(scenario.duplicateSeed() % withDuplicate.size());
        withDuplicate.add(duplicated);
        List<ManifestProblem> duplicateProblems = ManifestValidator.validate(
            new ParityManifest(BASELINE, null, withDuplicate), ManifestValidator.Options.DEFAULT);
        assertEquals(List.of(Code.DUPLICATE_ID + " " + duplicated.id()),
                     duplicateProblems.stream().map(p -> p.code() + " " + p.subject()).toList(),
                     "A repeated entry is reported once as a duplicate");
        collectStatistics(expected);
    }

    private static void collectStatistics(Prediction expected) {
        Statistics.label("outcome").collect(expected.problems().isEmpty() ? "generated" : "rejected");
        Statistics.label("outcome").coverage(c -> {
            c.check("generated").percentage(p -> p >= 25);
            c.check("rejected").percentage(p -> p >= 40);
        });
        for (Code code : PREDICTED_CODES) {
            Statistics.label(code.name()).collect(expected.problems().stream().anyMatch(p -> p.code() == code));
            Statistics.label(code.name()).coverage(c -> c.check(true).percentage(p -> p >= 1));
        }
        for (Classification classification : Classification.values()) {
            String label = "classified " + classification;
            Statistics.label(label).collect(expected.entries().values().stream()
                                                    .anyMatch(w -> w.assignment().classification() == classification));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 20));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenario description

    /**
     * One inventory item.
     *
     * @param source       index of its source in {@link #SOURCE_NAMES}
     * @param id           its ID
     * @param declaredKind the kind its source reports
     * @param wellFormed   whether {@code id} follows {@code declaredKind}'s scheme, known by construction
     */
    record ItemSpec(int source, String id, EntryKind declaredKind, boolean wellFormed, boolean deprecated,
                    String v2Surface, String artifact) {

        static ItemSpec create(int source, EntryKind poolKind, int index, int kindShift, boolean malformed,
                               boolean deprecated, String v2Surface, String artifact) {
            List<String> pool = malformed ? MALFORMED_IDS : IDS.get(poolKind);
            EntryKind[] kinds = EntryKind.values();
            EntryKind declared = kinds[(poolKind.ordinal() + kindShift) % kinds.length];
            return new ItemSpec(source, pool.get(index % pool.size()), declared, !malformed && kindShift == 0,
                                deprecated, v2Surface, artifact);
        }

        InventoryItem item() {
            return new InventoryItem(id, declaredKind, v2Surface, new SourceLocation(artifact, null), deprecated);
        }
    }

    /** A rule match; {@code null} components are unconstrained. */
    record MatchSpec(String id, EntryKind kind, Boolean deprecated) {
        boolean constrains() {
            return id != null || kind != null || deprecated != null;
        }

        /** The key clean scenarios keep rules unique by, so that no two of their rules tie. */
        String key() {
            return id != null ? id : String.valueOf(kind);
        }
    }

    /** The final values a rule assigns. */
    record AssignmentSpec(Classification classification, List<CompatibilityDimension> dimensions, String rationale,
                          List<String> differences, String v3Artifact, String replacement, MigrationKind migrationKind,
                          String migration, String supportEvidence, List<String> evidenceIds) {

        AssignmentSpec mutate(List<Mutation> mutations) {
            Draft draft = new Draft(this);
            mutations.forEach(m -> m.change.accept(draft));
            return draft.freeze();
        }
    }

    /** A mutable copy of an assignment that mutations change. */
    static final class Draft {
        private final Classification classification;
        private final List<CompatibilityDimension> dimensions;
        private final List<String> differences;
        private final List<String> evidenceIds;
        private String rationale;
        private String v3Artifact;
        private String replacement;
        private MigrationKind migrationKind;
        private String migration;
        private String supportEvidence;

        Draft(AssignmentSpec a) {
            classification = a.classification();
            dimensions = new ArrayList<>(a.dimensions());
            rationale = a.rationale();
            differences = new ArrayList<>(a.differences());
            v3Artifact = a.v3Artifact();
            replacement = a.replacement();
            migrationKind = a.migrationKind();
            migration = a.migration();
            supportEvidence = a.supportEvidence();
            evidenceIds = new ArrayList<>(a.evidenceIds());
        }

        AssignmentSpec freeze() {
            return new AssignmentSpec(classification, List.copyOf(dimensions), rationale, List.copyOf(differences),
                                      v3Artifact, replacement, migrationKind, migration, supportEvidence,
                                      List.copyOf(evidenceIds));
        }
    }

    /** A change to one assigned field; most break a schema rule for some classification. */
    enum Mutation {
        CLEAR_RATIONALE(d -> d.rationale = null),
        BLANK_RATIONALE(d -> d.rationale = " "),
        ADD_DIMENSION(d -> d.dimensions.add(CompatibilityDimension.BINARY)),
        CLEAR_DIMENSIONS(d -> d.dimensions.clear()),
        ADD_DIFFERENCE(d -> d.differences.add("throws a different exception")),
        ADD_BLANK_DIFFERENCE(d -> d.differences.add(" ")),
        CLEAR_DIFFERENCES(d -> d.differences.clear()),
        WELL_FORMED_REPLACEMENT(d -> d.replacement = "api:software.amazon.awssdk.services.s3.S3Client"),
        MALFORMED_REPLACEMENT(d -> d.replacement = MALFORMED_REPLACEMENTS.get(0)),
        SPACED_REPLACEMENT(d -> d.replacement = MALFORMED_REPLACEMENTS.get(1)),
        BLANK_REPLACEMENT(d -> d.replacement = " "),
        CLEAR_REPLACEMENT(d -> d.replacement = null),
        RECIPE(d -> d.migrationKind = MigrationKind.RECIPE),
        DIAGNOSTIC(d -> d.migrationKind = MigrationKind.DIAGNOSTIC),
        COMPILE_TIME(d -> d.migrationKind = MigrationKind.COMPILE_TIME),
        CLEAR_MIGRATION_KIND(d -> d.migrationKind = null),
        SET_MIGRATION(d -> d.migration = "use the V3 API"),
        BLANK_MIGRATION(d -> d.migration = ""),
        CLEAR_MIGRATION(d -> d.migration = null),
        SET_SUPPORT_EVIDENCE(d -> d.supportEvidence = "unsupported since 2.20"),
        BLANK_SUPPORT_EVIDENCE(d -> d.supportEvidence = " "),
        CLEAR_SUPPORT_EVIDENCE(d -> d.supportEvidence = null),
        ADD_EVIDENCE(d -> d.evidenceIds.add("MutatedEvidenceTest")),
        ADD_BLANK_EVIDENCE(d -> d.evidenceIds.add(" ")),
        CLEAR_EVIDENCE(d -> d.evidenceIds.clear()),
        CLEAR_V3_ARTIFACT(d -> d.v3Artifact = null);

        private final Consumer<Draft> change;

        Mutation(Consumer<Draft> change) {
            this.change = change;
        }
    }

    record RuleSpec(MatchSpec match, boolean isDefault, AssignmentSpec assignment) {
    }

    /**
     * One try: the inventory items in source order, the rules (named {@code rule<index>}), and the seed choosing the
     * entry repeated in the duplicate check.
     */
    record Scenario(List<ItemSpec> items, List<RuleSpec> rules, int duplicateSeed) {

        static String ruleName(int index) {
            return "rule" + index;
        }

        /** One static source per source index used, each with its items in order. */
        List<InventorySource> sources() {
            Map<Integer, List<InventoryItem>> bySource = new TreeMap<>();
            for (ItemSpec item : items) {
                bySource.computeIfAbsent(item.source(), k -> new ArrayList<>()).add(item.item());
            }
            List<InventorySource> sources = new ArrayList<>();
            bySource.forEach((index, list) -> sources.add(new StaticInventorySource(SOURCE_NAMES.get(index), list)));
            return sources;
        }

        ClassificationRules classificationRules() {
            List<ClassificationRules.Rule> out = new ArrayList<>();
            for (int i = 0; i < rules.size(); i++) {
                RuleSpec r = rules.get(i);
                MatchSpec m = r.match();
                AssignmentSpec a = r.assignment();
                out.add(new ClassificationRules.Rule(
                    ruleName(i), new ClassificationRules.Match(m.id(), m.kind(), m.deprecated()),
                    new ClassificationRules.Assignment(a.classification(), a.dimensions(), a.rationale(),
                                                       a.differences(), a.v3Artifact(), a.replacement(),
                                                       a.migrationKind(), a.migration(), a.supportEvidence(),
                                                       a.evidenceIds()),
                    r.isDefault()));
            }
            return ClassificationRules.of(out);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle

    /** One predicted (or observed) problem: its code, subject, and the field it names or its detail. */
    record Expected(Code code, String subject, String detail) implements Comparable<Expected> {
        private static final Comparator<Expected> ORDER =
            Comparator.comparing(Expected::subject).thenComparing(Expected::code).thenComparing(Expected::detail);

        @Override
        public int compareTo(Expected o) {
            return ORDER.compare(this, o);
        }
    }

    /** The rule that classifies an item. */
    record Winner(ItemSpec item, String rule, AssignmentSpec assignment) {
        /** The entry the item and the assignment make. */
        ManifestEntry toEntry() {
            AssignmentSpec a = assignment;
            return new ManifestEntry(item.id(), item.declaredKind(), item.v2Surface(),
                                     new SourceLocation(item.artifact(), null), item.deprecated(), a.classification(),
                                     new TreeSet<>(a.dimensions()), a.rationale(), a.differences(), a.v3Artifact(),
                                     a.replacement(), a.migrationKind(), a.migration(), a.supportEvidence(),
                                     new TreeSet<>(a.evidenceIds()));
        }
    }

    /**
     * The predicted outcome.
     *
     * @param problems every predicted problem, sorted; empty when generation succeeds
     * @param entries  the winner of every classified item, by ID
     */
    record Prediction(List<Expected> problems, SortedMap<String, Winner> entries) {
    }

    /**
     * Predicts the generation outcome from the scenario descriptions: ID well-formedness is known by construction,
     * rule matching uses its own wildcard matcher, precedence is a lexicographic comparison of documented
     * specificity tuples, and entry fields are checked against the {@link #SCHEMA} table.
     */
    static final class Oracle {

        private Oracle() {
        }

        static Prediction predict(Scenario scenario) {
            List<Expected> problems = new ArrayList<>();
            if (scenario.items().isEmpty()) {
                problems.add(new Expected(Code.INVENTORY, "$", ""));
            }
            Map<String, List<ItemSpec>> reports = new TreeMap<>();
            for (ItemSpec item : scenario.items()) {
                if (item.wellFormed()) {
                    reports.computeIfAbsent(item.id(), k -> new ArrayList<>()).add(item);
                } else {
                    problems.add(new Expected(Code.INVALID_ID, item.id(), SOURCE_NAMES.get(item.source())));
                }
            }
            SortedMap<String, Winner> entries = new TreeMap<>();
            reports.forEach((id, items) -> {
                if (items.size() > 1) {
                    problems.add(new Expected(Code.DUPLICATE_ID, id, String.valueOf(items.size())));
                } else {
                    classify(scenario.rules(), items.get(0), problems, entries);
                }
            });
            entries.values().forEach(w -> checkFields(w.item(), w.assignment(), problems));
            Collections.sort(problems);
            return new Prediction(List.copyOf(problems), entries);
        }

        private static void classify(List<RuleSpec> rules, ItemSpec item, List<Expected> problems,
                                     Map<String, Winner> entries) {
            List<Integer> matching = new ArrayList<>();
            for (int i = 0; i < rules.size(); i++) {
                if (matches(rules.get(i).match(), item)) {
                    matching.add(i);
                }
            }
            if (matching.isEmpty()) {
                problems.add(new Expected(Code.UNMATCHED, item.id(), ""));
                return;
            }
            int[] best = matching.stream().map(i -> specificity(rules.get(i))).max(Arrays::compare).orElseThrow();
            List<Integer> winners = matching.stream().filter(i -> Arrays.equals(specificity(rules.get(i)), best))
                                            .toList();
            if (winners.size() > 1) {
                problems.add(new Expected(Code.CONFLICTING_RULES, item.id(), String.join(
                    ", ", winners.stream().map(Scenario::ruleName).sorted().toList())));
                return;
            }
            int winner = winners.get(0);
            entries.put(item.id(), new Winner(item, Scenario.ruleName(winner), rules.get(winner).assignment()));
        }

        private static boolean matches(MatchSpec match, ItemSpec item) {
            return (match.id() == null || wildcard(match.id(), item.id()))
                   && (match.kind() == null || match.kind() == item.declaredKind())
                   && (match.deprecated() == null || match.deprecated() == item.deprecated());
        }

        /**
         * The documented specificity, compared lexicographically: rules beat defaults; an exact ID beats a glob,
         * which beats no ID; more literal glob characters win; a kind constraint, then a deprecated constraint, wins.
         */
        private static int[] specificity(RuleSpec rule) {
            String id = rule.match().id();
            int idRank = id == null ? 0 : id.indexOf('*') >= 0 ? 1 : 2;
            int literal = id == null ? 0 : (int) id.chars().filter(c -> c != '*').count();
            return new int[] {rule.isDefault() ? 0 : 1, idRank, literal, rule.match().kind() == null ? 0 : 1,
                              rule.match().deprecated() == null ? 0 : 1};
        }

        /** Whether {@code text} matches {@code glob}, where {@code *} matches any run of characters. */
        static boolean wildcard(String glob, String text) {
            boolean[][] match = new boolean[glob.length() + 1][text.length() + 1];
            match[glob.length()][text.length()] = true;
            for (int i = glob.length() - 1; i >= 0; i--) {
                for (int j = text.length(); j >= 0; j--) {
                    match[i][j] = glob.charAt(i) == '*'
                                  ? match[i + 1][j] || j < text.length() && match[i][j + 1]
                                  : j < text.length() && glob.charAt(i) == text.charAt(j) && match[i + 1][j + 1];
                }
            }
            return match[0][0];
        }

        /** The schema problems of one classified entry. */
        private static void checkFields(ItemSpec item, AssignmentSpec a, List<Expected> problems) {
            String id = item.id();
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("v2Surface", item.v2Surface());
            values.put("source.artifact", item.artifact());
            values.put("rationale", a.rationale());
            values.put("dimensions", a.dimensions());
            values.put("differences", a.differences());
            values.put("replacement", a.replacement());
            values.put("migrationKind", a.migrationKind());
            values.put("migration", a.migration());
            values.put("supportEvidence", a.supportEvidence());
            values.put("evidenceIds", a.evidenceIds());

            for (String field : ALWAYS_REQUIRED) {
                if (absent(values.get(field))) {
                    problems.add(new Expected(Code.MISSING_FIELD, id, field));
                }
            }
            SCHEMA.get(a.classification()).forEach((field, presence) -> {
                Object value = values.get(field);
                boolean violated = switch (presence) {
                    case REQUIRED -> absent(value);
                    case FORBIDDEN -> value != null && !(value instanceof Collection<?> c && c.isEmpty());
                    case REQUIRED_UNLESS_DEPRECATED -> !item.deprecated() && absent(value);
                };
                if (violated) {
                    problems.add(new Expected(presence == FORBIDDEN ? Code.FORBIDDEN_FIELD : Code.MISSING_FIELD, id,
                                              field));
                }
            });
            Set<MigrationKind> allowed = MIGRATION_KINDS.get(a.classification());
            if (allowed != null && a.migrationKind() != null && !allowed.contains(a.migrationKind())) {
                problems.add(new Expected(Code.INVALID_VALUE, id, "migrationKind"));
            }
            // Values every classification constrains: a replacement is an entry ID; array items are not blank.
            if (!absent(a.replacement()) && !VALID_IDS.contains(a.replacement())) {
                problems.add(new Expected(Code.INVALID_VALUE, id, "replacement"));
            }
            if (a.differences().stream().anyMatch(String::isBlank)) {
                problems.add(new Expected(Code.INVALID_VALUE, id, "differences"));
            }
            if (a.evidenceIds().stream().anyMatch(String::isBlank)) {
                problems.add(new Expected(Code.INVALID_VALUE, id, "evidenceIds"));
            }
        }

        private static boolean absent(Object value) {
            return value == null
                   || value instanceof String s && s.isBlank()
                   || value instanceof Collection<?> c && c.isEmpty();
        }
    }

    /** The observed problems in the oracle's form, sorted. */
    private static List<Expected> observed(List<ManifestProblem> problems) {
        return problems.stream().map(p -> new Expected(p.code(), p.subject(), detail(p))).sorted().toList();
    }

    private static String detail(ManifestProblem p) {
        return switch (p.code()) {
            case MISSING_FIELD, FORBIDDEN_FIELD, INVALID_VALUE -> firstField(p.message());
            case DUPLICATE_ID -> group(p.message(), "reported (\\d+) times");
            case CONFLICTING_RULES -> group(p.message(), "equally specific rules match: (.+)$");
            case INVALID_ID -> group(p.message(), "^source (\\S+) reported");
            default -> "";
        };
    }

    /** The field the message names first. */
    private static String firstField(String message) {
        String first = "<no field: " + message + ">";
        int position = Integer.MAX_VALUE;
        for (Map.Entry<String, Pattern> field : FIELD_PATTERNS.entrySet()) {
            Matcher m = field.getValue().matcher(message);
            if (m.find() && m.start() < position) {
                position = m.start();
                first = field.getKey();
            }
        }
        return first;
    }

    private static String group(String message, String regex) {
        Matcher m = Pattern.compile(regex).matcher(message);
        return m.find() ? m.group(1) : "<unparsed: " + message + ">";
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Scenario> clean = Combinators.combine(
            item(false).list().ofMinSize(1).ofMaxSize(10).uniqueElements(ItemSpec::id),
            cleanRule().list().ofMaxSize(6).uniqueElements(r -> r.match().key()),
            catchAll(validAssignment()),
            Arbitraries.integers().between(0, 99))
            .as((items, rules, fallback, seed) -> new Scenario(items, append(rules, fallback), seed));
        Arbitrary<Scenario> noisy = Combinators.combine(
            item(true).list().ofMaxSize(10),
            noisyRule().list().ofMaxSize(8),
            catchAll(noisyAssignment()).injectNull(0.5),
            Arbitraries.integers().between(0, 99))
            .as((items, rules, fallback, seed) -> new Scenario(items, fallback == null ? rules : append(rules, fallback),
                                                              seed));
        return Arbitraries.frequencyOf(Tuple.of(2, clean), Tuple.of(3, noisy));
    }

    private static List<RuleSpec> append(List<RuleSpec> rules, RuleSpec rule) {
        List<RuleSpec> all = new ArrayList<>(rules);
        all.add(rule);
        return List.copyOf(all);
    }

    /**
     * Inventory items. Noisy items sometimes declare a kind other than their ID's pool, use an ID no kind accepts, or
     * have a blank V2 surface or source artifact.
     */
    private static Arbitrary<ItemSpec> item(boolean noisy) {
        Arbitrary<Integer> source = Arbitraries.integers().between(0, SOURCE_NAMES.size() - 1);
        Arbitrary<EntryKind> kind = Arbitraries.of(EntryKind.values());
        Arbitrary<Integer> index = Arbitraries.integers().between(0, 2);
        Arbitrary<Integer> kindShift = noisy ? Arbitraries.frequency(Tuple.of(10, 0), Tuple.of(1, 1), Tuple.of(1, 3))
                                             : Arbitraries.just(0);
        Arbitrary<Boolean> malformed = noisy ? Arbitraries.frequency(Tuple.of(20, false), Tuple.of(1, true))
                                             : Arbitraries.just(false);
        Arbitrary<Boolean> deprecated = Arbitraries.of(true, false);
        Arbitrary<String> surface = noisy ? Arbitraries.frequency(Tuple.of(20, SURFACE), Tuple.of(1, ""),
                                                                  Tuple.of(1, " "))
                                          : Arbitraries.just(SURFACE);
        Arbitrary<String> artifact = noisy ? Arbitraries.frequency(Tuple.of(20, ARTIFACT), Tuple.of(1, " "))
                                           : Arbitraries.just(ARTIFACT);
        return Combinators.combine(source, kind, index, kindShift, malformed, deprecated, surface, artifact)
                          .as(ItemSpec::create);
    }

    /** Exact-ID or kind rules with valid assignments; kept unique by {@link MatchSpec#key()}, so none tie. */
    private static Arbitrary<RuleSpec> cleanRule() {
        Arbitrary<Boolean> deprecated = Arbitraries.of(true, false).injectNull(0.7);
        Arbitrary<MatchSpec> exact = Combinators.combine(Arbitraries.of(VALID_IDS), deprecated)
                                                .as((id, d) -> new MatchSpec(id, null, d));
        Arbitrary<MatchSpec> byKind = Combinators.combine(Arbitraries.of(EntryKind.values()), deprecated)
                                                 .as((k, d) -> new MatchSpec(null, k, d));
        return Combinators.combine(Arbitraries.frequencyOf(Tuple.of(3, exact), Tuple.of(1, byKind)), validAssignment())
                          .as((m, a) -> new RuleSpec(m, false, a));
    }

    /** Rules of both tiers matching exact IDs, globs, malformed IDs, kinds and deprecation, with noisy assignments. */
    private static Arbitrary<RuleSpec> noisyRule() {
        Arbitrary<String> id = Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.of(VALID_IDS)),
                                                       Tuple.of(3, Arbitraries.of(GLOBS)),
                                                       Tuple.of(1, Arbitraries.of(MALFORMED_IDS)))
                                          .injectNull(0.3);
        Arbitrary<EntryKind> kind = Arbitraries.of(EntryKind.values()).injectNull(0.6);
        Arbitrary<Boolean> deprecated = Arbitraries.of(true, false).injectNull(0.7);
        Arbitrary<Boolean> isDefault = Arbitraries.frequency(Tuple.of(4, false), Tuple.of(1, true));
        return Combinators.combine(id, kind, deprecated, isDefault, noisyAssignment())
                          .as((i, k, d, def, a) -> new RuleSpec(new MatchSpec(i, k, d), def, a))
                          // Rules outside the defaults tier must constrain something (ClassificationRules contract).
                          .filter(r -> r.isDefault() || r.match().constrains());
    }

    /** A default matching every item. */
    private static Arbitrary<RuleSpec> catchAll(Arbitrary<AssignmentSpec> assignment) {
        return assignment.map(a -> new RuleSpec(new MatchSpec(null, null, null), true, a));
    }

    /** A valid assignment followed, in two of five cases, by one to three mutations. */
    private static Arbitrary<AssignmentSpec> noisyAssignment() {
        Arbitrary<List<Mutation>> mutations = Arbitraries.frequencyOf(
            Tuple.of(2, Arbitraries.just(List.<Mutation>of())),
            Tuple.of(3, Arbitraries.of(Mutation.values()).list().ofMinSize(1).ofMaxSize(3)));
        return Combinators.combine(validAssignment(), mutations).as(AssignmentSpec::mutate);
    }

    /** An assignment satisfying the schema for every item. */
    private static Arbitrary<AssignmentSpec> validAssignment() {
        return Combinators.combine(Arbitraries.of(Classification.values()),
                                   Arbitraries.of(CompatibilityDimension.values()).set().ofMinSize(1),
                                   Arbitraries.of(VALID_IDS),
                                   Arbitraries.of(true, false),
                                   Arbitraries.of(true, false))
                          .as(ManifestClassificationPartitionPropertyTest::template);
    }

    private static AssignmentSpec template(Classification classification, Set<CompatibilityDimension> dimensionSet,
                                           String replacement, boolean first, boolean extras) {
        List<CompatibilityDimension> dimensions = List.copyOf(new TreeSet<>(dimensionSet));
        return switch (classification) {
            case PRESERVED -> new AssignmentSpec(classification, dimensions, "kept as in V2", List.of(),
                                                 extras ? ARTIFACT : null, null, null, null,
                                                 extras ? "supported" : null,
                                                 extras ? List.of("PreservedTest") : List.of());
            case ADAPTED -> new AssignmentSpec(classification, extras ? dimensions : List.of(), "changed in V3",
                                               List.of("returns a V3 type"), ARTIFACT, replacement,
                                               first ? MigrationKind.RECIPE : MigrationKind.DIAGNOSTIC,
                                               "apply the V3 recipe", null, List.of("AdaptedTest"));
            case EXCLUDED -> new AssignmentSpec(classification, List.of(), "unsupported in V3", List.of(), null,
                                                extras ? replacement : null,
                                                first ? MigrationKind.COMPILE_TIME : MigrationKind.DIAGNOSTIC,
                                                "remove the call", "deprecated in V2", List.of());
        };
    }

    private static Map<EntryKind, List<String>> idPools() {
        Map<EntryKind, List<String>> pools = new EnumMap<>(EntryKind.class);
        pools.put(EntryKind.ARTIFACT, List.of("artifact:software.amazon.awssdk:sdk-core",
                                              "artifact:software.amazon.awssdk:s3"));
        pools.put(EntryKind.SERVICE, List.of("service:s3", "service:dynamodb"));
        pools.put(EntryKind.PUBLIC_TYPE, List.of("api:software.amazon.awssdk.core.SdkClient",
                                                 "api:software.amazon.awssdk.services.s3.S3Client"));
        pools.put(EntryKind.PUBLIC_MEMBER, List.of(
            "api:software.amazon.awssdk.services.s3.S3Client#create()Lsoftware/amazon/awssdk/services/s3/S3Client;",
            "api:software.amazon.awssdk.core.SdkClient#serviceName()Ljava/lang/String;",
            "api:software.amazon.awssdk.core.SdkSystemSetting#AWS_REGION:Lsoftware/amazon/awssdk/core/SdkSystemSetting;"));
        pools.put(EntryKind.CONFIGURATION, List.of("config:env:AWS_REGION", "config:profile:s3_use_arn_region"));
        pools.put(EntryKind.HTTP_MODE, List.of("http-mode:apache-client:sync", "http-mode:netty-nio-client:async"));
        pools.put(EntryKind.BEHAVIOR, List.of("behavior:retry.standard", "behavior:s3.checksum-validation"));
        return Collections.unmodifiableMap(pools);
    }

    private static Map<String, Pattern> fieldPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        for (String field : FIELDS) {
            patterns.put(field, Pattern.compile("\\b" + Pattern.quote(field) + "\\b"));
        }
        return Collections.unmodifiableMap(patterns);
    }
}
