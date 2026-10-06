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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.CANDIDATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.all;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.assertDecided;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.manifest;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migrateSource;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migrated;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.only;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.preserved;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.renamed;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.BUILD;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.BUILDER_CREDENTIALS;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.BUILDER_REGION;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CLIENT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CLIENT_BUILDER;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CLIENT_BUILDER_FACTORY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.CREDENTIALS_CREATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.DESCRIBE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.ENDPOINT_URL;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.IDENTITY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.IDENTITY_CREATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.LEGACY_CREDENTIALS;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.LEGACY_ENDPOINT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.NEW_HOOK;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.NEW_SHAPE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.NEW_SHAPE_CREATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.NEW_SHAPE_NAME;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OLD_DESCRIBE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OLD_HOOK;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OLD_SHAPE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OLD_TIMEOUT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OPTION;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OVERRIDE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OVERRIDE_BUILD;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.OVERRIDE_FACTORY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.PUT_HEADER;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.PUT_LEGACY_HEADER;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.SHAPE_CREATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.SHAPE_NAME;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.TIMEOUT;
import static software.amazon.awssdk.v2migration.recipe.core.CoreStubs.V3_BUILDER_CREDENTIALS;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/**
 * {@link RenamedTypeRecipe}, the {@link RenamedMemberRecipe} family and {@link RenamedConfigurationKeyRecipe}: renamed
 * model, client, builder, identity, configuration and request override APIs, and the source they must leave alone.
 */
class SourceRecipesTest {
    private static final String TYPES = "imports.renamed-types";
    private static final String BUILDERS = "builders.renamed-members";
    private static final String CLIENTS = "clients.renamed-members";
    private static final String CONFIGURATION = "configuration.renamed-members";
    private static final String REQUEST_OVERRIDES = "configuration.request-overrides";
    private static final String KEYS = "configuration.renamed-keys";

    private static final String OLD_REGION_ENV = EntryKind.configurationId(EntryKind.ENVIRONMENT_SOURCE,
                                                                           "DEMO_OLD_REGION");
    private static final String REGION_ENV = EntryKind.configurationId(EntryKind.ENVIRONMENT_SOURCE, "DEMO_REGION");

