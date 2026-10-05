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

package software.amazon.awssdk.smithy.runtime;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Maven-resolved runtime (compile + runtime scope) dependency set of {@code smithy-runtime}, recorded by the
 * {@code maven-dependency-plugin} executions in this module's POM and passed to Surefire as system properties. Test
 * dependencies are never part of this set.
 */
final class RuntimeClasspath {

    /** {@code groupId:artifactId:type[:classifier]:version:scope}, as written by {@code dependency:list}. */
    private static final Pattern DEPENDENCY_LINE =
        Pattern.compile("^\\s*([^:\\s]+):([^:\\s]+):([^:\\s]+)(?::([^:\\s]+))?:([^:\\s]+):(compile|runtime)\\b.*$");

    private RuntimeClasspath() {
    }

    record Artifact(String groupId, String artifactId, String version, String scope) {
        String coordinates() {
            return groupId + ":" + artifactId;
        }
    }

    static List<Artifact> artifacts() {
        List<Artifact> artifacts = new ArrayList<>();
        for (String line : readLines(requiredPath("smithy-runtime.dependencies"))) {
            Matcher matcher = DEPENDENCY_LINE.matcher(line);
            if (matcher.matches()) {
                artifacts.add(new Artifact(matcher.group(1), matcher.group(2), matcher.group(5), matcher.group(6)));
            }
        }
        if (artifacts.isEmpty()) {
            throw new IllegalStateException("No runtime dependencies were parsed from "
                                            + requiredPath("smithy-runtime.dependencies"));
        }
        return artifacts;
    }

    /** The runtime dependency jars of {@code smithy-runtime}, in Maven resolution order. */
    static List<Path> jars() {
        String classpath = String.join("", readLines(requiredPath("smithy-runtime.classpath"))).trim();
        List<Path> jars = new ArrayList<>();
        for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
            if (!entry.isBlank()) {
                jars.add(Path.of(entry));
            }
        }
        return jars;
    }

    static Path jar(String artifactId, String version) {
        String fileName = artifactId + "-" + version + ".jar";
        return jars().stream()
                     .filter(path -> path.getFileName().toString().equals(fileName))
                     .findFirst()
                     .orElseThrow(() -> new IllegalStateException(fileName + " is not on the runtime classpath"));
    }

    static Path mainClasses() {
        return requiredPath("smithy-runtime.classes");
    }

    static Path testClasses() {
        return requiredPath("smithy-runtime.test-classes");
    }

    static String smithyJavaVersion() {
        return requiredProperty("smithy-runtime.smithy-java.version");
    }

    static String smithyVersion() {
        return requiredProperty("smithy-runtime.smithy.version");
    }

    private static Path requiredPath(String property) {
        Path path = Path.of(requiredProperty(property));
        if (!Files.exists(path)) {
            throw new IllegalStateException(property + " points to a missing path: " + path);
        }
        return path;
    }

    private static String requiredProperty(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("System property " + property + " is not set; run the tests through Maven.");
        }
        return value;
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
