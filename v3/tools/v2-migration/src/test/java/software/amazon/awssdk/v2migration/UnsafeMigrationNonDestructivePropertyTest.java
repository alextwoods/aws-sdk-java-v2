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

package software.amazon.awssdk.v2migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestBaseline;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/**
 * Property 60 (Requirements 17.9, 18.9): for any construct that cannot be safely transformed, the migration leaves the
 * construct unchanged and emits a source-located unresolved diagnostic with the affected construct, the
 * classification rationale, and the replacement and documentation identifier when available.
 *
 * <p>Each try writes one to three Java files, compiled against a V2-shaped stub API, and migrates them with every
 * registered recipe plus adversarial test recipes. The files are assembled from a catalog of {@link Fragment}s, each a
 * class member and the imports it needs. Unsafe fragments are constructs the real recipes must decline: on-demand
 * imports and comment-interrupted qualified names of renamed types, static imports of renamed members, qualified
 * names of moved Modern Retry types interrupted by comments, uses of excluded legacy signer and retry APIs (with and
 * without a manifest replacement), V2 APIs without a manifest entry, unattributable sources, and an interceptor hook
 * whose lambda reads {@code ExecutionAttributes}. Hostile fragments are calls that test recipes answer by throwing,
 * returning nothing, proposing edits outside the file, outside the item, in a non-editable file or overlapping
 * themselves, other items or unresolved items, or disagreeing with each other. Transformable fragments are neighbors
 * real or test recipes rewrite; they are interleaved with the unsafe ones, in random order, files and gaps.
 *
 * <p>The oracle is the construction itself, not the planner: every fragment carries its original and its expected
 * text, written out by hand. A transformable fragment keeps its original text only when a fragment of the same
 * renamed-type group in the same file makes the type's file-wide rename unsafe (a documented rule of the renamed-type
 * recipe: a unit is renamed consistently or not at all). The property checks that
 * <ul>
 *   <li>every file's migrated text equals the file assembled from the fragments' expected texts, so unsafe constructs
 *       are byte-for-byte unchanged while their neighbors transform, and files with nothing to transform are not
 *       changed;</li>
 *   <li>every fragment's constructs get the expected disposition, recipe and reason;</li>
 *   <li>independently of the fragments, the original text of every unresolved item reappears unchanged at its
 *       position in the output, shifted only by the applied edits before it, and no applied edit touches it;</li>
 *   <li>every unresolved item has a diagnostic at its source location (file, line, column) naming the construct, a
 *       non-blank reason, and exactly the classification, rationale, replacement, migration and differences of its
 *       manifest entry (looked up in the test's own entry table), with the documentation identifier of an adapted or
 *       excluded entry; an excluded facility without a replacement cites its exclusion rationale and documentation
 *       identifier; and the machine-readable report carries the same diagnostics as blocking.</li>
 * </ul>
 */
class UnsafeMigrationNonDestructivePropertyTest {
    private static final String V2 = "software.amazon.awssdk.";
    private static final String STRING = "Ljava/lang/String;";

    // V2 stub types.
    private static final String OLD_SHAPE = V2 + "services.demo.model.OldShape";
    private static final String NEW_SHAPE = V2 + "services.demo.model.NewShape";
    private static final String LEGACY_SHAPE = V2 + "services.other.model.LegacyShape";
    private static final String MODERN_SHAPE = V2 + "services.other.model.ModernShape";
    private static final String CLIENT = V2 + "services.demo.DemoClient";
    private static final String CLIENT_BUILDER = V2 + "services.demo.DemoClientBuilder";
    private static final String HOSTILE = V2 + "services.demo.HostileApi";
    private static final String UNLISTED = V2 + "services.demo.UnlistedApi";
    private static final String OPTION = V2 + "core.client.config.DemoOption";
    private static final String STATIC_OPTION = V2 + "core.client.config.StaticOption";
    private static final String RETRY_STRATEGY = V2 + "retries.api.RetryStrategy";
    private static final String V3_RETRY_STRATEGY = V2 + "retries.RetryStrategy";
    private static final String RETRY_POLICY = V2 + "core.retry.RetryPolicy";
    private static final String SIGNER = V2 + "core.signer.Signer";
    private static final String AWS4_SIGNER = V2 + "auth.signer.Aws4Signer";
    private static final String OVERRIDE = V2 + "core.RequestOverrideConfiguration";
    private static final String OVERRIDE_BUILDER = OVERRIDE + "$Builder";
    private static final String INTERCEPTOR = V2 + "core.interceptor.ExecutionInterceptor";
    private static final String CONTEXT = V2 + "core.interceptor.Context";
    private static final String ATTRIBUTES = V2 + "core.interceptor.ExecutionAttributes";
    private static final String SDK_REQUEST = V2 + "core.SdkRequest";
    private static final String HTTP_REQUEST = V2 + "http.SdkHttpRequest";
    private static final String V3_INTERCEPTOR = V2 + "pipeline.ExecutionInterceptor";
    private static final String HOOK_CONTEXT = V2 + "pipeline.HookContext";
    private static final String STRUCT = "software.amazon.smithy.java.core.schema.SerializableStruct";

    // Member and configuration IDs.
    private static final String CLIENT_BUILDER_FACTORY = method(CLIENT, "builder", "", d(CLIENT_BUILDER));
    private static final String LEGACY_ENDPOINT = method(CLIENT_BUILDER, "legacyEndpoint", STRING, d(CLIENT_BUILDER));
    private static final String ENDPOINT_URL = method(CLIENT_BUILDER, "endpointUrl", STRING, d(CLIENT_BUILDER));
    private static final String BUILDER_REGION = method(CLIENT_BUILDER, "region", STRING, d(CLIENT_BUILDER));
    private static final String BUILD = method(CLIENT_BUILDER, "build", "", d(CLIENT));
    private static final String OLD_TIMEOUT = EntryKind.fieldId(OPTION, "OLD_TIMEOUT", d(OPTION));
    private static final String TIMEOUT = EntryKind.fieldId(OPTION, "TIMEOUT", d(OPTION));
    private static final String OLD_FLAG = EntryKind.fieldId(STATIC_OPTION, "OLD_FLAG", d(STATIC_OPTION));
    private static final String NEW_FLAG = EntryKind.fieldId(STATIC_OPTION, "NEW_FLAG", d(STATIC_OPTION));
    private static final String OLD_REGION_ENV = EntryKind.configurationId(EntryKind.ENVIRONMENT_SOURCE,
                                                                           "DEMO_OLD_REGION");
    private static final String REGION_ENV = EntryKind.configurationId(EntryKind.ENVIRONMENT_SOURCE, "DEMO_REGION");
    private static final String RETRY_POLICY_NONE = method(RETRY_POLICY, "none", "", d(RETRY_POLICY));
    private static final String AWS4_SIGNER_CREATE = method(AWS4_SIGNER, "create", "", d(AWS4_SIGNER));
    private static final String OVERRIDE_SIGNER = method(OVERRIDE_BUILDER, "signer", d(SIGNER), d(OVERRIDE_BUILDER));
    private static final String UNLISTED_CALL = method(UNLISTED, "call", STRING, STRING);
    private static final String MODIFY_HTTP_REQUEST = method(INTERCEPTOR, "modifyHttpRequest",
                                                             d(CONTEXT + "$ModifyHttpRequest") + d(ATTRIBUTES),
                                                             d(HTTP_REQUEST));
    private static final String MODIFY_REQUEST = method(INTERCEPTOR, "modifyRequest",
                                                        d(CONTEXT + "$ModifyRequest") + d(ATTRIBUTES), d(SDK_REQUEST));

    private static final String RETRY_RATIONALE = "Legacy retry policies are replaced by the Modern Retry strategy API.";
    private static final String SIGNER_RATIONALE = "Pre-SRA signer APIs are excluded; signing is selected by auth "
                                                   + "scheme and identity provider.";
    private static final String RETRY_REPLACEMENT = EntryKind.typeId(V3_RETRY_STRATEGY);
    private static final String SIGNER_REPLACEMENT = "api:software.amazon.awssdk.auth.api.HttpSigner";

    /** The HostileApi methods taking and returning a string; each one is a different adversarial answer. */
    private static final List<String> HOSTILE_METHODS = List.of(
        "boom", "boomInner", "boomPierced", "boomEdge", "nothing", "far", "wide", "foreign", "tangled", "outer",
        "wrap", "pierce", "edge", "contested", "split", "vetoed", "rename", "tag");
    private static final String HOSTILE_INNER = method(HOSTILE, "inner", "", STRING);
    private static final String HOSTILE_ENGULF = method(HOSTILE, "engulf", "Ljava/lang/Object;", STRING);

