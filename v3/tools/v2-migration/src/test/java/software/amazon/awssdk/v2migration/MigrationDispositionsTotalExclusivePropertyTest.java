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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
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
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/**
 * Property 59 (Requirements 17.4, 17.10, 17.11): for any encountered migration item, the tooling assigns exactly one
 * disposition (preserved, transformed or unresolved), and every represented dependency, BOM, build, import, builder,
 * client, configuration, interceptor, HTTP, retry, signer, streaming and high-level-library item receives one.
 *
 * <p>Each try generates a V2 project: a random selection of {@link Construct}s, one per required category of
 * Requirement 17.11 (Java constructs attributed against a small compiled V2-shaped stub API, and POM declarations,
 * BOM imports, version properties, Java language settings and effective transitive dependencies), and a Parity
 * Manifest in which every construct ID independently is {@code PRESERVED}, {@code ADAPTED} (with a recipe or a
 * manual diagnostic), {@code EXCLUDED} or has no entry at all. Version properties, build settings and an unmatched
 * static import have no ID, so every try with them also holds items without manifest entries.
 *
 * <p>The expected inventory is written out here per construct (kind, ID and category of each item a construct's
 * source text introduces), not derived from {@link MigrationCategory#of} or the engine's inventory. The expected
 * disposition of each item is predicted by {@link #expect}, a restatement of the documented contract of
 * {@link MigrationRecipe} over the generated recipe answers and the generated manifest lookups; it shares no code with
 * the engine or {@code DispositionPlanner}. Two properties use it:
 * <ul>
 *   <li>{@link #dispositionsAreTotalAndExclusive}: three generated recipes answer each item with a generated
 *       not-applicable, preserve, transform (a unique insertion at the end of the item's file, so transformations
 *       never conflict) or unsafe answer. The reported partition must equal the prediction exactly, item by item and
 *       in the report's per-category and per-classification summaries.</li>
 *   <li>{@link #shippedRecipesKeepDispositionsTotalAndExclusive}: the recipes registered with
 *       {@link java.util.ServiceLoader} answer instead, recorded as they answer. The prediction is exact except that a proposed transformation may
 *       end unresolved when its edits conflict with others (the non-destructive rule of Property 60).</li>
 * </ul>
 * Both check that the reported items are exactly the expected inventory (totality, no unexpected or duplicate items)
 * and that each item's manifest entry, diagnostic and edits agree with its single disposition and manifest lookup.
 */
class MigrationDispositionsTotalExclusivePropertyTest {

