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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
 * V2- and V3-shaped stub APIs (packages, nesting and signatures mirroring the real interceptor, HTTP, streaming and
 * high-level-library surfaces), manifest entries and an engine runner for the runtime recipe tests. Migrated sources
 * are compiled against the V3 stubs to show that a transformation without unresolved items compiles.
 */
final class RuntimeFixtures {

    static final String V2 = "software.amazon.awssdk.";
    static final String INTERCEPTOR = V2 + "core.interceptor.ExecutionInterceptor";
    static final String CONTEXT = V2 + "core.interceptor.Context";
    static final String ATTRIBUTES = V2 + "core.interceptor.ExecutionAttributes";
    static final String SDK_REQUEST = V2 + "core.SdkRequest";
    static final String HTTP_REQUEST = V2 + "http.SdkHttpRequest";
    static final String HTTP_CLIENT = V2 + "http.SdkHttpClient";
    static final String APACHE = V2 + "http.apache.ApacheHttpClient";
    static final String PUBLISHER = V2 + "core.async.SdkPublisher";
    static final String ASYNC_BODY = V2 + "core.async.AsyncRequestBody";
    static final String TRANSFER_MANAGER = V2 + "transfer.s3.S3TransferManager";
    static final String SNS = V2 + "messagemanager.sns.SnsMessageManager";

    static final String V3_INTERCEPTOR = V2 + "pipeline.ExecutionInterceptor";
    static final String HOOK_CONTEXT = V2 + "pipeline.HookContext";
    static final String STRUCT = "software.amazon.smithy.java.core.schema.SerializableStruct";
    static final String SYNC_CLIENT = V2 + "http.SdkSyncHttpClient";

    private static final String ATTRIBUTES_D = d(ATTRIBUTES);

    static final String MODIFY_HTTP_REQUEST = method(INTERCEPTOR, "modifyHttpRequest",
                                                     d(CONTEXT + "$ModifyHttpRequest") + ATTRIBUTES_D,
                                                     d(HTTP_REQUEST));
    static final String MODIFY_REQUEST = method(INTERCEPTOR, "modifyRequest", d(CONTEXT + "$ModifyRequest") + ATTRIBUTES_D,
                                                d(SDK_REQUEST));
    static final String HTTP_REQUEST_ACCESSOR = method(CONTEXT + "$AfterMarshalling", "httpRequest", "",
                                                       d(HTTP_REQUEST));
    static final String REQUEST_ACCESSOR = method(CONTEXT + "$BeforeExecution", "request", "", d(SDK_REQUEST));
    static final String GET_ATTRIBUTE = method(ATTRIBUTES, "getAttribute", "Ljava/lang/String;", "Ljava/lang/Object;");
    static final String TO_BUILDER = method(HTTP_REQUEST, "toBuilder", "", d(HTTP_REQUEST + "$Builder"));
    static final String PUT_HEADER = method(HTTP_REQUEST + "$Builder", "putHeader",
                                            "Ljava/lang/String;Ljava/lang/String;", d(HTTP_REQUEST + "$Builder"));
    static final String BUILD = method(HTTP_REQUEST + "$Builder", "build", "", d(HTTP_REQUEST));
    static final String APACHE_CREATE = method(APACHE, "create", "", d(HTTP_CLIENT));
    static final String PUBLISHER_MAP = method(PUBLISHER, "map", "Ljava/util/function/Function;", d(PUBLISHER));
    static final String FROM_STRING = method(ASYNC_BODY, "fromString", "Ljava/lang/String;", d(ASYNC_BODY));
    static final String PRESIGNED_DOWNLOAD = method(TRANSFER_MANAGER, "downloadFileWithPresignedUrl",
                                                    "Ljava/lang/Object;", "Ljava/lang/Object;");
    static final String SNS_BUILDER = method(SNS, "builder", "", d(SNS + "$Builder"));
    static final String SNS_HTTP_CLIENT = method(SNS + "$Builder", "httpClient", d(HTTP_CLIENT), d(SNS + "$Builder"));

