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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code;

/**
 * Property 57: Adapted replacements exist in the candidate (Requirement 16.12).
 *
 * <p>Each try generates a release candidate (artifacts with their {@code api:} types and members, generated services,
 * HTTP providers and declared behaviors/settings, plus capabilities declared outside any artifact) and a Parity
 * Manifest of {@code ADAPTED} entries (and some {@code EXCLUDED} entries naming an optional replacement), deprecated or
 * not, whose replacements are shipped IDs, near misses of shipped IDs (the declaring type of a shipped member, a member
 * with the same name and another descriptor, which satisfies a source-level {@code PRESERVED} check but must not
 * resolve a replacement, the other HTTP mode, another group's artifact), arbitrary IDs of the same
 * ID space, or the entry's own V2 ID.
 *
 * <p>The candidate inventory is built in code or, in about a quarter of the tries, scanned by
 * {@link CandidateInventoryScanner} from candidate artifact directories written for the try. {@link Oracle} indexes
 * the generated candidate on its own (plain string concatenation over the generated model, not {@link EntryKind} or
 * {@link CandidateInventory}) and resolves every replacement by exact membership. The property checks that:
 * <ul>
 *   <li>the inventory's ID index equals the oracle's index;</li>
 *   <li>at both gate stages, {@link LaunchGate} reports {@link Code#REPLACEMENT_NOT_SHIPPED} for exactly the entries
 *       whose replacement the oracle cannot resolve (and only at {@link LaunchGate.Stage#PACKAGING} severity, naming
 *       the replacement), so packaging passes the replacement check iff every replacement resolves;</li>
 *   <li>the per-category summaries count as shipped exactly the non-deprecated {@code ADAPTED} entries whose
 *       replacement resolves;</li>
 *   <li>{@link ManifestValidator} given the candidate's IDs reports {@code UNKNOWN_REPLACEMENT} for the same entries.</li>
 * </ul>
 */
class AdaptedReplacementsExistPropertyTest {
    private static final String CANDIDATE_VERSION = "3.0.0";
    private static final List<String> GROUPS = List.of("software.amazon.awssdk", "software.amazon.eventstream");
    private static final String FOREIGN_GROUP = "software.amazon.other";
    private static final List<String> ARTIFACT_IDS =
        List.of("sdk-core", "dynamodb", "s3", "apache-client", "netty-nio-client", "regions", "checksums", "transfer");
    private static final List<String> PACKAGES =
        List.of("software.amazon.awssdk.core", "software.amazon.awssdk.services.dynamodb",
                "software.amazon.awssdk.retries.api");
    private static final List<String> SIMPLE_NAMES = List.of("Client", "Result", "RetryStrategy", "Waiter",
                                                             "Client$Builder");
    /** Member suffixes: overloads and fields share names so that same-name, other-descriptor near misses occur. */
    private static final List<String> MEMBERS =
        List.of("get()Ljava/lang/String;", "get(Ljava/lang/String;)V", "<init>()V", "VALUE:I", "VALUE:J", "close()V");
    private static final List<String> DECLARABLE =
        List.of("behavior:retry.backoff", "behavior:checksum.request-calculation", "behavior:compression.request",
                "config:env:AWS_REGION", "config:profile:region", "config:system:aws.region");

    private static final List<String> TYPE_IDS = new ArrayList<>();
    private static final List<String> API_IDS = new ArrayList<>();
    /** Every ID a candidate can ship or a replacement can name. */
    private static final List<String> UNIVERSE = new ArrayList<>();

    static {
        for (String p : PACKAGES) {
            for (String n : SIMPLE_NAMES) {
                TYPE_IDS.add("api:" + p + "." + n);
            }
        }
        API_IDS.addAll(TYPE_IDS);
        for (String t : TYPE_IDS) {
            for (String m : MEMBERS) {
                API_IDS.add(t + "#" + m);
            }
        }
        UNIVERSE.addAll(API_IDS);
        UNIVERSE.addAll(DECLARABLE);
        for (String a : ARTIFACT_IDS) {
            for (String g : GROUPS) {
                UNIVERSE.add("artifact:" + g + ":" + a);
            }
            UNIVERSE.add("artifact:" + FOREIGN_GROUP + ":" + a);
            UNIVERSE.add("service:" + a);
            UNIVERSE.add("http-mode:" + a + ":sync");
            UNIVERSE.add("http-mode:" + a + ":async");
        }
    }

