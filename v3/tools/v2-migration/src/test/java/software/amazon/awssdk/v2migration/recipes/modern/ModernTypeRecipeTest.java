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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.HTTP_SIGNER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.MAX_ATTEMPTS;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.RETRY_STRATEGY;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.RETRY_STRATEGY_BUILDER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.RETRY_TOKEN;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.V3_HTTP_SIGNER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.V3_RETRY_STRATEGY;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.V3_RETRY_STRATEGY_BUILDER;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.V3_RETRY_TOKEN;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.adapted;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.manifest;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.moved;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.movedMember;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.result;
import static software.amazon.awssdk.v2migration.recipes.modern.ModernFixtures.results;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/** The Modern Auth and Modern Retry package moves ({@code auth.modern-type-package}, {@code retry.modern-type-package}). */
class ModernTypeRecipeTest {
    private static final String TYPES_APP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;",
        "import software.amazon.awssdk.retries.api.RetryStrategy;",
        "",
        "public class App {",
        "    RetryStrategy strategy;",
        "    RetryStrategy.Builder<?, ?> builder;",
        "    HttpSigner<String> signer;",
        "    software.amazon.awssdk.retries.api.RetryToken token;",
        "}",
        "");

    @TempDir
    static Path stubDirectory;
    private static Path v2;
    private static Path v3;

    @TempDir
    Path project;

    @BeforeAll
    static void compileStubs() throws Exception {
        v2 = ModernFixtures.v2Stubs(stubDirectory.resolve("v2"));
        v3 = ModernFixtures.v3Stubs(stubDirectory.resolve("v3"));
    }

    private static MigrationManifest movedTypes() {
        return manifest(moved(RETRY_STRATEGY, V3_RETRY_STRATEGY), moved(RETRY_STRATEGY_BUILDER, V3_RETRY_STRATEGY_BUILDER),
                        moved(RETRY_TOKEN, V3_RETRY_TOKEN), moved(HTTP_SIGNER, V3_HTTP_SIGNER),
                        movedMember(MAX_ATTEMPTS, V3_RETRY_STRATEGY));
    }

    @Test
    void run_modernTypes_movesImportsAndQualifiedNamesAndTheResultCompilesAgainstV3() throws Exception {
        MigrationResult result = ModernFixtures.run(project, v2, TYPES_APP, movedTypes(), ModernFixtures.recipes());

        String expected = TYPES_APP.replace("import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;",
                                            "import software.amazon.awssdk.auth.api.HttpSigner;")
                                   .replace("import software.amazon.awssdk.retries.api.RetryStrategy;",
                                            "import software.amazon.awssdk.retries.RetryStrategy;")
                                   .replace("software.amazon.awssdk.retries.api.RetryToken token",
                                            "software.amazon.awssdk.retries.RetryToken token");
        assertEquals(expected, result.changed().get(APP_PATH));
        assertFalse(result.hasUnresolved(), result.diagnostics().toString());
        assertEquals("", ModernFixtures.compileErrors(project.resolve("v3-check"), expected, v3));

        ItemResult retryImport = result(result, ItemKind.IMPORT, EntryKind.typeId(RETRY_STRATEGY));
        assertEquals(Disposition.TRANSFORMED, retryImport.disposition());
        assertEquals("retry.modern-type-package", retryImport.recipe());
        assertTrue(retryImport.reason().contains("api:" + V3_RETRY_STRATEGY), retryImport.reason());
        assertEquals("auth.modern-type-package",
                     result(result, ItemKind.IMPORT, EntryKind.typeId(HTTP_SIGNER)).recipe());
        assertEquals(Disposition.TRANSFORMED,
                     result(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_TOKEN)).disposition());
        // Simple names resolve through the moved import; a nested type follows its moved enclosing type.
        for (ItemResult simple : results(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_STRATEGY))) {
            assertEquals(Disposition.PRESERVED, simple.disposition(), simple.toString());
        }
        ItemResult nested = result(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_STRATEGY_BUILDER));
        assertEquals(Disposition.PRESERVED, nested.disposition());
        assertTrue(nested.reason().contains("qualified by " + RETRY_STRATEGY), nested.reason());
    }

    @Test
    void run_simpleNameThroughOnDemandImport_isLeftUnchangedWithADiagnostic() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.retries.api.*;",
            "",
            "public class App {",
            "    RetryStrategy strategy;",
            "}",
            "");
        MigrationResult result = ModernFixtures.run(project, v2, app, movedTypes(), ModernFixtures.recipes());

        assertTrue(result.changed().isEmpty());
        ItemResult reference = result(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(RETRY_STRATEGY));
        assertEquals(Disposition.UNRESOLVED, reference.disposition());
        assertEquals(APP_PATH + ":6:5", reference.diagnostic().location().toString());
        assertTrue(reference.diagnostic().reason().contains("without a single-type import"),
                   reference.diagnostic().reason());
        assertEquals("api:" + V3_RETRY_STRATEGY, reference.diagnostic().replacement());
    }

    @Test
    void run_memberOfAMovedType_isADiagnosticNotARewrite() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.retries.api.RetryStrategy;",
            "",
            "public class App {",
            "    int attempts(RetryStrategy strategy) {",
            "        return strategy.maxAttempts();",
            "    }",
            "}",
            "");
        MigrationResult result = ModernFixtures.run(project, v2, app, movedTypes(), ModernFixtures.recipes());

        ItemResult call = result(result, ItemKind.METHOD_INVOCATION, MAX_ATTEMPTS);
        assertEquals(Disposition.UNRESOLVED, call.disposition());
        assertNull(call.recipe());
        assertEquals("api:" + V3_RETRY_STRATEGY, call.diagnostic().replacement());
        assertEquals(MAX_ATTEMPTS, call.diagnostic().documentationId());
        assertTrue(result.changed().get(APP_PATH).contains("return strategy.maxAttempts();"));
        assertEquals(Disposition.TRANSFORMED,
                     result(result, ItemKind.IMPORT, EntryKind.typeId(RETRY_STRATEGY)).disposition());
    }

    @Test
    void run_entriesThatAreNotRecipeBackedPackageMoves_keepTheManifestDisposition() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.retries.api.RetryStrategy;",
            "import software.amazon.awssdk.retries.api.RetryToken;",
            "",
            "public class App {",
            "}",
            "");
        // A diagnostic-only migration, and a recipe-backed rename that is not a package move (the name changes).
        MigrationManifest manifest = manifest(
            adapted(EntryKind.typeId(RETRY_STRATEGY), MigrationKind.DIAGNOSTIC, "api:" + V3_RETRY_STRATEGY),
            adapted(EntryKind.typeId(RETRY_TOKEN), MigrationKind.RECIPE, "api:" + V3_RETRY_STRATEGY));
        MigrationResult result = ModernFixtures.run(project, v2, app, manifest, ModernFixtures.recipes());

        assertTrue(result.changed().isEmpty());
        for (String type : List.of(RETRY_STRATEGY, RETRY_TOKEN)) {
            ItemResult imported = result(result, ItemKind.IMPORT, EntryKind.typeId(type));
            assertEquals(Disposition.UNRESOLVED, imported.disposition());
            assertNull(imported.recipe(), imported.toString());
        }
    }
}
