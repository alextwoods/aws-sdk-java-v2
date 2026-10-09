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

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;

/**
 * Module-path smoke (task 18.9; Requirements 16.10, 19.13, 19.14): the named fixture module
 * ({@code src/test/fixture}) is compiled with javac on the module path and run in a forked
 * {@code java --module-path ... -m} JVM with no class path, building a generated DynamoDB client over every supported
 * HTTP Implementation mode and completing one modeled call per mode against a loopback fake.
 */
class ModulePathSmokeTest {
    private static final String V3_GROUP = "software.amazon.awssdk";

    @Test
    void everyV3ArtifactHasAStableModuleNameAndNoPackageIsSplit() throws IOException {
        Map<String, Path> jars = SmokeFixture.moduleJars();
        Map<String, String> moduleOwners = new TreeMap<>();
        Map<String, Set<String>> packageOwners = new TreeMap<>();
        List<String> unnamed = new ArrayList<>();
        for (Map.Entry<String, Path> jar : jars.entrySet()) {
            String gav = jar.getKey();
            // Throws FindException for a jar the module system cannot use (an invalid derived or declared name).
            Set<ModuleReference> found = ModuleFinder.of(jar.getValue()).findAll();
            assertThat(found).as("module of %s", gav).hasSize(1);
            ModuleDescriptor descriptor = found.iterator().next().descriptor();
            if (gav.startsWith(V3_GROUP + ":") && !gav.split(":")[2].startsWith("2.")) {
                if (descriptor.isAutomatic() && declaredAutomaticName(jar.getValue()) == null) {
                    unnamed.add(gav);
                }
                assertThat(descriptor.name()).as("module name of %s", gav).startsWith("software.amazon.awssdk.");
            }
            String previous = moduleOwners.put(descriptor.name(), gav);
            assertThat(previous).as("module %s is declared by both %s and %s", descriptor.name(), previous, gav)
                                .isNull();
            for (String pkg : descriptor.packages()) {
                packageOwners.computeIfAbsent(pkg, k -> new TreeSet<>()).add(gav);
            }
        }
        assertThat(unnamed).as("V3 artifacts without an explicit module-info or Automatic-Module-Name (16.10)")
                           .isEmpty();
        Map<String, Set<String>> split = new TreeMap<>();
        packageOwners.forEach((pkg, owners) -> {
            if (owners.size() > 1) {
                split.put(pkg, owners);
            }
        });
        assertThat(split).as("split packages on the module path").isEmpty();
        // Every artifact of the module path resolves together, as the module system requires.
        assertThat(ModuleFinder.of(jars.values().toArray(Path[]::new)).findAll()).hasSize(jars.size());
        assertThat(moduleOwners).containsEntry("software.amazon.awssdk.services.dynamodb",
                                               V3_GROUP + ":dynamodb:3.0.0-SNAPSHOT")
                                .containsEntry("software.amazon.awssdk.core", V3_GROUP + ":sdk-core:3.0.0-SNAPSHOT")
                                .containsEntry("software.amazon.awssdk.http", V3_GROUP + ":http-spi:3.0.0-SNAPSHOT");
    }

    @Test
    void noForbiddenDependencyIsOnTheModulePath() throws IOException {
        ForbiddenDependencies policy = ForbiddenDependencies.load(SmokeFixture.policy());
        Map<String, List<String>> violations = new TreeMap<>();
        for (String gav : SmokeFixture.moduleJars().keySet()) {
            List<String> classes = policy.violations(gav);
            if (!classes.isEmpty()) {
                violations.put(gav, classes);
            }
        }
        assertThat(violations).as("forbidden dependency classes of dependency-policy.xml").isEmpty();
    }

    @Test
    void generatedClientCompletesAModeledCallOverEveryHttpModeOnTheModulePath() throws Exception {
        Path app = SmokeFixture.compiledModule();
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        SmokeFixture.Run run = SmokeFixture.run(
            List.of(java, "--module-path", SmokeFixture.modulePath(List.of(app)),
                    "-m", SmokeFixture.MODULE + "/" + SmokeFixture.MAIN_CLASS),
            Duration.ofMinutes(5));
        assertThat(run.exit()).as("forked module-path run:%n%s", run.output()).isZero();
        assertThat(run.lines("DONE")).as(run.output()).hasSize(1);
        assertThat(run.lines("APP ")).as("the application runs as a named module").containsExactly(
            "APP " + SmokeFixture.MODULE);

        List<String> modes = new ArrayList<>();
        for (String line : run.lines("MODE ")) {
            String[] fields = line.split(" ", 5);
            assertThat(fields[2]).as(line).isEqualTo("OK");
            assertThat(fields[3]).as("transport module of %s", fields[1])
                                 .isEqualTo(SmokeFixture.TRANSPORT_MODULES.get(fields[1]));
            assertThat(fields[4]).as(line).isEqualTo("[module-path-smoke]");
            modes.add(fields[1]);
        }
        assertThat(modes).as("every supported HTTP mode completed a call")
                         .containsExactlyInAnyOrderElementsOf(SmokeFixture.TRANSPORT_MODULES.keySet());

        // The resolved module graph: only module-path artifacts and system modules, nothing forbidden.
        Map<String, String> jarsByUri = new TreeMap<>();
        SmokeFixture.moduleJars().forEach((gav, jar) -> jarsByUri.put(jar.toUri().toString(), gav));
        ForbiddenDependencies policy = ForbiddenDependencies.load(SmokeFixture.policy());
        Set<String> resolved = new TreeSet<>();
        List<String> problems = new ArrayList<>();
        for (String line : run.lines("RESOLVED ")) {
            String[] fields = line.split(" ", 3);
            resolved.add(fields[1]);
            String location = fields[2];
            if (location.startsWith("jrt:")) {
                continue;
            }
            String gav = jarsByUri.get(URI.create(location).toString());
            if (gav == null) {
                if (!location.equals(app.toUri().toString()) && !Path.of(URI.create(location)).equals(app)) {
                    problems.add(fields[1] + " resolved from outside the module path: " + location);
                }
                continue;
            }
            List<String> classes = policy.violations(gav);
            if (!classes.isEmpty()) {
                problems.add(fields[1] + " (" + gav + ") is forbidden: " + classes);
            }
        }
        assertThat(problems).as("resolved module graph").isEmpty();
        assertThat(resolved).contains(SmokeFixture.MODULE, "software.amazon.awssdk.services.dynamodb",
                                      "software.amazon.awssdk.core", "software.amazon.awssdk.awscore",
                                      "software.amazon.awssdk.pipeline", "software.amazon.awssdk.http")
                            .containsAll(SmokeFixture.TRANSPORT_MODULES.values());
    }

    private static String declaredAutomaticName(Path jar) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            Manifest manifest = file.getManifest();
            return manifest == null ? null : manifest.getMainAttributes().getValue("Automatic-Module-Name");
        }
    }
}
