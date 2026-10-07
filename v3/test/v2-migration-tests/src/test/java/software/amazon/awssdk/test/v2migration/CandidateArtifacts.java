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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The release-candidate artifacts migrated corpus projects compile and run against: this module's test dependencies
 * on V3 artifacts, as Maven resolved them in this build (reactor outputs in a reactor build, the local repository
 * otherwise). {@code maven-dependency-plugin} writes their files ({@code dependency-list.txt}) and their verbose
 * dependency graph ({@code dependency-tree.txt}) to {@code -Dv3.corpus.candidate}; the class path of one migrated
 * project is the graph closure of the SDK artifacts its migrated POM selects.
 */
final class CandidateArtifacts {
    private static final Pattern LISTED = Pattern.compile(
        "^\\s*([^:\\s]+):([^:\\s]+):([^:\\s]+):(?:([^:\\s]+):)?([^:\\s]+):(compile|provided|runtime|test|system):"
        + "(.+?)(?:\\s+--\\s+module\\s+.*)?$");

    private final Map<String, Path> files;
    private final DependencyGraph graph;

    private CandidateArtifacts(Map<String, Path> files, DependencyGraph graph) {
        this.files = files;
        this.graph = graph;
    }

    static CandidateArtifacts load(Path directory) throws IOException {
        Path list = directory.resolve("dependency-list.txt");
        Path tree = directory.resolve("dependency-tree.txt");
        if (!Files.isRegularFile(list) || !Files.isRegularFile(tree)) {
            throw new AssertionError("No candidate dependency list/tree in " + directory + "; run this module through "
                                     + "Maven (generate-test-resources) before its tests");
        }
        Map<String, Path> files = new LinkedHashMap<>();
        for (String line : Files.readAllLines(list, StandardCharsets.UTF_8)) {
            Matcher m = LISTED.matcher(line);
            if (m.matches()) {
                files.put(m.group(1) + ":" + m.group(2), Path.of(m.group(7).strip()));
            }
        }
        if (files.isEmpty()) {
            throw new AssertionError("No resolved artifacts in " + list);
        }
        return new CandidateArtifacts(files, DependencyGraph.parse(Files.readString(tree, StandardCharsets.UTF_8),
                                                                   tree.toString()));
    }

    /** True if {@code groupId:artifactId} is resolved for this build. */
    boolean has(String key) {
        return files.containsKey(key);
    }

    /** The files of every resolved artifact in {@code keys}. */
    List<Path> files(Set<String> keys) {
        List<Path> out = new ArrayList<>();
        files.forEach((key, file) -> {
            if (keys.contains(key)) {
                out.add(file);
            }
        });
        return out;
    }

    /** The files of {@code roots} and everything they depend on at run time. */
    List<Path> classpath(Set<String> roots) {
        List<Path> out = new ArrayList<>();
        for (String key : graph.closure(roots)) {
            Path file = files.get(key);
            if (file == null) {
                throw new AssertionError("The candidate artifact " + key + " was not resolved for this build");
            }
            out.add(file);
        }
        return out;
    }
}
