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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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

/**
 * V2- and V3-shaped stub APIs of the Modern Auth, Modern Retry and legacy signer/retry surfaces (packages, nesting
 * and generics mirroring the real ones), manifest entries shaped like the checked-in classification rules, and an
 * engine runner for the recipe tests of this package.
 */
final class ModernFixtures {
    static final String V2 = "software.amazon.awssdk.";
    static final String APP_PATH = "src/main/java/com/example/App.java";

    static final String RETRY_STRATEGY = V2 + "retries.api.RetryStrategy";
    static final String RETRY_STRATEGY_BUILDER = RETRY_STRATEGY + "$Builder";
    static final String RETRY_TOKEN = V2 + "retries.api.RetryToken";
    static final String HTTP_SIGNER = V2 + "http.auth.spi.signer.HttpSigner";
    static final String V3_RETRY_STRATEGY = V2 + "retries.RetryStrategy";
    static final String V3_RETRY_STRATEGY_BUILDER = V3_RETRY_STRATEGY + "$Builder";
    static final String V3_RETRY_TOKEN = V2 + "retries.RetryToken";
    static final String V3_HTTP_SIGNER = V2 + "auth.api.HttpSigner";
    static final String MAX_ATTEMPTS = EntryKind.methodId(RETRY_STRATEGY, "maxAttempts", "()I");

    static final String RETRY_POLICY = V2 + "core.retry.RetryPolicy";
    static final String RETRY_POLICY_NONE = EntryKind.methodId(RETRY_POLICY, "none", "()L" + path(RETRY_POLICY) + ";");
    static final String SIGNER = V2 + "core.signer.Signer";
    static final String AWS4_SIGNER = V2 + "auth.signer.Aws4Signer";
    static final String AWS4_SIGNER_CREATE = EntryKind.methodId(AWS4_SIGNER, "create", "()L" + path(AWS4_SIGNER) + ";");
    static final String OVERRIDE_BUILDER = V2 + "core.RequestOverrideConfiguration$Builder";
    static final String OVERRIDE_SIGNER = EntryKind.methodId(OVERRIDE_BUILDER, "signer",
                                                             "(L" + path(SIGNER) + ";)L" + path(OVERRIDE_BUILDER) + ";");
    static final String CHECKSUM_ALGORITHM = V2 + "core.checksums.Algorithm";
    static final String CRC32 = EntryKind.fieldId(CHECKSUM_ALGORITHM, "CRC32", "L" + path(CHECKSUM_ALGORITHM) + ";");

    static final String HTTP_SIGNER_REPLACEMENT = "api:" + V3_HTTP_SIGNER;
    static final String RETRY_STRATEGY_REPLACEMENT = "api:" + V3_RETRY_STRATEGY;

