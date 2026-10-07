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

package software.amazon.awssdk.v2migration.recipes.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationDiagnostic;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.SourceLocation;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestBaseline;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/**
 * Property 61 (Requirements 18.7, 18.8): for any encountered excluded signer or retry API with a replacement, the
 * migration tooling names the corresponding Modern Auth or Modern Retry replacement.
 *
 * <p>Each try writes one to three Java files that use a V2-shaped legacy signer and retry surface (compiled once from
 * the stubs below, with the packages, nesting and descriptors of the real V2 APIs): imports or fully qualified names,
 * type references (including nested types and generic arguments), static and instance method invocations (including
 * builder chains split over lines), constructor calls, field accesses ({@code SdkAdvancedClientOption.SIGNER}),
 * overrides of legacy {@code Signer}, {@code Presigner}, {@code BackoffStrategy} and {@code RetryCondition} methods in
 * named and anonymous classes, and the request-override {@code signer(...)} and client-override
 * {@code retryPolicy(...)} setters. Files vary their line terminators, indentation, header lines, import order and
 * non-ASCII comments before constructs. The Parity Manifest classifies every legacy facility API {@code EXCLUDED}, with
 * a generated Modern Auth or Modern Retry replacement (the checked-in replacements, a nested type and a member ID) or,
 * for an API whose name alone makes it a signer or retry API, with none; the API's container types are
 * {@code PRESERVED}. The engine runs with every registered recipe.
 *
 * <p>The oracle is the renderer itself: it records the offsets of every construct as it writes it, following the
 * documented span of each {@link ItemKind}, and computes lines and columns with its own line scan. It knows each
 * API's facility from the catalog below and the expected replacement from the generated manifest choice. The property
 * checks that
 * <ul>
 *   <li>the encountered excluded facility constructs are exactly the rendered ones, each at the oracle's path,
 *       offsets, line and column, with the rendered construct text;</li>
 *   <li>each is unresolved by {@code excluded.legacy-signer-retry} with a diagnostic whose replacement is exactly the
 *       manifest's, belongs to the API's own facility, and is named in the reason under that facility's label (and
 *       never under the other's); without a replacement, the reason says no safe replacement of that facility exists;
 *       the documentation identifier is the entry ID and the rationale the entry's;</li>
 *   <li>every other construct is preserved, nothing is unattributed, and no source text changes.</li>
 * </ul>
 */
class ModernExclusionDiagnosticsPropertyTest {
    private static final String V2 = "software.amazon.awssdk.";
    private static final String RECIPE = "excluded.legacy-signer-retry";

    /** The two facilities, their diagnostic labels and the Modern replacements a manifest may record for them. */
    enum Family {
        AUTH("Modern Auth", Map.of(
            "api:software.amazon.awssdk.auth.api.HttpSigner", "software.amazon.awssdk.auth.api.HttpSigner",
            "api:software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions", "software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions",
            "api:software.amazon.awssdk.auth.aws.AwsV4Presigning", "software.amazon.awssdk.auth.aws.AwsV4Presigning",
            "api:software.amazon.awssdk.auth.aws.AwsV4Presigning#presign(Ljava/lang/String;)Ljava/lang/String;",
            "software.amazon.awssdk.auth.aws.AwsV4Presigning#presign")),
        RETRY("Modern Retry", Map.of(
            "api:software.amazon.awssdk.retries.RetryStrategy", "software.amazon.awssdk.retries.RetryStrategy",
            "api:software.amazon.awssdk.retries.BackoffStrategy", "software.amazon.awssdk.retries.BackoffStrategy",
            "api:software.amazon.awssdk.retries.RetryClassifier", "software.amazon.awssdk.retries.RetryClassifier",
            "api:software.amazon.awssdk.retries.StandardRetryStrategy",
            "software.amazon.awssdk.retries.StandardRetryStrategy",
            "api:software.amazon.awssdk.retries.RetryStrategy$Builder",
            "software.amazon.awssdk.retries.RetryStrategy.Builder"));

        final String label;
        /** Replacement ID to the name a diagnostic shows for it, written out by hand. */
        final Map<String, String> displays;
        final List<String> replacements;

        Family(String label, Map<String, String> displays) {
            this.label = label;
            this.displays = displays;
            this.replacements = displays.keySet().stream().sorted().toList();
        }

        Family other() {
            return this == AUTH ? RETRY : AUTH;
        }
    }

    // V2 binary names of the stub surface.
    private static final String SIGNER = V2 + "core.signer.Signer";
    private static final String PRESIGNER = V2 + "core.signer.Presigner";
    private static final String AWS4_SIGNER = V2 + "auth.signer.Aws4Signer";
    private static final String REQUEST_OVERRIDE = V2 + "core.RequestOverrideConfiguration";
    private static final String REQUEST_OVERRIDE_BUILDER = REQUEST_OVERRIDE + "$Builder";
    private static final String ADVANCED_OPTION = V2 + "core.client.config.SdkAdvancedClientOption";
    private static final String CLIENT_OVERRIDE = V2 + "core.client.config.ClientOverrideConfiguration";
    private static final String CLIENT_OVERRIDE_BUILDER = CLIENT_OVERRIDE + "$Builder";
    private static final String SIGNER_UTILS = V2 + "awscore.util.SignerOverrideUtils";
    private static final String RETRY_POLICY = V2 + "core.retry.RetryPolicy";
    private static final String RETRY_POLICY_BUILDER = RETRY_POLICY + "$Builder";
    private static final String BACKOFF = V2 + "core.retry.backoff.BackoffStrategy";
    private static final String FIXED_DELAY = V2 + "core.retry.backoff.FixedDelayBackoffStrategy";
    private static final String RETRY_CONDITION = V2 + "core.retry.conditions.RetryCondition";
    private static final String LEGACY_STRATEGY = V2 + "retries.LegacyRetryStrategy";
    private static final String LEGACY_STRATEGY_BUILDER = LEGACY_STRATEGY + "$Builder";
    private static final String AWS_RETRY_POLICY = V2 + "awscore.retry.AwsRetryPolicy";

