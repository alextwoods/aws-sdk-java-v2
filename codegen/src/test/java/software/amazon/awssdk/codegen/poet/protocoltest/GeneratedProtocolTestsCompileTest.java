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

package software.amazon.awssdk.codegen.poet.protocoltest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline;

/**
 * Generates the {@code demo.smithy} client and its protocol tests through the Codegen_Pipeline with
 * {@code "protocolTests": true}, compiles all of it with {@code --release 8} against the test class path (which holds
 * the SDK runtime and {@code smithy-protocol-tests-core}), and runs generated tests against the generated client through
 * {@link HttpClientHarnessFactory}.
 */
class GeneratedProtocolTestsCompileTest {
    private static final String DEMO = "software.amazon.awssdk.services.protodemo.protocoltests.";

    @TempDir
    static Path dir;

    private static URLClassLoader classes;

    @BeforeAll
    static void generateAndCompile() throws IOException {
        Path demo = generate(ProtocolTestGeneratorTest.DEMO);
        Path streaming = generate(ProtocolTestGeneratorTest.STREAMING);

        Path output = dir.resolve("classes");
        Files.createDirectories(output);
        List<String> args = new ArrayList<>(Arrays.asList("--release", "8", "-proc:none", "-nowarn", "-encoding", "UTF-8",
                                                          "-classpath", System.getProperty("java.class.path"),
                                                          "-d", output.toString()));
        for (Path service : Arrays.asList(demo, streaming)) {
            args.addAll(javaFiles(service.resolve("out/src")));
            args.addAll(javaFiles(service.resolve("out/tests")));
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        int result = compiler.run(null, log, log, args.toArray(new String[0]));
        assertThat(result).as("javac output:%n%s", new String(log.toByteArray(), StandardCharsets.UTF_8)).isZero();
        classes = new URLClassLoader(new URL[] {output.toUri().toURL(),
                                                demo.resolve("out/resources").toUri().toURL(),
                                                streaming.resolve("out/resources").toUri().toURL()},
                                     GeneratedProtocolTestsCompileTest.class.getClassLoader());
    }

    /**
     * Runs the Codegen_Pipeline for a fixture with protocol tests on and the event stream case skipped.
     */
    private static Path generate(String fixture) throws IOException {
        Path service = dir.resolve(fixture.replace(".smithy", ""));
        Files.createDirectories(service);
        Path skips = Files.write(service.resolve("protocol-test-skips.json"),
                                 (ProtocolTestGeneratorTest.DEMO.equals(fixture) ? ProtocolTestGeneratorTest.SKIP_EVENT_STREAM
                                                                                 : "[]").getBytes(StandardCharsets.UTF_8));
        CustomizationConfig config = CustomizationConfig.create();
        config.setProtocolTests(true);
        config.setProtocolTestSkips(skips.getFileName().toString());
        config.resolveProtocolTestSkips(service);
        CodegenPipeline.create().run(ProtocolTestGeneratorTest.inputs(service, ProtocolTestGeneratorTest.resource(fixture),
                                                                       config).build());
        return service;
    }

    private static List<String> javaFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return new ArrayList<>();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".java")).map(Path::toString).collect(Collectors.toList());
        }
    }

    @Test
    void generatedTestClassesAreWrittenToTheTestsDirectory() throws IOException {
        assertThat(javaFiles(dir.resolve("demo/out/tests")))
            .extracting(p -> new File(p).getName())
            .containsExactlyInAnyOrder("ProtoDemoAwsJson10RequestTests1.java", "ProtoDemoAwsJson10ResponseTests1.java",
                                       "ProtoDemoRestJson1RequestTests1.java");
        assertThat(javaFiles(dir.resolve("demo/out/src"))).noneMatch(p -> p.contains("protocoltests"));
    }

    @Test
    void generatedTestsRunAgainstTheGeneratedClient() throws Exception {
        String stream = "software.amazon.awssdk.services.streamdemo.protocoltests.";
        Map<String, TestExecutionResult> results = run(
            DEMO + "ProtoDemoAwsJson10ResponseTests1#response_GetThingOutput",
            DEMO + "ProtoDemoAwsJson10ResponseTests1#response_GetThingEmptyOutput",
            DEMO + "ProtoDemoAwsJson10ResponseTests1#error_ThingNotFoundError",
            DEMO + "ProtoDemoAwsJson10ResponseTests1#error_ServiceFaultError",
            DEMO + "ProtoDemoAwsJson10RequestTests1#request_WatchThingsEventStream",
            stream + "StreamDemoRequestTests1#request_UploadThingStream",
            stream + "StreamDemoResponseTests1#response_DownloadThingStream");

        assertThat(results.keySet()).as(describe(results))
                                    .containsExactlyInAnyOrder("response_GetThingOutput", "response_GetThingEmptyOutput",
                                                               "error_ThingNotFoundError", "error_ServiceFaultError",
                                                               "request_UploadThingStream", "response_DownloadThingStream");
        assertThat(results.values()).as(describe(results))
                                    .allMatch(r -> r.getStatus() == TestExecutionResult.Status.SUCCESSFUL);
    }

    @Test
    void failingGeneratedTest_reportsTheMismatch() throws Exception {
        // The fixture's PutThingAllTypes body lists only Name, so the generated body assertion fails against the
        // client's serialization once the method, URI and header assertions before it pass: the generated test compares
        // against the case, not against the SDK's own output.
        Map<String, TestExecutionResult> results = run(DEMO + "ProtoDemoAwsJson10RequestTests1#request_PutThingAllTypes");


        TestExecutionResult result = results.get("request_PutThingAllTypes");
        assertThat(result.getStatus()).as(describe(results)).isEqualTo(TestExecutionResult.Status.FAILED);
        assertThat(result.getThrowable().get()).isInstanceOf(AssertionError.class)
                                             .hasMessageContaining("JSON bodies differ")
                                             .hasMessageContaining("$.Big: unexpected member bigdecimal 9007199254740993");
    }

    private static Map<String, TestExecutionResult> run(String... methods) throws Exception {
        Map<String, TestExecutionResult> results = new LinkedHashMap<>();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classes);
        try {
            LauncherDiscoveryRequestBuilder request = LauncherDiscoveryRequestBuilder.request();
            for (String method : methods) {
                request.selectors(selectMethod(classes.loadClass(method.substring(0, method.indexOf('#'))),
                                               method.substring(method.indexOf('#') + 1)));
            }
            LauncherDiscoveryRequest discovery = request.build();
            Launcher launcher = LauncherFactory.create();
            launcher.execute(discovery, new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
                    if (identifier.isTest()) {
                        String id = identifier.getUniqueId();
                        String name = id.substring(id.lastIndexOf("[method:") + "[method:".length(), id.lastIndexOf('('));
                        results.put(name, result);
                    }
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
        return results;
    }

    private static String describe(Map<String, TestExecutionResult> results) {
        StringBuilder text = new StringBuilder();
        results.forEach((name, result) -> {
            text.append(name).append(": ").append(result.getStatus());
            result.getThrowable().ifPresent(t -> text.append(" ").append(t));
            text.append('\n');
        });
        return text.toString();
    }
}