    private static final String HOSTILE_A = "test.hostile-a";
    private static final String HOSTILE_B = "test.hostile-b";
    private static final String REWRITE_EXCLUDED = "test.rewrite-excluded";
    /** A project file the test recipes target although the engine may not edit it. */
    private static final String NOT_EDITABLE = "src/main/java/com/example/NotInventoried.java";
    /** Matches any recipe in an {@link Expect}; {@code null} requires that no recipe decided. */
    private static final String ANY_RECIPE = "<any>";

    private static final String APP_PATH = "src/main/java/com/example/App.java";
    private static final String OTHER_PATH = "src/main/java/com/example/Other.java";
    private static final String TENANT_PATH = "src/main/java/com/example/TenantHeader.java";

    private static final ManifestBaseline BASELINE = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                          "parity-manifest/property60", null, null);

    private static final Map<String, String> V2_SOURCES = v2Sources();

    /** Gaps written before a member: interleaving noise outside every fragment. */
    private static final List<String> GAPS = List.of("", "\n", "    // An unrelated neighbor follows.\n",
                                                     "    /* gap */\n\n");

    /** Lines of an interceptor hook that read the V2 {@code ExecutionAttributes} parameter inside a lambda. */
    private static final List<String> ATTRIBUTE_LAMBDAS = List.of(
        "        java.util.function.Supplier<Object> tenant = () -> attributes.getAttribute(\"tenant\");\n",
        "        Runnable read = () -> {\n"
        + "            Object tenant = attributes.getAttribute(\"tenant\");\n"
        + "        };\n",
        "        java.util.function.Function<String, Object> lookup = attributes::getAttribute;\n");

    private static final List<Arbitrary<Fragment>> CATALOG = catalog();
    /** The fragment names, one per catalog entry (variants of an entry share its name). */
    private static final List<String> CATALOG_NAMES = CATALOG.stream().map(a -> a.sample().name()).toList();

    private static Path stubDirectory;
    private static Path v2Classes;

    @BeforeContainer
    static void compileStubs() throws IOException {
        stubDirectory = Files.createTempDirectory("property60-stubs");
        v2Classes = compile(stubDirectory, V2_SOURCES);
    }

    @AfterContainer
    static void deleteStubs() throws IOException {
        delete(stubDirectory);
    }

    // Feature: aws-sdk-java-v3, Property 60: Unsafe migration is non-destructive
    // Embedded edge cases are disabled: they collapse the placement frequencies to their first alternative (no
    // fragment at all). The statistics coverage checks fail the property if the generated mix degrades.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 60: Unsafe migration is non-destructive")
    void unsafeMigrationIsNonDestructive(@ForAll("scenarios") Scenario scenario) throws IOException {
        check(scenario);
        collectStatistics(scenario);
    }

    /** Every fragment once, interleaved in one file, with the interceptor and the excluded-API rewriter. */
    @Example
    void everyFragmentInOneFile_unsafeConstructsUnchangedAndNeighborsTransformed() throws IOException {
        List<Placed> members = new ArrayList<>();
        for (Arbitrary<Fragment> fragment : CATALOG) {
            members.add(new Placed(fragment.sample(), GAPS.get(members.size() % GAPS.size())));
        }
        check(new Scenario(List.of(new SourceFile(APP_PATH, "App", members)), ATTRIBUTE_LAMBDAS.get(0), true, false,
                           true));
    }

    private static void check(Scenario scenario) throws IOException {
        Path root = Files.createTempDirectory("property60-");
        try {
            Map<String, String> originals = new LinkedHashMap<>();
            Map<String, String> expected = new LinkedHashMap<>();
            Map<String, List<Span>> spans = new LinkedHashMap<>();
            for (SourceFile file : scenario.files()) {
                Rendered original = render(file, false);
                originals.put(file.path(), original.text());
                expected.put(file.path(), render(file, true).text());
                spans.put(file.path(), original.spans());
            }
            if (scenario.interceptorLambda() != null) {
                String original = tenantHeader(scenario.interceptorLambda(), false);
                originals.put(TENANT_PATH, original);
                expected.put(TENANT_PATH, tenantHeader(scenario.interceptorLambda(), true));
                spans.put(TENANT_PATH, List.of(new Span(interceptorFragment(), List.of(new int[] {0, original.length()}))));
            }
            List<Path> sources = new ArrayList<>();
            for (Map.Entry<String, String> file : originals.entrySet()) {
                Path path = root.resolve(file.getKey());
                Files.createDirectories(path.getParent());
                Files.writeString(path, file.getValue(), StandardCharsets.UTF_8);
                sources.add(path);
            }
            List<ManifestEntry> entries = entries(scenario.retryReplacement(), scenario.signerReplacement());
            List<MigrationRecipe> recipes = new ArrayList<>(Recipes.discover(
                UnsafeMigrationNonDestructivePropertyTest.class.getClassLoader()));
            recipes.add(new HostileRecipe(HOSTILE_A, true));
            recipes.add(new HostileRecipe(HOSTILE_B, false));
            if (scenario.rewriteExcluded()) {
                recipes.add(new RewriteExcludedRecipe());
            }
            MigrationResult result = MigrationEngine.run(MigrationRequest.builder()
                                                                         .projectRoot(root)
                                                                         .project("com.example:property60")
                                                                         .sources(sources)
                                                                         .classpath(List.of(v2Classes))
                                                                         .manifest(MigrationManifest.of(BASELINE,
                                                                                                        entries))
                                                                         .recipes(recipes)
                                                                         .build());

            assertEquals(originals, result.originals(), "The engine read the files as written");
            for (Map.Entry<String, String> file : expected.entrySet()) {
                String path = file.getKey();
                String actual = result.changed().getOrDefault(path, originals.get(path));
                assertEquals(file.getValue(), actual, () -> "Migrated text of " + path + "; items:\n" + describe(result));
                if (file.getValue().equals(originals.get(path))) {
                    assertFalse(result.changed().containsKey(path), () -> path + " has nothing to transform");
                }
            }
            checkFragments(result, spans, scenario);
            checkUnresolvedUnchanged(result);
            checkDiagnostics(result, entries);
            checkReport(result);
        } finally {
            delete(root);
        }
    }

    /** Every fragment's constructs get the expected disposition, recipe and reason. */
    private static void checkFragments(MigrationResult result, Map<String, List<Span>> spans, Scenario scenario) {
        for (Map.Entry<String, List<Span>> file : spans.entrySet()) {
            Set<Group> poisoned = poisoned(file.getValue().stream().map(Span::fragment).toList());
            for (Span span : file.getValue()) {
                Fragment f = span.fragment();
                List<Expect> expects = f.isPoisonedBy(poisoned) ? f.poisonedExpects() : f.expects();
                for (Expect e : expects) {
                    List<ItemResult> matches = result.items().stream()
                                                     .filter(r -> r.item().location().path().equals(file.getKey())
                                                                  && r.item().kind() == e.kind()
                                                                  && Objects.equals(r.item().id(), e.id())
                                                                  && span.contains(r.item().location()))
                                                     .toList();
                    assertFalse(matches.isEmpty(), () -> "Fragment " + f.name() + " in " + file.getKey() + " has no "
                                                         + e.kind() + " " + e.id() + " item; items:\n"
                                                         + describe(result));
                    for (ItemResult r : matches) {
                        String context = "Fragment " + f.name() + " in " + file.getKey() + " (" + scenario.flags()
                                         + "): " + r;
                        assertEquals(e.disposition(), r.disposition(), context);
                        if (!ANY_RECIPE.equals(e.recipe())) {
                            assertEquals(e.recipe(), r.recipe(), context);
                        }
                        if (e.reasonPart() != null) {
                            assertTrue(r.reason() != null && r.reason().contains(e.reasonPart()),
                                       () -> context + "\nexpected the reason to contain: " + e.reasonPart());
                        }
                    }
                }
            }
        }
    }

    /**
     * Planner-independent: no applied edit touches an unresolved range, and the range's original text reappears in
     * the output at its offset shifted by the length changes of the applied edits that end at or before it.
     */
    private static void checkUnresolvedUnchanged(MigrationResult result) {
        Map<String, List<TextEdit>> applied = new TreeMap<>();
        for (ItemResult r : result.items()) {
            if (r.disposition() == Disposition.TRANSFORMED) {
                for (TextEdit e : r.edits()) {
                    applied.computeIfAbsent(e.path(), p -> new ArrayList<>()).add(e);
                }
            }
        }
        for (ItemResult r : result.items()) {
            SourceLocation loc = r.item().location();
            if (r.disposition() != Disposition.UNRESOLVED || loc.isPosition()) {
                continue;
            }
            String original = result.originals().get(loc.path());
            assertNotNull(original, () -> "Unresolved item outside the editable files: " + r);
            String output = result.changed().getOrDefault(loc.path(), original);
            int shift = 0;
            Set<String> insertions = new HashSet<>();
            for (TextEdit e : applied.getOrDefault(loc.path(), List.of())) {
                boolean touches = e.isInsertion()
                                  ? loc.startOffset() < e.startOffset() && e.startOffset() < loc.endOffset()
                                  : e.startOffset() < loc.endOffset() && loc.startOffset() < e.endOffset();
                assertFalse(touches, () -> "Applied edit " + e + " touches the unresolved construct " + r);
                boolean counted = !e.isInsertion() || insertions.add(e.startOffset() + ":" + e.replacement());
                if (e.endOffset() <= loc.startOffset() && counted) {
                    shift += e.replacement().length() - (e.endOffset() - e.startOffset());
                }
            }
            int start = loc.startOffset() + shift;
            int length = loc.endOffset() - loc.startOffset();
            boolean unchanged = start >= 0 && start + length <= output.length()
                                && output.regionMatches(start, original, loc.startOffset(), length);
            assertTrue(unchanged, () -> "The unresolved construct " + r + " is not byte-for-byte unchanged in the "
                                        + "output:\n" + output);
        }
    }

    /** Every unresolved item has a complete, source-located diagnostic matching its manifest entry. */
    private static void checkDiagnostics(MigrationResult result, List<ManifestEntry> entries) {
        Map<String, ManifestEntry> byId = entries.stream().collect(Collectors.toMap(ManifestEntry::id, e -> e));
        int unresolved = 0;
        for (ItemResult r : result.items()) {
            if (r.disposition() != Disposition.UNRESOLVED) {
                assertNull(r.diagnostic(), () -> "Only unresolved items carry a diagnostic: " + r);
                continue;
            }
            unresolved++;
            MigrationDiagnostic d = r.diagnostic();
            String context = "Diagnostic of " + r;
            assertNotNull(d, context);
            SourceLocation loc = d.location();
            assertEquals(r.item().location(), loc, context);
            String text = result.originals().get(loc.path());
            assertEquals(lineOf(text, loc.startOffset()), loc.line(), context);
            assertEquals(columnOf(text, loc.startOffset()), loc.column(), context);
            assertEquals(lineOf(text, loc.endOffset()), loc.endLine(), context);
            assertEquals(columnOf(text, loc.endOffset()), loc.endColumn(), context);
            assertFalse(d.construct().isBlank(), context);
            if (!loc.isPosition()) {
                String normalized = text.substring(loc.startOffset(), loc.endOffset()).strip().replaceAll("\\s+", " ");
                String shown = d.construct().endsWith("\u2026")
                               ? d.construct().substring(0, d.construct().length() - 1) : d.construct();
                assertTrue(normalized.startsWith(shown), () -> context + "\nconstruct is not the source text: "
                                                                + normalized);
            }
            assertFalse(d.reason().isBlank(), context);
            ManifestEntry entry = r.item().id() == null ? null : byId.get(r.item().id());
            if (entry == null) {
                assertEquals(r.item().id(), d.entryId(), context);
                assertNull(d.classification(), context);
                assertNull(d.rationale(), context);
                assertNull(d.replacement(), context);
                assertNull(d.migrationKind(), context);
                assertNull(d.migration(), context);
                assertEquals(List.of(), d.differences(), context);
                assertNull(d.documentationId(), context);
                continue;
            }
            assertEquals(entry.id(), d.entryId(), context);
            assertEquals(entry.classification(), d.classification(), context);
            assertEquals(entry.rationale(), d.rationale(), context);
            assertEquals(entry.replacement(), d.replacement(), context);
            assertEquals(entry.migrationKind(), d.migrationKind(), context);
            assertEquals(entry.migration(), d.migration(), context);
            assertEquals(entry.differences(), d.differences(), context);
            if (entry.classification() == Classification.PRESERVED) {
                assertNull(d.documentationId(), context);
                continue;
            }
            assertFalse(d.rationale().isBlank(), () -> context + ": the classification rationale is required");
            assertEquals(entry.id(), d.documentationId(), context);
            assertTrue(d.message().contains("Documentation: " + entry.id() + "."), () -> context + "\n" + d.message());
            if (entry.replacement() != null) {
                assertTrue(d.message().contains(entry.replacement()), () -> context + "\n" + d.message());
            } else if (entry.classification() == Classification.EXCLUDED) {
                // Requirement 18.9: no safe replacement, so the blocking diagnostic cites the exclusion rationale.
                assertTrue(d.message().contains(entry.rationale()), () -> context + "\n" + d.message());
            }
        }
        assertEquals(unresolved, result.diagnostics().size(), "One diagnostic per unresolved item");
    }

    /** The machine-readable report carries every diagnostic as blocking, at the same location. */
    private static void checkReport(MigrationResult result) {
        ObjectNode report = Node.parse(result.reportJson(false)).expectObjectNode();
        List<ObjectNode> reported = report.expectArrayMember("diagnostics").getElementsAs(ObjectNode.class);
        List<MigrationDiagnostic> diagnostics = result.diagnostics();
        assertEquals(diagnostics.size(), reported.size(), "Reported diagnostics");
        for (int i = 0; i < diagnostics.size(); i++) {
            MigrationDiagnostic d = diagnostics.get(i);
            ObjectNode r = reported.get(i);
            ObjectNode location = r.expectObjectMember("location");
            assertEquals("BLOCKING", r.expectStringMember("severity").getValue());
            assertEquals(d.location().path(), location.expectStringMember("path").getValue());
            assertEquals(d.location().line(), location.expectNumberMember("line").getValue().intValue());
            assertEquals(d.location().column(), location.expectNumberMember("column").getValue().intValue());
            assertEquals(d.construct(), r.expectStringMember("construct").getValue());
            assertEquals(d.reason(), r.expectStringMember("reason").getValue());
            Node documentation = r.expectMember("documentationId");
            assertEquals(d.documentationId(), documentation.isNullNode() ? null
                                                                         : documentation.expectStringNode().getValue());
        }
    }

    private static void collectStatistics(Scenario scenario) {
        for (SourceFile file : scenario.files()) {
            List<Fragment> fragments = file.members().stream().map(Placed::fragment).toList();
            fragments.forEach(f -> Statistics.label("fragment").collect(f.name()));
            Set<Group> poisoned = poisoned(fragments);
            Statistics.label("poisoned neighbor").collect(
                fragments.stream().anyMatch(f -> !f.poisons() && f.isPoisonedBy(poisoned)));
        }
        Statistics.label("interceptor").collect(scenario.interceptorLambda() != null);
        Statistics.label("member files").collect(scenario.files().size());
        Statistics.label("excluded rewriter").collect(scenario.rewriteExcluded());
        Statistics.label("excluded replacements").collect(scenario.retryReplacement() + "/" + scenario.signerReplacement());

        Statistics.label("fragment").coverage(c -> {
            for (String name : CATALOG_NAMES) {
                c.check(name).count(n -> n >= 5);
            }
        });
        Statistics.label("poisoned neighbor").coverage(c -> c.check(true).count(n -> n >= 5));
        Statistics.label("interceptor").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Statistics.label("member files").coverage(c -> {
            c.check(1).percentage(p -> p >= 15);
            c.check(2).percentage(p -> p >= 15);
        });
        Statistics.label("excluded rewriter").coverage(c -> c.check(true).percentage(p -> p >= 25));
        Statistics.label("excluded replacements").coverage(c -> {
            for (String combination : List.of("true/true", "true/false", "false/true", "false/false")) {
                c.check(combination).count(n -> n >= 5);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generation
    // ---------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary<Scenario> scenarios() {
        List<Arbitrary<List<Assigned>>> placements = new ArrayList<>();
        for (Arbitrary<Fragment> fragment : CATALOG) {
            placements.add(Combinators.combine(fragment,
                                               Arbitraries.frequency(Tuple.of(14, 0), Tuple.of(4, 1), Tuple.of(2, 2),
                                                                     Tuple.of(1, 3)),
                                               Arbitraries.integers().between(0, 999),
                                               Arbitraries.integers().between(0, 999),
                                               Arbitraries.of(GAPS),
                                               Arbitraries.of(GAPS))
                                      .as((f, where, appOrder, otherOrder, appGap, otherGap) -> {
                                          List<Assigned> out = new ArrayList<>();
                                          if ((where & 1) != 0) {
                                              out.add(new Assigned(0, appOrder, new Placed(f, appGap)));
                                          }
                                          if ((where & 2) != 0) {
                                              out.add(new Assigned(1, otherOrder, new Placed(f, otherGap)));
                                          }
                                          return out;
                                      }));
        }
        Arbitrary<List<Assigned>> assigned = Combinators.combine(placements)
                                                        .as(lists -> lists.stream().flatMap(List::stream).toList());
        Arbitrary<String> interceptor = Arbitraries.of(ATTRIBUTE_LAMBDAS).injectNull(0.67);
        Arbitrary<Boolean> flag = Arbitraries.of(true, false);
        Arbitrary<Boolean> singleFile = Arbitraries.frequency(Tuple.of(2, false), Tuple.of(1, true));
        return Combinators.combine(assigned, singleFile, interceptor, flag, flag, flag)
                          .as(UnsafeMigrationNonDestructivePropertyTest::scenario)
                          .filter(s -> s.interceptorLambda() != null
                                       || s.files().stream().flatMap(f -> f.members().stream())
                                           .anyMatch(p -> p.fragment().unsafe()));
    }

    private static Scenario scenario(List<Assigned> placements, boolean singleFile, String interceptor,
                                     boolean rewriteExcluded, boolean retryReplacement, boolean signerReplacement) {
        List<Assigned> assigned = placements;
        if (singleFile) {
            // Every fragment once, all in App.
            Map<String, Assigned> first = new LinkedHashMap<>();
            placements.forEach(a -> first.putIfAbsent(a.placed().fragment().name(),
                                                      new Assigned(0, a.order(), a.placed())));
            assigned = List.copyOf(first.values());
        }
        List<SourceFile> files = new ArrayList<>();
        String[][] names = {{APP_PATH, "App"}, {OTHER_PATH, "Other"}};
        for (int file = 0; file < names.length; file++) {
            int index = file;
            List<Placed> members = assigned.stream().filter(a -> a.file() == index)
                                           .sorted(Comparator.comparingInt(Assigned::order)
                                                             .thenComparing(a -> a.placed().fragment().name()))
                                           .map(Assigned::placed).toList();
            if (!members.isEmpty()) {
                files.add(new SourceFile(names[file][0], names[file][1], members));
            }
        }
        return new Scenario(files, interceptor, rewriteExcluded, retryReplacement, signerReplacement);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The fragment catalog: original and expected text written out by hand
    // ---------------------------------------------------------------------------------------------------------------

    private static List<Arbitrary<Fragment>> catalog() {
        List<Arbitrary<Fragment>> c = new ArrayList<>();
        ImportLine hostileImport = keep("import " + HOSTILE + ";");

        // Transformable neighbors.
        c.add(just(new Fragment(
            "renamed type, single-type import", Group.SHAPE, false,
            List.of(new ImportLine("import " + OLD_SHAPE + ";", "import " + NEW_SHAPE + ";")),
            "    OldShape keepShape(OldShape shape) {\n        return shape;\n    }\n",
            "    NewShape keepShape(NewShape shape) {\n        return shape;\n    }\n",
            List.of(transformed(ItemKind.IMPORT, EntryKind.typeId(OLD_SHAPE), "imports.renamed-types"),
                    transformed(ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE), "imports.renamed-types")),
            List.of(unresolved(ItemKind.IMPORT, EntryKind.typeId(OLD_SHAPE), "imports.renamed-types",
                               "cannot be rewritten safely"),
                    unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE), "imports.renamed-types",
                               "cannot be rewritten safely")))));
        c.add(just(new Fragment(
            "renamed type, fully qualified", Group.SHAPE, false, List.of(),
            "    " + OLD_SHAPE + " qualifiedShape() {\n        return null;\n    }\n",
            "    " + NEW_SHAPE + " qualifiedShape() {\n        return null;\n    }\n",
            List.of(transformed(ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE), "imports.renamed-types")),
            List.of(unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE), "imports.renamed-types",
                               "cannot be rewritten safely")))));
        c.add(just(new Fragment(
            "renamed type, fully qualified (other package)", Group.LEGACY, false, List.of(),
            "    " + LEGACY_SHAPE + " legacyShape() {\n        return null;\n    }\n",
            "    " + MODERN_SHAPE + " legacyShape() {\n        return null;\n    }\n",
            List.of(transformed(ItemKind.TYPE_REFERENCE, EntryKind.typeId(LEGACY_SHAPE), "imports.renamed-types")),
            List.of(unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(LEGACY_SHAPE), "imports.renamed-types",
                               "cannot be rewritten safely")))));
        String endpoint = "    DemoClient endpointClient() {\n"
                          + "        return DemoClient.builder()\n"
                          + "                         .legacyEndpoint(\"https://localhost\")\n"
                          + "                         .region(\"us-west-2\")\n"
                          + "                         .build();\n"
                          + "    }\n";
        c.add(just(neighbor("renamed builder member", List.of(keep("import " + CLIENT + ";")), endpoint,
                            endpoint.replace(".legacyEndpoint(", ".endpointUrl("),
                            transformed(ItemKind.METHOD_INVOCATION, LEGACY_ENDPOINT, "builders.renamed-members"))));
        c.add(just(neighbor("renamed field, qualified", List.of(keep("import " + OPTION + ";")),
                            "    Object timeout() {\n        return DemoOption.OLD_TIMEOUT;\n    }\n",
                            "    Object timeout() {\n        return DemoOption.TIMEOUT;\n    }\n",
                            transformed(ItemKind.FIELD_ACCESS, OLD_TIMEOUT, "configuration.renamed-members"))));
        c.add(just(neighbor("renamed configuration key", List.of(),
                            "    String regionSetting() {\n        return System.getenv(\"DEMO_OLD_REGION\");\n    }\n",
                            "    String regionSetting() {\n        return System.getenv(\"DEMO_REGION\");\n    }\n",
                            transformed(ItemKind.CONFIGURATION_KEY, OLD_REGION_ENV, "configuration.renamed-keys"))));
        c.add(just(new Fragment(
            "moved Modern Retry type, single-type import", null, false,
            List.of(new ImportLine("import " + RETRY_STRATEGY + ";", "import " + V3_RETRY_STRATEGY + ";")),
            "    RetryStrategy strategy() {\n        return null;\n    }\n",
            "    RetryStrategy strategy() {\n        return null;\n    }\n",
            List.of(transformed(ItemKind.IMPORT, EntryKind.typeId(RETRY_STRATEGY), "retry.modern-type-package"),
                    new Expect(ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_STRATEGY), Disposition.PRESERVED,
                               "retry.modern-type-package", null)),
            List.of())));
        c.add(just(hostile("test recipe rename", hostileImport, "hostileRename", "HostileApi.rename(\"x\")",
                           "HostileApi.renamed(\"x\")",
                           transformed(ItemKind.METHOD_INVOCATION, hostileId("rename"), HOSTILE_A))));
        c.add(just(hostile("test recipe insertion", hostileImport, "hostileTag", "HostileApi.tag(\"x\")",
                           "HostileApi./* tagged */ tag(\"x\")",
                           transformed(ItemKind.METHOD_INVOCATION, hostileId("tag"), HOSTILE_A))));
        c.add(just(hostile("insertion at an unresolved boundary", hostileImport, "hostileEdge",
                           "HostileApi.edge(HostileApi.boomEdge(\"x\"))",
                           "HostileApi.edge(HostileApi.boomEdge(\"x\") /* after */)",
                           transformed(ItemKind.METHOD_INVOCATION, hostileId("edge"), HOSTILE_A),
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("boomEdge"), HOSTILE_A,
                                      "the recipe failed (IllegalStateException"))));

        // Unsafe constructs the real recipes must decline.
        c.add(Arbitraries.of(OLD_SHAPE.replace(".OldShape", "./* V2 */ OldShape"),
                             OLD_SHAPE.replace(".model.", "/* model */.model."),
                             OLD_SHAPE.replace(".OldShape", " // the V2 name\n        .OldShape"))
                         .map(name -> unsafe("renamed type, qualified name interrupted by a comment", Group.SHAPE,
                                             List.of(), "    " + name + " commentedShape() {\n        return null;\n"
                                                        + "    }\n",
                                             unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE),
                                                        "imports.renamed-types", "cannot be rewritten safely"))));
        c.add(just(unsafe("renamed type, on-demand import", Group.LEGACY,
                          List.of(keep("import " + V2 + "services.other.model.*;")),
                          "    LegacyShape onDemandShape() {\n        return null;\n    }\n",
                          unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(LEGACY_SHAPE),
                                     "imports.renamed-types", "without a single-type import"))));
        c.add(just(unsafe("static import of a renamed member", null,
                          List.of(keep("import static " + STATIC_OPTION + ".OLD_FLAG;")),
                          "    Object flag() {\n        return OLD_FLAG;\n    }\n",
                          unresolved(ItemKind.STATIC_IMPORT, OLD_FLAG, "configuration.renamed-members",
                                     "static import"),
                          unresolved(ItemKind.FIELD_ACCESS, OLD_FLAG, "configuration.renamed-members",
                                     "static import"))));
        c.add(Arbitraries.of(RETRY_STRATEGY.replace(".RetryStrategy", "./* moved */RetryStrategy"),
                             RETRY_STRATEGY.replace(".api.", "/* api */.api."))
                         .map(name -> unsafe("moved Modern Retry type, qualified name interrupted by a comment", null,
                                             List.of(), "    " + name + " commentedStrategy() {\n"
                                                        + "        return null;\n    }\n",
                                             unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_STRATEGY),
                                                        "retry.modern-type-package", "cannot be rewritten to"))));
        c.add(just(unsafe("excluded legacy retry policy", null, List.of(keep("import " + RETRY_POLICY + ";")),
                          "    Object retryPolicy() {\n        return RetryPolicy.none();\n    }\n",
                          unresolved(ItemKind.IMPORT, EntryKind.typeId(RETRY_POLICY), "excluded.legacy-signer-retry",
                                     "excluded legacy retry API"),
                          unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_POLICY),
                                     "excluded.legacy-signer-retry", "excluded legacy retry API"),
                          unresolved(ItemKind.METHOD_INVOCATION, RETRY_POLICY_NONE, "excluded.legacy-signer-retry",
                                     "excluded legacy retry API"))));
        c.add(just(unsafe("excluded legacy signer", null,
                          List.of(keep("import " + AWS4_SIGNER + ";"), keep("import " + OVERRIDE + ";")),
                          "    Object sign(RequestOverrideConfiguration.Builder override) {\n"
                          + "        return override.signer(Aws4Signer.create());\n    }\n",
                          unresolved(ItemKind.IMPORT, EntryKind.typeId(AWS4_SIGNER), "excluded.legacy-signer-retry",
                                     "excluded legacy signer API"),
                          unresolved(ItemKind.METHOD_INVOCATION, AWS4_SIGNER_CREATE, "excluded.legacy-signer-retry",
                                     "excluded legacy signer API"),
                          // The request override's signer is also an unsupported configuration option, whose
                          // recipe (first in ID order) answers unsafe too; both reasons are reported.
                          unresolved(ItemKind.METHOD_INVOCATION, OVERRIDE_SIGNER, ANY_RECIPE,
                                     "Recipe excluded.legacy-signer-retry judged the transformation unsafe: "))));
        c.add(just(unsafe("V2 API without a manifest entry", null, List.of(),
                          "    Object unlisted() {\n        return " + UNLISTED + ".call(\"x\");\n    }\n",
                          unresolved(ItemKind.TYPE_REFERENCE, EntryKind.typeId(UNLISTED), null,
                                     "No Parity Manifest entry matches this construct"),
                          unresolved(ItemKind.METHOD_INVOCATION, UNLISTED_CALL, null,
                                     "No Parity Manifest entry matches this construct"))));
        c.add(just(unsafe("unattributable type", null, List.of(),
                          "    Object ghost() {\n        return new " + V2 + "services.ghost.Ghost();\n    }\n",
                          unresolved(ItemKind.UNATTRIBUTED_SOURCE, null, null, "could not attribute"))));
        c.add(just(unsafe("unattributable member", null, List.of(),
                          "    Object vanished(" + CLIENT + " client) {\n        return client.vanished(\"x\");\n"
                          + "    }\n",
                          unresolved(ItemKind.UNATTRIBUTED_SOURCE, null, null, "could not attribute"))));

        // Adversarial test recipe answers.
        c.add(just(hostile("test recipe throws", hostileImport, "hostileBoom", "HostileApi.boom(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("boom"), HOSTILE_A,
                                      "the recipe failed (IllegalStateException"))));
        c.add(just(hostile("test recipe returns null", hostileImport, "hostileNothing", "HostileApi.nothing(\"x\")",
                           null, unresolved(ItemKind.METHOD_INVOCATION, hostileId("nothing"), HOSTILE_A,
                                            "the recipe returned no answer"))));
        c.add(just(hostile("edit beyond the end of the file", hostileImport, "hostileFar", "HostileApi.far(\"x\")",
                           null, unresolved(ItemKind.METHOD_INVOCATION, hostileId("far"), HOSTILE_A, "lies outside"))));
        c.add(just(hostile("edit outside the item", hostileImport, "hostileWide", "HostileApi.wide(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("wide"), HOSTILE_A,
                                      "replaces text outside the item's own range"))));
        c.add(just(hostile("edit of a non-editable file", hostileImport, "hostileForeign",
                           "HostileApi.foreign(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("foreign"), HOSTILE_A,
                                      "which the tool may not edit"))));
        c.add(just(hostile("edits overlapping each other", hostileImport, "hostileTangled",
                           "HostileApi.tangled(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("tangled"), HOSTILE_A,
                                      "its own edits overlap each other"))));
        c.add(just(hostile("edits of nested items overlapping", hostileImport, "hostileNested",
                           "HostileApi.outer(HostileApi.inner())", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("outer"), HOSTILE_A,
                                      "overlap those of the construct"),
                           unresolved(ItemKind.METHOD_INVOCATION, HOSTILE_INNER, HOSTILE_A,
                                      "overlap those of the construct"))));
        c.add(just(hostile("edit replacing an unresolved item", hostileImport, "hostileWrap",
                           "HostileApi.wrap(HostileApi.boomInner(\"x\"))", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("wrap"), HOSTILE_A,
                                      "would change the unresolved construct"),
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("boomInner"), HOSTILE_A,
                                      "the recipe failed (IllegalStateException"))));
        c.add(just(hostile("insertion inside an unresolved item", hostileImport, "hostilePierce",
                           "HostileApi.pierce(HostileApi.boomPierced(\"x\"))", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("pierce"), HOSTILE_A,
                                      "would change the unresolved construct"),
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("boomPierced"), HOSTILE_A,
                                      "the recipe failed (IllegalStateException"))));
        c.add(just(hostile("two test recipes transform", hostileImport, "hostileContested",
                           "HostileApi.contested(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("contested"), ANY_RECIPE,
                                      "Recipes disagree"))));
        c.add(just(hostile("test recipes transform and preserve", hostileImport, "hostileSplit",
                           "HostileApi.split(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("split"), ANY_RECIPE,
                                      "Recipes disagree"))));
        c.add(just(hostile("test recipes transform and veto", hostileImport, "hostileVetoed",
                           "HostileApi.vetoed(\"x\")", null,
                           unresolved(ItemKind.METHOD_INVOCATION, hostileId("vetoed"), HOSTILE_B,
                                      "judged the transformation unsafe"))));
        c.add(just(hostile("test recipe edit overlapping a real transformation", hostileImport, "hostileEngulf",
                           "HostileApi.engulf(" + OPTION + ".OLD_TIMEOUT)", null,
                           unresolved(ItemKind.METHOD_INVOCATION, HOSTILE_ENGULF, HOSTILE_A,
                                      "overlap those of the construct"),
                           unresolved(ItemKind.FIELD_ACCESS, OLD_TIMEOUT, "configuration.renamed-members",
                                      "overlap those of the construct"))));
        return List.copyOf(c);
    }

    /** The interceptor file; its expectations span the whole file. */
    private static Fragment interceptorFragment() {
        return new Fragment("interceptor hook reading ExecutionAttributes in a lambda", null, false, List.of(), "",
                            "",
                            List.of(unresolved(ItemKind.METHOD_OVERRIDE, MODIFY_REQUEST, "interceptors.hooks",
                                               "reads the V2 parameter attributes"),
                                    transformed(ItemKind.METHOD_OVERRIDE, MODIFY_HTTP_REQUEST, "interceptors.hooks")),
                            List.of());
    }

    /**
     * An interceptor whose {@code modifyHttpRequest} hook converts and whose {@code modifyRequest} hook reads the V2
     * attributes parameter in a lambda ({@code lambda}), so it must stay unchanged with its V2 signature.
     */
    private static String tenantHeader(String lambda, boolean migrated) {
        String unsafeHook = "    @Override\n"
                            + "    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes "
                            + "attributes) {\n"
                            + lambda
                            + "        return context.request();\n"
                            + "    }\n";
        if (!migrated) {
            return "package com.example;\n"
                   + "\n"
                   + "import software.amazon.awssdk.core.SdkRequest;\n"
                   + "import software.amazon.awssdk.core.interceptor.Context;\n"
                   + "import software.amazon.awssdk.core.interceptor.ExecutionAttributes;\n"
                   + "import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;\n"
                   + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                   + "\n"
                   + "public class TenantHeader implements ExecutionInterceptor {\n"
                   + "    private static final String TENANT = \"t-1\";\n"
                   + "\n"
                   + "    @Override\n"
                   + "    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, "
                   + "ExecutionAttributes attributes) {\n"
                   + "        return context.httpRequest().toBuilder().putHeader(\"x-tenant\", TENANT).build();\n"
                   + "    }\n"
                   + "\n"
                   + unsafeHook
                   + "}\n";
        }
        // The converted hook and the interceptor and context imports change (each import is its own item, and the
        // converted hook needs HookContext); the unsafe hook, and the imports of the ADAPTED-by-diagnostic types it
        // still names, stay byte-for-byte as written and are reported.
        return "package com.example;\n"
               + "\n"
               + "import software.amazon.awssdk.core.SdkRequest;\n"
               + "import software.amazon.awssdk.pipeline.HookContext;\n"
               + "import software.amazon.awssdk.core.interceptor.ExecutionAttributes;\n"
               + "import software.amazon.awssdk.pipeline.ExecutionInterceptor;\n"
               + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
               + "\n"
               + "public class TenantHeader implements ExecutionInterceptor {\n"
               + "    private static final String TENANT = \"t-1\";\n"
               + "\n"
               + "    @Override\n"
               + "    public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {\n"
               + "        return context.httpRequest().toBuilder().putHeader(\"x-tenant\", TENANT).build();\n"
               + "    }\n"
               + "\n"
               + unsafeHook
               + "}\n";
    }

    private static Arbitrary<Fragment> just(Fragment fragment) {
        return Arbitraries.just(fragment);
    }

    private static ImportLine keep(String line) {
        return new ImportLine(line, line);
    }

    private static Fragment neighbor(String name, List<ImportLine> imports, String member, String transformedMember,
                                     Expect... expects) {
        return new Fragment(name, null, false, imports, member, transformedMember, List.of(expects), List.of());
    }

    private static Fragment unsafe(String name, Group poisons, List<ImportLine> imports, String member,
                                   Expect... expects) {
        return new Fragment(name, poisons, poisons != null, imports, member, member, List.of(expects),
                            List.of(expects));
    }

    /** A member {@code String memberName() { return call; }} that the test recipes answer for. */
    private static Fragment hostile(String name, ImportLine hostileImport, String memberName, String call,
                                    String transformedCall, Expect... expects) {
        String member = "    String " + memberName + "() {\n        return " + call + ";\n    }\n";
        String transformed = transformedCall == null ? member : member.replace(call, transformedCall);
        return new Fragment(name, null, false, List.of(hostileImport), member, transformed, List.of(expects),
                            List.of());
    }

    private static Expect transformed(ItemKind kind, String id, String recipe) {
        return new Expect(kind, id, Disposition.TRANSFORMED, recipe, null);
    }

    private static Expect unresolved(ItemKind kind, String id, String recipe, String reasonPart) {
        return new Expect(kind, id, Disposition.UNRESOLVED, recipe, reasonPart);
    }

    private static String hostileId(String name) {
        return method(HOSTILE, name, STRING, STRING);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------------------------------

    /** The groups whose file-wide rename some fragment of the file makes unsafe. */
    private static Set<Group> poisoned(List<Fragment> fragments) {
        Set<Group> groups = EnumSet.noneOf(Group.class);
        fragments.stream().filter(Fragment::poisons).forEach(f -> groups.add(f.group()));
        return groups;
    }

    /** {@code file} with every fragment's original or expected text, and each fragment's ranges in it. */
    private static Rendered render(SourceFile file, boolean expected) {
        List<Fragment> fragments = file.members().stream().map(Placed::fragment).toList();
        Set<Group> poisoned = poisoned(fragments);
        Map<String, String> imports = new TreeMap<>();
        for (Fragment f : fragments) {
            for (ImportLine i : f.imports()) {
                String line = expected && !f.isPoisonedBy(poisoned) ? i.transformed() : i.original();
                String previous = imports.put(i.original(), line);
                if (previous != null && !previous.equals(line)) {
                    throw new IllegalStateException("Fragments disagree on the import " + i.original());
                }
            }
        }
        StringBuilder out = new StringBuilder("package com.example;\n\n");
        Map<String, int[]> importRanges = new TreeMap<>();
        for (Map.Entry<String, String> i : imports.entrySet()) {
            int start = out.length();
            out.append(i.getValue());
            importRanges.put(i.getKey(), new int[] {start, out.length()});
            out.append('\n');
        }
        if (!imports.isEmpty()) {
            out.append('\n');
        }
        out.append("public class ").append(file.className()).append(" {\n");
        List<Span> spans = new ArrayList<>();
        for (Placed p : file.members()) {
            Fragment f = p.fragment();
            out.append(p.gap());
            int start = out.length();
            out.append(expected && !f.isPoisonedBy(poisoned) ? f.transformedMember() : f.member());
            List<int[]> ranges = new ArrayList<>();
            ranges.add(new int[] {start, out.length()});
            f.imports().forEach(i -> ranges.add(importRanges.get(i.original())));
            spans.add(new Span(f, ranges));
        }
        out.append("}\n");
        return new Rendered(out.toString(), spans);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Test recipes
    // ---------------------------------------------------------------------------------------------------------------

    /** The HostileApi method an item invokes, or {@code null}. */
    private static String hostileMethod(MigrationItem item) {
        String prefix = "api:" + HOSTILE + "#";
        if (item.kind() != ItemKind.METHOD_INVOCATION || item.id() == null || !item.id().startsWith(prefix)) {
            return null;
        }
        String rest = item.id().substring(prefix.length());
        return rest.substring(0, rest.indexOf('('));
    }

    /**
     * Adversarial answers for HostileApi calls: the primary recipe throws, returns null, or proposes invalid,
     * overlapping or valid edits; the secondary one disagrees with it.
     */
    private static final class HostileRecipe implements MigrationRecipe {
        private final String id;
        private final boolean primary;

        HostileRecipe(String id, boolean primary) {
            this.id = id;
            this.primary = primary;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String description() {
            return "Answers adversarially for HostileApi calls (Property 60).";
        }

        @Override
        public RecipeOutcome visit(RecipeContext context) {
            String method = hostileMethod(context.item());
            if (method == null) {
                return RecipeOutcome.notApplicable();
            }
            SourceLocation own = context.item().location();
            String path = own.path();
            String text = context.text(path);
            int start = own.startOffset();
            if (!primary) {
                return switch (method) {
                    case "contested" -> rewrite(new TextEdit(path, start, start + method.length(), method + "B"));
                    case "split" -> RecipeOutcome.preserve("The secondary test recipe keeps " + method + ".");
                    case "vetoed" -> RecipeOutcome.unsafe("The secondary test recipe vetoes " + method + ".");
                    default -> RecipeOutcome.notApplicable();
                };
            }
            if (method.startsWith("boom")) {
                throw new IllegalStateException("boom at " + own);
            }
            return switch (method) {
                case "nothing" -> null;
                case "far" -> rewrite(new TextEdit(path, text.length() + 1, text.length() + 2, "x"));
                case "wide" -> rewrite(new TextEdit(path, start - 1, own.endOffset(),
                                                    text.substring(start - 1, own.endOffset())));
                case "foreign" -> rewrite(new TextEdit(NOT_EDITABLE, 0, 1, "x"));
                case "tangled" -> rewrite(new TextEdit(path, start, start + 3, "abc"),
                                          new TextEdit(path, start + 1, start + 4, "def"));
                case "outer", "inner", "wrap", "engulf" -> rewrite(TextEdit.replace(own, "rewritten()"));
                case "pierce" -> rewrite(TextEdit.insert(path, text.indexOf("boomPierced", start) + 2, "/* x */"));
                case "edge" -> {
                    String inner = "boomEdge(\"x\")";
                    yield rewrite(TextEdit.insert(path, text.indexOf(inner, start) + inner.length(), " /* after */"));
                }
                case "contested", "split", "vetoed" -> rewrite(new TextEdit(path, start, start + method.length(),
                                                                            method + "A"));
                case "rename" -> rewrite(new TextEdit(path, start, start + method.length(), "renamed"));
                case "tag" -> rewrite(TextEdit.insert(path, start, "/* tagged */ "));
                default -> RecipeOutcome.notApplicable();
            };
        }

        private static RecipeOutcome rewrite(TextEdit... edits) {
            return RecipeOutcome.transform("Test rewrite.", List.of(edits));
        }
    }

    /** Proposes to rewrite every use of an excluded API, which the excluded-facility recipe must overrule. */
    private static final class RewriteExcludedRecipe implements MigrationRecipe {
        @Override
        public String id() {
            return REWRITE_EXCLUDED;
        }

        @Override
        public String description() {
            return "Rewrites every use of an excluded API (Property 60).";
        }

        @Override
        public RecipeOutcome visit(RecipeContext context) {
            boolean excluded = context.entry().map(e -> e.classification() == Classification.EXCLUDED).orElse(false);
            if (!excluded || context.item().location().isPosition()) {
                return RecipeOutcome.notApplicable();
            }
            return RecipeOutcome.transform("Rewrite the excluded API.",
                                           List.of(TextEdit.replace(context.item().location(), "/* rewritten */")));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Manifest and stub API
    // ---------------------------------------------------------------------------------------------------------------

    private static List<ManifestEntry> entries(boolean retryReplacement, boolean signerReplacement) {
        String retry = retryReplacement ? RETRY_REPLACEMENT : null;
        String signer = signerReplacement ? SIGNER_REPLACEMENT : null;
        String retryMigration = "Replace the legacy retry policy with a Modern Retry strategy on the client builder.";
        String signerMigration = "Remove the legacy Signer usage; configure the auth scheme.";
        List<ManifestEntry> entries = new ArrayList<>(List.of(
            recipe(EntryKind.typeId(OLD_SHAPE), EntryKind.typeId(NEW_SHAPE)),
            recipe(EntryKind.typeId(LEGACY_SHAPE), EntryKind.typeId(MODERN_SHAPE)),
            preserved(EntryKind.typeId(CLIENT)), preserved(EntryKind.typeId(CLIENT_BUILDER)),
            preserved(CLIENT_BUILDER_FACTORY), preserved(BUILDER_REGION), preserved(BUILD),
            recipe(LEGACY_ENDPOINT, ENDPOINT_URL),
            preserved(EntryKind.typeId(OPTION)), recipe(OLD_TIMEOUT, TIMEOUT),
            preserved(EntryKind.typeId(STATIC_OPTION)), recipe(OLD_FLAG, NEW_FLAG),
            recipe(OLD_REGION_ENV, REGION_ENV),
            recipe(EntryKind.typeId(RETRY_STRATEGY), EntryKind.typeId(V3_RETRY_STRATEGY)),
            excluded(EntryKind.typeId(RETRY_POLICY), RETRY_RATIONALE, retry, retryMigration),
            excluded(RETRY_POLICY_NONE, RETRY_RATIONALE, retry, retryMigration),
            excluded(EntryKind.typeId(AWS4_SIGNER), SIGNER_RATIONALE, signer, signerMigration),
            excluded(AWS4_SIGNER_CREATE, SIGNER_RATIONALE, signer, signerMigration),
            excluded(EntryKind.typeId(SIGNER), SIGNER_RATIONALE, signer, signerMigration),
            excluded(OVERRIDE_SIGNER, SIGNER_RATIONALE, signer, signerMigration),
            preserved(EntryKind.typeId(OVERRIDE)), preserved(EntryKind.typeId(OVERRIDE_BUILDER)),
            preserved(EntryKind.typeId(HOSTILE)), preserved(HOSTILE_INNER), preserved(HOSTILE_ENGULF),
            // The interceptor surface as a real manifest classifies it (see InterceptorRecipeTest).
            recipe(EntryKind.typeId(INTERCEPTOR), EntryKind.typeId(V3_INTERCEPTOR)),
            recipe(EntryKind.typeId(CONTEXT), EntryKind.typeId(HOOK_CONTEXT)),
            recipe(EntryKind.typeId(CONTEXT + "$ModifyHttpRequest"),
                   EntryKind.typeId(HOOK_CONTEXT + "$ModifyBeforeSigning")),
            adapted(EntryKind.typeId(ATTRIBUTES), EntryKind.typeId(V2 + "core.context.ContextKey"),
                    "Store per-call state under typed context keys."),
            adapted(EntryKind.typeId(SDK_REQUEST), EntryKind.typeId(STRUCT), "Modeled values are SerializableStruct."),
            recipe(MODIFY_HTTP_REQUEST, method(V3_INTERCEPTOR, "modifyBeforeSigning",
                                               d(HOOK_CONTEXT + "$ModifyBeforeSigning"), d(HTTP_REQUEST))),
            recipe(MODIFY_REQUEST, method(V3_INTERCEPTOR, "modifyRequest", d(HOOK_CONTEXT + "$ModifyRequest"),
                                          d(STRUCT))),
            recipe(method(CONTEXT + "$AfterMarshalling", "httpRequest", "", d(HTTP_REQUEST)),
                   method(HOOK_CONTEXT + "$ModifyBeforeSigning", "httpRequest", "", d(HTTP_REQUEST))),
            recipe(method(CONTEXT + "$BeforeExecution", "request", "", d(SDK_REQUEST)),
                   method(HOOK_CONTEXT, "request", "", d(STRUCT))),
            preserved(EntryKind.typeId(HTTP_REQUEST)), preserved(EntryKind.typeId(HTTP_REQUEST + "$Builder")),
            preserved(method(HTTP_REQUEST, "toBuilder", "", d(HTTP_REQUEST + "$Builder"))),
            preserved(method(HTTP_REQUEST + "$Builder", "putHeader", STRING + STRING, d(HTTP_REQUEST + "$Builder"))),
            preserved(method(HTTP_REQUEST + "$Builder", "build", "", d(HTTP_REQUEST))),
            preserved(method(ATTRIBUTES, "getAttribute", STRING, "Ljava/lang/Object;"))));
        HOSTILE_METHODS.forEach(m -> entries.add(preserved(hostileId(m))));
        return entries;
    }

    private static ManifestEntry preserved(String id) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.PRESERVED,
                                 List.of("source"), "Kept unchanged in V3.", List.of(), null, null, null, null, null,
                                 List.of());
    }

    private static ManifestEntry recipe(String id, String replacement) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "Rationale of " + id + ".", List.of("The V3 contract of " + id + " differs."),
                                 "software.amazon.awssdk:sdk-core", replacement, MigrationKind.RECIPE,
                                 "Use " + replacement + ".", null, List.of("test:evidence"));
    }

    private static ManifestEntry adapted(String id, String replacement, String migration) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "V3 changes the contract of " + id + ".",
                                 List.of("The V3 contract differs."), "software.amazon.awssdk:sdk-core", replacement,
                                 MigrationKind.DIAGNOSTIC, migration, null, List.of("test:evidence"));
    }

    private static ManifestEntry excluded(String id, String rationale, String replacement, String migration) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, true, Classification.EXCLUDED,
                                 List.of(), rationale, List.of(), null, replacement, MigrationKind.DIAGNOSTIC, migration,
                                 "Deprecated in V2.", List.of());
    }

    private static EntryKind kind(String id) {
        return EntryKind.ofId(id).orElseThrow(() -> new IllegalArgumentException(id));
    }

    private static String d(String binaryName) {
        return "L" + binaryName.replace('.', '/') + ";";
    }

    private static String method(String owner, String name, String parameters, String result) {
        return EntryKind.methodId(owner, name, "(" + parameters + ")" + result);
    }

    private static Map<String, String> v2Sources() {
        Map<String, String> s = new LinkedHashMap<>();
        s.put(OLD_SHAPE, "package software.amazon.awssdk.services.demo.model;\npublic class OldShape {}\n");
        s.put(LEGACY_SHAPE, "package software.amazon.awssdk.services.other.model;\npublic class LegacyShape {}\n");
        s.put(CLIENT_BUILDER, "package software.amazon.awssdk.services.demo;\n"
                              + "public interface DemoClientBuilder {\n"
                              + "    DemoClientBuilder legacyEndpoint(String endpoint);\n"
                              + "    DemoClientBuilder region(String region);\n"
                              + "    DemoClient build();\n"
                              + "}\n");
        s.put(CLIENT, "package software.amazon.awssdk.services.demo;\n"
                      + "public interface DemoClient {\n"
                      + "    static DemoClientBuilder builder() { return null; }\n"
                      + "}\n");
        StringBuilder hostile = new StringBuilder("package software.amazon.awssdk.services.demo;\n"
                                                  + "public final class HostileApi {\n"
                                                  + "    private HostileApi() {}\n"
                                                  + "    public static String inner() { return \"\"; }\n"
                                                  + "    public static String engulf(Object o) { return \"\"; }\n");
        HOSTILE_METHODS.forEach(m -> hostile.append("    public static String ").append(m)
                                            .append("(String s) { return s; }\n"));
        s.put(HOSTILE, hostile.append("}\n").toString());
        s.put(UNLISTED, "package software.amazon.awssdk.services.demo;\n"
                        + "public final class UnlistedApi {\n"
                        + "    public static String call(String s) { return s; }\n"
                        + "}\n");
        s.put(OPTION, "package software.amazon.awssdk.core.client.config;\n"
                      + "public final class DemoOption {\n"
                      + "    public static final DemoOption OLD_TIMEOUT = new DemoOption();\n"
                      + "}\n");
        s.put(STATIC_OPTION, "package software.amazon.awssdk.core.client.config;\n"
                             + "public final class StaticOption {\n"
                             + "    public static final StaticOption OLD_FLAG = new StaticOption();\n"
                             + "}\n");
        s.put(RETRY_STRATEGY, "package software.amazon.awssdk.retries.api;\npublic interface RetryStrategy {}\n");
        s.put(RETRY_POLICY, "package software.amazon.awssdk.core.retry;\n"
                            + "@Deprecated\n"
                            + "public final class RetryPolicy {\n"
                            + "    private RetryPolicy() {}\n"
                            + "    public static RetryPolicy none() { return null; }\n"
                            + "}\n");
        s.put(SIGNER, "package software.amazon.awssdk.core.signer;\n@Deprecated\npublic interface Signer {}\n");
        s.put(AWS4_SIGNER, "package software.amazon.awssdk.auth.signer;\n"
                           + "import software.amazon.awssdk.core.signer.Signer;\n"
                           + "@Deprecated\n"
                           + "public final class Aws4Signer implements Signer {\n"
                           + "    public static Aws4Signer create() { return new Aws4Signer(); }\n"
                           + "}\n");
        s.put(OVERRIDE, "package software.amazon.awssdk.core;\n"
                        + "import software.amazon.awssdk.core.signer.Signer;\n"
                        + "public abstract class RequestOverrideConfiguration {\n"
                        + "    public interface Builder {\n"
                        + "        @Deprecated Builder signer(Signer signer);\n"
                        + "    }\n"
                        + "}\n");
        s.put(SDK_REQUEST, "package software.amazon.awssdk.core;\npublic abstract class SdkRequest {}\n");
        s.put(HTTP_REQUEST, "package software.amazon.awssdk.http;\n"
                            + "public interface SdkHttpRequest {\n"
                            + "    Builder toBuilder();\n"
                            + "    interface Builder {\n"
                            + "        Builder putHeader(String name, String value);\n"
                            + "        SdkHttpRequest build();\n"
                            + "    }\n"
                            + "}\n");
        s.put(CONTEXT, "package software.amazon.awssdk.core.interceptor;\n"
                       + "import software.amazon.awssdk.core.SdkRequest;\n"
                       + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                       + "public final class Context {\n"
                       + "    private Context() {}\n"
                       + "    public interface BeforeExecution { SdkRequest request(); }\n"
                       + "    public interface ModifyRequest extends BeforeExecution {}\n"
                       + "    public interface AfterMarshalling extends ModifyRequest { SdkHttpRequest httpRequest(); }\n"
                       + "    public interface ModifyHttpRequest extends AfterMarshalling {}\n"
                       + "}\n");
        s.put(ATTRIBUTES, "package software.amazon.awssdk.core.interceptor;\n"
                          + "public final class ExecutionAttributes {\n"
                          + "    public Object getAttribute(String key) { return null; }\n"
                          + "}\n");
        s.put(INTERCEPTOR, "package software.amazon.awssdk.core.interceptor;\n"
                           + "import software.amazon.awssdk.core.SdkRequest;\n"
                           + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                           + "public interface ExecutionInterceptor {\n"
                           + "    default SdkRequest modifyRequest(Context.ModifyRequest c, ExecutionAttributes a) {\n"
                           + "        return c.request();\n"
                           + "    }\n"
                           + "    default SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest c,\n"
                           + "                                             ExecutionAttributes a) {\n"
                           + "        return c.httpRequest();\n"
                           + "    }\n"
                           + "}\n");
        return s;
    }

    private static Path compile(Path directory, Map<String, String> sources) throws IOException {
        Path src = directory.resolve("src");
        Path classes = Files.createDirectories(directory.resolve("classes"));
        List<String> args = new ArrayList<>(List.of("-d", classes.toString(), "-proc:none", "--release", "21",
                                                    "-Xlint:-deprecation"));
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = src.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            args.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        if (compiler.run(null, errors, errors, args.toArray(new String[0])) != 0) {
            throw new IllegalStateException("Stub compilation failed:\n" + errors.toString(StandardCharsets.UTF_8));
        }
        return classes;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    /** The 1-based line of {@code offset}; the generated sources use only {@code \n} line ends. */
    private static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** The 1-based column of {@code offset}. */
    private static int columnOf(String text, int offset) {
        return offset - (text.lastIndexOf('\n', offset - 1) + 1) + 1;
    }

    private static String describe(MigrationResult result) {
        return result.items().stream()
                     .map(r -> "  " + r.item().location() + " " + r.item().kind() + " " + r.item().id() + " ["
                               + r.item().construct() + "] " + r.disposition() + " by " + r.recipe() + ": "
                               + r.reason())
                     .collect(Collectors.joining("\n"));
    }

    private static void delete(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Model
    // ---------------------------------------------------------------------------------------------------------------

    /** A renamed type whose rename the renamed-type recipe plans file-wide. */
    enum Group {
        SHAPE,
        LEGACY
    }

    /** An import a fragment needs, as written and as expected after migration. */
    record ImportLine(String original, String transformed) {
    }

    /** An expected disposition of the fragment's items of {@code kind} and {@code id}. */
    record Expect(ItemKind kind, String id, Disposition disposition, String recipe, String reasonPart) {
    }

    /**
     * A class member and its imports. {@code group} names the renamed type it uses; {@code poisons} marks a use that
     * makes the file-wide rename of that type unsafe. {@code poisonedExpects} apply instead of {@code expects} when
     * a poisoning fragment of the group shares the file.
     */
    record Fragment(String name, Group group, boolean poisons, List<ImportLine> imports, String member,
                    String transformedMember, List<Expect> expects, List<Expect> poisonedExpects) {
        boolean isPoisonedBy(Set<Group> poisoned) {
            return group != null && poisoned.contains(group);
        }

        boolean unsafe() {
            return poisons || expects.stream().anyMatch(e -> e.disposition() == Disposition.UNRESOLVED);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** A fragment with the gap written before it. */
    record Placed(Fragment fragment, String gap) {
    }

    /** A placement of a fragment in file 0 (App) or 1 (Other), ordered by {@code order}. */
    record Assigned(int file, int order, Placed placed) {
    }

    record SourceFile(String path, String className, List<Placed> members) {
    }

    /** A fragment's ranges in the original file: its member and its import lines. */
    record Span(Fragment fragment, List<int[]> ranges) {
        boolean contains(SourceLocation location) {
            return ranges.stream().anyMatch(r -> r[0] <= location.startOffset() && location.endOffset() <= r[1]);
        }
    }

    record Rendered(String text, List<Span> spans) {
    }

    record Scenario(List<SourceFile> files, String interceptorLambda, boolean rewriteExcluded,
                    boolean retryReplacement, boolean signerReplacement) {
        String flags() {
            return "rewriteExcluded=" + rewriteExcluded + ", retryReplacement=" + retryReplacement
                   + ", signerReplacement=" + signerReplacement;
        }

        @Override
        public String toString() {
            return files.stream().map(f -> f.path() + ": " + f.members().stream().map(p -> p.fragment().name())
                                                               .toList())
                        .collect(Collectors.joining("; "))
                   + (interceptorLambda == null ? "" : "; interceptor lambda " + interceptorLambda.strip())
                   + "; " + flags();
        }
    }
}
