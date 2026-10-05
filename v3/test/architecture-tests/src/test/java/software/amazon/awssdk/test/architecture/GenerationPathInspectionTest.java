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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.test.architecture.ClassFile.Member;
import software.amazon.awssdk.test.architecture.ClassFile.MemberRef;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Allowance;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.CodegenLayout;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Scope;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Violation;

/**
 * Negative and positive fixtures for {@link GenerationPathInspector}: every {@link GenerationPathRule} is triggered by
 * at least one fixture, and allowed patterns next to them are not reported. Stub sources impersonate Smithy, V2 and
 * V3 types; fixture modules that use them are compiled with the JDK compiler. Resource and generated-source fixtures
 * are synthetic trees.
 */
class GenerationPathInspectionTest {

    /** A generator layout over the fixture packages. */
    private static final CodegenLayout LAYOUT = new CodegenLayout(
        "fixture-codegen", "fixture-plugin", "fixture.codegen.MainGenerator", "fixture.codegen.Entry", "generate",
        "(Ljava/lang/String;)V", "fixture.codegen.validation.", Set.of("fixture.codegen.Entry"));

    private static final Allowance DISCOVERY_ALLOWANCE = new Allowance(
        "fixture-runtime", "fixture.rt.HttpDiscovery", "discover", GenerationPathRule.SERVICE_DISCOVERY,
        "HTTP implementation discovery at client construction (Requirement 12.11)");
    private static final Allowance STALE_ALLOWANCE = new Allowance(
        "fixture-runtime", "fixture.rt.Pipeline", "unused", GenerationPathRule.SERVICE_DISCOVERY, "stale");

    @TempDir
    static Path work;

    private static Map<String, ClassFile> stubs;
    private static Map<String, ClassFile> service;
    private static Map<String, ClassFile> runtime;
    private static Map<String, ClassFile> dynamic;
    private static Map<String, ClassFile> codegen;
    private static Map<String, ClassFile> plugin;