    // Manifest IDs of the members the fixtures use, with their JVM descriptors written out by hand.
    private static final String AWS4_CREATE =
        "api:software.amazon.awssdk.auth.signer.Aws4Signer#create()Lsoftware/amazon/awssdk/auth/signer/Aws4Signer;";
    private static final String AWS4_INIT = "api:software.amazon.awssdk.auth.signer.Aws4Signer#<init>()V";
    private static final String REQUEST_SIGNER =
        "api:software.amazon.awssdk.core.RequestOverrideConfiguration$Builder#signer(Lsoftware/amazon/awssdk/core/signer/"
        + "Signer;)Lsoftware/amazon/awssdk/core/RequestOverrideConfiguration$Builder;";
    private static final String ADVANCED_SIGNER =
        "api:software.amazon.awssdk.core.client.config.SdkAdvancedClientOption#SIGNER:Lsoftware/amazon/awssdk/core/client/"
        + "config/SdkAdvancedClientOption;";
    private static final String SIGNER_SIGN =
        "api:software.amazon.awssdk.core.signer.Signer#sign(Ljava/lang/String;)Ljava/lang/String;";
    private static final String PRESIGNER_PRESIGN =
        "api:software.amazon.awssdk.core.signer.Presigner#presign(Ljava/lang/String;)Ljava/lang/String;";
    private static final String SIGNER_OVERRIDDEN =
        "api:software.amazon.awssdk.awscore.util.SignerOverrideUtils#isSignerOverridden(Ljava/lang/Object;)Z";
    private static final String RETRY_POLICY_NONE =
        "api:software.amazon.awssdk.core.retry.RetryPolicy#none()Lsoftware/amazon/awssdk/core/retry/RetryPolicy;";
    private static final String RETRY_POLICY_BUILDER_METHOD =
        "api:software.amazon.awssdk.core.retry.RetryPolicy#builder()Lsoftware/amazon/awssdk/core/retry/RetryPolicy$Builder;";
    private static final String NUM_RETRIES =
        "api:software.amazon.awssdk.core.retry.RetryPolicy$Builder#numRetries(Ljava/lang/Integer;)Lsoftware/amazon/awssdk/"
        + "core/retry/RetryPolicy$Builder;";
    private static final String RETRY_POLICY_BUILD =
        "api:software.amazon.awssdk.core.retry.RetryPolicy$Builder#build()Lsoftware/amazon/awssdk/core/retry/RetryPolicy;";
    private static final String RETRY_POLICY_SETTER =
        "api:software.amazon.awssdk.core.client.config.ClientOverrideConfiguration$Builder#retryPolicy(Lsoftware/amazon/"
        + "awssdk/core/retry/RetryPolicy;)Lsoftware/amazon/awssdk/core/client/config/ClientOverrideConfiguration$Builder;";
    private static final String FIXED_DELAY_CREATE =
        "api:software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy#create(J)Lsoftware/amazon/awssdk/core/"
        + "retry/backoff/FixedDelayBackoffStrategy;";
    private static final String FIXED_DELAY_INIT =
        "api:software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy#<init>(J)V";
    private static final String BACKOFF_DELAY = "api:software.amazon.awssdk.core.retry.backoff.BackoffStrategy#delayMillis(I)J";
    private static final String CONDITION_SHOULD_RETRY =
        "api:software.amazon.awssdk.core.retry.conditions.RetryCondition#shouldRetry(Ljava/lang/Object;)Z";
    private static final String CONDITION_NONE =
        "api:software.amazon.awssdk.core.retry.conditions.RetryCondition#none()Lsoftware/amazon/awssdk/core/retry/"
        + "conditions/RetryCondition;";
    private static final String LEGACY_BUILDER_METHOD =
        "api:software.amazon.awssdk.retries.LegacyRetryStrategy#builder()Lsoftware/amazon/awssdk/retries/"
        + "LegacyRetryStrategy$Builder;";
    private static final String LEGACY_BUILD =
        "api:software.amazon.awssdk.retries.LegacyRetryStrategy$Builder#build()Lsoftware/amazon/awssdk/retries/"
        + "LegacyRetryStrategy;";
    private static final String AWS_DEFAULT_POLICY =
        "api:software.amazon.awssdk.awscore.retry.AwsRetryPolicy#defaultRetryPolicy()Lsoftware/amazon/awssdk/core/retry/"
        + "RetryPolicy;";
    private static final String AWS_DEFAULT_CONDITION =
        "api:software.amazon.awssdk.awscore.retry.AwsRetryPolicy#defaultRetryCondition()Lsoftware/amazon/awssdk/core/"
        + "retry/conditions/RetryCondition;";

    /**
     * A legacy facility API the manifest excludes. {@code named} is true when the API is a signer or retry API by its
     * own name (a {@code signer}, {@code retry} or {@code retries} package of V2, or a member named for signing or
     * retrying); only such an API may be excluded without a replacement and still be recognised as a facility, since
     * otherwise only its Modern replacement says which facility it belongs to.
     */
    record Api(String id, Family family, boolean named) {
    }

