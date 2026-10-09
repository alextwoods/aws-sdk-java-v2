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

package software.amazon.awssdk.test.fleet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * The checked-in record of intentional V3-versus-V2 wire deltas ({@code wire-differential/intentional-deltas.json},
 * format version 1, documented in {@code wire-differential/README.md}; Property 62 checks it).
 *
 * <p>Each record names the delta it covers ({@code case} and {@code aspect} globs over
 * {@code <service>/<operation>/<category>} and the aspect names of {@link Wire.Request#aspects()} and the response
 * observation) and the Parity Manifest entry that makes it intentional: {@code manifestEntry} (a Parity Manifest entry
 * ID such as {@code behavior:telemetry.user-agent}) and {@code rule} (the name of the classification rule of
 * {@code tools/parity-manifest/.../classification-rules.json} that classifies that entry). A record is valid only when
 * the rule exists and classifies {@code ADAPTED} or {@code EXCLUDED}, and the Parity Manifest's own precedence
 * ({@code ClassificationRules}: the {@code rules} tier over {@code defaults}; then an exact ID over a glob over no ID;
 * more literal characters; a kind constraint; a deprecation constraint) makes it the single most specific rule that
 * matches the entry, whatever the entry's V2 deprecation flag (which a record cannot name), and the rationale is not
 * blank. A Parity Manifest <em>known gap</em> ({@code high-level-library-matrix.json}) is not a classification: a
 * record of a known-gap entry is invalid, so the delta stays unclassified and fails. Only valid records classify a
 * delta ({@link #classify}).
 */
final class IntentionalDeltas {
    static final Path RULES = Fleet.V3_ROOT.resolve(
        "tools/parity-manifest/src/main/resources/software/amazon/awssdk/tools/paritymanifest/classification-rules.json");
    static final Path MATRIX = Fleet.V3_ROOT.resolve(
        "tools/parity-manifest/src/main/resources/software/amazon/awssdk/tools/paritymanifest/"
        + "high-level-library-matrix.json");
    private static final Set<String> INTENTIONAL = Set.of("ADAPTED", "EXCLUDED");
    /** Entry ID prefixes of the Parity Manifest entry kinds ({@code EntryKind}); {@code api:} is resolved apart. */
    private static final Map<String, String> KIND_PREFIXES = Map.of("artifact:", "ARTIFACT", "service:", "SERVICE",
                                                                    "config:", "CONFIGURATION",
                                                                    "http-mode:", "HTTP_MODE",
                                                                    "behavior:", "BEHAVIOR");

    /** One record. */
    record Delta(String id, Pattern cases, Pattern aspect, String manifestEntry, String rule, String rationale) {
        boolean covers(String caseId, String aspectName) {
            return cases.matcher(caseId).matches() && aspect.matcher(aspectName).matches();
        }
    }

    /** Why a record is not an intentional classification. */
    enum Reason {
        BLANK_RATIONALE,
        /** The entry is a Parity Manifest known gap. */
        KNOWN_GAP,
        NO_SUCH_RULE,
        /** The named rule classifies PRESERVED (or anything but ADAPTED or EXCLUDED). */
        NOT_INTENTIONAL,
        RULE_DOES_NOT_MATCH,
        /** The named rule matches the entry only for one V2 deprecation flag. */
        DEPRECATION_DEPENDENT,
        /** A more specific rule matches the entry. */
        OVERRIDDEN,
        /** An equally specific rule matches the entry (a Parity Manifest CONFLICTING_RULES problem). */
        CONFLICTING
    }

    /** One problem of record {@code delta}. */
    record Problem(String delta, Reason reason, String detail) {
        @Override
        public String toString() {
            return delta + ": " + detail;
        }
    }

    /**
     * What records are checked against: a classification rules document (the {@code classification-rules.json} form)
     * and the known-gap entry globs. Package-private so Property 62 can inject generated ones.
     */
    record Sources(ObjectNode rules, List<String> knownGaps) {
        /** The checked-in rules and the known gaps of the checked-in high-level-library matrix. */
        static Sources checkedIn() {
            List<String> gaps = new ArrayList<>();
            for (Node library : read(MATRIX).expectArrayMember("libraries").getElements()) {
                for (Node gap : library.expectObjectNode().getArrayMember("knownGaps").map(a -> a.getElements())
                                       .orElse(List.of())) {
                    gap.expectObjectNode().expectArrayMember("entries").getElements()
                       .forEach(e -> gaps.add(e.expectStringNode().getValue()));
                }
            }
            return new Sources(read(RULES), List.copyOf(gaps));
        }
    }

    /** A classification rule; null match components are unconstrained. */
    private record Rule(String name, boolean isDefault, String id, String kind, Boolean deprecated,
                        String classification) {
        private static final Comparator<Rule> PRECEDENCE =
            Comparator.comparingInt((Rule r) -> r.isDefault() ? 0 : 1)
                      .thenComparingInt(r -> r.id() == null ? 0 : r.id().contains("*") ? 1 : 2)
                      .thenComparingInt(r -> r.id() == null ? 0 : r.id().replace("*", "").length())
                      .thenComparingInt(r -> r.kind() == null ? 0 : 1)
                      .thenComparingInt(r -> r.deprecated() == null ? 0 : 1);

        boolean matches(String entry, String entryKind, boolean entryDeprecated) {
            return (id == null || glob(id).matcher(entry).matches()) && (kind == null || kind.equals(entryKind))
                   && (deprecated == null || deprecated == entryDeprecated);
        }
    }

    private IntentionalDeltas() {
    }

    /** The records of the checked-in file, in file order. */
    static List<Delta> load() {
        ObjectNode root;
        try (InputStream in = IntentionalDeltas.class.getResourceAsStream("/wire-differential/intentional-deltas.json")) {
            if (in == null) {
                throw new AssertionError("wire-differential/intentional-deltas.json is not on the test class path");
            }
            root = Node.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (root.expectNumberMember("formatVersion").getValue().intValue() != 1) {
            throw new AssertionError("Unsupported intentional-deltas.json formatVersion");
        }
        List<Delta> result = new ArrayList<>();
        for (Node node : root.expectArrayMember("deltas").getElements()) {
            ObjectNode d = node.expectObjectNode();
            d.expectNoAdditionalProperties(Set.of("id", "case", "aspect", "manifestEntry", "rule", "rationale"));
            result.add(new Delta(d.expectStringMember("id").getValue(), glob(d.expectStringMember("case").getValue()),
                                 glob(d.expectStringMember("aspect").getValue()),
                                 d.expectStringMember("manifestEntry").getValue(),
                                 d.expectStringMember("rule").getValue(),
                                 d.expectStringMember("rationale").getValue()));
        }
        return result;
    }

    /** Every problem of {@code delta} against the checked-in rules and known gaps (empty when it is valid). */
    static List<Problem> validate(Delta delta) {
        return validate(delta, Sources.checkedIn());
    }

    /** Every problem of {@code delta} against {@code sources} (empty when it is valid). */
    static List<Problem> validate(Delta delta, Sources sources) {
        Set<Problem> problems = new LinkedHashSet<>();
        String entry = delta.manifestEntry();
        if (delta.rationale().isBlank()) {
            problems.add(new Problem(delta.id(), Reason.BLANK_RATIONALE, "no rationale"));
        }
        sources.knownGaps().stream().filter(g -> glob(g).matcher(entry).matches()).findFirst().ifPresent(
            g -> problems.add(new Problem(delta.id(), Reason.KNOWN_GAP, entry + " is a Parity Manifest known gap (" + g
                                                                        + "), which is not a classification")));
        List<Rule> rules = rules(sources.rules());
        Optional<Rule> named = rules.stream().filter(r -> r.name().equals(delta.rule())).findFirst();
        if (named.isEmpty()) {
            problems.add(new Problem(delta.id(), Reason.NO_SUCH_RULE, "no classification rule named " + delta.rule()));
            return List.copyOf(problems);
        }
        Rule rule = named.get();
        if (!INTENTIONAL.contains(rule.classification())) {
            problems.add(new Problem(delta.id(), Reason.NOT_INTENTIONAL, "rule " + rule.name() + " classifies "
                                                                       + rule.classification()
                                                                       + ", not ADAPTED or EXCLUDED"));
        }
        String kind = kind(entry);
        boolean whenCurrent = rule.matches(entry, kind, false);
        boolean whenDeprecated = rule.matches(entry, kind, true);
        if (!whenCurrent && !whenDeprecated) {
            problems.add(new Problem(delta.id(), Reason.RULE_DOES_NOT_MATCH, "rule " + rule.name()
                                                                           + " does not match " + entry));
            return List.copyOf(problems);
        }
        if (whenCurrent != whenDeprecated) {
            problems.add(new Problem(delta.id(), Reason.DEPRECATION_DEPENDENT,
                                     "rule " + rule.name() + " matches " + entry + " only if it is "
                                     + (whenDeprecated ? "" : "not ") + "deprecated in V2, which a record cannot "
                                     + "state"));
            return List.copyOf(problems);
        }
        for (boolean entryDeprecated : new boolean[] {false, true}) {
            List<Rule> matching = rules.stream().filter(r -> r != rule && r.matches(entry, kind, entryDeprecated))
                                       .toList();
            String above = names(matching.stream().filter(r -> Rule.PRECEDENCE.compare(r, rule) > 0).toList());
            String equal = names(matching.stream().filter(r -> Rule.PRECEDENCE.compare(r, rule) == 0).toList());
            if (!above.isEmpty()) {
                problems.add(new Problem(delta.id(), Reason.OVERRIDDEN, "the more specific rule(s) " + above
                                                                       + " classify " + entry + ", not "
                                                                       + rule.name()));
            } else if (!equal.isEmpty()) {
                problems.add(new Problem(delta.id(), Reason.CONFLICTING, "rule(s) " + equal + " match " + entry
                                                                        + " as specifically as " + rule.name()
                                                                        + " (conflicting rules)"));
            }
        }
        return List.copyOf(problems);
    }

    /**
     * The record that classifies the observed delta {@code (caseId, aspect)}: the first valid record of
     * {@code records} that covers it. Empty means the delta is unclassified.
     */
    static Optional<Delta> classify(List<Delta> records, Sources sources, String caseId, String aspect) {
        return records.stream().filter(d -> d.covers(caseId, aspect) && validate(d, sources).isEmpty()).findFirst();
    }

    private static List<Rule> rules(ObjectNode document) {
        List<Rule> result = new ArrayList<>();
        for (String tier : List.of("rules", "defaults")) {
            for (Node node : document.getArrayMember(tier).map(a -> a.getElements()).orElse(List.of())) {
                ObjectNode rule = node.expectObjectNode();
                ObjectNode match = rule.getObjectMember("match").orElse(Node.objectNode());
                result.add(new Rule(rule.expectStringMember("name").getValue(), tier.equals("defaults"),
                                    match.getStringMember("id").map(StringNode::getValue).orElse(null),
                                    match.getStringMember("kind").map(StringNode::getValue).orElse(null),
                                    match.getBooleanMember("deprecated").map(b -> b.getValue()).orElse(null),
                                    rule.expectStringMember("classification").getValue()));
            }
        }
        return result;
    }

    /** The Parity Manifest entry kind of an entry ID, or null for an ID of no kind. */
    private static String kind(String entryId) {
        if (entryId.startsWith("api:")) {
            return entryId.indexOf('#') >= 0 ? "PUBLIC_MEMBER" : "PUBLIC_TYPE";
        }
        return KIND_PREFIXES.entrySet().stream().filter(e -> entryId.startsWith(e.getKey())).map(Map.Entry::getValue)
                            .findFirst().orElse(null);
    }

    private static String names(List<Rule> rules) {
        return rules.stream().map(Rule::name).sorted().collect(Collectors.joining(", "));
    }

    private static ObjectNode read(Path file) {
        try {
            return Node.parse(Files.readString(file, StandardCharsets.UTF_8)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the Parity Manifest file " + file, e);
        }
    }

    /** A glob ({@code *} matches any run of characters) as an anchored pattern. */
    static Pattern glob(String glob) {
        return Pattern.compile(java.util.Arrays.stream(glob.split("\\*", -1)).map(Pattern::quote)
                                               .collect(Collectors.joining(".*")));
    }
}