    private static final ManifestBaseline BASELINE = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                          "parity-manifest/test", null, null);

    private static final Map<String, String> V2_SOURCES = Map.ofEntries(
        Map.entry(RETRY_STRATEGY, "package software.amazon.awssdk.retries.api;\n"
                                  + "public interface RetryStrategy {\n"
                                  + "    int maxAttempts();\n"
                                  + "    interface Builder<B extends Builder<B, T>, T extends RetryStrategy> {\n"
                                  + "        T build();\n"
                                  + "    }\n"
                                  + "}\n"),
        Map.entry(RETRY_TOKEN, "package software.amazon.awssdk.retries.api;\npublic interface RetryToken {}\n"),
        Map.entry(HTTP_SIGNER, "package software.amazon.awssdk.http.auth.spi.signer;\n"
                               + "public interface HttpSigner<T> {}\n"),
        Map.entry(RETRY_POLICY, "package software.amazon.awssdk.core.retry;\n"
                                + "@Deprecated\n"
                                + "public final class RetryPolicy {\n"
                                + "    private RetryPolicy() {}\n"
                                + "    public static RetryPolicy none() { return null; }\n"
                                + "}\n"),
        Map.entry(SIGNER, "package software.amazon.awssdk.core.signer;\n@Deprecated\npublic interface Signer {}\n"),
        Map.entry(AWS4_SIGNER, "package software.amazon.awssdk.auth.signer;\n"
                               + "import software.amazon.awssdk.core.signer.Signer;\n"
                               + "@Deprecated\n"
                               + "public final class Aws4Signer implements Signer {\n"
                               + "    public static Aws4Signer create() { return new Aws4Signer(); }\n"
                               + "}\n"),
        Map.entry(V2 + "core.RequestOverrideConfiguration", "package software.amazon.awssdk.core;\n"
                                + "import software.amazon.awssdk.core.signer.Signer;\n"
                                + "public abstract class RequestOverrideConfiguration {\n"
                                + "    public interface Builder {\n"
                                + "        @Deprecated Builder signer(Signer signer);\n"
                                + "    }\n"
                                + "}\n"),
        Map.entry(CHECKSUM_ALGORITHM, "package software.amazon.awssdk.core.checksums;\n"
                                      + "@Deprecated public enum Algorithm { CRC32 }\n"));

    private static final Map<String, String> V3_SOURCES = Map.ofEntries(
        Map.entry(V3_RETRY_STRATEGY, "package software.amazon.awssdk.retries;\n"
                                     + "public interface RetryStrategy {\n"
                                     + "    int maxAttempts();\n"
                                     + "    interface Builder<B extends Builder<B, S>, S extends RetryStrategy> {\n"
                                     + "        S build();\n"
                                     + "    }\n"
                                     + "}\n"),
        Map.entry(V3_RETRY_TOKEN, "package software.amazon.awssdk.retries;\npublic interface RetryToken {}\n"),
        Map.entry(V3_HTTP_SIGNER, "package software.amazon.awssdk.auth.api;\npublic interface HttpSigner<T> {}\n"));

    private ModernFixtures() {
    }

    /** The recipes of this package, as the service registration lists them. */
    static List<MigrationRecipe> recipes() {
        return List.of(new ModernTypeRecipe.Auth(), new ModernTypeRecipe.Retry(), new ExcludedFacilityRecipe());
    }

    /** Compiles the V2 stub API into {@code directory} and returns the class directory. */
    static Path v2Stubs(Path directory) throws IOException {
        return compile(directory, V2_SOURCES, List.of()).classes();
    }

    /** Compiles the V3 stub API into {@code directory} and returns the class directory. */
    static Path v3Stubs(Path directory) throws IOException {
        return compile(directory, V3_SOURCES, List.of()).classes();
    }

    /** The compiler errors of {@code source} (a file at {@link #APP_PATH}) against {@code classpath}. */
    static String compileErrors(Path directory, String source, Path classpath) throws IOException {
        return compile(directory, Map.of("com.example.App", source), List.of(classpath)).errors();
    }

    /** Runs the engine over {@code app} (written to {@link #APP_PATH} in {@code root}) with {@code recipes}. */
    static MigrationResult run(Path root, Path stubs, String app, MigrationManifest manifest,
                               List<? extends MigrationRecipe> recipes) throws IOException {
        Path file = root.resolve(APP_PATH);
        Files.createDirectories(file.getParent());
        Files.writeString(file, app, StandardCharsets.UTF_8);
        return MigrationEngine.run(MigrationRequest.builder()
                                                   .projectRoot(root)
                                                   .project("com.example:app")
                                                   .sources(List.of(file))
                                                   .classpath(List.of(stubs))
                                                   .manifest(manifest)
                                                   .recipes(recipes)
                                                   .build());
    }

    /** The single result of kind {@code kind} joined by {@code id}. */
    static ItemResult result(MigrationResult result, ItemKind kind, String id) {
        List<ItemResult> matches = result.items().stream()
                                         .filter(r -> r.item().kind() == kind && id.equals(r.item().id())).toList();
        if (matches.size() != 1) {
            throw new AssertionError("Expected one " + kind + " " + id + " item, found " + matches);
        }
        return matches.get(0);
    }

    /** Every result of kind {@code kind} joined by {@code id}. */
    static List<ItemResult> results(MigrationResult result, ItemKind kind, String id) {
        return result.items().stream().filter(r -> r.item().kind() == kind && id.equals(r.item().id())).toList();
    }

    static MigrationManifest manifest(ManifestEntry... entries) {
        return MigrationManifest.of(BASELINE, List.of(entries));
    }

    static ManifestEntry preserved(String id) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.PRESERVED,
                                 List.of("source"), "Kept unchanged in V3.", List.of(), null, null, null, null, null,
                                 List.of());
    }

    /** A recipe-backed package move, as the modern-retry-* and modern-auth-* rules classify the moved types. */
    static ManifestEntry moved(String v2Type, String v3Type) {
        String id = EntryKind.typeId(v2Type);
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "The SPI moved to a V3 package.", List.of("The package changed."),
                                 "software.amazon.awssdk:retries", EntryKind.typeId(v3Type), MigrationKind.RECIPE,
                                 "Import " + v3Type.replace('$', '.') + " instead.", null, List.of("SomeTest"));
    }

    /** A moved type's member, as the *-members rules classify it. */
    static ManifestEntry movedMember(String id, String v3Type) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "The SPI moved to a V3 package.", List.of("The package changed."),
                                 "software.amazon.awssdk:retries", EntryKind.typeId(v3Type), MigrationKind.DIAGNOSTIC,
                                 "Check this use against the V3 type.", null, List.of("SomeTest"));
    }

    static ManifestEntry adapted(String id, MigrationKind kind, String replacement) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "V3 changes the contract.", List.of("The V3 contract differs."),
                                 "software.amazon.awssdk:retries", replacement, kind, "Adapt the use.", null,
                                 List.of("SomeTest"));
    }

    static ManifestEntry excluded(String id, String rationale, String replacement, String migration) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, true, Classification.EXCLUDED,
                                 List.of(), rationale, List.of(), null, replacement, MigrationKind.DIAGNOSTIC,
                                 migration, "Deprecated in V2.", List.of());
    }

    private static EntryKind kind(String id) {
        return EntryKind.ofId(id).orElseThrow(() -> new IllegalArgumentException(id));
    }

    private static String path(String binaryName) {
        return binaryName.replace('.', '/');
    }

    private record Compiled(Path classes, String errors) {
    }

    private static Compiled compile(Path directory, Map<String, String> sources, List<Path> classpath)
        throws IOException {
        Path src = directory.resolve("src");
        Path classes = Files.createDirectories(directory.resolve("classes"));
        List<String> args = new ArrayList<>(List.of("-d", classes.toString(), "-proc:none", "--release", "21",
                                                    "-Xlint:-deprecation"));
        if (!classpath.isEmpty()) {
            args.addAll(List.of("-cp", classpath.get(0).toString()));
        }
        for (Map.Entry<String, String> source : sources.entrySet()) {
            String name = source.getKey().contains("$") ? source.getKey().substring(0, source.getKey().indexOf('$'))
                                                        : source.getKey();
            Path file = src.resolve(name.replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            args.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int status = compiler.run(null, null, errors, args.toArray(new String[0]));
        String output = errors.toString(StandardCharsets.UTF_8);
        if (status != 0 && sources.size() > 1) {
            throw new IllegalStateException("Stub compilation failed:\n" + output);
        }
        return new Compiled(classes, status == 0 ? "" : output);
    }
}
