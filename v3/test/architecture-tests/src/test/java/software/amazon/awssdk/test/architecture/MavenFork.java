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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Forks the Maven installation running the outer build ({@code -Dmaven.home}) on the running JDK, with the outer
 * build's local repository ({@code -Dv3.localRepository}) and offline setting ({@code -Dv3.offline}).
 */
final class MavenFork {

    private MavenFork() {
    }

    record Result(int exitCode, String output) {
    }

    /** Runs {@code mvn -B -fae <arguments>} in {@code directory}; the combined output is kept in build.log. */
    static Result run(Path directory, String... arguments) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path mvn = Path.of(System.getProperty("maven.home"), "bin", windows ? "mvn.cmd" : "mvn");
        List<String> command = new ArrayList<>(List.of(mvn.toString(), "-B", "-fae",
                                                       "-Dmaven.repo.local=" + System.getProperty("v3.localRepository")));
        if (Boolean.getBoolean("v3.offline")) {
            command.add("-o");
        }
        command.addAll(List.of(arguments));
        Path log = directory.resolve("build.log");
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
                                                            .redirectErrorStream(true)
                                                            .redirectOutput(log.toFile());
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        builder.environment().remove("MAVEN_ARGS");
        Process process = builder.start();
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new AssertionError("Forked Maven build timed out: " + command);
        }
        return new Result(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
    }

    static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /** Deletes {@code path} and everything below it, if it exists. */
    static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