    @BeforeAll
    static void compileFixtures() throws IOException {
        Path stubRoot = JavaFixtures.compile(work, "stubs", List.of(),
            "package software.amazon.smithy.codegen.core.directed; public interface DirectedCodegen<A, B, C> { }",
            "package software.amazon.smithy.codegen.core.directed; public class CodegenDirector {"
            + " public void run() { } }",
            "package software.amazon.smithy.build; public interface SmithyBuildPlugin { }",
            "package software.amazon.smithy.build; public interface FileManifest { }",
            "package software.amazon.smithy.java.codegen; public final class JavaCodegenPlugin { }",
            "package software.amazon.smithy.model; public final class Model { }",
            "package software.amazon.smithy.model.shapes; public final class ShapeId {"
            + " public static ShapeId from(String id) { return new ShapeId(); } }",
            "package software.amazon.smithy.model.shapes; public final class ModelSerializer { }",
            "package software.amazon.smithy.model.loader; public final class ModelAssembler { }",
            "package software.amazon.smithy.model.node; public class Node {"
            + " public static Node parse(String json) { return null; } }",
            "package software.amazon.awssdk.codegen.model.intermediate; public final class IntermediateModel { }",
            "package software.amazon.awssdk.core.internal.http; public final class AmazonSyncHttpClient { }",
            "package software.amazon.awssdk.http; public final class SdkHttpFullRequest { }",
            "package software.amazon.awssdk.core.runtime.transform; public interface Marshaller<T> { }",
            "package software.amazon.awssdk.protocols.restjson; public final class RestJsonProtocol { }",
            "package software.amazon.awssdk.awscore.operation; public final class AwsOperationDescriptor { }",
            "package software.amazon.awssdk.awscore.eventstream; public interface EventStreamResponseHandler<R, E> { }",
            "package com.fasterxml.jackson.databind; public class ObjectMapper { }");
        stubs = ClassPath.readClasses(stubRoot);
        List<Path> onStubs = List.of(stubRoot);

        service = ClassPath.readClasses(JavaFixtures.compile(work, "service", onStubs,
            "package fixture.gen; import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;"
            + " public final class FixtureOperations {"
            + " public static final AwsOperationDescriptor GET_THING = GetThingOperation.INSTANCE;"
            + " public static AwsOperationDescriptor operation(String id) {"
            + " return \"GetThing\".equals(id) ? GET_THING : null; } }",
            "package fixture.gen; import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;"
            + " final class GetThingOperation { static final AwsOperationDescriptor INSTANCE ="
            + " new AwsOperationDescriptor(); }",
            "package fixture.gen; import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;"
            + " final class OrphanOperation { static final AwsOperationDescriptor INSTANCE ="
            + " new AwsOperationDescriptor(); }",
            "package fixture.gen; public class FixtureClient { Object call() { return FixtureOperations.GET_THING; }"
            + " Object protocol() { return new software.amazon.awssdk.protocols.restjson.RestJsonProtocol(); } }",
            "package fixture.gen; public class GetThingResponse { Object id() {"
            + " return software.amazon.smithy.model.shapes.ShapeId.from(\"a#B\"); } }",
            // A generated model type named for its operation (V2's <Operation>Request), with the nested builder and
            // member reader of the model-type templates: not a per-operation transformer.
            "package fixture.gen; public final class GetThingRequest {"
            + " public interface Builder { GetThingRequest build(); }"
            + " static final class BuilderImpl implements Builder {"
            + " public GetThingRequest build() { return new GetThingRequest(); } }"
            + " private static final class MemberReader { } }",
            // V2's Event Stream callback interface (<Operation>ResponseHandler), which customers implement: not a
            // per-operation transformer.
            "package fixture.gen; public interface GetThingResponseHandler extends"
            + " software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler<GetThingResponse, Object> { }",
            "package fixture.gen; public class ReflectiveLookup {"
            + " interface Loader { Class<?> load(String name) throws ClassNotFoundException; }"
            + " Object byName() throws Exception { return Class.forName(\"fixture.gen.GetThingOperation\")"
            + ".getDeclaredField(\"INSTANCE\").get(null); }"
            + " Loader loader() { return Class::forName; } }",
            "package fixture.gen; public class LookupClient { Object find() {"
            + " return FixtureOperations.operation(\"GetThing\"); } }",
            // The service metadata's tooling lookup forwards to the table under the same name and descriptor: not a
            // violation itself, but generated callers of it are, and a forwarder under another name is one too.
            "package fixture.gen; import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;"
            + " public final class FixtureServiceMetadata {"
            + " public static AwsOperationDescriptor operation(String id) { return FixtureOperations.operation(id); }"
            + " public static AwsOperationDescriptor find(String id) { return FixtureOperations.operation(id); } }",
            "package fixture.gen; public class MetadataLookupClient { Object find() {"
            + " return FixtureServiceMetadata.operation(\"GetThing\"); } }",
            "package fixture.gen; public class DiscoveringClient { Object discover() {"
            + " return java.util.ServiceLoader.load(Runnable.class); } }",
            "package fixture.gen; public class ModelLoadingClient {"
            + " Object assemble() { return new software.amazon.smithy.model.loader.ModelAssembler(); }"
            + " Object parse() { return software.amazon.smithy.model.node.Node.parse(\"{}\"); }"
            + " Object resource() { return ModelLoadingClient.class.getResourceAsStream(\"model.json\"); } }",
            "package fixture.gen; public class GetThingRequestMarshaller"
            + " implements software.amazon.awssdk.core.runtime.transform.Marshaller<Object> { }",
            "package fixture.gen; public class GetThingSerializer { Object protocol() {"
            + " return new software.amazon.awssdk.protocols.restjson.RestJsonProtocol(); } }",
            "package fixture.gen.transform; public class Helper { }",
            "package fixture.gen; public class V2Fallback {"
            + " Object pipeline() { return new software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient(); }"
            + " Object http() { return new software.amazon.awssdk.http.SdkHttpFullRequest(); }"
            + " Object model() { return new software.amazon.awssdk.codegen.model.intermediate.IntermediateModel(); }"
            + " String sidecar() { return \"paginators-1.json\"; } }",
            "package fixture.gen; public class EmbeddedModel {"
            + " static final String MODEL = \"{\\\"smithy\\\": \\\"2.0\\\", \\\"shapes\\\": {}}\"; }"));

        runtime = ClassPath.readClasses(JavaFixtures.compile(work, "runtime", onStubs,
            "package fixture.rt; public class HttpDiscovery { static Object discover() {"
            + " return java.util.ServiceLoader.load(Runnable.class); } }",
            "package fixture.rt; public class Pipeline { Object execute() {"
            + " return java.util.ServiceLoader.load(Runnable.class); } }",
            "package fixture.rt; import java.lang.invoke.MethodHandles; import java.lang.invoke.VarHandle;"
            + " public class Atomics { private volatile int state; private static final VarHandle STATE;"
            + " static { try { STATE = MethodHandles.lookup().findVarHandle(Atomics.class, \"state\", int.class); }"
            + " catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); } } }",
            "package fixture.rt; public class LateHandles { int x; Object handle() throws ReflectiveOperationException"
            + " { return java.lang.invoke.MethodHandles.lookup().findVarHandle(LateHandles.class, \"x\", int.class);"
            + " } }",
            "package fixture.rt; public class VersionResources { Object read() throws java.io.IOException {"
            + " return VersionResources.class.getClassLoader().getResources(\"META-INF/aws-sdk-java-v3/modules/\");"
            + " } }",
            "package fixture.rt; public class CustomLoader extends ClassLoader {"
            + " Class<?> load(String name) throws ClassNotFoundException { return loadClass(name); } }",
            "package fixture.rt; public class ModelNames { String name() { return \"service.smithy\"; }"
            + " String path() { return \"META-INF/models/api.v2.smithy.json\"; }"
            + " String group() { return \"software.amazon.smithy\"; }"
            + " Object parse() { return software.amazon.smithy.model.node.Node.parse(\"[]\"); } }",
            "package fixture.rt; public class Proxying { Object proxy() {"
            + " return java.lang.reflect.Proxy.newProxyInstance(null, new Class<?>[0], (p, m, a) -> null); } }",
            "package fixture.rt; public class Switches {"
            + " int dense(int i) { switch (i) { case 0: case 1: case 2: case 3: return i;"
            + " default: return Integer.hashCode(i); } }"
            + " int sparse(int i) { switch (i) { case 1: case 1000: case 100000: return 1;"
            + " default: return Integer.signum(i); } } }"));

        dynamic = ClassPath.readClasses(JavaFixtures.compile(work, "dynamic", onStubs,
            "package fixture.dyn; public class DynamicModels {"
            + " Object load() { return new software.amazon.smithy.model.loader.ModelAssembler(); }"
            + " Object parse() { return software.amazon.smithy.model.node.Node.parse(\"{}\"); }"
            + " Class<?> reflect() throws Exception { return Class.forName(\"x\"); } }"));

        StringBuilder names = new StringBuilder();
        for (String legacy : GenerationPathInspector.LEGACY_INPUT_FILE_NAMES) {
            if (!legacy.equals("smoke.json")) {
                names.append(names.length() == 0 ? "" : ", ").append('"').append(legacy).append('"');
            }
        }
        Path codegenRoot = JavaFixtures.compile(work, "codegen", onStubs,
            "package fixture.codegen; import software.amazon.smithy.codegen.core.directed.DirectedCodegen;"
            + " public class MainGenerator implements DirectedCodegen<Object, Object, Object> { }",
            "package fixture.codegen; import software.amazon.smithy.codegen.core.directed.DirectedCodegen;"
            + " public abstract class AbstractGenerator implements DirectedCodegen<Object, Object, Object> { }",
            "package fixture.codegen; public class SecondGenerator extends AbstractGenerator { }",
            "package fixture.codegen; import software.amazon.smithy.codegen.core.directed.CodegenDirector;"
            + " public class Entry { public static void generate(String service) { new CodegenDirector().run(); }"
            + " public static void other() { } }",
            "package fixture.codegen; import software.amazon.smithy.codegen.core.directed.CodegenDirector;"
            + " public class OtherEntry { void run() { new CodegenDirector().run(); } }",
            "package fixture.codegen; public class CustomDirector"
            + " extends software.amazon.smithy.codegen.core.directed.CodegenDirector { }",
            "package fixture.codegen; public class BuildPlugin implements software.amazon.smithy.build.SmithyBuildPlugin"
            + " { }",
            "package fixture.codegen; public class Manifests { software.amazon.smithy.build.FileManifest manifest; }",
            "package fixture.codegen; public class Writer { }",
            "package fixture.codegen; public class UsesSmithyJava { Object plugin() {"
            + " return new software.amazon.smithy.java.codegen.JavaCodegenPlugin(); } }",
            "package fixture.codegen; public class Cli { public static void main(String[] args) { } }",
            "package fixture.codegen; public class ServiceIntermediateModel { }",
            "package fixture.codegen; public class ModelCache { static software.amazon.smithy.model.Model cached;"
            + " static java.util.Map<software.amazon.smithy.model.shapes.ShapeId, String> names; }",
            "package fixture.codegen; public class Dumper {"
            + " Object serializer() { return new software.amazon.smithy.model.shapes.ModelSerializer(); }"
            + " void write(java.io.OutputStream out) throws java.io.IOException {"
            + " new java.io.ObjectOutputStream(out).writeObject(\"x\"); } }",
            "package fixture.codegen; public class SerializablePlan implements java.io.Serializable { }",
            "package fixture.codegen; public class SidecarReader {"
            + " Object read() { return software.amazon.smithy.model.node.Node.parse(\"service-2.json\"); }"
            + " Object mapper() { return new com.fasterxml.jackson.databind.ObjectMapper(); } }",
            "package fixture.codegen.validation; import software.amazon.smithy.model.node.Node;"
            + " public final class Classifier { static final java.util.List<String> NAMES = java.util.List.of("
            + names + "); public Node parsed;"
            + " Object generator() { return new fixture.codegen.Entry(); }"
            + " void write(java.nio.file.Path p) throws java.io.IOException {"
            + " java.nio.file.Files.writeString(p, \"x\"); }"
            + " private Object classify(String json) { return Node.parse(json); } }");
        codegen = ClassPath.readClasses(codegenRoot);

        plugin = ClassPath.readClasses(JavaFixtures.compile(work, "plugin", List.of(stubRoot, codegenRoot),
            "package fixture.plugin; public class Mojo { void execute() { fixture.codegen.Entry.generate(\"x\"); } }",
            "package fixture.plugin; public class LeakyMojo { void execute() throws java.io.IOException {"
            + " fixture.codegen.Entry.other(); Object writer = new fixture.codegen.Writer();"
            + " Object model = new software.amazon.smithy.model.Model();"
            + " java.nio.file.Files.createDirectories(java.nio.file.Path.of(\"out\")); } }"));
    }

