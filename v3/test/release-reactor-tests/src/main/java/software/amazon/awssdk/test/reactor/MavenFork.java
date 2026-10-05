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

package software.amazon.awssdk.test.reactor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runs Maven in a separate process with an explicit JDK ({@code JAVA_HOME}), the way a contributor runs it from a
 * shell, and records every descendant process command line observed while the build runs so that callers can audit
 * what the build executed (for example, that no Gradle executable was started).
 *
 * <p>The forked build always gets {@code -B -ntp} (output formatting only), the caller's local repository and
 * offline settings, and {@link ReleaseReactorCommand#NESTED_BUILD_ENVIRONMENT_VARIABLE} so that reactor-forking tests
 * never recurse. {@code MAVEN_ARGS} is removed from the environment so that it cannot change the forked command.
 *
 * @param mavenHome Maven installation to use, or {@code null} to use {@code mvn} from {@code PATH}
 * @param localRepository local repository to use, or {@code null} for Maven's default
 * @param offline whether to pass {@code -o}
 * @param timeout maximum duration of one forked build
 */
public record MavenFork(Path mavenHome, Path localRepository, boolean offline, Duration timeout) {

    private static final long SAMPLE_INTERVAL_MILLIS = 100;

    /**
     * A fork configured from the surefire system properties shared by this module's tests: {@code maven.home},
     * {@code v3.localRepository}, and {@code v3.offline}.
     */
    public static MavenFork fromSystemProperties(Duration timeout) {
        return new MavenFork(optionalPath("maven.home"), optionalPath("v3.localRepository"),
                             Boolean.parseBoolean(System.getProperty("v3.offline", "false")), timeout);
    }

    /** Outcome of one forked build. */
    public record Result(List<String> command, int exitCode, Path log, Set<String> descendantCommandLines) {

        public Result {
            command = List.copyOf(command);
            descendantCommandLines = Set.copyOf(descendantCommandLines);
        }

        /** The combined stdout/stderr of the build. */
        public List<String> readLog() {
            try {
                return Files.readAllLines(log, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** A diagnostic with the command, exit code, and the last lines of the log. */
        public String describe() {
            List<String> lines = readLog();
            return "'" + String.join(" ", command) + "' exited with " + exitCode + "; see " + log + "\n"
                   + String.join("\n", lines.subList(Math.max(0, lines.size() - 60), lines.size()));
        }
    }

    /**
     * Runs {@code mvn <arguments>} in {@code workingDirectory} with the JDK at {@code javaHome}, writing combined
     * output to {@code log}.
     */
    public Result run(Path workingDirectory, Path javaHome, List<String> arguments, Path log)
        throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(mavenExecutable());
        command.add("-B");
        command.add("-ntp");
        if (offline) {
            command.add("-o");
        }
        if (localRepository != null) {
            command.add("-Dmaven.repo.local=" + localRepository);
        }
        command.addAll(arguments);
        Files.createDirectories(log.toAbsolutePath().getParent());
        ProcessBuilder builder = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile());
        Map<String, String> environment = builder.environment();
        environment.put("JAVA_HOME", javaHome.toString());
        environment.put(ReleaseReactorCommand.NESTED_BUILD_ENVIRONMENT_VARIABLE, "true");
        environment.remove("MAVEN_ARGS");
        Process process = builder.start();
        Set<String> descendants = new LinkedHashSet<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!process.waitFor(SAMPLE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)) {
            sampleDescendants(process, descendants);
            if (System.nanoTime() > deadline) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new IllegalStateException("'" + String.join(" ", command) + "' did not finish within "
                                                + timeout + "; see " + log);
            }
        }
        return new Result(command, process.exitValue(), log, descendants);
    }

    private static void sampleDescendants(Process process, Set<String> descendants) {
        process.descendants().forEach(handle -> {
            ProcessHandle.Info info = handle.info();
            info.commandLine().or(info::command).ifPresent(descendants::add);
        });
    }

    private String mavenExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String executable = windows ? "mvn.cmd" : "mvn";
        return mavenHome == null ? executable : mavenHome.resolve("bin").resolve(executable).toString();
    }

    private static Path optionalPath(String property) {
        String value = System.getProperty(property);
        return value == null || value.isBlank() || value.startsWith("${") ? null : Path.of(value);
    }
}
