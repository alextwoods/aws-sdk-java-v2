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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;
import software.amazon.awssdk.test.architecture.DependencyTree.Node;

/**
 * Runtime side of the dynamic-layer architecture (task 12.4; Requirements 10.1, 10.4, 10.5, 14.14, 14.18, 15.5).
 * Probes compiled from {@code src/test/resources/dynamic-isolation} run the generated codegen-lifecycle fixture
 * client (and, for the shared-pipeline check, dynamic clients of the same model) in fresh class loaders whose class
 * path is exactly a resolved runtime dependency tree, against an in-memory transport. The loaders record every class
 * name requested from them, including lookups that fail, so a generated client that loads (or merely probes for) a
 * Smithy model parser or dynamic-layer class is detected whether or not the dynamic layer is installed.
 * <ul>
 *   <li>Without the dynamic layer on the class path, the generated client works and requests no parser or dynamic
 *       class; the dynamic layer is provably absent.</li>
 *   <li>With dynamic-client's whole runtime tree added, the generated client behaves identically and still requests
 *       none of them (no discovery or registration pulls the dynamic layer in).</li>
 *   <li>A negative-control probe that only looks a dynamic-model type up is reported in both configurations.</li>
 *   <li>Generated and dynamic sync calls, and generated and dynamic async calls, of the same operation run the same
 *       interceptor hooks, each dispatched from the same aws-pipeline engine and stage frames.</li>
 * </ul>
 * {@link PipelineArchitectureTest} and {@link BytecodePurityTest} prove the same properties over bytecode.
 */
class DynamicIsolationRuntimeTest {

    private static final String GENERATED_FIXTURE = "codegen-lifecycle-tests";
    private static final String DYNAMIC_CLIENT = "dynamic-client";
    private static final String DYNAMIC_PACKAGE = "software.amazon.awssdk.dynamic.";
    private static final String DYNAMIC_MODEL_CLASS = DYNAMIC_PACKAGE + "model.DynamicModel";
    private static final String PING_REQUEST = "requests=[POST CodegenLifecycleFixture.Ping]";

    @TempDir
    static Path work;

    private static V3Reactor reactor;
    private static DependencyPolicy policy;
    private static List<Path> serviceClassPath;
    private static List<Path> withDynamicClassPath;
    private static Path serviceProbes;
    private static Path dynamicProbes;
    private static String isolatedTranscript;

    @BeforeAll
    static void compileProbes() throws IOException {
        ResolvedDependencyTrees trees = ResolvedDependencyTrees.load();
        reactor = trees.reactor();
        policy = trees.policy();
        // The generated classes and the runtime trees of a generated awsJson1_0 service: the fixture module's own
        // tree (it declares no protocol module, because its tests never invoke the client) plus the minimal
        // aws-json-protocol service's tree, which is exactly the documented service runtime set.
        Set<Path> service = new LinkedHashSet<>();
        service.add(reactor.compiledClasses(GENERATED_FIXTURE));
        for (String module : serviceTreeModules()) {
            service.addAll(trees.runtimeClassPathRoots(module).values());
        }
        serviceClassPath = List.copyOf(service);
        Set<Path> withDynamic = new LinkedHashSet<>(service);
        withDynamic.add(reactor.compiledClasses(DYNAMIC_CLIENT));
        withDynamic.addAll(trees.runtimeClassPathRoots(DYNAMIC_CLIENT).values());
        withDynamicClassPath = List.copyOf(withDynamic);

        serviceProbes = JavaFixtures.compile(work, "service-probes", serviceClassPath,
                                             probe("ProbeSupport"), probe("GeneratedProbe"),
                                             probe("LeakyGeneratedProbe"));
        dynamicProbes = JavaFixtures.compile(work, "dynamic-probes", withDynamicClassPath,
                                             probe("ProbeSupport"), probe("SharedPipelineProbe"));
        try (RecordingClassLoader loader = loader(serviceProbes, serviceClassPath)) {
            isolatedTranscript = loader.run("probe.GeneratedProbe", "ping");
        }
    }