    private static final Map<String, String> V2_SOURCES = Map.ofEntries(
        Map.entry(SDK_REQUEST, "package software.amazon.awssdk.core;\npublic abstract class SdkRequest {}\n"),
        Map.entry(HTTP_REQUEST, "package software.amazon.awssdk.http;\n"
                                + "public interface SdkHttpRequest {\n"
                                + "    Builder toBuilder();\n"
                                + "    interface Builder {\n"
                                + "        Builder putHeader(String name, String value);\n"
                                + "        SdkHttpRequest build();\n"
                                + "    }\n"
                                + "}\n"),
        Map.entry(HTTP_CLIENT, "package software.amazon.awssdk.http;\n"
                               + "public interface SdkHttpClient extends AutoCloseable {}\n"),
        Map.entry(APACHE, "package software.amazon.awssdk.http.apache;\n"
                          + "import software.amazon.awssdk.http.SdkHttpClient;\n"
                          + "public final class ApacheHttpClient implements SdkHttpClient {\n"
                          + "    public static SdkHttpClient create() { return new ApacheHttpClient(); }\n"
                          + "    public void close() {}\n"
                          + "}\n"),
        Map.entry(CONTEXT, "package software.amazon.awssdk.core.interceptor;\n"
                           + "import software.amazon.awssdk.core.SdkRequest;\n"
                           + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                           + "public final class Context {\n"
                           + "    private Context() {}\n"
                           + "    public interface BeforeExecution { SdkRequest request(); }\n"
                           + "    public interface ModifyRequest extends BeforeExecution {}\n"
                           + "    public interface AfterMarshalling extends ModifyRequest { SdkHttpRequest httpRequest(); }\n"
                           + "    public interface ModifyHttpRequest extends AfterMarshalling {}\n"
                           + "}\n"),
        Map.entry(ATTRIBUTES, "package software.amazon.awssdk.core.interceptor;\n"
                              + "public final class ExecutionAttributes {\n"
                              + "    public Object getAttribute(String key) { return null; }\n"
                              + "}\n"),
        Map.entry(INTERCEPTOR, "package software.amazon.awssdk.core.interceptor;\n"
                               + "import software.amazon.awssdk.core.SdkRequest;\n"
                               + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                               + "public interface ExecutionInterceptor {\n"
                               + "    default SdkRequest modifyRequest(Context.ModifyRequest c, ExecutionAttributes a) {\n"
                               + "        return c.request();\n"
                               + "    }\n"
                               + "    default SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest c,\n"
                               + "                                             ExecutionAttributes a) {\n"
                               + "        return c.httpRequest();\n"
                               + "    }\n"
                               + "}\n"),
        Map.entry(PUBLISHER, "package software.amazon.awssdk.core.async;\n"
                             + "import java.util.function.Function;\n"
                             + "public interface SdkPublisher<T> {\n"
                             + "    default <U> SdkPublisher<U> map(Function<T, U> mapper) { return null; }\n"
                             + "}\n"),
        Map.entry(ASYNC_BODY, "package software.amazon.awssdk.core.async;\n"
                              + "public interface AsyncRequestBody {\n"
                              + "    static AsyncRequestBody fromString(String s) { return null; }\n"
                              + "}\n"),
        Map.entry(TRANSFER_MANAGER, "package software.amazon.awssdk.transfer.s3;\n"
                                    + "public interface S3TransferManager {\n"
                                    + "    default Object downloadFileWithPresignedUrl(Object request) { return null; }\n"
                                    + "}\n"),
        Map.entry(SNS, "package software.amazon.awssdk.messagemanager.sns;\n"
                       + "import software.amazon.awssdk.http.SdkHttpClient;\n"
                       + "public interface SnsMessageManager {\n"
                       + "    static Builder builder() { return null; }\n"
                       + "    interface Builder { Builder httpClient(SdkHttpClient client); }\n"
                       + "}\n"));

    private static final Map<String, String> V3_SOURCES = Map.ofEntries(
        Map.entry(STRUCT, "package software.amazon.smithy.java.core.schema;\npublic interface SerializableStruct {}\n"),
        Map.entry(HTTP_REQUEST, V2_SOURCES.get(HTTP_REQUEST)),
        Map.entry(SYNC_CLIENT, "package software.amazon.awssdk.http;\n"
                               + "public interface SdkSyncHttpClient extends AutoCloseable {}\n"),
        Map.entry(APACHE, "package software.amazon.awssdk.http.apache;\n"
                          + "import software.amazon.awssdk.http.SdkSyncHttpClient;\n"
                          + "public final class ApacheHttpClient implements SdkSyncHttpClient {\n"
                          + "    public static SdkSyncHttpClient create() { return new ApacheHttpClient(); }\n"
                          + "    public void close() {}\n"
                          + "}\n"),
        Map.entry(HOOK_CONTEXT, "package software.amazon.awssdk.pipeline;\n"
                                + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                                + "import software.amazon.smithy.java.core.schema.SerializableStruct;\n"
                                + "public interface HookContext {\n"
                                + "    SerializableStruct request();\n"
                                + "    interface ModifyRequest extends HookContext {}\n"
                                + "    interface ModifyBeforeSigning extends HookContext { SdkHttpRequest httpRequest(); }\n"
                                + "}\n"),
        Map.entry(V3_INTERCEPTOR, "package software.amazon.awssdk.pipeline;\n"
                                  + "import software.amazon.awssdk.http.SdkHttpRequest;\n"
                                  + "import software.amazon.smithy.java.core.schema.SerializableStruct;\n"
                                  + "public interface ExecutionInterceptor {\n"
                                  + "    default SerializableStruct modifyRequest(HookContext.ModifyRequest c) {\n"
                                  + "        return c.request();\n"
                                  + "    }\n"
                                  + "    default SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning c) {\n"
                                  + "        return c.httpRequest();\n"
                                  + "    }\n"
                                  + "}\n"));

