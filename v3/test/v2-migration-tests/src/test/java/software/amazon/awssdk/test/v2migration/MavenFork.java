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

package software.amazon.awssdk.test.v2migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Forks the Maven installation running the outer build ({@code -Dmaven.home}) on the running JDK, with the outer
 * build's local repository ({@code -Dv3.localRepository}) and offline setting ({@code -Dv3.offline}).
 */
final class MavenFork {
    private static final long TIMEOUT_MINUTES = 5;

    private MavenFork() {
    }

    /** Runs {@code mvn -B <arguments>} in {@code directory}; the combined output is kept in {@code log}. */
    static void run(Path directory, Path log, String... arguments) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path mvn = Path.of(System.getProperty("maven.home"), "bin", windows ? "mvn.cmd" : "mvn");
        List<String> command = new ArrayList<>(List.of(mvn.toString(), "-B",
                                                       "-Dmaven.repo.local=" + System.getProperty("v3.localRepository")));
        if (Boolean.getBoolean("v3.offline")) {
            command.add("-o");
        }
        command.addAll(List.of(arguments));
        Files.createDirectories(log.getParent());
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
                                                            .redirectErrorStream(true)
                                                            .redirectOutput(log.toFile());
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        builder.environment().remove("MAVEN_ARGS");
        Process process = builder.start();
        if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new AssertionError("Forked Maven build timed out after " + TIMEOUT_MINUTES + " minutes: " + command);
        }
        if (process.exitValue() != 0) {
            throw new AssertionError("Forked Maven build failed (" + process.exitValue() + "): " + command + "\n"
                                     + Files.readString(log, StandardCharsets.UTF_8));
        }
    }
}
