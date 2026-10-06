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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.ATTRIBUTES;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.BUILD;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.CONTEXT;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.GET_ATTRIBUTE;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.HOOK_CONTEXT;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.HTTP_REQUEST;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.HTTP_REQUEST_ACCESSOR;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.INTERCEPTOR;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.MODIFY_HTTP_REQUEST;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.MODIFY_REQUEST;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.PUT_HEADER;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.REQUEST_ACCESSOR;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.SDK_REQUEST;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.STRUCT;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.TO_BUILDER;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.V3_INTERCEPTOR;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.adapted;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.d;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.method;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.preserved;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.recipe;
import static software.amazon.awssdk.v2migration.recipes.runtime.RuntimeFixtures.type;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/** {@code interceptors.hooks} and {@code conversions.unused-imports} on V2 interceptors (Requirement 11.12). */
class InterceptorRecipeTest {

    private static final String PATH = "src/main/java/com/example/TenantHeader.java";

    private static final String TENANT_HEADER = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.core.SdkRequest;",
        "import software.amazon.awssdk.core.interceptor.Context;",
        "import software.amazon.awssdk.core.interceptor.ExecutionAttributes;",
        "import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;",
        "import software.amazon.awssdk.http.SdkHttpRequest;",
        "",
        "public class TenantHeader implements ExecutionInterceptor {",
        "    private static final String TENANT = \"t-1\";",
        "",
        "    @Override",
        "    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attributes) {",
        "        return context.httpRequest().toBuilder().putHeader(\"x-tenant\", TENANT).build();",
        "    }",
        "",
        "    @Override",
        "    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes attributes) {",
        "        return context.request();",
        "    }",
        "}",
        "");

    /** The V3 interceptor: hooks, typed contexts and imports converted; unused V2 imports removed (blank lines). */
    private static final String CONVERTED = String.join("\n",
        "package com.example;",
        "",
        "",
        "import software.amazon.awssdk.pipeline.HookContext;",
        "",
        "import software.amazon.awssdk.pipeline.ExecutionInterceptor;",
        "import software.amazon.awssdk.http.SdkHttpRequest;",
        "",
        "public class TenantHeader implements ExecutionInterceptor {",
        "    private static final String TENANT = \"t-1\";",
        "",
        "    @Override",
        "    public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {",
        "        return context.httpRequest().toBuilder().putHeader(\"x-tenant\", TENANT).build();",
        "    }",
        "",
        "    @Override",
        "    public software.amazon.smithy.java.core.schema.SerializableStruct modifyRequest(HookContext.ModifyRequest "
        + "context) {",
        "        return context.request();",
        "    }",
        "}",
        "");

    private static Path v2;
    private static Path v3;

    @TempDir
    Path root;

    @BeforeAll
    static void compileStubs(@TempDir Path stubs) throws Exception {
        v2 = RuntimeFixtures.compileV2(stubs);
        v3 = RuntimeFixtures.compileV3(stubs);
    }

    /** The interceptor surface as a real manifest would classify it: hooks, contexts and accessors recipe-replaced. */
    private static List<ManifestEntry> interceptorEntries() {
        List<ManifestEntry> entries = new ArrayList<>(List.of(
            recipe(type(INTERCEPTOR), type(V3_INTERCEPTOR)),
            recipe(type(CONTEXT), type(HOOK_CONTEXT)),
            recipe(type(CONTEXT + "$ModifyHttpRequest"), type(HOOK_CONTEXT + "$ModifyBeforeSigning")),
            adapted(type(ATTRIBUTES), MigrationKind.DIAGNOSTIC, type("software.amazon.awssdk.core.context.ContextKey"),
                    "Store per-call state under typed context keys."),
            adapted(type(SDK_REQUEST), MigrationKind.DIAGNOSTIC, type(STRUCT),
                    "Modeled values are SerializableStruct."),
            recipe(MODIFY_HTTP_REQUEST, method(V3_INTERCEPTOR, "modifyBeforeSigning",
                                               d(HOOK_CONTEXT + "$ModifyBeforeSigning"), d(HTTP_REQUEST))),
            recipe(MODIFY_REQUEST, method(V3_INTERCEPTOR, "modifyRequest", d(HOOK_CONTEXT + "$ModifyRequest"),
                                          d(STRUCT))),
            recipe(HTTP_REQUEST_ACCESSOR, method(HOOK_CONTEXT + "$ModifyBeforeSigning", "httpRequest", "",
                                                 d(HTTP_REQUEST))),
            recipe(REQUEST_ACCESSOR, method(HOOK_CONTEXT, "request", "", d(STRUCT))),
            preserved(type(HTTP_REQUEST)),
            preserved(type(HTTP_REQUEST + "$Builder")),
            preserved(TO_BUILDER),
            preserved(PUT_HEADER),
            preserved(BUILD)));
        return entries;
    }

    private MigrationResult migrate(String source) throws Exception {
        return migrate(source, interceptorEntries());
    }

    private MigrationResult migrate(String source, List<ManifestEntry> entries) throws Exception {
        MigrationManifest manifest = RuntimeFixtures.manifest(entries);
        return RuntimeFixtures.migrate(root, v2, manifest, Map.of(PATH, source));
    }

    @Test
    void migrate_sideEffectFreeHooks_convertsToV3HooksContextsAndImports() throws Exception {
        MigrationResult result = migrate(TENANT_HEADER);

        assertEquals(List.of(), result.diagnostics(), () -> result.diagnostics().toString());
        assertEquals(CONVERTED, result.changed().get(PATH));
        assertEquals(Disposition.TRANSFORMED,
                     RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_HTTP_REQUEST).disposition());
        ItemResult modifyRequest = RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_REQUEST);
        assertEquals("interceptors.hooks", modifyRequest.recipe());
        assertTrue(modifyRequest.reason().contains("parameter attributes is removed"), modifyRequest.reason());
        assertEquals(Disposition.TRANSFORMED,
                     RuntimeFixtures.result(result, ItemKind.IMPORT, type(ATTRIBUTES)).disposition());
        assertEquals("conversions.unused-imports",
                     RuntimeFixtures.result(result, ItemKind.IMPORT, type(SDK_REQUEST)).recipe());
    }

    @Test
    void migrate_withEveryRegisteredRecipe_convertsTheSameWay() throws Exception {
        Path file = root.resolve(PATH);
        Files.createDirectories(file.getParent());
        Files.writeString(file, TENANT_HEADER, StandardCharsets.UTF_8);

        MigrationResult result = MigrationEngine.run(
            MigrationRequest.builder().projectRoot(root).sources(List.of(file)).classpath(List.of(v2))
                            .manifest(RuntimeFixtures.manifest(interceptorEntries()))
                            .recipes(Recipes.discover(InterceptorRecipe.class.getClassLoader())).build());

        assertEquals(List.of(), result.diagnostics(), () -> result.diagnostics().toString());
        assertEquals(CONVERTED, result.changed().get(PATH));
    }

    @Test
    void migrate_sideEffectFreeHooks_producesSourceThatCompilesAgainstV3() throws Exception {
        MigrationResult result = migrate(TENANT_HEADER);

        String errors = RuntimeFixtures.compileAgainst(root.resolve("compile"),
                                                       Map.of("com.example.TenantHeader", result.changed().get(PATH)),
                                                       v3);

        assertEquals("", errors);
    }

    @Test
    void migrate_renamedHookWithSideEffects_leavesTheHookUnchangedWithAFrequencyDiagnostic() throws Exception {
        String counting = TENANT_HEADER.replace(
            "        return context.httpRequest().toBuilder()",
            "        java.util.concurrent.atomic.AtomicInteger calls = CALLS;\n"
            + "        calls.incrementAndGet();\n"
            + "        return context.httpRequest().toBuilder()")
                                       .replace("    private static final String TENANT = \"t-1\";",
                                                "    private static final String TENANT = \"t-1\";\n"
                                                + "    private static final java.util.concurrent.atomic.AtomicInteger "
                                                + "CALLS = new java.util.concurrent.atomic.AtomicInteger();");

        MigrationResult result = migrate(counting);

        ItemResult hook = RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_HTTP_REQUEST);
        assertEquals(Disposition.UNRESOLVED, hook.disposition());
        String message = hook.diagnostic().reason();
        assertTrue(message.contains("becomes V3 modifyBeforeSigning, which runs at a different lifecycle point or "
                                    + "frequency"), message);
        assertTrue(message.contains("calls calls.incrementAndGet"), message);
        String text = result.changed().get(PATH);
        int start = hook.item().location().startOffset();
        String original = counting.substring(start, hook.item().location().endOffset());
        assertTrue(text.contains(original), "The unresolved hook must be left byte-for-byte unchanged");
        // The same-named hook, whose frequency does not change, is still converted.
        assertEquals(Disposition.TRANSFORMED,
                     RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_REQUEST).disposition());
    }

    @Test
    void migrate_hookReadingExecutionAttributes_isUnresolvedAndUnchanged() throws Exception {
        String reading = TENANT_HEADER.replace("        return context.request();",
                                               "        Object tenant = attributes.getAttribute(\"tenant\");\n"
                                               + "        return context.request();");
        List<ManifestEntry> entries = interceptorEntries();
        entries.add(preserved(GET_ATTRIBUTE));

        MigrationResult result = migrate(reading, entries);

        ItemResult hook = RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_REQUEST);
        assertEquals(Disposition.UNRESOLVED, hook.disposition());
        assertTrue(hook.diagnostic().reason().contains("reads the V2 parameter attributes"),
                   hook.diagnostic().reason());
        assertTrue(result.changed().get(PATH).contains(
            "    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes attributes) {"));
        // ExecutionAttributes is still used, so its import stays (and is reported by its manifest diagnostic).
        ItemResult attributesImport = RuntimeFixtures.result(result, ItemKind.IMPORT, type(ATTRIBUTES));
        assertEquals(Disposition.UNRESOLVED, attributesImport.disposition());
    }

    @Test
    void migrate_hookWhoseManifestEntryIsADiagnostic_isLeftToTheManifest() throws Exception {
        List<ManifestEntry> entries = new ArrayList<>(interceptorEntries());
        entries.removeIf(e -> e.id().equals(MODIFY_HTTP_REQUEST));
        entries.add(adapted(MODIFY_HTTP_REQUEST, MigrationKind.DIAGNOSTIC,
                            method(V3_INTERCEPTOR, "modifyBeforeSigning", d(HOOK_CONTEXT + "$ModifyBeforeSigning"),
                                   d(HTTP_REQUEST)),
                            "Move the header mutation to modifyBeforeSigning."));

        MigrationResult result = migrate(TENANT_HEADER, entries);

        ItemResult hook = RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_HTTP_REQUEST);
        assertEquals(Disposition.UNRESOLVED, hook.disposition());
        assertEquals(null, hook.recipe());
        assertEquals("Move the header mutation to modifyBeforeSigning.", hook.diagnostic().migration());
        assertFalse(result.changed().get(PATH).contains("modifyBeforeSigning"));
    }

    @Test
    void migrate_contextEscapingTheHook_isUnresolved() throws Exception {
        String escaping = TENANT_HEADER.replace("        return context.request();",
                                                "        Object kept = context;\n        return context.request();");

        MigrationResult result = migrate(escaping);

        ItemResult hook = RuntimeFixtures.result(result, ItemKind.METHOD_OVERRIDE, MODIFY_REQUEST);
        assertEquals(Disposition.UNRESOLVED, hook.disposition());
        assertTrue(hook.diagnostic().reason().contains("passes or stores the V2 context context"),
                   hook.diagnostic().reason());
    }
}
