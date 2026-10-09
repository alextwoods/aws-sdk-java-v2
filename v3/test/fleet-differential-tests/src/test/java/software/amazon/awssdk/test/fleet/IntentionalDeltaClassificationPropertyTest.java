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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
import software.amazon.awssdk.test.fleet.IntentionalDeltas.Reason;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Property 62 (Requirement 19.5): intentional wire differences are classified. A V3/V2 wire delta passes the fleet
 * wire differential only through an intentional-delta record that resolves to exactly one ADAPTED or EXCLUDED Parity
 * Manifest entry; every other delta is reported unclassified.
 *
 * <p>Each try generates a small universe of Parity Manifest entry IDs of several kinds (bodies over {@code a}, {@code b}
 * and {@code .}, so globs collide), a classification rule set in the {@code classification-rules.json} form (both
 * tiers; exact IDs, {@code *} globs cut from the entries, kind and deprecation constraints, ADAPTED, EXCLUDED and
 * PRESERVED), known-gap entry globs, intentional-delta records, and observed deltas (case ID x aspect). Records are
 * built to an intent: valid; naming a PRESERVED rule, a known gap, a missing rule, a rule whose ID does not match the
 * entry, a glob rule an exact-ID rule overrides, an equally specific twin, a deprecation-constrained rule (the record
 * cannot name the entry's V2 deprecation flag); a blank rationale; or a random rule of the set.
 *
 * <p>{@link Oracle} resolves every entry independently of {@link IntentionalDeltas}: its own {@code *} matcher, its own
 * rule precedence (the documented {@code ClassificationRules} order: rules tier over defaults, exact ID over glob over
 * none, more literal characters, kind constraint, deprecation constraint; equal best matches conflict), under both
 * possible deprecation flags, and the generator's own entry kinds. The property checks that
 * <ol>
 *   <li>(a) every record {@link IntentionalDeltas#validate} accepts, and every observed delta
 *       {@link IntentionalDeltas#classify} classifies, resolves to exactly the named ADAPTED or EXCLUDED entry rule
 *       and to no known gap;</li>
 *   <li>(b) every invalid record is rejected with exactly the oracle's reasons (and the reason its intent guarantees);
 *       </li>
 *   <li>(c) an observed delta is classified by exactly the first valid record covering it, so every delta not covered
 *       by a valid record (none, or only invalid ones) is reported unclassified.</li>
 * </ol>
 */
class IntentionalDeltaClassificationPropertyTest {
    private static final List<String> CASES = List.of("s3/GetObject/LABEL", "s3/PutObject/PAYLOAD",
                                                      "sts/GetCallerIdentity/BODY", "dynamodb/GetItem/BODY");
    /** Record case globs; the last matches no case. */
    private static final List<String> CASE_GLOBS = List.of("s3/GetObject/LABEL", "s3/*", "*/BODY", "*",
                                                           "dynamodb/*/BODY", "sts/*/LABEL");
    private static final List<String> ASPECTS = List.of("request.path", "request.body", "request.header:content-type",
                                                        "response.exception");
    private static final List<String> ASPECT_GLOBS = List.of("request.path", "request.*", "*", "*.exception",
                                                             "request.header:*");
    private static final Set<String> INTENTIONAL = Set.of("ADAPTED", "EXCLUDED");

    /** The Parity Manifest entry kinds and their ID prefixes (EntryKind of tools/parity-manifest). */
    enum Kind {
        ARTIFACT("artifact:sdk:", ""),
        SERVICE("service:", ""),
        PUBLIC_TYPE("api:p.", ""),
        PUBLIC_MEMBER("api:p.", "#m()V"),
        CONFIGURATION("config:env:", ""),
        HTTP_MODE("http-mode:", ":sync"),
        BEHAVIOR("behavior:", "");

        private final String prefix;
        private final String suffix;

        Kind(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        String id(String body) {
            return prefix + body + suffix;
        }
    }

    enum IdMode { NONE, EXACT, GLOB }

    enum KindMode { NONE, OWN, OTHER }

    enum Deprecation { ANY, TRUE, FALSE }

    enum Intent {
        VALID, PRESERVED, KNOWN_GAP, MISSING_RULE, NON_MATCHING, OVERRIDDEN, TIE, BLANK_RATIONALE,
        DEPRECATION_DEPENDENT, EXISTING_RULE
    }

    record EntrySpec(Kind kind, String body) {
    }

    /** A glob cut from entry {@code entry}: the run {@code [start, start + length)} becomes {@code *}. */
    record GlobSpec(int entry, int start, int length) {
    }

    record RuleSpec(boolean isDefault, IdMode idMode, GlobSpec target, KindMode kindMode, Deprecation deprecation,
                    String classification) {
    }

    record RecordSpec(Intent intent, int entry, int other, GlobSpec cut, int caseGlob, int aspectGlob, int choice) {
    }

    record Observation(int caseIndex, int aspectIndex) {
    }

    record Scenario(List<EntrySpec> entries, List<RuleSpec> rules, List<GlobSpec> gaps, List<RecordSpec> records,
                    List<Observation> observations) {
    }

    /** A rule as the oracle sees it; null match components are unconstrained. */
    record Rule(String name, boolean isDefault, String id, Kind kind, Boolean deprecated, String classification) {
        Node toNode() {
            ObjectNode.Builder match = Node.objectNodeBuilder();
            if (id != null) {
                match.withMember("id", id);
            }
            if (kind != null) {
                match.withMember("kind", kind.name());
            }
            if (deprecated != null) {
                match.withMember("deprecated", Node.from(deprecated));
            }
            return Node.objectNodeBuilder().withMember("name", name).withMember("match", match.build())
                       .withMember("classification", classification).build();
        }
    }

    /** One generated record with the glob strings the oracle matches itself. */
    record Rec(Intent intent, String id, String caseGlob, String aspectGlob, String entry, String rule,
               String rationale, boolean equalLiteralOverride) {
        IntentionalDeltas.Delta delta() {
            return new IntentionalDeltas.Delta(id, IntentionalDeltas.glob(caseGlob), IntentionalDeltas.glob(aspectGlob),
                                               entry, rule, rationale);
        }
    }

    /** The built universe of one try. */
    record World(Map<String, Kind> kinds, List<Rule> rules, List<String> knownGaps, List<Rec> records,
                 List<String[]> observations) {
        IntentionalDeltas.Sources sources() {
            ArrayNode ruleTier = rules.stream().filter(r -> !r.isDefault()).map(Rule::toNode).collect(ArrayNode.collect());
            ArrayNode defaultTier = rules.stream().filter(Rule::isDefault).map(Rule::toNode)
                                         .collect(ArrayNode.collect());
            ObjectNode document = Node.objectNodeBuilder().withMember("schemaVersion", 1)
                                      .withMember("rules", ruleTier).withMember("defaults", defaultTier).build();
            return new IntentionalDeltas.Sources(document, List.copyOf(knownGaps));
        }

        Optional<Rule> rule(String name) {
            return rules.stream().filter(r -> r.name().equals(name)).findFirst();
        }

        String describe() {
            return "entries " + kinds + "\nrules\n  "
                   + rules.stream().map(Rule::toString).collect(Collectors.joining("\n  ")) + "\nknown gaps "
                   + knownGaps + "\nrecords\n  " + records.stream().map(Rec::toString)
                                                          .collect(Collectors.joining("\n  "));
        }

        static World build(Scenario s) {
            Map<String, Kind> kinds = new LinkedHashMap<>();
            for (EntrySpec e : s.entries()) {
                kinds.putIfAbsent(e.kind().id(e.body()), e.kind());
            }
            if (kinds.size() < 2) {
                kinds.putIfAbsent(Kind.BEHAVIOR.id("zz"), Kind.BEHAVIOR);
                kinds.putIfAbsent(Kind.SERVICE.id("zz"), Kind.SERVICE);
            }
            List<String> entries = List.copyOf(kinds.keySet());
            List<Rule> rules = new ArrayList<>();
            for (int i = 0; i < s.rules().size(); i++) {
                RuleSpec r = s.rules().get(i);
                String target = pick(entries, r.target().entry());
                String id = switch (r.idMode()) {
                    case NONE -> null;
                    case EXACT -> target;
                    case GLOB -> cut(target, r.target());
                };
                Kind own = kinds.get(target);
                Kind kind = switch (r.kindMode()) {
                    case NONE -> null;
                    case OWN -> own;
                    case OTHER -> Kind.values()[(own.ordinal() + 1) % Kind.values().length];
                };
                Boolean deprecated = deprecation(r.deprecation());
                if (!r.isDefault() && id == null && kind == null && deprecated == null) {
                    kind = own; // a rule of the rules tier must constrain something (ClassificationRules)
                }
                rules.add(new Rule("rule-" + i, r.isDefault(), id, kind, deprecated, r.classification()));
            }
            int generated = rules.size();
            List<String> gaps = new ArrayList<>();
            for (GlobSpec g : s.gaps()) {
                gaps.add(cut(pick(entries, g.entry()), g));
            }
            List<Rec> records = new ArrayList<>();
            for (int j = 0; j < s.records().size(); j++) {
                RecordSpec r = s.records().get(j);
                String entry = pick(entries, r.entry());
                String other = pick(entries, r.other());
                if (other.equals(entry)) {
                    other = entries.get((entries.indexOf(entry) + 1) % entries.size());
                }
                String intentional = r.choice() % 2 == 0 ? "ADAPTED" : "EXCLUDED";
                String anyClassification = List.of("ADAPTED", "EXCLUDED", "PRESERVED").get((r.choice() / 4) % 3);
                String name = "rec" + j + "-rule";
                boolean equalLiteral = false;
                switch (r.intent()) {
                    // An exact ID, sometimes with the entry's kind (which outranks another record's plain exact ID).
                    case VALID -> rules.add(new Rule(name, false, entry, (r.choice() / 2) % 3 == 0 ? kinds.get(entry)
                                                                                                   : null,
                                                     null, intentional));
                    case BLANK_RATIONALE -> rules.add(new Rule(name, false, entry, null, null, intentional));
                    case KNOWN_GAP -> {
                        rules.add(new Rule(name, false, entry, null, null, intentional));
                        gaps.add(cut(entry, r.cut()));
                    }
                    case PRESERVED -> rules.add(new Rule(name, false, entry, null, null, "PRESERVED"));
                    case MISSING_RULE -> name = "rec" + j + "-missing";
                    case NON_MATCHING -> rules.add(new Rule(name, false, other, null, null, intentional));
                    case OVERRIDDEN -> {
                        String glob = cut(entry, r.cut());
                        equalLiteral = literal(glob) == entry.length();
                        rules.add(new Rule(name, false, glob, null, null, intentional));
                        rules.add(new Rule("rec" + j + "-override", false, entry, null, null, anyClassification));
                    }
                    case TIE -> {
                        rules.add(new Rule(name, false, entry, null, null, intentional));
                        rules.add(new Rule("rec" + j + "-twin", false, entry, null, null, anyClassification));
                    }
                    case DEPRECATION_DEPENDENT ->
                        rules.add(new Rule(name, false, entry, null, (r.choice() / 2) % 2 == 0, intentional));
                    case EXISTING_RULE -> name = generated == 0 ? "rec" + j + "-missing"
                                                                : rules.get(r.choice() % generated).name();
                    default -> throw new IllegalStateException(r.intent().name());
                }
                String rationale = r.intent() != Intent.BLANK_RATIONALE ? "rationale of rec" + j
                                                                        : r.choice() % 2 == 0 ? "" : " \t";
                records.add(new Rec(r.intent(), "rec" + j, pick(CASE_GLOBS, r.caseGlob()),
                                    pick(ASPECT_GLOBS, r.aspectGlob()), entry, name, rationale, equalLiteral));
            }
            List<String[]> observations = s.observations().stream()
                                           .map(o -> new String[] {pick(CASES, o.caseIndex()),
                                                                   pick(ASPECTS, o.aspectIndex())})
                                           .toList();
            return new World(kinds, rules, gaps, records, observations);
        }

        private static Boolean deprecation(Deprecation d) {
            return switch (d) {
                case ANY -> null;
                case TRUE -> Boolean.TRUE;
                case FALSE -> Boolean.FALSE;
            };
        }
    }

    /**
     * Independent resolution: its own glob matcher and precedence, the generator's entry kinds, and both deprecation
     * flags (a record cannot name the flag, so it must resolve the same way under either).
     */
    static final class Oracle {
        private Oracle() {
        }

        /** {@code *} matches any run of characters; everything else matches itself. Iterative wildcard matching. */
        static boolean globMatches(String glob, String text) {
            int g = 0;
            int t = 0;
            int star = -1;
            int resume = 0;
            while (t < text.length()) {
                if (g < glob.length() && glob.charAt(g) != '*' && glob.charAt(g) == text.charAt(t)) {
                    g++;
                    t++;
                } else if (g < glob.length() && glob.charAt(g) == '*') {
                    star = g++;
                    resume = t;
                } else if (star >= 0) {
                    g = star + 1;
                    t = ++resume;
                } else {
                    return false;
                }
            }
            while (g < glob.length() && glob.charAt(g) == '*') {
                g++;
            }
            return g == glob.length();
        }

        static boolean matches(Rule r, String entry, Kind kind, boolean deprecated) {
            return (r.id() == null || globMatches(r.id(), entry)) && (r.kind() == null || r.kind() == kind)
                   && (r.deprecated() == null || r.deprecated() == deprecated);
        }

        /** Tier, ID rank (exact 2, glob 1, none 0), literal characters, kind constraint, deprecation constraint. */
        static int[] rank(Rule r) {
            int idRank = r.id() == null ? 0 : r.id().indexOf('*') >= 0 ? 1 : 2;
            return new int[] {r.isDefault() ? 0 : 1, idRank, r.id() == null ? 0 : literal(r.id()),
                              r.kind() == null ? 0 : 1, r.deprecated() == null ? 0 : 1};
        }

        static int compare(int[] a, int[] b) {
            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i]) {
                    return Integer.compare(a[i], b[i]);
                }
            }
            return 0;
        }

        /** The names of the most specific matching rules (one is a classification, several a conflict). */
        static Set<String> winners(World w, String entry, boolean deprecated) {
            Kind kind = w.kinds().get(entry);
            List<Rule> matching = w.rules().stream().filter(r -> matches(r, entry, kind, deprecated)).toList();
            int[] best = null;
            for (Rule r : matching) {
                if (best == null || compare(rank(r), best) > 0) {
                    best = rank(r);
                }
            }
            Set<String> result = new TreeSet<>();
            for (Rule r : matching) {
                if (compare(rank(r), best) == 0) {
                    result.add(r.name());
                }
            }
            return result;
        }

        static boolean knownGap(World w, String entry) {
            return w.knownGaps().stream().anyMatch(g -> globMatches(g, entry));
        }

        /** Every reason the record is not an intentional classification (empty when it is one). */
        static Set<Reason> reasons(World w, Rec r) {
            Set<Reason> out = EnumSet.noneOf(Reason.class);
            if (r.rationale().strip().isEmpty()) {
                out.add(Reason.BLANK_RATIONALE);
            }
            if (knownGap(w, r.entry())) {
                out.add(Reason.KNOWN_GAP);
            }
            Optional<Rule> named = w.rule(r.rule());
            if (named.isEmpty()) {
                out.add(Reason.NO_SUCH_RULE);
                return out;
            }
            if (!INTENTIONAL.contains(named.get().classification())) {
                out.add(Reason.NOT_INTENTIONAL);
            }
            Kind kind = w.kinds().get(r.entry());
            boolean whenCurrent = matches(named.get(), r.entry(), kind, false);
            boolean whenDeprecated = matches(named.get(), r.entry(), kind, true);
            if (!whenCurrent && !whenDeprecated) {
                out.add(Reason.RULE_DOES_NOT_MATCH);
                return out;
            }
            if (whenCurrent != whenDeprecated) {
                out.add(Reason.DEPRECATION_DEPENDENT);
                return out;
            }
            for (boolean deprecated : new boolean[] {false, true}) {
                Set<String> winners = winners(w, r.entry(), deprecated);
                if (!winners.equals(Set.of(r.rule()))) {
                    out.add(winners.contains(r.rule()) ? Reason.CONFLICTING : Reason.OVERRIDDEN);
                }
            }
            return out;
        }

        /** (a): the record resolves, under either deprecation flag, to exactly its rule, which is intentional. */
        static boolean resolvesToIntentionalEntry(World w, Rec r) {
            Optional<Rule> named = w.rule(r.rule());
            return named.isPresent() && INTENTIONAL.contains(named.get().classification())
                   && winners(w, r.entry(), false).equals(Set.of(r.rule()))
                   && winners(w, r.entry(), true).equals(Set.of(r.rule()))
                   && !knownGap(w, r.entry()) && !r.rationale().strip().isEmpty();
        }

        static boolean covers(Rec r, String caseId, String aspect) {
            return globMatches(r.caseGlob(), caseId) && globMatches(r.aspectGlob(), aspect);
        }
    }

    // Feature: aws-sdk-java-v3, Property 62: Intentional wire differences are classified
    // Embedded edge cases are disabled: they collapse the rule sets and records to their first alternatives. The
    // statistics coverage checks fail the property if the generated mix of intents, verdicts and observations degrades.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 62: Intentional wire differences are classified")
    void intentionalWireDifferencesAreClassified(@ForAll("scenarios") Scenario scenario) {
        World world = World.build(scenario);
        IntentionalDeltas.Sources sources = world.sources();
        List<IntentionalDeltas.Delta> deltas = world.records().stream().map(Rec::delta).toList();
        Map<String, Boolean> valid = new LinkedHashMap<>();
        for (Rec r : world.records()) {
            List<IntentionalDeltas.Problem> problems = IntentionalDeltas.validate(r.delta(), sources);
            Set<Reason> actual = problems.stream().map(IntentionalDeltas.Problem::reason)
                                         .collect(Collectors.toCollection(() -> EnumSet.noneOf(Reason.class)));
            Set<Reason> expected = Oracle.reasons(world, r);
            // (b) invalid records are rejected with exactly the right reasons, each naming the record.
            assertEquals(expected, actual, () -> "Rejection reasons of " + r.id() + " " + problems + "\n"
                                                 + world.describe());
            assertTrue(problems.stream().allMatch(p -> p.delta().equals(r.id())), () -> problems.toString());
            assertTrue(guaranteed(r.intent(), actual), () -> r.intent() + " record " + r.id() + " was rejected for "
                                                             + actual + "\n" + world.describe());
            // (a) an accepted record resolves to exactly its ADAPTED or EXCLUDED rule.
            if (problems.isEmpty()) {
                assertTrue(Oracle.resolvesToIntentionalEntry(world, r),
                           () -> "Accepted " + r.id() + " does not resolve to an intentional entry\n" + world.describe());
            }
            valid.put(r.id(), problems.isEmpty());
            collectRecord(r, actual);
        }
        for (String[] observed : world.observations()) {
            String caseId = observed[0];
            String aspect = observed[1];
            Optional<String> classified = IntentionalDeltas.classify(deltas, sources, caseId, aspect)
                                                           .map(IntentionalDeltas.Delta::id);
            List<Rec> covering = world.records().stream().filter(r -> Oracle.covers(r, caseId, aspect)).toList();
            Optional<String> expected = covering.stream().filter(r -> Oracle.reasons(world, r).isEmpty())
                                                .map(Rec::id).findFirst();
            // (c) classified by exactly the first valid covering record; otherwise reported unclassified.
            assertEquals(expected, classified, () -> "Classification of " + caseId + " " + aspect + "\n"
                                                     + world.describe());
            classified.ifPresent(id -> assertTrue(
                valid.get(id) && Oracle.resolvesToIntentionalEntry(
                    world, world.records().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow()),
                () -> "Classified by " + id + ", which is no intentional entry"));
            if (classified.isPresent()) {
                Statistics.label("observed delta").collect("classified");
            } else {
                Statistics.label("observed delta").collect(covering.isEmpty() ? "unclassified: no covering record"
                                                                              : "unclassified: only invalid records");
            }
        }
        Statistics.label("observed delta").coverage(c -> {
            c.check("classified").percentage(p -> p >= 5);
            c.check("unclassified: no covering record").percentage(p -> p >= 10);
            c.check("unclassified: only invalid records").percentage(p -> p >= 10);
        });
    }

    /** The reason a record's construction guarantees (VALID and EXISTING_RULE guarantee none). */
    private static boolean guaranteed(Intent intent, Set<Reason> reasons) {
        return switch (intent) {
            case VALID, EXISTING_RULE -> true;
            case PRESERVED -> reasons.contains(Reason.NOT_INTENTIONAL);
            case KNOWN_GAP -> reasons.contains(Reason.KNOWN_GAP);
            case MISSING_RULE -> reasons.contains(Reason.NO_SUCH_RULE);
            case NON_MATCHING -> reasons.contains(Reason.RULE_DOES_NOT_MATCH);
            case OVERRIDDEN -> reasons.contains(Reason.OVERRIDDEN);
            // A random rule may be more specific than both twins.
            case TIE -> reasons.contains(Reason.CONFLICTING) || reasons.contains(Reason.OVERRIDDEN);
            case BLANK_RATIONALE -> reasons.contains(Reason.BLANK_RATIONALE);
            case DEPRECATION_DEPENDENT -> reasons.contains(Reason.DEPRECATION_DEPENDENT);
        };
    }

    private static void collectRecord(Rec r, Set<Reason> reasons) {
        Statistics.label("record intent").collect(r.intent().name());
        Statistics.label("record intent").coverage(c -> {
            for (Intent intent : Intent.values()) {
                c.check(intent.name()).percentage(p -> p >= 4);
            }
        });
        if (reasons.isEmpty()) {
            Statistics.label("record verdict").collect("valid");
        }
        for (Reason reason : reasons) {
            Statistics.label("record verdict").collect(reason.name());
        }
        Statistics.label("record verdict").coverage(c -> {
            c.check("valid").percentage(p -> p >= 8);
            for (Reason reason : Reason.values()) {
                c.check(reason.name()).count(n -> n >= 10);
            }
        });
        if (r.intent() == Intent.OVERRIDDEN) {
            // The glob keeps every character of the exact ID (the '*' stands for nothing): only the ID rank decides.
            Statistics.label("override by an exact ID of equal literal length").collect(r.equalLiteralOverride());
            Statistics.label("override by an exact ID of equal literal length")
                      .coverage(c -> c.check(true).count(n -> n >= 5));
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Integer> index = Arbitraries.integers().between(0, 20);
        Arbitrary<EntrySpec> entry = Combinators.combine(Arbitraries.of(Kind.class),
                                                         Arbitraries.strings().withChars("ab.").ofMinLength(1)
                                                                    .ofMaxLength(3))
                                                .as(EntrySpec::new);
        Arbitrary<GlobSpec> glob = Combinators.combine(index, index, Arbitraries.integers().between(0, 6))
                                              .as(GlobSpec::new);
        Arbitrary<RuleSpec> rule = Combinators.combine(
            Arbitraries.frequency(Tuple.of(1, true), Tuple.of(3, false)),
            Arbitraries.frequency(Tuple.of(1, IdMode.NONE), Tuple.of(2, IdMode.EXACT), Tuple.of(3, IdMode.GLOB)),
            glob,
            Arbitraries.frequency(Tuple.of(4, KindMode.NONE), Tuple.of(2, KindMode.OWN), Tuple.of(1, KindMode.OTHER)),
            Arbitraries.frequency(Tuple.of(4, Deprecation.ANY), Tuple.of(1, Deprecation.TRUE),
                                  Tuple.of(1, Deprecation.FALSE)),
            Arbitraries.of("ADAPTED", "EXCLUDED", "PRESERVED"))
                                              .as(RuleSpec::new);
        // VALID is weighted up: other records' rules and random rules often override or tie with it.
        Arbitrary<Intent> intent = Arbitraries.frequencyOf(
            Tuple.of(6, Arbitraries.just(Intent.VALID)),
            Tuple.of(9, Arbitraries.of(Intent.class).filter(i -> i != Intent.VALID)));
        Arbitrary<RecordSpec> record = Combinators.combine(intent, index, index, glob, index,
                                                           index, Arbitraries.integers().between(0, 999))
                                                  .as(RecordSpec::new);
        Arbitrary<Observation> observation = Combinators.combine(index, index).as(Observation::new);
        return Combinators.combine(entry.list().ofMinSize(3).ofMaxSize(8),
                                   rule.list().ofMinSize(0).ofMaxSize(6),
                                   Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.just(List.<GlobSpec>of())),
                                                           Tuple.of(1, glob.list().ofMinSize(1).ofMaxSize(2))),
                                   record.list().ofMinSize(1).ofMaxSize(4),
                                   observation.list().ofMinSize(1).ofMaxSize(6))
                          .as(Scenario::new);
    }

    private static <T> T pick(List<T> values, int index) {
        return values.get(index % values.size());
    }

    /** {@code text} with the run {@code [start, start + length)} (both reduced into range) replaced by {@code *}. */
    private static String cut(String text, GlobSpec spec) {
        int start = spec.start() % (text.length() + 1);
        int length = spec.length() % (text.length() - start + 1);
        return text.substring(0, start) + "*" + text.substring(start + length);
    }

    private static int literal(String glob) {
        int n = 0;
        for (int i = 0; i < glob.length(); i++) {
            if (glob.charAt(i) != '*') {
                n++;
            }
        }
        return n;
    }
}
