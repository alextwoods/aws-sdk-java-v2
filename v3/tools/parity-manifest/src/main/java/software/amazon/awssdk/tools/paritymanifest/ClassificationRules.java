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

import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.CONFLICTING_RULES;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.MALFORMED;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.UNMATCHED;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The human-maintained classification rules ({@code classification-rules.json}, schema
 * {@code classification-rules-1.schema.json}) that turn raw inventory items into classified entries.
 *
 * <p>A rule has a unique {@code name}, a {@code match} and an assignment (classification, dimensions, rationale,
 * differences, V3 artifact, replacement, migration kind and text, support evidence, evidence IDs). A match may
 * constrain the entry {@code id} (exact, or a glob where {@code *} matches any characters), the {@code kind}, and
 * the V2 {@code deprecated} flag; an entry matches when every given constraint holds.
 *
 * <p>Precedence: any rule in {@code rules} beats any rule in {@code defaults}, which apply only to entries no rule
 * matches. Within a tier the most specific match wins, comparing in order: an exact ID beats a glob, which beats no
 * ID constraint; between globs, more literal (non-{@code *}) characters win; then a kind constraint beats none; then
 * a deprecated constraint beats none. Several matches of equal highest specificity are a
 * {@link ManifestProblem.Code#CONFLICTING_RULES} problem, even when they agree. An entry matched by nothing is
 * {@link ManifestProblem.Code#UNMATCHED}. Rules in {@code rules} must constrain something; defaults may match all.
 */
public final class ClassificationRules {
    /** The classpath location of the checked-in rules. */
    public static final String DEFAULT_RESOURCE = "/software/amazon/awssdk/tools/paritymanifest/classification-rules.json";
    /** The schema version of the rules JSON form. */
    public static final int SCHEMA_VERSION = 1;

    private static final Set<String> TOP = Set.of("schemaVersion", "comment", "rules", "defaults");
    private static final Set<String> RULE = Set.of("name", "comment", "match", "classification", "dimensions",
                                                   "rationale", "differences", "v3Artifact", "replacement",
                                                   "migrationKind", "migration", "supportEvidence", "evidenceIds");
    private static final Set<String> MATCH = Set.of("id", "kind", "deprecated");

    private static final Map<String, Pattern> GLOBS = new ConcurrentHashMap<>();

    private final List<Rule> rules;

    private ClassificationRules(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Which entries a rule applies to; {@code null} components are unconstrained.
     *
     * @param id         exact ID or {@code *} glob
     * @param kind       entry kind
     * @param deprecated V2 deprecation flag
     */
    public record Match(String id, EntryKind kind, Boolean deprecated) {
        boolean matches(InventoryItem item) {
            return (id == null || glob(id).matcher(item.id()).matches())
                   && (kind == null || kind == item.kind())
                   && (deprecated == null || deprecated == item.deprecated());
        }

        Specificity specificity(boolean isDefault) {
            int idRank = id == null ? 0 : id.contains("*") ? 1 : 2;
            int literal = id == null ? 0 : id.replace("*", "").length();
            return new Specificity(isDefault ? 0 : 1, idRank, literal, kind == null ? 0 : 1, deprecated == null ? 0 : 1);
        }

        private static Pattern glob(String glob) {
            // Inventories hold one item per public member, so each glob is compiled once.
            return GLOBS.computeIfAbsent(glob, g -> Pattern.compile(Pattern.quote(g).replace("*", "\\E.*\\Q")));
        }
    }

    /**
     * The values a rule assigns to every entry it classifies.
     */
    public record Assignment(Classification classification,
                             List<CompatibilityDimension> dimensions,
                             String rationale,
                             List<String> differences,
                             String v3Artifact,
                             String replacement,
                             MigrationKind migrationKind,
                             String migration,
                             String supportEvidence,
                             List<String> evidenceIds) {
        public Assignment {
            Objects.requireNonNull(classification, "classification");
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            differences = differences == null ? List.of() : List.copyOf(differences);
            evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        }
    }

    /**
     * One rule.
     *
     * @param name      unique rule name
     * @param match     the entries it applies to
     * @param assignment what it assigns
     * @param isDefault true for a rule of the {@code defaults} tier
     */
    public record Rule(String name, Match match, Assignment assignment, boolean isDefault) {
    }

    record Specificity(int tier, int idRank, int literal, int kind, int deprecated)
        implements Comparable<Specificity> {
        private static final Comparator<Specificity> ORDER =
            Comparator.comparingInt(Specificity::tier).thenComparingInt(Specificity::idRank)
                      .thenComparingInt(Specificity::literal).thenComparingInt(Specificity::kind)
                      .thenComparingInt(Specificity::deprecated);

        @Override
        public int compareTo(Specificity o) {
            return ORDER.compare(this, o);
        }
    }

    /**
     * The outcome of classifying an inventory.
     *
     * @param entries      one classified entry per item that exactly one most specific rule matched
     * @param ruleByEntry  the name of the rule that classified each entry, by entry ID
     * @param problems     unmatched and conflicting items
     */
    public record Classified(List<ManifestEntry> entries, Map<String, String> ruleByEntry,
                             List<ManifestProblem> problems) {
    }

    /** Rules built in code. */
    public static ClassificationRules of(List<Rule> rules) {
        return new ClassificationRules(rules);
    }

    /** The checked-in rules on the classpath ({@link #DEFAULT_RESOURCE}). */
    public static ClassificationRules loadDefault() throws ManifestValidationException {
        try (InputStream in = ClassificationRules.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + DEFAULT_RESOURCE);
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Parses a rules document, reporting every malformed rule at once. */
    public static ClassificationRules parse(String json) throws ManifestValidationException {
        List<ManifestProblem> problems = new ArrayList<>();
        Node root;
        try {
            root = Node.parse(json);
        } catch (RuntimeException e) {
            throw new ManifestValidationException(List.of(new ManifestProblem(MALFORMED, "$", e.getMessage())));
        }
        ObjectNode top = JsonFields.object(root, "$", "the rules document", problems);
        List<Rule> rules = new ArrayList<>();
        if (top != null) {
            JsonFields fields = new JsonFields(top, "$", TOP, problems);
            Integer version = fields.integer("schemaVersion");
            if (version == null || version != SCHEMA_VERSION) {
                fields.bad("schemaVersion", "must be " + SCHEMA_VERSION);
            }
            readTier(fields, "rules", false, rules, problems);
            readTier(fields, "defaults", true, rules, problems);
            Set<String> names = new HashSet<>();
            for (Rule rule : rules) {
                if (!names.add(rule.name())) {
                    problems.add(new ManifestProblem(MALFORMED, "rule:" + rule.name(), "duplicate rule name"));
                }
            }
        }
        ManifestValidationException.throwIfAny(problems);
        return new ClassificationRules(rules);
    }

    private static void readTier(JsonFields fields, String member, boolean isDefault, List<Rule> out,
                                 List<ManifestProblem> problems) {
        Node tier = fields.member(member).orElse(Node.arrayNode());
        if (!tier.isArrayNode()) {
            fields.bad(member, "must be an array");
            return;
        }
        int index = 0;
        for (Node element : tier.expectArrayNode().getElements()) {
            String fallback = "rule:" + member + "[" + index++ + "]";
            ObjectNode node = JsonFields.object(element, fallback, "a rule", problems);
            if (node != null) {
                Rule rule = readRule(node, fallback, isDefault, problems);
                if (rule != null) {
                    out.add(rule);
                }
            }
        }
    }

    private static Rule readRule(ObjectNode node, String fallback, boolean isDefault, List<ManifestProblem> problems) {
        String name = node.getStringMember("name").map(s -> s.getValue()).orElse(null);
        String subject = name == null ? fallback : "rule:" + name;
        int before = problems.size();
        JsonFields f = new JsonFields(node, subject, RULE, problems);
        f.requiredString("name");
        Match match = new Match(null, null, null);
        boolean constrained = false;
        if (f.has("match")) {
            ObjectNode m = JsonFields.object(f.member("match").get(), subject, "'match'", problems);
            if (m != null) {
                JsonFields mf = new JsonFields(m, subject, MATCH, problems);
                constrained = MATCH.stream().anyMatch(mf::has);
                match = new Match(mf.string("id"), mf.enumValue("kind", EntryKind::valueOf), mf.bool("deprecated"));
            }
        }
        if (!isDefault && !constrained) {
            problems.add(new ManifestProblem(MALFORMED, subject, "a rule must constrain id, kind or deprecated; "
                                                                 + "put match-all rules under 'defaults'"));
        }
        Classification classification = f.enumValue("classification", Classification::valueOf);
        if (classification == null && !f.has("classification")) {
            f.bad("classification", "is required");
        }
        List<CompatibilityDimension> dimensions = new ArrayList<>();
        for (String d : f.strings("dimensions")) {
            try {
                dimensions.add(CompatibilityDimension.fromJsonName(d));
            } catch (IllegalArgumentException e) {
                f.bad("dimensions", "has unknown value '" + d + "'");
            }
        }
        Assignment assignment = classification == null ? null : new Assignment(
            classification, dimensions, f.string("rationale"), f.strings("differences"), f.string("v3Artifact"),
            f.string("replacement"), f.enumValue("migrationKind", MigrationKind::valueOf), f.string("migration"),
            f.string("supportEvidence"), f.strings("evidenceIds"));
        return problems.size() == before ? new Rule(name, match, assignment, isDefault) : null;
    }

    /** The rules, in document order. */
    public List<Rule> rules() {
        return rules;
    }

    /** Classifies every item, reporting unmatched and conflicting items at once. */
    public Classified classify(List<InventoryItem> items) {
        List<ManifestEntry> entries = new ArrayList<>();
        Map<String, String> ruleByEntry = new LinkedHashMap<>();
        List<ManifestProblem> problems = new ArrayList<>();
        for (InventoryItem item : items) {
            List<Rule> matched = rules.stream().filter(r -> r.match().matches(item)).toList();
            if (matched.isEmpty()) {
                problems.add(new ManifestProblem(UNMATCHED, item.id(), "no classification rule or default matches this "
                                                                       + (item.deprecated() ? "deprecated " : "")
                                                                       + item.kind() + " entry"));
                continue;
            }
            Specificity best = matched.stream().map(r -> r.match().specificity(r.isDefault()))
                                      .max(Comparator.naturalOrder()).orElseThrow();
            List<Rule> winners = matched.stream().filter(r -> r.match().specificity(r.isDefault()).equals(best))
                                        .toList();
            if (winners.size() > 1) {
                problems.add(new ManifestProblem(CONFLICTING_RULES, item.id(), "equally specific rules match: "
                                                                               + winners.stream().map(Rule::name)
                                                                                        .sorted()
                                                                                        .collect(Collectors.joining(", "))));
                continue;
            }
            Rule rule = winners.get(0);
            entries.add(apply(item, rule.assignment()));
            ruleByEntry.put(item.id(), rule.name());
        }
        return new Classified(entries, ruleByEntry, problems.stream().sorted().toList());
    }

    private static ManifestEntry apply(InventoryItem item, Assignment a) {
        return new ManifestEntry(item.id(), item.kind(), item.v2Surface(), item.source(), item.deprecated(),
                                 a.classification(), new TreeSet<>(a.dimensions()), a.rationale(),
                                 a.differences(), a.v3Artifact(), a.replacement(), a.migrationKind(), a.migration(),
                                 a.supportEvidence(), new TreeSet<>(a.evidenceIds()));
    }
}