    @Test
    void generatedCodeViolationsAreDetected() {
        assertEquals(sortedList(
            "fixture.gen.DiscoveringClient#discover service-discovery",
            "fixture.gen.EmbeddedModel serialized-semantic-representation",
            "fixture.gen.GetThingRequestMarshaller per-operation-transformer",
            "fixture.gen.GetThingRequestMarshaller per-operation-transformer",
            "fixture.gen.GetThingRequestMarshaller v2-protocol-marshaller",
            "fixture.gen.GetThingSerializer per-operation-protocol-coupling",
            "fixture.gen.GetThingSerializer per-operation-transformer",
            "fixture.gen.FixtureServiceMetadata#find runtime-operation-lookup",
            "fixture.gen.LookupClient#find runtime-operation-lookup",
            "fixture.gen.MetadataLookupClient#find runtime-operation-lookup",
            "fixture.gen.ModelLoadingClient runtime-model-loading",
            "fixture.gen.ModelLoadingClient#parse runtime-model-loading",
            "fixture.gen.ModelLoadingClient#resource runtime-model-loading",
            "fixture.gen.OrphanOperation non-static-descriptor-reference",
            "fixture.gen.ReflectiveLookup#byName class-name-lookup",
            "fixture.gen.ReflectiveLookup#byName reflective-member-access",
            "fixture.gen.ReflectiveLookup#byName reflective-member-access",
            "fixture.gen.ReflectiveLookup#loader class-name-lookup",
            "fixture.gen.V2Fallback v2-codegen-model",
            "fixture.gen.V2Fallback v2-execution-pipeline",
            "fixture.gen.V2Fallback v2-http-adapter",
            "fixture.gen.V2Fallback legacy-input-file-name",
            "fixture.gen.transform.Helper per-operation-transformer"),
            summary(inspect("fixture-service", Scope.GENERATED, service)));
    }