    private static final List<Api> FACILITY = List.of(
        new Api("api:" + SIGNER, Family.AUTH, true),
        new Api(SIGNER_SIGN, Family.AUTH, true),
        new Api("api:" + PRESIGNER, Family.AUTH, true),
        new Api(PRESIGNER_PRESIGN, Family.AUTH, true),
        new Api("api:" + AWS4_SIGNER, Family.AUTH, true),
        new Api(AWS4_CREATE, Family.AUTH, true),
        new Api(AWS4_INIT, Family.AUTH, true),
        new Api(REQUEST_SIGNER, Family.AUTH, true),
        new Api(ADVANCED_SIGNER, Family.AUTH, true),
        new Api("api:" + SIGNER_UTILS, Family.AUTH, false),
        new Api(SIGNER_OVERRIDDEN, Family.AUTH, true),
        new Api("api:" + RETRY_POLICY, Family.RETRY, true),
        new Api("api:" + RETRY_POLICY_BUILDER, Family.RETRY, true),
        new Api(RETRY_POLICY_NONE, Family.RETRY, true),
        new Api(RETRY_POLICY_BUILDER_METHOD, Family.RETRY, true),
        new Api(NUM_RETRIES, Family.RETRY, true),
        new Api(RETRY_POLICY_BUILD, Family.RETRY, true),
        new Api(RETRY_POLICY_SETTER, Family.RETRY, true),
        new Api("api:" + BACKOFF, Family.RETRY, true),
        new Api(BACKOFF_DELAY, Family.RETRY, true),
        new Api("api:" + FIXED_DELAY, Family.RETRY, true),
        new Api(FIXED_DELAY_CREATE, Family.RETRY, true),
        new Api(FIXED_DELAY_INIT, Family.RETRY, true),
        new Api("api:" + RETRY_CONDITION, Family.RETRY, true),
        new Api(CONDITION_SHOULD_RETRY, Family.RETRY, true),
        new Api(CONDITION_NONE, Family.RETRY, true),
        new Api("api:" + LEGACY_STRATEGY, Family.RETRY, true),
        new Api("api:" + LEGACY_STRATEGY_BUILDER, Family.RETRY, true),
        new Api(LEGACY_BUILDER_METHOD, Family.RETRY, true),
        new Api(LEGACY_BUILD, Family.RETRY, true),
        new Api("api:" + AWS_RETRY_POLICY, Family.RETRY, true),
        new Api(AWS_DEFAULT_POLICY, Family.RETRY, true),
        new Api(AWS_DEFAULT_CONDITION, Family.RETRY, true));
    private static final Map<String, Api> FACILITY_BY_ID =
        FACILITY.stream().collect(Collectors.toMap(Api::id, a -> a, (a, b) -> a, LinkedHashMap::new));

    /** The containers of the excluded members; V3 keeps them. */
    private static final List<String> PRESERVED = List.of(
        "api:" + REQUEST_OVERRIDE, "api:" + REQUEST_OVERRIDE_BUILDER, "api:" + ADVANCED_OPTION, "api:" + CLIENT_OVERRIDE,
        "api:" + CLIENT_OVERRIDE_BUILDER);

    private static final List<String> RATIONALES = List.of(
        "Pre-SRA signer APIs are excluded; signing is selected by auth scheme and identity provider",
        "Legacy retry policies are replaced by the Modern Retry strategy API.",
        "Deprecated before the feature freeze; V3 makes no preservation claim for it.");
    private static final String MIGRATION = "Configure the Modern facility on the client builder instead.";

