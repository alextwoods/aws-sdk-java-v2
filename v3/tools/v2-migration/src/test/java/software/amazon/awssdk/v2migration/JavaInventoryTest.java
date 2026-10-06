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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.Fixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.Fixtures.CONFIG_AWS_REGION;
import static software.amazon.awssdk.v2migration.Fixtures.result;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;

/** The Java inventory: which V2 constructs become items, how they join the manifest, and where they are located. */
class JavaInventoryTest {

    @TempDir
    static Path stubDirectory;
    private static Path stubs;

    @TempDir
    Path project;

    @BeforeAll
    static void compileStubs() throws Exception {
        stubs = V2Stubs.compile(stubDirectory);
    }

    @Test
    void run_v2Application_inventoriesEveryConstructByExactManifestId() throws Exception {
        MigrationResult result = MigrationEngine.run(Fixtures.app(project, stubs, List.of()).build());

        String type = "TYPE_REFERENCE ";
        String context = EntryKind.typeId("software.amazon.awssdk.core.interceptor.Context");
        String attributes = EntryKind.typeId("software.amazon.awssdk.core.interceptor.ExecutionAttributes");
        String interceptor = EntryKind.typeId(V2Stubs.INTERCEPTOR);
        String region = EntryKind.typeId(V2Stubs.REGION);
        String s3 = EntryKind.typeId(V2Stubs.S3_CLIENT);
        List<String> expected = List.of(
            "IMPORT " + context, "IMPORT " + attributes, "IMPORT " + interceptor, "IMPORT " + region, "IMPORT " + s3,
            "STATIC_IMPORT " + V2Stubs.DEFAULT_RETRY_POLICY,
            type + interceptor,
            "METHOD_OVERRIDE " + V2Stubs.BEFORE_EXECUTION,
            type + context + "$BeforeExecution", type + context, type + attributes,
            "CONFIGURATION_KEY " + CONFIG_AWS_REGION,
            type + s3, "METHOD_INVOCATION " + V2Stubs.S3_BUILDER, "METHOD_INVOCATION " + V2Stubs.BUILDER_REGION,
            type + region, "METHOD_INVOCATION " + V2Stubs.REGION_OF, "METHOD_INVOCATION " + V2Stubs.BUILDER_BUILD,
            "METHOD_INVOCATION " + V2Stubs.DEFAULT_RETRY_POLICY,
            type + EntryKind.typeId(V2Stubs.OUTER),
            "CONSTRUCTOR_CALL " + V2Stubs.INNER_INIT, type + EntryKind.typeId(V2Stubs.OUTER + "$Inner"),
            type + region, "FIELD_ACCESS " + V2Stubs.REGION_US_EAST_1, type + region);

        assertEquals(sorted(expected),
                     sorted(result.items().stream().map(r -> r.item().kind() + " " + r.item().id()).toList()));
        assertTrue(result.items().stream().allMatch(r -> r.item().location().path().equals(APP_PATH)));
    }