    @Test
    void diagnosticsNameModuleLocationRuleDetailAndRequirements() {
        assertTrue(inspect("fixture-service", Scope.GENERATED, service).stream().map(Violation::toString).toList()
                          .contains("fixture-service: fixture.gen.ReflectiveLookup#loader: class-name-lookup: uses "
                                    + "java.lang.Class.forName(Ljava/lang/String;)Ljava/lang/Class; (Requirements 3.9, "
                                    + "10.12, 20.22)"));
    }

    @Test
    void runtimeViolationsAreDetectedAndAllowancesApplyToOneMethod() {
        List<Violation> all = inspect("fixture-runtime", Scope.RUNTIME, runtime);
        Set<Allowance> used = new HashSet<>();
        List<Violation> remaining = GenerationPathInspector.withoutAllowed(
            all, List.of(DISCOVERY_ALLOWANCE, STALE_ALLOWANCE), used);
        assertEquals(sortedList(
            "fixture.rt.CustomLoader#load class-name-lookup",
            "fixture.rt.LateHandles#handle reflective-member-access",
            "fixture.rt.ModelNames runtime-model-loading",
            "fixture.rt.ModelNames runtime-model-loading",
            "fixture.rt.ModelNames#parse runtime-model-loading",
            "fixture.rt.Pipeline#execute service-discovery",
            "fixture.rt.Proxying#proxy reflective-member-access"),
            summary(remaining));
        assertEquals(Set.of(DISCOVERY_ALLOWANCE), used, "the stale allowance covers nothing");
        assertTrue(summary(all).contains("fixture.rt.HttpDiscovery#discover service-discovery"));
    }