    static final ManifestBaseline BASELINE = new ManifestBaseline("2.54.0", "abc123", null, null,
                                                                  "parity-manifest/test", null, null);

    private RuntimeFixtures() {
    }

    /** The JVM descriptor of a class. */
    static String d(String binaryName) {
        return "L" + binaryName.replace('.', '/') + ";";
    }

    static String method(String owner, String name, String parameters, String result) {
        return EntryKind.methodId(owner, name, "(" + parameters + ")" + result);
    }

    static String type(String binaryName) {
        return EntryKind.typeId(binaryName);
    }

    static ManifestEntry preserved(String id) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.PRESERVED,
                                 List.of("source"), "Kept unchanged in V3.", List.of(), null, null, null, null, null,
                                 List.of());
    }

    static ManifestEntry recipe(String id, String replacement) {
        return adapted(id, MigrationKind.RECIPE, replacement, "Use " + replacement + ".");
    }

    static ManifestEntry adapted(String id, MigrationKind kind, String replacement, String migration) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "V3 changes the contract.", List.of("The V3 contract differs."),
                                 "software.amazon.awssdk:sdk-core", replacement, kind, migration, null,
                                 List.of("test:evidence"));
    }

    static MigrationManifest manifest(List<ManifestEntry> entries) {
        return MigrationManifest.of(BASELINE, entries);
    }

    /** Every recipe of this package, as the plugin discovers them. */
    static List<MigrationRecipe> recipes() {
        return List.of(new InterceptorRecipe(), new HttpRecipe(), new StreamingRecipe(), new PaginatorRecipe(),
                       new WaiterRecipe(), new HighLevelLibraryRecipe(), new KnownGapRecipe(),
                       new ConvertedImportRecipe());
    }

    /** Compiles the V2 stubs into {@code directory}/v2 and returns the class directory. */
    static Path compileV2(Path directory) throws IOException {
        return compile(directory.resolve("v2"), V2_SOURCES, List.of());
    }

    /** Compiles the V3 stubs into {@code directory}/v3 and returns the class directory. */
    static Path compileV3(Path directory) throws IOException {
        return compile(directory.resolve("v3"), V3_SOURCES, List.of());
    }

    /** Writes {@code sources} (path to text) under {@code root} and migrates them with every recipe of this package. */
    static MigrationResult migrate(Path root, Path v2Classes, MigrationManifest manifest, Map<String, String> sources)
        throws IOException {
        List<Path> files = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = root.resolve(source.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            files.add(file);
        }
        return MigrationEngine.run(MigrationRequest.builder()
                                                   .projectRoot(root)
                                                   .sources(files)
                                                   .classpath(List.of(v2Classes))
                                                   .manifest(manifest)
                                                   .recipes(recipes())
                                                   .build());
    }

    /**
     * Compiles {@code sources} (binary name to text) against {@code classpath}.
     *
     * @return the compiler's error output, empty if compilation succeeded
     */
    static String compileAgainst(Path directory, Map<String, String> sources, Path classpath) throws IOException {
        try {
            compile(directory, sources, List.of("-cp", classpath.toString()));
            return "";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
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

    /** Every result for items of {@code kind} joined by {@code id}. */
    static List<ItemResult> results(MigrationResult result, ItemKind kind, String id) {
        return result.items().stream().filter(r -> r.item().kind() == kind && id.equals(r.item().id())).toList();
    }

    private static Path compile(Path directory, Map<String, String> sources, List<String> options) throws IOException {
        Path src = directory.resolve("src");
        Path classes = directory.resolve("classes");
        Files.createDirectories(classes);
        List<String> args = new ArrayList<>(List.of("-d", classes.toString(), "-proc:none", "--release", "21"));
        args.addAll(options);
        for (Map.Entry<String, String> source : new TreeMap<>(sources).entrySet()) {
            Path file = src.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            args.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int status = compiler.run(null, errors, errors, args.toArray(new String[0]));
        if (status != 0) {
            throw new IllegalStateException("Compilation failed: " + errors.toString(StandardCharsets.UTF_8));
        }
        return classes;
    }

    private static EntryKind kind(String id) {
        return EntryKind.ofId(id).orElseThrow(() -> new IllegalArgumentException(id));
    }
}