    @Test
    void run_v2Application_locatesItemsExactly() throws Exception {
        MigrationResult result = MigrationEngine.run(Fixtures.app(project, stubs, List.of()).build());

        // Invocations span the method name through the argument list, so a builder chain's items do not overlap.
        ItemResult regionCall = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.BUILDER_REGION);
        assertEquals("region(Region.of(region))", regionCall.item().construct());
        assertEquals(17, regionCall.item().location().line());
        assertEquals(35, regionCall.item().location().column());
        ItemResult builder = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.S3_BUILDER);
        assertEquals("builder()", builder.item().construct());
        assertTrue(builder.item().location().endOffset() <= regionCall.item().location().startOffset());

        // A hook spans its whole declaration, annotations included.
        ItemResult hook = result(result, ItemKind.METHOD_OVERRIDE, V2Stubs.BEFORE_EXECUTION);
        assertEquals(11, hook.item().location().line());
        assertEquals(13, hook.item().location().endLine());
        assertTrue(hook.item().construct().startsWith("@Override public void beforeExecution("));
        assertEquals(MigrationCategory.INTERCEPTOR, hook.item().category());

        ItemResult staticImport = result(result, ItemKind.STATIC_IMPORT, V2Stubs.DEFAULT_RETRY_POLICY);
        assertEquals("import static software.amazon.awssdk.core.retry.RetryPolicy.defaultRetryPolicy;",
                     staticImport.item().construct());
        assertEquals(8, staticImport.item().location().line());
        assertEquals(1, staticImport.item().location().column());

        ItemResult key = result(result, ItemKind.CONFIGURATION_KEY, CONFIG_AWS_REGION);
        assertEquals("\"AWS_REGION\"", key.item().construct());
        assertEquals(MigrationCategory.CONFIGURATION, key.item().category());
    }

    @Test
    void run_withoutRecipes_manifestClassificationDecidesEveryDisposition() throws Exception {
        MigrationResult result = MigrationEngine.run(Fixtures.app(project, stubs, List.of()).build());

        List<String> unresolved = result.items().stream().filter(r -> r.disposition() == Disposition.UNRESOLVED)
                                        .map(r -> r.item().kind() + " " + r.item().id()).toList();
        assertEquals(List.of("STATIC_IMPORT " + V2Stubs.DEFAULT_RETRY_POLICY,
                             "METHOD_OVERRIDE " + V2Stubs.BEFORE_EXECUTION,
                             "METHOD_INVOCATION " + V2Stubs.DEFAULT_RETRY_POLICY), unresolved);
        assertEquals(result.items().size() - 3, result.count(Disposition.PRESERVED));
        assertEquals(0, result.count(Disposition.TRANSFORMED));
        assertTrue(result.changed().isEmpty());

        MigrationDiagnostic excluded = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.DEFAULT_RETRY_POLICY)
            .diagnostic();
        assertEquals(Classification.EXCLUDED, excluded.classification());
        assertEquals("api:software.amazon.awssdk.retries.StandardRetryStrategy", excluded.replacement());
        assertEquals("Use StandardRetryStrategy.create().", excluded.migration());
        assertEquals("Deprecated legacy facility.", excluded.rationale());
        assertEquals("defaultRetryPolicy()", excluded.construct());
        assertEquals(APP_PATH + ":21:16", excluded.location().toString());
        assertTrue(excluded.message().startsWith(APP_PATH + ":21:16: defaultRetryPolicy() [" + V2Stubs.DEFAULT_RETRY_POLICY
                                                 + ", EXCLUDED]: EXCLUDED from V3"), excluded.message());

        MigrationDiagnostic adapted = result(result, ItemKind.METHOD_OVERRIDE, V2Stubs.BEFORE_EXECUTION).diagnostic();
        assertEquals(Classification.ADAPTED, adapted.classification());
        assertEquals(List.of("The V3 contract differs."), adapted.differences());
    }

    @Test
    void run_unattributableSourceAndUnknownApi_areUnresolvedNotDropped() throws Exception {
        Fixtures.write(project, "src/main/java/com/example/Broken.java", String.join("\n",
            "package com.example;",
            "import software.amazon.awssdk.regions.internal.RegionCache;",
            "class Broken {",
            "    Object cache = new RegionCache();",
            "    Missing missing;",
            "}",
            ""));
        Fixtures.write(project, "src/main/java/com/example/Internal.java", String.join("\n",
            "package com.example;",
            "class Internal {",
            "    Object outer = new software.amazon.awssdk.core.Outer();",
            "}",
            ""));

        MigrationResult result = MigrationEngine.run(
            MigrationRequest.builder().projectRoot(project)
                            .sources(List.of(Path.of("src/main/java/com/example/Broken.java"),
                                             Path.of("src/main/java/com/example/Internal.java")))
                            .classpath(List.of(stubs)).manifest(Fixtures.manifest()).build());

        List<ItemResult> unattributed = result.items().stream()
                                              .filter(r -> r.item().kind() == ItemKind.UNATTRIBUTED_SOURCE).toList();
        assertTrue(unattributed.size() >= 2, unattributed.toString());
        assertTrue(unattributed.stream().allMatch(r -> r.disposition() == Disposition.UNRESOLVED
                                                       && r.item().id() == null
                                                       && r.diagnostic().reason().startsWith(
                                                           "The compiler could not attribute")));
        assertTrue(unattributed.stream().anyMatch(r -> r.item().location().line() == 5), unattributed.toString());

        // An attributed V2 member with no manifest entry (here a constructor the manifest omits) is unresolved.
        ItemResult unknown = result(result, ItemKind.CONSTRUCTOR_CALL, "api:" + V2Stubs.OUTER + "#<init>()V");
        assertEquals(Disposition.UNRESOLVED, unknown.disposition());
        assertNull(unknown.entry());
        assertTrue(unknown.diagnostic().reason().startsWith("No Parity Manifest entry matches this construct"));
        assertEquals("api:" + V2Stubs.OUTER + "#<init>()V", unknown.diagnostic().entryId());
    }

    @Test
    void run_projectTypeInTheV2Namespace_isNotAV2Item() throws Exception {
        Fixtures.write(project, "src/main/java/software/amazon/awssdk/custom/Mine.java", String.join("\n",
            "package software.amazon.awssdk.custom;",
            "public class Mine { public static Mine create() { return new Mine(); } }",
            ""));
        Fixtures.write(project, "src/main/java/com/example/UsesMine.java", String.join("\n",
            "package com.example;",
            "import software.amazon.awssdk.custom.Mine;",
            "class UsesMine { Object m = Mine.create(); }",
            ""));

        MigrationResult result = MigrationEngine.run(
            MigrationRequest.builder().projectRoot(project)
                            .sources(List.of(Path.of("src/main/java/software/amazon/awssdk/custom/Mine.java"),
                                             Path.of("src/main/java/com/example/UsesMine.java")))
                            .classpath(List.of(stubs)).manifest(Fixtures.manifest()).build());

        assertEquals(List.of(), result.items());
    }

    private static String sorted(List<String> values) {
        return values.stream().sorted().collect(Collectors.joining("\n"));
    }
}