    @Test
    void dynamicModulesMayLoadModelsButNotLookUpClassesByName() {
        assertEquals(sortedList("fixture.dyn.DynamicModels#reflect class-name-lookup"),
                     summary(inspect("fixture-dynamic", Scope.DYNAMIC, dynamic)));
    }

    @Test
    void codegenViolationsAreDetectedAndTheClassifierIsExemptOnlyFromNamingAndParsingLegacyInputs() {
        assertEquals(sortedList(
            "fixture.codegen.BuildPlugin smithy-build-plugin",
            "fixture.codegen.Cli#main codegen-main-entry-point",
            "fixture.codegen.Dumper model-serialization",
            "fixture.codegen.Dumper model-serialization",
            "fixture.codegen.ModelCache#cached static-semantic-model-state",
            "fixture.codegen.ServiceIntermediateModel intermediate-model-type",
            "fixture.codegen.SidecarReader legacy-input-file-name",
            "fixture.codegen.SidecarReader legacy-input-parsing",
            "fixture.codegen.SidecarReader#read legacy-input-parsing",
            "fixture.codegen.UsesSmithyJava second-generator-toolchain",
            "fixture.codegen.validation.* legacy-classifier-purity",
            "fixture.codegen.validation.Classifier legacy-classifier-purity",
            "fixture.codegen.validation.Classifier#parsed legacy-classifier-purity",
            "fixture.codegen.validation.Classifier#write legacy-classifier-purity"),
            summary(inspect("fixture-codegen", Scope.CODEGEN, codegen)));
        assertTrue(inspect("fixture-codegen", Scope.CODEGEN, codegen).stream().map(Violation::toString)
                       .anyMatch(v -> v.contains("does not recognize smoke.json")));
    }

    @Test
    void thePluginIsALifecycleWrapperOnly() {
        assertEquals(sortedList(
            "fixture.plugin.LeakyMojo plugin-emitter",
            "fixture.plugin.LeakyMojo plugin-emitter",
            "fixture.plugin.LeakyMojo#execute plugin-emitter"),
            summary(inspect("fixture-plugin", Scope.CODEGEN, plugin)));
    }

