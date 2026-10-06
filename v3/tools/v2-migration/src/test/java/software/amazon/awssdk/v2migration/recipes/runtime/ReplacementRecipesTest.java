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

package software.amazon.awssdk.v2migration.recipes.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.APACHE;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.APACHE_CREATE;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.ASYNC_BODY;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.FROM_STRING;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.HTTP_CLIENT;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.PRESIGNED_DOWNLOAD;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.PUBLISHER;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.PUBLISHER_MAP;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.SNS;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.SNS_BUILDER;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.SNS_HTTP_CLIENT;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.SYNC_CLIENT;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.TRANSFER_MANAGER;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.adapted;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.d;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.method;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.preserved;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.recipe;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.type;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/** The HTTP, streaming and high-level-library families, the known-gap recipe and recipe discovery. */
class ReplacementRecipesTest {

    private static final String HTTP_PATH = "src/main/java/com/example/Http.java";
    private static final String HTTP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.http.SdkHttpClient;",
        "import software.amazon.awssdk.http.apache.ApacheHttpClient;",
        "",
        "class Http {",
        "    SdkHttpClient client() {",
        "        SdkHttpClient client = ApacheHttpClient.create();",
        "        return client;",
        "    }",
        "}",
        "");

    private static final List<ManifestEntry> HTTP_ENTRIES = List.of(
        recipe(type(HTTP_CLIENT), type(SYNC_CLIENT)),
        preserved(type(APACHE)),
        recipe(APACHE_CREATE, method(APACHE, "create", "", d(SYNC_CLIENT))));

    private static Path v2;
    private static Path v3;

    @TempDir
    Path root;

    @BeforeAll
    static void compileStubs(@TempDir Path stubs) throws Exception {
        v2 = RuntimeFixtures.compileV2(stubs);
        v3 = RuntimeFixtures.compileV3(stubs);
    }

    private MigrationResult migrate(List<ManifestEntry> entries, String path, String source) throws Exception {
        return RuntimeFixtures.migrate(root, v2, RuntimeFixtures.manifest(entries), Map.of(path, source));
    }

    @Test
    void migrate_replacedHttpSpiType_renamesTheImportAndReferencesAndCompilesAgainstV3() throws Exception {
        MigrationResult result = migrate(HTTP_ENTRIES, HTTP_PATH, HTTP);

        assertEquals(List.of(), result.diagnostics(), () -> result.diagnostics().toString());
        String converted = HTTP.replace("SdkHttpClient", "SdkSyncHttpClient");
        assertEquals(converted, result.changed().get(HTTP_PATH));
        ItemResult create = RuntimeFixtures.result(result, ItemKind.METHOD_INVOCATION, APACHE_CREATE);
        assertEquals(Disposition.PRESERVED, create.disposition());
        assertEquals("http.replacements", create.recipe());
        assertEquals("", RuntimeFixtures.compileAgainst(root.resolve("compile"), Map.of("com.example.Http", converted),
                                                        v3));
    }

    @Test
    void migrate_replacedHttpSpiTypeWithEveryRegisteredRecipe_convertsTheSameWay() throws Exception {
        Path file = root.resolve(HTTP_PATH);
        Files.createDirectories(file.getParent());
        Files.writeString(file, HTTP, StandardCharsets.UTF_8);

        MigrationResult result = MigrationEngine.run(
            MigrationRequest.builder().projectRoot(root).sources(List.of(file)).classpath(List.of(v2))
                            .manifest(RuntimeFixtures.manifest(HTTP_ENTRIES))
                            .recipes(Recipes.discover(HttpRecipe.class.getClassLoader())).build());

        assertEquals(List.of(), result.diagnostics(), () -> result.diagnostics().toString());
        assertEquals(HTTP.replace("SdkHttpClient", "SdkSyncHttpClient"), result.changed().get(HTTP_PATH));
    }

    @Test
    void migrate_replacedTypeWithoutASingleTypeImport_writesTheQualifiedName() throws Exception {
        String onDemand = HTTP.replace("import software.amazon.awssdk.http.SdkHttpClient;",
                                       "import software.amazon.awssdk.http.*;");

        MigrationResult result = migrate(HTTP_ENTRIES, HTTP_PATH, onDemand);

        assertEquals(onDemand.replace("SdkHttpClient client", "software.amazon.awssdk.http.SdkSyncHttpClient client"),
                     result.changed().get(HTTP_PATH));
    }

    @Test
    void migrate_adaptedOptionWithADiagnosticMigration_isUnresolvedWithTheManifestText() throws Exception {
        String path = "src/main/java/com/example/Sns.java";
        String source = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.http.SdkHttpClient;",
            "import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;",
            "",
            "class Sns {",
            "    Object manager(SdkHttpClient client) {",
            "        return SnsMessageManager.builder().httpClient(client);",
            "    }",
            "}",
            "");
        String migration = "Pass a V3 SdkSyncHttpClient, or omit httpClient to use the installed V3 HTTP "
                           + "implementation.";
        List<ManifestEntry> entries = List.of(
            recipe(type(HTTP_CLIENT), type(SYNC_CLIENT)),
            preserved(type(SNS)),
            preserved(SNS_BUILDER),
            adapted(SNS_HTTP_CLIENT, MigrationKind.DIAGNOSTIC,
                    method(SNS + "$Builder", "httpClient", d(SYNC_CLIENT), d(SNS + "$Builder")), migration));

        MigrationResult result = migrate(entries, path, source);

        ItemResult httpClient = RuntimeFixtures.result(result, ItemKind.METHOD_INVOCATION, SNS_HTTP_CLIENT);
        assertEquals(Disposition.UNRESOLVED, httpClient.disposition());
        assertEquals(migration, httpClient.diagnostic().migration());
        assertTrue(result.changed().get(path).contains(".httpClient(client);"));
    }

    @Test
    void migrate_knownLaunchGaps_areUnresolvedEvenWhenTheManifestPreservesThem() throws Exception {
        String path = "src/main/java/com/example/Gaps.java";
        String source = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.core.async.SdkPublisher;",
            "import software.amazon.awssdk.transfer.s3.S3TransferManager;",
            "",
            "class Gaps {",
            "    Object upper(SdkPublisher<String> pages) {",
            "        return pages.map(String::toUpperCase);",
            "    }",
            "",
            "    Object download(S3TransferManager tm, Object request) {",
            "        return tm.downloadFileWithPresignedUrl(request);",
            "    }",
            "}",
            "");
        List<ManifestEntry> entries = List.of(preserved(type(PUBLISHER)), preserved(PUBLISHER_MAP),
                                              preserved(type(TRANSFER_MANAGER)), preserved(PRESIGNED_DOWNLOAD));

        MigrationResult result = migrate(entries, path, source);

        ItemResult map = RuntimeFixtures.result(result, ItemKind.METHOD_INVOCATION, PUBLISHER_MAP);
        assertEquals(Disposition.UNRESOLVED, map.disposition());
        assertEquals("hll.known-gaps", map.recipe());
        assertTrue(map.diagnostic().reason().contains("known launch gap of the dynamodb-enhanced parity matrix"),
                   map.diagnostic().reason());
        ItemResult presigned = RuntimeFixtures.result(result, ItemKind.METHOD_INVOCATION, PRESIGNED_DOWNLOAD);
        assertEquals(Disposition.UNRESOLVED, presigned.disposition());
        assertTrue(presigned.diagnostic().reason().contains("presigned-URL download API is not shipped"),
                   presigned.diagnostic().reason());
        assertEquals(Disposition.PRESERVED,
                     RuntimeFixtures.result(result, ItemKind.IMPORT, type(TRANSFER_MANAGER)).disposition());
        assertEquals(null, result.changed().get(path));
    }

    @Test
    void migrate_renamedStreamingFactory_renamesTheCallAndRejectsChangedParameters() throws Exception {
        String path = "src/main/java/com/example/Bodies.java";
        String source = String.join("\n",
            "package com.example;",
            "",
            "import software.amazon.awssdk.core.async.AsyncRequestBody;",
            "",
            "class Bodies {",
            "    AsyncRequestBody body() {",
            "        return AsyncRequestBody.fromString(\"x\");",
            "    }",
            "}",
            "");
        List<ManifestEntry> renamed = List.of(
            preserved(type(ASYNC_BODY)),
            recipe(FROM_STRING, method(ASYNC_BODY, "fromUtf8String", "Ljava/lang/String;", d(ASYNC_BODY))));

        MigrationResult result = migrate(renamed, path, source);

        assertEquals(source.replace("fromString(", "fromUtf8String("), result.changed().get(path));
        assertEquals("streaming.replacements",
                     RuntimeFixtures.result(result, ItemKind.METHOD_INVOCATION, FROM_STRING).recipe());

        List<ManifestEntry> retyped = List.of(
            preserved(type(ASYNC_BODY)),
            recipe(FROM_STRING, method(ASYNC_BODY, "fromString", "Ljava/lang/CharSequence;J", d(ASYNC_BODY))));
        MigrationResult unsafe = migrate(retyped, path, source);
        ItemResult call = RuntimeFixtures.result(unsafe, ItemKind.METHOD_INVOCATION, FROM_STRING);
        assertEquals(Disposition.UNRESOLVED, call.disposition());
        assertTrue(call.diagnostic().reason().contains("takes different parameters"), call.diagnostic().reason());
        assertEquals(null, unsafe.changed().get(path));
    }

    @Test
    void discover_pluginClassLoader_findsEveryRuntimeRecipe() {
        List<String> ids = Recipes.discover(InterceptorRecipe.class.getClassLoader()).stream()
                                  .map(MigrationRecipe::id).toList();

        for (MigrationRecipe recipe : RuntimeFixtures.recipes()) {
            assertTrue(ids.contains(recipe.id()), recipe.id() + " is not registered: " + ids);
        }
    }
}
