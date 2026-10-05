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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary.SMITHY_GROUP_ID;
import static software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary.SMITHY_JAVA_GROUP_ID;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.smithy.runtime.RuntimeClasspath.Artifact;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;

/**
 * Verifies the artifact layer of the Smithy Java runtime boundary against the Maven-resolved runtime dependency set of
 * {@code smithy-runtime}, which every generated service client inherits (Requirements 15.3, 15.4, 15.5).
 */
class SmithyRuntimeClasspathTest {

    @Test
    void runtimeDependencies_whenResolved_areExactlyTheCuratedSmithyJavaAndTransitionalSmithyArtifacts() {
        Set<String> expected = new TreeSet<>();
        SmithyRuntimeBoundary.SMITHY_JAVA_RUNTIME_ARTIFACTS.forEach(a -> expected.add(SMITHY_JAVA_GROUP_ID + ":" + a));
        SmithyRuntimeBoundary.TRANSITIONAL_SMITHY_ARTIFACTS.forEach(a -> expected.add(SMITHY_GROUP_ID + ":" + a));

        Set<String> actual = RuntimeClasspath.artifacts().stream()
                                             .map(Artifact::coordinates)
                                             .collect(Collectors.toCollection(TreeSet::new));

        // A closed set: no codegen, dynamic-schema, model-bundle, client/server, V2 bridge or third-party artifact.
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    void runtimeDependencies_whenResolved_usePinnedSmithyJavaAndSmithyReleases() {
        for (Artifact artifact : RuntimeClasspath.artifacts()) {
            String pinned = artifact.groupId().equals(SMITHY_JAVA_GROUP_ID) ? RuntimeClasspath.smithyJavaVersion()
                                                                            : RuntimeClasspath.smithyVersion();
            assertThat(artifact.version()).as(artifact.coordinates()).isEqualTo(pinned);
        }
    }

    @Test
    void runtimeDependencies_whenResolved_classpathJarsMatchResolvedArtifacts() {
        List<String> expectedJars = RuntimeClasspath.artifacts().stream()
                                                    .map(a -> a.artifactId() + "-" + a.version() + ".jar")
                                                    .sorted()
                                                    .toList();
        List<String> actualJars = RuntimeClasspath.jars().stream()
                                                  .map(jar -> jar.getFileName().toString())
                                                  .sorted()
                                                  .toList();

        assertThat(actualJars).isEqualTo(expectedJars);
    }

    @Test
    void smithyJavaRuntimeJars_whenScanned_containNoParserAssemblerOrCodegenClass() {
        for (Path jar : RuntimeClasspath.jars()) {
            if (!jar.getFileName().toString().startsWith("smithy-model-")) {
                assertThat(forbiddenClassesIn(jar)).as(jar.getFileName().toString()).isEmpty();
            }
        }
    }

    /**
     * Records the known limitation of the pinned Smithy Java release: smithy-model is the only runtime artifact that
     * contains forbidden classes, while the shape-identity, trait and node classes that Schema exposes are inside the
     * boundary. When Smithy Java publishes a lightweight shape-identity/runtime-trait artifact and smithy-model leaves
     * the runtime classpath, the closed-set test above fails and this limitation is removed.
     */
    @Test
    void smithyModelJar_whenScanned_containsTheParserButKeepsSchemaTypesInsideTheBoundary() {
        Path smithyModel = RuntimeClasspath.jar("smithy-model", RuntimeClasspath.smithyVersion());
        List<String> forbidden = forbiddenClassesIn(smithyModel);

        assertThat(forbidden).contains(
            "software.amazon.smithy.model.Model",
            "software.amazon.smithy.model.loader.ModelAssembler",
            "software.amazon.smithy.model.loader.IdlModelLoader");
        assertThat(forbidden).noneMatch(name -> name.startsWith("software.amazon.smithy.model.shapes.Shape")
                                                || name.startsWith("software.amazon.smithy.model.traits.")
                                                || name.startsWith("software.amazon.smithy.model.node."));
    }

    private static List<String> forbiddenClassesIn(Path jar) {
        List<String> forbidden = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            zip.stream()
               .map(ZipEntry::getName)
               .filter(name -> name.endsWith(".class") && !name.startsWith("META-INF/"))
               .map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
               .filter(SmithyRuntimeBoundary::isForbiddenRuntimeClass)
               .forEach(forbidden::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return forbidden;
    }
}