    private static final ManifestBaseline BASELINE = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                          "parity-manifest/property61", null, null);

    /** V2-shaped stubs: top-level binary name to source. */
    private static final Map<String, String> STUBS = Map.ofEntries(
        Map.entry(SIGNER, "package software.amazon.awssdk.core.signer;\n"
                          + "@Deprecated\npublic interface Signer {\n    String sign(String request);\n}\n"),
        Map.entry(PRESIGNER, "package software.amazon.awssdk.core.signer;\n"
                             + "@Deprecated\npublic interface Presigner {\n    String presign(String request);\n}\n"),
        Map.entry(AWS4_SIGNER, "package software.amazon.awssdk.auth.signer;\n"
                               + "import software.amazon.awssdk.core.signer.Signer;\n"
                               + "@Deprecated\npublic final class Aws4Signer implements Signer {\n"
                               + "    public Aws4Signer() {}\n"
                               + "    public static Aws4Signer create() { return new Aws4Signer(); }\n"
                               + "    @Override public String sign(String request) { return request; }\n}\n"),
        Map.entry(REQUEST_OVERRIDE, "package software.amazon.awssdk.core;\n"
                                    + "import software.amazon.awssdk.core.signer.Signer;\n"
                                    + "public abstract class RequestOverrideConfiguration {\n"
                                    + "    public interface Builder {\n"
                                    + "        @Deprecated Builder signer(Signer signer);\n    }\n}\n"),
        Map.entry(ADVANCED_OPTION, "package software.amazon.awssdk.core.client.config;\n"
                                   + "import software.amazon.awssdk.core.signer.Signer;\n"
                                   + "public final class SdkAdvancedClientOption<T> {\n"
                                   + "    @Deprecated public static final SdkAdvancedClientOption<Signer> SIGNER =\n"
                                   + "        new SdkAdvancedClientOption<>();\n"
                                   + "    private SdkAdvancedClientOption() {}\n}\n"),
        Map.entry(CLIENT_OVERRIDE, "package software.amazon.awssdk.core.client.config;\n"
                                   + "import software.amazon.awssdk.core.retry.RetryPolicy;\n"
                                   + "public final class ClientOverrideConfiguration {\n"
                                   + "    private ClientOverrideConfiguration() {}\n"
                                   + "    public interface Builder {\n"
                                   + "        @Deprecated Builder retryPolicy(RetryPolicy retryPolicy);\n    }\n}\n"),
        Map.entry(SIGNER_UTILS, "package software.amazon.awssdk.awscore.util;\n"
                                + "@Deprecated\npublic final class SignerOverrideUtils {\n"
                                + "    private SignerOverrideUtils() {}\n"
                                + "    public static boolean isSignerOverridden(Object configuration) { return false; }\n}\n"),
        Map.entry(RETRY_POLICY, "package software.amazon.awssdk.core.retry;\n"
                                + "@Deprecated\npublic final class RetryPolicy {\n"
                                + "    private RetryPolicy() {}\n"
                                + "    public static RetryPolicy none() { return new RetryPolicy(); }\n"
                                + "    public static Builder builder() { return null; }\n"
                                + "    public interface Builder {\n"
                                + "        Builder numRetries(Integer numRetries);\n"
                                + "        RetryPolicy build();\n    }\n}\n"),
        Map.entry(BACKOFF, "package software.amazon.awssdk.core.retry.backoff;\n"
                           + "@Deprecated\npublic interface BackoffStrategy {\n    long delayMillis(int attempt);\n}\n"),
        Map.entry(FIXED_DELAY, "package software.amazon.awssdk.core.retry.backoff;\n"
                               + "@Deprecated\npublic final class FixedDelayBackoffStrategy implements BackoffStrategy {\n"
                               + "    public FixedDelayBackoffStrategy(long millis) {}\n"
                               + "    public static FixedDelayBackoffStrategy create(long millis) {\n"
                               + "        return new FixedDelayBackoffStrategy(millis);\n    }\n"
                               + "    @Override public long delayMillis(int attempt) { return 0L; }\n}\n"),
        Map.entry(RETRY_CONDITION, "package software.amazon.awssdk.core.retry.conditions;\n"
                                   + "@Deprecated\npublic interface RetryCondition {\n"
                                   + "    boolean shouldRetry(Object context);\n"
                                   + "    static RetryCondition none() { return context -> false; }\n}\n"),
        Map.entry(LEGACY_STRATEGY, "package software.amazon.awssdk.retries;\n"
                                   + "public interface LegacyRetryStrategy {\n"
                                   + "    static Builder builder() { return null; }\n"
                                   + "    interface Builder {\n        LegacyRetryStrategy build();\n    }\n}\n"),
        Map.entry(AWS_RETRY_POLICY, "package software.amazon.awssdk.awscore.retry;\n"
                                    + "import software.amazon.awssdk.core.retry.RetryPolicy;\n"
                                    + "import software.amazon.awssdk.core.retry.conditions.RetryCondition;\n"
                                    + "@Deprecated\npublic final class AwsRetryPolicy {\n"
                                    + "    private AwsRetryPolicy() {}\n"
                                    + "    public static RetryPolicy defaultRetryPolicy() { return RetryPolicy.none(); }\n"
                                    + "    public static RetryCondition defaultRetryCondition() {\n"
                                    + "        return RetryCondition.none();\n    }\n}\n"));

    private static final Set<ItemKind> USAGE_KINDS = EnumSet.of(
        ItemKind.IMPORT, ItemKind.TYPE_REFERENCE, ItemKind.METHOD_INVOCATION, ItemKind.CONSTRUCTOR_CALL,
        ItemKind.FIELD_ACCESS, ItemKind.METHOD_OVERRIDE);

    private static Path stubDirectory;
    private static Path stubClasses;
    private static List<MigrationRecipe> recipes;

    @BeforeContainer
    static void compileStubs() throws IOException {
        stubDirectory = Files.createTempDirectory("property61-stubs");
        stubClasses = compile(stubDirectory, STUBS);
        recipes = Recipes.discover(ModernExclusionDiagnosticsPropertyTest.class.getClassLoader());
        assertTrue(recipes.stream().anyMatch(r -> r.id().equals(RECIPE)), "The recipe under test is registered");
    }

    @AfterContainer
    static void deleteStubs() {
        delete(stubDirectory);
    }

    // Feature: aws-sdk-java-v3, Property 61: Excluded facilities receive modern diagnostics
    // Embedded edge cases are disabled: they collapse every choice to its first alternative (one file, one usage).
    // The statistics coverage checks fail the property if the generated mix degrades.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 61: Excluded facilities receive modern diagnostics")
    void excludedFacilitiesReceiveModernDiagnostics(@ForAll("scenarios") Scenario scenario) throws IOException {
        Map<String, Expected> manifestChoice = expectations(scenario.choices());
        List<Rendered> files = new ArrayList<>();
        for (int i = 0; i < scenario.files().size(); i++) {
            files.add(render(i, scenario.files().get(i)));
        }

        Path project = Files.createTempDirectory("property61-project");
        try {
            List<Path> sources = new ArrayList<>();
            for (Rendered file : files) {
                Path path = project.resolve(file.path());
                Files.createDirectories(path.getParent());
                Files.writeString(path, file.text(), StandardCharsets.UTF_8);
                sources.add(path);
            }
            MigrationResult result = MigrationEngine.run(MigrationRequest.builder()
                                                                         .projectRoot(project)
                                                                         .project("com.example:property61")
                                                                         .sources(sources)
                                                                         .classpath(List.of(stubClasses))
                                                                         .manifest(manifest(manifestChoice))
                                                                         .recipes(recipes)
                                                                         .build());

            // Exactly the rendered excluded facility constructs are encountered, at the oracle's positions.
            Map<String, Span> expected = new LinkedHashMap<>();
            for (Rendered file : files) {
                for (Span span : file.spans()) {
                    if (FACILITY_BY_ID.containsKey(span.id())) {
                        expected.merge(span.key(), span, (a, b) -> a);
                    }
                }
            }
            List<String> expectedKeys = files.stream().flatMap(f -> f.spans().stream())
                                             .filter(s -> FACILITY_BY_ID.containsKey(s.id())).map(Span::key).sorted()
                                             .toList();
            List<ItemResult> facilityItems = result.items().stream()
                                                   .filter(r -> r.item().id() != null
                                                                && FACILITY_BY_ID.containsKey(r.item().id()))
                                                   .toList();
            assertEquals(expectedKeys,
                         facilityItems.stream().map(r -> key(r.item().kind(), r.item().id(), r.item().location()))
                                      .sorted().toList(),
                         "Encountered excluded signer and retry constructs");

            for (ItemResult item : facilityItems) {
                Span span = expected.get(key(item.item().kind(), item.item().id(), item.item().location()));
                checkDiagnostic(item, span, manifestChoice.get(item.item().id()), result);
            }

            // Everything else is a preserved container, and the compiler attributed every construct.
            for (ItemResult item : result.items()) {
                assertNotNull(item.item().id(), () -> "Unidentified construct " + item);
                if (!FACILITY_BY_ID.containsKey(item.item().id())) {
                    assertTrue(PRESERVED.contains(item.item().id()), () -> "Unexpected construct " + item);
                    assertEquals(Disposition.PRESERVED, item.disposition(), item::toString);
                }
            }
            // The source is unchanged, in the result and on disk.
            assertEquals(Map.of(), result.changed(), "Changed files");
            assertTrue(result.items().stream().noneMatch(r -> r.disposition() == Disposition.TRANSFORMED));
            for (Rendered file : files) {
                assertEquals(file.text(), Files.readString(project.resolve(file.path()), StandardCharsets.UTF_8));
            }
            collectStatistics(scenario, files, facilityItems);
        } finally {
            delete(project);
        }
    }

    private static void checkDiagnostic(ItemResult item, Span span, Expected expected, MigrationResult result) {
        String id = item.item().id();
        Api api = FACILITY_BY_ID.get(id);
        assertEquals(Disposition.UNRESOLVED, item.disposition(), item::toString);
        MigrationDiagnostic d = item.diagnostic();
        assertEquals(span.location(), d.location(), "Location of " + span.key());
        assertEquals(span.construct(), d.construct(), "Construct of " + span.key());
        assertEquals(id, d.entryId());
        assertEquals(Classification.EXCLUDED, d.classification());
        assertEquals(id, d.documentationId(), "Documentation identifier");
        assertEquals(expected.rationale(), d.rationale());
        assertEquals(expected.replacement(), d.replacement(), "Replacement of " + id);
        assertTrue(result.diagnostics().contains(d), "The result reports the item's diagnostic");

        String reason = d.reason();
        assertTrue(reason.contains("Recipe " + RECIPE + " judged the transformation unsafe: "), reason);
        // The manifest's rationale and migration text may mention either facility; the recipe's own wording may not.
        Family other = api.family().other();
        assertFalse(reason.contains(other.label + " replacement") || reason.contains("No safe " + other.label),
                    () -> "Names the other facility: " + reason);
        if (expected.replacement() != null) {
            assertTrue(api.family().replacements.contains(d.replacement()), "Replacement family of " + id);
            String named = api.family().label + " replacement: " + api.family().displays.get(expected.replacement())
                           + " (" + expected.replacement() + ")";
            assertTrue(reason.contains(named), () -> "Expected '" + named + "' in: " + reason);
            assertFalse(reason.contains("No safe"), reason);
            if (expected.migration() != null) {
                assertTrue(reason.contains(expected.migration()), reason);
            }
            assertTrue(d.message().contains(" Replacement: " + expected.replacement() + "."), d.message());
        } else {
            assertNull(d.replacement());
            assertTrue(reason.contains("No safe " + api.family().label + " replacement exists"), reason);
            assertTrue(reason.contains(expected.rationale()), reason);
            assertTrue(reason.contains("Documentation: " + id + "."), reason);
            assertFalse(reason.contains(" replacement: "), reason);
        }
    }

    private static void collectStatistics(Scenario scenario, List<Rendered> files, List<ItemResult> items) {
        Set<ItemKind> kinds = items.stream().map(r -> r.item().kind()).collect(Collectors.toSet());
        for (ItemKind kind : USAGE_KINDS) {
            String label = "excluded " + kind;
            Statistics.label(label).collect(kinds.contains(kind));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 20));
        }
        for (Family family : Family.values()) {
            String label = family.label + " diagnostics";
            Statistics.label(label).collect(items.stream()
                                                 .anyMatch(r -> FACILITY_BY_ID.get(r.item().id()).family() == family));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 40));
            for (String replacement : family.replacements) {
                String named = "names " + replacement;
                Statistics.label(named).collect(items.stream()
                                                     .anyMatch(r -> replacement.equals(r.diagnostic().replacement())));
                Statistics.label(named).coverage(c -> c.check(true).percentage(p -> p >= 5));
            }
        }
        Statistics.label("diagnostic without replacement")
                  .collect(items.stream().anyMatch(r -> r.diagnostic().replacement() == null));
        Statistics.label("diagnostic without replacement").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("files").collect(files.size());
        Statistics.label("files").coverage(c -> {
            c.check(1).percentage(p -> p >= 15);
            c.check(3).percentage(p -> p >= 15);
        });
        Statistics.label("CRLF file").collect(scenario.files().stream().anyMatch(f -> f.newline().equals("\r\n")));
        Statistics.label("CRLF file").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("qualified and imported files")
                  .collect(scenario.files().stream().anyMatch(FileSpec::qualified)
                           && scenario.files().stream().anyMatch(f -> !f.qualified()));
        Statistics.label("qualified and imported files").coverage(c -> c.check(true).percentage(p -> p >= 15));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenarios

    /** One class member using the legacy surface; {@code variant} selects its shape, {@code comment} a prefix. */
    record UsageSpec(Usage usage, int variant, int comment) {
    }

    /** One source file. */
    record FileSpec(boolean qualified, String newline, String indent, int headerLines, boolean reverseImports,
                    List<UsageSpec> usages) {
    }

    /** The manifest choice for one {@link #FACILITY} API, in catalog order. */
    record Choice(int replacement, int rationale, boolean migration) {
    }

    record Scenario(List<FileSpec> files, List<Choice> choices) {
    }

    /** The oracle's expectation of an API's entry. */
    record Expected(String replacement, String rationale, String migration) {
    }

    enum Usage {
        AWS4_SIGNER_CREATE, AWS4_SIGNER_NEW, REQUEST_SIGNER_OVERRIDE, ADVANCED_SIGNER_OPTION, SIGNER_IMPLEMENTATION,
        PRESIGNER_IMPLEMENTATION, SIGNER_OVERRIDE_UTILS, SIGNER_FIELD, RETRY_POLICY_NONE, RETRY_POLICY_BUILDER,
        RETRY_POLICY_SETTER, BACKOFF_STRATEGY, BACKOFF_IMPLEMENTATION, RETRY_CONDITION, LEGACY_RETRY_STRATEGY,
        AWS_RETRY_POLICY
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<UsageSpec> usage = Combinators.combine(Arbitraries.of(Usage.class),
                                                         Arbitraries.integers().between(0, 11),
                                                         Arbitraries.integers().between(0, 3))
                                                .as(UsageSpec::new);
        Arbitrary<FileSpec> file = Combinators.combine(Arbitraries.of(true, false),
                                                       Arbitraries.of("\n", "\r\n"),
                                                       Arbitraries.of("    ", "  ", "\t"),
                                                       Arbitraries.integers().between(0, 3),
                                                       Arbitraries.of(true, false),
                                                       usage.list().ofMinSize(1).ofMaxSize(5))
                                              .as(FileSpec::new);
        Arbitrary<Choice> choice = Combinators.combine(Arbitraries.integers().between(0, 5),
                                                       Arbitraries.integers().between(0, RATIONALES.size() - 1),
                                                       Arbitraries.of(true, false))
                                              .as(Choice::new);
        return Combinators.combine(file.list().ofMinSize(1).ofMaxSize(3), choice.list().ofSize(FACILITY.size()))
                          .as(Scenario::new);
    }

    /**
     * Each API's replacement: index {@code n} of its family's replacements, or none when {@code n} is past the end;
     * an API that is not a facility by name always keeps a replacement (the first).
     */
    private static Map<String, Expected> expectations(List<Choice> choices) {
        Map<String, Expected> out = new LinkedHashMap<>();
        for (int i = 0; i < FACILITY.size(); i++) {
            Api api = FACILITY.get(i);
            Choice c = choices.get(i);
            List<String> pool = api.family().replacements;
            String replacement = c.replacement() < pool.size() ? pool.get(c.replacement())
                                                               : api.named() ? null : pool.get(0);
            out.put(api.id(), new Expected(replacement, RATIONALES.get(c.rationale()), c.migration() ? MIGRATION : null));
        }
        return out;
    }

    private static MigrationManifest manifest(Map<String, Expected> choices) {
        List<ManifestEntry> entries = new ArrayList<>();
        choices.forEach((id, e) -> entries.add(new ManifestEntry(
            id, kind(id), "V2 surface " + id, "software.amazon.awssdk:sdk-core", null, true, Classification.EXCLUDED,
            List.of(), e.rationale(), List.of(), null, e.replacement(), MigrationKind.DIAGNOSTIC, e.migration(),
            "Deprecated in the V2 baseline.", List.of())));
        for (String id : PRESERVED) {
            entries.add(new ManifestEntry(id, kind(id), "V2 surface " + id, "software.amazon.awssdk:sdk-core", null,
                                          false, Classification.PRESERVED, List.of("source"), "Kept unchanged in V3.",
                                          List.of(), null, null, null, null, null, List.of()));
        }
        return MigrationManifest.of(BASELINE, entries);
    }

    private static EntryKind kind(String id) {
        return id.contains("#") ? EntryKind.PUBLIC_MEMBER : EntryKind.PUBLIC_TYPE;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rendering oracle

    /** An expected construct: its kind, manifest ID, location and construct text. */
    record Span(ItemKind kind, String id, SourceLocation location, String construct) {
        String key() {
            return ModernExclusionDiagnosticsPropertyTest.key(kind, id, location);
        }
    }

    record Rendered(String path, String text, List<Span> spans) {
    }

    private static String key(ItemKind kind, String id, SourceLocation l) {
        return kind + " " + id + " @ " + l.path() + "[" + l.startOffset() + "," + l.endOffset() + ") " + l.line() + ":"
               + l.column() + "-" + l.endLine() + ":" + l.endColumn();
    }

    /** A raw span: offsets into the text being written. */
    private record Raw(ItemKind kind, String id, int start, int end) {
    }

    /** Writes a class body and records the offsets of every V2 construct as it writes it. */
    private static final class Body {
        private final FileSpec spec;
        private final StringBuilder text = new StringBuilder();
        private final List<Raw> spans = new ArrayList<>();
        private final Set<String> imports = new TreeSet<>();
        private int names;

        Body(FileSpec spec) {
            this.spec = spec;
        }

        int at() {
            return text.length();
        }

        String name(String prefix) {
            return prefix + names++;
        }

        Body lit(String s) {
            text.append(s);
            return this;
        }

        Body nl() {
            text.append(spec.newline());
            return this;
        }

        Body ind(int depth) {
            text.append(spec.indent().repeat(depth));
            return this;
        }

        void expect(ItemKind kind, String id, int start) {
            spans.add(new Raw(kind, id, start, at()));
        }

        /**
         * The source name of a V2 type: simple and imported, or fully qualified. A nested type is written through
         * its enclosing type, which is a type reference of its own spanning the leading part.
         */
        Body type(String binary) {
            int start = at();
            String[] parts = binary.split("\\$");
            String top = parts[0];
            if (spec.qualified()) {
                text.append(top);
            } else {
                imports.add(top);
                text.append(top.substring(top.lastIndexOf('.') + 1));
            }
            expect(ItemKind.TYPE_REFERENCE, "api:" + top, start);
            String current = top;
            for (int i = 1; i < parts.length; i++) {
                text.append('.').append(parts[i]);
                current = current + "$" + parts[i];
                expect(ItemKind.TYPE_REFERENCE, "api:" + current, start);
            }
            return this;
        }

        /** A method invocation spans its name through its closing parenthesis. */
        Body call(String name, String id, Consumer<Body> arguments) {
            int start = at();
            text.append(name).append('(');
            arguments.accept(this);
            text.append(')');
            expect(ItemKind.METHOD_INVOCATION, id, start);
            return this;
        }

        Body staticCall(String owner, String name, String id, Consumer<Body> arguments) {
            return type(owner).lit(".").call(name, id, arguments);
        }

        /** A constructor call spans the whole {@code new} expression. */
        Body construct(String type, String id, String arguments) {
            int start = at();
            lit("new ").type(type).lit("(" + arguments + ")");
            expect(ItemKind.CONSTRUCTOR_CALL, id, start);
            return this;
        }

        /** A field access spans the qualified name. */
        Body field(String owner, String name, String id) {
            int start = at();
            type(owner).lit("." + name);
            expect(ItemKind.FIELD_ACCESS, id, start);
            return this;
        }

        /** Optional non-ASCII comment before a statement, so columns count UTF-16 units. */
        Body comment(UsageSpec usage) {
            return lit(switch (usage.comment()) {
                case 1 -> "/* \u00fcber */ ";
                case 2 -> "/* \uD83D\uDE00 */ ";
                default -> "";
            });
        }
    }

    private static final Consumer<Body> NO_ARGUMENTS = b -> { };

    private static Rendered render(int index, FileSpec spec) {
        Body body = new Body(spec);
        for (UsageSpec usage : spec.usages()) {
            usage(body, usage);
            body.nl();
        }
        String nl = spec.newline();
        String path = "src/main/java/com/example/p" + index + "/App" + index + ".java";
        StringBuilder text = new StringBuilder();
        List<Raw> raw = new ArrayList<>();
        for (int i = 0; i < spec.headerLines(); i++) {
            text.append("// Fixture header line ").append(i).append(i % 2 == 0 ? " \u00e9" : "").append(nl);
        }
        text.append("package com.example.p").append(index).append(';').append(nl).append(nl);
        List<String> imports = new ArrayList<>(body.imports);
        if (spec.reverseImports()) {
            imports = imports.reversed();
        }
        for (String type : imports) {
            int start = text.length();
            text.append("import ").append(type).append(';');
            raw.add(new Raw(ItemKind.IMPORT, "api:" + type, start, text.length()));
            text.append(nl);
        }
        text.append(nl).append("public class App").append(index).append(" {").append(nl);
        int shift = text.length();
        text.append(body.text).append('}').append(nl);
        for (Raw r : body.spans) {
            raw.add(new Raw(r.kind(), r.id(), r.start() + shift, r.end() + shift));
        }
        String content = text.toString();
        List<Span> spans = raw.stream()
                              .map(r -> new Span(r.kind(), r.id(), locate(path, content, r.start(), r.end()),
                                                 content.substring(r.start(), r.end()).strip().replaceAll("\\s+", " ")))
                              .toList();
        return new Rendered(path, content, spans);
    }

    /** The location of {@code [start, end)}, by scanning lines terminated by LF, CRLF or a lone CR. */
    private static SourceLocation locate(String path, String text, int start, int end) {
        int[] s = lineAndColumn(text, start);
        int[] e = lineAndColumn(text, end);
        return new SourceLocation(path, start, end, s[0], s[1], e[0], e[1]);
    }

    private static int[] lineAndColumn(String text, int offset) {
        int line = 1;
        int lineStart = 0;
        for (int i = 0; i < offset; i++) {
            char c = text.charAt(i);
            boolean terminator = c == '\n' || (c == '\r' && (i + 1 == text.length() || text.charAt(i + 1) != '\n'));
            if (terminator) {
                line++;
                lineStart = i + 1;
            }
        }
        return new int[] {line, offset - lineStart + 1};
    }

    private static void usage(Body b, UsageSpec u) {
        int v = u.variant();
        switch (u.usage()) {
            case AWS4_SIGNER_CREATE -> returning(b, u, "Object", x -> x.staticCall(AWS4_SIGNER, "create", AWS4_CREATE,
                                                                                    NO_ARGUMENTS));
            case AWS4_SIGNER_NEW -> returning(b, u, "Object", x -> x.construct(AWS4_SIGNER, AWS4_INIT, ""));
            case REQUEST_SIGNER_OVERRIDE -> {
                b.ind(1).lit("Object " + b.name("m") + "(").type(REQUEST_OVERRIDE_BUILDER).lit(" override) {").nl();
                b.ind(2).comment(u).lit("return override.").call("signer", REQUEST_SIGNER, x -> signer(x, v)).lit(";")
                 .nl();
                b.ind(1).lit("}").nl();
            }
            case ADVANCED_SIGNER_OPTION -> returning(b, u, "Object", x -> {
                if (v % 2 == 0) {
                    x.field(ADVANCED_OPTION, "SIGNER", ADVANCED_SIGNER);
                } else {
                    x.lit("java.util.Objects.requireNonNull(").field(ADVANCED_OPTION, "SIGNER", ADVANCED_SIGNER).lit(")");
                }
            });
            case SIGNER_IMPLEMENTATION -> implementation(b, u, SIGNER, SIGNER_SIGN, "String sign(String request)",
                                                         "request");
            case PRESIGNER_IMPLEMENTATION -> implementation(b, u, PRESIGNER, PRESIGNER_PRESIGN,
                                                            "String presign(String request)", "request + \"?signed\"");
            case SIGNER_OVERRIDE_UTILS -> {
                b.ind(1).lit("boolean " + b.name("m") + "(Object configuration) {").nl();
                b.ind(2).comment(u).lit("return ")
                 .staticCall(SIGNER_UTILS, "isSignerOverridden", SIGNER_OVERRIDDEN, x -> x.lit("configuration"))
                 .lit(";").nl();
                b.ind(1).lit("}").nl();
            }
            case SIGNER_FIELD -> {
                b.ind(1).lit("private ");
                if (v % 2 == 0) {
                    b.type(SIGNER);
                } else {
                    b.lit("java.util.List<").type(SIGNER).lit(">");
                }
                b.lit(" " + b.name("f") + ";").nl();
            }
            case RETRY_POLICY_NONE -> returning(b, u, "Object", x -> x.staticCall(RETRY_POLICY, "none",
                                                                                   RETRY_POLICY_NONE, NO_ARGUMENTS));
            case RETRY_POLICY_BUILDER -> {
                b.ind(1).lit("Object " + b.name("m") + "() {").nl();
                b.ind(2).type(RETRY_POLICY_BUILDER).lit(" builder = ")
                 .staticCall(RETRY_POLICY, "builder", RETRY_POLICY_BUILDER_METHOD, NO_ARGUMENTS).lit(";").nl();
                b.ind(2).comment(u).lit("return builder");
                boolean split = v % 2 == 1;
                if (split) {
                    b.nl().ind(4);
                }
                b.lit(".").call("numRetries", NUM_RETRIES, x -> x.lit(String.valueOf(v)));
                if (split) {
                    b.nl().ind(4);
                }
                b.lit(".").call("build", RETRY_POLICY_BUILD, NO_ARGUMENTS).lit(";").nl();
                b.ind(1).lit("}").nl();
            }
            case RETRY_POLICY_SETTER -> {
                b.ind(1).lit("void " + b.name("m") + "(").type(CLIENT_OVERRIDE_BUILDER).lit(" override) {").nl();
                b.ind(2).comment(u).lit("override.").call("retryPolicy", RETRY_POLICY_SETTER, x -> retryPolicy(x, v))
                 .lit(";").nl();
                b.ind(1).lit("}").nl();
            }
            case BACKOFF_STRATEGY -> {
                b.ind(1).lit("long " + b.name("m") + "() {").nl();
                b.ind(2).type(BACKOFF).lit(" strategy = ");
                if (v % 2 == 0) {
                    b.staticCall(FIXED_DELAY, "create", FIXED_DELAY_CREATE, x -> x.lit("100L"));
                } else {
                    b.construct(FIXED_DELAY, FIXED_DELAY_INIT, "100L");
                }
                b.lit(";").nl();
                b.ind(2).comment(u).lit("return strategy.").call("delayMillis", BACKOFF_DELAY, x -> x.lit("1")).lit(";")
                 .nl();
                b.ind(1).lit("}").nl();
            }
            case BACKOFF_IMPLEMENTATION -> implementation(b, u, BACKOFF, BACKOFF_DELAY, "long delayMillis(int attempt)",
                                                          "10L * attempt");
            case RETRY_CONDITION -> {
                if (v % 3 == 2) {
                    returning(b, u, "Object", x -> x.staticCall(RETRY_CONDITION, "none", CONDITION_NONE, NO_ARGUMENTS));
                } else {
                    implementation(b, u, RETRY_CONDITION, CONDITION_SHOULD_RETRY,
                                   "boolean shouldRetry(Object context)", "context == null");
                }
            }
            case LEGACY_RETRY_STRATEGY -> returning(b, u, "Object", x -> x.staticCall(LEGACY_STRATEGY, "builder",
                                                                                       LEGACY_BUILDER_METHOD, NO_ARGUMENTS)
                                                                           .lit(".")
                                                                           .call("build", LEGACY_BUILD, NO_ARGUMENTS));
            case AWS_RETRY_POLICY -> returning(b, u, "Object", x -> {
                if (v % 2 == 0) {
                    x.staticCall(AWS_RETRY_POLICY, "defaultRetryPolicy", AWS_DEFAULT_POLICY, NO_ARGUMENTS);
                } else {
                    x.staticCall(AWS_RETRY_POLICY, "defaultRetryCondition", AWS_DEFAULT_CONDITION, NO_ARGUMENTS);
                }
            });
            default -> throw new IllegalStateException(u.usage().name());
        }
    }

    /** {@code T mN() { return <expression>; }}. */
    private static void returning(Body b, UsageSpec u, String returnType, Consumer<Body> expression) {
        b.ind(1).lit(returnType + " " + b.name("m") + "() {").nl();
        b.ind(2).comment(u).lit("return ");
        expression.accept(b);
        b.lit(";").nl();
        b.ind(1).lit("}").nl();
    }

    /** A legacy {@code Signer} argument: none, a factory call or a constructor call. */
    private static void signer(Body b, int variant) {
        switch (variant % 3) {
            case 0 -> b.lit("null");
            case 1 -> b.staticCall(AWS4_SIGNER, "create", AWS4_CREATE, NO_ARGUMENTS);
            default -> b.construct(AWS4_SIGNER, AWS4_INIT, "");
        }
    }

    /** A legacy {@code RetryPolicy} argument. */
    private static void retryPolicy(Body b, int variant) {
        switch (variant % 4) {
            case 0 -> b.lit("null");
            case 1 -> b.staticCall(RETRY_POLICY, "none", RETRY_POLICY_NONE, NO_ARGUMENTS);
            case 2 -> b.staticCall(AWS_RETRY_POLICY, "defaultRetryPolicy", AWS_DEFAULT_POLICY, NO_ARGUMENTS);
            default -> b.staticCall(RETRY_POLICY, "builder", RETRY_POLICY_BUILDER_METHOD, NO_ARGUMENTS)
                        .lit(".").call("numRetries", NUM_RETRIES, x -> x.lit("2"))
                        .lit(".").call("build", RETRY_POLICY_BUILD, NO_ARGUMENTS);
        }
    }

    /**
     * An implementation of a legacy interface method, in a named nested class or an anonymous class, with or without
     * {@code @Override}. The override spans the whole declaration including its modifiers and annotations.
     */
    private static void implementation(Body b, UsageSpec u, String type, String overriddenId, String signature,
                                       String result) {
        boolean anonymous = u.variant() % 2 == 1;
        boolean annotated = (u.variant() / 2) % 2 == 0;
        int depth;
        if (anonymous) {
            b.ind(1).lit("Object " + b.name("m") + "() {").nl();
            b.ind(2).comment(u).lit("return new ").type(type).lit("() {").nl();
            depth = 3;
        } else {
            b.ind(1).lit("static final class " + b.name("C") + " implements ").type(type).lit(" {").nl();
            depth = 2;
        }
        b.ind(depth);
        int start = b.at();
        if (annotated) {
            b.lit("@Override").nl().ind(depth);
        }
        b.lit("public " + signature + " {").nl();
        b.ind(depth + 1).lit("return " + result + ";").nl();
        b.ind(depth).lit("}");
        b.expect(ItemKind.METHOD_OVERRIDE, overriddenId, start);
        b.nl();
        if (anonymous) {
            b.ind(2).lit("};").nl();
        }
        b.ind(1).lit("}").nl();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Stub compilation and cleanup

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
        int status = compiler.run(null, null, errors, args.toArray(new String[0]));
        if (status != 0) {
            throw new IllegalStateException("Stub compilation failed:\n" + errors.toString(StandardCharsets.UTF_8));
        }
        return classes;
    }

    private static void delete(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