    private static final String APP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.auth.credentials.LegacyCredentials;",
        "import software.amazon.awssdk.core.RequestOverrideConfiguration;",
        "import software.amazon.awssdk.core.client.config.DemoOption;",
        "import software.amazon.awssdk.services.demo.DemoClient;",
        "import software.amazon.awssdk.services.demo.model.OldShape;",
        "",
        "public class App {",
        "    public static String run() {",
        "        OldShape shape = OldShape.create();",
        "        DemoClient client = DemoClient.builder()",
        "                                      .legacyEndpoint(\"https://localhost\")",
        "                                      .region(\"us-west-2\")",
        "                                      .credentials(LegacyCredentials.create())",
        "                                      .build();",
        "        DemoOption option = DemoOption.OLD_TIMEOUT;",
        "        RequestOverrideConfiguration override = RequestOverrideConfiguration.builder()",
        "                                                                            .putLegacyHeader(\"x-tenant\", \"a\")",
        "                                                                            .build();",
        "        software.amazon.awssdk.services.demo.model.OldShape qualified = shape;",
        "        return client.oldDescribe(shape.name()) + option + override + qualified",
        "               + System.getenv(\"DEMO_OLD_REGION\");",
        "    }",
        "}",
        "");

    private static Path v2;
    private static Path v3;

    @TempDir
    Path project;

    @BeforeAll
    static void compileStubs(@TempDir Path stubs) throws Exception {
        v2 = CoreStubs.compileV2(stubs.resolve("v2"));
        v3 = CoreStubs.compileV3(stubs.resolve("v3"));
    }

    @Test
    void visit_manifestRenames_rewriteEveryUseAndTheResultCompilesAgainstV3() throws Exception {
        MigrationResult result = migrateSource(project, APP, v2, renames());

        assertEquals(List.of(), result.diagnostics(), () -> result.diagnostics().toString());
        String expected = APP.replace("import software.amazon.awssdk.auth.credentials.LegacyCredentials;",
                                      "import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;")
                             .replace("LegacyCredentials.create()", "AwsCredentialsIdentity.create()")
                             .replace("OldShape", "NewShape")
                             .replace(".legacyEndpoint(", ".endpointUrl(")
                             .replace("DemoOption.OLD_TIMEOUT", "DemoOption.TIMEOUT")
                             .replace(".putLegacyHeader(", ".putHeader(")
                             .replace(".oldDescribe(", ".describe(")
                             .replace("\"DEMO_OLD_REGION\"", "\"DEMO_REGION\"");
        assertEquals(expected, migrated(result, APP_PATH));
        CoreFixtures.compile(project.resolve("compile"), Map.of("com.example.App", expected), List.of(v3));

        assertDecided(only(result, ItemKind.IMPORT, EntryKind.typeId(OLD_SHAPE)), Disposition.TRANSFORMED, TYPES);
        assertDecided(only(result, ItemKind.IMPORT, EntryKind.typeId(LEGACY_CREDENTIALS)), Disposition.TRANSFORMED,
                      TYPES);
        List<ItemResult> shapes = all(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE));
        assertEquals(3, shapes.size(), shapes::toString);
        shapes.forEach(s -> assertEquals(TYPES, s.recipe()));
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, LEGACY_ENDPOINT), Disposition.TRANSFORMED, BUILDERS);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, BUILDER_CREDENTIALS), Disposition.PRESERVED, BUILDERS);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, OLD_DESCRIBE), Disposition.TRANSFORMED, CLIENTS);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, SHAPE_CREATE), Disposition.PRESERVED, CLIENTS);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, SHAPE_NAME), Disposition.PRESERVED, CLIENTS);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, CREDENTIALS_CREATE), Disposition.PRESERVED,
                      CONFIGURATION);
        assertDecided(only(result, ItemKind.FIELD_ACCESS, OLD_TIMEOUT), Disposition.TRANSFORMED, CONFIGURATION);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, PUT_LEGACY_HEADER), Disposition.TRANSFORMED,
                      REQUEST_OVERRIDES);
        assertDecided(only(result, ItemKind.CONFIGURATION_KEY, OLD_REGION_ENV), Disposition.TRANSFORMED, KEYS);
        assertDecided(only(result, ItemKind.METHOD_INVOCATION, BUILDER_REGION), Disposition.PRESERVED, null);
    }

    @Test
    void visit_sameProjectTwice_producesTheSameReport() throws Exception {
        MigrationResult first = migrateSource(project, APP, v2, renames());
        MigrationResult second = migrateSource(project, APP, v2, renames());

        assertEquals(first.changed(), second.changed());
        assertEquals(first.reportJson(false), second.reportJson(false));
    }

    @Test
    void visit_onDemandImport_leavesEveryUseOfTheRenamedTypeUnchanged() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.services.demo.model.*;",
            "",
            "class App {",
            "    OldShape shape() {",
            "        return OldShape.create();",
            "    }",
            "}",
            "");

        MigrationResult result = migrateSource(project, app, v2, renames());

        assertNull(result.changed().get(APP_PATH));
        List<ItemResult> shapes = all(result, ItemKind.TYPE_REFERENCE, EntryKind.typeId(OLD_SHAPE));
        assertEquals(2, shapes.size(), shapes::toString);
        for (ItemResult shape : shapes) {
            assertDecided(shape, Disposition.UNRESOLVED, TYPES);
            assertTrue(shape.reason().contains("without a single-type import"), shape.reason());
            assertEquals("OldShape", shape.diagnostic().construct());
        }
    }

    @Test
    void visit_newSimpleNameAlreadyUsed_leavesTheFileUnchanged() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.services.demo.model.OldShape;",
            "",
            "class App {",
            "    static class NewShape {",
            "    }",
            "",
            "    OldShape shape() {",
            "        return null;",
            "    }",
            "}",
            "");

        MigrationResult result = migrateSource(project, app, v2, renames());

        assertNull(result.changed().get(APP_PATH));
        ItemResult imported = only(result, ItemKind.IMPORT, EntryKind.typeId(OLD_SHAPE));
        assertDecided(imported, Disposition.UNRESOLVED, TYPES);
        assertTrue(imported.reason().contains("already uses the new simple name NewShape"), imported.reason());
        assertEquals("src/main/java/com/example/App.java:3:1", imported.diagnostic().location().toString());
    }

    @Test
    void visit_staticImportOfRenamedMember_isUnresolvedWithItsUses() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import static software.amazon.awssdk.core.client.config.DemoOption.OLD_TIMEOUT;",
            "",
            "import software.amazon.awssdk.core.client.config.DemoOption;",
            "",
            "class App {",
            "    DemoOption option = OLD_TIMEOUT;",
            "}",
            "");

        MigrationResult result = migrateSource(project, app, v2, renames());

        assertNull(result.changed().get(APP_PATH));
        ItemResult staticImport = only(result, ItemKind.STATIC_IMPORT, OLD_TIMEOUT);
        assertDecided(staticImport, Disposition.UNRESOLVED, CONFIGURATION);
        assertTrue(staticImport.reason().contains(OPTION + "#TIMEOUT"), staticImport.reason());
        ItemResult use = only(result, ItemKind.FIELD_ACCESS, OLD_TIMEOUT);
        assertDecided(use, Disposition.UNRESOLVED, CONFIGURATION);
        assertTrue(use.reason().contains("static import"), use.reason());
    }

    @Test
    void visit_keyMovedToAnotherSource_isUnresolved() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "class App {",
            "    String region = System.getenv(\"DEMO_OLD_REGION\");",
            "}",
            "");
        MigrationManifest manifest = manifest(CANDIDATE, renamed(OLD_REGION_ENV, EntryKind.configurationId(
            EntryKind.SYSTEM_PROPERTY_SOURCE, "demo.region")));

        MigrationResult result = migrateSource(project, app, v2, manifest);

        assertNull(result.changed().get(APP_PATH));
        ItemResult key = only(result, ItemKind.CONFIGURATION_KEY, OLD_REGION_ENV);
        assertDecided(key, Disposition.UNRESOLVED, KEYS);
        assertTrue(key.reason().contains("config:sysprop:demo.region, from a different source"), key.reason());
        assertEquals("config:sysprop:demo.region", key.diagnostic().replacement());
    }

    @Test
    void visit_replacementWithAnotherSignature_isLeftToTheManifestDiagnostic() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.services.demo.DemoClient;",
            "",
            "class App {",
            "    String describe(DemoClient client) {",
            "        return client.oldDescribe(\"id\");",
            "    }",
            "}",
            "");
        String otherSignature = "api:" + CLIENT + "#describe(I)Ljava/lang/String;";
        MigrationManifest manifest = manifest(CANDIDATE, preserved(EntryKind.typeId(CLIENT)),
                                              renamed(OLD_DESCRIBE, otherSignature));

        MigrationResult result = migrateSource(project, app, v2, manifest);

        assertNull(result.changed().get(APP_PATH));
        ItemResult call = only(result, ItemKind.METHOD_INVOCATION, OLD_DESCRIBE);
        assertDecided(call, Disposition.UNRESOLVED, null);
        assertEquals(otherSignature, call.diagnostic().replacement());
    }

    @Test
    void visit_renamedTypeOfAnotherRecipeFamily_isNotAnsweredByTheCoreRecipes() throws Exception {
        String app = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.core.interceptor.OldHook;",
            "",
            "class App {",
            "    OldHook hook;",
            "}",
            "");
        MigrationManifest manifest = manifest(CANDIDATE, renamed(EntryKind.typeId(OLD_HOOK),
                                                                 EntryKind.typeId(NEW_HOOK)));

        MigrationResult result = migrateSource(project, app, v2, manifest);

        assertNull(result.changed().get(APP_PATH));
        assertFalse(result.items().isEmpty());
        result.items().forEach(i -> assertDecided(i, Disposition.UNRESOLVED, null));
    }

    @Test
    void scope_partitionsTheV2SurfaceBetweenTheCoreRecipes() {
        assertEquals(Optional.of(Scope.REQUEST_OVERRIDES), Scope.of(CoreStubs.OVERRIDE_BUILDER));
        assertEquals(Optional.of(Scope.REQUEST_OVERRIDES),
                     Scope.of("software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration"));
        assertEquals(Optional.of(Scope.BUILDERS), Scope.of(CLIENT_BUILDER));
        assertEquals(Optional.of(Scope.BUILDERS), Scope.of(OLD_SHAPE + "$Builder"));
        assertEquals(Optional.of(Scope.CLIENTS), Scope.of(CLIENT));
        assertEquals(Optional.of(Scope.CLIENTS), Scope.of(OLD_SHAPE));
        assertEquals(Optional.of(Scope.CONFIGURATION), Scope.of(OPTION));
        assertEquals(Optional.of(Scope.CONFIGURATION), Scope.of("software.amazon.awssdk.regions.Region"));
        assertEquals(Optional.of(Scope.CONFIGURATION), Scope.of(LEGACY_CREDENTIALS));
        assertEquals(Optional.of(Scope.CONFIGURATION), Scope.of(IDENTITY));
        for (String other : List.of(OLD_HOOK, CoreStubs.RETRY_STRATEGY, "software.amazon.awssdk.http.SdkHttpClient",
                                    "software.amazon.awssdk.core.async.AsyncRequestBody",
                                    "software.amazon.awssdk.core.signer.Signer",
                                    "software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable",
                                    "software.amazon.awssdk.services.s3.presigner.S3Presigner",
                                    "software.amazon.awssdk.core.SdkBytes")) {
            assertEquals(Optional.empty(), Scope.of(other), other);
        }
    }

    /** Renames of every core scope, and preserved entries for the rest of {@link #APP}. */
    private static MigrationManifest renames() {
        List<ManifestEntry> entries = List.of(
            preserved(EntryKind.typeId(CLIENT)), preserved(EntryKind.typeId(CLIENT_BUILDER)),
            preserved(EntryKind.typeId(OPTION)), preserved(EntryKind.typeId(OVERRIDE)),
            preserved(CLIENT_BUILDER_FACTORY), preserved(BUILD), preserved(BUILDER_REGION),
            preserved(OVERRIDE_FACTORY), preserved(OVERRIDE_BUILD),
            renamed(EntryKind.typeId(OLD_SHAPE), EntryKind.typeId(NEW_SHAPE)),
            renamed(SHAPE_CREATE, NEW_SHAPE_CREATE), renamed(SHAPE_NAME, NEW_SHAPE_NAME),
            renamed(EntryKind.typeId(LEGACY_CREDENTIALS), EntryKind.typeId(IDENTITY)),
            renamed(CREDENTIALS_CREATE, IDENTITY_CREATE), renamed(BUILDER_CREDENTIALS, V3_BUILDER_CREDENTIALS),
            renamed(LEGACY_ENDPOINT, ENDPOINT_URL), renamed(OLD_DESCRIBE, DESCRIBE),
            renamed(OLD_TIMEOUT, TIMEOUT), renamed(PUT_LEGACY_HEADER, PUT_HEADER),
            renamed(OLD_REGION_ENV, REGION_ENV));
        return manifest(CANDIDATE, entries.toArray(ManifestEntry[]::new));
    }
}
