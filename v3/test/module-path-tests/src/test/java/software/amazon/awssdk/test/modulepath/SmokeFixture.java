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

package software.amazon.awssdk.test.modulepath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * The fixture application of the module-path and native-image smoke suites: its module path (the copied test-scope
 * closure, in repository layout), compilation with javac on that module path, and forked processes.
 */
final class SmokeFixture {
    static final String MODULE = "software.amazon.awssdk.test.modulepath.smoke";
    static final String MAIN_CLASS = MODULE + ".SmokeApp";

    /** The module each HTTP mode's transport class must be loaded from (its V2 module name). */
    static final Map<String, String> TRANSPORT_MODULES = Map.of(
        "apache-client:sync", "software.amazon.awssdk.http.apache",
        "apache5-client:sync", "software.amazon.awssdk.http.apache5",
        "url-connection-client:sync", "software.amazon.awssdk.http.urlconnection",
        "netty-nio-client:async", "software.amazon.awssdk.http.nio.netty",
        "aws-crt-client:sync", "software.amazon.awssdk.http.crt",
        "aws-crt-client:async", "software.amazon.awssdk.http.crt");

    private static Path compiled;

    private SmokeFixture() {
    }

    static Path moduleDirectory() {
        return Path.of(property("v3.modulePath.dir"));
    }

    static Path workDirectory() {
        return Path.of(property("v3.modulePath.work"));
    }

    static Path policy() {
        return Path.of(property("v3.modulePath.policy"));
    }

    /** Every jar of the module path, sorted, keyed by its {@code groupId:artifactId:version}. */
    static Map<String, Path> moduleJars() {
        Path root = moduleDirectory();
        try (Stream<Path> files = Files.walk(root)) {
            Map<String, Path> jars = new TreeMap<>();
            for (Path jar : files.filter(p -> p.toString().endsWith(".jar")).sorted().toList()) {
                jars.put(coordinates(root.relativize(jar)), jar);
            }
            assertThat(jars).as("module path %s", root).isNotEmpty();
            return jars;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The {@code groupId:artifactId:version} of a jar at {@code relative} in repository layout. */
    static String coordinates(Path relative) {
        int n = relative.getNameCount();
        assertThat(n).as("repository-layout path %s", relative).isGreaterThanOrEqualTo(4);
        String group = relative.subpath(0, n - 3).toString().replace(File.separatorChar, '.');
        return group + ":" + relative.getName(n - 3) + ":" + relative.getName(n - 2);
    }

    /** The module path string of the jars, optionally preceded by extra entries. */
    static String modulePath(List<Path> first) {
        List<String> entries = new ArrayList<>();
        first.forEach(p -> entries.add(p.toString()));
        moduleJars().values().forEach(p -> entries.add(p.toString()));
        return String.join(File.pathSeparator, entries);
    }

    /** Compiles the fixture module with javac on the module path, once per JVM; returns its exploded module. */
    static synchronized Path compiledModule() throws IOException {
        if (compiled != null) {
            return compiled;
        }
        Path sources = Path.of(property("v3.modulePath.fixture")).resolve(MODULE);
        Path out = workDirectory().resolve("classes").resolve(MODULE);
        Files.createDirectories(out);
        List<String> args = new ArrayList<>(List.of("--release", "21", "--module-path", modulePath(List.of()),
                                                    "-d", out.toString()));
        try (Stream<Path> files = Files.walk(sources)) {
            files.filter(p -> p.toString().endsWith(".java")).map(Path::toString).sorted().forEach(args::add);
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int exit = javac.run(null, diagnostics, diagnostics, args.toArray(String[]::new));
        assertThat(exit).as("javac of the fixture module on the module path:%n%s",
                            diagnostics.toString(StandardCharsets.UTF_8)).isZero();
        compiled = out;
        return out;
    }

    /** A completed forked process. */
    record Run(int exit, String output) {
        List<String> lines(String prefix) {
            return output.lines().filter(l -> l.startsWith(prefix)).collect(Collectors.toList());
        }
    }

    /** Runs {@code command} with no inherited class path and fails if it does not finish within {@code timeout}. */
    static Run run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().remove("CLASSPATH");
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("JDK_JAVA_OPTIONS");
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread pump = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                in.transferTo(output);
            } catch (IOException e) {
                // The process was destroyed; the timeout below reports it.
            }
        });
        pump.start();
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly().waitFor();
            pump.join();
            fail("%s did not finish within %s (Requirement 19.14):%n%s", command.get(0), timeout,
                 output.toString(StandardCharsets.UTF_8));
        }
        pump.join();
        return new Run(process.exitValue(), output.toString(StandardCharsets.UTF_8));
    }

    private static String property(String name) {
        String value = System.getProperty(name);
        assertThat(value).as("system property %s (set by the module's surefire configuration)", name).isNotBlank();
        return value;
    }
}
