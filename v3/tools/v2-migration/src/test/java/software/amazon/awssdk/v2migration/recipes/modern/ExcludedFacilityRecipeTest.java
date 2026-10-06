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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.AWS4_SIGNER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.AWS4_SIGNER_CREATE;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.CRC32;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.HTTP_SIGNER_REPLACEMENT;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.OVERRIDE_BUILDER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.OVERRIDE_SIGNER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.RETRY_POLICY;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.RETRY_POLICY_NONE;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.RETRY_STRATEGY_REPLACEMENT;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.SIGNER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.V2;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.excluded;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.preserved;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.result;

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
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.Recipes;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/** Excluded legacy signer, presigner and retry facilities ({@code excluded.legacy-signer-retry}). */
class ExcludedFacilityRecipeTest {
    private static final String RETRY_RATIONALE = "Legacy retry policies are replaced by the Modern Retry strategy API.";
    private static final String SIGNER_RATIONALE = "Pre-SRA signer APIs are excluded; signing is selected by auth "
                                                   + "scheme and identity provider";
    private static final String APP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.auth.signer.Aws4Signer;",
        "import software.amazon.awssdk.core.RequestOverrideConfiguration;",
        "import software.amazon.awssdk.core.checksums.Algorithm;",
        "import software.amazon.awssdk.core.retry.RetryPolicy;",
        "",
        "public class App {",
        "    Object retry() {",
        "        return RetryPolicy.none();",
        "    }",
        "",
        "    Object sign(RequestOverrideConfiguration.Builder override) {",
        "        return override.signer(Aws4Signer.create());",
        "    }",
        "",
        "    Object checksum() {",
        "        return Algorithm.CRC32;",
        "    }",
        "}",
        "");

    @TempDir
    static Path stubDirectory;
    private static Path stubs;

    @TempDir
    Path project;

    @BeforeAll
    static void compileStubs() throws Exception {
        stubs = ModernFixtures.v2Stubs(stubDirectory);
    }

    /** The manifest of {@link #APP}, shaped like the checked-in legacy-* rules; the signer override has no replacement. */
    private static MigrationManifest manifest(String signerOverrideReplacement) {
        String retryMigration = "Replace the legacy retry policy with a Modern Retry strategy on the client builder.";
        String signerMigration = "Remove the legacy Signer usage; configure the auth scheme.";
        return ModernFixtures.manifest(
            excluded(EntryKind.typeId(RETRY_POLICY), RETRY_RATIONALE, RETRY_STRATEGY_REPLACEMENT, retryMigration),
            excluded(RETRY_POLICY_NONE, RETRY_RATIONALE, RETRY_STRATEGY_REPLACEMENT, retryMigration),
            excluded(EntryKind.typeId(AWS4_SIGNER), SIGNER_RATIONALE, HTTP_SIGNER_REPLACEMENT, signerMigration),
            excluded(AWS4_SIGNER_CREATE, SIGNER_RATIONALE, HTTP_SIGNER_REPLACEMENT, signerMigration),
            excluded(EntryKind.typeId(SIGNER), SIGNER_RATIONALE, HTTP_SIGNER_REPLACEMENT, signerMigration),
            excluded(OVERRIDE_SIGNER, SIGNER_RATIONALE, signerOverrideReplacement, signerMigration),
            preserved(EntryKind.typeId(V2 + "core.RequestOverrideConfiguration")),
            preserved(EntryKind.typeId(OVERRIDE_BUILDER)),
            excluded(EntryKind.typeId(CHECKSUM_ALGORITHM), "Deprecated sdk-core checksums are excluded.", null,
                     "Use the checksums module."),
            excluded(CRC32, "Deprecated sdk-core checksums are excluded.", null, "Use the checksums module."));
    }

    @Test
    void run_excludedRetryApi_isUnchangedAndNamesTheModernRetryReplacement() throws Exception {
        MigrationResult result = ModernFixtures.run(project, stubs, APP, manifest(null), ModernFixtures.recipes());

        assertTrue(result.changed().isEmpty());
        for (ItemResult item : List.of(result(result, ItemKind.IMPORT, EntryKind.typeId(RETRY_POLICY)),
                                       result(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_POLICY)),
                                       result(result, ItemKind.METHOD_INVOCATION, RETRY_POLICY_NONE))) {
            assertEquals(Disposition.UNRESOLVED, item.disposition(), item.toString());
            assertEquals("excluded.legacy-signer-retry", item.recipe());
            MigrationDiagnostic d = item.diagnostic();
            assertTrue(d.reason().contains("excluded legacy retry API")
                       && d.reason().contains("Modern Retry replacement: software.amazon.awssdk.retries.RetryStrategy "
                                              + "(" + RETRY_STRATEGY_REPLACEMENT + ")"), d.reason());
            assertEquals(RETRY_STRATEGY_REPLACEMENT, d.replacement());
            assertEquals(RETRY_RATIONALE, d.rationale());
            assertEquals(Classification.EXCLUDED, d.classification());
            assertEquals(item.item().id(), d.documentationId());
            assertTrue(d.message().endsWith(" Documentation: " + item.item().id() + "."), d.message());
        }
        MigrationDiagnostic call = result(result, ItemKind.METHOD_INVOCATION, RETRY_POLICY_NONE).diagnostic();
        assertEquals(APP_PATH + ":10:28", call.location().toString());
        assertEquals("none()", call.construct());
    }

    @Test
    void run_excludedSignerApis_namesTheModernAuthReplacement() throws Exception {
        MigrationResult result = ModernFixtures.run(project, stubs, APP,
                                                    manifest("api:software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions"),
                                                    ModernFixtures.recipes());

        for (ItemResult item : List.of(result(result, ItemKind.IMPORT, EntryKind.typeId(AWS4_SIGNER)),
                                       result(result, ItemKind.METHOD_INVOCATION, AWS4_SIGNER_CREATE))) {
            assertEquals(Disposition.UNRESOLVED, item.disposition());
            assertTrue(item.diagnostic().reason().contains("Modern Auth replacement: "
                                                           + "software.amazon.awssdk.auth.api.HttpSigner"),
                       item.diagnostic().reason());
        }
        // The request override's signer member is a signing facility by name, though its owner is a builder.
        ItemResult override = result(result, ItemKind.METHOD_INVOCATION, OVERRIDE_SIGNER);
        assertEquals("excluded.legacy-signer-retry", override.recipe());
        assertTrue(override.diagnostic().reason().contains(
            "Modern Auth replacement: software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions"), override.diagnostic().reason());
    }

    @Test
    void run_excludedFacilityWithoutReplacement_haltsWithRationaleAndDocumentationId() throws Exception {
        MigrationResult result = ModernFixtures.run(project, stubs, APP, manifest(null), ModernFixtures.recipes());

        ItemResult override = result(result, ItemKind.METHOD_INVOCATION, OVERRIDE_SIGNER);
        assertEquals(Disposition.UNRESOLVED, override.disposition());
        MigrationDiagnostic d = override.diagnostic();
        assertEquals("No safe Modern Auth replacement exists for this excluded legacy signer API, so the use is left "
                     + "unchanged and its migration halts. Exclusion rationale: " + SIGNER_RATIONALE + ". "
                     + "Documentation: " + OVERRIDE_SIGNER + ".",
                     d.reason().substring(d.reason().indexOf("No safe")));
        assertNull(d.replacement());
        assertEquals(OVERRIDE_SIGNER, d.documentationId());
        assertEquals("signer(Aws4Signer.create())", d.construct());
        assertEquals(APP_PATH + ":14:25", d.location().toString());

        // The machine-readable report carries the documentation identifier of every diagnostic.
        ObjectNode report = Node.parse(result.reportJson(false)).expectObjectNode();
        List<String> documentationIds = new ArrayList<>();
        for (ObjectNode diagnostic : report.expectArrayMember("diagnostics").getElementsAs(ObjectNode.class)) {
            Node id = diagnostic.expectMember("documentationId");
            documentationIds.add(id.isNullNode() ? null : id.expectStringNode().getValue());
        }
        assertTrue(documentationIds.contains(OVERRIDE_SIGNER), documentationIds.toString());
    }

    @Test
    void run_anotherRecipeTransformingAnExcludedFacility_isOverruledAndTheSourceUnchanged() throws Exception {
        MigrationRecipe hostile = new MigrationRecipe() {
            @Override
            public String id() {
                return "test.rewrite-everything";
            }

            @Override
            public String description() {
                return "Rewrites every method invocation.";
            }

            @Override
            public RecipeOutcome visit(RecipeContext c) {
                return c.item().kind() == ItemKind.METHOD_INVOCATION
                       ? RecipeOutcome.transform("Rewrite.", List.of(TextEdit.replace(c.item().location(), "x()")))
                       : RecipeOutcome.notApplicable();
            }
        };
        List<MigrationRecipe> recipes = new ArrayList<>(ModernFixtures.recipes());
        recipes.add(hostile);
        MigrationResult result = ModernFixtures.run(project, stubs, APP, manifest(null), recipes);

        for (String id : List.of(RETRY_POLICY_NONE, AWS4_SIGNER_CREATE, OVERRIDE_SIGNER)) {
            ItemResult item = result(result, ItemKind.METHOD_INVOCATION, id);
            assertEquals(Disposition.UNRESOLVED, item.disposition(), id);
            assertTrue(item.diagnostic().reason().contains("excluded.legacy-signer-retry"), item.diagnostic().reason());
        }
        String migrated = result.changed().getOrDefault(APP_PATH, APP);
        assertTrue(migrated.contains("return RetryPolicy.none();"), migrated);
        assertTrue(migrated.contains("return override.signer(Aws4Signer.create());"), migrated);
    }

    @Test
    void run_excludedApiOutsideTheSigningAndRetryFacilities_keepsTheEngineDiagnostic() throws Exception {
        MigrationResult result = ModernFixtures.run(project, stubs, APP, manifest(null), ModernFixtures.recipes());

        ItemResult field = result(result, ItemKind.FIELD_ACCESS, CRC32);
        assertEquals(Disposition.UNRESOLVED, field.disposition());
        assertNull(field.recipe());
        assertTrue(field.diagnostic().reason().startsWith("EXCLUDED from V3 by the Parity Manifest"),
                   field.diagnostic().reason());
        assertEquals(CRC32, field.diagnostic().documentationId());
        assertEquals("Deprecated sdk-core checksums are excluded.", field.diagnostic().rationale());
    }

    @Test
    void discover_pluginClassLoader_registersTheModernAuthAndRetryRecipes() {
        List<String> ids = Recipes.discover(ExcludedFacilityRecipe.class.getClassLoader()).stream()
                                  .map(MigrationRecipe::id).toList();
        assertTrue(ids.containsAll(List.of("auth.modern-type-package", "excluded.legacy-signer-retry",
                                           "retry.modern-type-package")), ids.toString());
    }
}
