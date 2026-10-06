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

package software.amazon.awssdk.v2migration.recipe.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.CANDIDATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.allRecipes;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.assertDecided;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.excluded;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.manifest;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migrateSource;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migrated;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.only;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.preserved;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.renamed;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.BUILD;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.BUILDER_OVERRIDE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CLIENT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CLIENT_BUILDER_FACTORY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CLIENT_OVERRIDE_BUILDER;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.DUALSTACK;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.LEGACY_FLAG;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.MAX_ATTEMPTS;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OLD_TIMEOUT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OPTION;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.RETRY_STRATEGY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.RETRY_STRATEGY_CONSUMER;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.RETRY_STRATEGY_SETTER;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.TIMEOUT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.V3_RETRY_STRATEGY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.V3_RETRY_STRATEGY_SETTER;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationDiagnostic;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/**
 * {@link UnsupportedOptionRecipe} (Requirement 9.10) and {@link ReplacedSignatureTypeRecipe}, including the Modern
 * Retry setter of the client override configuration together with every registered recipe family.
 */
class ConfigurationRecipesTest {
    private static final String UNSUPPORTED = "configuration.unsupported-options";
    private static final String SIGNATURES = "members.replaced-signature-types";
    private static final String LEGACY_MODE = EntryKind.configurationId(EntryKind.SYSTEM_PROPERTY_SOURCE,
                                                                        "demo.legacy.mode");