    @Test
    void generatorUniquenessIsCheckedAcrossModules() {
        Map<String, Map<String, ClassFile>> modules = new TreeMap<>();
        modules.put("fixture-codegen", codegen);
        modules.put("fixture-plugin", plugin);
        modules.put("fixture-stubs", stubs);
        List<Violation> violations = GenerationPathInspector.inspectGenerators(modules, LAYOUT);
        assertEquals(sortedList(
            "fixture.codegen.AbstractGenerator single-directed-codegen",
            "fixture.codegen.CustomDirector single-codegen-director",
            "fixture.codegen.CustomDirector#<init> single-codegen-director",
            "fixture.codegen.OtherEntry#run single-codegen-director",
            "fixture.codegen.OtherEntry#run single-codegen-director",
            "fixture.codegen.SecondGenerator single-directed-codegen",
            "fixture.plugin.LeakyMojo#execute generator-entry-point",
            "software.amazon.awssdk.codegen.model.intermediate.IntermediateModel v2-marker-defined-by-v3",
            "software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient v2-marker-defined-by-v3",
            "software.amazon.awssdk.core.runtime.transform.Marshaller v2-marker-defined-by-v3",
            "software.amazon.awssdk.http.SdkHttpFullRequest v2-marker-defined-by-v3"),
            summary(violations));
    }

    @Test
    void aMissingGeneratorIsReported() {
        List<Violation> violations = GenerationPathInspector.inspectGenerators(Map.of("fixture-plugin", plugin),
                                                                               LAYOUT);
        assertTrue(summary(violations).containsAll(List.of(
            "fixture.codegen.MainGenerator single-directed-codegen",
            "fixture.codegen.Entry#generate single-codegen-director")), summary(violations).toString());
    }