    @Test
    void theServiceClassPathHoldsNoDynamicOrModelParserArtifact() {
        // Precondition of the isolation checks: only the waived smithy-model jar (whose parser classes the boundary
        // forbids) belongs to the smithy-model-parser artifact class, and no dynamic module is present.
        Set<String> parserArtifacts = new TreeSet<>();
        for (String module : serviceTreeModules()) {
            for (Node node : ResolvedDependencyTrees.load().tree(module).resolvedArtifacts()) {
                Coordinate c = node.coordinate();
                if (policy.matches("smithy-model-parser", c)) {
                    parserArtifacts.add(c.groupId() + ":" + c.artifactId());
                }
            }
        }
        assertEquals(Set.of("software.amazon.smithy:smithy-model"), parserArtifacts);
        assertFalse(serviceClassPath.contains(reactor.compiledClasses(DYNAMIC_CLIENT)));
        assertFalse(serviceClassPath.contains(reactor.compiledClasses("dynamic-model")));
        assertTrue(withDynamicClassPath.contains(reactor.compiledClasses("dynamic-model")),
                   "dynamic-client's runtime tree must contain dynamic-model");
    }

    @Test
    void generatedClientWorksWithoutTheDynamicLayerAndRequestsNoneOfIt() {
        assertTrue(isolatedTranscript.startsWith("reply=pong\n" + PING_REQUEST + "\n"), isolatedTranscript);
        try (RecordingClassLoader loader = loader(serviceProbes, serviceClassPath)) {
            assertEquals(isolatedTranscript, loader.run("probe.GeneratedProbe", "ping"));
            Set<String> requested = loader.requested();
            assertRecordedTheGeneratedCall(requested);
            assertEquals(Set.of(), forbidden(requested),
                         "A generated client requested Smithy model parser or dynamic-layer classes");
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass(DYNAMIC_MODEL_CLASS));
            assertThrows(ClassNotFoundException.class,
                         () -> loader.loadClass("software.amazon.smithy.java.dynamicschemas.SchemaConverter"));
        }
    }

    @Test
    void generatedClientRequestsNoDynamicClassWhenTheDynamicLayerIsInstalled() throws ClassNotFoundException {
        try (RecordingClassLoader loader = loader(serviceProbes, withDynamicClassPath)) {
            assertEquals(isolatedTranscript, loader.run("probe.GeneratedProbe", "ping"),
                         "Installing the dynamic layer changed a generated call");
            Set<String> requested = loader.requested();
            assertRecordedTheGeneratedCall(requested);
            assertEquals(Set.of(), forbidden(requested),
                         "Installing the dynamic layer made a generated client request its classes");
            // The layer was installed: it resolves once something asks for it.
            assertEquals(DYNAMIC_MODEL_CLASS, loader.loadClass(DYNAMIC_MODEL_CLASS).getName());
        }
    }

    @Test
    void aLookupOfTheDynamicLayerIsDetectedWithAndWithoutIt() {
        // Negative control for the two checks above: the recording sees a non-initializing optional lookup, both
        // when it fails (layer absent) and when it succeeds (layer present).
        for (List<Path> classPath : List.of(serviceClassPath, withDynamicClassPath)) {
            try (RecordingClassLoader loader = loader(serviceProbes, classPath)) {
                assertEquals(isolatedTranscript, loader.run("probe.LeakyGeneratedProbe", "ping"));
                assertEquals(Set.of(DYNAMIC_MODEL_CLASS), forbidden(loader.requested()));
            }
        }
    }

    @Test
    void dynamicAndGeneratedCallsTraverseTheSameEngineStages() {
        Path model = reactor.moduleDirectory(GENERATED_FIXTURE)
                            .resolve("src/main/smithy/codegen-lifecycle-fixture.smithy");
        assertTrue(Files.isRegularFile(model), model.toString());
        Map<String, List<String>> sections;
        try (RecordingClassLoader loader = loader(dynamicProbes, withDynamicClassPath)) {
            sections = sections(loader.run("probe.SharedPipelineProbe", model.toString()));
        }
        assertEquals(List.of("generated-sync", "dynamic-sync", "generated-async", "dynamic-async"),
                     List.copyOf(sections.keySet()));
        for (String mode : List.of("sync", "async")) {
            List<String> generated = sections.get("generated-" + mode);
            assertEquals(List.of("reply=pong", PING_REQUEST), generated.subList(0, 2), String.join("\n", generated));
            // Every lifecycle boundary of a signed, successful single-attempt call ran, through the engine driver.
            List<String> hooks = generated.stream().skip(2).map(e -> e.substring(0, e.indexOf(' '))).toList();
            assertEquals(List.of("beforeExecution", "modifyRequest", "beforeAttempt", "afterSerialization",
                                 "modifyBeforeSigning", "modifyAfterSigning", "beforeTransmission",
                                 "afterTransmission", "modifyHttpResponse", "modifyResponseBody",
                                 "beforeDeserialization", "afterAttempt", "modifyResponse", "afterExecution"),
                         hooks, mode);
            String driver = "AwsCallEngine#" + (mode.equals("sync") ? "executeSync" : "executeAsync");
            assertTrue(generated.get(2).startsWith("beforeExecution <- ") && generated.get(2).contains(driver),
                       generated.get(2));
            // The endpoint, auth-scheme and identity stages ran before signing and are visible to later hooks.
            String signed = generated.stream().filter(e -> e.startsWith("modifyAfterSigning ")).findFirst()
                                     .orElseThrow();
            assertTrue(signed.contains("[attempt=1 endpoint=https://fixture.test") && signed.contains("auth=aws.auth#sigv4")
                       && !signed.contains("identity=-]"), signed);
            assertEquals(generated, sections.get("dynamic-" + mode),
                         "Generated and dynamic " + mode + " calls of the same operation ran different engine stages");
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /** The modules whose runtime trees make up the generated service's class path. */
    private static List<String> serviceTreeModules() {
        return List.of(GENERATED_FIXTURE, PurityFixtures.minimalService("aws-json-protocol"));
    }

    /** Requested classes that the generated-client path must never resolve. */
    private static Set<String> forbidden(Set<String> requested) {
        Set<String> result = new TreeSet<>();
        requested.stream()
                 .filter(name -> SmithyRuntimeBoundary.isForbiddenRuntimeClass(name) || name.startsWith(DYNAMIC_PACKAGE))
                 .forEach(result::add);
        return result;
    }

    /** The recording is meaningful only if the call's own types were resolved through the isolated loader. */
    private static void assertRecordedTheGeneratedCall(Set<String> requested) {
        assertTrue(requested.containsAll(Set.of(
                       "probe.GeneratedProbe",
                       "software.amazon.awssdk.test.codegen.lifecycle.DefaultCodegenLifecycleFixtureClient",
                       "software.amazon.awssdk.pipeline.AwsCallEngine",
                       "software.amazon.awssdk.pipeline.client.AwsClientBuildState",
                       "software.amazon.smithy.java.core.schema.Schema",
                       "software.amazon.smithy.model.shapes.ShapeId")),
                   requested.toString());
    }

    /** {@code ## name} headed sections of a probe transcript. */
    private static Map<String, List<String>> sections(String transcript) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        List<String> current = null;
        for (String line : transcript.split("\n")) {
            if (line.startsWith("## ")) {
                current = new ArrayList<>();
                result.put(line.substring(3), current);
            } else {
                current.add(line);
            }
        }
        return result;
    }

    private static String probe(String name) {
        try (InputStream in = DynamicIsolationRuntimeTest.class.getResourceAsStream("/dynamic-isolation/" + name
                                                                                   + ".java")) {
            assertTrue(in != null, "Missing probe source " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static RecordingClassLoader loader(Path probes, List<Path> classPath) {
        List<Path> roots = new ArrayList<>();
        roots.add(probes);
        roots.addAll(classPath);
        return new RecordingClassLoader(roots);
    }

    /**
     * A class loader over exactly {@code roots}, above the platform loader (so no test-class-path class is visible),
     * that records every class name requested from it, whether or not the class exists.
     */
    private static final class RecordingClassLoader extends URLClassLoader {
        private final Set<String> requested = ConcurrentHashMap.newKeySet();

        RecordingClassLoader(List<Path> roots) {
            super("dynamic-isolation", urls(roots), ClassLoader.getPlatformClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            requested.add(name);
            return super.loadClass(name, resolve);
        }

        Set<String> requested() {
            return new TreeSet<>(requested);
        }

        /** Runs the probe {@code className} (a {@code Function<String, String>}) with this loader as context loader. */
        @SuppressWarnings("unchecked")
        String run(String className, String argument) {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(this);
            try {
                Function<String, String> probe = (Function<String, String>) Class.forName(className, true, this)
                                                                                   .getConstructor().newInstance();
                return probe.apply(argument);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Cannot run probe " + className, e);
            } finally {
                thread.setContextClassLoader(previous);
            }
        }

        @Override
        public void close() {
            try {
                super.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static URL[] urls(List<Path> roots) {
            URL[] urls = new URL[roots.size()];
            for (int i = 0; i < urls.length; i++) {
                try {
                    urls[i] = roots.get(i).toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(roots.get(i).toString(), e);
                }
            }
            return urls;
        }
    }
}