    private static final String JAVA_PATH = "src/main/java/com/example/App.java";
    private static final String POM_PATH = "pom.xml";
    private static final String GROUP = "software.amazon.awssdk";
    private static final String V2_VERSION = "2.25.0";
    private static final String VERSION_PROPERTY = "aws.sdk.version";
    private static final ManifestBaseline BASELINE = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                          "parity-manifest/property59", null, null);

    // Manifest IDs of the stub API, written out by hand (binary names and JVM descriptors).
    private static final String SDK_BYTES = "api:software.amazon.awssdk.core.SdkBytes";
    private static final String SDK_NUMBER = "api:software.amazon.awssdk.core.SdkNumber";
    private static final String STRING_UTILS = "api:software.amazon.awssdk.utils.StringUtils";
    private static final String CLIENT_BUILDER = "api:software.amazon.awssdk.awscore.client.builder.AwsClientBuilder";
    private static final String CLIENT_BUILDER_BUILD = CLIENT_BUILDER + "#build()Ljava/lang/Object;";
    private static final String DEMO_CLIENT = "api:software.amazon.awssdk.services.demo.DemoClient";
    private static final String DEMO_CLIENT_SERVICE_NAME = DEMO_CLIENT + "#serviceName()Ljava/lang/String;";
    private static final String REGION = "api:software.amazon.awssdk.regions.Region";
    private static final String REGION_OF = REGION + "#of(Ljava/lang/String;)Lsoftware/amazon/awssdk/regions/Region;";
    private static final String ENV_AWS_REGION = "config:env:AWS_REGION";
    private static final String SYSPROP_AWS_REGION = "config:sysprop:aws.region";
    private static final String INTERCEPTOR = "api:software.amazon.awssdk.core.interceptor.ExecutionInterceptor";
    private static final String ATTRIBUTES = "api:software.amazon.awssdk.core.interceptor.ExecutionAttributes";
    private static final String BEFORE_EXECUTION =
        INTERCEPTOR + "#beforeExecution(Lsoftware/amazon/awssdk/core/interceptor/ExecutionAttributes;)V";
    private static final String HTTP_CLIENT = "api:software.amazon.awssdk.http.SdkHttpClient";
    private static final String HTTP_CLIENT_NAME = HTTP_CLIENT + "#clientName()Ljava/lang/String;";
    private static final String RETRY_POLICY = "api:software.amazon.awssdk.core.retry.RetryPolicy";
    private static final String DEFAULT_RETRY_POLICY =
        RETRY_POLICY + "#defaultRetryPolicy()Lsoftware/amazon/awssdk/core/retry/RetryPolicy;";
    private static final String SIGNER = "api:software.amazon.awssdk.auth.signer.Aws4Signer";
    private static final String SIGNER_CREATE = SIGNER + "#create()Lsoftware/amazon/awssdk/auth/signer/Aws4Signer;";
    private static final String REQUEST_BODY = "api:software.amazon.awssdk.core.sync.RequestBody";
    private static final String REQUEST_BODY_FROM_STRING =
        REQUEST_BODY + "#fromString(Ljava/lang/String;)Lsoftware/amazon/awssdk/core/sync/RequestBody;";
    private static final String TABLE = "api:software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable";
    private static final String TABLE_NAME = TABLE + "#tableName()Ljava/lang/String;";

    private static final List<String> IMPORT_TYPES = List.of(SDK_BYTES, SDK_NUMBER, STRING_UTILS);
    private static final List<String> DIRECT_ARTIFACTS = List.of("s3", "dynamodb", "sqs");
    private static final List<String> MANAGED_ARTIFACTS = List.of("kms", "sts");
    private static final List<String> TRANSITIVE_ARTIFACTS = List.of("sdk-core", "regions", "http-client-spi");
    private static final List<String> BUILD_SETTINGS = List.of("maven.compiler.source", "maven.compiler.target",
                                                               "plugin:release");

    /** The stub V2 API the generated Java sources are attributed against. */
    private static final Map<String, String> STUBS = Map.ofEntries(
        stub(SDK_BYTES, "public final class SdkBytes {}"),
        stub(SDK_NUMBER, "public final class SdkNumber {}"),
        stub(STRING_UTILS, "public final class StringUtils {}"),
        stub(CLIENT_BUILDER, "public interface AwsClientBuilder<B extends AwsClientBuilder<B, C>, C> {\n"
                             + "    C build();\n}"),
        stub(DEMO_CLIENT, "public interface DemoClient {\n    String serviceName();\n}"),
        stub(REGION, "public final class Region {\n    private Region() {}\n"
                     + "    public static Region of(String id) { return new Region(); }\n}"),
        stub(INTERCEPTOR, "public interface ExecutionInterceptor {\n"
                          + "    default void beforeExecution(ExecutionAttributes executionAttributes) {}\n}"),
        stub(ATTRIBUTES, "public final class ExecutionAttributes {}"),
        stub(HTTP_CLIENT, "public interface SdkHttpClient {\n    String clientName();\n}"),
        stub(RETRY_POLICY, "public final class RetryPolicy {\n"
                           + "    public static RetryPolicy defaultRetryPolicy() { return null; }\n}"),
        stub(SIGNER, "public final class Aws4Signer {\n    public static Aws4Signer create() { return null; }\n}"),
        stub(REQUEST_BODY, "public final class RequestBody {\n"
                           + "    public static RequestBody fromString(String contents) { return null; }\n}"),
        stub(TABLE, "public interface DynamoDbTable<T> {\n    String tableName();\n}"));

    /** The generated recipes of {@link #dispositionsAreTotalAndExclusive}, in ID order. */
    private static final List<String> RECIPE_IDS = List.of("p59.recipe-a", "p59.recipe-b", "p59.recipe-c");

    /** The migration concerns of Requirements 17.6 and 17.11; one generated construct per category. */
    enum Construct {
        IMPORT(MigrationCategory.IMPORT),
        BUILDER(MigrationCategory.BUILDER),
        CLIENT(MigrationCategory.CLIENT),
        CONFIGURATION(MigrationCategory.CONFIGURATION),
        INTERCEPTOR(MigrationCategory.INTERCEPTOR),
        HTTP(MigrationCategory.HTTP),
        RETRY(MigrationCategory.RETRY),
        SIGNER(MigrationCategory.SIGNER),
        STREAMING(MigrationCategory.STREAMING),
        HIGH_LEVEL_LIBRARY(MigrationCategory.HIGH_LEVEL_LIBRARY),
        DEPENDENCY(MigrationCategory.DEPENDENCY),
        BOM(MigrationCategory.BOM),
        BUILD(MigrationCategory.BUILD);

        final MigrationCategory category;

        Construct(MigrationCategory category) {
            this.category = category;
        }

        boolean java() {
            return compareTo(DEPENDENCY) < 0;
        }
    }

    /** The generated manifest lookup of one ID. */
    enum Lookup {
        PRESERVED, ADAPTED_RECIPE, ADAPTED_DIAGNOSTIC, EXCLUDED, ABSENT
    }

    /** A generated recipe answer. */
    enum Answer {
        NOT_APPLICABLE, PRESERVE, TRANSFORM, UNSAFE
    }

    /** One expected inventory item: its kind, manifest ID (or {@code null}) and migration category. */
    record Expected(ItemKind kind, String id, MigrationCategory category) {
        String key() {
            return key(kind, id);
        }

        static String key(ItemKind kind, String id) {
            return kind + " " + id;
        }

        String describe() {
            return kind + " " + id + " " + category;
        }
    }

    /**
     * The generated POM.
     *
     * @param direct           V2 artifacts of {@code <dependencies>}
     * @param directByProperty those of {@code direct} whose version is {@code ${aws.sdk.version}}
     * @param managed          V2 artifacts of {@code <dependencyManagement>} (not BOM imports)
     * @param transitive       V2 artifacts of the effective graph that the POM does not declare
     * @param bomByProperty    whether the BOM import's version is {@code ${aws.sdk.version}}
     * @param buildSettings    the Java language level settings written
     */
    record PomSpec(List<String> direct, List<String> directByProperty, List<String> managed, List<String> transitive,
                   boolean bomByProperty, List<String> buildSettings) {
    }

    /** One generated try. */
    record Scenario(Set<Construct> constructs, Map<Construct, Integer> repeats, List<String> imports,
                    boolean staticImport, PomSpec pom, Map<String, Lookup> lookups,
                    Map<String, List<Answer>> answers) {

        Lookup lookup(String id) {
            return id == null ? Lookup.ABSENT : lookups.getOrDefault(id, Lookup.ABSENT);
        }

        List<Answer> answers(ItemKind kind, String id) {
            return answers.getOrDefault(Expected.key(kind, id), List.of());
        }

        boolean has(Construct c) {
            return constructs.contains(c);
        }

        boolean hasJava() {
            return constructs.stream().anyMatch(Construct::java);
        }

        boolean hasPom() {
            return constructs.stream().anyMatch(c -> !c.java());
        }

        @Override
        public String toString() {
            return "Scenario[constructs=" + constructs + ", repeats=" + repeats + ", imports=" + imports
                   + ", staticImport=" + staticImport + ", pom=" + pom + ", lookups=" + new TreeMap<>(lookups)
                   + ", answers=" + answers.entrySet().stream()
                                           .filter(e -> e.getValue().stream().anyMatch(a -> a != Answer.NOT_APPLICABLE))
                                           .collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                                                    Map::putAll) + "]";
        }
    }

    /** Every manifest ID a scenario can encounter; each try looks every one of them up (entries for unused IDs too). */
    private static final List<String> ALL_IDS = Stream.of(
        IMPORT_TYPES.stream(),
        Stream.of(CLIENT_BUILDER, CLIENT_BUILDER_BUILD, DEMO_CLIENT, DEMO_CLIENT_SERVICE_NAME, REGION, REGION_OF,
                  ENV_AWS_REGION, SYSPROP_AWS_REGION, INTERCEPTOR, ATTRIBUTES, BEFORE_EXECUTION, HTTP_CLIENT,
                  HTTP_CLIENT_NAME, RETRY_POLICY, DEFAULT_RETRY_POLICY, SIGNER, SIGNER_CREATE, REQUEST_BODY,
                  REQUEST_BODY_FROM_STRING, TABLE, TABLE_NAME),
        Stream.of(DIRECT_ARTIFACTS, MANAGED_ARTIFACTS, TRANSITIVE_ARTIFACTS, List.of("bom"))
              .flatMap(List::stream).map(MigrationDispositionsTotalExclusivePropertyTest::artifact))
        .flatMap(s -> s).sorted().toList();

    /** Every (kind, ID) answer key a scenario can encounter: those of the largest project, with and without entries. */
    private static final List<String> ALL_KEYS = Stream.of(Lookup.PRESERVED, Lookup.ABSENT)
        .flatMap(lookup -> expectedInventory(maximal(lookup)).stream())
        .map(Expected::key).distinct().sorted().toList();

    private static Path stubRoot;
    private static Path stubClasses;

    @BeforeContainer
    static void compileStubs() throws IOException {
        stubRoot = Files.createTempDirectory("property59-stubs-");
        Path sources = stubRoot.resolve("src");
        stubClasses = stubRoot.resolve("classes");
        Files.createDirectories(stubClasses);
        List<String> args = new ArrayList<>(List.of("-d", stubClasses.toString(), "-proc:none", "--release", "21"));
        for (Map.Entry<String, String> s : STUBS.entrySet()) {
            Path file = sources.resolve(s.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, s.getValue(), StandardCharsets.UTF_8);
            args.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler.run(null, null, null, args.toArray(new String[0])) != 0) {
            throw new IllegalStateException("Stub compilation failed; see the compiler output");
        }
    }

    @AfterContainer
    static void deleteStubs() throws IOException {
        if (stubRoot != null) {
            deleteRecursively(stubRoot);
        }
    }

    // Feature: aws-sdk-java-v3, Property 59: Migration dispositions are total and exclusive
    // Embedded edge cases are disabled: they collapse the frequency choices to their first alternative (no answers,
    // every entry PRESERVED), which starves the unresolved and transformed partitions. The statistics coverage checks
    // fail the property if the generated mix degrades.
    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 59: Migration dispositions are total and exclusive")
    void dispositionsAreTotalAndExclusive(@ForAll("scenarios") Scenario scenario) throws IOException {
        List<MigrationRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < RECIPE_IDS.size(); i++) {
            recipes.add(generatedRecipe(scenario, i));
        }
        Path root = Files.createTempDirectory("property59-");
        try {
            MigrationResult result = MigrationEngine.run(request(root, scenario, recipes));
            checkInventory(scenario, result);
            List<Disposition> predicted = new ArrayList<>();
            for (ItemResult r : result.items()) {
                MigrationItem item = r.item();
                List<Vote> votes = new ArrayList<>();
                List<Answer> answers = scenario.answers(item.kind(), item.id());
                for (int i = 0; i < answers.size(); i++) {
                    votes.add(new Vote(RECIPE_IDS.get(i), answers.get(i), null));
                }
                Prediction p = expect(votes, scenario.lookup(item.id()));
                assertEquals(p.disposition(), r.disposition(), () -> "Disposition of " + r);
                assertEquals(p.recipe(), r.recipe(), () -> "Deciding recipe of " + r);
                if (r.disposition() == Disposition.TRANSFORMED) {
                    assertEquals(List.of(TextEdit.insert(item.location().path(),
                                                         result.originals().get(item.location().path()).length(),
                                                         marker(p.recipe(), item))),
                                 r.edits(), () -> "Edits of " + r);
                }
                predicted.add(p.disposition());
            }
            checkConsistency(scenario, result);
            checkChangedText(result);
            checkReport(scenario, result, predicted);
            collectStatistics(scenario, result, true);
        } finally {
            deleteRecursively(root);
        }
    }

    // Feature: aws-sdk-java-v3, Property 59: Migration dispositions are total and exclusive
    // The same generated projects migrated by the recipes the Maven plugin discovers.
    @Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 59: Migration dispositions are total and exclusive with the shipped recipes")
    void shippedRecipesKeepDispositionsTotalAndExclusive(@ForAll("scenarios") Scenario scenario) throws IOException {
        Map<MigrationItem, List<Vote>> recorded = new LinkedHashMap<>();
        List<MigrationRecipe> recipes = Recipes.discover(getClass().getClassLoader()).stream()
                                               .map(r -> recording(r, recorded)).toList();
        assertTrue(recipes.size() > 0, "No recipes are registered");
        Path root = Files.createTempDirectory("property59-shipped-");
        try {
            MigrationResult result = MigrationEngine.run(request(root, scenario, recipes));
            checkInventory(scenario, result);
            assertEquals(new HashSet<>(recorded.keySet()),
                         new HashSet<>(result.items().stream().map(ItemResult::item).toList()),
                         "Every reported item, and only those, was offered to the recipes");
            List<Disposition> reported = new ArrayList<>();
            for (ItemResult r : result.items()) {
                List<Vote> votes = new ArrayList<>(recorded.get(r.item()));
                votes.sort(Comparator.comparing(Vote::recipe));
                assertEquals(recipes.size(), votes.size(), () -> "Every recipe answered once for " + r.item());
                Prediction p = expect(votes, scenario.lookup(r.item().id()));
                if (p.disposition() == Disposition.TRANSFORMED) {
                    // A valid proposal is applied as proposed; one that conflicts is left unresolved (Property 60).
                    assertTrue(r.disposition() == Disposition.TRANSFORMED || r.disposition() == Disposition.UNRESOLVED,
                               () -> "Disposition of " + r);
                    if (r.disposition() == Disposition.TRANSFORMED) {
                        RecipeOutcome.Transform proposal = (RecipeOutcome.Transform) votes.stream()
                            .filter(v -> v.answer() == Answer.TRANSFORM).findFirst().orElseThrow().outcome();
                        assertEquals(proposal.edits(), r.edits(), () -> "Edits of " + r);
                    }
                } else {
                    assertEquals(p.disposition(), r.disposition(), () -> "Disposition of " + r + " given " + votes);
                }
                assertEquals(p.recipe(), r.recipe(), () -> "Deciding recipe of " + r + " given " + votes);
                reported.add(r.disposition());
            }
            checkConsistency(scenario, result);
            checkReport(scenario, result, reported);
            collectStatistics(scenario, result, false);
        } finally {
            deleteRecursively(root);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle

    /** One recipe's answer; {@code outcome} is the recorded outcome of a shipped recipe, else {@code null}. */
    record Vote(String recipe, Answer answer, RecipeOutcome outcome) {
    }

    /** The predicted disposition and deciding recipe. */
    record Prediction(Disposition disposition, String recipe) {
    }

    /**
     * The disposition the {@link MigrationRecipe} contract assigns to an item, given every recipe's answer in recipe-ID
     * order and the item's manifest lookup: any unsafe answer, or disagreeing answers (two transformations, or a
     * transformation and a preservation), leave it unresolved; a single transformation transforms it; preservations
     * preserve it; without an answer, a {@code PRESERVED} entry preserves it and anything else (an {@code ADAPTED} or
     * {@code EXCLUDED} entry, or no entry) leaves it unresolved.
     */
    static Prediction expect(List<Vote> votes, Lookup lookup) {
        List<String> unsafe = recipes(votes, Answer.UNSAFE);
        List<String> transforms = recipes(votes, Answer.TRANSFORM);
        List<String> preserves = recipes(votes, Answer.PRESERVE);
        if (!unsafe.isEmpty()) {
            return new Prediction(Disposition.UNRESOLVED, unsafe.get(0));
        }
        if (transforms.size() > 1 || (transforms.size() == 1 && !preserves.isEmpty())) {
            return new Prediction(Disposition.UNRESOLVED, null);
        }
        if (transforms.size() == 1) {
            return new Prediction(Disposition.TRANSFORMED, transforms.get(0));
        }
        if (!preserves.isEmpty()) {
            return new Prediction(Disposition.PRESERVED, preserves.get(0));
        }
        return new Prediction(lookup == Lookup.PRESERVED ? Disposition.PRESERVED : Disposition.UNRESOLVED, null);
    }

    private static List<String> recipes(List<Vote> votes, Answer answer) {
        return votes.stream().filter(v -> v.answer() == answer).map(Vote::recipe).toList();
    }

    /** The classification a lookup stands for, or {@code null} without an entry. */
    private static Classification classification(Lookup lookup) {
        return switch (lookup) {
            case PRESERVED -> Classification.PRESERVED;
            case ADAPTED_RECIPE, ADAPTED_DIAGNOSTIC -> Classification.ADAPTED;
            case EXCLUDED -> Classification.EXCLUDED;
            case ABSENT -> null;
        };
    }

    /**
     * The items the scenario's source text introduces, construct by construct. A configuration key is a V2 item only
     * when the manifest names it (the inventory's definition of a V2 configuration key), and a static import joins by
     * the manifest member it names, so those two depend on the lookups.
     */
    static List<Expected> expectedInventory(Scenario s) {
        List<Expected> items = new ArrayList<>();
        if (s.has(Construct.IMPORT)) {
            for (String type : s.imports()) {
                items.add(new Expected(ItemKind.IMPORT, type, MigrationCategory.IMPORT));
            }
            if (s.staticImport()) {
                String id = s.lookup(DEFAULT_RETRY_POLICY) == Lookup.ABSENT ? null : DEFAULT_RETRY_POLICY;
                items.add(new Expected(ItemKind.STATIC_IMPORT, id, MigrationCategory.IMPORT));
            }
        }
        if (s.has(Construct.INTERCEPTOR)) {
            items.add(new Expected(ItemKind.TYPE_REFERENCE, INTERCEPTOR, MigrationCategory.INTERCEPTOR));
            items.add(new Expected(ItemKind.METHOD_OVERRIDE, BEFORE_EXECUTION, MigrationCategory.INTERCEPTOR));
            items.add(new Expected(ItemKind.TYPE_REFERENCE, ATTRIBUTES, MigrationCategory.INTERCEPTOR));
        }
        for (Construct c : Construct.values()) {
            if (!s.has(c) || c == Construct.IMPORT || c == Construct.INTERCEPTOR || !c.java()) {
                continue;
            }
            for (int r = 0; r < s.repeats().getOrDefault(c, 1); r++) {
                switch (c) {
                    case BUILDER -> pair(items, CLIENT_BUILDER, CLIENT_BUILDER_BUILD, c.category);
                    case CLIENT -> pair(items, DEMO_CLIENT, DEMO_CLIENT_SERVICE_NAME, c.category);
                    case CONFIGURATION -> {
                        pair(items, REGION, REGION_OF, c.category);
                        for (String key : List.of(ENV_AWS_REGION, SYSPROP_AWS_REGION)) {
                            if (s.lookup(key) != Lookup.ABSENT) {
                                items.add(new Expected(ItemKind.CONFIGURATION_KEY, key, c.category));
                            }
                        }
                    }
                    case HTTP -> pair(items, HTTP_CLIENT, HTTP_CLIENT_NAME, c.category);
                    case RETRY -> pair(items, RETRY_POLICY, DEFAULT_RETRY_POLICY, c.category);
                    case SIGNER -> pair(items, SIGNER, SIGNER_CREATE, c.category);
                    case STREAMING -> pair(items, REQUEST_BODY, REQUEST_BODY_FROM_STRING, c.category);
                    case HIGH_LEVEL_LIBRARY -> pair(items, TABLE, TABLE_NAME, c.category);
                    default -> throw new IllegalStateException(c.name());
                }
            }
        }
        PomSpec pom = s.pom();
        boolean property = false;
        if (s.has(Construct.DEPENDENCY)) {
            pom.direct().forEach(a -> items.add(new Expected(ItemKind.DEPENDENCY, artifact(a),
                                                             MigrationCategory.DEPENDENCY)));
            pom.managed().forEach(a -> items.add(new Expected(ItemKind.MANAGED_DEPENDENCY, artifact(a),
                                                              MigrationCategory.DEPENDENCY)));
            pom.transitive().forEach(a -> items.add(new Expected(ItemKind.EFFECTIVE_DEPENDENCY, artifact(a),
                                                                 MigrationCategory.DEPENDENCY)));
            property = !pom.directByProperty().isEmpty();
        }
        if (s.has(Construct.BOM)) {
            items.add(new Expected(ItemKind.BOM_IMPORT, artifact("bom"), MigrationCategory.BOM));
            property |= pom.bomByProperty();
        }
        if (property) {
            items.add(new Expected(ItemKind.VERSION_PROPERTY, null, MigrationCategory.DEPENDENCY));
        }
        if (s.has(Construct.BUILD)) {
            pom.buildSettings().forEach(b -> items.add(new Expected(ItemKind.BUILD_SETTING, null,
                                                                    MigrationCategory.BUILD)));
        }
        return items;
    }

    /** A type named by a fully qualified name and one of its members invoked. */
    private static void pair(List<Expected> items, String type, String member, MigrationCategory category) {
        items.add(new Expected(ItemKind.TYPE_REFERENCE, type, category));
        items.add(new Expected(ItemKind.METHOD_INVOCATION, member, category));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Checks

    /** Totality and exclusivity of the inventory: exactly the expected items, each reported once. */
    private static void checkInventory(Scenario scenario, MigrationResult result) {
        List<String> expected = expectedInventory(scenario).stream().map(Expected::describe).sorted().toList();
        List<String> reported = result.items().stream()
                                      .map(r -> new Expected(r.item().kind(), r.item().id(), r.item().category())
                                          .describe())
                                      .sorted().toList();
        assertEquals(expected, reported, "Reported items (kind, ID, category) are exactly the expected inventory");
        List<MigrationItem> items = result.items().stream().map(ItemResult::item).toList();
        assertEquals(items.size(), new HashSet<>(items).size(), "No item is reported twice");
    }

    /** Each item's single disposition agrees with its manifest lookup, diagnostic and edits. */
    private static void checkConsistency(Scenario scenario, MigrationResult result) {
        Map<Disposition, Set<MigrationItem>> partition = new EnumMap<>(Disposition.class);
        for (Disposition d : Disposition.values()) {
            partition.put(d, new LinkedHashSet<>());
        }
        List<MigrationDiagnostic> diagnostics = new ArrayList<>();
        for (ItemResult r : result.items()) {
            MigrationItem item = r.item();
            partition.get(r.disposition()).add(item);
            Lookup lookup = scenario.lookup(item.id());
            ManifestEntry generated = entry(item.id(), lookup);
            assertEquals(generated, r.entry(), () -> "Manifest entry joined by " + item);
            assertEquals(r.disposition() == Disposition.TRANSFORMED, !r.edits().isEmpty(), () -> "Edits of " + r);
            if (r.disposition() != Disposition.UNRESOLVED) {
                assertNull(r.diagnostic(), () -> "Only unresolved items carry a diagnostic: " + r);
                continue;
            }
            MigrationDiagnostic d = r.diagnostic();
            assertNotNull(d, () -> "An unresolved item carries a diagnostic: " + r);
            diagnostics.add(d);
            assertEquals(item.location(), d.location(), "Diagnostic location");
            assertEquals(item.construct(), d.construct(), "Diagnostic construct");
            assertEquals(item.id(), d.entryId(), "Diagnostic entry ID");
            assertEquals(classification(lookup), d.classification(), "Diagnostic classification");
            assertEquals(generated == null ? null : generated.rationale(), d.rationale(), "Diagnostic rationale");
            assertEquals(generated == null ? null : generated.replacement(), d.replacement(), "Diagnostic replacement");
            assertEquals(generated == null ? null : generated.migrationKind(), d.migrationKind(),
                         "Diagnostic migration kind");
            assertEquals(generated == null ? null : generated.migration(), d.migration(), "Diagnostic migration");
            assertEquals(r.reason(), d.reason(), "Diagnostic reason");
            assertTrue(d.reason() != null && !d.reason().isBlank(), "Diagnostic reason is present");
        }
        int total = 0;
        for (Disposition d : Disposition.values()) {
            assertEquals(partition.get(d).size(), result.count(d), () -> "Count of " + d);
            total += partition.get(d).size();
        }
        assertEquals(result.items().size(), total, "The three dispositions partition the items");
        assertEquals(diagnostics, result.diagnostics(), "One diagnostic per unresolved item, in item order");
    }

    /** Exactly the transformed items' insertions were applied, each once, after the unchanged original text. */
    private static void checkChangedText(MigrationResult result) {
        Map<String, List<String>> markers = new TreeMap<>();
        for (ItemResult r : result.items()) {
            if (r.disposition() == Disposition.TRANSFORMED) {
                markers.computeIfAbsent(r.item().location().path(), p -> new ArrayList<>())
                       .add(marker(r.recipe(), r.item()));
            }
        }
        assertEquals(markers.keySet(), result.changed().keySet(), "Exactly the files with transformed items change");
        markers.forEach((path, expected) -> {
            String original = result.originals().get(path);
            String changed = result.changed().get(path);
            assertTrue(changed.startsWith(original), () -> path + " keeps its original text");
            String appended = changed.substring(original.length());
            assertEquals(expected.stream().mapToInt(String::length).sum(), appended.length(),
                         () -> path + " gains exactly the transformed items' insertions: " + appended);
            expected.forEach(m -> assertTrue(appended.contains(m), () -> path + " lacks " + m));
        });
    }

    /** The machine-readable report's items and summaries partition exactly as {@code expected} (in item order). */
    private static void checkReport(Scenario scenario, MigrationResult result, List<Disposition> expected) {
        ObjectNode report = Node.parse(result.reportJson(false)).expectObjectNode();
        List<Node> items = report.expectArrayMember("items").getElements();
        assertEquals(result.items().size(), items.size(), "Report items");
        Map<Disposition, Integer> dispositions = zeroCounts();
        Map<MigrationCategory, Map<Disposition, Integer>> categories = new EnumMap<>(MigrationCategory.class);
        for (MigrationCategory c : MigrationCategory.values()) {
            categories.put(c, zeroCounts());
        }
        Map<String, Integer> classifications = new TreeMap<>();
        for (Classification c : Classification.values()) {
            classifications.put(c.name(), 0);
        }
        classifications.put("UNCLASSIFIED", 0);
        for (int i = 0; i < items.size(); i++) {
            ObjectNode node = items.get(i).expectObjectNode();
            MigrationItem item = result.items().get(i).item();
            Disposition d = expected.get(i);
            assertEquals(item.kind().name(), node.expectStringMember("kind").getValue(), "Report item kind");
            assertEquals(item.id(), nullableString(node, "id"), "Report item ID");
            assertEquals(item.category().name(), node.expectStringMember("category").getValue(),
                         "Report item category");
            assertEquals(d.name(), node.expectStringMember("disposition").getValue(),
                         () -> "Reported disposition of " + item);
            Classification classification = classification(scenario.lookup(item.id()));
            assertEquals(classification == null ? null : classification.name(),
                         nullableString(node, "classification"),
                         () -> "Reported classification of " + item);
            dispositions.merge(d, 1, Integer::sum);
            categories.get(item.category()).merge(d, 1, Integer::sum);
            classifications.merge(classification == null ? "UNCLASSIFIED" : classification.name(), 1, Integer::sum);
        }
        ObjectNode summary = report.expectObjectMember("summary");
        assertEquals(items.size(), summary.expectNumberMember("items").getValue().intValue(), "Summary items");
        assertEquals(dispositions, counts(summary.expectObjectMember("dispositions")), "Summary dispositions");
        for (MigrationCategory c : MigrationCategory.values()) {
            assertEquals(categories.get(c), counts(summary.expectObjectMember("categories").expectObjectMember(c.name())),
                         () -> "Summary dispositions of category " + c);
        }
        ObjectNode byClassification = summary.expectObjectMember("classifications");
        classifications.forEach((name, n) -> assertEquals(n, byClassification.expectNumberMember(name).getValue()
                                                                              .intValue(),
                                                          () -> "Summary items classified " + name));
        assertEquals(dispositions.get(Disposition.UNRESOLVED), report.expectArrayMember("diagnostics").size(),
                     "Report diagnostics");
    }

    /** A string member that the report writes as {@code null} when absent. */
    private static String nullableString(ObjectNode node, String member) {
        Node value = node.expectMember(member);
        return value.isNullNode() ? null : value.expectStringNode().getValue();
    }

    private static Map<Disposition, Integer> zeroCounts() {
        Map<Disposition, Integer> counts = new EnumMap<>(Disposition.class);
        for (Disposition d : Disposition.values()) {
            counts.put(d, 0);
        }
        return counts;
    }

    private static Map<Disposition, Integer> counts(ObjectNode node) {
        Map<Disposition, Integer> counts = new EnumMap<>(Disposition.class);
        for (Disposition d : Disposition.values()) {
            counts.put(d, node.expectNumberMember(d.name()).getValue().intValue());
        }
        return counts;
    }

    /**
     * Coverage of every required category, every disposition and items without entries. With the shipped recipes
     * the recipes, not the generator, decide which items are transformed, so only the generated-recipe property
     * requires transformations and manifest-decided adapted or excluded items.
     */
    private static void collectStatistics(Scenario scenario, MigrationResult result, boolean generatedRecipes) {
        for (MigrationCategory category : EnumSet.complementOf(EnumSet.of(MigrationCategory.API))) {
            String label = "encountered " + category;
            Statistics.label(label).collect(result.items().stream().anyMatch(r -> r.item().category() == category));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 30));
        }
        for (Disposition disposition : Disposition.values()) {
            String label = "any " + disposition;
            Statistics.label(label).collect(result.count(disposition) > 0);
            if (generatedRecipes || disposition != Disposition.TRANSFORMED) {
                Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 50));
            }
        }
        boolean unclassified = result.items().stream().anyMatch(r -> r.entry() == null);
        Statistics.label("item without manifest entry").collect(unclassified);
        Statistics.label("item without manifest entry").coverage(c -> c.check(true).percentage(p -> p >= 50));
        boolean manifestDecided = result.items().stream()
                                        .anyMatch(r -> r.recipe() == null && r.entry() != null
                                                       && r.entry().classification() != Classification.PRESERVED);
        Statistics.label("adapted or excluded entry decided by the manifest").collect(manifestDecided);
        if (generatedRecipes) {
            Statistics.label("adapted or excluded entry decided by the manifest")
                      .coverage(c -> c.check(true).percentage(p -> p >= 30));
        }
        Statistics.label("project").collect(scenario.hasJava() && scenario.hasPom() ? "source and POM"
                                            : scenario.hasJava() ? "source only" : "POM only");
        Statistics.label("project").coverage(c -> c.check("source and POM").percentage(p -> p >= 50));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Projects, manifests and recipes

    private static MigrationRequest request(Path root, Scenario s, List<MigrationRecipe> recipes) throws IOException {
        MigrationRequest.Builder b = MigrationRequest.builder()
                                                     .projectRoot(root)
                                                     .project("com.example:app")
                                                     .manifest(manifest(s))
                                                     .recipes(recipes);
        if (s.hasJava()) {
            write(root, JAVA_PATH, javaSource(s));
            b.sources(List.of(root.resolve(JAVA_PATH))).classpath(List.of(stubClasses));
        }
        if (s.hasPom()) {
            String pom = pomText(s);
            write(root, POM_PATH, pom);
            List<ProjectDependencies.Resolved> resolved = new ArrayList<>();
            if (s.has(Construct.DEPENDENCY)) {
                String via = GROUP + ":" + s.pom().direct().get(0);
                for (String a : s.pom().transitive()) {
                    resolved.add(new ProjectDependencies.Resolved(GROUP, a, V2_VERSION, "compile",
                                                                  List.of(via, GROUP + ":" + a)));
                }
            }
            ProjectDependencies declared = PomDocument.parse(POM_PATH, pom).standaloneDependencies();
            b.pomFile(Path.of(POM_PATH)).dependencies(new ProjectDependencies(declared.declared(), resolved));
        }
        return b.build();
    }

    private static String javaSource(Scenario s) {
        StringBuilder out = new StringBuilder("package com.example;\n\n");
        if (s.has(Construct.IMPORT)) {
            s.imports().forEach(t -> out.append("import ").append(t.substring("api:".length())).append(";\n"));
            if (s.staticImport()) {
                out.append("import static software.amazon.awssdk.core.retry.RetryPolicy.defaultRetryPolicy;\n");
            }
            out.append('\n');
        }
        out.append("public class App");
        if (s.has(Construct.INTERCEPTOR)) {
            out.append(" implements software.amazon.awssdk.core.interceptor.ExecutionInterceptor");
        }
        out.append(" {\n");
        if (s.has(Construct.INTERCEPTOR)) {
            out.append("    @Override\n"
                       + "    public void beforeExecution(software.amazon.awssdk.core.interceptor.ExecutionAttributes "
                       + "attributes) {\n"
                       + "    }\n\n");
        }
        for (Construct c : Construct.values()) {
            if (!s.has(c) || c == Construct.IMPORT || c == Construct.INTERCEPTOR || !c.java()) {
                continue;
            }
            for (int r = 0; r < s.repeats().getOrDefault(c, 1); r++) {
                out.append(member(c, r)).append('\n');
            }
        }
        return out.append("}\n").toString();
    }

    private static String member(Construct c, int r) {
        return switch (c) {
            case BUILDER -> "    Object builder" + r + "(software.amazon.awssdk.awscore.client.builder.AwsClientBuilder"
                            + "<?, ?> builder) {\n        return builder.build();\n    }\n";
            case CLIENT -> "    String client" + r + "(software.amazon.awssdk.services.demo.DemoClient client) {\n"
                           + "        return client.serviceName();\n    }\n";
            case CONFIGURATION -> "    Object region" + r + "() {\n"
                                  + "        return software.amazon.awssdk.regions.Region.of(\"us-west-2\");\n    }\n\n"
                                  + "    String environment" + r + "() {\n"
                                  + "        return System.getenv(\"AWS_REGION\");\n    }\n\n"
                                  + "    String property" + r + "() {\n"
                                  + "        return System.getProperty(\"aws.region\");\n    }\n";
            case HTTP -> "    String http" + r + "(software.amazon.awssdk.http.SdkHttpClient http) {\n"
                         + "        return http.clientName();\n    }\n";
            case RETRY -> "    Object retry" + r + "() {\n"
                          + "        return software.amazon.awssdk.core.retry.RetryPolicy.defaultRetryPolicy();\n"
                          + "    }\n";
            case SIGNER -> "    Object signer" + r + "() {\n"
                           + "        return software.amazon.awssdk.auth.signer.Aws4Signer.create();\n    }\n";
            case STREAMING -> "    Object body" + r + "() {\n"
                              + "        return software.amazon.awssdk.core.sync.RequestBody.fromString(\"x\");\n"
                              + "    }\n";
            case HIGH_LEVEL_LIBRARY -> "    String table" + r + "(software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable"
                                       + "<?> table) {\n        return table.tableName();\n    }\n";
            default -> throw new IllegalStateException(c.name());
        };
    }

    private static String pomText(Scenario s) {
        PomSpec pom = s.pom();
        boolean dependencies = s.has(Construct.DEPENDENCY);
        boolean bom = s.has(Construct.BOM);
        boolean build = s.has(Construct.BUILD);
        boolean property = (dependencies && !pom.directByProperty().isEmpty()) || (bom && pom.bomByProperty());
        StringBuilder out = new StringBuilder();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n")
           .append("  <modelVersion>4.0.0</modelVersion>\n")
           .append("  <groupId>com.example</groupId>\n")
           .append("  <artifactId>app</artifactId>\n")
           .append("  <version>1.0.0</version>\n");
        List<String> compilerProperties = build ? pom.buildSettings().stream().filter(p -> !p.startsWith("plugin:"))
                                                     .toList()
                                                : List.of();
        if (property || !compilerProperties.isEmpty()) {
            out.append("  <properties>\n");
            if (property) {
                out.append("    <" + VERSION_PROPERTY + ">" + V2_VERSION + "</" + VERSION_PROPERTY + ">\n");
            }
            compilerProperties.forEach(p -> out.append("    <" + p + ">8</" + p + ">\n"));
            out.append("  </properties>\n");
        }
        if (bom || (dependencies && !pom.managed().isEmpty())) {
            out.append("  <dependencyManagement>\n    <dependencies>\n");
            if (bom) {
                out.append(dependency("bom", pom.bomByProperty(), "      ", "        <type>pom</type>\n"
                                                                            + "        <scope>import</scope>\n"));
            }
            if (dependencies) {
                pom.managed().forEach(a -> out.append(dependency(a, false, "      ", "")));
            }
            out.append("    </dependencies>\n  </dependencyManagement>\n");
        }
        if (dependencies) {
            out.append("  <dependencies>\n");
            pom.direct().forEach(a -> out.append(dependency(a, pom.directByProperty().contains(a), "    ", "")));
            out.append("  </dependencies>\n");
        }
        if (build && pom.buildSettings().contains("plugin:release")) {
            out.append("  <build>\n    <plugins>\n      <plugin>\n")
               .append("        <groupId>org.apache.maven.plugins</groupId>\n")
               .append("        <artifactId>maven-compiler-plugin</artifactId>\n")
               .append("        <configuration>\n          <release>8</release>\n        </configuration>\n")
               .append("      </plugin>\n    </plugins>\n  </build>\n");
        }
        return out.append("</project>\n").toString();
    }

    private static String dependency(String artifactId, boolean byProperty, String indent, String extra) {
        String inner = indent + "  ";
        String version = byProperty ? "${" + VERSION_PROPERTY + "}" : V2_VERSION;
        return indent + "<dependency>\n"
               + inner + "<groupId>" + GROUP + "</groupId>\n"
               + inner + "<artifactId>" + artifactId + "</artifactId>\n"
               + inner + "<version>" + version + "</version>\n"
               + extra
               + indent + "</dependency>\n";
    }

    private static MigrationManifest manifest(Scenario s) {
        List<ManifestEntry> entries = new ArrayList<>();
        for (String id : ALL_IDS) {
            ManifestEntry e = entry(id, s.lookup(id));
            if (e != null) {
                entries.add(e);
            }
        }
        return MigrationManifest.of(BASELINE, entries);
    }

    /** The generated manifest entry of {@code id}, or {@code null} without one. */
    private static ManifestEntry entry(String id, Lookup lookup) {
        if (id == null || lookup == Lookup.ABSENT) {
            return null;
        }
        EntryKind kind = id.startsWith("artifact:") ? EntryKind.ARTIFACT
                         : id.startsWith("config:") ? EntryKind.CONFIGURATION
                         : id.contains("#") ? EntryKind.PUBLIC_MEMBER : EntryKind.PUBLIC_TYPE;
        String surface = "V2 " + id;
        String source = "software.amazon.awssdk:sdk-core";
        return switch (lookup) {
            case PRESERVED -> new ManifestEntry(id, kind, surface, source, null, false, Classification.PRESERVED,
                                                List.of("source"), "Kept unchanged in V3: " + id + ".", List.of(),
                                                null, null, null, null, null, List.of());
            case ADAPTED_RECIPE, ADAPTED_DIAGNOSTIC ->
                new ManifestEntry(id, kind, surface, source, null, false, Classification.ADAPTED, List.of(),
                                  "V3 changes the contract of " + id + ".", List.of("The V3 contract differs."),
                                  "software.amazon.awssdk:sdk-core", "api:software.amazon.awssdk.v3.Replacement",
                                  lookup == Lookup.ADAPTED_RECIPE ? MigrationKind.RECIPE : MigrationKind.DIAGNOSTIC,
                                  "Adapt " + id + ".", null, List.of("test:property59"));
            case EXCLUDED -> new ManifestEntry(id, kind, surface, source, null, true, Classification.EXCLUDED,
                                               List.of(), "Deprecated legacy facility " + id + ".", List.of(), null,
                                               id.hashCode() % 2 == 0 ? null : "api:software.amazon.awssdk.v3.Modern",
                                               MigrationKind.DIAGNOSTIC, "Remove " + id + ".", null, List.of());
            case ABSENT -> null;
        };
    }

    /** Generated recipe {@code index}: answers each item as the scenario says, transforming by a unique insertion. */
    private static MigrationRecipe generatedRecipe(Scenario s, int index) {
        String id = RECIPE_IDS.get(index);
        return new MigrationRecipe() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String description() {
                return "Generated recipe " + id + ".";
            }

            @Override
            public RecipeOutcome visit(RecipeContext context) {
                MigrationItem item = context.item();
                List<Answer> answers = s.answers(item.kind(), item.id());
                Answer answer = answers.isEmpty() ? Answer.NOT_APPLICABLE : answers.get(index);
                return switch (answer) {
                    case NOT_APPLICABLE -> RecipeOutcome.notApplicable();
                    case PRESERVE -> RecipeOutcome.preserve(id + " preserves " + item.construct() + ".");
                    case UNSAFE -> RecipeOutcome.unsafe(id + " cannot transform " + item.construct() + ".");
                    case TRANSFORM -> {
                        String path = item.location().path();
                        yield RecipeOutcome.transform(id + " transforms " + item.construct() + ".", List.of(
                            TextEdit.insert(path, context.text(path).length(), marker(id, item))));
                    }
                };
            }
        };
    }

    /** The unique end-of-file comment that recipe {@code recipe} inserts to transform {@code item}. */
    private static String marker(String recipe, MigrationItem item) {
        String body = "property59 " + recipe + " " + item.kind() + " " + item.id() + " @"
                      + item.location().startOffset() + "-" + item.location().endOffset();
        return item.location().path().endsWith(".xml") ? "\n<!-- " + body + " -->" : "\n// " + body;
    }

    /** {@code delegate}, recording each answer (a thrown exception or a missing answer as unsafe). */
    private static MigrationRecipe recording(MigrationRecipe delegate, Map<MigrationItem, List<Vote>> recorded) {
        return new MigrationRecipe() {
            @Override
            public String id() {
                return delegate.id();
            }

            @Override
            public String description() {
                return delegate.description();
            }

            @Override
            public RecipeOutcome visit(RecipeContext context) {
                List<Vote> votes = recorded.computeIfAbsent(context.item(), i -> new ArrayList<>());
                RecipeOutcome outcome;
                try {
                    outcome = delegate.visit(context);
                } catch (RuntimeException e) {
                    votes.add(new Vote(delegate.id(), Answer.UNSAFE, null));
                    throw e;
                }
                Answer answer;
                if (outcome == null || outcome instanceof RecipeOutcome.Unsafe) {
                    answer = Answer.UNSAFE;
                } else if (outcome instanceof RecipeOutcome.Transform) {
                    answer = Answer.TRANSFORM;
                } else if (outcome instanceof RecipeOutcome.Preserve) {
                    answer = Answer.PRESERVE;
                } else {
                    answer = Answer.NOT_APPLICABLE;
                }
                votes.add(new Vote(delegate.id(), answer, outcome));
                return outcome;
            }
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators

    @Provide
    Arbitrary<Scenario> scenarios() {
        int constructs = Construct.values().length;
        Arbitrary<List<Boolean>> picks = Arbitraries.of(true, false).list().ofSize(constructs);
        Arbitrary<List<Integer>> repeats = Arbitraries.integers().between(1, 2).list().ofSize(constructs);
        Arbitrary<List<String>> imports = ordered(Arbitraries.subsetOf(IMPORT_TYPES).ofMinSize(1), IMPORT_TYPES);
        Arbitrary<Boolean> staticImport = Arbitraries.of(true, false);
        Arbitrary<List<Lookup>> lookups = Arbitraries.frequency(Tuple.of(3, Lookup.PRESERVED),
                                                                Tuple.of(2, Lookup.ADAPTED_RECIPE),
                                                                Tuple.of(2, Lookup.ADAPTED_DIAGNOSTIC),
                                                                Tuple.of(2, Lookup.EXCLUDED),
                                                                Tuple.of(2, Lookup.ABSENT))
                                                     .list().ofSize(ALL_IDS.size());
        Arbitrary<List<Answer>> votes = Arbitraries.frequency(Tuple.of(4, Answer.NOT_APPLICABLE),
                                                              Tuple.of(2, Answer.PRESERVE),
                                                              Tuple.of(3, Answer.TRANSFORM),
                                                              Tuple.of(1, Answer.UNSAFE))
                                                   .list().ofSize(RECIPE_IDS.size());
        List<Answer> none = RECIPE_IDS.stream().map(r -> Answer.NOT_APPLICABLE).toList();
        Arbitrary<List<Answer>> answer = Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(none)),
                                                                 Tuple.of(1, votes));
        Arbitrary<List<List<Answer>>> answers = answer.list().ofSize(ALL_KEYS.size());
        return Combinators.combine(picks, repeats, imports, staticImport, pomSpecs(), lookups, answers)
                          .as(MigrationDispositionsTotalExclusivePropertyTest::scenario);
    }

    private static Arbitrary<PomSpec> pomSpecs() {
        Arbitrary<List<String>> direct = ordered(Arbitraries.subsetOf(DIRECT_ARTIFACTS).ofMinSize(1),
                                                 DIRECT_ARTIFACTS);
        Arbitrary<List<String>> byProperty = ordered(Arbitraries.subsetOf(DIRECT_ARTIFACTS), DIRECT_ARTIFACTS);
        Arbitrary<List<String>> managed = ordered(Arbitraries.subsetOf(MANAGED_ARTIFACTS), MANAGED_ARTIFACTS);
        Arbitrary<List<String>> transitive = ordered(Arbitraries.subsetOf(TRANSITIVE_ARTIFACTS), TRANSITIVE_ARTIFACTS);
        Arbitrary<List<String>> settings = ordered(Arbitraries.subsetOf(BUILD_SETTINGS).ofMinSize(1), BUILD_SETTINGS);
        return Combinators.combine(direct, byProperty, managed, transitive, Arbitraries.of(true, false), settings)
                          .as((d, p, m, t, b, s) -> new PomSpec(d, p.stream().filter(d::contains).toList(), m, t, b,
                                                                s));
    }

    private static Arbitrary<List<String>> ordered(Arbitrary<Set<String>> subsets, List<String> pool) {
        return subsets.map(set -> pool.stream().filter(set::contains).toList());
    }

    private static Scenario scenario(List<Boolean> picks, List<Integer> repeats, List<String> imports,
                                     boolean staticImport, PomSpec pom, List<Lookup> lookups,
                                     List<List<Answer>> answers) {
        Set<Construct> constructs = EnumSet.noneOf(Construct.class);
        Map<Construct, Integer> repeatsByConstruct = new EnumMap<>(Construct.class);
        Construct[] values = Construct.values();
        for (int i = 0; i < values.length; i++) {
            if (picks.get(i)) {
                constructs.add(values[i]);
            }
            repeatsByConstruct.put(values[i], repeats.get(i));
        }
        if (constructs.isEmpty()) {
            constructs.add(Construct.IMPORT);
        }
        // The POM's build settings are inventoried only for a project that uses V2 artifacts.
        if (constructs.contains(Construct.BUILD) && !constructs.contains(Construct.BOM)) {
            constructs.add(Construct.DEPENDENCY);
        }
        Map<String, Lookup> lookupsById = new TreeMap<>();
        for (int i = 0; i < ALL_IDS.size(); i++) {
            lookupsById.put(ALL_IDS.get(i), lookups.get(i));
        }
        Map<String, List<Answer>> answersByKey = new TreeMap<>();
        for (int i = 0; i < ALL_KEYS.size(); i++) {
            answersByKey.put(ALL_KEYS.get(i), answers.get(i));
        }
        return new Scenario(constructs, repeatsByConstruct, imports, staticImport, pom, lookupsById, answersByKey);
    }

    /** Every construct, import, POM declaration and setting, with every ID looked up as {@code lookup}. */
    private static Scenario maximal(Lookup lookup) {
        PomSpec pom = new PomSpec(DIRECT_ARTIFACTS, DIRECT_ARTIFACTS, MANAGED_ARTIFACTS, TRANSITIVE_ARTIFACTS, true,
                                  BUILD_SETTINGS);
        Map<String, Lookup> lookups = new TreeMap<>();
        ALL_IDS.forEach(id -> lookups.put(id, lookup));
        return new Scenario(EnumSet.allOf(Construct.class), Map.of(), IMPORT_TYPES, true, pom, lookups, Map.of());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Files

    private static Map.Entry<String, String> stub(String id, String body) {
        String binary = id.substring("api:".length());
        String pkg = binary.substring(0, binary.lastIndexOf('.'));
        return Map.entry(binary, "package " + pkg + ";\n\n" + body + "\n");
    }

    private static String artifact(String artifactId) {
        return "artifact:" + GROUP + ":" + artifactId;
    }

    private static void write(Path root, String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
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
}