    @Test
    void serializedSemanticRepresentationsAreDetectedInResourceTrees() throws IOException {
        Path root = Files.createDirectories(work.resolve("resources"));
        String ast = "{\"smithy\": \"2.0\", \"shapes\": {}}";
        write(root, "model.smithy", "namespace example");
        write(root, "notes/embedded.txt", "// copy\n$version: \"2\"\nnamespace example.foo\n\nstructure A {}\n");
        write(root, "ast.json", ast);
        write(root, "nested/bundle.json",
              "{\"config\": {}, \"model\": {\"smithy\": \"2.0\", \"shapes\": {\"a#B\": {\"type\": \"string\"}}}}");
        write(root, "c2j.json", "{\"metadata\": {\"protocol\": \"json\"}, \"operations\": {}}");
        write(root, "docs.json", "{\"service\": \"x\", \"operations\": {}, \"shapes\": {}}");
        write(root, "ir.json", "{\"shapes\": {\"Thing\": {\"type\": \"structure\", \"members\": {}}}}");
        write(root, "graph.json", "[{\"shapes\": [{\"type\": \"structure\", \"members\": {\"a\": {}}}]}]");
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(serialized)) {
            out.writeObject(List.of("a#B"));
        }
        Files.write(root.resolve("object.ser"), serialized.toByteArray());
        ByteArrayOutputStream gzip = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(gzip)) {
            out.write(ast.getBytes(StandardCharsets.UTF_8));
        }
        Files.write(root.resolve("ast.json.gz"), gzip.toByteArray());
        write(root, GenerationPathInspector.SMITHY_BUILD_PLUGIN_SERVICE, "example.Plugin");
        // Accepted: inventories, manifests, properties, invalid JSON, binary data, class files.
        write(root, "META-INF/aws-sdk-java-v3/model-provenance.json",
              "{\"serviceShapeId\": \"a#B\", \"modelRevision\": \"0123456789abcdef\", \"shapes\": [\"a#B\", \"a#C\"],"
              + " \"files\": [{\"path\": \"a/B.java\", \"sha256\": \"00\"}]}");
        write(root, "inventory.json", "{\"shapes\": {\"a#B\": \"structure\"}, \"operations\": [{\"id\": \"a#Op\","
                                      + " \"type\": \"operation\"}]}");
        write(root, "inventory-list.json", "{\"shapes\": [{\"id\": \"a#B\", \"type\": \"structure\"}]}");
        write(root, "META-INF/aws-sdk-java-v3/modules/fixture.properties", "version=3.0.0");
        write(root, "broken.json", "{not json");
        write(root, "META-INF/services/java.lang.Runnable", "example.Task");
        Files.write(root.resolve("binary.bin"), new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 1, 2});
        Files.write(root.resolve("Some.class"), new byte[] {(byte) 0xAC, (byte) 0xED, 0, 5});

        List<Violation> violations = GenerationPathInspector.inspectResources("fixture", root);
        assertEquals(sortedList(
            "resource META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin: smithy-build-plugin: "
            + "registers a smithy-build plugin",
            "resource ast.json.gz: serialized-semantic-representation: is a gzip-compressed Smithy JSON AST model",
            "resource ast.json: serialized-semantic-representation: is a Smithy JSON AST model",
            "resource c2j.json: serialized-semantic-representation: is a C2J service model (or V2 intermediate model)",
            "resource docs.json: serialized-semantic-representation: is a C2J documentation model",
            "resource graph.json: serialized-semantic-representation: is a serialized shape graph",
            "resource ir.json: serialized-semantic-representation: is a serialized shape graph",
            "resource model.smithy: serialized-semantic-representation: is a Smithy IDL model file",
            "resource nested/bundle.json: serialized-semantic-representation: is a Smithy JSON AST model",
            "resource notes/embedded.txt: serialized-semantic-representation: is a Smithy IDL model",
            "resource object.ser: serialized-semantic-representation: is a Java object-serialization stream"),
            violations.stream().map(v -> v.location() + ": " + v.rule().id() + ": " + v.detail()).sorted().toList());
    }

    @Test
    void generatedSourceTreesAreCheckedForTransformersAndSerializedModels() throws IOException {
        Path root = Files.createDirectories(work.resolve("generated-sources"));
        write(root, "fixture/gen/GetThingRequestMarshaller.java", "class GetThingRequestMarshaller { }");
        write(root, "fixture/gen/GetThingDeserializer.java", "class GetThingDeserializer { }");
        write(root, "fixture/gen/transform/Helper.java", "class Helper { }");
        // Generated model types named for their operation (V2's <Operation>Request/Response) are not transformers.
        write(root, "fixture/gen/GetThingResponse.java", "class GetThingResponse { }");
        write(root, "fixture/gen/GetThingRequest.java", "class GetThingRequest { }");
        write(root, "fixture/gen/GetThingerClient.java", "class GetThingerClient { }");
        // V2's Event Stream callback interface is not a transformer; a class with its name is.
        write(root, "fixture/gen/GetThingResponseHandler.java",
              "package fixture.gen;\n\nimport software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;"
              + "\n\npublic interface GetThingResponseHandler extends EventStreamResponseHandler<GetThingResponse, "
              + "Object> {\n}\n");
        write(root, "fixture/gen/other/GetThingResponseHandler.java",
              "package fixture.gen.other;\n\nclass GetThingResponseHandler { }\n");
        write(root, "META-INF/model.json", "{\"smithy\": \"2.0\", \"shapes\": {}}");
        write(root, "fixture/gen/service.smithy", "namespace fixture");
        List<Violation> violations = GenerationPathInspector.inspectGeneratedSources("fixture", root,
                                                                                     Set.of("GetThing"));
        assertEquals(sortedList(
            "resource META-INF/model.json serialized-semantic-representation",
            "resource fixture/gen/service.smithy serialized-semantic-representation",
            "source fixture/gen/GetThingDeserializer.java per-operation-transformer",
            "source fixture/gen/GetThingRequestMarshaller.java per-operation-transformer",
            "source fixture/gen/GetThingRequestMarshaller.java per-operation-transformer",
            "source fixture/gen/other/GetThingResponseHandler.java per-operation-transformer",
            "source fixture/gen/transform/Helper.java per-operation-transformer"),
            summary(violations));
    }

    @Test
    void v2MarkersAreExactAndDoNotMatchV3Packages() {
        assertEquals(GenerationPathRule.V2_CODEGEN_MODEL,
                     GenerationPathInspector.v2Marker("software.amazon.awssdk.codegen.C2jModels").orElseThrow());
        assertEquals(GenerationPathRule.V2_HTTP_ADAPTER,
                     GenerationPathInspector.v2Marker("software.amazon.awssdk.http.SdkHttpFullRequest$Builder")
                                            .orElseThrow());
        for (String v3 : List.of("software.amazon.awssdk.awscodegen.AwsCodegen",
                                 "software.amazon.awssdk.test.codegen.lifecycle.GeneratedServiceInfo",
                                 "software.amazon.awssdk.http.SdkHttpRequest",
                                 "software.amazon.awssdk.protocols.core.ProtocolSelection",
                                 "software.amazon.awssdk.protocols.restjson.RestJsonProtocol",
                                 "software.amazon.awssdk.protocols.rpcv2cbor.RpcV2CborProtocol",
                                 "software.amazon.awssdk.pipeline.AwsCallEngine",
                                 "software.amazon.awssdk.core.SdkBytes")) {
            assertTrue(GenerationPathInspector.v2Marker(v3).isEmpty(), v3);
        }
    }

    @Test
    void classFileReaderAttributesMemberReferencesToMethods() {
        ClassFile lookup = service.get("fixture.gen.ReflectiveLookup");
        MemberRef forName = new MemberRef("java.lang.Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        assertTrue(method(lookup, "loader").memberReferences().contains(forName),
                   "a method reference is attributed through its invokedynamic bootstrap arguments");
        assertTrue(method(lookup, "byName").memberReferences().contains(forName));
        assertTrue(lookup.memberReferences().contains(forName));
        assertTrue(lookup.stringConstants().containsAll(Set.of("fixture.gen.GetThingOperation", "INSTANCE")));
        ClassFile switches = runtime.get("fixture.rt.Switches");
        assertTrue(method(switches, "dense").memberReferences()
                       .contains(new MemberRef("java.lang.Integer", "hashCode", "(I)I")), "after a tableswitch");
        assertTrue(method(switches, "sparse").memberReferences()
                       .contains(new MemberRef("java.lang.Integer", "signum", "(I)I")), "after a lookupswitch");
        assertFalse(method(switches, "dense").memberReferences()
                        .contains(new MemberRef("java.lang.Integer", "signum", "(I)I")));
        assertTrue(method(runtime.get("fixture.rt.Atomics"), "<clinit>").memberReferences()
                       .contains(new MemberRef("java.lang.invoke.MethodHandles$Lookup", "findVarHandle",
                                               "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)"
                                               + "Ljava/lang/invoke/VarHandle;")));
    }

    @Test
    void everyRuleIsTriggeredByAFixture() {
        Set<GenerationPathRule> triggered = EnumSet.noneOf(GenerationPathRule.class);
        triggered.addAll(rules(inspect("fixture-service", Scope.GENERATED, service)));
        triggered.addAll(rules(inspect("fixture-runtime", Scope.RUNTIME, runtime)));
        triggered.addAll(rules(inspect("fixture-dynamic", Scope.DYNAMIC, dynamic)));
        triggered.addAll(rules(inspect("fixture-codegen", Scope.CODEGEN, codegen)));
        triggered.addAll(rules(inspect("fixture-plugin", Scope.CODEGEN, plugin)));
        Map<String, Map<String, ClassFile>> modules = new TreeMap<>(Map.of("fixture-codegen", codegen,
                                                                           "fixture-plugin", plugin,
                                                                           "fixture-stubs", stubs));
        triggered.addAll(rules(GenerationPathInspector.inspectGenerators(modules, LAYOUT)));
        Set<GenerationPathRule> untriggered = EnumSet.allOf(GenerationPathRule.class);
        untriggered.removeAll(triggered);
        assertEquals(Set.of(), untriggered, "Rules without a negative fixture");
    }

    @Test
    void theToolingScopeIsOnlyCheckedForASecondGenerator() {
        assertEquals(sortedList("fixture.codegen.BuildPlugin smithy-build-plugin",
                             "fixture.codegen.UsesSmithyJava second-generator-toolchain"),
                     summary(inspect("fixture-tool", Scope.TOOLING, codegen)));
    }

    private static List<Violation> inspect(String module, Scope scope, Map<String, ClassFile> classes) {
        return GenerationPathInspector.inspectModule(module, scope, classes, LAYOUT);
    }

    /** {@code location rule} per violation, sorted. */
    private static List<String> summary(List<Violation> violations) {
        List<String> result = new ArrayList<>();
        violations.forEach(v -> result.add(v.location() + " " + v.rule().id()));
        return result.stream().sorted().toList();
    }

    private static List<String> sortedList(String... values) {
        return List.of(values).stream().sorted().toList();
    }

    private static Set<GenerationPathRule> rules(List<Violation> violations) {
        Set<GenerationPathRule> result = EnumSet.noneOf(GenerationPathRule.class);
        violations.forEach(v -> result.add(v.rule()));
        return result;
    }

    private static Member method(ClassFile type, String name) {
        return type.methods().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    private static void write(Path root, String relative, String content) throws IOException {
        MavenFork.write(root.resolve(relative), content);
    }
}
