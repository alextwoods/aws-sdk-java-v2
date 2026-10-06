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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestBaseline;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/** Manifests, stub APIs and engine runs for the core recipe tests. */
final class CoreFixtures {

    static final String V3_VERSION = "3.0.0";

    /** A candidate manifest baseline: the V3 version the dependency recipes write. */
    static final ManifestBaseline CANDIDATE = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                   "parity-manifest/test", V3_VERSION, null);

    /** A baseline-only manifest: no V3 version is known. */
    static final ManifestBaseline BASELINE_ONLY = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                       "parity-manifest/test", null, null);

    static final String APP_PATH = "src/main/java/com/example/App.java";

    private CoreFixtures() {
    }

    static ManifestEntry preserved(String id) {
        return entry(id, Classification.PRESERVED, null, null, null);
    }

    /** {@code ADAPTED} with an automated recipe and {@code replacement}. */
    static ManifestEntry renamed(String id, String replacement) {
        return entry(id, Classification.ADAPTED, replacement, MigrationKind.RECIPE, "Use " + replacement + ".");
    }

    /** {@code ADAPTED} with a manual-adaptation diagnostic. */
    static ManifestEntry adaptedManually(String id, String replacement) {
        return entry(id, Classification.ADAPTED, replacement, MigrationKind.DIAGNOSTIC, "Adapt manually.");
    }

    static ManifestEntry excluded(String id, String replacement, String migration) {
        return entry(id, Classification.EXCLUDED, replacement, MigrationKind.DIAGNOSTIC, migration);
    }

    private static ManifestEntry entry(String id, Classification classification, String replacement,
                                       MigrationKind kind, String migration) {
        boolean preserved = classification == Classification.PRESERVED;
        return new ManifestEntry(id, EntryKind.ofId(id).orElseThrow(), "surface " + id, "artifact", null,
                                 classification == Classification.EXCLUDED, classification,
                                 preserved ? List.of("source") : List.of(), "Rationale of " + id + ".",
                                 preserved ? List.of() : List.of("The V3 contract differs."), null, replacement, kind,
                                 migration, null, List.of());
    }

    static MigrationManifest manifest(ManifestBaseline baseline, ManifestEntry... entries) {
        return MigrationManifest.of(baseline, List.of(entries));
    }

    static String artifact(String artifactId) {
        return EntryKind.artifactId(DependencyPlans.SDK_GROUP, artifactId);
    }

    /** The core recipes, discovered as the Maven plugin discovers them, without other recipe families. */
    static List<MigrationRecipe> coreRecipes() {
        return Recipes.discover(CoreFixtures.class.getClassLoader()).stream()
                      .filter(r -> r.getClass().getPackageName().equals(CoreFixtures.class.getPackageName()))
                      .toList();
    }

    /** Migrates a project holding only {@code pom} (inventoried standalone). */
    static MigrationResult migratePom(Path root, String pom, MigrationManifest manifest) throws IOException {
        write(root, "pom.xml", pom);
        return MigrationEngine.run(MigrationRequest.builder().projectRoot(root).pomFile(Path.of("pom.xml"))
                                                   .manifest(manifest).recipes(coreRecipes()).build());
    }

    /** Migrates a project holding only {@link #APP_PATH} with {@code source}, attributed against {@code classpath}. */
    static MigrationResult migrateSource(Path root, String source, Path classpath, MigrationManifest manifest)
        throws IOException {
        return migrateSource(root, source, classpath, manifest, coreRecipes());
    }

    /** {@link #migrateSource(Path, String, Path, MigrationManifest)} with {@code recipes}. */
    static MigrationResult migrateSource(Path root, String source, Path classpath, MigrationManifest manifest,
                                         List<MigrationRecipe> recipes) throws IOException {
        write(root, APP_PATH, source);
        return MigrationEngine.run(MigrationRequest.builder().projectRoot(root).project("com.example:app")
                                                   .sources(List.of(root.resolve(APP_PATH)))
                                                   .classpath(List.of(classpath)).manifest(manifest)
                                                   .recipes(recipes).build());
    }

    /** Every registered recipe, of every family, as the Maven plugin discovers them. */
    static List<MigrationRecipe> allRecipes() {
        return Recipes.discover(CoreFixtures.class.getClassLoader());
    }

    static void write(Path root, String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /** The single result for the item of {@code kind} whose ID is {@code id} (which may be {@code null}). */
    static ItemResult only(MigrationResult result, ItemKind kind, String id) {
        List<ItemResult> matches = all(result, kind, id);
        assertEquals(1, matches.size(), () -> "Expected one " + kind + " " + id + " item, found " + result.items());
        return matches.get(0);
    }

    static List<ItemResult> all(MigrationResult result, ItemKind kind, String id) {
        return result.items().stream()
                     .filter(r -> r.item().kind() == kind && java.util.Objects.equals(id, r.item().id()))
                     .toList();
    }

    /** Asserts the item's disposition and deciding recipe. */
    static void assertDecided(ItemResult result, Disposition disposition, String recipe) {
        assertEquals(disposition, result.disposition(), () -> "Unexpected disposition: " + result);
        assertEquals(recipe, result.recipe(), () -> "Unexpected recipe: " + result);
    }

    /** The text of {@code path} after migration. */
    static String migrated(MigrationResult result, String path) {
        return result.changed().getOrDefault(path, result.originals().get(path));
    }

    /** Compiles {@code sources} (binary name to source) into {@code directory}/classes against {@code classpath}. */
    static Path compile(Path directory, Map<String, String> sources, List<Path> classpath) throws IOException {
        Path sourceRoot = directory.resolve("src");
        Path classes = directory.resolve("classes");
        Files.createDirectories(classes);
        List<String> args = new ArrayList<>(List.of("-d", classes.toString(), "-proc:none", "--release", "21"));
        if (!classpath.isEmpty()) {
            args.add("-classpath");
            args.add(String.join(java.io.File.pathSeparator, classpath.stream().map(Path::toString).toList()));
        }
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = sourceRoot.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            args.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, args.toArray(new String[0]));
        if (status != 0) {
            throw new AssertionError("Compilation of " + sources.keySet() + " failed; see the compiler output");
        }
        return classes;
    }
}
