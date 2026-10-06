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

import static software.amazon.awssdk.v2migration.Entries.adapted;
import static software.amazon.awssdk.v2migration.Entries.excluded;
import static software.amazon.awssdk.v2migration.Entries.preserved;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/** A V2 test application, its manifest and recipe helpers shared by the engine tests. */
final class Fixtures {

    static final String APP_PATH = "src/main/java/com/example/App.java";

    /** Uses every inventoried construct: imports, hooks, builders, members, constructors, fields and a config key. */
    static final String APP = String.join("\n",
        "package com.example;",
        "",
        "import software.amazon.awssdk.core.interceptor.Context;",
        "import software.amazon.awssdk.core.interceptor.ExecutionAttributes;",
        "import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;",
        "import software.amazon.awssdk.regions.Region;",
        "import software.amazon.awssdk.services.s3.S3Client;",
        "import static software.amazon.awssdk.core.retry.RetryPolicy.defaultRetryPolicy;",
        "",
        "public class App implements ExecutionInterceptor {",
        "    @Override",
        "    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attributes) {",
        "    }",
        "",
        "    Object client() {",
        "        String region = System.getenv(\"AWS_REGION\");",
        "        return S3Client.builder().region(Region.of(region)).build();",
        "    }",
        "",
        "    Object retry() {",
        "        return defaultRetryPolicy();",
        "    }",
        "",
        "    Object inner(software.amazon.awssdk.core.Outer outer) {",
        "        return outer.new Inner(\"x\");",
        "    }",
        "",
        "    Region constant() {",
        "        return Region.US_EAST_1;",
        "    }",
        "}",
        "");

    static final String CONFIG_AWS_REGION = EntryKind.configurationId("env", "AWS_REGION");

    private Fixtures() {
    }

    /** The manifest of {@link #APP}: one adapted hook, one excluded retry facility, everything else preserved. */
    static MigrationManifest manifest() {
        return Entries.manifest(
            preserved(EntryKind.typeId("software.amazon.awssdk.core.interceptor.Context")),
            preserved(EntryKind.typeId("software.amazon.awssdk.core.interceptor.Context$BeforeExecution")),
            preserved(EntryKind.typeId("software.amazon.awssdk.core.interceptor.ExecutionAttributes")),
            preserved(EntryKind.typeId(V2Stubs.INTERCEPTOR)),
            preserved(EntryKind.typeId(V2Stubs.REGION)),
            preserved(EntryKind.typeId(V2Stubs.S3_CLIENT)),
            preserved(EntryKind.typeId(V2Stubs.OUTER)),
            preserved(EntryKind.typeId(V2Stubs.OUTER + "$Inner")),
            preserved(V2Stubs.S3_BUILDER),
            preserved(V2Stubs.BUILDER_REGION),
            preserved(V2Stubs.BUILDER_BUILD),
            preserved(V2Stubs.REGION_OF),
            preserved(V2Stubs.REGION_US_EAST_1),
            preserved(V2Stubs.INNER_INIT),
            preserved(CONFIG_AWS_REGION),
            adapted(V2Stubs.BEFORE_EXECUTION, MigrationKind.DIAGNOSTIC,
                    "api:software.amazon.awssdk.core.interceptor.ExecutionInterceptor",
                    "Move per-attempt work to the attempt hooks."),
            excluded(V2Stubs.DEFAULT_RETRY_POLICY, "api:software.amazon.awssdk.retries.StandardRetryStrategy",
                     "Use StandardRetryStrategy.create()."));
    }

    /** Writes {@link #APP} into {@code root} and returns the request for it. */
    static MigrationRequest.Builder app(Path root, Path stubs, List<? extends MigrationRecipe> recipes)
        throws IOException {
        write(root, APP_PATH, APP);
        return MigrationRequest.builder()
                               .projectRoot(root)
                               .project("com.example:app")
                               .sources(List.of(root.resolve(APP_PATH)))
                               .classpath(List.of(stubs))
                               .manifest(manifest())
                               .recipes(recipes);
    }

    static void write(Path root, String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /** A recipe answering {@code answer} for each item. */
    static MigrationRecipe recipe(String id, Function<RecipeContext, RecipeOutcome> answer) {
        return new MigrationRecipe() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String description() {
                return "Test recipe " + id + ".";
            }

            @Override
            public RecipeOutcome visit(RecipeContext context) {
                return answer.apply(context);
            }
        };
    }

    /** A recipe answering {@code answer} for items joined by {@code entryId}, and not applicable otherwise. */
    static MigrationRecipe forEntry(String id, String entryId, ItemKind kind,
                                    Function<RecipeContext, RecipeOutcome> answer) {
        return recipe(id, c -> entryId.equals(c.item().id()) && c.item().kind() == kind
                               ? answer.apply(c) : RecipeOutcome.notApplicable());
    }

    /** The single result for the item of {@code kind} joined by {@code id}. */
    static ItemResult result(MigrationResult result, ItemKind kind, String id) {
        List<ItemResult> matches = result.items().stream()
                                         .filter(r -> r.item().kind() == kind && id.equals(r.item().id())).toList();
        if (matches.size() != 1) {
            throw new AssertionError("Expected one " + kind + " " + id + " item, found " + matches);
        }
        return matches.get(0);
    }
}