    // Feature: aws-sdk-java-v3, Property 57: Adapted replacements exist in the candidate
    @Property(tries = 300)
    @Label("Property 57: Adapted replacements exist in the candidate")
    void adaptedReplacementsExistInTheCandidate(@ForAll("scenarios") Scenario scenario) throws IOException {
        Set<String> index = Oracle.index(scenario.candidate());
        SortedSet<String> unresolved = Oracle.unresolved(scenario.entries(), index);
        ParityManifest manifest = new ParityManifest(Entries.BASELINE, new CandidateIdentity(CANDIDATE_VERSION, null),
                                                     scenario.entries());
        assertEquals(List.of(), ManifestValidator.validate(manifest, ManifestValidator.Options.DEFAULT),
                     "the generated manifest is structurally valid");

        CandidateInventory inventory = scenario.scan() ? scanned(scenario.candidate()) : built(scenario.candidate());
        assertEquals(new TreeSet<>(index), new TreeSet<>(inventory.ids()), "candidate inventory ID index");

        for (LaunchGate.Stage stage : LaunchGate.Stage.values()) {
            LaunchGateReport report = LaunchGate.builder().manifest(manifest).candidate(inventory).build()
                                                .evaluate(stage);
            List<LaunchProblem> notShipped = report.problems().stream()
                                                   .filter(p -> p.code() == Code.REPLACEMENT_NOT_SHIPPED).toList();
            assertEquals(List.copyOf(unresolved), notShipped.stream().map(LaunchProblem::subject).sorted().toList(),
                         stage + ": entries whose replacement is not shipped");
            for (LaunchProblem p : notShipped) {
                ManifestEntry e = manifest.entry(p.subject()).orElseThrow();
                assertEquals(LaunchGate.Stage.PACKAGING, p.stage(), "a missing replacement blocks packaging");
                assertTrue(p.message().contains(e.replacement()), "the problem names the replacement: " + p);
            }
            assertEquals(Oracle.nonDeprecatedAdapted(scenario.entries()).size(),
                         report.categories().stream().mapToInt(LaunchGateReport.CategorySummary::adapted).sum(),
                         stage + ": non-deprecated ADAPTED entries");
            assertEquals(Oracle.nonDeprecatedAdapted(scenario.entries()).stream()
                               .filter(e -> index.contains(e.replacement())).count(),
                         report.categories().stream().mapToInt(LaunchGateReport.CategorySummary::shipped).sum(),
                         stage + ": non-deprecated ADAPTED entries counted as shipped");
        }

        List<String> unknown = ManifestValidator.validate(manifest, new ManifestValidator.Options(false, inventory.ids()))
                                                .stream()
                                                .filter(p -> p.code() == ManifestProblem.Code.UNKNOWN_REPLACEMENT)
                                                .map(ManifestProblem::subject).sorted().toList();
        assertEquals(List.copyOf(unresolved), unknown, "ManifestValidator replacement resolution");

        collectStatistics(scenario, index);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle

    /** The independent candidate index and replacement resolution. */
    static final class Oracle {
        private Oracle() {
        }

        /** Every ID the generated candidate ships, derived from the generated model alone. */
        static Set<String> index(Candidate candidate) {
            Set<String> ids = new HashSet<>(candidate.declared());
            for (Artifact a : candidate.artifacts()) {
                ids.add("artifact:" + a.group() + ":" + a.artifactId());
                ids.addAll(a.apis());
                ids.addAll(a.declared());
                if (a.service()) {
                    ids.add("service:" + a.artifactId());
                }
                if (a.syncHttp()) {
                    ids.add("http-mode:" + a.artifactId() + ":sync");
                }
                if (a.asyncHttp()) {
                    ids.add("http-mode:" + a.artifactId() + ":async");
                }
            }
            return ids;
        }

        /** IDs of the entries naming a replacement that is not exactly an ID of {@code index}. */
        static SortedSet<String> unresolved(List<ManifestEntry> entries, Set<String> index) {
            return entries.stream()
                          .filter(e -> e.replacement() != null && !index.contains(e.replacement()))
                          .map(ManifestEntry::id)
                          .collect(Collectors.toCollection(TreeSet::new));
        }

        static List<ManifestEntry> nonDeprecatedAdapted(List<ManifestEntry> entries) {
            return entries.stream().filter(e -> !e.deprecated() && e.classification() == Classification.ADAPTED)
                          .toList();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Candidate inventories under test

    private static CandidateInventory built(Candidate c) {
        CandidateInventory.Builder b = CandidateInventory.builder();
        for (Artifact a : c.artifacts()) {
            String coordinates = a.coordinates();
            b.artifact(new CandidateInventory.CandidateArtifact(coordinates, CANDIDATE_VERSION, coordinates + ".jar",
                                                                null, false));
            a.apis().forEach(id -> b.capability(id, coordinates));
            a.declared().forEach(id -> b.capability(id, coordinates));
            if (a.service()) {
                b.capability(EntryKind.SERVICE.prefix() + a.artifactId(), coordinates);
            }
            if (a.syncHttp()) {
                b.capability(EntryKind.httpModeId(a.artifactId(), false), coordinates);
            }
            if (a.asyncHttp()) {
                b.capability(EntryKind.httpModeId(a.artifactId(), true), coordinates);
            }
        }
        c.declared().forEach(id -> b.capability(id, CandidateInventory.DECLARED));
        return b.build();
    }

    /** Writes each artifact as a candidate class/resource directory and scans them. */
    private static CandidateInventory scanned(Candidate c) throws IOException {
        Path root = Files.createTempDirectory("adapted-replacements-");
        try {
            List<Path> roots = new ArrayList<>();
            for (Artifact a : c.artifacts()) {
                Path dir = root.resolve(a.group() + "_" + a.artifactId());
                CandidateArtifacts.write(dir, "META-INF/aws-sdk-java-v3/modules/" + a.artifactId() + ".properties",
                                         "groupId=" + a.group() + "\nartifactId=" + a.artifactId() + "\nversion="
                                         + CANDIDATE_VERSION + "\n");
                StringBuilder capabilities = new StringBuilder("# generated for this try\n");
                Stream.concat(a.apis().stream(), a.declared().stream())
                      .forEach(id -> capabilities.append(id).append('\n'));
                CandidateArtifacts.write(dir, CandidateInventoryScanner.CAPABILITIES_RESOURCE, capabilities.toString());
                if (a.service()) {
                    CandidateArtifacts.write(dir, "META-INF/aws-sdk-java-v3/services/software.amazon.awssdk.services."
                                                  + a.artifactId().replace("-", "") + "/generation-manifest.json",
                                             "{}");
                }
                if (a.syncHttp()) {
                    CandidateArtifacts.write(dir, "META-INF/services/" + CandidateInventoryScanner.SYNC_HTTP_SERVICE,
                                             "example.SyncProvider\n");
                }
                if (a.asyncHttp()) {
                    CandidateArtifacts.write(dir, "META-INF/services/" + CandidateInventoryScanner.ASYNC_HTTP_SERVICE,
                                             "example.AsyncProvider\n");
                }
                roots.add(dir);
            }
            return CandidateInventoryScanner.scan(roots, List.copyOf(c.declared()));
        } finally {
            deleteRecursively(root);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators

    /** One generated candidate artifact. */
    record Artifact(String group, String artifactId, SortedSet<String> apis, SortedSet<String> declared,
                    boolean service, boolean syncHttp, boolean asyncHttp) {
        String coordinates() {
            return group + ":" + artifactId;
        }
    }

    /** A generated candidate: its artifacts and the capabilities declared outside any artifact. */
    record Candidate(List<Artifact> artifacts, SortedSet<String> declared) {
    }

    /** One try. */
    record Scenario(Candidate candidate, List<ManifestEntry> entries, boolean scan) {
        @Override
        public String toString() {
            return "Scenario[scan=" + scan + ", candidate=" + candidate + ", replacements="
                   + entries.stream().map(e -> e.id() + " (" + e.classification() + (e.deprecated() ? ", deprecated" : "")
                                               + ") -> " + e.replacement()).toList() + "]";
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Boolean> scan = Arbitraries.integers().between(0, 3).map(i -> i == 0);
        return Combinators.combine(candidates(), scan).flatAs((candidate, s) -> {
            List<String> shipped = Oracle.index(candidate).stream().sorted().toList();
            return Arbitraries.subsetOf(API_IDS).ofMinSize(1).ofMaxSize(8).flatMap(ids -> {
                List<Arbitrary<ManifestEntry>> entries = ids.stream().sorted().map(id -> entry(id, shipped)).toList();
                return Combinators.combine(entries).as(List::copyOf);
            }).map(entries -> new Scenario(candidate, entries, s));
        });
    }

    private static Arbitrary<Candidate> candidates() {
        List<String> coordinates = new ArrayList<>();
        GROUPS.forEach(g -> ARTIFACT_IDS.forEach(a -> coordinates.add(g + ":" + a)));
        Arbitrary<List<Artifact>> artifacts = Arbitraries.subsetOf(coordinates).ofMaxSize(5).flatMap(set -> {
            if (set.isEmpty()) {
                return Arbitraries.just(List.<Artifact>of());
            }
            return Combinators.combine(set.stream().sorted().map(AdaptedReplacementsExistPropertyTest::artifact)
                                          .toList()).as(List::copyOf);
        });
        Arbitrary<SortedSet<String>> declared = Arbitraries.subsetOf(UNIVERSE.stream()
                                                                             .filter(id -> id.startsWith("api:")
                                                                                           || DECLARABLE.contains(id))
                                                                             .toList())
                                                           .ofMaxSize(3).map(TreeSet::new);
        return Combinators.combine(artifacts, declared).as(Candidate::new);
    }

    private static Arbitrary<Artifact> artifact(String coordinates) {
        int colon = coordinates.indexOf(':');
        String group = coordinates.substring(0, colon);
        String artifactId = coordinates.substring(colon + 1);
        Arbitrary<Boolean> flag = Arbitraries.of(true, false);
        return Combinators.combine(Arbitraries.subsetOf(API_IDS).ofMaxSize(6).map(TreeSet::new),
                                   Arbitraries.subsetOf(DECLARABLE).ofMaxSize(2).map(TreeSet::new), flag, flag, flag)
                          .as((apis, declared, service, sync, async) -> new Artifact(group, artifactId, apis, declared,
                                                                                     service, sync, async));
    }

    private static Arbitrary<ManifestEntry> entry(String id, List<String> shipped) {
        Arbitrary<String> anyId = Arbitraries.of(UNIVERSE);
        Arbitrary<String> shippedId = shipped.isEmpty() ? anyId : Arbitraries.of(shipped);
        Arbitrary<String> nearMiss = shipped.isEmpty() ? anyId
                                                       : Arbitraries.of(shipped).flatMap(s -> Arbitraries.of(nearMisses(s)));
        // A same-name member with another descriptor satisfies a source-level PRESERVED check but not a replacement.
        List<String> overloaded = shipped.stream().filter(s -> !sameNameVariants(s).isEmpty()).toList();
        Arbitrary<String> sameName = overloaded.isEmpty() ? nearMiss
                                                          : Arbitraries.of(overloaded)
                                                                       .flatMap(s -> Arbitraries.of(sameNameVariants(s)));
        Arbitrary<String> replacement = Arbitraries.frequencyOf(Tuple.of(4, shippedId), Tuple.of(3, nearMiss),
                                                                Tuple.of(1, sameName), Tuple.of(2, anyId),
                                                                Tuple.of(1, Arbitraries.just(id)));
        Arbitrary<Classification> classification =
            Arbitraries.frequency(Tuple.of(4, Classification.ADAPTED), Tuple.of(1, Classification.EXCLUDED));
        Arbitrary<Boolean> deprecated = Arbitraries.integers().between(0, 3).map(i -> i == 0);
        Arbitrary<Boolean> excludedNamesReplacement = Arbitraries.of(true, false);
        return Combinators.combine(classification, deprecated, replacement, excludedNamesReplacement)
                          .as((c, d, r, named) -> {
                              Entries.B b = c == Classification.ADAPTED ? Entries.adapted(id) : Entries.excluded(id);
                              b.deprecated = d;
                              b.replacement = c == Classification.ADAPTED || named ? r : null;
                              return b.build();
                          });
    }

    /** Well-formed IDs close to {@code id} that differ from it. */
    static List<String> nearMisses(String id) {
        List<String> out = new ArrayList<>();
        int hash = id.indexOf('#');
        if (id.startsWith("api:") && hash > 0) {
            String type = id.substring(0, hash);
            out.add(type);
            MEMBERS.forEach(m -> out.add(type + "#" + m));
        } else if (id.startsWith("api:")) {
            MEMBERS.forEach(m -> out.add(id + "#" + m));
            out.add(id + "V3");
        } else if (id.startsWith("artifact:")) {
            out.add("artifact:" + FOREIGN_GROUP + ":" + id.substring(id.lastIndexOf(':') + 1));
        } else if (id.startsWith("http-mode:")) {
            out.add(id.endsWith(":sync") ? id.replace(":sync", ":async") : id.replace(":async", ":sync"));
        } else {
            out.add(id + "-v3");
        }
        out.remove(id);
        return out;
    }

    /** The other members of the same type, name and member kind as the member ID {@code id} (none for other IDs). */
    static List<String> sameNameVariants(String id) {
        int hash = id.indexOf('#');
        if (!id.startsWith("api:") || hash < 0) {
            return List.of();
        }
        String type = id.substring(0, hash);
        String member = id.substring(hash + 1);
        String key = memberName(member);
        return MEMBERS.stream().filter(m -> !m.equals(member) && memberName(m).equals(key)).map(m -> type + "#" + m)
                      .toList();
    }

    /** {@code name(} for a method or constructor, {@code name:} for a field. */
    private static String memberName(String member) {
        int i = 0;
        while (member.charAt(i) != '(' && member.charAt(i) != ':') {
            i++;
        }
        return member.substring(0, i + 1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Statistics

    private static void collectStatistics(Scenario scenario, Set<String> index) {
        Statistics.label("inventory").collect(scenario.scan() ? "scanned" : "built");
        Statistics.label("inventory").coverage(c -> c.check("scanned").percentage(p -> p >= 15));
        for (ManifestEntry e : scenario.entries()) {
            if (e.replacement() == null) {
                continue;
            }
            String outcome = index.contains(e.replacement()) ? "resolved"
                             : sameNamedMemberShipped(e.replacement(), index) ? "unresolved same-name member"
                             : "unresolved";
            Statistics.label("replacement").collect(outcome);
        }
        Statistics.label("replacement").coverage(c -> {
            c.check("resolved").percentage(p -> p >= 20);
            c.check("unresolved").percentage(p -> p >= 10);
            c.check("unresolved same-name member").percentage(p -> p >= 3);
        });
        boolean anyUnresolved = scenario.entries().stream()
                                        .anyMatch(e -> e.replacement() != null && !index.contains(e.replacement()));
        Statistics.label("packaging replacement check").collect(anyUnresolved ? "blocked" : "passes");
        Statistics.label("packaging replacement check").coverage(c -> {
            c.check("blocked").percentage(p -> p >= 10);
            c.check("passes").percentage(p -> p >= 10);
        });
    }

    /** True if a member of the same type, name and member kind as {@code memberId} ships with another descriptor. */
    private static boolean sameNamedMemberShipped(String memberId, Set<String> index) {
        return sameNameVariants(memberId).stream().anyMatch(index::contains);
    }
}
