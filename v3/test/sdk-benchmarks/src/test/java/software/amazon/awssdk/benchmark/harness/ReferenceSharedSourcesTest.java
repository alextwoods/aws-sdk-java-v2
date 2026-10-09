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

package software.amazon.awssdk.benchmark.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The sources every reference client compiles with its own SDK ({@link ClientDriver#REFERENCE_SHARED_SOURCES}) are
 * SDK-free: they import only the JDK, Smithy {@code Node} JSON and each other, and name no other harness class. The V2
 * baseline shares V3's package names, so a V3 type here would bind to V2 (or fail) on the reference class path. Also
 * checks the V2 reference module compiles exactly this list.
 */
class ReferenceSharedSourcesTest {
    private static final Path SOURCES = Path.of("src/main/java/software/amazon/awssdk/benchmark/harness");
    private static final Path V2_POM = Path.of("../sdk-benchmarks-v2-reference/pom.xml");
    private static final Pattern IMPORT = Pattern.compile("^import (static )?([\\w.]+)", Pattern.MULTILINE);
    private static final Pattern IDENT = Pattern.compile("\\b[A-Z]\\w*\\b");
    private static final Pattern INCLUDE = Pattern.compile(
        "<include>software/amazon/awssdk/benchmark/harness/(\\w+)\\.java</include>");

    @Test
    void sharedSourcesAreSdkFree() throws IOException {
        Set<String> shared = Set.copyOf(ClientDriver.REFERENCE_SHARED_SOURCES);
        Set<String> harness = new TreeSet<>();
        try (var files = Files.list(SOURCES)) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".java"))
                 .forEach(n -> harness.add(n.substring(0, n.length() - 5)));
        }
        List<String> problems = new ArrayList<>();
        for (String name : shared) {
            String src = Files.readString(SOURCES.resolve(name + ".java"));
            Matcher m = IMPORT.matcher(src);
            while (m.find()) {
                String imported = m.group(2);
                if (!imported.startsWith("java.") && !imported.startsWith("software.amazon.smithy.model.node.")) {
                    problems.add(name + " imports " + imported);
                }
            }
            String code = src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "")
                             .replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
            Matcher id = IDENT.matcher(code);
            Set<String> used = new TreeSet<>();
            while (id.find()) {
                used.add(id.group());
            }
            for (String u : used) {
                if (harness.contains(u) && !shared.contains(u)) {
                    problems.add(name + " uses harness class " + u + ", which is not shared");
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void v2ReferenceModuleCompilesExactlyTheSharedSources() throws IOException {
        Matcher m = INCLUDE.matcher(Files.readString(V2_POM));
        Set<String> included = new TreeSet<>();
        while (m.find()) {
            included.add(m.group(1));
        }
        assertEquals(new TreeSet<>(ClientDriver.REFERENCE_SHARED_SOURCES), included);
    }
}
