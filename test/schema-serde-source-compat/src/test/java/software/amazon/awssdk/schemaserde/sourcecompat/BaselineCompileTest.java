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

package software.amazon.awssdk.schemaserde.sourcecompat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;

/**
 * Shows that the fixtures are Baseline_Build-era code: compiles them against the Baseline_Build JARs alone (Requirement
 * 16.8). Surefire compiles them against the feature build.
 *
 * <p>The Baseline_Build is the merge base of this branch, built by {@code scripts/schema-serde/build-size-baseline.sh}
 * into {@code source-compat.baselineDir} (see README.md). The test fails, naming the script, when it is missing.
 */
class BaselineCompileTest {
    private static final String BASELINE_COMMIT = "fc8b74e9fe21";
    private static final String BUILD_SCRIPT = "scripts/schema-serde/build-size-baseline.sh";

    private static Path baselineJars;
    private static Path fixtureSources;

    @BeforeAll
    static void locateInputs() throws IOException {
        Path baselineDir = Paths.get(requiredProperty("source-compat.baselineDir"));
        fixtureSources = Paths.get(requiredProperty("source-compat.fixtureSources"));
        baselineJars = baselineDir.resolve("jars");
        Path buildInfo = baselineDir.resolve("build-info.properties");
        if (!Files.isRegularFile(buildInfo) || !Files.isDirectory(baselineJars)) {
            throw new IllegalStateException("No Baseline_Build in " + baselineDir + ". Build it with " + BUILD_SCRIPT);
        }
        Properties info = new Properties();
        try (InputStream in = Files.newInputStream(buildInfo)) {
            info.load(in);
        }
        assertThat(info.getProperty("baseline.commit"))
            .as("Baseline_Build commit in %s", buildInfo)
            .startsWith(BASELINE_COMMIT);
    }

    @Test
    void fixtures_compileAgainstBaselineBuild(@TempDir Path out) throws Exception {
        List<File> sources;
        try (Stream<Path> files = Files.list(fixtureSources)) {
            sources = files.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
        }
        assertThat(sources).extracting(File::getName)
                           .contains("CloudWatchFixture.java", "SqsFixture.java", "DynamoDbFixture.java",
                                     "SecretsManagerFixture.java", "AccountFixture.java", "Route53Fixture.java",
                                     "SnsFixture.java", "LegacyDocumentFixture.java");

        List<Diagnostic<? extends JavaFileObject>> errors = compile(sources, out);

        assertThat(errors).as("errors compiling the fixtures against %s:%n%s", baselineJars, describe(errors)).isEmpty();
        assertThat(out.resolve("software/amazon/awssdk/schemaserde/sourcecompat/fixtures/CloudWatchFixture.class"))
            .isRegularFile();
    }

    /**
     * The check discriminates: code that uses a feature-only API does not compile against the Baseline_Build.
     */
    @Test
    void featureOnlyApi_failsAgainstBaselineBuild(@TempDir Path dir) throws Exception {
        Path source = dir.resolve("FeatureOnly.java");
        Files.write(source, Arrays.asList(
            "class FeatureOnly {",
            "    Object schema = software.amazon.awssdk.services.cloudwatch.model.MetricAlarm.SCHEMA;",
            "}"), StandardCharsets.UTF_8);
        Path out = Files.createDirectory(dir.resolve("out"));

        List<Diagnostic<? extends JavaFileObject>> errors = compile(Arrays.asList(source.toFile()), out);

        assertThat(describe(errors)).contains("cannot find symbol").contains("SCHEMA");
    }

    private static List<Diagnostic<? extends JavaFileObject>> compile(List<File> sources, Path out)
        throws IOException, URISyntaxException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            List<String> options = Arrays.asList("--release", "8", "-proc:none", "-Xlint:-options",
                                                 "-classpath", baselineClasspath(), "-d", out.toString());
            Boolean ok = compiler.getTask(null, files, diagnostics, options, null, files.getJavaFileObjectsFromFiles(sources))
                                 .call();
            List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(d);
                }
            }
            assertThat(ok).as("javac result agrees with its diagnostics").isEqualTo(errors.isEmpty());
            return errors;
        }
    }

    /**
     * The Baseline_Build JARs (the services and their software.amazon.awssdk runtime dependencies) and reactive-streams,
     * the one third-party API type the fixtures' async paginator needs. Nothing from the feature build is on it.
     */
    private static String baselineClasspath() throws IOException, URISyntaxException {
        List<String> entries;
        try (Stream<Path> jars = Files.list(baselineJars)) {
            entries = jars.filter(p -> p.toString().endsWith(".jar")).map(Path::toString).sorted().collect(Collectors.toList());
        }
        assertThat(entries).extracting(e -> Paths.get(e).getFileName().toString())
                           .contains("cloudwatch.jar", "sqs.jar", "dynamodb.jar", "secretsmanager.jar", "account.jar",
                                     "route53.jar", "sns.jar", "sdk-core.jar", "http-client-spi.jar");
        entries.add(Paths.get(Publisher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        return String.join(File.pathSeparator, entries);
    }

    private static String describe(List<Diagnostic<? extends JavaFileObject>> errors) {
        return errors.stream()
                     .map(d -> (d.getSource() == null ? "" : d.getSource().getName() + ":" + d.getLineNumber() + ": ")
                               + d.getMessage(null))
                     .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("System property " + name + " is not set; run the test through Maven");
        }
        return value;
    }
}