    private static final String OPTIONS_APP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.core.client.config.DemoOption;",
        "import software.amazon.awssdk.services.demo.DemoClient;",
        "",
        "class App {",
        "    DemoOption flag = DemoOption.LEGACY_FLAG;",
        "    DemoClient client = DemoClient.builder().dualstack(true).build();",
        "    String mode = System.getProperty(\"demo.legacy.mode\");",
        "    DemoOption timeout = DemoOption.OLD_TIMEOUT;",
        "}",
        "");

    private static final String RETRY_APP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.retries.api.RetryStrategy;",
        "import software.amazon.awssdk.services.demo.DemoClient;",
        "",
        "class App {",
        "    DemoClient client(RetryStrategy strategy) {",
        "        return DemoClient.builder()",
        "                         .overrideConfiguration(o -> o.retryStrategy(strategy))",
        "                         .build();",
        "    }",
        "",
        "    DemoClient configured() {",
        "        return DemoClient.builder()",
        "                         .overrideConfiguration(o -> o.retryStrategy(b -> b.maxAttempts(3)))",
        "                         .build();",
        "    }",
        "}",
        "");

    private static Path v2;

    @TempDir
    Path project;

    @BeforeAll
    static void compileStubs(@TempDir Path stubs) throws Exception {
        v2 = CoreStubs.compileV2(stubs);
    }

    @Test
    void visit_optionsWithoutV3Behavior_areUnchangedAndReportedWithNameDispositionAndLocation() throws Exception {
        MigrationManifest manifest = manifest(
            CANDIDATE, preserved(EntryKind.typeId(CLIENT)), preserved(EntryKind.typeId(OPTION)),
            preserved(CLIENT_BUILDER_FACTORY), preserved(BUILD),
            excluded(LEGACY_FLAG, null, "Delete the flag."), excluded(DUALSTACK, null, null),
            excluded(LEGACY_MODE, null, null), excluded(OLD_TIMEOUT, TIMEOUT, "Use TIMEOUT."));

        MigrationResult result = migrateSource(project, OPTIONS_APP, v2, manifest);

        assertNull(result.changed().get(APP_PATH));
        ItemResult flag = only(result, ItemKind.FIELD_ACCESS, LEGACY_FLAG);
        assertUnsupported(flag, "LEGACY_FLAG", 7);
        assertTrue(flag.reason().contains("V3 rejects client construction when it is supplied. Remove it; Delete the "
                                          + "flag."), flag.reason());
        ItemResult dualstack = only(result, ItemKind.METHOD_INVOCATION, DUALSTACK);
        assertUnsupported(dualstack, "dualstack", 8);
        ItemResult mode = only(result, ItemKind.CONFIGURATION_KEY, LEGACY_MODE);
        assertUnsupported(mode, "demo.legacy.mode", 9);
        assertTrue(mode.reason().contains("V3 does not read this setting"), mode.reason());

        // An excluded option with a replacement is left to the recipe family of the replacement.
        ItemResult replaced = only(result, ItemKind.FIELD_ACCESS, OLD_TIMEOUT);
        assertDecided(replaced, Disposition.UNRESOLVED, null);
        assertEquals(TIMEOUT, replaced.diagnostic().replacement());
    }

    @Test
    void visit_preservedSetterWithRenamedSignatureType_isUnresolvedWhileEveryOtherRecipeRuns() throws Exception {
        MigrationResult result = migrateSource(project, RETRY_APP, v2, retryManifest(preserved(RETRY_STRATEGY_SETTER)),
                                               allRecipes());

        // The Modern Retry recipe moves the import; the setter call and its lambda are left byte-for-byte unchanged.
        assertEquals(RETRY_APP.replace("import " + RETRY_STRATEGY + ";", "import " + V3_RETRY_STRATEGY + ";"),
                     migrated(result, APP_PATH));
        ItemResult setter = only(result, ItemKind.METHOD_INVOCATION, RETRY_STRATEGY_SETTER);
        assertDecided(setter, Disposition.UNRESOLVED, SIGNATURES);
        assertTrue(setter.reason().contains("classifies " + CLIENT_OVERRIDE_BUILDER.replace('$', '.')
                                            + "#retryStrategy as PRESERVED, but its signature uses " + RETRY_STRATEGY
                                            + ", which the manifest replaces with " + V3_RETRY_STRATEGY),
                   setter.reason());
        assertEquals(9, setter.diagnostic().location().line());

        // A type argument of the generic signature counts too.
        ItemResult consumer = only(result, ItemKind.METHOD_INVOCATION, RETRY_STRATEGY_CONSUMER);
        assertDecided(consumer, Disposition.UNRESOLVED, SIGNATURES);
        assertTrue(consumer.reason().contains(RETRY_STRATEGY + ".Builder, which the manifest replaces with "
                                              + V3_RETRY_STRATEGY + ".Builder"), consumer.reason());

        // Members whose signatures use only preserved types are left to the manifest.
        List<ItemResult> overrides = CoreFixtures.all(result, ItemKind.METHOD_INVOCATION, BUILDER_OVERRIDE);
        assertEquals(2, overrides.size(), overrides::toString);
        overrides.forEach(o -> assertDecided(o, Disposition.PRESERVED, null));
    }

    @Test
    void visit_setterAdaptedWithTheRenamedSignature_isPreserved() throws Exception {
        MigrationManifest manifest = retryManifest(renamed(RETRY_STRATEGY_SETTER, V3_RETRY_STRATEGY_SETTER));

        MigrationResult result = migrateSource(project, RETRY_APP, v2, manifest, allRecipes());

        ItemResult setter = only(result, ItemKind.METHOD_INVOCATION, RETRY_STRATEGY_SETTER);
        assertDecided(setter, Disposition.PRESERVED, "configuration.renamed-members");
        assertTrue(setter.reason().contains("only types that the Parity Manifest renames change around it (its "
                                            + "signature)"), setter.reason());
    }

    private static void assertUnsupported(ItemResult result, String option, int line) {
        assertDecided(result, Disposition.UNRESOLVED, UNSUPPORTED);
        assertTrue(result.reason().contains("The V2 configuration option " + option + " (disposition EXCLUDED) has no "
                                            + "V3 behavior"), result.reason());
        MigrationDiagnostic diagnostic = result.diagnostic();
        assertEquals(APP_PATH, diagnostic.location().path());
        assertEquals(line, diagnostic.location().line());
        assertEquals(Classification.EXCLUDED, diagnostic.classification());
        assertEquals(result.item().id(), diagnostic.entryId());
    }

    /** The {@link #RETRY_APP} entries, with {@code setter} for the override configuration's strategy setter. */
    private static MigrationManifest retryManifest(ManifestEntry setter) {
        List<ManifestEntry> entries = new ArrayList<>(List.of(
            preserved(EntryKind.typeId(CLIENT)), preserved(CLIENT_BUILDER_FACTORY), preserved(BUILD),
            preserved(BUILDER_OVERRIDE), preserved(EntryKind.typeId(CLIENT_OVERRIDE_BUILDER)),
            preserved(RETRY_STRATEGY_CONSUMER), preserved(MAX_ATTEMPTS),
            renamed(EntryKind.typeId(RETRY_STRATEGY), EntryKind.typeId(V3_RETRY_STRATEGY)),
            renamed(EntryKind.typeId(RETRY_STRATEGY + "$Builder"), EntryKind.typeId(V3_RETRY_STRATEGY + "$Builder"))));
        entries.add(setter);
        return manifest(CANDIDATE, entries.toArray(ManifestEntry[]::new));
    }
}
