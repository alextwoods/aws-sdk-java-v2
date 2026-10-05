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

package software.amazon.awssdk.smithy.runtime.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeVersionHandshakeTest {

    private static final String RELEASE = "3.0.0";
    private static final List<String> CLIENT_MODULES = List.of("s3", "aws-core", "sdk-core", "smithy-runtime");

    @TempDir
    Path tempDir;

    private final List<URLClassLoader> loaders = new ArrayList<>();

    @AfterEach
    void closeLoaders() throws IOException {
        for (URLClassLoader loader : loaders) {
            loader.close();
        }
    }

    @Test
    void alignedModulesPass() throws IOException {
        Path jar = moduleRoot("jar");
        for (String module : CLIENT_MODULES) {
            writeModule(jar, module, RELEASE);
        }

        RuntimeVersionHandshake.create(loaderFor(jar)).verify(RELEASE, CLIENT_MODULES);
    }

    @Test
    void reportsEveryMismatchedAndMissingModule() throws IOException {
        Path jar = moduleRoot("jar");
        writeModule(jar, "s3", RELEASE);
        writeModule(jar, "sdk-core", "3.0.1");
        writeModule(jar, "smithy-runtime", "3.1.0");
        // aws-core is missing.

        RuntimeVersionMismatchException e = assertThrows(
            RuntimeVersionMismatchException.class,
            () -> RuntimeVersionHandshake.create(loaderFor(jar)).verify(RELEASE, CLIENT_MODULES));

        assertEquals(RELEASE, e.expectedVersion());
        assertEquals(List.of("aws-core", "sdk-core", "smithy-runtime"),
                     e.conflicts().stream().map(RuntimeVersionConflict::artifactId).toList());
        assertTrue(e.conflicts().get(0).missing());
        assertEquals("3.0.1", e.conflicts().get(1).observed().get(0).version());
        assertEquals("3.1.0", e.conflicts().get(2).observed().get(0).version());

        String message = e.getMessage();
        assertTrue(message.contains("exactly 3.0.0"), message);
        assertTrue(message.contains("3 modules conflict"), message);
        assertTrue(message.contains("aws-core: missing (no META-INF/aws-sdk-java-v3/modules/aws-core.properties"), message);
        assertTrue(message.contains("sdk-core: found 3.0.1 ("), message);
        assertTrue(message.contains("smithy-runtime: found 3.1.0 ("), message);
        assertTrue(message.contains("software.amazon.awssdk:bom:3.0.0"), message);
        assertTrue(!message.contains("s3:"), message);
    }

    @Test
    void duplicateCopiesWithDifferentVersionsAreAllReported() throws IOException {
        Path first = moduleRoot("first");
        Path second = moduleRoot("second");
        writeModule(first, "sdk-core", RELEASE);
        writeModule(second, "sdk-core", "3.0.1");

        RuntimeVersionMismatchException e = assertThrows(
            RuntimeVersionMismatchException.class,
            () -> RuntimeVersionHandshake.create(loaderFor(first, second)).verify(RELEASE, List.of("sdk-core")));

        RuntimeVersionConflict conflict = e.conflicts().get(0);
        assertEquals(List.of(RELEASE, "3.0.1"), conflict.observed().stream().map(RuntimeModuleVersion::version).toList());
        assertTrue(e.getMessage().contains("sdk-core: found 3.0.0 (") && e.getMessage().contains(", 3.0.1 ("),
                   e.getMessage());
    }

    @Test
    void duplicateCopiesWithTheExpectedVersionAreAccepted() throws IOException {
        Path first = moduleRoot("first");
        Path second = moduleRoot("second");
        writeModule(first, "sdk-core", RELEASE);
        writeModule(second, "sdk-core", RELEASE);

        RuntimeVersionHandshake handshake = RuntimeVersionHandshake.create(loaderFor(first, second));
        handshake.verify(RELEASE, List.of("sdk-core"));
        assertEquals(2, handshake.discover(List.of("sdk-core")).get("sdk-core").size());
    }

    @Test
    void malformedResourcesAreConflicts() throws IOException {
        Path jar = moduleRoot("jar");
        write(jar, "sdk-core", "artifactId=sdk-core\n");
        write(jar, "aws-core", "artifactId=sdk-core\nversion=3.0.0\n");

        RuntimeVersionMismatchException e = assertThrows(
            RuntimeVersionMismatchException.class,
            () -> RuntimeVersionHandshake.create(loaderFor(jar)).verify(RELEASE, List.of("sdk-core", "aws-core")));

        assertEquals(List.of("aws-core", "sdk-core"),
                     e.conflicts().stream().map(RuntimeVersionConflict::artifactId).toList());
        assertTrue(e.getMessage().contains("aws-core: found 3.0.0 declaring artifactId=sdk-core ("), e.getMessage());
        assertTrue(e.getMessage().contains("sdk-core: found <no version> ("), e.getMessage());
    }

    @Test
    void findConflictsComparesExactVersionsAndIgnoresNonParticipants() {
        Map<String, List<RuntimeModuleVersion>> observed = Map.of(
            "sdk-core", List.of(version("sdk-core", "3.0.0-SNAPSHOT")),
            "aws-core", List.of(version("aws-core", RELEASE)),
            "netty-nio-client", List.of(version("netty-nio-client", "3.9.9")));

        List<RuntimeVersionConflict> conflicts = RuntimeVersionHandshake.findConflicts(
            RELEASE, List.of("sdk-core", "aws-core", "aws-core", "regions"), observed);

        assertEquals(List.of(new RuntimeVersionConflict("regions", RELEASE, List.of()),
                             new RuntimeVersionConflict("sdk-core", RELEASE,
                                                        List.of(version("sdk-core", "3.0.0-SNAPSHOT")))),
                     conflicts);
    }

    @Test
    void rejectsInvalidArguments() {
        RuntimeVersionHandshake handshake = RuntimeVersionHandshake.create(getClass().getClassLoader());
        assertThrows(IllegalArgumentException.class, () -> handshake.verify(" ", List.of("sdk-core")));
        assertThrows(IllegalArgumentException.class, () -> handshake.verify(RELEASE, List.of("")));
        assertThrows(NullPointerException.class, () -> RuntimeVersionHandshake.create(null));
    }

    @Test
    void buildGeneratesThisModulesVersionResource() {
        // The V3 parent POM generates META-INF/aws-sdk-java-v3/modules/<artifactId>.properties for every module.
        RuntimeVersionHandshake handshake = RuntimeVersionHandshake.create(getClass().getClassLoader());
        List<RuntimeModuleVersion> copies = handshake.discover(List.of("smithy-runtime")).get("smithy-runtime");

        assertEquals(1, copies.size(), copies.toString());
        RuntimeModuleVersion self = copies.get(0);
        assertEquals("smithy-runtime", self.artifactId());
        assertTrue(self.version().matches("3\\.\\d+\\.\\d+(-SNAPSHOT)?"), self.version());
        handshake.verify(self.version(), List.of("smithy-runtime"));
    }

    private static RuntimeModuleVersion version(String artifactId, String version) {
        return new RuntimeModuleVersion(artifactId, version, "test:" + artifactId);
    }

    private Path moduleRoot(String name) throws IOException {
        return Files.createDirectories(tempDir.resolve(name));
    }

    private static void writeModule(Path root, String artifactId, String version) throws IOException {
        write(root, artifactId, "groupId=software.amazon.awssdk\nartifactId=" + artifactId + "\nversion=" + version + "\n");
    }

    private static void write(Path root, String artifactId, String content) throws IOException {
        Path file = root.resolve(RuntimeVersionHandshake.resourceName(artifactId));
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.ISO_8859_1);
    }

    /** A class loader that sees only the given roots, so the build's own module resources do not interfere. */
    private ClassLoader loaderFor(Path... roots) throws MalformedURLException {
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toUri().toURL();
        }
        URLClassLoader loader = new URLClassLoader(urls, null);
        loaders.add(loader);
        return loader;
    }
}
